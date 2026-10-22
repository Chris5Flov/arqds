package com.restaurante.application.port.out;

import com.restaurante.domain.Plato;
import java.util.List;
import java.util.Optional;

public interface PlatoRepository {
    void guardar(Plato plato);

    Optional<Plato> buscarPorId(String id);

    List<Plato> listar();

    void eliminar(String id);
}
