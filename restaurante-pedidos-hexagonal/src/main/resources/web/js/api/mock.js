// Simulador de la API en memoria, para trabajar sin backend (?mock en la URL).
// Exporta las MISMAS funciones que api.js e imita las reglas principales del dominio.
// Aquí sí se calculan montos, de forma aproximada; con la API real los calcula el backend.

import { ErrorApi } from "./http.js";

const LATENCIA_MS = 150;
const LIMITE_TARJETA_Y_PASARELA = 20000;

const TIPOS_ORDEN = ["SALA", "PARA_LLEVAR", "DELIVERY"];
const ESTADOS_ORDEN = ["ABIERTA", "CERRADA", "CANCELADA"];
const TIPOS_MODIFICADOR = ["EXTRA", "EXCLUSION"];
const METODOS_PAGO = ["EFECTIVO", "TARJETA", "PASARELA_DIGITAL"];
const PREFIJOS_REFERENCIA = { EFECTIVO: "EFE-", TARJETA: "TAR-", PASARELA_DIGITAL: "PAS-" };

// ---------------------------------------------------------------- datos iniciales

const platos = [
    { id: "P1", nombre: "Tacos al pastor", precioBase: "85.00", tasaImpuesto: "0.16" },
    { id: "P2", nombre: "Pozole", precioBase: "120.00", tasaImpuesto: "0.16" },
    { id: "P3", nombre: "Agua fresca", precioBase: "25.00", tasaImpuesto: "0" }
];

const modificadores = [
    { id: "M1", nombre: "Queso extra", tipo: "EXTRA", costoExtra: "10.00" },
    { id: "M2", nombre: "Sin cebolla", tipo: "EXCLUSION", costoExtra: "0.00" }
];

/** Órdenes internas; los montos se guardan como número y se convierten a string en los DTO. */
const ordenes = [];

(function crearOrdenDeEjemplo() {
    const orden = nuevaOrden("SALA", "Mesa 1");
    orden.lineas.push(nuevaLinea(platos[0], 2, [modificadores[0], modificadores[1]]));
    const pozole = nuevaLinea(platos[1], 1, []);
    pozole.estadoCocina = "EN_COCCION";
    orden.lineas.push(pozole);
    ordenes.push(orden);
})();

// ---------------------------------------------------------------- Menú

export function listarPlatos() {
    return responder(() => [...platos].sort(porNombre));
}

export function registrarPlato({ id, nombre, precioBase, tasaImpuesto }) {
    return responder(() => {
        exigirTexto(id, "El id del plato es obligatorio");
        exigirTexto(nombre, "El nombre del plato es obligatorio");
        const precio = aNumero(precioBase, "El precio es obligatorio");
        if (precio <= 0) {
            fallar("El precio debe ser mayor a cero");
        }
        exigirDosDecimales(precioBase, "El precio no puede tener más de dos decimales");
        const tasa = aNumero(tasaImpuesto, "La tasa de impuesto es obligatoria");
        if (tasa < 0 || tasa > 1) {
            fallar("La tasa de impuesto debe estar entre 0 y 1 (por ejemplo 0.16)");
        }
        if (platos.some((p) => p.id === id.trim())) {
            fallar(`Ya existe un plato con id ${id.trim()}`);
        }
        const plato = { id: id.trim(), nombre: nombre.trim(), precioBase: dinero(precio), tasaImpuesto: String(tasa) };
        platos.push(plato);
        return plato;
    });
}

export function listarModificadores() {
    return responder(() => [...modificadores].sort(porNombre));
}

export function registrarModificador({ id, nombre, tipo, costoExtra }) {
    return responder(() => {
        exigirTexto(id, "El id del modificador es obligatorio");
        exigirTexto(nombre, "El nombre del modificador es obligatorio");
        if (!TIPOS_MODIFICADOR.includes(tipo)) {
            fallar("El tipo del modificador es obligatorio (EXTRA o EXCLUSION)");
        }
        const costo = aNumero(costoExtra, "El costo extra es obligatorio (usa 0 si no tiene costo)");
        if (costo < 0) {
            fallar("El costo extra no puede ser negativo");
        }
        exigirDosDecimales(costoExtra, "El costo extra no puede tener más de dos decimales");
        if (tipo === "EXCLUSION" && costo !== 0) {
            fallar("Una exclusión no puede tener costo extra");
        }
        if (modificadores.some((m) => m.id === id.trim())) {
            fallar(`Ya existe un modificador con id ${id.trim()}`);
        }
        const modificador = { id: id.trim(), nombre: nombre.trim(), tipo, costoExtra: dinero(costo) };
        modificadores.push(modificador);
        return modificador;
    });
}

// ---------------------------------------------------------------- Órdenes

export function listarOrdenes(estado) {
    return responder(() => {
        if (estado && !ESTADOS_ORDEN.includes(estado)) {
            fallar(`Estado de orden no válido: ${estado}`);
        }
        return ordenes.filter((o) => !estado || o.estado === estado).map(aOrdenDto);
    });
}

export function obtenerOrden(ordenId) {
    return responder(() => aOrdenDto(buscarOrden(ordenId)));
}

export function abrirOrden({ tipo, referencia }) {
    return responder(() => {
        if (!tipo) {
            fallar("La orden necesita un tipo");
        }
        if (!TIPOS_ORDEN.includes(tipo)) {
            fallar(`Tipo de orden no válido: ${tipo}`);
        }
        exigirTexto(referencia, "La orden necesita una mesa o un identificador de pedido");
        const orden = nuevaOrden(tipo, referencia.trim());
        ordenes.push(orden);
        return aOrdenDto(orden);
    });
}

export function agregarLinea(ordenId, { platoId, cantidad, modificadorIds }) {
    return responder(() => {
        const orden = buscarOrden(ordenId);
        exigirAbierta(orden);
        const plato = platos.find((p) => p.id === platoId);
        if (!plato) {
            fallar(`No existe el plato ${platoId}`);
        }
        exigirCantidad(cantidad);
        orden.lineas.push(nuevaLinea(plato, cantidad, buscarModificadores(modificadorIds)));
        return aOrdenDto(orden);
    });
}

export function modificarLinea(ordenId, lineaId, { cantidad, modificadorIds }) {
    return responder(() => {
        const orden = buscarOrden(ordenId);
        exigirAbierta(orden);
        const linea = buscarLinea(orden, lineaId);
        exigirEditable(linea, "modificar");
        exigirCantidad(cantidad);
        linea.modificadores = buscarModificadores(modificadorIds).map(copiarModificador);
        linea.cantidad = cantidad;
        return aOrdenDto(orden);
    });
}

export function cancelarLinea(ordenId, lineaId) {
    return responder(() => {
        const orden = buscarOrden(ordenId);
        exigirAbierta(orden);
        const linea = buscarLinea(orden, lineaId);
        exigirEditable(linea, "cancelar");
        linea.estadoCocina = "CANCELADA";
        return aOrdenDto(orden);
    });
}

export function cancelarOrden(ordenId) {
    return responder(() => {
        const orden = buscarOrden(ordenId);
        exigirAbierta(orden);
        if (orden.pagos.some((p) => p.estado === "APROBADO")) {
            fallar("No se puede cancelar una orden con un pago aprobado");
        }
        const activas = lineasActivas(orden);
        if (activas.some((l) => l.estadoCocina !== "PENDIENTE")) {
            fallar("No se puede cancelar la orden: hay líneas en cocina o listas");
        }
        activas.forEach((l) => { l.estadoCocina = "CANCELADA"; });
        orden.estado = "CANCELADA";
        return aOrdenDto(orden);
    });
}

// ---------------------------------------------------------------- Cocina

export function listarPendientesCocina() {
    return responder(() => {
        const resultado = [];
        for (const orden of ordenes) {
            if (orden.estado !== "ABIERTA") {
                continue;
            }
            for (const linea of orden.lineas) {
                if (linea.estadoCocina === "PENDIENTE" || linea.estadoCocina === "EN_COCCION") {
                    resultado.push({
                        ordenId: orden.id,
                        lineaId: linea.id,
                        referenciaOrden: orden.referencia,
                        plato: linea.platoNombre,
                        cantidad: linea.cantidad,
                        modificadores: linea.modificadores.map((m) => m.nombre),
                        estado: linea.estadoCocina
                    });
                }
            }
        }
        return resultado;
    });
}

export function iniciarCoccion(ordenId, lineaId) {
    return responder(() => {
        const orden = buscarOrden(ordenId);
        exigirAbierta(orden);
        const linea = buscarLinea(orden, lineaId);
        if (linea.estadoCocina !== "PENDIENTE") {
            fallar(`Solo se puede iniciar la cocción de una línea PENDIENTE (estado actual: ${linea.estadoCocina})`);
        }
        linea.estadoCocina = "EN_COCCION";
        return null;
    });
}

export function marcarLista(ordenId, lineaId) {
    return responder(() => {
        const orden = buscarOrden(ordenId);
        exigirAbierta(orden);
        const linea = buscarLinea(orden, lineaId);
        if (linea.estadoCocina !== "EN_COCCION") {
            fallar(`Solo se puede marcar como lista una línea EN_COCCION (estado actual: ${linea.estadoCocina})`);
        }
        linea.estadoCocina = "LISTO";
        return null;
    });
}

// ---------------------------------------------------------------- Caja

export function calcularTotales(ordenId, { descuento, propina }) {
    return responder(() => aTotalesDto(calcular(buscarOrden(ordenId), descuento, propina)));
}

export function cobrar(ordenId, { metodo, descuento, propina }) {
    return responder(() => {
        if (!metodo) {
            fallar("El método de pago es obligatorio");
        }
        if (!METODOS_PAGO.includes(metodo)) {
            fallar(`Método de pago no válido: ${metodo}`);
        }
        const orden = buscarOrden(ordenId);
        exigirAbierta(orden);
        if (lineasActivas(orden).length === 0) {
            fallar("No se puede cobrar una orden sin líneas");
        }
        if (orden.pagos.some((p) => p.estado === "APROBADO")) {
            fallar("La orden ya tiene un pago aprobado");
        }
        const totales = calcular(orden, descuento, propina);
        const rechazado = metodo !== "EFECTIVO" && totales.total > LIMITE_TARJETA_Y_PASARELA;
        const pago = {
            id: nuevoId(),
            ordenId: orden.id,
            fecha: fechaLocal(),
            estado: rechazado ? "RECHAZADO" : "APROBADO",
            metodo,
            monto: dinero(totales.total),
            referencia: PREFIJOS_REFERENCIA[metodo] + nuevoId().slice(0, 8)
        };
        orden.pagos.push(pago);
        if (!rechazado) {
            orden.totalesCerrados = totales;
            orden.estado = "CERRADA";
        }
        return pago;
    });
}

// ---------------------------------------------------------------- reglas y cálculos

function calcular(orden, descuentoTexto, propinaTexto) {
    const descuento = redondear(aNumero(descuentoTexto ?? "0", "El descuento debe ser un número"));
    const propina = redondear(aNumero(propinaTexto ?? "0", "La propina debe ser un número"));
    if (descuento < 0) {
        fallar("El descuento no puede ser negativo");
    }
    if (propina < 0) {
        fallar("La propina no puede ser negativa");
    }
    const activas = lineasActivas(orden);
    const importes = activas.map((l) => redondear(importe(l)));
    const subtotal = redondear(importes.reduce((suma, valor) => suma + valor, 0));
    if (descuento > subtotal) {
        fallar("El descuento no puede ser mayor al subtotal");
    }
    const base = subtotal - descuento;
    let impuestos = 0;
    if (subtotal > 0) {
        activas.forEach((linea, i) => {
            impuestos += redondear(importes[i] * linea.tasaImpuesto * base / subtotal);
        });
    }
    impuestos = redondear(impuestos);
    const total = redondear(base + impuestos + propina);
    return { subtotal, descuento, impuestos, propina, total };
}

function importe(linea) {
    const unitario = linea.precioUnitario + linea.modificadores.reduce((suma, m) => suma + m.costoExtra, 0);
    return unitario * linea.cantidad;
}

function lineasActivas(orden) {
    return orden.lineas.filter((l) => l.estadoCocina !== "CANCELADA");
}

function buscarOrden(ordenId) {
    const orden = ordenes.find((o) => o.id === ordenId);
    if (!orden) {
        fallar(`No existe la orden ${ordenId}`);
    }
    return orden;
}

function buscarLinea(orden, lineaId) {
    const linea = orden.lineas.find((l) => l.id === lineaId);
    if (!linea) {
        fallar(`No existe la línea ${lineaId}`);
    }
    return linea;
}

function buscarModificadores(ids) {
    return (ids ?? []).map((id) => {
        const modificador = modificadores.find((m) => m.id === id);
        if (!modificador) {
            fallar(`No existe el modificador ${id}`);
        }
        return modificador;
    });
}

function exigirAbierta(orden) {
    if (orden.estado !== "ABIERTA") {
        fallar(`La orden no está abierta (estado: ${orden.estado})`);
    }
}

function exigirEditable(linea, accion) {
    if (linea.estadoCocina !== "PENDIENTE") {
        fallar(`No se puede ${accion} la línea: solo se permite mientras está PENDIENTE (estado actual: ${linea.estadoCocina})`);
    }
}

function exigirCantidad(cantidad) {
    if (!Number.isInteger(cantidad) || cantidad <= 0) {
        fallar("La cantidad debe ser mayor a cero");
    }
}

function exigirTexto(valor, mensaje) {
    if (typeof valor !== "string" || valor.trim() === "") {
        fallar(mensaje);
    }
}

function exigirDosDecimales(texto, mensaje) {
    const decimales = String(texto).trim().split(".")[1] ?? "";
    if (decimales.replace(/0+$/, "").length > 2) {
        fallar(mensaje);
    }
}

function aNumero(texto, mensaje) {
    const limpio = String(texto ?? "").trim();
    const numero = Number(limpio);
    if (limpio === "" || !Number.isFinite(numero)) {
        fallar(mensaje);
    }
    return numero;
}

function fallar(mensaje) {
    throw new ErrorApi(400, mensaje);
}

// ---------------------------------------------------------------- construcción y DTO

function nuevaOrden(tipo, referencia) {
    return { id: nuevoId(), tipo, referencia, estado: "ABIERTA", fechaCreacion: fechaLocal(), lineas: [], pagos: [], totalesCerrados: null };
}

function nuevaLinea(plato, cantidad, mods) {
    return {
        id: nuevoId(),
        platoId: plato.id,
        platoNombre: plato.nombre,
        cantidad,
        precioUnitario: Number(plato.precioBase),
        tasaImpuesto: Number(plato.tasaImpuesto),
        modificadores: mods.map(copiarModificador),
        estadoCocina: "PENDIENTE"
    };
}

function copiarModificador(m) {
    return { id: m.id, nombre: m.nombre, tipo: m.tipo, costoExtra: Number(m.costoExtra) };
}

function aOrdenDto(orden) {
    return {
        id: orden.id,
        tipo: orden.tipo,
        referencia: orden.referencia,
        estado: orden.estado,
        fechaCreacion: orden.fechaCreacion,
        lineas: orden.lineas.map((l) => ({
            id: l.id,
            platoId: l.platoId,
            platoNombre: l.platoNombre,
            cantidad: l.cantidad,
            precioUnitario: dinero(l.precioUnitario),
            tasaImpuesto: String(l.tasaImpuesto),
            modificadores: l.modificadores.map((m) => ({ id: m.id, nombre: m.nombre, tipo: m.tipo, costoExtra: dinero(m.costoExtra) })),
            estadoCocina: l.estadoCocina,
            editable: orden.estado === "ABIERTA" && l.estadoCocina === "PENDIENTE"
        })),
        totales: aTotalesDto(orden.totalesCerrados ?? calcular(orden, "0", "0")),
        pagos: orden.pagos.map((p) => ({ ...p }))
    };
}

function aTotalesDto(t) {
    return {
        subtotal: dinero(t.subtotal),
        descuento: dinero(t.descuento),
        impuestos: dinero(t.impuestos),
        propina: dinero(t.propina),
        total: dinero(t.total)
    };
}

// ---------------------------------------------------------------- utilidades

/** Redondeo a 2 decimales "hacia arriba en el 5" (HALF_UP), tolerante a errores de coma flotante. */
function redondear(valor) {
    return Math.sign(valor) * Math.round(Math.abs(valor) * 100 + 1e-7) / 100;
}

function dinero(valor) {
    return redondear(valor).toFixed(2);
}

function porNombre(a, b) {
    return a.nombre.localeCompare(b.nombre, "es");
}

function nuevoId() {
    if (globalThis.crypto?.randomUUID) {
        return crypto.randomUUID();
    }
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
        const r = Math.floor(Math.random() * 16);
        return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
    });
}

function fechaLocal() {
    const d = new Date();
    const dos = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}T${dos(d.getHours())}:${dos(d.getMinutes())}:${dos(d.getSeconds())}`;
}

/** Ejecuta la operación tras la latencia simulada y devuelve una copia, como si viniera por la red. */
function responder(operacion) {
    return new Promise((resolver, rechazar) => {
        setTimeout(() => {
            try {
                const resultado = operacion();
                resolver(resultado === null ? null : structuredClone(resultado));
            } catch (error) {
                rechazar(error instanceof ErrorApi ? error : new ErrorApi(500, "Error interno del simulador"));
            }
        }, LATENCIA_MS);
    });
}
