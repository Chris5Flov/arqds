# arqds
ProyectosADS

## Sistema de Pedidos de Restaurante (`restaurante-pedidos-hexagonal/`)

Aplicación en Java 17 con arquitectura hexagonal (puertos y adaptadores) para gestionar las
comandas de un restaurante. Se usa desde una interfaz web (con una API REST) o desde la consola:

| Actor | Funcionalidad |
|---|---|
| Administrador | Registrar y consultar platos y modificadores (extras y exclusiones) — RF1 |
| Mesero | Abrir órdenes y agregar, modificar o cancelar líneas; cancelar una orden — RF2, RF3 |
| Cocinero | Consultar los pedidos pendientes y avanzar su estado (PENDIENTE → EN_COCCION → LISTO) — RF3 |
| Cajero | Calcular totales con descuento y propina, y cobrar en efectivo, tarjeta o pasarela digital — RF4, RF5 |

### Requisitos
- JDK 17 o superior
- Maven 3.8 o superior

### Compilar, probar y ejecutar
```bash
cd restaurante-pedidos-hexagonal
mvn test                                       # ejecuta las pruebas JUnit 5
mvn compile exec:java                          # servidor web en http://localhost:8080
mvn compile exec:java -Dexec.args="--demo"     # ... con un menú y una orden de ejemplo
mvn compile exec:java -Dexec.mainClass=com.restaurante.infrastructure.in.cli.Main   # menú de consola
# o bien, un jar ejecutable con sus dependencias:
mvn package
java -jar target/pedidos-hexagonal-0.1.0.jar --demo --puerto=8080
```
Desde el IDE: ejecuta `MainWeb` (web) o `Main` (consola). Ambos aceptan el argumento `--demo`.

### Documentación
- [Arquitectura y reglas de negocio](restaurante-pedidos-hexagonal/docs/arquitectura-base.md)
- [Contrato de la API REST](restaurante-pedidos-hexagonal/docs/interfaz/contrato-api.md)
- [Diagramas](restaurante-pedidos-hexagonal/docs/diagramas/)
