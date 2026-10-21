package com.restaurante.infrastructure.in.web;

import com.restaurante.infrastructure.in.web.http.Enrutador;

/** Un controlador REST registra sus endpoints en el enrutador; solo conoce puertos de entrada y el dominio. */
public interface RestController {
    void registrarRutas(Enrutador enrutador);
}
