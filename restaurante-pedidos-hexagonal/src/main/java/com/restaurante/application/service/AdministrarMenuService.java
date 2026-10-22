package com.restaurante.application.service;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.application.port.out.ModificadorRepository;
import com.restaurante.application.port.out.PlatoRepository;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoModificador;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class AdministrarMenuService implements AdministrarMenuUseCase {
    private static final int MAX_DECIMALES_PRECIO = 2;

    private final PlatoRepository platos;
    private final ModificadorRepository modificadores;

    public AdministrarMenuService(PlatoRepository platos, ModificadorRepository modificadores) {
        this.platos = platos;
        this.modificadores = modificadores;
    }

    @Override
    public Plato registrarPlato(Plato plato) {
        validarPlato(plato);

        if (platos.buscarPorId(plato.id()).isPresent()) {
            throw new ReglaDeNegocioException("Ya existe un plato con id " + plato.id());
        }

        platos.guardar(plato);
        return plato;
    }

    @Override
    public List<Plato> listarPlatos() {
        return platos.listar().stream()
                .sorted(Comparator.comparing(Plato::nombre))
                .toList();
    }

    @Override
    public Plato actualizarPlato(Plato plato) {
        validarPlato(plato);
        exigirPlato(plato.id());

        platos.guardar(plato);
        return plato;
    }

    @Override
    public void eliminarPlato(String platoId) {
        exigirPlato(platoId);
        platos.eliminar(platoId);
    }

    @Override
    public Modificador registrarModificador(Modificador modificador) {
        validarModificador(modificador);

        if (modificadores.buscarPorId(modificador.id()).isPresent()) {
            throw new ReglaDeNegocioException("Ya existe un modificador con id " + modificador.id());
        }

        modificadores.guardar(modificador);
        return modificador;
    }

    @Override
    public List<Modificador> listarModificadores() {
        return modificadores.listar().stream()
                .sorted(Comparator.comparing(Modificador::nombre))
                .toList();
    }

    @Override
    public Modificador actualizarModificador(Modificador modificador) {
        validarModificador(modificador);
        exigirModificador(modificador.id());

        modificadores.guardar(modificador);
        return modificador;
    }

    @Override
    public void eliminarModificador(String modificadorId) {
        exigirModificador(modificadorId);
        modificadores.eliminar(modificadorId);
    }

    private void validarPlato(Plato plato) {
        if (plato == null) {
            throw new ReglaDeNegocioException("El plato es obligatorio");
        }
        validarTexto(plato.id(), "El id del plato es obligatorio");
        validarTexto(plato.nombre(), "El nombre del plato es obligatorio");
        validarPrecio(plato.precioBase());
        validarImpuesto(plato.tasaImpuesto());
    }

    private void validarModificador(Modificador modificador) {
        if (modificador == null) {
            throw new ReglaDeNegocioException("El modificador es obligatorio");
        }
        validarTexto(modificador.id(), "El id del modificador es obligatorio");
        validarTexto(modificador.nombre(), "El nombre del modificador es obligatorio");
        if (modificador.tipo() == null) {
            throw new ReglaDeNegocioException("El tipo del modificador es obligatorio (EXTRA o EXCLUSION)");
        }
        validarCostoExtra(modificador.tipo(), modificador.costoExtra());
    }

    private void exigirPlato(String platoId) {
        if (platoId == null || platos.buscarPorId(platoId).isEmpty()) {
            throw new ReglaDeNegocioException("No existe el plato " + platoId);
        }
    }

    private void exigirModificador(String modificadorId) {
        if (modificadorId == null || modificadores.buscarPorId(modificadorId).isEmpty()) {
            throw new ReglaDeNegocioException("No existe el modificador " + modificadorId);
        }
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
        validarDecimales(precio, "El precio no puede tener más de dos decimales");
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

    // Un EXTRA puede costar 0 o más; una EXCLUSION (quitar un ingrediente) no cambia el precio
    private void validarCostoExtra(TipoModificador tipo, BigDecimal costo) {
        if (costo == null) {
            throw new ReglaDeNegocioException("El costo extra es obligatorio (usa 0 si no tiene costo)");
        }
        if (costo.signum() < 0) {
            throw new ReglaDeNegocioException("El costo extra no puede ser negativo");
        }
        if (tipo == TipoModificador.EXCLUSION && costo.signum() != 0) {
            throw new ReglaDeNegocioException("Una exclusión no puede tener costo extra");
        }
        validarDecimales(costo, "El costo extra no puede tener más de dos decimales");
    }

    private void validarDecimales(BigDecimal monto, String mensaje) {
        if (monto.stripTrailingZeros().scale() > MAX_DECIMALES_PRECIO) {
            throw new ReglaDeNegocioException(mensaje);
        }
    }
}
