package com.restaurante.infrastructure.in.web.http;

import java.util.Map;

/** Respuesta de un controlador: código HTTP y cuerpo a serializar como JSON (null = sin cuerpo). */
public record Respuesta(int estado, Object cuerpo) {

    public static Respuesta ok(Object cuerpo) {
        return new Respuesta(200, cuerpo);
    }

    public static Respuesta creado(Object cuerpo) {
        return new Respuesta(201, cuerpo);
    }

    public static Respuesta sinContenido() {
        return new Respuesta(204, null);
    }

    /** Formato de error del contrato: { "error": "mensaje" }. */
    public static Respuesta error(int estado, String mensaje) {
        return new Respuesta(estado, Map.of("error", mensaje));
    }
}
