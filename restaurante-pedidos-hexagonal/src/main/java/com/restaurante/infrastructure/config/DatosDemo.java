package com.restaurante.infrastructure.config;

import java.math.BigDecimal;
import java.util.List;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Plato;
import com.restaurante.domain.TipoModificador;
import com.restaurante.domain.TipoOrden;

/** Carga un menú y una orden de ejemplo usando los casos de uso (para demostraciones y pruebas manuales). */
public final class DatosDemo {

    private DatosDemo() {
    }

    public static void cargar(Aplicacion app) {
        app.administrarMenu().registrarPlato(plato("P-001", "Tacos al pastor", "85.00", "0.16"));
        app.administrarMenu().registrarPlato(plato("P-002", "Pozole rojo", "120.00", "0.16"));
        app.administrarMenu().registrarPlato(plato("P-003", "Quesadilla", "60.00", "0.16"));
        app.administrarMenu().registrarPlato(plato("P-004", "Agua de horchata", "35.00", "0"));

        app.administrarMenu().registrarModificador(
                new Modificador("M-001", "Queso extra", TipoModificador.EXTRA, new BigDecimal("15.00")));
        app.administrarMenu().registrarModificador(
                new Modificador("M-002", "Aguacate", TipoModificador.EXTRA, new BigDecimal("20.00")));
        app.administrarMenu().registrarModificador(
                new Modificador("M-003", "Sin cebolla", TipoModificador.EXCLUSION, BigDecimal.ZERO));

        Orden mesa = app.gestionarOrden().abrir(TipoOrden.SALA, "Mesa 5");
        app.gestionarOrden().agregarLinea(mesa.getId(), "P-001", 3, List.of("M-001", "M-003"));
        app.gestionarOrden().agregarLinea(mesa.getId(), "P-004", 2, List.of());
    }

    private static Plato plato(String id, String nombre, String precio, String tasa) {
        return new Plato(id, nombre, new BigDecimal(precio), new BigDecimal(tasa));
    }
}
