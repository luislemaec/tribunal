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

-- ============================================================================
-- 8) MIGRACION: ProcesoElectoral como eje funcional del sistema
-- ----------------------------------------------------------------------------
-- Objetivo:
--   - Mantener los datos historicos de tec.periodos (incluido 2022).
--   - Crear un proceso electoral equivalente por cada periodo existente.
--   - Agregar proce_id a tablas electorales que actualmente dependen de periodo_id.
--   - Poblar proce_id desde periodo_id sin eliminar columnas legacy.
--
-- Nota:
--   No retirar periodo_id todavia. Esa limpieza debe hacerse solo cuando toda
--   la aplicacion y reportes hayan sido validados usando proce_id.
-- ============================================================================

-- 8.a) Asegurar tabla de procesos electorales.
CREATE TABLE IF NOT EXISTS tec.proceso_electoral (
    proce_id SERIAL PRIMARY KEY,
    proce_nombre VARCHAR(150) NOT NULL,
    proce_descripcion VARCHAR(500),
    proce_fecha_inicio TIMESTAMP,
    proce_fecha_fin TIMESTAMP,
    proce_activo BOOLEAN DEFAULT FALSE,
    estado BOOLEAN DEFAULT TRUE,
    f_crea TIMESTAMP DEFAULT NOW(),
    f_actualiza TIMESTAMP,
    u_crea VARCHAR(100),
    u_actualiza VARCHAR(100)
);

-- 8.b) Migrar periodos existentes a procesos electorales equivalentes.
INSERT INTO tec.proceso_electoral (
    proce_nombre,
    proce_descripcion,
    proce_fecha_inicio,
    proce_fecha_fin,
    proce_activo,
    estado,
    f_crea,
    u_crea
)
SELECT p.periodo_nombre,
       p.periodo_descripcion,
       p.f_inicio,
       p.f_fin,
       FALSE,
       COALESCE(p.estado, TRUE),
       NOW(),
       'migracion-periodo'
  FROM tec.periodos p
 WHERE NOT EXISTS (
        SELECT 1
          FROM tec.proceso_electoral pe
         WHERE pe.proce_nombre = p.periodo_nombre
           AND COALESCE(pe.proce_fecha_inicio, TIMESTAMP '1900-01-01')
               = COALESCE(p.f_inicio, TIMESTAMP '1900-01-01')
           AND COALESCE(pe.proce_fecha_fin, TIMESTAMP '1900-01-01')
               = COALESCE(p.f_fin, TIMESTAMP '1900-01-01')
   );

-- 8.c) Si no hay proceso activo, activar el mas reciente migrado.
UPDATE tec.proceso_electoral pe
   SET proce_activo = TRUE
 WHERE pe.proce_id = (
       SELECT pe2.proce_id
         FROM tec.proceso_electoral pe2
        WHERE COALESCE(pe2.estado, TRUE) = TRUE
        ORDER BY pe2.proce_fecha_inicio DESC NULLS LAST, pe2.proce_id DESC
        LIMIT 1
 )
   AND NOT EXISTS (
       SELECT 1 FROM tec.proceso_electoral activo
        WHERE activo.proce_activo = TRUE AND COALESCE(activo.estado, TRUE) = TRUE
   );

-- 8.d) Agregar proce_id a tablas electorales.
ALTER TABLE tec.padron ADD COLUMN IF NOT EXISTS proce_id INTEGER;
ALTER TABLE tec.candidatos ADD COLUMN IF NOT EXISTS proce_id INTEGER;
ALTER TABLE tec.escrutinio ADD COLUMN IF NOT EXISTS proce_id INTEGER;
ALTER TABLE tec.miembros_jrv ADD COLUMN IF NOT EXISTS proce_id INTEGER;
ALTER TABLE tec.tribunal ADD COLUMN IF NOT EXISTS proce_id INTEGER;

-- 8.e) Poblar proce_id desde periodo_id buscando el proceso equivalente.
--      En la BD actual, tec.candidatos, tec.miembros_jrv y tec.tribunal
--      estaban vacias antes de esta migracion; sus UPDATE quedan como
--      seguridad/idempotencia para otros ambientes o restauraciones historicas.
UPDATE tec.padron t
   SET proce_id = pe.proce_id
  FROM tec.periodos p
  JOIN tec.proceso_electoral pe
    ON pe.proce_nombre = p.periodo_nombre
   AND COALESCE(pe.proce_fecha_inicio, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_inicio, TIMESTAMP '1900-01-01')
   AND COALESCE(pe.proce_fecha_fin, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_fin, TIMESTAMP '1900-01-01')
 WHERE t.periodo_id = p.periodo_id
   AND t.proce_id IS NULL;

UPDATE tec.candidatos t
   SET proce_id = pe.proce_id
  FROM tec.periodos p
  JOIN tec.proceso_electoral pe
    ON pe.proce_nombre = p.periodo_nombre
   AND COALESCE(pe.proce_fecha_inicio, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_inicio, TIMESTAMP '1900-01-01')
   AND COALESCE(pe.proce_fecha_fin, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_fin, TIMESTAMP '1900-01-01')
 WHERE t.periodo_id = p.periodo_id
   AND t.proce_id IS NULL;

UPDATE tec.escrutinio t
   SET proce_id = pe.proce_id
  FROM tec.periodos p
  JOIN tec.proceso_electoral pe
    ON pe.proce_nombre = p.periodo_nombre
   AND COALESCE(pe.proce_fecha_inicio, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_inicio, TIMESTAMP '1900-01-01')
   AND COALESCE(pe.proce_fecha_fin, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_fin, TIMESTAMP '1900-01-01')
 WHERE t.periodo_id = p.periodo_id
   AND t.proce_id IS NULL;

UPDATE tec.miembros_jrv t
   SET proce_id = pe.proce_id
  FROM tec.periodos p
  JOIN tec.proceso_electoral pe
    ON pe.proce_nombre = p.periodo_nombre
   AND COALESCE(pe.proce_fecha_inicio, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_inicio, TIMESTAMP '1900-01-01')
   AND COALESCE(pe.proce_fecha_fin, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_fin, TIMESTAMP '1900-01-01')
 WHERE t.periodo_id = p.periodo_id
   AND t.proce_id IS NULL;

UPDATE tec.tribunal t
   SET proce_id = pe.proce_id
  FROM tec.periodos p
  JOIN tec.proceso_electoral pe
    ON pe.proce_nombre = p.periodo_nombre
   AND COALESCE(pe.proce_fecha_inicio, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_inicio, TIMESTAMP '1900-01-01')
   AND COALESCE(pe.proce_fecha_fin, TIMESTAMP '1900-01-01')
       = COALESCE(p.f_fin, TIMESTAMP '1900-01-01')
 WHERE t.periodo_id = p.periodo_id
   AND t.proce_id IS NULL;

-- 8.e.1) Padron existente con proce_id = 2.
--        En la BD actual ya existen registros de tec.padron asociados al
--        proceso electoral 2. La migracion debe conservar esa asignacion y
--        validar que el proceso exista antes de crear la FK.
SELECT COUNT(*) AS registros_padron_proceso_2
  FROM tec.padron
 WHERE proce_id = 2;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM tec.padron WHERE proce_id = 2)
     AND NOT EXISTS (SELECT 1 FROM tec.proceso_electoral WHERE proce_id = 2) THEN
    RAISE EXCEPTION
      'Existen registros en tec.padron con proce_id=2, pero no existe tec.proceso_electoral.proce_id=2';
  END IF;
END $$;

-- Fallback controlado: si luego del cruce por periodo_id quedan registros del
-- padron sin proceso y el proceso 2 existe, se asignan al proceso 2 porque es
-- el proceso ya utilizado por el padron actual.
UPDATE tec.padron
   SET proce_id = 2
 WHERE proce_id IS NULL
   AND EXISTS (SELECT 1 FROM tec.proceso_electoral WHERE proce_id = 2);

-- Diagnostico final: debe retornar 0 antes de marcar proce_id como obligatorio.
SELECT COUNT(*) AS registros_padron_sin_proceso
  FROM tec.padron
 WHERE proce_id IS NULL;

-- 8.f) FKs e indices para la nueva relacion funcional.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_padron_proceso_electoral') THEN
    ALTER TABLE tec.padron
      ADD CONSTRAINT fk_padron_proceso_electoral
      FOREIGN KEY (proce_id) REFERENCES tec.proceso_electoral(proce_id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_candidatos_proceso_electoral') THEN
    ALTER TABLE tec.candidatos
      ADD CONSTRAINT fk_candidatos_proceso_electoral
      FOREIGN KEY (proce_id) REFERENCES tec.proceso_electoral(proce_id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_escrutinio_proceso_electoral') THEN
    ALTER TABLE tec.escrutinio
      ADD CONSTRAINT fk_escrutinio_proceso_electoral
      FOREIGN KEY (proce_id) REFERENCES tec.proceso_electoral(proce_id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_miembros_jrv_proceso_electoral') THEN
    ALTER TABLE tec.miembros_jrv
      ADD CONSTRAINT fk_miembros_jrv_proceso_electoral
      FOREIGN KEY (proce_id) REFERENCES tec.proceso_electoral(proce_id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tribunal_proceso_electoral') THEN
    ALTER TABLE tec.tribunal
      ADD CONSTRAINT fk_tribunal_proceso_electoral
      FOREIGN KEY (proce_id) REFERENCES tec.proceso_electoral(proce_id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_padron_proce_id ON tec.padron(proce_id);
CREATE INDEX IF NOT EXISTS idx_candidatos_proce_id ON tec.candidatos(proce_id);
CREATE INDEX IF NOT EXISTS idx_escrutinio_proce_id ON tec.escrutinio(proce_id);
CREATE INDEX IF NOT EXISTS idx_miembros_jrv_proce_id ON tec.miembros_jrv(proce_id);
CREATE INDEX IF NOT EXISTS idx_tribunal_proce_id ON tec.tribunal(proce_id);

-- ============================================================================
-- 9) BUENAS PRACTICAS JPA/BD: nucleo del proceso electoral
-- ----------------------------------------------------------------------------
-- Alinea la BD con las anotaciones agregadas en las entidades del nucleo:
--   - ProcesoElectoral
--   - CronogramaFase
--   - Padron
--   - Candidato
--   - Escrutinio
--   - MiembroJRV
--   - Tribunal
--
-- Los constraints UNIQUE se crean solo si no hay duplicados existentes.
-- Si se omite alguno por datos repetidos, revisar el SELECT diagnostico
-- correspondiente antes de volver a ejecutar.
-- ============================================================================

-- 9.a) Defaults y campos obligatorios simples.
UPDATE tec.proceso_electoral SET proce_activo = FALSE WHERE proce_activo IS NULL;
ALTER TABLE tec.proceso_electoral ALTER COLUMN proce_activo SET DEFAULT FALSE;
ALTER TABLE tec.proceso_electoral ALTER COLUMN proce_activo SET NOT NULL;

UPDATE tec.padron SET sufrago = FALSE WHERE sufrago IS NULL;
ALTER TABLE tec.padron ALTER COLUMN sufrago SET DEFAULT FALSE;
ALTER TABLE tec.padron ALTER COLUMN sufrago SET NOT NULL;

UPDATE tec.escrutinio SET total_votos = 0 WHERE total_votos IS NULL;
ALTER TABLE tec.escrutinio ALTER COLUMN total_votos SET DEFAULT 0;
ALTER TABLE tec.escrutinio ALTER COLUMN total_votos SET NOT NULL;

-- 9.b) Indices funcionales del nucleo.
CREATE INDEX IF NOT EXISTS idx_proceso_electoral_activo ON tec.proceso_electoral(proce_activo);
CREATE INDEX IF NOT EXISTS idx_proceso_electoral_fechas ON tec.proceso_electoral(proce_fecha_inicio, proce_fecha_fin);

CREATE INDEX IF NOT EXISTS idx_cronograma_fase_proce_id ON tec.cronograma_fase(proce_id);
CREATE INDEX IF NOT EXISTS idx_cronograma_fase_fechas ON tec.cronograma_fase(cref_fecha_inicio, cref_fecha_fin);

CREATE INDEX IF NOT EXISTS idx_padron_mesa_id ON tec.padron(mesa_id);
CREATE INDEX IF NOT EXISTS idx_padron_igpe_id ON tec.padron(igpe_id);
CREATE INDEX IF NOT EXISTS idx_padron_proceso_mesa ON tec.padron(proce_id, mesa_id);

CREATE INDEX IF NOT EXISTS idx_candidatos_lista_id ON tec.candidatos(lista_id);
CREATE INDEX IF NOT EXISTS idx_candidatos_cargo_id ON tec.candidatos(cargo_id);
CREATE INDEX IF NOT EXISTS idx_candidatos_igpe_id ON tec.candidatos(igpe_id);

CREATE INDEX IF NOT EXISTS idx_escrutinio_mesa_id ON tec.escrutinio(mesa_id);
CREATE INDEX IF NOT EXISTS idx_escrutinio_categoria_id ON tec.escrutinio(cat_voto_id);

CREATE INDEX IF NOT EXISTS idx_miembros_jrv_mesa_id ON tec.miembros_jrv(mesa_id);
CREATE INDEX IF NOT EXISTS idx_miembros_jrv_igpe_id ON tec.miembros_jrv(igpe_id);
CREATE INDEX IF NOT EXISTS idx_miembros_jrv_proceso_igpe ON tec.miembros_jrv(proce_id, igpe_id);

CREATE INDEX IF NOT EXISTS idx_tribunal_cargo_id ON tec.tribunal(cargo_id);
CREATE INDEX IF NOT EXISTS idx_tribunal_igpe_id ON tec.tribunal(igpe_id);

-- 9.c) Unicidad de reglas de negocio.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_proceso_electoral_nombre')
     AND NOT EXISTS (
          SELECT 1
            FROM tec.proceso_electoral
           WHERE proce_nombre IS NOT NULL
           GROUP BY proce_nombre
          HAVING COUNT(*) > 1
     ) THEN
    ALTER TABLE tec.proceso_electoral
      ADD CONSTRAINT uk_proceso_electoral_nombre UNIQUE (proce_nombre);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_cronograma_fase_proceso_fase')
     AND NOT EXISTS (
          SELECT 1
            FROM tec.cronograma_fase
           WHERE proce_id IS NOT NULL AND cref_fase IS NOT NULL
           GROUP BY proce_id, cref_fase
          HAVING COUNT(*) > 1
     ) THEN
    ALTER TABLE tec.cronograma_fase
      ADD CONSTRAINT uk_cronograma_fase_proceso_fase UNIQUE (proce_id, cref_fase);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_padron_proceso_iglesia_persona')
     AND NOT EXISTS (
          SELECT 1
            FROM tec.padron
           WHERE proce_id IS NOT NULL AND igpe_id IS NOT NULL
           GROUP BY proce_id, igpe_id
          HAVING COUNT(*) > 1
     ) THEN
    ALTER TABLE tec.padron
      ADD CONSTRAINT uk_padron_proceso_iglesia_persona UNIQUE (proce_id, igpe_id);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_candidatos_proceso_lista_cargo_persona')
     AND NOT EXISTS (
          SELECT 1
            FROM tec.candidatos
           WHERE proce_id IS NOT NULL AND lista_id IS NOT NULL AND cargo_id IS NOT NULL AND igpe_id IS NOT NULL
           GROUP BY proce_id, lista_id, cargo_id, igpe_id
          HAVING COUNT(*) > 1
     ) THEN
    ALTER TABLE tec.candidatos
      ADD CONSTRAINT uk_candidatos_proceso_lista_cargo_persona UNIQUE (proce_id, lista_id, cargo_id, igpe_id);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_escrutinio_proceso_mesa_categoria')
     AND NOT EXISTS (
          SELECT 1
            FROM tec.escrutinio
           WHERE proce_id IS NOT NULL AND mesa_id IS NOT NULL AND cat_voto_id IS NOT NULL
           GROUP BY proce_id, mesa_id, cat_voto_id
          HAVING COUNT(*) > 1
     ) THEN
    ALTER TABLE tec.escrutinio
      ADD CONSTRAINT uk_escrutinio_proceso_mesa_categoria UNIQUE (proce_id, mesa_id, cat_voto_id);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_miembros_jrv_proceso_mesa_cargo')
     AND NOT EXISTS (
          SELECT 1
            FROM tec.miembros_jrv
           WHERE proce_id IS NOT NULL AND mesa_id IS NOT NULL AND cargo_id IS NOT NULL
           GROUP BY proce_id, mesa_id, cargo_id
          HAVING COUNT(*) > 1
     ) THEN
    ALTER TABLE tec.miembros_jrv
      ADD CONSTRAINT uk_miembros_jrv_proceso_mesa_cargo UNIQUE (proce_id, mesa_id, cargo_id);
  END IF;
END $$;

-- Regla activa adicional: una persona solo puede pertenecer a una JRV por proceso.
-- Al ser eliminacion logica, la restriccion debe ser parcial sobre estado activo.
DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM pg_indexes
       WHERE schemaname = 'tec'
         AND tablename = 'miembros_jrv'
         AND indexname = 'uk_miembros_jrv_activo_proceso_igpe'
  )
     AND NOT EXISTS (
          SELECT 1
            FROM tec.miembros_jrv
           WHERE proce_id IS NOT NULL
             AND igpe_id IS NOT NULL
             AND COALESCE(estado, TRUE) = TRUE
           GROUP BY proce_id, igpe_id
          HAVING COUNT(*) > 1
     ) THEN
    CREATE UNIQUE INDEX uk_miembros_jrv_activo_proceso_igpe
      ON tec.miembros_jrv(proce_id, igpe_id)
      WHERE COALESCE(estado, TRUE) = TRUE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tribunal_proceso_cargo')
     AND NOT EXISTS (
          SELECT 1
            FROM tec.tribunal
           WHERE proce_id IS NOT NULL AND cargo_id IS NOT NULL
           GROUP BY proce_id, cargo_id
          HAVING COUNT(*) > 1
     ) THEN
    ALTER TABLE tec.tribunal
      ADD CONSTRAINT uk_tribunal_proceso_cargo UNIQUE (proce_id, cargo_id);
  END IF;
END $$;

-- 9.d) Diagnostico de duplicados si algun UNIQUE no se creo.
SELECT proce_nombre, COUNT(*) AS repeticiones
  FROM tec.proceso_electoral
 WHERE proce_nombre IS NOT NULL
 GROUP BY proce_nombre
HAVING COUNT(*) > 1;

SELECT proce_id, igpe_id, COUNT(*) AS repeticiones
  FROM tec.padron
 WHERE proce_id IS NOT NULL AND igpe_id IS NOT NULL
 GROUP BY proce_id, igpe_id
HAVING COUNT(*) > 1;

SELECT proce_id, mesa_id, cat_voto_id, COUNT(*) AS repeticiones
  FROM tec.escrutinio
 WHERE proce_id IS NOT NULL AND mesa_id IS NOT NULL AND cat_voto_id IS NOT NULL
 GROUP BY proce_id, mesa_id, cat_voto_id
HAVING COUNT(*) > 1;

-- ============================================================================
-- 10) SANEAMIENTO: padron solo con miembros habilitados
-- ----------------------------------------------------------------------------
-- Regla de negocio:
--   public.tb_iglesia_persona.igpe_habilitado_padron debe ser TRUE para que
--   una persona sea agregada o permanezca activa en tec.padron.
--
-- FALSE y NULL se consideran NO habilitados.
-- ============================================================================

-- 10.a) Diagnostico: registros activos del padron con miembros no habilitados.
SELECT p.padron_id,
       p.proce_id,
       p.mesa_id,
       p.igpe_id,
       ip.igpe_habilitado_padron
  FROM tec.padron p
  JOIN public.tb_iglesia_persona ip ON ip.igpe_id = p.igpe_id
 WHERE COALESCE(p.estado, TRUE) = TRUE
   AND COALESCE(ip.igpe_habilitado_padron, FALSE) = FALSE
 ORDER BY p.padron_id;

-- 10.b) Desactivar del padron activo a miembros no habilitados.
UPDATE tec.padron p
   SET estado = FALSE,
       f_actualiza = NOW(),
       u_actualiza = 'migracion-habilitado-padron'
  FROM public.tb_iglesia_persona ip
 WHERE ip.igpe_id = p.igpe_id
   AND COALESCE(p.estado, TRUE) = TRUE
   AND COALESCE(ip.igpe_habilitado_padron, FALSE) = FALSE;

-- 10.c) Verificacion: debe retornar 0.
SELECT COUNT(*) AS padron_activo_con_miembros_no_habilitados
  FROM tec.padron p
  JOIN public.tb_iglesia_persona ip ON ip.igpe_id = p.igpe_id
 WHERE COALESCE(p.estado, TRUE) = TRUE
   AND COALESCE(ip.igpe_habilitado_padron, FALSE) = FALSE;

-- ============================================================================
-- 11) RECINTOS: ubicacion geografica obligatoria
-- ----------------------------------------------------------------------------
-- tec.recintos.gelo_id almacena la parroquia (public.tb_geograp.gelo_id),
-- igual que public.tb_iglesia.gelo_id y tec.mesas.gelo_id.
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_recintos_gelo_id ON tec.recintos(gelo_id);

-- Diagnostico: debe retornar 0 antes de marcar la columna como NOT NULL.
SELECT rec_id, rec_nombre
  FROM tec.recintos
 WHERE gelo_id IS NULL
 ORDER BY rec_id;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM tec.recintos WHERE gelo_id IS NULL) THEN
    ALTER TABLE tec.recintos ALTER COLUMN gelo_id SET NOT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM tec.recintos WHERE rec_nombre IS NULL OR TRIM(rec_nombre) = '') THEN
    ALTER TABLE tec.recintos ALTER COLUMN rec_nombre SET NOT NULL;
  END IF;
END $$;

-- ============================================================================
-- 12) ROLES: Presidente de Mesa
-- ----------------------------------------------------------------------------
-- Requerido por el flujo de Miembros JRV. Al completar una junta, el sistema
-- crea o reutiliza el usuario del Presidente y le asigna este rol.
-- ============================================================================

INSERT INTO public.tb_rol (estado, f_crea, u_crea, rol_description, rol_nombre)
SELECT TRUE, NOW(), 'migracion-mjrv', 'Presidente de mesa electoral', 'SITEC-Presidente-mesa'
WHERE NOT EXISTS (
    SELECT 1 FROM public.tb_rol WHERE rol_nombre = 'SITEC-Presidente-mesa'
);

-- ============================================================================
-- 13) ESCRUTINIOS: cabecera normalizada por mesa/proceso
-- ----------------------------------------------------------------------------
-- Se separa la responsabilidad del escrutinio de tec.mesas. La tabla
-- tec.escrutinio conserva el detalle por categoria; esta cabecera concentra
-- estado, fechas, responsable, totales y observaciones del proceso de conteo.
-- No se eliminan columnas legacy de tec.mesas en esta migracion.
-- ============================================================================

CREATE TABLE IF NOT EXISTS tec.escrutinio_cabecera (
    esca_id SERIAL PRIMARY KEY,
    mesa_id INTEGER NOT NULL,
    proce_id INTEGER NOT NULL,
    esca_estado VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE',
    esca_presidente VARCHAR(100),
    esca_fecha_apertura TIMESTAMP,
    esca_fecha_inicio_conteo TIMESTAMP,
    esca_fecha_cierre TIMESTAMP,
    esca_total_sufragantes INTEGER NOT NULL DEFAULT 0,
    esca_total_votos_registrados INTEGER NOT NULL DEFAULT 0,
    esca_total_votos_validos INTEGER NOT NULL DEFAULT 0,
    esca_total_votos_blancos INTEGER NOT NULL DEFAULT 0,
    esca_total_votos_nulos INTEGER NOT NULL DEFAULT 0,
    esca_obs_apertura VARCHAR(1000),
    esca_obs_conteo VARCHAR(1000),
    esca_obs_cierre VARCHAR(1000),
    estado BOOLEAN DEFAULT TRUE,
    f_crea TIMESTAMP DEFAULT NOW(),
    f_actualiza TIMESTAMP,
    u_crea VARCHAR(100),
    u_actualiza VARCHAR(100)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_escrutinio_cabecera_mesa') THEN
    ALTER TABLE tec.escrutinio_cabecera
      ADD CONSTRAINT fk_escrutinio_cabecera_mesa
      FOREIGN KEY (mesa_id) REFERENCES tec.mesas(mesa_id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_escrutinio_cabecera_proceso') THEN
    ALTER TABLE tec.escrutinio_cabecera
      ADD CONSTRAINT fk_escrutinio_cabecera_proceso
      FOREIGN KEY (proce_id) REFERENCES tec.proceso_electoral(proce_id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_escrutinio_cabecera_proceso_mesa') THEN
    ALTER TABLE tec.escrutinio_cabecera
      ADD CONSTRAINT uk_escrutinio_cabecera_proceso_mesa UNIQUE (proce_id, mesa_id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_escrutinio_cabecera_proce_id ON tec.escrutinio_cabecera(proce_id);
CREATE INDEX IF NOT EXISTS idx_escrutinio_cabecera_mesa_id ON tec.escrutinio_cabecera(mesa_id);
CREATE INDEX IF NOT EXISTS idx_escrutinio_cabecera_estado ON tec.escrutinio_cabecera(esca_estado);

-- Backfill desde detalles existentes y columnas legacy de mesa.
INSERT INTO tec.escrutinio_cabecera (
    mesa_id,
    proce_id,
    esca_estado,
    esca_presidente,
    esca_fecha_apertura,
    esca_fecha_inicio_conteo,
    esca_fecha_cierre,
    esca_total_sufragantes,
    esca_total_votos_registrados,
    esca_total_votos_validos,
    esca_total_votos_blancos,
    esca_total_votos_nulos,
    esca_obs_conteo,
    esca_obs_cierre,
    estado,
    f_crea,
    u_crea
)
SELECT e.mesa_id,
       e.proce_id,
       CASE
         WHEN m.estado_tarea = 'COMPLETADO' THEN 'CERRADO'
         WHEN COALESCE(SUM(e.total_votos), 0) > 0 THEN 'CONTEO_REGISTRADO'
         ELSE 'PENDIENTE'
       END,
       m.u_responsable,
       NULL,
       CASE WHEN COALESCE(SUM(e.total_votos), 0) > 0 THEN NOW() ELSE NULL END,
       CASE WHEN m.estado_tarea = 'COMPLETADO' THEN COALESCE(m.f_actualiza, NOW()) ELSE NULL END,
       COALESCE(m.totalvotos, 0),
       COALESCE(SUM(e.total_votos), 0),
       COALESCE(SUM(CASE
           WHEN UPPER(COALESCE(cv.nombre, '')) LIKE '%BLANCO%' THEN 0
           WHEN UPPER(COALESCE(cv.nombre, '')) LIKE '%NULO%' THEN 0
           ELSE e.total_votos
       END), 0),
       COALESCE(SUM(CASE WHEN UPPER(COALESCE(cv.nombre, '')) LIKE '%BLANCO%' THEN e.total_votos ELSE 0 END), 0),
       COALESCE(SUM(CASE WHEN UPPER(COALESCE(cv.nombre, '')) LIKE '%NULO%' THEN e.total_votos ELSE 0 END), 0),
       m.observacion,
       CASE WHEN m.estado_tarea = 'COMPLETADO' THEN m.observacion ELSE NULL END,
       TRUE,
       NOW(),
       'migracion-escrutinio-cabecera'
  FROM tec.escrutinio e
  JOIN tec.mesas m ON m.mesa_id = e.mesa_id
  LEFT JOIN tec.categoria_voto cv ON cv.cat_voto_id = e.cat_voto_id
 WHERE e.proce_id IS NOT NULL
 GROUP BY e.mesa_id, e.proce_id, m.estado_tarea, m.u_responsable, m.f_actualiza,
          m.totalvotos, m.observacion
ON CONFLICT (proce_id, mesa_id) DO NOTHING;
