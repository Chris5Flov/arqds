package com.restaurante.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF3: ciclo de cocina PENDIENTE -> EN_COCCION -> LISTO, modificación y cancelación de una línea. */
class LineaPedidoTest {

    private Plato tacos;
    private LineaPedido linea;

    @BeforeEach
    void setUp() {
        tacos = new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16"));
        linea = new LineaPedido("L1", tacos, 1, List.of());
    }

    private static Modificador queso(String costo) {
        return new Modificador("M1", "Queso", TipoModificador.EXTRA, new BigDecimal(costo));
    }

    @Test
    void empiezaPendienteYEditable() {
        assertEquals(EstadoCocina.PENDIENTE, linea.getEstadoCocina());
        assertTrue(linea.esEditable());
    }

    @Test
    void copiaPrecioYTasaDelPlatoAlCrearse() {
        assertEquals(new BigDecimal("85"), linea.getPrecioUnitario());
        assertEquals(new BigDecimal("0.16"), linea.getTasaImpuestoAplicada());
    }

    @Test
    void guardaElCostoExtraVigenteDeCadaModificador() {
        LineaPedido conQueso = new LineaPedido("L2", tacos, 1, List.of(queso("12.50")));

        ModificadorAplicado aplicado = conQueso.getModificadores().get(0);
        assertEquals("Queso", aplicado.nombre());
        assertEquals(new BigDecimal("12.50"), aplicado.costoExtraAplicado());
    }

    @Test
    void modificadoresNulosSeTomanComoNinguno() {
        LineaPedido sinModificadores = new LineaPedido("L2", tacos, 1, null);

        assertTrue(sinModificadores.getModificadores().isEmpty());
    }

    @Test
    void rechazaPlatoNulo() {
        assertThrows(ReglaDeNegocioException.class, () -> new LineaPedido("L2", null, 1, List.of()));
    }

    @Test
    void rechazaCantidadNoPositiva() {
        assertThrows(ReglaDeNegocioException.class, () -> new LineaPedido("L2", tacos, 0, List.of()));
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

    @Test
    void modificarCambiaCantidadYModificadoresMientrasEstaPendiente() {
        linea.modificar(3, List.of(queso("10")));

        assertEquals(3, linea.getCantidad());
        assertEquals(1, linea.getModificadores().size());
    }

    @Test
    void modificarRechazaCantidadNoPositivaYNoCambiaNada() {
        assertThrows(ReglaDeNegocioException.class, () -> linea.modificar(0, List.of(queso("10"))));

        assertEquals(1, linea.getCantidad());
        assertTrue(linea.getModificadores().isEmpty());
    }

    @Test
    void noSePuedeModificarUnaLineaEnCoccion() {
        linea.iniciarCoccion();

        assertThrows(ReglaDeNegocioException.class, () -> linea.modificar(2, List.of()));
        assertEquals(1, linea.getCantidad());
    }

    @Test
    void cancelarLaDejaCanceladaYYaNoEsEditable() {
        linea.cancelar();

        assertEquals(EstadoCocina.CANCELADA, linea.getEstadoCocina());
        assertTrue(linea.estaCancelada());
        assertFalse(linea.esEditable());
        assertThrows(ReglaDeNegocioException.class, linea::iniciarCoccion);
    }

    @Test
    void noSePuedeCancelarUnaLineaEnCoccion() {
        linea.iniciarCoccion();

        assertThrows(ReglaDeNegocioException.class, linea::cancelar);
        assertEquals(EstadoCocina.EN_COCCION, linea.getEstadoCocina());
    }
}
