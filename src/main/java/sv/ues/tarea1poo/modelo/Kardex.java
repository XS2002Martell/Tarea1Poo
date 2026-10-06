package sv.ues.tarea1poo.modelo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import sv.ues.tarea1poo.excepciones.DatosInvalidosException;

/**
 * Movimiento de inventario (entrada o salida). Equivale a la "tabla" kardex.txt.
 *
 * La clase es INMUTABLE: todos los atributos son final y no hay setters,
 * porque un movimiento ya registrado no se edita. Si hubo un error, se
 * registra otro movimiento que lo corrija.
 *
 * Formato de linea: codKardex;codInvent;fecha;tipoMov;cantidad;precio
 */
public class Kardex {

    public static final String SEPARADOR = ";";

    private final int codKardex;
    private final String codInvent;
    private final LocalDate fecha;
    private final TipoMovimiento tipoMov;
    private final int cantidad;
    private final double precio;

    public Kardex(int codKardex, String codInvent, LocalDate fecha,
            TipoMovimiento tipoMov, int cantidad, double precio) {
        if (codKardex <= 0) {
            throw new DatosInvalidosException("El codigo del movimiento debe ser mayor que cero (recibido: "
                    + codKardex + ").");
        }
        if (fecha == null) {
            throw new DatosInvalidosException("La fecha del movimiento no puede estar vacia.");
        }
        if (tipoMov == null) {
            throw new DatosInvalidosException("El tipo de movimiento no puede estar vacio.");
        }
        if (cantidad <= 0) {
            throw new DatosInvalidosException("La cantidad del movimiento debe ser mayor que cero (recibida: "
                    + cantidad + ").");
        }
        if (precio < 0) {
            throw new DatosInvalidosException("El precio del movimiento no puede ser negativo (recibido: "
                    + precio + ").");
        }
        this.codKardex = codKardex;
        this.codInvent = CataInvent.normalizarCodigo(codInvent);
        this.fecha = fecha;
        this.tipoMov = tipoMov;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public int getCodKardex() {
        return codKardex;
    }

    public String getCodInvent() {
        return codInvent;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public TipoMovimiento getTipoMov() {
        return tipoMov;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecio() {
        return precio;
    }

    /**
     * Convierte el objeto a una linea de kardex.txt. LocalDate.toString()
     * ya entrega el formato ISO yyyy-MM-dd.
     */
    public String aLinea() {
        return codKardex + SEPARADOR
                + codInvent + SEPARADOR
                + fecha + SEPARADOR
                + tipoMov + SEPARADOR
                + cantidad + SEPARADOR
                + precio;
    }

    public static Kardex desdeLinea(String linea) {
        if (linea == null) {
            throw new DatosInvalidosException("La linea esta nula.");
        }
        String[] campos = linea.split(SEPARADOR, -1);
        if (campos.length != 6) {
            throw new DatosInvalidosException(
                    "Se esperaban 6 campos (codKardex;codInvent;fecha;tipoMov;cantidad;precio) y llegaron "
                    + campos.length + ".");
        }
        try {
            return new Kardex(
                    Integer.parseInt(campos[0].trim()),
                    campos[1],
                    LocalDate.parse(campos[2].trim()),
                    TipoMovimiento.valueOf(campos[3].trim().toUpperCase()),
                    Integer.parseInt(campos[4].trim()),
                    Double.parseDouble(campos[5].trim()));

            // CONCEPTO: MULTI-CATCH. Un solo bloque atiende dos familias de error.
            // Nota importante para el video: NO se puede escribir
            // "catch (NumberFormatException | IllegalArgumentException e)" porque
            // NumberFormatException YA es hija de IllegalArgumentException y Java
            // prohibe listar alternativas emparentadas. IllegalArgumentException
            // cubre los dos casos: el parseInt/parseDouble fallido y el
            // TipoMovimiento.valueOf con un texto que no es ENTRADA ni SALIDA.
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new DatosInvalidosException("Un campo del movimiento no es valido: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s | %s | %-7s | cantidad: %d | precio: %.4f",
                fecha, codInvent, tipoMov, cantidad, precio);
    }
}
