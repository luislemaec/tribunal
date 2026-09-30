package ec.com.antenasur.dto;

import ec.com.antenasur.enums.EstadoEscrutinio;
import static ec.com.antenasur.enums.EstadoEscrutinio.*;

/** Ejecutable sin JSF/EJB ni base de datos: verifica la proyeccion, no transiciones. */
public class ProgresoEscrutinioCheck {
    public static void main(String[] args) {
        verificar(null, false, null, 0, 0, false);
        verificar(PENDIENTE, false, null, 0, 0, false);
        verificar(ABIERTO, true, null, 1, 1, false);
        verificar(EN_CONTEO, true, null, 1, 1, false);
        verificar(CONTEO_REGISTRADO, true, null, 2, 2, false);
        verificar(CERRADO, true, null, 3, 3, false);
        verificar(CERRADO, true, "PENDIENTE_REVISION", 4, 4, false);
        verificar(CERRADO, true, "VALIDADA", 4, 5, true);
        verificar(CERRADO, true, "OBSERVADA", 3, 3, false);
        verificar(CERRADO, true, "RECHAZADA", 3, 3, false);
        verificar(REABIERTO, true, "VALIDADA", 1, 1, false);
        verificar(OBSERVADO, true, "VALIDADA", 1, 1, false);
        verificar(OBSERVADO, false, null, 0, 0, false);
        verificar(ANULADO, true, "VALIDADA", 1, 1, false);
        verificar(ANULADO, false, null, 0, 0, false);
        verificar(CERRADO, true, "DESCONOCIDA", 4, 4, false);
        for (EstadoEscrutinio estado : new EstadoEscrutinio[]{ANULADO, OBSERVADO}) {
            var p = ProgresoEscrutinioDTO.determinar(estado, true, null);
            exigir(p.isTieneIncidencia() && p.estadoPaso(p.getIndice()).equals("bloqueado"), "Bloqueo explicito");
        }
        for (String revision : new String[]{"OBSERVADA", "RECHAZADA"}) {
            var p = ProgresoEscrutinioDTO.determinar(CERRADO, true, revision);
            exigir(p.isTieneIncidencia() && !p.completado(3), "Acta devuelta no completa");
        }
        System.out.println("OK: 16 escenarios; 5 hitos; filtro exacto; incidencias; reapertura y oficializacion.");
    }

    private static void verificar(EstadoEscrutinio estado, boolean apertura, String revision,
            int indice, int completados, boolean oficial) {
        var p = ProgresoEscrutinioDTO.determinar(estado, apertura, revision);
        exigir(p.getIndice() == indice && p.isOficial() == oficial, "Etapa: " + estado + "/" + revision);
        for (int i = 0; i < 5; i++) {
            exigir(p.completado(i) == (i < completados), "Hito " + i + ": " + estado);
            exigir(p.coincideFiltro(ProgresoEscrutinioDTO.Etapa.values()[i].name()) == (i == indice),
                    "Filtro no acumulativo " + i);
        }
        exigir(p.coincideFiltro(null) && p.coincideFiltro(""), "Todos");
        exigir(!p.coincideFiltro("S:EN_PROCESO"), "No admite clasificacion anterior");
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
