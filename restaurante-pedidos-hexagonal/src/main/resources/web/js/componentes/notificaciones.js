// Avisos breves (toasts) en la esquina inferior derecha. Desaparecen solos a los 4 s.

import { crear } from "../util/dom.js";

const DURACION_MS = 4000;
const MAXIMO_VISIBLES = 4;

export function mostrarExito(mensaje) {
    mostrar(mensaje, "exito");
}

/** Acepta un ErrorApi (usa su .message), cualquier Error o un texto. */
export function mostrarError(errorOMensaje) {
    const mensaje = errorOMensaje instanceof Error ? errorOMensaje.message : String(errorOMensaje ?? "Ocurrió un error");
    if (errorOMensaje instanceof Error && errorOMensaje.name !== "ErrorApi") {
        console.error(errorOMensaje);
    }
    mostrar(mensaje, "error");
}

function mostrar(mensaje, tipo) {
    const contenedor = document.getElementById("notificaciones");
    if (!contenedor) {
        return;
    }
    const aviso = crear("div", { class: `notificacion notificacion--${tipo}`, role: tipo === "error" ? "alert" : "status" },
        crear("span", { class: "notificacion__texto" }, mensaje),
        crear("button", { type: "button", class: "notificacion__cerrar", "aria-label": "Cerrar aviso", onclick: () => quitar() }, "×"));

    let temporizador = setTimeout(quitar, DURACION_MS);
    function quitar() {
        clearTimeout(temporizador);
        temporizador = null;
        aviso.remove();
    }
    contenedor.appendChild(aviso);

    // Si se acumulan muchos avisos, se quitan los más viejos para no tapar la pantalla.
    while (contenedor.children.length > MAXIMO_VISIBLES) {
        contenedor.firstElementChild.remove();
    }
}
