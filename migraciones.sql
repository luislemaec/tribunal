-- 1) Crear el rol Tribunal
INSERT INTO tb_rol (rol_nombre, rol_description, estado, f_crea, u_crea)
VALUES ('SITEC-Tribunal', 'Tribunal Electoral (cronograma, resoluciones)', TRUE, NOW(), 'admin');

-- 2) Registrar el menú (si quieres exponer la pantalla)
INSERT INTO tb_menu (menu_nombre, menu_url, menu_ico, estado, f_crea, u_crea)
VALUES ('Cronograma', '/cronograma.jsf', 'pi pi-calendar', TRUE, NOW(), 'admin');

-- 3) Asociar el menú al rol
INSERT INTO tb_menu_rol (rol_id, menu_id, estado, f_crea, u_crea)
SELECT r.rol_id, m.menu_id, TRUE, NOW(), 'admin'
FROM tb_rol r, tb_menu m
WHERE r.rol_nombre IN ('SITEC-Administrador', 'SITEC-Tribunal')
AND m.menu_nombre = 'Cronograma';

-- 4) Crear el primer proceso electoral
INSERT INTO tec.proceso_electoral
  (proce_nombre, proce_descripcion, proce_fecha_inicio, proce_fecha_fin, proce_activo, estado, f_crea, u_crea)
VALUES ('Elecciones 2026', 'Proceso electoral 2026', '2026-01-01', '2026-12-31', TRUE, TRUE, NOW(), 'admin');

-- 5) (Alternativa al paso 4) crear el proceso desde la nueva pantalla /cronograma.jsf

-- ============================================================================
-- 6) MIGRACIÓN: codificación genérica de iglesias (secuencia + backfill + UNIQUE)
-- ----------------------------------------------------------------------------
-- Contexto: IglesiaFacade.generarDocumentoGenerico() ahora usa la secuencia
--           seq_iglesia_codigo_generico (creada lazy por la app en el primer uso),
--           y el discriminador en Java/XHTML pasó de "000000000000" (12 ceros, bug
--           que rompía a partir del código #10) a "00" (2 ceros, RUC reales nunca
--           empiezan con 00 — provincias 01-24).
--
-- Ejecutar UNA SOLA VEZ, después de redesplegar la app con la nueva lógica.
-- Pasos 6.a / 6.b / 6.e son diagnóstico (read-only). 6.c / 6.d / 6.f modifican datos.
-- ============================================================================

-- 6.a) Iglesias sin documento (debe quedar en 0 tras el backfill 6.c).
SELECT COUNT(*) AS iglesias_sin_documento
  FROM public.tb_iglesia
 WHERE igl_documento IS NULL OR TRIM(igl_documento) = '';

-- 6.b) Diagnóstico previo: cualquier documento duplicado (reales o genéricos).
SELECT igl_documento, COUNT(*) AS repeticiones, ARRAY_AGG(id ORDER BY id) AS ids
  FROM public.tb_iglesia
 WHERE igl_documento IS NOT NULL AND TRIM(igl_documento) <> ''
 GROUP BY igl_documento
HAVING COUNT(*) > 1;

-- 6.c) Backfill: asignar código genérico desde la secuencia a iglesias sin documento.
--      LPAD(...,13,'0') reproduce el formato String.format("%013d", n) del Java.
--      Si la secuencia aún no existe (la app la crea lazy en el primer toggle
--      "No tiene RUC"), descomentá el DO siguiente para crearla aquí.
/*
DO $$
DECLARE max_val BIGINT;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relkind='S' AND relname='seq_iglesia_codigo_generico') THEN
    SELECT COALESCE(MAX(CAST(igl_documento AS BIGINT)), 0) INTO max_val
      FROM public.tb_iglesia
     WHERE igl_documento ~ '^00[0-9]{11}$';
    EXECUTE format('CREATE SEQUENCE seq_iglesia_codigo_generico START %s', max_val + 1);
  END IF;
END $$;
*/
UPDATE public.tb_iglesia
   SET igl_documento = LPAD(nextval('seq_iglesia_codigo_generico')::text, 13, '0')
 WHERE igl_documento IS NULL OR TRIM(igl_documento) = '';

-- 6.d) Resolver duplicados de códigos genéricos (causa: bug del prefijo previo).
--      Conserva el id menor; al resto le reasigna un código nuevo desde la secuencia.
WITH dups AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY igl_documento ORDER BY id) AS rn
      FROM public.tb_iglesia
     WHERE igl_documento ~ '^00[0-9]{11}$'
)
UPDATE public.tb_iglesia ig
   SET igl_documento = LPAD(nextval('seq_iglesia_codigo_generico')::text, 13, '0')
  FROM dups d
 WHERE ig.id = d.id AND d.rn > 1;

-- 6.e) Verificación: debe retornar 0 filas. Si retorna duplicados, son RUCs
--      reales que requieren revisión manual antes de aplicar el constraint 6.f.
SELECT igl_documento, COUNT(*) AS repeticiones, ARRAY_AGG(id ORDER BY id) AS ids
  FROM public.tb_iglesia
 WHERE igl_documento IS NOT NULL
 GROUP BY igl_documento
HAVING COUNT(*) > 1;

-- 6.f) Constraint UNIQUE como red de seguridad ante inserciones manuales o
--      futuros bugs. Sólo ejecutar si 6.e retornó 0 filas.
ALTER TABLE public.tb_iglesia
  ADD CONSTRAINT uk_iglesia_documento UNIQUE (igl_documento);

-- ============================================================================
-- 7) MENÚ: Asignación de Usuarios a Iglesias
-- ----------------------------------------------------------------------------
-- Registra la entrada de menú para la nueva pantalla /asignacionUsuarios.jsf.
-- Solo asigna acceso a SITEC-Administrador y SITEC-Tribunal por defecto.
-- ============================================================================

-- 7.a) Registrar el menú (idempotente: usa NOT EXISTS para no duplicar).
INSERT INTO tb_menu (menu_nombre, menu_url, menu_ico, estado, f_crea, u_crea)
SELECT 'Asignación de Usuarios', '/asignacionUsuarios.jsf', 'pi pi-users', TRUE, NOW(), 'admin'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_menu WHERE menu_url = '/asignacionUsuarios.jsf'
);

-- 7.b) Asociar el menú a los roles que pueden gestionar la asignación.
INSERT INTO tb_menu_rol (rol_id, menu_id, estado, f_crea, u_crea)
SELECT r.rol_id, m.menu_id, TRUE, NOW(), 'admin'
  FROM tb_rol r, tb_menu m
 WHERE r.rol_nombre IN ('SITEC-Administrador', 'SITEC-Tribunal')
   AND m.menu_url = '/asignacionUsuarios.jsf'
   AND NOT EXISTS (
        SELECT 1 FROM tb_menu_rol mr
         WHERE mr.rol_id = r.rol_id AND mr.menu_id = m.menu_id
   );
