package co.com.tmsolutions.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.hibernate.criterion.Restrictions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.com.tmsolutions.dao.UsuarioDao;
import co.com.tmsolutions.dao.UsuarioPartidoDao;
import co.com.tmsolutions.model.Equipo;
import co.com.tmsolutions.model.Partido;
import co.com.tmsolutions.model.PartidosUsuario;
import co.com.tmsolutions.model.Usuario;

/**
 * Sincroniza los resultados oficiales (usuario real@real.com) consultando la API
 * publica no oficial de ESPN. Se actualizan los partidos terminados cuyos dos
 * equipos ya estan definidos y que NO hayan sido cargados antes por la API
 * (atributo origen=api): es decir, los ingresados a mano se re-sincronizan
 * (sobreescriben) con el dato oficial, y los que ya puso la API no se re-tocan.
 * Tras actualizar, dispara el recalculo de puntajes de todos.
 *
 * No requiere API key. El emparejamiento con cada Partido se hace por el par de
 * equipos (no por numero de partido), traduciendo el nombre EN->ES.
 */
@Stateless
public class ResultadosSyncService {

	private static final String BASE = "https://site.api.espn.com/apis/site/v2/sports/soccer/fifa.world";
	private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
	// Partido inaugural de la Copa Mundial 2026
	private static final LocalDate INICIO_MUNDIAL = LocalDate.of(2026, 6, 11);
	private static final String REAL_MAIL = "real@real.com";

	@Inject
	private UsuarioDao usuarioDao;

	@Inject
	private UsuarioPartidoDao usuarioPartidoDao;

	private final ObjectMapper mapper = new ObjectMapper();

	public ResultadoSync sincronizar() {
		ResultadoSync r = new ResultadoSync();

		List<Usuario> reales = usuarioDao.findByCriteria(Restrictions.eq("mail", REAL_MAIL));
		if (reales.isEmpty()) {
			r.error = "No existe el usuario de resultados oficiales (" + REAL_MAIL + ").";
			return r;
		}
		Usuario real = reales.get(0);
		PartidosUsuario pu = usuarioPartidoDao.getPorUsuario(real);
		if (pu == null || pu.getPartidos() == null) {
			r.error = "El usuario de resultados oficiales aun no tiene partidos cargados.";
			return r;
		}
		List<Partido> partidos = pu.getPartidos();

		// 1) Descargar una sola vez los eventos de cada fecha del Mundial.
		List<JsonNode> eventos = new ArrayList<>();
		LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
		for (LocalDate d = INICIO_MUNDIAL; !d.isAfter(hoy); d = d.plusDays(1)) {
			JsonNode scoreboard;
			try {
				scoreboard = get(BASE + "/scoreboard?dates=" + d.format(YYYYMMDD));
			} catch (Exception e) {
				r.fechasConError.add(d.toString());
				continue;
			}
			for (JsonNode ev : scoreboard.path("events")) {
				eventos.add(ev);
			}
		}

		// 2) Aplicar resultados propagando el bracket entre rondas: al cerrarse una
		// llave (p. ej. la Ronda de 32) su ganador avanza a la siguiente fase
		// (octavos) en el bracket oficial, lo que (a) hace aparecer el partido de la
		// ronda siguiente con sus equipos y (b) habilita otorgar los puntos de
		// clasificacion. Sin esta propagacion, el ganador "no pasaba" a octavos y el
		// partido quedaba vacio. CalculadoraBracket.recalcular SOLO reescribe los
		// equipos (eq1/eq2) de cada cruce; nunca toca goles, penales ni tarjetas.
		// Se repite hasta que una pasada no cambie nada (cada ronda habilita la
		// siguiente para emparejarla con ESPN dentro del mismo sync).
		boolean huboCambios = false;
		boolean cambioEnPasada = true;
		int pasadas = 0;
		while (cambioEnPasada && pasadas < 8) {
			cambioEnPasada = false;
			new CalculadoraBracket().recalcular(partidos);
			for (JsonNode ev : eventos) {
				if (procesarEvento(ev, partidos, r)) {
					cambioEnPasada = true;
					huboCambios = true;
				}
			}
			pasadas++;
		}
		// Propaga el ganador de la ultima llave cerrada para que el partido de la
		// siguiente ronda aparezca con sus equipos en el bracket real.
		new CalculadoraBracket().recalcular(partidos);

		// Guardamos siempre: aunque no se hayan cargado marcadores nuevos, la
		// propagacion del bracket pudo dejar nuevos equipos en las rondas siguientes.
		usuarioPartidoDao.save(pu);
		// Recalcula siempre (aunque no haya cambios nuevos) para aplicar las reglas
		// de puntaje vigentes sobre los resultados ya cargados (p.ej. el bloqueo de
		// los terceros hasta que terminen todos los grupos).
		usuarioPartidoDao.calcularResultados(real);
		r.recalculado = true;
		return r;
	}

	/**
	 * @return true si actualizo algun Partido.
	 */
	private boolean procesarEvento(JsonNode ev, List<Partido> partidos, ResultadoSync r) {
		JsonNode comp = ev.path("competitions").path(0);
		boolean completado = comp.path("status").path("type").path("completed").asBoolean(false);
		if (!completado) {
			return false;
		}
		JsonNode competidores = comp.path("competitors");
		if (competidores.size() < 2) {
			return false;
		}
		JsonNode cA = competidores.get(0);
		JsonNode cB = competidores.get(1);
		String enA = cA.path("team").path("displayName").asText(null);
		String enB = cB.path("team").path("displayName").asText(null);
		String esA = EquiposNombreMapper.aEspanol(enA);
		String esB = EquiposNombreMapper.aEspanol(enB);
		if (esA == null || esB == null) {
			// procesarEvento se ejecuta en varias pasadas; evitamos duplicar el aviso.
			String sm = (enA == null ? "?" : enA) + " vs " + (enB == null ? "?" : enB);
			if (!r.sinMapeo.contains(sm)) {
				r.sinMapeo.add(sm);
			}
			return false;
		}

		Partido p = buscarPartido(partidos, esA, esB);
		if (p == null) {
			// Ya fue cargado por la API (origen=api, no se re-toca), o el bracket
			// todavia no tiene a estos equipos.
			return false;
		}

		boolean eq1EsA = esA.equals(nombre(p.getEq1()));
		int golA = cA.path("score").asInt(0);
		int golB = cB.path("score").asInt(0);
		int penA = cA.path("shootoutScore").asInt(0);
		int penB = cB.path("shootoutScore").asInt(0);

		p.setGoles1(eq1EsA ? golA : golB);
		p.setGoles2(eq1EsA ? golB : golA);
		p.setPenales1(eq1EsA ? penA : penB);
		p.setPenales2(eq1EsA ? penB : penA);

		// Las tarjetas solo influyen en el desempate de fase de grupos.
		if ("partidos_grupos".equals(p.getFase())) {
			aplicarTarjetas(ev.path("id").asText(null), p);
		}

		p.setRealizado(Boolean.TRUE);
		// Marca el partido como cargado por la API: en proximas sincronizaciones
		// no se vuelve a tocar (solo se re-sincronizan los ingresados a mano).
		p.getAtributos().put("origen", "api");
		r.actualizados.add(esA + " " + golA + " - " + golB + " " + esB);
		return true;
	}

	/** Consulta el summary del evento y asigna amarillas/rojas por equipo. */
	private void aplicarTarjetas(String eventId, Partido p) {
		if (eventId == null) {
			return;
		}
		JsonNode summary;
		try {
			summary = get(BASE + "/summary?event=" + eventId);
		} catch (Exception e) {
			return; // sin tarjetas; el marcador igual queda guardado
		}
		for (JsonNode t : summary.path("boxscore").path("teams")) {
			String es = EquiposNombreMapper.aEspanol(t.path("team").path("displayName").asText(null));
			if (es == null) {
				continue;
			}
			int amarillas = stat(t, "yellowCards");
			int rojas = stat(t, "redCards");
			if (es.equals(nombre(p.getEq1()))) {
				p.setTa1(amarillas);
				p.setTr1(rojas);
			} else if (es.equals(nombre(p.getEq2()))) {
				p.setTa2(amarillas);
				p.setTr2(rojas);
			}
		}
	}

	private int stat(JsonNode team, String nombreStat) {
		for (JsonNode s : team.path("statistics")) {
			if (nombreStat.equals(s.path("name").asText())) {
				return s.path("displayValue").asInt(0);
			}
		}
		return 0;
	}

	/**
	 * Busca un Partido aun no realizado cuyos dos equipos coincidan (en cualquier
	 * orden) con el par dado.
	 */
	private Partido buscarPartido(List<Partido> partidos, String esA, String esB) {
		for (Partido p : partidos) {
			// Los ya cargados por la API no se re-sincronizan; los manuales o
			// pendientes si (se sobreescriben con el dato oficial de ESPN).
			if ("api".equals(p.getAtributos().get("origen"))) {
				continue;
			}
			String n1 = nombre(p.getEq1());
			String n2 = nombre(p.getEq2());
			if (n1 == null || n2 == null) {
				continue;
			}
			if ((n1.equals(esA) && n2.equals(esB)) || (n1.equals(esB) && n2.equals(esA))) {
				return p;
			}
		}
		return null;
	}

	private String nombre(Equipo e) {
		return e == null ? null : (String) e.getAtributos().get("nombre");
	}

	private JsonNode get(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		// Liberty usa su propio truststore (key.p12) sin las CA publicas, lo que
		// rompe el handshake TLS contra sitios publicos como ESPN. Forzamos el uso
		// del truststore de CA publicas del JDK (cacerts) solo en esta conexion.
		if (con instanceof HttpsURLConnection) {
			SSLSocketFactory f = sslSocketFactory();
			if (f != null) {
				((HttpsURLConnection) con).setSSLSocketFactory(f);
			}
		}
		con.setRequestMethod("GET");
		con.setConnectTimeout(8000);
		con.setReadTimeout(12000);
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (PollaResultadosSync)");
		con.setRequestProperty("Accept", "application/json");
		try (InputStream in = con.getInputStream()) {
			return mapper.readTree(in);
		} finally {
			con.disconnect();
		}
	}

	private volatile SSLSocketFactory sslSocketFactory;

	/**
	 * SSLSocketFactory que confia en las CA publicas del JDK (cacerts), no en el
	 * truststore de Liberty. Se cachea. Devuelve null si no se pudo construir (en
	 * cuyo caso se usa el default del servidor).
	 */
	private SSLSocketFactory sslSocketFactory() {
		SSLSocketFactory f = sslSocketFactory;
		if (f != null) {
			return f;
		}
		synchronized (this) {
			if (sslSocketFactory != null) {
				return sslSocketFactory;
			}
			try {
				Path cacerts = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
				KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
				try (InputStream in = Files.newInputStream(cacerts)) {
					ks.load(in, "changeit".toCharArray());
				}
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(ks);
				SSLContext ctx = SSLContext.getInstance("TLS");
				ctx.init(null, tmf.getTrustManagers(), null);
				sslSocketFactory = ctx.getSocketFactory();
			} catch (Exception e) {
				sslSocketFactory = null;
			}
			return sslSocketFactory;
		}
	}

	/** Resumen de lo que hizo una sincronizacion. */
	public static class ResultadoSync implements Serializable {
		private static final long serialVersionUID = 1L;
		public final List<String> actualizados = new ArrayList<>();
		public final List<String> sinMapeo = new ArrayList<>();
		public final List<String> fechasConError = new ArrayList<>();
		public boolean recalculado;
		public String error;
	}
}
