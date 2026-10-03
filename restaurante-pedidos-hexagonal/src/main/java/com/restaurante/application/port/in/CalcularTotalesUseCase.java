package com.restaurante.application.port.in;

import com.restaurante.domain.Totales;
import java.math.BigDecimal;

/** RF4 */
public interface CalcularTotalesUseCase {
    Totales calcular(String ordenId, BigDecimal propina);
}
