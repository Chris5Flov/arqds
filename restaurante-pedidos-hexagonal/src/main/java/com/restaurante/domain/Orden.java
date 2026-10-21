package com.restaurante.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Entidad raíz: una comanda ligada a una mesa o a un pedido externo (RF2). */
public class Orden {
    // RF4: redondeo fijo de todos los montos
    private static final int DECIMALES = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final String id;
    private final TipoOrden tipo;
    private final String referencia; // número de mesa o id de pedido externo
    private final LocalDateTime fechaCreacion;
    private final List<LineaPedido> lineas = new ArrayList<>();
    private final List<Pago> pagos = new ArrayList<>();
    private EstadoOrden estado = EstadoOrden.ABIERTA;
    private Totales totales; // se fija al cerrar la orden (RF5)

    private Orden(String id, TipoOrden tipo, String referencia) {
        this.id = id;
        this.tipo = tipo;
        this.referencia = referencia;
        this.fechaCreacion = LocalDateTime.now();
    }

    /** RF2: abrir una orden. */
    public static Orden abrir(TipoOrden tipo, String referencia) {
        if (tipo == null) {
            throw new ReglaDeNegocioException("La orden necesita un tipo");
        }
        if (referencia == null || referencia.isBlank()) {
            throw new ReglaDeNegocioException("La orden necesita una mesa o un identificador de pedido");
        }
        return new Orden(UUID.randomUUID().toString(), tipo, referencia);
    }

    // ---------------------------------------------------------------- líneas (RF3)

    public LineaPedido agregarLinea(Plato plato, int cantidad, List<Modificador> modificadores) {
        exigirAbierta();
        LineaPedido linea = new LineaPedido(UUID.randomUUID().toString(), plato, cantidad, modificadores);
        lineas.add(linea);
        return linea;
    }

    /** RF3: solo con la orden abierta y la línea PENDIENTE. */
    public void modificarLinea(String lineaId, int cantidad, List<Modificador> modificadores) {
        exigirAbierta();
        buscarLinea(lineaId).modificar(cantidad, modificadores);
    }

    /** RF3: la línea queda CANCELADA (se conserva en la orden, pero no cuenta en los totales). */
    public void cancelarLinea(String lineaId) {
        exigirAbierta();
        buscarLinea(lineaId).cancelar();
    }

    /** RF3: la cocina empieza a preparar una línea (PENDIENTE -> EN_COCCION). */
    public void iniciarCoccionLinea(String lineaId) {
        exigirAbierta();
        buscarLinea(lineaId).iniciarCoccion();
    }

    /** RF3: la cocina termina una línea (EN_COCCION -> LISTO). */
    public void marcarLineaLista(String lineaId) {
        exigirAbierta();
        buscarLinea(lineaId).marcarLista();
    }

    // ---------------------------------------------------------------- totales (RF4)

    /** RF4: totales de la orden sin descuento ni propina. */
    public Totales calcularTotales() {
        return calcularTotales(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * RF4: desglose determinista del cobro, redondeado siempre a 2 decimales (HALF_UP).
     * <ul>
     *   <li>subtotal: suma de (precioUnitario + costo extra de los modificadores) × cantidad de cada
     *       línea no cancelada, con los precios congelados al agregar la línea.</li>
     *   <li>descuento: monto fijo sobre el subtotal; se reparte en proporción al importe de cada
     *       línea para que cada plato pague impuesto solo sobre lo que realmente se cobra.</li>
     *   <li>impuestos: por línea, con la tasa de impuesto aplicada, redondeados línea por línea.</li>
     *   <li>total: subtotal - descuento + impuestos + propina.</li>
     * </ul>
     */
    public Totales calcularTotales(BigDecimal descuento, BigDecimal propina) {
        BigDecimal descuentoFinal = redondear(descuento == null ? BigDecimal.ZERO : descuento);
        BigDecimal propinaFinal = redondear(propina == null ? BigDecimal.ZERO : propina);
        if (descuentoFinal.signum() < 0) {
            throw new ReglaDeNegocioException("El descuento no puede ser negativo");
        }
        if (propinaFinal.signum() < 0) {
            throw new ReglaDeNegocioException("La propina no puede ser negativa");
        }

        List<LineaPedido> activas = getLineasActivas();
        BigDecimal subtotal = activas.stream()
                .map(l -> redondear(l.importe()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotal = redondear(subtotal);
        if (descuentoFinal.compareTo(subtotal) > 0) {
            throw new ReglaDeNegocioException("El descuento no puede ser mayor al subtotal");
        }

        BigDecimal baseGravable = subtotal.subtract(descuentoFinal);
        BigDecimal impuestos = BigDecimal.ZERO;
        if (subtotal.signum() > 0) {
            for (LineaPedido linea : activas) {
                // importe × tasa × (subtotal - descuento) / subtotal, con un solo redondeo por línea
                impuestos = impuestos.add(redondear(linea.importe()).multiply(linea.getTasaImpuestoAplicada())
                        .multiply(baseGravable)
                        .divide(subtotal, DECIMALES, REDONDEO));
            }
        }
        impuestos = redondear(impuestos);

        BigDecimal total = redondear(baseGravable.add(impuestos).add(propinaFinal));
        return new Totales(subtotal, descuentoFinal, impuestos, propinaFinal, total);
    }

    // ---------------------------------------------------------------- cobro y cierre (RF5)

    /**
     * RF5: valida que la orden se pueda cobrar y devuelve el monto a cobrar.
     * Se llama antes de pedir el cargo al procesador de pago, para no cobrar nada si algo falla.
     */
    public Totales totalesParaCobrar(BigDecimal descuento, BigDecimal propina) {
        exigirAbierta();
        if (getLineasActivas().isEmpty()) {
            throw new ReglaDeNegocioException("No se puede cobrar una orden sin líneas");
        }
        if (getPagoAprobado().isPresent()) {
            throw new ReglaDeNegocioException("La orden ya tiene un pago aprobado");
        }
        return calcularTotales(descuento, propina);
    }

    /** RF5: registra un intento de cobro. Máximo un pago aprobado por orden. */
    public void registrarPago(Pago pago) {
        exigirAbierta();
        if (pago == null) {
            throw new ReglaDeNegocioException("El pago es obligatorio");
        }
        if (!id.equals(pago.ordenId())) {
            throw new ReglaDeNegocioException("El pago no corresponde a esta orden");
        }
        if (pago.esAprobado() && getPagoAprobado().isPresent()) {
            throw new ReglaDeNegocioException("La orden ya tiene un pago aprobado");
        }
        pagos.add(pago);
    }

    /** RF5: cerrar sin descuento ni propina. */
    public void cerrar() {
        cerrar(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** RF5: la orden se cierra únicamente con un pago aprobado que cubra el total. */
    public void cerrar(BigDecimal descuento, BigDecimal propina) {
        exigirAbierta();
        if (getLineasActivas().isEmpty()) {
            throw new ReglaDeNegocioException("No se puede cerrar una orden sin líneas");
        }
        Totales calculados = calcularTotales(descuento, propina);
        Pago aprobado = getPagoAprobado().orElseThrow(() -> new ReglaDeNegocioException(
                "La orden se cierra únicamente con un pago aprobado"));
        if (aprobado.monto() == null || aprobado.monto().compareTo(calculados.total()) < 0) {
            throw new ReglaDeNegocioException("El pago aprobado no cubre el total de la orden ("
                    + calculados.total().toPlainString() + ")");
        }
        this.totales = calculados;
        this.estado = EstadoOrden.CERRADA;
    }

    /** Cancela la orden completa; solo si ninguna línea ha entrado a cocina y no hay pago aprobado. */
    public void cancelar() {
        exigirAbierta();
        if (getPagoAprobado().isPresent()) {
            throw new ReglaDeNegocioException("No se puede cancelar una orden con un pago aprobado");
        }
        boolean hayEnCocina = getLineasActivas().stream().anyMatch(l -> !l.esEditable());
        if (hayEnCocina) {
            throw new ReglaDeNegocioException("No se puede cancelar la orden: hay líneas en cocina o listas");
        }
        getLineasActivas().forEach(LineaPedido::cancelar);
        this.estado = EstadoOrden.CANCELADA;
    }

    // ---------------------------------------------------------------- utilidades

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(DECIMALES, REDONDEO);
    }

    private LineaPedido buscarLinea(String lineaId) {
        return lineas.stream()
                .filter(l -> l.getId().equals(lineaId))
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la línea " + lineaId));
    }

    private void exigirAbierta() {
        if (estado != EstadoOrden.ABIERTA) {
            throw new ReglaDeNegocioException("La orden no está abierta (estado: " + estado + ")");
        }
    }

    public String getId() { return id; }
    public TipoOrden getTipo() { return tipo; }
    public String getReferencia() { return referencia; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public EstadoOrden getEstado() { return estado; }

    /** Todas las líneas, incluidas las canceladas. */
    public List<LineaPedido> getLineas() { return Collections.unmodifiableList(lineas); }

    /** Líneas que cuentan para cocina y totales (todas menos las canceladas). */
    public List<LineaPedido> getLineasActivas() {
        return lineas.stream().filter(l -> !l.estaCancelada()).toList();
    }

    /** Todos los intentos de cobro (aprobados, rechazados o pendientes). */
    public List<Pago> getPagos() { return Collections.unmodifiableList(pagos); }

    public Optional<Pago> getPagoAprobado() {
        return pagos.stream().filter(Pago::esAprobado).findFirst();
    }

    /** Totales con los que se cerró la orden; vacío mientras no esté cerrada. */
    public Optional<Totales> getTotales() { return Optional.ofNullable(totales); }
}
