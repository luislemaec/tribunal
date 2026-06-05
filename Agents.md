# Agents.md

Guia rapida para agentes que trabajen en este proyecto.

## Proyecto

- Nombre Maven: `tribunal`
- Modulo/aplicacion: Tribunal Electoral System (`TEC` en `web.xml`)
- Grupo Maven: `ec.com.antenasur.tec`
- Version: `1.0.1`
- Empaquetado: `war`
- Plataforma: Java 17, Jakarta EE 10, WildFly 39, JSF/Jakarta Faces 4, PrimeFaces 15, JPA/Hibernate 6
- Persistencia: JTA datasource `java:jboss/datasources/TribunalDS`
- Seguridad: FORM login con Elytron/WildFly mediante `request.login(...)`
- Security domain del deployment: `TribunalSecurityDomain`
- Password hashing: BCrypt (`at.favre.lib:bcrypt`) con costo 12, compatible con Elytron/modular crypt
- Configuracion de documentos: propiedad JVM `-Drpm.files.path=/var/app/tec/documentos` en `standalone.conf`

## Estructura Raiz

```text
tec/
|-- Agents.md
|-- pom.xml
|-- migraciones.sql
|-- db_tribunal.backup
+-- src/
    |-- main/
    |   |-- java/
    |   |-- resources/
    |   +-- webapp/
    +-- test/
        +-- resources/
```

Notas:

- `target/` es salida generada por Maven; no asumir que existe antes de compilar.
- No hay `src/test/java` con pruebas activas en la estructura actual.
- `README.md` no existe actualmente en la raiz.

## Backend Java

Base de paquetes:

```text
src/main/java/ec/com/antenasur/
|-- audit/          Auditoria Hibernate Envers
|-- bean/           Beans de sesion y estado de UI
|-- blockviewer/    Componentes de soporte visual
|-- component/      Componentes JSF personalizados
|-- controller/     Controladores JSF/CDI por pantalla
|-- converter/      Convertidores JSF
|-- dto/            Objetos de transferencia hacia UI
|-- enums/          Enumeraciones de dominio
|-- exception/      Excepciones de negocio
|-- facade/         Acceso a datos JPA
|   +-- tec/        Facades del modulo electoral
|-- itext/          Generacion de reportes PDF/XLSX
|-- model/          Entidades JPA
|   |-- generic/    Infraestructura generica JPA
|   +-- tec/        Entidades del modulo electoral
|-- report/         Controladores/plantillas de reportes
|-- service/        Servicios EJB/CDI de negocio
|   +-- tec/        Servicios del modulo electoral
+-- util/           Utilidades JSF, filtros, correo, archivos, constantes
```

## Capas Principales

- `controller/`: coordina eventos JSF, validaciones de pantalla, navegacion y llamadas a servicios.
- `service/`: concentra reglas de negocio y transacciones EJB/CDI.
- `facade/`: consultas JPA y persistencia directa.
- `model/`: entidades JPA, varias auditables mediante infraestructura generica/Envers.
- `dto/`: datos planos para vistas; preferir DTOs sobre entidades completas en JSF/sesion.
- `bean/`: estado conversacional o de sesion, especialmente `LoginBean`.
- `util/`: utilidades transversales; evitar agregar aqui reglas de negocio nuevas.

## Nucleo Electoral

El sistema esta migrado funcionalmente para usar `ProcesoElectoral` como eje del modulo electoral. Evitar volver a introducir logica basada solo en `Periodo` cuando se trate de padron, JRV, actas o escrutinios.

Entidades/tablas centrales:

- `ProcesoElectoral` / `tec.proceso_electoral`: proceso activo y contexto electoral.
- `Padron` / `tec.padron`: relaciona `igpe_id`, `mesa_id` y `proce_id`; la unicidad funcional es persona-iglesia por proceso.
- `MiembroJRV` / `tec.miembros_jrv`: designacion de miembros de Junta Receptora del Voto por `igpe_id`, `mesa_id`, `proce_id` y `cargo_id`.
- `EscrutinioCabecera` / `tec.escrutinio_cabecera`: estado normalizado de apertura, conteo y cierre de mesa por `mesa_id` y `proce_id`.
- `Escrutinio` / detalle de votos: totales por categoria de voto, mesa y proceso.

Reglas importantes:

- Consultas de padron por mesa deben filtrar tambien por `proce_id`; no usar solo `mesa_id`, porque existen datos historicos de procesos anteriores.
- La asignacion al padron solo debe considerar `IglesiaPersona.habilitadoPadron = true`.
- Una iglesia asignada a una mesa dentro de un proceso no debe aparecer como disponible para otra mesa del mismo proceso.
- JRV debe validar que la persona designada pertenezca al padron de la mesa y del proceso seleccionado.
- La mesa del Presidente de Mesa en `actaE.xhtml` se resuelve primero desde `miembros_jrv` por `personaId + proce_id + cargo PRESIDENTE`; `mesa.responsable` queda como compatibilidad.
- Completar una JRV crea o reutiliza el usuario del presidente y asigna el rol `SITEC-Presidente-mesa`.

## Seguridad y Login

Archivos relevantes:

```text
src/main/webapp/login.xhtml
src/main/webapp/WEB-INF/web.xml
src/main/webapp/WEB-INF/jboss-web.xml
src/main/java/ec/com/antenasur/controller/LoginController.java
src/main/java/ec/com/antenasur/bean/LoginBean.java
src/main/java/ec/com/antenasur/service/PasswordService.java
src/main/java/ec/com/antenasur/service/UsuarioService.java
src/main/java/ec/com/antenasur/util/LoginFilter.java
```

Flujo esperado:

1. JSF envia credenciales desde `login.xhtml`.
2. `LoginController.login()` inicializa auditoria y llama primero a `request.login(...)`.
3. Elytron valida usuario/password contra `TribunalSecurityDomain`.
4. Solo si Elytron autentica, la app carga contexto con `UsuarioService.cargarContextoUsuarioAutenticado(...)`.
5. La sesion se prepara con usuario, roles, menu, timeout y auditoria de acceso.
6. Usuarios permanentes van a `dashboard.jsf` o `actaE.jsf` segun rol; usuarios no permanentes van a `cambioClave.jsf`.

Notas de seguridad:

- No validar password manualmente en controllers.
- No guardar passwords en texto plano.
- No usar SHA-1 ni utilidades legacy para contrasenas.
- No registrar passwords en logs, auditoria o mensajes.
- No cargar `PersonaDTO` separado en sesion salvo necesidad demostrada; `UsuarioDTO` ya contiene datos planos utiles.
- Mantener `LoginBean.roles` porque varias vistas usan `loginBean.roles.contains(...)`.
- Si se agrega cambio/recuperacion de clave, usar `PasswordService.hashBcrypt(...)`.
- Si se cambia Elytron, verificar `standalone.xml`, `TribunalSecurityDomain`, `TribunalRealm` y `modular-crypt-mapper`.
- El rol `SITEC-Presidente-mesa` redirige a `actaE.jsf` y debe quedar limitado a la mesa asignada en JRV para el proceso activo.

## Webapp JSF

```text
src/main/webapp/
|-- index.html              Welcome file
|-- *.xhtml                 Pantallas JSF principales
|-- errors/                 Paginas de error
|-- WEB-INF/
|   |-- web.xml             Faces Servlet, FORM login, LoginFilter
|   |-- jboss-web.xml       Security domain del deployment
|   |-- faces-config.xml
|   |-- tribunal.taglib.xml
|   |-- template.xhtml      Layout base
|   |-- menu.xhtml          Menu de aplicacion
|   |-- toopbar.xhtml       Barra superior
|   |-- navegacion.xhtml    Navegacion/sesion
|   |-- globals.xhtml
|   |-- config.xhtml
|   |-- footer.xhtml
|   |-- right_panel.xhtml
|   +-- status.xhtml
+-- resources/
    |-- css/
    |-- demo/
    |-- ecuador-layout/
    |-- fonts/
    |-- img/
    |-- primeblocks/
    |-- primefaces-ecuador-*/
    +-- sass/
```

Mapeos JSF en `web.xml`:

- `/faces/*`
- `*.jsf`
- `*.faces`
- `*.xhtml`

Pantallas destacadas:

- `login.xhtml`: inicio de sesion.
- `cambioClave.xhtml`, `olvidoClave.xhtml`, `recuperaClaveCorrecto.xhtml`: claves y recuperacion.
- `dashboard.xhtml`, `perfil.xhtml`: entrada y perfil.
- `usuarios.xhtml`, `asignacionUsuarios.xhtml`: administracion de usuarios.
- `permisos.xhtml`, `roles.xhtml`, `menu.xhtml`: permisos y navegacion.
- `personas.xhtml`, `iglesias.xhtml`, `padron.xhtml`: gestion de personas/iglesias/padron.
- `procesos.xhtml`, `periodos.xhtml`, `cronograma.xhtml`: configuracion del proceso electoral.
- `recintos.xhtml`, `mesas.xhtml`, `mjrv.xhtml`: lugares y miembros JRV.
- `listas.xhtml`, `candidatos.xhtml`, `autoridades.xhtml`: postulaciones y autoridades.
- `actaE.xhtml`, `escrutinios.xhtml`, `consultar.xhtml`: flujo electoral y consulta.
- `catalogos.xhtml`, `plantillaCorreos.xhtml`: configuracion general.

## Recursos y Persistencia

```text
src/main/resources/
|-- META-INF/
|   +-- persistence.xml
|-- import.sql
+-- ec/com/antenasur/resources/
    +-- messages_es.properties
```

`persistence.xml`:

```xml
<persistence-unit name="tribunalPU">
    <jta-data-source>java:jboss/datasources/TribunalDS</jta-data-source>
</persistence-unit>
```

Notas:

- `hibernate.hbm2ddl.auto` esta en `update`; para produccion preferir `validate` o `none` y aplicar cambios con scripts/versionado.
- `hibernate.jpa.compliance.query=false` conserva compatibilidad con queries heredadas de Hibernate 5, especialmente aliases en `JOIN FETCH`.
- `migraciones.sql` contiene scripts manuales de BD; tratarlo como archivo sensible.
- Todos los textos visibles al usuario deben centralizarse en `messages_es.properties`.
- En `messages_es.properties`, usar caracteres especiales escapados en ASCII/Unicode (`\u00e1`, `\u00e9`, `\u00ed`, `\u00f3`, `\u00fa`, `\u00f1`, `\u00bf`, etc.).
- Las claves de configuracion antes tomadas de `rpm-catalogos.properties` se centralizan en `messages_es.properties` y/o propiedades JVM:
  - `rpm.files.path`
  - `rpm.server.pruebas`
  - `rpm.server.produccion`
  - `tec.actas.escrutinio.dir`
  - `rpm.actas.escrutinio.dir`
- `rpm-catalogos.properties` no debe tratarse como dependencia obligatoria de runtime.

## Documentos y Reportes

La ubicacion base de documentos se resuelve en `Constantes`:

1. Propiedad JVM `rpm.files.path`.
2. Clave `rpm.files.path` en `messages_es.properties`.
3. `jboss.server.data.dir/documentos`.
4. `java.io.tmpdir/tec/documentos`.

Con `-Drpm.files.path=/var/app/tec/documentos`:

- Actas de escrutinio PDF: `/var/app/tec/documentos/actas-escrutinio`.
- Listas/Excel de miembros: `/var/app/tec/documentos/listas-miembros`.

Notas:

- No usar rutas duras como `/opt/ACTASE` o `C:\ARCHIVOS\ACTASE`.
- Al guardar archivos, crear el directorio padre si no existe.
- `Documentos.doc_path` debe guardar la ruta real donde se escribio el archivo.
- Para PDF/XLSX revisar `itext/`, controladores involucrados y `DocumentoBean`/`DocumentoService`.
- Los logos en reportes deben conservar proporcion y no distorsionarse.
- `ReportePFD.guardarDocumentosActasEObligatorio(...)` debe fallar si no existe contenido PDF o no se puede escribir el archivo.
- En `actaE.xhtml`, el cierre de mesa debe generar/guardar el PDF antes de marcar el escrutinio como `CERRADO`.

## Pantallas Electorales Clave

`padron.xhtml`:

- Carga recintos bajo demanda; no cargar todo por defecto.
- Al cambiar canton/parroquia/recinto/mesa, liberar selecciones dependientes.
- El listado del padron debe respetar el proceso activo.
- El total de sufragantes de mesa sale de `tec.padron` filtrado por `mesa_id + proce_id + estado`.

`mjrv.xhtml`:

- Registra integrantes de JRV en `tec.miembros_jrv`.
- No permitir eliminar/modificar una junta completada salvo flujo autorizado.
- Mensajes deben salir por el growl global.
- La designacion de Presidente de Mesa habilita el usuario con rol `SITEC-Presidente-mesa`.

`actaE.xhtml`:

- Restringe al Presidente de Mesa a su mesa asignada por JRV.
- El valor "Sufragantes asignados" se calcula desde el padron del proceso activo, no desde `Mesa.totalVotos`.
- Flujo esperado: apertura -> conteo/borrador -> generar PDF -> cerrar mesa.
- Si una mesa ya esta cerrada, puede regenerarse el acta PDF sin modificar el conteo.
- La tarjeta de cierre debe mostrar "Mesa cerrada" cuando el estado sea `CERRADO`, no "Validacion pendiente".

`reportePadron.xhtml`:

- Exporta Excel usando las utilidades existentes de `ReporteXLSX`.
- El reporte de padron por mesa tiene encabezado independiente con proceso, provincia, canton, parroquia, recinto y mesa.
- No repetir en columnas los datos que ya estan en la cabecera del reporte por mesa.

## Pruebas

```text
src/test/resources/
|-- arquillian.xml
|-- test-ds.xml
+-- META-INF/test-persistence.xml
```

- JUnit Jupiter esta declarado como dependencia de test.
- Surefire esta configurado con `<skip>true</skip>`, por lo que `mvn test` no ejecuta pruebas por defecto.
- Perfiles Arquillian disponibles: `arq-wildfly-managed` y `arq-wildfly-remote`.

## Build

Comandos utiles:

```bash
mvn -DskipTests compile
mvn clean package
mvn clean package wildfly:deploy
mvn wildfly:undeploy
```

Antes de finalizar cambios de codigo, ejecutar:

```bash
mvn -DskipTests compile
```

## Dependencias Clave

- Jakarta EE API `10.0.0` (`provided`)
- WildFly BOM `39.0.1.Final`
- WildFly Maven Plugin `5.1.3.Final`
- PrimeFaces `15.0.0` con classifier `jakarta`
- PrimeFaces Extensions `15.0.0` con classifier `jakarta`
- Hibernate Envers (`provided`)
- BCrypt `at.favre.lib:bcrypt:0.10.2`
- SLF4J API `2.0.16` (`provided`)
- Lombok `1.18.34`
- iText `5.5.13.4` / XMLWorker `5.5.13.3`
- Apache POI `5.3.0`
- Jakarta Mail API `2.1.3` (`provided`)
- Commons BeanUtils `1.9.4`
- Commons Lang `3.14.0`
- JUnit Jupiter `5.10.3` (`test`)

## Convenciones de Trabajo

- Mantener controllers delgados; mover reglas de negocio a `service/`.
- Mantener consultas JPA en `facade/`.
- Usar DTOs para vistas; no exponer entidades JPA completas en sesion.
- Evitar cargar objetos pesados en `LoginBean`.
- Preservar `request.login(...)` como autoridad de autenticacion.
- Preservar auditoria de acceso con `AccessAuditory`/`AccessService` cuando se modifique login/logout.
- Mantener `listaPermisos` en sesion cuando se altere menu/navegacion.
- Respetar los prefijos/propiedades de roles usados por `JsfUtil.getProperty("roles.sitec", true)` y `roles.mnemonic`.
- Al agregar una pantalla JSF, revisar permisos/menu y rutas permitidas, no solo el `.xhtml`.
- Si se cambian reportes, revisar tanto `itext/` como controladores `report/` y recursos usados por el PDF/XLSX.
- Evitar cambios amplios en temas PrimeFaces generados (`primefaces-ecuador-*`) salvo que el objetivo sea theming.
- En entidades JPA, preferir nombres explicitos de foreign keys con `@ForeignKey(name = "...")`.
- Para nuevas relaciones `@ManyToOne`, revisar `nullable`, `fetch`, indices y nombres de constraints.
- Para JSF/PrimeFaces, respetar `id`, `process`, `update`, `action`, `actionListener`, `rendered`, `disabled` y validaciones existentes.
- Usar el componente global de mensajes (`WEB-INF/globals.xhtml`) y evitar duplicar growls locales salvo necesidad justificada.
- Al cambiar seleccion multiple en PrimeFaces, actualizar explicitamente botones/resumenes afectados mediante AJAX.
- En interfaces institucionales, mantener diseno sobrio, claro y consistente; no saturar formularios ni tablas.

## Archivos Sensibles

- `pom.xml`: versiones Java/WildFly/PrimeFaces y dependencias.
- `src/main/resources/META-INF/persistence.xml`: datasource JTA y comportamiento Hibernate.
- `src/main/webapp/WEB-INF/web.xml`: filtros, Faces Servlet, FORM login, session timeout y errores.
- `src/main/webapp/WEB-INF/jboss-web.xml`: security domain.
- `src/main/java/ec/com/antenasur/controller/LoginController.java`: flujo de autenticacion.
- `src/main/java/ec/com/antenasur/bean/LoginBean.java`: estado de sesion/logout.
- `src/main/java/ec/com/antenasur/service/PasswordService.java`: hashing/verificacion BCrypt.
- `src/main/java/ec/com/antenasur/util/LoginFilter.java`: control transversal de acceso.
- `src/main/java/ec/com/antenasur/service/UsuarioService.java`: carga de contexto autenticado.
- `src/main/java/ec/com/antenasur/util/Constantes.java`: rutas centralizadas, roles y propiedades de configuracion.
- `src/main/java/ec/com/antenasur/controller/PadronController.java`: asignacion de iglesias/personas al padron por proceso.
- `src/main/java/ec/com/antenasur/controller/JrvController.java`: designacion de JRV y habilitacion de Presidente de Mesa.
- `src/main/java/ec/com/antenasur/controller/ActaEController.java`: apertura, conteo, cierre y generacion de acta PDF.
- `src/main/java/ec/com/antenasur/service/tec/EscrutinioService.java`: reglas de estado de escrutinio.
- `src/main/java/ec/com/antenasur/itext/ReportePFD.java`: generacion y guardado obligatorio de PDF.
- `src/main/java/ec/com/antenasur/itext/ReporteXLSX.java`: reportes Excel institucionales.
- `src/main/resources/ec/com/antenasur/resources/messages_es.properties`: textos visibles y configuracion centralizada.
- `migraciones.sql`: scripts manuales de BD.
- `db_tribunal.backup`: respaldo de BD; no modificar ni reemplazar sin indicacion explicita.
