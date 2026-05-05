package ec.com.antenasur.util;

import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 * @fecha 2022-09-06 14:30
 * @version 1.0.0 Maneja acta de escritinios
 */
@Setter
@Getter
@NoArgsConstructor
public class ModeloColumna implements Serializable {

    private static final long serialVersionUID = 1L;

    private String property;
    private String header;
    private Class<?> type;

}
