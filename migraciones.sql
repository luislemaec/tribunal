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
