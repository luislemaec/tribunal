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

}
