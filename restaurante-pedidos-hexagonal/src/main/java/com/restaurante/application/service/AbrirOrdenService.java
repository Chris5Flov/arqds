package com.restaurante.application.service;

import com.restaurante.application.port.in.AbrirOrdenUseCase;
import com.restaurante.application.port.out.RepositorioOrdenes;
import com.restaurante.domain.Orden;

public class AbrirOrdenService implements AbrirOrdenUseCase {
    private final RepositorioOrdenes ordenes;

    public AbrirOrdenService(RepositorioOrdenes ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public Orden abrir(Comando comando) {
        Orden orden = Orden.abrir(comando.tipo(), comando.referencia());
        ordenes.guardar(orden);
        return orden;
    }
}
