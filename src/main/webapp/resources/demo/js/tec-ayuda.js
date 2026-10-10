/* ============================================================
 * tec-ayuda.js
 * Experiencia de chat del asistente de ayuda (WEB-INF/ayudaWidget.xhtml).
 *
 * El servidor decide el contenido (AyudaController); este script solo mejora la
 * percepción mientras llega la respuesta:
 *  - muestra al instante el mensaje del usuario y un indicador «escribiendo…»;
 *  - al recibir la respuesta, desplaza la conversación al final;
 *  - revela el texto nuevo del asistente de forma progresiva (una sola vez), salvo que
 *    el usuario tenga activada la preferencia «reducir movimiento».
 * Todo el texto se inserta con textContent: nunca se interpreta como HTML.
 * ============================================================ */
(function (global) {
    'use strict';

    var doc = global.document;

    function lista() {
        return doc.querySelector('.tec-chat__mensajes');
    }

    function alFinal() {
        var contenedor = lista();
        if (contenedor) {
            contenedor.scrollTop = contenedor.scrollHeight;
        }
    }

    function reducirMovimiento() {
        return global.matchMedia && global.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    function burbujaUsuario(texto) {
        var fila = doc.createElement('div');
        fila.className = 'tec-chat-msg tec-chat-msg--usuario tec-chat-msg--provisional';
        var burbuja = doc.createElement('div');
        burbuja.className = 'tec-chat-burbuja';
        var contenido = doc.createElement('span');
        contenido.className = 'tec-chat-texto';
        contenido.textContent = texto;
        burbuja.appendChild(contenido);
        fila.appendChild(burbuja);
        return fila;
    }

    function indicadorEscribiendo() {
        var fila = doc.createElement('div');
        fila.className = 'tec-chat-msg tec-chat-msg--asistente tec-chat-msg--provisional';
        fila.setAttribute('aria-label', 'El asistente está escribiendo');
        var burbuja = doc.createElement('div');
        burbuja.className = 'tec-chat-burbuja tec-chat-escribiendo';
        for (var i = 0; i < 3; i++) {
            burbuja.appendChild(doc.createElement('span'));
        }
        fila.appendChild(burbuja);
        return fila;
    }

    function mostrarPendiente(textoUsuario) {
        // Dentro de la lista (role="log", flex en columna) para que cada burbuja se alinee.
        var contenedor = doc.querySelector('.tec-chat__mensajes [role="log"]');
        if (!contenedor) {
            return;
        }
        if (textoUsuario) {
            contenedor.appendChild(burbujaUsuario(textoUsuario));
        }
        contenedor.appendChild(indicadorEscribiendo());
        alFinal();
    }

    /** Revela el texto palabra a palabra en ~1 s como máximo. */
    function revelar(elemento) {
        var completo = elemento.textContent;
        if (!completo || reducirMovimiento()) {
            return;
        }
        var palabras = completo.split(/(\s+)/);
        var porPaso = Math.max(1, Math.ceil(palabras.length / 60));
        var mostradas = 0;
        elemento.textContent = '';
        elemento.classList.add('tec-chat-texto--revelando');
        var intervalo = global.setInterval(function () {
            mostradas = Math.min(palabras.length, mostradas + porPaso);
            elemento.textContent = palabras.slice(0, mostradas).join('');
            alFinal();
            if (mostradas >= palabras.length) {
                global.clearInterval(intervalo);
                elemento.classList.remove('tec-chat-texto--revelando');
            }
        }, 16);
    }

    global.TecAyuda = {
        /** onstart del botón Enviar: no envía si está vacío. */
        enviando: function () {
            var entrada = doc.querySelector('.tec-chat__entrada input');
            var texto = entrada ? entrada.value.trim() : '';
            if (!texto) {
                return false;
            }
            mostrarPendiente(texto);
            return true;
        },

        /** onstart de una pregunta sugerida: la muestra como mensaje del usuario. */
        eligiendo: function (boton) {
            var etiqueta = boton && boton.querySelector('.ui-button-text');
            mostrarPendiente(etiqueta ? etiqueta.textContent.trim() : '');
        },

        /** onstart de acciones del encabezado (contacto, nueva conversación). */
        esperando: function () {
            mostrarPendiente(null);
        },

        /** oncomplete: final de la conversación, revelado de lo nuevo y foco en la entrada. */
        alActualizar: function () {
            var nuevos = doc.querySelectorAll('.tec-chat-msg[data-nuevo="true"] .tec-chat-texto');
            Array.prototype.forEach.call(nuevos, revelar);
            alFinal();
            var entrada = doc.querySelector('.tec-chat__entrada input');
            if (entrada && global.matchMedia && !global.matchMedia('(pointer: coarse)').matches) {
                entrada.focus();
            }
        }
    };
}(window));
