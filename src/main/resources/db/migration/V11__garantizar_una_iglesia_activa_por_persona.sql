-- Impide crear nuevas inconsistencias sin decidir automaticamente cual de las
-- relaciones historicas debe conservarse. Los casos existentes permanecen
-- disponibles para el flujo de regularizacion controlado por SITEC-Tribunal.

CREATE OR REPLACE FUNCTION public.fn_validar_iglesia_activa_persona()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_documento TEXT;
BEGIN
    IF NEW.estado IS DISTINCT FROM TRUE THEN
        RETURN NEW;
    END IF;

    SELECT NULLIF(BTRIM(p.pers_documento), '')
      INTO v_documento
      FROM public.tb_persona p
     WHERE p.pers_id = NEW.pers_id;

    IF v_documento IS NULL THEN
        RETURN NEW;
    END IF;

    -- Serializa por documento los intentos concurrentes de asociacion.
    PERFORM pg_advisory_xact_lock(hashtextextended(v_documento, 0));

    IF EXISTS (
        SELECT 1
          FROM public.tb_iglesia_persona ip
          JOIN public.tb_persona p ON p.pers_id = ip.pers_id
         WHERE ip.estado = TRUE
           AND BTRIM(p.pers_documento) = v_documento
           AND ip.igpe_id IS DISTINCT FROM NEW.igpe_id
    ) THEN
        RAISE EXCEPTION
            USING ERRCODE = '23505',
                  MESSAGE = 'La persona ya mantiene una relacion activa con una iglesia.';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_una_iglesia_activa_persona
    ON public.tb_iglesia_persona;

CREATE TRIGGER trg_una_iglesia_activa_persona
BEFORE INSERT OR UPDATE OF estado, pers_id, igl_id
ON public.tb_iglesia_persona
FOR EACH ROW
EXECUTE FUNCTION public.fn_validar_iglesia_activa_persona();
