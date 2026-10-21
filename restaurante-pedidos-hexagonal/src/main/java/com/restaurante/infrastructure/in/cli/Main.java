package com.restaurante.infrastructure.in.cli;

import java.util.Arrays;
import java.util.Scanner;

import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.config.DatosDemo;

/**
 * Punto de entrada de la consola. Los casos de uso se arman en {@link Aplicacion};
 * la interacción con el usuario vive en {@link MenuConsola}.
 * Argumento opcional: --demo (carga un menú de ejemplo).
 */
public class Main {
    public static void main(String[] args) {
        Aplicacion app = Aplicacion.crear();
        if (Arrays.asList(args).contains("--demo")) {
            DatosDemo.cargar(app);
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new MenuConsola(app.administrarMenu(), app.gestionarOrden(), app.gestionarCocina(), app.cobrarOrden(),
                    scanner, System.out).iniciar();
        }
    }
}
