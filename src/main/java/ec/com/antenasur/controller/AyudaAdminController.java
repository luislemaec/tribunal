package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.dto.AyudaContactoDTO;
import ec.com.antenasur.dto.AyudaPreguntaAdminDTO;
import ec.com.antenasur.dto.AyudaSinRespuestaDTO;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.security.menu.AutorizacionMenuService;
import ec.com.antenasur.service.tec.AyudaService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/** Mantenimiento del chatbot de ayuda (ayuda.xhtml): preguntas, consultas sin respuesta y contactos. */
@Named
@ViewScoped
@Slf4j
public class AyudaAdminController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AyudaService ayudaService;

    @Inject
    private AutorizacionMenuService autorizacionMenuService;

    @Getter
    private List<AyudaPreguntaAdminDTO> preguntas = Collections.emptyList();

    @Getter
    @Setter
    private List<AyudaPreguntaAdminDTO> preguntasFiltradas;

    @Getter
    private List<AyudaSinRespuestaDTO> sinRespuesta = Collections.emptyList();

    @Getter
    private List<AyudaContactoDTO> contactos = Collections.emptyList();

    /** Roles para el selector: id y nombre (Rol no se guarda en el estado de la vista). */
    @Getter
    private List<Object[]> roles = Collections.emptyList();

    /** Pantallas válidas para «sugerir en» y «enlace»: las del menú del usuario. */
    @Getter
    private List<String> paginas = Collections.emptyList();

    @Getter
    private final FaseElectoral[] fases = FaseElectoral.values();

    @Getter
    private final List<String> tiposContacto = List.of("WHATSAPP", "TELEFONO", "CORREO");

    @Getter
    @Setter
    private AyudaPreguntaAdminDTO pregunta = new AyudaPreguntaAdminDTO();

    @Getter
    @Setter
    private AyudaContactoDTO contacto = new AyudaContactoDTO();

    @Getter
    @Setter
    private int pestanaActiva;

    @PostConstruct
    public void init() {
        try {
            List<Object[]> opciones = new ArrayList<>();
            for (Rol rol : ayudaService.rolesDisponibles()) {
                opciones.add(new Object[] { rol.getId(), rol.getNombre() });
            }
            roles = opciones;
            paginas = autorizacionMenuService.paginasActuales().stream().sorted().toList();
        } catch (Exception e) {
            log.error("Error al cargar los catalogos de ayuda", e);
        }
        cargar();
    }

    private void cargar() {
        try {
            preguntas = ayudaService.listarPreguntas();
            preguntasFiltradas = null;
            sinRespuesta = ayudaService.listarSinRespuesta();
            contactos = ayudaService.listarContactos();
        } catch (Exception e) {
            log.error("Error al cargar la ayuda", e);
            JsfUtil.addErrorMessage(JsfUtil.getMessage("ayuda.admin.error.carga"));
        }
    }

    public long getTotalActivas() {
        return preguntas.stream().filter(AyudaPreguntaAdminDTO::isActivo).count();
    }

    public long getTotalBorradores() {
        return preguntas.size() - getTotalActivas();
    }

    public void nuevaPregunta() {
        pregunta = new AyudaPreguntaAdminDTO();
        pregunta.setActivo(true);
    }

    public void editarPregunta(AyudaPreguntaAdminDTO seleccionada) {
        pregunta = copiar(seleccionada);
    }

    /** «Crear pregunta» desde una consulta sin respuesta: la abre en Preguntas ya prellenada. */
    public void crearDesdeSinRespuesta(AyudaSinRespuestaDTO consulta) {
        nuevaPregunta();
        pregunta.setPregunta(consulta.getTexto());
        pregunta.setPagina(consulta.getPagina() == null || consulta.getPagina().isBlank() ? null : consulta.getPagina());
        pestanaActiva = 0;
    }

    public void guardarPregunta() {
        try {
            ayudaService.guardarPregunta(pregunta);
            JsfUtil.addSuccessMessage(JsfUtil.getMessage("ayuda.admin.pregunta.guardada"));
            cargar();
            PrimeFaces.current().executeScript("PF('wdDlgPregunta').hide()");
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error al guardar la pregunta de ayuda", e);
            JsfUtil.addErrorMessage(JsfUtil.getMessage("ayuda.admin.error.guardar"));
        }
    }

    public void descartarSinRespuesta(AyudaSinRespuestaDTO consulta) {
        try {
            ayudaService.descartarSinRespuesta(consulta.getId());
            sinRespuesta = ayudaService.listarSinRespuesta();
        } catch (Exception e) {
            log.error("Error al descartar consulta sin respuesta id={}", consulta.getId(), e);
            JsfUtil.addErrorMessage(JsfUtil.getMessage("ayuda.admin.error.guardar"));
        }
    }

    public void nuevoContacto() {
        contacto = new AyudaContactoDTO();
        contacto.setTipo("WHATSAPP");
    }

    public void editarContacto(AyudaContactoDTO seleccionado) {
        contacto = new AyudaContactoDTO();
        contacto.setId(seleccionado.getId());
        contacto.setTipo(seleccionado.getTipo());
        contacto.setValor(seleccionado.getValor());
        contacto.setEtiqueta(seleccionado.getEtiqueta());
        contacto.setHorario(seleccionado.getHorario());
        contacto.setOrden(seleccionado.getOrden());
        contacto.setActivo(seleccionado.isActivo());
    }

    public void guardarContacto() {
        try {
            ayudaService.guardarContacto(contacto);
            JsfUtil.addSuccessMessage(JsfUtil.getMessage("ayuda.admin.contacto.guardado"));
            contactos = ayudaService.listarContactos();
            PrimeFaces.current().executeScript("PF('wdDlgContacto').hide()");
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error al guardar el contacto de ayuda", e);
            JsfUtil.addErrorMessage(JsfUtil.getMessage("ayuda.admin.error.guardar"));
        }
    }

    private static AyudaPreguntaAdminDTO copiar(AyudaPreguntaAdminDTO origen) {
        AyudaPreguntaAdminDTO copia = new AyudaPreguntaAdminDTO();
        copia.setId(origen.getId());
        copia.setPregunta(origen.getPregunta());
        copia.setRespuesta(origen.getRespuesta());
        copia.setPalabrasClave(origen.getPalabrasClave());
        copia.setPagina(origen.getPagina());
        copia.setFase(origen.getFase());
        copia.setEnlacePagina(origen.getEnlacePagina());
        copia.setOrden(origen.getOrden());
        copia.setActivo(origen.isActivo());
        copia.setRolIds(new ArrayList<>(origen.getRolIds()));
        return copia;
    }
}
