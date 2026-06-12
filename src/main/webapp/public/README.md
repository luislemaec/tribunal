# Portal publico de resultados

Esta carpeta contiene los archivos necesarios para publicar la pagina publica de resultados sin depender de JSF ni de recursos fuera de `public`.

## Archivos

- `resultados.html`: pagina publica.
- `resultados.json`: endpoint relativo esperado. En WildFly lo atiende el servlet `/public/resultados.json`.
- `assets/css/resultados.css`: estilos propios.
- `assets/js/resultados.js`: logica de la pagina.
- `assets/js/chart.js`: libreria Chart.js local.
- `assets/img/logo.png`: logo local.
- `assets/img/favicon.ico`: icono local.

## Migracion a Nginx

Copiar toda la carpeta `public` al sitio estatico de Nginx y proxyar solo:

```nginx
location = /resultados.json {
    proxy_pass http://127.0.0.1:8080/tec/public/resultados.json;
    proxy_set_header Host $host;
    proxy_cache_valid 200 15s;
}
```

La pagina usa `fetch("resultados.json")`, por lo que funcionara igual si `resultados.html` y `resultados.json` estan bajo el mismo subdominio.
