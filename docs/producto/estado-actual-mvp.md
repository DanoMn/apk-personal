# Estado actual del MVP

Fecha de referencia: 2026-05-20.

## Que es la app

`Vocal / Autonomia sin limites` es una APK Android local-first para sostener la
base diaria del usuario: cuerpo, cuidado personal, interior, conducta,
alimentacion/casa, vinculos y proyecto.

No busca medir felicidad abstracta. Busca responder una pregunta mas concreta:

> Estoy sosteniendo la base que evita que vuelva el bucle?

## Direccion vigente

La direccion actual ya no es cyberpunk, terminal ni neon.

La interfaz debe sentirse como:

- un cuaderno oscuro bajo luz calida;
- una herramienta personal, no corporativa;
- una presencia tranquila;
- una estructura que ayuda sin humillar;
- una lectura rapida del dia, no una pantalla de castigo.

Referencias internas:

- `docs/frontend/frontend-design.md`
- `docs/frontend/prototipo/index.html`
- `docs/frontend/prototipo/dashboard.html`

## MVP funcional actual

La app ya tiene base Android con:

- Kotlin + Jetpack Compose.
- Persistencia local con Room.
- Datos sensibles locales por defecto.
- Drawer navigation.
- Pantallas: Dashboard, Checklist, Sobriedad, Progreso, Riesgo, Configuracion.
- Tablas v1 para capas, actividades, logs, rachas y eventos de riesgo.
- Seed inicial de capas, actividades y rachas.
- APK debug generable con `build-apk.ps1`.

## Lo mas definido

### Frontend

Ya esta bastante claro:

- paleta carbon/calida/carton/coral;
- tipografia editorial;
- tarjetas planas;
- iconografia por capas;
- iconografia UI y de senales;
- dashboard mobile como primera pantalla real;
- drawer lateral como navegacion objetivo.

### Dominio

Definido a nivel suficiente para MVP:

- la app mide base diaria;
- los estados son operativos, no diagnosticos;
- sobriedad/abstinencias son feature propia;
- el estado del dia debe ser calculado;
- Room guarda hechos y el dominio calcula senales.
- las anclas nuevas requieren meta semanal (`2..7`) y tiempo por sesion (`1..900`);
- la duracion del compromiso admite `Indefinido` (`commitmentDurationMonths = null`) y no es frecuencia mensual.
- la UX cerrada de Mis anclas esta canonizada en `docs/frontend/mis-anclas-ux-canon-v1.md`.

## Lo que sigue abierto

### Actividades por capa

Hay que cerrar:

- que actividades predeterminadas van en cada capa;
- que actividades son editables desde el inicio;
- que actividades son diarias, semanales o contextuales;
- que actividades pertenecen a cuidado personal y no a productividad.

### Tipos de actividad

Tipos candidatos vigentes:

- check simple;
- tiempo;
- frecuencia semanal;
- hora;
- conteo;
- nota corta;
- abstinencia / no hacer;
- cuidado personal.

La pregunta clave no es solo que tipos existen, sino que datos generan para
metricas.

### Metricas

Pendiente definir:

- como se calcula progreso por capa;
- como se combinan checks, minutos, frecuencias y abstinencias;
- que significa "en marcha", "estable", "bajo movimiento" o "riesgo" con datos reales;
- que umbrales quedan configurables mas adelante.

### Tono final de mensajes

El tono esta encaminado, pero debe cerrarse antes de escribir muchos textos en
Compose.

Referencia:

- `docs/producto/tono-comunicacion.md`

### Privacidad, identidad y portabilidad

La direccion futura es local-first y privacy-first:

- autenticacion opcional, no obligatoria;
- datos sensibles solo en el dispositivo;
- cuenta remota separada del perfil local;
- export/import cifrado como mecanismo de portabilidad entre dispositivos.

Auth futura puede servir para identidad, licencia, recuperacion no sensible o
integraciones no sensibles. No debe convertir el servidor en fuente de verdad
de sueno, recaidas, abstinencias, uso digital, logs personales ni scoring.

## No hacer todavia

- No implementar export/import todavia, aunque queda como feature futura
  necesaria cuando el esquema local este estable.
- No agregar servidor remoto como fuente de datos personales, login
  obligatorio, analytics remotos, nube de logs sensibles ni comunidad.
- No meter tracking automatico de celular hasta decidir permisos y privacidad.
- No complejizar algoritmo antes de tener logs confiables.
- No convertir la app en fitness, productividad pura o moralismo de sobriedad.

## Siguiente fase recomendada

1. Cerrar actividades iniciales por capa.
2. Cerrar tipos de actividad y campos necesarios para metricas.
3. Ajustar Room si faltan campos antes de seguir creciendo UI.
4. Llevar el dashboard Compose hacia el prototipo HTML.
5. Escribir mensajes finales con `tono-comunicacion.md` como referencia.
