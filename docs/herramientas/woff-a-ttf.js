// Convierte una fuente WOFF 1.0 (tablas comprimidas con zlib) a TrueType, sin dependencias.
// Se usa para obtener Montserrat Bold (700) en TTF a partir de la WOFF que ya trae el proyecto,
// porque iText 5 solo incrusta TTF/OTF. Uso: node docs/herramientas/woff-a-ttf.js <entrada.woff> <salida.ttf>
'use strict';
const fs = require('fs');
const zlib = require('zlib');

const [entrada, salida] = process.argv.slice(2);
const woff = fs.readFileSync(entrada);
if (woff.toString('ascii', 0, 4) !== 'wOFF') throw new Error('No es un archivo WOFF 1.0');
const flavor = woff.readUInt32BE(4), numTablas = woff.readUInt16BE(12);
const tablas = [];
for (let i = 0; i < numTablas; i++) {
    const e = 44 + i * 20;
    const etiqueta = woff.toString('ascii', e, e + 4), offset = woff.readUInt32BE(e + 4);
    const largoComp = woff.readUInt32BE(e + 8), largo = woff.readUInt32BE(e + 12), suma = woff.readUInt32BE(e + 16);
    const datos = woff.subarray(offset, offset + largoComp);
    tablas.push({ etiqueta, suma, datos: largoComp < largo ? zlib.inflateSync(datos) : Buffer.from(datos) });
}
tablas.sort((a, b) => (a.etiqueta < b.etiqueta ? -1 : 1));
let potencia = 1, exp = 0;
while (potencia * 2 <= numTablas) { potencia *= 2; exp++; }
const cabecera = Buffer.alloc(12 + 16 * numTablas);
cabecera.writeUInt32BE(flavor, 0); cabecera.writeUInt16BE(numTablas, 4);
cabecera.writeUInt16BE(potencia * 16, 6); cabecera.writeUInt16BE(exp, 8); cabecera.writeUInt16BE(numTablas * 16 - potencia * 16, 10);
let posicion = cabecera.length;
const cuerpos = [];
tablas.forEach((t, i) => {
    const e = 12 + i * 16;
    cabecera.write(t.etiqueta, e, 'ascii'); cabecera.writeUInt32BE(t.suma, e + 4);
    cabecera.writeUInt32BE(posicion, e + 8); cabecera.writeUInt32BE(t.datos.length, e + 12);
    const relleno = (4 - (t.datos.length % 4)) % 4;
    cuerpos.push(t.datos, Buffer.alloc(relleno));
    posicion += t.datos.length + relleno;
});
fs.writeFileSync(salida, Buffer.concat([cabecera, ...cuerpos]));
console.log(`${salida}: ${numTablas} tablas, ${posicion} bytes`);
