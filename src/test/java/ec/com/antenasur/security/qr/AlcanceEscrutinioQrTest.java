package ec.com.antenasur.security.qr;

import java.lang.reflect.Proxy;
import java.util.List;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ec.com.antenasur.dto.*;
import ec.com.antenasur.facade.tec.*;
import ec.com.antenasur.model.tec.*;
import ec.com.antenasur.service.tec.*;

class AlcanceEscrutinioQrTest {
    private final ContextoSesionQr contexto = new ContextoSesionQr("prueba", 1, "usuario", 2, 3, 4);

    private AlcanceEscrutinioQrInterceptor crear() throws Exception {
        var interceptor = new AlcanceEscrutinioQrInterceptor();
        inyectar(interceptor, "alcance", new AlcanceSesionQrService() {
            public ContextoSesionQr contexto() { return contexto; }
            public void validar(Integer mesa, Integer proceso) {
                if (!contexto.mesaId().equals(mesa) || !contexto.procesoId().equals(proceso)) throw new AccesoQrException();
            }
        });
        inyectar(interceptor, "documentos", new DocumentoFacade() {
            public void bloquearMesaParaVersion(Integer mesa) { }
        });
        inyectar(interceptor, "padron", new PadronFacade() {
            public long contarPadronPorMesaYProceso(Integer mesa, Integer proceso) { return 10L; }
        });
        inyectar(interceptor, "categorias", new CategoriaVotoFacade() {
            public List<CategoriaVoto> getCategoriasOrdenados(Integer proceso) {
                var categoria = new CategoriaVoto(); categoria.setId(5); return List.of(categoria);
            }
        });
        inyectar(interceptor, "escrutinios", new EscrutinioFacade() {
            public List<Escrutinio> listarPorMesaProceso(Integer mesa, Integer proceso) { return List.of(); }
        });
        return interceptor;
    }

    private static void inyectar(Object destino, String nombre, Object valor) throws Exception {
        var campo = destino.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(destino, valor);
    }

    private InvocationContext invocar(String metodo, Class<?>[] tipos, Object... valores) throws Exception {
        var referencia = EscrutinioService.class.getMethod(metodo, tipos);
        return (InvocationContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{InvocationContext.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getMethod" -> referencia;
                    case "getParameters" -> valores;
                    case "setParameters" -> null;
                    case "proceed" -> Boolean.TRUE;
                    default -> null;
                });
    }

    private EscrutinioDTO voto() {
        var dto = new EscrutinioDTO(); var mesa = new MesaDTO(); mesa.setId(4); dto.setMesa(mesa);
        dto.setProcesoId(2); dto.setCategoriaId(5); dto.setTotalVotos(10); return dto;
    }

    @Test void cierreCompletoDeSuMesaPermitido() throws Exception {
        assertEquals(Boolean.TRUE, crear().verificar(invocar("guardarActaCompletaDTO",
                new Class<?>[]{Integer.class, List.class}, 4, List.of(voto()))));
    }

    @Test void rechazaOtraMesaYProceso() throws Exception {
        var i = crear();
        assertThrows(AccesoQrException.class, () -> i.verificar(invocar("buscarCabeceraDTO",
                new Class<?>[]{Integer.class, Integer.class}, 99, 2)));
        assertThrows(AccesoQrException.class, () -> i.verificar(invocar("buscarCabeceraDTO",
                new Class<?>[]{Integer.class, Integer.class}, 4, 99)));
    }

    @Test void idDeDetalleAjenoNoPuedeReasignarse() throws Exception {
        var dto = voto(); dto.setId(999);
        assertThrows(AccesoQrException.class, () -> crear().verificar(invocar("guardarActaCompletaDTO",
                new Class<?>[]{Integer.class, List.class}, 4, List.of(dto))));
    }

    @Test void rechazaDuplicadosYConteosIncompletos() throws Exception {
        assertThrows(AccesoQrException.class, () -> crear().verificar(invocar("guardarActaCompletaDTO",
                new Class<?>[]{Integer.class, List.class}, 4, List.of(voto(), voto()))));
        var dto = voto(); dto.setTotalVotos(9);
        assertThrows(AccesoQrException.class, () -> crear().verificar(invocar("guardarActaCompletaDTO",
                new Class<?>[]{Integer.class, List.class}, 4, List.of(dto))));
    }

    @Test void lecturaGlobalDenegada() throws Exception {
        assertThrows(AccesoQrException.class, () -> crear().verificar(invocar("listarDTOsPorMesa",
                new Class<?>[]{Integer.class}, 4)));
    }
}
