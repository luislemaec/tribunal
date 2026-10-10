-- Contenido inicial del chatbot de ayuda: BORRADOR para revisión del Tribunal.
-- Redactado a partir de los mensajes y reglas vigentes del sistema. Todas las preguntas se
-- cargan INACTIVAS (estado = FALSE): el Tribunal las revisa y activa desde TRIBUNAL > Ayuda.
-- Idempotente: no duplica una pregunta con el mismo texto.

WITH datos (pregunta, respuesta, claves, pagina, fase, enlace, orden, roles) AS (
  VALUES
    ('¿Por qué no puedo editar el RUC de mi iglesia?',
     'Una vez guardada la iglesia, el campo RUC queda bloqueado.
Solo los usuarios Administrador y Tribunal pueden modificarlo. Si el RUC de su iglesia es incorrecto, comuníquese con el Tribunal por los canales oficiales.',
     'ruc bloqueado cambiar corregir modificar', 'iglesias.jsf', NULL, 'iglesias.jsf', 10, 'SITEC-IglesiaAdmin'),
    ('Mi iglesia no tiene RUC, ¿cómo la registro?',
     'En el formulario de la iglesia seleccione «No tiene RUC». El sistema asigna un código interno que empieza con 00.
Cuando la iglesia obtenga su RUC, el Administrador o el Tribunal pueden reemplazar ese código por el RUC real.',
     'sin ruc codigo interno generico 00', 'iglesias.jsf', NULL, 'iglesias.jsf', 20, 'SITEC-Administrador,SITEC-Tribunal,SITEC-IglesiaAdmin'),
    ('¿Puedo cambiar el RUC de una iglesia ya registrada?',
     'Sí, como Administrador o Tribunal. Abra la iglesia en Iglesias, cambie el RUC y guarde. También puede pasar de «No tiene RUC» a un RUC real y al revés.
El sistema no permite repetir el RUC de otra iglesia, aunque esté eliminada: en ese caso restaure la iglesia eliminada en lugar de crear una nueva.',
     'editar ruc cambiar duplicado eliminada restaurar', 'iglesias.jsf', NULL, 'iglesias.jsf', 30, 'SITEC-Administrador,SITEC-Tribunal'),
    ('¿Por qué no puedo registrar iglesias?',
     'El registro de iglesias solo está habilitado durante la fase del cronograma que lo permite. Si aparece «El registro de iglesias no está habilitado en la fase electoral vigente», la fase actual no lo permite.
Los usuarios administradores de iglesia no crean iglesias: solo gestionan la iglesia que tienen asignada.',
     'crear nueva iglesia registro cronograma fase inscripcion', 'iglesias.jsf', 'INSCRIPCION_IGLESIAS', 'iglesias.jsf', 40, NULL),
    ('¿Qué significa que una persona esté «habilitada»?',
     'Que está habilitada para participar en las elecciones.
No significa que ya tenga una mesa asignada: la asignación al padrón de una mesa es un paso posterior. Solo las personas habilitadas pueden incorporarse al padrón.',
     'habilitado habilitada padron participar votar elecciones', 'personas.jsf', NULL, 'personas.jsf', 10, NULL),
    ('¿Cómo habilito a un miembro para las elecciones?',
     'En Personas, edite al miembro y marque que está habilitado para participar en las elecciones. Revise antes que sus datos (cédula y nombres) sean correctos.
Solo es posible mientras la fase del cronograma permita actualizar miembros.',
     'habilitar miembro persona marcar', 'personas.jsf', 'ACTUALIZACION_MIEMBROS', 'personas.jsf', 20, 'SITEC-Administrador,SITEC-Tribunal,SITEC-IglesiaAdmin'),
    ('¿Por qué no puedo actualizar a los miembros de mi iglesia?',
     'La actualización de miembros solo está abierta durante la fase del cronograma que la permite. Si aparece «La actualización del padrón está cerrada por el cronograma electoral», la fase actual no lo permite.
Si necesita corregir datos fuera de esa fase, comuníquese con el Tribunal.',
     'no puedo editar miembros cerrado cronograma actualizacion', 'personas.jsf', NULL, 'personas.jsf', 30, NULL),
    ('Dice que la persona ya está registrada en otra iglesia, ¿qué hago?',
     'Una persona solo puede pertenecer a una iglesia. Si ya está registrada en otra, no se puede asociar a la suya.
Si la persona cambió de iglesia, comuníquese con el Tribunal: es quien regulariza estos casos.',
     'otra iglesia registrada asociar cambio de iglesia', 'personas.jsf', NULL, NULL, 40, 'SITEC-IglesiaAdmin'),
    ('La cédula ingresada ya pertenece a otra persona, ¿qué significa?',
     'Ya existe en el sistema otra persona con ese número de cédula. Verifique que la cédula esté bien escrita.
Si es correcta, puede tratarse de un registro duplicado: comuníquese con el Tribunal para revisarlo.',
     'cedula duplicada repetida documento existe', 'personas.jsf', NULL, NULL, 50, NULL),
    ('¿Cómo regularizo a una persona que aparece en varias iglesias?',
     'Solo un usuario Tribunal puede hacerlo. En Personas, use la acción «Regularizar iglesias» sobre la persona marcada con inconsistencia y elija la única iglesia que quedará activa.
Las demás relaciones se dan de baja y conservan su historial. Mientras tanto, la edición normal de esa persona está bloqueada.',
     'inconsistencia varias iglesias regularizar duplicado', 'personas.jsf', NULL, 'personas.jsf', 60, 'SITEC-Tribunal'),
    ('¿Qué requisitos debe cumplir mi contraseña?',
     'Entre 8 y 16 caracteres, al menos una letra mayúscula, una minúscula y un número, sin espacios en blanco. Los símbolos son opcionales.
La nueva contraseña debe ser distinta de la actual.',
     'contrasena clave requisitos segura longitud', 'cambioClave.jsf', NULL, NULL, 10, NULL),
    ('¿Por qué me pide cambiar la contraseña al ingresar?',
     'La primera vez que ingresa, su usuario tiene una contraseña temporal. El sistema le pide cambiarla antes de continuar.
Cámbiela por una fácil de recordar para usted y difícil de adivinar para los demás.',
     'clave temporal primer ingreso cambiar contrasena', 'cambioClave.jsf', NULL, NULL, 20, NULL),
    ('Olvidé mi contraseña, ¿cómo la recupero?',
     'En la pantalla de inicio de sesión use la opción para recuperar la contraseña e ingrese su correo registrado.
Si el correo está registrado, recibirá un enlace de un solo uso para crear una nueva contraseña. El enlace vence en 30 minutos.',
     'olvide clave recuperar contrasena correo enlace', 'cambioClave.jsf', NULL, NULL, 30, NULL),
    ('El enlace para restablecer la contraseña no funciona',
     'El enlace sirve una sola vez y vence a los 30 minutos. Si ya lo usó o pasó ese tiempo, solicite uno nuevo desde la pantalla de inicio de sesión.
Revise también la carpeta de correo no deseado.',
     'enlace vencido expirado restablecer correo no llega', 'cambioClave.jsf', NULL, NULL, 40, NULL),
    ('¿Por qué se cerró mi sesión?',
     'Por seguridad, la sesión se cierra tras unos 15 minutos sin actividad. Un minuto antes aparece un aviso con cuenta regresiva: pulse Continuar para seguir trabajando.
Mientras escribe o usa el sistema, la sesión se mantiene abierta aunque no guarde. Si se cerró, vuelva a iniciar sesión; lo que no guardó debe ingresarse de nuevo.',
     'sesion cerrada expirada tiempo inactividad', NULL, NULL, NULL, 10, NULL),
    ('No veo una opción en el menú, ¿por qué?',
     'Las opciones del menú dependen de su rol. Si necesita acceder a una pantalla que no aparece, comuníquese con el administrador del sistema.',
     'menu opcion no aparece permiso acceso rol', NULL, NULL, NULL, 20, NULL),
    ('Dice que mi usuario no tiene una iglesia asignada',
     'Su usuario de administrador de iglesia aún no está vinculado a una iglesia, o la iglesia no está activa. Comuníquese con el administrador del sistema para que revise la asignación.',
     'sin iglesia asignada usuario', 'dashboard.jsf', NULL, NULL, 10, 'SITEC-IglesiaAdmin'),
    ('¿Dónde veo la fase vigente del proceso electoral?',
     'En el Escritorio se muestran el proceso electoral activo y la fase vigente del cronograma. Varias acciones (registro de iglesias, actualización de miembros, asignación de administradores) solo están disponibles en su fase.',
     'fase vigente cronograma proceso activo escritorio', 'dashboard.jsf', NULL, 'dashboard.jsf', 20, NULL),
    ('¿Cómo asigno el administrador de una iglesia?',
     'En Iglesias, use la acción de asignar administrador y elija una persona que pertenezca activamente a esa iglesia. Solo está disponible en la fase del cronograma que lo permite.
Una persona solo puede administrar una iglesia.',
     'asignar administrador iglesia usuario', 'iglesias.jsf', 'ASIGNACION_USUARIOS', 'iglesias.jsf', 50, 'SITEC-Administrador,SITEC-Tribunal'),
    ('No puedo asignar el administrador por cédulas duplicadas',
     'La asignación se bloquea cuando existen personas activas duplicadas con la misma cédula. Corrija primero los duplicados en Personas y luego vuelva a asignar el administrador.',
     'administrador bloqueado cedula duplicada asignar', 'iglesias.jsf', NULL, 'personas.jsf', 60, 'SITEC-Administrador,SITEC-Tribunal'),
    ('¿Cómo genero el padrón de una mesa?',
     'En Padrón, seleccione el proceso y la mesa, y asigne las iglesias de la ubicación. El sistema incorpora a las personas habilitadas de esas iglesias.
Una iglesia asignada a una mesa de un proceso no aparece disponible para otra mesa del mismo proceso.',
     'generar padron mesa asignar iglesias', 'padron.jsf', NULL, 'padron.jsf', 10, 'SITEC-Administrador,SITEC-Tribunal'),
    ('No puedo quitar una iglesia de la mesa',
     'No se pueden quitar del padrón los registros que ya registran sufragio. Si no hay registros que quitar, el sistema lo indica.',
     'quitar iglesia mesa padron sufragio', 'padron.jsf', NULL, NULL, 20, 'SITEC-Administrador,SITEC-Tribunal'),
    ('Mi usuario de Presidente de Mesa no tiene mesa asignada',
     'El sistema no encuentra una mesa asignada a su usuario en el proceso electoral activo. Comuníquese con el Tribunal para que revise su designación en la Junta Receptora del Voto.',
     'presidente mesa sin mesa asignada jrv', 'actaE.jsf', NULL, NULL, 10, 'SITEC-Presidente-mesa'),
    ('¿Por qué debo registrar la apertura de la mesa?',
     'La apertura de la mesa es obligatoria antes de ingresar o cerrar el conteo de votos. Regístrela al iniciar la jornada.',
     'apertura mesa conteo iniciar', 'actaE.jsf', NULL, 'actaE.jsf', 20, 'SITEC-Presidente-mesa'),
    ('¿Puedo guardar el conteo sin cerrar la mesa?',
     'Sí. Guarde el conteo como borrador y complételo después. Al cerrar la mesa ya no podrá modificar el conteo sin autorización.',
     'borrador conteo guardar cerrar mesa', 'actaE.jsf', NULL, 'actaE.jsf', 30, 'SITEC-Presidente-mesa'),
    ('¿Qué es el cuadre de papeletas?',
     'Es la diferencia entre sufragantes, votos emitidos (válidos, nulos y blancos) y papeletas no utilizadas. Cuando está en cero, todas las papeletas están justificadas.',
     'cuadre papeletas votos nulos blancos sufragantes', 'actaE.jsf', NULL, NULL, 40, 'SITEC-Presidente-mesa,SITEC-Tribunal'),
    ('¿Cómo cargo el acta física?',
     'Cargue una fotografía legible del acta llenada a mano. Quedará pendiente de revisión del Tribunal.',
     'acta fisica foto fotografia cargar subir', 'actaE.jsf', NULL, 'actaE.jsf', 50, 'SITEC-Presidente-mesa'),
    ('Dice que el archivo del acta no está disponible',
     'Existe el registro del acta, pero el archivo no está disponible o no supera la verificación de integridad. Puede generar nuevamente el acta para recuperar el documento.',
     'acta archivo no disponible integridad generar', 'actaE.jsf', NULL, 'actaE.jsf', 60, 'SITEC-Presidente-mesa,SITEC-Tribunal'),
    ('¿Qué muestra el Reporte de registros?',
     'Tiene varias pestañas: Registros (personas habilitadas por cantón y parroquia), Cobertura del padrón, Iglesias, Administradores y Actividad del sistema. Cada una tiene su filtro de cantón; al elegir un cantón se muestran sus parroquias.',
     'reporte registros estadisticas cobertura administradores', 'reporteRegistros.jsf', NULL, 'reporteRegistros.jsf', 10, 'SITEC-Administrador,SITEC-Tribunal'),
    ('¿Qué actividades veo en Actividades?',
     'El Administrador ve la bitácora de todos los usuarios. El Tribunal ve las actividades de todos los usuarios excepto las de los usuarios Administrador. Los demás roles ven solo sus propias actividades.',
     'bitacora actividades auditoria historial', 'procesos.jsf', NULL, 'procesos.jsf', 10, 'SITEC-Administrador,SITEC-Tribunal')
), nuevas AS (
  INSERT INTO tec.ayuda_pregunta (ayup_pregunta, ayup_respuesta, ayup_palabras_clave, ayup_pagina, ayup_fase,
                                  ayup_enlace_pagina, ayup_orden, ayup_texto_busqueda, estado, f_crea, u_crea)
  SELECT d.pregunta, d.respuesta, d.claves, d.pagina, d.fase, d.enlace, d.orden,
         -- Misma normalización que TextoAyuda (minúsculas, sin acentos).
         translate(lower(d.pregunta || ' ' || COALESCE(d.claves, '') || ' ' || d.respuesta),
                   'áéíóúüñ', 'aeiouun'),
         FALSE, NOW(), 'flyway'
    FROM datos d
   WHERE NOT EXISTS (SELECT 1 FROM tec.ayuda_pregunta p WHERE p.ayup_pregunta = d.pregunta)
  RETURNING ayup_id, ayup_pregunta
)
INSERT INTO tec.ayuda_pregunta_rol (ayup_id, rol_id)
SELECT n.ayup_id, r.rol_id
  FROM nuevas n
  JOIN datos d ON d.pregunta = n.ayup_pregunta
  JOIN public.tb_rol r ON r.rol_nombre = ANY (string_to_array(d.roles, ','));
