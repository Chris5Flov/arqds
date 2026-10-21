package com.restaurante.application.port.in;

import com.restaurante.domain.Modificador;
import com.restaurante.domain.Plato;
import java.util.List;

/** RF1: el administrador registra y consulta los platos y modificadores del menú. */
public interface AdministrarMenuUseCase {
    Plato registrarPlato(Plato plato);

    List<Plato> listarPlatos();

    Modificador registrarModificador(Modificador modificador);

    List<Modificador> listarModificadores();
}
