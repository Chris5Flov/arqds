package com.restaurante.infrastructure.in.web;

import java.util.List;

import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.domain.EstadoOrden;
import com.restaurante.domain.Orden;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.in.web.dto.MapeadorDto;
import com.restaurante.infrastructure.in.web.dto.SolicitudesDto.AbrirOrdenRequest;
import com.restaurante.infrastructure.in.web.dto.SolicitudesDto.AgregarLineaRequest;
import com.restaurante.infrastructure.in.web.dto.SolicitudesDto.ModificarLineaRequest;
import com.restaurante.infrastructure.in.web.http.Conversion;
import com.restaurante.infrastructure.in.web.http.Enrutador;
import com.restaurante.infrastructure.in.web.http.Peticion;
import com.restaurante.infrastructure.in.web.http.Respuesta;

/** RF2 y RF3 (Mesero): órdenes y sus líneas. */
public class OrdenRestController implements RestController {

    private final GestionarOrdenUseCase gestionarOrden;

    public OrdenRestController(GestionarOrdenUseCase gestionarOrden) {
        this.gestionarOrden = gestionarOrden;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador
                .get("/api/ordenes", this::listar)
                .get("/api/ordenes/{ordenId}", this::consultar)
                .post("/api/ordenes", this::abrir)
                .post("/api/ordenes/{ordenId}/lineas", this::agregarLinea)
                .put("/api/ordenes/{ordenId}/lineas/{lineaId}", this::modificarLinea)
                .delete("/api/ordenes/{ordenId}/lineas/{lineaId}", this::cancelarLinea)
                .post("/api/ordenes/{ordenId}/cancelar", this::cancelarOrden);
    }

    /** ?estado=ABIERTA|CERRADA|CANCELADA es opcional; sin él se devuelven todas. */
    private Respuesta listar(Peticion peticion) {
        EstadoOrden estado = Conversion.enumeracion(EstadoOrden.class, peticion.consulta("estado"), "estado");
        List<Orden> ordenes = estado == null
                ? gestionarOrden.listar()
                : gestionarOrden.listar().stream().filter(o -> o.getEstado() == estado).toList();
        return Respuesta.ok(MapeadorDto.ordenes(ordenes));
    }

    private Respuesta consultar(Peticion peticion) {
        return Respuesta.ok(MapeadorDto.aDto(gestionarOrden.consultar(peticion.parametro("ordenId"))));
    }

    private Respuesta abrir(Peticion peticion) {
        AbrirOrdenRequest solicitud = peticion.cuerpoComo(AbrirOrdenRequest.class);
        Orden orden = gestionarOrden.abrir(
                Conversion.enumeracion(TipoOrden.class, solicitud.tipo(), "tipo"),
                solicitud.referencia());
        return Respuesta.creado(MapeadorDto.aDto(orden));
    }

    private Respuesta agregarLinea(Peticion peticion) {
        AgregarLineaRequest solicitud = peticion.cuerpoComo(AgregarLineaRequest.class);
        Orden orden = gestionarOrden.agregarLinea(peticion.parametro("ordenId"), solicitud.platoId(),
                Conversion.entero(solicitud.cantidad()), solicitud.modificadorIds());
        return Respuesta.ok(MapeadorDto.aDto(orden));
    }

    private Respuesta modificarLinea(Peticion peticion) {
        ModificarLineaRequest solicitud = peticion.cuerpoComo(ModificarLineaRequest.class);
        Orden orden = gestionarOrden.modificarLinea(peticion.parametro("ordenId"), peticion.parametro("lineaId"),
                Conversion.entero(solicitud.cantidad()), solicitud.modificadorIds());
        return Respuesta.ok(MapeadorDto.aDto(orden));
    }

    private Respuesta cancelarLinea(Peticion peticion) {
        Orden orden = gestionarOrden.cancelarLinea(peticion.parametro("ordenId"), peticion.parametro("lineaId"));
        return Respuesta.ok(MapeadorDto.aDto(orden));
    }

    private Respuesta cancelarOrden(Peticion peticion) {
        return Respuesta.ok(MapeadorDto.aDto(gestionarOrden.cancelarOrden(peticion.parametro("ordenId"))));
    }
}
