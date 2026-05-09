package ec.com.antenasur.audit;

import ec.com.antenasur.model.generic.EntidadAuditable;
import java.io.Serializable;
import java.util.Date;
import javax.naming.Name;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
