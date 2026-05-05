/* ============================================================
 * tribunal-globals.js
 * Capa de UX global del sistema Tribunal Electoral.
 *
 * Integra:
 *   - NProgress: barra superior durante ajax JSF/PrimeFaces
 *   - PrimeFaces <p:growl widgetVar="msgs"> (definido en globals.xhtml)
 *     accesible vía Tribunal.toast.* y Tribunal.notify(...)
 *   - SweetAlert2: helper global Tribunal.confirm(...) / Tribunal.alert(...)
 *   - Inputmask: máscaras automáticas para .mask-cedula, .mask-ruc,
 *     .mask-fecha, .mask-telefono
 *
 * Todos los assets se sirven localmente desde resources/demo (sin CDN).
 * El template.xhtml los carga vía h:outputScript/Stylesheet en este orden:
 *   nprogress.min.css/js
 *   sweetalert2.min.css / sweetalert2.all.min.js
 *   jquery.inputmask.min.js
 *   tribunal-globals.js (este archivo)
 * ============================================================ */
(function (global) {
    'use strict';

    var Tribunal = global.Tribunal || {};

    // ------------------------------------------------------------------
    // Notificaciones flotantes — PrimeFaces <p:growl widgetVar="msgs">
    //
    // El componente está definido en /WEB-INF/globals.xhtml con autoUpdate,
    // de modo que aplica a todas las páginas del sistema.
    //
    // Uso desde JS de página:
    //   Tribunal.toast.success('Operación exitosa');
    //   Tribunal.toast.warn('Cuidado', 'Verifique los datos');
    //
    // Uso desde controllers Java (recomendado):
    //   JsfUtil.addInfoMessage(...) / addSuccessMessage(...) / etc.
    //   El growl con autoUpdate los muestra sin tocar UI.
    // ------------------------------------------------------------------
    /**
     * Muestra un mensaje en el <p:growl widgetVar="msgs"> global.
     * En PrimeFaces 11 el growl expone renderMessage(msg) como API JS.
     * El objeto debe tener {severity, summary, detail}.
     */
    function showToast(severity, summary, detail) {
        if (!global.PF) { return; }
        var widget = PF('msgs');
        if (!widget) { return; }
        try {
            widget.renderMessage({
                severity: severity || 'info',
                summary: summary || '',
                detail: detail || ''
            });
        } catch (e) {
            // Fallback: log si la versión de PF no expone renderMessage.
            if (global.console) { console.warn('PF growl renderMessage no disponible:', e); }
        }
    }

    Tribunal.toast = {
        info:    function (m, t) { showToast('info',    t || 'Información', m); },
        success: function (m, t) { showToast('success', t || 'Éxito',       m); },
        warn:    function (m, t) { showToast('warn',    t || 'Advertencia', m); },
        error:   function (m, t) { showToast('error',   t || 'Error',       m); }
    };

    // ------------------------------------------------------------------
    // SweetAlert2 helpers
    // ------------------------------------------------------------------
    if (global.Swal) {
        Tribunal.confirm = function (opts) {
            opts = opts || {};
            return Swal.fire({
                title: opts.title || '¿Está seguro?',
                text: opts.text || 'Esta acción no se puede deshacer',
                icon: opts.icon || 'warning',
                showCancelButton: true,
                confirmButtonText: opts.confirmText || 'Sí, continuar',
                cancelButtonText: opts.cancelText || 'Cancelar',
                confirmButtonColor: opts.confirmColor || '#d33',
                cancelButtonColor: '#6c757d',
                reverseButtons: true
            });
        };
        Tribunal.alert = function (msg, icon) {
            return Swal.fire({ text: msg, icon: icon || 'info', confirmButtonText: 'OK' });
        };
    }

    // ------------------------------------------------------------------
    // NProgress: engancha al ciclo ajax de JSF
    // ------------------------------------------------------------------
    if (global.NProgress && global.jsf && global.jsf.ajax) {
        NProgress.configure({ showSpinner: false, trickleSpeed: 200 });
        jsf.ajax.addOnEvent(function (data) {
            if (data.status === 'begin')   { NProgress.start();  }
            if (data.status === 'success') { NProgress.done();   }
            if (data.status === 'complete'){ NProgress.done();   }
        });
        jsf.ajax.addOnError(function () { NProgress.done(); });
    }

    // ------------------------------------------------------------------
    // Puente programático FacesMessages -> growl
    // Útil cuando el JS necesita emitir un mensaje con la misma forma
    // que produce el server (severity + summary + detail).
    // ------------------------------------------------------------------
    Tribunal.notify = function (severity, summary, detail) {
        var s = (severity || 'info').toLowerCase();
        if (s === 'fatal' || s === 'error')   { showToast('error',   summary, detail); return; }
        if (s === 'warn'  || s === 'warning') { showToast('warn',    summary, detail); return; }
        if (s === 'success')                  { showToast('success', summary, detail); return; }
        showToast('info', summary, detail);
    };

    // ------------------------------------------------------------------
    // Inputmask global
    // ------------------------------------------------------------------
    function applyMasks(scope) {
        if (!global.Inputmask) { return; }
        var $root = scope ? jQuery(scope) : jQuery(document);
        try {
            Inputmask({ mask: '9999999999', placeholder: '_' }).mask($root.find('.mask-cedula').toArray());
            Inputmask({ mask: '9999999999999', placeholder: '_' }).mask($root.find('.mask-ruc').toArray());
            Inputmask('99/99/9999').mask($root.find('.mask-fecha').toArray());
            Inputmask({ mask: ['(999) 999-9999', '+99 999 999 9999'], keepStatic: true })
                .mask($root.find('.mask-telefono').toArray());
        } catch (e) { /* noop */ }
    }
    Tribunal.applyMasks = applyMasks;

    if (global.jQuery) {
        jQuery(function () { applyMasks(); });
        // re-aplicar tras cualquier ajax JSF
        if (global.jsf && global.jsf.ajax) {
            jsf.ajax.addOnEvent(function (d) { if (d.status === 'success') { applyMasks(); } });
        }
    }

    // ------------------------------------------------------------------
    // Atajos de teclado mínimos (Esc cierra el último p:dialog)
    // ------------------------------------------------------------------
    if (global.jQuery && global.PrimeFaces) {
        jQuery(document).on('keydown', function (e) {
            if (e.key === 'Escape') {
                jQuery('.ui-dialog:visible:last .ui-dialog-titlebar-close').trigger('click');
            }
        });
    }

    global.Tribunal = Tribunal;
}(window));
