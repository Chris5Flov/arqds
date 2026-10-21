# Arquitectura (hexagonal)

## Capas (`src/main/java/com/restaurante`)

| Paquete | Contenido | Puede depender de |
|---|---|---|
| `domain` | Entidades y reglas de negocio (`Orden`, `LineaPedido`, `Plato`, `Modificador`, `Pago`...) | nada |
| `application.port.in` | Puertos de entrada: un caso de uso por actor | `domain` |
| `application.port.out` | Puertos de salida: repositorios y procesador de pago | `domain` |
| `application.service` | Implementaciones de los casos de uso | `domain`, `application.port` |
| `infrastructure` | Adaptadores: entrada web (REST) y por consola, persistencia en memoria, pagos; `config` arma la aplicación | `application`, `domain` |

**Regla de dependencias:** todo apunta hacia el dominio. El dominio no importa nada de
`application` ni de `infrastructure`, y `application` no importa nada de `infrastructure`.

## Componentes (ver `diagramas/3-arquitectura-hexagonal.jpeg`)

| Puerto de entrada | Servicio | Actor | RF | Usa los puertos de salida |
|---|---|---|---|---|
| `AdministrarMenuUseCase` | `AdministrarMenuService` | Administrador | RF1 | `PlatoRepository`, `ModificadorRepository` |
| `GestionarOrdenUseCase` | `GestionarOrdenService` | Mesero | RF2, RF3 | `OrdenRepository`, `PlatoRepository`, `ModificadorRepository` |
| `GestionarCocinaUseCase` | `GestionarCocinaService` | Cocinero | RF3 | `OrdenRepository` |
| `CobrarOrdenUseCase` | `CobrarOrdenService` | Cajero | RF4, RF5 | `OrdenRepository`, `ProcesadorPago` |

| Puerto de salida | Adaptador |
|---|---|
| `PlatoRepository` | `PlatoRepositoryMemoria` |
| `ModificadorRepository` | `ModificadorRepositoryMemoria` |
| `OrdenRepository` | `OrdenRepositoryMemoria` |
| `ProcesadorPago` | `PagoEfectivoAdapter`, `PagoTarjetaSimuladoAdapter`, `PagoPasarelaSimuladoAdapter` |

### Adaptadores de entrada

| Adaptador | Clases | Usa |
|---|---|---|
| Web (REST + interfaz) | `infrastructure.in.web`: `MenuRestController`, `OrdenRestController`, `CocinaRestController`, `CajaRestController`, servidos por `ServidorWeb` | los 4 puertos de entrada |
| Consola | `infrastructure.in.cli.MenuConsola` (opciones agrupadas por actor) | los 4 puertos de entrada |

- `infrastructure.config.Aplicacion` es la raíz de composición: crea los adaptadores de salida y se los
  pasa a los servicios por constructor. La usan `MainWeb` (servidor web) y `Main` (consola).
- `infrastructure.config.DatosDemo` carga un menú y una orden de ejemplo con `--demo`.
- El adaptador web usa el servidor HTTP del JDK (`com.sun.net.httpserver`) y Gson para el JSON.
  Su estructura:
  - `web.http`: `Enrutador`, `Peticion`, `Respuesta`, `ApiHandler` (traduce `ReglaDeNegocioException`
    a 400), `Json` y `Conversion`.
  - `web.dto`: los DTO del contrato y `MapeadorDto`.
  - `ArchivosEstaticosHandler`: sirve `src/main/resources/web/` en `/`.
- Contrato completo de la API: [interfaz/contrato-api.md](interfaz/contrato-api.md).

> **Alcance:** el diagrama hexagonal muestra además repositorios con base de datos (`*RepositoryBD`).
> En esta entrega la persistencia es en memoria. Para agregar una base de datos basta con escribir
> nuevos adaptadores para los mismos puertos de salida y conectarlos en `Aplicacion`, sin tocar
> `domain` ni `application`.

## Reglas de negocio principales (ver `diagramas/2-clases-dominio.jpeg`)

- **RF1:** precio mayor a 0 con 2 decimales como máximo; tasa de impuesto como fracción entre 0 y 1;
  ids únicos. Un modificador EXTRA cuesta 0 o más y una EXCLUSION no tiene costo.
- **RF2:** una orden necesita un tipo (SALA, PARA_LLEVAR, DELIVERY) y una referencia (mesa o pedido externo).
- **RF3:** las líneas solo se agregan, modifican o cancelan con la orden ABIERTA y la línea PENDIENTE.
  Una línea cancelada queda en estado CANCELADA y se excluye de la cocina y de los totales.
  El precio unitario, la tasa de impuesto y el costo de los modificadores se copian al crear la línea,
  así la orden conserva sus importes aunque cambie el catálogo.
- **Cocina:** PENDIENTE → EN_COCCION → LISTO.
- **Cancelar orden:** solo si ninguna línea ha entrado a cocina y no hay un pago aprobado.
- **RF4:** subtotal − descuento + impuestos (por línea, sobre la base con descuento) + propina,
  redondeado a 2 decimales (HALF_UP).
- **RF5:** cada intento de cobro se registra como `Pago` (APROBADO, RECHAZADO o PENDIENTE).
  Se permite como máximo un pago aprobado por orden, y la orden se cierra únicamente con un pago
  aprobado que cubra el total. Al cerrar se guardan los totales cobrados. Si el pago se rechaza, la
  orden sigue abierta y se puede reintentar con otro método.

## Cómo agregar un caso de uso
1. Declara la operación en el puerto de entrada que corresponda en `application/port/in`.
2. Impleméntala en su `XxxService` de `application/service`, que recibe los puertos de salida por constructor.
3. Pon las reglas de negocio dentro del dominio (`Orden`, `LineaPedido`, etc.), no en el servicio.
4. Expónla en el controlador REST que corresponda (actualizando `docs/interfaz/contrato-api.md`) y/o en
   `MenuConsola`. Si es un servicio nuevo, conéctalo en `Aplicacion`.

## Fuera de alcance / mejoras posibles
- Persistencia en base de datos (ver la nota de alcance).
- Integración con una terminal bancaria o pasarela de pago reales (hoy son simulaciones).
