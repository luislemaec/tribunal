package ec.com.antenasur.service.tec;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import ec.com.antenasur.dto.FiltroPadronDTO;
import ec.com.antenasur.facade.tec.GestionPadronFacade;

/** Compila HQL y comprueba parametros con el metamodelo real, sin conexiones JDBC. */
public class GestionPadronQueriesCheck {
    public static void main(String[] args) throws Exception {
        var registro = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .applySetting("hibernate.integration.envers.enabled", false).build();
        var fuentes = new MetadataSources(registro);
        Path raiz = Path.of("target/classes");
        try (var archivos = Files.walk(raiz.resolve("ec/com/antenasur"))) {
            for (Path ruta : archivos.filter(p -> p.toString().endsWith(".class")).toList()) {
                String nombre = raiz.relativize(ruta).toString().replace('\\', '.').replace('/', '.').replaceAll("\\.class$", "");
                Class<?> clase = Class.forName(nombre, false, GestionPadronQueriesCheck.class.getClassLoader());
                if (clase.isAnnotationPresent(Entity.class)) fuentes.addAnnotatedClass(clase);
            }
        }
        try (var sf = fuentes.buildMetadata().buildSessionFactory(); var real = sf.createEntityManager()) {
            EntityManager em = (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, (proxy, method, valores) -> {
                Object resultado;
                try { resultado = method.invoke(real, valores); }
                catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                if (resultado instanceof TypedQuery<?> query) return Proxy.newProxyInstance(TypedQuery.class.getClassLoader(), new Class[]{TypedQuery.class}, (p, m, a) -> {
                    if (m.getName().equals("getResultList")) return List.of();
                    if (m.getName().equals("getSingleResult")) return 0L;
                    Object r;
                    try { r = m.invoke(query, a); }
                    catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                    return r instanceof TypedQuery ? p : r;
                });
                return resultado;
            });
            GestionPadronFacade f = new GestionPadronFacade() { @Override protected EntityManager getEntityManager() { return em; } };
            FiltroPadronDTO filtro = new FiltroPadronDTO();
            filtro.setProcesoId(1); filtro.setIglesiaId(1); filtro.setMesaId(1);
            f.listar(filtro, true, 0, 20, "nombres", false, null); f.contar(filtro, true);
            f.listar(filtro, false, 0, 20, "documento", true, null); f.contar(filtro, false);
            filtro.setProvinciaId(1); filtro.setCantonId(2); filtro.setParroquiaId(3); filtro.setRecintoId(4);
            filtro.setBusqueda("texto_100%"); filtro.setIglesiaNombre("iglesia");
            f.listar(filtro, false, 0, 500, null, false, 0); f.contar(filtro, false);
            f.recintos(filtro); f.opcionesMesas(1); f.iglesias(1, 1); f.geografia(null); f.geografia(1);
            f.resumenIglesias(1, 1, false); f.resumenIglesias(1, 1, true);
            f.habilitadosPendientes(1, 1, 1); f.empadronadosIglesia(1, 1, 1);
            for (Boolean estado : new Boolean[]{null, true, false}) {
                filtro.setConPadron(estado); f.listarMesas(filtro, 0, 20); f.contarMesas(filtro, estado);
            }
            f.miembros(List.of(1)); f.existentes(1, List.of(1)); f.iglesiaEnOtraMesa(1, 1, 1);
            f.seleccion(List.of(1), 1, 1); f.tieneJrv(List.of(1), 1); f.escrutinioIniciado(1, 1);
        } finally { StandardServiceRegistryBuilder.destroy(registro); }
        System.out.println("OK: proyecciones JPA, filtros combinados, parametros, consultas de control y mesas con/sin padron.");
    }
}
