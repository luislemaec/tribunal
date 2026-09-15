package ec.com.antenasur.model.tec;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sesion_qr_acta", schema = "tec")
@Getter
@Setter
public class SesionQrActa {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "sesion_id")
	private Long id;
	
	@Column(name = "qr_id", nullable = false, unique = true)
	private Long qrId;
	
	@Column(name = "prueba_hash", nullable = false, unique = true, length = 64)
	private String pruebaHash;
	
	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;
	
	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;
	
	@Column(name = "revocada_en")
	private Instant revocadaEn;
	
	@Column(name = "login_hash", length = 60)
	private String loginHash;
	
	@Column(name = "login_hasta")
	private Instant loginHasta;
	
	@Column(name = "login_confirmado", nullable = false)
	private boolean loginConfirmado;
}
