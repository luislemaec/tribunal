-- =====================================================================================
-- Limpieza manual: cabeceras de escrutinio PENDIENTES de mesas sin padrón (proceso activo)
-- =====================================================================================
-- Origen: hasta la corrección de ActaEController.cargaDatosMesaSeleccionada, elegir una mesa
-- en actaE.xhtml creaba su cabecera (tec.escrutinio_cabecera) aunque la mesa no tuviera
-- padrón y nunca pudiera abrirse. Esas cabeceras aparecen en el listado de actaE.
--
-- Solo se eliminan cabeceras que cumplen TODO esto:
--   * del proceso electoral activo;
--   * en estado PENDIENTE (sin apertura registrada: sin fecha de apertura);
--   * de una mesa sin padrón activo en ese proceso;
--   * sin votos registrados (tec.escrutinio) ni documentos (tec.documentos) de la mesa y proceso.
-- Las mesas cerradas (p. ej. Majipamba 1 y 2) NO se tocan: se revisan y, si corresponde, se
-- anulan desde el control administrativo de actaE, que registra el motivo.
--
-- Uso: ejecutar en una sola sesión con un usuario con permiso DELETE sobre tec.escrutinio_cabecera.
--   1. Ejecutar el bloque 1 y revisar el listado.
--   2. Ejecutar el bloque 2 (BEGIN + DELETE ... RETURNING) y comparar con el listado.
--   3. Si coincide, COMMIT; si no, ROLLBACK.
-- Nota: el borrado por SQL no genera revisión en la auditoría Envers (escrutinio_cabecera_aud);
-- el RETURNING del bloque 2 sirve de constancia: guardarlo con el registro del cambio.
-- No es una migración Flyway: no copiar a db/migration.
-- =====================================================================================

-- 1. Vista previa
SELECT c.esca_id, c.mesa_id, m.mesa_nombre, r.rec_nombre, c.esca_estado, c.f_crea, c.u_crea
  FROM tec.escrutinio_cabecera c
  JOIN tec.proceso_electoral pe ON pe.proce_id = c.proce_id AND pe.proce_activo = TRUE
  JOIN tec.mesas m ON m.mesa_id = c.mesa_id
  LEFT JOIN tec.recintos r ON r.rec_id = m.rec_id
 WHERE c.esca_estado = 'PENDIENTE'
   AND c.esca_fecha_apertura IS NULL
   AND NOT EXISTS (SELECT 1 FROM tec.padron p
                    WHERE p.mesa_id = c.mesa_id AND p.proce_id = c.proce_id AND p.estado = TRUE)
   AND NOT EXISTS (SELECT 1 FROM tec.escrutinio e
                    WHERE e.mesa_id = c.mesa_id AND e.proce_id = c.proce_id)
   AND NOT EXISTS (SELECT 1 FROM tec.documentos d
                    WHERE d.mesa_id = c.mesa_id AND d.proce_id = c.proce_id)
 ORDER BY r.rec_nombre, m.mesa_nombre;

-- 2. Borrado (revisar antes de confirmar)
BEGIN;

DELETE FROM tec.escrutinio_cabecera c
 USING tec.proceso_electoral pe
 WHERE pe.proce_id = c.proce_id AND pe.proce_activo = TRUE
   AND c.esca_estado = 'PENDIENTE'
   AND c.esca_fecha_apertura IS NULL
   AND NOT EXISTS (SELECT 1 FROM tec.padron p
                    WHERE p.mesa_id = c.mesa_id AND p.proce_id = c.proce_id AND p.estado = TRUE)
   AND NOT EXISTS (SELECT 1 FROM tec.escrutinio e
                    WHERE e.mesa_id = c.mesa_id AND e.proce_id = c.proce_id)
   AND NOT EXISTS (SELECT 1 FROM tec.documentos d
                    WHERE d.mesa_id = c.mesa_id AND d.proce_id = c.proce_id)
RETURNING c.esca_id, c.mesa_id, c.esca_estado, c.f_crea, c.u_crea;

-- COMMIT;    -- si el RETURNING coincide con la vista previa
-- ROLLBACK;  -- en caso contrario
