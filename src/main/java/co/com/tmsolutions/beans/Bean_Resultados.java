package co.com.tmsolutions.beans;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import co.com.tmsolutions.dao.UsuarioDao;
import co.com.tmsolutions.dao.UsuarioPartidoDao;
import co.com.tmsolutions.model.Usuario;
import co.com.tmsolutions.service.ResultadosSyncService;

@Named("bean_Resultados")
@ViewScoped
public class Bean_Resultados implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final String ADMIN_MAIL = "real@real.com";

	// Valores definidos en las reglas (ver puntos.xhtml)
	private static final long VALOR_INGRESO = 110000L; // $110.000 COP por cupo
	private static final long APORTE_POZO = 100000L; // $100.000 COP al pozo de premios

	@Inject
	private Bean_User bean_User;
	@Inject
	private UsuarioDao usuarioDao;
	@Inject
	private UsuarioPartidoDao usuarioPartidoDao;
	@Inject
	private ResultadosSyncService resultadosSyncService;

	private List<Usuario> usuarios;
	private Set<String> usuariosLlenos;

	private boolean mundialIniciado;

	// Contadores de participantes
	private int totalParticipantes;
	private int totalLlenos;
	private int totalPendientes;

	// Contadores de consignación (pago)
	private int totalPagados;
	private int totalSinPago;

	// Pozo y premios (en COP)
	private long pozoTotal;
	private long premioAntipolla;
	private long pozoRestante;
	private long premioPrimero;
	private long premioSegundo;
	private long premioTercero;

	public Bean_Resultados() {
		super();
	}

	@PostConstruct
	private void init() {
		// Solo participantes reales (se excluye el usuario administrador con los
		// resultados oficiales) ordenados por nombre.
		usuarios = usuarioDao.findAll().stream().filter(u -> !ADMIN_MAIL.equals(u.getMail()))
				.sorted(Comparator.comparing(this::nombreUsuario, String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());

		usuariosLlenos = usuarioPartidoDao.usuariosConCampeon();

		totalParticipantes = usuarios.size();
		totalLlenos = (int) usuarios.stream().filter(u -> usuariosLlenos.contains(u.getId())).count();
		totalPendientes = totalParticipantes - totalLlenos;

		mundialIniciado = calcularMundialIniciado();

		recalcularPagos();
	}

	/**
	 * Recalcula los contadores de consignación y el pozo. El pozo refleja el dinero
	 * efectivamente recaudado, por lo que se arma solo con los participantes que ya
	 * consignaron.
	 */
	private void recalcularPagos() {
		totalPagados = (int) usuarios.stream().filter(this::isPago).count();
		totalSinPago = totalParticipantes - totalPagados;
		calcularPozo();
	}

	/**
	 * El mundial inicia (y se bloquean tanto la edición como la visualización de
	 * pronósticos ajenos) el 11 de junio de 2026 a la 1:00 p.m. hora de Bogotá.
	 */
	private boolean calcularMundialIniciado() {
		Calendar inicio = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota"));
		inicio.set(2026, Calendar.JUNE, 11, 13, 55, 0);
		Calendar ahora = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota"));
		return ahora.compareTo(inicio) > 0;
	}

	private void calcularPozo() {
		// El pozo lo arma el aporte de $100.000 de cada participante que ya consignó.
		pozoTotal = (long) totalPagados * APORTE_POZO;
		// Del pozo total se saca primero el premio Antipolla (devolución de inscripción).
		premioAntipolla = totalPagados > 0 ? VALOR_INGRESO : 0L;
		pozoRestante = Math.max(0L, pozoTotal - premioAntipolla);
		// El restante se reparte 70% / 20% / 10%.
		premioPrimero = Math.round(pozoRestante * 0.70);
		premioSegundo = Math.round(pozoRestante * 0.20);
		premioTercero = Math.round(pozoRestante * 0.10);
	}

	private String nombreUsuario(Usuario u) {
		Object nombre = u.getAtributos().get("nombre");
		return nombre != null ? nombre.toString() : "";
	}

	/**
	 * @return true si el usuario ya completó su pronóstico (tiene campeón definido).
	 */
	public boolean isLleno(Usuario u) {
		return u != null && usuariosLlenos != null && usuariosLlenos.contains(u.getId());
	}

	/**
	 * @return true si el usuario actual es el administrador (real@real.com).
	 */
	public boolean isAdmin() {
		Usuario actual = bean_User.getUsuario();
		return actual != null && ADMIN_MAIL.equals(actual.getMail());
	}

	/**
	 * @return true si el usuario ya realizó la consignación.
	 */
	public boolean isPago(Usuario u) {
		return u != null && Boolean.TRUE.equals(u.getAtributos().get("pago"));
	}

	/**
	 * Alterna y persiste el estado de consignación del usuario. Solo el
	 * administrador puede modificarlo.
	 */
	public void guardarPago(Usuario u) {
		if (!isAdmin() || u == null) {
			return;
		}
		boolean nuevo = !isPago(u);
		u.getAtributos().put("pago", nuevo);
		usuarioDao.merge(u);
		recalcularPagos();
		FacesMessage msg = new FacesMessage(
				(nuevo ? "Consignación registrada" : "Consignación retirada") + " para " + nombreUsuario(u), "");
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	/**
	 * Consulta la API de ESPN y actualiza los resultados oficiales
	 * (real@real.com). Solo el administrador puede ejecutarla. Cada partido se
	 * actualiza una sola vez (los ya realizados se omiten).
	 */
	public void sincronizarResultados() {
		if (!isAdmin()) {
			addMensaje(FacesMessage.SEVERITY_WARN, "Solo el administrador puede sincronizar resultados.");
			return;
		}
		ResultadosSyncService.ResultadoSync r;
		try {
			r = resultadosSyncService.sincronizar();
		} catch (Exception e) {
			addMensaje(FacesMessage.SEVERITY_ERROR, "Error al sincronizar: " + e.getClass().getSimpleName()
					+ (e.getMessage() != null ? " - " + e.getMessage() : ""));
			return;
		}
		if (r.error != null) {
			addMensaje(FacesMessage.SEVERITY_ERROR, r.error);
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(r.actualizados.size()).append(" partido(s) actualizado(s).");
		if (!r.sinMapeo.isEmpty()) {
			sb.append(" Sin emparejar (revisar nombres): ").append(String.join("; ", r.sinMapeo)).append('.');
		}
		if (!r.fechasConError.isEmpty()) {
			sb.append(" Fechas con error de conexión: ").append(String.join(", ", r.fechasConError)).append('.');
		}
		addMensaje(FacesMessage.SEVERITY_INFO, sb.toString());
		init(); // refrescar contadores y pozo
	}

	private void addMensaje(FacesMessage.Severity severidad, String texto) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, ""));
	}

	public Bean_User getBean_User() {
		return bean_User;
	}

	public void setBean_User(Bean_User bean_User) {
		this.bean_User = bean_User;
	}

	public UsuarioDao getUsuarioDao() {
		return usuarioDao;
	}

	public void setUsuarioDao(UsuarioDao usuarioDao) {
		this.usuarioDao = usuarioDao;
	}

	public List<Usuario> getUsuarios() {
		return usuarios;
	}

	public void setUsuarios(List<Usuario> usuarios) {
		this.usuarios = usuarios;
	}

	public boolean isMundialIniciado() {
		return mundialIniciado;
	}

	public int getTotalParticipantes() {
		return totalParticipantes;
	}

	public int getTotalLlenos() {
		return totalLlenos;
	}

	public int getTotalPendientes() {
		return totalPendientes;
	}

	public int getTotalPagados() {
		return totalPagados;
	}

	public int getTotalSinPago() {
		return totalSinPago;
	}

	public long getPozoTotal() {
		return pozoTotal;
	}

	public long getPremioAntipolla() {
		return premioAntipolla;
	}

	public long getPozoRestante() {
		return pozoRestante;
	}

	public long getPremioPrimero() {
		return premioPrimero;
	}

	public long getPremioSegundo() {
		return premioSegundo;
	}

	public long getPremioTercero() {
		return premioTercero;
	}

}
