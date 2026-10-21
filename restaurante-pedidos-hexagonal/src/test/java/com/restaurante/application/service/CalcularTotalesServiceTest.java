package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.restaurante.domain.Orden;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import com.restaurante.domain.Totales;
import com.restaurante.infrastructure.persistence.RepositorioOrdenesEnMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalcularTotalesServiceTest {

    private RepositorioOrdenesEnMemoria repositorio;
    private CalcularTotalesService servicio;
    private Orden orden;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioOrdenesEnMemoria();
        servicio = new CalcularTotalesService(repositorio);
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        orden.agregarLinea(new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16")), 2, List.of());
        repositorio.guardar(orden);
    }

    @Test
    void calculaLosTotalesDeLaOrdenConPropina() {
        Totales t = servicio.calcular(orden.getId(), BigDecimal.ZERO, new BigDecimal("10"));

        assertEquals(new BigDecimal("170.00"), t.subtotal());
        assertEquals(new BigDecimal("0.00"), t.descuento());
        assertEquals(new BigDecimal("27.20"), t.impuestos());
        assertEquals(new BigDecimal("10.00"), t.propina());
        assertEquals(new BigDecimal("207.20"), t.total());
    }

    @Test
    void aplicaElDescuentoAntesDelImpuesto() {
        // 170 - 17 = 153 gravable al 16 % = 24.48
        Totales t = servicio.calcular(orden.getId(), new BigDecimal("17"), BigDecimal.ZERO);

        assertEquals(new BigDecimal("17.00"), t.descuento());
        assertEquals(new BigDecimal("24.48"), t.impuestos());
        assertEquals(new BigDecimal("177.48"), t.total());
    }

    @Test
    void descuentoYPropinaNulosSeTomanComoCero() {
        Totales t = servicio.calcular(orden.getId(), null, null);

        assertEquals(new BigDecimal("0.00"), t.descuento());
        assertEquals(new BigDecimal("0.00"), t.propina());
        assertEquals(new BigDecimal("197.20"), t.total());
    }

    @Test
    void rechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcular("no-existe", BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void rechazaUnaPropinaNegativa() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcular(orden.getId(), BigDecimal.ZERO, new BigDecimal("-1")));
    }

    @Test
    void rechazaUnDescuentoMayorAlSubtotal() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcular(orden.getId(), new BigDecimal("170.01"), BigDecimal.ZERO));
    }
}
