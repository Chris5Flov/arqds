package com.restaurante.infrastructure.config;

import java.util.List;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.application.port.out.ModificadorRepository;
import com.restaurante.application.port.out.OrdenRepository;
import com.restaurante.application.port.out.PlatoRepository;
import com.restaurante.application.port.out.ProcesadorPago;
import com.restaurante.application.service.AdministrarMenuService;
import com.restaurante.application.service.CobrarOrdenService;
import com.restaurante.application.service.GestionarCocinaService;
import com.restaurante.application.service.GestionarOrdenService;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoPasarelaSimuladoAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaSimuladoAdapter;
import com.restaurante.infrastructure.persistence.ModificadorRepositoryMemoria;
import com.restaurante.infrastructure.persistence.OrdenRepositoryMemoria;
import com.restaurante.infrastructure.persistence.PlatoRepositoryMemoria;

/**
 * Raíz de composición: conecta los adaptadores de salida con los casos de uso.
 * La comparten todos los adaptadores de entrada (consola y web).
 */
public record Aplicacion(AdministrarMenuUseCase administrarMenu,
                         GestionarOrdenUseCase gestionarOrden,
                         GestionarCocinaUseCase gestionarCocina,
                         CobrarOrdenUseCase cobrarOrden) {

    public static Aplicacion crear() {
        // 1. Adaptadores de salida (persistencia en memoria y procesadores de pago)
        PlatoRepository platoRepository = new PlatoRepositoryMemoria();
        ModificadorRepository modificadorRepository = new ModificadorRepositoryMemoria();
        OrdenRepository ordenRepository = new OrdenRepositoryMemoria();
        List<ProcesadorPago> procesadoresPago = List.of(
                new PagoEfectivoAdapter(),
                new PagoTarjetaSimuladoAdapter(),
                new PagoPasarelaSimuladoAdapter());

        // 2. Casos de uso (reciben los puertos de salida)
        return new Aplicacion(
                new AdministrarMenuService(platoRepository, modificadorRepository),
                new GestionarOrdenService(ordenRepository, platoRepository, modificadorRepository),
                new GestionarCocinaService(ordenRepository),
                new CobrarOrdenService(ordenRepository, procesadoresPago));
    }
}
