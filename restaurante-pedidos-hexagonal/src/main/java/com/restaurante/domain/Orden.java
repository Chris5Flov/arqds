package com.restaurante.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Entidad raíz: una comanda ligada a una mesa o a un pedido externo (RF2). */
public class Orden {
    // RF4: redondeo fijo de todos los montos
    private static final int DECIMALES = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final String id;
    private final TipoOrden tipo;
    private final String referencia; // número de mesa o id de pedido externo
    private final List<LineaPedido> lineas = new ArrayList<>();
    private EstadoOrden estado = EstadoOrden.ABIERTA;
    private Pago pago; // se llena al cerrar la orden (RF5)

    private Orden(String id, TipoOrden tipo, String referencia) {
        this.id = id;
        this.tipo = tipo;
        this.referencia = referencia;
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

    public LineaPedido agregarLinea(Plato plato, int cantidad, List<Modificador> modificadores) {
        exigirAbierta();
        if (cantidad <= 0) {
            throw new ReglaDeNegocioException("La cantidad debe ser mayor a cero");
        }
        LineaPedido linea = new LineaPedido(UUID.randomUUID().toString(), plato, cantidad, modificadores);
        lineas.add(linea);
        return linea;
    }

    public void cancelarLinea(String lineaId) {
    exigirAbierta();
    LineaPedido linea = lineas.stream()
            .filter(l -> l.getId().equals(lineaId))
            .findFirst()
            .orElseThrow(() -> new ReglaDeNegocioException("No existe la línea " + lineaId));
    if (!linea.esEditable()) {
        throw new ReglaDeNegocioException("La línea ya no es editable porque está en cocina");
    }
    lineas.remove(linea);
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

    /** RF4: totales de la orden sin descuento ni propina. */
    public Totales calcularTotales() {
        return calcularTotales(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * RF4: desglose determinista del cobro, redondeado siempre a 2 decimales (HALF_UP).
     * <ul>
     *   <li>subtotal: suma de (precioBase + costoExtra de los modificadores) × cantidad de cada línea.</li>
     *   <li>descuento: monto fijo sobre el subtotal; se reparte en proporción al importe de cada
     *       línea para que cada plato pague impuesto solo sobre lo que realmente se cobra.</li>
     *   <li>impuestos: por línea, con la tasaImpuesto de su plato, redondeados línea por línea.</li>
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

        BigDecimal subtotal = lineas.stream()
                .map(this::importeDe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotal = redondear(subtotal);
        if (descuentoFinal.compareTo(subtotal) > 0) {
            throw new ReglaDeNegocioException("El descuento no puede ser mayor al subtotal");
        }

        BigDecimal baseGravable = subtotal.subtract(descuentoFinal);
        BigDecimal impuestos = BigDecimal.ZERO;
        if (subtotal.signum() > 0) {
            for (LineaPedido linea : lineas) {
                BigDecimal tasa = linea.getPlato().tasaImpuesto() == null
                        ? BigDecimal.ZERO : linea.getPlato().tasaImpuesto();
                // importe × tasa × (subtotal - descuento) / subtotal, con un solo redondeo por línea
                impuestos = impuestos.add(importeDe(linea).multiply(tasa).multiply(baseGravable)
                        .divide(subtotal, DECIMALES, REDONDEO));
            }
        }
        impuestos = redondear(impuestos);

        BigDecimal total = redondear(baseGravable.add(impuestos).add(propinaFinal));
        return new Totales(subtotal, descuentoFinal, impuestos, propinaFinal, total);
    }

    /** (precioBase + costoExtra de cada modificador) × cantidad, a 2 decimales. */
    private BigDecimal importeDe(LineaPedido linea) {
        BigDecimal precioUnitario = linea.getPlato().precioBase() == null
                ? BigDecimal.ZERO : linea.getPlato().precioBase();
        for (Modificador modificador : linea.getModificadores()) {
            if (modificador.costoExtra() != null) {
                precioUnitario = precioUnitario.add(modificador.costoExtra());
            }
        }
        return redondear(precioUnitario.multiply(BigDecimal.valueOf(linea.getCantidad())));
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(DECIMALES, REDONDEO);
    }

    /** RF5: cerrar la orden una vez registrado el pago que la cubre. */
    public void cerrar(Pago pago) {
        exigirAbierta();
        if (pago == null) {
            throw new ReglaDeNegocioException("Para cerrar la orden se necesita el pago registrado");
        }
        if (!id.equals(pago.ordenId())) {
            throw new ReglaDeNegocioException("El pago no corresponde a esta orden");
        }
        this.pago = pago;
        this.estado = EstadoOrden.CERRADA;
    }

    private LineaPedido buscarLinea(String lineaId) {
    return lineas.stream()
            .filter(l -> l.getId().equals(lineaId))
            .findFirst()
            .orElseThrow(() -> new ReglaDeNegocioException("No existe la línea " + lineaId));
    
    }
    
    private void exigirAbierta() {
        if (estado != EstadoOrden.ABIERTA) {
            throw new ReglaDeNegocioException("La orden no está abierta");
        }
    }

    public String getId() { return id; }
    public TipoOrden getTipo() { return tipo; }
    public String getReferencia() { return referencia; }
    public EstadoOrden getEstado() { return estado; }
    public Pago getPago() { return pago; }
    public List<LineaPedido> getLineas() { return Collections.unmodifiableList(lineas); }
}
