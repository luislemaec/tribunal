package ec.com.antenasur.audit;

import ec.com.antenasur.domain.generic.EntidadAuditable;
import java.io.Serializable;
import java.util.Date;
import javax.naming.Name;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import lombok.Data;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 *
 * @author Admindba
 */
@Entity
@Table(name = "tec_auditoria", schema = "tec")
@RevisionEntity(CustomRevisionListener.class)
@Data

@AttributeOverrides({
    @AttributeOverride(name = "status", column = @Column(name = "data_aud_status")),
    @AttributeOverride(name = "createDate", column = @Column(name = "create_date")),
    @AttributeOverride(name = "updateDate", column = @Column(name = "update_date")),
    @AttributeOverride(name = "createUser", column = @Column(name = "create_user")),
    @AttributeOverride(name = "updateUser", column = @Column(name = "update_user"))})
public class Auditoria extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id    
    @GeneratedValue(strategy = GenerationType.IDENTITY)    
    @Column(name = "aud_id")
    @RevisionNumber
    private Integer id;

    @Column(name = "audit_date")
    @RevisionTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

}
