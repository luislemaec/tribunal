package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.bean.AyudaConversacion;
import ec.com.antenasur.dto.AyudaContactoDTO;
import ec.com.antenasur.dto.AyudaRespuestaDTO;
import ec.com.antenasur.dto.MensajeAyudaDTO;
import ec.com.antenasur.security.menu.PaginasMenu;
import ec.com.antenasur.service.tec.AyudaService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Asistente de ayuda en formato de conversación (template.xhtml). Por reglas, sin IA:
 * responde con las preguntas aprobadas por el Tribunal. La conversación vive en la sesión
 * ({@link AyudaConversacion}) y se conserva al cambiar de pantalla. No consulta nada hasta
 * que el usuario abre el panel.
 */
@Named
@ViewScoped
@Slf4j
public class AyudaController implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tope de consultas sin respuesta registradas por vista. */
    private static final int MAX_REGISTROS_POR_VISTA = 30;

    /** Preguntas relacionadas que se ofrecen bajo una respuesta. */
    private static final int MAX_RELACIONADAS = 3;

    @Inject
    private AyudaService ayudaService;

    @Inject
    private AyudaConversacion conversacion;

    @Getter
    @Setter
    private String consulta;

    @Getter
    private List<AyudaContactoDTO> contactos = Collections.emptyList();

    private boolean contactosCargados;

    private int registros;

    /** Al abrir el panel: saludo la primera vez y sugerencias al llegar a otra pantalla. */
    public void abrir() {
        conversacion.marcarVistos();
        cargarContactos();
        String pagina = paginaActual();
        if (conversacion.isVacia()) {
            saludar(pagina);
        } else if (!Objects.equals(pagina, conversacion.getUltimaPaginaSugerida())) {
            List<AyudaRespuestaDTO> sugerencias = sugerencias(pagina);
            conversacion.setUltimaPaginaSugerida(pagina);
            if (!sugerencias.isEmpty()) {
                MensajeAyudaDTO mensaje = MensajeAyudaDTO.delAsistente(JsfUtil.getMessage("ayuda.chat.sugerenciasPantalla"));
                mensaje.getOpciones().addAll(sugerencias);
                conversacion.agregar(mensaje);
            }
        }
    }

    /** Enviar lo escrito (Enter o botón). */
    public void enviar() {
        String texto = consulta == null ? "" : consulta.trim();
        consulta = null;
        if (texto.isEmpty()) {
            return;
        }
        if (texto.length() > AyudaService.MAX_LONGITUD_CONSULTA) {
            texto = texto.substring(0, AyudaService.MAX_LONGITUD_CONSULTA);
        }
        conversacion.agregar(MensajeAyudaDTO.delUsuario(texto));
        if (texto.length() < AyudaService.MIN_LONGITUD_CONSULTA) {
            conversacion.agregar(MensajeAyudaDTO.delAsistente(JsfUtil.getMessage("ayuda.chat.muyCorta")));
            return;
        }
        boolean registrar = registros < MAX_REGISTROS_POR_VISTA;
        registros++;
        List<AyudaRespuestaDTO> encontradas;
        try {
            encontradas = ayudaService.buscar(texto, paginaActual(), registrar);
        } catch (Exception e) {
            log.warn("No se pudo buscar en la ayuda", e);
            conversacion.agregar(error());
            return;
        }
        responder(encontradas);
    }

    /** Clic en una pregunta sugerida: se muestra como si el usuario la hubiera escrito. */
    public void elegir(Integer preguntaId) {
        AyudaRespuestaDTO respuesta;
        try {
            respuesta = ayudaService.respuesta(preguntaId);
        } catch (Exception e) {
            log.warn("No se pudo obtener la respuesta de ayuda id={}", preguntaId, e);
            conversacion.agregar(error());
            return;
        }
        if (respuesta == null) {
            conversacion.agregar(MensajeAyudaDTO.delAsistente(JsfUtil.getMessage("ayuda.chat.noDisponible")));
            return;
        }
        conversacion.agregar(MensajeAyudaDTO.delUsuario(respuesta.getPregunta()));
        responder(List.of(respuesta));
    }

    /** «¿Le sirvió?»: un voto por mensaje. */
    public void valorar(String mensajeId, boolean util) {
        conversacion.marcarVistos();
        MensajeAyudaDTO mensaje = buscarMensaje(mensajeId);
        if (mensaje == null || mensaje.isValorado() || mensaje.getRespuesta() == null) {
            return;
        }
        mensaje.setValorado(true);
        try {
            ayudaService.valorar(mensaje.getRespuesta().getId(), util);
        } catch (Exception e) {
            log.warn("No se pudo registrar la valoracion de ayuda", e);
        }
    }

    public void mostrarContacto() {
        cargarContactos();
        MensajeAyudaDTO mensaje = MensajeAyudaDTO.delAsistente(JsfUtil.getMessage("ayuda.chat.contacto"));
        mensaje.setConContacto(true);
        conversacion.agregar(mensaje);
    }

    public void nuevaConversacion() {
        conversacion.reiniciar();
        saludar(paginaActual());
    }

    public List<MensajeAyudaDTO> getMensajes() {
        return conversacion.getMensajes();
    }

    private void responder(List<AyudaRespuestaDTO> encontradas) {
        if (encontradas.isEmpty()) {
            MensajeAyudaDTO mensaje = MensajeAyudaDTO.delAsistente(JsfUtil.getMessage("ayuda.chat.sinRespuesta"));
            mensaje.setConContacto(true);
            conversacion.agregar(mensaje);
            return;
        }
        AyudaRespuestaDTO mejor = encontradas.get(0);
        MensajeAyudaDTO mensaje = MensajeAyudaDTO.delAsistente(mejor.getRespuesta());
        mensaje.setRespuesta(mejor);
        encontradas.stream().skip(1).limit(MAX_RELACIONADAS).forEach(mensaje.getOpciones()::add);
        conversacion.agregar(mensaje);
    }

    private void saludar(String pagina) {
        MensajeAyudaDTO saludo = MensajeAyudaDTO.delAsistente(JsfUtil.getMessage("ayuda.chat.saludo"));
        saludo.getOpciones().addAll(sugerencias(pagina));
        conversacion.agregar(saludo);
        conversacion.setUltimaPaginaSugerida(pagina);
    }

    private List<AyudaRespuestaDTO> sugerencias(String pagina) {
        try {
            return ayudaService.sugerencias(pagina);
        } catch (Exception e) {
            log.warn("No se pudieron cargar las sugerencias de ayuda", e);
            return Collections.emptyList();
        }
    }

    private void cargarContactos() {
        if (contactosCargados) {
            return;
        }
        try {
            contactos = ayudaService.contactos();
            contactosCargados = true;
        } catch (Exception e) {
            log.warn("No se pudieron cargar los contactos de ayuda", e);
        }
    }

    private MensajeAyudaDTO buscarMensaje(String id) {
        return conversacion.getMensajes().stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
    }

    private static MensajeAyudaDTO error() {
        MensajeAyudaDTO mensaje = MensajeAyudaDTO.delAsistente(JsfUtil.getMessage("ayuda.chat.error"));
        mensaje.setConContacto(true);
        return mensaje;
    }

    /** Página actual normalizada (p. ej. iglesias.jsf), para sugerencias por pantalla. */
    private static String paginaActual() {
        FacesContext contexto = FacesContext.getCurrentInstance();
        String vista = contexto == null || contexto.getViewRoot() == null ? null : contexto.getViewRoot().getViewId();
        return PaginasMenu.normalizar(vista);
    }
}
