package com.restaurante.application.port.in;

import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Totales;
import java.math.BigDecimal;

/** RF4 y RF5: el cajero consulta los totales y cobra la orden, que se cierra si el pago se aprueba. */
public interface CobrarOrdenUseCase {

    /** RF4 */
    Totales calcularTotales(String ordenId, BigDecimal descuento, BigDecimal propina);

    /** RF5: incluye calcular los totales con el mismo descuento y propina. */
    Pago cobrar(String ordenId, MetodoPago metodo, BigDecimal descuento, BigDecimal propina);
}
