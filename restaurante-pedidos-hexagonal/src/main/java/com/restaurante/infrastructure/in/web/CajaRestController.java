package com.restaurante.infrastructure.in.web;

import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Totales;
import com.restaurante.infrastructure.in.web.dto.MapeadorDto;
import com.restaurante.infrastructure.in.web.dto.SolicitudesDto.CobrarRequest;
import com.restaurante.infrastructure.in.web.http.Conversion;
import com.restaurante.infrastructure.in.web.http.Enrutador;
import com.restaurante.infrastructure.in.web.http.Peticion;
import com.restaurante.infrastructure.in.web.http.Respuesta;

/** RF4 y RF5 (Cajero): totales y cobro de una orden. */
public class CajaRestController implements RestController {

    private final CobrarOrdenUseCase cobrarOrden;

    public CajaRestController(CobrarOrdenUseCase cobrarOrden) {
        this.cobrarOrden = cobrarOrden;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador
                .get("/api/caja/ordenes/{ordenId}/totales", this::calcularTotales)
                .post("/api/caja/ordenes/{ordenId}/cobrar", this::cobrar);
    }

    /** ?descuento=19&propina=10 son opcionales (vacío o ausente = 0). */
    private Respuesta calcularTotales(Peticion peticion) {
        Totales totales = cobrarOrden.calcularTotales(peticion.parametro("ordenId"),
                Conversion.decimal(peticion.consulta("descuento"), "descuento"),
                Conversion.decimal(peticion.consulta("propina"), "propina"));
        return Respuesta.ok(MapeadorDto.aDto(totales));
    }

    /** Responde 200 tanto si el pago se aprueba como si se rechaza; el estado viene en el PagoDto. */
    private Respuesta cobrar(Peticion peticion) {
        CobrarRequest solicitud = peticion.cuerpoComo(CobrarRequest.class);
        Pago pago = cobrarOrden.cobrar(peticion.parametro("ordenId"),
                Conversion.enumeracion(MetodoPago.class, solicitud.metodo(), "metodo"),
                Conversion.decimal(solicitud.descuento(), "descuento"),
                Conversion.decimal(solicitud.propina(), "propina"));
        return Respuesta.ok(MapeadorDto.aDto(pago));
    }
}
