package com.restaurante.application.service;

import com.restaurante.application.port.in.CerrarOrdenUseCase;
import com.restaurante.application.port.out.PuertoCobro;
import com.restaurante.application.port.out.RepositorioOrdenes;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.Totales;
import java.math.BigDecimal;
import java.util.List;

/** RF5: cobra la orden con el puerto que soporte el método elegido y la cierra. */
public class CerrarOrdenService implements CerrarOrdenUseCase {
    private final RepositorioOrdenes ordenes;
    private final List<PuertoCobro> puertosCobro;

    public CerrarOrdenService(RepositorioOrdenes ordenes, List<PuertoCobro> puertosCobro) {
        this.ordenes = ordenes;
        this.puertosCobro = List.copyOf(puertosCobro);
    }

    @Override
    public Pago cerrar(String ordenId, MetodoPago metodo, BigDecimal propina) {
        if (metodo == null) {
            throw new ReglaDeNegocioException("El método de pago es obligatorio");
        }
        BigDecimal propinaFinal = propina == null ? BigDecimal.ZERO : propina;
        if (propinaFinal.signum() < 0) {
            throw new ReglaDeNegocioException("La propina no puede ser negativa");
        }

        Orden orden = ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la orden " + ordenId));
        if (orden.getEstado() != EstadoOrden.ABIERTA) {
            throw new ReglaDeNegocioException("La orden no está abierta");
        }

        // Se valida todo antes de cobrar: si algo falla, no se hace ningún cargo.
        PuertoCobro puerto = elegirPuerto(metodo);
        Totales totales = orden.calcularTotales();
        BigDecimal monto = totales.total().add(propinaFinal);

        Pago pago = puerto.cobrar(orden.getId(), monto);
        orden.cerrar(pago);
        ordenes.guardar(orden);
        return pago;
    }

    private PuertoCobro elegirPuerto(MetodoPago metodo) {
        return puertosCobro.stream()
                .filter(p -> p.soporta(metodo))
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "No hay un cobro configurado para el método " + metodo));
    }
}
