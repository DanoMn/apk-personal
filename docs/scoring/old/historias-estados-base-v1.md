> **Estado: borrador para marcar** — NO es contrato

# Historias de estado base (déficit / cumplimiento parcial)

Esta tanda decide dónde cortan los 4 estados base: **R Restauración / A Atención / EM En marcha / P Plenitud**.

Las cajitas están **vacías**. El dueño las marca con R, A, EM o P.

---

## Leyenda

| Columna | Significado |
|---------|-------------|
| Meta | frecuencia semanal · tiempo por sesión |
| Hizo | días realizados · minutos reales |
| Extra | `—` cumplió justo · `➖` déficit · `➕` superávit |
| `[ ]` | rellenás con R, A, EM o P |

**Config de Juan (fija en todos los casos)**

| Ancla | Capa | Meta |
|-------|------|------|
| 🧘 Meditar | Interior | 3d · 10min |
| 📖 Leer | Interior | 4d · 20min |
| 🚶 Caminar | Cuerpo | 4d · 30min |
| 📵 Higiene digital | Conducta | 3d · 15min |
| 😴 Sueño | Cuerpo (30%) | — |

Sin soportes activos. Sin sobriedad opt-in. Sin Tasks con capa asignada.

---

## Espectro de cumplimiento (ceteris paribus — solo varía el nivel global)

### C-01 — Abandono total (0%)

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 0d · 0min | ➖ |
| 📖 Leer | 4d · 20min | 0d · 0min | ➖ |
| 🚶 Caminar | 4d · 30min | 0d · 0min | ➖ |
| 📵 Higiene digital | 3d · 15min | 0d · 0min | ➖ |
| 😴 Sueño | todas | durmió bien | — |

Juan no hizo ninguna ancla en la semana. Sueño registrado correctamente.

**Respuesta (R / A / EM / P): [R]**

---

### C-02 — Casi nada (~15%)

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 0d · 0min | ➖ |
| 📖 Leer | 4d · 20min | 1d · 20min | ➖ |
| 🚶 Caminar | 4d · 30min | 0d · 0min | ➖ |
| 📵 Higiene digital | 3d · 15min | 0d · 0min | ➖ |
| 😴 Sueño | todas | durmió bien | — |

Solo salvó 1 sesión de lectura en toda la semana.

**Respuesta (R / A / EM / P): [R]**

---

### C-03 — Alrededor de un tercio (~33%)

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 1d · 10min | ➖ |
| 📖 Leer | 4d · 20min | 1d · 20min | ➖ |
| 🚶 Caminar | 4d · 30min | 1d · 30min | ➖ |
| 📵 Higiene digital | 3d · 15min | 1d · 15min | ➖ |
| 😴 Sueño | todas | durmió bien | — |

Una sesión por ancla: cumple el "sabor" de cada práctica pero a un tercio de la frecuencia.

**Respuesta (R / A / EM / P): [A]**

---

### C-04 — Mitad exacta (50%)

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 1d · 10min | ➖ (≈33% días) |
| 📖 Leer | 4d · 20min | 2d · 20min | ➖ (50% días) |
| 🚶 Caminar | 4d · 30min | 2d · 30min | ➖ (50% días) |
| 📵 Higiene digital | 3d · 15min | 1d · 15min | ➖ (≈33% días) |
| 😴 Sueño | todas | durmió bien | — |

Cada ancla a ~50% de su frecuencia objetivo (días justos, minutos exactos).

**Respuesta (R / A / EM / P): [A]**

---

### C-05 — Mayoría (~75%)

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 2d · 10min | ➖ |
| 📖 Leer | 4d · 20min | 3d · 20min | ➖ |
| 🚶 Caminar | 4d · 30min | 3d · 30min | ➖ |
| 📵 Higiene digital | 3d · 15min | 2d · 15min | ➖ |
| 😴 Sueño | todas | durmió bien | — |

Juan hizo el 75% de sus frecuencias, con tiempo exacto cada vez.

**Respuesta (R / A / EM / P): [EM]**

---

### C-06 — Casi todo (falló 1 marca en toda la semana)

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 3d · 10min | — |
| 📖 Leer | 4d · 20min | 4d · 20min | — |
| 🚶 Caminar | 4d · 30min | **3d** · 30min | ➖ (−1 día) |
| 📵 Higiene digital | 3d · 15min | 3d · 15min | — |
| 😴 Sueño | todas | durmió bien | — |

Solo le faltó 1 día de Caminar. El resto, perfecto.

**Respuesta (R / A / EM / P): [P]**

---

### C-07 — 100% justo (sin superávit, sin déficit)

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 3d · 10min | — |
| 📖 Leer | 4d · 20min | 4d · 20min | — |
| 🚶 Caminar | 4d · 30min | 4d · 30min | — |
| 📵 Higiene digital | 3d · 15min | 3d · 15min | — |
| 😴 Sueño | todas | durmió bien | — |

Cumple exacto cada ancla: ni un día de más, ni un minuto de más.

**Respuesta (R / A / EM / P): [P]**

---

## Tensiones reales

### T-01 — Una capa entera a 0 con el resto perfecto

> Variable: peso de la peor capa en la fórmula `base = 0.75·avg + 0.25·worst`.

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 3d · 10min | — |
| 📖 Leer | 4d · 20min | 4d · 20min | — |
| 🚶 Caminar | 4d · 30min | **0d** · 0min | ➖ (capa Cuerpo = 0) |
| 📵 Higiene digital | 3d · 15min | 3d · 15min | — |
| 😴 Sueño | todas | durmió bien | — |

Interior: perfecto. Conducta: perfecto. Cuerpo: cayó a cero (no caminó ningún día). Sueño registrado.

¿El estado se hunde por la capa peor aunque el promedio sea alto?

**Respuesta (R / A / EM / P): [EM]**

---

### T-02 — No registra sueño con el resto perfecto (sleep cap)

> Variable: ¿el sleep cap topa el estado aunque la base sea alta?

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 3d · 10min | — |
| 📖 Leer | 4d · 20min | 4d · 20min | — |
| 🚶 Caminar | 4d · 30min | 4d · 30min | — |
| 📵 Higiene digital | 3d · 15min | 3d · 15min | — |
| 😴 Sueño | todas | **no registrado** | ➖ |

Todas las anclas al 100%. Sueño: sin ningún registro en la semana.

¿Llegar a Plenitud es posible sin sueño registrado?

**Respuesta (R / A / EM / P): [EM]**

---

### T-03 — Intermitente (2 anclas perfectas, 2 en cero)

> Variable: ¿qué pesa más, el promedio o la dispersión extrema?

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | 3d · 10min | — |
| 📖 Leer | 4d · 20min | **0d** · 0min | ➖ |
| 🚶 Caminar | 4d · 30min | 4d · 30min | — |
| 📵 Higiene digital | 3d · 15min | **0d** · 0min | ➖ |
| 😴 Sueño | todas | durmió bien | — |

Meditar y Caminar: 100%. Leer e Higiene digital: 0%. 50% de las anclas abandonadas.

**Respuesta (R / A / EM / P): [A]**

---

### T-04 — Se mató en una ancla pero falló en las otras

> Variable: ¿el superávit localizado compensa el déficit generalizado?

| Ancla | Meta | Hizo | Extra |
|-------|------|------|-------|
| 🧘 Meditar | 3d · 10min | **7d** · 10min | ➕ +4 días |
| 📖 Leer | 4d · 20min | **1d** · 20min | ➖ |
| 🚶 Caminar | 4d · 30min | **1d** · 30min | ➖ |
| 📵 Higiene digital | 3d · 15min | **1d** · 15min | ➖ |
| 😴 Sueño | todas | durmió bien | — |

Meditó todos los días (máximo superávit posible en esa ancla). El resto al 25%.

¿El esfuerzo concentrado levanta el estado o la base colectiva lo arrastra?

**Respuesta (R / A / EM / P): [R]**

---

## Hoja de respuestas

Rellenala de un vistazo cuando termines de marcar.

| ID | Descripción corta | Estado |
|----|-------------------|--------|
| C-01 | Abandono total (0%) | [ ] |
| C-02 | Casi nada (~15%) | [ ] |
| C-03 | Un tercio (~33%) | [ ] |
| C-04 | Mitad exacta (50%) | [ ] |
| C-05 | Mayoría (~75%) | [ ] |
| C-06 | Casi todo (−1 marca) | [ ] |
| C-07 | 100% justo | [ ] |
| T-01 | Capa entera a 0, resto perfecto | [ ] |
| T-02 | Sin sueño registrado, resto perfecto | [ ] |
| T-03 | 2 anclas perfectas / 2 en cero | [ ] |
| T-04 | Superávit máximo en 1 ancla, resto al 25% | [ ] |
