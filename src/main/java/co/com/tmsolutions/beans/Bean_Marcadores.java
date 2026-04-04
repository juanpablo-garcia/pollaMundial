package co.com.tmsolutions.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.criterion.Restrictions;
import org.primefaces.context.PrimeFacesContext;

import co.com.tmsolutions.dao.UsuarioDao;
import co.com.tmsolutions.dao.UsuarioPartidoDao;
import co.com.tmsolutions.model.Equipo;
import co.com.tmsolutions.model.Partido;
import co.com.tmsolutions.model.PartidosUsuario;
import co.com.tmsolutions.model.Posicion;
import co.com.tmsolutions.model.Usuario;

@Named("bean_Marcadores")
@ViewScoped
public class Bean_Marcadores implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String firstName = "John";
	private String lastName = "Doe";
	private List<Equipo> equipos;
	private HashMap<Integer, List<Partido>> partidos_grupos = new HashMap<>();
	private HashMap<Integer, List<Posicion>> posiciones_grupos = new HashMap<>();
	private List<Partido> octavosFinal = new LinkedList<>();
	private List<Partido> cuartosFinal = new LinkedList<>();
	private List<Partido> semifinales = new LinkedList<>();
	private List<Partido> finales = new LinkedList<>();
	private List<Partido> ronda32 = new LinkedList<>();
	private Equipo campeon;
	@Inject
	private Bean_User bean_User;
	@Inject
	private UsuarioPartidoDao usuarioPartidoDao;
	@Inject
	private UsuarioDao usuarioDao;
	private String id_partidoUsuario;
	private boolean canedit = true;
	private String id_user;
	private String[] letras = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L" };

	public Bean_Marcadores() {
		super();
	}

	@PostConstruct
	private void init() {
		this.id_user = (String) FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.get("id");
		GenerarEquipos();
		cargarPartidoPorUsuario();
		System.out.println(partidos_grupos);
		ArmarPartidos();
		initPartidosRonda32();
		initPartidosOctavos();
		initPartidosCuartos();
		initPartidosSemifinales();
		initPartidosFinales();
		calcularPosiciones();
		verificarPuedeEditar();
		calcularCampeon();
	}

	private void verificarPuedeEditar() {
		Calendar c = Calendar.getInstance(TimeZone.getDefault());
		c.set(2026, Calendar.JUNE, 11, 7, 0);
		Calendar c1 = Calendar.getInstance();
		if (c1.compareTo(c) > 0) {
			canedit = false;
		}
		if (this.id_user != null) {
			this.canedit = false;
		}
		if (this.id_user == null && bean_User.getUsuario().getMail().equals("real@real.com")) {
			this.canedit = true;
		}

	}

	private void cargarPartidoPorUsuario() {
		Usuario us = bean_User.getUsuario();
		if (id_user != null) {
			us = usuarioDao.get(id_user);
		}
		List<PartidosUsuario> partidos = usuarioPartidoDao.findByCriteria(Restrictions.eq("usuario", us));
		PartidosUsuario partidoUsuario;
		if (!partidos.isEmpty()) {
			// Partidos de grupos
			partidoUsuario = partidos.get(0);
			id_partidoUsuario = partidoUsuario.getId();
			List<Partido> parts = partidoUsuario.getPartidos();

			List<Partido> grupos = parts.stream().filter(o -> o.getFase().equals("partidos_grupos"))
					.sorted((p1, p2) -> p1.getIdpartido().compareTo(p2.getIdpartido())).collect(Collectors.toList());
			for (int i = 0; i < 12; i++) {
				this.partidos_grupos.put(new Integer(i), new ArrayList<Partido>());
			}
			for (Partido p : grupos) {
				// Cada grupo tiene exactamente 6 partidos (ids secuenciales por grupo)
				int key = p.getIdpartido().intValue() / 6;
				if (key < 0 || key > 11)
					key = 11;
				this.partidos_grupos.get(new Integer(key)).add(p);
			}

			// Ronda de 32
			grupos = parts.stream().filter(o -> o.getFase().equals("ronda32"))
					.sorted((p1, p2) -> p1.getIdpartido().compareTo(p2.getIdpartido())).collect(Collectors.toList());
			for (Partido p : grupos) {
				this.ronda32.add(p);
			}

			// Octavos
			grupos = parts.stream().filter(o -> o.getFase().equals("octavosFinal"))
					.sorted((p1, p2) -> p1.getIdpartido().compareTo(p2.getIdpartido())).collect(Collectors.toList());
			for (Partido p : grupos) {
				this.octavosFinal.add(p);
			}

			// Cuartos
			grupos = parts.stream().filter(o -> o.getFase().equals("cuartosFinal"))
					.sorted((p1, p2) -> p1.getIdpartido().compareTo(p2.getIdpartido())).collect(Collectors.toList());
			for (Partido p : grupos) {
				this.cuartosFinal.add(p);
			}

			// Semis
			grupos = parts.stream().filter(o -> o.getFase().equals("semifinales"))
					.sorted((p1, p2) -> p1.getIdpartido().compareTo(p2.getIdpartido())).collect(Collectors.toList());
			for (Partido p : grupos) {
				this.semifinales.add(p);
			}

			// Finales
			grupos = parts.stream().filter(o -> o.getFase().equals("finales"))
					.sorted((p1, p2) -> p1.getIdpartido().compareTo(p2.getIdpartido())).collect(Collectors.toList());
			for (Partido p : grupos) {
				this.finales.add(p);
			}
		}
	}

	public void guardar() {
		verificarPuedeEditar();
		if (this.canedit) {
			List<Partido> partidos = new ArrayList<>();

			for (Iterator iterator = partidos_grupos.keySet().iterator(); iterator.hasNext();) {
				Integer key = (Integer) iterator.next();
				partidos.addAll(this.partidos_grupos.get(key));
			}
			partidos.addAll(this.ronda32);
			partidos.addAll(this.octavosFinal);
			partidos.addAll(this.cuartosFinal);
			partidos.addAll(this.semifinales);
			partidos.addAll(this.finales);
			this.id_partidoUsuario = usuarioPartidoDao.savePartidos(id_partidoUsuario, partidos,
					bean_User.getUsuario());
			// Si se esta guardando el real se debe calcular los puntajes de todos
			if (bean_User.getUsuario().getMail().equals("real@real.com")) {
				usuarioPartidoDao.calcularResultados(bean_User.getUsuario());
			}
			addMessage("Marcadores guardados");
		} else {
			addMessage("No se puede modificar");
		}

	}

	private void addMessage(String ms) {
		FacesMessage message = new FacesMessage(ms, "");
		PrimeFacesContext.getCurrentInstance().addMessage(null, message);

	}

	private void initPartidosFinales() {
		if (finales.isEmpty()) {
			// Primero grupo A contra tercero
			int i = 0;
			String fase = "finales";
			Partido p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "final");
			p1.getAtributos().put("eq1", "Ganador semi1");
			p1.getAtributos().put("eq2", "Ganador semi2");
			finales.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "tercero y cuarto");
			p1.getAtributos().put("eq1", "Perdedor semi1");
			p1.getAtributos().put("eq2", "Perdedor semi2");
			finales.add(p1);
		}

	}

	private void initPartidosSemifinales() {
		if (semifinales.isEmpty()) {
			int i = 0;
			String fase = "semifinales";
			// Primero grupo A contra tercero
			Partido p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "Semifinal_101");
			p1.getAtributos().put("eq1", "Ganador cuartos_97");
			p1.getAtributos().put("eq2", "Ganador cuartos_98");
			semifinales.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "Semifinal_102");
			p1.getAtributos().put("eq1", "Ganador cuartos_99");
			p1.getAtributos().put("eq2", "Ganador cuartos_100");
			semifinales.add(p1);
		}
	}

	private void initPartidosOctavos() {

		if (octavosFinal.isEmpty()) {
			int i = 0;
			String fase = "octavosFinal";
			Partido p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "octavos_89");
			p1.getAtributos().put("eq1", "Ganador r32_74");
			p1.getAtributos().put("eq2", "Ganador r32_77");
			octavosFinal.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "octavos_90");
			p1.getAtributos().put("eq1", "Ganador r32_73");
			p1.getAtributos().put("eq2", "Ganador r32_75");
			octavosFinal.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "octavos_91");
			p1.getAtributos().put("eq1", "Ganador r32_76");
			p1.getAtributos().put("eq2", "Ganador r32_78");
			octavosFinal.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "octavos_92");
			p1.getAtributos().put("eq1", "Ganador r32_79");
			p1.getAtributos().put("eq2", "Ganador r32_80");
			octavosFinal.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "octavos_93");
			p1.getAtributos().put("eq1", "Ganador r32_83");
			p1.getAtributos().put("eq2", "Ganador r32_84");
			octavosFinal.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "octavos_94");
			p1.getAtributos().put("eq1", "Ganador r32_81");
			p1.getAtributos().put("eq2", "Ganador r32_82");
			octavosFinal.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "octavos_95");
			p1.getAtributos().put("eq1", "Ganador r32_86");
			p1.getAtributos().put("eq2", "Ganador r32_88");
			octavosFinal.add(p1);

			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "octavos_96");
			p1.getAtributos().put("eq1", "Ganador r32_85");
			p1.getAtributos().put("eq2", "Ganador r32_87");
			octavosFinal.add(p1);
		}

	}

	private void initPartidosCuartos() {
		if (cuartosFinal.isEmpty()) {
			// Primero grupo A contra tercero
			int i = 0;
			String fase = "cuartosFinal";
			Partido p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "cuartos_97");
			p1.getAtributos().put("eq1", "Ganador octavos_89");
			p1.getAtributos().put("eq2", "Ganador octavos_90");

			cuartosFinal.add(p1);
			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "cuartos_98");
			p1.getAtributos().put("eq1", "Ganador octavos_93");
			p1.getAtributos().put("eq2", "Ganador octavos_94");
			cuartosFinal.add(p1);
			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "cuartos_99");
			p1.getAtributos().put("eq1", "Ganador octavos_91");
			p1.getAtributos().put("eq2", "Ganador octavos_92");
			cuartosFinal.add(p1);
			p1 = new Partido(null, null, new Integer(i++), fase);
			p1.getAtributos().put("nombre", "cuartos_100");
			p1.getAtributos().put("eq1", "Ganador octavos_95");
			p1.getAtributos().put("eq2", "Ganador octavos_96");
			cuartosFinal.add(p1);
		}

	}

	private void GenerarEquipos() {
		// Copa Mundial 2026 - 48 equipos, 12 grupos (A-L)
		// NOTA: verificar y actualizar con el sorteo oficial de la FIFA
		this.equipos = new ArrayList<>();
		// Grupo A (sede: USA)
		anadirEquipo("Mexico", 0, 0);
		anadirEquipo("Sudafrica", 0, 1);
		anadirEquipo("Corea del Sur", 0, 2);
		anadirEquipo("Chequia", 0, 3);

		// Grupo B (sede: Mexico)
		anadirEquipo("Canada", 1, 0);
		anadirEquipo("Bosnia y Herzegovina", 1, 1);
		anadirEquipo("Qatar", 1, 2);
		anadirEquipo("Suiza", 1, 3);

		// Grupo C (sede: Canada)
		anadirEquipo("Brasil", 2, 0);
		anadirEquipo("Marruecos", 2, 1);
		anadirEquipo("Haiti", 2, 2);
		anadirEquipo("Escocia", 2, 3);

		// Grupo D
		anadirEquipo("Estados Unidos", 3, 0);
		anadirEquipo("Paraguay", 3, 1);
		anadirEquipo("Australia", 3, 2);
		anadirEquipo("Turquia", 3, 3);

		// Grupo E
		anadirEquipo("Alemania", 4, 0);
		anadirEquipo("Curazao", 4, 1);
		anadirEquipo("Costa de Marfil", 4, 2);
		anadirEquipo("Ecuador", 4, 3);

		// Grupo F
		anadirEquipo("Paises Bajos", 5, 0);
		anadirEquipo("Japon", 5, 1);
		anadirEquipo("Suecia", 5, 2);
		anadirEquipo("Tunez", 5, 3);

		// Grupo G
		anadirEquipo("Belgica", 6, 0);
		anadirEquipo("Egipto", 6, 1);
		anadirEquipo("Iran", 6, 2);
		anadirEquipo("Nueva Zelanda", 6, 3);

		// Grupo H
		anadirEquipo("España", 7, 0);
		anadirEquipo("Cabo Verde", 7, 1);
		anadirEquipo("Arabia Saudita", 7, 2);
		anadirEquipo("Uruguay", 7, 3);

		// Grupo I
		anadirEquipo("Francia", 8, 0);
		anadirEquipo("Senegal", 8, 1);
		anadirEquipo("Irak", 8, 2);
		anadirEquipo("Noruega", 8, 3);

		// Grupo J
		anadirEquipo("Argentina", 9, 0);
		anadirEquipo("Algeria", 9, 1);
		anadirEquipo("Austria", 9, 2);
		anadirEquipo("Jordania", 9, 3);

		// Grupo K
		anadirEquipo("Portugal", 10, 0);
		anadirEquipo("RD Congo", 10, 1);
		anadirEquipo("Uzbekistan", 10, 2);
		anadirEquipo("Colombia", 10, 3);

		// Grupo L
		anadirEquipo("Inglaterra", 11, 0);
		anadirEquipo("Croacia", 11, 1);
		anadirEquipo("Ghana", 11, 2);
		anadirEquipo("Panama", 11, 3);

	}

	public Equipo getEquipo(Integer grupo, Integer pos) {

		Optional<Equipo> opteq = this.equipos.stream()
				.filter(o -> o.getAtributos().get("grupo").equals(grupo) && o.getAtributos().get("orden").equals(pos))
				.findFirst();
		if (opteq.isPresent()) {
			return opteq.get();
		}
		return null;
	}

	private void anadirEquipo(String nombre, Integer grupo, Integer pos) {
		Equipo eq = new Equipo();
		eq.getAtributos().put("nombre", nombre);
		eq.getAtributos().put("grupo", grupo);
		eq.getAtributos().put("orden", pos);

		this.equipos.add(eq);
	}

	private void ArmarPartidos() {
		// Partidos de cada grupo
		boolean vacios = true;
		for (Iterator iterator = partidos_grupos.keySet().iterator(); iterator.hasNext();) {
			Integer key = (Integer) iterator.next();
			if (!partidos_grupos.get(key).isEmpty()) {
				vacios = false;
				break;
			}
		}
		if (vacios) {
			int i = 0;
			String fase = "partidos_grupos";
			for (int grupo = 0; grupo < 12; grupo++) {
				this.partidos_grupos.put(new Integer(grupo), new ArrayList<Partido>());
				// Fecha 1 1-2 y 3-4
				Equipo eq1 = getEquipo(grupo, 0);
				Equipo eq2 = getEquipo(grupo, 1);
				Partido p1 = new Partido(eq1, eq2, new Integer(i++), fase);
				this.partidos_grupos.get(new Integer(grupo)).add(p1);

				eq1 = getEquipo(grupo, 2);
				eq2 = getEquipo(grupo, 3);
				p1 = new Partido(eq1, eq2, new Integer(i++), fase);
				this.partidos_grupos.get(new Integer(grupo)).add(p1);

				// Fecha 2 1-3 2-4
				eq1 = getEquipo(grupo, 0);
				eq2 = getEquipo(grupo, 2);
				p1 = new Partido(eq1, eq2, new Integer(i++), fase);
				this.partidos_grupos.get(new Integer(grupo)).add(p1);

				eq1 = getEquipo(grupo, 1);
				eq2 = getEquipo(grupo, 3);
				p1 = new Partido(eq1, eq2, new Integer(i++), fase);
				this.partidos_grupos.get(new Integer(grupo)).add(p1);

				// Fecha 3 1-4 3-2
				eq1 = getEquipo(grupo, 0);
				eq2 = getEquipo(grupo, 3);
				p1 = new Partido(eq1, eq2, new Integer(i++), fase);
				this.partidos_grupos.get(new Integer(grupo)).add(p1);
				eq1 = getEquipo(grupo, 2);
				eq2 = getEquipo(grupo, 1);
				p1 = new Partido(eq1, eq2, new Integer(i++), fase);
				this.partidos_grupos.get(new Integer(grupo)).add(p1);
			}
		}
	}

	private Posicion getPosicionEquipo(Integer grupo, Equipo eq) {

		Optional<Posicion> opeq1 = posiciones_grupos.get(grupo).stream().filter(pg -> pg.getEquipo().equals(eq))
				.findFirst();
		Posicion peq1 = null;
		if (opeq1.isPresent()) {
			peq1 = opeq1.get();
		} else {
			peq1 = new Posicion();
			peq1.setEquipo(eq);
			peq1.getAtributos().put("gf", new Integer(0));
			peq1.getAtributos().put("gc", new Integer(0));
			peq1.getAtributos().put("gd", new Integer(0));
			peq1.getAtributos().put("pj", new Integer(0));
			peq1.getAtributos().put("pp", new Integer(0));
			peq1.getAtributos().put("pe", new Integer(0));
			peq1.getAtributos().put("pg", new Integer(0));
			peq1.getAtributos().put("pt", new Integer(0));
			peq1.getAtributos().put("ta", new Integer(0));
			peq1.getAtributos().put("tr", new Integer(0));
			posiciones_grupos.get(grupo).add(peq1);

		}
		return peq1;
	}

	private void posicionPorEquipos(List<Posicion> posiciones, String keypos) {
		// Se verifican las posiciones segun los puntos
		posiciones.sort(new Comparator<Posicion>() {
			@Override
			public int compare(Posicion o1, Posicion o2) {
				// TODO Auto-generated method stub
				Integer i1 = (Integer) o1.getAtributos().get("pt");
				Integer i2 = (Integer) o2.getAtributos().get("pt");
				return i2.compareTo(i1);
			}
		});
		// Se pone la posicion
		int psin = 1;
		for (Posicion pos : posiciones) {
			pos.getAtributos().put(keypos, new Integer(psin++));
		}

		// Primer criterio de desempate
//		Mejor diferencia de gol en todos los partidos de grupo.
		// Se verifican las posiciones segun los puntos
		HashMap<Integer, List<Posicion>> puntosmap = new HashMap<>();
		for (Posicion posicion : posiciones) {
			Integer key = (Integer) posicion.getAtributos().get("pt");
			puntosmap.putIfAbsent(key, new LinkedList<>());
			puntosmap.get(key).add(posicion);
		}
		for (Iterator iterator2 = puntosmap.keySet().iterator(); iterator2.hasNext();) {
			Integer key = (Integer) iterator2.next();
			List<Posicion> ps = puntosmap.get(key);
			if (ps.size() > 1) {
				int minpos = ps.stream().mapToInt(o -> (Integer) o.getAtributos().get(keypos)).min().getAsInt();
				ps.sort(new Comparator<Posicion>() {
					@Override
					public int compare(Posicion o1, Posicion o2) {
						// TODO Auto-generated method stub
						Integer i1 = (Integer) o1.getAtributos().get("gd");
						Integer i2 = (Integer) o2.getAtributos().get("gd");
						return i2.compareTo(i1);
					}
				});
				for (Posicion posicion : ps) {
					posicion.getAtributos().put(keypos, new Integer(minpos++));
				}
			}

		}

		// Segundo criterio de desempate
//		Mayor cantidad de goles marcados en todos los partidos de grupo

		HashMap<String, List<Posicion>> puntosmap1 = new HashMap<>();
		for (Posicion posicion : posiciones) {
			String key = ((Integer) posicion.getAtributos().get("pt")).toString()
					+ ((Integer) posicion.getAtributos().get("gd")).toString();
			puntosmap1.putIfAbsent(key, new LinkedList<>());
			puntosmap1.get(key).add(posicion);
		}
		for (Iterator iterator2 = puntosmap1.keySet().iterator(); iterator2.hasNext();) {
			String key = (String) iterator2.next();
			List<Posicion> ps = puntosmap1.get(key);
			if (ps.size() > 1) {
				int minpos = ps.stream().mapToInt(o -> (Integer) o.getAtributos().get(keypos)).min().getAsInt();
				ps.sort(new Comparator<Posicion>() {
					@Override
					public int compare(Posicion o1, Posicion o2) {
						// TODO Auto-generated method stub
						Integer i1 = (Integer) o1.getAtributos().get("gf");
						Integer i2 = (Integer) o2.getAtributos().get("gf");
						return i2.compareTo(i1);
					}
				});
				for (Posicion posicion : ps) {
					posicion.getAtributos().put(keypos, new Integer(minpos++));
				}
			}

		}
		// tercer criterio de desempate
//		Si el empate se mantiene entre dos equipos del mismo grupo, clasifica el equipo ganador del partido jugado entre los equipos implicados.

		HashMap<String, List<Posicion>> puntosmap11 = new HashMap<>();
		for (Posicion posicion : posiciones) {
			String key = ((Integer) posicion.getAtributos().get("pt")).toString()
					+ ((Integer) posicion.getAtributos().get("gd")).toString()
					+ ((Integer) posicion.getAtributos().get("gf")).toString();
			puntosmap11.putIfAbsent(key, new LinkedList<>());
			puntosmap11.get(key).add(posicion);
		}

		for (Iterator iterator2 = puntosmap11.keySet().iterator(); iterator2.hasNext();) {
			String key = (String) iterator2.next();
			List<Posicion> ps = puntosmap11.get(key);
			int minpos = ps.stream().mapToInt(o -> (Integer) o.getAtributos().get(keypos)).min().getAsInt();
			if (ps.size() == 2) {
				Posicion p1 = ps.get(0);
				Posicion p2 = ps.get(1);
				Partido partido = getPartido(p1.getEquipo(), p2.getEquipo());
				if (partido != null) {
					// Gana 1
					if (partido.getGoles1() > partido.getGoles2()) {
						if (partido.getEq1().equals(p1.getEquipo())) {
							p1.getAtributos().put(keypos, new Integer(minpos++));
							p2.getAtributos().put(keypos, new Integer(minpos++));
						} else {
							p2.getAtributos().put(keypos, new Integer(minpos++));
							p1.getAtributos().put(keypos, new Integer(minpos++));
						}
					}

					// Gana 2
					else if (partido.getGoles2() > partido.getGoles1()) {
						if (partido.getEq2().equals(p1.getEquipo())) {
							p1.getAtributos().put(keypos, new Integer(minpos++));
							p2.getAtributos().put(keypos, new Integer(minpos++));
						} else {
							p2.getAtributos().put(keypos, new Integer(minpos++));
							p1.getAtributos().put(keypos, new Integer(minpos++));
						}
					}

				}

			}

		}

		// Cuarto criterio de desempate: fair play (tarjetas amarillas + 3 * tarjetas rojas), menor es mejor
		HashMap<String, List<Posicion>> puntosmap2 = new HashMap<>();
		for (Posicion posicion : posiciones) {
			String key = ((Integer) posicion.getAtributos().get("pt")).toString()
					+ ((Integer) posicion.getAtributos().get("gd")).toString()
					+ ((Integer) posicion.getAtributos().get("gf")).toString();
			puntosmap2.putIfAbsent(key, new LinkedList<>());
			puntosmap2.get(key).add(posicion);
		}
		for (Iterator iterator2 = puntosmap2.keySet().iterator(); iterator2.hasNext();) {
			String key = (String) iterator2.next();
			List<Posicion> ps = puntosmap2.get(key);
			if (ps.size() > 1) {
				int minpos = ps.stream().mapToInt(o -> (Integer) o.getAtributos().get(keypos)).min().getAsInt();
				ps.sort(new Comparator<Posicion>() {
					@Override
					public int compare(Posicion o1, Posicion o2) {
						Integer fp1 = (Integer) o1.getAtributos().get("ta") + 3 * (Integer) o1.getAtributos().get("tr");
						Integer fp2 = (Integer) o2.getAtributos().get("ta") + 3 * (Integer) o2.getAtributos().get("tr");
						return fp1.compareTo(fp2); // menor fair play primero
					}
				});
				for (Posicion posicion : ps) {
					posicion.getAtributos().put(keypos, new Integer(minpos++));
				}
			}
		}
	}

	public void calcularPosiciones() {
		this.posiciones_grupos = new HashMap<>();
		for (Iterator iterator = partidos_grupos.keySet().iterator(); iterator.hasNext();) {
			Integer grupo = (Integer) iterator.next();
			posiciones_grupos.put(grupo, new ArrayList<>());
			List<Partido> partidos = partidos_grupos.get(grupo);
			for (Partido partido : partidos) {
				Posicion peq1 = getPosicionEquipo(grupo, partido.getEq1());
				Posicion peq2 = getPosicionEquipo(grupo, partido.getEq2());
				verificarResultado(partido, peq1, peq2);

			}

		}

		for (Iterator iterator = posiciones_grupos.keySet().iterator(); iterator.hasNext();) {
			Integer grupo = (Integer) iterator.next();
			List<Posicion> posiciones = posiciones_grupos.get(grupo);

			posicionPorEquipos(posiciones, "pos");

		}

//		calcularCuartos();
		calcularRonda32();
	}

	private Partido getPartido(Equipo eq1, Equipo eq2) {
		for (Iterator iterator = getPartidos_grupos().keySet().iterator(); iterator.hasNext();) {
			Integer key = (Integer) iterator.next();
			Optional<Partido> opt = getPartidos_grupos().get(key).stream()
					.filter(o -> (o.getEq1().equals(eq1) && o.getEq2().equals(eq2))
							|| (o.getEq1().equals(eq2) && o.getEq2().equals(eq1)))
					.findFirst();
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return null;
	}

	private void verificarResultado(Partido partido, Posicion peq1, Posicion peq2) {
		peq1.getAtributos().put("gf", (Integer) peq1.getAtributos().get("gf") + partido.getGoles1());
		peq1.getAtributos().put("gc", (Integer) peq1.getAtributos().get("gc") + partido.getGoles2());
		peq1.getAtributos().put("gd",
				(Integer) peq1.getAtributos().get("gf") - (Integer) peq1.getAtributos().get("gc"));
		peq1.getAtributos().put("pj", (Integer) peq1.getAtributos().get("pj") + new Integer(1));

		peq2.getAtributos().put("gf", (Integer) peq2.getAtributos().get("gf") + partido.getGoles2());
		peq2.getAtributos().put("gc", (Integer) peq2.getAtributos().get("gc") + partido.getGoles1());
		peq2.getAtributos().put("gd",
				(Integer) peq2.getAtributos().get("gf") - (Integer) peq2.getAtributos().get("gc"));
		peq2.getAtributos().put("pj", (Integer) peq2.getAtributos().get("pj") + new Integer(1));

		// Gana 1 pierde 2
		if (partido.getGoles1().compareTo(partido.getGoles2()) > 0) {
			peq1.getAtributos().put("pg", (Integer) peq1.getAtributos().get("pg") + new Integer(1));
			peq1.getAtributos().put("pt", (Integer) peq1.getAtributos().get("pt") + new Integer(3));
			peq2.getAtributos().put("pp", (Integer) peq2.getAtributos().get("pp") + new Integer(1));
		}
		// empata
		else if (partido.getGoles1().compareTo(partido.getGoles2()) == 0) {
			peq1.getAtributos().put("pe", (Integer) peq1.getAtributos().get("pe") + new Integer(1));
			peq1.getAtributos().put("pt", (Integer) peq1.getAtributos().get("pt") + new Integer(1));

			peq2.getAtributos().put("pe", (Integer) peq2.getAtributos().get("pe") + new Integer(1));
			peq2.getAtributos().put("pt", (Integer) peq2.getAtributos().get("pt") + new Integer(1));
		}
		// Gana 2 pierde 1
		else if (partido.getGoles1().compareTo(partido.getGoles2()) < 0) {
			peq1.getAtributos().put("pp", (Integer) peq1.getAtributos().get("pp") + new Integer(1));
			peq2.getAtributos().put("pg", (Integer) peq2.getAtributos().get("pg") + new Integer(1));
			peq2.getAtributos().put("pt", (Integer) peq2.getAtributos().get("pt") + new Integer(3));
		}

		// Tarjetas (fair play)
		peq1.getAtributos().put("ta", (Integer) peq1.getAtributos().get("ta") + partido.getTa1());
		peq1.getAtributos().put("tr", (Integer) peq1.getAtributos().get("tr") + partido.getTr1());
		peq2.getAtributos().put("ta", (Integer) peq2.getAtributos().get("ta") + partido.getTa2());
		peq2.getAtributos().put("tr", (Integer) peq2.getAtributos().get("tr") + partido.getTr2());

	}

	private void initPartidosRonda32() {
		if (ronda32.isEmpty()) {
			int i = 0;
			String fase = "ronda32";
			// Partidos 1-8: ganadores de grupo vs mejores terceros
			Partido p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_73");
			p.getAtributos().put("eq1", "2.° Grupo A");
			p.getAtributos().put("eq2", "2.° Grupo B");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_74");
			p.getAtributos().put("eq1", "1.° Grupo E");
			p.getAtributos().put("eq2", "Mejor 3.° A/B/C/D/F");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_75");
			p.getAtributos().put("eq1", "1.° Grupo F");
			p.getAtributos().put("eq2", "2.° Grupo C");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_76");
			p.getAtributos().put("eq1", "1.° Grupo C");
			p.getAtributos().put("eq2", "2.° Grupo F");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_77");
			p.getAtributos().put("eq1", "1.° Grupo I");
			p.getAtributos().put("eq2", "Mejor 3.° C/D/F/G/H");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_78");
			p.getAtributos().put("eq1", "2.° Grupo E");
			p.getAtributos().put("eq2", "2.° Grupo I");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_79");
			p.getAtributos().put("eq1", "1.° Grupo A");
			p.getAtributos().put("eq2", "Mejor 3.° C/E/F/H/I");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_80");
			p.getAtributos().put("eq1", "1.° Grupo L");
			p.getAtributos().put("eq2", "Mejor 3.° E/H/I/J/K");
			ronda32.add(p);

			// Partidos 9-12: ganadores de grupo vs subcampeones
			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_81");
			p.getAtributos().put("eq1", "1.° Grupo D");
			p.getAtributos().put("eq2", "Mejor 3.° B/E/F/I/J");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_82");
			p.getAtributos().put("eq1", "1.° Grupo G");
			p.getAtributos().put("eq2", "Mejor 3.° A/E/H/I/J");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_83");
			p.getAtributos().put("eq1", "2.° Grupo K");
			p.getAtributos().put("eq2", "2.° Grupo L");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_84");
			p.getAtributos().put("eq1", "1.° Grupo H");
			p.getAtributos().put("eq2", "2.° Grupo J");
			ronda32.add(p);

			// Partidos 13-16: subcampeones vs subcampeones
			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_85");
			p.getAtributos().put("eq1", "1.° Grupo B");
			p.getAtributos().put("eq2", "Mejor 3.° E/F/G/I/J");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_86");
			p.getAtributos().put("eq1", "1.° Grupo J");
			p.getAtributos().put("eq2", "2.° Grupo H");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_87");
			p.getAtributos().put("eq1", "1.° Grupo K");
			p.getAtributos().put("eq2", "Mejor 3.° D/E/I/J/L");
			ronda32.add(p);

			p = new Partido(null, null, new Integer(i++), fase);
			p.getAtributos().put("nombre", "r32_88");
			p.getAtributos().put("eq1", "2.° Grupo D");
			p.getAtributos().put("eq2", "2.° Grupo G");
			ronda32.add(p);
		}
	}

	public void calcularRonda32() {
		// Recopilar los terceros de cada grupo y ordenar por mejor rendimiento
		List<Posicion> terceros = new ArrayList<>();
		for (int g = 0; g < 12; g++) {
			if (posiciones_grupos.containsKey(g) && !posiciones_grupos.get(g).isEmpty()) {
				Optional<Posicion> opt = posiciones_grupos.get(g).stream()
						.filter(o -> ((Integer) o.getAtributos().get("pos")).equals(3)).findFirst();
				if (opt.isPresent())
					terceros.add(opt.get());
			}
		}
		// Ordenar: puntos desc, diferencia de gol desc, goles a favor desc, fair play asc (menor = mejor)
		terceros.sort((a, b) -> {
			int cmp = ((Integer) b.getAtributos().get("pt")).compareTo((Integer) a.getAtributos().get("pt"));
			if (cmp != 0)
				return cmp;
			cmp = ((Integer) b.getAtributos().get("gd")).compareTo((Integer) a.getAtributos().get("gd"));
			if (cmp != 0)
				return cmp;
			cmp = ((Integer) b.getAtributos().get("gf")).compareTo((Integer) a.getAtributos().get("gf"));
			if (cmp != 0)
				return cmp;
			// Fair play: tarjetas amarillas + 3 * tarjetas rojas (menor es mejor)
			Integer fpA = (Integer) a.getAtributos().get("ta") + 3 * (Integer) a.getAtributos().get("tr");
			Integer fpB = (Integer) b.getAtributos().get("ta") + 3 * (Integer) b.getAtributos().get("tr");
			return fpA.compareTo(fpB);
		});
		List<Posicion> mejoresTerceros = terceros.stream().collect(Collectors.toList());
		System.out.println("mejores terceros " + mejoresTerceros);
//		Mejor Tercero A/B/C/D/F 
//		Equipo = mejoresTerceros.stream().filter(o-> o.get)

		// Partidos 1-8: ganadores vs mejores terceros
		// 2do Grupo A contra 2do Del Grupo B
		setR32Equipos("r32_73", getEquipoPosicion(0, 2), getEquipoPosicion(1, 2));
		// 1 del E contra Mejor Tercero A/B/C/D/F
		setR32Equipos("r32_74", getEquipoPosicion(4, 1),
				extraerMejorTercero(mejoresTerceros, Arrays.asList(0, 1, 2, 3, 5)).getEquipo());
		// 1 del F contra 2 del C
		setR32Equipos("r32_75", getEquipoPosicion(5, 1), getEquipoPosicion(2, 2));
		// 1 Del C contra 2 del F
		setR32Equipos("r32_76", getEquipoPosicion(2, 1), getEquipoPosicion(5, 2));
		// 1 del I contra Mejor Tercero C/D/F/G/H
		setR32Equipos("r32_77", getEquipoPosicion(8, 1),
				extraerMejorTercero(mejoresTerceros, Arrays.asList(2, 3, 5, 6, 7)).getEquipo());
		// 2 del E conta 2 del I
		setR32Equipos("r32_78", getEquipoPosicion(4, 2), getEquipoPosicion(8, 2));
		// 1 del A contra mejor Tercer C/E/F/H/I
		setR32Equipos("r32_79", getEquipoPosicion(0, 1),
				extraerMejorTercero(mejoresTerceros, Arrays.asList(2, 4, 5, 7, 8)).getEquipo());
		// 1 del L contra mejor Tercer E/H/I/J/K
		setR32Equipos("r32_80", getEquipoPosicion(11, 1),
				extraerMejorTercero(mejoresTerceros, Arrays.asList(4, 7, 8, 9, 10)).getEquipo());
		// 1 del D contra mejor Tercer B/E/F/I/J
		setR32Equipos("r32_81", getEquipoPosicion(3, 1),
				extraerMejorTercero(mejoresTerceros, Arrays.asList(1, 4, 5, 8, 9)).getEquipo());
		// 1 del G contra mejor Tercer A/E/H/I/J
		setR32Equipos("r32_82", getEquipoPosicion(6, 1),
				extraerMejorTercero(mejoresTerceros, Arrays.asList(0, 4, 7, 8, 9)).getEquipo());
		// 2 Del K contra 2 del L
		setR32Equipos("r32_83", getEquipoPosicion(10, 2), getEquipoPosicion(11, 2));
		// 1 del H contra 2 del J
		setR32Equipos("r32_84", getEquipoPosicion(7, 1), getEquipoPosicion(9, 2));
		// 1 del B contra mejor Tercer E/F/G/I/J
		setR32Equipos("r32_85", getEquipoPosicion(6, 1),
				extraerMejorTercero(mejoresTerceros, Arrays.asList(4, 5, 6, 8, 9)).getEquipo());

		// 1 Del J contra 2 del H
		setR32Equipos("r32_86", getEquipoPosicion(9, 1), getEquipoPosicion(7, 2));

		// 1 del K contra mejor Tercer D/E/I/J/L
		setR32Equipos("r32_87", getEquipoPosicion(6, 1),
				extraerMejorTercero(mejoresTerceros, Arrays.asList(3, 4, 8, 9, 11)).getEquipo());

		// 2 del D contra 2 del G
		setR32Equipos("r32_88", getEquipoPosicion(3, 2), getEquipoPosicion(6, 2));

		calcularOctavos();
	}

	public static Posicion extraerMejorTercero(List<Posicion> mejoresTerceros, List<Integer> gruposPermitidos) {
		for (Iterator<Posicion> it = mejoresTerceros.iterator(); it.hasNext();) {
			Posicion tercero = it.next();
			Integer grupo = (Integer) tercero.getEquipo().getAtributos().get("grupo");
			// si el grupo lo guardas con otra llave, cámbiala aquí

			if (gruposPermitidos.contains(grupo)) {
				it.remove(); // lo saca de la lista
				System.out.println("si lo encuentra " + tercero.getEquipo().getAtributos());
				return tercero;
			}
		}
		System.out.println("no lo encuentra de los grupos " + gruposPermitidos);
		return null;
	}

	private void setR32Equipos(String nombre, Equipo eq1, Equipo eq2) {
		Optional<Partido> opt = ronda32.stream().filter(o -> o.getAtributos().get("nombre").equals(nombre)).findFirst();
		if (opt.isPresent())
			opt.get().setEquipos(eq1, eq2);
	}

	private Equipo getR32Ganador(String nombre) {
		Optional<Partido> opt = ronda32.stream().filter(o -> o.getAtributos().get("nombre").equals(nombre)).findFirst();
		return opt.isPresent() ? opt.get().verificarResultadoFaseFinal() : null;
	}

	private void setOctavosEquipos(String nombre, Equipo eq1, Equipo eq2) {
		Optional<Partido> opt = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals(nombre))
				.findFirst();
		if (opt.isPresent())
			opt.get().setEquipos(eq1, eq2);
	}

	public void calcularOctavos() {
		setOctavosEquipos("octavos_89", getR32Ganador("r32_74"), getR32Ganador("r32_77"));
		setOctavosEquipos("octavos_90", getR32Ganador("r32_73"), getR32Ganador("r32_75"));
		setOctavosEquipos("octavos_91", getR32Ganador("r32_76"), getR32Ganador("r32_78"));
		setOctavosEquipos("octavos_92", getR32Ganador("r32_79"), getR32Ganador("r32_80"));
		setOctavosEquipos("octavos_93", getR32Ganador("r32_83"), getR32Ganador("r32_84"));
		setOctavosEquipos("octavos_94", getR32Ganador("r32_81"), getR32Ganador("r32_82"));
		setOctavosEquipos("octavos_95", getR32Ganador("r32_86"), getR32Ganador("r32_88"));
		setOctavosEquipos("octavos_96", getR32Ganador("r32_85"), getR32Ganador("r32_87"));
		calcularCuartos();
	}

	public void calcularCuartos() {
		System.out.println("calcularCuartos");
		octavosFinal.forEach(o -> {
			System.out.println(o.getAtributos().get("nombre"));
		});
		// Octavos 1
		Partido p = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("octavos_89")).findFirst()
				.get();
		Equipo ganadorc1 = p.verificarResultadoFaseFinal();
		// Octavos 2
		Partido p2 = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("octavos_90")).findFirst()
				.get();
		Equipo ganadorc2 = p2.verificarResultadoFaseFinal();
		Partido cuartos1 = cuartosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("cuartos_97"))
				.findFirst().get();
		cuartos1.setEquipos(ganadorc1, ganadorc2);

		// Octavos 3
		p = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("octavos_93")).findFirst().get();
		ganadorc1 = p.verificarResultadoFaseFinal();
		// Octavos 4
		p2 = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("octavos_94")).findFirst().get();
		ganadorc2 = p2.verificarResultadoFaseFinal();
		cuartos1 = cuartosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("cuartos_98")).findFirst()
				.get();
		cuartos1.setEquipos(ganadorc1, ganadorc2);

		// Octavos 5
		p = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("octavos_91")).findFirst().get();
		ganadorc1 = p.verificarResultadoFaseFinal();
		// Octavos 6
		p2 = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("octavos_92")).findFirst().get();
		ganadorc2 = p2.verificarResultadoFaseFinal();
		cuartos1 = cuartosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("cuartos_99")).findFirst()
				.get();
		cuartos1.setEquipos(ganadorc1, ganadorc2);

		// Octavos 5
		p = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("octavos_95")).findFirst().get();
		ganadorc1 = p.verificarResultadoFaseFinal();
		// Octavos 6
		p2 = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("octavos_96")).findFirst().get();
		ganadorc2 = p2.verificarResultadoFaseFinal();
		cuartos1 = cuartosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("cuartos_100")).findFirst()
				.get();
		cuartos1.setEquipos(ganadorc1, ganadorc2);

		calcularSemifinales();
	}

	public void calcularSemifinales() {
		System.out.println("calcularSemifinales");

		// Cuartos 1
		Partido p = cuartosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("cuartos_97")).findFirst()
				.get();
		Equipo ganadorc1 = p.verificarResultadoFaseFinal();

		// Cuartos 2
		Partido p2 = cuartosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("cuartos_98")).findFirst()
				.get();
		Equipo ganadorc2 = p2.verificarResultadoFaseFinal();
		Partido semi1 = semifinales.stream().filter(o -> o.getAtributos().get("nombre").equals("Semifinal_101"))
				.findFirst().get();
		semi1.setEquipos(ganadorc1, ganadorc2);

		// Cuartos 3
		p = cuartosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("cuartos_99")).findFirst().get();
		ganadorc1 = p.verificarResultadoFaseFinal();

		// Cuartos 4
		p2 = cuartosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals("cuartos_100")).findFirst().get();
		ganadorc2 = p2.verificarResultadoFaseFinal();

		semi1 = semifinales.stream().filter(o -> o.getAtributos().get("nombre").equals("Semifinal_102")).findFirst()
				.get();
		semi1.setEquipos(ganadorc1, ganadorc2);
		calcularFinales();

	}

	public void calcularFinales() {
		System.out.println("calcularFinales");
		// Cuartos 1
		Partido p = semifinales.stream().filter(o -> o.getAtributos().get("nombre").equals("Semifinal_101")).findFirst()
				.get();
		Equipo ganadorc1 = p.verificarResultadoFaseFinal();

		Equipo perdedor = null;
		if (ganadorc1 != null) {
			if (p.getEq1().equals(ganadorc1)) {
				perdedor = p.getEq2();
			} else {
				perdedor = p.getEq1();
			}
		}

		// Cuartos 2
		Partido p2 = semifinales.stream().filter(o -> o.getAtributos().get("nombre").equals("Semifinal_102"))
				.findFirst().get();
		Equipo ganadorc2 = p2.verificarResultadoFaseFinal();

		Equipo perdedor2 = null;
		if (ganadorc2 != null) {
			if (p2.getEq1().equals(ganadorc2)) {
				perdedor2 = p2.getEq2();
			} else {
				perdedor2 = p2.getEq1();
			}
		}

		Partido pfinal = finales.stream().filter(o -> o.getAtributos().get("nombre").equals("final")).findFirst().get();
		pfinal.setEquipos(ganadorc1, ganadorc2);

		Partido tercerocuarto = finales.stream().filter(o -> o.getAtributos().get("nombre").equals("tercero y cuarto"))
				.findFirst().get();
		tercerocuarto.setEquipos(perdedor, perdedor2);
		calcularCampeon();
	}

	public void calcularCampeon() {
		System.out.println("calcularCampeon");
		// Cuartos 1
		Partido p = finales.stream().filter(o -> o.getAtributos().get("nombre").equals("final")).findFirst().get();
		this.campeon = p.verificarResultadoFaseFinal();

	}

	public void calcularResultadosCuartos() {

	}

	private Equipo getEquipoPosicion(Integer grupo, Integer posicion) {
		if (!posiciones_grupos.containsKey(grupo) || posiciones_grupos.get(grupo).isEmpty()) {
			return null;
		}
		Optional<Posicion> opt = posiciones_grupos.get(grupo).stream()
				.filter(o -> ((Integer) o.getAtributos().get("pos")).equals(posicion)).findFirst();
		if (opt.isPresent()) {
			return opt.get().getEquipo();
		}

		return null;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String showGreeting() {
		return "Hello " + firstName + " " + lastName + "!";
	}

	public List<Equipo> getEquipos() {
		return equipos;
	}

	public void setEquipos(List<Equipo> equipos) {
		this.equipos = equipos;
	}

	public HashMap<Integer, List<Partido>> getPartidos_grupos() {
		return partidos_grupos;
	}

	public void setPartidos_grupos(HashMap<Integer, List<Partido>> partidos_grupos) {
		this.partidos_grupos = partidos_grupos;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public HashMap<Integer, List<Posicion>> getPosiciones_grupos() {
		return posiciones_grupos;
	}

	public void setPosiciones_grupos(HashMap<Integer, List<Posicion>> posiciones_grupos) {
		this.posiciones_grupos = posiciones_grupos;
	}

	public List<Partido> getRonda32() {
		return ronda32;
	}

	public void setRonda32(List<Partido> ronda32) {
		this.ronda32 = ronda32;
	}

	public List<Partido> getCuartosFinal() {
		return cuartosFinal;
	}

	public void setCuartosFinal(List<Partido> cuartosFinal) {
		this.cuartosFinal = cuartosFinal;
	}

	public List<Partido> getSemifinales() {
		return semifinales;
	}

	public void setSemifinales(List<Partido> semifinales) {
		this.semifinales = semifinales;
	}

	public List<Partido> getFinales() {
		return finales;
	}

	public void setFinales(List<Partido> finales) {
		this.finales = finales;
	}

	public Equipo getCampeon() {
		return campeon;
	}

	public void setCampeon(Equipo campeon) {
		this.campeon = campeon;
	}

	public Bean_User getBean_User() {
		return bean_User;
	}

	public void setBean_User(Bean_User bean_User) {
		this.bean_User = bean_User;
	}

	/**
	 * @return the usuarioPartidoDao
	 */
	public UsuarioPartidoDao getUsuarioPartidoDao() {
		return usuarioPartidoDao;
	}

	/**
	 * @param usuarioPartidoDao the usuarioPartidoDao to set
	 */
	public void setUsuarioPartidoDao(UsuarioPartidoDao usuarioPartidoDao) {
		this.usuarioPartidoDao = usuarioPartidoDao;
	}

	/**
	 * @return the id_partidoUsuario
	 */
	public String getId_partidoUsuario() {
		return id_partidoUsuario;
	}

	/**
	 * @param id_partidoUsuario the id_partidoUsuario to set
	 */
	public void setId_partidoUsuario(String id_partidoUsuario) {
		this.id_partidoUsuario = id_partidoUsuario;
	}

	/**
	 * @return the canedit
	 */
	public boolean isCanedit() {
		return canedit;
	}

	/**
	 * @param canedit the canedit to set
	 */
	public void setCanedit(boolean canedit) {
		this.canedit = canedit;
	}

	public String[] getLetras() {
		return letras;
	}

	public void setLetras(String[] letras) {
		this.letras = letras;
	}

	/**
	 * @return the octavosFinal
	 */
	public List<Partido> getOctavosFinal() {
		return octavosFinal;
	}

	/**
	 * @param octavosFinal the octavosFinal to set
	 */
	public void setOctavosFinal(List<Partido> octavosFinal) {
		this.octavosFinal = octavosFinal;
	}

	/**
	 * @return the usuarioDao
	 */
	public UsuarioDao getUsuarioDao() {
		return usuarioDao;
	}

	/**
	 * @param usuarioDao the usuarioDao to set
	 */
	public void setUsuarioDao(UsuarioDao usuarioDao) {
		this.usuarioDao = usuarioDao;
	}

	/**
	 * @return the id_user
	 */
	public String getId_user() {
		return id_user;
	}

	/**
	 * @param id_user the id_user to set
	 */
	public void setId_user(String id_user) {
		this.id_user = id_user;
	}
}
