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

## Despliegue recomendado en subdominios

La pagina resuelve los resultados en este orden:

1. Meta tag `resultados-api-url` o `window.RESULTADOS_API_URL`, si existe.
2. `resultados.json` relativo al mismo origen.

Para `https://resultados.conpociiech.org`, la recomendacion es publicar los archivos estaticos de `public` y exponer `https://resultados.conpociiech.org/resultados.json` con proxy hacia WildFly.

## Nginx recomendado

```nginx
location = /resultados.json {
    proxy_pass http://127.0.0.1:8080/public/resultados.json;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_cache_valid 200 15s;
}
```

Con este esquema el navegador siempre consulta `resultados.json` en el mismo subdominio y no depende de CORS.

