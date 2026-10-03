package com.restaurante.application.port.in;

import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;

/** RF5 */
public interface CerrarOrdenUseCase {
    Pago cerrar(String ordenId, MetodoPago metodo, java.math.BigDecimal propina);
}
