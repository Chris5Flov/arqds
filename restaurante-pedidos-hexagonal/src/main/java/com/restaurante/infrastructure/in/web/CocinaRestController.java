package com.restaurante.infrastructure.in.web;

import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.infrastructure.in.web.dto.MapeadorDto;
import com.restaurante.infrastructure.in.web.http.Enrutador;
import com.restaurante.infrastructure.in.web.http.Peticion;
import com.restaurante.infrastructure.in.web.http.Respuesta;

/** RF3 (Cocinero): líneas pendientes y avance de su estado en cocina. */
public class CocinaRestController implements RestController {

    private final GestionarCocinaUseCase gestionarCocina;

    public CocinaRestController(GestionarCocinaUseCase gestionarCocina) {
        this.gestionarCocina = gestionarCocina;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador
                .get("/api/cocina/pendientes", this::listarPendientes)
                .post("/api/cocina/ordenes/{ordenId}/lineas/{lineaId}/iniciar", this::iniciarCoccion)
                .post("/api/cocina/ordenes/{ordenId}/lineas/{lineaId}/lista", this::marcarLista);
    }

    private Respuesta listarPendientes(Peticion peticion) {
        return Respuesta.ok(gestionarCocina.listarPendientes().stream().map(MapeadorDto::aDto).toList());
    }

    private Respuesta iniciarCoccion(Peticion peticion) {
        gestionarCocina.iniciarCoccion(peticion.parametro("ordenId"), peticion.parametro("lineaId"));
        return Respuesta.sinContenido();
    }

    private Respuesta marcarLista(Peticion peticion) {
        gestionarCocina.marcarLista(peticion.parametro("ordenId"), peticion.parametro("lineaId"));
        return Respuesta.sinContenido();
    }
}
