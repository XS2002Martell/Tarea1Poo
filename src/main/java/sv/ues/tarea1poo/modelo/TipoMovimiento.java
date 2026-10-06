package sv.ues.tarea1poo.modelo;

/**
 * Tipos de movimiento que puede tener el kardex.
 *
 * Al ser un enum, solo existen estos dos valores posibles: es imposible
 * guardar un movimiento con un tipo invalido. Si el archivo trae otro texto,
 * TipoMovimiento.valueOf lanza IllegalArgumentException.
 */
public enum TipoMovimiento {
    ENTRADA,
    SALIDA
}
