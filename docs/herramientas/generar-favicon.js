// Genera un favicon.ico multi-tamaño (16, 32 y 48 px) a partir del PNG 256x256 que contiene el
// favicon actual, sin cambiar el diseño. Sin dependencias: solo Node (zlib).
//
// Uso: node docs/herramientas/generar-favicon.js <origen.ico> <destino.ico>
//
// El favicon original (un único PNG de 256x256, ~111 KB) hacía que el navegador cancelara la
// descarga al navegar; Mojarra lo registraba como JSF1064 seguido de «Connection reset by peer».
'use strict';
const fs = require('fs');
const zlib = require('zlib');

const TAMANOS = [16, 32, 48];

function crc32(buf) {
    let c, crc = 0xffffffff;
    for (let n = 0; n < buf.length; n++) {
        c = (crc ^ buf[n]) & 0xff;
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
        crc = (crc >>> 8) ^ c;
    }
    return (crc ^ 0xffffffff) >>> 0;
}

/** PNG RGBA 8 bits sin entrelazado → { ancho, alto, datos (RGBA) }. */
function leerPng(png) {
    let pos = 8, ancho, alto, idat = [];
    while (pos < png.length) {
        const largo = png.readUInt32BE(pos), tipo = png.toString('ascii', pos + 4, pos + 8);
        const datos = png.subarray(pos + 8, pos + 8 + largo);
        if (tipo === 'IHDR') {
            ancho = datos.readUInt32BE(0); alto = datos.readUInt32BE(4);
            if (datos[8] !== 8 || datos[9] !== 6 || datos[12] !== 0) throw new Error('Se esperaba PNG RGBA de 8 bits sin entrelazado');
        } else if (tipo === 'IDAT') idat.push(datos);
        pos += 12 + largo;
    }
    const crudo = zlib.inflateSync(Buffer.concat(idat)), bpp = 4, fila = ancho * bpp;
    const salida = Buffer.alloc(alto * fila);
    for (let y = 0; y < alto; y++) {
        const filtro = crudo[y * (fila + 1)], ini = y * (fila + 1) + 1;
        for (let x = 0; x < fila; x++) {
            const a = x >= bpp ? salida[y * fila + x - bpp] : 0;
            const b = y > 0 ? salida[(y - 1) * fila + x] : 0;
            const c = x >= bpp && y > 0 ? salida[(y - 1) * fila + x - bpp] : 0;
            let v = crudo[ini + x];
            if (filtro === 1) v += a;
            else if (filtro === 2) v += b;
            else if (filtro === 3) v += (a + b) >> 1;
            else if (filtro === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
            salida[y * fila + x] = v & 0xff;
        }
    }
    return { ancho, alto, datos: salida };
}

/** Reducción por promedio de área con alfa premultiplicado (bordes sin halos). */
function reducir(img, destino) {
    const out = Buffer.alloc(destino * destino * 4), escala = img.ancho / destino;
    for (let dy = 0; dy < destino; dy++) for (let dx = 0; dx < destino; dx++) {
        let r = 0, g = 0, b = 0, a = 0, n = 0;
        for (let sy = Math.floor(dy * escala); sy < Math.floor((dy + 1) * escala); sy++)
            for (let sx = Math.floor(dx * escala); sx < Math.floor((dx + 1) * escala); sx++) {
                const i = (sy * img.ancho + sx) * 4, al = img.datos[i + 3];
                r += img.datos[i] * al; g += img.datos[i + 1] * al; b += img.datos[i + 2] * al; a += al; n++;
            }
        const o = (dy * destino + dx) * 4;
        out[o] = a ? Math.round(r / a) : 0; out[o + 1] = a ? Math.round(g / a) : 0;
        out[o + 2] = a ? Math.round(b / a) : 0; out[o + 3] = Math.round(a / n);
    }
    return out;
}

function escribirPng(rgba, lado) {
    const fila = lado * 4, crudo = Buffer.alloc(lado * (fila + 1));
    for (let y = 0; y < lado; y++) rgba.copy(crudo, y * (fila + 1) + 1, y * fila, (y + 1) * fila);
    const trozo = (tipo, datos) => {
        const t = Buffer.from(tipo, 'ascii'), l = Buffer.alloc(4), c = Buffer.alloc(4);
        l.writeUInt32BE(datos.length); c.writeUInt32BE(crc32(Buffer.concat([t, datos])));
        return Buffer.concat([l, t, datos, c]);
    };
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(lado, 0); ihdr.writeUInt32BE(lado, 4); ihdr[8] = 8; ihdr[9] = 6;
    return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), trozo('IHDR', ihdr),
        trozo('IDAT', zlib.deflateSync(crudo, { level: 9 })), trozo('IEND', Buffer.alloc(0))]);
}

const [origen, destino] = process.argv.slice(2);
const ico = fs.readFileSync(origen);
const img = leerPng(ico.subarray(ico.readUInt32LE(18), ico.readUInt32LE(18) + ico.readUInt32LE(14)));
const pngs = TAMANOS.map(t => escribirPng(reducir(img, t), t));
const cab = Buffer.alloc(6 + 16 * pngs.length);
cab.writeUInt16LE(0, 0); cab.writeUInt16LE(1, 2); cab.writeUInt16LE(pngs.length, 4);
let desplazamiento = cab.length;
pngs.forEach((png, i) => {
    const e = 6 + 16 * i;
    cab[e] = TAMANOS[i]; cab[e + 1] = TAMANOS[i]; cab.writeUInt16LE(1, e + 4); cab.writeUInt16LE(32, e + 6);
    cab.writeUInt32LE(png.length, e + 8); cab.writeUInt32LE(desplazamiento, e + 12);
    desplazamiento += png.length;
});
fs.writeFileSync(destino, Buffer.concat([cab, ...pngs]));
console.log(`${destino}: ${TAMANOS.join(', ')} px, ${desplazamiento} bytes (origen ${img.ancho}x${img.alto}, ${ico.length} bytes)`);
