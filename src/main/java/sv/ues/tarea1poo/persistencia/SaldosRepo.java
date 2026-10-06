package sv.ues.tarea1poo.persistencia;

import java.nio.file.Path;
import sv.ues.tarea1poo.modelo.SaldosInventario;

/**
 * Repositorio de la "tabla" saldos.txt.
 */
public class SaldosRepo extends ArchivoTxt<SaldosInventario> {

    public static final String NOMBRE_ARCHIVO = "saldos.txt";

    public SaldosRepo(Path carpetaDatos) {
        super(carpetaDatos.resolve(NOMBRE_ARCHIVO));
    }

    @Override
    protected SaldosInventario parsear(String linea) {
        return SaldosInventario.desdeLinea(linea);
    }

    @Override
    protected String serializar(SaldosInventario entidad) {
        return entidad.aLinea();
    }
}
