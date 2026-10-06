package sv.ues.tarea1poo.excepciones;

/**
 * Se lanza cuando una salida pide mas unidades de las que hay en el saldo.
 *
 * CONCEPTO: excepcion VERIFICADA y PERSONALIZADA.
 * El mensaje siempre incluye la cantidad disponible para orientar al usuario.
 */
public class StockInsuficienteException extends InventarioException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
