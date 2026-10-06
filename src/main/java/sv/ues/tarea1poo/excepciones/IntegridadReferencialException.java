package sv.ues.tarea1poo.excepciones;

/**
 * Se lanza al intentar eliminar un producto que ya tiene movimientos en el
 * kardex. Borrarlo dejaria movimientos apuntando a un producto inexistente.
 *
 * CONCEPTO: excepcion VERIFICADA y PERSONALIZADA.
 * Equivale a la restriccion de llave foranea de una base de datos.
 */
public class IntegridadReferencialException extends InventarioException {

    public IntegridadReferencialException(String mensaje) {
        super(mensaje);
    }
}
