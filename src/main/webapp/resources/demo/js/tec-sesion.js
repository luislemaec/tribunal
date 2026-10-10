/* ============================================================
 * tec-sesion.js
 * Inactividad de la sesión web, alineada con el timeout del servidor (web.xml).
 *
 * El servidor cuenta desde la última PETICIÓN; el usuario percibe desde su última
 * ACTIVIDAD (teclado, ratón, toque). Para que coincidan:
 *  - mientras hay actividad, se renueva la sesión con GET /sesion/ping como máximo una
 *    vez por minuto (también cuenta cualquier petición AJAX de PrimeFaces);
 *  - el plazo se calcula desde el último contacto con el servidor, así el navegador
 *    siempre avisa y cierra ANTES de que el servidor destruya la sesión;
 *  - el último contacto se comparte entre pestañas (localStorage): trabajar en una
 *    pestaña mantiene la sesión de todas.
 *
 * Aviso: 60 s antes del cierre, con cuenta regresiva (diálogo dlgIdle de globals.xhtml).
 * Cierre: 30 s antes del timeout del servidor, con tecCerrarSesionExpirada (LoginBean),
 * que registra la causa en la bitácora y lleva a login.jsf?expirado=1.
 *
 * Sesiones QR (acta parcial): urlPing = null. Su filtro invalida la sesión ante rutas no
 * permitidas, así que no se hace ping; el plazo corre desde la última petición AJAX.
 * ============================================================ */
(function (global) {
    'use strict';

    var CLAVE_CONTACTO = 'tec.sesion.ultimoContacto';
    var MARGEN_CIERRE_MS = 30000;
    var ANTICIPO_AVISO_MS = 60000;
    var INTERVALO_PING_MS = 60000;
    var EVENTOS_ACTIVIDAD = ['keydown', 'mousedown', 'mousemove', 'wheel', 'touchstart', 'scroll'];

    var cfg = null;
    var ultimoContacto = 0;
    var ultimaActividad = 0;
    var pingEnCurso = false;
    var avisoVisible = false;
    var cerrando = false;

    function ahora() {
        return Date.now();
    }

    function leerCompartido() {
        try {
            var valor = parseInt(global.localStorage.getItem(CLAVE_CONTACTO), 10);
            return isNaN(valor) ? 0 : valor;
        } catch (e) {
            return 0;
        }
    }

    function registrarContacto(instante) {
        ultimoContacto = Math.max(ultimoContacto, instante);
        try {
            if (leerCompartido() < ultimoContacto) {
                global.localStorage.setItem(CLAVE_CONTACTO, String(ultimoContacto));
            }
        } catch (e) {
            /* Sin almacenamiento: cada pestaña lleva su propio contacto. */
        }
    }

    function contactoVigente() {
        return Math.max(ultimoContacto, leerCompartido());
    }

    function ping(alTerminar) {
        if (!cfg.urlPing) {
            // Sesión sin ping (QR): solo cuentan las peticiones AJAX normales.
            if (alTerminar) {
                alTerminar();
            }
            return;
        }
        if (pingEnCurso || cerrando) {
            return;
        }
        pingEnCurso = true;
        var inicio = ahora();
        global.fetch(cfg.urlPing, {method: 'GET', credentials: 'same-origin', cache: 'no-store', redirect: 'manual'})
            .then(function (respuesta) {
                if (respuesta.status === 204) {
                    registrarContacto(inicio);
                }
            })
            .catch(function () { /* Sin red: el plazo sigue corriendo desde el último contacto. */ })
            .then(function () {
                pingEnCurso = false;
                if (alTerminar) {
                    alTerminar();
                }
            });
    }

    function alActividad() {
        // Con el aviso abierto solo cuenta el botón Continuar: mover el ratón no extiende la sesión.
        if (avisoVisible || cerrando) {
            return;
        }
        ultimaActividad = ahora();
    }

    function widgetAviso() {
        return global.PF ? global.PF('dlgIdle') : null;
    }

    function mostrarAviso(restanteMs) {
        var segundos = Math.max(0, Math.ceil(restanteMs / 1000));
        var contador = global.document.getElementById('tecSesionSegundos');
        if (contador) {
            contador.textContent = String(segundos);
        }
        if (!avisoVisible) {
            avisoVisible = true;
            var dialogo = widgetAviso();
            if (dialogo) {
                dialogo.show();
            }
        }
    }

    function ocultarAviso() {
        if (avisoVisible) {
            avisoVisible = false;
            var dialogo = widgetAviso();
            if (dialogo) {
                dialogo.hide();
            }
        }
    }

    function cerrar() {
        if (cerrando) {
            return;
        }
        cerrando = true;
        ocultarAviso();
        if (typeof global.tecCerrarSesionExpirada === 'function') {
            global.tecCerrarSesionExpirada();
            // Respaldo si la petición no redirige (sesión ya destruida, red caída).
            global.setTimeout(function () { global.location.href = cfg.urlLogin; }, 5000);
        } else {
            global.location.href = cfg.urlLogin;
        }
    }

    function revisar() {
        if (cerrando) {
            return;
        }
        var contacto = contactoVigente();
        var instante = ahora();
        var cierre = contacto + cfg.timeoutMs - MARGEN_CIERRE_MS;
        var restante = cierre - instante;

        if (restante <= 0) {
            cerrar();
            return;
        }
        if (restante <= ANTICIPO_AVISO_MS) {
            mostrarAviso(restante);
            return;
        }
        // Otra pestaña renovó la sesión mientras el aviso estaba abierto.
        ocultarAviso();
        if (ultimaActividad > contacto && instante - contacto >= INTERVALO_PING_MS) {
            ping();
        }
    }

    global.TecSesion = {
        /**
         * @param {{timeoutSegundos:number, urlPing:string, conPing:boolean, urlLogin:string}} opciones
         */
        iniciar: function (opciones) {
            if (cfg || !opciones || !(opciones.timeoutSegundos > 0) || !global.fetch) {
                return;
            }
            cfg = {
                timeoutMs: opciones.timeoutSegundos * 1000,
                urlPing: opciones.conPing === false ? null : opciones.urlPing,
                urlLogin: opciones.urlLogin
            };
            // La carga de la página fue una petición: la sesión acaba de renovarse.
            registrarContacto(ahora());
            ultimaActividad = ahora();
            EVENTOS_ACTIVIDAD.forEach(function (evento) {
                global.addEventListener(evento, alActividad, {passive: true, capture: true});
            });
            // Toda respuesta AJAX de PrimeFaces también renovó la sesión en el servidor.
            if (global.jQuery) {
                global.jQuery(global.document).on('pfAjaxComplete', function () {
                    registrarContacto(ahora());
                });
            }
            global.setInterval(revisar, 1000);
        },

        /** Botón Continuar del aviso. */
        continuar: function () {
            ultimaActividad = ahora();
            ping(function () {
                ocultarAviso();
                revisar();
            });
        }
    };
}(window));
