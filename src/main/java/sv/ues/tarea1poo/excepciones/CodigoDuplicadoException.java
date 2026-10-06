package sv.ues.tarea1poo.excepciones;

/**
 * Se lanza al registrar un producto cuyo codigo ya existe en el catalogo.
 *
 * CONCEPTO: excepcion VERIFICADA y PERSONALIZADA.
 * Situacion del enunciado cubierta: "codigo duplicado".
 */
public class CodigoDuplicadoException extends InventarioException {

    public CodigoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
