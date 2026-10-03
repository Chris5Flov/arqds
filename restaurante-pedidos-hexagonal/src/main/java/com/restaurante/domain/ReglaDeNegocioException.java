package com.restaurante.domain;

/** Se lanza cuando se viola una regla del negocio. */
public class ReglaDeNegocioException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
