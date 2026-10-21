package com.restaurante.infrastructure.in.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.PagosDePrueba;
import com.restaurante.domain.Plato;
import com.restaurante.domain.TipoModificador;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.LineaDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.OrdenDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapeadorDtoTest {

    private static final Plato TACOS = new Plato("P1", "Tacos", new BigDecimal("85"), new BigDecimal("0.160"));

    @Test
    void formateaDineroConDosDecimalesYTasasSinCerosSobrantes() {
        assertEquals("85.00", MapeadorDto.dinero(new BigDecimal("85")));
        assertEquals("85.50", MapeadorDto.dinero(new BigDecimal("85.5")));
        assertEquals("0.16", MapeadorDto.tasa(new BigDecimal("0.160")));
        assertEquals("0", MapeadorDto.tasa(new BigDecimal("0.00")));
    }

    @Test
    void formateaFechasSiempreConSegundos() {
        assertEquals("2026-10-20T13:45:00", MapeadorDto.fecha(LocalDateTime.of(2026, 10, 20, 13, 45, 0, 999)));
    }

    @Test
    void mapeaUnPlato() {
        assertEquals(new RespuestasDto.PlatoDto("P1", "Tacos", "85.00", "0.16"), MapeadorDto.aDto(TACOS));
    }

    @Test
    void mapeaLaLineaConElCostoAplicadoYSiEsEditable() {
        Orden orden = Orden.abrir(TipoOrden.SALA, "Mesa 1");
        Modificador queso = new Modificador("M1", "Queso", TipoModificador.EXTRA, new BigDecimal("10"));
        LineaPedido linea = orden.agregarLinea(TACOS, 2, List.of(queso));

        LineaDto dto = MapeadorDto.aDto(linea);

        assertEquals("P1", dto.platoId());
        assertEquals("Tacos", dto.platoNombre());
        assertEquals(2, dto.cantidad());
        assertEquals("85.00", dto.precioUnitario());
        assertEquals("0.16", dto.tasaImpuesto());
        assertEquals(List.of(new RespuestasDto.ModificadorDto("M1", "Queso", "EXTRA", "10.00")), dto.modificadores());
        assertEquals("PENDIENTE", dto.estadoCocina());
        assertTrue(dto.editable());

        orden.iniciarCoccionLinea(linea.getId());
        assertFalse(MapeadorDto.aDto(linea).editable());
    }

    @Test
    void unaOrdenAbiertaLlevaLosTotalesActualesYUnaCerradaLosCobrados() {
        Orden orden = Orden.abrir(TipoOrden.DELIVERY, "Pedido 1");
        orden.agregarLinea(TACOS, 1, List.of());

        OrdenDto abierta = MapeadorDto.aDto(orden);
        assertEquals("ABIERTA", abierta.estado());
        assertEquals("DELIVERY", abierta.tipo());
        assertEquals("98.60", abierta.totales().total());
        assertTrue(abierta.pagos().isEmpty());

        PagosDePrueba.cerrar(orden);
        OrdenDto cerrada = MapeadorDto.aDto(orden);
        assertEquals("CERRADA", cerrada.estado());
        assertEquals("98.60", cerrada.totales().total());
        assertEquals(1, cerrada.pagos().size());
        assertEquals("APROBADO", cerrada.pagos().get(0).estado());
    }
}
