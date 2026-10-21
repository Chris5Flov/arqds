package com.restaurante.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Utilidades de las pruebas para crear pagos y cerrar órdenes sin repetir código. */
public final class PagosDePrueba {

    private PagosDePrueba() {
    }

    public static Pago pago(String ordenId, EstadoPago estado, BigDecimal monto) {
        return new Pago("PG-" + estado, ordenId, LocalDateTime.now(), estado, MetodoPago.EFECTIVO, monto, "ref");
    }

    public static Pago aprobado(Orden orden) {
        return pago(orden.getId(), EstadoPago.APROBADO, orden.calcularTotales().total());
    }

    /** Agrega una línea si hace falta, registra un pago aprobado por el total y cierra la orden. */
    public static void cerrar(Orden orden) {
        if (orden.getLineasActivas().isEmpty()) {
            orden.agregarLinea(new Plato("PX", "Plato", BigDecimal.TEN, BigDecimal.ZERO), 1, null);
        }
        orden.registrarPago(aprobado(orden));
        orden.cerrar();
    }
}
