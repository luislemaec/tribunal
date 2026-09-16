# Auditoria de autorizacion TEC

## Regla vigente: Usuarios y Roles (2026-09-16)

La confirmacion posterior del usuario reemplaza la exclusividad administrativa
descrita historicamente abajo SOLO para Usuarios y Roles. Tribunal puede
consultar, crear, editar, deshabilitar y asignar roles cuando su menu autorice
la pagina. LoginFilter conserva listaPermisos como control de esas paginas,
sin la segunda exigencia de Administrador. UsuarioService, RolService y
RolUsuarioService permiten Administrador/Tribunal, incluidos metodos heredados.
No se cambiaron permisos de menu en BD ni el constructor del menu.
Permisos de menu y asignacionUsuarios legacy conservan su alcance anterior.
El control QR precede al filtro normal y no permite paginas administrativas.
Las pruebas de filtro cubren permiso presente/ausente para ambos perfiles;
las pruebas declarativas mantienen MenuRolService exclusivo de Administrador.
La prueba integrada local debe repetirse contra el WAR actualizado: resultados
de despliegues anteriores no validan esta nueva matriz.

## Alcance y limites

Revision estatica de anotaciones, EJB y herencia CRUD, filtros, principal,
roles, descriptores web y configuracion local de WildFly. No equivale a una
prueba integrada de todas las pantallas ni certifica ausencia de vulnerabilidades.
No se modificaron BD, migraciones, realm Elytron ni configuracion del servidor.
Se preservaron cambios preexistentes en el arbol de trabajo.

Roles vigentes confirmados: Administrador, Tribunal, IglesiaAdmin y
Presidente-mesa (prefijo SITEC-). Tecnico y Analista no estan habilitados.
TEC-QR identifica una sesion restringida; no es un permiso administrativo.

## Correcciones aplicadas

### Dependencias EJB de asignacion

AsignacionAdministradorIglesiaService e IglesiaController invocan
obtenerAdminDeIglesia, removerAdminDeIglesia y
obtenerUsuarioPorPersonaIncluyendoInactivos en UsuarioService.
TribunalService invoca findUsuarioPorPersonaIncluyendoInactivos,
provisionarUsuarioExistenteConRol y retirarRolDePersonaSiNoTieneOtrosRoles.
Esos metodos carecian de permisos explicitos en un EJB que ya utiliza seguridad
declarativa. Se autorizan exclusivamente Administrador/Tribunal, conservando
transacciones, validaciones y bajas logicas existentes.
Se cubren ambas sobrecargas publicas de provisionarUsuarioExistenteConRol.

### Identidad normal en LoginFilter

Antes se aceptaba el contexto LoginBean sin comprobar el principal HTTP.
Ahora una peticion protegida sin principal invalida la sesion residual y
redirige a login sin continuar la cadena. Se controla tambien usuario nulo.
El control QR sigue ejecutandose primero. Las rutas publicas permanecen publicas.
La redireccion AJAX sigue usando respuesta parcial, sin forward de ViewState.

### Cambio de clave

cambiarContraseniaAutenticada exige roles vigentes y verifica que username
coincida con el principal. Rechaza TEC-QR y el prefijo TECQR:, antes de consultar
o modificar usuarios. Conserva verificacion BCrypt y validacion de cuenta activa.

## Hallazgos pendientes (no corregidos globalmente)

### Separacion confirmada y aplicada (2026-09-16)

El usuario confirmo que Tribunal solo gestiona cuentas derivadas de autoridades,
Presidentes e IglesiaAdmin. Usuarios, roles y permisos generales son exclusivos
de Administrador. La pantalla antigua asignacionUsuarios queda solo para
Administrador; Tribunal utiliza iglesias.xhtml.

- UsuarioService declara permisos Administrador en CRUD general, consultas
  administrativas, reactivacion, cambio de rol y restablecimiento administrativo.
- UsuarioService, RolService, RolUsuarioService y MenuRolService sobrescriben
  explicitamente los ocho metodos CRUD/consulta heredados con permiso
  Administrador. No se depende de la propagacion de una anotacion de clase a
  metodos declarados en una superclase no EJB.
- Los servicios de roles, relaciones usuario-rol y menu-rol son administrativos.
- Los flujos derivados consultan el rol por ID en BD y exigen el nombre activo
  correspondiente a la operacion. Un nombre manipulado con ID de Administrador
  no permite asignar ni retirar ese rol desde Tribunal.
- LoginFilter rechaza las cuatro pantallas administrativas para no
  administradores, incluso si listaPermisos conserva una entrada historica.
  No se borraron menus ni relaciones historicas de BD; pueden seguir visibles
  hasta ajustar sus permisos de menu, pero no habilitan acceso.
- Pruebas adicionales: CRUD heredado, contratos de roles, ID de rol manipulado,
  Tribunal con menu administrativo historico y acceso normal de Administrador.
- Cambios de esta etapa: UsuarioService.java, RolService.java,
  RolUsuarioService.java, MenuRolService.java, LoginFilter.java,
  messages_es.properties y las dos clases de prueba ya indicadas.

### Pendientes de la auditoria global

- Muchos EJB Service/Facade no declaran permisos. La configuracion local tiene
  default-missing-method-permissions-deny-access=true. Es necesario comprobar
  permisos efectivos por bean y metodo en despliegue, incluyendo herencia y
  llamadas internas; la ausencia textual no prueba por si sola un rechazo.
- AbstractService y AbstractFacade exponen CRUD heredado. Autorizar una clase
  completa sin separar lectura/escritura puede conceder operaciones indebidas.
- UsuarioService conserva metodos de recuperacion publica sin permisos
  explicitos. Recuperacion requiere revisar consumo concurrente del token
  antes de habilitar entradas preautenticacion. CRUD y provision de Presidente
  ya fueron acotados en la etapa descrita arriba.
- Persisten referencias declarativas a Tecnico/Analista en ProcesoService,
  ProcesoFacade, TribunalService y UsuarioService. No se reactivaron roles ni
  se modificaron asignaciones historicas. Falta retirar autorizaciones legacy
  de forma consistente con los puntos de entrada y la carga de roles del realm.
- LoginFilterExcluder usa startsWith tambien para paginas individuales y un
  singleton dependiente del primer contextPath. Requiere pruebas de alias JSF
  y rutas antes de sustituirlo por coincidencia exacta de paginas.
- web.xml declara un security-constraint XHTML sin auth-constraint: no debe
  considerarse sustituto del filtro o de la autorizacion en servicios.
- Dashboard/Iglesia consultan su asignacion mediante
  obtenerContextoIglesiaUsuarioAutenticado, restringido a IglesiaAdmin y basado
  en principal. No se debe traducir EJBAccessException a "sin iglesia".
  La correspondencia efectiva usuario/rol/iglesia necesita prueba con BD.
- PermitAll existentes en canje previo a autenticacion, cierre por listener y
  contexto QR opcional tienen finalidades distintas. No se ampliaron ni se
  eliminaron: cada entrada debe conservar su validacion de token/alcance.

## Pruebas y aceptacion

- PermisosUsuarioServiceTest comprueba los siete metodos/sobrecargas de
  asignacion y el rechazo de cambio de clave QR o de otro principal.
- RedireccionSesionTest incluye sesion normal residual sin principal.
- Ejecutar mvn -Pqr-tests -Dtest=*Test test y mvn -DskipTests compile.
- En WildFly probar login FORM de los cuatro roles vigentes, asignacion y
  reasignacion IglesiaAdmin, alta/baja Tribunal, cambio de clave propio,
  QR sobre mesa propia y rechazo de otra mesa/proceso, expiracion y multipart.
- Las pruebas Java directas no ejercitan RolesAllowedInterceptor de WildFly.
  Esta auditoria permanece abierta hasta completar matriz por metodo y
  aceptacion integrada. No utilizar PermitAll global ni desactivar deny-access.
