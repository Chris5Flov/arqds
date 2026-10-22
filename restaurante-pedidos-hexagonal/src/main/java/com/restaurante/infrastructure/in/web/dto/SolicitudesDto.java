package com.restaurante.infrastructure.in.web.dto;

import java.util.List;

/**
 * Cuerpos JSON que recibe la API. Los montos llegan como String ("85.50") y los enums por su nombre;
 * los controladores los convierten con {@code Conversion}.
 */
public final class SolicitudesDto {

    private SolicitudesDto() {
    }

    public record RegistrarPlatoRequest(String id, String nombre, String precioBase, String tasaImpuesto) { }

    public record ActualizarPlatoRequest(String nombre, String precioBase, String tasaImpuesto) { }

    public record RegistrarModificadorRequest(String id, String nombre, String tipo, String costoExtra) { }

    public record ActualizarModificadorRequest(String nombre, String tipo, String costoExtra) { }

    public record AbrirOrdenRequest(String tipo, String referencia) { }

    public record AgregarLineaRequest(String platoId, Integer cantidad, List<String> modificadorIds) { }

    public record ModificarLineaRequest(Integer cantidad, List<String> modificadorIds) { }

    public record CobrarRequest(String metodo, String descuento, String propina) { }
}
