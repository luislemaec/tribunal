const fs = require('fs'), zlib = require('zlib'), subsetFont = require('subset-font');
const [, , woffRef, ttf, salida] = process.argv;
// Caracteres del archivo de referencia (woff1): tabla cmap formato 4 y 12
function codepoints(f) {
  const b = fs.readFileSync(f); const n = b.readUInt16BE(12); let cmap;
  for (let i = 0; i < n; i++) { const o = 44 + i * 20; if (b.toString('ascii', o, o + 4) !== 'cmap') continue;
    const off = b.readUInt32BE(o + 4), comp = b.readUInt32BE(o + 8), orig = b.readUInt32BE(o + 12);
    cmap = b.slice(off, off + comp); if (comp < orig) cmap = zlib.inflateSync(cmap); }
  const cps = new Set(); const nt = cmap.readUInt16BE(2);
  for (let i = 0; i < nt; i++) { const so = cmap.readUInt32BE(4 + i * 8 + 4); const fmt = cmap.readUInt16BE(so);
    if (fmt === 4) { const s = cmap.readUInt16BE(so + 6) / 2, e = so + 14, st = e + s * 2 + 2;
      for (let k = 0; k < s; k++) { const a = cmap.readUInt16BE(st + k * 2), z = cmap.readUInt16BE(e + k * 2); for (let c = a; c <= z && c < 0xFFFF; c++) cps.add(c); } }
    if (fmt === 12) { const ng = cmap.readUInt32BE(so + 12); for (let k = 0; k < ng; k++) { const a = cmap.readUInt32BE(so + 16 + k * 12), z = cmap.readUInt32BE(so + 20 + k * 12); for (let c = a; c <= z; c++) cps.add(c); } } }
  return [...cps];
}
(async () => {
  const cps = codepoints(woffRef); const texto = String.fromCodePoint(...cps);
  const fuente = fs.readFileSync(ttf);
  for (const fmt of ['woff2', 'woff']) {
    const out = await subsetFont(fuente, texto, { targetFormat: fmt });
    fs.writeFileSync(`${salida}.${fmt}`, out);
    console.log(`${salida}.${fmt}: ${out.length} bytes (${cps.length} caracteres)`);
  }
})();
