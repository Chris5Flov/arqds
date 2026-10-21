package com.restaurante.domain;

import static com.restaurante.domain.PagosDePrueba.pago;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF5: la orden se cierra únicamente con un pago aprobado que cubra el total. */
class OrdenCerrarTest {

    private Orden orden;

    @BeforeEach
    void setUp() {
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        // 100 al 16 % = 116.00
        orden.agregarLinea(new Plato("P1", "Plato", new BigDecimal("100"), new BigDecimal("0.16")), 1, List.of());
    }

    private static BigDecimal dinero(String valor) {
        return new BigDecimal(valor);
    }

    // ---------------------------------------------------------------- registrar pago

    @Test
    void registraLosPagosDeLaOrden() {
        Pago rechazado = pago(orden.getId(), EstadoPago.RECHAZADO, dinero("116.00"));
        Pago aprobado = pago(orden.getId(), EstadoPago.APROBADO, dinero("116.00"));

        orden.registrarPago(rechazado);
        orden.registrarPago(aprobado);

        assertEquals(List.of(rechazado, aprobado), orden.getPagos());
        assertEquals(aprobado, orden.getPagoAprobado().orElseThrow());
    }

    @Test
    void rechazaUnPagoNulo() {
        assertThrows(ReglaDeNegocioException.class, () -> orden.registrarPago(null));
    }

    @Test
    void rechazaUnPagoDeOtraOrden() {
        assertThrows(ReglaDeNegocioException.class,
                () -> orden.registrarPago(pago("otra-orden", EstadoPago.APROBADO, dinero("116.00"))));
        assertTrue(orden.getPagos().isEmpty());
    }

    @Test
    void permiteMaximoUnPagoAprobado() {
        orden.registrarPago(pago(orden.getId(), EstadoPago.APROBADO, dinero("116.00")));

        assertThrows(ReglaDeNegocioException.class,
                () -> orden.registrarPago(pago(orden.getId(), EstadoPago.APROBADO, dinero("116.00"))));
        assertEquals(1, orden.getPagos().size());
    }

    // ---------------------------------------------------------------- cerrar

    @Test
    void cerrarConUnPagoAprobadoGuardaLosTotalesYCambiaElEstado() {
        orden.registrarPago(pago(orden.getId(), EstadoPago.APROBADO, dinero("116.00")));

        orden.cerrar();

        assertEquals(EstadoOrden.CERRADA, orden.getEstado());
        assertEquals(dinero("116.00"), orden.getTotales().orElseThrow().total());
    }

    @Test
    void cerrarConDescuentoYPropinaUsaEsosTotales() {
        // (100 - 10) al 16 % = 104.40 + 5 de propina = 109.40
        orden.registrarPago(pago(orden.getId(), EstadoPago.APROBADO, dinero("109.40")));

        orden.cerrar(dinero("10"), dinero("5"));

        Totales t = orden.getTotales().orElseThrow();
        assertEquals(dinero("10.00"), t.descuento());
        assertEquals(dinero("5.00"), t.propina());
        assertEquals(dinero("109.40"), t.total());
    }

    @Test
    void rechazaCerrarSinPagos() {
        assertThrows(ReglaDeNegocioException.class, orden::cerrar);
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    @Test
    void rechazaCerrarSoloConPagosRechazados() {
        orden.registrarPago(pago(orden.getId(), EstadoPago.RECHAZADO, dinero("116.00")));

        assertThrows(ReglaDeNegocioException.class, orden::cerrar);
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    @Test
    void rechazaCerrarSiElPagoNoCubreElTotal() {
        orden.registrarPago(pago(orden.getId(), EstadoPago.APROBADO, dinero("115.99")));

        assertThrows(ReglaDeNegocioException.class, orden::cerrar);
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
        assertTrue(orden.getTotales().isEmpty());
    }

    @Test
    void rechazaCerrarUnaOrdenSinLineas() {
        Orden vacia = Orden.abrir(TipoOrden.PARA_LLEVAR, "Pedido 1");
        vacia.registrarPago(pago(vacia.getId(), EstadoPago.APROBADO, dinero("0.00")));

        assertThrows(ReglaDeNegocioException.class, vacia::cerrar);
    }

    @Test
    void noSePuedeCerrarDosVeces() {
        orden.registrarPago(pago(orden.getId(), EstadoPago.APROBADO, dinero("116.00")));
        orden.cerrar();

        assertThrows(ReglaDeNegocioException.class, orden::cerrar);
    }

    @Test
    void noSeRegistranPagosEnUnaOrdenCerrada() {
        orden.registrarPago(pago(orden.getId(), EstadoPago.APROBADO, dinero("116.00")));
        orden.cerrar();

        assertThrows(ReglaDeNegocioException.class,
                () -> orden.registrarPago(pago(orden.getId(), EstadoPago.RECHAZADO, dinero("1"))));
    }

    // ---------------------------------------------------------------- totales para cobrar

    @Test
    void totalesParaCobrarDevuelveElMontoACobrar() {
        assertEquals(dinero("121.00"), orden.totalesParaCobrar(BigDecimal.ZERO, dinero("5")).total());
    }

    @Test
    void totalesParaCobrarRechazaUnaOrdenSinLineasActivas() {
        orden.cancelarLinea(orden.getLineas().get(0).getId());

        assertThrows(ReglaDeNegocioException.class, () -> orden.totalesParaCobrar(null, null));
    }

    @Test
    void totalesParaCobrarRechazaUnaOrdenCancelada() {
        orden.cancelar();

        assertThrows(ReglaDeNegocioException.class, () -> orden.totalesParaCobrar(null, null));
    }
}
