// Formato de dinero, fechas, porcentajes y etiquetas de los estados.

const FORMATO_DINERO = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });

/** "85.5" → "$85.50". Solo convierte a número para mostrar; no hace cálculos. */
export function formatoDinero(valor) {
    const numero = Number(valor);
    return FORMATO_DINERO.format(Number.isFinite(numero) ? numero : 0);
}

/** "2026-10-20T13:45:00" → "20/10/2026 13:45". */
export function formatoFecha(isoLocal) {
    if (!isoLocal) {
        return "";
    }
    const coincide = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(isoLocal);
    if (!coincide) {
        return String(isoLocal);
    }
    const [, anio, mes, dia, hora, minuto] = coincide;
    return `${dia}/${mes}/${anio} ${hora}:${minuto}`;
}

/** "0.16" → "16 %". */
export function formatoPorcentaje(fraccion) {
    const numero = Number(fraccion);
    if (!Number.isFinite(numero)) {
        return "";
    }
    const porcentaje = Math.round(numero * 10000) / 100;
    return `${porcentaje} %`;
}

/**
 * Prepara un monto escrito por el usuario para mandarlo al backend como string.
 * Acepta coma como separador decimal; un campo vacío se manda como "0".
 */
export function normalizarMonto(texto) {
    const limpio = String(texto ?? "").trim().replace(",", ".");
    return limpio === "" ? "0" : limpio;
}

/** Hora actual como "hh:mm:ss". */
export function horaActual() {
    return new Date().toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false });
}

export const ETIQUETAS = {
    PENDIENTE: "Pendiente", EN_COCCION: "En cocción", LISTO: "Listo", CANCELADA: "Cancelada",
    ABIERTA: "Abierta", CERRADA: "Cerrada", APROBADO: "Aprobado", RECHAZADO: "Rechazado",
    SALA: "Sala", PARA_LLEVAR: "Para llevar", DELIVERY: "Delivery",
    EFECTIVO: "Efectivo", TARJETA: "Tarjeta", PASARELA_DIGITAL: "Pasarela digital",
    EXTRA: "Extra", EXCLUSION: "Exclusión"
};

/** Etiqueta en español de un enum; si no se conoce, devuelve el valor tal cual. */
export function etiqueta(valor) {
    return ETIQUETAS[valor] ?? valor;
}
