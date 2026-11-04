package ec.com.antenasur.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.primefaces.model.TreeNode;

/**
 * Conserva los nodos expandidos de un p:treeTable al reconstruir su modelo.
 *
 * <p>PrimeFaces solo procesa los campos de las filas cuyo nodo padre está expandido en el
 * servidor ({@code UITree.shouldVisitNode}). Si tras guardar se reconstruye el árbol con
 * todos los nodos contraídos, el navegador sigue mostrándolos abiertos (el rowEdit solo
 * redibuja la fila) y la siguiente edición llega sin sus valores: se guardan los anteriores
 * sin error. Por eso cada reconstrucción debe restaurar la expansión por la clave del dato.</p>
 */
public final class EstadoArbol {

    private EstadoArbol() {
    }

    /** Claves de los nodos expandidos (vacío si el árbol aún no existe). */
    public static <T> Set<Object> expandidos(TreeNode<T> raiz, Function<T, ?> clave) {
        if (raiz == null) {
            return Collections.emptySet();
        }
        Set<Object> claves = new HashSet<>();
        recolectar(raiz, clave, claves);
        return claves;
    }

    /** Vuelve a expandir los nodos cuyo dato tiene una de las claves indicadas. */
    public static <T> void restaurar(TreeNode<T> raiz, Set<Object> claves, Function<T, ?> clave) {
        if (raiz == null || claves == null || claves.isEmpty()) {
            return;
        }
        for (TreeNode<T> hijo : raiz.getChildren()) {
            Object valor = hijo.getData() != null ? clave.apply(hijo.getData()) : null;
            if (valor != null && claves.contains(valor)) {
                hijo.setExpanded(true);
            }
            restaurar(hijo, claves, clave);
        }
    }

    private static <T> void recolectar(TreeNode<T> nodo, Function<T, ?> clave, Set<Object> claves) {
        for (TreeNode<T> hijo : nodo.getChildren()) {
            if (hijo.isExpanded() && hijo.getData() != null) {
                Object valor = clave.apply(hijo.getData());
                if (valor != null) {
                    claves.add(valor);
                }
            }
            recolectar(hijo, clave, claves);
        }
    }
}
