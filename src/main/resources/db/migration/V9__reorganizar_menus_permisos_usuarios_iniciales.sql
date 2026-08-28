-- Reorganiza los menus institucionales y restringe los permisos sin eliminar
-- registros historicos. Los identificadores se resuelven por componentes,
-- URL y mnemonicos estables en lugar de depender de IDs numericos.
DO $$
DECLARE
    v_admin_id bigint;
    v_tribunal_id bigint;
    v_seguridad_id bigint;
    v_reportes_id bigint;
    v_asignacion_id bigint;
    v_reporte_padron_id bigint;
    v_documentos_mesa_id bigint;
    v_carga_datos_id bigint;
    v_mesas_id bigint;
    v_periodos_id bigint;
    v_rol_admin_id bigint;
    v_rol_tribunal_id bigint;
BEGIN
    SELECT menu_id
      INTO v_admin_id
      FROM public.tb_menu
     WHERE componente_id = 'm_administracion'
        OR upper(menu_nombre) = 'ADMIN'
     ORDER BY CASE WHEN componente_id = 'm_administracion' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_tribunal_id
      FROM public.tb_menu
     WHERE componente_id = 'm_tribunal'
        OR upper(menu_nombre) = 'TRIBUNAL'
     ORDER BY CASE WHEN componente_id = 'm_tribunal' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_seguridad_id
      FROM public.tb_menu
     WHERE componente_id = 'm_seguridad'
        OR upper(menu_nombre) = 'SEGURIDAD'
     ORDER BY CASE WHEN componente_id = 'm_seguridad' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_reportes_id
      FROM public.tb_menu
     WHERE componente_id = 'm_reportes'
        OR upper(menu_nombre) = 'REPORTES'
     ORDER BY CASE WHEN componente_id = 'm_reportes' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_asignacion_id
      FROM public.tb_menu
     WHERE componente_id = 'm_asignacionUsuario'
        OR menu_url = '/asignacionUsuarios.jsf'
     ORDER BY CASE WHEN componente_id = 'm_asignacionUsuario' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_reporte_padron_id
      FROM public.tb_menu
     WHERE componente_id = 'm_reportePadron'
        OR menu_url = '/reportePadron.jsf'
     ORDER BY CASE WHEN componente_id = 'm_reportePadron' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_documentos_mesa_id
      FROM public.tb_menu
     WHERE componente_id = 'm_reportesMesa'
        OR menu_url = '/reportesMesa.jsf'
     ORDER BY CASE WHEN componente_id = 'm_reportesMesa' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_carga_datos_id
      FROM public.tb_menu
     WHERE componente_id = 'm_cargadatos'
        OR menu_url = '/cargaDatos.jsf'
     ORDER BY CASE WHEN componente_id = 'm_cargadatos' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_mesas_id
      FROM public.tb_menu
     WHERE componente_id = 'm_mesas'
        OR menu_url = '/mesas.jsf'
     ORDER BY CASE WHEN componente_id = 'm_mesas' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_periodos_id
      FROM public.tb_menu
     WHERE componente_id = 'm_periodos'
        OR menu_url = '/periodos.jsf'
     ORDER BY CASE WHEN componente_id = 'm_periodos' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT rol_id
      INTO v_rol_admin_id
      FROM public.tb_rol
     WHERE rol_nombre = 'SITEC-Administrador'
     ORDER BY rol_id
     LIMIT 1;

    SELECT rol_id
      INTO v_rol_tribunal_id
      FROM public.tb_rol
     WHERE rol_nombre = 'SITEC-Tribunal'
     ORDER BY rol_id
     LIMIT 1;

    IF v_admin_id IS NULL OR v_tribunal_id IS NULL OR v_seguridad_id IS NULL
       OR v_reportes_id IS NULL OR v_asignacion_id IS NULL
       OR v_reporte_padron_id IS NULL OR v_documentos_mesa_id IS NULL
       OR v_carga_datos_id IS NULL OR v_mesas_id IS NULL
       OR v_periodos_id IS NULL THEN
        RAISE EXCEPTION 'No se encontraron todos los menus requeridos para la reorganizacion';
    END IF;

    IF v_rol_admin_id IS NULL OR v_rol_tribunal_id IS NULL THEN
        RAISE EXCEPTION 'No se encontraron los roles SITEC-Administrador y SITEC-Tribunal';
    END IF;

    -- Opciones retiradas funcionalmente, conservando filas y permisos historicos.
    UPDATE public.tb_menu
       SET estado = false,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE menu_id IN (v_carga_datos_id, v_mesas_id, v_periodos_id)
       AND estado IS DISTINCT FROM false;

    -- Asignacion conserva el mismo registro y permisos, pero ahora pertenece a ADMIN.
    UPDATE public.tb_menu
       SET menu_padre_id = v_admin_id,
           menu_orden = 1,
           estado = true,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE menu_id = v_asignacion_id
       AND (menu_padre_id IS DISTINCT FROM v_admin_id
            OR menu_orden IS DISTINCT FROM 1
            OR estado IS DISTINCT FROM true);

    -- Asegura que el padre ADMIN sea visible para cada rol que conserva Asignacion.
    INSERT INTO public.tb_menu_rol
           (menu_id, rol_id, estado, f_crea, u_crea)
    SELECT v_admin_id, mr.rol_id, true, now(), 'flyway'
      FROM public.tb_menu_rol mr
     WHERE mr.menu_id = v_asignacion_id
       AND mr.estado = true
    ON CONFLICT (menu_id, rol_id) DO UPDATE
        SET estado = true,
            f_actualiza = now(),
            u_actualiza = 'flyway';

    -- ADMIN mantiene posiciones unicas entre sus opciones activas.
    WITH ordenados AS (
        SELECT menu_id,
               (row_number() OVER (ORDER BY menu_orden NULLS LAST, menu_id) + 1)::integer AS nuevo_orden
          FROM public.tb_menu
         WHERE menu_padre_id = v_admin_id
           AND estado = true
           AND menu_id <> v_asignacion_id
    )
    UPDATE public.tb_menu m
       SET menu_orden = o.nuevo_orden,
           f_actualiza = now(),
           u_actualiza = 'flyway'
      FROM ordenados o
     WHERE m.menu_id = o.menu_id
       AND m.menu_orden IS DISTINCT FROM o.nuevo_orden;

    -- Reporte Padron se mueve a REPORTES y conserva sus permisos actuales.
    UPDATE public.tb_menu
       SET estado = true,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE menu_id = v_reportes_id
       AND estado IS DISTINCT FROM true;

    UPDATE public.tb_menu
       SET menu_padre_id = v_reportes_id,
           menu_orden = 2,
           estado = true,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE menu_id = v_reporte_padron_id
       AND (menu_padre_id IS DISTINCT FROM v_reportes_id
            OR menu_orden IS DISTINCT FROM 2
            OR estado IS DISTINCT FROM true);

    UPDATE public.tb_menu
       SET menu_padre_id = v_reportes_id,
           menu_orden = 1,
           estado = true,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE menu_id = v_documentos_mesa_id
       AND (menu_padre_id IS DISTINCT FROM v_reportes_id
            OR menu_orden IS DISTINCT FROM 1
            OR estado IS DISTINCT FROM true);

    -- Cualquier opcion adicional de REPORTES se ordena desde la posicion 3.
    WITH ordenados AS (
        SELECT menu_id,
               (row_number() OVER (ORDER BY menu_orden NULLS LAST, menu_id) + 2)::integer AS nuevo_orden
          FROM public.tb_menu
         WHERE menu_padre_id = v_reportes_id
           AND estado = true
           AND menu_id NOT IN (v_documentos_mesa_id, v_reporte_padron_id)
    )
    UPDATE public.tb_menu m
       SET menu_orden = o.nuevo_orden,
           f_actualiza = now(),
           u_actualiza = 'flyway'
      FROM ordenados o
     WHERE m.menu_id = o.menu_id
       AND m.menu_orden IS DISTINCT FROM o.nuevo_orden;

    -- Docs. Mesa queda autorizado exclusivamente para Administrador y Tribunal.
    UPDATE public.tb_menu_rol mr
       SET estado = false,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE mr.menu_id = v_documentos_mesa_id
       AND mr.rol_id NOT IN (v_rol_admin_id, v_rol_tribunal_id)
       AND mr.estado IS DISTINCT FROM false;

    INSERT INTO public.tb_menu_rol
           (menu_id, rol_id, estado, f_crea, u_crea)
    VALUES (v_documentos_mesa_id, v_rol_admin_id, true, now(), 'flyway'),
           (v_documentos_mesa_id, v_rol_tribunal_id, true, now(), 'flyway')
    ON CONFLICT (menu_id, rol_id) DO UPDATE
        SET estado = true,
            f_actualiza = now(),
            u_actualiza = 'flyway';

    -- REPORTES se habilita para la union de roles autorizados en sus hijos.
    INSERT INTO public.tb_menu_rol
           (menu_id, rol_id, estado, f_crea, u_crea)
    SELECT DISTINCT v_reportes_id, mr.rol_id, true, now(), 'flyway'
      FROM public.tb_menu hijo
      JOIN public.tb_menu_rol mr ON mr.menu_id = hijo.menu_id
     WHERE hijo.menu_padre_id = v_reportes_id
       AND hijo.estado = true
       AND mr.estado = true
    ON CONFLICT (menu_id, rol_id) DO UPDATE
        SET estado = true,
            f_actualiza = now(),
            u_actualiza = 'flyway';

    UPDATE public.tb_menu_rol padre
       SET estado = false,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE padre.menu_id = v_reportes_id
       AND padre.estado = true
       AND NOT EXISTS (
           SELECT 1
             FROM public.tb_menu hijo
             JOIN public.tb_menu_rol permiso_hijo
               ON permiso_hijo.menu_id = hijo.menu_id
              AND permiso_hijo.rol_id = padre.rol_id
              AND permiso_hijo.estado = true
            WHERE hijo.menu_padre_id = v_reportes_id
              AND hijo.estado = true
       );

    -- V2 creo estas cuentas tecnicas/no administrativas. Se conservan sus filas
    -- para auditoria, pero se desactivan solo cuando siguen identificadas como
    -- semillas Flyway; los usuarios creados posteriormente no son afectados.
    UPDATE public.tb_role_user ru
       SET estado = false,
           f_actualiza = now(),
           u_actualiza = 'flyway'
      FROM public.tb_usuario u
     WHERE u.usu_id = ru.usu_id
       AND u.u_crea = 'flyway'
       AND u.usu_nombre IN (
           '0603553496', '0605015122', '0603642331', '0603587478',
           '0603763921', '0604367136', '0604532960', '0605015239',
           '0604679316', '0604845495', '0604913087', '0603645888',
           '0606033249'
       )
       AND ru.estado IS DISTINCT FROM false;

    UPDATE public.tb_usuario
       SET estado = false,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE u_crea = 'flyway'
       AND usu_nombre IN (
           '0603553496', '0605015122', '0603642331', '0603587478',
           '0603763921', '0604367136', '0604532960', '0605015239',
           '0604679316', '0604845495', '0604913087', '0603645888',
           '0606033249'
       )
       AND estado IS DISTINCT FROM false;

    -- Validaciones transaccionales: cualquier desviacion revierte la migracion.
    IF NOT EXISTS (
        SELECT 1 FROM public.tb_menu
         WHERE menu_id = v_asignacion_id
           AND menu_padre_id = v_admin_id
           AND menu_orden = 1
           AND estado = true
    ) THEN
        RAISE EXCEPTION 'Asignacion no quedo ubicada en ADMIN, posicion 1';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.tb_menu
         WHERE menu_id IN (v_carga_datos_id, v_mesas_id, v_periodos_id)
           AND estado = true
    ) THEN
        RAISE EXCEPTION 'Carga Datos, Mesas o Periodos permanecen habilitados';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.tb_menu
         WHERE menu_id = v_reporte_padron_id
           AND menu_padre_id = v_reportes_id
           AND menu_orden = 2
           AND estado = true
    ) THEN
        RAISE EXCEPTION 'Reporte Padron no quedo ubicado en REPORTES, posicion 2';
    END IF;

    IF EXISTS (
        SELECT menu_orden
          FROM public.tb_menu
         WHERE menu_padre_id = v_reportes_id
           AND estado = true
         GROUP BY menu_orden
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'REPORTES contiene posiciones activas duplicadas';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.tb_menu_rol
         WHERE menu_id = v_documentos_mesa_id
           AND estado = true
           AND rol_id NOT IN (v_rol_admin_id, v_rol_tribunal_id)
    ) OR (SELECT count(*)
            FROM public.tb_menu_rol
           WHERE menu_id = v_documentos_mesa_id
             AND estado = true
             AND rol_id IN (v_rol_admin_id, v_rol_tribunal_id)) <> 2 THEN
        RAISE EXCEPTION 'Docs. Mesa no quedo restringido a Administrador y Tribunal';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.tb_usuario u
         WHERE u.u_crea = 'flyway'
           AND u.estado = true
           AND u.usu_nombre IN (
               '0603553496', '0605015122', '0603642331', '0603587478',
               '0603763921', '0604367136', '0604532960', '0605015239',
               '0604679316', '0604845495', '0604913087', '0603645888',
               '0606033249'
           )
    ) THEN
        RAISE EXCEPTION 'Permanecen usuarios iniciales no administrativos habilitados';
    END IF;
END
$$;
