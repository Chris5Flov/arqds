package com.restaurante.infrastructure.persistence;

import com.restaurante.application.port.out.ModificadorRepository;
import com.restaurante.domain.Modificador;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ModificadorRepositoryMemoria implements ModificadorRepository {
    private final Map<String, Modificador> datos = new ConcurrentHashMap<>();

    @Override
    public void guardar(Modificador modificador) {
        datos.put(modificador.id(), modificador);
    }

    @Override
    public Optional<Modificador> buscarPorId(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<Modificador> listar() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public void eliminar(String id) {
        datos.remove(id);
    }
}
