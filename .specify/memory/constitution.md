<!--
Informe de sincronización
- Versión: plantilla → 1.0.0 (primera ratificación).
- Principios: I–X definidos a partir de CLAUDE.md, Agents.md y las reglas acordadas en el trabajo diario.
- Secciones agregadas: Restricciones técnicas, Flujo de trabajo y calidad, Gobernanza.
- Plantillas por revisar: .specify/templates/plan-template.md (Constitution Check),
  spec-template.md y tasks-template.md: deben verificarse contra los principios I–X.
- Pendientes: ninguno.
-->

# Constitución del Tribunal Electoral (TEC)

Sistema `ec.com.antenasur.tec:tec` (WAR) del Tribunal Electoral de CONPOCIIECH: registro de iglesias
y miembros, padrón, Juntas Receptoras del Voto (JRV), escrutinio, actas y reportes. Esta constitución
fija las reglas no negociables para especificar, planificar e implementar cambios. El detalle técnico
vive en `CLAUDE.md` y `Agents.md`; ante conflicto, prevalece esta constitución.

## Principios fundamentales

### I. Preguntar antes de asumir reglas de negocio (NO NEGOCIABLE)

Toda ambigüedad sobre reglas electorales o de negocio (estados, quién puede qué, alcance de un
filtro, efectos sobre el padrón, la JRV o el escrutinio) se consulta al responsable antes de
implementar, ofreciendo opciones con una recomendación. Cuando se pida "solo analizar", no se
modifican archivos ni la base de datos. Las decisiones tomadas se documentan en la especificación.

### II. Integridad del proceso electoral

- El núcleo es `ProcesoElectoral` (no el legado `Periodo`). Toda consulta de padrón, JRV o escrutinio
  filtra por el proceso (`proce_id`), nunca solo por mesa: los datos de procesos anteriores conviven
  en las mismas tablas.
- Una iglesia pertenece a una sola mesa por proceso; una persona tiene un solo padrón activo por proceso
  y una sola designación de JRV.
- No se modifica ni elimina lo que el proceso en curso ya usa: mesas con padrón, JRV o escrutinio;
  recintos con mesas en uso; juntas completadas (solo mediante reapertura autorizada y auditada);
  escrutinios cerrados o con sufragio registrado.
- Las bajas son lógicas y conservan el historial.

### III. Reglas de negocio en el backend, no en la vista

Las reglas viven en `service/` (EJB) y las consultas en `facade/`. Los controllers JSF son delgados y
toda validación de la vista se repite en el servicio. Un cambio nunca es solo visual: si la pantalla
permite u oculta algo, el backend aplica la misma regla. Se reutiliza la regla existente en lugar de
duplicar condiciones; la vista y el servicio comparten un único punto de decisión.

### IV. Seguridad por rol y cronograma en el servicio

- Las escrituras sensibles se protegen con `@RolesAllowed` en el EJB, no solo con `rendered` en XHTML.
- WildFly niega los métodos sin anotar de un EJB con seguridad
  (`default-missing-method-permissions-deny-access`): al anotar un método, la clase debe declarar su
  política (`@RolesAllowed` o `@PermitAll` justificado) y los métodos heredados de `AbstractService`
  deben quedar cubiertos.
- Las operaciones sujetas al cronograma usan `CronogramaService.permite(...)`, que exime a
  Administrador y Tribunal mientras haya proceso activo.
- No se otorgan permisos adicionales por conveniencia, no se debilita Elytron ni el login FORM
  (`request.login(...)` es la autoridad), y `@PermitAll` solo se usa con justificación escrita.

### V. Esquema solo por Flyway

`persistence.xml` usa `hbm2ddl.auto=none`. Todo cambio de esquema es una migración nueva en
`src/main/resources/db/migration/`. Nunca se modifica una migración ya ejecutada; `V1` y `V2` son solo
para bases nuevas.

### VI. Datos personales y archivos sensibles

- No se consultan, buscan ni reproducen `db_tribunal.backup`, `V2__datos_iniciales.sql` ni
  `importacion_iglesias_personas.sql` (contienen datos reales y hashes); las búsquedas los excluyen.
- No se vuelca `standalone.xml`, no se copian claves privadas (p. ej. `rootCA-key.pem`), no se
  registran contraseñas, tokens ni URLs completas de QR en logs, mensajes o auditoría.
- Contraseñas solo con `PasswordService.hashBcrypt(...)`.
- Toda exportación con datos personales (Excel/PDF) queda registrada en la bitácora.

### VII. Auditoría de lo que importa

Las operaciones sensibles dejan rastro: Envers en entidades auditadas, entradas automáticas de
`AbstractFacade` y entradas estructuradas en `tec.procesos`
(`ACCION | MÓDULO: …; RESULTADO: …; DETALLE: …`, máximo 255 caracteres, acortando el detalle y nunca
el formato), con su regla en `CatalogoActividades` y su prueba.

### VIII. Español e interfaz institucional coherente

- Conversación, código, comentarios y textos visibles en español.
- Todo texto visible vive en `messages_es.properties`, escapado en `\uXXXX`.
- Interfaz sobria y accesible (PrimeFaces 15, estilos y tokens institucionales de `tribunal-globals.css`,
  tipografía base 16px), sin depender solo del color, y responsive (móvil, tablet, escritorio).
- Un mismo concepto se nombra y presenta igual en toda la pantalla: estados, filtros y etiquetas usan
  una sola fuente de verdad.

### IX. Rendimiento y consultas

Sin consultas N+1: conteos y estados se resuelven con consultas agregadas o en lote. Las tablas grandes
se paginan en la base. Los filtros que no requieren datos nuevos trabajan en memoria. No se cargan
entidades pesadas en sesión; las vistas usan DTO.

### X. Verificación antes de entregar

Todo cambio compila (`mvn -DskipTests compile`, con JDK 17), valida sus XHTML y, cuando aplica,
agrega o actualiza pruebas. Las consultas JPQL nuevas se validan contra el metamodelo. Se informa con
precisión qué se probó y qué no; nunca se afirma un resultado no verificado.

## Restricciones técnicas

- Java 17 (compilación con JDK 17; Lombok 1.18.34 no soporta JDK 24+), Jakarta EE 10, WildFly 39,
  Jakarta Faces 4, PrimeFaces 15 (+Extensions), Hibernate 6 con Envers, Flyway 11, PostgreSQL.
- Despliegue como `ROOT.war`; el sistema corre en `https://tribunal.local/`. El equipo compila y
  despliega; los agentes no inician ni compilan WildFly.
- Rutas de documentos solo vía `Constantes` (nunca rutas absolutas); PDF con `TipografiaPdf` y
  `PlantillaA4`; Excel con `ReporteXLSX`/`ReporteTabularXLSX`, liberando el libro al terminar.
- No se agregan bibliotecas nuevas sin aprobación.
- El acceso QR al acta parcial permanece deshabilitado (`tec.qr.enabled`) salvo instalación aprobada.

## Flujo de trabajo y calidad

1. Análisis: identificar vista, controller, servicio, facade, entidades y consultas; señalar reglas
   existentes, riesgos y preguntas de negocio.
2. Especificación y plan con Spec Kit (`/speckit-specify`, `/speckit-clarify`, `/speckit-plan`); el
   plan debe superar el "Constitution Check" contra los principios I–X.
3. Implementación en cambios pequeños y coherentes, sin tocar lo que no corresponde.
4. Verificación (principio X) y, cuando sea posible, prueba en el navegador sin modificar datos sin
   autorización.
5. Commits con el formato `Tec v1.0.NNNN- Web <Add|Up|Fx> - <descripción>`, título y detalle separados.

## Gobernanza

- Esta constitución prevalece sobre otras prácticas; `CLAUDE.md` y `Agents.md` la detallan y deben
  mantenerse alineados con ella.
- Toda especificación, plan y revisión verifica su cumplimiento; cualquier excepción se justifica por
  escrito en el plan.
- Enmiendas: se proponen con motivo, se aprueban por el responsable del proyecto y se versionan
  (MAJOR: se elimina o redefine un principio; MINOR: se agrega un principio o sección; PATCH:
  aclaraciones de redacción).

**Versión**: 1.0.0 | **Ratificada**: 2026-11-06 | **Última enmienda**: 2026-11-06
