package sv.ues.tarea1poo.modelo;

import java.util.regex.Pattern;
import sv.ues.tarea1poo.excepciones.DatosInvalidosException;

/**
 * Representa un producto del catalogo. Equivale a la "tabla" catalogo.txt.
 *
 * Formato de linea: codInvent;nombreInventario
 */
public class CataInvent {

    public static final String SEPARADOR = ";";
    public static final int LARGO_MAXIMO_NOMBRE = 60;

    private static final Pattern PATRON_CODIGO = Pattern.compile("^[A-Z0-9-]{3,15}$");

    // El codigo es el identificador del producto: final porque nunca cambia.
    private final String codInvent;
    private String nombreInventario;

    public CataInvent(String codInvent, String nombreInventario) {
        this.codInvent = normalizarCodigo(codInvent);
        // Se usa el setter para no repetir la validacion del nombre.
        setNombreInventario(nombreInventario);
    }

    /**
     * Normaliza y valida un codigo de producto. Es publico y estatico porque
     * SaldosInventario y Kardex tambien necesitan normalizar su codInvent.
     *
     * CONCEPTO: throw de una excepcion NO VERIFICADA.
     */
    public static String normalizarCodigo(String codInvent) {
        if (codInvent == null || codInvent.isBlank()) {
            throw new DatosInvalidosException("El codigo del producto no puede estar vacio.");
        }
        String codigoNormalizado = codInvent.trim().toUpperCase();
        if (!PATRON_CODIGO.matcher(codigoNormalizado).matches()) {
            throw new DatosInvalidosException("El codigo \"" + codigoNormalizado
                    + "\" es invalido. Debe tener de 3 a 15 caracteres, solo letras, numeros y guiones.");
        }
        return codigoNormalizado;
    }

    public String getCodInvent() {
        return codInvent;
    }

    public String getNombreInventario() {
        return nombreInventario;
    }

    /**
     * Unico dato modificable del producto.
     */
    public void setNombreInventario(String nombreInventario) {
        if (nombreInventario == null || nombreInventario.isBlank()) {
            throw new DatosInvalidosException("El nombre del producto no puede estar vacio.");
        }
        String nombreLimpio = nombreInventario.trim();
        if (nombreLimpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new DatosInvalidosException("El nombre no puede pasar de "
                    + LARGO_MAXIMO_NOMBRE + " caracteres (recibidos: " + nombreLimpio.length() + ").");
        }
        // El separador partiria la linea del archivo en campos de mas.
        if (nombreLimpio.contains(SEPARADOR)) {
            throw new DatosInvalidosException("El nombre no puede contener el caracter \"" + SEPARADOR + "\".");
        }
        this.nombreInventario = nombreLimpio;
    }

    /**
     * Convierte el objeto a una linea de catalogo.txt.
     */
    public String aLinea() {
        return codInvent + SEPARADOR + nombreInventario;
    }

    /**
     * Reconstruye un producto a partir de una linea del archivo.
     *
     * CONCEPTO: validacion de datos leidos de disco. Cualquier problema se
     * reporta como DatosInvalidosException para que el repositorio decida
     * si descarta la linea o aborta la carga.
     */
    public static CataInvent desdeLinea(String linea) {
        if (linea == null) {
            throw new DatosInvalidosException("La linea esta nula.");
        }
        // El -1 conserva los campos vacios del final; sin el, "PRD-001;" daria 1 campo.
        String[] campos = linea.split(SEPARADOR, -1);
        if (campos.length != 2) {
            throw new DatosInvalidosException("Se esperaban 2 campos (codInvent;nombreInventario) y llegaron "
                    + campos.length + ".");
        }
        return new CataInvent(campos[0], campos[1]);
    }

    @Override
    public String toString() {
        return codInvent + " - " + nombreInventario;
    }
}
