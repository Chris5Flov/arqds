package com.restaurante.infrastructure.in.web.http;

import java.util.Map;

/**
 * Datos de una petición HTTP ya resuelta por el {@link Enrutador}.
 *
 * @param parametrosRuta    valores de los segmentos {nombre} de la ruta
 * @param parametrosConsulta valores de la query string (?clave=valor), ya decodificados
 */
public record Peticion(String metodo,
                       String ruta,
                       Map<String, String> parametrosRuta,
                       Map<String, String> parametrosConsulta,
                       String cuerpo) {

    public String parametro(String nombre) {
        return parametrosRuta.get(nombre);
    }

    /** @return el valor de la query string, o null si no viene. */
    public String consulta(String nombre) {
        return parametrosConsulta.get(nombre);
    }

    public <T> T cuerpoComo(Class<T> tipo) {
        return Json.leer(cuerpo, tipo);
    }
}
