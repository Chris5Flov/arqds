package com.restaurante.infrastructure.in.web.http;

/** La petición HTTP está mal formada (JSON inválido, número o enum con formato incorrecto...). Se responde con 400. */
public class SolicitudInvalidaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
