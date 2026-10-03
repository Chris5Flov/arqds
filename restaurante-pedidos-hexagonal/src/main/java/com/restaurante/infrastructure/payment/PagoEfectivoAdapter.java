package com.restaurante.infrastructure.payment;

import com.restaurante.application.port.out.PuertoCobro;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import java.math.BigDecimal;
import java.util.UUID;

/** Esqueleto: simula el cobro. TODO: integrar con el proveedor real si hace falta. */
public class PagoEfectivoAdapter implements PuertoCobro {
    @Override
    public boolean soporta(MetodoPago metodo) {
        return metodo == MetodoPago.EFECTIVO;
    }

    @Override
    public Pago cobrar(String ordenId, BigDecimal monto) {
        return new Pago(MetodoPago.EFECTIVO, monto, UUID.randomUUID().toString());
    }
}
