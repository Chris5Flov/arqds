package com.restaurante.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdenCalcularTotalesTest {

    private Orden orden;

    @BeforeEach
    void setUp() {
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
    }

    private Plato plato(String precio, String tasa) {
        return new Plato("P", "Plato", new BigDecimal(precio), new BigDecimal(tasa));
    }

    private Modificador extra(String costo) {
        return new Modificador("M", "Extra", TipoModificador.EXTRA, new BigDecimal(costo));
    }

    private static BigDecimal dinero(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void unaOrdenVaciaTotalizaCeroConDosDecimales() {
        Totales t = orden.calcularTotales();

        assertEquals(dinero("0.00"), t.subtotal());
        assertEquals(dinero("0.00"), t.descuento());
        assertEquals(dinero("0.00"), t.impuestos());
        assertEquals(dinero("0.00"), t.propina());
        assertEquals(dinero("0.00"), t.total());
    }

    @Test
    void sumaExtrasDeModificadoresPorCantidadEImpuestoPorPlato() {
        // (85 + 10) x 2 = 190.00 al 16 % ; 20 x 1 = 20.00 al 0 %
        orden.agregarLinea(plato("85", "0.16"), 2, List.of(extra("10")));
        orden.agregarLinea(plato("20", "0"), 1, List.of());

        Totales t = orden.calcularTotales();

        assertEquals(dinero("210.00"), t.subtotal());
        assertEquals(dinero("30.40"), t.impuestos());
        assertEquals(dinero("240.40"), t.total());
    }

    @Test
    void unaExclusionSinCostoNoCambiaElPrecio() {
        Modificador sinCebolla = new Modificador("M2", "Sin cebolla", TipoModificador.EXCLUSION, BigDecimal.ZERO);
        orden.agregarLinea(plato("50", "0.16"), 1, List.of(sinCebolla));

        assertEquals(dinero("50.00"), orden.calcularTotales().subtotal());
    }

    @Test
    void elDescuentoReduceLaBaseDelImpuesto() {
        orden.agregarLinea(plato("85", "0.16"), 2, List.of(extra("10")));
        orden.agregarLinea(plato("20", "0"), 1, List.of());

        // 10 % de descuento: la base gravable de cada plato baja en la misma proporción
        Totales t = orden.calcularTotales(dinero("21"), BigDecimal.ZERO);

        assertEquals(dinero("210.00"), t.subtotal());
        assertEquals(dinero("21.00"), t.descuento());
        assertEquals(dinero("27.36"), t.impuestos());
        assertEquals(dinero("216.36"), t.total());
    }

    @Test
    void laPropinaSeSumaAlTotal() {
        orden.agregarLinea(plato("100", "0.16"), 1, List.of());

        Totales t = orden.calcularTotales(BigDecimal.ZERO, dinero("15.5"));

        assertEquals(dinero("15.50"), t.propina());
        assertEquals(dinero("131.50"), t.total());
    }

    @Test
    void redondeaHalfUpADosDecimales() {
        // 12.50 x 0.01 = 0.125 -> 0.13
        orden.agregarLinea(plato("12.50", "0.01"), 1, List.of());

        Totales t = orden.calcularTotales();

        assertEquals(dinero("0.13"), t.impuestos());
        assertEquals(dinero("12.63"), t.total());
    }

    @Test
    void elImpuestoSeRedondeaPorCadaPlato() {
        // Cada línea genera 0.125 -> 0.13; redondeando por plato el total es 0.26 (no 0.25)
        orden.agregarLinea(plato("12.50", "0.01"), 1, List.of());
        orden.agregarLinea(plato("12.50", "0.01"), 1, List.of());

        assertEquals(dinero("0.26"), orden.calcularTotales().impuestos());
    }

    @Test
    void esDeterminista() {
        orden.agregarLinea(plato("33.33", "0.16"), 3, List.of(extra("1.11")));

        assertEquals(orden.calcularTotales(dinero("5"), dinero("2")),
                orden.calcularTotales(dinero("5"), dinero("2")));
    }

    @Test
    void lasLineasCanceladasNoCuentan() {
        LineaPedido linea = orden.agregarLinea(plato("85", "0.16"), 1, List.of());
        orden.agregarLinea(plato("20", "0"), 1, List.of());

        orden.cancelarLinea(linea.getId());

        assertEquals(dinero("20.00"), orden.calcularTotales().total());
    }

    @Test
    void rechazaDescuentoNegativo() {
        orden.agregarLinea(plato("100", "0.16"), 1, List.of());

        assertThrows(ReglaDeNegocioException.class,
                () -> orden.calcularTotales(dinero("-1"), BigDecimal.ZERO));
    }

    @Test
    void rechazaDescuentoMayorAlSubtotal() {
        orden.agregarLinea(plato("100", "0.16"), 1, List.of());

        assertThrows(ReglaDeNegocioException.class,
                () -> orden.calcularTotales(dinero("100.01"), BigDecimal.ZERO));
    }

    @Test
    void rechazaPropinaNegativa() {
        assertThrows(ReglaDeNegocioException.class,
                () -> orden.calcularTotales(BigDecimal.ZERO, dinero("-1")));
    }
}
