package sv.ues.tarea1poo.servicio;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sv.ues.tarea1poo.excepciones.CodigoDuplicadoException;
import sv.ues.tarea1poo.excepciones.IntegridadReferencialException;
import sv.ues.tarea1poo.excepciones.PersistenciaException;
import sv.ues.tarea1poo.excepciones.ProductoNoEncontradoException;
import sv.ues.tarea1poo.excepciones.StockInsuficienteException;
import sv.ues.tarea1poo.modelo.CataInvent;
import sv.ues.tarea1poo.modelo.Kardex;
import sv.ues.tarea1poo.modelo.SaldosInventario;
import sv.ues.tarea1poo.modelo.TipoMovimiento;
import sv.ues.tarea1poo.persistencia.CataInventRepo;
import sv.ues.tarea1poo.persistencia.KardexRepo;
import sv.ues.tarea1poo.persistencia.SaldosRepo;

/**
 * Reglas de negocio del inventario. Coordina las tres "tablas" como si fueran
 * una transaccion de base de datos: valida, modifica la memoria, guarda en
 * disco y, si el guardado falla, DEVUELVE la memoria a como estaba.
 *
 * La vista nunca toca archivos: solo llama metodos de esta clase.
 */
public class InventarioService {

    private final CataInventRepo cataInventRepo;
    private final SaldosRepo saldosRepo;
    private final KardexRepo kardexRepo;

    // LinkedHashMap conserva el orden en que se registraron los productos, asi
    // los listados de la consola salen siempre igual.
    private final Map<String, CataInvent> catalogo = new LinkedHashMap<>();
    private final Map<String, SaldosInventario> saldos = new LinkedHashMap<>();
    private final List<Kardex> listaKardex = new ArrayList<>();

    private final List<String> advertencias = new ArrayList<>();

    /**
     * CONCEPTO: throws. El constructor no puede trabajar si no logra leer los
     * archivos, asi que delega el problema a Main, que decide si cierra el
     * programa.
     */
    public InventarioService(Path carpetaDatos) throws PersistenciaException {
        this.cataInventRepo = new CataInventRepo(carpetaDatos);
        this.saldosRepo = new SaldosRepo(carpetaDatos);
        this.kardexRepo = new KardexRepo(carpetaDatos);
        cargarDatos();
    }

    /**
     * Lee las tres tablas y verifica que sean consistentes entre si, igual que
     * haria una base de datos al revisar sus llaves foraneas.
     */
    private void cargarDatos() throws PersistenciaException {
        catalogo.clear();
        saldos.clear();
        listaKardex.clear();
        advertencias.clear();

        List<CataInvent> productosLeidos = cataInventRepo.leerTodos();
        List<SaldosInventario> saldosLeidos = saldosRepo.leerTodos();
        List<Kardex> movimientosLeidos = kardexRepo.leerTodos();

        // Advertencias de los repositorios: archivos creados, vacios o con
        // lineas corruptas descartadas.
        advertencias.addAll(cataInventRepo.getAdvertencias());
        advertencias.addAll(saldosRepo.getAdvertencias());
        advertencias.addAll(kardexRepo.getAdvertencias());

        for (CataInvent producto : productosLeidos) {
            if (catalogo.containsKey(producto.getCodInvent())) {
                advertencias.add("Codigo repetido en el catalogo (" + producto.getCodInvent()
                        + "); se conservo el primero.");
                continue;
            }
            catalogo.put(producto.getCodInvent(), producto);
        }

        for (SaldosInventario saldo : saldosLeidos) {
            if (!catalogo.containsKey(saldo.getCodInvent())) {
                advertencias.add("Saldo descartado: el producto " + saldo.getCodInvent()
                        + " no existe en el catalogo.");
                continue;
            }
            if (saldos.containsKey(saldo.getCodInvent())) {
                advertencias.add("Saldo repetido para " + saldo.getCodInvent()
                        + "; se conservo el primero.");
                continue;
            }
            saldos.put(saldo.getCodInvent(), saldo);
        }

        for (Kardex movimiento : movimientosLeidos) {
            if (!catalogo.containsKey(movimiento.getCodInvent())) {
                advertencias.add("Movimiento " + movimiento.getCodKardex()
                        + " descartado: el producto " + movimiento.getCodInvent()
                        + " no existe en el catalogo.");
                continue;
            }
            listaKardex.add(movimiento);
        }

        // Un producto del catalogo siempre debe tener su fila de saldo.
        for (CataInvent producto : catalogo.values()) {
            if (!saldos.containsKey(producto.getCodInvent())) {
                saldos.put(producto.getCodInvent(),
                        new SaldosInventario(siguienteCodigoSaldo(), producto.getCodInvent()));
                advertencias.add("El producto " + producto.getCodInvent()
                        + " no tenia saldo registrado; se creo en cero.");
            }
        }
    }

    public List<String> getAdvertencias() {
        return new ArrayList<>(advertencias);
    }

    // ------------------------------------------------------------------
    // Catalogo
    // ------------------------------------------------------------------

    /**
     * Registra un producto nuevo y le crea su saldo en cero.
     *
     * CONCEPTO: throws con las excepciones VERIFICADAS que realmente puede
     * lanzar. Tambien puede lanzar DatosInvalidosException, pero al ser NO
     * VERIFICADA no se declara.
     */
    public void registrarProducto(String codInvent, String nombre)
            throws CodigoDuplicadoException, PersistenciaException {

        // El constructor valida y normaliza; si el dato esta mal, lanza
        // DatosInvalidosException antes de tocar nada.
        CataInvent producto = new CataInvent(codInvent, nombre);
        String codigo = producto.getCodInvent();

        if (catalogo.containsKey(codigo)) {
            // SITUACION DEL ENUNCIADO: "codigo duplicado".
            throw new CodigoDuplicadoException("El producto " + codigo
                    + " ya existe en el catalogo (" + catalogo.get(codigo).getNombreInventario() + ").");
        }

        SaldosInventario saldoInicial = new SaldosInventario(siguienteCodigoSaldo(), codigo);
        catalogo.put(codigo, producto);
        saldos.put(codigo, saldoInicial);

        try {
            guardarCatalogo();
            guardarSaldos();
        } catch (PersistenciaException e) {
            // REVERSION: si no se pudo escribir, la memoria vuelve a su estado
            // anterior para que no quede un producto que el archivo no conoce.
            catalogo.remove(codigo);
            saldos.remove(codigo);
            // CONCEPTO: relanzar la misma excepcion. El servicio limpia su
            // estado, pero la vista debe enterarse del fallo.
            throw e;
        }
    }

    public void modificarProducto(String codInvent, String nuevoNombre)
            throws ProductoNoEncontradoException, PersistenciaException {

        CataInvent producto = buscarProducto(codInvent);
        String nombreAnterior = producto.getNombreInventario();
        producto.setNombreInventario(nuevoNombre);

        try {
            guardarCatalogo();
        } catch (PersistenciaException e) {
            producto.setNombreInventario(nombreAnterior);
            throw e;
        }
    }

    public void eliminarProducto(String codInvent)
            throws ProductoNoEncontradoException, IntegridadReferencialException, PersistenciaException {

        CataInvent producto = buscarProducto(codInvent);
        String codigo = producto.getCodInvent();

        long cantidadMovimientos = listaKardex.stream()
                .filter(movimiento -> movimiento.getCodInvent().equals(codigo))
                .count();
        if (cantidadMovimientos > 0) {
            // Igual que una restriccion de llave foranea: borrar el producto
            // dejaria movimientos huerfanos en el kardex.
            throw new IntegridadReferencialException("No se puede eliminar " + codigo
                    + " porque tiene " + cantidadMovimientos + " movimiento(s) en el kardex.");
        }

        SaldosInventario saldoAnterior = saldos.get(codigo);
        catalogo.remove(codigo);
        saldos.remove(codigo);

        try {
            guardarCatalogo();
            guardarSaldos();
        } catch (PersistenciaException e) {
            catalogo.put(codigo, producto);
            if (saldoAnterior != null) {
                saldos.put(codigo, saldoAnterior);
            }
            throw e;
        }
    }

    public CataInvent buscarProducto(String codInvent) throws ProductoNoEncontradoException {
        // normalizarCodigo puede lanzar DatosInvalidosException si viene vacio
        // o con formato invalido.
        String codigo = CataInvent.normalizarCodigo(codInvent);
        CataInvent producto = catalogo.get(codigo);
        if (producto == null) {
            // SITUACION DEL ENUNCIADO: "producto inexistente".
            throw new ProductoNoEncontradoException("El producto " + codigo + " no existe en el catalogo.");
        }
        return producto;
    }

    /**
     * Busca por coincidencia parcial del nombre, sin distinguir mayusculas.
     * No lanza excepcion: una busqueda sin resultados es un resultado valido.
     */
    public List<CataInvent> buscarPorNombre(String texto) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        List<CataInvent> encontrados = new ArrayList<>();
        for (CataInvent producto : catalogo.values()) {
            if (producto.getNombreInventario().toLowerCase().contains(buscado)) {
                encontrados.add(producto);
            }
        }
        return encontrados;
    }

    public List<CataInvent> listarCatalogo() {
        return new ArrayList<>(catalogo.values());
    }

    // ------------------------------------------------------------------
    // Movimientos
    // ------------------------------------------------------------------

    /**
     * Registra una compra: suma al saldo (recalculando el costo promedio) y
     * deja constancia en el kardex.
     */
    public void registrarEntrada(String codInvent, int cantidad, double precioUnitario)
            throws ProductoNoEncontradoException, PersistenciaException {

        CataInvent producto = buscarProducto(codInvent);
        String codigo = producto.getCodInvent();
        SaldosInventario saldo = saldos.get(codigo);

        // Copia de seguridad del saldo ANTES de modificarlo, para poder revertir.
        SaldosInventario saldoAnterior = saldo.copiar();

        saldo.aplicarEntrada(cantidad, precioUnitario);
        Kardex movimiento = new Kardex(siguienteCodigoKardex(), codigo, LocalDate.now(),
                TipoMovimiento.ENTRADA, cantidad, precioUnitario);
        listaKardex.add(movimiento);

        try {
            guardarSaldos();
            guardarKardex();
        } catch (PersistenciaException e) {
            // REVERSION: se devuelve el saldo viejo y se quita el movimiento,
            // para que memoria y archivos vuelvan a coincidir.
            saldos.put(codigo, saldoAnterior);
            listaKardex.remove(movimiento);
            throw e;
        }
    }

    /**
     * Registra una venta o consumo. El movimiento se valua al costo promedio
     * vigente, no a un precio digitado.
     */
    public void registrarSalida(String codInvent, int cantidad)
            throws ProductoNoEncontradoException, StockInsuficienteException, PersistenciaException {

        CataInvent producto = buscarProducto(codInvent);
        String codigo = producto.getCodInvent();
        SaldosInventario saldo = saldos.get(codigo);

        SaldosInventario saldoAnterior = saldo.copiar();
        double precioVigente = saldo.getPrecioU();

        // Puede lanzar StockInsuficienteException; en ese caso el saldo no se
        // toco y no hay nada que revertir.
        saldo.aplicarSalida(cantidad);
        Kardex movimiento = new Kardex(siguienteCodigoKardex(), codigo, LocalDate.now(),
                TipoMovimiento.SALIDA, cantidad, precioVigente);
        listaKardex.add(movimiento);

        try {
            guardarSaldos();
            guardarKardex();
        } catch (PersistenciaException e) {
            saldos.put(codigo, saldoAnterior);
            listaKardex.remove(movimiento);
            throw e;
        }
    }

    public SaldosInventario consultarSaldo(String codInvent) throws ProductoNoEncontradoException {
        CataInvent producto = buscarProducto(codInvent);
        return saldos.get(producto.getCodInvent());
    }

    public List<SaldosInventario> listarSaldos() {
        return new ArrayList<>(saldos.values());
    }

    public List<Kardex> consultarKardex(String codInvent) throws ProductoNoEncontradoException {
        CataInvent producto = buscarProducto(codInvent);
        String codigo = producto.getCodInvent();
        List<Kardex> movimientos = new ArrayList<>();
        for (Kardex movimiento : listaKardex) {
            if (movimiento.getCodInvent().equals(codigo)) {
                movimientos.add(movimiento);
            }
        }
        return movimientos;
    }

    // ------------------------------------------------------------------
    // Respaldo y utilidades internas
    // ------------------------------------------------------------------

    /**
     * Copia las tres tablas a datos/respaldos.
     *
     * @return las rutas de los tres respaldos creados.
     */
    public List<Path> respaldarDatos() throws PersistenciaException {
        List<Path> rutasCreadas = new ArrayList<>();
        rutasCreadas.add(cataInventRepo.respaldar());
        rutasCreadas.add(saldosRepo.respaldar());
        rutasCreadas.add(kardexRepo.respaldar());
        return rutasCreadas;
    }

    private void guardarCatalogo() throws PersistenciaException {
        cataInventRepo.guardarTodos(new ArrayList<>(catalogo.values()));
    }

    private void guardarSaldos() throws PersistenciaException {
        saldosRepo.guardarTodos(new ArrayList<>(saldos.values()));
    }

    private void guardarKardex() throws PersistenciaException {
        kardexRepo.guardarTodos(new ArrayList<>(listaKardex));
    }

    /**
     * Autoincremental: el mayor codigo existente mas uno.
     */
    private int siguienteCodigoSaldo() {
        int mayor = 0;
        for (SaldosInventario saldo : saldos.values()) {
            mayor = Math.max(mayor, saldo.getCodSaldos());
        }
        return mayor + 1;
    }

    private int siguienteCodigoKardex() {
        int mayor = 0;
        for (Kardex movimiento : listaKardex) {
            mayor = Math.max(mayor, movimiento.getCodKardex());
        }
        return mayor + 1;
    }
}
