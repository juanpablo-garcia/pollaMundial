package co.com.tmsolutions.beans;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@Named
@ApplicationScoped
public class Bean_Theme implements Serializable {

	private static final long serialVersionUID = 1L;

	private String theme;

	public Bean_Theme() {
		String env = System.getenv("POLLA_THEME");
		this.theme = (env != null && !env.isEmpty()) ? env : "green";
	}

	public String getTheme() {
		return theme;
	}

	public boolean isBlue() {
		return "blue".equalsIgnoreCase(theme);
	}
}
