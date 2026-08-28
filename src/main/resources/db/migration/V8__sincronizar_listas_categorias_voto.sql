-- Relacion explicita y normalizacion de categorias de voto por lista y proceso.

ALTER TABLE tec.categoria_voto
    ADD COLUMN IF NOT EXISTS lista_id INTEGER,
    ADD COLUMN IF NOT EXISTS proce_id INTEGER,
    ADD COLUMN IF NOT EXISTS cat_tipo VARCHAR(20) DEFAULT 'LEGACY';

ALTER TABLE tec.categoria_voto_aud
    ADD COLUMN IF NOT EXISTS lista_id INTEGER,
    ADD COLUMN IF NOT EXISTS proce_id INTEGER,
    ADD COLUMN IF NOT EXISTS cat_tipo VARCHAR(20);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_categoria_voto_lista'
           AND conrelid = 'tec.categoria_voto'::regclass
    ) THEN
        ALTER TABLE tec.categoria_voto
            ADD CONSTRAINT fk_categoria_voto_lista
            FOREIGN KEY (lista_id) REFERENCES tec.listas(lista_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_categoria_voto_proceso'
           AND conrelid = 'tec.categoria_voto'::regclass
    ) THEN
        ALTER TABLE tec.categoria_voto
            ADD CONSTRAINT fk_categoria_voto_proceso
            FOREIGN KEY (proce_id) REFERENCES tec.proceso_electoral(proce_id);
    END IF;
END $$;

-- Las categorias no asociadas a listas son universales dentro de cada acta.
UPDATE tec.categoria_voto
   SET cat_tipo = CASE
           WHEN UPPER(TRIM(nombre)) IN ('BLANCOS', 'NULOS', 'PAPELETAS RESTANTES')
               THEN 'ESPECIAL'
           ELSE COALESCE(NULLIF(cat_tipo, ''), 'LEGACY')
       END,
       lista_id = CASE
           WHEN UPPER(TRIM(nombre)) IN ('BLANCOS', 'NULOS', 'PAPELETAS RESTANTES')
               THEN NULL ELSE lista_id
       END,
       proce_id = CASE
           WHEN UPPER(TRIM(nombre)) IN ('BLANCOS', 'NULOS', 'PAPELETAS RESTANTES')
               THEN NULL ELSE proce_id
       END;

UPDATE tec.categoria_voto
   SET cat_orden = CASE UPPER(TRIM(nombre))
           WHEN 'BLANCOS' THEN 10001
           WHEN 'NULOS' THEN 10002
           WHEN 'PAPELETAS RESTANTES' THEN 10003
           ELSE cat_orden
       END
 WHERE cat_tipo = 'ESPECIAL';

-- Reutiliza categorias legacy solo cuando la lista y el proceso son univocos.
WITH coincidencias AS (
    SELECT cv.cat_voto_id,
           l.lista_id,
           ROW_NUMBER() OVER (
               PARTITION BY cv.cat_voto_id
               ORDER BY l.estado DESC, l.lista_id
           ) AS rn
      FROM tec.categoria_voto cv
      JOIN tec.listas l
        ON UPPER(TRIM(cv.nombre)) = UPPER(TRIM(l.lista_nombre))
        OR UPPER(TRIM(cv.nombre)) = UPPER('LISTA ' || TRIM(l.lista_numero))
        OR UPPER(TRIM(cv.nombre)) = UPPER(
               'LISTA ' || TRIM(l.lista_numero) || ' - ' || TRIM(l.lista_nombre)
           )
     WHERE cv.cat_tipo = 'LEGACY'
), estadisticas AS (
    SELECT c.cat_voto_id,
           c.lista_id,
           COUNT(DISTINCT e.proce_id) AS procesos_escrutinio,
           MIN(e.proce_id) AS proceso_escrutinio,
           COUNT(DISTINCT can.proce_id) AS procesos_candidato,
           MIN(can.proce_id) AS proceso_candidato
      FROM coincidencias c
      LEFT JOIN tec.escrutinio e
        ON e.cat_voto_id = c.cat_voto_id
      LEFT JOIN tec.candidatos can
        ON can.lista_id = c.lista_id
     WHERE c.rn = 1
     GROUP BY c.cat_voto_id, c.lista_id
), proceso_activo AS (
    SELECT MIN(proce_id) AS proce_id
      FROM tec.proceso_electoral
     WHERE estado = TRUE AND proce_activo = TRUE
    HAVING COUNT(*) = 1
), resueltas AS (
    SELECT e.cat_voto_id,
           e.lista_id,
           CASE
               WHEN e.procesos_escrutinio = 1 THEN e.proceso_escrutinio
               WHEN e.procesos_escrutinio > 1 THEN NULL
               WHEN e.procesos_candidato = 1 THEN e.proceso_candidato
               WHEN e.procesos_candidato > 1 THEN NULL
               ELSE pa.proce_id
           END AS proce_id
      FROM estadisticas e
      LEFT JOIN proceso_activo pa ON TRUE
)
UPDATE tec.categoria_voto cv
   SET lista_id = r.lista_id,
       proce_id = r.proce_id,
       cat_tipo = 'LISTA'
  FROM resueltas r
 WHERE cv.cat_voto_id = r.cat_voto_id
   AND r.proce_id IS NOT NULL;

-- Contextos historicos de candidaturas y contexto del proceso actualmente activo.
WITH contextos AS (
    SELECT DISTINCT c.lista_id, c.proce_id
      FROM tec.candidatos c
     WHERE c.lista_id IS NOT NULL AND c.proce_id IS NOT NULL
    UNION
    SELECT l.lista_id, p.proce_id
      FROM tec.listas l
      JOIN tec.proceso_electoral p
        ON p.estado = TRUE AND p.proce_activo = TRUE
     WHERE l.estado = TRUE
)
INSERT INTO tec.categoria_voto (
    nombre, categoriavoto, cat_orden, cat_tipo, lista_id, proce_id,
    estado, f_crea, u_crea
)
SELECT 'LISTA ' || TRIM(l.lista_numero)
           || CASE WHEN NULLIF(TRIM(l.lista_nombre), '') IS NULL
                   THEN '' ELSE ' - ' || TRIM(l.lista_nombre) END,
       l.lista_id,
       COALESCE(NULLIF(REGEXP_REPLACE(l.lista_numero, '[^0-9]', '', 'g'), '')::INTEGER,
                1000 + l.lista_id),
       'LISTA', l.lista_id, c.proce_id,
       l.estado, NOW(), 'flyway'
  FROM contextos c
  JOIN tec.listas l ON l.lista_id = c.lista_id
 WHERE NOT EXISTS (
       SELECT 1
         FROM tec.categoria_voto cv
        WHERE cv.lista_id = c.lista_id
          AND cv.proce_id = c.proce_id
          AND cv.cat_tipo = 'LISTA'
 );

-- Actualiza datos derivados de la lista sin alterar las categorias especiales.
UPDATE tec.categoria_voto cv
   SET nombre = 'LISTA ' || TRIM(l.lista_numero)
           || CASE WHEN NULLIF(TRIM(l.lista_nombre), '') IS NULL
                   THEN '' ELSE ' - ' || TRIM(l.lista_nombre) END,
       categoriavoto = l.lista_id,
       cat_orden = COALESCE(
           NULLIF(REGEXP_REPLACE(l.lista_numero, '[^0-9]', '', 'g'), '')::INTEGER,
           1000 + l.lista_id
       ),
       estado = CASE WHEN l.estado = TRUE THEN cv.estado ELSE FALSE END
  FROM tec.listas l
 WHERE cv.lista_id = l.lista_id
   AND cv.cat_tipo = 'LISTA';

-- Conserva una categoria canonica por Lista-Proceso y da de baja duplicados.
WITH ordenadas AS (
    SELECT cv.cat_voto_id,
           ROW_NUMBER() OVER (
               PARTITION BY cv.lista_id, cv.proce_id
               ORDER BY
                   EXISTS (
                       SELECT 1 FROM tec.escrutinio e
                        WHERE e.cat_voto_id = cv.cat_voto_id
                   ) DESC,
                   cv.estado DESC,
                   cv.cat_voto_id
           ) AS rn
      FROM tec.categoria_voto cv
     WHERE cv.cat_tipo = 'LISTA'
       AND cv.lista_id IS NOT NULL
       AND cv.proce_id IS NOT NULL
)
UPDATE tec.categoria_voto cv
   SET estado = FALSE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
  FROM ordenadas o
 WHERE cv.cat_voto_id = o.cat_voto_id
   AND o.rn > 1;

-- Reactiva la misma categoria canonica de cada lista vigente en el proceso activo.
WITH ordenadas AS (
    SELECT cv.cat_voto_id,
           ROW_NUMBER() OVER (
               PARTITION BY cv.lista_id, cv.proce_id
               ORDER BY
                   EXISTS (
                       SELECT 1 FROM tec.escrutinio e
                        WHERE e.cat_voto_id = cv.cat_voto_id
                   ) DESC,
                   cv.estado DESC,
                   cv.cat_voto_id
           ) AS rn
      FROM tec.categoria_voto cv
      JOIN tec.listas l ON l.lista_id = cv.lista_id AND l.estado = TRUE
      JOIN tec.proceso_electoral p
        ON p.proce_id = cv.proce_id AND p.estado = TRUE AND p.proce_activo = TRUE
     WHERE cv.cat_tipo = 'LISTA'
)
UPDATE tec.categoria_voto cv
   SET estado = TRUE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
  FROM ordenadas o
 WHERE cv.cat_voto_id = o.cat_voto_id
   AND o.rn = 1;

-- Evita categorias especiales repetidas sin borrar las referencias historicas.
WITH especiales AS (
    SELECT cv.cat_voto_id,
           ROW_NUMBER() OVER (
               PARTITION BY UPPER(TRIM(cv.nombre))
               ORDER BY
                   EXISTS (
                       SELECT 1 FROM tec.escrutinio e
                        WHERE e.cat_voto_id = cv.cat_voto_id
                   ) DESC,
                   cv.estado DESC,
                   cv.cat_voto_id
           ) AS rn
      FROM tec.categoria_voto cv
     WHERE cv.cat_tipo = 'ESPECIAL'
)
UPDATE tec.categoria_voto cv
   SET estado = FALSE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
  FROM especiales e
 WHERE cv.cat_voto_id = e.cat_voto_id
   AND e.rn > 1;

WITH especiales AS (
    SELECT cv.cat_voto_id,
           ROW_NUMBER() OVER (
               PARTITION BY UPPER(TRIM(cv.nombre))
               ORDER BY
                   EXISTS (
                       SELECT 1 FROM tec.escrutinio e
                        WHERE e.cat_voto_id = cv.cat_voto_id
                   ) DESC,
                   cv.estado DESC,
                   cv.cat_voto_id
           ) AS rn
      FROM tec.categoria_voto cv
     WHERE cv.cat_tipo = 'ESPECIAL'
)
UPDATE tec.categoria_voto cv
   SET estado = TRUE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
  FROM especiales e
 WHERE cv.cat_voto_id = e.cat_voto_id
   AND e.rn = 1;

-- Una categoria de lista sin ambas relaciones no puede participar en actas nuevas.
UPDATE tec.categoria_voto
   SET cat_tipo = 'LEGACY',
       estado = FALSE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
 WHERE cat_tipo = 'LISTA'
   AND (lista_id IS NULL OR proce_id IS NULL);

-- Legacy sin uso permanece registrado pero no participa en actas nuevas.
UPDATE tec.categoria_voto cv
   SET estado = FALSE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
 WHERE cv.cat_tipo = 'LEGACY'
   AND NOT EXISTS (
       SELECT 1 FROM tec.escrutinio e WHERE e.cat_voto_id = cv.cat_voto_id
   );

UPDATE tec.categoria_voto
   SET cat_tipo = 'LEGACY'
 WHERE cat_tipo IS NULL OR TRIM(cat_tipo) = '';

ALTER TABLE tec.categoria_voto
    ALTER COLUMN cat_tipo SET DEFAULT 'LEGACY',
    ALTER COLUMN cat_tipo SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_categoria_voto_proceso_tipo_estado
    ON tec.categoria_voto (proce_id, cat_tipo, estado);

CREATE INDEX IF NOT EXISTS idx_categoria_voto_lista_proceso
    ON tec.categoria_voto (lista_id, proce_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_categoria_voto_lista_proceso_activa
    ON tec.categoria_voto (lista_id, proce_id)
    WHERE estado = TRUE
      AND cat_tipo = 'LISTA'
      AND lista_id IS NOT NULL
      AND proce_id IS NOT NULL;
