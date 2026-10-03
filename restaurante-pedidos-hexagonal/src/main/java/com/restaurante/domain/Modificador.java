package com.restaurante.domain;

import java.math.BigDecimal;

/** Ingrediente extra o exclusión aplicada a un plato (RF1). */
public record Modificador(String nombre, TipoModificador tipo, BigDecimal costoExtra) { }
