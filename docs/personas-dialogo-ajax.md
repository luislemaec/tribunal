# Actualizar Persona: clic unico y foco

## Diagnostico en el navegador integrado (2026-10-19)

Sin guardar cambios validos sobre personas reales:

- Abrir Editar: el panel ejecutaba `PrimeFaces.focus` a Cedula y, despues,
  `Tribunal.mostrarDialogo` configuraba Nombres como segundo destino de foco.
  Se observaron ambos destinos sucesivos sin teclear.
- Desde Nombres, Cancelar y la X no cerraban en el primer clic. El segundo
  cerraba. El blur iniciaba un AJAX global que mostraba `statusDialog`, modal,
  antes de completarse el clic sobre el control de cierre.
- Nombres vacio, cursor dentro, Guardar: primer clic actualizaba solamente
  `msgNombres` (campo sin clase de error); segundo clic ejecutaba el proceso
  completo, actualizaba el panel y marcaba el campo invalido.
- Cambiar Cedula y avanzar: `change` sustituia `outPnlPersonas` completo y
  ejecutaba de nuevo el foco del panel. Se observaron el mensaje de cedula
  invalida y el retorno a Cedula incluso al intentar avanzar a otro control.
- La consulta con cedula valida y posterior cancelacion no escribio datos.
  La consulta sigue teniendo la misma logica funcional.

## Correccion

- AJAX de campos con `global=false`: se mantienen eventos, validadores,
  `required`, listeners, cola de peticiones y mensajes; no abren el modal de
  espera. Guardar conserva su estado de carga y proceso completo del panel.
- Cedula actualiza su mensaje. Solo si la consulta encuentra una Persona,
  el controller restablece los valores locales de Nombres/Sexo y actualiza
  esos controles, el mensaje de nombres, aviso de regularizacion y Guardar.
  No se reconstruyen la iglesia, habilitacion ni el resto del panel.
- Un unico foco inicial (primer campo del dialogo); `p:focus` solo se emite
  tras validacion JSF fallida. Se conserva el foco de errores de negocio.
- Nuevo/Editar restablecen los inputs actualizados (`resetValues`) y
  refrescan tambien Guardar para no conservar un estado disabled anterior.
- Cancelar/X/Escape conservan el cierre cliente y `cerrarFormulario`.
  No se modifica `actualizarPersona`, servicios, padron, permisos ni auditoria.

## Verificacion y limites

- `mvn -DskipTests compile`: correcto.
- `mvn -q -Pqr-tests -Dtest=EdicionPersonaMiembroTest test`: 13 pruebas,
  cero errores y fallos. En este entorno fue necesario ejecutarlo fuera del
  sandbox para que javac pudiera leer las clases locales.
- XHTML comprobado como XML y `git diff --check` correcto.
- No se desplego el cambio. La reproduccion anterior corresponde a la version
  desplegada antes de corregir; no se presenta como prueba posterior al cambio.

Al desplegar, repetir abrir/cerrar, Cancelar y X desde Nombres, cambiar Cedula
y avanzar, y Guardar con campo aun enfocado (nombre y cedula, validos e invalidos).
Los invalidos deben mostrar mensajes y conservar el modal; los validos deben
guardar/cerrar con un clic. Para la ultima comprobacion se necesita un registro
de prueba; no se hicieron guardados validos sobre personas reales.

## Auditoria de la version desplegada (2026-10-19)

El usuario confirmo el despliegue de la primera correccion. Se comprobaron
en el HTML real `global=false`, el update acotado de Cedula y la ausencia de
`p:focus` al abrir sin errores.

| Escenario | Resultado observado en el navegador |
| --- | --- |
| Abrir Editar | Foco inicial estable en Cedula |
| Nombres enfocado -> Cancelar | Cierra en un clic |
| Guardar con Nombres vacio y aun enfocado | Primer clic procesa el panel, muestra el error y enfoca Nombres |
| X desde el formulario con error | Cierra en un clic |
| Reabrir tras cancelar errores | Valores originales y mensajes limpios |
| Nuevo -> Guardar vacio | Tres mensajes requeridos visibles, foco en Cedula, modal permanece abierto |
| Escape desde Nuevo con errores | Cierra en una pulsacion |
| Carga de Guardar | Indicador y boton ocupado desaparecen al terminar la respuesta |
| Consola | Sin errores ni advertencias en los escenarios probados |

### Inconsistencias encontradas y corregidas en esta auditoria

- Tras corregir Nombres, desaparecia su mensaje pero persistia la clase de
  error. Cedula invalida mostraba el mensaje sin marcar el input. Ahora las
  validaciones parciales actualizan el componente origen y su mensaje;
  no se reconstruye el panel. PrimeFaces 15 conserva el foco que estaba activo
  al recibir la respuesta, sin introducir un foco manual.
- Las comprobaciones no globales carecian de carga y aviso de fallo de red.
  `tec-personas.js` muestra estado local accesible y `aria-busy`, sin modal
  bloqueante, cambios de foco, temporizadores, reintentos ni reenvios.
  Se reserva una linea de altura para no mover los botones al iniciar carga.
  Cancelar/abrir limpia el estado y `oncomplete` no borra un fallo de `onerror`.
- Ante una respuesta de guardado no confirmada, se mantiene el dialogo y se
  solicita revisar el listado antes de guardar otra vez. Solo la respuesta
  explicita `personaGuardada=true` cierra el formulario. Esa bandera se agrega
  despues de completar el flujo existente, sin cambiar sus reglas.
- Nuevo/Editar informan fallos de comunicacion y abren solo cuando
  `dialogReady=true`; una respuesta ausente no abre un formulario obsoleto
  ni accede a `args` indefinido.
- Textos centralizados en `messages_es.properties`; estilos en `personas.css`.

### Validacion de los ajustes adicionales

- Compilacion correcta y XHTML valido como XML.
- `node --test src/test/js/tec-personas.test.cjs`: 7 pruebas correctas
  (carga, controles reemplazados, errores, limpieza y callbacks de apertura/cierre).
- `mvn -q -Pqr-tests -Dtest=EdicionPersonaMiembroTest test`: 13 pruebas correctas.
- Captura de la validacion observada en la version desplegada:
  `target/personas-diagnostico/validacion-desplegada.jpg`, sin datos personales.
- Los nuevos ajustes de esta auditoria aun no se han desplegado. No se simulo
  una caida de red en el servidor real; sus callbacks se verificaron con pruebas
  aisladas. Sigue pendiente comprobar un guardado valido con un registro de prueba.
- No se modificaron servicios, permisos, padron ni auditoria. Las pruebas web
  se descartaron al cerrar el formulario y no persistieron cambios de personas.
