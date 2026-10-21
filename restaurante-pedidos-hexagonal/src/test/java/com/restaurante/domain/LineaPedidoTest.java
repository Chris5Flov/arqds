package com.restaurante.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF3: ciclo de cocina PENDIENTE -> EN_COCCION -> LISTO de una línea. */
class LineaPedidoTest {

    private LineaPedido linea;

    @BeforeEach
    void setUp() {
        Plato tacos = new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16"));
        linea = new LineaPedido("L1", tacos, 1, List.of());
    }

    @Test
    void empiezaPendienteYEditable() {
        assertEquals(EstadoCocina.PENDIENTE, linea.getEstadoCocina());
        assertTrue(linea.esEditable());
    }

    @Test
    void enCoccionDejaDeSerEditable() {
        linea.iniciarCoccion();

        assertEquals(EstadoCocina.EN_COCCION, linea.getEstadoCocina());
        assertFalse(linea.esEditable());
    }

    @Test
    void listaNoEsEditable() {
        linea.iniciarCoccion();
        linea.marcarLista();

        assertEquals(EstadoCocina.LISTO, linea.getEstadoCocina());
        assertFalse(linea.esEditable());
    }

    @Test
    void noSePuedeMarcarListaSinHaberIniciadoLaCoccion() {
        assertThrows(ReglaDeNegocioException.class, linea::marcarLista);
        assertEquals(EstadoCocina.PENDIENTE, linea.getEstadoCocina());
    }

    @Test
    void noSePuedeIniciarLaCoccionDosVeces() {
        linea.iniciarCoccion();

        assertThrows(ReglaDeNegocioException.class, linea::iniciarCoccion);
        assertEquals(EstadoCocina.EN_COCCION, linea.getEstadoCocina());
    }

    @Test
    void unaLineaListaNoRetrocedeNiSeVuelveAMarcar() {
        linea.iniciarCoccion();
        linea.marcarLista();

        assertThrows(ReglaDeNegocioException.class, linea::iniciarCoccion);
        assertThrows(ReglaDeNegocioException.class, linea::marcarLista);
        assertEquals(EstadoCocina.LISTO, linea.getEstadoCocina());
    }
}
