// Insignias de colores para los estados (de cocina, de orden, de pago) y los tipos.

import { crear } from "../util/dom.js";
import { etiqueta } from "../util/formato.js";

/**
 * Devuelve <span class="insignia insignia--{estado en minúsculas}">Etiqueta</span>.
 * contexto "pago" distingue el PENDIENTE de un pago (amarillo) del PENDIENTE de cocina (gris).
 */
export function insigniaEstado(estado, contexto) {
    const clase = String(estado ?? "").toLowerCase();
    const extra = contexto === "pago" ? ` insignia--pago-${clase}` : "";
    return crear("span", { class: `insignia insignia--${clase}${extra}` }, etiqueta(estado));
}
