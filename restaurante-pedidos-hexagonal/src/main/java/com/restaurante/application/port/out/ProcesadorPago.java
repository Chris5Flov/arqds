package com.restaurante.application.port.out;

import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import java.math.BigDecimal;

/** Cobro con un método de pago concreto. Devuelve el pago con su estado (APROBADO, RECHAZADO...). */
public interface ProcesadorPago {
    boolean soporta(MetodoPago metodo);

    Pago cobrar(String ordenId, BigDecimal monto);
}
