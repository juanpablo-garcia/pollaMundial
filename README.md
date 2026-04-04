# Polla — Copa Mundial FIFA 2026

Aplicación web de pronósticos para el Mundial de Fútbol 2026. Cada usuario predice los marcadores de todos los partidos y acumula puntos según la exactitud de sus predicciones. Al final gana quien más puntos tenga.

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Frontend | JSF 2.3 + PrimeFaces 10 (Chart.js 2.9.4) |
| Backend | Java EE 8 — CDI 2.0, EJB Lite 3.2, JPA 2.2 |
| Persistencia | Hibernate 5.6 + PostgreSQL |
| Servidor | Open Liberty |
| Build | Maven |

---

## Requisitos

- Java 11
- Maven 3.x
- PostgreSQL corriendo en `localhost:5433`, base de datos `polla`
- Driver JDBC: `postgresql-42.5.0.jar` copiado en `src/main/liberty/config/`

---

## Configuración de la base de datos

Copie el archivo de ejemplo y edítelo con sus credenciales:

```bash
cp src/main/liberty/config/bootstrap.properties.example \
   src/main/liberty/config/bootstrap.properties
```

Edite `bootstrap.properties` con sus valores reales. Este archivo está en `.gitignore` y nunca se sube al repositorio.

---

## Comandos

```bash
# Compilar y generar WAR
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn clean package

# Modo desarrollo (hot reload)
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn liberty:dev
```

La app queda disponible en: `http://localhost:9080/polla`

---

## Estructura del proyecto

```
src/main/
├── java/co/com/tmsolutions/
│   ├── model/          # Entidades JPA (Partido, Equipo, Usuario, PartidosUsuario...)
│   ├── dao/            # DAOs Hibernate (@Stateless EJBs)
│   └── beans/          # Managed beans JSF (@Named + scope CDI)
├── liberty/config/
│   └── server.xml      # Configuración Open Liberty (datasource, features)
├── resources/
│   └── META-INF/persistence.xml
└── webapp/
    ├── index.xhtml         # Login / Registro
    └── secured/            # Páginas protegidas (requieren sesión)
        ├── marcadores.xhtml    # Ingreso de pronósticos
        ├── resultados.xhtml    # Ver pronósticos de todos
        └── ranking.xhtml       # Tabla de posiciones + gráfica
```

---

## Formato del Mundial 2026

- **48 equipos** divididos en **12 grupos** de 4 equipos
- Los **2 primeros** de cada grupo clasifican a la Ronda de 32 (32 equipos)
- Los **8 mejores terceros** de los 12 grupos también clasifican
- Desde la Ronda de 32 en adelante es eliminación directa:
  Ronda de 32 → Octavos → Cuartos → Semifinales → Final (+ 3.° y 4.° puesto)

### Desempate en fase de grupos

Cuando dos o más equipos terminan con los mismos puntos en el grupo, el orden se determina así:

1. Mayor diferencia de goles en todos los partidos del grupo
2. Mayor cantidad de goles marcados en el grupo
3. Resultado del partido directo entre los equipos empatados
4. **Fair play**: menor puntaje de tarjetas (amarilla = 1 pt, roja = 3 pts)

El mismo criterio se aplica para seleccionar los **mejores terceros** que clasifican a la Ronda de 32.

---

## Sistema de puntos

### Fase de grupos — marcador de cada partido

| Resultado pronosticado | Puntos |
|---|---|
| Marcador exacto (ej. 2-1 y el real fue 2-1) | **3 pts** |
| Ganador correcto o empate correcto (ej. pronostica 3-1 y gana el mismo equipo) | **1 pt** |
| Ganador incorrecto | 0 pts |

### Clasificación de equipos a cada fase

Los puntos por clasificación se otorgan **por equipo** que el usuario acertó correctamente en cada partido de fase eliminatoria. Solo se evalúa si el grupo del equipo ya terminó todos sus partidos.

| Fase | Puntos por equipo acertado |
|---|---|
| Ronda de 32 (clasificados de grupo) | **3 pts** por equipo |
| Octavos de final | **3 pts** por equipo |
| Cuartos de final | **4 pts** por equipo |
| Semifinales | **5 pts** por equipo |
| Partido por el 3.° puesto | **6 pts** por equipo |
| Final | **6 pts** por equipo |

### Bonificaciones especiales (partido final)

| Acierto | Puntos adicionales |
|---|---|
| Campeón correcto | **+12 pts** |
| Subcampeón correcto | **+6 pts** |

### Partido por el 3.° y 4.° puesto

| Acierto | Puntos adicionales |
|---|---|
| 3.° lugar correcto | **+5 pts** |
| 4.° lugar correcto | **+5 pts** |

---

## Usuario administrador

El usuario `real@real.com` es el administrador del sistema. Es el único que puede:

- Marcar partidos como **realizados** (checkbox "realizado")
- Ingresar el resultado oficial de cada partido

Una vez que un partido está marcado como realizado, el sistema calcula automáticamente los puntos de todos los usuarios al recalcular posiciones.

---

## Seguridad

- Las páginas bajo `/secured/*` están protegidas por `SecuredFilter`
- Las contraseñas se almacenan cifradas en PostgreSQL mediante `pgp_sym_encrypt`
- La sesión del usuario se maneja con `Bean_User` (`@SessionScoped`)
