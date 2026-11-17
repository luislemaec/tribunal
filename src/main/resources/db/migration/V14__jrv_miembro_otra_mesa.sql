-- JRV (mjrv.xhtml): miembro designado desde otra mesa del padron del mismo proceso.
-- miem_otra_mesa marca la designacion que se acepto expresamente fuera del padron de la mesa;
-- miem_acepta_usuario y miem_acepta_fecha guardan quien acepto la condicion y cuando.
-- La persona sigue votando en la mesa donde esta empadronada: el padron no cambia.
-- No modifica datos existentes (todas las designaciones previas quedan en FALSE). Idempotente.
ALTER TABLE tec.miembros_jrv
    ADD COLUMN IF NOT EXISTS miem_otra_mesa BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS miem_acepta_usuario VARCHAR(255),
    ADD COLUMN IF NOT EXISTS miem_acepta_fecha TIMESTAMP;

-- Tabla de auditoria de Envers (MiembroJRV es @Audited): mismas columnas, sin restricciones.
ALTER TABLE tec.miembros_jrv_aud
    ADD COLUMN IF NOT EXISTS miem_otra_mesa BOOLEAN,
    ADD COLUMN IF NOT EXISTS miem_acepta_usuario VARCHAR(255),
    ADD COLUMN IF NOT EXISTS miem_acepta_fecha TIMESTAMP;
