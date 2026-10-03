package com.restaurante.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Entidad raíz: una comanda ligada a una mesa o a un pedido externo (RF2). */
public class Orden {
    private final String id;
    private final TipoOrden tipo;
    private final String referencia; // número de mesa o id de pedido externo
    private final List<LineaPedido> lineas = new ArrayList<>();
    private EstadoOrden estado = EstadoOrden.ABIERTA;

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
        throw new UnsupportedOperationException("TODO RF3: cancelar línea solo si esEditable()");
    }

    public Totales calcularTotales() {
        throw new UnsupportedOperationException("TODO RF4: subtotal, descuentos, impuestos y propina");
    }

    public void cerrar() {
        throw new UnsupportedOperationException("TODO RF5: cerrar la orden tras registrar el pago");
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
    public List<LineaPedido> getLineas() { return Collections.unmodifiableList(lineas); }
}
