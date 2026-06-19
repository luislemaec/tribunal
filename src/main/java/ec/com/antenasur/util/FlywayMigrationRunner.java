package ec.com.antenasur.util;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import lombok.extern.slf4j.Slf4j;

/**
 * Runner controlado para migraciones Flyway.
 *
 * Por seguridad permanece deshabilitado por defecto. Para ejecutarlo al
 * desplegar el WAR se debe definir -Dtec.flyway.enabled=true en WildFly.
 */
@Singleton
@Startup
@Slf4j
public class FlywayMigrationRunner {

    private static final String PROP_ENABLED = "tec.flyway.enabled";
    private static final String PROP_BASELINE_ON_MIGRATE = "tec.flyway.baselineOnMigrate";
    private static final String PROP_BASELINE_VERSION = "tec.flyway.baselineVersion";
    private static final String PROP_LOCATIONS = "tec.flyway.locations";
    private static final String PROP_DEFAULT_SCHEMA = "tec.flyway.defaultSchema";
    private static final String PROP_HISTORY_TABLE = "tec.flyway.table";

    private static final String DEFAULT_LOCATIONS = "classpath:db/migration";
    private static final String DEFAULT_SCHEMA = "public";
    private static final String DEFAULT_HISTORY_TABLE = "flyway_schema_history";

    @Resource(lookup = "java:jboss/datasources/TribunalDS")
    private DataSource dataSource;

    @PostConstruct
    public void migrate() {
        if (!getBoolean(PROP_ENABLED, false)) {
            log.info("Flyway deshabilitado. Defina -D{}=true para ejecutar migraciones.", PROP_ENABLED);
            return;
        }

        String locations = getString(PROP_LOCATIONS, DEFAULT_LOCATIONS);
        String defaultSchema = getString(PROP_DEFAULT_SCHEMA, DEFAULT_SCHEMA);
        String historyTable = getString(PROP_HISTORY_TABLE, DEFAULT_HISTORY_TABLE);
        boolean baselineOnMigrate = getBoolean(PROP_BASELINE_ON_MIGRATE, false);
        String baselineVersion = getString(PROP_BASELINE_VERSION, "1");

        try {
            FluentConfiguration configuration = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(locations)
                    .table(historyTable)
                    .baselineOnMigrate(baselineOnMigrate)
                    .baselineVersion(baselineVersion)
                    .baselineDescription("Baseline esquema existente TEC")
                    .cleanDisabled(true);

            if (!defaultSchema.isBlank()) {
                configuration.defaultSchema(defaultSchema);
            }

            Flyway flyway = configuration.load();
            var result = flyway.migrate();
            log.info("Flyway ejecutado correctamente. Migraciones aplicadas: {}. Version final: {}.",
                    result.migrationsExecuted,
                    result.targetSchemaVersion == null ? "sin cambios" : result.targetSchemaVersion);
        } catch (Exception e) {
            log.error("Error ejecutando migraciones Flyway. Se cancela el despliegue para proteger la BD.", e);
            throw new IllegalStateException("No se pudieron ejecutar las migraciones Flyway.", e);
        }
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private static String getString(String key, String defaultValue) {
        String value = System.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
