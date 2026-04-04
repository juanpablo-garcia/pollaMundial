package co.com.tmsolutions.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "partidosusuario")
public class PartidosUsuario extends BaseModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<Partido> partidos = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario usuario;

	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaModificacion;

	public PartidosUsuario() {
		super();
		// TODO Auto-generated constructor stub
	}/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof BaseModel))
			return false;
		BaseModel other = (BaseModel) obj;
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		return true;
	}

	public PartidosUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public List<Partido> getPartidos() {
		return partidos;
	}

	public void setPartidos(List<Partido> partidos) {
		this.partidos = partidos;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * @return the usuario
	 */
	public Usuario getUsuario() {
		return usuario;
	}

	/**
	 * @param usuario the usuario to set
	 */
	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	/**
	 * @return the fechaModificacion
	 */
	public Date getFechaModificacion() {
		return fechaModificacion;
	}

	/**
	 * @param fechaModificacion the fechaModificacion to set
	 */
	public void setFechaModificacion(Date fechaModificacion) {
		this.fechaModificacion = fechaModificacion;
	}

}
