package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.MesaBean;
import ec.com.antenasur.bean.RecintoBean;
import ec.com.antenasur.dto.ResumenEscrutinioDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.GeograpService;
import ec.com.antenasur.service.tec.ResumenEscrutinioService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Pantalla de escrutinios (escrutinios.xhtml): totales del proceso electoral activo por cantón,
 * parroquia, recinto y mesa. Los filtros se encadenan: al cambiar uno se limpian los inferiores, y
 * «— Seleccione —» vuelve al nivel superior. El cálculo vive en {@link ResumenEscrutinioService}.
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class TotalVotosController implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Provincia cuyos cantones se ofrecen en el filtro. */
    private static final int PROVINCIA_ID = 7;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private GeograpService geograpService;

    @Inject
    private RecintoBean recintoBean;

    @Inject
    private MesaBean mesaBean;

    @Inject
    private ResumenEscrutinioService resumenEscrutinioService;

    @Getter
    private List<Geograp> cantones = Collections.emptyList();

    @Getter
    private List<Geograp> parroquias = Collections.emptyList();

    @Getter
    private List<Recinto> recintos = Collections.emptyList();

    @Getter
    private List<Mesa> mesas = Collections.emptyList();

    @Getter
    @Setter
    private Integer cantonId;

    @Getter
    @Setter
    private Integer parroquiaId;

    @Getter
    @Setter
    private Integer recintoId;

    @Getter
    @Setter
    private Integer mesaId;

    @Getter
    private ResumenEscrutinioDTO resumen = new ResumenEscrutinioDTO();

    /** Parroquias de todos los cantones: base cuando no hay cantón seleccionado. */
    private List<Geograp> todasParroquias = Collections.emptyList();

    @PostConstruct
    private void init() {
        try {
            cantones = noNulo(geograpBean.getByFatherId(PROVINCIA_ID));
            todasParroquias = noNulo(geograpService.obtenerParroquiasDeCantones(cantones));
            recalcular();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR ESCRUTINIOS", e);
        }
    }

    public void cambiarCanton() {
        parroquiaId = null;
        recintoId = null;
        mesaId = null;
        recalcular();
    }

    public void cambiarParroquia() {
        recintoId = null;
        mesaId = null;
        recalcular();
    }

    public void cambiarRecinto() {
        mesaId = null;
        recalcular();
    }

    public void cambiarMesa() {
        recalcular();
    }

    /** El recinto se muestra junto a la mesa cuando no hay un recinto elegido (los nombres de mesa se repiten). */
    public String etiquetaMesa(Mesa mesa) {
        if (mesa == null) {
            return "";
        }
        if (recintoId != null || mesa.getRecinto() == null) {
            return mesa.getNombre();
        }
        return mesa.getNombre() + " · " + mesa.getRecinto().getNombre();
    }

    private void recalcular() {
        try {
            Geograp canton = buscar(cantones, cantonId);
            cantonId = canton != null ? canton.getId() : null;
            parroquias = canton == null ? todasParroquias : noNulo(geograpBean.getByFatherGeograp(canton));
            Geograp parroquia = buscar(parroquias, parroquiaId);
            parroquiaId = parroquia != null ? parroquia.getId() : null;
            List<Geograp> parroquiasFiltro = parroquia == null ? parroquias : List.of(parroquia);

            recintos = parroquiasFiltro.isEmpty() ? Collections.emptyList()
                    : noNulo(recintoBean.recintosPorParroquias(parroquiasFiltro));
            Recinto recinto = recintos.stream().filter(r -> Objects.equals(r.getId(), recintoId)).findFirst().orElse(null);
            recintoId = recinto != null ? recinto.getId() : null;
            List<Recinto> recintosFiltro = recinto == null ? recintos : List.of(recinto);

            mesas = recintosFiltro.isEmpty() ? Collections.emptyList()
                    : noNulo(mesaBean.mesasPorRecintos(recintosFiltro));
            Mesa mesa = mesas.stream().filter(m -> Objects.equals(m.getId(), mesaId)).findFirst().orElse(null);
            mesaId = mesa != null ? mesa.getId() : null;
            List<Mesa> mesasFiltro = mesa == null ? mesas : List.of(mesa);

            List<Integer> mesaIds = new ArrayList<>();
            for (Mesa m : mesasFiltro) {
                mesaIds.add(m.getId());
            }
            resumen = resumenEscrutinioService.resumir(recintosFiltro.size(), mesaIds);
        } catch (Exception e) {
            log.error("ERROR AL CALCULAR EL RESUMEN DE ESCRUTINIOS", e);
            resumen = new ResumenEscrutinioDTO();
        }
    }

    private static Geograp buscar(List<Geograp> lista, Integer id) {
        if (id == null) {
            return null;
        }
        return lista.stream().filter(g -> Objects.equals(g.getId(), id)).findFirst().orElse(null);
    }

    private static <T> List<T> noNulo(List<T> lista) {
        return lista != null ? lista : Collections.emptyList();
    }
}
