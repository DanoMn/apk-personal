# Preset de Soportes — v1

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Proyecto: Vocal / Autonomía sin límites  
Destino: seed de `ActivityDefinitionEntity` para actividades de soporte  
Estado: propuesta corregida para revisión/implementación  

---

## 1. Propósito

Este documento define el preset inicial de **Soportes** para la app.

Los Soportes son actividades de mantenimiento diario. No son anclas, no son tasks, no son features especializadas y no deben tener targets.

El objetivo del seed no es cubrir todos los casos posibles, sino ofrecer un catálogo base con acciones simples, genéricas y replicables. El usuario podrá crear soportes personalizados si necesita algo más específico.

---

## 2. Definición canónica

**Soporte = acción concreta de mantenimiento diario que conserva la base mínima del usuario sin exigir progreso, meta, planificación ni interpretación.**

Una actividad de soporte debe poder aparecer todos los días en la experiencia de usuario sin sonar rara, excesiva o como un pendiente puntual.

Regla conceptual:

```text
Ancla construye base.
Soporte conserva base.
Task resuelve algo puntual.
Feature especializada registra un dominio propio.
```

---

## 3. Reglas de inclusión

Una actividad entra al preset de Soportes solo si cumple estas reglas:

```text
1. Es diaria o razonablemente diaria.
2. Es concreta y directa.
3. No necesita explicación para entender qué se marca.
4. No exige progreso ni práctica cultivable.
5. No tiene target de frecuencia, tiempo ni duración.
6. No es una tarea puntual con cierre.
7. No duplica una feature propia de la app.
8. No depende demasiado del contexto personal, laboral o financiero del usuario.
9. Puede registrarse con baja fricción.
10. Puede vivir en UX inversa: el sistema asume cumplido y el usuario desmarca lo que no hizo.
```

---

## 4. Reglas de exclusión

No incluir en el seed base:

```text
- Revisiones periódicas profundas.
- Pendientes administrativos concretos.
- Obligaciones tributarias específicas.
- Limpiezas o tareas domésticas demasiado particulares.
- Microacciones innecesarias.
- Frases ambiguas o psicológicas.
- Actividades que ya pertenecen a Sueño.
- Actividades que ya pertenecen a Sobriedad.
- Actividades que pertenecen mejor a Anclas.
- Actividades que pertenecen mejor a Tasks.
```

Ejemplos explícitos a NO incluir:

```text
- Higiene personal
- Lavarse la cara
- Lavar taza o vaso usado
- Limpiar mesa después de comer
- Dejar basura en su lugar
- Guardar objetos usados
- Cargar celular
- Revisar agenda
- Revisar mensajes
- Marcar estado de ánimo
- Anotar cómo estoy
- No usar celular en cama
- Apagar pantallas en la noche
- Revisar presupuesto
- Revisar deudas
- Revisar impuestos
- Revisar obligaciones con SUNAT/SAT
- Revisar backups
- Configurar contraseñas
- Revisar garantías/contratos
- Revisar pagos pendientes
```

---

## 5. Dataset final por capas reales de la app

Capas reales:

```text
1. Interior
2. Cuerpo
3. Conducta
4. Vínculos
5. Proyecto
```

Nota: no se fuerza que todas las capas tengan presets. Proyecto queda sin preset base porque se contamina fácilmente con Anclas o Tasks.

---

## 6. Presets de Soportes

| ID | Nombre | Capa | Tipo | Unidad | Role | Contribution | Importance |
|---|---|---|---|---|---|---|---|
| `sup_escribir_diario_personal` | Escribir diario personal | Interior | Note | Text | SelfCare | Support | Low |
| `sup_banarse` | Bañarse | Cuerpo | Check | Boolean | SelfCare | Support | Medium |
| `sup_cepillarse_dientes` | Cepillarse los dientes | Cuerpo | Check | Boolean | SelfCare | Support | Medium |
| `sup_usar_ropa_limpia` | Usar ropa limpia | Cuerpo | Check | Boolean | SelfCare | Support | Low |
| `sup_tomar_agua` | Tomar agua | Cuerpo | Check | Boolean | SelfCare | Support | Medium |
| `sup_comer` | Comer | Cuerpo | Check | Boolean | SelfCare | Support | Medium |
| `sup_cocinar_en_casa` | Cocinar en casa | Conducta | Check | Boolean | DomesticOrder | Support | Medium |
| `sup_lavar_platos` | Lavar los platos | Conducta | Check | Boolean | DomesticOrder | Support | Medium |
| `sup_tender_cama` | Tender la cama | Conducta | Check | Boolean | DomesticOrder | Support | Low |
| `sup_registrar_gastos` | Registrar gastos | Conducta | Check | Boolean | AdministrativeOrder | Support | Medium |
| `sup_revisar_correo` | Revisar correo | Conducta | Check | Boolean | AdministrativeOrder | Support | Low |
| `sup_responder_mensajes` | Responder mensajes | Vínculos | Check | Boolean | RelationalHabit | Support | Low |

---

## 7. Agrupación visual sugerida

```text
Interior
- Escribir diario personal

Cuerpo
- Bañarse
- Cepillarse los dientes
- Usar ropa limpia
- Tomar agua
- Comer

Conducta
- Cocinar en casa
- Lavar los platos
- Tender la cama
- Registrar gastos
- Revisar correo

Vínculos
- Responder mensajes

Proyecto
- Sin preset base por ahora
```

---

## 8. Notas de implementación

- Estos presets deben crearse como definiciones de catálogo.
- No deben guardar configuración de usuario.
- No deben tener `weeklyFrequencyTarget`.
- No deben tener `sessionTargetMinutes`.
- No deben tener `commitmentDurationMonths`.
- No deben tener targets heredados.
- La capa es obligatoria.
- `contributionRole = Support` para todos.
- `ActivitySurface = Support` o equivalente actual.
- UX inversa: el soporte aparece como cumplido por defecto; el usuario desmarca lo que no hizo.
- El usuario puede crear soportes personalizados fuera de este preset.

---

## 9. Criterio para futuras ampliaciones

No ampliar el seed con actividades “útiles” solo porque podrían ayudar.

Antes de agregar una actividad, validar:

```text
¿Es diaria?
¿Es concreta?
¿Es genérica?
¿No es task?
¿No es ancla?
¿No pertenece a una feature especializada?
¿No suena rara en una checklist diaria?
```

Si alguna respuesta falla, no entra al preset base.
