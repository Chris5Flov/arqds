package com.restaurante.infrastructure.payment;

import com.restaurante.application.port.out.PuertoCobro;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import java.math.BigDecimal;
import java.util.UUID;

/** Esqueleto: simula el cobro. TODO: integrar con el proveedor real si hace falta. */
public class PagoPasarelaDigitalAdapter implements PuertoCobro {
    @Override
    public boolean soporta(MetodoPago metodo) {
        return metodo == MetodoPago.PASARELA_DIGITAL;
    }

    @Override
    public Pago cobrar(String ordenId, BigDecimal monto) {
        return new Pago(MetodoPago.PASARELA_DIGITAL, monto, UUID.randomUUID().toString());
    }
}
