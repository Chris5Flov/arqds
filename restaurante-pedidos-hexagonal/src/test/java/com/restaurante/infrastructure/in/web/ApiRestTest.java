package com.restaurante.infrastructure.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

/** Recorre la API REST real (servidor HTTP + controladores + casos de uso) según el contrato. */
class ApiRestTest {

    private record Resultado(int estado, String cuerpo) {
        JsonObject objeto() {
            return JsonParser.parseString(cuerpo).getAsJsonObject();
        }

        JsonArray arreglo() {
            return JsonParser.parseString(cuerpo).getAsJsonArray();
        }

        String error() {
            return objeto().get("error").getAsString();
        }
    }

    private ServidorWeb servidor;
    private HttpClient cliente;
    private String base;

    @BeforeEach
    void setUp() throws IOException {
        servidor = new ServidorWeb(Aplicacion.crear(), 0);
        servidor.iniciar();
        cliente = HttpClient.newHttpClient();
        base = "http://localhost:" + servidor.getPuerto();
    }

    @AfterEach
    void tearDown() {
        servidor.detener();
    }

    // ---------------------------------------------------------------- utilidades

    private Resultado pedir(String metodo, String ruta, String json) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(base + ruta));
        if (json == null) {
            peticion.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            peticion.header("Content-Type", "application/json")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(json));
        }
        HttpResponse<String> respuesta = cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
        return new Resultado(respuesta.statusCode(), respuesta.body());
    }

    private Resultado get(String ruta) throws Exception {
        return pedir("GET", ruta, null);
    }

    private Resultado post(String ruta, String json) throws Exception {
        return pedir("POST", ruta, json);
    }

    private static String texto(JsonElement e, String campo) {
        return e.getAsJsonObject().get(campo).getAsString();
    }

    private void registrarMenu() throws Exception {
        assertEquals(201, post("/api/platos",
                "{\"id\":\"P1\",\"nombre\":\"Tacos\",\"precioBase\":\"85\",\"tasaImpuesto\":\"0.16\"}").estado());
        assertEquals(201, post("/api/platos",
                "{\"id\":\"P2\",\"nombre\":\"Agua\",\"precioBase\":\"20\",\"tasaImpuesto\":\"0\"}").estado());
        assertEquals(201, post("/api/modificadores",
                "{\"id\":\"M1\",\"nombre\":\"Queso\",\"tipo\":\"EXTRA\",\"costoExtra\":\"10\"}").estado());
    }

    private String abrirOrden(String referencia) throws Exception {
        Resultado r = post("/api/ordenes", "{\"tipo\":\"SALA\",\"referencia\":\"" + referencia + "\"}");
        assertEquals(201, r.estado());
        return r.objeto().get("id").getAsString();
    }

    // ---------------------------------------------------------------- menú

    @Test
    void registraYListaPlatosYModificadores() throws Exception {
        registrarMenu();

        JsonArray platos = get("/api/platos").arreglo();
        assertEquals(2, platos.size());
        assertEquals("Agua", texto(platos.get(0), "nombre"));
        assertEquals("85.00", texto(platos.get(1), "precioBase"));
        assertEquals("0.16", texto(platos.get(1), "tasaImpuesto"));

        JsonArray modificadores = get("/api/modificadores").arreglo();
        assertEquals("EXTRA", texto(modificadores.get(0), "tipo"));
        assertEquals("10.00", texto(modificadores.get(0), "costoExtra"));
    }

    @Test
    void lasReglasDeNegocioYLosDatosInvalidosResponden400ConMensaje() throws Exception {
        Resultado negativo = post("/api/platos",
                "{\"id\":\"P1\",\"nombre\":\"Tacos\",\"precioBase\":\"-1\",\"tasaImpuesto\":\"0.16\"}");
        assertEquals(400, negativo.estado());
        assertEquals("El precio debe ser mayor a cero", negativo.error());

        Resultado noNumero = post("/api/platos",
                "{\"id\":\"P1\",\"nombre\":\"Tacos\",\"precioBase\":\"abc\",\"tasaImpuesto\":\"0.16\"}");
        assertEquals(400, noNumero.estado());
        assertTrue(noNumero.error().contains("precioBase"));

        Resultado tipoMalo = post("/api/modificadores",
                "{\"id\":\"M1\",\"nombre\":\"X\",\"tipo\":\"OTRO\",\"costoExtra\":\"0\"}");
        assertEquals(400, tipoMalo.estado());
        assertTrue(tipoMalo.error().contains("EXTRA, EXCLUSION"));

        assertEquals(400, post("/api/platos", "{no es json").estado());
        assertEquals(400, post("/api/platos", null).estado());
    }

    @Test
    void aceptaMontosEnviadosComoNumero() throws Exception {
        Resultado r = post("/api/platos",
                "{\"id\":\"P1\",\"nombre\":\"Tacos\",\"precioBase\":85.5,\"tasaImpuesto\":0.16}");

        assertEquals(201, r.estado());
        assertEquals("85.50", r.objeto().get("precioBase").getAsString());
    }

    // ---------------------------------------------------------------- órdenes

    @Test
    void abreUnaOrdenYGestionaSusLineas() throws Exception {
        registrarMenu();
        String ordenId = abrirOrden("Mesa 5");

        JsonObject orden = post("/api/ordenes/" + ordenId + "/lineas",
                "{\"platoId\":\"P1\",\"cantidad\":2,\"modificadorIds\":[\"M1\"]}").objeto();
        JsonObject linea = orden.getAsJsonArray("lineas").get(0).getAsJsonObject();
        String lineaId = linea.get("id").getAsString();
        assertEquals("Tacos", linea.get("platoNombre").getAsString());
        assertEquals("10.00", texto(linea.getAsJsonArray("modificadores").get(0), "costoExtra"));
        assertTrue(linea.get("editable").getAsBoolean());
        // (85 + 10) x 2 = 190 al 16 % = 220.40
        assertEquals("220.40", orden.getAsJsonObject("totales").get("total").getAsString());

        Resultado modificada = pedir("PUT", "/api/ordenes/" + ordenId + "/lineas/" + lineaId,
                "{\"cantidad\":3,\"modificadorIds\":[]}");
        assertEquals(200, modificada.estado());
        assertEquals(3, modificada.objeto().getAsJsonArray("lineas").get(0).getAsJsonObject()
                .get("cantidad").getAsInt());

        Resultado cancelada = pedir("DELETE", "/api/ordenes/" + ordenId + "/lineas/" + lineaId, null);
        assertEquals(200, cancelada.estado());
        assertEquals("CANCELADA", texto(cancelada.objeto().getAsJsonArray("lineas").get(0), "estadoCocina"));
        assertEquals("0.00", cancelada.objeto().getAsJsonObject("totales").get("total").getAsString());

        JsonObject consultada = get("/api/ordenes/" + ordenId).objeto();
        assertEquals("Mesa 5", consultada.get("referencia").getAsString());
        assertEquals("ABIERTA", consultada.get("estado").getAsString());
        assertTrue(consultada.get("fechaCreacion").getAsString().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void listaOrdenesFiltrandoPorEstado() throws Exception {
        abrirOrden("Mesa 1");
        String cancelada = abrirOrden("Mesa 2");
        assertEquals(200, post("/api/ordenes/" + cancelada + "/cancelar", null).estado());

        assertEquals(2, get("/api/ordenes").arreglo().size());
        JsonArray abiertas = get("/api/ordenes?estado=ABIERTA").arreglo();
        assertEquals(1, abiertas.size());
        assertEquals("Mesa 1", texto(abiertas.get(0), "referencia"));
        assertEquals(1, get("/api/ordenes?estado=CANCELADA").arreglo().size());
        assertEquals(400, get("/api/ordenes?estado=RARO").estado());
    }

    @Test
    void unaOrdenQueNoExisteResponde400() throws Exception {
        Resultado r = get("/api/ordenes/no-existe");

        assertEquals(400, r.estado());
        assertEquals("No existe la orden no-existe", r.error());
    }

    // ---------------------------------------------------------------- cocina

    @Test
    void laCocinaAvanzaLasLineasYYaNoSePuedenEditar() throws Exception {
        registrarMenu();
        String ordenId = abrirOrden("Mesa 5");
        post("/api/ordenes/" + ordenId + "/lineas", "{\"platoId\":\"P1\",\"cantidad\":1,\"modificadorIds\":[\"M1\"]}");

        JsonArray pendientes = get("/api/cocina/pendientes").arreglo();
        assertEquals(1, pendientes.size());
        JsonObject pendiente = pendientes.get(0).getAsJsonObject();
        assertEquals("Mesa 5", pendiente.get("referenciaOrden").getAsString());
        assertEquals("Queso", pendiente.getAsJsonArray("modificadores").get(0).getAsString());
        String lineaId = pendiente.get("lineaId").getAsString();

        Resultado iniciar = post("/api/cocina/ordenes/" + ordenId + "/lineas/" + lineaId + "/iniciar", null);
        assertEquals(204, iniciar.estado());
        assertTrue(iniciar.cuerpo().isEmpty());
        assertEquals("EN_COCCION", texto(get("/api/cocina/pendientes").arreglo().get(0), "estado"));

        Resultado editar = pedir("PUT", "/api/ordenes/" + ordenId + "/lineas/" + lineaId,
                "{\"cantidad\":2,\"modificadorIds\":[]}");
        assertEquals(400, editar.estado());

        assertEquals(204, post("/api/cocina/ordenes/" + ordenId + "/lineas/" + lineaId + "/lista", null).estado());
        assertEquals(0, get("/api/cocina/pendientes").arreglo().size());
    }

    // ---------------------------------------------------------------- caja

    @Test
    void calculaTotalesYCobraElMismoMonto() throws Exception {
        registrarMenu();
        String ordenId = abrirOrden("Mesa 5");
        post("/api/ordenes/" + ordenId + "/lineas", "{\"platoId\":\"P1\",\"cantidad\":2,\"modificadorIds\":[]}");
        post("/api/ordenes/" + ordenId + "/lineas", "{\"platoId\":\"P2\",\"cantidad\":1,\"modificadorIds\":[]}");

        // 190 - 19 = 171; impuestos 170 x 0.16 x 171 / 190 = 24.48; + 10 de propina = 205.48
        JsonObject totales = get("/api/caja/ordenes/" + ordenId + "/totales?descuento=19&propina=10").objeto();
        assertEquals("190.00", totales.get("subtotal").getAsString());
        assertEquals("19.00", totales.get("descuento").getAsString());
        assertEquals("24.48", totales.get("impuestos").getAsString());
        assertEquals("205.48", totales.get("total").getAsString());
        assertEquals("217.20", get("/api/caja/ordenes/" + ordenId + "/totales").objeto().get("total").getAsString());

        Resultado cobro = post("/api/caja/ordenes/" + ordenId + "/cobrar",
                "{\"metodo\":\"EFECTIVO\",\"descuento\":\"19\",\"propina\":\"10\"}");
        assertEquals(200, cobro.estado());
        JsonObject pago = cobro.objeto();
        assertEquals("APROBADO", pago.get("estado").getAsString());
        assertEquals("EFECTIVO", pago.get("metodo").getAsString());
        assertEquals("205.48", pago.get("monto").getAsString());
        assertEquals(ordenId, pago.get("ordenId").getAsString());

        JsonObject cerrada = get("/api/ordenes/" + ordenId).objeto();
        assertEquals("CERRADA", cerrada.get("estado").getAsString());
        assertEquals("205.48", cerrada.getAsJsonObject("totales").get("total").getAsString());
    }

    @Test
    void unPagoRechazadoResponde200YLaOrdenSigueAbierta() throws Exception {
        assertEquals(201, post("/api/platos",
                "{\"id\":\"P9\",\"nombre\":\"Banquete\",\"precioBase\":\"25000\",\"tasaImpuesto\":\"0\"}").estado());
        String ordenId = abrirOrden("Evento");
        post("/api/ordenes/" + ordenId + "/lineas", "{\"platoId\":\"P9\",\"cantidad\":1,\"modificadorIds\":[]}");

        Resultado rechazado = post("/api/caja/ordenes/" + ordenId + "/cobrar",
                "{\"metodo\":\"TARJETA\",\"descuento\":\"\",\"propina\":\"\"}");
        assertEquals(200, rechazado.estado());
        assertEquals("RECHAZADO", rechazado.objeto().get("estado").getAsString());

        JsonObject orden = get("/api/ordenes/" + ordenId).objeto();
        assertEquals("ABIERTA", orden.get("estado").getAsString());
        assertEquals(1, orden.getAsJsonArray("pagos").size());

        Resultado efectivo = post("/api/caja/ordenes/" + ordenId + "/cobrar", "{\"metodo\":\"EFECTIVO\"}");
        assertEquals("APROBADO", efectivo.objeto().get("estado").getAsString());
    }

    @Test
    void cobrarConErroresResponde400SinCobrar() throws Exception {
        String ordenId = abrirOrden("Mesa vacía");

        assertEquals("No se puede cobrar una orden sin líneas",
                post("/api/caja/ordenes/" + ordenId + "/cobrar", "{\"metodo\":\"EFECTIVO\"}").error());
        assertEquals(400, post("/api/caja/ordenes/" + ordenId + "/cobrar", "{\"metodo\":\"CHEQUE\"}").estado());
        assertEquals(400, get("/api/caja/ordenes/" + ordenId + "/totales?descuento=x").estado());
        assertTrue(get("/api/ordenes/" + ordenId).objeto().getAsJsonArray("pagos").isEmpty());
    }

    // ---------------------------------------------------------------- rutas y archivos

    @Test
    void unaRutaInexistenteResponde404EnFormatoJson() throws Exception {
        Resultado r = get("/api/no-existe");

        assertEquals(404, r.estado());
        assertFalse(r.error().isBlank());
        assertEquals(404, pedir("PATCH", "/api/platos", "{}").estado());
    }

    @Test
    void laRaizEntregaUnaPaginaHtml() throws Exception {
        HttpResponse<String> r = cliente.send(HttpRequest.newBuilder(URI.create(base + "/")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
        assertEquals(404, get("/no-existe.js").estado());
        assertEquals(404, get("/../pom.xml").estado());
    }
}
