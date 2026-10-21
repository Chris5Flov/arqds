package com.restaurante.infrastructure.in.cli;

import java.util.List;
import java.util.Scanner;

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
 * Punto de entrada y raíz de composición: aquí se "conecta" todo.
 * La interacción con el usuario vive en {@link MenuConsola}.
 */
public class Main {
    public static void main(String[] args) {
        // 1. Adaptadores de salida (persistencia en memoria y procesadores de pago)
        PlatoRepository platoRepository = new PlatoRepositoryMemoria();
        ModificadorRepository modificadorRepository = new ModificadorRepositoryMemoria();
        OrdenRepository ordenRepository = new OrdenRepositoryMemoria();
        List<ProcesadorPago> procesadoresPago = List.of(
                new PagoEfectivoAdapter(),
                new PagoTarjetaSimuladoAdapter(),
                new PagoPasarelaSimuladoAdapter());

        // 2. Casos de uso (reciben los puertos de salida)
        AdministrarMenuUseCase administrarMenu = new AdministrarMenuService(platoRepository, modificadorRepository);
        GestionarOrdenUseCase gestionarOrden =
                new GestionarOrdenService(ordenRepository, platoRepository, modificadorRepository);
        GestionarCocinaUseCase gestionarCocina = new GestionarCocinaService(ordenRepository);
        CobrarOrdenUseCase cobrarOrden = new CobrarOrdenService(ordenRepository, procesadoresPago);

        // 3. Adaptador de entrada (menú interactivo por teclado)
        try (Scanner scanner = new Scanner(System.in)) {
            new MenuConsola(administrarMenu, gestionarOrden, gestionarCocina, cobrarOrden,
                    scanner, System.out).iniciar();
        }
    }
}
