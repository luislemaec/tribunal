# Etapa actual del escrutinio

`ProgresoEscrutinioDTO.determinar` es la proyeccion comun de la tabla Mesas,
su filtro y el indicador `p:steps` de Escrutinio. No consulta BD, no agrega
estados persistidos y no autoriza operaciones.

| Dato vigente | Etapa actual |
| --- | --- |
| Sin cabecera o PENDIENTE | 1 Apertura |
| ABIERTO o EN_CONTEO | 2 Conteo |
| CONTEO_REGISTRADO | 3 Cierre |
| CERRADO sin acta fisica | 4 Acta fisica |
| CERRADO con acta OBSERVADA/RECHAZADA | 4 Acta fisica, incidencia visible |
| CERRADO con acta pendiente de revision | 5 Dato oficial, pendiente de validacion |
| CERRADO con acta VALIDADA | 5 Dato oficial, cinco pasos completados |
| REABIERTO | 2 Conteo, sin reutilizar cierre/validacion historicos |

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
javac -encoding UTF-8 -d target/progreso-check src/main/java/ec/com/antenasur/enums/EstadoEscrutinio.java src/main/java/ec/com/antenasur/dto/ProgresoEscrutinioDTO.java src/test/java/ec/com/antenasur/dto/ProgresoEscrutinioCheck.java
java -cp target/progreso-check ec.com.antenasur.dto.ProgresoEscrutinioCheck
mvn -DskipTests compile
```

La prueba cubre 16 escenarios y los cinco hitos/filtros. La aceptacion integrada
requiere WildFly: abrir, guardar cuadre, cerrar, cargar, observar/rechazar,
validar, revertir y reabrir; comprobar filtro, paginacion, roles y vista movil.
