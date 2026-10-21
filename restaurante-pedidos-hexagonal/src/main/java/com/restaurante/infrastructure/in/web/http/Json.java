package com.restaurante.infrastructure.in.web.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/** Serialización JSON del adaptador web. Los DTO son records con tipos simples (String, int, List...). */
public final class Json {
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private Json() {
    }

    public static String escribir(Object valor) {
        return GSON.toJson(valor);
    }

    /** @throws SolicitudInvalidaException si el cuerpo está vacío o no es JSON válido para el tipo pedido. */
    public static <T> T leer(String json, Class<T> tipo) {
        if (json == null || json.isBlank()) {
            throw new SolicitudInvalidaException("La petición necesita un cuerpo JSON");
        }
        try {
            T valor = GSON.fromJson(json, tipo);
            if (valor == null) {
                throw new SolicitudInvalidaException("La petición necesita un cuerpo JSON");
            }
            return valor;
        } catch (JsonParseException | IllegalArgumentException e) {
            throw new SolicitudInvalidaException("El cuerpo de la petición no es un JSON válido");
        }
    }
}
