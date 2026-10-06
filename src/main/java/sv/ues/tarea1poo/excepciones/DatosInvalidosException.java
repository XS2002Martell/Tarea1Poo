package sv.ues.tarea1poo.excepciones;

/**
 * Se lanza cuando un dato no cumple las reglas de validacion: texto vacio,
 * cantidad negativa, codigo con formato incorrecto, linea del archivo mal
 * formada, etc.
 *
 * CONCEPTO: excepcion NO VERIFICADA (unchecked). Hereda de RuntimeException,
 * asi que el compilador NO obliga a declararla con throws. Se usa a proposito
 * para las validaciones, porque un dato invalido es un error de uso que puede
 * aparecer en cualquier constructor del modelo y llenar de throws cada firma
 * haria el codigo ilegible.
 *
 * Situacion del enunciado cubierta: "datos invalidos".
 */
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }

    public DatosInvalidosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
