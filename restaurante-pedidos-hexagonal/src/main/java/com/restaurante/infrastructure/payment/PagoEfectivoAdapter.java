package com.restaurante.infrastructure.payment;

import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.domain.EstadoPago;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** El efectivo lo recibe el cajero en el momento, así que el pago siempre queda APROBADO. */
public class PagoEfectivoAdapter implements ProcesadorPago {
    @Override
    public boolean soporta(MetodoPago metodo) {
        return metodo == MetodoPago.EFECTIVO;
    }

    @Override
    public Pago cobrar(String ordenId, BigDecimal monto) {
        return new Pago(UUID.randomUUID().toString(), ordenId, LocalDateTime.now(), EstadoPago.APROBADO,
                MetodoPago.EFECTIVO, monto, "EFE-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
