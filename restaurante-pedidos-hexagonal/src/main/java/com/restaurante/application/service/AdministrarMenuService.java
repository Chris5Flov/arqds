package com.restaurante.application.service;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.application.port.out.RepositorioMenu;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class AdministrarMenuService implements AdministrarMenuUseCase {
    private static final int MAX_DECIMALES_PRECIO = 2;

    private final RepositorioMenu menu;

    public AdministrarMenuService(RepositorioMenu menu) {
        this.menu = menu;
    }

    @Override
    public Plato registrar(Plato plato) {
        if (plato == null) {
            throw new ReglaDeNegocioException("El plato es obligatorio");
        }
        validarTexto(plato.id(), "El id del plato es obligatorio");
        validarTexto(plato.nombre(), "El nombre del plato es obligatorio");
        validarPrecio(plato.precioBase());
        validarImpuesto(plato.tasaImpuesto());

        if (menu.buscarPorId(plato.id()).isPresent()) {
            throw new ReglaDeNegocioException("Ya existe un plato con id " + plato.id());
        }

        menu.guardar(plato);
        return plato;
    }

    @Override
    public List<Plato> listar() {
        return menu.listar().stream()
                .sorted(Comparator.comparing(Plato::nombre))
                .toList();
    }

    private void validarTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDeNegocioException(mensaje);
        }
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new ReglaDeNegocioException("El precio es obligatorio");
        }
        if (precio.signum() <= 0) {
            throw new ReglaDeNegocioException("El precio debe ser mayor a cero");
        }
        if (precio.stripTrailingZeros().scale() > MAX_DECIMALES_PRECIO) {
            throw new ReglaDeNegocioException("El precio no puede tener más de dos decimales");
        }
    }

    // La tasa se maneja como fracción (0.16 = 16 %), no como porcentaje
    private void validarImpuesto(BigDecimal tasa) {
        if (tasa == null) {
            throw new ReglaDeNegocioException("La tasa de impuesto es obligatoria");
        }
        if (tasa.signum() < 0 || tasa.compareTo(BigDecimal.ONE) > 0) {
            throw new ReglaDeNegocioException("La tasa de impuesto debe estar entre 0 y 1 (por ejemplo 0.16)");
        }
    }
}
