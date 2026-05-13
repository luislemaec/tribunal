--
-- JBoss, Home of Professional Open Source
-- Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
-- contributors by the @authors tag. See the copyright.txt in the
-- distribution for a full listing of individual contributors.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- http://www.apache.org/licenses/LICENSE-2.0
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- You can use this file to load seed data into the database using SQL statements
ALTER SEQUENCE public.tb_iglesia_igl_id_seq RESTART WITH 1 OWNED BY public.tb_iglesia.igl_id;
ALTER SEQUENCE public.tb_iglesia_igl_id_seq OWNER TO postgres;

ALTER SEQUENCE public.tb_iglesia_persona_igpe_id_seq RESTART WITH 1 OWNED BY public.tb_iglesia_persona.igpe_id;
ALTER SEQUENCE public.tb_iglesia_persona_igpe_id_seq OWNER TO postgres;

ALTER SEQUENCE public.tb_persona_pers_id_seq RESTART WITH 8 OWNED BY public.tb_persona.pers_id;
ALTER SEQUENCE public.tb_persona_pers_id_seq OWNER TO postgres;

ALTER SEQUENCE tec.padron_padron_id_seq RESTART WITH 1 OWNED BY tec.padron.padron_id;
ALTER SEQUENCE tec.padron_padron_id_seq OWNER TO postgres;

-- ============================================================================
-- ROLES: IglesiaAdmin y Tribunal
-- Usa SELECT … WHERE NOT EXISTS para ser idempotente (no falla en re-ejecución).
-- ============================================================================

INSERT INTO public.tb_rol (estado, f_crea, u_crea, rol_description, rol_nombre)
SELECT TRUE, NOW(), 'Admin', 'Administrador de iglesias', 'SITEC-IglesiaAdmin'
WHERE NOT EXISTS (
    SELECT 1 FROM public.tb_rol WHERE rol_nombre = 'SITEC-IglesiaAdmin'
);

INSERT INTO public.tb_rol (estado, f_crea, u_crea, rol_description, rol_nombre)
SELECT TRUE, NOW(), 'Admin', 'Tribunal Electoral (cronograma, resoluciones)', 'SITEC-Tribunal'
WHERE NOT EXISTS (
    SELECT 1 FROM public.tb_rol WHERE rol_nombre = 'SITEC-Tribunal'
);

-- ============================================================================
-- MENÚ: Cronograma
-- IMPORTANTE: verificar que menu_padre_id = 11 corresponde al menú padre
-- (nodo raíz "Administración") en la BD destino antes de ejecutar.
-- ============================================================================

INSERT INTO public.tb_menu (
    estado, f_crea, u_crea,
    menu_accion, componente_id, menu_ico, menu_nodo_final,
    menu_nombre, menu_orden, menu_url, menu_padre_id)
SELECT TRUE, NOW(), 'Admin',
       '/cronograma', 'm_cronograma', 'pi pi-calendar', TRUE,
       'Cronograma', 8, '/tribunal/cronograma.jsf', 11
WHERE NOT EXISTS (
    SELECT 1 FROM public.tb_menu WHERE menu_url = '/tribunal/cronograma.jsf'
);

-- ============================================================================
-- MENÚ: Asignación de Usuarios
-- ============================================================================

INSERT INTO public.tb_menu (
    estado, f_crea, u_crea,
    menu_accion, componente_id, menu_ico, menu_nodo_final,
    menu_nombre, menu_orden, menu_url, menu_padre_id)
SELECT TRUE, NOW(), 'Admin',
       '/asignacionUsuario', 'm_asignacionUsuario', 'pi pi-users', TRUE,
       'Asignación', 7, '/tribunal/asignacionUsuarios.jsf', 16
WHERE NOT EXISTS (
    SELECT 1 FROM public.tb_menu WHERE menu_url = '/tribunal/asignacionUsuarios.jsf'
);

-- ============================================================================
-- MENÚ-ROL: asociar los menús a los roles con acceso
-- Patrón: CROSS JOIN filtrado por nombre, guard NOT EXISTS para idempotencia.
-- ============================================================================

-- Cronograma → SITEC-Administrador y SITEC-Tribunal
INSERT INTO public.tb_menu_rol (rol_id, menu_id, estado, f_crea, u_crea)
SELECT r.rol_id, m.menu_id, TRUE, NOW(), 'Admin'
FROM public.tb_rol r
CROSS JOIN public.tb_menu m
WHERE r.rol_nombre IN ('SITEC-Administrador', 'SITEC-Tribunal')
  AND m.menu_url = '/tribunal/cronograma.jsf'
  AND NOT EXISTS (
      SELECT 1 FROM public.tb_menu_rol mr
       WHERE mr.rol_id = r.rol_id AND mr.menu_id = m.menu_id
  );

-- Asignación de Usuarios → SITEC-Administrador y SITEC-Tribunal
INSERT INTO public.tb_menu_rol (rol_id, menu_id, estado, f_crea, u_crea)
SELECT r.rol_id, m.menu_id, TRUE, NOW(), 'Admin'
FROM public.tb_rol r
CROSS JOIN public.tb_menu m
WHERE r.rol_nombre IN ('SITEC-Administrador', 'SITEC-Tribunal')
  AND m.menu_url = '/tribunal/asignacionUsuarios.jsf'
  AND NOT EXISTS (
      SELECT 1 FROM public.tb_menu_rol mr
       WHERE mr.rol_id = r.rol_id AND mr.menu_id = m.menu_id
  );

-- ============================================================================
-- CRONOGRAMA: proceso electoral inicial + fases de arranque
-- ----------------------------------------------------------------------------
-- Las fechas son de referencia para 2026; ajustar según el calendario real
-- antes de ejecutar en producción.
-- Estrategia: subquery por nombre del proceso en cada fase → sin IDs fijos.
-- ============================================================================

-- 1) Proceso electoral base
INSERT INTO tec.proceso_electoral (
    estado, f_crea, u_crea,
    proce_nombre, proce_descripcion,
    proce_fecha_inicio, proce_fecha_fin, proce_activo)
SELECT TRUE, NOW(), 'Admin',
       'Elecciones 2026',
       'Proceso electoral ordinario 2026. Comprende inscripción de iglesias, '
       || 'actualización del padrón, candidaturas, jornada electoral y cierre.',
       '2026-01-01 00:00:00',
       '2026-12-31 23:59:59',
       TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tec.proceso_electoral WHERE proce_nombre = 'Elecciones 2026'
);

-- ── Bloque 1: Registro inicial ───────────────────────────────────────────────

-- 2) INSCRIPCION_IGLESIAS  (orden 1)
INSERT INTO tec.cronograma_fase (
    estado, f_crea, u_crea,
    proce_id, cref_fase, cref_orden, cref_titulo, cref_mensaje,
    cref_severidad, cref_fecha_inicio, cref_fecha_fin, cref_permite_edicion)
SELECT TRUE, NOW(), 'Admin',
       p.proce_id,
       'INSCRIPCION_IGLESIAS', 1,
       'Inscripción de iglesias',
       'Período de inscripción de iglesias al sistema. '
       || 'Las iglesias deben registrar su información institucional y documentación.',
       'INFO',
       '2026-01-05 08:00:00', '2026-01-31 23:59:59',
       FALSE
FROM tec.proceso_electoral p
WHERE p.proce_nombre = 'Elecciones 2026'
  AND NOT EXISTS (
      SELECT 1 FROM tec.cronograma_fase cf
       WHERE cf.proce_id = p.proce_id AND cf.cref_fase = 'INSCRIPCION_IGLESIAS'
  );

-- 3) ASIGNACION_USUARIOS  (orden 2)
INSERT INTO tec.cronograma_fase (
    estado, f_crea, u_crea,
    proce_id, cref_fase, cref_orden, cref_titulo, cref_mensaje,
    cref_severidad, cref_fecha_inicio, cref_fecha_fin, cref_permite_edicion)
SELECT TRUE, NOW(), 'Admin',
       p.proce_id,
       'ASIGNACION_USUARIOS', 2,
       'Asignación de usuarios',
       'Asignación de administradores de iglesia (IglesiaAdmin) a cada congregación registrada.',
       'INFO',
       '2026-02-01 08:00:00', '2026-02-15 23:59:59',
       FALSE
FROM tec.proceso_electoral p
WHERE p.proce_nombre = 'Elecciones 2026'
  AND NOT EXISTS (
      SELECT 1 FROM tec.cronograma_fase cf
       WHERE cf.proce_id = p.proce_id AND cf.cref_fase = 'ASIGNACION_USUARIOS'
  );

-- ── Bloque 2: Gestión de miembros ────────────────────────────────────────────

-- 4) ACTUALIZACION_MIEMBROS  (orden 3 — permite edición del padrón)
INSERT INTO tec.cronograma_fase (
    estado, f_crea, u_crea,
    proce_id, cref_fase, cref_orden, cref_titulo, cref_mensaje,
    cref_severidad, cref_fecha_inicio, cref_fecha_fin, cref_permite_edicion)
SELECT TRUE, NOW(), 'Admin',
       p.proce_id,
       'ACTUALIZACION_MIEMBROS', 3,
       'Actualización de miembros',
       'Los IglesiaAdmin deben revisar y actualizar el listado de miembros de su iglesia. '
       || 'Verifique datos personales y estado de habilitación para el padrón electoral.',
       'WARNING',
       '2026-02-16 08:00:00', '2026-03-15 23:59:59',
       TRUE
FROM tec.proceso_electoral p
WHERE p.proce_nombre = 'Elecciones 2026'
  AND NOT EXISTS (
      SELECT 1 FROM tec.cronograma_fase cf
       WHERE cf.proce_id = p.proce_id AND cf.cref_fase = 'ACTUALIZACION_MIEMBROS'
  );

-- 5) PADRON_PRELIMINAR  (orden 4)
INSERT INTO tec.cronograma_fase (
    estado, f_crea, u_crea,
    proce_id, cref_fase, cref_orden, cref_titulo, cref_mensaje,
    cref_severidad, cref_fecha_inicio, cref_fecha_fin, cref_permite_edicion)
SELECT TRUE, NOW(), 'Admin',
       p.proce_id,
       'PADRON_PRELIMINAR', 4,
       'Padrón preliminar',
       'Publicación del padrón preliminar. Los interesados pueden revisar su estado '
       || 'e interponer observaciones dentro del plazo establecido.',
       'INFO',
       '2026-03-16 08:00:00', '2026-03-31 23:59:59',
       FALSE
FROM tec.proceso_electoral p
WHERE p.proce_nombre = 'Elecciones 2026'
  AND NOT EXISTS (
      SELECT 1 FROM tec.cronograma_fase cf
       WHERE cf.proce_id = p.proce_id AND cf.cref_fase = 'PADRON_PRELIMINAR'
  );

-- 6) PADRON_DEFINITIVO  (orden 5)
INSERT INTO tec.cronograma_fase (
    estado, f_crea, u_crea,
    proce_id, cref_fase, cref_orden, cref_titulo, cref_mensaje,
    cref_severidad, cref_fecha_inicio, cref_fecha_fin, cref_permite_edicion)
SELECT TRUE, NOW(), 'Admin',
       p.proce_id,
       'PADRON_DEFINITIVO', 5,
       'Padrón definitivo',
       'Cierre y publicación del padrón definitivo oficial. '
       || 'No se aceptarán modificaciones al listado de habilitados.',
       'SUCCESS',
       '2026-04-01 08:00:00', '2026-04-15 23:59:59',
       FALSE
FROM tec.proceso_electoral p
WHERE p.proce_nombre = 'Elecciones 2026'
  AND NOT EXISTS (
      SELECT 1 FROM tec.cronograma_fase cf
       WHERE cf.proce_id = p.proce_id AND cf.cref_fase = 'PADRON_DEFINITIVO'
  );