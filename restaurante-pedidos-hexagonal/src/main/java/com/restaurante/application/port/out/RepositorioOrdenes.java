package com.restaurante.application.port.out;

import com.restaurante.domain.Orden;
import java.util.Optional;

public interface RepositorioOrdenes {
    void guardar(Orden orden);

    Optional<Orden> buscarPorId(String id);
}