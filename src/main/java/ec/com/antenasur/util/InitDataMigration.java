package ec.com.antenasur.util;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Migraciones automáticas que se ejecutan al desplegar la aplicación.
 *
 * Cada bloque de migración debe ser idempotente: ejecutarlo varias veces no
 * debe alterar el resultado más allá de la primera ejecución.
 *
 * No se introduce ningún framework de migraciones (Flyway/Liquibase) para
 * mantener mínima la huella; cuando crezca el número de migraciones conviene
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
     * Cuando se agregó {@code @Version igl_version} a la entidad {@code Iglesia},
     * Hibernate creó la columna sin default, dejando NULL en todas las filas
     * preexistentes. Eso hace que el siguiente UPDATE lance NullPointerException
     * en {@code Versioning.increment(next(null))}.
     *
     * Aquí rellenamos a 0 las filas con version NULL para que la primera edición
     * pueda incrementar correctamente a 1.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void backfillIglesiaVersion() {
        try {
            int actualizadas = em.createNativeQuery(
                    "UPDATE public.tb_iglesia SET igl_version = 0 WHERE igl_version IS NULL")
                    .executeUpdate();
            if (actualizadas > 0) {
                log.info("Migración: igl_version inicializada a 0 en {} fila(s) de tb_iglesia.", actualizadas);
            }
        } catch (Exception e) {
            // No lanzamos: la migración es best-effort. Si falla aquí (p.ej. la
            // columna aún no existe en un primer arranque), Hibernate la creará
            // en este mismo despliegue y la próxima activación rellenará.
            log.warn("Migración igl_version omitida: {}", e.getMessage());
        }
    }
}
