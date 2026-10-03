package com.restaurante.infrastructure.in.cli;

import com.restaurante.application.port.in.AbrirOrdenUseCase;
import com.restaurante.application.service.AbrirOrdenService;
import com.restaurante.domain.Orden;
import com.restaurante.domain.TipoOrden;
import com.restaurante.infrastructure.persistence.RepositorioOrdenesEnMemoria;

/** Adaptador de entrada (consola) y punto donde se "conecta" todo. */
public class Main {
    public static void main(String[] args) {
        // 1. Adaptadores de salida
        RepositorioOrdenesEnMemoria repositorioOrdenes = new RepositorioOrdenesEnMemoria();

        // 2. Casos de uso (reciben los puertos de salida)
        AbrirOrdenUseCase abrirOrden = new AbrirOrdenService(repositorioOrdenes);

        // 3. Ejemplo de punta a punta: abrir una orden para la mesa 5
        Orden orden = abrirOrden.abrir(new AbrirOrdenUseCase.Comando(TipoOrden.SALA, "Mesa 5"));

        System.out.println("Orden abierta: " + orden.getId());
        System.out.println("Tipo: " + orden.getTipo() + " | Referencia: " + orden.getReferencia()
                + " | Estado: " + orden.getEstado());
        System.out.println("Guardada en el repositorio: "
                + repositorioOrdenes.buscarPorId(orden.getId()).isPresent());
    }
}
