package ec.com.antenasur.util;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Migraciones automÃ¡ticas que se ejecutan al desplegar la aplicaciÃ³n.
 *
 * Cada bloque de migraciÃ³n debe ser idempotente: ejecutarlo varias veces no
 * debe alterar el resultado mÃ¡s allÃ¡ de la primera ejecuciÃ³n.
 *
 * No se introduce ningÃºn framework de migraciones (Flyway/Liquibase) para
 * mantener mÃ­nima la huella; cuando crezca el nÃºmero de migraciones conviene
 * migrar a uno de ellos.
 */
@Singleton
@Startup
@Slf4j
public class InitDataMigration {

    @PersistenceContext(unitName = "tribunalPU")
    private EntityManager em;

    @PostConstruct
    public void run() {
        backfillIglesiaVersion();
    }

    /**
     * Cuando se agregÃ³ {@code @Version igl_version} a la entidad {@code Iglesia},
     * Hibernate creÃ³ la columna sin default, dejando NULL en todas las filas
     * preexistentes. Eso hace que el siguiente UPDATE lance NullPointerException
     * en {@code Versioning.increment(next(null))}.
     *
     * AquÃ­ rellenamos a 0 las filas con version NULL para que la primera ediciÃ³n
     * pueda incrementar correctamente a 1.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void backfillIglesiaVersion() {
        try {
            int actualizadas = em.createNativeQuery(
                    "UPDATE public.tb_iglesia SET igl_version = 0 WHERE igl_version IS NULL")
                    .executeUpdate();
            if (actualizadas > 0) {
                log.info("MigraciÃ³n: igl_version inicializada a 0 en {} fila(s) de tb_iglesia.", actualizadas);
            }
        } catch (Exception e) {
            // No lanzamos: la migraciÃ³n es best-effort. Si falla aquÃ­ (p.ej. la
            // columna aÃºn no existe en un primer arranque), Hibernate la crearÃ¡
            // en este mismo despliegue y la prÃ³xima activaciÃ³n rellenarÃ¡.
            log.warn("MigraciÃ³n igl_version omitida: {}", e.getMessage());
        }
    }
}
