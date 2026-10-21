package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.application.port.in.GestionarCocinaUseCase.LineaEnCocina;
import com.restaurante.domain.EstadoCocina;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.PagosDePrueba;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoModificador;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.persistence.OrdenRepositoryMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GestionarCocinaServiceTest {

    private OrdenRepositoryMemoria ordenes;
    private GestionarCocinaService servicio;
    private Orden orden;
    private LineaPedido linea;

    @BeforeEach
    void setUp() {
        ordenes = new OrdenRepositoryMemoria();
        servicio = new GestionarCocinaService(ordenes);

        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        Modificador sinCebolla = new Modificador("M2", "Sin cebolla", TipoModificador.EXCLUSION, BigDecimal.ZERO);
        linea = orden.agregarLinea(new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16")), 2,
                List.of(sinCebolla));
        ordenes.guardar(orden);
    }

    @Test
    void listaLasLineasPendientesConSusDatos() {
        List<LineaEnCocina> pendientes = servicio.listarPendientes();

        assertEquals(List.of(new LineaEnCocina(orden.getId(), linea.getId(), "Mesa 5", "Tacos", 2,
                List.of("Sin cebolla"), EstadoCocina.PENDIENTE)), pendientes);
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
    void noListaLineasCanceladas() {
        orden.cancelarLinea(linea.getId());

        assertTrue(servicio.listarPendientes().isEmpty());
    }

    @Test
    void noListaLineasDeOrdenesCerradas() {
        PagosDePrueba.cerrar(orden);

        assertTrue(servicio.listarPendientes().isEmpty());
    }

    @Test
    void rechazaIniciarLaCoccionDeUnaLineaCancelada() {
        orden.cancelarLinea(linea.getId());

        assertThrows(ReglaDeNegocioException.class, () -> servicio.iniciarCoccion(orden.getId(), linea.getId()));
        assertEquals(EstadoCocina.CANCELADA, linea.getEstadoCocina());
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
