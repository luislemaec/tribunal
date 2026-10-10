-- Índices para la bitácora (tec.procesos) y la resolución de usuarios y roles.
-- Aceleran Rep. Registros > Actividad y la pantalla Actividades (procesos.xhtml):
-- hasta ahora tec.procesos solo tenía la clave primaria y cada consulta la recorría completa.
-- Solo agrega índices; no cambia datos ni otras tablas.
--
-- Nota: CREATE INDEX (sin CONCURRENTLY, porque Flyway ejecuta en transacción) bloquea las
-- escrituras de la tabla mientras se construye el índice. En una bitácora grande conviene
-- aplicar esta migración fuera del horario de uso.

-- Periodos (f_crea >= ...) y «más recientes primero» (ORDER BY f_crea DESC, proceso_id DESC LIMIT n).
CREATE INDEX IF NOT EXISTS idx_procesos_f_crea ON tec.procesos (f_crea DESC, proceso_id DESC);

-- Filtro Usuario de Actividades y alcance «solo sus actividades» de los demás roles.
CREATE INDEX IF NOT EXISTS idx_procesos_u_crea ON tec.procesos (u_crea);

-- Identidad de los usuarios de la bitácora (u_crea se compara con usu_nombre).
CREATE INDEX IF NOT EXISTS idx_usuario_nombre ON public.tb_usuario (usu_nombre);

-- Roles de un usuario y usuarios de un rol (exclusión de Administradores para Tribunal).
CREATE INDEX IF NOT EXISTS idx_role_user_usuario ON public.tb_role_user (usu_id);
CREATE INDEX IF NOT EXISTS idx_role_user_rol ON public.tb_role_user (rol_id);
