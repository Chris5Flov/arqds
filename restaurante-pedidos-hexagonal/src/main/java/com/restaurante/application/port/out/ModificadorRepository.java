package com.restaurante.application.port.out;

import com.restaurante.domain.Modificador;
import java.util.List;
import java.util.Optional;

public interface ModificadorRepository {
    void guardar(Modificador modificador);

    Optional<Modificador> buscarPorId(String id);

    List<Modificador> listar();

    void eliminar(String id);
}
