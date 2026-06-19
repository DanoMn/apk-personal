# Historias de scoring — Refinamiento v3

> **Estado: borrador para marcar** — NO es contrato

Fecha: 2026-06-04
Propósito: que el dueño marque qué estado le parece correcto en cada caso.
El motor no aparece en estas páginas: solo conducta concreta, meta y hecho.

---

## Leyenda

| Columna | Significado |
| --- | --- |
| **Meta** | Frecuencia semanal · minutos por sesión que Juan se comprometió |
| **Hizo** | Lo que ocurrió esa semana (días completados · minutos) |
| **Extra** | `—` cumplido justo · `➕` superávit · `➖` déficit |

**Opciones de respuesta**

| Letra | Estado |
| --- | --- |
| **R** | Restauración — la base está caída, volver a lo mínimo |
| **A** | Atención — algo se afloja; señal, no condena |
| **EM** | En marcha — sosteniendo la base |
| **P** | Plenitud — semana plena, cumplió con holgura |

**Config de Juan (fija en todos los casos)**

| Ancla | Capa | Meta |
| --- | --- | --- |
| 🧘 Meditar | Interior | 3d · 10 min |
| 📖 Leer | Interior | 4d · 20 min |
| 🚶 Caminar | Cuerpo | 4d · 30 min |
| 📵 Higiene digital | Conducta | 3d · 15 min |

Total de marcas posibles: 14 por semana.
Sueño: siempre presente como fila (es parte de Cuerpo).
Sobriedad: no activa para Juan.

---

## GRUPO K — ¿Interior flojo bloquea Plenitud como el Cuerpo?

**Qué se prueba:** ambos casos están al ~86% (12 de 14 marcas). Cuerpo y Conducta perfectos. Solo Interior baja. Si el dueño ya sabe que Cuerpo flojo bloquea Plenitud, ¿Interior flojo también la bloquea?

---

### K1 — Interior flojo por Meditar (solo 1 sesión en vez de 3)

| Ancla | Meta | Hizo | Extra |
| --- | --- | --- | --- |
| 🧘 Meditar | 3d · 10 min | **1d** · 10 min | ➖ −2 días |
| 📖 Leer | 4d · 20 min | 4d · 20 min | — |
| 🚶 Caminar | 4d · 30 min | 4d · 30 min | — |
| 📵 Higiene digital | 3d · 15 min | 3d · 15 min | — |
| 😴 Sueño | toda la semana | durmió bien | — |

Respuesta (R / A / EM / P): **[P]**

---

### K2 — Interior flojo por Leer (solo 2 sesiones en vez de 4)

| Ancla | Meta | Hizo | Extra |
| --- | --- | --- | --- |
| 🧘 Meditar | 3d · 10 min | 3d · 10 min | — |
| 📖 Leer | 4d · 20 min | **2d** · 20 min | ➖ −2 días |
| 🚶 Caminar | 4d · 30 min | 4d · 30 min | — |
| 📵 Higiene digital | 3d · 15 min | 3d · 15 min | — |
| 😴 Sueño | toda la semana | durmió bien | — |

Respuesta (R / A / EM / P): **[P]**

---

## GRUPO S — El sueño como palanca propia

**Qué se prueba:** el sueño varía; las anclas son constantes. ¿Cuánto cambia el estado solo por el sueño?

---

### S1 — Anclas al 100%, sueño malo

| Ancla | Meta | Hizo | Extra |
| --- | --- | --- | --- |
| 🧘 Meditar | 3d · 10 min | 3d · 10 min | — |
| 📖 Leer | 4d · 20 min | 4d · 20 min | — |
| 🚶 Caminar | 4d · 30 min | 4d · 30 min | — |
| 📵 Higiene digital | 3d · 15 min | 3d · 15 min | — |
| 😴 Sueño | toda la semana | **durmió mal** | ➖ |

Respuesta (R / A / EM / P): **[EM]**

---

### S2 — Anclas al 86% parejo, sueño malo

| Ancla | Meta | Hizo | Extra |
| --- | --- | --- | --- |
| 🧘 Meditar | 3d · 10 min | 3d · 10 min | — |
| 📖 Leer | 4d · 20 min | **3d** · 20 min | ➖ −1 día |
| 🚶 Caminar | 4d · 30 min | **3d** · 30 min | ➖ −1 día |
| 📵 Higiene digital | 3d · 15 min | 3d · 15 min | — |
| 😴 Sueño | toda la semana | **durmió mal** | ➖ |

Respuesta (R / A / EM / P): **[EM]**

---

### S3 — Anclas al 100%, sueño no registrado

| Ancla | Meta | Hizo | Extra |
| --- | --- | --- | --- |
| 🧘 Meditar | 3d · 10 min | 3d · 10 min | — |
| 📖 Leer | 4d · 20 min | 4d · 20 min | — |
| 🚶 Caminar | 4d · 30 min | 4d · 30 min | — |
| 📵 Higiene digital | 3d · 15 min | 3d · 15 min | — |
| 😴 Sueño | toda la semana | **no registrado** | — |

Respuesta (R / A / EM / P): **[EM]**

---

## GRUPO J — Criticidad de capa: ¿qué capa pesa más cuando cae fuerte?

**Qué se prueba:** tres formas de llegar a ~64% o ~50%, pero con la caída en capas distintas. ¿El estado cambia según cuál capa cae?

---

### J1 — ~64%: Cuerpo a cero (Caminar no ocurrió)

| Ancla | Meta | Hizo | Extra |
| --- | --- | --- | --- |
| 🧘 Meditar | 3d · 10 min | 3d · 10 min | — |
| 📖 Leer | 4d · 20 min | 4d · 20 min | — |
| 🚶 Caminar | 4d · 30 min | **0d** | ➖ −4 días |
| 📵 Higiene digital | 3d · 15 min | **2d** · 15 min | ➖ −1 día |
| 😴 Sueño | toda la semana | durmió bien | — |

Respuesta (R / A / EM / P): **[A]**

---

### J2 — ~64%: Interior débil (Meditar 1, Leer 2)

| Ancla | Meta | Hizo | Extra |
| --- | --- | --- | --- |
| 🧘 Meditar | 3d · 10 min | **1d** · 10 min | ➖ −2 días |
| 📖 Leer | 4d · 20 min | **2d** · 20 min | ➖ −2 días |
| 🚶 Caminar | 4d · 30 min | 4d · 30 min | — |
| 📵 Higiene digital | 3d · 15 min | **2d** · 15 min | ➖ −1 día |
| 😴 Sueño | toda la semana | durmió bien | — |

Respuesta (R / A / EM / P): **[EM]**

---

### J3 — ~50%: Interior a cero (Cuerpo y Conducta cargan solos)

| Ancla | Meta | Hizo | Extra |
| --- | --- | --- | --- |
| 🧘 Meditar | 3d · 10 min | **0d** | ➖ −3 días |
| 📖 Leer | 4d · 20 min | **0d** | ➖ −4 días |
| 🚶 Caminar | 4d · 30 min | 4d · 30 min | — |
| 📵 Higiene digital | 3d · 15 min | 3d · 15 min | — |
| 😴 Sueño | toda la semana | durmió bien | — |

Respuesta (R / A / EM / P): **[A]**

---

## Hoja de respuestas

Marcá una sola letra por fila. Cuando estén todas, el patrón se puede inferir.

| ID | Qué prueba | Respuesta |
| --- | --- | --- |
| K1 | Interior flojo (Meditar 1/3) — resto perfecto | [ ] |
| K2 | Interior flojo (Leer 2/4) — resto perfecto | [ ] |
| S1 | Anclas 100% + sueño malo | [ ] |
| S2 | Anclas 86% parejo + sueño malo | [ ] |
| S3 | Anclas 100% + sueño no registrado | [ ] |
| J1 | ~64% con Cuerpo a cero (Caminar 0) | [ ] |
| J2 | ~64% con Interior débil (Meditar 1, Leer 2) | [ ] |
| J3 | ~50% con Interior a cero (Cuerpo+Conducta cargan) | [ ] |
