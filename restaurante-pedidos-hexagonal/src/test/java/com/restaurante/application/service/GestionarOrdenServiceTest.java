package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.domain.EstadoCocina;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.ModificadorAplicado;
import com.restaurante.domain.Orden;
import com.restaurante.domain.PagosDePrueba;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoModificador;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.persistence.ModificadorRepositoryMemoria;
import com.restaurante.infrastructure.persistence.OrdenRepositoryMemoria;
import com.restaurante.infrastructure.persistence.PlatoRepositoryMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GestionarOrdenServiceTest {

    private OrdenRepositoryMemoria ordenes;
    private GestionarOrdenService servicio;
    private Plato tacos;
    private Modificador queso;
    private Orden orden;

    @BeforeEach
    void setUp() {
        ordenes = new OrdenRepositoryMemoria();
        PlatoRepositoryMemoria platos = new PlatoRepositoryMemoria();
        ModificadorRepositoryMemoria modificadores = new ModificadorRepositoryMemoria();
        servicio = new GestionarOrdenService(ordenes, platos, modificadores);

        tacos = new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16"));
        platos.guardar(tacos);
        queso = new Modificador("M1", "Queso", TipoModificador.EXTRA, BigDecimal.TEN);
        modificadores.guardar(queso);
        orden = servicio.abrir(TipoOrden.SALA, "Mesa 5");
    }

    // ---------------------------------------------------------------- abrir

    @Test
    void abrirGuardaLaOrdenEnElRepositorio() {
        assertSame(orden, ordenes.buscarPorId(orden.getId()).orElseThrow());
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    @Test
    void abrirRechazaDatosInvalidosYNoGuardaNada() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.abrir(null, "Mesa 1"));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.abrir(TipoOrden.DELIVERY, " "));
        assertEquals(1, ordenes.listar().size());
    }

    // ---------------------------------------------------------------- agregar

    @Test
    void agregaUnaLineaConElPlatoYLosModificadoresDelCatalogo() {
        Orden resultado = servicio.agregarLinea(orden.getId(), "P1", 2, List.of("M1"));

        LineaPedido linea = resultado.getLineas().get(0);
        assertEquals(1, resultado.getLineas().size());
        assertSame(tacos, linea.getPlato());
        assertEquals(2, linea.getCantidad());
        assertEquals(List.of(ModificadorAplicado.de(queso)), linea.getModificadores());
    }

    @Test
    void agregarSinModificadoresAceptaListaVaciaONula() {
        servicio.agregarLinea(orden.getId(), "P1", 1, List.of());
        servicio.agregarLinea(orden.getId(), "P1", 1, null);

        assertEquals(2, orden.getLineas().size());
    }

    @Test
    void agregarRechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregarLinea("no-existe", "P1", 1, List.of()));
    }

    @Test
    void agregarRechazaUnPlatoQueNoEstaEnElMenu() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregarLinea(orden.getId(), "no-existe", 1, List.of()));
        assertTrue(orden.getLineas().isEmpty());
    }

    @Test
    void agregarRechazaUnModificadorQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregarLinea(orden.getId(), "P1", 1, List.of("no-existe")));
        assertTrue(orden.getLineas().isEmpty());
    }

    @Test
    void agregarRechazaCantidadNoPositiva() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregarLinea(orden.getId(), "P1", 0, List.of()));
        assertTrue(orden.getLineas().isEmpty());
    }

    @Test
    void agregarRechazaUnaOrdenCerrada() {
        PagosDePrueba.cerrar(orden);

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregarLinea(orden.getId(), "P1", 1, List.of()));
    }

    // ---------------------------------------------------------------- modificar

    @Test
    void modificaCantidadYModificadoresDeUnaLineaPendiente() {
        LineaPedido linea = servicio.agregarLinea(orden.getId(), "P1", 1, List.of()).getLineas().get(0);

        servicio.modificarLinea(orden.getId(), linea.getId(), 3, List.of("M1"));

        assertEquals(3, linea.getCantidad());
        assertEquals(List.of(ModificadorAplicado.de(queso)), linea.getModificadores());
    }

    @Test
    void modificarRechazaUnaLineaQueYaEstaEnCocina() {
        LineaPedido linea = servicio.agregarLinea(orden.getId(), "P1", 1, List.of()).getLineas().get(0);
        orden.iniciarCoccionLinea(linea.getId());

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.modificarLinea(orden.getId(), linea.getId(), 2, List.of()));
        assertEquals(1, linea.getCantidad());
    }

    @Test
    void modificarRechazaUnModificadorQueNoExisteSinCambiarLaLinea() {
        LineaPedido linea = servicio.agregarLinea(orden.getId(), "P1", 1, List.of()).getLineas().get(0);

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.modificarLinea(orden.getId(), linea.getId(), 5, List.of("no-existe")));
        assertEquals(1, linea.getCantidad());
    }

    // ---------------------------------------------------------------- cancelar

    @Test
    void cancelaUnaLineaPendiente() {
        Orden conLineas = servicio.agregarLinea(orden.getId(), "P1", 1, List.of());
        LineaPedido linea = conLineas.getLineas().get(0);

        Orden resultado = servicio.cancelarLinea(orden.getId(), linea.getId());

        assertEquals(EstadoCocina.CANCELADA, linea.getEstadoCocina());
        assertTrue(resultado.getLineasActivas().isEmpty());
    }

    @Test
    void cancelarLineaRechazaUnaOrdenOLineaQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.cancelarLinea("no-existe", "L1"));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.cancelarLinea(orden.getId(), "no-existe"));
    }

    @Test
    void cancelaUnaOrdenCompleta() {
        servicio.agregarLinea(orden.getId(), "P1", 1, List.of());

        Orden resultado = servicio.cancelarOrden(orden.getId());

        assertEquals(EstadoOrden.CANCELADA, resultado.getEstado());
        assertTrue(servicio.listarAbiertas().isEmpty());
    }

    @Test
    void noCancelaUnaOrdenConLineasEnCocina() {
        LineaPedido linea = servicio.agregarLinea(orden.getId(), "P1", 1, List.of()).getLineas().get(0);
        orden.iniciarCoccionLinea(linea.getId());

        assertThrows(ReglaDeNegocioException.class, () -> servicio.cancelarOrden(orden.getId()));
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    // ---------------------------------------------------------------- consultar

    @Test
    void consultaUnaOrdenPorId() {
        assertSame(orden, servicio.consultar(orden.getId()));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.consultar("no-existe"));
    }

    @Test
    void listaSoloLasOrdenesAbiertas() {
        Orden otra = servicio.abrir(TipoOrden.DELIVERY, "Pedido 77");
        Orden cerrada = servicio.abrir(TipoOrden.PARA_LLEVAR, "Pedido 78");
        PagosDePrueba.cerrar(cerrada);

        assertEquals(List.of(orden, otra), servicio.listarAbiertas());
        assertEquals(List.of(orden, otra, cerrada), servicio.listar());
    }
}
