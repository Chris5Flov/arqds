package com.restaurante.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Plato pedido dentro de una orden. El precio unitario y la tasa de impuesto se copian del
 * plato al crear la línea, así la orden conserva sus importes aunque cambie el catálogo.
 */
public class LineaPedido {
    private final String id;
    private final Plato plato;
    private final BigDecimal precioUnitario;
    private final BigDecimal tasaImpuestoAplicada;
    private int cantidad;
    private List<ModificadorAplicado> modificadores;
    private EstadoCocina estadoCocina = EstadoCocina.PENDIENTE;

    public LineaPedido(String id, Plato plato, int cantidad, List<Modificador> modificadores) {
        if (plato == null) {
            throw new ReglaDeNegocioException("El plato es obligatorio");
        }
        this.id = id;
        this.plato = plato;
        this.precioUnitario = plato.precioBase() == null ? BigDecimal.ZERO : plato.precioBase();
        this.tasaImpuestoAplicada = plato.tasaImpuesto() == null ? BigDecimal.ZERO : plato.tasaImpuesto();
        asignar(cantidad, modificadores);
    }

    /** RF3: no es editable después de iniciar la cocción (ni si ya se canceló). */
    public boolean esEditable() {
        return estadoCocina == EstadoCocina.PENDIENTE;
    }

    public boolean estaCancelada() {
        return estadoCocina == EstadoCocina.CANCELADA;
    }

    /** RF3: cambia cantidad y modificadores; solo mientras la línea está PENDIENTE. */
    public void modificar(int cantidad, List<Modificador> modificadores) {
        exigirEditable("modificar");
        asignar(cantidad, modificadores);
    }

    /** RF3: la línea queda CANCELADA y deja de contar en los totales. */
    public void cancelar() {
        exigirEditable("cancelar");
        estadoCocina = EstadoCocina.CANCELADA;
    }

    /** RF3: PENDIENTE -> EN_COCCION. Desde ese momento la línea deja de ser editable. */
    public void iniciarCoccion() {
        if (estadoCocina != EstadoCocina.PENDIENTE) {
            throw new ReglaDeNegocioException(
                    "Solo se puede iniciar la cocción de una línea PENDIENTE (estado actual: " + estadoCocina + ")");
        }
        estadoCocina = EstadoCocina.EN_COCCION;
    }

    /** RF3: EN_COCCION -> LISTO. */
    public void marcarLista() {
        if (estadoCocina != EstadoCocina.EN_COCCION) {
            throw new ReglaDeNegocioException(
                    "Solo se puede marcar como lista una línea EN_COCCION (estado actual: " + estadoCocina + ")");
        }
        estadoCocina = EstadoCocina.LISTO;
    }

    /** (precioUnitario + costo extra de cada modificador) × cantidad, sin redondear. */
    BigDecimal importe() {
        BigDecimal unitario = precioUnitario;
        for (ModificadorAplicado m : modificadores) {
            unitario = unitario.add(m.costoExtraAplicado());
        }
        return unitario.multiply(BigDecimal.valueOf(cantidad));
    }

    private void asignar(int cantidad, List<Modificador> modificadores) {
        if (cantidad <= 0) {
            throw new ReglaDeNegocioException("La cantidad debe ser mayor a cero");
        }
        List<Modificador> lista = modificadores == null ? List.of() : modificadores;
        this.modificadores = lista.stream().map(ModificadorAplicado::de).toList();
        this.cantidad = cantidad;
    }

    private void exigirEditable(String accion) {
        if (!esEditable()) {
            throw new ReglaDeNegocioException("No se puede " + accion
                    + " la línea: solo se permite mientras está PENDIENTE (estado actual: " + estadoCocina + ")");
        }
    }

    public String getId() { return id; }
    public Plato getPlato() { return plato; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getTasaImpuestoAplicada() { return tasaImpuestoAplicada; }
    public List<ModificadorAplicado> getModificadores() { return modificadores; }
    public EstadoCocina getEstadoCocina() { return estadoCocina; }
}
