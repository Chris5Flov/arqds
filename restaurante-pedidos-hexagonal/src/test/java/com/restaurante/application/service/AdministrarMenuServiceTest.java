package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.infrastructure.persistence.RepositorioMenuEnMemoria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdministrarMenuServiceTest {

    private RepositorioMenuEnMemoria repositorio;
    private AdministrarMenuService servicio;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioMenuEnMemoria();
        servicio = new AdministrarMenuService(repositorio);
    }

    private Plato plato(String id, String nombre, String precio, String tasa) {
        return new Plato(id, nombre,
                precio == null ? null : new BigDecimal(precio),
                tasa == null ? null : new BigDecimal(tasa));
    }

    @Test
    void registraUnPlatoValido() {
        Plato tacos = plato("P1", "Tacos al pastor", "85.00", "0.16");

        Plato resultado = servicio.registrar(tacos);

        assertEquals(tacos, resultado);
        assertTrue(repositorio.buscarPorId("P1").isPresent());
    }

    @Test
    void aceptaPlatoSinImpuesto() {
        servicio.registrar(plato("P2", "Agua natural", "20", "0"));

        assertTrue(repositorio.buscarPorId("P2").isPresent());
    }

    @Test
    void rechazaPrecioCero() {
        Plato p = plato("P1", "Tacos", "0", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(p));
    }

    @Test
    void rechazaPrecioNegativo() {
        Plato p = plato("P1", "Tacos", "-10", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(p));
    }

    @Test
    void rechazaPrecioConMasDeDosDecimales() {
        Plato p = plato("P1", "Tacos", "85.999", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(p));
    }

    @Test
    void rechazaPrecioNulo() {
        Plato p = plato("P1", "Tacos", null, "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(p));
    }

    @Test
    void rechazaImpuestoNegativo() {
        Plato p = plato("P1", "Tacos", "85", "-0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(p));
    }

    @Test
    void rechazaImpuestoEscritoComoPorcentaje() {
        // 16 en lugar de 0.16
        Plato p = plato("P1", "Tacos", "85", "16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(p));
    }

    @Test
    void rechazaImpuestoNulo() {
        Plato p = plato("P1", "Tacos", "85", null);

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(p));
    }

    @Test
    void rechazaNombreVacio() {
        Plato p = plato("P1", "   ", "85", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(p));
    }

    @Test
    void rechazaIdRepetido() {
        servicio.registrar(plato("P1", "Tacos", "85", "0.16"));
        Plato repetido = plato("P1", "Quesadilla", "60", "0.16");

        assertThrows(ReglaDeNegocioException.class, () -> servicio.registrar(repetido));
        assertEquals("Tacos", repositorio.buscarPorId("P1").get().nombre());
    }

    @Test
    void noGuardaNadaSiLaValidacionFalla() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.registrar(plato("P1", "Tacos", "-1", "0.16")));

        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void listaLosPlatosOrdenadosPorNombre() {
        servicio.registrar(plato("P1", "Tacos", "85", "0.16"));
        servicio.registrar(plato("P2", "Agua", "20", "0"));
        servicio.registrar(plato("P3", "Pozole", "120", "0.16"));

        List<String> nombres = servicio.listar().stream().map(Plato::nombre).toList();

        assertEquals(List.of("Agua", "Pozole", "Tacos"), nombres);
    }
}
