# Pro-Prompt: Identidad opcional y portabilidad local-first

## Contexto

El usuario aclaro que Vocal puede tener autenticacion futura con Google, Auth0
o un proveedor similar, pero los datos sensibles no deben vivir en servidores.
La app debe poder usarse como APK local-first, con los datos bajo control del
usuario.

La portabilidad entre dispositivos debe resolverse con export/import, no con
sync cloud de datos personales. Por la sensibilidad de los registros, el export
debe ser cifrado por defecto.

## Decisiones

- La autenticacion futura es opcional.
- El perfil local sigue siendo la fuente de verdad del dominio.
- Una cuenta remota no equivale a datos en nube.
- Auth puede servir para identidad, licencia, recuperacion futura o
  integraciones no sensibles.
- Sueno, abstinencias, recaidas, uso digital, logs personales y scoring no
  dependen de un servidor.
- Export/import sera el mecanismo oficial de portabilidad entre dispositivos.
- El export debe ser cifrado por defecto.

## Entregables documentales

- Actualizar `docs/arquitectura-recomendada-autonomia.md` con identidad,
  privacidad y portabilidad.
- Actualizar `docs/nucleo-dominio-autonomia.md` para corregir alcance de auth,
  cuenta y datos locales.
- Actualizar `docs/estado-actual-mvp.md` para aclarar export/import como
  futuro necesario, aunque no inmediato.
- Actualizar `meta/meta-prompting.md` con la decision.

