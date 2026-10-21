# Contrato de la API REST

Contrato entre el backend (`infrastructure.in.web`) y la interfaz web (`src/main/resources/web/`).
**Cualquier cambio se acuerda entre las dos partes antes de implementarlo.**

## Convenciones
- Mismo origen, prefijo `/api`, JSON en UTF-8.
- **El dinero y las tasas viajan como string** con punto decimal: `"85.50"`, `"0.16"`.
  - Respuestas: el dinero siempre lleva 2 decimales (`"85.00"`); las tasas, sin ceros sobrantes (`"0.16"`, `"0"`).
  - Peticiones: se mandan como string. El backend también acepta números (`85.5`). En descuento y propina,
    un valor vacío o ausente equivale a 0.
- Los enums viajan por su nombre: `"SALA"`, `"EN_COCCION"`, etc.
- Fechas en ISO local sin zona y siempre con segundos: `"2026-10-20T13:45:00"`.
- Errores: el cuerpo siempre es `{ "error": "mensaje para el usuario" }`.
  - `400`: regla de negocio violada, recurso inexistente (p. ej. "No existe la orden X") o petición mal formada.
  - `404`: ruta inexistente.
  - `500`: error interno.

## Enums
| Enum | Valores |
|---|---|
| TipoOrden | `SALA`, `PARA_LLEVAR`, `DELIVERY` |
| EstadoOrden | `ABIERTA`, `CERRADA`, `CANCELADA` |
| EstadoCocina | `PENDIENTE`, `EN_COCCION`, `LISTO`, `CANCELADA` |
| TipoModificador | `EXTRA`, `EXCLUSION` |
| MetodoPago | `EFECTIVO`, `TARJETA`, `PASARELA_DIGITAL` |
| EstadoPago | `PENDIENTE`, `APROBADO`, `RECHAZADO` |

## Objetos (DTO)
```jsonc
PlatoDto        { "id": "P1", "nombre": "Tacos", "precioBase": "85.00", "tasaImpuesto": "0.16" }
ModificadorDto  { "id": "M1", "nombre": "Queso extra", "tipo": "EXTRA", "costoExtra": "10.00" }
TotalesDto      { "subtotal": "285.00", "descuento": "19.00", "impuestos": "42.56", "propina": "10.00", "total": "318.56" }
PagoDto         { "id": "uuid", "ordenId": "uuid", "fecha": "2026-10-20T13:45:00", "estado": "APROBADO",
                  "metodo": "TARJETA", "monto": "318.56", "referencia": "TAR-1a2b3c4d" }
LineaDto        { "id": "uuid", "platoId": "P1", "platoNombre": "Tacos", "cantidad": 3,
                  "precioUnitario": "85.00", "tasaImpuesto": "0.16",
                  "modificadores": [ ModificadorDto ],   // costoExtra = el que se aplicó al agregar la línea
                  "estadoCocina": "PENDIENTE", "editable": true }
OrdenDto        { "id": "uuid", "tipo": "SALA", "referencia": "Mesa 5", "estado": "ABIERTA",
                  "fechaCreacion": "2026-10-20T13:40:00",
                  "lineas": [ LineaDto ],                // incluye las CANCELADAS
                  "totales": TotalesDto,                 // CERRADA: los cobrados; si no: sin descuento ni propina
                  "pagos": [ PagoDto ] }                 // todos los intentos, incluidos los rechazados
LineaCocinaDto  { "ordenId": "uuid", "lineaId": "uuid", "referenciaOrden": "Mesa 5", "plato": "Tacos",
                  "cantidad": 3, "modificadores": ["Queso extra"], "estado": "EN_COCCION" }
```

## Endpoints
| Método | Ruta | Cuerpo | Respuesta | Controlador |
|---|---|---|---|---|
| GET | `/api/platos` | | 200 `PlatoDto[]` ordenados por nombre | Menu |
| POST | `/api/platos` | `{id, nombre, precioBase, tasaImpuesto}` | 201 `PlatoDto` | Menu |
| GET | `/api/modificadores` | | 200 `ModificadorDto[]` ordenados por nombre | Menu |
| POST | `/api/modificadores` | `{id, nombre, tipo, costoExtra}` | 201 `ModificadorDto` | Menu |
| GET | `/api/ordenes?estado=ABIERTA` (`estado` es opcional) | | 200 `OrdenDto[]`, de la más antigua a la más reciente | Orden |
| GET | `/api/ordenes/{ordenId}` | | 200 `OrdenDto` | Orden |
| POST | `/api/ordenes` | `{tipo, referencia}` | 201 `OrdenDto` | Orden |
| POST | `/api/ordenes/{ordenId}/lineas` | `{platoId, cantidad, modificadorIds: []}` | 200 `OrdenDto` | Orden |
| PUT | `/api/ordenes/{ordenId}/lineas/{lineaId}` | `{cantidad, modificadorIds: []}` | 200 `OrdenDto` | Orden |
| DELETE | `/api/ordenes/{ordenId}/lineas/{lineaId}` | | 200 `OrdenDto` (la línea queda CANCELADA) | Orden |
| POST | `/api/ordenes/{ordenId}/cancelar` | | 200 `OrdenDto` | Orden |
| GET | `/api/cocina/pendientes` | | 200 `LineaCocinaDto[]` (PENDIENTE y EN_COCCION) | Cocina |
| POST | `/api/cocina/ordenes/{ordenId}/lineas/{lineaId}/iniciar` | | 204 | Cocina |
| POST | `/api/cocina/ordenes/{ordenId}/lineas/{lineaId}/lista` | | 204 | Cocina |
| GET | `/api/caja/ordenes/{ordenId}/totales?descuento=19&propina=10` | | 200 `TotalesDto` | Caja |
| POST | `/api/caja/ordenes/{ordenId}/cobrar` | `{metodo, descuento, propina}` | 200 `PagoDto` (APROBADO **o** RECHAZADO) | Caja |

`cantidad` es un entero. `modificadorIds` es un arreglo de ids (puede ir vacío u omitirse).

## Ejemplos rápidos (curl)
```bash
curl localhost:8080/api/platos
curl -X POST localhost:8080/api/ordenes -d '{"tipo":"SALA","referencia":"Mesa 5"}'
curl -X POST localhost:8080/api/ordenes/<ordenId>/lineas -d '{"platoId":"P-001","cantidad":2,"modificadorIds":["M-001"]}'
curl "localhost:8080/api/caja/ordenes/<ordenId>/totales?descuento=10&propina=5"
curl -X POST localhost:8080/api/caja/ordenes/<ordenId>/cobrar -d '{"metodo":"TARJETA","descuento":"10","propina":"5"}'
```
