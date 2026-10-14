package com.restaurante.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdenCerrarTest {

    private Orden orden;

    @BeforeEach
    void setUp() {
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
    }

    private Pago pagoDe(String ordenId) {
        return new Pago("PG1", ordenId, MetodoPago.EFECTIVO, new BigDecimal("100.00"), "ref-1");
    }

    @Test
    void cerrarConSuPagoCambiaElEstadoYGuardaElPago() {
        Pago pago = pagoDe(orden.getId());

        orden.cerrar(pago);

        assertEquals(EstadoOrden.CERRADA, orden.getEstado());
        assertSame(pago, orden.getPago());
    }

    @Test
    void unaOrdenAbiertaNoTienePago() {
        assertNull(orden.getPago());
    }

    @Test
    void rechazaCerrarSinPago() {
        assertThrows(ReglaDeNegocioException.class, () -> orden.cerrar(null));
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    @Test
    void rechazaUnPagoDeOtraOrden() {
        Pago ajeno = pagoDe("otra-orden");

        assertThrows(ReglaDeNegocioException.class, () -> orden.cerrar(ajeno));
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
        assertNull(orden.getPago());
    }

    @Test
    void noSePuedeCerrarDosVeces() {
        orden.cerrar(pagoDe(orden.getId()));

        assertThrows(ReglaDeNegocioException.class, () -> orden.cerrar(pagoDe(orden.getId())));
    }
}
