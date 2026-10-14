package com.restaurante.infrastructure.in.cli;

import com.restaurante.application.port.in.AbrirOrdenUseCase;
import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.application.port.in.CerrarOrdenUseCase;
import com.restaurante.application.port.out.PuertoCobro;
import com.restaurante.application.port.out.RepositorioMenu;
import com.restaurante.application.port.out.RepositorioOrdenes;
import com.restaurante.application.service.AbrirOrdenService;
import com.restaurante.application.service.AdministrarMenuService;
import com.restaurante.application.service.CerrarOrdenService;
import com.restaurante.infrastructure.payment.PagoEfectivoAdapter;
import com.restaurante.infrastructure.payment.PagoPasarelaDigitalAdapter;
import com.restaurante.infrastructure.payment.PagoTarjetaAdapter;
import com.restaurante.infrastructure.persistence.RepositorioMenuEnMemoria;
import com.restaurante.infrastructure.persistence.RepositorioOrdenesEnMemoria;
import java.util.List;
import java.util.Scanner;

/**
 * Punto de entrada y raíz de composición: aquí se "conecta" todo.
 * La interacción con el usuario vive en {@link MenuConsola}.
 */
public class Main {
    public static void main(String[] args) {
        // 1. Adaptadores de salida (persistencia en memoria y cobros)
        RepositorioOrdenes repositorioOrdenes = new RepositorioOrdenesEnMemoria();
        RepositorioMenu repositorioMenu = new RepositorioMenuEnMemoria();
        List<PuertoCobro> puertosCobro = List.of(
                new PagoEfectivoAdapter(),
                new PagoTarjetaAdapter(),
                new PagoPasarelaDigitalAdapter());

        // 2. Casos de uso (reciben los puertos de salida)
        AdministrarMenuUseCase administrarMenu = new AdministrarMenuService(repositorioMenu);
        AbrirOrdenUseCase abrirOrden = new AbrirOrdenService(repositorioOrdenes);
        CerrarOrdenUseCase cerrarOrden = new CerrarOrdenService(repositorioOrdenes, puertosCobro);

        // 3. Adaptador de entrada (menú interactivo por teclado)
        try (Scanner scanner = new Scanner(System.in)) {
            new MenuConsola(administrarMenu, abrirOrden, cerrarOrden, scanner, System.out).iniciar();
        }
    }
}
