package ec.com.antenasur.security.qr;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.facade.tec.CategoriaVotoFacade;
import ec.com.antenasur.facade.tec.EscrutinioFacade;
import ec.com.antenasur.facade.tec.PadronFacade;
import ec.com.antenasur.model.tec.Escrutinio;
import ec.com.antenasur.service.tec.AlcanceSesionQrService;

/** Denegacion por defecto, incluidos los CRUD heredados del servicio. */
public class AlcanceEscrutinioQrInterceptor {
    @Inject private AlcanceSesionQrService alcance;
    @Inject private EscrutinioFacade escrutinios;
    @Inject private CategoriaVotoFacade categorias;
    @Inject private PadronFacade padron;
    @Inject private ec.com.antenasur.facade.tec.DocumentoFacade documentos;

    @AroundInvoke
    public Object verificar(InvocationContext invocation) throws Exception {
        var contexto = alcance.contexto();
        if (contexto == null) return invocation.proceed();
        String metodo = invocation.getMethod().getName();
        // Calculo puro utilizado por la vista; no lee ni escribe registros.
        if ("calcularTotalVotos".equals(metodo)) return invocation.proceed();
        Object[] args = invocation.getParameters();
        if (!Set.of("prepararActaPorMesaDTO", "obtenerOCrearCabeceraDTO", "abrirEscrutinioDTO",
                "guardarBorradorConteoDTO", "guardarActaCompletaDTO", "buscarCabeceraDTO",
                "listarDTOsPorMesaYProceso").contains(metodo)) throw new AccesoQrException();
        if ("guardarBorradorConteoDTO".equals(metodo) && args.length != 4) throw new AccesoQrException();
        boolean cierre = "guardarActaCompletaDTO".equals(metodo);
        if (!contexto.mesaId().equals(args[0])) throw new AccesoQrException();
        documentos.bloquearMesaParaVersion(contexto.mesaId());
        alcance.validar((Integer) args[0], cierre ? contexto.procesoId() : (Integer) args[1]);
        if ("abrirEscrutinioDTO".equals(metodo)) args[2] = contexto.username();
        if (Set.of("obtenerOCrearCabeceraDTO", "abrirEscrutinioDTO", "guardarBorradorConteoDTO").contains(metodo))
            args[args.length - 1] = Math.toIntExact(padron.contarPadronPorMesaYProceso(contexto.mesaId(), contexto.procesoId()));
        if ("prepararActaPorMesaDTO".equals(metodo)) {
            Set<Integer> permitidas = categorias.getCategoriasOrdenados(contexto.procesoId()).stream()
                    .map(c -> c.getId()).collect(Collectors.toSet());
            if (!(args[2] instanceof List<?> ids) || ids.isEmpty() || !permitidas.containsAll(ids))
                throw new AccesoQrException();
        }
        if (cierre || "guardarBorradorConteoDTO".equals(metodo)) {
            if (!(args[cierre ? 1 : 2] instanceof List<?> items) || items.isEmpty()) throw new AccesoQrException();
            Set<Integer> permitidas = categorias.getCategoriasOrdenados(contexto.procesoId()).stream()
                    .map(c -> c.getId()).collect(Collectors.toSet());
            Map<Integer, Escrutinio> existentes = escrutinios.listarPorMesaProceso(contexto.mesaId(), contexto.procesoId())
                    .stream().collect(Collectors.toMap(Escrutinio::getId, e -> e));
            Set<Integer> recibidas = new HashSet<>();
            long total = 0;
            for (Object valor : items) {
                if (!(valor instanceof EscrutinioDTO dto) || dto.getMesa() == null
                        || !contexto.mesaId().equals(dto.getMesa().getId())
                        || !contexto.procesoId().equals(dto.getProcesoId())
                        || !permitidas.contains(dto.getCategoriaId()) || !recibidas.add(dto.getCategoriaId())
                        || (dto.getTotalVotos() != null && dto.getTotalVotos() < 0)
                        || (cierre && dto.getTotalVotos() == null)) throw new AccesoQrException();
                total += dto.getTotalVotos() == null ? 0 : dto.getTotalVotos();
                if (dto.getId() != null) {
                    Escrutinio existente = existentes.get(dto.getId());
                    if (existente == null || !dto.getCategoriaId().equals(existente.getCategoria().getId()))
                        throw new AccesoQrException();
                } else if (existentes.values().stream().anyMatch(e -> dto.getCategoriaId().equals(e.getCategoria().getId()))) {
                    throw new AccesoQrException();
                }
            }
            if (cierre && !recibidas.equals(permitidas)) throw new AccesoQrException();
            if (cierre && total != padron.contarPadronPorMesaYProceso(contexto.mesaId(), contexto.procesoId()))
                throw new AccesoQrException();
        }
        invocation.setParameters(args);
        return invocation.proceed();
    }
}
