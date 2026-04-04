package co.com.tmsolutions.model;

public class Posicion extends BaseModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Equipo equipo;

	public Posicion() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Equipo getEquipo() {
		return equipo;
	}

	public void setEquipo(Equipo equipo) {
		this.equipo = equipo;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
