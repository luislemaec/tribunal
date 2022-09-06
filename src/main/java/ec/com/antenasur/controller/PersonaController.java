package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.Iglesia;
import ec.com.antenasur.domain.IglesiaPersona;
import ec.com.antenasur.domain.Persona;
import ec.com.antenasur.service.GeograpFacade;
import ec.com.antenasur.service.IglesiaFacade;
import ec.com.antenasur.service.IglesiaPersonaFacade;
import ec.com.antenasur.service.PersonaFacade;
import ec.com.antenasur.util.JsfUtil;
import java.io.InputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class PersonaController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    //private static final Logger LOG = Logger.getLogger(cargarControl.class);
    @Inject
    private LoginBean loginBean;

    @Inject
    private PersonaFacade personaFacade;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private GeograpFacade geograpFacade;

    @Getter
    private Persona personaSeleccionado;

    @Setter
    @Getter
    private Iglesia iglesiaSeleccionado;

    @Setter
    @Getter
    private IglesiaPersona iglesiaPersonaSeleccionado;

    @Setter
    @Getter
    private Geograp parroquiaSeleccionado, cantonSeleccionado;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private List<Iglesia> listaIglesias;

    @Setter
    @Getter
    private List<IglesiaPersona> listaIglesiaPersona, listaIglesiaPersonaSeleccionados;

    @Setter
    @Getter
    private UploadedFile file;

    @Setter
    @Getter
    private StreamedContent fileDown;

    @Setter
    @Getter
    private InputStream in;

    @Setter
    @Getter
    private XSSFWorkbook excelMigracion;

    @Setter
    @Getter
    private List<Persona> listaPersonas;

    @PostConstruct
    private void init() {
        try {
            listaIglesias = new ArrayList<>();
            listaIglesiaPersona = new ArrayList<>();
            parroquiaSeleccionado = cantonSeleccionado = new Geograp();
            iglesiaSeleccionado = new Iglesia();

            cantones = geograpFacade.findByFatherId(7);

            listaIglesias = iglesiaFacade.findAll();
            //obtiene todas las iglesias
            listaIglesiaPersona = iglesiaPersonaFacade.findAll();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpFacade.find(cantonSeleccionado.getId());
            parroquias = geograpFacade.findByFatherId(cantonSeleccionado.getId());

            listaIglesiaPersona = iglesiaPersonaFacade.getIglesiasPersonasPorParroquias(parroquias);
            listaIglesias = iglesiaFacade.getIglesiasPorParroquias(parroquias);
        } else {
            parroquias.clear();
            iglesiaSeleccionado = new Iglesia();
            listaIglesias.clear();
            listaIglesiaPersona.clear();
        }
    }

    public void obtieneIglesiasPorParroquia() {
        if (parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpFacade.find(parroquiaSeleccionado.getId());

            List<Geograp> parroquiasTmp = new ArrayList<>();
            parroquiasTmp.add(parroquiaSeleccionado);

            listaIglesias = iglesiaFacade.getIglesiasPorParroquias(parroquiasTmp);
            listaIglesiaPersona = iglesiaPersonaFacade.getIglesiasPersonasPorParroquias(parroquiasTmp);
            if (listaIglesias == null) {

                JsfUtil.addWarningMessage("No existe registro de Iglesias en " + parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessage(listaIglesias.size() + " Iglesias registradas");
            }
        } else {
            iglesiaSeleccionado = new Iglesia();
            listaIglesias.clear();
            listaIglesiaPersona.clear();
        }
    }

    public void obtienePersonasPorIglesias() {
        if (iglesiaSeleccionado.getId() != null) {
            iglesiaSeleccionado = iglesiaFacade.find(iglesiaSeleccionado.getId());
            listaIglesiaPersona = iglesiaPersonaFacade.getPersonasIglesiasPorIglesia(iglesiaSeleccionado.getId());
            if (listaIglesiaPersona == null) {
                JsfUtil.addWarningMessage("No existe registro de personas en " + iglesiaSeleccionado.getNombre());
            } else {
                JsfUtil.addInfoMessage(listaIglesiaPersona.size() + " personas registradas");
            }
        } else {
            listaIglesiaPersona.clear();
        }
    }

    /**
     * Inicializa medio seleccionado
     */
    public void inicializaPersonaSeleccionado() {
        if (listaIglesiaPersona != null) {
            listaIglesiaPersona.clear();
        }
        iglesiaPersonaSeleccionado = new IglesiaPersona();
        iglesiaPersonaSeleccionado.setPersona(new Persona());
        iglesiaPersonaSeleccionado.setIglesia(new Iglesia());
        this.iglesiaSeleccionado = new Iglesia();
        this.personaSeleccionado = new Persona();
    }

    public void nuevaPersona() {
        inicializaPersonaSeleccionado();
    }

    public boolean existeIglesiaPersonasSeleccionadas() {
        return this.listaIglesiaPersonaSeleccionados != null && !this.listaIglesiaPersonaSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeIglesiaPersonasSeleccionadas()) {
            int size = this.listaIglesiaPersonaSeleccionados.size();
            return size > 1 ? size + " personas seleccionadas" : "1 persona seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarIglesiaPersonaSeleccionadas() {
        if (listaIglesiaPersonaSeleccionados != null) {
            for (IglesiaPersona item : listaIglesiaPersonaSeleccionados) {
                iglesiaPersonaFacade.delete(item);
            }
        }
        listaIglesiaPersona = iglesiaPersonaFacade.findAll();
        JsfUtil.addInfoMessage(+listaIglesiaPersonaSeleccionados.size() + " Personas eliminadas");
        this.listaIglesiaPersonaSeleccionados = null;
        PrimeFaces.current().ajax().update("frmPersonas", "msgs");
        
    }

    public void buscaPersonaPorCedula() {
        if (iglesiaPersonaSeleccionado != null) {
            Persona personaBuscado = personaFacade.buscarPorCedula(iglesiaPersonaSeleccionado.getPersona().getDocumento());
            if (personaBuscado != null) {
                iglesiaPersonaSeleccionado.setPersona(personaBuscado);
                JsfUtil.addInfoMessage("Persona con CI: " + personaBuscado.getDocumento() + " ya se encuentra registrado ");
            }
        }
    }

    public void actualizarPersona() {
        try {
            setPersonaSeleccionado(iglesiaPersonaSeleccionado.getPersona());
            setIglesiaSeleccionado(iglesiaPersonaSeleccionado.getIglesia());
            iglesiaPersonaSeleccionado.setPersona(getPersonaSeleccionado());
            iglesiaPersonaSeleccionado.setIglesia(getIglesiaSeleccionado());
            if (personaSeleccionado != null && iglesiaSeleccionado != null) {
                if (this.iglesiaPersonaSeleccionado.getId() != null
                        && this.iglesiaPersonaSeleccionado.getIglesia() != null
                        && this.iglesiaPersonaSeleccionado.getPersona() != null) {
                    IglesiaPersona iglesiaPersonaActualiza = iglesiaPersonaFacade.edit(iglesiaPersonaSeleccionado);
                    if (iglesiaPersonaActualiza != null) {
                        JsfUtil.addSuccessMessage("Persona actualido");
                        iglesiaSeleccionado = null;
                        personaSeleccionado = null;
                        iglesiaPersonaSeleccionado = null;
                        listaIglesiaPersona = iglesiaPersonaFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", "frmIglesias");
                    }
                } else {
                    IglesiaPersona iglesiaPersonaActualiza = iglesiaPersonaFacade.create(iglesiaPersonaSeleccionado);
                    if (iglesiaPersonaActualiza != null) {
                        JsfUtil.addSuccessMessage("Persona agregado");
                        iglesiaSeleccionado = null;
                        personaSeleccionado = null;
                        iglesiaPersonaSeleccionado = null;
                        PrimeFaces.current().ajax().update("msgs", "frmIglesias");
                    }
                }
            }
        } catch (Exception e) {
        }
        PrimeFaces.current().executeScript("PF('dlgPersona').hide()");
        PrimeFaces.current().ajax().update("frmPersonas:messages", "frmPersonas:tblPersonas");
    }

    private void setPersonaSeleccionado(Persona persona) {
        try {
            if (persona != null) {
                if (persona.getId() != null) {
                    this.personaSeleccionado = personaFacade.edit(persona);
                } else {
                    this.personaSeleccionado = personaFacade.create(persona);
                }
            }
        } catch (Exception e) {
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            file = event.getFile();
            if (file != null && file.getContent() != null && file.getContent().length > 0 && file.getFileName() != null) {
                /**
                 * Obtiene el excel cargado para su posterior tratamiento
                 */
                excelMigracion = new XSSFWorkbook(file.getInputStream());
                if (excelMigracion == null) {
                    JsfUtil.addFatalMessage("Error al procesar archivo");
                } else {
                    cargaArchivoABD();
                    JsfUtil.addInfoMessage("Archivo cargado correctamente");
                }
            }
        } catch (Exception e) {
            JsfUtil.addFatalMessage("Error en formato de archivo");
            file = null;
            log.error("ERROR AL CARGAR ARCHIVO", e);
        }
    }

    public void cargaArchivoABD() {
        if (file != null) {
            procesaArchivo(file);
            JsfUtil.addSuccessMessage(file.getFileName() + " is uploaded.");
        }
    }

    public void procesaArchivo(UploadedFile file) {
        try {
            if (file != null && file.getContent() != null && file.getContent().length > 0 && file.getFileName() != null) {
                if (excelMigracion != null) {
                    //Obtenemos la primera pestaña
                    XSSFSheet hojaUno = excelMigracion.getSheetAt(0);
                    //Obtenemos el interator  que nos permite recorrer cada una de las filas que contiene.
                    Iterator<Row> filasInterator = hojaUno.iterator();

                    Row fila;

                    listaPersonas = new ArrayList<>();
                    listaIglesias = new ArrayList<>();
                    int contadorFila = 0;

                    while (filasInterator.hasNext()) {
                        Iglesia iglesiaTmp = new Iglesia();
                        Persona personaTmp = new Persona();
                        IglesiaPersona igpeTmp = new IglesiaPersona();
                        fila = filasInterator.next();
                        //Inicia en la fila 3 {0,1,2,3,4..}
                        if (contadorFila >= 2) {
                            //inicia en la columna (columna B)
                            for (int contadorColumna = 1; contadorColumna < fila.getLastCellNum(); contadorColumna++) {

                                Cell cell = fila.getCell(contadorColumna);
                                if (contadorColumna < fila.getLastCellNum()) {
                                    switch (contadorColumna) {
                                        case 0:
                                            break;
                                        case 1:
                                            String nombres = procesaTamanioColumna(cell.getStringCellValue().trim(), 100);
                                            personaTmp.setNombres(!nombres.isEmpty() ? nombres.toUpperCase() : null);
                                            break;
                                        case 2:
                                            String cedula = procesaTamanioColumna(cell.getStringCellValue().trim(), 11);
                                            personaTmp.setDocumento(!cedula.isEmpty() ? cedula : null);
                                            break;
                                        case 3:
                                            String sexo = procesaTamanioColumna(cell.getStringCellValue().trim(), 1);
                                            personaTmp.setSexo(sexo);
                                            break;
                                    }
                                }
                            }
                            Persona personaBuscado = personaFacade.buscarPorCedula(personaTmp.getDocumento());
                            if (personaBuscado != null) {

                            } else {
                                Persona personaNuevo = personaFacade.create(personaTmp);

                                IglesiaPersona iglesiaPersonaNueva = new IglesiaPersona();
                                iglesiaPersonaNueva.setPersona(personaNuevo);
                                iglesiaPersonaNueva.setIglesia(iglesiaSeleccionado);

                                iglesiaPersonaFacade.create(iglesiaPersonaNueva);
                            }
                        }
                        contadorFila++;
                    }
                    listaPersonas = personaFacade.findAll();
                    // cerramos el libro excel
                    excelMigracion.close();
                } else {
                    JsfUtil.addWarningMessage("Archivo formato incorrecto");
                }
            }

        } catch (Exception e) {
            log.error("ERROR AL CARGAR ARCHIVO", e);
        }
    }

    private String procesaTamanioColumna(String cadena, int tamanioColumna) {
        int tamanioCadena = cadena.length();
        String nuevaCadena = "";
        if (tamanioCadena > tamanioColumna) {
            nuevaCadena = cadena.substring(0, tamanioColumna - 1);
        } else {
            nuevaCadena = cadena;
        }
        return nuevaCadena;
    }

}
