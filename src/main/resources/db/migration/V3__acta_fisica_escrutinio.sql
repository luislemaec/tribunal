-- Metadatos de revisi\u00f3n para evidencias electorales y tipo documental del acta f\u00edsica.
ALTER TABLE tec.documentos
    ADD COLUMN IF NOT EXISTS doc_estado_revision VARCHAR(32),
    ADD COLUMN IF NOT EXISTS doc_fecha_revision TIMESTAMP,
    ADD COLUMN IF NOT EXISTS doc_usuario_revision VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doc_observacion_revision VARCHAR(1000);

ALTER TABLE tec.documentos_aud
    ADD COLUMN IF NOT EXISTS doc_estado_revision VARCHAR(32),
    ADD COLUMN IF NOT EXISTS doc_fecha_revision TIMESTAMP,
    ADD COLUMN IF NOT EXISTS doc_usuario_revision VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doc_observacion_revision VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_documentos_mesa_proceso_tipo_revision
    ON tec.documentos (mesa_id, proce_id, tipdoc_id, doc_estado_revision)
    WHERE estado = TRUE;

INSERT INTO tec.tipo_documentos (estado, f_crea, u_crea, tipdoc_nombre)
SELECT TRUE, NOW(), 'flyway', 'ACTA FISICA DE ESCRUTINIO'
WHERE NOT EXISTS (
    SELECT 1 FROM tec.tipo_documentos
    WHERE UPPER(tipdoc_nombre) = 'ACTA FISICA DE ESCRUTINIO'
);
