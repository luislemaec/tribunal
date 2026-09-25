package ec.com.antenasur.controller;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.tec.MesaService;
import jakarta.faces.component.UIOutput;
import java.util.List;
import org.primefaces.event.SelectEvent;

/** Prueba focalizada del estado de vista, sin persistencia ni contenedor. */
public class RecintosSeleccionCheck {
    public static void main(String[] args) throws Exception {
        MesaController mesas = new MesaController();
        inyectar(mesas, "mesaService", new MesaService() {
            @Override
            public List<MesaDTO> listarDTOsPorRecintos(List<Recinto> recintos) {
                if (recintos.get(0).getId() == 2) return List.of();
                MesaDTO mesa = new MesaDTO();
                mesa.setId(recintos.get(0).getId());
                return List.of(mesa);
            }
        });
        RecintoDTO primero = recinto(1, 10, 100);
        mesas.seleccionarRecintoDesdeTabla(new SelectEvent<>(new UIOutput(), null, primero));
        comprobar(mesas.getPestanaActiva() == 1 && mesas.getListaMesas().size() == 1, "Seleccion y tab Mesas");
        mesas.getListaMesas().sort((a, b) -> a.getId().compareTo(b.getId()));
        mesas.setMesaSeleccionado(mesas.getListaMesas().get(0));
        mesas.setListaMesasSeleccionados(List.copyOf(mesas.getListaMesas()));
        mesas.seleccionarRecintoDesdeTabla(new SelectEvent<>(new UIOutput(), null, recinto(2, 20, 200)));
        comprobar(mesas.getListaMesas().isEmpty() && mesas.getMesaSeleccionado() == null
                && mesas.getListaMesasSeleccionados().isEmpty(), "Cambio limpia contexto anterior");
        mesas.liberarRecintoSeleccionado();
        comprobar(mesas.getPestanaActiva() == 0 && mesas.getRecintoSeleccionado().getId() == null,
                "Liberar vuelve a Recintos");

        RecintoController recintos = new RecintoController();
        inyectar(recintos, "geograpBean", new GeograpBean() {
            @Override
            public List<Geograp> getByFatherId(Integer id) { return List.of(); }
        });
        Geograp canton = new Geograp();
        canton.setId(10);
        Geograp parroquia = new Geograp();
        parroquia.setId(100);
        recintos.setCantonSeleccionado(canton);
        recintos.setParroquiaSeleccionado(parroquia);
        recintos.prepararEdicion(primero, 0);
        recintos.getRecintoSeleccionado().setNombre("Edicion sin guardar");
        comprobar(primero.getNombre() == null, "Editar no muta fila antes de guardar");
        recintos.setCantonEdicionId(20);
        recintos.cambiarCantonEdicion();
        comprobar(recintos.getRecintoSeleccionado().getUbicacionId() == null
                && canton.getId() == 10 && parroquia.getId() == 100, "Dialogo no modifica filtros");
        recintos.nuevaRecinto();
        comprobar(recintos.getRecintoSeleccionado().getId() == null
                && recintos.getRecintoSeleccionado().getUbicacionId() == 100, "Nuevo utiliza filtro concreto");
        canton.setId(0);
        parroquia.setId(0);
        recintos.nuevaRecinto();
        comprobar(recintos.getCantonEdicionId() == null
                && recintos.getRecintoSeleccionado().getUbicacionId() == null, "Todos no es un ID geografico");
        System.out.println("OK: seleccion, cambio, lista mutable, pestanas y aislamiento del dialogo.");
    }

    private static RecintoDTO recinto(int id, int canton, int parroquia) {
        RecintoDTO dto = new RecintoDTO();
        dto.setId(id);
        dto.setCantonId(canton);
        dto.setUbicacionId(parroquia);
        return dto;
    }

    private static void inyectar(Object destino, String nombre, Object valor) throws Exception {
        var campo = destino.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(destino, valor);
    }

    private static void comprobar(boolean resultado, String mensaje) {
        if (!resultado) throw new AssertionError(mensaje);
    }
}
