package ec.com.antenasur.service.tec;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ec.com.antenasur.facade.tec.DependenciasDocumentoMesaFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.MiembroJRVFacade;

/** Compila HQL contra los mapeos reales sin conectarse a una base de datos. */
class ConsultasDocumentoMesaTest {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test void proyeccionesValidasParaHibernate() throws Exception {
        var config = new Configuration();
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        config.setProperty("hibernate.hbm2ddl.auto", "none");
        config.setProperty("hibernate.envers.autoRegisterListeners", "false");
        Path raiz = Path.of(ec.com.antenasur.model.tec.Mesa.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (var archivos = Files.walk(raiz.resolve("ec/com/antenasur/model"))) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".class") && !p.toString().contains("$")).toList()) {
                String nombre = raiz.relativize(archivo).toString().replace('\\','.').replace('/','.').replaceFirst("\\.class$", "");
                Class<?> clase = Class.forName(nombre);
                if (clase.isAnnotationPresent(Entity.class)) config.addAnnotatedClass(clase);
            }
        }
        try (var sf = config.buildSessionFactory(); var session = sf.openSession()) {
            EntityManager em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                    (p,m,a) -> {
                        if (!m.getName().equals("createQuery")) throw new UnsupportedOperationException(m.getName());
                        String hql = (String) a[0];
                        if (hql.contains("FROM MiembroJRV j")) {
                            // La conformacion cuenta las designaciones mostradas en MJRV, sin requisitos documentales.
                            assertTrue(hql.contains("j.estado = TRUE"));
                            assertTrue(hql.contains("j.proceso.id = :proceso"));
                            assertTrue(hql.contains("j.mesa.id IN :mesas"));
                            for (String condicionAjena : List.of("Padron", "Documentos", "habilitadoPadron",
                                    "pr.nombres", "pr.apellidos", "c.padre", "c.estado", "ip.estado", "pr.estado"))
                                assertFalse(hql.contains(condicionAjena), condicionAjena);
                        }
                        if (hql.contains("SELECT m FROM Mesa m") && hql.contains("ORDER BY r.nombre, m.nombre, m.id")) {
                            assertTrue(hql.contains("JOIN FETCH m.recinto r"));
                            assertTrue(hql.contains("LEFT JOIN FETCH r.ubicacion ru"));
                            assertTrue(hql.contains("LEFT JOIN FETCH ru.geograp canton"));
                            assertTrue(hql.contains("m.estado = TRUE AND r.estado = TRUE"));
                        }
                        var real = session.createQuery((String) a[0], (Class) a[1]);
                        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class}, (qp,qm,qa) -> {
                            if (qm.getName().equals("setParameter")) { real.setParameter((String) qa[0], qa[1]); return qp; }
                            if (qm.getName().equals("getResultList")) return List.of();
                            throw new UnsupportedOperationException(qm.getName());
                        });
                    });
            var facade = new DependenciasDocumentoMesaFacade() {
                @Override protected EntityManager getEntityManager() { return em; }
            };
            facade.padrones(1, List.of(4,5));
            var juntas = new MiembroJRVFacade() {
                @Override protected EntityManager getEntityManager() { return em; }
            };
            juntas.consultarConformacion(1, List.of(4,5));
            var mesas = new MesaFacade() {
                @Override protected EntityManager getEntityManager() { return em; }
            };
            mesas.listarActivasConUbicacion();
            facade.cabeceras(1, List.of(4,5)); facade.documentos(1, List.of(4,5));
            facade.tiposActivos();
        }
    }
}
