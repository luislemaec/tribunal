-- Actas de inscripcion de listas: tipos documentales y trazabilidad.

INSERT INTO tec.tipo_documentos
    (tipdoc_nombre, estado, f_crea, u_crea)
SELECT 'ACTA DE INSCRIPCION GENERADA', TRUE, NOW(), 'flyway'
WHERE NOT EXISTS (
    SELECT 1 FROM tec.tipo_documentos
    WHERE UPPER(tipdoc_nombre) = 'ACTA DE INSCRIPCION GENERADA'
);

INSERT INTO tec.tipo_documentos
    (tipdoc_nombre, estado, f_crea, u_crea)
SELECT 'ACTA DE INSCRIPCION FIRMADA', TRUE, NOW(), 'flyway'
WHERE NOT EXISTS (
    SELECT 1 FROM tec.tipo_documentos
    WHERE UPPER(tipdoc_nombre) = 'ACTA DE INSCRIPCION FIRMADA'
);

UPDATE tec.tipo_documentos
SET estado = TRUE,
    f_actualiza = NOW(),
    u_actualiza = 'flyway'
WHERE UPPER(tipdoc_nombre) IN (
    'ACTA DE INSCRIPCION GENERADA',
    'ACTA DE INSCRIPCION FIRMADA'
)
AND estado IS DISTINCT FROM TRUE;

SELECT setval(
    pg_get_serial_sequence('tec.tipo_documentos', 'tipdoc_id'),
    GREATEST((SELECT COALESCE(MAX(tipdoc_id), 1) FROM tec.tipo_documentos), 1),
    TRUE
);

ALTER TABLE tec.documentos
    ADD COLUMN IF NOT EXISTS doc_origen_id INTEGER,
    ADD COLUMN IF NOT EXISTS doc_contexto_hash VARCHAR(64);

ALTER TABLE tec.documentos_aud
    ADD COLUMN IF NOT EXISTS doc_origen_id INTEGER,
    ADD COLUMN IF NOT EXISTS doc_contexto_hash VARCHAR(64);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_documentos_documento_origen'
          AND conrelid = 'tec.documentos'::regclass
    ) THEN
        ALTER TABLE tec.documentos
            ADD CONSTRAINT fk_documentos_documento_origen
            FOREIGN KEY (doc_origen_id) REFERENCES tec.documentos(doc_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_documentos_entidad_tipo_estado
    ON tec.documentos (entidad_id, tipdoc_id, estado);

CREATE INDEX IF NOT EXISTS idx_documentos_origen
    ON tec.documentos (doc_origen_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_documentos_acta_contexto_activa
    ON tec.documentos (entidad_id, tipdoc_id, doc_contexto_hash)
    WHERE estado = TRUE AND doc_contexto_hash IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_documentos_acta_firmada_origen_activa
    ON tec.documentos (doc_origen_id)
    WHERE estado = TRUE AND doc_origen_id IS NOT NULL;
