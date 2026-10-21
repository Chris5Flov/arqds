package com.restaurante.application.service;

import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.Totales;
import java.math.BigDecimal;
import java.util.List;

/**
 * RF4 y RF5: calcula los totales y cobra la orden con el procesador que soporte el método elegido.
 * Si el pago se aprueba la orden se cierra; si se rechaza queda registrado y la orden sigue abierta.
 */
public class CobrarOrdenService implements CobrarOrdenUseCase {
    private final OrdenRepository ordenes;
    private final List<ProcesadorPago> procesadores;

    public CobrarOrdenService(OrdenRepository ordenes, List<ProcesadorPago> procesadores) {
        this.ordenes = ordenes;
        this.procesadores = List.copyOf(procesadores);
    }

    @Override
    public Totales calcularTotales(String ordenId, BigDecimal descuento, BigDecimal propina) {
        return buscarOrden(ordenId).calcularTotales(descuento, propina);
    }

    @Override
    public Pago cobrar(String ordenId, MetodoPago metodo, BigDecimal descuento, BigDecimal propina) {
        if (metodo == null) {
            throw new ReglaDeNegocioException("El método de pago es obligatorio");
        }
        Orden orden = buscarOrden(ordenId);

        // Se valida todo antes de cobrar: si algo falla, no se hace ningún cargo.
        Totales totales = orden.totalesParaCobrar(descuento, propina);
        ProcesadorPago procesador = elegirProcesador(metodo);

        Pago pago = procesador.cobrar(orden.getId(), totales.total());
        orden.registrarPago(pago);
        if (pago.esAprobado()) {
            orden.cerrar(descuento, propina);
        }
        ordenes.guardar(orden);
        return pago;
    }

    private Orden buscarOrden(String ordenId) {
        return ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la orden " + ordenId));
    }

    private ProcesadorPago elegirProcesador(MetodoPago metodo) {
        return procesadores.stream()
                .filter(p -> p.soporta(metodo))
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "No hay un procesador de pago configurado para el método " + metodo));
    }
}
