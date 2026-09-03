# ADR-0002 · Sin backend y sin cuentas de usuario

- **Fecha:** 2026-09-03
- **Estado:** aceptada

## Contexto

Hay que decidir explícitamente si NexaPDF necesita servidor y gestión de
usuarios, y en caso afirmativo montarlo en la capa gratuita de Firebase con
alertas de gasto de 5 €/mes.

## Decisión

**No hay backend, no hay cuentas y no hay red.** La aplicación **no declara el
permiso `INTERNET`**.

## Por qué

Repasando lo que hace la aplicación, no queda nada que necesite un servidor:

| Necesidad habitual de un backend | En NexaPDF |
|---|---|
| Guardar los datos del usuario | Los documentos son ficheros que ya están en su teléfono. |
| Sincronizar entre dispositivos | Se resuelve con exportar/importar `.nexaPDF.bak`, sin cuentas. |
| Procesar cosas pesadas | Todo el trabajo con PDF cabe de sobra en un móvil actual. |
| Autenticar | No hay nada que proteger: no hay datos en ningún servidor. |
| Cobrar | No se cobra nada. La donación es un enlace externo. |
| Analítica | No se recoge ningún dato, por decisión de diseño. |

Un servidor solo añadiría: coste recurrente, una superficie de ataque que hoy no
existe, una política de privacidad que explicar, obligaciones de RGPD, y un
punto de fallo que dejaría la aplicación inútil cuando no haya cobertura, que es
justo cuando más falta hace una app de documentos.

## La consecuencia interesante

No declarar `INTERNET` convierte la promesa de privacidad en algo **verificable**:
no es que la aplicación prometa no enviar tus documentos, es que el sistema
operativo no la deja. Cualquiera puede comprobarlo en el manifiesto o con
`aapt dump permissions`.

Eso simplifica de golpe la ficha de Google Play (formulario de Seguridad de los
datos: no se recogen ni se comparten datos), la política de privacidad y este
propio documento.

## Presupuesto de nube

**0 €/mes.** No hay ningún servicio de pago contratado, ni gratuito, así que no
hay ningún límite de gasto que configurar. Si algún día hiciera falta uno, este
ADR se sustituye por otro que lo justifique y se documenta el procedimiento de
alertas de presupuesto antes de integrarlo.

## Qué se pierde

- No hay sincronización automática entre dispositivos. Se cubre a mano con la
  exportación de ajustes.
- No hay informes de fallos automáticos: los fallos llegan por correo, si el
  usuario se molesta en escribir. Es el precio de no instalar telemetría, y se
  paga a gusto.
