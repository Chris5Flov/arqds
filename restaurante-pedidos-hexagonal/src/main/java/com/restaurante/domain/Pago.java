package com.restaurante.domain;

import java.math.BigDecimal;

/** Resultado de cobrar una orden (RF5). Cada pago queda ligado a la orden que cobró. */
public record Pago(String id, String ordenId, MetodoPago metodo, BigDecimal monto, String referencia) { }
