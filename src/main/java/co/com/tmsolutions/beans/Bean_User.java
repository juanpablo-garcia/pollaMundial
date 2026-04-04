package co.com.tmsolutions.beans;

import java.io.IOException;
import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import co.com.tmsolutions.model.Usuario;

@Named
@SessionScoped
public class Bean_User implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Usuario usuario;

	public Bean_User() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void cerrarSession() {
		this.usuario = null;
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

}
