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
 * Notificación push pendiente o enviada (bandeja de salida). El evento la encola en su
 * transacción y EnvioNotificacionesProgramado la envía. Contenido mínimo, sin datos personales.
 */
@Entity
@Table(name = "notificacion_movil", schema = "tec")
@Getter
@Setter
public class NotificacionMovil {

	public static final String DESTINO_USUARIO = "USUARIO";
	public static final String DESTINO_ROL = "ROL";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notif_id")
	private Long id;

	@Column(name = "destino_tipo", nullable = false, length = 10)
	private String destinoTipo;

	/** usu_id (USUARIO) o nombre del rol SITEC-... (ROL). */
	@Column(name = "destino", nullable = false, length = 80)
	private String destino;

	@Column(name = "titulo", nullable = false, length = 80)
	private String titulo;

	@Column(name = "cuerpo", nullable = false, length = 200)
	private String cuerpo;

	/** Ruta interna de la App que se abre al tocar el aviso. */
	@Column(name = "ruta", length = 80)
	private String ruta;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	@Column(name = "enviada_en")
	private Instant enviadaEn;

	@Column(name = "intentos", nullable = false)
	private int intentos;

	@Column(name = "ultimo_error", length = 200)
	private String ultimoError;
}
