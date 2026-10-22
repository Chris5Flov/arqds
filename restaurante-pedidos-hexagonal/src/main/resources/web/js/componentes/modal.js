// Diálogo modal. Se cierra con Esc o con clic fuera del diálogo.
// Se usa también para las confirmaciones (no se usan alert, confirm ni prompt).

import { crear, limpiar } from "../util/dom.js";

let focoAnterior = null;
let alPresionarTecla = null;

/**
 * acciones: [{ texto, tipo: "primario" | "peligro" | "secundario", alHacerClic }]
 * - Una acción sin alHacerClic solo cierra el modal.
 * - Si alHacerClic devuelve una promesa, los botones se deshabilitan mientras se resuelve.
 *   La acción decide si cierra el modal (llamando a cerrarModal).
 * - Si el contenido tiene un formulario, Enter ejecuta la primera acción primaria o de peligro.
 */
export function abrirModal({ titulo, contenido, acciones = [] }) {
    cerrarModal();
    const raiz = document.getElementById("modal-raiz");
    focoAnterior = document.activeElement;

    const botones = acciones.map((accion) => crear("button", {
        type: "button",
        class: `boton boton--${accion.tipo ?? "secundario"}`,
        onclick: () => ejecutar(accion)
    }, accion.texto));

    const dialogo = crear("div", { class: "modal", role: "dialog", "aria-modal": "true", "aria-labelledby": "modal-titulo" },
        crear("h2", { class: "modal__titulo", id: "modal-titulo" }, titulo),
        crear("div", { class: "modal__contenido" }, contenido ?? null),
        botones.length ? crear("div", { class: "modal__acciones" }, botones) : null);

    const fondo = crear("div", {
        class: "modal-fondo",
        onclick: (evento) => { if (evento.target === fondo) cerrarModal(); }
    }, dialogo);

    const principal = acciones.find((a) => a.tipo === "primario" || a.tipo === "peligro");
    dialogo.addEventListener("submit", (evento) => {
        evento.preventDefault();
        if (principal) {
            ejecutar(principal);
        }
    });
    // Un formulario con varios campos de texto y sin botón submit no se envía solo con Enter.
    dialogo.addEventListener("keydown", (evento) => {
        const esCampo = evento.target.matches?.("input:not([type=checkbox]):not([type=radio]), select");
        if (evento.key === "Enter" && esCampo && principal) {
            evento.preventDefault();
            if (!botones.some((b) => b.disabled)) {
                ejecutar(principal);
            }
        }
    });

    alPresionarTecla = (evento) => {
        if (evento.key === "Escape") {
            cerrarModal();
        }
    };
    document.addEventListener("keydown", alPresionarTecla);

    raiz.appendChild(fondo);
    const primerCampo = dialogo.querySelector("input, select, textarea") ?? botones[botones.length - 1];
    primerCampo?.focus();

    async function ejecutar(accion) {
        if (!accion.alHacerClic) {
            cerrarModal();
            return;
        }
        botones.forEach((b) => { b.disabled = true; });
        try {
            await accion.alHacerClic();
        } finally {
            botones.forEach((b) => { b.disabled = false; });
        }
    }
}

export function cerrarModal() {
    const raiz = document.getElementById("modal-raiz");
    if (!raiz || !raiz.firstChild) {
        return;
    }
    limpiar(raiz);
    if (alPresionarTecla) {
        document.removeEventListener("keydown", alPresionarTecla);
        alPresionarTecla = null;
    }
    if (focoAnterior && document.contains(focoAnterior)) {
        focoAnterior.focus();
    }
    focoAnterior = null;
}

/** Atajo para pedir confirmación: ejecuta alConfirmar y cierra el modal si termina bien. */
export function confirmar({ titulo, mensaje, textoConfirmar = "Confirmar", tipo = "peligro", alConfirmar }) {
    abrirModal({
        titulo,
        contenido: crear("p", {}, mensaje),
        acciones: [
            { texto: "Volver", tipo: "secundario" },
            {
                texto: textoConfirmar,
                tipo,
                alHacerClic: async () => {
                    const exito = await alConfirmar();
                    if (exito !== false) {
                        cerrarModal();
                    }
                }
            }
        ]
    });
}
