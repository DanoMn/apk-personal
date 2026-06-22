# Sistema de diseno frontend

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Este documento es la guia visual vigente de `Autonomía sin límites`.
Reemplaza cualquier direccion anterior tipo cyberpunk, terminal, neon o
corporativa fria.

La fuente visual viva esta en:

- `docs/frontend/prototipo/index.html`
- `docs/frontend/prototipo/dashboard.html`

## Filosofia principal

Autonomía sin límites es una herramienta personal de salud mental, autonomia y estructura
diaria. La interfaz debe transmitir:

- tranquilidad;
- control humano;
- calidez;
- dignidad;
- presencia;
- orden sin castigo.

La app debe sentirse como un cuaderno oscuro bajo luz calida, no como un panel
de control militar ni una terminal futurista.

Referencias internas:

- Zen Browser en modo oscuro por su calma.
- Claude por su calidez editorial.
- Papel reciclado, tinta suave y superficies tactiles.

## Paleta base

No usar negros puros ni colores neon.

- `--bg-base`: `#1F1E1D`. Carbon calido, casi sepia.
- `--bg-surface`: `#2A2927`. Superficie principal.
- `--color-cardboard`: `#E0D8C3`. Carton/beige para titulos y acciones primarias.
- `--color-coral`: `#E57B65`. Coral mate para accion, checks y alertas suaves.
- `--text-main`: `#EAE5D9`. Hueso claro.
- `--text-muted`: gris calido secundario.

Regla:

- Separar con contraste sutil de superficies.
- Evitar bordes duros como recurso principal.
- Evitar sombras pesadas.

## Tipografia

### Titulos

- Familia: `Lora`, `Georgia`, `ui-serif`, `serif`.
- Peso: `500`.
- Uso: H1, H2, titulos de tarjetas, frases ancla.
- Deben sentirse editoriales, humanos, de libro.

### Cuerpo y controles

- Familia: `Outfit`, `Inter`, `sans-serif`.
- Pesos: `300`, `400`, `500`, `600`.
- Uso: listas, botones, datos, etiquetas.

No usar monospace para UI general.

## Componentes

### Tarjetas

- Planas.
- Fondo `--bg-surface`.
- Radio aproximado: `14px`.
- Sin borde visible salvo que haya una razon funcional.
- Sin sombras profundas.

### Botones

- Primario: carton/beige con texto oscuro.
- Secundario: superficie oscura.
- Riesgo: coral/terracota oscuro, sin gritar.
- Radio aproximado: `8px`.

### Checklist

- No debe ocupar todo el dashboard.
- En dashboard aparece como acceso o lista compacta.
- En pantalla propia puede agruparse por capas.
- Los completados bajan debajo de un separador `Completados`.
- El check activo usa coral.

### Bottom sheets y configuracion rapida

- Deben contraerse cuando hay poco contenido y crecer solo hasta el maximo
  disponible cuando el contenido lo necesita.
- En Compose, usar un patron de alto maximo adaptativo (`heightIn(max = ...)`)
  en lugar de forzar `fillMaxHeight(...)`.
- Respetar `navigationBarsPadding()` en acciones inferiores.
- Evitar ripple u oscurecimiento en contenedores usados solo para consumir
  toques.

### Mis anclas

El patron canonico de Mis anclas esta en `docs/frontend/mis-anclas-ux-canon-v1.md`.

- El editor ordena: identidad/nombre, tiempo objetivo, meta semanal, duracion
  del compromiso y acciones.
- El selector de tiempo debe estar arriba para reducir friccion entre scroll de
  pantalla y wheel.
- En actividad personalizada, la capa queda fija encima de los botones de
  accion.
- La recomendacion de `Indefinido` vive dentro del dialogo de configurar
  duracion.

## Iconografia

La iconografia ya no es generica. Es parte del lenguaje del producto.

### Capas

Las capas son sellos primarios. Tienen mas peso visual que los iconos UI.

Capas vigentes:

- Interior: rombo con nucleo solido.
- Cuerpo: ondas.
- Conducta: infinito.
- Casa/comida: tienda/refugio.
- Vinculos: hexagono con nucleo solido.
- Proyecto: triangulo solido.

### UI esencial

Los iconos utilitarios deben ser finos y discretos:

- trazo `1.5px`;
- remates redondeados;
- viewBox `24x24`;
- no competir con las capas.

Cuando Lucide se siente demasiado generico, se define simbolo propio en el
prototipo.

Simbolos propios vigentes:

- `icon-vocal-checklist`
- `icon-vocal-logbook`
- `icon-vocal-layer-stack`
- `icon-vocal-intimate-boundary`
- `icon-vocal-sleep`
- `icon-vocal-no-phone-bed`

### Truco de peso visual

No engrosar lineas para hacer que un icono pese mas.

Si una senal queda debil, darle masa desde adentro:

- relleno solido;
- nucleo geometrico;
- contraste entre figura solida y trazo fino.

No agregar subrayados, bases o adornos externos para explicar el simbolo.

Ejemplo:

- Sueño = luna solida + estrella geometrica.
- Proyecto = triangulo solido.
- Interior = rombo con nucleo solido.

## Dashboard mobile

El dashboard debe responder en 5 segundos:

- Como esta la base hoy?
- Que capas estan activas?
- Que falta ahora?
- Hay senales importantes?
- Que accion minima puedo hacer?

Debe mostrar:

- frase ancla;
- estado calculado;
- progreso del dia;
- capas;
- senales;
- abstinencias activas;
- checklist compacta;
- resumen semanal.

No debe mostrar:

- lista larga de tareas como primera experiencia;
- texto motivacional generico;
- indicadores que parezcan diagnostico;
- bottom nav como estructura principal.

La navegacion objetivo es drawer lateral.

### Card de estado: normal vs arranque

El area de estado del dashboard tiene DOS cards posibles, y se elige uno segun la cuenta:

- **StatusCard** (normal): el estado calculado real (orbe con el score, banda, headline/body). Es el
  card de siempre; no se toca.
- **StartupStatusCard** (arranque de cuenta): cuando la cuenta es nueva (sus anclas estan en gracia,
  los primeros 7 dias), en vez del blackout "Sin datos" se muestra una **barra de carga**. Misma
  FORMA que StatusCard (texto a la izquierda + orbe a la derecha) para coherencia visual, pero es un
  componente hermano e independiente. El orbe reusa el ScoreOrbit: el numero central sube `0 → score
  real` (animado) y el arco se llena `d/7` (animado). Copy compasivo: pill "Arranque", headline "La
  base esta cargando", y "Faltan N dias para tu puntaje real" (singular/plural; "Manana llega tu
  puntaje real" en el dia 7). Sin tono punitivo ni clinico.

**Color calido propio del arranque:** NO se inventa paleta. Se deriva de los tokens existentes
`colorCoral` y `colorCardboard` (cartn/beige + coral mate) via la utilidad `mix`, dando un tono
calido que lo distingue del estado normal pero respeta la base oscura organica. Ningun token nuevo.

La eleccion `startup != null` la resuelve el dominio (DashboardProjection); el Composable solo
presenta y anima — cero logica de negocio en la card (state hoisting).

## Tono visual

La UI debe sentirse:

- tranquila;
- madura;
- tactil;
- cercana;
- seria sin ser fria.

Debe evitar:

- cyberpunk;
- terminal;
- gamificacion ruidosa;
- colores electricos;
- dashboards corporativos;
- aesthetic de productividad agresiva.

## Implementacion Compose

Al portar el prototipo a Compose:

- conservar paleta antes que componentes Material por defecto;
- usar drawer lateral;
- evitar bottom nav como patron principal;
- separar hechos de inferencias;
- no meter reglas de estado dentro de composables;
- replicar iconografia como vectores propios cuando Lucide no alcance.
