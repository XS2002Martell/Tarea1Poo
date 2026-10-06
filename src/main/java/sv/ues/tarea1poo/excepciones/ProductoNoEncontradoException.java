package sv.ues.tarea1poo.excepciones;

/**
 * Se lanza cuando se busca un codigo de producto que no existe en el catalogo.
 *
 * CONCEPTO: excepcion VERIFICADA y PERSONALIZADA.
 * Situacion del enunciado cubierta: "producto inexistente".
 */
public class ProductoNoEncontradoException extends InventarioException {

    public ProductoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
