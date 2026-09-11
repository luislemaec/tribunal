-- Puente de autenticacion: BCrypt efimero, nunca la clave del usuario ni el token QR.
ALTER TABLE tec.sesion_qr_acta ADD COLUMN login_hash VARCHAR(60);
ALTER TABLE tec.sesion_qr_acta ADD COLUMN login_hasta TIMESTAMP WITH TIME ZONE;
ALTER TABLE tec.sesion_qr_acta ADD COLUMN login_confirmado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tec.sesion_qr_acta ADD CONSTRAINT ck_sesion_qr_login
    CHECK (login_hash IS NULL OR (login_hash ~ '^\$2[aby]\$12\$' AND login_hasta IS NOT NULL));

-- El realm usa el usuario real despues del post-realm-principal-transformer.
-- No concede roles administrativos, aunque el usuario los tenga en el login normal.
CREATE VIEW tec.credencial_qr_elytron AS
SELECT u.usu_nombre AS username, s.login_hash AS password_hash,
       'SITEC-Presidente-mesa'::VARCHAR AS rol
FROM tec.sesion_qr_acta s
JOIN tec.acceso_qr_acta q ON q.qr_id = s.qr_id
JOIN tec.documentos d ON d.doc_id = q.doc_id
JOIN tec.proceso_electoral p ON p.proce_id = q.proce_id
JOIN public.tb_usuario u ON u.usu_id = q.usu_id
WHERE s.revocada_en IS NULL AND s.login_confirmado = FALSE
  AND s.login_hash IS NOT NULL AND s.login_hasta > CURRENT_TIMESTAMP
  AND s.expira_en > CURRENT_TIMESTAMP AND q.estado = 'CANJEADO'
  AND q.vigente_desde <= CURRENT_TIMESTAMP AND q.vigente_hasta > CURRENT_TIMESTAMP
  AND d.estado = TRUE AND d.doc_version = q.doc_version
  AND d.mesa_id = q.mesa_id AND d.proce_id = q.proce_id AND d.rec_id = q.rec_id
  AND d.tipdoc_id = q.tipdoc_id AND u.estado = TRUE AND p.estado = TRUE AND p.proce_activo = TRUE
  AND EXISTS (SELECT 1 FROM public.tb_role_user ru JOIN public.tb_rol r ON r.rol_id = ru.rol_id
              WHERE ru.usu_id = u.usu_id AND ru.estado = TRUE AND r.estado = TRUE
                AND r.rol_nombre = 'SITEC-Presidente-mesa');
