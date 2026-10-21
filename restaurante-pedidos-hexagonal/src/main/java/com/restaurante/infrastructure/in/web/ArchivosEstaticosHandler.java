package com.restaurante.infrastructure.in.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Sirve la interfaz web (HTML, CSS, JS) desde el classpath: src/main/resources/web/.
 * "/" entrega index.html. Si la interfaz aún no existe, muestra una página que indica que la API está activa.
 */
public class ArchivosEstaticosHandler implements HttpHandler {

    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "ico", "image/x-icon");

    private static final String PAGINA_SIN_INTERFAZ = """
            <!doctype html><html lang="es"><head><meta charset="utf-8"><title>Sistema de Pedidos</title></head>
            <body style="font-family:system-ui,sans-serif;margin:40px">
            <h1>Sistema de Pedidos de Restaurante</h1>
            <p>La API REST está activa en <code>/api</code> (por ejemplo <a href="/api/platos">/api/platos</a>).</p>
            <p>La interfaz web todavía no está instalada en <code>src/main/resources/web/</code>.</p>
            </body></html>
            """;

    private final String carpeta;

    public ArchivosEstaticosHandler(String carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            String metodo = intercambio.getRequestMethod();
            if (!metodo.equals("GET") && !metodo.equals("HEAD")) {
                enviar(intercambio, 405, "text/plain; charset=utf-8", bytes("Método no permitido"));
                return;
            }

            String ruta = intercambio.getRequestURI().getPath();
            if (ruta.equals("/") || ruta.isEmpty()) {
                ruta = "/index.html";
            }
            if (ruta.contains("..") || ruta.contains("\\")) {
                enviar(intercambio, 404, "text/plain; charset=utf-8", bytes("No encontrado"));
                return;
            }

            byte[] contenido = leerRecurso(carpeta + ruta);
            if (contenido == null) {
                if (ruta.equals("/index.html")) {
                    enviar(intercambio, 200, TIPOS.get("html"), bytes(PAGINA_SIN_INTERFAZ));
                } else {
                    enviar(intercambio, 404, "text/plain; charset=utf-8", bytes("No encontrado: " + ruta));
                }
                return;
            }
            enviar(intercambio, 200, tipoDe(ruta), contenido);
        } finally {
            intercambio.close();
        }
    }

    private static byte[] leerRecurso(String nombre) throws IOException {
        try (InputStream entrada = ArchivosEstaticosHandler.class.getClassLoader().getResourceAsStream(nombre)) {
            return entrada == null ? null : entrada.readAllBytes();
        }
    }

    private static String tipoDe(String ruta) {
        int punto = ruta.lastIndexOf('.');
        String extension = punto < 0 ? "" : ruta.substring(punto + 1).toLowerCase();
        return TIPOS.getOrDefault(extension, "application/octet-stream");
    }

    private static void enviar(HttpExchange intercambio, int estado, String tipo, byte[] cuerpo) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        // Durante el desarrollo siempre se pide la versión más reciente de los archivos.
        intercambio.getResponseHeaders().set("Cache-Control", "no-cache");
        boolean esHead = intercambio.getRequestMethod().equals("HEAD");
        intercambio.sendResponseHeaders(estado, esHead ? -1 : cuerpo.length);
        if (!esHead) {
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        }
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }
}
