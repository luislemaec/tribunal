package ec.com.antenasur.controller;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.Iglesia;
import ec.com.antenasur.domain.IglesiaPersona;
import ec.com.antenasur.domain.Persona;
import ec.com.antenasur.service.CatalogoGeneralFacade;
import ec.com.antenasur.service.GeograpFacade;
import ec.com.antenasur.service.IglesiaFacade;
import ec.com.antenasur.service.IglesiaPersonaFacade;
import ec.com.antenasur.service.PersonaFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class CargarControl implements Serializable {

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
    private CatalogoGeneralFacade catalogoFacade;

    @Inject
    private GeograpFacade geograpFacade;

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
    private String prefijoRoles;

    @Setter
    @Getter
    private Persona persona;

    @Setter
    @Getter
    private Iglesia iglesia;

    @Setter
    @Getter
    private IglesiaPersona iglesiaPersona;

    @Setter
    @Getter
    private List<Persona> listaPersonas;

    @Setter
    @Getter
    private List<Iglesia> listaIglesias;

    @Setter
    @Getter
    private List<IglesiaPersona> listaIglesiaPersona;

    @Setter
    @Getter
    private Geograp parroquiaSeleccionado, cantonSeleccionado;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Iglesia iglesiaSeleccionado;

    @PostConstruct
    private void init() {
        try {
            this.listaPersonas = new ArrayList<>();
            this.listaIglesias = new ArrayList<>();
            this.listaIglesiaPersona = new ArrayList<>();
            // prefijoRoles = (String) JsfUtil.getProperty("roles.rpm", true);
            this.parroquiaSeleccionado = cantonSeleccionado = new Geograp();
            this.iglesiaSeleccionado = new Iglesia();

            this.cantones = geograpFacade.findByFatherId(7);
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void upload() {
        if (file != null) {
            openFile(file);
            JsfUtil.addSuccessMessage(file.getFileName() + " is uploaded.");
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
            }
            JsfUtil.addSuccessMessage(event.getFile().getFileName() + " cargado");
        } catch (Exception e) {
            //LOG.error("ERROR AL REDIRIGIR LA PAGINA", e);
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

    public void openFile(UploadedFile file) {
        try {
            if (file != null && file.getContent() != null && file.getContent().length > 0 && file.getFileName() != null) {

                //Obtenemos la primera pestaña
                XSSFSheet hojaUno = excelMigracion.getSheetAt(0);
                if (hojaUno != null) {
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

    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpFacade.find(cantonSeleccionado.getId());
            parroquias = geograpFacade.findByFatherId(cantonSeleccionado.getId());

            listaIglesiaPersona = iglesiaPersonaFacade.getIglesiasPersonasPorParroquias(parroquias);
            listaIglesias = iglesiaFacade.getIglesiasPorParroquias(parroquias);
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

}
