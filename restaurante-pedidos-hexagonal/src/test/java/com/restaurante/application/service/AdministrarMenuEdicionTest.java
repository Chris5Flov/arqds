package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoModificador;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.persistence.ModificadorRepositoryMemoria;
import com.restaurante.infrastructure.persistence.PlatoRepositoryMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF1: editar y eliminar platos y modificadores del menú. */
class AdministrarMenuEdicionTest {

    private PlatoRepositoryMemoria platos;
    private ModificadorRepositoryMemoria modificadores;
    private AdministrarMenuService servicio;

    @BeforeEach
    void setUp() {
        platos = new PlatoRepositoryMemoria();
        modificadores = new ModificadorRepositoryMemoria();
        servicio = new AdministrarMenuService(platos, modificadores);
        servicio.registrarPlato(plato("P1", "Tacos", "85", "0.16"));
        servicio.registrarModificador(modificador("M1", "Queso", TipoModificador.EXTRA, "10"));
    }

    private static Plato plato(String id, String nombre, String precio, String tasa) {
        return new Plato(id, nombre, new BigDecimal(precio), new BigDecimal(tasa));
    }

    private static Modificador modificador(String id, String nombre, TipoModificador tipo, String costo) {
        return new Modificador(id, nombre, tipo, new BigDecimal(costo));
    }

    // ---------------------------------------------------------------- platos

    @Test
    void actualizaNombrePrecioYTasaDeUnPlato() {
        Plato editado = plato("P1", "Tacos de suadero", "90.50", "0");

        assertEquals(editado, servicio.actualizarPlato(editado));
        assertEquals(editado, platos.buscarPorId("P1").get());
        assertEquals(1, servicio.listarPlatos().size());
    }

    @Test
    void actualizarAplicaLasMismasValidacionesQueRegistrar() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.actualizarPlato(plato("P1", "Tacos", "-1", "0.16")));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.actualizarPlato(plato("P1", "Tacos", "85", "16")));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.actualizarPlato(plato("P1", " ", "85", "0.16")));

        assertEquals("Tacos", platos.buscarPorId("P1").get().nombre());
    }

    @Test
    void noSePuedeActualizarUnPlatoQueNoExiste() {
        ReglaDeNegocioException error = assertThrows(ReglaDeNegocioException.class,
                () -> servicio.actualizarPlato(plato("P9", "Pozole", "120", "0.16")));

        assertEquals("No existe el plato P9", error.getMessage());
        assertTrue(platos.buscarPorId("P9").isEmpty());
    }

    @Test
    void eliminaUnPlato() {
        servicio.eliminarPlato("P1");

        assertTrue(servicio.listarPlatos().isEmpty());
    }

    @Test
    void noSePuedeEliminarUnPlatoQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.eliminarPlato("P9"));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.eliminarPlato(null));
    }

    @Test
    void editarOEliminarUnPlatoNoCambiaLasOrdenesQueYaLoTienen() {
        Orden orden = Orden.abrir(TipoOrden.SALA, "Mesa 1");
        LineaPedido linea = orden.agregarLinea(platos.buscarPorId("P1").get(), 2, List.of());

        servicio.actualizarPlato(plato("P1", "Tacos", "200", "0.16"));
        servicio.eliminarPlato("P1");

        assertEquals(new BigDecimal("85"), linea.getPrecioUnitario());
        assertEquals(new BigDecimal("197.20"), orden.calcularTotales().total());
    }

    // ---------------------------------------------------------------- modificadores

    @Test
    void actualizaNombreTipoYCostoDeUnModificador() {
        Modificador editado = modificador("M1", "Sin queso", TipoModificador.EXCLUSION, "0");

        assertEquals(editado, servicio.actualizarModificador(editado));
        assertEquals(editado, modificadores.buscarPorId("M1").get());
    }

    @Test
    void actualizarModificadorAplicaLasMismasValidacionesQueRegistrar() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.actualizarModificador(modificador("M1", "Queso", TipoModificador.EXCLUSION, "5")));
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.actualizarModificador(modificador("M1", "Queso", TipoModificador.EXTRA, "-1")));

        assertEquals(new BigDecimal("10"), modificadores.buscarPorId("M1").get().costoExtra());
    }

    @Test
    void noSePuedeActualizarUnModificadorQueNoExiste() {
        ReglaDeNegocioException error = assertThrows(ReglaDeNegocioException.class,
                () -> servicio.actualizarModificador(modificador("M9", "Crema", TipoModificador.EXTRA, "5")));

        assertEquals("No existe el modificador M9", error.getMessage());
    }

    @Test
    void eliminaUnModificador() {
        servicio.eliminarModificador("M1");

        assertTrue(servicio.listarModificadores().isEmpty());
        assertThrows(ReglaDeNegocioException.class, () -> servicio.eliminarModificador("M1"));
    }
}
