package com.restaurante.infrastructure.in.web.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tabla de rutas de la API. Cada ruta es un método HTTP y un patrón como
 * "/api/ordenes/{ordenId}/lineas/{lineaId}"; los segmentos entre llaves se entregan como parámetros.
 */
public class Enrutador {

    /** Lógica de un endpoint. */
    @FunctionalInterface
    public interface Accion {
        Respuesta ejecutar(Peticion peticion);
    }

    /** Ruta encontrada junto con los parámetros extraídos del path. */
    public record Coincidencia(Accion accion, Map<String, String> parametros) { }

    private record Ruta(String metodo, String[] segmentos, Accion accion) { }

    private final List<Ruta> rutas = new ArrayList<>();

    public Enrutador get(String patron, Accion accion) {
        return agregar("GET", patron, accion);
    }

    public Enrutador post(String patron, Accion accion) {
        return agregar("POST", patron, accion);
    }

    public Enrutador put(String patron, Accion accion) {
        return agregar("PUT", patron, accion);
    }

    public Enrutador delete(String patron, Accion accion) {
        return agregar("DELETE", patron, accion);
    }

    private Enrutador agregar(String metodo, String patron, Accion accion) {
        rutas.add(new Ruta(metodo, segmentar(patron), accion));
        return this;
    }

    public Optional<Coincidencia> resolver(String metodo, String ruta) {
        String[] segmentos = segmentar(ruta);
        for (Ruta candidata : rutas) {
            if (!candidata.metodo().equals(metodo) || candidata.segmentos().length != segmentos.length) {
                continue;
            }
            Map<String, String> parametros = coincidir(candidata.segmentos(), segmentos);
            if (parametros != null) {
                return Optional.of(new Coincidencia(candidata.accion(), parametros));
            }
        }
        return Optional.empty();
    }

    private static Map<String, String> coincidir(String[] patron, String[] ruta) {
        Map<String, String> parametros = new HashMap<>();
        for (int i = 0; i < patron.length; i++) {
            String esperado = patron[i];
            if (esperado.startsWith("{") && esperado.endsWith("}")) {
                parametros.put(esperado.substring(1, esperado.length() - 1), ruta[i]);
            } else if (!esperado.equals(ruta[i])) {
                return null;
            }
        }
        return parametros;
    }

    private static String[] segmentar(String ruta) {
        String limpia = ruta.replaceAll("^/+|/+$", "");
        return limpia.isEmpty() ? new String[0] : limpia.split("/+");
    }
}
