package com.restaurante.infrastructure.in.web;

import java.io.IOException;

import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.config.DatosDemo;

/**
 * Punto de entrada web. Argumentos opcionales:
 * <ul>
 *   <li>--puerto=N  puerto HTTP (por defecto 8080, o la variable de entorno PUERTO)</li>
 *   <li>--demo      carga un menú y una orden de ejemplo</li>
 * </ul>
 */
public class MainWeb {
    private static final int PUERTO_POR_DEFECTO = 8080;

    public static void main(String[] args) throws IOException {
        int puerto = puerto(args);
        Aplicacion app = Aplicacion.crear();
        boolean demo = false;
        for (String arg : args) {
            if (arg.equals("--demo")) {
                demo = true;
            }
        }
        if (demo) {
            DatosDemo.cargar(app);
        }

        ServidorWeb servidor = new ServidorWeb(app, puerto);
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));

        System.out.println("=== Sistema de Pedidos de Restaurante ===");
        System.out.println("Interfaz web: http://localhost:" + servidor.getPuerto());
        System.out.println("API REST:     http://localhost:" + servidor.getPuerto() + "/api");
        if (demo) {
            System.out.println("Datos de ejemplo cargados.");
        }
        System.out.println("Presiona Ctrl+C para detener el servidor.");
    }

    private static int puerto(String[] args) {
        String valor = System.getenv("PUERTO");
        for (String arg : args) {
            if (arg.startsWith("--puerto=")) {
                valor = arg.substring("--puerto=".length());
            }
        }
        if (valor == null || valor.isBlank()) {
            return PUERTO_POR_DEFECTO;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Puerto no válido: " + valor);
        }
    }
}
