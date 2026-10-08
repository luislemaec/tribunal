package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Value;

/**
 * Vistas de solo lectura para la App móvil (docs/api-movil.md, «Módulo Tribunal»). Son
 * planas y con los datos mínimos: sin cédulas, correos ni identificadores internos que la
 * App no necesita.
 */
public final class ConsultaMovilDTO {

    private ConsultaMovilDTO() {
    }

    /** Ubicación de una mesa. */
    @Value
    public static class Ubicacion implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer mesaId;
        String mesa;
        String recinto;
        String parroquia;
        String canton;
    }

    @Value
    public static class Proceso implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer id;
        String nombre;
        String faseVigente;
    }

    // ---------------------------------------------------------------- Presidente de mesa

    @Value
    public static class MiembroJunta implements Serializable {
        private static final long serialVersionUID = 1L;
        String cargo;
        String nombre;
    }

    @Value
    public static class VotosCategoria implements Serializable {
        private static final long serialVersionUID = 1L;
        String categoria;
        String tipo;
        Integer votos;
    }

    /** Totales de la mesa; solo se entregan con la mesa cerrada. */
    @Value
    public static class ResultadosMesa implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer sufragantes;
        Integer votosRegistrados;
        Integer votosValidos;
        Integer votosBlancos;
        Integer votosNulos;
        List<VotosCategoria> categorias;
    }

    /** Mesa asignada al Presidente por su designación JRV en el proceso activo. */
    @Value
    public static class MesaPresidente implements Serializable {
        private static final long serialVersionUID = 1L;
        Proceso proceso;
        Ubicacion ubicacion;
        /** Estado del escrutinio (PENDIENTE si aún no se abrió). */
        String estado;
        Date fechaApertura;
        Date fechaCierre;
        int electores;
        List<MiembroJunta> junta;
        /** {@code null} mientras la mesa no esté cerrada. */
        ResultadosMesa resultados;
    }

    @Value
    public static class Elector implements Serializable {
        private static final long serialVersionUID = 1L;
        String nombre;
        String iglesia;
        Boolean sufrago;
    }

    // ---------------------------------------------------------------- IglesiaAdmin

    @Value
    public static class Iglesia implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer id;
        String nombre;
        String comunidad;
        String parroquia;
        String canton;
        String provincia;
        int totalMiembros;
        int miembrosHabilitados;
        int miembrosNoHabilitados;
        int informacionCompleta;
        int pendientesRevision;
        int enOtraIglesia;
    }

    @Value
    public static class Miembro implements Serializable {
        private static final long serialVersionUID = 1L;
        String nombre;
        boolean habilitado;
        boolean revisado;
    }

    @Value
    public static class Pagina<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        long total;
        int pagina;
        int tamano;
        List<T> elementos;
    }

    // ---------------------------------------------------------------- Tribunal / Administrador

    @Value
    public static class ResumenProceso implements Serializable {
        private static final long serialVersionUID = 1L;
        Proceso proceso;
        long iglesias;
        long recintos;
        long mesas;
        long electores;
        long mesasConJuntaCompleta;
        long mesasCerradas;
        int porcentajeEscrutinio;
        java.util.Map<String, Long> mesasPorEstado;
    }

    @Value
    public static class AvanceMesa implements Serializable {
        private static final long serialVersionUID = 1L;
        Ubicacion ubicacion;
        String estado;
        boolean juntaRegistrada;
    }

    @Value
    public static class ResultadoCategoria implements Serializable {
        private static final long serialVersionUID = 1L;
        String categoria;
        String tipo;
        String lista;
        long votos;
        java.math.BigDecimal porcentaje;
    }

    @Value
    public static class ResultadosProceso implements Serializable {
        private static final long serialVersionUID = 1L;
        String proceso;
        long mesas;
        long mesasCerradas;
        int porcentajeMesasCerradas;
        long votosRegistrados;
        long votosBlancos;
        long votosNulos;
        List<ResultadoCategoria> categorias;
        Date actualizado;
    }
}
