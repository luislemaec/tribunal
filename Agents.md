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
- `migraciones.sql`: scripts manuales de BD.
- `db_tribunal.backup`: respaldo de BD; no modificar ni reemplazar sin indicacion explicita.
