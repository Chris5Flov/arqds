package com.restaurante.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF2 y RF3: abrir la orden y agregar, modificar, cancelar y avanzar en cocina sus líneas. */
class OrdenLineasTest {

    private Orden orden;
    private Plato tacos;
    private Modificador queso;

    @BeforeEach
    void setUp() {
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        tacos = new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16"));
        queso = new Modificador("M1", "Queso", TipoModificador.EXTRA, BigDecimal.ONE);
    }

    // ---------------------------------------------------------------- abrir

    @Test
    void abrirCreaUnaOrdenAbiertaYSinLineas() {
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
        assertEquals(TipoOrden.SALA, orden.getTipo());
        assertEquals("Mesa 5", orden.getReferencia());
        assertNotNull(orden.getFechaCreacion());
        assertTrue(orden.getLineas().isEmpty());
        assertTrue(orden.getPagos().isEmpty());
        assertTrue(orden.getTotales().isEmpty());
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
        LineaPedido linea = orden.agregarLinea(tacos, 2, List.of(queso));

        assertEquals(List.of(linea), orden.getLineas());
        assertSame(tacos, linea.getPlato());
        assertEquals(2, linea.getCantidad());
        assertEquals(List.of(ModificadorAplicado.de(queso)), linea.getModificadores());
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
        PagosDePrueba.cerrar(orden);

        assertThrows(ReglaDeNegocioException.class, () -> orden.agregarLinea(tacos, 1, List.of()));
    }

    @Test
    void losModificadoresDeLaLineaNoCambianSiSeModificaLaListaOriginal() {
        List<Modificador> modificadores = new ArrayList<>();
        LineaPedido linea = orden.agregarLinea(tacos, 1, modificadores);

        modificadores.add(queso);

        assertTrue(linea.getModificadores().isEmpty());
    }

    @Test
    void lasLineasNoSePuedenModificarDesdeFuera() {
        orden.agregarLinea(tacos, 1, List.of());

        assertThrows(UnsupportedOperationException.class, () -> orden.getLineas().clear());
    }

    // ---------------------------------------------------------------- modificar

    @Test
    void modificarCambiaUnaLineaPendiente() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());

        orden.modificarLinea(linea.getId(), 3, List.of(queso));

        assertEquals(3, linea.getCantidad());
        assertEquals(List.of(ModificadorAplicado.de(queso)), linea.getModificadores());
    }

    @Test
    void noSePuedeModificarUnaLineaEnCoccion() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.iniciarCoccionLinea(linea.getId());

        assertThrows(ReglaDeNegocioException.class, () -> orden.modificarLinea(linea.getId(), 2, List.of()));
        assertEquals(1, linea.getCantidad());
    }

    @Test
    void modificarRechazaUnaLineaQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> orden.modificarLinea("no-existe", 1, List.of()));
    }

    @Test
    void noSePuedeModificarEnUnaOrdenCerrada() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        PagosDePrueba.cerrar(orden);

        assertThrows(ReglaDeNegocioException.class, () -> orden.modificarLinea(linea.getId(), 2, List.of()));
    }

    // ---------------------------------------------------------------- cancelar línea

    @Test
    void cancelarMarcaLaLineaComoCanceladaYLaSacaDeLasActivas() {
        LineaPedido primera = orden.agregarLinea(tacos, 1, List.of());
        LineaPedido segunda = orden.agregarLinea(tacos, 2, List.of());

        orden.cancelarLinea(primera.getId());

        assertEquals(EstadoCocina.CANCELADA, primera.getEstadoCocina());
        assertEquals(List.of(primera, segunda), orden.getLineas());
        assertEquals(List.of(segunda), orden.getLineasActivas());
    }

    @Test
    void noSePuedeCancelarUnaLineaEnCoccion() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.iniciarCoccionLinea(linea.getId());

        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea(linea.getId()));
        assertEquals(1, orden.getLineasActivas().size());
    }

    @Test
    void noSePuedeCancelarUnaLineaLista() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.iniciarCoccionLinea(linea.getId());
        orden.marcarLineaLista(linea.getId());

        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea(linea.getId()));
    }

    @Test
    void noSePuedeCancelarDosVecesLaMismaLinea() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.cancelarLinea(linea.getId());

        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea(linea.getId()));
    }

    @Test
    void cancelarRechazaUnaLineaQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea("no-existe"));
    }

    @Test
    void noSePuedeCancelarEnUnaOrdenCerrada() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        PagosDePrueba.cerrar(orden);

        assertThrows(ReglaDeNegocioException.class, () -> orden.cancelarLinea(linea.getId()));
        assertEquals(EstadoCocina.PENDIENTE, linea.getEstadoCocina());
    }

    // ---------------------------------------------------------------- cancelar orden

    @Test
    void cancelarLaOrdenCancelaSusLineasPendientes() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());

        orden.cancelar();

        assertEquals(EstadoOrden.CANCELADA, orden.getEstado());
        assertEquals(EstadoCocina.CANCELADA, linea.getEstadoCocina());
        assertThrows(ReglaDeNegocioException.class, () -> orden.agregarLinea(tacos, 1, List.of()));
    }

    @Test
    void noSePuedeCancelarLaOrdenSiHayLineasEnCocina() {
        LineaPedido linea = orden.agregarLinea(tacos, 1, List.of());
        orden.iniciarCoccionLinea(linea.getId());

        assertThrows(ReglaDeNegocioException.class, orden::cancelar);
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    @Test
    void noSePuedeCancelarUnaOrdenCerrada() {
        PagosDePrueba.cerrar(orden);

        assertThrows(ReglaDeNegocioException.class, orden::cancelar);
        assertEquals(EstadoOrden.CERRADA, orden.getEstado());
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
        PagosDePrueba.cerrar(orden);

        assertThrows(ReglaDeNegocioException.class, () -> orden.iniciarCoccionLinea(linea.getId()));
        assertEquals(EstadoCocina.PENDIENTE, linea.getEstadoCocina());
    }
}
