# Tarea1Poo — Sistema de Inventario con Kardex

Tarea 1 de Programación Orientada a Objetos — Universidad de El Salvador.

**Clave 2:** manejo de excepciones y persistencia con Java I/O y NIO.2.
**Caso de estudio:** sistema de inventario para una pequeña empresa.

La "base de datos" se simula con tres archivos de texto (`catalogo.txt`,
`saldos.txt`, `kardex.txt`), un registro por línea y campos separados por `;`.
El método de valuación es **costo promedio ponderado**: el precio unitario
nunca se digita, siempre se calcula.

## Requisitos

- JDK 21
- NetBeans 22 (trae Maven incluido, no hace falta instalarlo aparte)
- Sin dependencias externas: solo la biblioteca estándar de Java

## Cómo abrir en NetBeans 22

1. `File` → `Open Project...`
2. Seleccionar la carpeta `Tarea1Poo` (la que contiene el `pom.xml`).
   NetBeans la reconoce como proyecto Maven por el ícono de la cajita café.
3. Si avisa que falta una plataforma Java, ir a `Tools` → `Java Platforms` y
   agregar el JDK 21.

## Cómo ejecutar

**Desde NetBeans:** clic derecho en el proyecto → `Run`, o la tecla `F6`.
La entrada del teclado se escribe en la ventana `Output`.

**Desde la terminal:**

```bash
mvn clean compile
mvn exec:java
```

O bien, sin el plugin exec:

```bash
mvn clean compile
java -cp target/classes sv.ues.tarea1poo.Main
```

La carpeta `datos/` **no se crea a mano**: el programa la crea en la primera
ejecución junto con los tres archivos vacíos, y lo avisa en pantalla.

## Datos de ejemplo

En `datos_ejemplo/` hay cuatro productos con nueve movimientos ya cuadrados.
Para usarlos en la demostración, copiar los tres `.txt` a `datos/`:

```bash
cp datos_ejemplo/*.txt datos/
```

Para ver en cambio la creación de la carpeta desde cero, borrar `datos/` antes
de ejecutar.

## Diagramas

`docs/Tarea1Poo-diagramas.drawio` contiene cuatro páginas (pestañas abajo):

1. **Diagrama de clases** — las 16 clases agrupadas por paquete, con atributos,
   métodos, cláusulas `throws` y las relaciones entre ellas.
2. **Jerarquía de excepciones** — el árbol completo desde `Throwable`, marcando
   qué es verificada y qué no, y dónde se lanza cada una.
3. **Manejo de excepciones** — dónde nace cada excepción (`throw`), cómo sube
   (`throws`) y dónde se captura (`catch`), con el número de línea de cada
   bloque `try`, `catch` y `finally`.
4. **Casos de uso** — el encargado de inventario y las 11 funciones del sistema,
   con el comportamiento compartido como `«include»` y cada flujo de error como
   `«extend»`.

Se abre en [app.diagrams.net](https://app.diagrams.net) con `File` → `Open From`
→ `Device`, o directamente en NetBeans/VS Code con una extensión de draw.io.

## Estructura del proyecto

```
src/main/java/sv/ues/tarea1poo/
├── Main.java                 punto de entrada
├── modelo/                   CataInvent, SaldosInventario, Kardex, TipoMovimiento
├── persistencia/             ArchivoTxt<T> y los tres repositorios
├── servicio/                 InventarioService (reglas de negocio)
├── excepciones/              las siete excepciones del sistema
└── vista/                    MenuConsola
```

Arquitectura en capas: `vista` → `servicio` → `persistencia` → archivos.
La vista nunca abre un archivo; los repositorios son los únicos que leen y
escriben.

## Mapa de conceptos: dónde ver cada uno

| Concepto | Dónde encontrarlo |
|---|---|
| Excepción verificada | `excepciones/InventarioException` y sus cinco subclases |
| Excepción no verificada | `excepciones/DatosInvalidosException`, más `NumberFormatException` en `MenuConsola.leerEntero` |
| Excepciones personalizadas | todo el paquete `excepciones` |
| `try/catch` | `MenuConsola.iniciar`, `ArchivoTxt.leerTodos`, `Main.main` |
| `finally` | `ArchivoTxt.guardarTodos` (borra el `.tmp`) y `ArchivoTxt.respaldar` (cierra a mano) |
| try-with-resources | `ArchivoTxt.leerTodos` y `ArchivoTxt.guardarTodos` |
| multi-catch | `MenuConsola.iniciar` y `Kardex.desdeLinea` |
| `throw` | `CataInvent.normalizarCodigo`, `SaldosInventario.aplicarSalida`, `InventarioService` |
| `throws` | firmas de `InventarioService` y de `ArchivoTxt` |
| Encadenamiento de excepciones | `PersistenciaException` y `DatosInvalidosException` con `(mensaje, causa)`; se lee con `getCause()` |
| Java I/O clásico | `ArchivoTxt.respaldar` (`File`, `FileReader`, `FileWriter`) |
| NIO.2 (`Path`, `Files`) | el resto de `ArchivoTxt`, y `Paths.get` en `Main` |
| Persistencia | tres archivos txt: se cargan al iniciar y se guardan tras cada operación |

## Las siete situaciones del enunciado

| Situación | Cómo provocarla en la demostración |
|---|---|
| Producto inexistente | opción 4 con un código que no existe, por ejemplo `PRD-999` |
| Código duplicado | opción 1 dos veces con el mismo código |
| Datos inválidos | opción 1 con el nombre vacío, o escribir letras donde se pide una cantidad |
| Archivo inexistente | borrar la carpeta `datos/` y ejecutar |
| Archivo vacío | dejar los tres `.txt` en cero bytes y ejecutar |
| Error durante lectura | escribir a mano una línea mal formada en `kardex.txt`: se descarta, se avisa y las demás sí cargan |
| Error durante escritura | marcar `saldos.txt` como solo lectura e intentar registrar una entrada |

## Detalles de implementación que vale la pena señalar

- **Escritura segura:** `guardarTodos` nunca escribe directo sobre el archivo
  real. Escribe en un `.tmp` y solo al terminar bien lo reemplaza con
  `Files.move(..., ATOMIC_MOVE)`. Si el programa se corta a la mitad, el
  archivo bueno queda intacto.
- **Reversión ante errores:** antes de guardar, el servicio copia el saldo con
  `copiar()`. Si la escritura falla, devuelve la memoria a su estado anterior y
  relanza la excepción, para que memoria y archivos no queden desfasados.
- **Una línea corrupta no cancela la carga:** se descarta, se registra una
  advertencia con su número de línea y el resto del archivo sí se lee.
- **Respaldo con Java I/O a propósito:** `respaldar()` usa `File`, `FileReader`
  y `FileWriter` cerrando los recursos a mano en un `finally`, justamente para
  poder compararlo con el try-with-resources y NIO.2 del resto de la clase.
- **Mensajes sin tildes:** la salida por consola va sin acentos a propósito,
  porque la consola de Windows y la ventana de NetBeans usan codificaciones
  distintas y los acentos se verían mal en una de las dos.
