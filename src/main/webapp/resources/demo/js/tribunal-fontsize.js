/* Tamaño de texto elegido por el usuario (11, 12, 13 [predeterminado] o 15 px de base).
 * Se guarda en localStorage y se aplica como atributo data-tec-fs en <html> ANTES de pintar (el script va en el <head>),
 * para evitar saltos de tamaño. La raíz cambia en tribunal-globals.css / layout-tribunal.css; no hay servidor ni AJAX. */
(function (w, d) {
    'use strict';
    var KEY = 'tec.fontSize', DEFAULT = '13', VALID = {'11': 1, '12': 1, '13': 1, '15': 1};

    function stored() {
        try {
            var v = w.localStorage.getItem(KEY);
            return VALID[v] ? v : DEFAULT;
        } catch (e) {            // almacenamiento bloqueado (modo privado, política): tamaño estándar
            return DEFAULT;
        }
    }

    function mark(v) {
        var botones = d.querySelectorAll('.tec-fs-selector [data-fs]');
        for (var i = 0; i < botones.length; i++) {
            botones[i].setAttribute('aria-pressed', botones[i].getAttribute('data-fs') === v ? 'true' : 'false');
        }
    }

    function apply(v) {
        d.documentElement.setAttribute('data-tec-fs', v);
        mark(v);
    }

    w.TecFontSize = {
        get: stored,
        set: function (v) {
            v = VALID[v] ? v : DEFAULT;
            try { w.localStorage.setItem(KEY, v); } catch (e) { /* se aplica solo en esta página */ }
            apply(v);
        }
    };

    apply(stored());                                   // antes del primer pintado
    d.addEventListener('DOMContentLoaded', function () { mark(stored()); });
    w.addEventListener('storage', function (e) {       // sincroniza otras pestañas
        if (e.key === KEY) { apply(stored()); }
    });
})(window, document);
