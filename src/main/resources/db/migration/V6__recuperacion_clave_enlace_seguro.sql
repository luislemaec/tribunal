UPDATE public.tb_usuario
SET usu_clave_temp = NULL
WHERE usu_clave_temp IS NOT NULL;

UPDATE tec.correo_plantilla
SET mensaje = '<p>Estimado usuario: <strong>{nombreApellido}</strong></p><p>Se solicitó el restablecimiento de su contraseña para el usuario <strong>{nombreUsuario}</strong>.</p><p>Para continuar, use este enlace de un solo uso:</p><p><a href="{enlaceRecuperacion}">Restablecer contraseña</a></p><p>El enlace vence en 30 minutos. Si no solicitó este cambio, ignore este mensaje.</p>'
WHERE asunto = 'CORREO RECUPERAR CLAVE';

UPDATE tec.correo_plantilla
SET mensaje = '<p>Estimado usuario: <strong>{nombreApellido}</strong></p><p>Su contraseña para el usuario <strong>{nombreUsuario}</strong> fue actualizada correctamente el {fechaRegistro} a las {horaRegistro}.</p><p>Si no realizó este cambio, comuníquese de inmediato con el administrador.</p>'
WHERE asunto = 'CAMBIO CLAVE';
