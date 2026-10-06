package sv.ues.tarea1poo.persistencia;

import java.nio.file.Path;
import sv.ues.tarea1poo.modelo.CataInvent;

/**
 * Repositorio de la "tabla" catalogo.txt.
 *
 * Toda la logica de archivos la hereda de ArchivoTxt; aqui solo se indica
 * cual es el archivo y como se traduce una linea a objeto y al reves.
 */
public class CataInventRepo extends ArchivoTxt<CataInvent> {

    public static final String NOMBRE_ARCHIVO = "catalogo.txt";

    public CataInventRepo(Path carpetaDatos) {
        // CONCEPTO: NIO.2 - Path.resolve arma "carpetaDatos/catalogo.txt" sin
        // tener que concatenar separadores de carpeta a mano.
        super(carpetaDatos.resolve(NOMBRE_ARCHIVO));
    }

    @Override
    protected CataInvent parsear(String linea) {
        return CataInvent.desdeLinea(linea);
    }

    @Override
    protected String serializar(CataInvent entidad) {
        return entidad.aLinea();
    }
}
