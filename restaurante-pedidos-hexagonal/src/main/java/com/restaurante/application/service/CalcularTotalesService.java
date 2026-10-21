package com.restaurante.application.service;

import com.restaurante.application.port.in.CalcularTotalesUseCase;
import com.restaurante.application.port.out.RepositorioOrdenes;
import com.restaurante.domain.Orden;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.Totales;
import java.math.BigDecimal;

/** RF4: consulta el desglose del cobro de una orden. Las reglas de cálculo viven en {@link Orden}. */
public class CalcularTotalesService implements CalcularTotalesUseCase {
    private final RepositorioOrdenes ordenes;

    public CalcularTotalesService(RepositorioOrdenes ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public Totales calcular(String ordenId, BigDecimal descuento, BigDecimal propina) {
        Orden orden = ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> new ReglaDeNegocioException("No existe la orden " + ordenId));

        return orden.calcularTotales(descuento, propina);
    }
}
