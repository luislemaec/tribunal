package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CategoriaVotoDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.CategoriaVotoFacade;
import ec.com.antenasur.facade.tec.ListaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.util.Constantes;

@Stateless
public class CategoriaVotoService extends AbstractService<CategoriaVoto, Integer, CategoriaVotoFacade> {

    public static final String TIPO_LISTA = "LISTA";
    public static final String TIPO_ESPECIAL = "ESPECIAL";
    public static final String TIPO_LEGACY = "LEGACY";

    @Inject
    private CategoriaVotoFacade categoriaVotoFacade;

    @Inject
    private ListaFacade listaFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Override
    protected CategoriaVotoFacade getFacade() {
        return categoriaVotoFacade;
    }

    public List<CategoriaVoto> getCategoriasOrdenados() {
        return categoriaVotoFacade.getCategoriasOrdenados();
    }

    public List<CategoriaVoto> getCategoriasOrdenados(Integer procesoId) {
        return categoriaVotoFacade.getCategoriasOrdenados(procesoId);
    }

    public CategoriaVotoDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return CategoriaVotoDTO.fromEntity(categoriaVotoFacade.find(id));
    }

    public List<CategoriaVotoDTO> listarDTOsOrdenados() {
        return listarDTOsOrdenados(null);
    }

    public List<CategoriaVotoDTO> listarDTOsOrdenados(Integer procesoId) {
        List<CategoriaVotoDTO> resultado = new ArrayList<>();
        List<CategoriaVoto> cs = categoriaVotoFacade.getCategoriasOrdenados(procesoId);
        if (cs == null) return resultado;
        for (CategoriaVoto c : cs) resultado.add(CategoriaVotoDTO.fromEntity(c));
        return resultado;
    }

    /** Crea, actualiza o reactiva una única categoría para lista y proceso. */
    public CategoriaVoto sincronizarLista(Lista lista, Integer procesoId) {
        if (lista == null || lista.getId() == null || procesoId == null) {
            throw new IllegalStateException("No se pudo determinar la lista o el proceso electoral.");
        }
        ProcesoElectoral proceso = procesoElectoralFacade.find(procesoId);
        if (proceso == null) {
            throw new IllegalStateException("El proceso electoral seleccionado no existe.");
        }
        List<CategoriaVoto> existentes = categoriaVotoFacade
                .buscarPorListaProcesoIncluyendoInactivas(lista.getId(), procesoId);
        CategoriaVoto categoria = existentes.isEmpty() ? null : existentes.get(0);

        for (int i = 1; i < existentes.size(); i++) {
            CategoriaVoto duplicada = existentes.get(i);
            if (Boolean.TRUE.equals(duplicada.getEstado())) {
                duplicada.setEstado(false);
                categoriaVotoFacade.edit(duplicada);
            }
        }
        CategoriaVoto persistida;
        if (categoria == null) {
            categoria = new CategoriaVoto();
            categoria.setLista(lista);
            categoria.setProceso(proceso);
            actualizarDatos(categoria, lista);
            categoria.setEstado(Boolean.TRUE.equals(lista.getEstado()));
            persistida = categoriaVotoFacade.create(categoria);
        } else {
            actualizarDatos(categoria, lista);
            categoria.setLista(lista);
            categoria.setProceso(proceso);
            categoria.setEstado(Boolean.TRUE.equals(lista.getEstado()));
            persistida = categoriaVotoFacade.edit(categoria);
        }
        long activas = categoriaVotoFacade.contarActivasPorListaProceso(lista.getId(), procesoId);
        long esperado = Boolean.TRUE.equals(lista.getEstado()) ? 1L : 0L;
        if (activas != esperado) {
            throw new IllegalStateException("La categoría de voto no cumple la unicidad Lista-Proceso.");
        }
        return persistida;
    }

    /** Propaga nombre y número a todas las categorías históricas de la lista. */
    public void actualizarCategoriasLista(Lista lista) {
        if (lista == null || lista.getId() == null) {
            return;
        }
        for (CategoriaVoto categoria : categoriaVotoFacade
                .buscarPorListaIncluyendoInactivas(lista.getId())) {
            actualizarDatos(categoria, lista);
            categoriaVotoFacade.edit(categoria);
        }
    }

    public void desactivarCategoriasLista(Integer listaId) {
        for (CategoriaVoto categoria : categoriaVotoFacade.buscarPorListaIncluyendoInactivas(listaId)) {
            if (Boolean.TRUE.equals(categoria.getEstado())) {
                categoria.setEstado(false);
                categoriaVotoFacade.edit(categoria);
            }
        }
        if (categoriaVotoFacade.contarActivasPorLista(listaId) != 0L) {
            throw new IllegalStateException("Persisten categorías de voto activas para la lista dada de baja.");
        }
    }

    /** Garantiza categorías al activar un proceso electoral nuevo. */
    public void sincronizarListasVigentes(ProcesoElectoral proceso) {
        if (proceso == null || proceso.getId() == null) {
            return;
        }
        List<Lista> listas = listaFacade.findAll();
        if (listas != null) {
            for (Lista lista : listas) {
                sincronizarLista(lista, proceso.getId());
            }
        }
    }

    private void actualizarDatos(CategoriaVoto categoria, Lista lista) {
        categoria.setNombre(nombreCategoria(lista));
        categoria.setCategoriaVoto(lista.getId());
        categoria.setOrden(ordenLista(lista));
        categoria.setTipo(TIPO_LISTA);
    }

    private String nombreCategoria(Lista lista) {
        String numero = lista.getNumero() != null ? lista.getNumero().trim() : "";
        String nombre = lista.getNombre() != null ? lista.getNombre().trim() : "";
        return ("LISTA " + numero + (nombre.isBlank() ? "" : " - " + nombre)).trim();
    }

    private int ordenLista(Lista lista) {
        try {
            return Integer.parseInt(lista.getNumero().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 1000 + lista.getId();
        }
    }

    public CategoriaVotoDTO guardarDesdeDTO(CategoriaVotoDTO dto) {
        if (dto == null) return null;
        if (dto.getId() == null) {
            CategoriaVoto nueva = dto.toEntity();
            if (nueva.getTipo() == null || nueva.getTipo().isBlank()) {
                nueva.setTipo(TIPO_LEGACY);
            }
            return CategoriaVotoDTO.fromEntity(categoriaVotoFacade.create(nueva));
        }
        CategoriaVoto actual = categoriaVotoFacade.find(dto.getId());
        if (actual == null) return null;
        validarCategoriaNoGestionadaPorLista(actual);
        actual.setNombre(dto.getNombre());
        actual.setCategoriaVoto(dto.getCategoriaVoto());
        actual.setOrden(dto.getOrden());
        actual.setTipo(dto.getTipo() == null || dto.getTipo().isBlank()
                ? TIPO_LEGACY : dto.getTipo());
        return CategoriaVotoDTO.fromEntity(categoriaVotoFacade.edit(actual));
    }

    public CategoriaVotoDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        CategoriaVoto c = categoriaVotoFacade.find(id);
        if (c == null) return null;
        validarCategoriaNoGestionadaPorLista(c);
        return CategoriaVotoDTO.fromEntity(categoriaVotoFacade.delete(c));
    }

    private void validarCategoriaNoGestionadaPorLista(CategoriaVoto categoria) {
        if (categoria.getLista() != null) {
            throw new NegocioException(Constantes.getMensaje(
                    "form.candidatos.lista.error.categoria.directa"));
        }
    }
}
