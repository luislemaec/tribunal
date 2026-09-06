-- Tipo independiente: no altera los documentos de padron ni las actas existentes.
INSERT INTO tec.tipo_documentos (tipdoc_nombre, estado, f_crea, u_crea)
SELECT 'CERTIFICADOS DE VOTACION DE MESA', TRUE, NOW(), 'flyway'
WHERE NOT EXISTS (
    SELECT 1 FROM tec.tipo_documentos
    WHERE tipdoc_nombre = 'CERTIFICADOS DE VOTACION DE MESA'
);

UPDATE tec.tipo_documentos
SET estado = TRUE, f_actualiza = NOW(), u_actualiza = 'flyway'
WHERE tipdoc_id = (
    SELECT MIN(tipdoc_id) FROM tec.tipo_documentos
    WHERE tipdoc_nombre = 'CERTIFICADOS DE VOTACION DE MESA'
) AND estado IS DISTINCT FROM TRUE;
