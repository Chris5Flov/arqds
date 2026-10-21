package com.restaurante.application.service;

import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.application.port.out.ModificadorRepository;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.application.port.out.PlatoRepository;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import java.util.Comparator;
import java.util.List;

/** RF2 y RF3. Las reglas (orden abierta, línea PENDIENTE, cantidad...) viven en {@link Orden}. */
public class GestionarOrdenService implements GestionarOrdenUseCase {
    private final OrdenRepository ordenes;
    private final PlatoRepository platos;
    private final ModificadorRepository modificadores;

    public GestionarOrdenService(OrdenRepository ordenes, PlatoRepository platos,
                                 ModificadorRepository modificadores) {
        this.ordenes = ordenes;
        this.platos = platos;
        this.modificadores = modificadores;
    }

    @Override
    public Orden abrir(TipoOrden tipo, String referencia) {
        Orden orden = Orden.abrir(tipo, referencia);
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden agregarLinea(String ordenId, String platoId, int cantidad, List<String> modificadorIds) {
        Orden orden = buscarOrden(ordenId);
        Plato plato = platos.buscarPorId(platoId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe el plato " + platoId));

        orden.agregarLinea(plato, cantidad, buscarModificadores(modificadorIds));
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden modificarLinea(String ordenId, String lineaId, int cantidad, List<String> modificadorIds) {
        Orden orden = buscarOrden(ordenId);
        orden.modificarLinea(lineaId, cantidad, buscarModificadores(modificadorIds));
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden cancelarLinea(String ordenId, String lineaId) {
        Orden orden = buscarOrden(ordenId);
        orden.cancelarLinea(lineaId);
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden cancelarOrden(String ordenId) {
        Orden orden = buscarOrden(ordenId);
        orden.cancelar();
        ordenes.guardar(orden);
        return orden;
    }

    @Override
    public Orden consultar(String ordenId) {
        return buscarOrden(ordenId);
    }

    @Override
    public List<Orden> listar() {
        return ordenes.listar().stream()
                .sorted(Comparator.comparing(Orden::getFechaCreacion))
                .toList();
    }

    @Override
    public List<Orden> listarAbiertas() {
        return listar().stream()
                .filter(o -> o.getEstado() == EstadoOrden.ABIERTA)
                .toList();
    }

    private Orden buscarOrden(String ordenId) {
        return ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la orden " + ordenId));
    }

    private List<Modificador> buscarModificadores(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .map(id -> modificadores.buscarPorId(id)
                        .orElseThrow(() -> new ReglaDeNegocioException("No existe el modificador " + id)))
                .toList();
    }
}
