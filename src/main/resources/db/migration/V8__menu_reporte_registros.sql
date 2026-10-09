-- REPORTES > Rep. Registros (/reporteRegistros.jsf): personas habilitadas por cantón y parroquia.
-- Solo agrega datos de menú; no cambia esquema ni otros menús.
-- Acceso: SITEC-Administrador y SITEC-Tribunal (el reporte muestra todos los cantones).
-- Padre y roles se resuelven por nombre, no por id, y cada INSERT es idempotente.

-- Las secuencias pueden haber quedado atrás si los menús se crearon con ids explícitos.
SELECT setval(
    pg_get_serial_sequence('public.tb_menu', 'menu_id'),
    GREATEST((SELECT COALESCE(MAX(menu_id), 1) FROM public.tb_menu), 1),
    TRUE
);
SELECT setval(
    pg_get_serial_sequence('public.tb_menu_rol', 'mero_id'),
    GREATEST((SELECT COALESCE(MAX(mero_id), 1) FROM public.tb_menu_rol), 1),
    TRUE
);

INSERT INTO public.tb_menu (estado, f_crea, u_crea, menu_accion, componente_id, menu_ico, menu_img,
                            menu_nodo_final, menu_nombre, menu_orden, menu_url, menu_padre_id)
SELECT TRUE, NOW(), 'flyway', '/reporteRegistros', 'm_reporteRegistros', 'pi pi-fw pi-chart-bar', NULL,
       TRUE, 'Rep. Registros', 3, '/reporteRegistros.jsf', padre.menu_id
  FROM public.tb_menu padre
 WHERE padre.componente_id = 'm_reportes'
   AND NOT EXISTS (SELECT 1 FROM public.tb_menu m WHERE m.componente_id = 'm_reporteRegistros');

INSERT INTO public.tb_menu_rol (rol_id, menu_id, estado, f_crea, u_crea)
SELECT r.rol_id, m.menu_id, TRUE, NOW(), 'flyway'
  FROM public.tb_rol r
  JOIN public.tb_menu m ON m.componente_id = 'm_reporteRegistros'
 WHERE r.rol_nombre IN ('SITEC-Administrador', 'SITEC-Tribunal')
   AND NOT EXISTS (SELECT 1 FROM public.tb_menu_rol mr WHERE mr.rol_id = r.rol_id AND mr.menu_id = m.menu_id);
