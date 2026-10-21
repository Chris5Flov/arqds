package com.restaurante.infrastructure.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.domain.EstadoPago;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProcesadoresPagoTest {

    @Test
    void cadaProcesadorSoportaSoloSuMetodo() {
        assertTrue(new PagoEfectivoAdapter().soporta(MetodoPago.EFECTIVO));
        assertFalse(new PagoEfectivoAdapter().soporta(MetodoPago.TARJETA));
        assertTrue(new PagoTarjetaSimuladoAdapter().soporta(MetodoPago.TARJETA));
        assertFalse(new PagoTarjetaSimuladoAdapter().soporta(MetodoPago.PASARELA_DIGITAL));
        assertTrue(new PagoPasarelaSimuladoAdapter().soporta(MetodoPago.PASARELA_DIGITAL));
        assertFalse(new PagoPasarelaSimuladoAdapter().soporta(MetodoPago.EFECTIVO));
    }

    @Test
    void elEfectivoSiempreSeAprueba() {
        Pago pago = new PagoEfectivoAdapter().cobrar("O1", new BigDecimal("999999"));

        assertEquals(EstadoPago.APROBADO, pago.estado());
        assertEquals("O1", pago.ordenId());
        assertEquals(MetodoPago.EFECTIVO, pago.metodo());
        assertNotNull(pago.fecha());
        assertNotNull(pago.referencia());
    }

    @Test
    void laTarjetaSimuladaApruebaHastaElLimiteYRechazaLoQueLoExcede() {
        PagoTarjetaSimuladoAdapter tarjeta = new PagoTarjetaSimuladoAdapter(new BigDecimal("100.00"));

        assertEquals(EstadoPago.APROBADO, tarjeta.cobrar("O1", new BigDecimal("100.00")).estado());
        assertEquals(EstadoPago.RECHAZADO, tarjeta.cobrar("O1", new BigDecimal("100.01")).estado());
    }

    @Test
    void laPasarelaSimuladaApruebaHastaElLimiteYRechazaLoQueLoExcede() {
        PagoPasarelaSimuladoAdapter pasarela = new PagoPasarelaSimuladoAdapter(new BigDecimal("50"));

        Pago aprobado = pasarela.cobrar("O1", new BigDecimal("50"));
        assertEquals(EstadoPago.APROBADO, aprobado.estado());
        assertEquals(MetodoPago.PASARELA_DIGITAL, aprobado.metodo());
        assertEquals(EstadoPago.RECHAZADO, pasarela.cobrar("O1", new BigDecimal("50.01")).estado());
    }
}
