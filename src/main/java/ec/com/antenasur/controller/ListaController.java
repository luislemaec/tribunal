package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.ListaDTO;
import ec.com.antenasur.service.tec.ListaService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class ListaController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmListas";
    private static final String TABLA = "tblListas";
    private static final String MENSAJE_REGISTRA_OK = "Lista registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Lista actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Lista eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

    @Inject
    private LoginBean loginBean;

    @Inject
    private ListaService listaService;

    @Setter
    @Getter
    private ListaDTO listaSeleccionado;

    @Setter
    @Getter
    private List<ListaDTO> listas, listasSeleccionadas;

    @PostConstruct
    private void init() {
        try {
            this.listasSeleccionadas = new ArrayList<>();
            this.listas = listaService.listarDTOs();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializarSeleccionado() {
        if (listas != null) {
            listas.clear();
        }
        this.listaSeleccionado = new ListaDTO();
    }

    public void nuevo() {
        inicializarSeleccionado();
    }

    public boolean existeSeleccionados() {
        return this.listasSeleccionadas != null && !this.listasSeleccionadas.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeSeleccionados()) {
            int size = this.listasSeleccionadas.size();
            return size > 1 ? size + " Listas seleccionadas" : "1 lista seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarSeleccionado() {
        if (listaSeleccionado != null && listaSeleccionado.getId() != null) {
            listaService.eliminarPorId(listaSeleccionado.getId());
        }
        JsfUtil.addInfoMessage(MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
    }

    public void guardarSeleccionado() {
        try {
            if (listaSeleccionado == null) {
                return;
            }
            boolean esEdicion = listaSeleccionado.getId() != null;
            ListaDTO persistido = listaService.guardarDesdeDTO(listaSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessage(esEdicion ? MENSAJE_ACTUALIZA_OK : MENSAJE_REGISTRA_OK);
                listaSeleccionado = null;
                listas = listaService.listarDTOs();
                PrimeFaces.current().ajax().update("msgs", FORMULARIO);
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR LISTA", e);
        }
        PrimeFaces.current().executeScript("PF('dlgMesa').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA);
    }

    public void eliminarSeleccionados() {
        int eliminados = 0;
        if (listasSeleccionadas != null) {
            for (ListaDTO item : listasSeleccionadas) {
                if (item.getId() != null && listaService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        JsfUtil.addInfoMessage(eliminados + " " + MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
    }

    public void cargaDatosSeleccionado() {
        // hook para hidratar datos relacionados al seleccionar fila
    }
}
