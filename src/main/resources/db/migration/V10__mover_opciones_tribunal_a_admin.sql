-- Mueve opciones existentes de TRIBUNAL a ADMIN sin recrearlas ni alterar
-- sus permisos de acceso. Los IDs se resuelven por componente y URL.
DO $$
DECLARE
    v_admin_id integer;
    v_tribunal_id integer;
    v_asignacion_id integer;
    v_personas_id integer;
    v_candidatos_id integer;
    v_mjrv_id integer;
    v_padron_id integer;
    v_acta_escrutinio_id integer;
    v_orden_personas integer;
    v_orden_padron integer;
BEGIN
    SELECT menu_id
      INTO v_admin_id
      FROM public.tb_menu
     WHERE componente_id = 'm_administracion'
        OR (upper(menu_nombre) = 'ADMIN' AND menu_padre_id IS NOT NULL)
     ORDER BY CASE WHEN componente_id = 'm_administracion' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_tribunal_id
      FROM public.tb_menu
     WHERE componente_id = 'm_tribunal'
        OR (upper(menu_nombre) = 'TRIBUNAL' AND menu_padre_id IS NOT NULL)
     ORDER BY CASE WHEN componente_id = 'm_tribunal' THEN 0 ELSE 1 END,
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
      INTO v_personas_id
      FROM public.tb_menu
     WHERE componente_id = 'm_personas'
        OR menu_url = '/personas.jsf'
     ORDER BY CASE WHEN componente_id = 'm_personas' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_candidatos_id
      FROM public.tb_menu
     WHERE componente_id = 'm_candidatos'
        OR menu_url = '/candidatos.jsf'
     ORDER BY CASE WHEN componente_id = 'm_candidatos' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_mjrv_id
     FROM public.tb_menu
     WHERE menu_url = '/mjrv.jsf'
        OR componente_id = 'm_mjrv'
        OR (componente_id = 'm_documentos' AND upper(menu_nombre) = 'MJRV')
     ORDER BY CASE
                  WHEN menu_url = '/mjrv.jsf' THEN 0
                  WHEN componente_id = 'm_mjrv' THEN 1
                  ELSE 2
              END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_padron_id
      FROM public.tb_menu
     WHERE componente_id = 'm_padron'
        OR menu_url = '/padron.jsf'
     ORDER BY CASE WHEN componente_id = 'm_padron' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    SELECT menu_id
      INTO v_acta_escrutinio_id
      FROM public.tb_menu
     WHERE menu_url = '/actaE.jsf'
        OR componente_id IN ('m_actaE', 'm_acta_escrutinio')
     ORDER BY CASE WHEN menu_url = '/actaE.jsf' THEN 0 ELSE 1 END,
              menu_id
     LIMIT 1;

    IF v_admin_id IS NULL OR v_tribunal_id IS NULL
       OR v_personas_id IS NULL OR v_candidatos_id IS NULL
       OR v_mjrv_id IS NULL OR v_padron_id IS NULL
       OR v_acta_escrutinio_id IS NULL THEN
        RAISE EXCEPTION 'No se encontraron todos los menus requeridos para mover opciones de TRIBUNAL a ADMIN';
    END IF;

    SELECT menu_orden INTO v_orden_personas
      FROM public.tb_menu
     WHERE menu_id = v_personas_id;

    SELECT menu_orden INTO v_orden_padron
      FROM public.tb_menu
     WHERE menu_id = v_padron_id;

    IF v_orden_personas IS NULL OR v_orden_padron IS NULL THEN
        RAISE EXCEPTION 'Personas y Padron deben tener un orden definido dentro de ADMIN';
    END IF;

    -- Se conserva cada fila, estado, URL, icono y relacion menu-rol.
    UPDATE public.tb_menu
       SET menu_padre_id = v_admin_id,
           f_actualiza = now(),
           u_actualiza = 'flyway'
     WHERE menu_id IN (v_candidatos_id, v_mjrv_id, v_acta_escrutinio_id)
       AND menu_padre_id IS DISTINCT FROM v_admin_id;

    -- Los mismos roles de las opciones movidas deben poder resolver el nuevo
    -- nodo padre. No se modifican ni amplian los permisos de las hojas.
    INSERT INTO public.tb_menu_rol
           (menu_id, rol_id, estado, f_crea, u_crea)
    SELECT DISTINCT v_admin_id, permiso.rol_id, true, now(), 'flyway'
      FROM public.tb_menu_rol permiso
     WHERE permiso.menu_id IN (v_candidatos_id, v_mjrv_id, v_acta_escrutinio_id)
       AND permiso.estado = true
    ON CONFLICT (menu_id, rol_id) DO UPDATE
        SET estado = true,
            f_actualiza = now(),
            u_actualiza = 'flyway';

    -- Orden activo: conserva la secuencia previa e inserta las opciones en sus
    -- anclas. Asignacion permanece primero conforme a V9. Las opciones
    -- inactivas se mantienen, pero se ordenan despues de las visibles.
    WITH ordenados AS (
        SELECT menu_id,
               (row_number() OVER (
                   ORDER BY
                       CASE WHEN estado = true THEN 0 ELSE 1 END,
                       CASE
                           WHEN menu_id IN (v_candidatos_id, v_mjrv_id) THEN v_orden_personas
                           WHEN menu_id = v_acta_escrutinio_id THEN v_orden_padron
                           WHEN menu_id = v_asignacion_id THEN -2147483648
                           ELSE COALESCE(menu_orden, 2147483647)
                       END,
                       CASE
                           WHEN menu_id = v_asignacion_id THEN 0
                           WHEN menu_id = v_personas_id THEN 0
                           WHEN menu_id = v_candidatos_id THEN 1
                           WHEN menu_id = v_mjrv_id THEN 2
                           WHEN menu_id = v_padron_id THEN 0
                           WHEN menu_id = v_acta_escrutinio_id THEN 1
                           ELSE 3
                       END,
                       menu_id
               ))::integer AS nuevo_orden
          FROM public.tb_menu
         WHERE menu_padre_id = v_admin_id
    )
    UPDATE public.tb_menu menu
       SET menu_orden = ordenados.nuevo_orden,
           f_actualiza = now(),
           u_actualiza = 'flyway'
      FROM ordenados
     WHERE menu.menu_id = ordenados.menu_id
       AND menu.menu_orden IS DISTINCT FROM ordenados.nuevo_orden;

    -- Validaciones transaccionales del estado final.
    IF EXISTS (
        SELECT 1
          FROM public.tb_menu
         WHERE menu_id IN (v_candidatos_id, v_mjrv_id, v_acta_escrutinio_id)
           AND menu_padre_id IS DISTINCT FROM v_admin_id
    ) THEN
        RAISE EXCEPTION 'Una o mas opciones no quedaron asignadas al menu ADMIN';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.tb_menu
         WHERE menu_padre_id = v_tribunal_id
           AND menu_id IN (v_candidatos_id, v_mjrv_id, v_acta_escrutinio_id)
    ) THEN
        RAISE EXCEPTION 'Candidatos, MJRV o Acta Escrutinio permanecen dentro de TRIBUNAL';
    END IF;

    IF (SELECT menu_orden FROM public.tb_menu WHERE menu_id = v_candidatos_id)
           <> (SELECT menu_orden + 1 FROM public.tb_menu WHERE menu_id = v_personas_id)
       OR (SELECT menu_orden FROM public.tb_menu WHERE menu_id = v_mjrv_id)
           <> (SELECT menu_orden + 1 FROM public.tb_menu WHERE menu_id = v_candidatos_id)
       OR (SELECT menu_orden FROM public.tb_menu WHERE menu_id = v_acta_escrutinio_id)
           <> (SELECT menu_orden + 1 FROM public.tb_menu WHERE menu_id = v_padron_id) THEN
        RAISE EXCEPTION 'El orden relativo Personas-Candidatos-MJRV o Padron-Acta Escrutinio es incorrecto';
    END IF;

    IF EXISTS (
        SELECT menu_orden
          FROM public.tb_menu
         WHERE menu_padre_id = v_admin_id
         GROUP BY menu_orden
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'ADMIN contiene posiciones duplicadas';
    END IF;
END
$$;
