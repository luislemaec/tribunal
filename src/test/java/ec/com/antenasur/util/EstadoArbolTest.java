package ec.com.antenasur.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/** Expansión conservada al reconstruir un árbol (edición en línea de catálogos y menús). */
class EstadoArbolTest {

    private static final java.util.function.Function<Integer, Object> CLAVE = valor -> valor;

    /** raíz → 1 → (11, 12 → 121), 2 */
    private static TreeNode<Integer> arbol() {
        TreeNode<Integer> raiz = new DefaultTreeNode<>(0, null);
        TreeNode<Integer> uno = new DefaultTreeNode<>(1, raiz);
        new DefaultTreeNode<>(11, uno);
        TreeNode<Integer> doce = new DefaultTreeNode<>(12, uno);
        new DefaultTreeNode<>(121, doce);
        new DefaultTreeNode<>(2, raiz);
        return raiz;
    }

    private static TreeNode<Integer> nodo(TreeNode<Integer> raiz, int valor) {
        for (TreeNode<Integer> hijo : raiz.getChildren()) {
            if (hijo.getData() == valor) {
                return hijo;
            }
            TreeNode<Integer> encontrado = nodo(hijo, valor);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    @Test
    void reconstruirConservaLosNodosExpandidos() {
        TreeNode<Integer> anterior = arbol();
        nodo(anterior, 1).setExpanded(true);
        nodo(anterior, 12).setExpanded(true);

        Set<Object> expandidos = EstadoArbol.expandidos(anterior, CLAVE);
        assertEquals(Set.of(1, 12), expandidos);

        TreeNode<Integer> nuevo = arbol();
        EstadoArbol.restaurar(nuevo, expandidos, CLAVE);
        assertTrue(nodo(nuevo, 1).isExpanded());
        assertTrue(nodo(nuevo, 12).isExpanded());
        assertFalse(nodo(nuevo, 2).isExpanded());
        assertFalse(nodo(nuevo, 11).isExpanded());
    }

    @Test
    void sinArbolPrevioNoExpandeNada() {
        Set<Object> expandidos = EstadoArbol.expandidos(null, CLAVE);
        assertTrue(expandidos.isEmpty());
        TreeNode<Integer> nuevo = arbol();
        EstadoArbol.restaurar(nuevo, expandidos, CLAVE);
        assertFalse(nodo(nuevo, 1).isExpanded());
    }
}
