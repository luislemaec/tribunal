# Importaciones manuales

Esta carpeta contiene guias o scripts no automaticos para datos operativos.

No colocar estos archivos en `src/main/resources/db/migration`, porque Flyway
los ejecutaria automaticamente.

## Datos operativos sensibles

Los archivos `tb_persona.xlsx`, `tb_iglesia.xlsx` y
`tb_iglesia_persona.xlsx` contienen datos reales de personas, iglesias y
relaciones institucionales.

Script preparado:

- `importacion_iglesias_personas.sql`
- `permisos_datasource.sql`: permisos runtime para el usuario de `TribunalDS`.

Este script se conserva como alternativa manual. No ejecutarlo si
`V2__datos_iniciales.sql` ya fue aplicado, porque V2 contiene estos datos.

La numeracion esta normalizada: las antiguas personas `1-7` fueron excluidas
por duplicidad y los IDs restantes se desplazaron restando `7`. Las relaciones
`tb_iglesia_persona.pers_id` usan la misma numeracion.

Para cargar esos datos en un ambiente autorizado:

1. Validar respaldo y autorizacion de tratamiento de datos.
2. Cargar primero `tb_persona`.
3. Cargar despues `tb_iglesia`.
4. Cargar finalmente `tb_iglesia_persona`.
5. Sincronizar secuencias con `setval`.
6. Validar integridad de claves foraneas.

Orden de dependencia:

```text
public.tb_geograp
public.tb_persona
public.tb_iglesia
public.tb_iglesia_persona
```

`public.tb_geograp` si forma parte de Flyway como catalogo base.
