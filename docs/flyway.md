# Flyway en TEC

## Diagnostico

Flyway es viable en TEC porque el proyecto usa Java 17, WAR Jakarta EE y un
datasource administrado por WildFly. La aplicacion no debe abrir conexiones
JDBC hardcodeadas; las migraciones de arranque usan:

`java:jboss/datasources/TribunalDS`

El proyecto ahora centraliza estructura y datos iniciales en Flyway. El archivo
`persistence.xml` mantiene `hibernate.hbm2ddl.auto=update`; para produccion, la
transicion correcta es:

1. Respaldar la base de datos.
2. Crear baseline Flyway sobre la base existente.
3. Cambiar Hibernate a `validate` o `none`.
4. Aplicar cambios futuros solo mediante scripts versionados.

`src/main/resources/db/migration/V1__baseline_inicial.sql` representa la
estructura base para ambientes nuevos. No debe ejecutarse sobre bases existentes.
`V2__datos_iniciales.sql` carga las semillas y los datos operativos iniciales
autorizados para una base nueva.

## Dependencias

Se agregaron:

```xml
org.flywaydb:flyway-core
org.flywaydb:flyway-database-postgresql
```

Version usada: `11.14.1`.

## Ejecucion al arranque del WAR

El runner `ec.com.antenasur.util.FlywayMigrationRunner` esta deshabilitado por
defecto.

Para habilitarlo:

```bash
-Dtec.flyway.enabled=true
```

Configuracion disponible:

```bash
-Dtec.flyway.enabled=true
-Dtec.flyway.locations=classpath:db/migration
-Dtec.flyway.defaultSchema=public
-Dtec.flyway.table=flyway_schema_history
-Dtec.flyway.baselineOnMigrate=false
-Dtec.flyway.baselineVersion=1
```

Para una base existente, usar baseline solo despues de respaldo. En ese caso
Flyway marca la version 1 como ya aplicada y no ejecuta
`V1__baseline_inicial.sql`:

```bash
-Dtec.flyway.enabled=true
-Dtec.flyway.baselineOnMigrate=true
-Dtec.flyway.baselineVersion=1
```

Con baseline version `1` en una base existente, revisar si los datos de V2 ya
estan cargados. Si ya existen, ejecutar V2 no debe duplicarlos porque usa
validaciones idempotentes; aun asi debe probarse sobre una copia.

## Migraciones

Ubicacion:

`src/main/resources/db/migration`

Convencion:

```text
V1__baseline_inicial.sql
V2__datos_iniciales.sql
V3__agregar_indice_padron_proceso_mesa.sql
```

`V1__baseline_inicial.sql` contiene:

- Schemas `public` y `tec`.
- Tablas principales mapeadas por JPA.
- Tabla `tec.tec_auditoria` y tablas `_aud` para Hibernate Envers.
- Primary keys, foreign keys, unique constraints e indices funcionales.
- Vistas `tec.vw_lugar_votacion` y `tec.vw_total_escrutinios`.
- Secuencia `public.seq_iglesia_codigo_generico`.

Se excluyen de V1:

- Backfills sobre datos historicos.
- Diagnosticos `SELECT`.
- `ALTER SEQUENCE ... OWNER TO postgres`.
- `ALTER SEQUENCE ... RESTART WITH`.
- Usuarios con clave inicial.
- Datos que dependen de IDs de una base existente.

`V2__datos_iniciales.sql` contiene:

- Roles base completos tomados de `tb_rol`, con `estado=TRUE`.
- Catalogo general completo, insertado por nivel jerarquico.
- Tipos de documentos operativos completos con IDs explicitos y `setval`.
- Categorias de voto completas; se normaliza `PAPELETAS RESTANTES`.
- Catalogo geografico base tomado de `tb_geograp.xlsx`, con `estado=TRUE`.
- Personas e iglesias iniciales con IDs explicitos y secuencias sincronizadas.
- Relaciones iglesia-persona activas con `igpe_habilitado_padron=TRUE`.
- Usuarios iniciales con hashes BCrypt costo 12; no contiene claves en texto plano.
- Relaciones rol-usuario activas con `rous_id` autoincremental.
- Proceso electoral y cronograma inicial con IDs explicitos.
- Recintos y mesas electorales, respetando geografia y recintos padre.
- Plantillas de correo iniciales.
- Menu funcional completo tomado de `tb_menu.xlsx`, con `estado=TRUE`.
- Relaciones menu-rol activas tomadas de `tb_menu_rol.csv`; `mero_id` queda autoincremental.

Orden relevante de dependencias en V2:

```text
tb_geograp
  -> tb_iglesia
  -> recintos
      -> mesas
tb_persona + tb_iglesia
  -> tb_iglesia_persona
tb_rol + tb_usuario
  -> tb_role_user
catalogo_general (padres antes que hijos)
proceso_electoral
  -> cronograma_fase
tb_menu + tb_rol
  -> tb_menu_rol
```

Se excluyen de V2:

- Contrasenas en texto plano o hashes inseguros.
- Backfills y saneamientos historicos.
- DDL y cambios estructurales.

El script `src/main/resources/db/manual/importacion_iglesias_personas.sql` se
conserva solo como alternativa manual. No debe ejecutarse despues de V2 ni
usarse como fuente paralela en una instalacion gestionada por Flyway.

V2 contiene datos personales y credenciales BCrypt reales. El repositorio y el
WAR deben tratarse como artefactos sensibles, con acceso restringido.

La carga de personas excluye los siete registros administrativos iniciales que
estaban duplicados por documento. Los `pers_id` restantes y sus referencias se
normalizan restando `7`, de modo que persona e iglesia-persona usan el rango
continuo `1-26197`.

No crear scripts destructivos sin respaldo y ventana de mantenimiento. Evitar
`DROP`, cambios de tipo masivos y `UPDATE` sin `WHERE` salvo que hayan sido
probados en una copia real de produccion.

## Permisos requeridos en PostgreSQL

El usuario del datasource debe poder:

- Crear la tabla `flyway_schema_history` en el schema por defecto.
- Ejecutar `ALTER TABLE`, `CREATE INDEX`, `CREATE SEQUENCE` y DML segun el
  contenido de cada migracion.
- Usar los schemas donde viven las tablas: actualmente `public` y `tec`.

## Recomendacion de produccion

No activar Flyway automaticamente en produccion el mismo dia que se incorpora la
dependencia. Primero validar en una copia de la base productiva:

1. Restaurar backup en pruebas.
2. Desplegar con `tec.flyway.enabled=true` y `baselineOnMigrate=true`.
3. Confirmar que se crea `flyway_schema_history`.
4. Ejecutar una migracion real pequena.
5. Cambiar `hibernate.hbm2ddl.auto` a `validate`.
6. Repetir el despliegue y confirmar que no hay cambios pendientes.
