package ec.com.antenasur.service.tec;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.ArrayList;

import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class DocumentoService extends AbstractService<Documentos, Integer, DocumentoFacade> {

    @Inject
    private DocumentoFacade documentoFacade;

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

    private List<DocumentoDTO> mapearLista(List<Documentos> entidades) {
        List<DocumentoDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (Documentos d : entidades) resultado.add(DocumentoDTO.fromEntity(d));
        return resultado;
    }
}
