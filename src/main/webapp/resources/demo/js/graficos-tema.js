/* ============================================================
 * graficos-tema.js
 * Colores de los gráficos (p:chart / Chart.js) tomados del tema PrimeFaces activo.
 *
 * Los modelos JSON que arma el servidor no llevan colores: el servidor no conoce el
 * tema del usuario. Esta función se declara como extender de <p:chart>; PrimeFaces la
 * invoca con this = widget justo antes de dibujar, y aquí se completa la configuración
 * con las variables CSS del tema (--primary-*, --text-color*, --surface-border).
 *
 * Barras en --primary-700: contraste ≥ 3:1 sobre la tarjeta con el tema Tribunal
 * (el --primary-color puro no lo alcanza en los temas de primario claro). Un solo color
 * por serie: las barras se distinguen por su etiqueta, no por el color.
 *
 * Uso: <p:chart value="..." extender="tecGraficoTema"/> y
 *      <h:outputScript name="js/graficos-tema.js" library="demo" target="head"/>
 * ============================================================ */
(function (global) {
    'use strict';

    function variable(nombre, respaldo) {
        var valor = global.getComputedStyle(global.document.documentElement).getPropertyValue(nombre);
        return valor && valor.trim() ? valor.trim() : respaldo;
    }

    function colorearEje(eje, colorTexto, colorRejilla) {
        if (!eje) {
            return;
        }
        eje.ticks = eje.ticks || {};
        eje.ticks.color = colorTexto;
        eje.grid = eje.grid || {};
        if (eje.grid.display !== false) {
            eje.grid.color = colorRejilla;
        }
    }

    /* Tamaño en px de un token de la escala TEC (--tec-fs-*), calculado por el navegador. */
    function tamano(token, respaldo) {
        var doc = global.document, e = doc.createElement('span');
        e.style.cssText = 'position:absolute;visibility:hidden;font-size:var(' + token + ')';
        doc.body.appendChild(e);
        var px = parseFloat(global.getComputedStyle(e).fontSize);
        doc.body.removeChild(e);
        return px > 0 ? px : respaldo;
    }

    global.tecGraficoTema = function () {
        var config = this.cfg && this.cfg.config;
        if (!config) {
            return;
        }
        var relleno = variable('--primary-700', variable('--primary-color', '#1c80cf'));
        var borde = variable('--primary-800', relleno);
        var texto = variable('--text-color', '#495057');
        var textoSecundario = variable('--text-color-secondary', '#6c757d');
        var rejilla = variable('--surface-border', '#dee2e6');

        var datos = config.data || {};
        (datos.datasets || []).forEach(function (serie) {
            serie.backgroundColor = relleno;
            serie.borderColor = borde;
            serie.hoverBackgroundColor = borde;
        });

        config.options = config.options || {};
        var opciones = config.options;
        if (config.type === 'bar' || config.type === 'line') {
            opciones.scales = opciones.scales || {};
            opciones.scales.x = opciones.scales.x || {};
            opciones.scales.y = opciones.scales.y || {};
        }
        if (opciones.scales) {
            colorearEje(opciones.scales.x, texto, rejilla);
            colorearEje(opciones.scales.y, textoSecundario, rejilla);
        }
        opciones.plugins = opciones.plugins || {};
        opciones.plugins.legend = opciones.plugins.legend || {};
        opciones.plugins.legend.labels = opciones.plugins.legend.labels || {};
        opciones.plugins.legend.labels.color = texto;

        // Tipografía del tema: Montserrat, peso base 300 (Light) y 500 en títulos de tooltip.
        var familia = variable('--font-family', 'Montserrat, sans-serif');
        var xs = tamano('--tec-fs-2xs', 11);
        opciones.font = {family: familia, size: xs, weight: '300'};
        opciones.plugins.tooltip = opciones.plugins.tooltip || {};
        opciones.plugins.tooltip.titleFont = {family: familia, size: xs, weight: '500'};
        opciones.plugins.tooltip.bodyFont = {family: familia, size: xs, weight: '300'};
    };
}(window));
