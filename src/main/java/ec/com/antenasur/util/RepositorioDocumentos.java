package ec.com.antenasur.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Operaciones seguras sobre el repositorio institucional de documentos. */
public final class RepositorioDocumentos {

    private RepositorioDocumentos() {
    }

    public static Path prepararDirectorio(String subdirectorio) throws IOException {
        Path base = Paths.get(Constantes.getDirectorioDocumentos()).toAbsolutePath().normalize();
        Files.createDirectories(base);
        Path directorio = Paths.get(Constantes.getDirectorioDocumentos(subdirectorio)).toAbsolutePath().normalize();
        Files.createDirectories(directorio);
        Path baseReal = base.toRealPath();
        Path directorioReal = directorio.toRealPath();
        if (!directorioReal.startsWith(baseReal)) {
            throw new IOException("El directorio esta fuera del repositorio institucional.");
        }
        if (!Files.isDirectory(directorioReal) || !Files.isReadable(directorioReal)
                || !Files.isWritable(directorioReal)) {
            throw new IOException("El repositorio institucional no tiene permisos de lectura y escritura: "
                    + directorioReal);
        }
        return directorioReal;
    }

    public static Path escribirAtomico(String subdirectorio, String nombreArchivo, byte[] contenido)
            throws IOException {
        if (contenido == null || contenido.length == 0) {
            throw new IOException("No existe contenido para almacenar.");
        }
        String nombreSeguro = nombreArchivoSeguro(nombreArchivo);
        Path directorio = prepararDirectorio(subdirectorio);
        Path destino = directorio.resolve(nombreSeguro).normalize();
        if (!destino.startsWith(directorio)) {
            throw new IOException("La ruta destino esta fuera del repositorio institucional.");
        }
        if (Files.exists(destino)) {
            throw new IOException("El documento ya existe en la ruta destino: " + destino);
        }

        Path temporal = Files.createTempFile(directorio, ".documento-", ".tmp");
        try {
            Files.write(temporal, contenido);
            try {
                return Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                return Files.move(temporal, destino);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /** Resuelve rutas absolutas o relativas, siempre dentro del repositorio oficial. */
    public static Path resolverRutaAlmacenada(String ruta) throws IOException {
        if (ruta == null || ruta.isBlank()) {
            throw new IOException("El documento no tiene una ruta registrada.");
        }
        Path base = Paths.get(Constantes.getDirectorioDocumentos()).toAbsolutePath().normalize();
        Path almacenada = Paths.get(ruta.trim());
        Path resuelta = almacenada.isAbsolute()
                ? almacenada.toAbsolutePath().normalize()
                : base.resolve(almacenada).normalize();
        if (!resuelta.startsWith(base)) {
            throw new IOException("La ruta esta fuera del repositorio institucional.");
        }
        if (!Files.isRegularFile(resuelta) || !Files.isReadable(resuelta)) {
            throw new IOException("El documento no existe o no tiene permisos de lectura: " + resuelta);
        }
        Path baseReal = base.toRealPath();
        Path resueltaReal = resuelta.toRealPath();
        if (!resueltaReal.startsWith(baseReal)) {
            throw new IOException("La ruta fisica esta fuera del repositorio institucional.");
        }
        return resueltaReal;
    }

    public static boolean estaDisponible(String ruta) {
        try {
            resolverRutaAlmacenada(ruta);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    public static InputStream abrirLectura(String ruta) throws IOException {
        return Files.newInputStream(resolverRutaAlmacenada(ruta));
    }

    /** Convierte una ruta fisica validada en una ruta portable relativa a rpm.files.path. */
    public static String rutaRelativaParaPersistir(Path archivo) throws IOException {
        if (archivo == null) {
            throw new IOException("La ruta del documento es obligatoria.");
        }
        Path base = Paths.get(Constantes.getDirectorioDocumentos()).toAbsolutePath().normalize().toRealPath();
        Path archivoReal = archivo.toAbsolutePath().normalize().toRealPath();
        if (!archivoReal.startsWith(base) || !Files.isRegularFile(archivoReal)) {
            throw new IOException("El documento esta fuera del repositorio institucional.");
        }
        return base.relativize(archivoReal).toString().replace('\\', '/');
    }

    public static String nombreArchivoSeguro(String nombreArchivo) throws IOException {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            throw new IOException("El nombre del archivo es obligatorio.");
        }
        String nombre = Paths.get(nombreArchivo).getFileName().toString();
        nombre = nombre.replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        if (nombre.isBlank() || ".".equals(nombre) || "..".equals(nombre)) {
            throw new IOException("El nombre del archivo no es valido.");
        }
        return nombre.substring(0, Math.min(nombre.length(), 180));
    }

    public static String sha256(byte[] contenido) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (Exception e) {
            throw new IOException("No se pudo calcular el hash SHA-256.", e);
        }
    }

    public static void eliminarSilencioso(Path archivo) {
        if (archivo != null) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignored) {
                // La limpieza no debe ocultar el error original de persistencia.
            }
        }
    }
}
