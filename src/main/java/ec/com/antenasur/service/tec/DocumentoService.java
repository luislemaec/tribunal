package ec.com.antenasur.service.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.ArrayList;

import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.facade.tec.RecintoFacade;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class DocumentoService extends AbstractService<Documentos, Integer, DocumentoFacade> {

    @Inject
    private DocumentoFacade documentoFacade;

    @Inject private MesaFacade mesaFacade;
    @Inject private ProcesoElectoralFacade procesoElectoralFacade;
    @Inject private RecintoFacade recintoFacade;

    @Override
    protected DocumentoFacade getFacade() {
        return documentoFacade;
    }

    public List<Documentos> getDocumentosPorMesa(Mesa mesa) {
        return documentoFacade.getDocumentosPorMesa(mesa);
    }

    public List<Documentos> getDocumentosPorEntidadYTipoDoc(Integer entidadId, Integer tipoDocId) {
        return documentoFacade.getDocumentosPorEntidadYTipoDoc(entidadId, tipoDocId);
    }

    public Boolean getTieneDocumentosPorEntidadYTipoDoc(Integer entidadId, Integer tipoDocId) {
        return documentoFacade.getTieneDocumentosPorEntidadYTipoDoc(entidadId, tipoDocId);
    }

    public long contarDocumentosPorEntidadYTipoDoc(Integer entidadId, Integer tipoDocId) {
        return documentoFacade.contarDocumentosPorEntidadYTipoDoc(entidadId, tipoDocId);
    }

    public Documentos obtenerDocumentoPorWorkspace(String workspace) {
        return documentoFacade.obtenerDocumentoPorWorkspace(workspace);
    }

    public DocumentoDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return DocumentoDTO.fromEntity(documentoFacade.find(id));
    }

    public List<DocumentoDTO> listarDTOsPorEntidadYTipo(Integer entidadId, Integer tipoDocId) {
        return mapearLista(documentoFacade.getDocumentosPorEntidadYTipoDoc(entidadId, tipoDocId));
    }

    public List<DocumentoDTO> listarDTOsPorMesaProceso(Integer mesaId, Integer procesoId) {
        return mapearLista(documentoFacade.listarPorMesaProceso(mesaId, procesoId));
    }

    public Documentos registrarDocumentoMesa(Documentos documento, Integer mesaId,
            Integer procesoId, Integer recintoId) {
        if (documento == null || mesaId == null || procesoId == null) {
            return null;
        }
        documento.setMesa(mesaFacade.find(mesaId));
        documento.setProceso(procesoElectoralFacade.find(procesoId));
        documento.setRecinto(recintoId != null ? recintoFacade.find(recintoId) : null);
        return documentoFacade.create(documento);
    }

    public Documentos buscarActivoPorEntidadTipoYContexto(
            Integer entidadId, Integer tipoDocumentoId, String contextoHash) {
        return documentoFacade.buscarActivoPorEntidadTipoYContexto(entidadId, tipoDocumentoId, contextoHash);
    }

    public Documentos buscarFirmadoActivoPorOrigen(Integer documentoOrigenId, Integer tipoDocumentoId) {
        return documentoFacade.buscarFirmadoActivoPorOrigen(documentoOrigenId, tipoDocumentoId);
    }

    private List<DocumentoDTO> mapearLista(List<Documentos> entidades) {
        List<DocumentoDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (Documentos d : entidades) resultado.add(DocumentoDTO.fromEntity(d));
        return resultado;
    }
}
