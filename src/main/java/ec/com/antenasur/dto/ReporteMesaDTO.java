package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/** Vista consolidada y de solo lectura del proceso electoral por mesa. */
@Data
public class ReporteMesaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private ProcesoElectoralDTO proceso;
    private RecintoDTO recinto;
    private MesaDTO mesa;
    private EscrutinioCabeceraDTO cabecera;
    private List<EscrutinioDTO> escrutinios = new ArrayList<>();
    private List<PadronDTO> padron = new ArrayList<>();
    private List<MiembroJRVDTO> miembrosJrv = new ArrayList<>();
    private List<DocumentoDTO> documentos = new ArrayList<>();

    public int getTotalPadron() {
        return padron != null ? padron.size() : 0;
    }

    public int getTotalMiembrosJrv() {
        return miembrosJrv != null ? miembrosJrv.size() : 0;
    }
}
