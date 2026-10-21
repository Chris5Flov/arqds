package com.restaurante.domain;

import java.math.BigDecimal;

/**
 * Modificador tal como se aplicó a una línea de pedido. Guarda el costo extra vigente en ese
 * momento para que la orden no cambie aunque después se modifique el catálogo.
 */
public record ModificadorAplicado(Modificador modificador, BigDecimal costoExtraAplicado) {

    public static ModificadorAplicado de(Modificador modificador) {
        if (modificador == null) {
            throw new ReglaDeNegocioException("El modificador es obligatorio");
        }
        BigDecimal costo = modificador.costoExtra() == null ? BigDecimal.ZERO : modificador.costoExtra();
        return new ModificadorAplicado(modificador, costo);
    }

    public String nombre() {
        return modificador.nombre();
    }
}
