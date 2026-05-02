/* ============================================================
 * tribunal-globals.js
 * Capa de UX global del sistema Tribunal Electoral.
 *
 * Integra:
 *   - NProgress: barra superior durante ajax JSF/PrimeFaces
 *   - Toastr: notificaciones (puente con FacesMessages)
 *   - SweetAlert2: helper global Tribunal.confirm(...)
 *   - Inputmask: máscaras automáticas para .mask-cedula, .mask-ruc,
 *     .mask-fecha, .mask-telefono
 *
 * Requiere que template.xhtml cargue desde CDN (en este orden):
 *   nprogress.js / .css
 *   toastr.min.js / .css
 *   sweetalert2.all.min.js
 *   jquery.inputmask.min.js
 * ============================================================ */
(function (global) {
    'use strict';

    var Tribunal = global.Tribunal || {};

    // ------------------------------------------------------------------
    // Toastr (configuración por defecto)
    // ------------------------------------------------------------------
    if (global.toastr) {
        toastr.options = {
            closeButton: true,
            progressBar: true,
            positionClass: 'toast-top-right',
            timeOut: 4000,
            extendedTimeOut: 1500,
            newestOnTop: true,
            preventDuplicates: true
        };
        Tribunal.toast = {
            info:    function (m, t) { toastr.info(m, t || ''); },
            success: function (m, t) { toastr.success(m, t || ''); },
            warn:    function (m, t) { toastr.warning(m, t || ''); },
            error:   function (m, t) { toastr.error(m, t || ''); }
        };
    }

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
    // Puente FacesMessages -> Toastr
    // Para usarlo: en cualquier página, agrega:
    //   <p:growl id="msgs" widgetVar="msgs" globalOnly="true"
    //            autoUpdate="true" showDetail="true"/>
    // y luego invoca Tribunal.flushFacesMessages() después del ajax,
    // o deja que el observer de abajo lo capture automáticamente.
    // ------------------------------------------------------------------
    Tribunal.notify = function (severity, summary, detail) {
        if (!global.toastr) { return; }
        var msg = (detail && detail.length) ? detail : (summary || '');
        switch ((severity || 'info').toLowerCase()) {
            case 'error': case 'fatal': toastr.error(msg, summary); break;
            case 'warn':  case 'warning': toastr.warning(msg, summary); break;
            case 'info':  toastr.info(msg, summary); break;
            case 'success': toastr.success(msg, summary); break;
            default: toastr.info(msg, summary);
        }
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
