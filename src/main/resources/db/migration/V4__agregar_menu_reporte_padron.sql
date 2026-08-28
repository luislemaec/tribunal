-- Registra el reporte del padron como ultima opcion del menu ADMIN y replica
-- los permisos activos del nodo padre para que menu y acceso directo coincidan.
DO $$
DECLARE
    v_menu_admin_id INTEGER;
    v_menu_reporte_id INTEGER;
    v_orden_final INTEGER;
BEGIN
    SELECT m.menu_id
      INTO v_menu_admin_id
      FROM public.tb_menu m
     WHERE m.menu_nombre = 'ADMIN'
       AND m.menu_padre_id IS NOT NULL
     ORDER BY m.menu_id
     LIMIT 1;

    IF v_menu_admin_id IS NULL THEN
        RAISE EXCEPTION 'No existe el menu padre ADMIN para registrar Reporte Padron.';
    END IF;

    -- La URL y el componente identifican de forma estable el registro y hacen
    -- que la migracion sea segura aun cuando el menu ya exista en la base.
    SELECT m.menu_id
      INTO v_menu_reporte_id
      FROM public.tb_menu m
     WHERE m.menu_url = '/reportePadron.jsf'
        OR m.componente_id IN ('m_reportePadron', 'm_reporte_padron')
     ORDER BY m.menu_id
     LIMIT 1;

    SELECT COALESCE(MAX(m.menu_orden), 0) + 1
      INTO v_orden_final
      FROM public.tb_menu m
     WHERE m.menu_padre_id = v_menu_admin_id
       AND (v_menu_reporte_id IS NULL OR m.menu_id <> v_menu_reporte_id);

    IF v_menu_reporte_id IS NULL THEN
        INSERT INTO public.tb_menu (
            estado, f_crea, u_crea,
            menu_accion, componente_id, menu_ico, menu_nodo_final,
            menu_nombre, menu_orden, menu_url, menu_padre_id
        )
        VALUES (
            TRUE, NOW(), 'flyway',
            '/reportePadron', 'm_reportePadron', 'pi pi-fw pi-file-excel', TRUE,
            'Rep. Padrón', v_orden_final, '/reportePadron.jsf', v_menu_admin_id
        )
        RETURNING menu_id INTO v_menu_reporte_id;
    ELSE
        UPDATE public.tb_menu
           SET estado = TRUE,
               f_actualiza = NOW(),
               u_actualiza = 'flyway',
               menu_accion = '/reportePadron',
               componente_id = 'm_reportePadron',
               menu_ico = 'pi pi-fw pi-file-excel',
               menu_nodo_final = TRUE,
               menu_nombre = 'Rep. Padrón',
               menu_orden = v_orden_final,
               menu_url = '/reportePadron.jsf',
               menu_padre_id = v_menu_admin_id
         WHERE menu_id = v_menu_reporte_id;
    END IF;

    INSERT INTO public.tb_menu_rol (rol_id, menu_id, estado, f_crea, u_crea)
    SELECT mr.rol_id, v_menu_reporte_id, TRUE, NOW(), 'flyway'
      FROM public.tb_menu_rol mr
     WHERE mr.menu_id = v_menu_admin_id
       AND mr.estado = TRUE
    ON CONFLICT (menu_id, rol_id) DO UPDATE
       SET estado = TRUE,
           f_actualiza = NOW(),
           u_actualiza = 'flyway';
END
$$;
