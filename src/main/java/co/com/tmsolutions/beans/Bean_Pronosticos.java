package co.com.tmsolutions.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.criterion.Restrictions;

import co.com.tmsolutions.dao.UsuarioDao;
import co.com.tmsolutions.dao.UsuarioPartidoDao;
import co.com.tmsolutions.model.Equipo;
import co.com.tmsolutions.model.Partido;
import co.com.tmsolutions.model.Usuario;

/**
 * Bean publico (no requiere login) que muestra, para cada partido, el
 * pronostico de cada jugador. Permite simular un resultado y colorear cada
 * pronostico (verde = marcador exacto, amarillo = acierta ganador/empate, rojo
 * = falla). Si el partido ya se jugo, muestra el resultado real y no permite
 * simular.
 */
@Named("bean_Pronosticos")
@ViewScoped
public class Bean_Pronosticos implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private UsuarioDao usuarioDao;

	@Inject
	private UsuarioPartidoDao usuarioPartidoDao;

	private List<Partido> partidos = new ArrayList<>();
	private Partido seleccionado;
	private String realNombre;

	private Integer simGoles1 = new Integer(0);
	private Integer simGoles2 = new Integer(0);
	private boolean simulado = false;

	public Bean_Pronosticos() {
		super();
	}

	@PostConstruct
	private void init() {
		List<Usuario> ureal = usuarioDao.findByCriteria(Restrictions.eq("mail", "real@real.com"));
		if (!ureal.isEmpty()) {
			Usuario u = ureal.get(0);
			this.realNombre = (String) u.getAtributos().get("nombre");
			this.partidos = usuarioPartidoDao.getPartidosRanking(u);
		}
	}

	// ─────────────────────────────────────────────────────────────
	// Navegacion entre las dos vistas
	// ─────────────────────────────────────────────────────────────

	public void seleccionar(Partido p) {
		this.seleccionado = p;
		this.simGoles1 = new Integer(0);
		this.simGoles2 = new Integer(0);
		this.simulado = false;
	}

	public void volver() {
		this.seleccionado = null;
		this.simulado = false;
	}

	public void simular() {
		this.simulado = true;
	}

	public boolean isJugado() {
		return seleccionado != null && Boolean.TRUE.equals(seleccionado.getRealizado());
	}

	public boolean isMostrarColores() {
		return isJugado() || simulado;
	}

	// ─────────────────────────────────────────────────────────────
	// Pronosticos del partido seleccionado
	// ─────────────────────────────────────────────────────────────

	@SuppressWarnings("unchecked")
	public List<Partido> getPronosticos() {
		if (seleccionado == null) {
			return new ArrayList<>();
		}
		List<Partido> ls = (ArrayList<Partido>) seleccionado.getAtributos().get("partidos_usuario");
		if (ls == null) {
			return new ArrayList<>();
		}
		// Se excluye el usuario de resultados oficiales
		List<Partido> resultado = new ArrayList<>();
		for (Partido p : ls) {
			Object nombre = p.getAtributos().get("nombre_usuario");
			if (realNombre != null && realNombre.equals(nombre)) {
				continue;
			}
			resultado.add(p);
		}
		// Orden alfabetico por nombre de usuario (estable entre partidos)
		resultado.sort(Comparator.comparing(this::getNombreUsuario, String.CASE_INSENSITIVE_ORDER));
		return resultado;
	}

	public String getNombreUsuario(Partido pron) {
		Object n = pron.getAtributos().get("nombre_usuario");
		return n == null ? "" : n.toString();
	}

	/**
	 * Devuelve la clase CSS del pronostico segun el resultado de referencia (real
	 * si ya se jugo, o el simulado). Devuelve cadena vacia cuando todavia no hay
	 * resultado de referencia que aplicar.
	 */
	public String getColor(Partido pron) {
		if (!isMostrarColores()) {
			return "";
		}
		Integer rg1;
		Integer rg2;
		if (isJugado()) {
			rg1 = seleccionado.getGoles1();
			rg2 = seleccionado.getGoles2();
		} else {
			rg1 = simGoles1;
			rg2 = simGoles2;
		}
		if (rg1 == null || rg2 == null) {
			return "";
		}
		Integer pg1 = pron.getGoles1();
		Integer pg2 = pron.getGoles2();
		// Marcador exacto
		if (pg1.equals(rg1) && pg2.equals(rg2)) {
			return "pron-verde";
		}
		// Mismo desenlace (ganador o empate)
		int real = rg1.compareTo(rg2);
		int usu = pg1.compareTo(pg2);
		if ((real == 0 && usu == 0) || (real > 0 && usu > 0) || (real < 0 && usu < 0)) {
			return "pron-amarillo";
		}
		return "pron-rojo";
	}

	// ─────────────────────────────────────────────────────────────
	// Helpers de presentacion (nombres / banderas de los equipos)
	// ─────────────────────────────────────────────────────────────

	public String getNombreEq1(Partido p) {
		if (p == null) {
			return "";
		}
		if (p.getEq1() != null) {
			return (String) p.getEq1().getAtributos().get("nombre");
		}
		Object e = p.getAtributos().get("eq1");
		return e == null ? "" : e.toString();
	}

	public String getNombreEq2(Partido p) {
		if (p == null) {
			return "";
		}
		if (p.getEq2() != null) {
			return (String) p.getEq2().getAtributos().get("nombre");
		}
		Object e = p.getAtributos().get("eq2");
		return e == null ? "" : e.toString();
	}

	public String getNombreFase(Partido p) {
		if (p == null) {
			return "";
		}
		Object nombre = p.getAtributos().get("nombre");
		if (nombre != null && !"partidos_grupos".equals(p.getFase())) {
			// Para fases eliminatorias el atributo "nombre" ya describe el cruce
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

	// ─────────────────────────────────────────────────────────────
	// Getters / Setters
	// ─────────────────────────────────────────────────────────────

	public List<Partido> getPartidos() {
		return partidos;
	}

	public void setPartidos(List<Partido> partidos) {
		this.partidos = partidos;
	}

	public Partido getSeleccionado() {
		return seleccionado;
	}

	public void setSeleccionado(Partido seleccionado) {
		this.seleccionado = seleccionado;
	}

	public Integer getSimGoles1() {
		return simGoles1;
	}

	public void setSimGoles1(Integer simGoles1) {
		this.simGoles1 = simGoles1;
	}

	public Integer getSimGoles2() {
		return simGoles2;
	}

	public void setSimGoles2(Integer simGoles2) {
		this.simGoles2 = simGoles2;
	}

	public boolean isSimulado() {
		return simulado;
	}

	public void setSimulado(boolean simulado) {
		this.simulado = simulado;
	}
}
