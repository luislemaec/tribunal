
package ec.com.antenasur.audit;

import org.hibernate.envers.RevisionListener;

/**
 *
 * @author Luis Lema
 */
public class CustomRevisionListener implements RevisionListener {

    public void newRevision(Object revisionEntity) {
        final Auditoria audit=(Auditoria) revisionEntity;
    }

}
