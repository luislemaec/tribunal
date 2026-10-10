package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.AyudaContactoDTO;
import ec.com.antenasur.dto.AyudaRespuestaDTO;
import ec.com.antenasur.service.tec.AyudaService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Widget del chatbot de ayuda (template.xhtml). No consulta nada al cargar la página:
 * los datos se piden solo cuando el usuario abre el panel o busca.
 */
@Named
@ViewScoped
@Slf4j
public class AyudaController implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tope de búsquedas por vista: evita llenar el registro de consultas sin respuesta. */
    private static final int MAX_BUSQUEDAS_POR_VISTA = 30;

    @Inject
    private AyudaService ayudaService;

    @Getter
    private boolean cargado;

    @Getter
    @Setter
    private String consulta;

    @Getter
    private boolean buscado;

    @Getter
    private List<AyudaRespuestaDTO> sugerencias = Collections.emptyList();

    @Getter
    private List<AyudaRespuestaDTO> resultados = Collections.emptyList();

    @Getter
    private List<AyudaContactoDTO> contactos = Collections.emptyList();

    private final Set<Integer> valoradas = new HashSet<>();

    private int busquedas;

    /** Al abrir el panel por primera vez en la vista. */
    public void abrir() {
        if (cargado) {
            return;
        }
        try {
            sugerencias = ayudaService.sugerencias(paginaActual());
            contactos = ayudaService.contactos();
        } catch (Exception e) {
            log.warn("No se pudo cargar la ayuda", e);
            sugerencias = Collections.emptyList();
        }
        cargado = true;
    }

    public void buscar() {
        abrir();
        String texto = consulta == null ? "" : consulta.trim();
        if (texto.length() < AyudaService.MIN_LONGITUD_CONSULTA) {
            limpiar();
            return;
        }
        if (busquedas >= MAX_BUSQUEDAS_POR_VISTA) {
            return;
        }
        busquedas++;
        try {
            resultados = ayudaService.buscar(texto, paginaActual());
        } catch (Exception e) {
            log.warn("No se pudo buscar en la ayuda", e);
            resultados = Collections.emptyList();
        }
        buscado = true;
    }

    public void limpiar() {
        consulta = null;
        resultados = Collections.emptyList();
        buscado = false;
    }

    /** Un voto por pregunta y vista. */
    public void valorar(Integer preguntaId, boolean util) {
        if (preguntaId == null || !valoradas.add(preguntaId)) {
            return;
        }
        try {
            ayudaService.valorar(preguntaId, util);
        } catch (Exception e) {
            log.warn("No se pudo registrar la valoracion de ayuda id={}", preguntaId, e);
        }
    }

    public boolean isValorada(Integer preguntaId) {
        return valoradas.contains(preguntaId);
    }

    public boolean isSinResultados() {
        return buscado && resultados.isEmpty();
    }

    /** Lista que se muestra: resultados si hubo búsqueda, si no las sugerencias. */
    public List<AyudaRespuestaDTO> getRespuestas() {
        return buscado ? resultados : sugerencias;
    }

    private static String paginaActual() {
        FacesContext contexto = FacesContext.getCurrentInstance();
        return contexto == null || contexto.getViewRoot() == null ? null : contexto.getViewRoot().getViewId();
    }
}
