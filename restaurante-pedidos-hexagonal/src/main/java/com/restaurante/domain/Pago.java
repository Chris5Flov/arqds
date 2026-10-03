package com.restaurante.domain;

import java.math.BigDecimal;

/** Resultado de cobrar una orden (RF5). */
public record Pago(MetodoPago metodo, BigDecimal monto, String referencia) { }
