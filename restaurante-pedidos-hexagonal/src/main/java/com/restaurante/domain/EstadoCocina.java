package com.restaurante.domain;

/**
 * Ciclo de una línea en cocina: PENDIENTE -> EN_COCCION -> LISTO.
 * Una línea solo se puede modificar o cancelar mientras está PENDIENTE (RF3);
 * al cancelarla queda en CANCELADA y se excluye de los totales.
 */
public enum EstadoCocina { PENDIENTE, EN_COCCION, LISTO, CANCELADA }
