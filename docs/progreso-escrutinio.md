# Etapa actual del escrutinio

`ProgresoEscrutinioDTO.determinar` es la proyeccion comun de la tabla Mesas,
su filtro y el indicador `p:steps` de Escrutinio. No consulta BD, no agrega
estados persistidos y no autoriza operaciones.

| Dato vigente | Etapa actual |
| --- | --- |
| Sin cabecera o PENDIENTE | 1 Apertura |
| ABIERTO | 2 Conteo, pendiente |
| EN_CONTEO sin acta valida para cierre | 2 Conteo, en curso |
| CONTEO_REGISTRADO sin acta | 3 Acta fisica, pendiente de carga |
| CONTEO_REGISTRADO con acta OBSERVADA/RECHAZADA | 3 Acta fisica, pendiente de correccion (incidencia) |
| Conteo guardado con acta PENDIENTE_REVISION | 4 Cierre (incidencia REABIERTO si aplica) |
| REABIERTO sin acta valida para cierre | 2 Conteo, en curso, incidencia REABIERTO |
| CERRADO sin acta fisica (flujo anterior) | 3 Acta fisica, pendiente de carga |
| CERRADO con acta OBSERVADA/RECHAZADA | 3 Acta fisica, incidencia visible |
| CERRADO con acta pendiente de revision | 5 Dato oficial, pendiente de validacion |
| CERRADO con acta VALIDADA | 5 Dato oficial, cinco pasos completados |

Orden oficial: Apertura, Conteo, Acta fisica, Cierre, Validacion. "Conteo
guardado" es EN_CONTEO, CONTEO_REGISTRADO o REABIERTO. Las reglas de carga y
cierre estan en `ReglasFlujoEscrutinio` y `EscrutinioService` rechaza el
cierre sin acta fisica vigente PENDIENTE_REVISION. La API movil publica esta
misma proyeccion en el campo `fase` (ver `api-movil.md`).

OBSERVADO y ANULADO se muestran bloqueados. Solo se reconoce la apertura
historica si su fecha existe; no se infieren cierre ni conteo completo.
El indicador no permite navegacion ni ejecuta comandos. Los botones existentes
conservan sus guardas; servicios, roles, transacciones y cronograma no cambian.

El filtro es exacto, no acumulativo: Acta fisica no incluye mesas ya oficiales.
Dato oficial incluye tanto pendientes de validacion como validadas; el detalle
lo distingue. No se deduce oficialidad de `mesa.estadoTarea`, votos ni PDF.
La evidencia corresponde al documento vigente por mesa y proceso, obtenido
por los servicios existentes. La carga y revision actualizan indicador y fila.

## Verificacion

Prueba sin contenedor ni dependencias adicionales:

```powershell
javac -encoding UTF-8 -d target/progreso-check src/main/java/ec/com/antenasur/enums/EstadoEscrutinio.java src/main/java/ec/com/antenasur/dto/ProgresoEscrutinioDTO.java src/main/java/ec/com/antenasur/service/tec/ReglasFlujoEscrutinio.java src/test/java/ec/com/antenasur/dto/ProgresoEscrutinioCheck.java
java -cp target/progreso-check ec.com.antenasur.dto.ProgresoEscrutinioCheck
mvn -DskipTests compile
```

La prueba cubre 22 escenarios y los cinco hitos/filtros. La aceptacion integrada
requiere WildFly: abrir, guardar cuadre, cargar acta, cerrar, observar/rechazar,
validar, revertir y reabrir; comprobar filtro, paginacion, roles y vista movil.
