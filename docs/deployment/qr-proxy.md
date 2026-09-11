# Preparacion del proxy para acceso QR

Estado: configuracion externa pendiente de aplicar; endpoint y autenticacion QR implementados en el WAR.
Configuracion basada en standalone.xml local y el virtual host aportado por el usuario.
No se aplico ningun cambio al servidor.

## Cadena de transporte

Navegador HTTPS -> Nginx :443 -> HTTP 127.0.0.1:8080 -> WildFly.
Nginx termina TLS. El proxy_pass no cambia la URI: el contexto externo es la raiz
y el WAR debe estar publicado en la raiz internamente para esos enlaces.
La aplicacion no necesita conocer la ruta de standalone.xml ni el sistema operativo.

## Nginx

La plantilla tribunal.nginx.conf conserva dominio, certificados, limites y timeouts.
Normaliza Host/protocolo y la IP reenviada; borra Forwarded no confiable.
No usar $proxy_add_x_forwarded_for como prueba de IP real cuando Nginx es el unico proxy.
Si se incorpora Cloudflare u otro balanceador, revisar set_real_ip_from y real_ip_header
con redes confiables especificas antes de usar esta plantilla.

proxy_cookie_flags requiere Nginx 1.19.3 o posterior. Verificar nginx -v y nginx -t
antes de recargar. La politica se aplica a JSESSIONID al pasar por este dominio;
no garantiza las cookies de un acceso directo a WildFly ni sustituye su configuracion.
Conservar integraciones adicionales de Certbot presentes en el archivo real.
Los 100M de Nginx no elevan los limites de carga definidos en JSF/Servlet.

## WildFly

Los siguientes comandos son administrativos y requieren conexion mediante jboss-cli.
Las direcciones fueron comprobadas en el standalone.xml local. Antes de aplicar,
respaldar configuracion y comprobar si otras aplicaciones usan el listener HTTP.
No ejecutar automaticamente desde el WAR.

Consultar primero:

```text
/subsystem=undertow/server=default-server/http-listener=default:read-attribute(name=proxy-address-forwarding)
/socket-binding-group=standard-sockets/socket-binding=http:read-resource(include-runtime=true)
/interface=public:read-resource(include-runtime=true)
```

Si el puerto HTTP ya escucha exclusivamente en 127.0.0.1, mantener esa configuracion.
De lo contrario, este ejemplo crea una interfaz especifica para el listener HTTP:

```text
batch
/interface=tec-proxy-loopback:add(inet-address=127.0.0.1)
/socket-binding-group=standard-sockets/socket-binding=http:write-attribute(name=interface,value=tec-proxy-loopback)
/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=proxy-address-forwarding,value=true)
run-batch
```

El ejemplo supone que tec-proxy-loopback no existe; consultar antes de repetirlo.
No habilitar proxy-address-forwarding con 8080 expuesto a Internet: un cliente podria
falsificar el esquema HTTPS y la IP. Comprobar tambien IPv6, otros listeners y firewall.
Revisar el resultado de la operacion y realizar reload controlado cuando WildFly lo requiera.
No se cambian security-domain, realm, BCrypt ni credenciales con estos comandos.

El mismo modelo de gestion funciona en Windows y Linux. Solo cambia el lanzador:
bin/jboss-cli.bat o bin/jboss-cli.sh dentro de la instalacion de WildFly.
Pruebas desde otro equipo deben entrar por un proxy HTTPS de pruebas o HTTPS directo
correctamente configurado; http://IP:8080 no sera un acceso QR valido.

## Validacion del despliegue

1. nginx -t debe pasar antes de recargar el virtual host.
2. Comprobar desde otro equipo que 8080 no sea accesible; desde el servidor, el proxy debe alcanzarlo.
3. Verificar en una prueba de integracion privada request.isSecure()=true, scheme=https,
   puerto externo 443 y remoteAddr correspondiente al cliente. No publicar endpoints de diagnostico.
4. Una cabecera X-Forwarded-For o X-Forwarded-Proto inventada por el cliente debe quedar sobrescrita.
5. Inspeccionar Set-Cookie de JSESSIONID en HTTPS: Secure, HttpOnly y SameSite=Lax.
6. Verificar login FORM, roles EJB, logout, AJAX, descargas y carga de imagen.
7. El QR contiene `?token=...`. La ubicacion exacta `/acceso-acta` convierte esa consulta
   a fragmento temporal en Nginx antes de enviarla a WildFly, sin registrar esa peticion.
   No habilitar captura de Location, URL completa o cuerpo en APM/balanceadores.
   El servlet implementa tambien la conversion para HTTPS directo, pero en ese caso
   se deben excluir las consultas de los logs Undertow y de cualquier proxy externo.
   El canje usa POST sin logs de cuerpo; comprobar no-store,
   no-referrer, proteccion CSRF y limite de intentos a sus endpoints en la implementacion.

Reversion: restaurar los valores capturados del listener/socket binding y el virtual host
respaldado. No asumir que los valores previos eran los predeterminados.

## Referencias

- https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_cookie_flags
- https://docs.wildfly.org/39/Admin_Guide.html
