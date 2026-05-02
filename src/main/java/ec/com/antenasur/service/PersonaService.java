package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.model.Persona;

@Stateless
public class PersonaService extends AbstractService<Persona, Integer, PersonaFacade> {

    @Inject
    private PersonaFacade personaFacade;

    @Override
    protected PersonaFacade getFacade() {
        return personaFacade;
    }

    public List<Persona> getByRuc() {
        return personaFacade.getByRuc();
    }

    public Persona finByPersonaDocument(String documento) {
        return personaFacade.finByPersonaDocument(documento);
    }

    public Persona searchPersonaByUserId(Integer user_id) {
        return personaFacade.searchPersonaByUserId(user_id);
    }

    public Persona buscarPorCedula(String documento) {
        return personaFacade.buscarPorCedula(documento);
    }

    // ----- API basada en DTO -----

    /** Devuelve la persona como DTO, o null si no existe. */
    public PersonaDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return PersonaDTO.fromEntity(personaFacade.find(id));
    }

    /** Busca por documento (cédula/RUC) y retorna DTO; null si no existe. */
    public PersonaDTO buscarDTOPorDocumento(String documento) {
        if (documento == null || documento.isEmpty()) {
            return null;
        }
        return PersonaDTO.fromEntity(personaFacade.buscarPorCedula(documento));
    }

    /** Lista todas las personas activas como DTOs. */
    public List<PersonaDTO> listarDTOs() {
        return mapearLista(personaFacade.findAll());
    }

    /**
     * Persiste la persona descrita por el DTO. Si el id es null hace
     * {@code create}, si no, hidrata la entidad existente con los campos del
     * DTO y hace {@code edit} (preservando relaciones y campos de auditoría
     * que el DTO no expone).
     */
    public PersonaDTO guardarDesdeDTO(PersonaDTO dto) {
        if (dto == null) {
            return null;
        }
        if (dto.getId() == null) {
            return PersonaDTO.fromEntity(personaFacade.create(dto.toEntity()));
        }
        Persona actual = personaFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        actual.setNombres(dto.getNombres());
        actual.setApellidos(dto.getApellidos());
        actual.setDocumento(dto.getDocumento());
        actual.setTratamiento(dto.getTratamiento());
        actual.setSexo(dto.getSexo());
        return PersonaDTO.fromEntity(personaFacade.edit(actual));
    }

    /** Soft-delete por id. Retorna el DTO post-borrado o null si no existía. */
    public PersonaDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Persona p = personaFacade.find(id);
        if (p == null) {
            return null;
        }
        return PersonaDTO.fromEntity(personaFacade.delete(p));
    }

    private List<PersonaDTO> mapearLista(List<Persona> personas) {
        List<PersonaDTO> resultado = new ArrayList<>();
        if (personas == null) {
            return resultado;
        }
        for (Persona p : personas) {
            resultado.add(PersonaDTO.fromEntity(p));
        }
        return resultado;
    }
}
