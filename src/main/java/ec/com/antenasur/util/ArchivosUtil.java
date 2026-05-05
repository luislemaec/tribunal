package ec.com.antenasur.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ArchivosUtil {

    /**
     * Crear directorio de carpetas
     *
     * @param path
     */
    public static void crearDirectorio(String path) {
        File directorioTmp = new File(path);
        if (!directorioTmp.exists()) {
            if (directorioTmp.mkdirs());
        }

    }

    /**
     * Eliminar Directorio y archivos recursivo
     *
     * @param path
     */
    public static void eliminarDirectorioRecursivo(String path) {
        File pArchivo = new File(path);

        if (!pArchivo.exists()) {
            return;
        }
        if (pArchivo.isDirectory()) {
            for (File f : pArchivo.listFiles()) {
                eliminarDirectorioRecursivo(f.getPath());
            }
        }
        pArchivo.delete();
    }

    /**
     * Obtener bytes[] de un archivo
     *
     * @param path
     * @return
     */
    public static byte[] obtenerBytes(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Mtodo que permite convertir un archivo a byte.
     *
     * @param file
     * @return byte Return the value this
     */
    public static byte[] getBytesFromFile(File file) {
        try {
            FileInputStream is = new FileInputStream(file);
            long length = file.length();
            byte[] bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;

            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                is.close();
            }

            is.close();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
