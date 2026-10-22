package com.restaurante.application.port.in;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import java.util.List;

/** RF1: el administrador registra, consulta, edita y elimina los platos y modificadores del menú. */
public interface AdministrarMenuUseCase {
    Plato registrarPlato(Plato plato);

    List<Plato> listarPlatos();

    /** Reemplaza nombre, precio y tasa del plato con ese id. Las órdenes existentes no cambian. */
    Plato actualizarPlato(Plato plato);

    /** Quita el plato del menú. Las órdenes que ya lo tienen conservan sus líneas. */
    void eliminarPlato(String platoId);

    Modificador registrarModificador(Modificador modificador);

    List<Modificador> listarModificadores();

    /** Reemplaza nombre, tipo y costo del modificador con ese id. Las órdenes existentes no cambian. */
    Modificador actualizarModificador(Modificador modificador);

    /** Quita el modificador del menú. Las órdenes que ya lo tienen conservan sus líneas. */
    void eliminarModificador(String modificadorId);
}
