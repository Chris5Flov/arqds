package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.persistence.RepositorioOrdenesEnMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CancelarLineaServiceTest {

    private RepositorioOrdenesEnMemoria ordenes;
    private CancelarLineaService servicio;
    private Orden orden;
    private LineaPedido tacos;
    private LineaPedido agua;

    @BeforeEach
    void setUp() {
        ordenes = new RepositorioOrdenesEnMemoria();
        servicio = new CancelarLineaService(ordenes);

        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        tacos = orden.agregarLinea(new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16")), 1, List.of());
        agua = orden.agregarLinea(new Plato("P2", "Agua", new BigDecimal("20"), BigDecimal.ZERO), 1, List.of());
        ordenes.guardar(orden);
    }

    @Test
    void cancelaUnaLineaPendiente() {
        Orden resultado = servicio.cancelar(orden.getId(), tacos.getId());

        assertEquals(List.of(agua), resultado.getLineas());
        assertEquals(List.of(agua), ordenes.buscarPorId(orden.getId()).get().getLineas());
    }

    @Test
    void rechazaCancelarUnaLineaQueYaEstaEnCocina() {
        orden.iniciarCoccionLinea(tacos.getId());

        assertThrows(ReglaDeNegocioException.class, () -> servicio.cancelar(orden.getId(), tacos.getId()));
        assertEquals(2, orden.getLineas().size());
    }

    @Test
    void rechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.cancelar("no-existe", tacos.getId()));
    }

    @Test
    void rechazaUnaLineaQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.cancelar(orden.getId(), "no-existe"));
        assertEquals(2, orden.getLineas().size());
    }
}
