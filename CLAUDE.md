# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Polla** is a FIFA World Cup 2026 prediction pool web application (48 teams, 12 groups, Round of 32). Users predict match scores, and the system automatically calculates points based on accuracy.

## Build & Deploy

```bash
# Build the WAR file (requires JAVA_HOME pointing to Java 11)
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn clean package
# Output: target/polla.war

# Start Open Liberty dev mode (auto-redeploy on code changes)
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn liberty:dev

# Deploy manually: copy target/polla.war to Liberty's dropins folder
```

Java 11 is required. The app runs on **Open Liberty**. The datasource is configured in `src/main/liberty/config/server.xml` (PostgreSQL on `localhost:5433/polla`) and referenced via JNDI as `jdbc/polladb`. The PostgreSQL JDBC driver must be placed in `${shared.resource.dir}/postgres/` (the `liberty-maven-plugin` copies it there automatically during the build).

No test suite exists in the project.

## Architecture

**Stack:** JSF 2.3 + PrimeFaces 10 + Hibernate 5.6 + JPA 2.2 + PostgreSQL, packaged as a WAR, running on Open Liberty (Java EE 8 / `javax.*` namespace).

**Layer structure:**
- `co.com.tmsolutions.model/` — JPA entities. All extend `BaseModel` which provides a UUID `id` and a `HashMap<String, Object> attributes` stored as PostgreSQL JSONB.
- `co.com.tmsolutions.dao/` — Data access. `GenericDAOJPA` is the base Hibernate implementation; entity-specific DAOs are `@Stateless` EJBs extending it.
- `co.com.tmsolutions.beans/` — JSF managed beans. All are CDI `@Named` beans with JSF scopes (`@SessionScoped`, `@RequestScoped`, etc.).
- `src/main/webapp/secured/` — Pages behind authentication; `SecuredFilter` redirects unauthenticated requests to the login page.

**Key flows:**
- **Login/Registration** → `Bean_Login` → `UsuarioDao` — passwords encrypted via PostgreSQL `pgp_sym_encrypt` with key `tmssecret123`.
- **Predictions entry** → `Bean_Marcadores` — matches are grouped into tabs by phase (Group Stage, R16, QF, SF, Final, 3rd place).
- **Scoring calculation** → `UsuarioPartidoDao.calcularResultados()` — compares user predictions (`PartidosUsuario`) against actual results stored on `Partido`. Points awarded: 3 for exact score, 1 for correct winner/draw, 3–6 for correct team progression per phase, 12/6 for champion/runner-up.
- **Rankings** → `Bean_Ranking` — aggregates scores via Java Streams, renders a PrimeFaces horizontal bar chart.

## Key Domain Entities

| Entity | Purpose |
|---|---|
| `Usuario` | User account; password stored as `bytea` (encrypted) |
| `Equipo` | World Cup team |
| `Partido` | A match; holds actual result scores |
| `PartidosUsuario` | User's prediction for a specific match |
| `Posicion` | Ranking snapshot (not persisted; computed at runtime) |

## Session & Security

- `Bean_User` is `@SessionScoped` and holds the currently authenticated user.
- `SecuredFilter` guards all URLs under `/secured/*`.
- The encryption key (`tmssecret123`) and DB credentials in `src/main/liberty/config/server.xml` are hardcoded — do not expose these in logs or outputs.
