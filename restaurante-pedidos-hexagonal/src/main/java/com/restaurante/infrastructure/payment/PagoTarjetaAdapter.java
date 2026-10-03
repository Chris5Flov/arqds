package com.restaurante.infrastructure.payment;

import com.restaurante.application.port.out.PuertoCobro;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import java.math.BigDecimal;
import java.util.UUID;

/** Esqueleto: simula el cobro. TODO: integrar con el proveedor real si hace falta. */
public class PagoTarjetaAdapter implements PuertoCobro {
    @Override
    public boolean soporta(MetodoPago metodo) {
        return metodo == MetodoPago.TARJETA;
    }

    @Override
    public Pago cobrar(String ordenId, BigDecimal monto) {
        return new Pago(MetodoPago.TARJETA, monto, UUID.randomUUID().toString());
    }
}
