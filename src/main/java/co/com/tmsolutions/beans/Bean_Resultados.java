package co.com.tmsolutions.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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

@Named("bean_Resultados")
@ViewScoped
public class Bean_Resultados implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private Bean_User bean_User;
	@Inject
	private UsuarioDao usuarioDao;
	private List<Usuario> usuarios;

	public Bean_Resultados() {
		super();
	}

	@PostConstruct
	private void init() {
		usuarios = usuarioDao.findAll();
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
	 * @return the usuarios
	 */
	public List<Usuario> getUsuarios() {
		return usuarios;
	}

	/**
	 * @param usuarios the usuarios to set
	 */
	public void setUsuarios(List<Usuario> usuarios) {
		this.usuarios = usuarios;
	}

}
