package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoModificador;
import com.restaurante.infrastructure.persistence.ModificadorRepositoryMemoria;
import com.restaurante.infrastructure.persistence.PlatoRepositoryMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdministrarMenuServiceTest {

    private PlatoRepositoryMemoria platos;
    private ModificadorRepositoryMemoria modificadores;
    private AdministrarMenuService servicio;

    @BeforeEach
    void setUp() {
        platos = new PlatoRepositoryMemoria();
        modificadores = new ModificadorRepositoryMemoria();
        servicio = new AdministrarMenuService(platos, modificadores);
    }

    private Plato plato(String id, String nombre, String precio, String tasa) {
        return new Plato(id, nombre,
                precio == null ? null : new BigDecimal(precio),
                tasa == null ? null : new BigDecimal(tasa));
    }

    private Modificador modificador(String id, String nombre, TipoModificador tipo, String costo) {
        return new Modificador(id, nombre, tipo, costo == null ? null : new BigDecimal(costo));
    }

    // ---------------------------------------------------------------- platos

    @Test
    void registraUnPlatoValido() {
        Plato tacos = plato("P1", "Tacos al pastor", "85.00", "0.16");

        Plato resultado = servicio.registrarPlato(tacos);

        assertEquals(tacos, resultado);
        assertTrue(platos.buscarPorId("P1").isPresent());
    }

    @Test
    void aceptaPlatoSinImpuesto() {
        servicio.registrarPlato(plato("P2", "Agua natural", "20", "0"));

        assertTrue(platos.buscarPorId("P2").isPresent());
    }

    @Test
    void rechazaPlatoNulo() {
        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(null));
    }

    @Test
    void rechazaPrecioCero() {
        Plato p = plato("P1", "Tacos", "0", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(p));
    }

    @Test
    void rechazaPrecioNegativo() {
        Plato p = plato("P1", "Tacos", "-10", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(p));
    }

    @Test
    void rechazaPrecioConMasDeDosDecimales() {
        Plato p = plato("P1", "Tacos", "85.999", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(p));
    }

    @Test
    void rechazaPrecioNulo() {
        Plato p = plato("P1", "Tacos", null, "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(p));
    }

    @Test
    void rechazaImpuestoNegativo() {
        Plato p = plato("P1", "Tacos", "85", "-0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(p));
    }

    @Test
    void rechazaImpuestoEscritoComoPorcentaje() {
        // 16 en lugar de 0.16
        Plato p = plato("P1", "Tacos", "85", "16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(p));
    }

    @Test
    void rechazaImpuestoNulo() {
        Plato p = plato("P1", "Tacos", "85", null);

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(p));
    }

    @Test
    void rechazaNombreVacio() {
        Plato p = plato("P1", "   ", "85", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(p));
    }

    @Test
    void rechazaIdRepetido() {
        servicio.registrarPlato(plato("P1", "Tacos", "85", "0.16"));
        Plato repetido = plato("P1", "Quesadilla", "60", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarPlato(repetido));
        assertEquals("Tacos", platos.buscarPorId("P1").get().nombre());
    }

    @Test
    void noGuardaNadaSiLaValidacionFalla() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.registrarPlato(plato("P1", "Tacos", "-1", "0.16")));

        assertTrue(servicio.listarPlatos().isEmpty());
    }

    @Test
    void listaLosPlatosOrdenadosPorNombre() {
        servicio.registrarPlato(plato("P1", "Tacos", "85", "0.16"));
        servicio.registrarPlato(plato("P2", "Agua", "20", "0"));
        servicio.registrarPlato(plato("P3", "Pozole", "120", "0.16"));

        List<String> nombres = servicio.listarPlatos().stream().map(Plato::nombre).toList();

        assertEquals(List.of("Agua", "Pozole", "Tacos"), nombres);
    }

    // ---------------------------------------------------------------- modificadores

    @Test
    void registraUnExtraConCosto() {
        Modificador queso = modificador("M1", "Queso extra", TipoModificador.EXTRA, "15.50");

        assertEquals(queso, servicio.registrarModificador(queso));
        assertTrue(modificadores.buscarPorId("M1").isPresent());
    }

    @Test
    void registraUnaExclusionSinCosto() {
        servicio.registrarModificador(modificador("M2", "Sin cebolla", TipoModificador.EXCLUSION, "0"));

        assertTrue(modificadores.buscarPorId("M2").isPresent());
    }

    @Test
    void rechazaUnaExclusionConCosto() {
        Modificador m = modificador("M2", "Sin cebolla", TipoModificador.EXCLUSION, "5");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarModificador(m));
    }

    @Test
    void rechazaCostoExtraNegativo() {
        Modificador m = modificador("M1", "Queso", TipoModificador.EXTRA, "-1");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarModificador(m));
    }

    @Test
    void rechazaCostoExtraConMasDeDosDecimales() {
        Modificador m = modificador("M1", "Queso", TipoModificador.EXTRA, "1.234");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarModificador(m));
    }

    @Test
    void rechazaModificadorSinTipoSinCostoOSinNombre() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.registrarModificador(modificador("M1", "Queso", null, "1")));
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.registrarModificador(modificador("M1", "Queso", TipoModificador.EXTRA, null)));
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.registrarModificador(modificador("M1", " ", TipoModificador.EXTRA, "1")));
        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarModificador(null));
        assertTrue(servicio.listarModificadores().isEmpty());
    }

    @Test
    void rechazaModificadorConIdRepetido() {
        servicio.registrarModificador(modificador("M1", "Queso", TipoModificador.EXTRA, "10"));

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrarModificador(
                modificador("M1", "Tocino", TipoModificador.EXTRA, "20")));
        assertEquals("Queso", modificadores.buscarPorId("M1").get().nombre());
    }

    @Test
    void listaLosModificadoresOrdenadosPorNombre() {
        servicio.registrarModificador(modificador("M1", "Tocino", TipoModificador.EXTRA, "20"));
        servicio.registrarModificador(modificador("M2", "Sin cebolla", TipoModificador.EXCLUSION, "0"));
        servicio.registrarModificador(modificador("M3", "Queso", TipoModificador.EXTRA, "10"));

        List<String> nombres = servicio.listarModificadores().stream().map(Modificador::nombre).toList();

        assertEquals(List.of("Queso", "Sin cebolla", "Tocino"), nombres);
    }
}
