package com.restaurante.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Intento de cobro de una orden (RF5). Cada pago queda ligado a la orden que cobró. */
public record Pago(String id,
                   String ordenId,
                   LocalDateTime fecha,
                   EstadoPago estado,
                   MetodoPago metodo,
                   BigDecimal monto,
                   String referencia) {

    public boolean esAprobado() {
        return estado == EstadoPago.APROBADO;
    }
}
