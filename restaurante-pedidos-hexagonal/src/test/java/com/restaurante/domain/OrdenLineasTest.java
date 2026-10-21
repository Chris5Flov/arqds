package com.restaurante.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF3: agregar, cancelar y avanzar en cocina las líneas de una orden. */
class OrdenLineasTest {

    private Orden orden;
    private Plato tacos;

    @BeforeEach
    void setUp() {
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        tacos = new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16"));
    }

    private Pago pagoDe(Orden o) {
        return new Pago("PG1", o.getId(), MetodoPago.EFECTIVO, BigDecimal.TEN, "ref");
    }

    // ---------------------------------------------------------------- abrir

    @Test
    void abrirCreaUnaOrdenAbiertaYSinLineas() {
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
        assertEquals(TipoOrden.SALA, orden.getTipo());
        assertEquals("Mesa 5", orden.getReferencia());
        assertTrue(orden.getLineas().isEmpty());
    }

    @Test
    void abrirRechazaTipoNulo() {
        assertThrows(ReglaDeNegocioException.class, () -> Orden.abrir(null, "Mesa 5"));
    }

    @Test
    void abrirRechazaReferenciaVacia() {
        assertThrows(ReglaDeNegocioException.class, () -> Orden.abrir(TipoOrden.DELIVERY, "  "));
    }

    // ---------------------------------------------------------------- agregar

    @Test
    void agregarLineaLaDejaPendienteYEditable() {
        Modificador queso = new Modificador("M1", "Queso", TipoModificador.EXTRA, BigDecimal.ONE);

        LineaPedido linea = orden.agregarLinea(tacos, 2, List.of(queso));

        assertEquals(List.of(linea), orden.getLineas());
        assertSame(tacos, linea.getPlato());
        assertEquals(2, linea.getCantidad());
        assertEquals(List.of(queso), linea.getModificadores());
        assertEquals(EstadoCocina.PENDIENTE, linea.getEstadoCocina());
        assertTrue(linea.esEditable());
    }

    @Test
    void agregarLineaRechazaCantidadCero() {
        assertThrows(ReglaDeNegocioException.class, () -> orden.agregarLinea(tacos, 0, List.of()));
        assertTrue(orden.getLineas().isEmpty());
    }

    @Test
    void agregarLineaRechazaCantidadNegativa() {
        assertThrows(ReglaDeNegocioException.class, () -> orden.agregarLinea(tacos, -1, List.of()));
    }

    @Test
    void noSePuedeAgregarAUnaOrdenCerrada() {
        orden.cerrar(pagoDe(orden));

        assertThrows(ReglaDeNegocioException.class, () -> orden.agregarLinea(tacos, 1, List.of()));
    }

    @Test
    void losModificadoresDeLaLineaNoCambianSiSeModificaLaListaOriginal() {
        List<Modificador> modificadores = new ArrayList<>();
        LineaPedido linea = orden.agregarLinea(tacos, 1, modificadores);

        modificadores.add(new Modificador("M1", "Queso", TipoModificador.EXTRA, BigDecimal.ONE));

        assertTrue(linea.getModificadores().isEmpty());
    }

    @Test
    void lasLineasNoSePuedenModificarDesdeFuera() {
        orden.agregarLinea(tacos, 1, List.of());

        assertThrows(UnsupportedOperationException.class, () -> orden.getLineas().clear());
    }

    // ---------------------------------------------------------------- cancelar

    @Test
    void cancelarQuitaUnaLineaPendiente() {
        LineaPedido primera = orden.agregarLinea(tacos, 1, List.of());
        LineaPedido segunda = orden.agregarLinea(tacos, 2, List.of());

        orden.cancelarLinea(primera.getId());

        assertEquals(List.of(segunda), orden.getLineas());
    }

    @Test
    void noSePuedeCancelarUnaLineaEnCoccion() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.iniciarCoccionLinea(linea.getId());

        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea(linea.getId()));
        assertEquals(1, orden.getLineas().size());
    }

    @Test
    void noSePuedeCancelarUnaLineaLista() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.iniciarCoccionLinea(linea.getId());
        orden.marcarLineaLista(linea.getId());

        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea(linea.getId()));
    }

    @Test
    void cancelarRechazaUnaLineaQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea("no-existe"));
    }

    @Test
    void noSePuedeCancelarEnUnaOrdenCerrada() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.cerrar(pagoDe(orden));

        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea(linea.getId()));
        assertEquals(1, orden.getLineas().size());
    }

    // ---------------------------------------------------------------- cocina

    @Test
    void laCocinaAvanzaPendienteEnCoccionLista() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());

        orden.iniciarCoccionLinea(linea.getId());
        assertEquals(EstadoCocina.EN_COCCION, linea.getEstadoCocina());

        orden.marcarLineaLista(linea.getId());
        assertEquals(EstadoCocina.LISTO, linea.getEstadoCocina());
    }

    @Test
    void laCocinaRechazaUnaLineaQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> orden.iniciarCoccionLinea("no-existe"));
        assertThrows(ReglaDeNegocioException.class, () -> orden.marcarLineaLista("no-existe"));
    }

    @Test
    void laCocinaNoAvanzaLineasDeUnaOrdenCerrada() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.cerrar(pagoDe(orden));

        assertThrows(ReglaDeNegocioException.class, () -> orden.iniciarCoccionLinea(linea.getId()));
        assertEquals(EstadoCocina.PENDIENTE, linea.getEstadoCocina());
    }
}
