const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/resources/qr/acceso.js', 'utf8');

function ejecutar(hash) {
    const events = {};
    let submit;
    let solicitudes = 0;
    const form = {
        addEventListener: (name, listener) => { assert.equal(name, 'submit'); submit = listener; },
        requestSubmit: () => {
            let cancelado = false;
            submit({ preventDefault: () => { cancelado = true; } });
            if (!cancelado) solicitudes++;
        }
    };
    const elements = { token: { value: '', form }, aviso: { hidden: true }, procesando: { hidden: false } };
    vm.runInNewContext(source, {
        location: { hash, pathname: '/acceso-acta' },
        history: { replaceState: (_, __, url) => assert.equal(url, '/acceso-acta') },
        document: { getElementById: id => elements[id] },
        window: { addEventListener: (name, listener) => { events[name] = listener; } }
    });
    return { elements, form, events, cantidad: () => solicitudes };
}

const valido = ejecutar('#t=' + 'A'.repeat(43));
assert.equal(valido.cantidad(), 1);
assert.equal(valido.elements.token.value, 'A'.repeat(43));
valido.form.requestSubmit();
assert.equal(valido.cantidad(), 1);
valido.events.pageshow({ persisted: true });
assert.equal(valido.elements.token.value, '');
valido.form.requestSubmit();
assert.equal(valido.cantidad(), 1);
for (const hash of ['', '#t=incorrecto', '#t=' + 'A'.repeat(43) + '&otro=1']) {
    const invalido = ejecutar(hash);
    assert.equal(invalido.cantidad(), 0);
    assert.equal(invalido.elements.aviso.hidden, false);
    assert.equal(invalido.elements.procesando.hidden, true);
}
console.log('OK: POST automatico unico, token invalido sin envio y BFCache sin reutilizacion');
