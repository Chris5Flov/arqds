package com.restaurante.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.restaurante.application.port.out.RepositorioOrdenes;
import com.restaurante.domain.Orden;

public class RepositorioOrdenesEnMemoria implements RepositorioOrdenes {
    private final Map<String, Orden> datos = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void guardar(Orden orden) {
        datos.put(orden.getId(), orden);
    }

    @Override
    public Optional<Orden> buscarPorId(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<Orden> listar() {
        synchronized (datos) {
            return new ArrayList<>(datos.values());
        }
    }
}
