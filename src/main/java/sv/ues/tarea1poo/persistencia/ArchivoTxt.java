package sv.ues.tarea1poo.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import sv.ues.tarea1poo.excepciones.DatosInvalidosException;
import sv.ues.tarea1poo.excepciones.PersistenciaException;

/**
 * Clase abstracta generica que concentra TODO el manejo de archivos del
 * sistema. Los tres repositorios la heredan y solo dicen como convertir una
 * linea en objeto y un objeto en linea, asi que la logica de entrada/salida
 * se escribe una sola vez.
 *
 * El parametro generico T es el tipo de entidad que guarda el archivo
 * (CataInvent, SaldosInventario o Kardex).
 *
 * Esta clase es la que concentra los conceptos del tema:
 *  - NIO.2 (Path, Files) en crearSiNoExiste, leerTodos y guardarTodos.
 *  - Java I/O clasico (File, FileReader, FileWriter) en respaldar.
 *  - try-with-resources, finally, encadenamiento de excepciones y throws.
 */
public abstract class ArchivoTxt<T> {

    public static final String EXTENSION_TEMPORAL = ".tmp";
    public static final String CARPETA_RESPALDOS = "respaldos";

    private static final DateTimeFormatter FORMATO_MARCA_TIEMPO =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // CONCEPTO: NIO.2 - Path. Representa la ruta del archivo sin abrirlo.
    protected final Path ruta;

    /**
     * Problemas no fatales detectados en la ultima lectura: archivo creado,
     * archivo vacio, lineas corruptas descartadas. No son excepciones porque
     * no impiden seguir trabajando, pero el usuario debe enterarse.
     */
    private final List<String> advertencias = new ArrayList<>();

    protected ArchivoTxt(Path ruta) {
        this.ruta = ruta;
    }

    /**
     * Convierte una linea del archivo en objeto. Cada repositorio lo resuelve
     * llamando al desdeLinea de su entidad.
     */
    protected abstract T parsear(String linea);

    /**
     * Convierte un objeto en linea del archivo. Cada repositorio lo resuelve
     * llamando al aLinea de su entidad.
     */
    protected abstract String serializar(T entidad);

    /**
     * Se devuelve una copia para que nadie de afuera modifique la lista interna.
     */
    public List<String> getAdvertencias() {
        return new ArrayList<>(advertencias);
    }

    public Path getRuta() {
        return ruta;
    }

    /**
     * Garantiza que la carpeta y el archivo existan antes de usarlos.
     *
     * SITUACION DEL ENUNCIADO: "archivo inexistente". En la primera ejecucion
     * no hay carpeta datos ni archivos; el programa los crea y avisa, en lugar
     * de reventar con una excepcion.
     */
    protected void crearSiNoExiste() throws PersistenciaException {
        try {
            Path carpeta = ruta.toAbsolutePath().getParent();
            if (carpeta != null) {
                // CONCEPTO: NIO.2 - Files. createDirectories no falla si ya existe.
                Files.createDirectories(carpeta);
            }
            if (Files.notExists(ruta)) {
                Files.createFile(ruta);
                advertencias.add("Archivo inexistente, se creo uno nuevo: " + ruta.getFileName());
            }
        } catch (IOException e) {
            // CONCEPTO: encadenamiento. La IOException tecnica se envuelve en una
            // excepcion del dominio, pero viaja dentro como causa.
            throw new PersistenciaException("No se pudo preparar el archivo " + ruta + ".", e);
        }
    }

    /**
     * Lee todos los registros del archivo.
     *
     * Una linea corrupta NO cancela la carga: se descarta, se registra una
     * advertencia con su numero y se sigue con las demas. Asi un solo renglon
     * mal escrito a mano no deja al usuario sin inventario.
     */
    public List<T> leerTodos() throws PersistenciaException {
        advertencias.clear();
        crearSiNoExiste();

        List<T> registros = new ArrayList<>();
        try {
            // SITUACION DEL ENUNCIADO: "archivo vacio". Es un caso normal
            // (recien creado), no un error.
            if (Files.size(ruta) == 0) {
                advertencias.add("Archivo vacio: " + ruta.getFileName());
                return registros;
            }

            // CONCEPTO: try-with-resources. El BufferedReader se cierra solo al
            // salir del bloque, incluso si se lanza una excepcion. No hace falta
            // escribir un finally para cerrarlo.
            try (BufferedReader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
                String linea;
                int numeroLinea = 0;
                while ((linea = lector.readLine()) != null) {
                    numeroLinea++;
                    if (linea.isBlank()) {
                        continue;
                    }
                    try {
                        registros.add(parsear(linea));
                    } catch (DatosInvalidosException e) {
                        // CONCEPTO: try/catch dentro del ciclo para aislar el fallo
                        // de una sola linea y continuar con el resto.
                        advertencias.add("Linea " + numeroLinea + " de " + ruta.getFileName()
                                + " descartada: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            // SITUACION DEL ENUNCIADO: "error durante la lectura".
            throw new PersistenciaException("Error durante la lectura de " + ruta + ".", e);
        }
        return registros;
    }

    /**
     * Guarda la lista completa en el archivo con ESCRITURA SEGURA.
     *
     * No se escribe directo sobre el archivo real: primero se escribe todo en
     * un archivo temporal y solo si eso termina bien se reemplaza el original
     * con un movimiento (idealmente atomico). Si el programa se corta a mitad
     * de la escritura, el archivo bueno sigue intacto y lo unico perdido es
     * el temporal.
     */
    public void guardarTodos(List<T> registros) throws PersistenciaException {
        crearSiNoExiste();
        Path temporal = ruta.resolveSibling(ruta.getFileName() + EXTENSION_TEMPORAL);
        try {
            // CONCEPTO: try-with-resources, ahora para escribir.
            try (BufferedWriter escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                for (T registro : registros) {
                    escritor.write(serializar(registro));
                    escritor.newLine();
                }
            }

            try {
                // CONCEPTO: NIO.2 - Files.move. ATOMIC_MOVE pide que el reemplazo
                // sea instantaneo: o queda el archivo viejo o queda el nuevo,
                // nunca uno a medias.
                Files.move(temporal, ruta,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Algunos sistemas de archivos (una USB con FAT32, una carpeta de
                // red) no soportan el movimiento atomico. Se reintenta sin el.
                Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // SITUACION DEL ENUNCIADO: "error durante la escritura".
            throw new PersistenciaException("Error durante la escritura de " + ruta + ".", e);
        } finally {
            // CONCEPTO: finally. Se ejecuta siempre, haya salido bien o mal, para
            // no dejar basura .tmp en la carpeta de datos.
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException e) {
                advertencias.add("No se pudo borrar el archivo temporal " + temporal.getFileName() + ".");
            }
        }
    }

    /**
     * Copia el archivo a la carpeta de respaldos.
     *
     * Este metodo usa A PROPOSITO el Java I/O clasico (File, FileReader,
     * FileWriter) y cierra los recursos a mano en un finally, para poder
     * compararlo con el try-with-resources y NIO.2 de los metodos de arriba.
     *
     * @return la ruta del respaldo creado.
     */
    public Path respaldar() throws PersistenciaException {
        crearSiNoExiste();

        // CONCEPTO: Java I/O - File. Es solo un nombre de archivo, el estilo
        // anterior a Path.
        File archivoOrigen = ruta.toFile();
        File carpetaRespaldos = new File(ruta.toAbsolutePath().getParent().toFile(), CARPETA_RESPALDOS);
        if (!carpetaRespaldos.exists() && !carpetaRespaldos.mkdirs()) {
            // mkdirs devuelve false en vez de lanzar excepcion: hay que revisarlo
            // a mano. Files.createDirectories de NIO.2 si lanza IOException con
            // el motivo, y por eso es mas comodo.
            throw new PersistenciaException("No se pudo crear la carpeta de respaldos "
                    + carpetaRespaldos.getAbsolutePath() + ".");
        }

        String marcaTiempo = LocalDateTime.now().format(FORMATO_MARCA_TIEMPO);
        File archivoDestino = new File(carpetaRespaldos,
                "respaldo_" + marcaTiempo + "_" + nombreSinExtension() + ".txt");

        // Los recursos se declaran FUERA del try para poder cerrarlos en el
        // finally. Con try-with-resources estas cuatro lineas no existirian.
        BufferedReader lector = null;
        BufferedWriter escritor = null;
        try {
            lector = new BufferedReader(new FileReader(archivoOrigen, StandardCharsets.UTF_8));
            escritor = new BufferedWriter(new FileWriter(archivoDestino, StandardCharsets.UTF_8));

            String linea;
            while ((linea = lector.readLine()) != null) {
                escritor.write(linea);
                escritor.newLine();
            }
            return archivoDestino.toPath();

            // CONCEPTO: orden de los catch, de lo MAS ESPECIFICO a lo mas general.
            // FileNotFoundException es hija de IOException; si se pusiera despues,
            // el compilador marcaria error porque nunca se alcanzaria.
        } catch (FileNotFoundException e) {
            throw new PersistenciaException("No se encontro el archivo a respaldar: "
                    + archivoOrigen.getAbsolutePath() + ".", e);
        } catch (IOException e) {
            throw new PersistenciaException("Error durante el respaldo de " + ruta + ".", e);
        } finally {
            // CONCEPTO: finally cerrando recursos a mano. Primero el escritor,
            // para que vacie su buffer al disco antes de soltar el archivo.
            cerrarSilenciosamente(escritor);
            cerrarSilenciosamente(lector);
        }
    }

    /**
     * Cierra un recurso ignorando el error de cierre.
     *
     * Hace falta porque close() tambien lanza IOException: si se llamara
     * directo en el finally, habria que envolverlo en OTRO try/catch adentro.
     * Esta es justamente la incomodidad que elimina try-with-resources.
     */
    private static void cerrarSilenciosamente(Closeable recurso) {
        if (recurso == null) {
            return;
        }
        try {
            recurso.close();
        } catch (IOException e) {
            // No hay nada util que hacer: el respaldo ya se escribio o ya fallo.
        }
    }

    /**
     * "catalogo.txt" -> "catalogo", para armar el nombre del respaldo.
     */
    private String nombreSinExtension() {
        String nombre = ruta.getFileName().toString();
        int posicionPunto = nombre.lastIndexOf('.');
        return posicionPunto > 0 ? nombre.substring(0, posicionPunto) : nombre;
    }
}
