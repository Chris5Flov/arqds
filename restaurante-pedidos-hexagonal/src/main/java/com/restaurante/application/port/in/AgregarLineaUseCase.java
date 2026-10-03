package com.restaurante.application.port.in;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import java.util.List;

/** RF3 */
public interface AgregarLineaUseCase {
    Orden agregar(Comando comando);

    record Comando(String ordenId, String platoId, int cantidad, List<Modificador> modificadores) { }
}
