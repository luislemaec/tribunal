/*
 * Consulta pública de lugar de votación: resuelve el desafío anti-robot (protocolo ALTCHA,
 * implementación propia, sin librerías externas). Busca el número n tal que
 * SHA-256(salt + n) = challenge y lo entrega en campos ocultos; el servidor verifica la firma,
 * la prueba, el vencimiento y que no se reutilice (DesafioAntiRobot). Para una persona tarda
 * unos instantes y se hace en segundo plano mientras escribe la cédula.
 */
(function () {
    'use strict';

    var LOTE = 400;
    var listoParaEnviar = false;
    var ejecucion = 0;

    function campo(id) {
        return document.getElementById('frmLugar:' + id);
    }

    function estado(texto, error) {
        var p = document.getElementById('desafioEstado');
        if (p) {
            p.textContent = texto || '';
            p.classList.toggle('text-red-600', !!error);
        }
    }

    function hex(buffer) {
        var bytes = new Uint8Array(buffer), salida = '';
        for (var i = 0; i < bytes.length; i++) {
            salida += (bytes[i] < 16 ? '0' : '') + bytes[i].toString(16);
        }
        return salida;
    }

    async function resolver() {
        var datos = document.getElementById('desafioDatos');
        if (!datos) {
            return;
        }
        var propia = ++ejecucion;
        listoParaEnviar = false;
        if (!window.crypto || !window.crypto.subtle || !window.TextEncoder) {
            estado(datos.dataset.error, true);
            return;
        }
        estado(datos.dataset.verificando, false);
        var challenge = datos.dataset.challenge, salt = datos.dataset.salt;
        var maximo = parseInt(datos.dataset.maximo, 10) || 0;
        var codificador = new TextEncoder();
        // Lotes en paralelo: rápido incluso en celulares y sin bloquear la página.
        for (var inicio = 0; inicio <= maximo; inicio += LOTE) {
            if (propia !== ejecucion) {
                return; // Llegó un desafío nuevo (otra consulta); este quedó obsoleto.
            }
            var promesas = [];
            for (var n = inicio; n < inicio + LOTE && n <= maximo; n++) {
                promesas.push(window.crypto.subtle.digest('SHA-256', codificador.encode(salt + n)));
            }
            var resultados = await Promise.all(promesas);
            for (var i = 0; i < resultados.length; i++) {
                if (hex(resultados[i]) === challenge) {
                    campo('challenge').value = challenge;
                    campo('salt').value = salt;
                    campo('firma').value = datos.dataset.firma;
                    campo('numero').value = String(inicio + i);
                    listoParaEnviar = true;
                    estado(datos.dataset.listo, false);
                    return;
                }
            }
        }
        estado(datos.dataset.error, true);
    }

    /** onclick del botón: solo se envía con el desafío resuelto. */
    function listo() {
        if (!listoParaEnviar) {
            var datos = document.getElementById('desafioDatos');
            estado(datos ? datos.dataset.verificando : '', false);
            return false;
        }
        listoParaEnviar = false; // Cada desafío se usa una sola vez.
        return true;
    }

    window.TecConsultaLugar = { resolver: resolver, listo: listo };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', resolver);
    } else {
        resolver();
    }
})();
