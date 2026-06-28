package co.com.tmsolutions.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import co.com.tmsolutions.model.Equipo;
import co.com.tmsolutions.model.Partido;
import co.com.tmsolutions.model.Posicion;

/**
 * Recalcula, para un único pronóstico (el conjunto de partidos de un usuario), los
 * EQUIPOS del bracket eliminatorio a partir de los marcadores ya cargados: tabla de
 * posiciones de cada grupo, cruce de la Ronda de 32 (incluyendo los mejores terceros
 * según la tabla oficial FIFA) y propagación de ganadores hasta la final.
 *
 * <p>Solo modifica los nombres de equipo (eq1/eq2) de cada partido; NUNCA toca los
 * goles, penales ni tarjetas. Es la misma lógica que usa {@code Bean_Marcadores} al
 * pintar el bracket, pero empaquetada para poder ejecutarla en lote sobre todos los
 * jugadores (ver {@code UsuarioPartidoDao.recalcularBrackets}).
 *
 * <p>NOTA: esta lógica (posiciones, cruce de R32 con la tabla Anexo C y avance
 * octavos→final) está replicada respecto a {@code Bean_Marcadores} de forma
 * deliberada, para no alterar el flujo de la UI en vivo. La tabla {@link #TABLA_TERCEROS}
 * es un dato congelado tomado del reglamento oficial; si alguna vez cambia, hay que
 * actualizar AMBAS copias (esta y la de {@code Bean_Marcadores}).
 */
public class CalculadoraBracket {

	/**
	 * Cupos de la Ronda de 32 que reciben un mejor tercero, en el MISMO orden que
	 * las columnas de la tabla oficial FIFA: 1A, 1B, 1D, 1E, 1G, 1I, 1K, 1L.
	 */
	public static final String[] CUPOS_TERCEROS_ORDEN = { "r32_79", "r32_85", "r32_81", "r32_74", "r32_82", "r32_77",
			"r32_87", "r32_80" };

	/**
	 * Tabla oficial del Anexo C del reglamento FIFA 2026: las 495 combinaciones de
	 * los 8 grupos que clasifican un tercero. Cada entrada tiene 16 caracteres: los
	 * 8 primeros son las letras (ordenadas) de los grupos cuyo tercero clasifica, y
	 * los 8 siguientes el grupo del tercero asignado a cada ganador, en el orden de
	 * columnas 1A, 1B, 1D, 1E, 1G, 1I, 1K, 1L (igual que {@link #CUPOS_TERCEROS_ORDEN}).
	 */
	private static final String[] ANEXO_C_TERCEROS = {
			"ABCDEFGHHGBCAFDE", "ABCDEFGICGBDAFEI", "ABCDEFGJCGBDAFEJ", "ABCDEFGKCGBDAFEK", "ABCDEFGLCGBDAFLE", "ABCDEFHIHEBCAFDI", "ABCDEFHJHJBCAFDE", "ABCDEFHKHEBCAFDK",
			"ABCDEFHLHFBCADLE", "ABCDEFIJCJBDAFEI", "ABCDEFIKCEBDAFIK", "ABCDEFILCEBDAFLI", "ABCDEFJKCJBDAFEK", "ABCDEFJLCJBDAFLE", "ABCDEFKLCEBDAFLK", "ABCDEGHIHGBCADEI",
			"ABCDEGHJHGBCADEJ", "ABCDEGHKHGBCADEK", "ABCDEGHLHGBCADLE", "ABCDEGIJEGBCADIJ", "ABCDEGIKEGBCADIK", "ABCDEGILEGBCADLI", "ABCDEGJKEGBCADJK", "ABCDEGJLEGBCADLJ",
			"ABCDEGKLEGBCADLK", "ABCDEHIJHJBCADEI", "ABCDEHIKHEBCADIK", "ABCDEHILHEBCADLI", "ABCDEHJKHJBCADEK", "ABCDEHJLHJBCADLE", "ABCDEHKLHEBCADLK", "ABCDEIJKEJBCADIK",
			"ABCDEIJLEJBCADLI", "ABCDEIKLEIBCADLK", "ABCDEJKLEJBCADLK", "ABCDFGHIHGBCAFDI", "ABCDFGHJHGBCAFDJ", "ABCDFGHKHGBCAFDK", "ABCDFGHLCGBDAFLH", "ABCDFGIJCGBDAFIJ",
			"ABCDFGIKCGBDAFIK", "ABCDFGILCGBDAFLI", "ABCDFGJKCGBDAFJK", "ABCDFGJLCGBDAFLJ", "ABCDFGKLCGBDAFLK", "ABCDFHIJHJBCAFDI", "ABCDFHIKHFBCADIK", "ABCDFHILHFBCADLI",
			"ABCDFHJKHJBCAFDK", "ABCDFHJLCJBDAFLH", "ABCDFHKLHFBCADLK", "ABCDFIJKCJBDAFIK", "ABCDFIJLCJBDAFLI", "ABCDFIKLCIBDAFLK", "ABCDFJKLCJBDAFLK", "ABCDGHIJHGBCADIJ",
			"ABCDGHIKHGBCADIK", "ABCDGHILHGBCADLI", "ABCDGHJKHGBCADJK", "ABCDGHJLHGBCADLJ", "ABCDGHKLHGBCADLK", "ABCDGIJKCJBDAGIK", "ABCDGIJLCJBDAGLI", "ABCDGIKLIGBCADLK",
			"ABCDGJKLCJBDAGLK", "ABCDHIJKHJBCADIK", "ABCDHIJLHJBCADLI", "ABCDHIKLHIBCADLK", "ABCDHJKLHJBCADLK", "ABCDIJKLIJBCADLK", "ABCEFGHIHGBCAFEI", "ABCEFGHJHGBCAFEJ",
			"ABCEFGHKHGBCAFEK", "ABCEFGHLHGBCAFLE", "ABCEFGIJEGBCAFIJ", "ABCEFGIKEGBCAFIK", "ABCEFGILEGBCAFLI", "ABCEFGJKEGBCAFJK", "ABCEFGJLEGBCAFLJ", "ABCEFGKLEGBCAFLK",
			"ABCEFHIJHJBCAFEI", "ABCEFHIKHEBCAFIK", "ABCEFHILHEBCAFLI", "ABCEFHJKHJBCAFEK", "ABCEFHJLHJBCAFLE", "ABCEFHKLHEBCAFLK", "ABCEFIJKEJBCAFIK", "ABCEFIJLEJBCAFLI",
			"ABCEFIKLEIBCAFLK", "ABCEFJKLEJBCAFLK", "ABCEGHIJHJBCAGEI", "ABCEGHIKEGBCAHIK", "ABCEGHILEGBCAHLI", "ABCEGHJKHJBCAGEK", "ABCEGHJLHJBCAGLE", "ABCEGHKLEGBCAHLK",
			"ABCEGIJKEJBCAGIK", "ABCEGIJLEJBCAGLI", "ABCEGIKLEGBAICLK", "ABCEGJKLEJBCAGLK", "ABCEHIJKEJBCAHIK", "ABCEHIJLEJBCAHLI", "ABCEHIKLEIBCAHLK", "ABCEHJKLEJBCAHLK",
			"ABCEIJKLEJBAICLK", "ABCFGHIJHGBCAFIJ", "ABCFGHIKHGBCAFIK", "ABCFGHILHGBCAFLI", "ABCFGHJKHGBCAFJK", "ABCFGHJLHGBCAFLJ", "ABCFGHKLHGBCAFLK", "ABCFGIJKCJBFAGIK",
			"ABCFGIJLCJBFAGLI", "ABCFGIKLIGBCAFLK", "ABCFGJKLCJBFAGLK", "ABCFHIJKHJBCAFIK", "ABCFHIJLHJBCAFLI", "ABCFHIKLHIBCAFLK", "ABCFHJKLHJBCAFLK", "ABCFIJKLIJBCAFLK",
			"ABCGHIJKHJBCAGIK", "ABCGHIJLHJBCAGLI", "ABCGHIKLIGBCAHLK", "ABCGHJKLHJBCAGLK", "ABCGIJKLIJBCAGLK", "ABCHIJKLIJBCAHLK", "ABDEFGHIHGBDAFEI", "ABDEFGHJHGBDAFEJ",
			"ABDEFGHKHGBDAFEK", "ABDEFGHLHGBDAFLE", "ABDEFGIJEGBDAFIJ", "ABDEFGIKEGBDAFIK", "ABDEFGILEGBDAFLI", "ABDEFGJKEGBDAFJK", "ABDEFGJLEGBDAFLJ", "ABDEFGKLEGBDAFLK",
			"ABDEFHIJHJBDAFEI", "ABDEFHIKHEBDAFIK", "ABDEFHILHEBDAFLI", "ABDEFHJKHJBDAFEK", "ABDEFHJLHJBDAFLE", "ABDEFHKLHEBDAFLK", "ABDEFIJKEJBDAFIK", "ABDEFIJLEJBDAFLI",
			"ABDEFIKLEIBDAFLK", "ABDEFJKLEJBDAFLK", "ABDEGHIJHJBDAGEI", "ABDEGHIKEGBDAHIK", "ABDEGHILEGBDAHLI", "ABDEGHJKHJBDAGEK", "ABDEGHJLHJBDAGLE", "ABDEGHKLEGBDAHLK",
			"ABDEGIJKEJBDAGIK", "ABDEGIJLEJBDAGLI", "ABDEGIKLEGBAIDLK", "ABDEGJKLEJBDAGLK", "ABDEHIJKEJBDAHIK", "ABDEHIJLEJBDAHLI", "ABDEHIKLEIBDAHLK", "ABDEHJKLEJBDAHLK",
			"ABDEIJKLEJBAIDLK", "ABDFGHIJHGBDAFIJ", "ABDFGHIKHGBDAFIK", "ABDFGHILHGBDAFLI", "ABDFGHJKHGBDAFJK", "ABDFGHJLHGBDAFLJ", "ABDFGHKLHGBDAFLK", "ABDFGIJKFJBDAGIK",
			"ABDFGIJLFJBDAGLI", "ABDFGIKLIGBDAFLK", "ABDFGJKLFJBDAGLK", "ABDFHIJKHJBDAFIK", "ABDFHIJLHJBDAFLI", "ABDFHIKLHIBDAFLK", "ABDFHJKLHJBDAFLK", "ABDFIJKLIJBDAFLK",
			"ABDGHIJKHJBDAGIK", "ABDGHIJLHJBDAGLI", "ABDGHIKLIGBDAHLK", "ABDGHJKLHJBDAGLK", "ABDGIJKLIJBDAGLK", "ABDHIJKLIJBDAHLK", "ABEFGHIJHJBFAGEI", "ABEFGHIKEGBFAHIK",
			"ABEFGHILEGBFAHLI", "ABEFGHJKHJBFAGEK", "ABEFGHJLHJBFAGLE", "ABEFGHKLEGBFAHLK", "ABEFGIJKEJBFAGIK", "ABEFGIJLEJBFAGLI", "ABEFGIKLEGBAIFLK", "ABEFGJKLEJBFAGLK",
			"ABEFHIJKEJBFAHIK", "ABEFHIJLEJBFAHLI", "ABEFHIKLEIBFAHLK", "ABEFHJKLEJBFAHLK", "ABEFIJKLEJBAIFLK", "ABEGHIJKEJBAHGIK", "ABEGHIJLEJBAHGLI", "ABEGHIKLEGBAIHLK",
			"ABEGHJKLEJBAHGLK", "ABEGIJKLEJBAIGLK", "ABEHIJKLEJBAIHLK", "ABFGHIJKHJBFAGIK", "ABFGHIJLHJBFAGLI", "ABFGHIKLHGBAIFLK", "ABFGHJKLHJBFAGLK", "ABFGIJKLIJBFAGLK",
			"ABFHIJKLHJBAIFLK", "ABGHIJKLHJBAIGLK", "ACDEFGHIHGECAFDI", "ACDEFGHJHGJCAFDE", "ACDEFGHKHGECAFDK", "ACDEFGHLHGFCADLE", "ACDEFGIJCGJDAFEI", "ACDEFGIKCGEDAFIK",
			"ACDEFGILCGEDAFLI", "ACDEFGJKCGJDAFEK", "ACDEFGJLCGJDAFLE", "ACDEFGKLCGEDAFLK", "ACDEFHIJHJECAFDI", "ACDEFHIKHEFCADIK", "ACDEFHILHEFCADLI", "ACDEFHJKHJECAFDK",
			"ACDEFHJLHJFCADLE", "ACDEFHKLHEFCADLK", "ACDEFIJKCJEDAFIK", "ACDEFIJLCJEDAFLI", "ACDEFIKLCEIDAFLK", "ACDEFJKLCJEDAFLK", "ACDEGHIJHGJCADEI", "ACDEGHIKHGECADIK",
			"ACDEGHILHGECADLI", "ACDEGHJKHGJCADEK", "ACDEGHJLHGJCADLE", "ACDEGHKLHGECADLK", "ACDEGIJKEGJCADIK", "ACDEGIJLEGJCADLI", "ACDEGIKLEGICADLK", "ACDEGJKLEGJCADLK",
			"ACDEHIJKHJECADIK", "ACDEHIJLHJECADLI", "ACDEHIKLHEICADLK", "ACDEHJKLHJECADLK", "ACDEIJKLEJICADLK", "ACDFGHIJHGJCAFDI", "ACDFGHIKHGFCADIK", "ACDFGHILHGFCADLI",
			"ACDFGHJKHGJCAFDK", "ACDFGHJLCGJDAFLH", "ACDFGHKLHGFCADLK", "ACDFGIJKCGJDAFIK", "ACDFGIJLCGJDAFLI", "ACDFGIKLCGIDAFLK", "ACDFGJKLCGJDAFLK", "ACDFHIJKHJFCADIK",
			"ACDFHIJLHJFCADLI", "ACDFHIKLHFICADLK", "ACDFHJKLHJFCADLK", "ACDFIJKLCJIDAFLK", "ACDGHIJKHGJCADIK", "ACDGHIJLHGJCADLI", "ACDGHIKLHGICADLK", "ACDGHJKLHGJCADLK",
			"ACDGIJKLIGJCADLK", "ACDHIJKLHJICADLK", "ACEFGHIJHGJCAFEI", "ACEFGHIKHGECAFIK", "ACEFGHILHGECAFLI", "ACEFGHJKHGJCAFEK", "ACEFGHJLHGJCAFLE", "ACEFGHKLHGECAFLK",
			"ACEFGIJKEGJCAFIK", "ACEFGIJLEGJCAFLI", "ACEFGIKLEGICAFLK", "ACEFGJKLEGJCAFLK", "ACEFHIJKHJECAFIK", "ACEFHIJLHJECAFLI", "ACEFHIKLHEICAFLK", "ACEFHJKLHJECAFLK",
			"ACEFIJKLEJICAFLK", "ACEGHIJKEGJCAHIK", "ACEGHIJLEGJCAHLI", "ACEGHIKLEGICAHLK", "ACEGHJKLEGJCAHLK", "ACEGIJKLEJICAGLK", "ACEHIJKLEJICAHLK", "ACFGHIJKHGJCAFIK",
			"ACFGHIJLHGJCAFLI", "ACFGHIKLHGICAFLK", "ACFGHJKLHGJCAFLK", "ACFGIJKLIGJCAFLK", "ACFHIJKLHJICAFLK", "ACGHIJKLHJICAGLK", "ADEFGHIJHGJDAFEI", "ADEFGHIKHGEDAFIK",
			"ADEFGHILHGEDAFLI", "ADEFGHJKHGJDAFEK", "ADEFGHJLHGJDAFLE", "ADEFGHKLHGEDAFLK", "ADEFGIJKEGJDAFIK", "ADEFGIJLEGJDAFLI", "ADEFGIKLEGIDAFLK", "ADEFGJKLEGJDAFLK",
			"ADEFHIJKHJEDAFIK", "ADEFHIJLHJEDAFLI", "ADEFHIKLHEIDAFLK", "ADEFHJKLHJEDAFLK", "ADEFIJKLEJIDAFLK", "ADEGHIJKEGJDAHIK", "ADEGHIJLEGJDAHLI", "ADEGHIKLEGIDAHLK",
			"ADEGHJKLEGJDAHLK", "ADEGIJKLEJIDAGLK", "ADEHIJKLEJIDAHLK", "ADFGHIJKHGJDAFIK", "ADFGHIJLHGJDAFLI", "ADFGHIKLHGIDAFLK", "ADFGHJKLHGJDAFLK", "ADFGIJKLIGJDAFLK",
			"ADFHIJKLHJIDAFLK", "ADGHIJKLHJIDAGLK", "AEFGHIJKEGJFAHIK", "AEFGHIJLEGJFAHLI", "AEFGHIKLEGIFAHLK", "AEFGHJKLEGJFAHLK", "AEFGIJKLEJIFAGLK", "AEFHIJKLEJIFAHLK",
			"AEGHIJKLEJIAHGLK", "AFGHIJKLHJIFAGLK", "BCDEFGHICGBDHFEI", "BCDEFGHJHGBCJFDE", "BCDEFGHKCGBDHFEK", "BCDEFGHLCGBDHFLE", "BCDEFGIJCGBDJFEI", "BCDEFGIKCGBDEFIK",
			"BCDEFGILCGBDEFLI", "BCDEFGJKCGBDJFEK", "BCDEFGJLCGBDJFLE", "BCDEFGKLCGBDEFLK", "BCDEFHIJCJBDHFEI", "BCDEFHIKCEBDHFIK", "BCDEFHILCEBDHFLI", "BCDEFHJKCJBDHFEK",
			"BCDEFHJLCJBDHFLE", "BCDEFHKLCEBDHFLK", "BCDEFIJKCJBDEFIK", "BCDEFIJLCJBDEFLI", "BCDEFIKLCEBDIFLK", "BCDEFJKLCJBDEFLK", "BCDEGHIJHGBCJDEI", "BCDEGHIKEGBCHDIK",
			"BCDEGHILEGBCHDLI", "BCDEGHJKHGBCJDEK", "BCDEGHJLHGBCJDLE", "BCDEGHKLEGBCHDLK", "BCDEGIJKEGBCJDIK", "BCDEGIJLEGBCJDLI", "BCDEGIKLEGBCIDLK", "BCDEGJKLEGBCJDLK",
			"BCDEHIJKEJBCHDIK", "BCDEHIJLEJBCHDLI", "BCDEHIKLEIBCHDLK", "BCDEHJKLEJBCHDLK", "BCDEIJKLEJBCIDLK", "BCDFGHIJHGBCJFDI", "BCDFGHIKCGBDHFIK", "BCDFGHILCGBDHFLI",
			"BCDFGHJKHGBCJFDK", "BCDFGHJLCGBDHFLJ", "BCDFGHKLCGBDHFLK", "BCDFGIJKCGBDJFIK", "BCDFGIJLCGBDJFLI", "BCDFGIKLCGBDIFLK", "BCDFGJKLCGBDJFLK", "BCDFHIJKCJBDHFIK",
			"BCDFHIJLCJBDHFLI", "BCDFHIKLCIBDHFLK", "BCDFHJKLCJBDHFLK", "BCDFIJKLCJBDIFLK", "BCDGHIJKHGBCJDIK", "BCDGHIJLHGBCJDLI", "BCDGHIKLHGBCIDLK", "BCDGHJKLHGBCJDLK",
			"BCDGIJKLIGBCJDLK", "BCDHIJKLHJBCIDLK", "BCEFGHIJHGBCJFEI", "BCEFGHIKEGBCHFIK", "BCEFGHILEGBCHFLI", "BCEFGHJKHGBCJFEK", "BCEFGHJLHGBCJFLE", "BCEFGHKLEGBCHFLK",
			"BCEFGIJKEGBCJFIK", "BCEFGIJLEGBCJFLI", "BCEFGIKLEGBCIFLK", "BCEFGJKLEGBCJFLK", "BCEFHIJKEJBCHFIK", "BCEFHIJLEJBCHFLI", "BCEFHIKLEIBCHFLK", "BCEFHJKLEJBCHFLK",
			"BCEFIJKLEJBCIFLK", "BCEGHIJKEJBCHGIK", "BCEGHIJLEJBCHGLI", "BCEGHIKLEGBCIHLK", "BCEGHJKLEJBCHGLK", "BCEGIJKLEJBCIGLK", "BCEHIJKLEJBCIHLK", "BCFGHIJKHGBCJFIK",
			"BCFGHIJLHGBCJFLI", "BCFGHIKLHGBCIFLK", "BCFGHJKLHGBCJFLK", "BCFGIJKLIGBCJFLK", "BCFHIJKLHJBCIFLK", "BCGHIJKLHJBCIGLK", "BDEFGHIJHGBDJFEI", "BDEFGHIKEGBDHFIK",
			"BDEFGHILEGBDHFLI", "BDEFGHJKHGBDJFEK", "BDEFGHJLHGBDJFLE", "BDEFGHKLEGBDHFLK", "BDEFGIJKEGBDJFIK", "BDEFGIJLEGBDJFLI", "BDEFGIKLEGBDIFLK", "BDEFGJKLEGBDJFLK",
			"BDEFHIJKEJBDHFIK", "BDEFHIJLEJBDHFLI", "BDEFHIKLEIBDHFLK", "BDEFHJKLEJBDHFLK", "BDEFIJKLEJBDIFLK", "BDEGHIJKEJBDHGIK", "BDEGHIJLEJBDHGLI", "BDEGHIKLEGBDIHLK",
			"BDEGHJKLEJBDHGLK", "BDEGIJKLEJBDIGLK", "BDEHIJKLEJBDIHLK", "BDFGHIJKHGBDJFIK", "BDFGHIJLHGBDJFLI", "BDFGHIKLHGBDIFLK", "BDFGHJKLHGBDJFLK", "BDFGIJKLIGBDJFLK",
			"BDFHIJKLHJBDIFLK", "BDGHIJKLHJBDIGLK", "BEFGHIJKEJBFHGIK", "BEFGHIJLEJBFHGLI", "BEFGHIKLEGBFIHLK", "BEFGHJKLEJBFHGLK", "BEFGIJKLEJBFIGLK", "BEFHIJKLEJBFIHLK",
			"BEGHIJKLEJIBHGLK", "BFGHIJKLHJBFIGLK", "CDEFGHIJCGJDHFEI", "CDEFGHIKCGEDHFIK", "CDEFGHILCGEDHFLI", "CDEFGHJKCGJDHFEK", "CDEFGHJLCGJDHFLE", "CDEFGHKLCGEDHFLK",
			"CDEFGIJKCGEDJFIK", "CDEFGIJLCGEDJFLI", "CDEFGIKLCGEDIFLK", "CDEFGJKLCGEDJFLK", "CDEFHIJKCJEDHFIK", "CDEFHIJLCJEDHFLI", "CDEFHIKLCEIDHFLK", "CDEFHJKLCJEDHFLK",
			"CDEFIJKLCJEDIFLK", "CDEGHIJKEGJCHDIK", "CDEGHIJLEGJCHDLI", "CDEGHIKLEGICHDLK", "CDEGHJKLEGJCHDLK", "CDEGIJKLEGICJDLK", "CDEHIJKLEJICHDLK", "CDFGHIJKCGJDHFIK",
			"CDFGHIJLCGJDHFLI", "CDFGHIKLCGIDHFLK", "CDFGHJKLCGJDHFLK", "CDFGIJKLCGIDJFLK", "CDFHIJKLCJIDHFLK", "CDGHIJKLHGICJDLK", "CEFGHIJKEGJCHFIK", "CEFGHIJLEGJCHFLI",
			"CEFGHIKLEGICHFLK", "CEFGHJKLEGJCHFLK", "CEFGIJKLEGICJFLK", "CEFHIJKLEJICHFLK", "CEGHIJKLEJICHGLK", "CFGHIJKLHGICJFLK", "DEFGHIJKEGJDHFIK", "DEFGHIJLEGJDHFLI",
			"DEFGHIKLEGIDHFLK", "DEFGHJKLEGJDHFLK", "DEFGIJKLEGIDJFLK", "DEFHIJKLEJIDHFLK", "DEGHIJKLEJIDHGLK", "DFGHIJKLHGIDJFLK", "EFGHIJKLEJIFHGLK" };

	/** clave = grupos clasificados (8 letras ordenadas) -> asignación (8 letras). */
	public static final Map<String, String> TABLA_TERCEROS = new HashMap<>();
	static {
		for (String fila : ANEXO_C_TERCEROS) {
			TABLA_TERCEROS.put(fila.substring(0, 8), fila.substring(8));
		}
	}

	private Map<Integer, List<Partido>> partidos_grupos = new HashMap<>();
	private Map<Integer, List<Posicion>> posiciones_grupos = new HashMap<>();
	private List<Partido> ronda32 = new ArrayList<>();
	private List<Partido> octavosFinal = new ArrayList<>();
	private List<Partido> cuartosFinal = new ArrayList<>();
	private List<Partido> semifinales = new ArrayList<>();
	private List<Partido> finales = new ArrayList<>();
	private Equipo campeon;

	/**
	 * Recalcula, EN SITIO, los equipos del bracket de un pronóstico completo. Los
	 * objetos {@link Partido} de la lista se modifican (eq1/eq2); los goles quedan
	 * intactos.
	 */
	public void recalcular(List<Partido> partidos) {
		partidos_grupos = new HashMap<>();
		for (int i = 0; i < 12; i++) {
			partidos_grupos.put(new Integer(i), new ArrayList<Partido>());
		}
		List<Partido> grupos = partidos.stream().filter(o -> "partidos_grupos".equals(o.getFase()))
				.sorted((p1, p2) -> p1.getIdpartido().compareTo(p2.getIdpartido())).collect(Collectors.toList());
		for (Partido p : grupos) {
			int key = p.getIdpartido().intValue() / 6;
			if (key < 0 || key > 11)
				key = 11;
			partidos_grupos.get(new Integer(key)).add(p);
		}
		ronda32 = filtrarFase(partidos, "ronda32");
		octavosFinal = filtrarFase(partidos, "octavosFinal");
		cuartosFinal = filtrarFase(partidos, "cuartosFinal");
		semifinales = filtrarFase(partidos, "semifinales");
		finales = filtrarFase(partidos, "finales");

		// Sin la R32 / fases finales no hay nada que recalcular (pronóstico incompleto)
		if (ronda32.isEmpty() || octavosFinal.isEmpty() || cuartosFinal.isEmpty() || semifinales.isEmpty()
				|| finales.isEmpty()) {
			return;
		}
		calcularPosiciones();
	}

	private List<Partido> filtrarFase(List<Partido> partidos, String fase) {
		return partidos.stream().filter(o -> fase.equals(o.getFase()))
				.sorted((p1, p2) -> p1.getIdpartido().compareTo(p2.getIdpartido())).collect(Collectors.toList());
	}

	public Map<Integer, List<Posicion>> getPosicionesGrupos() {
		return posiciones_grupos;
	}

	public Equipo getCampeon() {
		return campeon;
	}

	private void calcularPosiciones() {
		this.posiciones_grupos = new HashMap<>();
		for (Iterator<Integer> iterator = partidos_grupos.keySet().iterator(); iterator.hasNext();) {
			Integer grupo = iterator.next();
			posiciones_grupos.put(grupo, new ArrayList<>());
			List<Partido> partidos = partidos_grupos.get(grupo);
			for (Partido partido : partidos) {
				Posicion peq1 = getPosicionEquipo(grupo, partido.getEq1());
				Posicion peq2 = getPosicionEquipo(grupo, partido.getEq2());
				verificarResultado(partido, peq1, peq2);
			}
		}

		for (Iterator<Integer> iterator = posiciones_grupos.keySet().iterator(); iterator.hasNext();) {
			Integer grupo = iterator.next();
			List<Posicion> posiciones = posiciones_grupos.get(grupo);
			posicionPorEquipos(posiciones, "pos");
		}

		calcularRonda32();
	}

	private Posicion getPosicionEquipo(Integer grupo, Equipo eq) {
		Optional<Posicion> opeq1 = posiciones_grupos.get(grupo).stream().filter(pg -> pg.getEquipo().equals(eq))
				.findFirst();
		Posicion peq1 = null;
		if (opeq1.isPresent()) {
			peq1 = opeq1.get();
		} else {
			peq1 = new Posicion();
			peq1.setEquipo(eq);
			peq1.getAtributos().put("gf", new Integer(0));
			peq1.getAtributos().put("gc", new Integer(0));
			peq1.getAtributos().put("gd", new Integer(0));
			peq1.getAtributos().put("pj", new Integer(0));
			peq1.getAtributos().put("pp", new Integer(0));
			peq1.getAtributos().put("pe", new Integer(0));
			peq1.getAtributos().put("pg", new Integer(0));
			peq1.getAtributos().put("pt", new Integer(0));
			peq1.getAtributos().put("ta", new Integer(0));
			peq1.getAtributos().put("tr", new Integer(0));
			posiciones_grupos.get(grupo).add(peq1);
		}
		return peq1;
	}

	private void verificarResultado(Partido partido, Posicion peq1, Posicion peq2) {
		peq1.getAtributos().put("gf", (Integer) peq1.getAtributos().get("gf") + partido.getGoles1());
		peq1.getAtributos().put("gc", (Integer) peq1.getAtributos().get("gc") + partido.getGoles2());
		peq1.getAtributos().put("gd", (Integer) peq1.getAtributos().get("gf") - (Integer) peq1.getAtributos().get("gc"));
		peq1.getAtributos().put("pj", (Integer) peq1.getAtributos().get("pj") + new Integer(1));

		peq2.getAtributos().put("gf", (Integer) peq2.getAtributos().get("gf") + partido.getGoles2());
		peq2.getAtributos().put("gc", (Integer) peq2.getAtributos().get("gc") + partido.getGoles1());
		peq2.getAtributos().put("gd", (Integer) peq2.getAtributos().get("gf") - (Integer) peq2.getAtributos().get("gc"));
		peq2.getAtributos().put("pj", (Integer) peq2.getAtributos().get("pj") + new Integer(1));

		if (partido.getGoles1().compareTo(partido.getGoles2()) > 0) {
			peq1.getAtributos().put("pg", (Integer) peq1.getAtributos().get("pg") + new Integer(1));
			peq1.getAtributos().put("pt", (Integer) peq1.getAtributos().get("pt") + new Integer(3));
			peq2.getAtributos().put("pp", (Integer) peq2.getAtributos().get("pp") + new Integer(1));
		} else if (partido.getGoles1().compareTo(partido.getGoles2()) == 0) {
			peq1.getAtributos().put("pe", (Integer) peq1.getAtributos().get("pe") + new Integer(1));
			peq1.getAtributos().put("pt", (Integer) peq1.getAtributos().get("pt") + new Integer(1));
			peq2.getAtributos().put("pe", (Integer) peq2.getAtributos().get("pe") + new Integer(1));
			peq2.getAtributos().put("pt", (Integer) peq2.getAtributos().get("pt") + new Integer(1));
		} else if (partido.getGoles1().compareTo(partido.getGoles2()) < 0) {
			peq1.getAtributos().put("pp", (Integer) peq1.getAtributos().get("pp") + new Integer(1));
			peq2.getAtributos().put("pg", (Integer) peq2.getAtributos().get("pg") + new Integer(1));
			peq2.getAtributos().put("pt", (Integer) peq2.getAtributos().get("pt") + new Integer(3));
		}

		peq1.getAtributos().put("ta", (Integer) peq1.getAtributos().get("ta") + partido.getTa1());
		peq1.getAtributos().put("tr", (Integer) peq1.getAtributos().get("tr") + partido.getTr1());
		peq2.getAtributos().put("ta", (Integer) peq2.getAtributos().get("ta") + partido.getTa2());
		peq2.getAtributos().put("tr", (Integer) peq2.getAtributos().get("tr") + partido.getTr2());
	}

	private Partido getPartido(Equipo eq1, Equipo eq2) {
		for (Iterator<Integer> iterator = partidos_grupos.keySet().iterator(); iterator.hasNext();) {
			Integer key = iterator.next();
			Optional<Partido> opt = partidos_grupos.get(key).stream()
					.filter(o -> (o.getEq1().equals(eq1) && o.getEq2().equals(eq2))
							|| (o.getEq1().equals(eq2) && o.getEq2().equals(eq1)))
					.findFirst();
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return null;
	}

	private void posicionPorEquipos(List<Posicion> posiciones, String keypos) {
		posiciones.sort(new Comparator<Posicion>() {
			@Override
			public int compare(Posicion o1, Posicion o2) {
				Integer i1 = (Integer) o1.getAtributos().get("pt");
				Integer i2 = (Integer) o2.getAtributos().get("pt");
				return i2.compareTo(i1);
			}
		});
		int psin = 1;
		for (Posicion pos : posiciones) {
			pos.getAtributos().put(keypos, new Integer(psin++));
		}

		HashMap<Integer, List<Posicion>> puntosmap = new HashMap<>();
		for (Posicion posicion : posiciones) {
			Integer key = (Integer) posicion.getAtributos().get("pt");
			puntosmap.putIfAbsent(key, new LinkedList<>());
			puntosmap.get(key).add(posicion);
		}
		for (Iterator<Integer> iterator2 = puntosmap.keySet().iterator(); iterator2.hasNext();) {
			Integer key = iterator2.next();
			List<Posicion> ps = puntosmap.get(key);
			if (ps.size() > 1) {
				int minpos = ps.stream().mapToInt(o -> (Integer) o.getAtributos().get(keypos)).min().getAsInt();
				ps.sort(new Comparator<Posicion>() {
					@Override
					public int compare(Posicion o1, Posicion o2) {
						Integer i1 = (Integer) o1.getAtributos().get("gd");
						Integer i2 = (Integer) o2.getAtributos().get("gd");
						return i2.compareTo(i1);
					}
				});
				for (Posicion posicion : ps) {
					posicion.getAtributos().put(keypos, new Integer(minpos++));
				}
			}
		}

		HashMap<String, List<Posicion>> puntosmap1 = new HashMap<>();
		for (Posicion posicion : posiciones) {
			String key = ((Integer) posicion.getAtributos().get("pt")).toString()
					+ ((Integer) posicion.getAtributos().get("gd")).toString();
			puntosmap1.putIfAbsent(key, new LinkedList<>());
			puntosmap1.get(key).add(posicion);
		}
		for (Iterator<String> iterator2 = puntosmap1.keySet().iterator(); iterator2.hasNext();) {
			String key = iterator2.next();
			List<Posicion> ps = puntosmap1.get(key);
			if (ps.size() > 1) {
				int minpos = ps.stream().mapToInt(o -> (Integer) o.getAtributos().get(keypos)).min().getAsInt();
				ps.sort(new Comparator<Posicion>() {
					@Override
					public int compare(Posicion o1, Posicion o2) {
						Integer i1 = (Integer) o1.getAtributos().get("gf");
						Integer i2 = (Integer) o2.getAtributos().get("gf");
						return i2.compareTo(i1);
					}
				});
				for (Posicion posicion : ps) {
					posicion.getAtributos().put(keypos, new Integer(minpos++));
				}
			}
		}

		HashMap<String, List<Posicion>> puntosmap11 = new HashMap<>();
		for (Posicion posicion : posiciones) {
			String key = ((Integer) posicion.getAtributos().get("pt")).toString()
					+ ((Integer) posicion.getAtributos().get("gd")).toString()
					+ ((Integer) posicion.getAtributos().get("gf")).toString();
			puntosmap11.putIfAbsent(key, new LinkedList<>());
			puntosmap11.get(key).add(posicion);
		}
		for (Iterator<String> iterator2 = puntosmap11.keySet().iterator(); iterator2.hasNext();) {
			String key = iterator2.next();
			List<Posicion> ps = puntosmap11.get(key);
			int minpos = ps.stream().mapToInt(o -> (Integer) o.getAtributos().get(keypos)).min().getAsInt();
			if (ps.size() == 2) {
				Posicion p1 = ps.get(0);
				Posicion p2 = ps.get(1);
				Partido partido = getPartido(p1.getEquipo(), p2.getEquipo());
				if (partido != null) {
					if (partido.getGoles1() > partido.getGoles2()) {
						if (partido.getEq1().equals(p1.getEquipo())) {
							p1.getAtributos().put(keypos, new Integer(minpos++));
							p2.getAtributos().put(keypos, new Integer(minpos++));
						} else {
							p2.getAtributos().put(keypos, new Integer(minpos++));
							p1.getAtributos().put(keypos, new Integer(minpos++));
						}
					} else if (partido.getGoles2() > partido.getGoles1()) {
						if (partido.getEq2().equals(p1.getEquipo())) {
							p1.getAtributos().put(keypos, new Integer(minpos++));
							p2.getAtributos().put(keypos, new Integer(minpos++));
						} else {
							p2.getAtributos().put(keypos, new Integer(minpos++));
							p1.getAtributos().put(keypos, new Integer(minpos++));
						}
					}
				}
			}
		}

		HashMap<String, List<Posicion>> puntosmap2 = new HashMap<>();
		for (Posicion posicion : posiciones) {
			String key = ((Integer) posicion.getAtributos().get("pt")).toString()
					+ ((Integer) posicion.getAtributos().get("gd")).toString()
					+ ((Integer) posicion.getAtributos().get("gf")).toString();
			puntosmap2.putIfAbsent(key, new LinkedList<>());
			puntosmap2.get(key).add(posicion);
		}
		for (Iterator<String> iterator2 = puntosmap2.keySet().iterator(); iterator2.hasNext();) {
			String key = iterator2.next();
			List<Posicion> ps = puntosmap2.get(key);
			if (ps.size() > 1) {
				int minpos = ps.stream().mapToInt(o -> (Integer) o.getAtributos().get(keypos)).min().getAsInt();
				ps.sort(new Comparator<Posicion>() {
					@Override
					public int compare(Posicion o1, Posicion o2) {
						Integer fp1 = (Integer) o1.getAtributos().get("ta") + 3 * (Integer) o1.getAtributos().get("tr");
						Integer fp2 = (Integer) o2.getAtributos().get("ta") + 3 * (Integer) o2.getAtributos().get("tr");
						return fp1.compareTo(fp2);
					}
				});
				for (Posicion posicion : ps) {
					posicion.getAtributos().put(keypos, new Integer(minpos++));
				}
			}
		}

		posiciones.sort((o1, o2) -> ((Integer) o1.getAtributos().get(keypos))
				.compareTo((Integer) o2.getAtributos().get(keypos)));
	}

	private void calcularRonda32() {
		List<Posicion> terceros = new ArrayList<>();
		for (int g = 0; g < 12; g++) {
			if (posiciones_grupos.containsKey(g) && !posiciones_grupos.get(g).isEmpty()) {
				Optional<Posicion> opt = posiciones_grupos.get(g).stream()
						.filter(o -> ((Integer) o.getAtributos().get("pos")).equals(3)).findFirst();
				if (opt.isPresent())
					terceros.add(opt.get());
			}
		}
		terceros.sort((a, b) -> {
			int cmp = ((Integer) b.getAtributos().get("pt")).compareTo((Integer) a.getAtributos().get("pt"));
			if (cmp != 0)
				return cmp;
			cmp = ((Integer) b.getAtributos().get("gd")).compareTo((Integer) a.getAtributos().get("gd"));
			if (cmp != 0)
				return cmp;
			cmp = ((Integer) b.getAtributos().get("gf")).compareTo((Integer) a.getAtributos().get("gf"));
			if (cmp != 0)
				return cmp;
			Integer fpA = (Integer) a.getAtributos().get("ta") + 3 * (Integer) a.getAtributos().get("tr");
			Integer fpB = (Integer) b.getAtributos().get("ta") + 3 * (Integer) b.getAtributos().get("tr");
			return fpA.compareTo(fpB);
		});
		Map<String, Equipo> asignacionTerceros = asignarMejoresTerceros(terceros);

		setR32Equipos("r32_73", getEquipoPosicion(0, 2), getEquipoPosicion(1, 2));
		setR32Equipos("r32_74", getEquipoPosicion(4, 1), asignacionTerceros.get("r32_74"));
		setR32Equipos("r32_75", getEquipoPosicion(5, 1), getEquipoPosicion(2, 2));
		setR32Equipos("r32_76", getEquipoPosicion(2, 1), getEquipoPosicion(5, 2));
		setR32Equipos("r32_77", getEquipoPosicion(8, 1), asignacionTerceros.get("r32_77"));
		setR32Equipos("r32_78", getEquipoPosicion(4, 2), getEquipoPosicion(8, 2));
		setR32Equipos("r32_79", getEquipoPosicion(0, 1), asignacionTerceros.get("r32_79"));
		setR32Equipos("r32_80", getEquipoPosicion(11, 1), asignacionTerceros.get("r32_80"));
		setR32Equipos("r32_81", getEquipoPosicion(3, 1), asignacionTerceros.get("r32_81"));
		setR32Equipos("r32_82", getEquipoPosicion(6, 1), asignacionTerceros.get("r32_82"));
		setR32Equipos("r32_83", getEquipoPosicion(10, 2), getEquipoPosicion(11, 2));
		setR32Equipos("r32_84", getEquipoPosicion(7, 1), getEquipoPosicion(9, 2));
		setR32Equipos("r32_85", getEquipoPosicion(1, 1), asignacionTerceros.get("r32_85"));
		setR32Equipos("r32_86", getEquipoPosicion(9, 1), getEquipoPosicion(7, 2));
		setR32Equipos("r32_87", getEquipoPosicion(10, 1), asignacionTerceros.get("r32_87"));
		setR32Equipos("r32_88", getEquipoPosicion(3, 2), getEquipoPosicion(6, 2));

		calcularOctavos();
	}

	private Map<String, Equipo> asignarMejoresTerceros(List<Posicion> tercerosOrdenados) {
		Map<String, Equipo> resultado = new LinkedHashMap<>();
		for (String cupo : CUPOS_TERCEROS_ORDEN) {
			resultado.put(cupo, null);
		}
		if (tercerosOrdenados.size() < 8) {
			return resultado;
		}
		Map<Character, Equipo> equipoPorGrupo = new HashMap<>();
		char[] grupos = new char[8];
		for (int i = 0; i < 8; i++) {
			Equipo eq = tercerosOrdenados.get(i).getEquipo();
			char letra = (char) ('A' + (Integer) eq.getAtributos().get("grupo"));
			grupos[i] = letra;
			equipoPorGrupo.put(letra, eq);
		}
		Arrays.sort(grupos);
		String asignacion = TABLA_TERCEROS.get(new String(grupos));
		if (asignacion == null) {
			return resultado;
		}
		for (int c = 0; c < CUPOS_TERCEROS_ORDEN.length; c++) {
			resultado.put(CUPOS_TERCEROS_ORDEN[c], equipoPorGrupo.get(asignacion.charAt(c)));
		}
		return resultado;
	}

	private void setR32Equipos(String nombre, Equipo eq1, Equipo eq2) {
		Optional<Partido> opt = ronda32.stream().filter(o -> o.getAtributos().get("nombre").equals(nombre)).findFirst();
		if (opt.isPresent())
			opt.get().setEquipos(eq1, eq2);
	}

	private Equipo getR32Ganador(String nombre) {
		Optional<Partido> opt = ronda32.stream().filter(o -> o.getAtributos().get("nombre").equals(nombre)).findFirst();
		return opt.isPresent() ? opt.get().verificarResultadoFaseFinal() : null;
	}

	private void setOctavosEquipos(String nombre, Equipo eq1, Equipo eq2) {
		Optional<Partido> opt = octavosFinal.stream().filter(o -> o.getAtributos().get("nombre").equals(nombre))
				.findFirst();
		if (opt.isPresent())
			opt.get().setEquipos(eq1, eq2);
	}

	private void calcularOctavos() {
		setOctavosEquipos("octavos_89", getR32Ganador("r32_74"), getR32Ganador("r32_77"));
		setOctavosEquipos("octavos_90", getR32Ganador("r32_73"), getR32Ganador("r32_75"));
		setOctavosEquipos("octavos_91", getR32Ganador("r32_76"), getR32Ganador("r32_78"));
		setOctavosEquipos("octavos_92", getR32Ganador("r32_79"), getR32Ganador("r32_80"));
		setOctavosEquipos("octavos_93", getR32Ganador("r32_83"), getR32Ganador("r32_84"));
		setOctavosEquipos("octavos_94", getR32Ganador("r32_81"), getR32Ganador("r32_82"));
		setOctavosEquipos("octavos_95", getR32Ganador("r32_86"), getR32Ganador("r32_88"));
		setOctavosEquipos("octavos_96", getR32Ganador("r32_85"), getR32Ganador("r32_87"));
		calcularCuartos();
	}

	private void calcularCuartos() {
		Partido p = buscar(octavosFinal, "octavos_89");
		Equipo ganadorc1 = p.verificarResultadoFaseFinal();
		Partido p2 = buscar(octavosFinal, "octavos_90");
		Equipo ganadorc2 = p2.verificarResultadoFaseFinal();
		buscar(cuartosFinal, "cuartos_97").setEquipos(ganadorc1, ganadorc2);

		p = buscar(octavosFinal, "octavos_93");
		ganadorc1 = p.verificarResultadoFaseFinal();
		p2 = buscar(octavosFinal, "octavos_94");
		ganadorc2 = p2.verificarResultadoFaseFinal();
		buscar(cuartosFinal, "cuartos_98").setEquipos(ganadorc1, ganadorc2);

		p = buscar(octavosFinal, "octavos_91");
		ganadorc1 = p.verificarResultadoFaseFinal();
		p2 = buscar(octavosFinal, "octavos_92");
		ganadorc2 = p2.verificarResultadoFaseFinal();
		buscar(cuartosFinal, "cuartos_99").setEquipos(ganadorc1, ganadorc2);

		p = buscar(octavosFinal, "octavos_95");
		ganadorc1 = p.verificarResultadoFaseFinal();
		p2 = buscar(octavosFinal, "octavos_96");
		ganadorc2 = p2.verificarResultadoFaseFinal();
		buscar(cuartosFinal, "cuartos_100").setEquipos(ganadorc1, ganadorc2);

		calcularSemifinales();
	}

	private void calcularSemifinales() {
		Partido p = buscar(cuartosFinal, "cuartos_97");
		Equipo ganadorc1 = p.verificarResultadoFaseFinal();
		Partido p2 = buscar(cuartosFinal, "cuartos_98");
		Equipo ganadorc2 = p2.verificarResultadoFaseFinal();
		buscar(semifinales, "Semifinal_101").setEquipos(ganadorc1, ganadorc2);

		p = buscar(cuartosFinal, "cuartos_99");
		ganadorc1 = p.verificarResultadoFaseFinal();
		p2 = buscar(cuartosFinal, "cuartos_100");
		ganadorc2 = p2.verificarResultadoFaseFinal();
		buscar(semifinales, "Semifinal_102").setEquipos(ganadorc1, ganadorc2);

		calcularFinales();
	}

	private void calcularFinales() {
		Partido p = buscar(semifinales, "Semifinal_101");
		Equipo ganadorc1 = p.verificarResultadoFaseFinal();
		Equipo perdedor = null;
		if (ganadorc1 != null) {
			perdedor = p.getEq1().equals(ganadorc1) ? p.getEq2() : p.getEq1();
		}

		Partido p2 = buscar(semifinales, "Semifinal_102");
		Equipo ganadorc2 = p2.verificarResultadoFaseFinal();
		Equipo perdedor2 = null;
		if (ganadorc2 != null) {
			perdedor2 = p2.getEq1().equals(ganadorc2) ? p2.getEq2() : p2.getEq1();
		}

		buscar(finales, "final").setEquipos(ganadorc1, ganadorc2);
		buscar(finales, "tercero y cuarto").setEquipos(perdedor, perdedor2);
		calcularCampeon();
	}

	private void calcularCampeon() {
		this.campeon = buscar(finales, "final").verificarResultadoFaseFinal();
	}

	private Partido buscar(List<Partido> lista, String nombre) {
		return lista.stream().filter(o -> nombre.equals(o.getAtributos().get("nombre"))).findFirst().orElse(null);
	}

	private Equipo getEquipoPosicion(Integer grupo, Integer posicion) {
		if (!posiciones_grupos.containsKey(grupo) || posiciones_grupos.get(grupo).isEmpty()) {
			return null;
		}
		Optional<Posicion> opt = posiciones_grupos.get(grupo).stream()
				.filter(o -> ((Integer) o.getAtributos().get("pos")).equals(posicion)).findFirst();
		return opt.isPresent() ? opt.get().getEquipo() : null;
	}
}
