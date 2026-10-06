package sv.ues.tarea1poo.excepciones;

/**
 * Excepcion base de todo el sistema de inventario.
 *
 * CONCEPTO: excepcion VERIFICADA (checked). Hereda de Exception y NO de
 * RuntimeException, por eso el compilador obliga a declararla con throws
 * o a capturarla con try/catch. Se usa para errores de negocio que quien
 * llama al metodo puede y debe manejar.
 */
public class InventarioException extends Exception {

    public InventarioException(String mensaje) {
        super(mensaje);
    }

    // CONCEPTO: encadenamiento de excepciones. El parametro causa conserva
    // la excepcion original para no perder el rastro del error real.
    public InventarioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
