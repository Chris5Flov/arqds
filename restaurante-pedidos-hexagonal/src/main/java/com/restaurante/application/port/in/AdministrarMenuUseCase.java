package com.restaurante.application.port.in;

import com.restaurante.domain.Plato;
import java.util.List;

/** RF1 */
public interface AdministrarMenuUseCase {
    Plato registrar(Plato plato);

    List<Plato> listar();
}
