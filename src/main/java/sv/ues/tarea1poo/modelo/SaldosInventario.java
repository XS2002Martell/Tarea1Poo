package sv.ues.tarea1poo.modelo;

import java.util.Locale;
import sv.ues.tarea1poo.excepciones.DatosInvalidosException;
import sv.ues.tarea1poo.excepciones.StockInsuficienteException;

/**
 * Saldo acumulado de un producto. Equivale a la "tabla" saldos.txt.
 *
 * Metodo de valuacion: COSTO PROMEDIO PONDERADO. El precio unitario (precioU)
 * nunca se digita, siempre se calcula como precioTotal / cantidadTotal.
 *
 * Formato de linea: codSaldos;codInvent;cantidadTotal;precioTotal;precioU
 */
public class SaldosInventario {

    public static final String SEPARADOR = ";";
    private static final double FACTOR_REDONDEO = 10000.0;

    private final int codSaldos;
    private final String codInvent;
    private int cantidadTotal;
    private double precioTotal;
    private double precioU;

    public SaldosInventario(int codSaldos, String codInvent, int cantidadTotal,
            double precioTotal, double precioU) {
        if (codSaldos <= 0) {
            throw new DatosInvalidosException("El codigo del saldo debe ser mayor que cero (recibido: "
                    + codSaldos + ").");
        }
        if (cantidadTotal < 0) {
            throw new DatosInvalidosException("La cantidad total no puede ser negativa (recibida: "
                    + cantidadTotal + ").");
        }
        if (precioTotal < 0) {
            throw new DatosInvalidosException("El precio total no puede ser negativo (recibido: "
                    + precioTotal + ").");
        }
        if (precioU < 0) {
            throw new DatosInvalidosException("El precio unitario no puede ser negativo (recibido: "
                    + precioU + ").");
        }
        this.codSaldos = codSaldos;
        this.codInvent = CataInvent.normalizarCodigo(codInvent);
        this.cantidadTotal = cantidadTotal;
        this.precioTotal = redondear(precioTotal);
        this.precioU = redondear(precioU);
    }

    /**
     * Crea el saldo inicial en cero de un producto recien registrado.
     */
    public SaldosInventario(int codSaldos, String codInvent) {
        this(codSaldos, codInvent, 0, 0.0, 0.0);
    }

    /**
     * Suma una compra al saldo y recalcula el costo promedio ponderado.
     */
    public void aplicarEntrada(int cantidad, double precioUnitario) {
        if (cantidad <= 0) {
            throw new DatosInvalidosException("La cantidad de la entrada debe ser mayor que cero (recibida: "
                    + cantidad + ").");
        }
        if (precioUnitario <= 0) {
            throw new DatosInvalidosException("El precio unitario de la entrada debe ser mayor que cero (recibido: "
                    + precioUnitario + ").");
        }
        precioTotal = redondear(precioTotal + (cantidad * precioUnitario));
        cantidadTotal += cantidad;
        precioU = redondear(precioTotal / cantidadTotal);
    }

    /**
     * Descarga unidades del saldo al costo promedio vigente.
     *
     * CONCEPTO: throw de una excepcion VERIFICADA. Por eso el metodo declara
     * throws y quien lo llama esta obligado a manejarla.
     */
    public void aplicarSalida(int cantidad) throws StockInsuficienteException {
        if (cantidad <= 0) {
            throw new DatosInvalidosException("La cantidad de la salida debe ser mayor que cero (recibida: "
                    + cantidad + ").");
        }
        if (cantidad > cantidadTotal) {
            throw new StockInsuficienteException("Stock insuficiente para " + codInvent
                    + ": se piden " + cantidad + " y solo hay " + cantidadTotal + " disponibles.");
        }
        precioTotal = redondear(precioTotal - (cantidad * precioU));
        cantidadTotal -= cantidad;
        if (cantidadTotal == 0) {
            // Sin unidades no hay valor acumulado, pero se conserva el ultimo
            // precioU como referencia del costo historico.
            precioTotal = 0.0;
        } else if (precioTotal < 0) {
            // Proteccion contra un residuo negativo minimo por punto flotante.
            precioTotal = 0.0;
        }
    }

    /**
     * Recorta el valor a 4 decimales. Sin esto, restar y dividir doubles una y
     * otra vez va acumulando errores como 0.30000000000000004.
     */
    private double redondear(double valor) {
        return Math.round(valor * FACTOR_REDONDEO) / FACTOR_REDONDEO;
    }

    /**
     * Devuelve una copia independiente del saldo. Sirve para guardar el estado
     * anterior y poder revertirlo si falla la escritura en disco.
     */
    public SaldosInventario copiar() {
        return new SaldosInventario(codSaldos, codInvent, cantidadTotal, precioTotal, precioU);
    }

    public int getCodSaldos() {
        return codSaldos;
    }

    public String getCodInvent() {
        return codInvent;
    }

    public int getCantidadTotal() {
        return cantidadTotal;
    }

    public double getPrecioTotal() {
        return precioTotal;
    }

    public double getPrecioU() {
        return precioU;
    }

    /**
     * Convierte el objeto a una linea de saldos.txt.
     *
     * Se concatena con Double.toString (implicito en el +) y NO con
     * String.format, porque format usa el separador decimal del idioma del
     * sistema y en es_SV escribiria "0,25" en lugar de "0.25".
     */
    public String aLinea() {
        return codSaldos + SEPARADOR
                + codInvent + SEPARADOR
                + cantidadTotal + SEPARADOR
                + precioTotal + SEPARADOR
                + precioU;
    }

    public static SaldosInventario desdeLinea(String linea) {
        if (linea == null) {
            throw new DatosInvalidosException("La linea esta nula.");
        }
        String[] campos = linea.split(SEPARADOR, -1);
        if (campos.length != 5) {
            throw new DatosInvalidosException(
                    "Se esperaban 5 campos (codSaldos;codInvent;cantidadTotal;precioTotal;precioU) y llegaron "
                    + campos.length + ".");
        }
        try {
            return new SaldosInventario(
                    Integer.parseInt(campos[0].trim()),
                    campos[1],
                    Integer.parseInt(campos[2].trim()),
                    Double.parseDouble(campos[3].trim()),
                    Double.parseDouble(campos[4].trim()));
        } catch (NumberFormatException e) {
            // CONCEPTO: encadenamiento. Se guarda e como causa para no perder
            // el detalle de cual texto no se pudo convertir a numero.
            throw new DatosInvalidosException("Un campo numerico del saldo no es valido: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        // Locale.US fuerza el punto decimal para que la consola se vea igual
        // que los archivos de datos.
        return String.format(Locale.US, "%s | cantidad: %d | precio unitario: %.4f | valor total: %.4f",
                codInvent, cantidadTotal, precioU, precioTotal);
    }
}
