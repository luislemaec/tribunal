package ec.com.antenasur.controller;

import ec.com.antenasur.bean.DocumentoBean;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Documentos;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.service.tec.MesaFacade;
import ec.com.antenasur.service.tec.RecintoFacade;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.ModeloColumna;
import ec.com.antenasur.util.ReflectionColumnModelBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class MesaController implements Serializable {
    
    private static final String DESTINATION = System.getProperty("java.io.tmpdir");
    
    private static final long serialVersionUID = 1L;
    
    private static final String FORMULARIO = "frmMesas";
    private static final String TABLA = "tblMesas";
    private static final String MENSAJE_REGISTRA_OK = "Mesa registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Mesa actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Mesa eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";
    
    @Inject
    private LoginBean loginBean;
    
    @Inject
    private MesaFacade mesaFacade;
    
    @Inject
    private RecintoFacade recintoFacade;
    
    @Inject
    private GeograpBean geograpBean;
    
    @Inject
    private DocumentoBean documentoBean;
    
    @Setter
    @Getter
    private Mesa mesaSeleccionado;
    
    @Setter
    @Getter
    private Recinto recintoSeleccionado;
    
    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;
    
    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;
    
    @Setter
    @Getter
    private List<Mesa> listaMesas, listaMesasSeleccionados;
    
    @Setter
    @Getter
    private List<Recinto> listaRecintos;
    
    @Setter
    @Getter
    private List<ModeloColumna> columnas = new ArrayList<ModeloColumna>(0);
    
    @Setter
    @Getter
    private List<Documentos> documentos;
    
    @PostConstruct
    private void init() {
        try {
            this.columnas = new ReflectionColumnModelBuilder(Mesa.class).setExcludedProperties("id", "fechaCrea", "fechaActualiza", "usuarioCrea", "usuarioActualiza",
                    "estado", "seleccionado", "persisted").build();
            
            this.cantonSeleccionado = parroquiaSeleccionado = new Geograp();
            this.recintoSeleccionado = new Recinto();
            this.listaMesas = this.listaMesasSeleccionados = new ArrayList<>();
            //Trae cantones de la provincia de Chimborazo
            this.cantones = geograpBean.getByFatherId(7);
            this.listaMesas = mesaFacade.findAll();
            
            this.listaRecintos = recintoFacade.findAll();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }
    
    public void inicializaMesaSeleccionado() {
        if (listaMesas != null) {
            listaMesas.clear();
        }
        this.mesaSeleccionado = new Mesa();
        this.mesaSeleccionado.setUbicacion(new Geograp());
        this.mesaSeleccionado.setRecinto(new Recinto());
    }
    
    private List<Integer> listaIdParroquias(List<Geograp> parroquias) {
        try {
            List<Integer> listaIdParroquias = null;
            if (parroquias != null) {
                listaIdParroquias = new ArrayList<>();
                for (Geograp item : parroquias) {
                    listaIdParroquias.add(item.getId());
                }
            }
            return listaIdParroquias;
        } catch (Exception e) {
            return null;
        }
    }
    
    public void nuevaMesa() {
        inicializaMesaSeleccionado();
    }
    
    public boolean existeMesasSeleccionados() {
        return this.listaMesasSeleccionados != null && !this.listaMesasSeleccionados.isEmpty();
    }
    
    public String getMensajeBotonEliminar() {
        if (existeMesasSeleccionados()) {
            int size = this.listaMesasSeleccionados.size();
            return size > 1 ? size + " Recintos seleccionadas" : "1 recinto seleccionada";
        }
        return "Eliminar";
    }
    
    public void eliminarMesaSeleccionado() {
        if (mesaSeleccionado != null) {
            mesaSeleccionado = mesaFacade.delete(mesaSeleccionado);
        }
        JsfUtil.addInfoMessage(MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
    }
    
    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpBean.getById(cantonSeleccionado.getId());
            parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
            
            listaRecintos = recintoFacade.getRecintosPorParroquias(parroquias);
            listaMesas = mesaFacade.getMesasPorRecintos(listaRecintos);
        } else {
            parroquias.clear();
            mesaSeleccionado = null;
            listaMesas.clear();
        }
    }
    
    public void obtieneMesasPorParroquia() {
        if (parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpBean.getById(parroquiaSeleccionado.getId());
            
            List<Integer> listaIdParroquias = new ArrayList<>();
            if (parroquiaSeleccionado != null) {
                listaIdParroquias.add(parroquiaSeleccionado.getId());
            }
            listaRecintos = recintoFacade.getRecintosPorParroquias(parroquias);
            listaMesas = mesaFacade.getMesasPorRecintos(listaRecintos);
            
            if (listaMesas == null) {
                
                JsfUtil.addWarningMessage("No existe registro de Iglesias en " + parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessage(listaMesas.size() + " Iglesias registradas");
            }
        } else {
            mesaSeleccionado = new Mesa();
            listaMesas.clear();
        }
    }
    
    public void guardarMesaSeleccionado() {
        try {
            if (mesaSeleccionado != null) {
                if (this.mesaSeleccionado.getId() != null) {
                    Mesa recintoActualiza = mesaFacade.edit(mesaSeleccionado);
                    if (recintoActualiza != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_ACTUALIZA_OK);
                        mesaSeleccionado = null;
                        listaMesas = mesaFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                } else {
                    Mesa iglesiaPersonaActualiza = mesaFacade.create(mesaSeleccionado);
                    if (iglesiaPersonaActualiza != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_REGISTRA_OK);
                        
                        mesaSeleccionado = null;
                        listaMesas = mesaFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                }
            }
        } catch (Exception e) {
        }
        PrimeFaces.current().executeScript("PF('dlgMesa').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA);
    }
    
    public void eliminarMesasSeleccionados() {
        if (listaMesasSeleccionados != null) {
            for (Mesa item : listaMesasSeleccionados) {
                mesaFacade.delete(item);
            }
        }
        JsfUtil.addInfoMessage(+listaMesasSeleccionados.size() + MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
    }
    
    public void cargaDatosMesaSeleccionado() {
        try {
            if (mesaSeleccionado.getId() != null) {
                if (mesaSeleccionado.getUbicacion().getId() != null) {
                    this.cantonSeleccionado = mesaSeleccionado.getUbicacion().getGeograp();
                    this.parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
                }
            }
        } catch (Exception e) {
        }
    }
    
    public void obtieneMesasPorRecinto() {
        if (recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoFacade.find(recintoSeleccionado.getId());
            
            List<Recinto> listaRecintosTmp = new ArrayList<>();
            if (recintoSeleccionado != null) {
                listaRecintosTmp.add(recintoSeleccionado);
            }
            
            listaMesas = mesaFacade.getMesasPorRecintos(listaRecintosTmp);
            if (listaMesas == null) {
                JsfUtil.addWarningMessage("No existe registro de personas en " + recintoSeleccionado.getNombre());
            } else {
                JsfUtil.addInfoMessage(listaMesas.size() + " personas registradas");
            }
        } else {
            listaMesas.clear();
        }
    }
    
    public void cargaActasE() {
        try {
            documentos = documentoBean.getDocumentoPorMesa(mesaSeleccionado);
        } catch (Exception e) {
            log.error("ERROR AL OBTENER DOCUMENTOS", e);
        }
    }
}
