package com.restaurante.domain;

import java.math.BigDecimal;

/** Elemento del menú (RF1). tasaImpuesto se expresa como fracción, p. ej. 0.16. */
public record Plato(String id, String nombre, BigDecimal precioBase, BigDecimal tasaImpuesto) { }
