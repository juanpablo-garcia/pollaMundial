package co.com.tmsolutions.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;

import co.com.tmsolutions.model.Equipo;
import co.com.tmsolutions.model.Partido;
import co.com.tmsolutions.model.PartidosUsuario;
import co.com.tmsolutions.model.Usuario;

@Stateless
public class UsuarioPartidoDao extends GenericDAOJPA<PartidosUsuario, String> {

	public String savePartidos(String id, List<Partido> partidos, Usuario usuario) {
		PartidosUsuario partidoUsuario = null;
		if (id != null) {
			partidoUsuario = get(id);
		}
		if (partidoUsuario == null) {
			partidoUsuario = new PartidosUsuario(usuario);
		}
		partidoUsuario.getPartidos().clear();
		partidoUsuario.getPartidos().addAll(partidos);
		partidoUsuario.setFechaModificacion(new Date());
		partidoUsuario = merge(partidoUsuario);
		return partidoUsuario.getId();
	}

	public void calcularResultados(Usuario usuarioreal) {

		List<PartidosUsuario> partidos = findAll();
		PartidosUsuario real = partidos.stream().filter(o -> o.getUsuario().equals(usuarioreal)).findFirst()
				.orElse(null);
		if (real != null) {
			// Partidos ronda32
			List<Partido> ronda32real = real.getPartidos().stream().filter(o -> o.getAtributos().get("nombre") != null
					&& ((String) o.getAtributos().get("nombre")).contains("r32_")).collect(Collectors.toList());

			// Partidos octavos
			List<Partido> octavosreal = real.getPartidos().stream()
					.filter(o -> o.getAtributos().get("nombre") != null
							&& ((String) o.getAtributos().get("nombre")).contains("octavos"))
					.collect(Collectors.toList());

			// Partidos cuartos
			List<Partido> cuartosreal = real.getPartidos().stream()
					.filter(o -> o.getAtributos().get("nombre") != null
							&& ((String) o.getAtributos().get("nombre")).contains("cuartos"))
					.collect(Collectors.toList());

			// Partidos semifinal
			List<Partido> semisreal = real.getPartidos().stream()
					.filter(o -> o.getAtributos().get("nombre") != null
							&& ((String) o.getAtributos().get("nombre")).contains("Semifinal"))
					.collect(Collectors.toList());

			// tercerocuarto real
			Partido tercerocuartoreal = real.getPartidos().stream().filter(o -> o.getAtributos().get("nombre") != null
					&& ((String) o.getAtributos().get("nombre")).equals("tercero y cuarto")).findFirst().get();

			// Cuartos 1
			Partido freal = real.getPartidos().stream().filter(
					o -> o.getAtributos().get("nombre") != null && o.getAtributos().get("nombre").equals("final"))
					.findFirst().get();
			Equipo campeonreal = freal.verificarResultadoFaseFinal();

			Equipo subcampeonreal = freal.verificarResultadoFaseFinalSubCampeon();

			Equipo terceroreal = tercerocuartoreal.verificarResultadoFaseFinal();
			Equipo cuartoreal = tercerocuartoreal.verificarResultadoFaseFinalSubCampeon();

			HashMap<Integer, Boolean> terminoGrupo = new HashMap<>();

			// Partidos grupos
			List<Partido> pgrupos = real.getPartidos().stream().filter(o -> o.getFase().equals("partidos_grupos"))
					.collect(Collectors.toList());

			for (int i = 0; i < 12; i++) {
				final int v = i;
				boolean terminogA = !pgrupos.stream()
						.filter(o -> o.getEq1() != null
								&& ((Integer) o.getEq1().getAtributos().get("grupo")).equals(new Integer(v)))
						.collect(Collectors.toList()).stream().filter(o -> !o.getRealizado()).findFirst().isPresent();
				terminoGrupo.put(new Integer(i), terminogA);
			}

			for (PartidosUsuario pusuaruio : partidos) {

				List<Partido> partidos_usuario = pusuaruio.getPartidos();

				for (Partido p : partidos_usuario) {
					Partido partidocompara = real.getPartidos().stream()
							.filter(o -> o.getIdpartido().equals(p.getIdpartido()) && o.getFase().equals(p.getFase()))
							.findFirst().orElse(null);
					if (partidocompara != null && partidocompara.getRealizado()) {
						p.getAtributos().put("puntaje", new Integer(0));
						// Puntos por marcador segun fase
						int ptsExacto = getPuntajeMarcadorExacto(p.getFase(), p.getAtributos().get("nombre"));
						int ptsResultado = getPuntajeMarcadorResultado(p.getFase(), p.getAtributos().get("nombre"));
						// Le pega al marcador exacto
						if (p.getGoles1().equals(partidocompara.getGoles1())
								&& p.getGoles2().equals(partidocompara.getGoles2())) {
							p.getAtributos().put("puntaje", new Integer(ptsExacto));
						} else {
							// Empate
							if (partidocompara.getGoles1().compareTo(partidocompara.getGoles2()) == 0) {
								if (p.getGoles1().compareTo(p.getGoles2()) == 0) {
									p.getAtributos().put("puntaje", new Integer(ptsResultado));
								}
							} else
							// Gana equipo 1
							if (partidocompara.getGoles1().compareTo(partidocompara.getGoles2()) > 0) {
								if (p.getGoles1().compareTo(p.getGoles2()) > 0) {
									p.getAtributos().put("puntaje", new Integer(ptsResultado));
								}
							} else
							// Gana equipo 2
							if (partidocompara.getGoles1().compareTo(partidocompara.getGoles2()) < 0) {
								if (p.getGoles1().compareTo(p.getGoles2()) < 0) {
									p.getAtributos().put("puntaje", new Integer(ptsResultado));
								}
							}
						}
					}
				}

				// Verificar equipos que pasan de ronda32
				List<Partido> ronda32usuario = partidos_usuario.stream()
						.filter(o -> o.getAtributos().get("nombre") != null
								&& ((String) o.getAtributos().get("nombre")).contains("r32_"))
						.collect(Collectors.toList());

				// Verificar equpos que pasan de fase
				List<Partido> octavosusuario = partidos_usuario.stream()
						.filter(o -> o.getAtributos().get("nombre") != null
								&& ((String) o.getAtributos().get("nombre")).contains("octavos"))
						.collect(Collectors.toList());

				// Verificar equpos que pasan de fase
				List<Partido> cuartosusuario = partidos_usuario.stream()
						.filter(o -> o.getAtributos().get("nombre") != null
								&& ((String) o.getAtributos().get("nombre")).contains("cuartos"))
						.collect(Collectors.toList());

				// Partidos semifinal
				List<Partido> semisusuario = partidos_usuario.stream()
						.filter(o -> o.getAtributos().get("nombre") != null
								&& ((String) o.getAtributos().get("nombre")).contains("Semifinal"))
						.collect(Collectors.toList());

				// Partidos final
				Partido finalusuario = partidos_usuario.stream().filter(o -> o.getAtributos().get("nombre") != null
						&& ((String) o.getAtributos().get("nombre")).equals("final")).findFirst().get();

				// tercerocuarto real
				Partido tercerocuartousuario = partidos_usuario.stream()
						.filter(o -> o.getAtributos().get("nombre") != null
								&& ((String) o.getAtributos().get("nombre")).equals("tercero y cuarto"))
						.findFirst().get();

				for (Partido p : ronda32usuario) {
					p.getAtributos().put("puntajeeq1", new Integer(0));
					p.getAtributos().put("puntajeeq2", new Integer(0));
					for (Partido preal : ronda32real) {
						verificarPasoEquipo(preal, p, new Integer(3), terminoGrupo);
					}
				}

				for (Partido p : octavosusuario) {

					p.getAtributos().put("puntajeeq1", new Integer(0));
					p.getAtributos().put("puntajeeq2", new Integer(0));
					for (Partido preal : octavosreal) {
						verificarPasoEquipo(preal, p, new Integer(5), terminoGrupo);
					}
				}

				for (Partido p : cuartosusuario) {
					p.getAtributos().put("puntajeeq1", new Integer(0));
					p.getAtributos().put("puntajeeq2", new Integer(0));
					for (Partido preal : cuartosreal) {
						verificarPasoEquipo(preal, p, new Integer(7), terminoGrupo);
					}
				}

				for (Partido p : semisusuario) {
					p.getAtributos().put("puntajeeq1", new Integer(0));
					p.getAtributos().put("puntajeeq2", new Integer(0));
					for (Partido preal : semisreal) {
						verificarPasoEquipo(preal, p, new Integer(9), terminoGrupo);
					}
				}

//				tercero y cuarto
				if (tercerocuartousuario != null) {
					tercerocuartousuario.getAtributos().put("puntajeeq1", new Integer(0));
					tercerocuartousuario.getAtributos().put("puntajeeq2", new Integer(0));
					verificarPasoEquipo(tercerocuartoreal, tercerocuartousuario, new Integer(9), null);

					Equipo tercerousuario = tercerocuartousuario.verificarResultadoFaseFinal();
					Equipo cuartousuario = tercerocuartousuario.verificarResultadoFaseFinalSubCampeon();

					if (tercerousuario != null && terceroreal != null && terceroreal.getAtributos().get("nombre")
							.equals(tercerousuario.getAtributos().get("nombre"))) {
						tercerocuartousuario.getAtributos().put("puntajeeq1",
								((Integer) tercerocuartousuario.getAtributos().get("puntajeeq1")) + new Integer(6));
					}

					if (cuartousuario != null && cuartoreal != null && cuartoreal.getAtributos().get("nombre")
							.equals(cuartousuario.getAtributos().get("nombre"))) {
						tercerocuartousuario.getAtributos().put("puntajeeq2",
								((Integer) tercerocuartousuario.getAtributos().get("puntajeeq2")) + new Integer(4));
					}

				}

				if (finalusuario != null) {
					Equipo campeonusuario = finalusuario.verificarResultadoFaseFinal();
					Equipo subcampeonusuario = finalusuario.verificarResultadoFaseFinalSubCampeon();

					finalusuario.getAtributos().put("puntajecampeon", new Integer(0));
					finalusuario.getAtributos().put("puntajeeq1", new Integer(0));
					finalusuario.getAtributos().put("puntajeeq2", new Integer(0));
					verificarPasoEquipo(freal, finalusuario, new Integer(10), null);
					if (campeonusuario != null && campeonreal != null && campeonreal.getAtributos().get("nombre")
							.equals(campeonusuario.getAtributos().get("nombre"))) {
						finalusuario.getAtributos().put("puntajeeq1",
								((Integer) finalusuario.getAtributos().get("puntajeeq1")) + new Integer(15));
					}
					if (subcampeonusuario != null && subcampeonreal != null && subcampeonreal.getAtributos()
							.get("nombre").equals(subcampeonusuario.getAtributos().get("nombre"))) {
						finalusuario.getAtributos().put("puntajeeq2",
								((Integer) finalusuario.getAtributos().get("puntajeeq2")) + new Integer(8));
					}
				}
				pusuaruio.setFechaModificacion(new Date());
				save(pusuaruio);
			}

		}

	}

	private int getPuntajeMarcadorExacto(String fase, Object nombre) {
		switch (fase) {
		case "partidos_grupos":
			return 3;
		case "ronda32":
			return 4;
		case "octavosFinal":
			return 5;
		case "cuartosFinal":
			return 6;
		case "semifinales":
			return 7;
		case "finales":
			if (nombre != null && "tercero y cuarto".equals(nombre))
				return 7;
			return 8;
		default:
			return 3;
		}
	}

	private int getPuntajeMarcadorResultado(String fase, Object nombre) {
		switch (fase) {
		case "partidos_grupos":
			return 1;
		case "ronda32":
			return 1;
		case "octavosFinal":
			return 2;
		case "cuartosFinal":
			return 2;
		case "semifinales":
			return 3;
		case "finales":
			if (nombre != null && "tercero y cuarto".equals(nombre))
				return 3;
			return 3;
		default:
			return 1;
		}
	}

	private void verificarPasoEquipo(Partido preal, Partido pusuario, Integer puntaje,
			HashMap<Integer, Boolean> terminoGrupo) {
		Equipo eq1 = pusuario.getEq1();
		Equipo eq2 = pusuario.getEq2();

		if (terminoGrupo != null) {
			boolean terminog1 = false;
			boolean terminog2 = false;
			if (eq1 != null) {
				Integer grupo = (Integer) eq1.getAtributos().get("grupo");
				terminog1 = terminoGrupo.get(grupo);
//				if (!terminaron) {
//					eq1 = null;
//					eq2 = null;
//				}
			}
			if (eq2 != null) {
				Integer grupo = (Integer) eq2.getAtributos().get("grupo");
				terminog2 = terminoGrupo.get(grupo);
//				if (!terminaron) {
//					eq1 = null;
//					eq2 = null;
//				}
			}
			if (!terminog1) {
				eq1 = null;
			}
			if (!terminog2) {
				eq2 = null;
			}
		}

		if (preal.getEq1() != null) {
			if (eq1 != null && preal.getEq1().getAtributos().get("nombre").equals(eq1.getAtributos().get("nombre"))) {
				pusuario.getAtributos().put("puntajeeq1", puntaje);
			}
			if (eq2 != null && preal.getEq1().getAtributos().get("nombre").equals(eq2.getAtributos().get("nombre"))) {
				pusuario.getAtributos().put("puntajeeq2", puntaje);
			}
		}
		if (preal.getEq2() != null) {
			if (eq1 != null && preal.getEq2().getAtributos().get("nombre").equals(eq1.getAtributos().get("nombre"))) {
				pusuario.getAtributos().put("puntajeeq1", puntaje);
			}
			if (eq2 != null && preal.getEq2().getAtributos().get("nombre").equals(eq2.getAtributos().get("nombre"))) {
				pusuario.getAtributos().put("puntajeeq2", puntaje);
			}
		}

	}

	public List<Partido> getPartidosRanking(Usuario u) {
		List<Partido> partidosReales = new ArrayList<>();
		List<PartidosUsuario> todos = findAll();
		PartidosUsuario real = todos.stream().filter(o -> o.getUsuario().equals(u)).findFirst().orElse(null);
		if (real != null) {
			partidosReales = real.getPartidos();

			// poner los partidos de cada usuario
			for (Partido preal : partidosReales) {
				if (preal.getEq1() != null) {
					preal.getEq1().toString();
				}
				if (preal.getEq2() != null) {
					preal.getEq2().toString();
				}
				preal.getAtributos().putIfAbsent("partidos_usuario", new ArrayList<Partido>());
				for (PartidosUsuario pusuario : todos) {
					// Coger el partido que corresponde
					Partido pcorresponde = pusuario.getPartidos().stream().filter(
							o -> o.getIdpartido().equals(preal.getIdpartido()) && o.getFase().equals(preal.getFase()))
							.findFirst().orElse(null);
					if (pcorresponde != null) {
						ArrayList<Partido> ps = (ArrayList<Partido>) preal.getAtributos().get("partidos_usuario");
						pcorresponde.getAtributos().put("nombre_usuario",
								pusuario.getUsuario().getAtributos().get("nombre"));
						ps.add(pcorresponde);
					}
				}

			}
		}
		return partidosReales;
	}

}
