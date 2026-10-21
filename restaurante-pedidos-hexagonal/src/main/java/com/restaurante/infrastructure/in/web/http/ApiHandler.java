package com.restaurante.infrastructure.in.web.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.restaurante.domain.ReglaDeNegocioException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Atiende todo lo que llega a /api: resuelve la ruta, ejecuta el controlador y traduce los errores
 * al formato del contrato: 400 (regla de negocio o petición inválida), 404 (ruta inexistente) y 500.
 */
public class ApiHandler implements HttpHandler {

    private final Enrutador enrutador;
    // El dominio guarda las órdenes en memoria y no es seguro para hilos: se atiende una petición a la vez.
    private final Object bloqueo = new Object();

    public ApiHandler(Enrutador enrutador) {
        this.enrutador = enrutador;
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            Respuesta respuesta;
            try {
                respuesta = atender(intercambio);
            } catch (ReglaDeNegocioException | SolicitudInvalidaException e) {
                respuesta = Respuesta.error(400, e.getMessage());
            } catch (RuntimeException e) {
                System.err.println("Error inesperado en " + intercambio.getRequestMethod() + " "
                        + intercambio.getRequestURI());
                e.printStackTrace();
                respuesta = Respuesta.error(500, "Error interno del servidor");
            }
            enviar(intercambio, respuesta);
        } finally {
            intercambio.close();
        }
    }

    private Respuesta atender(HttpExchange intercambio) throws IOException {
        String metodo = intercambio.getRequestMethod();
        String ruta = intercambio.getRequestURI().getPath();

        Optional<Enrutador.Coincidencia> coincidencia = enrutador.resolver(metodo, ruta);
        if (coincidencia.isEmpty()) {
            return Respuesta.error(404, "Ruta no encontrada: " + metodo + " " + ruta);
        }

        Peticion peticion = new Peticion(metodo, ruta,
                coincidencia.get().parametros(),
                leerConsulta(intercambio.getRequestURI().getRawQuery()),
                leerCuerpo(intercambio.getRequestBody()));

        synchronized (bloqueo) {
            return coincidencia.get().accion().ejecutar(peticion);
        }
    }

    private static void enviar(HttpExchange intercambio, Respuesta respuesta) throws IOException {
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        if (respuesta.cuerpo() == null) {
            intercambio.sendResponseHeaders(respuesta.estado(), -1);
            return;
        }
        byte[] bytes = Json.escribir(respuesta.cuerpo()).getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(respuesta.estado(), bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private static String leerCuerpo(InputStream entrada) throws IOException {
        return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> leerConsulta(String consultaCruda) {
        Map<String, String> parametros = new HashMap<>();
        if (consultaCruda == null || consultaCruda.isEmpty()) {
            return parametros;
        }
        for (String par : consultaCruda.split("&")) {
            if (par.isEmpty()) {
                continue;
            }
            int igual = par.indexOf('=');
            String clave = igual < 0 ? par : par.substring(0, igual);
            String valor = igual < 0 ? "" : par.substring(igual + 1);
            parametros.put(URLDecoder.decode(clave, StandardCharsets.UTF_8),
                    URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
        return parametros;
    }
}
