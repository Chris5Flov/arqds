package com.restaurante.infrastructure.in.web.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrutadorTest {

    private Enrutador enrutador;

    @BeforeEach
    void setUp() {
        enrutador = new Enrutador()
                .get("/api/ordenes", p -> Respuesta.ok("lista"))
                .get("/api/ordenes/{ordenId}", p -> Respuesta.ok("una"))
                .put("/api/ordenes/{ordenId}/lineas/{lineaId}", p -> Respuesta.ok("modificar"));
    }

    private Object ejecutar(String metodo, String ruta) {
        Enrutador.Coincidencia c = enrutador.resolver(metodo, ruta).orElseThrow();
        return c.accion().ejecutar(null).cuerpo();
    }

    @Test
    void distingueRutasFijasDeRutasConParametros() {
        assertEquals("lista", ejecutar("GET", "/api/ordenes"));
        assertEquals("una", ejecutar("GET", "/api/ordenes/abc"));
    }

    @Test
    void extraeLosParametrosDeLaRuta() {
        Enrutador.Coincidencia c = enrutador.resolver("PUT", "/api/ordenes/O1/lineas/L9").orElseThrow();

        assertEquals(Map.of("ordenId", "O1", "lineaId", "L9"), c.parametros());
    }

    @Test
    void ignoraLaBarraFinal() {
        assertEquals("lista", ejecutar("GET", "/api/ordenes/"));
    }

    @Test
    void noCoincideConOtroMetodoNiConOtraLongitud() {
        assertTrue(enrutador.resolver("POST", "/api/ordenes").isEmpty());
        assertTrue(enrutador.resolver("GET", "/api/ordenes/O1/lineas").isEmpty());
        assertTrue(enrutador.resolver("GET", "/api/platos").isEmpty());
    }
}
