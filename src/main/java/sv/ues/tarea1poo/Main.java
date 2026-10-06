package sv.ues.tarea1poo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import sv.ues.tarea1poo.excepciones.PersistenciaException;
import sv.ues.tarea1poo.servicio.InventarioService;
import sv.ues.tarea1poo.vista.MenuConsola;

/**
 * Punto de entrada del sistema de inventario.
 *
 * Tarea 1 de Programacion Orientada a Objetos - Universidad de El Salvador.
 * Clave 2: manejo de excepciones y persistencia con Java I/O y NIO.2.
 *
 * Arquitectura en capas: vista -> servicio -> persistencia -> archivos txt.
 */
public class Main {

    public static final String CARPETA_DATOS = "datos";

    /**
     * CONCEPTO: a proposito main NO declara "throws PersistenciaException".
     *
     * Si lo declarara, una falla al cargar los archivos subiria hasta la JVM,
     * que imprimiria un volcado de pila tecnico y cerraria el programa de golpe.
     * Capturando aqui, el usuario recibe un mensaje claro de que paso y el
     * programa termina de forma ordenada. Capturar es mas robusto que delegar.
     */
    public static void main(String[] args) {
        // CONCEPTO: NIO.2 - Path. Ruta relativa a la carpeta del proyecto, asi
        // el programa funciona en cualquier computadora sin editar codigo.
        Path carpetaDatos = Paths.get(CARPETA_DATOS);

        System.out.println("===================================================");
        System.out.println("     SISTEMA DE INVENTARIO - Tarea 1 POO");
        System.out.println("===================================================");
        System.out.println("Carpeta de datos: " + carpetaDatos.toAbsolutePath());

        try {
            InventarioService servicio = new InventarioService(carpetaDatos);

            // Las advertencias no son errores: avisan que un archivo se creo,
            // estaba vacio o traia una linea que hubo que descartar.
            List<String> advertencias = servicio.getAdvertencias();
            if (!advertencias.isEmpty()) {
                System.out.println("\n--- Advertencias al cargar los datos ---");
                for (String advertencia : advertencias) {
                    System.out.println("  * " + advertencia);
                }
            }

            new MenuConsola(servicio).iniciar();

        } catch (PersistenciaException e) {
            System.out.println("\n[ERROR FATAL] No se pudieron cargar los datos del inventario.");
            System.out.println("  Detalle: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  Causa original: "
                        + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            System.out.println("\n  Revise que la carpeta \"" + CARPETA_DATOS
                    + "\" exista y que tenga permisos de lectura y escritura.");
            System.out.println("  El programa se cierra sin modificar los archivos.");
        }
    }
}
