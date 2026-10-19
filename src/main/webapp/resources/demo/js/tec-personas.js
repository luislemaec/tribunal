/* Estados locales del formulario Persona. No modifica valores, foco ni peticiones AJAX. */
(function (global) {
    'use strict';
    var pendientes = new Set();
    var error = false;

    function textos(clave) {
        var nodo = document.getElementById('personasTextosCliente');
        return nodo ? nodo.getAttribute('data-' + clave) : '';
    }

    function mostrar(mensaje, esError) {
        var estado = document.getElementById('personasEstadoAjax');
        if (!estado) { return; }
        estado.hidden = !mensaje;
        estado.classList.toggle('personas-estado-ajax--error', esError);
        estado.setAttribute('role', esError ? 'alert' : 'status');
        estado.querySelector('span').textContent = mensaje || '';
        estado.querySelector('i').className = esError
            ? 'pi pi-exclamation-circle' : 'pi pi-spin pi-spinner';
    }

    function ocupado(id, valor) {
        var campo = document.getElementById(id);
        if (campo) { campo.setAttribute('aria-busy', String(valor)); }
    }

    global.TecPersonas = {
        iniciar: function (id) {
            pendientes.add(id);
            error = false;
            ocupado(id, true);
            mostrar(textos('cargando'), false);
        },
        finalizar: function (id) {
            pendientes.delete(id);
            ocupado(id, false);
            if (!pendientes.size && !error) { mostrar('', false); }
        },
        fallar: function (id) {
            pendientes.delete(id);
            ocupado(id, false);
            error = true;
            mostrar(textos('error'), true);
        },
        errorGuardado: function () {
            error = true;
            mostrar(textos('error-guardado'), true);
        },
        errorApertura: function () {
            global.Tribunal.toast.error(textos('error'));
        },
        reiniciar: function () {
            pendientes.forEach(function (id) { ocupado(id, false); });
            pendientes.clear();
            error = false;
            mostrar('', false);
        }
    };
}(window));
