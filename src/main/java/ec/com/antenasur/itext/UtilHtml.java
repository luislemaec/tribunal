package ec.com.antenasur.itext;

import java.util.Map;
import java.util.Set;

public class UtilHtml {

    public static String replaceWord(String text, String value, String valueReplace) {
        try {
            text = text.replaceAll(value, valueReplace);
            return text;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Procesa el html
     *
     * @param mapa lista de parametros y valores a remplazar
     * @param text html a ser procesado
     * @return html procesaso
     */
    public static String builTextHTMLToMail(Map<String, String> mapa, String text) {
        if (mapa != null) {
            Set<Map.Entry<String, String>> parameterSet = mapa.entrySet();
            for (Map.Entry<String, String> entry : parameterSet) {
                text = text.replaceAll(entry.getKey(), (!entry.getValue().isEmpty()) ? entry.getValue() : "");
            }
        }
        return text;
    }

    /**
     * Crea fila para la tabla
     *
     * @param variable nombre de variable
     * @param newValue valor nuevo
     * @param oldValue valor anterior
     * @return
     */
    public static String createRowTableHTML(String variable, String newValue, String oldValue) {
        String bodyTableHTML = "<tr>"
                + "<td style=width: 33.3333%;>" + variable + "</td>"
                + "<td style=width: 33.3333%;>" + newValue + "</td>"
                + "<td style=width: 33.3333%;>" + oldValue + "</td>"
                + "</tr>";
        return bodyTableHTML;
    }

    /**
     * Crea cabecera de la tabla para una seccion de cambios
     *
     * @param seccion titulo
     * @return
     */
    public static String createTableHtmlLogSystem(String seccion) {
        String tableHTML
                = "<table style=border-collapse: collapse; width: 100%; border=1>"
                + "<tbody>"
                + "<tr>"
                + "<td style=width: 33.3333%;><b>VARIABLE</b></td>"
                + "<td style=width: 33.3333%;><b>NUEVO</span></b></td>"
                + "<td style=width: 33.3333%;><b>ANTERIOR</b></td>"
                + "</tr>";
        return tableHTML;
    }

    public static String valorNuevoEnHTMLLogSystem(String valorNuevo, String valorAnterior) {
        String valorNuevoHTL = "";
        try {
            int contador = 0;
            String vectorNuevo[] = null;
            String vectorAnterior[] = null;
            if (!valorNuevo.isEmpty()) {
                vectorNuevo = valorNuevo.split(";");
            }
            if (!valorAnterior.isEmpty()) {
                vectorAnterior = valorAnterior.split(";");
            }

            for (String itemNuevos : vectorNuevo) {
                String itemNuvo[] = itemNuevos.split(":");
                if (vectorAnterior != null) {
                    for (String itemAnteriores : vectorAnterior) {
                        String itemAnterior[] = itemAnteriores.split(":");
                        if (!itemNuvo[0].contains("id") && !itemNuvo[0].contains("ESTADO") && !itemNuvo[0].contains("usermedia_id")
                                && !itemNuvo[0].contains("type_peop_id") && !itemNuvo[0].contains("peop_id")
                                && !itemNuvo[0].contains("publication_id") && !itemNuvo[0].contains("Historial") && !itemNuvo[0].contains("document_id")) {
                            if (itemAnterior[0].equals(itemNuvo[0])) {
                                if (!itemAnterior[1].equals(itemNuvo[1])) {
                                    if (contador == 0) {
                                        valorNuevoHTL = valorNuevoHTL + UtilHtml.createRowTableHTML(itemNuvo[0], itemNuvo[1], itemAnterior[1]);
                                    } else {
                                        valorNuevoHTL = valorNuevoHTL + UtilHtml.createRowTableHTML(itemNuvo[0], itemNuvo[1], itemAnterior[1]);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (!itemNuvo[0].contains("id") && !itemNuvo[0].contains("ESTADO") && !itemNuvo[0].contains("usermedia_id")
                            && !itemNuvo[0].contains("type_peop_id") && !itemNuvo[0].contains("peop_id")
                            && !itemNuvo[0].contains("publication_id") && !itemNuvo[0].contains("Historial") && !itemNuvo[0].contains("document_id")) {
                        if (contador == 0) {
                            valorNuevoHTL = valorNuevoHTL + UtilHtml.createRowTableHTML(itemNuvo[0], itemNuvo[1], "");
                        } else {
                            valorNuevoHTL = valorNuevoHTL + UtilHtml.createRowTableHTML(itemNuvo[0], itemNuvo[1], "");
                        }
                    }
                }
                contador++;
            }
            return valorNuevoHTL;
        } catch (Exception e) {
            return valorNuevoHTL;
        }
    }

}
