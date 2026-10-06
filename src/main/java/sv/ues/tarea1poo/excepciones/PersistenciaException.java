package sv.ues.tarea1poo.excepciones;

/**
 * Se lanza cuando falla una operacion sobre los archivos de datos.
 *
 * CONCEPTO: excepcion VERIFICADA y PERSONALIZADA. Su papel principal es
 * ENVOLVER la IOException de bajo nivel: el servicio y la vista trabajan
 * con una excepcion del dominio, pero la causa original queda guardada y
 * se puede recuperar con getCause().
 *
 * Situaciones del enunciado cubiertas: "error durante lectura" y
 * "error durante escritura".
 */
public class PersistenciaException extends InventarioException {

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
