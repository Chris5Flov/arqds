package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.application.port.in.GestionarCocinaUseCase.LineaEnCocina;
import com.restaurante.domain.EstadoCocina;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.persistence.RepositorioOrdenesEnMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GestionarCocinaServiceTest {

    private RepositorioOrdenesEnMemoria ordenes;
    private GestionarCocinaService servicio;
    private Orden orden;
    private LineaPedido linea;

    @BeforeEach
    void setUp() {
        ordenes = new RepositorioOrdenesEnMemoria();
        servicio = new GestionarCocinaService(ordenes);

        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        linea = orden.agregarLinea(new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16")), 2, List.of());
        ordenes.guardar(orden);
    }

    @Test
    void listaLasLineasPendientesConSusDatos() {
        List<LineaEnCocina> pendientes = servicio.listarPendientes();

        assertEquals(List.of(new LineaEnCocina(orden.getId(), linea.getId(), "Mesa 5", "Tacos", 2,
                EstadoCocina.PENDIENTE)), pendientes);
    }

    @Test
    void avanzaLaLineaHastaListoYDejaDeListarla() {
        servicio.iniciarCoccion(orden.getId(), linea.getId());
        assertEquals(EstadoCocina.EN_COCCION, servicio.listarPendientes().get(0).estado());

        servicio.marcarLista(orden.getId(), linea.getId());
        assertEquals(EstadoCocina.LISTO, linea.getEstadoCocina());
        assertTrue(servicio.listarPendientes().isEmpty());
    }

    @Test
    void noListaLineasDeOrdenesCerradas() {
        orden.cerrar(new Pago("PG1", orden.getId(), MetodoPago.EFECTIVO, BigDecimal.TEN, "ref"));

        assertTrue(servicio.listarPendientes().isEmpty());
    }

    @Test
    void rechazaMarcarListaUnaLineaQueNoEstaEnCoccion() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.marcarLista(orden.getId(), linea.getId()));
        assertEquals(EstadoCocina.PENDIENTE, linea.getEstadoCocina());
    }

    @Test
    void rechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.iniciarCoccion("no-existe", linea.getId()));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.marcarLista("no-existe", linea.getId()));
    }

    @Test
    void rechazaUnaLineaQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.iniciarCoccion(orden.getId(), "no-existe"));
    }
}
