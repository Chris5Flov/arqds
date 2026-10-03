package com.restaurante.infrastructure.persistence;

import com.restaurante.application.port.out.RepositorioMenu;
import com.restaurante.domain.Plato;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RepositorioMenuEnMemoria implements RepositorioMenu {
    private final Map<String, Plato> datos = new ConcurrentHashMap<>();

    @Override
    public void guardar(Plato plato) {
        datos.put(plato.id(), plato);
    }

    @Override
    public Optional<Plato> buscarPorId(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<Plato> listar() {
        return new ArrayList<>(datos.values());
    }
}
