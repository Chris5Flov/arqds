package com.restaurante.infrastructure.payment;

import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.domain.EstadoPago;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simula una terminal bancaria: aprueba los cargos hasta un límite y rechaza los que lo exceden.
 * Para integrar una terminal real basta con otra implementación de {@link ProcesadorPago}.
 */
public class PagoTarjetaSimuladoAdapter implements ProcesadorPago {
    public static final BigDecimal LIMITE_POR_DEFECTO = new BigDecimal("20000.00");

    private final BigDecimal limite;

    public PagoTarjetaSimuladoAdapter() {
        this(LIMITE_POR_DEFECTO);
    }

    public PagoTarjetaSimuladoAdapter(BigDecimal limite) {
        this.limite = limite;
    }

    @Override
    public boolean soporta(MetodoPago metodo) {
        return metodo == MetodoPago.TARJETA;
    }

    @Override
    public Pago cobrar(String ordenId, BigDecimal monto) {
        EstadoPago estado = monto.compareTo(limite) <= 0 ? EstadoPago.APROBADO : EstadoPago.RECHAZADO;
        return new Pago(UUID.randomUUID().toString(), ordenId, LocalDateTime.now(), estado,
                MetodoPago.TARJETA, monto, "TAR-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
