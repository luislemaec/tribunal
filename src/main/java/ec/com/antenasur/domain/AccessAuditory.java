package ec.com.antenasur.domain;

import ec.com.antenasur.domain.generic.EntidadBase;
import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import lombok.Data;
import org.hibernate.annotations.Filter;

/**
 * The persistent class for the tb_access_auditory database table.
 *
 */
@Data
@Entity
@Table(name = "tb_access_auditory", schema = "public")
@NamedQuery(name = "AccessAuditory.findAll", query = "SELECT a FROM AccessAuditory a")

@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "access_status = 'TRUE'")
public class AccessAuditory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Integer id;

    @Column(name = "access_active")
    private Boolean active;

    @Column(name = "access_browser")
    private String browser;

    @Column(name = "access_ip")
    private String ip;

    @Column(name = "access_login")
    private Timestamp login;

    @Column(name = "access_logout")
    private Timestamp logout;

    @Column(name = "access_number_login")
    private Integer numberLogin;

    @Column(name = "access_session")
    private String session;

    @Column(name = "access_status")
    private Boolean status;

    @Column(name = "access_user")
    private String user;

    @Column(name = "access_aplication")
    private String aplication;

    public AccessAuditory() {
    }

    public AccessAuditory(String userAcces, Timestamp dateAcces, String ipAcces) {
        user = userAcces;
        login = dateAcces;
        ip = ipAcces;
        status = false;
        active = false;
        aplication = "seleccion_medios";
    }

}
