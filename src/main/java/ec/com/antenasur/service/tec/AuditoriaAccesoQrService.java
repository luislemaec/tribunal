package ec.com.antenasur.service.tec;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import ec.com.antenasur.facade.tec.AccesoQrActaFacade;
import ec.com.antenasur.model.tec.AccesoQrActa;
import ec.com.antenasur.security.qr.ResultadoAccesoQr;

@Stateless
public class AuditoriaAccesoQrService {
    @Inject private AccesoQrActaFacade facade;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void rechazo(AccesoQrActa qr, String ip, String agente, ResultadoAccesoQr resultado) {
        facade.auditar(qr, ip, agente, resultado.name());
    }
}
