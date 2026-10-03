package com.restaurante.application.port.in;

import com.restaurante.domain.Orden;
import com.restaurante.domain.TipoOrden;

/** RF2 */
public interface AbrirOrdenUseCase {
    Orden abrir(Comando comando);

    record Comando(TipoOrden tipo, String referencia) { }
}
