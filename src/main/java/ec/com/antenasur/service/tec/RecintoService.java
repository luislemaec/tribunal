package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.RecintoHistorialDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.GeograpFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.facade.tec.RecintoFacade;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.util.Constantes;

/**
 * Recintos electorales. Lecturas abiertas (@PermitAll: acta, padrón, reportes, App); escrituras
 * solo Administrador o Tribunal en el EJB. WildFly niega los métodos sin anotar de un EJB con
 * seguridad, por eso la anotación de clase.
 */
@Stateless
@PermitAll
@DeclareRoles({ RecintoService.ROL_ADMINISTRADOR, RecintoService.ROL_TRIBUNAL })
public class RecintoService extends AbstractService<Recinto, Integer, RecintoFacade> {

    static final String ROL_ADMINISTRADOR = "SITEC-Administrador";
    static final String ROL_TRIBUNAL = "SITEC-Tribunal";

    @Inject
    private RecintoFacade recintoFacade;

    @Inject
    private GeograpFacade geograpFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Override
    protected RecintoFacade getFacade() {
        return recintoFacade;
    }

    @Override
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public Recinto create(Recinto entity) {
        return super.create(entity);
    }

    @Override
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public Recinto edit(Recinto entity) {
        return super.edit(entity);
    }

    @Override
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public Recinto delete(Recinto entity) {
        return super.delete(entity);
    }

    @Override
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public void remove(Recinto entity) {
        super.remove(entity);
    }

    public Recinto buscaRecintoPorNombre(String nombreRecinto) {
        return recintoFacade.buscaRecintoPorNombre(nombreRecinto);
    }

    public List<Recinto> getRecintosPorParroquias(List<Geograp> parroquias) {
        return recintoFacade.getRecintosPorParroquias(parroquias);
    }

    // ----- API basada en DTO -----

    public RecintoDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return RecintoDTO.fromEntity(recintoFacade.find(id));
    }

    public List<RecintoDTO> listarDTOs() {
        return mapearLista(recintoFacade.findAll());
    }

    public List<RecintoDTO> listarDTOsPorParroquias(List<Geograp> parroquias) {
        return mapearLista(recintoFacade.getRecintosPorParroquias(parroquias));
    }

    public List<RecintoDTO> listarDTOsPorProceso(Integer procesoId) {
        return mapearLista(recintoFacade.listarPorProceso(procesoId));
    }

    public RecintoDTO buscarDTOPorNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return null;
        }
        return RecintoDTO.fromEntity(recintoFacade.buscaRecintoPorNombre(nombre));
    }

    /**
     * Persiste el recinto descrito por el DTO. Resuelve {@code ubicacionId}
     * contra {@link GeograpFacade}. Si el id es null crea, si no, hidrata la
     * entidad existente con los campos del DTO (preservando auditoría).
     */
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public RecintoDTO guardarDesdeDTO(RecintoDTO dto) {
        if (dto == null) {
            return null;
        }
        Geograp ubicacion = (dto.getUbicacionId() != null)
                ? geograpFacade.find(dto.getUbicacionId()) : null;
        if (ubicacion == null) {
            return null;
        }

        if (dto.getId() == null) {
            Recinto nuevo = dto.toEntity();
            nuevo.setUbicacion(ubicacion);
            return RecintoDTO.fromEntity(recintoFacade.create(nuevo));
        }
        Recinto actual = recintoFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        boolean cambiaParroquia = actual.getUbicacion() == null || !actual.getUbicacion().getId().equals(ubicacion.getId());
        if (cambiaParroquia) {
            // Cambiar la parroquia movería de ubicación todas sus mesas y su padrón, sin pasar por la
            // regla de ubicación del padrón: no se permite si el proceso activo ya usa el recinto.
            ProcesoElectoral activo = procesoElectoralFacade.getActivo();
            if (activo != null && mesaFacade.recintoEnUso(actual.getId(), activo.getId())) {
                throw new NegocioException(Constantes.getMensaje("recintos.mensaje.parroquia.en.uso", actual.getNombre()));
            }
        }
        actual.setNombre(dto.getNombre());
        actual.setUbicacion(ubicacion);
        actual.setEstadoTarea(dto.getEstadoTarea());
        return RecintoDTO.fromEntity(recintoFacade.edit(actual));
    }

    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public RecintoDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Recinto r = recintoFacade.find(id);
        if (r == null) {
            return null;
        }
        return RecintoDTO.fromEntity(recintoFacade.delete(r));
    }

    /**
     * Historial de cambios del recinto (auditoría Envers existente), de la revisión más reciente a
     * la más antigua. Lo consultan Administrador y Tribunal.
     */
    @RolesAllowed({ ROL_ADMINISTRADOR, ROL_TRIBUNAL })
    public List<RecintoHistorialDTO> obtenerHistorial(Integer recintoId) {
        return construirHistorial(recintoFacade.listarRevisionesAuditoria(recintoId));
    }

    /** Índices de la consulta {@code RecintoFacade.listarRevisionesAuditoria}. */
    private static final int REV = 0, TIPO = 1, FECHA = 2, NOMBRE = 3, PARROQUIA = 4, ESTADO = 5, USUARIO = 6,
            CANTON = 7, PROVINCIA = 8;

    /**
     * Interpreta las revisiones en orden cronológico: tipo 0 es alta y 2 eliminación; en una
     * modificación, el cambio de estado es baja o reactivación y, si no, actualización. La primera
     * revisión de un recinto cargado por migración (sin alta auditada) se muestra como «registro
     * auditado». Se marcan los campos que difieren de la revisión anterior.
     */
    static List<RecintoHistorialDTO> construirHistorial(List<Object[]> filas) {
        List<RecintoHistorialDTO> resultado = new ArrayList<>();
        if (filas == null) {
            return resultado;
        }
        Object[] previa = null;
        for (Object[] fila : filas) {
            int tipo = fila[TIPO] == null ? 1 : ((Number) fila[TIPO]).intValue();
            RecintoHistorialDTO h = new RecintoHistorialDTO();
            h.setRevision(fila[REV] == null ? null : ((Number) fila[REV]).intValue());
            h.setFecha((java.util.Date) fila[FECHA]);
            h.setUsuario(textoHistorial(fila[USUARIO]));
            h.setNombre(textoHistorial(fila[NOMBRE]));
            h.setParroquia(textoHistorial(fila[PARROQUIA]));
            h.setCanton(textoHistorial(fila[CANTON]));
            h.setProvincia(textoHistorial(fila[PROVINCIA]));
            if (tipo == 0) {
                h.setAccion(RecintoHistorialDTO.ACCION_CREA);
            } else if (tipo == 2) {
                h.setAccion(RecintoHistorialDTO.ACCION_ELIMINA);
            } else if (previa == null) {
                h.setAccion(RecintoHistorialDTO.ACCION_BASE);
            } else if (!java.util.Objects.equals(previa[ESTADO], fila[ESTADO])) {
                h.setAccion(Boolean.FALSE.equals(fila[ESTADO]) ? RecintoHistorialDTO.ACCION_DESACTIVA
                        : RecintoHistorialDTO.ACCION_REACTIVA);
            } else {
                h.setAccion(RecintoHistorialDTO.ACCION_ACTUALIZA);
            }
            if (previa != null && tipo != 0) {
                if (!java.util.Objects.equals(textoHistorial(previa[NOMBRE]), h.getNombre())) {
                    h.getCambiados().add(RecintoHistorialDTO.CAMPO_NOMBRE);
                    h.setNombreAnterior(textoHistorial(previa[NOMBRE]));
                }
                if (!java.util.Objects.equals(textoHistorial(previa[PARROQUIA]), h.getParroquia())) {
                    h.getCambiados().add(RecintoHistorialDTO.CAMPO_PARROQUIA);
                    h.setParroquiaAnterior(textoHistorial(previa[PARROQUIA]));
                }
            }
            resultado.add(h);
            previa = fila;
        }
        java.util.Collections.reverse(resultado);
        return resultado;
    }

    private static String textoHistorial(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private List<RecintoDTO> mapearLista(List<Recinto> recintos) {
        List<RecintoDTO> resultado = new ArrayList<>();
        if (recintos == null) {
            return resultado;
        }
        for (Recinto r : recintos) {
            resultado.add(RecintoDTO.fromEntity(r));
        }
        return resultado;
    }
}
