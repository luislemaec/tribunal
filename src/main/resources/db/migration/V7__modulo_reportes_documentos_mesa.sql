-- Modulo centralizado de reportes y documentos por proceso, recinto y mesa.

INSERT INTO tec.tipo_documentos (tipdoc_nombre, estado, f_crea, u_crea)
SELECT nombre, TRUE, NOW(), 'flyway'
FROM (VALUES
    ('ACTA PARCIAL DE ESCRUTINIO'),
    ('PADRON ELECTORAL DE MESA')
) AS tipos(nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tec.tipo_documentos td
    WHERE UPPER(td.tipdoc_nombre) = UPPER(tipos.nombre)
);

UPDATE tec.tipo_documentos
   SET estado = TRUE,
       f_actualiza = NOW(),
       u_actualiza = 'flyway'
 WHERE UPPER(tipdoc_nombre) IN ('ACTA PARCIAL DE ESCRUTINIO', 'PADRON ELECTORAL DE MESA')
   AND estado IS DISTINCT FROM TRUE;

ALTER TABLE tec.documentos
    ADD COLUMN IF NOT EXISTS proce_id INTEGER,
    ADD COLUMN IF NOT EXISTS rec_id INTEGER,
    ADD COLUMN IF NOT EXISTS mesa_id INTEGER;

ALTER TABLE tec.documentos_aud
    ADD COLUMN IF NOT EXISTS proce_id INTEGER,
    ADD COLUMN IF NOT EXISTS rec_id INTEGER,
    ADD COLUMN IF NOT EXISTS mesa_id INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_documentos_proceso_electoral'
           AND conrelid = 'tec.documentos'::regclass
    ) THEN
        ALTER TABLE tec.documentos
            ADD CONSTRAINT fk_documentos_proceso_electoral
            FOREIGN KEY (proce_id) REFERENCES tec.proceso_electoral(proce_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_documentos_recinto'
           AND conrelid = 'tec.documentos'::regclass
    ) THEN
        ALTER TABLE tec.documentos
            ADD CONSTRAINT fk_documentos_recinto
            FOREIGN KEY (rec_id) REFERENCES tec.recintos(rec_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_documentos_mesa'
           AND conrelid = 'tec.documentos'::regclass
    ) THEN
        ALTER TABLE tec.documentos
            ADD CONSTRAINT fk_documentos_mesa
            FOREIGN KEY (mesa_id) REFERENCES tec.mesas(mesa_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_documentos_mesa_proceso_estado
    ON tec.documentos (mesa_id, proce_id, estado);
CREATE INDEX IF NOT EXISTS idx_documentos_recinto_proceso_estado
    ON tec.documentos (rec_id, proce_id, estado);

-- Las actas finales existentes codifican proceso y mesa como ACTA-{proceso}-M{mesa}-...
WITH codigos AS (
    SELECT d.doc_id,
           ((regexp_match(d.doc_codigo, '^ACTA-([0-9]+)-M([0-9]+)-'))[1])::INTEGER AS proce_id,
           ((regexp_match(d.doc_codigo, '^ACTA-([0-9]+)-M([0-9]+)-'))[2])::INTEGER AS mesa_id
      FROM tec.documentos d
     WHERE d.doc_codigo ~ '^ACTA-[0-9]+-M[0-9]+-'
), validos AS (
    SELECT c.doc_id, c.proce_id, c.mesa_id, m.rec_id
      FROM codigos c
      JOIN tec.proceso_electoral p ON p.proce_id = c.proce_id
      JOIN tec.mesas m ON m.mesa_id = c.mesa_id
)
UPDATE tec.documentos d
   SET proce_id = COALESCE(d.proce_id, v.proce_id),
       mesa_id = COALESCE(d.mesa_id, v.mesa_id),
       rec_id = COALESCE(d.rec_id, v.rec_id)
  FROM validos v
 WHERE d.doc_id = v.doc_id;

DO $$
DECLARE
    v_raiz_id INTEGER;
    v_reportes_id INTEGER;
    v_opcion_id INTEGER;
    v_orden INTEGER;
BEGIN
    SELECT menu_id INTO v_raiz_id
      FROM public.tb_menu
     WHERE menu_padre_id IS NULL
     ORDER BY menu_id
     LIMIT 1;

    IF v_raiz_id IS NULL THEN
        RAISE EXCEPTION 'No existe el nodo raiz del menu para registrar REPORTES.';
    END IF;

    SELECT menu_id INTO v_reportes_id
      FROM public.tb_menu
     WHERE componente_id = 'm_reportes'
        OR (UPPER(menu_nombre) = 'REPORTES' AND menu_padre_id = v_raiz_id)
     ORDER BY menu_id
     LIMIT 1;

    SELECT COALESCE(MAX(menu_orden), 0) + 1 INTO v_orden
      FROM public.tb_menu
     WHERE menu_padre_id = v_raiz_id
       AND (v_reportes_id IS NULL OR menu_id <> v_reportes_id);

    IF v_reportes_id IS NULL THEN
        INSERT INTO public.tb_menu (
            estado, f_crea, u_crea, menu_accion, componente_id, menu_ico,
            menu_nodo_final, menu_nombre, menu_orden, menu_url, menu_padre_id
        ) VALUES (
            TRUE, NOW(), 'flyway', NULL, 'm_reportes', 'pi pi-fw pi-chart-bar',
            FALSE, 'REPORTES', v_orden, NULL, v_raiz_id
        ) RETURNING menu_id INTO v_reportes_id;
    ELSE
        UPDATE public.tb_menu
           SET estado = TRUE,
               f_actualiza = NOW(),
               u_actualiza = 'flyway',
               menu_accion = NULL,
               componente_id = 'm_reportes',
               menu_ico = 'pi pi-fw pi-chart-bar',
               menu_nodo_final = FALSE,
               menu_nombre = 'REPORTES',
               menu_orden = v_orden,
               menu_url = NULL,
               menu_padre_id = v_raiz_id
         WHERE menu_id = v_reportes_id;
    END IF;

    SELECT menu_id INTO v_opcion_id
      FROM public.tb_menu
     WHERE menu_url = '/reportesMesa.jsf'
        OR componente_id = 'm_reportesMesa'
     ORDER BY menu_id
     LIMIT 1;

    IF v_opcion_id IS NULL THEN
        INSERT INTO public.tb_menu (
            estado, f_crea, u_crea, menu_accion, componente_id, menu_ico,
            menu_nodo_final, menu_nombre, menu_orden, menu_url, menu_padre_id
        ) VALUES (
            TRUE, NOW(), 'flyway', '/reportesMesa', 'm_reportesMesa', 'pi pi-fw pi-folder-open',
            TRUE, 'Docs. mesa', 1, '/reportesMesa.jsf', v_reportes_id
        ) RETURNING menu_id INTO v_opcion_id;
    ELSE
        UPDATE public.tb_menu
           SET estado = TRUE,
               f_actualiza = NOW(),
               u_actualiza = 'flyway',
               menu_accion = '/reportesMesa',
               componente_id = 'm_reportesMesa',
               menu_ico = 'pi pi-fw pi-folder-open',
               menu_nodo_final = TRUE,
               menu_nombre = 'Docs. mesa',
               menu_orden = 1,
               menu_url = '/reportesMesa.jsf',
               menu_padre_id = v_reportes_id
         WHERE menu_id = v_opcion_id;
    END IF;

    -- Respeta permisos existentes: union de roles con acceso al acta o al reporte de padron.
    INSERT INTO public.tb_menu_rol (rol_id, menu_id, estado, f_crea, u_crea)
    SELECT DISTINCT mr.rol_id, destino.menu_id, TRUE, NOW(), 'flyway'
      FROM public.tb_menu_rol mr
      JOIN public.tb_menu origen ON origen.menu_id = mr.menu_id
      CROSS JOIN (VALUES (v_reportes_id), (v_opcion_id)) AS destino(menu_id)
     WHERE mr.estado = TRUE
       AND origen.estado = TRUE
       AND origen.menu_url IN ('/actaE.jsf', '/reportePadron.jsf')
    ON CONFLICT (menu_id, rol_id) DO UPDATE
       SET estado = TRUE,
           f_actualiza = NOW(),
           u_actualiza = 'flyway';
END $$;

SELECT setval(
    pg_get_serial_sequence('tec.tipo_documentos', 'tipdoc_id'),
    GREATEST((SELECT COALESCE(MAX(tipdoc_id), 1) FROM tec.tipo_documentos), 1), TRUE
);

SELECT setval(
    pg_get_serial_sequence('public.tb_menu', 'menu_id'),
    GREATEST((SELECT COALESCE(MAX(menu_id), 1) FROM public.tb_menu), 1), TRUE
);
