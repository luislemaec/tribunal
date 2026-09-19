package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.dto.PersonaHistorialDTO;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.util.Constantes;

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
	 * {@code create}, si no, hidrata la entidad existente con los campos del DTO y
	 * hace {@code edit} (preservando relaciones y campos de auditoría que el DTO no
	 * expone).
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

	// ----- Historial de auditoría (Envers) -----

	/** Índices de las columnas devueltas por {@code listarRevisionesAuditoria}. */
	private static final int REV = 0;
	private static final int REVTYPE = 1;
	private static final int FECHA = 2;
	private static final int NOMBRES = 3;
	private static final int APELLIDOS = 4;
	private static final int DOCUMENTO = 5;
	private static final int TRATAMIENTO = 6;
	private static final int SEXO = 7;
	private static final int ESTADO = 8;
	private static final int USUARIO = 9;

	/** Campos auditados que se comparan entre revisiones, con su clave de UI. */
	private static final int[] CAMPOS_INDICE = { NOMBRES, APELLIDOS, DOCUMENTO, TRATAMIENTO, SEXO, ESTADO };
	private static final String[] CAMPOS_CLAVE = { PersonaHistorialDTO.CAMPO_NOMBRES,
			PersonaHistorialDTO.CAMPO_APELLIDOS, PersonaHistorialDTO.CAMPO_DOCUMENTO,
			PersonaHistorialDTO.CAMPO_TRATAMIENTO, PersonaHistorialDTO.CAMPO_SEXO,
			PersonaHistorialDTO.CAMPO_ESTADO };

	/**
	 * Historial de cambios de una persona a partir de las revisiones de Envers.
	 *
	 * <p>Mismo formato que el historial de iglesias: cada fila del historial es una
	 * revisión con el valor que cada campo auditado tenía en ese momento; los
	 * campos que difieren de la revisión anterior se marcan para que la vista los
	 * resalte y muestre el valor previo. Las revisiones se devuelven de la más
	 * reciente a la más antigua.
	 *
	 * <p>Las personas cargadas por la migración de datos no tienen revisión de
	 * alta: su primera revisión no tiene contra qué compararse y se presenta como
	 * registro auditado con los valores vigentes en ese momento.
	 */
	public List<PersonaHistorialDTO> obtenerHistorial(Integer personaId) {
		List<Object[]> filas = personaFacade.listarRevisionesAuditoria(personaId);
		List<PersonaHistorialDTO> resultado = new ArrayList<>(filas.size());
		Object[] previa = null;
		for (Object[] fila : filas) {
			int tipo = fila[REVTYPE] == null ? 1 : ((Number) fila[REVTYPE]).intValue();
			PersonaHistorialDTO h = new PersonaHistorialDTO();
			h.setRevision(fila[REV] == null ? null : ((Number) fila[REV]).intValue());
			h.setFecha((Date) fila[FECHA]);
			h.setUsuario(texto(fila[USUARIO]));
			asignarValores(h, fila);
			if (tipo == 0) {
				h.setAccion(PersonaHistorialDTO.ACCION_CREA);
			} else if (tipo == 2) {
				h.setAccion(PersonaHistorialDTO.ACCION_ELIMINA);
			} else {
				boolean cambioEstado = previa != null && !Objects.equals(previa[ESTADO], fila[ESTADO]);
				if (cambioEstado) {
					h.setAccion(Boolean.FALSE.equals(fila[ESTADO]) ? PersonaHistorialDTO.ACCION_DESACTIVA
							: PersonaHistorialDTO.ACCION_REACTIVA);
				} else if (previa == null) {
					// Primera revisión de un registro cargado por migración: no hay
					// revisión previa contra la cual comparar.
					h.setAccion(PersonaHistorialDTO.ACCION_BASE);
				} else {
					h.setAccion(PersonaHistorialDTO.ACCION_ACTUALIZA);
				}
				marcarCambios(h, previa, fila);
			}
			resultado.add(h);
			previa = fila;
		}
		Collections.reverse(resultado);
		return resultado;
	}

	private static void asignarValores(PersonaHistorialDTO h, Object[] fila) {
		h.setNombres(texto(fila[NOMBRES]));
		h.setApellidos(texto(fila[APELLIDOS]));
		h.setDocumento(texto(fila[DOCUMENTO]));
		h.setTratamiento(texto(fila[TRATAMIENTO]));
		h.setSexo(texto(fila[SEXO]));
		h.setEstado(etiquetaEstado(fila[ESTADO]));
	}

	/** Campos cuyo valor difiere entre la revisión anterior y la actual. */
	private static void marcarCambios(PersonaHistorialDTO h, Object[] previa, Object[] fila) {
		if (previa == null) {
			return;
		}
		for (int i = 0; i < CAMPOS_INDICE.length; i++) {
			int indice = CAMPOS_INDICE[i];
			if (Objects.equals(previa[indice], fila[indice])) {
				continue;
			}
			String campo = CAMPOS_CLAVE[i];
			h.getCambiados().add(campo);
			// El estado se guarda como etiqueta, igual que el valor de la revisión.
			asignarAnterior(h, campo,
					indice == ESTADO ? etiquetaEstado(previa[indice]) : texto(previa[indice]));
		}
	}

	private static void asignarAnterior(PersonaHistorialDTO h, String campo, String anterior) {
		switch (campo) {
		case PersonaHistorialDTO.CAMPO_NOMBRES -> h.setNombresAnterior(anterior);
		case PersonaHistorialDTO.CAMPO_APELLIDOS -> h.setApellidosAnterior(anterior);
		case PersonaHistorialDTO.CAMPO_DOCUMENTO -> h.setDocumentoAnterior(anterior);
		case PersonaHistorialDTO.CAMPO_TRATAMIENTO -> h.setTratamientoAnterior(anterior);
		case PersonaHistorialDTO.CAMPO_SEXO -> h.setSexoAnterior(anterior);
		default -> h.setEstadoAnterior(anterior);
		}
	}

	private static String etiquetaEstado(Object valor) {
		if (valor == null) {
			return null;
		}
		return Boolean.TRUE.equals(valor) ? Constantes.getMensaje("form.personas.hist.estado.activo")
				: Constantes.getMensaje("form.personas.hist.estado.inactivo");
	}

	private static String texto(Object valor) {
		return valor == null ? null : valor.toString();
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
