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

	public Bean_Ranking() {
		super();
	}

	@PostConstruct
	private void init() {
		List<Usuario> ureal = usuarioDao.findByCriteria(Restrictions.eq("mail", "real@real.com"));
		columnas = new HashSet<>();
		if (!ureal.isEmpty()) {
			Usuario u = ureal.get(0);
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
			hm1.putIfAbsent(key, 0);
			for (Partido preal : partidosReales) {
				hm1.put(key, hm1.get(key) + getResultadoNumerico(key, preal));
			}
		}
		return sortByValue(hm1);
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
