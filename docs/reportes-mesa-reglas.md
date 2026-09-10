# Documentos de mesa: análisis y reglas

## Alcance y situación encontrada

Se revisó el árbol de trabajo existente, incluidos los cambios previos del flujo de acta física. No se modificaron las migraciones ni el respaldo de base de datos para esta implementación.

`reportesMesa.xhtml` carga el proceso activo, muestra mesas paginadas y permite filtrar cantón, parroquia, estado documental y texto. Cada fila abre un menú. Las acciones pasan a `ReporteMesaController`, luego a `ReporteMesaService`, servicios específicos y facades JPA.

| Opción existente | Generación/lectura | Condiciones previas encontradas |
|---|---|---|
| Padrón de Mesa | `ReporteMesaService.generarPadron` → `generarExcelPadron`; construye el XLSX con `ReporteXLSX.nuevoExcel`, `creaEspacioInformativoPadron` y `obtenerContenidoExcel` | Selección, proceso activo y padrón no vacío |
| Certificados | `generarCertificados` → `ReportePFD.generarCertificadosVotacion` | Rol Administrador/Tribunal, registros no vacíos/sin duplicados y fecha de sufragio; faltaba habilitación |
| MJRV | Enlace a `/mjrv`; no existía generador ni tipo de archivo MJRV | El resumen aceptaba cualquier cantidad positiva de integrantes |
| Acta Parcial | `generarActaParcial` → `ReportePFD.generarFormularioActaParcial` | Selección; faltaban padrón, junta completa y datos mínimos |
| Acta Física | `obtenerDocumentoActivo` / `visualizarActaFisica` | Archivo vigente disponible; faltaba cierre en esta lectura |
| Resultados finales | `ActaFisicaEscrutinioService.validarActaFinal` → `EscrutinioService.validarResultadosFinalesDTO` | Existencia de evidencia, rol y cierre; faltaban revisión explícita, completitud de categorías y cuadre independiente |

El acta parcial actual es un **formulario para llenado manual**. No necesita conteo ni cierre previo. Se conservan el PDF, sus firmas actuales y su diseño. La junta debe estar íntegra aunque el formulario utilice solamente las firmas de presidente y secretario.

La MJRV continúa sin un nuevo archivo PDF: se añadió su **visualización de conformación** para la mesa seleccionada y se conservó el enlace a gestión. No se inventó un tipo documental ni un historial de archivos que el sistema no tenía.

## Fallos corregidos

- Reutilización de archivos antes de validar el acceso y las precondiciones.
- Confianza en `personaId` y `presidenteRestringido` enviados por el Controller para decidir alcance.
- JRV marcada completa con un solo miembro y búsqueda de presidente por coincidencia parcial que también aceptaba vicepresidente.
- Certificados para personas no habilitadas.
- Visualización del acta física de una mesa no cerrada.
- Cuadre reducido a comprobar números no negativos; posibilidad de enviar solamente parte de los resultados o repetir registros.
- Posibilidad de marcar un acta `VALIDADA` por el método genérico de revisión, sin validar los resultados.
- Falta de confirmación explícita de revisión y de comparación con totales transcritos de la fotografía.
- Reutilización de documentos cuya fuente había cambiado sin cambiar el hash; nuevas versiones sin numeración uniforme.

## Matriz implementada

Todas las acciones documentales exigen proceso activo, acceso autorizado, mesa/recinto activos y correspondencia **Proceso + Recinto + Mesa**. Los archivos se buscan por proceso, mesa, tipo y versión activa, y se verifica su recinto y disponibilidad real en el repositorio.

| Documento | Proceso | Padrón | MJRV completa | Conteo | Mesa cerrada | Acta física | Acción permitida |
|---|---|---|---|---|---|---|---|
| Padrón de Mesa | Activo | No vacío | — | — | — | — | Generar; visualizar/regenerar si existe |
| Certificados | Activo | Personas activas y habilitadas, sin duplicados | — | — | — | — | Generar/visualizar/regenerar, Administrador o Tribunal |
| MJRV | Activo | Integrantes pertenecientes a esa mesa/proceso | Sí | — | — | — | Visualizar conformación; gestionar desde su módulo |
| Acta Parcial | Activo | No vacío | Sí | No requerido | No requerido | No requerida | Generar; visualizar/regenerar si existe |
| Acta Física | Activo | — | — | — | Sí | JPEG vigente disponible | Visualizar/descargar; pendiente cuando no existe imagen |
| Resultados finales | Activo | No vacío | Sí | Todas las categorías, sin duplicados, valores no negativos y cuadre | Sí | Vigente y revisión confirmada | Validar por Administrador/Tribunal |

Para el acta parcial se exige fecha activa de sufragio, al menos una categoría de lista del proceso, identificación de mesa/recinto y ubicación. Los certificados también requieren la fecha de sufragio.

Los cargos obligatorios son **PRESIDENTE, SECRETARIO, TESORERO y VOCAL**, definidos en `MiembroJRVService`. Se evalúan las designaciones activas de `miembros_jrv` mediante `MiembroJRVFacade.consultarConformacion`, filtradas por proceso y mesas, con el mismo alcance de `listarPorMesaProceso` utilizado por la gestión MJRV. El cargo coincide exactamente con su dignidad (con o sin «DE MESA»), una vez por cargo y con personas distintas. No se exige documento de MJRV, nombres/apellidos completos, habilitación actual en el padrón ni un catálogo padre específico para reconocer una junta ya designada. Esos filtros adicionales de la versión anterior podían descartar los cuatro cargos existentes y mostrar un bloqueo incorrecto. La pertenencia al padrón sigue validándose al designar un miembro; la autorización de acceso conserva el proceso y mesa. Un cargo duplicado o una designación sin persona sigue bloqueando.

`mjrv.xhtml` y Reportes consumen el mismo `EstadoJuntaDTO` calculado por `MiembroJRVService`; la acción Completar Junta también revalida esa regla. El requisito documental es la conformación válida de las cuatro dignidades. No depende del campo global `Mesa.responsable`, que puede proceder de otro proceso, ni exige conteo/cierre. Completar Junta conserva su acción adicional de habilitar al usuario presidente. La gestión muestra el estado actualizado después de cada designación/retiro; el botón **Actualizar disponibilidad** de Reportes recarga el menú conservando filtros y, cuando es posible, la página actual. Cada comando vuelve a consultar sus requisitos antes de ejecutarse.

La fotografía puede visualizarse aunque la junta esté incompleta para permitir inspeccionar evidencia. La **validación final** sí exige junta completa.

## Arquitectura y estados

`XHTML → Controller → Service → Facade → JPA`.

- `AccesoDocumentoMesaService`: identidad autenticada de Elytron, rol revisor y restricción de presidente según JRV del proceso activo. El rol de presidente conserva su restricción aunque coexista con otros roles.
- Permisos EJB: `@DeclareRoles` declara los roles pero no autoriza métodos. `@RolesAllowed` en `AccesoDocumentoMesaService` permite invocar sus métodos a `SITEC-Administrador`, `SITEC-Tribunal` y `SITEC-Presidente-mesa`; corrige el rechazo `WFLYEJB0364` antes de `mesaPermitida`. No se utiliza `@PermitAll` ni se elimina la comprobación interna de proceso/mesa. Los nombres de Administrador y Tribunal coinciden con el seed y ambos tienen el menú 29 (`reportesMesa`). Las pruebas verifican los permisos declarativos además de la lógica de ambos roles; una instancia Java directa por sí sola no prueba la interceptación de seguridad EJB.
- `DisponibilidadDocumentoMesaService`: única política de disponibilidad compartida entre resumen y comandos. Revalida antes de generar, regenerar o visualizar.
- `DependenciasDocumentoMesaFacade`: proyecciones escalares por lote de padrón, cabeceras y documentos; fecha, tipos documentales y categorías se cargan una vez por lote. `MiembroJRVFacade` consulta la conformación de todas las juntas del lote en una sola consulta.
- `DependenciasMesaDTO`: datos internos para evaluar las reglas.
- `EstadoDocumentoMesaDTO`: `puedeGenerar`, `puedeRegenerar`, `puedeVisualizar`, `bloqueado`, `motivoBloqueo` y `estado`.
- `MesaDocumentosDTO`: mapa de estados por tipo; su resumen cuenta requisitos disponibles. Los getters no ejecutan JPA.
- `DocumentoService`: integridad de proceso/mesa/recinto, bloqueo para versionado, numeración y baja lógica de la versión anterior. Se conserva la historia y la limpieza de archivos ante rollback.

El número de consultas del listado no crece por cada fila. No hay búsquedas individuales de padrón, junta, cabecera o fotografía dentro del bucle del menú. La existencia de archivos se verifica en disco sobre las rutas obtenidas en lote. La tabla mantiene su paginación en memoria; los filtros no reducen accidentalmente sus propias opciones de selección.

Cuando falta una condición, el menú muestra el motivo del Service. Un documento disponible ofrece visualizar y, si es generable, regenerar. Un acta física cerrada sin imagen muestra pendiente. Generar/regenerar un acta física sigue prohibido.

## Revisión y cuadre

`RevisionActaFinalDTO` separa los totales declarados en la fotografía de las sumas calculadas:

```
Válidos calculados = suma de listas
Total calculado = válidos + nulos + blancos
Diferencia de válidos = válidos declarados − válidos calculados
Diferencia total = total declarado − total calculado
```

Las papeletas restantes no se suman como votos. Ambas diferencias deben ser cero. Se usan sumas `long` para evitar desbordamiento. No se ajusta ningún valor automáticamente.

El operador confirma que revisó los valores. Cualquier edición AJAX desmarca esa confirmación. En el servidor, la clasificación se reconstruye desde las categorías persistidas: no se confía en los nombres o tipos enviados por el navegador. Se rechazan IDs ajenos, duplicados, categorías omitidas, evidencia de otro contexto y revisiones ya validadas. Todas las validaciones preceden a la primera modificación de votos. Los votos, cabecera, estado de mesa y revisión documental se actualizan en la misma transacción.

## Archivos modificados o añadidos

Vista y Controller: `reportesMesa.xhtml`, `ReporteMesaController`, `mjrv.xhtml`, `JrvController`.

Servicios: `ReporteMesaService`, `DocumentoService`, `ActaFisicaEscrutinioService`, `EscrutinioService`, `MiembroJRVService`, `AccesoDocumentoMesaService`, `DisponibilidadDocumentoMesaService`.

Facades: `DocumentoFacade`, `MiembroJRVFacade`, `PadronFacade`, `DependenciasDocumentoMesaFacade`.

DTO: `MesaDocumentosDTO`, `EstadoDocumentoMesaDTO`, `DependenciasMesaDTO`, `RevisionActaFinalDTO`, `EstadoJuntaDTO`.

Mensajes: `messages_es.properties`, con los nuevos textos escapados en ASCII/Unicode.

Pruebas: clases `*DocumentoMesaTest`, `RevisionActaFinalTest` y `ConformacionJuntaTest`; ejecutor `scripts/test-reportes-mesa.ps1`. La conformación cubre habilitación sin documento de MJRV, cargos faltantes/repetidos, persona ausente/duplicada, vicepresidente y separación del lote por mesa/proceso. La consulta preserva los filtros de designaciones activas por mesa/proceso y no introduce filtros de padrón, datos personales o documentos. También verifica la diferencia entre conformación y registro de junta completada, conservando su bloqueo de edición.

## Verificación reproducible

```powershell
./scripts/test-reportes-mesa.ps1
mvn -DskipTests compile
```

El ejecutor utiliza una copia temporal del POM con Surefire habilitado y salida aislada en `target/reportes-validation`; no cambia `pom.xml`. Valida reglas, cuadre, identidad/alcance, rutas alternativas de generación/lectura/validación y compilación HQL con Hibernate 6.6 contra las entidades reales, sin conexión a una base de datos.

La comprobación de integración pendiente en un entorno desplegado comprende: rendering PrimeFaces/AJAX, roles de Elytron, consultas SQL sobre PostgreSQL, generación/descarga real de archivos y concurrencia de dos revisores. Las pruebas locales no simulan esos servicios externos.
