package sv.ues.tarea1poo.persistencia;

import java.nio.file.Path;
import sv.ues.tarea1poo.modelo.Kardex;

/**
 * Repositorio de la "tabla" kardex.txt.
 */
public class KardexRepo extends ArchivoTxt<Kardex> {

    public static final String NOMBRE_ARCHIVO = "kardex.txt";

    public KardexRepo(Path carpetaDatos) {
        super(carpetaDatos.resolve(NOMBRE_ARCHIVO));
    }

    @Override
    protected Kardex parsear(String linea) {
        return Kardex.desdeLinea(linea);
    }

    @Override
    protected String serializar(Kardex entidad) {
        return entidad.aLinea();
    }
}
