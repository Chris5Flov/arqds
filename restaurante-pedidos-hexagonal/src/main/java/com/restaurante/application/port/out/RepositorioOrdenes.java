package com.restaurante.application.port.out;

import java.util.List;
import java.util.Optional;

import com.restaurante.domain.Orden;
public interface RepositorioOrdenes {
    void guardar(Orden orden);

    Optional<Orden> buscarPorId(String id);

    List<Orden> listar();
}
