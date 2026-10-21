package com.restaurante.infrastructure.in.web.http;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Convierte los valores de texto que llegan por HTTP a los tipos del dominio.
 * Un valor nulo o vacío se devuelve como null para que el dominio aplique su propia regla
 * (obligatorio, o 0 por defecto en descuento y propina).
 */
public final class Conversion {

    private Conversion() {
    }

    public static BigDecimal decimal(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            throw new SolicitudInvalidaException("El campo " + campo + " debe ser un número (por ejemplo \"85.50\")");
        }
    }

    public static <E extends Enum<E>> E enumeracion(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim());
        } catch (IllegalArgumentException e) {
            String permitidos = Arrays.stream(tipo.getEnumConstants()).map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new SolicitudInvalidaException(
                    "Valor no válido para " + campo + ": \"" + valor + "\". Valores permitidos: " + permitidos);
        }
    }

    /** Un número entero faltante se toma como 0 para que el dominio lo rechace con su mensaje. */
    public static int entero(Integer valor) {
        return valor == null ? 0 : valor;
    }
}
