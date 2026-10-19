'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../../..');
const script = fs.readFileSync(path.join(root, 'src/main/webapp/resources/demo/js/tec-personas.js'), 'utf8');

function entorno() {
    const atributos = new Map();
    const clases = new Set();
    const span = { textContent: '' }, icono = { className: '' };
    const estado = {
        hidden: true,
        classList: { toggle(c, activo) { activo ? clases.add(c) : clases.delete(c); } },
        setAttribute(k, v) { atributos.set(k, v); },
        querySelector(tag) { return tag === 'span' ? span : icono; }
    };
    const nodos = new Map([
        ['personasEstadoAjax', estado],
        ['personasTextosCliente', { getAttribute(k) { return {
            'data-cargando': 'Comprobando los datos...',
            'data-error': 'No se pudo completar la solicitud.',
            'data-error-guardado': 'Revise el listado antes de volver a guardar.'
        }[k]; } }]
    ]);
    const avisos = [];
    const window = { Tribunal: { toast: { error(m) { avisos.push(m); } } } };
    const contexto = { window, document: { getElementById(id) { return nodos.get(id); } }, Set };
    vm.runInNewContext(script, contexto);
    function campo(id) {
        const attrs = new Map();
        nodos.set(id, { setAttribute(k, v) { attrs.set(k, v); } });
        return attrs;
    }
    return { api: window.TecPersonas, estado, span, icono, atributos, clases, campo, avisos };
}

test('consulta indica carga sin bloquear; finaliza sobre el input reemplazado por JSF', () => {
    const e = entorno();
    e.campo('cedula');
    e.api.iniciar('cedula');
    assert.equal(e.estado.hidden, false);
    assert.match(e.span.textContent, /Comprobando/);
    const inputRecreado = e.campo('cedula');
    e.api.finalizar('cedula');
    assert.equal(inputRecreado.get('aria-busy'), 'false');
    assert.equal(e.estado.hidden, true);
});

test('oncomplete posterior a onerror no borra el aviso de fallo', () => {
    const e = entorno();
    const campo = e.campo('nombres');
    e.api.iniciar('nombres');
    e.api.fallar('nombres');
    e.api.finalizar('nombres');
    assert.equal(campo.get('aria-busy'), 'false');
    assert.equal(e.estado.hidden, false);
    assert.equal(e.atributos.get('role'), 'alert');
    assert.match(e.span.textContent, /No se pudo/);
});

test('una respuesta no oculta el estado de otra comprobacion pendiente', () => {
    const e = entorno();
    e.api.iniciar('cedula');
    e.api.iniciar('nombres');
    e.api.finalizar('cedula');
    assert.equal(e.estado.hidden, false);
    e.api.finalizar('nombres');
    assert.equal(e.estado.hidden, true);
});

test('cancelar limpia el estado; reabrir permite una nueva comprobacion', () => {
    const e = entorno();
    const campo = e.campo('cedula');
    e.api.iniciar('cedula');
    e.api.fallar('cedula');
    e.api.reiniciar();
    assert.equal(campo.get('aria-busy'), 'false');
    assert.equal(e.estado.hidden, true);
    e.api.iniciar('cedula');
    assert.equal(e.atributos.get('role'), 'status');
    e.api.finalizar('cedula');
    assert.equal(e.estado.hidden, true);
});

test('fallo de guardado solicita verificar resultado; fallo de apertura informa', () => {
    const e = entorno();
    e.api.errorGuardado();
    assert.equal(e.estado.hidden, false);
    assert.match(e.span.textContent, /Revise el listado/);
    e.api.errorApertura();
    assert.equal(e.avisos.length, 1);
});

test('Guardar cierra solo con confirmacion explicita, nunca por respuesta vacia o validacion fallida', () => {
    const vista = fs.readFileSync(path.join(root, 'src/main/webapp/personas.xhtml'), 'utf8');
    const boton = vista.match(/<p:commandButton id="btnGuardarPersona"[\s\S]*?\/>/)[0];
    const callback = boton.match(/oncomplete="([^"]+)"/)[1].replaceAll('&amp;', '&');
    let cierres = 0;
    const PF = () => ({ hide() { cierres++; } });
    for (const args of [undefined, {}, { validationFailed: true }, { personaGuardada: false }]) {
        vm.runInNewContext(callback, { args, PF });
    }
    assert.equal(cierres, 0);
    vm.runInNewContext(callback, { args: { personaGuardada: true }, PF });
    assert.equal(cierres, 1);
});

test('Nuevo y Editar no abren el dialogo cuando la respuesta falla o no esta listo', () => {
    const vista = fs.readFileSync(path.join(root, 'src/main/webapp/personas.xhtml'), 'utf8');
    const botones = [...vista.matchAll(/<p:commandButton\b[\s\S]*?>/g)].map(m => m[0]);
    for (const accion of ['nuevaPersona', 'prepararEdicion']) {
        const boton = botones.find(b => b.includes('personaController.' + accion));
        const callback = boton.match(/oncomplete="([^"]+)"/)[1].replaceAll('&amp;', '&');
        let aperturas = 0;
        const Tribunal = { mostrarDialogo() { aperturas++; } };
        for (const args of [undefined, {}, { dialogReady: false }]) {
            vm.runInNewContext(callback, { args, Tribunal });
        }
        assert.equal(aperturas, 0);
        vm.runInNewContext(callback, { args: { dialogReady: true }, Tribunal });
        assert.equal(aperturas, 1);
    }
});
