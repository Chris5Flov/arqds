package com.restaurante.application.port.out;

import java.math.BigDecimal;

import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;

public interface PuertoCobro {
    boolean soporta(MetodoPago metodo);

    Pago cobrar(String ordenId, BigDecimal monto);
}