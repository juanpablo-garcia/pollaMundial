package co.com.tmsolutions.service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduce los nombres de equipo en ingles que devuelve la API de ESPN al nombre
 * en espanol que usa la aplicacion (ver GenerarEquipos en Bean_Marcadores). La
 * comparacion es tolerante a mayusculas, acentos y signos (guiones, apostrofes),
 * por lo que cada equipo puede tener varios alias.
 */
public final class EquiposNombreMapper {

	private static final Map<String, String> EN_A_ES = new HashMap<>();

	private EquiposNombreMapper() {
	}

	private static void add(String es, String... alias) {
		for (String a : alias) {
			EN_A_ES.put(norm(a), es);
		}
	}

	static {
		// Grupo A
		add("Mexico", "Mexico");
		add("Sudafrica", "South Africa");
		add("Corea del Sur", "South Korea", "Korea Republic");
		add("Chequia", "Czechia", "Czech Republic");
		// Grupo B
		add("Canada", "Canada");
		add("Bosnia y Herzegovina", "Bosnia-Herzegovina", "Bosnia and Herzegovina", "Bosnia & Herzegovina");
		add("Qatar", "Qatar");
		add("Suiza", "Switzerland");
		// Grupo C
		add("Brasil", "Brazil");
		add("Marruecos", "Morocco");
		add("Haiti", "Haiti");
		add("Escocia", "Scotland");
		// Grupo D
		add("Estados Unidos", "United States", "USA");
		add("Paraguay", "Paraguay");
		add("Australia", "Australia");
		add("Turquia", "Turkey", "Turkiye");
		// Grupo E
		add("Alemania", "Germany");
		add("Curazao", "Curacao");
		add("Costa de Marfil", "Ivory Coast", "Cote d'Ivoire");
		add("Ecuador", "Ecuador");
		// Grupo F
		add("Paises Bajos", "Netherlands");
		add("Japon", "Japan");
		add("Suecia", "Sweden");
		add("Tunez", "Tunisia");
		// Grupo G
		add("Belgica", "Belgium");
		add("Egipto", "Egypt");
		add("Iran", "Iran", "IR Iran");
		add("Nueva Zelanda", "New Zealand");
		// Grupo H
		add("España", "Spain");
		add("Cabo Verde", "Cape Verde", "Cabo Verde");
		add("Arabia Saudita", "Saudi Arabia");
		add("Uruguay", "Uruguay");
		// Grupo I
		add("Francia", "France");
		add("Senegal", "Senegal");
		add("Irak", "Iraq");
		add("Noruega", "Norway");
		// Grupo J
		add("Argentina", "Argentina");
		add("Algeria", "Algeria");
		add("Austria", "Austria");
		add("Jordania", "Jordan");
		// Grupo K
		add("Portugal", "Portugal");
		add("RD Congo", "DR Congo", "Congo DR", "Democratic Republic of the Congo");
		add("Uzbekistan", "Uzbekistan");
		add("Colombia", "Colombia");
		// Grupo L
		add("Inglaterra", "England");
		add("Croacia", "Croatia");
		add("Ghana", "Ghana");
		add("Panama", "Panama");
	}

	/**
	 * @return el nombre en espanol de la app, o {@code null} si no hay equivalencia
	 *         (en ese caso el llamador deberia reportarlo para ajustar el alias).
	 */
	public static String aEspanol(String nombreIngles) {
		return nombreIngles == null ? null : EN_A_ES.get(norm(nombreIngles));
	}

	private static String norm(String s) {
		String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinAcentos.toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}
