// Extrae de la plantilla raster docs/diseno/A4TEC.png (2480x3508 px, 300 ppp) los únicos elementos que deben seguir
// siendo imagen: el logotipo TEC del encabezado y el de la marca de agua (letras con degradado y check, sin
// versión vectorial). Bandas, líneas y textos se dibujan como vectores y texto real en PlantillaA4.
// Los recortes conservan los píxeles originales (sin reescalar ni recomprimir con pérdida).
//
// Uso: node docs/herramientas/recortar-plantilla-a4.js docs/diseno/A4TEC.png src/main/resources/img
'use strict';
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// Recortes en píxeles de A4TEC.png (incluyen 2 px de margen alrededor del contenido medido).
const RECORTES = {
    'plantilla-a4-logo.png': { x: 462, y: 169, ancho: 586, alto: 194 },
    'plantilla-a4-marca-agua.png': { x: 538, y: 1378, ancho: 1580, alto: 516 },
};

function decodificar(png) {
    let pos = 8, w, h, ct, il; const idat = [];
    while (pos < png.length) {
        const len = png.readUInt32BE(pos), t = png.toString('ascii', pos + 4, pos + 8), d = png.subarray(pos + 8, pos + 8 + len);
        if (t === 'IHDR') { w = d.readUInt32BE(0); h = d.readUInt32BE(4); ct = d[9]; il = d[12]; if (d[8] !== 8) throw new Error('Se esperaban 8 bits'); }
        else if (t === 'IDAT') idat.push(d);
        pos += 12 + len;
    }
    const bpp = ct === 6 ? 4 : 3, raw = zlib.inflateSync(Buffer.concat(idat)), out = Buffer.alloc(w * h * 4);
    const pasadas = il ? [[0, 0, 8, 8], [4, 0, 8, 8], [0, 4, 4, 8], [2, 0, 4, 4], [0, 2, 2, 4], [1, 0, 2, 2], [0, 1, 1, 2]] : [[0, 0, 1, 1]];
    let p = 0;
    for (const [x0, y0, dx, dy] of pasadas) {
        const pw = Math.ceil((w - x0) / dx), ph = Math.ceil((h - y0) / dy);
        if (pw <= 0 || ph <= 0) continue;
        const fila = pw * bpp; let previa = Buffer.alloc(fila);
        for (let y = 0; y < ph; y++) {
            const f = raw[p++], cur = Buffer.from(raw.subarray(p, p + fila)); p += fila;
            for (let x = 0; x < fila; x++) {
                const a = x >= bpp ? cur[x - bpp] : 0, b = previa[x], c = x >= bpp ? previa[x - bpp] : 0;
                let v = cur[x];
                if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1;
                else if (f === 4) { const q = a + b - c, pa = Math.abs(q - a), pb = Math.abs(q - b), pc = Math.abs(q - c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
                cur[x] = v & 255;
            }
            for (let x = 0; x < pw; x++) {
                const o = ((y0 + y * dy) * w + (x0 + x * dx)) * 4;
                out[o] = cur[x * bpp]; out[o + 1] = cur[x * bpp + 1]; out[o + 2] = cur[x * bpp + 2]; out[o + 3] = bpp === 4 ? cur[x * bpp + 3] : 255;
            }
            previa = cur;
        }
    }
    return { w, h, px: out };
}

function crc32(buf) {
    let crc = 0xffffffff;
    for (let n = 0; n < buf.length; n++) {
        let c = (crc ^ buf[n]) & 0xff;
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
        crc = (crc >>> 8) ^ c;
    }
    return (crc ^ 0xffffffff) >>> 0;
}

function codificar(rgba, w, h) {
    const fila = w * 4, crudo = Buffer.alloc(h * (fila + 1));
    for (let y = 0; y < h; y++) {
        // Filtro Up: mejora la compresión de degradados verticales.
        crudo[y * (fila + 1)] = 2;
        for (let x = 0; x < fila; x++) {
            const v = rgba[y * fila + x], arriba = y > 0 ? rgba[(y - 1) * fila + x] : 0;
            crudo[y * (fila + 1) + 1 + x] = (v - arriba) & 255;
        }
    }
    const trozo = (t, d) => { const tb = Buffer.from(t, 'ascii'), l = Buffer.alloc(4), c = Buffer.alloc(4); l.writeUInt32BE(d.length); c.writeUInt32BE(crc32(Buffer.concat([tb, d]))); return Buffer.concat([l, tb, d, c]); };
    const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
    // pHYs a 300 ppp (11811 px/m), como la plantilla original.
    const phys = Buffer.alloc(9); phys.writeUInt32BE(11811, 0); phys.writeUInt32BE(11811, 4); phys[8] = 1;
    return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), trozo('IHDR', ihdr), trozo('pHYs', phys),
        trozo('IDAT', zlib.deflateSync(crudo, { level: 9 })), trozo('IEND', Buffer.alloc(0))]);
}

const [origen, destino] = process.argv.slice(2);
const img = decodificar(fs.readFileSync(origen));
for (const [nombre, r] of Object.entries(RECORTES)) {
    const px = Buffer.alloc(r.ancho * r.alto * 4);
    for (let y = 0; y < r.alto; y++) img.px.copy(px, y * r.ancho * 4, ((r.y + y) * img.w + r.x) * 4, ((r.y + y) * img.w + r.x + r.ancho) * 4);
    const png = codificar(px, r.ancho, r.alto);
    fs.writeFileSync(path.join(destino, nombre), png);
    console.log(`${nombre}: ${r.ancho}x${r.alto} px, ${(png.length / 1024).toFixed(1)} KB`);
}
