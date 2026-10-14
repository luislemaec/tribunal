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

    /** Una categoría del acta con los votos registrados (0 si aún no hay conteo). */
    @Value
    public static class CategoriaConteo implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer categoriaId;
        String nombre;
        String tipo;
        /** Papeletas no utilizadas: no son votos emitidos, cuentan para el cuadre. */
        boolean papeletas;
        int votos;
    }

    @Value
    public static class ActaFisica implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer version;
        /** PENDIENTE_REVISION, VALIDADA, OBSERVADA o RECHAZADA. */
        String estadoRevision;
        String observacion;
    }

    /**
     * Estado del escrutinio de la mesa del Presidente y lo que puede hacer en él. Los
     * indicadores {@code puede*} son una ayuda para la App: TEC vuelve a validar cada paso.
     */
    @Value
    public static class ActaPresidente implements Serializable {
        private static final long serialVersionUID = 1L;
        Ubicacion ubicacion;
        String estado;
        int sufragantes;
        int votosEmitidos;
        int papeletasNoUtilizadas;
        /** Sufragantes − votos emitidos − papeletas no utilizadas; 0 = cuadrado. */
        int cuadre;
        String observacionConteo;
        /** Por qué no se puede abrir la mesa (junta incompleta, sin padrón…); null si se puede. */
        String motivoBloqueoApertura;
        boolean puedeAbrir;
        boolean puedeEditarConteo;
        boolean puedeCerrar;
        boolean puedeSubirActaFisica;
        List<CategoriaConteo> categorias;
        ActaFisica actaFisica;
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
        /** El cronograma vigente permite cambiar la habilitación de los miembros. */
        boolean permiteEdicion;
    }

    @Value
    public static class Miembro implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Id del vínculo iglesia-persona; TEC vuelve a validar el alcance al usarlo. */
        Integer id;
        String nombre;
        /** M, F o null (valor almacenado; no se deduce). */
        String sexo;
        boolean habilitado;
        boolean revisado;
    }

    /**
     * Datos editables de la persona de un miembro (solo IglesiaAdmin, su iglesia). Incluye la
     * cédula porque es un campo editable; las listas siguen sin enviarla.
     */
    @Value
    public static class MiembroDetalle implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer id;
        /** Nombre para mostrar, igual que en la lista (apellidos y nombres). */
        String nombre;
        /** «Apellidos y Nombres», igual que el formulario web (persona.nombres). */
        String nombres;
        String documento;
        /** M, F o null. */
        String sexo;
        boolean habilitado;
        boolean revisado;
        /** El cronograma vigente permite editar el padrón (misma regla que la web). */
        boolean permiteEdicion;
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

    /** Pendiente operativo del proceso (mismas alertas que el dashboard web). */
    @Value
    public static class Alerta implements Serializable {
        private static final long serialVersionUID = 1L;
        String texto;
        long cantidad;
        /** danger o warn. */
        String severidad;
    }

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
        List<Alerta> alertas;
    }

    /** Acta física vigente de una mesa, vista por un revisor (Administrador o Tribunal). */
    @Value
    public static class ActaFisicaRevision implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer version;
        String estadoRevision;
        String observacion;
        Date cargadaEn;
    }

    /** Detalle de una mesa del proceso activo para Administrador y Tribunal. */
    @Value
    public static class DetalleMesa implements Serializable {
        private static final long serialVersionUID = 1L;
        Ubicacion ubicacion;
        String estado;
        int electores;
        Date fechaApertura;
        Date fechaCierre;
        List<MiembroJunta> junta;
        /** Votos registrados por categoría (vacío si aún no hay conteo). */
        List<VotosCategoria> conteo;
        Integer votosRegistrados;
        Integer votosValidos;
        Integer votosBlancos;
        Integer votosNulos;
        /** Sufragantes − votos emitidos − papeletas no utilizadas; null sin conteo. */
        Integer cuadre;
        String observacionConteo;
        ActaFisicaRevision actaFisica;
        /** Mesa cerrada con acta física vigente aún no validada (misma regla que actaE). */
        boolean puedeRevisarActa;
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
