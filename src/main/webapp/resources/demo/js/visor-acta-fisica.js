/*
 * Visor del acta física de escrutinio: zoom, rotación y restablecimiento.
 *
 * Compartido por reportesMesa.xhtml y actaE.xhtml, que antes lo tendrían duplicado en
 * línea. La imagen se localiza dentro del lienzo (#lienzoActaFisica) en lugar de por su
 * id de cliente JSF, que cambia según el formulario que la contiene; así el mismo código
 * sirve en cualquier pantalla que use el marcado del visor:
 *   #visorActaFisica > #lienzoActaFisica > img   y el control #controlZoomActaFisica.
 */
var escalaActaFisica = 1;
var rotacionActaFisica = 0;

function imagenVisorActaFisica() {
    var lienzo = document.getElementById('lienzoActaFisica');
    return lienzo ? lienzo.querySelector('img') : null;
}

function ajustarImagenActa(escala, rotacion) {
    escalaActaFisica = Math.max(0.5, Math.min(3, escalaActaFisica + escala));
    rotacionActaFisica = (rotacionActaFisica + rotacion) % 360;
    aplicarVistaActaFisica();
}

function establecerZoom(porcentaje) {
    escalaActaFisica = Math.max(0.5, Math.min(3, Number(porcentaje) / 100));
    aplicarVistaActaFisica();
}

function aplicarVistaActaFisica() {
    var control = document.getElementById('controlZoomActaFisica');
    var imagen = imagenVisorActaFisica();
    var lienzo = document.getElementById('lienzoActaFisica');
    if (!imagen || !lienzo) return;
    if (!imagen.dataset.anchoBase) {
        imagen.dataset.anchoBase = imagen.clientWidth;
        imagen.dataset.altoBase = imagen.clientHeight;
    }
    var ancho = Number(imagen.dataset.anchoBase) * escalaActaFisica;
    var alto = Number(imagen.dataset.altoBase) * escalaActaFisica;
    var rotacionNormalizada = ((rotacionActaFisica % 360) + 360) % 360;
    var intercambiaDimensiones = rotacionNormalizada === 90 || rotacionNormalizada === 270;
    imagen.style.maxWidth = 'none';
    imagen.style.maxHeight = 'none';
    imagen.style.width = ancho + 'px';
    imagen.style.height = alto + 'px';
    imagen.style.position = 'absolute';
    imagen.style.left = '50%';
    imagen.style.top = '50%';
    imagen.style.transform = 'translate(-50%, -50%) rotate(' + rotacionNormalizada + 'deg)';
    lienzo.style.position = 'relative';
    lienzo.style.display = 'block';
    lienzo.style.minWidth = (intercambiaDimensiones ? alto : ancho) + 'px';
    lienzo.style.minHeight = (intercambiaDimensiones ? ancho : alto) + 'px';
    lienzo.style.width = (intercambiaDimensiones ? alto : ancho) + 'px';
    lienzo.style.height = (intercambiaDimensiones ? ancho : alto) + 'px';
    if (control) control.value = Math.round(escalaActaFisica * 100);
}

function restablecerImagenActa() {
    escalaActaFisica = 1;
    rotacionActaFisica = 0;
    var imagen = imagenVisorActaFisica();
    var control = document.getElementById('controlZoomActaFisica');
    if (!imagen) return;
    imagen.style.width = '';
    imagen.style.height = '';
    imagen.style.maxWidth = '100%';
    imagen.style.maxHeight = '58vh';
    imagen.style.position = '';
    imagen.style.left = '';
    imagen.style.top = '';
    imagen.style.transform = '';
    var lienzo = document.getElementById('lienzoActaFisica');
    if (lienzo) {
        lienzo.style.position = '';
        lienzo.style.display = '';
        lienzo.style.minWidth = '100%';
        lienzo.style.minHeight = '100%';
        lienzo.style.width = '';
        lienzo.style.height = '';
    }
    delete imagen.dataset.anchoBase;
    delete imagen.dataset.altoBase;
    if (control) control.value = 100;
}
