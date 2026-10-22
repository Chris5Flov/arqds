package com.restaurante.infrastructure.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.restaurante.infrastructure.config.Aplicacion;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** PUT y DELETE de /api/platos/{platoId} y /api/modificadores/{modificadorId} con el servidor real. */
class MenuEdicionApiTest {

    private ServidorWeb servidor;
    private HttpClient cliente;
    private String base;

    @BeforeEach
    void setUp() throws Exception {
        servidor = new ServidorWeb(Aplicacion.crear(), 0);
        servidor.iniciar();
        cliente = HttpClient.newHttpClient();
        base = "http://localhost:" + servidor.getPuerto();

        assertEquals(201, pedir("POST", "/api/platos",
                "{\"id\":\"P1\",\"nombre\":\"Tacos\",\"precioBase\":\"85\",\"tasaImpuesto\":\"0.16\"}").statusCode());
        assertEquals(201, pedir("POST", "/api/modificadores",
                "{\"id\":\"M1\",\"nombre\":\"Queso\",\"tipo\":\"EXTRA\",\"costoExtra\":\"10\"}").statusCode());
    }

    @AfterEach
    void tearDown() {
        servidor.detener();
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String json) throws IOException, InterruptedException {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(base + ruta));
        if (json == null) {
            peticion.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            peticion.header("Content-Type", "application/json").method(metodo, HttpRequest.BodyPublishers.ofString(json));
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject objeto(HttpResponse<String> respuesta) {
        return JsonParser.parseString(respuesta.body()).getAsJsonObject();
    }

    private JsonArray listar(String ruta) throws Exception {
        return JsonParser.parseString(pedir("GET", ruta, null).body()).getAsJsonArray();
    }

    @Test
    void editaUnPlato() throws Exception {
        HttpResponse<String> respuesta = pedir("PUT", "/api/platos/P1",
                "{\"nombre\":\"Tacos de suadero\",\"precioBase\":\"90.50\",\"tasaImpuesto\":\"0.08\"}");

        assertEquals(200, respuesta.statusCode());
        JsonObject plato = objeto(respuesta);
        assertEquals("P1", plato.get("id").getAsString());
        assertEquals("Tacos de suadero", plato.get("nombre").getAsString());
        assertEquals("90.50", plato.get("precioBase").getAsString());
        assertEquals("Tacos de suadero", listar("/api/platos").get(0).getAsJsonObject().get("nombre").getAsString());
    }

    @Test
    void editarUnPlatoConDatosInvalidosResponde400SinCambiarlo() throws Exception {
        HttpResponse<String> respuesta = pedir("PUT", "/api/platos/P1",
                "{\"nombre\":\"Tacos\",\"precioBase\":\"-5\",\"tasaImpuesto\":\"0.16\"}");

        assertEquals(400, respuesta.statusCode());
        assertEquals("El precio debe ser mayor a cero", objeto(respuesta).get("error").getAsString());
        assertEquals("85.00", listar("/api/platos").get(0).getAsJsonObject().get("precioBase").getAsString());
    }

    @Test
    void editarOEliminarUnPlatoQueNoExisteResponde400() throws Exception {
        HttpResponse<String> editar = pedir("PUT", "/api/platos/P9",
                "{\"nombre\":\"Pozole\",\"precioBase\":\"120\",\"tasaImpuesto\":\"0.16\"}");
        HttpResponse<String> eliminar = pedir("DELETE", "/api/platos/P9", null);

        assertEquals(400, editar.statusCode());
        assertEquals("No existe el plato P9", objeto(editar).get("error").getAsString());
        assertEquals(400, eliminar.statusCode());
    }

    @Test
    void eliminaUnPlatoResponde204() throws Exception {
        HttpResponse<String> respuesta = pedir("DELETE", "/api/platos/P1", null);

        assertEquals(204, respuesta.statusCode());
        assertEquals(0, listar("/api/platos").size());
    }

    @Test
    void unaOrdenConservaSusLineasAunqueSeElimineElPlato() throws Exception {
        String ordenId = objeto(pedir("POST", "/api/ordenes", "{\"tipo\":\"SALA\",\"referencia\":\"Mesa 1\"}"))
                .get("id").getAsString();
        assertEquals(200, pedir("POST", "/api/ordenes/" + ordenId + "/lineas",
                "{\"platoId\":\"P1\",\"cantidad\":1,\"modificadorIds\":[\"M1\"]}").statusCode());

        assertEquals(204, pedir("DELETE", "/api/platos/P1", null).statusCode());
        assertEquals(204, pedir("DELETE", "/api/modificadores/M1", null).statusCode());

        JsonObject orden = objeto(pedir("GET", "/api/ordenes/" + ordenId, null));
        JsonObject linea = orden.getAsJsonArray("lineas").get(0).getAsJsonObject();
        assertEquals("Tacos", linea.get("platoNombre").getAsString());
        assertEquals("110.20", orden.getAsJsonObject("totales").get("total").getAsString());

        HttpResponse<String> nuevaLinea = pedir("POST", "/api/ordenes/" + ordenId + "/lineas",
                "{\"platoId\":\"P1\",\"cantidad\":1,\"modificadorIds\":[]}");
        assertEquals(400, nuevaLinea.statusCode());
        assertEquals("No existe el plato P1", objeto(nuevaLinea).get("error").getAsString());
    }

    @Test
    void editaYEliminaUnModificador() throws Exception {
        HttpResponse<String> editar = pedir("PUT", "/api/modificadores/M1",
                "{\"nombre\":\"Sin queso\",\"tipo\":\"EXCLUSION\",\"costoExtra\":\"0\"}");

        assertEquals(200, editar.statusCode());
        assertEquals("EXCLUSION", objeto(editar).get("tipo").getAsString());

        HttpResponse<String> invalido = pedir("PUT", "/api/modificadores/M1",
                "{\"nombre\":\"Sin queso\",\"tipo\":\"EXCLUSION\",\"costoExtra\":\"5\"}");
        assertEquals(400, invalido.statusCode());
        assertEquals("Una exclusión no puede tener costo extra", objeto(invalido).get("error").getAsString());

        assertEquals(204, pedir("DELETE", "/api/modificadores/M1", null).statusCode());
        assertTrue(listar("/api/modificadores").isEmpty());
    }
}
