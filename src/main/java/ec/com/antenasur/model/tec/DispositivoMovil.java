package ec.com.antenasur.model.tec;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Dispositivo de la App móvil con su token de Firebase Cloud Messaging, para las
 * notificaciones push. Ver V12__notificaciones_push.sql y docs/api-movil.md.
 */
@Entity
@Table(name = "dispositivo_movil", schema = "tec")
@Getter
@Setter
public class DispositivoMovil {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "disp_id")
	private Long id;

	@Column(name = "usu_id", nullable = false)
	private Integer usuarioId;

	@Column(name = "token", nullable = false, unique = true, length = 512)
	private String token;

	/** android o ios. */
	@Column(name = "plataforma", nullable = false, length = 10)
	private String plataforma;

	@Column(name = "registrado_en", nullable = false)
	private Instant registradoEn;

	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@Column(name = "activo", nullable = false)
	private boolean activo;
}
