package com.restaurante.application.service;

import java.util.ArrayList;
import java.util.List;

import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.domain.EstadoCocina;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.ModificadorAplicado;
import com.restaurante.domain.Orden;
import com.restaurante.domain.ReglaDeNegocioException;

public class GestionarCocinaService implements GestionarCocinaUseCase {

    private final OrdenRepository ordenes;

    public GestionarCocinaService(OrdenRepository ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public List<LineaEnCocina> listarPendientes() {
        List<LineaEnCocina> resultado = new ArrayList<>();
        for (Orden orden : ordenes.listar()) {
            if (orden.getEstado() != EstadoOrden.ABIERTA) {
                continue;
            }
            for (LineaPedido linea : orden.getLineasActivas()) {
                if (linea.getEstadoCocina() != EstadoCocina.LISTO) {
                    resultado.add(new LineaEnCocina(
                            orden.getId(),
                            linea.getId(),
                            orden.getReferencia(),
                            linea.getPlato().nombre(),
                            linea.getCantidad(),
                            linea.getModificadores().stream().map(ModificadorAplicado::nombre).toList(),
                            linea.getEstadoCocina()));
                }
            }
        }
        return resultado;
    }

    @Override
    public void iniciarCoccion(String ordenId, String lineaId) {
        Orden orden = buscarOrden(ordenId);
        orden.iniciarCoccionLinea(lineaId);
        ordenes.guardar(orden);
    }

    @Override
    public void marcarLista(String ordenId, String lineaId) {
        Orden orden = buscarOrden(ordenId);
        orden.marcarLineaLista(lineaId);
        ordenes.guardar(orden);
    }

    private Orden buscarOrden(String ordenId) {
        return ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la orden " + ordenId));
    }
}
