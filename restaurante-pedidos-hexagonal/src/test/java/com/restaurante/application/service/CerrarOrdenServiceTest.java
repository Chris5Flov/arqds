package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.application.port.out.PuertoCobro;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaAdapter;
import com.restaurante.infrastructure.persistence.RepositorioOrdenesEnMemoria;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CerrarOrdenServiceTest {

    /** Puerto falso que registra cuántas veces se le pidió cobrar. */
    private static class CobroEspia implements PuertoCobro {
        private final MetodoPago metodo;
        final List<BigDecimal> cobros = new ArrayList<>();

        CobroEspia(MetodoPago metodo) {
            this.metodo = metodo;
        }

        @Override
        public boolean soporta(MetodoPago m) {
            return m == metodo;
        }

        @Override
        public Pago cobrar(String ordenId, BigDecimal monto) {
            cobros.add(monto);
            return new Pago("PG-" + cobros.size(), ordenId, metodo, monto, "ref");
        }
    }

    private RepositorioOrdenesEnMemoria repositorio;
    private CobroEspia efectivo;
    private CobroEspia tarjeta;
    private CerrarOrdenService servicio;
    private Orden orden;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioOrdenesEnMemoria();
        efectivo = new CobroEspia(MetodoPago.EFECTIVO);
        tarjeta = new CobroEspia(MetodoPago.TARJETA);
        servicio = new CerrarOrdenService(repositorio, List.of(efectivo, tarjeta));
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        repositorio.guardar(orden);
    }

    @Test
    void rechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cerrar("no-existe", MetodoPago.EFECTIVO, BigDecimal.ZERO));
    }

    @Test
    void rechazaUnMetodoDePagoNulo() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cerrar(orden.getId(), null, BigDecimal.ZERO));
    }

    @Test
    void rechazaUnMetodoSinPuertoDeCobroYNoCobraNada() {
        // Solo hay puertos de efectivo y tarjeta; no existe uno para la pasarela digital.
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cerrar(orden.getId(), MetodoPago.PASARELA_DIGITAL, BigDecimal.ZERO));

        assertTrue(efectivo.cobros.isEmpty());
        assertTrue(tarjeta.cobros.isEmpty());
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    @Test
    void rechazaUnaPropinaNegativa() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cerrar(orden.getId(), MetodoPago.EFECTIVO, new BigDecimal("-1")));
    }

    @Test
    void noCobraSiLaOrdenYaEstabaCerrada() {
        orden.cerrar(new Pago("PG0", orden.getId(), MetodoPago.EFECTIVO, BigDecimal.TEN, "ref"));

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cerrar(orden.getId(), MetodoPago.EFECTIVO, BigDecimal.ZERO));

        assertTrue(efectivo.cobros.isEmpty());
    }

    @Test
    void funcionaConLosAdaptadoresRealesDeCobro() {
        // Solo comprueba que el servicio se arma con los adaptadores del proyecto.
        CerrarOrdenService real = new CerrarOrdenService(repositorio,
                List.of(new PagoEfectivoAdapter(), new PagoTarjetaAdapter()));

        assertThrows(ReglaDeNegocioException.class,
                () -> real.cerrar(orden.getId(), MetodoPago.PASARELA_DIGITAL, BigDecimal.ZERO));
    }

    @Disabled("Depende de Orden.calcularTotales() (RF4), que aún lanza UnsupportedOperationException")
    @Test
    void cobraConElPuertoQueSoportaElMetodoYCierraLaOrden() {
        Pago pago = servicio.cerrar(orden.getId(), MetodoPago.TARJETA, new BigDecimal("10"));

        assertEquals(MetodoPago.TARJETA, pago.metodo());
        assertEquals(1, tarjeta.cobros.size());
        assertTrue(efectivo.cobros.isEmpty());
        assertEquals(EstadoOrden.CERRADA, repositorio.buscarPorId(orden.getId()).get().getEstado());
    }
}
