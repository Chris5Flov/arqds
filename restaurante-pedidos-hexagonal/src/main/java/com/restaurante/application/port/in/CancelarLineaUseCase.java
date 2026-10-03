package com.restaurante.application.port.in;

import com.restaurante.domain.Orden;

/** RF3 */
public interface CancelarLineaUseCase {
    Orden cancelar(String ordenId, String lineaId);
}
