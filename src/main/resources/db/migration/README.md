# Migraciones Flyway TEC

Ubicacion estandar: `src/main/resources/db/migration`.

Convencion:

- `V1__descripcion.sql`
- `V2__descripcion.sql`
- `V3__descripcion.sql`

Reglas operativas:

- No ejecutar migraciones en produccion sin respaldo verificado.
- Usar `hibernate.hbm2ddl.auto=none` cuando Flyway se ejecuta al iniciar el WAR.
- Si se usa `validate`, ejecutar Flyway externamente antes de desplegar el WAR.
- Para una base existente, usar primero baseline controlado y documentado.
- `V1__baseline_inicial.sql` es solo para bases nuevas.
- La secuencia consolidada para una base limpia en desarrollo es: V1 estructura completa, V2 datos iniciales definitivos y V3 normalizacion de total de votos en mesas.
- En una base existente usar `baselineOnMigrate=true` y `baselineVersion=1`; no ejecutar V1.
- `V2__datos_iniciales.sql` carga catalogos, personas, iglesias, el usuario administrador BCrypt, tipos documentales, categorias de voto normalizadas, proceso, cronograma, recintos, mesas, plantillas, menus y relaciones iniciales autorizadas para una base nueva.
- V2 contiene datos sensibles; restringir acceso al repositorio y al WAR generado.
- Si se usa baseline en produccion con una base existente, iniciar los cambios
  nuevos despues de las semillas ya aplicadas y validar si V2 debe ejecutarse o
  marcarse como aplicada.
- Los scripts deben ser idempotentes cuando sea posible y usar nombres explicitos de constraints e indices.

Activacion por arranque del WAR:

```bash
-Dtec.flyway.enabled=true
-Dtec.flyway.baselineOnMigrate=false
-Dtec.flyway.locations=classpath:db/migration
-Dtec.flyway.defaultSchema=public
```

Para una base existente, despues de respaldo y validacion:

```bash
-Dtec.flyway.enabled=true
-Dtec.flyway.baselineOnMigrate=true
-Dtec.flyway.baselineVersion=1
```
