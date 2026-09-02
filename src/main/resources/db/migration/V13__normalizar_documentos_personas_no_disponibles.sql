-- Normaliza exclusivamente documentos historicos N/D usando el mismo formato
-- institucional de V12. SN-{pers_id} es un identificador interno historico y
-- no representa una cedula ecuatoriana.

DO $$
DECLARE
    v_antes        BIGINT;
    v_actualizados BIGINT;
    v_restantes    BIGINT;
    v_longitud     INTEGER;
    v_requerida    INTEGER;
    v_personas     INTEGER[];
BEGIN
    SELECT c.character_maximum_length
      INTO v_longitud
      FROM information_schema.columns c
     WHERE c.table_schema = 'public'
       AND c.table_name = 'tb_persona'
       AND c.column_name = 'pers_documento';

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No existe public.tb_persona.pers_documento; se cancela la normalizacion.';
    END IF;

    SELECT COUNT(*),
           COALESCE(MAX(LENGTH('SN-' || p.pers_id::TEXT)), 0),
           COALESCE(ARRAY_AGG(p.pers_id ORDER BY p.pers_id), ARRAY[]::INTEGER[])
      INTO v_antes, v_requerida, v_personas
      FROM public.tb_persona p
     WHERE BTRIM(p.pers_documento) = 'N/D';

    IF v_longitud IS NOT NULL AND v_requerida > v_longitud THEN
        RAISE EXCEPTION
            'pers_documento admite % caracteres y se requieren %.',
            v_longitud, v_requerida;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.tb_persona origen
          JOIN public.tb_persona existente
            ON BTRIM(existente.pers_documento) = 'SN-' || origen.pers_id::TEXT
           AND existente.pers_id <> origen.pers_id
         WHERE BTRIM(origen.pers_documento) = 'N/D'
    ) THEN
        RAISE EXCEPTION
            'Existe al menos un valor SN-{id} asignado a otra persona; se cancela la normalizacion.';
    END IF;

    UPDATE public.tb_persona p
       SET pers_documento = 'SN-' || p.pers_id::TEXT
     WHERE BTRIM(p.pers_documento) = 'N/D';

    GET DIAGNOSTICS v_actualizados = ROW_COUNT;

    IF v_actualizados <> v_antes THEN
        RAISE EXCEPTION
            'Conteo inconsistente: se esperaban % actualizaciones y se realizaron %.',
            v_antes, v_actualizados;
    END IF;

    SELECT COUNT(*)
      INTO v_restantes
      FROM public.tb_persona p
     WHERE BTRIM(p.pers_documento) = 'N/D';

    IF v_restantes <> 0 THEN
        RAISE EXCEPTION
            'Persisten % personas con documento N/D despues de la normalizacion.',
            v_restantes;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.tb_persona p
         WHERE p.pers_id = ANY(v_personas)
           AND p.pers_documento IS DISTINCT FROM 'SN-' || p.pers_id::TEXT
    ) THEN
        RAISE EXCEPTION
            'Al menos un documento generado no corresponde con el pers_id de la persona.';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.tb_persona normalizada
          JOIN public.tb_persona otra
            ON BTRIM(otra.pers_documento) = normalizada.pers_documento
           AND otra.pers_id <> normalizada.pers_id
         WHERE normalizada.pers_id = ANY(v_personas)
    ) THEN
        RAISE EXCEPTION
            'La normalizacion genero al menos un documento duplicado.';
    END IF;

    RAISE NOTICE
        'Personas con N/D antes: %, actualizadas: %, restantes: %.',
        v_antes, v_actualizados, v_restantes;
END;
$$;
