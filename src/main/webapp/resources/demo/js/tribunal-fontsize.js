/* Tamaño de texto elegido por el usuario con los botones A- y A+. Pasos: 11, 12 [predeterminado], 14 y 16 px de base.
 * Se guarda en localStorage y se aplica como atributo data-tec-fs en <html> ANTES de pintar (el script va en el <head>),
 * para evitar saltos de tamaño. La raíz cambia en layout-tribunal.css; no hay servidor ni AJAX. */
(function (w, d) {
    'use strict';
    var KEY = 'tec.fontSize', DEFAULT = '12', PASOS = ['11', '12', '14', '16'];

    function valido(v) {
        return PASOS.indexOf(v) !== -1;
    }

    function stored() {
        try {
            var v = w.localStorage.getItem(KEY);
            return valido(v) ? v : DEFAULT;
        } catch (e) {            // almacenamiento bloqueado (modo privado, política): tamaño estándar
            return DEFAULT;
        }
    }

    /* A- se deshabilita en el tamaño mínimo y A+ en el máximo. */
    function mark(v) {
        var i = PASOS.indexOf(v);
        var botones = d.querySelectorAll('.tec-fs-selector [data-fs-step]');
        for (var k = 0; k < botones.length; k++) {
            var paso = parseInt(botones[k].getAttribute('data-fs-step'), 10);
            botones[k].disabled = (paso < 0 && i <= 0) || (paso > 0 && i >= PASOS.length - 1);
        }
    }

    function apply(v) {
        d.documentElement.setAttribute('data-tec-fs', v);
        mark(v);
    }

    w.TecFontSize = {
        get: stored,
        set: function (v) {
            v = valido(v) ? v : DEFAULT;
            try { w.localStorage.setItem(KEY, v); } catch (e) { /* se aplica solo en esta página */ }
            apply(v);
        },
        /** dir: -1 reduce (A-), +1 aumenta (A+) un paso. */
        step: function (dir) {
            var i = PASOS.indexOf(stored()) + (dir < 0 ? -1 : 1);
            i = Math.max(0, Math.min(PASOS.length - 1, i));
            this.set(PASOS[i]);
        }
    };

    apply(stored());                                   // antes del primer pintado
    d.addEventListener('DOMContentLoaded', function () { mark(stored()); });
    w.addEventListener('storage', function (e) {       // sincroniza otras pestañas
        if (e.key === KEY) { apply(stored()); }
    });
})(window, document);
