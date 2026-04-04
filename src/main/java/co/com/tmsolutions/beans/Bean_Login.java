package co.com.tmsolutions.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.primefaces.context.PrimeFacesContext;

import co.com.tmsolutions.dao.UsuarioDao;
import co.com.tmsolutions.model.Usuario;

@Named("bean_Login")
@RequestScoped
public class Bean_Login implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private UsuarioDao usuariodao;
	private String nombre;
	private String mail;
	private String contrasena;
	private String telefono;
	@Inject
	private Bean_User bean_User;

	public void guardar() {
		List<Usuario> u = usuariodao.findByCriteria(Restrictions.eq("mail", mail));
		if (!u.isEmpty()) {
			addMessage("Mail existe " + this.mail);
			return;
		}
		Usuario entity = new Usuario();
		entity.setMail(mail);
		entity.setContrasena(contrasena);
		entity.getAtributos().put("nombre", nombre);
		entity.getAtributos().put("telefono", telefono);
		Usuario us = usuariodao.save(entity);
		doLogin(us);
	}

	private void addMessage(String ms) {
		FacesMessage message = new FacesMessage(ms, "");
		PrimeFacesContext.getCurrentInstance().addMessage(null, message);

	}

	private void doLogin(Usuario us) {

		bean_User.setUsuario(us);
		try {
			FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("user", bean_User.getUsuario());
			FacesContext.getCurrentInstance().getExternalContext().redirect("secured/marcadores.xhtml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void login() {
		List<Usuario> u = usuariodao.findByCriteria(Restrictions.ilike("mail", mail, MatchMode.EXACT));
		if (!u.isEmpty()) {
			Usuario us = u.get(0);
			if (us.getContrasena().equals(contrasena)) {
				doLogin(us);
			} else {
				addMessage("Contraseña no valida");
			}
		} else {
			addMessage("Usuario no existe");
		}
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getContrasena() {
		return contrasena;
	}

	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
