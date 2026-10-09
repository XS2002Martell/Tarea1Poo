package sv.ues.tarea1poo.vista;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import sv.ues.tarea1poo.excepciones.CodigoDuplicadoException;
import sv.ues.tarea1poo.excepciones.DatosInvalidosException;
import sv.ues.tarea1poo.excepciones.IntegridadReferencialException;
import sv.ues.tarea1poo.excepciones.PersistenciaException;
import sv.ues.tarea1poo.excepciones.ProductoNoEncontradoException;
import sv.ues.tarea1poo.excepciones.StockInsuficienteException;
import sv.ues.tarea1poo.modelo.CataInvent;
import sv.ues.tarea1poo.modelo.Kardex;
import sv.ues.tarea1poo.modelo.SaldosInventario;
import sv.ues.tarea1poo.servicio.InventarioService;

/**
 * Interfaz de consola. Esta capa solo pide datos, llama al servicio y muestra
 * resultados: nunca abre un archivo.
 *
 * Aqui es donde se CAPTURAN las excepciones. Las capas de abajo las lanzan
 * (throw) o las declaran (throws); la vista es la que decide que mensaje ve el
 * usuario. Ninguna excepcion cierra el programa: siempre se vuelve al menu.
 *
 * Los mensajes van sin tildes a proposito, para que se vean bien tanto en la
 * ventana de salida de NetBeans como en la consola de Windows, que usan
 * codificaciones distintas.
 */
public class MenuConsola {

    // El ancho de cada raya coincide con la suma de las columnas de su tabla.
    private static final String LINEA_INVENTARIO = "-".repeat(80);
    private static final String LINEA_KARDEX = "-".repeat(68);

    private final InventarioService servicio;

    // No se cierra el Scanner: cerrarlo cerraria System.in para todo el programa.
    private final Scanner entrada = new Scanner(System.in);

    public MenuConsola(InventarioService servicio) {
        this.servicio = servicio;
    }

       public void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerEntero("Seleccione una opcion: ");

            // CONCEPTO: try/catch con los bloques ordenados de lo MAS ESPECIFICO
            // a lo mas general. Todo el menu esta protegido, por eso un error
            // nunca tumba el programa: se muestra el mensaje y se repite el ciclo.
            try {
                // CONCEPTO: switch con sintaxis de flecha (Java 14+). No necesita
                // break y no se "cae" de un caso al siguiente.
                switch (opcion) {
                    case 1 -> registrarProducto();
                    case 2 -> modificarProducto();
                    case 3 -> eliminarProducto();
                    case 4 -> buscarPorCodigo();
                    case 5 -> buscarPorNombre();
                    case 6 -> consultarInventario();
                    case 7 -> registrarEntrada();
                    case 8 -> registrarSalida();
                    case 9 -> consultarKardex();
                    case 10 -> crearRespaldo();
                    case 0 -> System.out.println("\nCerrando el sistema. Los datos ya quedaron guardados.");
                    // Opcion que no existe en el menu.
                    default -> System.out.println("\n[ERROR] La opcion " + opcion + " no existe en el menu.");
                }

                // CONCEPTO: MULTI-CATCH. Dos excepciones distintas que se le
                // informan al usuario de la misma forma.
            } catch (ProductoNoEncontradoException | CodigoDuplicadoException e) {
                System.out.println("\n[AVISO] " + e.getMessage());

            } catch (StockInsuficienteException | IntegridadReferencialException e) {
                System.out.println("\n[OPERACION RECHAZADA] " + e.getMessage());

                // CONCEPTO: captura de una excepcion NO VERIFICADA. El compilador
                // no obligaba a ponerla, pero sin este catch un dato mal digitado
                // cerraria el programa con un volcado de pila.
            } catch (DatosInvalidosException e) {
                System.out.println("\n[DATO INVALIDO] " + e.getMessage());

            } catch (PersistenciaException e) {
                System.out.println("\n[ERROR DE ARCHIVO] " + e.getMessage());
                // CONCEPTO: recuperar la causa encadenada para ver el error
                // tecnico real (la IOException original).
                if (e.getCause() != null) {
                    System.out.println("          Causa original: "
                            + e.getCause().getClass().getSimpleName()
                            + ": " + e.getCause().getMessage());
                }
            }

            if (opcion != 0) {
                pausar();
            }
        } while (opcion != 0);
    }

    private void mostrarMenu() {
        System.out.println("\n===== SISTEMA DE INVENTARIO =====");
        System.out.println(" 1. Registrar producto");
        System.out.println(" 2. Modificar producto");
        System.out.println(" 3. Eliminar producto");
        System.out.println(" 4. Buscar producto por codigo");
        System.out.println(" 5. Buscar producto por nombre");
        System.out.println(" 6. Consultar inventario (catalogo + saldos)");
        System.out.println(" 7. Registrar entrada");
        System.out.println(" 8. Registrar salida");
        System.out.println(" 9. Consultar kardex de un producto");
        System.out.println("10. Crear respaldo de datos");
        System.out.println(" 0. Salir");
    }

    // ------------------------------------------------------------------
    // Opciones del menu. Declaran throws y dejan que iniciar() capture.
    // ------------------------------------------------------------------

    private void registrarProducto() throws CodigoDuplicadoException, PersistenciaException {
        System.out.println("\n--- Registrar producto ---");
        String codigo = leerTexto("Codigo (3 a 15 caracteres, letras/numeros/guiones): ");
        String nombre = leerTexto("Nombre del producto: ");
        servicio.registrarProducto(codigo, nombre);
        System.out.println("Producto registrado con saldo inicial en cero.");
    }

    private void modificarProducto() throws ProductoNoEncontradoException, PersistenciaException {
        System.out.println("\n--- Modificar producto ---");
        String codigo = leerTexto("Codigo del producto: ");
        CataInvent producto = servicio.buscarProducto(codigo);
        System.out.println("Nombre actual: " + producto.getNombreInventario());
        String nuevoNombre = leerTexto("Nombre nuevo: ");
        servicio.modificarProducto(codigo, nuevoNombre);
        System.out.println("Producto actualizado.");
    }

    private void eliminarProducto()
            throws ProductoNoEncontradoException, IntegridadReferencialException, PersistenciaException {
        System.out.println("\n--- Eliminar producto ---");
        String codigo = leerTexto("Codigo del producto: ");
        CataInvent producto = servicio.buscarProducto(codigo);
        System.out.println("Se va a eliminar: " + producto);

        String confirmacion = leerTexto("Confirma la eliminacion? (S/N): ");
        if (!confirmacion.trim().equalsIgnoreCase("S")) {
            System.out.println("Eliminacion cancelada.");
            return;
        }
        servicio.eliminarProducto(codigo);
        System.out.println("Producto eliminado del catalogo y de los saldos.");
    }

    private void buscarPorCodigo() throws ProductoNoEncontradoException {
        System.out.println("\n--- Buscar producto por codigo ---");
        String codigo = leerTexto("Codigo del producto: ");
        CataInvent producto = servicio.buscarProducto(codigo);
        SaldosInventario saldo = servicio.consultarSaldo(codigo);
        System.out.println("Encontrado: " + producto);
        System.out.println("Saldo:      " + saldo);
    }

    private void buscarPorNombre() {
        System.out.println("\n--- Buscar producto por nombre ---");
        String texto = leerTexto("Texto a buscar en el nombre: ");
        List<CataInvent> encontrados = servicio.buscarPorNombre(texto);
        if (encontrados.isEmpty()) {
            System.out.println("Ningun producto coincide con \"" + texto + "\".");
            return;
        }
        System.out.println("Coincidencias encontradas: " + encontrados.size());
        for (CataInvent producto : encontrados) {
            System.out.println("  " + producto);
        }
    }

    private void consultarInventario() {
        System.out.println("\n--- Inventario completo ---");
        List<CataInvent> productos = servicio.listarCatalogo();
        if (productos.isEmpty()) {
            System.out.println("El catalogo esta vacio. Registre productos con la opcion 1.");
            return;
        }

        System.out.println(LINEA_INVENTARIO);
        System.out.printf(Locale.US, "%-12s %-32s %10s %10s %12s%n",
                "CODIGO", "NOMBRE", "CANTIDAD", "PRECIO U.", "VALOR TOTAL");
        System.out.println(LINEA_INVENTARIO);

        double valorInventario = 0.0;
        for (CataInvent producto : productos) {
            SaldosInventario saldo;
            try {
                saldo = servicio.consultarSaldo(producto.getCodInvent());
            } catch (ProductoNoEncontradoException e) {
                // No puede pasar: el codigo viene del propio catalogo. Se captura
                // porque el metodo lo declara y el compilador lo exige.
                continue;
            }
            System.out.printf(Locale.US, "%-12s %-32s %10d %10.2f %12.2f%n",
                    producto.getCodInvent(),
                    recortar(producto.getNombreInventario(), 32),
                    saldo.getCantidadTotal(),
                    saldo.getPrecioU(),
                    saldo.getPrecioTotal());
            valorInventario += saldo.getPrecioTotal();
        }
        System.out.println(LINEA_INVENTARIO);
        System.out.printf(Locale.US, "%-12s %-32s %10s %10s %12.2f%n",
                "TOTAL", "", "", "", valorInventario);
    }

    private void registrarEntrada() throws ProductoNoEncontradoException, PersistenciaException {
        System.out.println("\n--- Registrar entrada ---");
        String codigo = leerTexto("Codigo del producto: ");
        int cantidad = leerEntero("Cantidad que ingresa: ");
        double precioUnitario = leerDecimal("Precio unitario de compra: ");
        servicio.registrarEntrada(codigo, cantidad, precioUnitario);

        SaldosInventario saldo = servicio.consultarSaldo(codigo);
        System.out.println("Entrada registrada. Nuevo saldo:");
        System.out.println("  " + saldo);
        System.out.println("  (el precio unitario es el costo promedio ponderado recalculado)");
    }

    private void registrarSalida()
            throws ProductoNoEncontradoException, StockInsuficienteException, PersistenciaException {
        System.out.println("\n--- Registrar salida ---");
        String codigo = leerTexto("Codigo del producto: ");
        SaldosInventario saldoActual = servicio.consultarSaldo(codigo);
        System.out.printf(Locale.US, "Disponible: %d unidades a %.4f cada una.%n",
                saldoActual.getCantidadTotal(), saldoActual.getPrecioU());

        int cantidad = leerEntero("Cantidad que sale: ");
        servicio.registrarSalida(codigo, cantidad);
        System.out.println("Salida registrada. Nuevo saldo:");
        System.out.println("  " + servicio.consultarSaldo(codigo));
    }

    private void consultarKardex() throws ProductoNoEncontradoException {
        System.out.println("\n--- Kardex del producto ---");
        String codigo = leerTexto("Codigo del producto: ");
        CataInvent producto = servicio.buscarProducto(codigo);
        List<Kardex> movimientos = servicio.consultarKardex(codigo);

        System.out.println("Producto: " + producto);
        if (movimientos.isEmpty()) {
            System.out.println("Este producto todavia no tiene movimientos.");
            return;
        }

        System.out.println(LINEA_KARDEX);
        System.out.printf(Locale.US, "%-6s %-12s %-9s %10s %12s %14s%n",
                "NO.", "FECHA", "TIPO", "CANTIDAD", "PRECIO U.", "IMPORTE");
        System.out.println(LINEA_KARDEX);
        for (Kardex movimiento : movimientos) {
            System.out.printf(Locale.US, "%-6d %-12s %-9s %10d %12.4f %14.2f%n",
                    movimiento.getCodKardex(),
                    movimiento.getFecha(),
                    movimiento.getTipoMov(),
                    movimiento.getCantidad(),
                    movimiento.getPrecio(),
                    movimiento.getCantidad() * movimiento.getPrecio());
        }
        System.out.println(LINEA_KARDEX);
        System.out.println("Saldo actual: " + servicio.consultarSaldo(codigo));
    }

    private void crearRespaldo() throws PersistenciaException {
        System.out.println("\n--- Crear respaldo de datos ---");
        List<Path> respaldos = servicio.respaldarDatos();
        System.out.println("Respaldos creados:");
        for (Path respaldo : respaldos) {
            System.out.println("  " + respaldo.toAbsolutePath());
        }
    }

    // ------------------------------------------------------------------
    // Lectura de datos desde el teclado
    // ------------------------------------------------------------------

    /**
     * Lee texto tal cual lo digita el usuario, incluso si viene vacio. Asi la
     * validacion le toca al modelo y se puede demostrar DatosInvalidosException.
     */
    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return entrada.nextLine();
    }

    /**
     * Lee un entero y vuelve a preguntar hasta que el texto sea convertible.
     *
     * SITUACION DEL ENUNCIADO: "datos invalidos". NumberFormatException es una
     * excepcion NO VERIFICADA del propio Java; se captura para no dejar que
     * tumbe el programa.
     */
    private int leerEntero(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return Integer.parseInt(texto.trim());
            } catch (NumberFormatException e) {
                System.out.println("  [DATO INVALIDO] \"" + texto
                        + "\" no es un numero entero. Intente de nuevo.");
            }
        }
    }

    private double leerDecimal(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                // Se acepta la coma como separador decimal porque el teclado
                // local la usa, pero parseDouble solo entiende el punto.
                return Double.parseDouble(texto.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("  [DATO INVALIDO] \"" + texto
                        + "\" no es un numero. Use punto decimal, por ejemplo 0.25.");
            }
        }
    }

    private void pausar() {
        System.out.print("\nPresione ENTER para volver al menu...");
        entrada.nextLine();
    }

    /**
     * Corta un texto largo para que no desalinee la tabla.
     */
    private String recortar(String texto, int largoMaximo) {
        return texto.length() <= largoMaximo ? texto : texto.substring(0, largoMaximo - 3) + "...";
    }
}
