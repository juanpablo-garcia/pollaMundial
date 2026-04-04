package co.com.tmsolutions.model;

public class Partido extends BaseModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Equipo eq1;
	private Equipo eq2;
	private Boolean realizado = Boolean.FALSE;
	private Integer goles1 = new Integer(0);
	private Integer goles2 = new Integer(0);
	private Integer penales1 = new Integer(0);
	private Integer penales2 = new Integer(0);
	private Integer ta1 = new Integer(0);  // tarjetas amarillas eq1
	private Integer ta2 = new Integer(0);  // tarjetas amarillas eq2
	private Integer tr1 = new Integer(0);  // tarjetas rojas eq1
	private Integer tr2 = new Integer(0);  // tarjetas rojas eq2
	private Integer idpartido;
	private String fase;

	public Partido() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Partido(Equipo eq1, Equipo eq2, Integer idpartido, String fase) {

		super();
		this.idpartido = idpartido;
		this.fase = fase;
		setEquipos(eq1, eq2);
	}

	public Equipo verificarResultadoFaseFinal() {
		if (this.getEq1() != null && this.getEq2() != null && this.getGoles1() != null && this.getGoles2() != null) {
			// Empate
			if (this.getGoles1().compareTo(this.getGoles2()) == 0) {
				// Se verifican los penales
				if (this.getPenales1().compareTo(this.getPenales2()) > 0) {
					return this.getEq1();
				} else if (this.getPenales2().compareTo(this.getPenales1()) > 0) {
					return this.getEq2();
				}
			} else if (this.getGoles1().compareTo(this.getGoles2()) > 0) {
				return this.getEq1();
			} else {
				return this.getEq2();
			}
		}
		return null;

	}
	
	public Equipo verificarResultadoFaseFinalSubCampeon() {
		if (this.getEq1() != null && this.getEq2() != null && this.getGoles1() != null && this.getGoles2() != null) {
			// Empate
			if (this.getGoles1().compareTo(this.getGoles2()) == 0) {
				// Se verifican los penales
				if (this.getPenales1().compareTo(this.getPenales2()) > 0) {
					return this.getEq2();
				} else if (this.getPenales2().compareTo(this.getPenales1()) > 0) {
					return this.getEq1();
				}
			} else if (this.getGoles1().compareTo(this.getGoles2()) > 0) {
				return this.getEq2();
			} else {
				return this.getEq1();
			}
		}
		return null;

	}

	public void setEquipos(Equipo eq1, Equipo eq2) {
		this.eq1 = eq1;
		this.eq2 = eq2;
	}

	public Equipo getEq1() {
		return eq1;
	}

	public void setEq1(Equipo eq1) {
		this.eq1 = eq1;
	}

	public Equipo getEq2() {
		return eq2;
	}

	public void setEq2(Equipo eq2) {
		this.eq2 = eq2;
	}

	public Integer getGoles1() {
		if (goles1 == null) {
			goles1 = new Integer(0);
		}
		return goles1;
	}

	public void setGoles1(Integer goles1) {
		this.goles1 = goles1;
	}

	public Integer getGoles2() {
		if (goles2 == null) {
			goles2 = new Integer(0);
		}
		return goles2;
	}

	public void setGoles2(Integer goles2) {
		this.goles2 = goles2;
	}

	public Integer getPenales1() {
		return penales1;
	}

	public void setPenales1(Integer penales1) {
		this.penales1 = penales1;
	}

	public Integer getPenales2() {
		return penales2;
	}

	public void setPenales2(Integer penales2) {
		this.penales2 = penales2;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Integer getIdpartido() {
		return idpartido;
	}

	public void setIdpartido(Integer idpartido) {
		this.idpartido = idpartido;
	}

	public String getFase() {
		return fase;
	}

	public void setFase(String fase) {
		this.fase = fase;
	}

	public Integer getTa1() {
		if (ta1 == null) ta1 = 0;
		return ta1;
	}

	public void setTa1(Integer ta1) {
		this.ta1 = ta1;
	}

	public Integer getTa2() {
		if (ta2 == null) ta2 = 0;
		return ta2;
	}

	public void setTa2(Integer ta2) {
		this.ta2 = ta2;
	}

	public Integer getTr1() {
		if (tr1 == null) tr1 = 0;
		return tr1;
	}

	public void setTr1(Integer tr1) {
		this.tr1 = tr1;
	}

	public Integer getTr2() {
		if (tr2 == null) tr2 = 0;
		return tr2;
	}

	public void setTr2(Integer tr2) {
		this.tr2 = tr2;
	}

	/**
	 * @return the realizado
	 */
	public Boolean getRealizado() {
		if (realizado == null) {
			realizado = Boolean.FALSE;
		}
		return realizado;
	}

	/**
	 * @param realizado the realizado to set
	 */
	public void setRealizado(Boolean realizado) {
		this.realizado = realizado;
	}

}
