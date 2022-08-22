package ec.com.antenasur.util;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

public class MenuVO implements Serializable {

    private static final long serialVersionUID = 3004111257549476329L;

    @Setter
    @Getter
    private Integer idMenu;

    @Setter
    @Getter
    private String labelMenu;

    @Setter
    @Getter
    private String actionMenu;

    @Setter
    @Getter
    private String urlMenu;

    @Setter
    @Getter
    private Integer idMenuParent;

    @Setter
    @Getter
    private Integer idStatusUsuario;
    @Setter
    @Getter
    private Integer idUser;

    @Setter
    @Getter
    private Boolean endNode;

    @Setter
    @Getter
    private Integer order;

    @Setter
    @Getter
    private String icon;

    public MenuVO(Integer idMenu, String labelMenu, String actionMenu, String urlMenu, Integer idMenuParent,
            Integer idUser, Boolean endNode, Integer order, String icon) {
        this.idMenu = idMenu;
        this.labelMenu = labelMenu;
        this.actionMenu = actionMenu;
        this.urlMenu = urlMenu;
        this.idMenuParent = idMenuParent;
        this.idUser = idUser;
        this.endNode = endNode;
        this.order = order;
        this.icon = icon;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idMenu != null ? idMenu.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MenuVO)) {
            return false;
        }
        MenuVO other = (MenuVO) object;
        if ((this.idMenu == null && other.idMenu != null)
                || (this.idMenu != null && !this.idMenu.equals(other.idMenu))) {
            return false;
        }
        return true;
    }

}
