package com.restaurante.domain;

import java.util.List;

public class LineaPedido {
    private final String id;
    private final Plato plato;
    private final int cantidad;
    private final List<Modificador> modificadores;
    private EstadoCocina estadoCocina = EstadoCocina.PENDIENTE;

    public LineaPedido(String id, Plato plato, int cantidad, List<Modificador> modificadores) {
        this.id = id;
        this.plato = plato;
        this.cantidad = cantidad;
        this.modificadores = List.copyOf(modificadores);
    }

    /** RF3: no es editable después de iniciar la cocción. */
    public boolean esEditable() {
        return estadoCocina == EstadoCocina.PENDIENTE;
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

    public String getId() { return id; }
    public Plato getPlato() { return plato; }
    public int getCantidad() { return cantidad; }
    public List<Modificador> getModificadores() { return modificadores; }
    public EstadoCocina getEstadoCocina() { return estadoCocina; }
}
