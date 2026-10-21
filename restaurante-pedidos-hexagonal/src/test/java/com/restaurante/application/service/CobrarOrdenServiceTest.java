package com.restaurante.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.EstadoPago;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.PagosDePrueba;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;
import com.restaurante.domain.Totales;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaSimuladoAdapter;
import com.restaurante.infrastructure.persistence.OrdenRepositoryMemoria;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CobrarOrdenServiceTest {

    /** Procesador falso que registra los montos cobrados y responde con el estado configurado. */
    private static class ProcesadorEspia implements ProcesadorPago {
        private final MetodoPago metodo;
        private EstadoPago respuesta = EstadoPago.APROBADO;
        final List<BigDecimal> cobros = new ArrayList<>();

        ProcesadorEspia(MetodoPago metodo) {
            this.metodo = metodo;
        }

        @Override
        public boolean soporta(MetodoPago m) {
            return m == metodo;
        }

        @Override
        public Pago cobrar(String ordenId, BigDecimal monto) {
            cobros.add(monto);
            return PagosDePrueba.pago(ordenId, respuesta, monto);
        }
    }

    private OrdenRepositoryMemoria repositorio;
    private ProcesadorEspia efectivo;
    private ProcesadorEspia tarjeta;
    private CobrarOrdenService servicio;
    private Orden orden;

    @BeforeEach
    void setUp() {
        repositorio = new OrdenRepositoryMemoria();
        efectivo = new ProcesadorEspia(MetodoPago.EFECTIVO);
        tarjeta = new ProcesadorEspia(MetodoPago.TARJETA);
        servicio = new CobrarOrdenService(repositorio, List.of(efectivo, tarjeta));
        orden = Orden.abrir(TipoOrden.SALA, "Mesa 5");
        // 85 x 2 al 16 % + 20 x 1 sin impuesto = 190.00 + 27.20 de impuestos = 217.20
        orden.agregarLinea(new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.16")), 2, List.of());
        orden.agregarLinea(new Plato("P2", "Agua", new BigDecimal("20"), BigDecimal.ZERO), 1, List.of());
        repositorio.guardar(orden);
    }

    private static BigDecimal dinero(String valor) {
        return new BigDecimal(valor);
    }

    // ---------------------------------------------------------------- calcular totales (RF4)

    @Test
    void calculaLosTotalesConDescuentoYPropina() {
        // 190 - 19 = 171; impuestos de los tacos: 170 x 0.16 x 171 / 190 = 24.48
        Totales t = servicio.calcularTotales(orden.getId(), dinero("19"), dinero("10"));

        assertEquals(dinero("190.00"), t.subtotal());
        assertEquals(dinero("19.00"), t.descuento());
        assertEquals(dinero("24.48"), t.impuestos());
        assertEquals(dinero("10.00"), t.propina());
        assertEquals(dinero("205.48"), t.total());
    }

    @Test
    void descuentoYPropinaNulosSeTomanComoCero() {
        assertEquals(dinero("217.20"), servicio.calcularTotales(orden.getId(), null, null).total());
    }

    @Test
    void calcularRechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcularTotales("no-existe", BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void calcularRechazaDescuentoMayorAlSubtotalOPropinaNegativa() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcularTotales(orden.getId(), dinero("190.01"), BigDecimal.ZERO));
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.calcularTotales(orden.getId(), BigDecimal.ZERO, dinero("-1")));
    }

    // ---------------------------------------------------------------- cobrar (RF5)

    @Test
    void cobraConElProcesadorQueSoportaElMetodoYCierraLaOrden() {
        Pago pago = servicio.cobrar(orden.getId(), MetodoPago.TARJETA, BigDecimal.ZERO, dinero("10"));

        // total + propina
        assertEquals(dinero("227.20"), pago.monto());
        assertEquals(List.of(dinero("227.20")), tarjeta.cobros);
        assertTrue(efectivo.cobros.isEmpty());
        Orden guardada = repositorio.buscarPorId(orden.getId()).orElseThrow();
        assertEquals(EstadoOrden.CERRADA, guardada.getEstado());
        assertEquals(List.of(pago), guardada.getPagos());
        assertEquals(dinero("227.20"), guardada.getTotales().orElseThrow().total());
    }

    @Test
    void cobraElMismoMontoQueMuestranLosTotalesConDescuento() {
        Totales mostrados = servicio.calcularTotales(orden.getId(), dinero("19"), dinero("10"));

        Pago pago = servicio.cobrar(orden.getId(), MetodoPago.EFECTIVO, dinero("19"), dinero("10"));

        assertEquals(mostrados.total(), pago.monto());
        assertEquals(mostrados, orden.getTotales().orElseThrow());
    }

    @Test
    void unPagoRechazadoQuedaRegistradoYLaOrdenSigueAbierta() {
        tarjeta.respuesta = EstadoPago.RECHAZADO;

        Pago rechazado = servicio.cobrar(orden.getId(), MetodoPago.TARJETA, null, null);

        assertFalse(rechazado.esAprobado());
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
        assertEquals(List.of(rechazado), orden.getPagos());

        // Se puede reintentar con otro método
        Pago aprobado = servicio.cobrar(orden.getId(), MetodoPago.EFECTIVO, null, null);
        assertTrue(aprobado.esAprobado());
        assertEquals(EstadoOrden.CERRADA, orden.getEstado());
        assertEquals(List.of(rechazado, aprobado), orden.getPagos());
    }

    @Test
    void rechazaUnaOrdenQueNoExiste() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar("no-existe", MetodoPago.EFECTIVO, null, null));
    }

    @Test
    void rechazaUnMetodoDePagoNulo() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(orden.getId(), null, null, null));
    }

    @Test
    void rechazaUnMetodoSinProcesadorYNoCobraNada() {
        // Solo hay procesadores de efectivo y tarjeta; no existe uno para la pasarela digital.
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(orden.getId(), MetodoPago.PASARELA_DIGITAL, null, null));

        assertTrue(efectivo.cobros.isEmpty());
        assertTrue(tarjeta.cobros.isEmpty());
        assertEquals(EstadoOrden.ABIERTA, orden.getEstado());
    }

    @Test
    void rechazaUnaPropinaNegativaSinCobrar() {
        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(orden.getId(), MetodoPago.EFECTIVO, null, dinero("-1")));
        assertTrue(efectivo.cobros.isEmpty());
    }

    @Test
    void noCobraUnaOrdenSinLineas() {
        Orden vacia = Orden.abrir(TipoOrden.PARA_LLEVAR, "Pedido 1");
        repositorio.guardar(vacia);

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(vacia.getId(), MetodoPago.EFECTIVO, null, null));
        assertTrue(efectivo.cobros.isEmpty());
    }

    @Test
    void noCobraSiLaOrdenYaEstabaCerrada() {
        PagosDePrueba.cerrar(orden);

        assertThrows(ReglaDeNegocioException.class,
                () -> servicio.cobrar(orden.getId(), MetodoPago.EFECTIVO, null, null));
        assertTrue(efectivo.cobros.isEmpty());
    }

    @Test
    void funcionaConLosAdaptadoresDelProyecto() {
        CobrarOrdenService real = new CobrarOrdenService(repositorio,
                List.of(new PagoEfectivoAdapter(), new PagoTarjetaSimuladoAdapter()));

        Pago pago = real.cobrar(orden.getId(), MetodoPago.TARJETA, null, null);

        assertEquals(EstadoPago.APROBADO, pago.estado());
        assertEquals(orden.getId(), pago.ordenId());
        assertEquals(EstadoOrden.CERRADA, orden.getEstado());
    }
}
