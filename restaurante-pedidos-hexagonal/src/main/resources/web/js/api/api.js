// ÚNICO punto de acceso a datos. Las vistas importan solo este módulo.
// Con "?mock" en la URL se usa el simulador en memoria; si no, la API real.

import { peticion } from "./http.js";
import * as simulador from "./mock.js";

export const usandoSimulador = new URLSearchParams(location.search).has("mock");

const id = (valor) => encodeURIComponent(valor);

// ---------------------------------------------------------------- Menú

export function listarPlatos() {
    return usandoSimulador ? simulador.listarPlatos() : peticion("GET", "/platos");
}

export function registrarPlato({ id: platoId, nombre, precioBase, tasaImpuesto }) {
    const cuerpo = { id: platoId, nombre, precioBase, tasaImpuesto };
    return usandoSimulador ? simulador.registrarPlato(cuerpo) : peticion("POST", "/platos", cuerpo);
}

export function actualizarPlato(platoId, { nombre, precioBase, tasaImpuesto }) {
    const cuerpo = { nombre, precioBase, tasaImpuesto };
    return usandoSimulador
        ? simulador.actualizarPlato(platoId, cuerpo)
        : peticion("PUT", `/platos/${id(platoId)}`, cuerpo);
}

export function eliminarPlato(platoId) {
    return usandoSimulador ? simulador.eliminarPlato(platoId) : peticion("DELETE", `/platos/${id(platoId)}`);
}

export function listarModificadores() {
    return usandoSimulador ? simulador.listarModificadores() : peticion("GET", "/modificadores");
}

export function registrarModificador({ id: modificadorId, nombre, tipo, costoExtra }) {
    const cuerpo = { id: modificadorId, nombre, tipo, costoExtra };
    return usandoSimulador ? simulador.registrarModificador(cuerpo) : peticion("POST", "/modificadores", cuerpo);
}

export function actualizarModificador(modificadorId, { nombre, tipo, costoExtra }) {
    const cuerpo = { nombre, tipo, costoExtra };
    return usandoSimulador
        ? simulador.actualizarModificador(modificadorId, cuerpo)
        : peticion("PUT", `/modificadores/${id(modificadorId)}`, cuerpo);
}

export function eliminarModificador(modificadorId) {
    return usandoSimulador
        ? simulador.eliminarModificador(modificadorId)
        : peticion("DELETE", `/modificadores/${id(modificadorId)}`);
}

// ---------------------------------------------------------------- Órdenes

export function listarOrdenes(estado) {
    if (usandoSimulador) {
        return simulador.listarOrdenes(estado);
    }
    return peticion("GET", estado ? `/ordenes?estado=${id(estado)}` : "/ordenes");
}

export function obtenerOrden(ordenId) {
    return usandoSimulador ? simulador.obtenerOrden(ordenId) : peticion("GET", `/ordenes/${id(ordenId)}`);
}

export function abrirOrden({ tipo, referencia }) {
    const cuerpo = { tipo, referencia };
    return usandoSimulador ? simulador.abrirOrden(cuerpo) : peticion("POST", "/ordenes", cuerpo);
}

export function agregarLinea(ordenId, { platoId, cantidad, modificadorIds }) {
    const cuerpo = { platoId, cantidad, modificadorIds: modificadorIds ?? [] };
    return usandoSimulador
        ? simulador.agregarLinea(ordenId, cuerpo)
        : peticion("POST", `/ordenes/${id(ordenId)}/lineas`, cuerpo);
}

export function modificarLinea(ordenId, lineaId, { cantidad, modificadorIds }) {
    const cuerpo = { cantidad, modificadorIds: modificadorIds ?? [] };
    return usandoSimulador
        ? simulador.modificarLinea(ordenId, lineaId, cuerpo)
        : peticion("PUT", `/ordenes/${id(ordenId)}/lineas/${id(lineaId)}`, cuerpo);
}

export function cancelarLinea(ordenId, lineaId) {
    return usandoSimulador
        ? simulador.cancelarLinea(ordenId, lineaId)
        : peticion("DELETE", `/ordenes/${id(ordenId)}/lineas/${id(lineaId)}`);
}

export function cancelarOrden(ordenId) {
    return usandoSimulador
        ? simulador.cancelarOrden(ordenId)
        : peticion("POST", `/ordenes/${id(ordenId)}/cancelar`);
}

// ---------------------------------------------------------------- Cocina

export function listarPendientesCocina() {
    return usandoSimulador ? simulador.listarPendientesCocina() : peticion("GET", "/cocina/pendientes");
}

export function iniciarCoccion(ordenId, lineaId) {
    return usandoSimulador
        ? simulador.iniciarCoccion(ordenId, lineaId)
        : peticion("POST", `/cocina/ordenes/${id(ordenId)}/lineas/${id(lineaId)}/iniciar`);
}

export function marcarLista(ordenId, lineaId) {
    return usandoSimulador
        ? simulador.marcarLista(ordenId, lineaId)
        : peticion("POST", `/cocina/ordenes/${id(ordenId)}/lineas/${id(lineaId)}/lista`);
}

// ---------------------------------------------------------------- Caja

export function calcularTotales(ordenId, { descuento, propina }) {
    if (usandoSimulador) {
        return simulador.calcularTotales(ordenId, { descuento, propina });
    }
    const parametros = new URLSearchParams({ descuento, propina });
    return peticion("GET", `/caja/ordenes/${id(ordenId)}/totales?${parametros}`);
}

export function cobrar(ordenId, { metodo, descuento, propina }) {
    const cuerpo = { metodo, descuento, propina };
    return usandoSimulador
        ? simulador.cobrar(ordenId, cuerpo)
        : peticion("POST", `/caja/ordenes/${id(ordenId)}/cobrar`, cuerpo);
}
