// Arranque de la aplicación y enrutador por hash (#/menu, #/ordenes, #/cocina, #/caja).

import { usandoSimulador } from "./api/api.js";
import { cerrarModal } from "./componentes/modal.js";
import { limpiar } from "./util/dom.js";
import * as vistaMenu from "./vistas/vista-menu.js";
import * as vistaOrdenes from "./vistas/vista-ordenes.js";
import * as vistaCocina from "./vistas/vista-cocina.js";
import * as vistaCaja from "./vistas/vista-caja.js";

const RUTAS = {
    menu: vistaMenu,
    ordenes: vistaOrdenes,
    cocina: vistaCocina,
    caja: vistaCaja
};
const RUTA_POR_DEFECTO = "ordenes";

let desmontarActual = null;

function rutaActual() {
    const nombre = location.hash.replace(/^#\/?/, "");
    return RUTAS[nombre] ? nombre : null;
}

function navegar() {
    const nombre = rutaActual();
    if (!nombre) {
        location.replace(`#/${RUTA_POR_DEFECTO}`);
        return;
    }

    if (typeof desmontarActual === "function") {
        desmontarActual();
    }
    desmontarActual = null;
    cerrarModal();

    const contenedor = limpiar(document.getElementById("contenido"));
    desmontarActual = RUTAS[nombre].montar(contenedor) ?? null;

    for (const pestana of document.querySelectorAll(".pestana")) {
        const activa = pestana.dataset.ruta === nombre;
        pestana.classList.toggle("pestana--activa", activa);
        if (activa) {
            pestana.setAttribute("aria-current", "page");
        } else {
            pestana.removeAttribute("aria-current");
        }
    }
}

document.getElementById("etiqueta-simulador").hidden = !usandoSimulador;
window.addEventListener("hashchange", navegar);
navegar();
