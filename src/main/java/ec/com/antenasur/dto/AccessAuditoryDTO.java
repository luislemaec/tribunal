package ec.com.antenasur.dto;

import java.io.Serializable;
import java.sql.Timestamp;

import ec.com.antenasur.model.AccessAuditory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de {@link AccessAuditory}: registro de acceso al sistema. Para
 * reportes de auditoría — no se edita desde la UI, solo se lista.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessAuditoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Boolean active;
    private String browser;
    private String ip;
    private Timestamp login;
    private Timestamp logout;
    private Integer numberLogin;
    private String session;
    private Boolean status;
    private String user;
    private String aplication;

    public static AccessAuditoryDTO fromEntity(AccessAuditory a) {
        if (a == null) {
            return null;
        }
        AccessAuditoryDTO dto = new AccessAuditoryDTO();
        dto.setId(a.getId());
        dto.setActive(a.getActive());
        dto.setBrowser(a.getBrowser());
        dto.setIp(a.getIp());
        dto.setLogin(a.getLogin());
        dto.setLogout(a.getLogout());
        dto.setNumberLogin(a.getNumberLogin());
        dto.setSession(a.getSession());
        dto.setStatus(a.getStatus());
        dto.setUser(a.getUser());
        dto.setAplication(a.getAplication());
        return dto;
    }
}
