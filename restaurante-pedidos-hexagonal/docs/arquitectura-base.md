# Arquitectura base (hexagonal)

## Capas (`src/main/java/com/restaurante`)

| Paquete | Contenido | Puede depender de |
|---|---|---|
| `domain` | Entidades y reglas de negocio (`Orden`, `LineaPedido`, `Plato`...) | nada |
| `application.port.in` | Puertos de entrada: un caso de uso por interfaz (RF1-RF5) | `domain` |
| `application.port.out` | Puertos de salida: repositorios y cobro | `domain` |
| `application.service` | Implementaciones de los casos de uso | `domain`, `application.port` |
| `infrastructure` | Adaptadores: persistencia, pagos, entrada por consola | `application`, `domain` |

**Regla de dependencias:** todo apunta hacia el dominio. El dominio no importa nada de
`application` ni de `infrastructure`, y `application` no importa nada de `infrastructure`.

## Cómo agregar un caso de uso
1. La interfaz ya existe en `application/port/in`.
2. Crea la clase `XxxService` en `application/service` que la implemente y reciba los puertos de salida por constructor.
3. Pon las reglas de negocio dentro del dominio (`Orden`, etc.), no en el servicio.
4. Conéctalo en `Main` (o en el adaptador de entrada que se use).

## Pendiente (TODO en el código)
- `Orden.cancelarLinea` (RF3), `Orden.calcularTotales` (RF4), `Orden.cerrar` (RF5)
- Servicios para `AgregarLinea`, `CancelarLinea`, `CalcularTotales`, `CerrarOrden`, `AdministrarMenu`
- Adaptadores de entrada adicionales (API web) y de persistencia real
- Pruebas del núcleo
