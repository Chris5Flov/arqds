package com.restaurante.infrastructure.in.web;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.domain.TipoModificador;
import com.restaurante.infrastructure.in.web.dto.MapeadorDto;
import com.restaurante.infrastructure.in.web.dto.SolicitudesDto.RegistrarModificadorRequest;
import com.restaurante.infrastructure.in.web.dto.SolicitudesDto.RegistrarPlatoRequest;
import com.restaurante.infrastructure.in.web.http.Conversion;
import com.restaurante.infrastructure.in.web.http.Enrutador;
import com.restaurante.infrastructure.in.web.http.Peticion;
import com.restaurante.infrastructure.in.web.http.Respuesta;

/** RF1 (Administrador): platos y modificadores del menú. */
public class MenuRestController implements RestController {

    private final AdministrarMenuUseCase administrarMenu;

    public MenuRestController(AdministrarMenuUseCase administrarMenu) {
        this.administrarMenu = administrarMenu;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador
                .get("/api/platos", this::listarPlatos)
                .post("/api/platos", this::registrarPlato)
                .get("/api/modificadores", this::listarModificadores)
                .post("/api/modificadores", this::registrarModificador);
    }

    private Respuesta listarPlatos(Peticion peticion) {
        return Respuesta.ok(administrarMenu.listarPlatos().stream().map(MapeadorDto::aDto).toList());
    }

    private Respuesta registrarPlato(Peticion peticion) {
        RegistrarPlatoRequest solicitud = peticion.cuerpoComo(RegistrarPlatoRequest.class);
        Plato plato = new Plato(solicitud.id(), solicitud.nombre(),
                Conversion.decimal(solicitud.precioBase(), "precioBase"),
                Conversion.decimal(solicitud.tasaImpuesto(), "tasaImpuesto"));
        return Respuesta.creado(MapeadorDto.aDto(administrarMenu.registrarPlato(plato)));
    }

    private Respuesta listarModificadores(Peticion peticion) {
        return Respuesta.ok(administrarMenu.listarModificadores().stream().map(MapeadorDto::aDto).toList());
    }

    private Respuesta registrarModificador(Peticion peticion) {
        RegistrarModificadorRequest solicitud = peticion.cuerpoComo(RegistrarModificadorRequest.class);
        Modificador modificador = new Modificador(solicitud.id(), solicitud.nombre(),
                Conversion.enumeracion(TipoModificador.class, solicitud.tipo(), "tipo"),
                Conversion.decimal(solicitud.costoExtra(), "costoExtra"));
        return Respuesta.creado(MapeadorDto.aDto(administrarMenu.registrarModificador(modificador)));
    }
}
