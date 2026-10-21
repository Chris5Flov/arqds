package com.restaurante.infrastructure.in.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.restaurante.infrastructure.config.Aplicacion;
import com.restaurante.infrastructure.in.web.http.ApiHandler;
import com.restaurante.infrastructure.in.web.http.Enrutador;
import com.sun.net.httpserver.HttpServer;

/**
 * Adaptador de entrada web: un servidor HTTP del JDK que expone la API REST en /api
 * y sirve la interfaz web (src/main/resources/web/) en /.
 */
public class ServidorWeb {

    private final HttpServer servidor;
    private final ExecutorService hilos;

    /** @param puerto puerto TCP; 0 elige uno libre (útil en pruebas). */
    public ServidorWeb(Aplicacion app, int puerto) throws IOException {
        Enrutador enrutador = new Enrutador();
        List<RestController> controladores = List.of(
                new MenuRestController(app.administrarMenu()),
                new OrdenRestController(app.gestionarOrden()),
                new CocinaRestController(app.gestionarCocina()),
                new CajaRestController(app.cobrarOrden()));
        controladores.forEach(c -> c.registrarRutas(enrutador));

        servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/", new ApiHandler(enrutador));
        servidor.createContext("/", new ArchivosEstaticosHandler("web"));
        hilos = Executors.newFixedThreadPool(8);
        servidor.setExecutor(hilos);
    }

    public void iniciar() {
        servidor.start();
    }

    public void detener() {
        servidor.stop(0);
        hilos.shutdownNow();
    }

    public int getPuerto() {
        return servidor.getAddress().getPort();
    }
}
