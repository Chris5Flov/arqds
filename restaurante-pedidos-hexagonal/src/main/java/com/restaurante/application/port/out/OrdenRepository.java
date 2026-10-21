package com.restaurante.application.port.out;

import com.restaurante.domain.Orden;
import java.util.List;
import java.util.Optional;

public interface OrdenRepository {
    void guardar(Orden orden);

    Optional<Orden> buscarPorId(String id);

    List<Orden> listar();
}
