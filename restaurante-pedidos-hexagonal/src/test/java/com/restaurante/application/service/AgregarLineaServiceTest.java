package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.application.port.in.AgregarLineaUseCase.Comando;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoModificador;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.persistence.RepositorioMenuEnMemoria;
import com.restaurante.infrastructure.persistence.RepositorioOrdenesEnMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgregarLineaServiceTest {

    private RepositorioOrdenesEnMemoria ordenes;
    private AgregarLineaService servicio;
    private Plato tacos;
    private Orden orden;

    @BeforeEach
    void setUp() {
        ordenes = new RepositorioOrdenesEnMemoria();
        RepositorioMenuEnMemoria menu = new RepositorioMenuEnMemoria();
        servicio = new AgregarLineaService(ordenes, menu);

        tacos = new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16"));
        menu.guardar(tacos);
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        ordenes.guardar(orden);
    }

    @Test
    void agregaUnaLineaConElPlatoDelMenu() {
        Modificador queso = new Modificador("M1", "Queso", TipoModificador.EXTRA, BigDecimal.TEN);

        Orden resultado = servicio.agregar(new Comando(orden.getId(), "P1", 2, List.of(queso)));

        LineaPedido linea = resultado.getLineas().get(0);
        assertEquals(1, resultado.getLineas().size());
        assertSame(tacos, linea.getPlato());
        assertEquals(2, linea.getCantidad());
        assertEquals(List.of(queso), linea.getModificadores());
        assertEquals(1, ordenes.buscarPorId(orden.getId()).get().getLineas().size());
    }

    @Test
    void rechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregar(new Comando("no-existe", "P1", 1, List.of())));
    }

    @Test
    void rechazaUnPlatoQueNoEstaEnElMenu() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregar(new Comando(orden.getId(), "no-existe", 1, List.of())));
        assertTrue(orden.getLineas().isEmpty());
    }

    @Test
    void rechazaCantidadNoPositiva() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregar(new Comando(orden.getId(), "P1", 0, List.of())));
        assertTrue(orden.getLineas().isEmpty());
    }

    @Test
    void rechazaAgregarAUnaOrdenCerrada() {
        orden.cerrar(new Pago("PG1", orden.getId(), MetodoPago.EFECTIVO, BigDecimal.TEN, "ref"));

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.agregar(new Comando(orden.getId(), "P1", 1, List.of())));
    }
}
