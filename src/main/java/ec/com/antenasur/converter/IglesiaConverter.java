package ec.com.antenasur.converter;

import ec.com.antenasur.model.Iglesia;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import org.primefaces.component.picklist.PickList;
import org.primefaces.model.DualListModel;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@FacesConverter(forClass = Iglesia.class, value = "IglesiaConverter")
public class IglesiaConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        Object ret = null;

        if (component instanceof PickList) {
            Object dualList = ((PickList) component).getValue();
            DualListModel<?> dl = (DualListModel<?>) dualList;
            for (Object o : dl.getSource()) {
                String id = "" + ((Iglesia) o).getId();
                if (value.equals(id)) {
                    ret = o;
                    break;
                }
            }
            if (ret == null) {
                for (Object o : dl.getTarget()) {
                    String id = "" + ((Iglesia) o).getId();
                    if (value.equals(id)) {
                        ret = o;
                        break;
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        String str = "";
        if (value instanceof Iglesia) {
            str = "" + ((Iglesia) value).getId();
        }
        return str;
    }

}
