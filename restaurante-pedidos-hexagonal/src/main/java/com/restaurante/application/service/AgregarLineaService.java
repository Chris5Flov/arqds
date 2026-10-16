package com.restaurante.application.service;

import com.restaurante.application.port.in.AgregarLineaUseCase;
import com.restaurante.application.port.out.RepositorioMenu;
import com.restaurante.application.port.out.RepositorioOrdenes;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;

public class AgregarLineaService implements AgregarLineaUseCase {

    private final RepositorioOrdenes ordenes;
    private final RepositorioMenu menu;

    public AgregarLineaService(RepositorioOrdenes ordenes, RepositorioMenu menu) {
        this.ordenes = ordenes;
        this.menu = menu;
    }

    @Override
    public Orden agregar(Comando comando) {
        Orden orden = ordenes.buscarPorId(comando.ordenId())
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "No existe la orden " + comando.ordenId()));

        Plato plato = menu.buscarPorId(comando.platoId())
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "No existe el plato " + comando.platoId()));

        orden.agregarLinea(plato, comando.cantidad(), comando.modificadores());
        ordenes.guardar(orden);
        return orden;
    }
}