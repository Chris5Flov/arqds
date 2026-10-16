package com.restaurante.application.service;

import com.restaurante.application.port.in.CancelarLineaUseCase;
import com.restaurante.application.port.out.RepositorioOrdenes;
import com.restaurante.domain.Orden;
import com.restaurante.domain.ReglaDeNegocioException;

public class CancelarLineaService implements CancelarLineaUseCase {

    private final RepositorioOrdenes ordenes;

    public CancelarLineaService(RepositorioOrdenes ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public Orden cancelar(String ordenId, String lineaId) {
        Orden orden = ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "No existe la orden " + ordenId));

        orden.cancelarLinea(lineaId);
        ordenes.guardar(orden);
        return orden;
    }
}
