(() => {
    'use strict';
    const fragmento = location.hash;
    history.replaceState(null, '', location.pathname);
    const token = document.getElementById('token');
    if (!token || !token.form) return;
    const aviso = document.getElementById('aviso');
    const procesando = document.getElementById('procesando');
    const rechazar = () => {
        token.value = '';
        if (procesando) procesando.hidden = true;
        if (aviso) aviso.hidden = false;
    };
    const coincidencia = /^#t=([A-Za-z0-9_-]{43})$/.exec(fragmento);
    if (!coincidencia) {
        rechazar();
        return;
    }
    token.value = coincidencia[1];
    let enviado = false;
    token.form.addEventListener('submit', (evento) => {
        if (enviado) { evento.preventDefault(); return; }
        enviado = true;
    });
    window.addEventListener('pageshow', (evento) => {
        if (evento.persisted) { enviado = true; rechazar(); }
    });
    // El GET no consume: el navegador realiza un unico POST protegido con CSRF.
    token.form.requestSubmit();
})();
