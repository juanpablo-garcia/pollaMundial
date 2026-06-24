package co.com.tmsolutions.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.criterion.Restrictions;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;

import co.com.tmsolutions.dao.UsuarioDao;
import co.com.tmsolutions.dao.UsuarioPartidoDao;
import co.com.tmsolutions.model.Partido;
import co.com.tmsolutions.model.Usuario;

@Named("bean_Ranking")
@ViewScoped
public class Bean_Ranking implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private Bean_User bean_User;
	@Inject
	private UsuarioDao usuarioDao;

	@Inject
	private UsuarioPartidoDao usuarioPartidoDao;

	private List<Partido> partidosReales = new ArrayList<>();
	private Set<String> columnas;
	private BarChartModel horizontalBarModel;
	// Nombre del usuario "real" (resultados oficiales); se excluye de la gráfica
	private String realNombre;

	public Bean_Ranking() {
		super();
	}

	@PostConstruct
	private void init() {
		List<Usuario> ureal = usuarioDao.findByCriteria(Restrictions.eq("mail", "real@real.com"));
		columnas = new HashSet<>();
		if (!ureal.isEmpty()) {
			Usuario u = ureal.get(0);
			this.realNombre = (String) u.getAtributos().get("nombre");
			this.partidosReales = usuarioPartidoDao.getPartidosRanking(u);
			for (Partido p : this.partidosReales) {
				ArrayList<Partido> ls = (ArrayList<Partido>) p.getAtributos().get("partidos_usuario");
				for (Partido pa : ls) {
					columnas.add((String) pa.getAtributos().get("nombre_usuario"));
				}
			}
		}
		createHorizontalBarModel();
	}

	public String getResultado(String us, Partido preal) {
		ArrayList<Partido> ls = (ArrayList<Partido>) preal.getAtributos().get("partidos_usuario");
		Partido p = ls.stream().filter(o -> o.getAtributos().get("nombre_usuario").equals(us)).findFirst().orElse(null);
		if (p != null) {
			Integer puntaje = (Integer) p.getAtributos().get("puntaje");
			Integer puntajeeq1 = (Integer) p.getAtributos().get("puntajeeq1");
			Integer puntajeeq2 = (Integer) p.getAtributos().get("puntajeeq2");

			Integer pt = new Integer(0);
			if (puntaje != null) {
				pt = pt + puntaje;
			}
			if (puntajeeq1 != null) {

				pt = pt + puntajeeq1;
			}
			if (puntajeeq2 != null) {

				pt = pt + puntajeeq2;
			}

			return p.getGoles1() + "-" + p.getGoles2() + "   (" + pt + ")";
		}
		return null;
	}

	public String getColor(String us, Partido preal) {
		ArrayList<Partido> ls = (ArrayList<Partido>) preal.getAtributos().get("partidos_usuario");
		Partido p = ls.stream().filter(o -> o.getAtributos().get("nombre_usuario").equals(us)).findFirst().orElse(null);
		if (p != null) {
			Integer puntaje = (Integer) p.getAtributos().get("puntaje");
			if (puntaje != null) {
				if (puntaje.compareTo(new Integer(3)) == 0) {
					return "#A9F5BC";
				} else if (puntaje.compareTo(new Integer(1)) == 0) {
					return "#F5DA81";
				} else if (puntaje.compareTo(new Integer(0)) == 0) {
					return "#F5BCA9";
				}
			}
		}
		return null;
	}

	public Integer getResultadoNumerico(String us, Partido preal) {
		ArrayList<Partido> ls = (ArrayList<Partido>) preal.getAtributos().get("partidos_usuario");
		Partido p = ls.stream().filter(o -> o.getAtributos().get("nombre_usuario").equals(us)).findFirst().orElse(null);
		if (p != null) {
			Integer puntaje = (Integer) p.getAtributos().get("puntaje");
			Integer puntajeeq1 = (Integer) p.getAtributos().get("puntajeeq1");
			Integer puntajeeq2 = (Integer) p.getAtributos().get("puntajeeq2");
			Integer pt = new Integer(0);

			if (puntaje != null) {
				pt = pt + puntaje;
			}
			if (puntajeeq1 != null) {
				pt = pt + puntajeeq1;
			}
			if (puntajeeq2 != null) {
				pt = pt + puntajeeq2;
			}
			return pt;
		}
		return new Integer(0);
	}

	// function to sort hashmap by values
	public static LinkedHashMap<String, Integer> sortByValue(HashMap<String, Integer> hm) {
		// Create a list from elements of HashMap
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(hm.entrySet());

		// Sort the list
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		// put data from sorted list to hashmap
		LinkedHashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
		}
		return temp;
	}

	private LinkedHashMap<String, Integer> buildResultados() {
		HashMap<String, Integer> hm1 = new LinkedHashMap<>();
		for (String key : columnas) {
			// El "real" (resultados oficiales) no se grafica para no descuadrar la escala
			if (realNombre != null && realNombre.equals(key)) {
				continue;
			}
			hm1.putIfAbsent(key, 0);
			for (Partido preal : partidosReales) {
				hm1.put(key, hm1.get(key) + getResultadoNumerico(key, preal));
			}
		}
		return sortByValue(hm1);
	}

	/**
	 * Lista de jugadores ordenada por puntaje (mayor a menor), excluyendo al
	 * usuario de resultados oficiales. Pensada para vistas publicas en tarjetas.
	 */
	public List<Map.Entry<String, Integer>> getRanking() {
		return new ArrayList<>(buildResultados().entrySet());
	}

	// ─────────────────────────────────────────────────────────────
	// Detalle por jugador: como consiguio sus puntos
	// ─────────────────────────────────────────────────────────────

	private String jugadorSeleccionado;

	public void seleccionarJugador(String nombre) {
		this.jugadorSeleccionado = nombre;
	}

	public void limpiarJugador() {
		this.jugadorSeleccionado = null;
	}

	public String getJugadorSeleccionado() {
		return jugadorSeleccionado;
	}

	public Integer getTotalJugador() {
		if (jugadorSeleccionado == null) {
			return new Integer(0);
		}
		int total = 0;
		for (Partido preal : partidosReales) {
			total += getResultadoNumerico(jugadorSeleccionado, preal);
		}
		return new Integer(total);
	}

	/**
	 * Desglose de los partidos en los que el jugador seleccionado sumo puntos:
	 * marcadores acertados y equipos que clasificaron.
	 */
	@SuppressWarnings("unchecked")
	public List<DetallePartido> getDetalleJugador() {
		List<DetallePartido> detalle = new ArrayList<>();
		if (jugadorSeleccionado == null) {
			return detalle;
		}

		// Paso 1: equipos que el jugador pronostico correctamente para que avanzaran
		// de fase. Los puntos se guardan en el slot del bracket del jugador, pero el
		// nombre del equipo acertado es un equipo que realmente clasifico. Se indexa
		// por nombre de equipo para poder mostrarlo bajo el partido real en el que ese
		// equipo juega (no bajo el slot del bracket del jugador, que puede diferir).
		Map<String, Integer> puntosPorEquipoAcertado = new HashMap<>();
		for (Partido preal : partidosReales) {
			Partido pred = prediccionDe(preal);
			if (pred == null) {
				continue;
			}
			Integer ptsEq1 = (Integer) pred.getAtributos().get("puntajeeq1");
			Integer ptsEq2 = (Integer) pred.getAtributos().get("puntajeeq2");
			if (ptsEq1 != null && ptsEq1 > 0 && pred.getEq1() != null) {
				puntosPorEquipoAcertado.put((String) pred.getEq1().getAtributos().get("nombre"), ptsEq1);
			}
			if (ptsEq2 != null && ptsEq2 > 0 && pred.getEq2() != null) {
				puntosPorEquipoAcertado.put((String) pred.getEq2().getAtributos().get("nombre"), ptsEq2);
			}
		}

		// Paso 2: por cada partido real, el marcador se evalua sobre el mismo slot del
		// jugador, y los equipos acertados son los equipos REALES de ese partido que
		// el jugador pronostico que clasificarian (asi el nombre corresponde al
		// partido mostrado). Se recorre en orden de numero de partido.
		List<Partido> ordenados = new ArrayList<>(partidosReales);
		ordenados.sort(Comparator.comparingInt(this::numeroPartido));
		for (Partido preal : ordenados) {
			Partido pred = prediccionDe(preal);
			if (pred == null) {
				continue;
			}

			Integer ptsMarcador = (Integer) pred.getAtributos().get("puntaje");
			int marcador = ptsMarcador == null ? 0 : ptsMarcador;

			List<String> equiposAcertados = new ArrayList<>();
			int equipos = 0;
			for (co.com.tmsolutions.model.Equipo eqReal : java.util.Arrays.asList(preal.getEq1(), preal.getEq2())) {
				if (eqReal == null) {
					continue;
				}
				String nombre = (String) eqReal.getAtributos().get("nombre");
				Integer pts = puntosPorEquipoAcertado.get(nombre);
				if (pts != null && pts > 0) {
					equiposAcertados.add(nombre);
					equipos += pts;
				}
			}

			// Solo interesa lo que aporto puntos al total
			if (marcador + equipos <= 0) {
				continue;
			}

			DetallePartido d = new DetallePartido();
			d.fase = nombreFase(preal);
			d.eq1 = nombreEquipo(preal.getEq1(), (String) preal.getAtributos().get("eq1"));
			d.eq2 = nombreEquipo(preal.getEq2(), (String) preal.getAtributos().get("eq2"));
			d.realizado = Boolean.TRUE.equals(preal.getRealizado());
			d.resultadoReal = preal.getGoles1() + " - " + preal.getGoles2();
			d.prediccion = pred.getGoles1() + " - " + pred.getGoles2();
			d.ptsMarcador = marcador;
			d.ptsEquipos = equipos;
			d.total = marcador + equipos;
			d.exacto = d.realizado && pred.getGoles1().equals(preal.getGoles1())
					&& pred.getGoles2().equals(preal.getGoles2());
			d.equiposAcertados = equiposAcertados;

			detalle.add(d);
		}
		return detalle;
	}

	/**
	 * Pronostico del jugador seleccionado para el slot del partido real dado.
	 */
	@SuppressWarnings("unchecked")
	private Partido prediccionDe(Partido preal) {
		ArrayList<Partido> ls = (ArrayList<Partido>) preal.getAtributos().get("partidos_usuario");
		if (ls == null) {
			return null;
		}
		return ls.stream().filter(o -> jugadorSeleccionado.equals(o.getAtributos().get("nombre_usuario")))
				.findFirst().orElse(null);
	}

	/**
	 * Numero de partido para ordenar el detalle. Grupos: idpartido 0-71 -> 1-72.
	 * Eliminatorias: el numero va en el atributo "nombre" (r32_73, octavos_89,
	 * cuartos_97, Semifinal_101...). "tercero y cuarto" = 103 y "final" = 104.
	 */
	private int numeroPartido(Partido p) {
		Object nombreObj = p.getAtributos().get("nombre");
		String nombre = nombreObj == null ? null : nombreObj.toString();
		if ("final".equals(nombre)) {
			return 104;
		}
		if ("tercero y cuarto".equals(nombre)) {
			return 103;
		}
		if (nombre != null) {
			int idx = nombre.lastIndexOf('_');
			if (idx >= 0 && idx < nombre.length() - 1) {
				try {
					return Integer.parseInt(nombre.substring(idx + 1));
				} catch (NumberFormatException e) {
					// cae al numero de grupo
				}
			}
		}
		Integer id = p.getIdpartido();
		return id == null ? Integer.MAX_VALUE : id + 1;
	}

	private String nombreEquipo(co.com.tmsolutions.model.Equipo eq, String placeholder) {
		if (eq != null) {
			return (String) eq.getAtributos().get("nombre");
		}
		return placeholder == null ? "" : placeholder;
	}

	private String nombreFase(Partido p) {
		Object nombre = p.getAtributos().get("nombre");
		if (nombre != null && !"partidos_grupos".equals(p.getFase())) {
			return nombre.toString();
		}
		String fase = p.getFase();
		if (fase == null) {
			return "";
		}
		switch (fase) {
		case "partidos_grupos":
			return "Fase de Grupos";
		case "ronda32":
			return "Ronda de 32";
		case "octavosFinal":
			return "Octavos de Final";
		case "cuartosFinal":
			return "Cuartos de Final";
		case "semifinales":
			return "Semifinales";
		case "finales":
			return "Finales";
		default:
			return fase;
		}
	}

	/**
	 * Estructura de presentacion del desglose de puntos de un partido para un
	 * jugador. Publica para que pueda leerse desde EL.
	 */
	public static class DetallePartido implements Serializable {
		private static final long serialVersionUID = 1L;
		private String fase;
		private String eq1;
		private String eq2;
		private boolean realizado;
		private String resultadoReal;
		private String prediccion;
		private int ptsMarcador;
		private int ptsEquipos;
		private int total;
		private boolean exacto;
		private List<String> equiposAcertados = new ArrayList<>();

		public String getFase() {
			return fase;
		}

		public String getEq1() {
			return eq1;
		}

		public String getEq2() {
			return eq2;
		}

		public boolean isRealizado() {
			return realizado;
		}

		public String getResultadoReal() {
			return resultadoReal;
		}

		public String getPrediccion() {
			return prediccion;
		}

		public int getPtsMarcador() {
			return ptsMarcador;
		}

		public int getPtsEquipos() {
			return ptsEquipos;
		}

		public int getTotal() {
			return total;
		}

		public boolean isExacto() {
			return exacto;
		}

		public List<String> getEquiposAcertados() {
			return equiposAcertados;
		}

		public String getEquiposAcertadosTexto() {
			return String.join(", ", equiposAcertados);
		}
	}

	public String getChartDataJson() {
		LinkedHashMap<String, Integer> resultados = buildResultados();
		StringBuilder labels = new StringBuilder("[");
		StringBuilder values = new StringBuilder("[");
		boolean first = true;
		for (Map.Entry<String, Integer> entry : resultados.entrySet()) {
			if (!first) {
				labels.append(",");
				values.append(",");
			}
			labels.append("\"").append(entry.getKey().replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
			values.append(entry.getValue());
			first = false;
		}
		return "{\"labels\":" + labels + "],\"values\":" + values + "]}";
	}

	private void createHorizontalBarModel() {
		LinkedHashMap<String, Integer> resultados = buildResultados();
		horizontalBarModel = new BarChartModel();
		// El extender configura indexAxis:'y' para barras horizontales (Chart.js)
		horizontalBarModel.setExtender("horizontalBarExtender");

		ChartData data = new ChartData();
		BarChartDataSet dataSet = new BarChartDataSet();
		dataSet.setLabel("Resultados");

		List<Number> values = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		List<String> bgColors = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : resultados.entrySet()) {
			labels.add(entry.getKey());
			values.add(entry.getValue());
			bgColors.add("rgba(54, 162, 235, 0.6)");
		}

		dataSet.setData(values);
		dataSet.setBackgroundColor(bgColors);
		data.addChartDataSet(dataSet);
		data.setLabels(labels);
		horizontalBarModel.setData(data);
	}

	/**
	 * @return the bean_User
	 */
	public Bean_User getBean_User() {
		return bean_User;
	}

	/**
	 * @param bean_User the bean_User to set
	 */
	public void setBean_User(Bean_User bean_User) {
		this.bean_User = bean_User;
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
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * @return the partidosReales
	 */
	public List<Partido> getPartidosReales() {
		return partidosReales;
	}

	/**
	 * @param partidosReales the partidosReales to set
	 */
	public void setPartidosReales(List<Partido> partidosReales) {
		this.partidosReales = partidosReales;
	}

	public Set<String> getColumnas() {
		return columnas;
	}

	public void setColumnas(Set<String> columnas) {
		this.columnas = columnas;
	}

	/**
	 * @return the horizontalBarModel
	 */
	public BarChartModel getHorizontalBarModel() {
		return horizontalBarModel;
	}

	/**
	 * @param horizontalBarModel the horizontalBarModel to set
	 */
	public void setHorizontalBarModel(BarChartModel horizontalBarModel) {
		this.horizontalBarModel = horizontalBarModel;
	}

}
