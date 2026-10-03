package com.restaurante.domain;

import java.math.BigDecimal;

/** Desglose determinista del cobro (RF4). */
public record Totales(BigDecimal subtotal, BigDecimal descuento, BigDecimal impuestos,
                      BigDecimal propina, BigDecimal total) { }
