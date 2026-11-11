package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;
import ec.com.antenasur.facade.tec.GestionPadronFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Padron;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.Recinto;

/** Reglas de servicio con repositorio en memoria, sin contenedor ni datos reales. */
public class GestionPadronServiceCheck {
    public static void main(String[] args) throws Exception {
        Repositorio repo = new Repositorio();
        GestionPadronService service = new GestionPadronService();
        inyectar(service, "facade", repo);
        inyectar(service, "mesaFacade", new MesaFacade() {
            @Override public Mesa buscarDetallePorId(Integer id) { return id == 20 ? repo.mesa : null; }
        });
        // Cronograma simulado: abierto salvo en el caso que comprueba el bloqueo.
        boolean[] cronogramaAbierto = {true};
        inyectar(service, "cronogramaService", new CronogramaService() {
            @Override public boolean permite(ec.com.antenasur.enums.OperacionCronograma operacion) { return cronogramaAbierto[0]; }
        });
        // Bitácora simulada: registra la actividad de cada operación del padrón.
        List<String> bitacora = new java.util.ArrayList<>();
        inyectar(service, "procesoService", new ProcesoService() {
            @Override public ec.com.antenasur.model.tec.Proceso create(ec.com.antenasur.model.tec.Proceso p) {
                bitacora.add(p.getActividad());
                return p;
            }
        });
        repo.miembros.get(3).setHabilitadoPadron(false);
        verificar(service.asignarIglesia(1, 10, 20, 30) == 3, "iglesia completa solo habilitados");
        rechaza(() -> service.asignarIglesia(1, 10, 20, 30), "iglesia sin pendientes");
        repo.miembros.get(0).setHabilitadoPadron(false);
        verificar(service.retirarIglesia(1, 10, 20, 30) == 3, "retiro incluye quien dejo de estar habilitado");
        verificar(repo.registros.stream().noneMatch(Padron::getEstado), "retiro iglesia completo");
        repo.miembros.get(0).setHabilitadoPadron(true);
        repo.registros.clear();
        service.asignar(1, 10, 20, 30, List.of(1));
        verificar(repo.registros.size() == 1, "asignacion individual");
        rechaza(() -> service.asignar(1, 10, 20, 30, List.of(1)), "duplicado");
        service.asignar(1, 10, 20, 30, List.of(2, 3));
        verificar(repo.registros.size() == 3, "asignacion multiple");
        repo.miembros.get(3).setHabilitadoPadron(false);
        rechaza(() -> service.asignar(1, 10, 20, 30, List.of(4)), "no habilitado");
        rechaza(() -> service.asignar(1, 999, 20, 30, List.of(4)), "recinto incorrecto");
        rechaza(() -> service.asignar(1, 10, 20, 999, List.of(4)), "iglesia incorrecta");
        repo.registros.get(0).setSufrago(true);
        rechaza(() -> service.retirar(1, 10, 20, List.of(1, 2)), "retiro atomico con sufrago");
        verificar(repo.registros.get(1).getEstado(), "no retiro parcial");
        repo.registros.get(0).setSufrago(false);
        repo.jrv = true;
        rechaza(() -> service.retirar(1, 10, 20, List.of(1)), "miembro JRV");
        repo.jrv = false;
        service.retirar(1, 10, 20, List.of(1));
        service.retirar(1, 10, 20, List.of(2, 3));
        verificar(repo.registros.stream().noneMatch(Padron::getEstado), "baja logica multiple");
        verificar(repo.miembros.stream().allMatch(IglesiaPersona::getEstado), "vinculos intactos");
        service.asignar(1, 10, 20, 30, List.of(1, 2, 3));
        verificar(repo.registros.size() == 3 && repo.registros.stream().allMatch(Padron::getEstado), "reutiliza registros");
        repo.miembros.get(3).setHabilitadoPadron(true);
        repo.miembros.get(3).setPersona(repo.miembros.get(0).getPersona());
        rechaza(() -> service.asignar(1, 10, 20, 30, List.of(4)), "persona con otro vinculo");
        repo.otraMesa = true;
        rechaza(() -> service.asignar(1, 10, 20, 30, List.of(4)), "iglesia en otra mesa");
        repo.otraMesa = false;
        cronogramaAbierto[0] = false;
        rechaza(() -> service.retirar(1, 10, 20, List.of(1)), "cronograma cerrado");
        cronogramaAbierto[0] = true;
        repo.proceso.setActivo(false);
        rechaza(() -> service.retirar(1, 10, 20, List.of(1)), "proceso inactivo");
        // Bitácora: una entrada por operación exitosa (ninguna por las rechazadas), con su resumen.
        verificar(bitacora.stream().anyMatch(a -> a.startsWith("ASIGNA_IGLESIA_PADRON | MÓDULO: PADRÓN; RESULTADO: EXITOSO")
                && a.endsWith("Electores: 3")), "bitacora asignar iglesia");
        verificar(bitacora.stream().anyMatch(a -> a.startsWith("RETIRA_IGLESIA_PADRON |") && a.endsWith("Electores: 3")),
                "bitacora retirar iglesia");
        verificar(bitacora.stream().anyMatch(a -> a.startsWith("ASIGNA_PADRON |")), "bitacora asignar seleccion");
        verificar(bitacora.stream().anyMatch(a -> a.startsWith("RETIRA_PADRON |")), "bitacora retirar seleccion");
        // 7 exitosas: asignar y retirar iglesia, asignar(1), asignar(2,3), retirar(1), retirar(2,3), asignar(1,2,3).
        verificar(bitacora.size() == 7, "una entrada por operacion exitosa: " + bitacora.size());
        verificar(bitacora.stream().allMatch(a -> a.length() <= 255), "bitacora dentro de 255");
        System.out.println("OK: asignacion/retiro individual y multiple, baja/reactivacion, duplicados, habilitacion, JRV, contexto y bitacora.");
    }

    private static void inyectar(Object bean, String campo, Object valor) throws Exception {
        var f = bean.getClass().getDeclaredField(campo); f.setAccessible(true); f.set(bean, valor);
    }
    private static void verificar(boolean valor, String caso) { if (!valor) throw new AssertionError(caso); }
    private static void rechaza(Runnable accion, String caso) {
        try { accion.run(); throw new AssertionError(caso); } catch (NegocioException esperado) { }
    }
    private static class Repositorio extends GestionPadronFacade {
        final ProcesoElectoral proceso = new ProcesoElectoral();
        final Mesa mesa = new Mesa();
        final List<IglesiaPersona> miembros = new ArrayList<>();
        final List<Padron> registros = new ArrayList<>();
        boolean jrv, otraMesa;
        Repositorio() {
            proceso.setId(1); proceso.setActivo(true);
            Geograp parroquia = new Geograp(); parroquia.setId(40);
            Recinto recinto = new Recinto(); recinto.setId(10); recinto.setUbicacion(parroquia);
            mesa.setId(20); mesa.setRecinto(recinto);
            Iglesia iglesia = new Iglesia(); iglesia.setId(30); iglesia.setUbicacion(parroquia);
            for (int i = 1; i <= 4; i++) {
                Persona persona = new Persona(); persona.setId(i);
                IglesiaPersona ip = new IglesiaPersona(iglesia, persona); ip.setId(i); ip.setHabilitadoPadron(true); miembros.add(ip);
            }
        }
        @Override public ProcesoElectoral bloquearProceso(Integer id) { return id == 1 ? proceso : null; }
        @Override public List<Integer> habilitadosPendientes(Integer iglesia, Integer mesa, Integer proceso) {
            return miembros.stream().filter(ip -> ip.getHabilitadoPadron() && ip.getIglesia().getId().equals(iglesia))
                    .filter(ip -> registros.stream().noneMatch(p -> p.getEstado()
                            && p.getIglesiaPersona().getPersona().getId().equals(ip.getPersona().getId())))
                    .map(IglesiaPersona::getId).toList();
        }
        @Override public List<Integer> empadronadosIglesia(Integer iglesia, Integer mesa, Integer proceso) {
            return registros.stream().filter(p -> p.getEstado() && p.getIglesiaPersona().getIglesia().getId().equals(iglesia))
                    .map(Padron::getId).toList();
        }
        @Override public boolean escrutinioIniciado(Integer m, Integer p) { return false; }
        @Override public List<IglesiaPersona> miembros(List<Integer> ids) { return miembros.stream().filter(i -> ids.contains(i.getId())).toList(); }
        @Override public List<Padron> existentes(Integer p, List<Integer> personas) { return registros.stream().filter(r -> personas.contains(r.getIglesiaPersona().getPersona().getId())).toList(); }
        @Override public boolean iglesiaEnOtraMesa(Integer i, Integer m, Integer p) { return otraMesa; }
        @Override public List<Padron> seleccion(List<Integer> ids, Integer m, Integer p) { return registros.stream().filter(r -> ids.contains(r.getId()) && r.getEstado()).toList(); }
        @Override public boolean tieneJrv(List<Integer> ids, Integer p) { return jrv; }
        @Override public void guardar(List<Padron> lote) {
            for (Padron p : lote) if (p.getId() == null) { p.setId(registros.size() + 1); registros.add(p); }
        }
    }
}
