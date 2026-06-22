-- ============================================================================
-- TEC - Permisos del usuario runtime de TribunalDS
-- ----------------------------------------------------------------------------
-- Ejecutar como propietario de los objetos o administrador PostgreSQL.
-- Cambiar una sola vez el valor de app_role por el usuario real de WildFly.
-- No conceder estos permisos a PUBLIC.
-- ============================================================================

DO $grant_runtime$
DECLARE
    app_role NAME := 'tec_user';
BEGIN
    IF app_role = 'tec_user' THEN
        RAISE EXCEPTION 'Debe reemplazar tec_user por el usuario real de TribunalDS';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = app_role) THEN
        RAISE EXCEPTION 'El rol PostgreSQL % no existe', app_role;
    END IF;

    EXECUTE format('GRANT USAGE ON SCHEMA public, tec TO %I', app_role);
    EXECUTE format(
        'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public, tec TO %I',
        app_role
    );
    EXECUTE format(
        'GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public, tec TO %I',
        app_role
    );

    -- Objetos futuros creados por el usuario que ejecuta este bloque.
    EXECUTE format(
        'ALTER DEFAULT PRIVILEGES IN SCHEMA public, tec '
        'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
        app_role
    );
    EXECUTE format(
        'ALTER DEFAULT PRIVILEGES IN SCHEMA public, tec '
        'GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO %I',
        app_role
    );

    IF NOT has_schema_privilege(app_role, 'tec', 'USAGE') THEN
        RAISE EXCEPTION 'No se pudo conceder USAGE sobre el schema tec a %', app_role;
    END IF;

    IF NOT has_table_privilege(app_role, 'tec.proceso_electoral', 'SELECT') THEN
        RAISE EXCEPTION 'No se pudo conceder SELECT sobre tec.proceso_electoral a %', app_role;
    END IF;

    RAISE NOTICE 'Permisos runtime concedidos correctamente a %', app_role;
END
$grant_runtime$;

-- Verificar conectandose con las mismas credenciales de TribunalDS:
-- SELECT current_user, session_user;
-- SELECT has_schema_privilege(current_user, 'tec', 'USAGE');
-- SELECT has_table_privilege(current_user, 'tec.proceso_electoral', 'SELECT');
-- SELECT has_table_privilege(current_user, 'public.tb_iglesia', 'UPDATE');
