package com.restaurante.infrastructure.persistence;

import com.restaurante.application.port.out.RepositorioOrdenes;
import com.restaurante.domain.Orden;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RepositorioOrdenesEnMemoria implements RepositorioOrdenes {
    private final Map<String, Orden> datos = new ConcurrentHashMap<>();

    @Override
    public void guardar(Orden orden) {
        datos.put(orden.getId(), orden);
    }

    @Override
    public Optional<Orden> buscarPorId(String id) {
        return Optional.ofNullable(datos.get(id));
    }
}
