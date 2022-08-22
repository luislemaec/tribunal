/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.controller;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.component.export.ExcelOptions;
import org.primefaces.component.export.PDFOptions;
import org.primefaces.event.SelectEvent;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.domain.Proceso;
import ec.com.antenasur.service.ProcesoFacade;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.ReportePFD;
import ec.com.antenasur.util.ReporteXLS;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Usuario
 */
@Named(value = "procesoControl")
@ViewScoped
@Model
public class ProcesoControl implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    @Setter
    @Getter
    private LoginBean loginBean;

    @Inject
    @Setter
    @Getter
    private ProcesoBean procesoBean;

    @Inject
    ProcesoFacade procesoFacade;

    @Setter
    @Getter
    private List<Proceso> listaProceso;

    @Setter
    @Getter
    private ExcelOptions excelOpt;

    @Setter
    @Getter
    private PDFOptions pdfOpt;

    @Setter
    @Getter
    private LocalDate fechaActual, fechaInicio, fechaFin, minfecha, maxfecha;

    public ProcesoControl() {

    }

    @PostConstruct
    private void init() {
        listaProceso = procesoBean.getTodoProceso();
        maxfecha = LocalDate.now();
        fechaActual = LocalDate.now();
    }

    public void buscar() {
        procesoBean.getListaProcesoFechas(Date.valueOf(fechaInicio), Date.valueOf(fechaFin));
        listaProceso = procesoBean.getListaProceso();
        if (listaProceso != null) {
            JsfUtil.addInfoMessage(listaProceso.size() + " Procesos encontrados");
            procesoBean.registraActividad("BUSCAR PROCESO");
        } else {
            JsfUtil.addWarningMessage("No se encuentran registros ");
            procesoBean.registraActividad("ERROR BUSCAR PROCESO");
        }

    }

    public void onDateSelect(SelectEvent<LocalDate> event) {
        minfecha = fechaInicio;
    }

    public void exportaPDF() {

        ByteArrayOutputStream baos;
        String nombreReporte = "Log de procesos";
        Document document = new Document(PageSize.A4);
        baos = new ByteArrayOutputStream();
        try {

            ReportePFD.nuevoPDF(document, baos, nombreReporte);

            generaContenidoDocumento(document, "Lista " + nombreReporte);

            ReportePFD.getFinalParagraph(document, loginBean.getUsuario().getUsername());

            document.close();

            ReportePFD.descargarPDF(baos, "Lista " + nombreReporte);
            procesoBean.registraActividad("DESCARGA PDF, LISTA PROCESOS");

        } catch (Exception e) {
            e.printStackTrace();
            procesoBean.registraActividad("ERROR EN DESCARGA PDF, LISTA PROCESOS");
        }
    }

    private void generaContenidoDocumento(Document document, String titulo) {
        try {
            int numColumns = 7;
            PdfPTable table = new PdfPTable(numColumns);

            String tableTitle = titulo;

            float[] columnWidth = new float[]{20, 50, 50, 125, 125, 50, 125};
            String[] listColumnNames = new String[]{"N°.", "Usuario", "IP",  "Fecha", "Actividad"};

            table.setLockedWidth(true);

            ReportePFD.addTableToDocument(table, numColumns, columnWidth, tableTitle, listColumnNames);

            Integer contador = 1;
            for (Proceso proceso : listaProceso) {

                table.addCell(new PdfPCell(new Paragraph(contador.toString(), FontFactory.getFont("arial", 6, Font.NORMAL, BaseColor.BLACK))));
                table.addCell(new PdfPCell(new Paragraph(proceso.getUsuarioCrea(), FontFactory.getFont("arial", 6, Font.NORMAL, BaseColor.BLACK))));
                table.addCell(new PdfPCell(new Paragraph(proceso.getIp(), FontFactory.getFont("arial", 6, Font.NORMAL, BaseColor.BLACK))));
               
                table.addCell(new PdfPCell(new Paragraph(proceso.getFechaCrea() != null ? proceso.getFechaCrea().toString().substring(0, 16) : "-", FontFactory.getFont("arial", 6, Font.NORMAL, BaseColor.BLACK))));
                table.addCell(new PdfPCell(new Paragraph(proceso.getActividad() != null ? proceso.getActividad() : "", FontFactory.getFont("arial", 6, Font.NORMAL, BaseColor.BLACK))));

                contador++;
            }
            document.add(table);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String[][] getListaStringDatos(String[] listColumnNames) {
        String[][] listaDatos = new String[listaProceso.size()][listColumnNames.length];
        int fila = 0;
        for (Proceso proceso : listaProceso) {
            listaDatos[fila][0] = String.valueOf(fila + 1);
            listaDatos[fila][1] = proceso.getUsuarioCrea();
            listaDatos[fila][4] = proceso.getIp();
            listaDatos[fila][5] = proceso.getFechaCrea() != null ? proceso.getFechaCrea().toString().substring(0, 16) : "-";
            listaDatos[fila][6] = proceso.getActividad() != null ? proceso.getActividad() : "";
            fila++;
        }
        return listaDatos;
    }

    public void exportaXLS() {
        String nombreReporte = "Log de procesos";
        String[] listColumnNames = new String[]{"N°.", "Usuario", "IP", "Fecha", "Actividad"};
        int[] columnWidth = new int[]{1200, 4000, 10000, 10000, 10000, 5000, 5000};

        ReporteXLS.nuevoExcel(nombreReporte);
        ReporteXLS.creaCabeceraTabla(listColumnNames, columnWidth);

        String[][] listaDatos = getListaStringDatos(listColumnNames);

        ReporteXLS.creaContenidoTabla(listaDatos, listColumnNames);
        ReporteXLS.setFinalParagraph(loginBean.getUserName(), listaProceso.size());
        ReporteXLS.descargarExcel(nombreReporte);
        procesoBean.registraActividad("DESCARGA EXCEL, SELECCIÓN DE MEDIOS");

    }
}
