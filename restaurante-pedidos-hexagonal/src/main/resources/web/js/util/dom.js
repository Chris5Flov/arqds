// Utilidades para crear y limpiar elementos del DOM sin usar innerHTML.

const PROPIEDADES = new Set(["value", "checked", "disabled", "selected"]);

/**
 * Crea un elemento.
 * atributos: class, id, type, value, disabled, dataset, on* (p. ej. onclick) y cualquier otro
 * atributo HTML (for, name, placeholder, aria-*, ...). Los valores null, undefined o false se omiten.
 * hijos: string | Node | arreglo (se aplanan; null, undefined y false se ignoran).
 */
export function crear(etiqueta, atributos = {}, ...hijos) {
    const elemento = document.createElement(etiqueta);

    for (const [nombre, valor] of Object.entries(atributos ?? {})) {
        if (valor === null || valor === undefined) {
            continue;
        }
        if (nombre.startsWith("on") && typeof valor === "function") {
            elemento.addEventListener(nombre.slice(2).toLowerCase(), valor);
        } else if (nombre === "class") {
            elemento.className = valor;
        } else if (nombre === "dataset") {
            Object.assign(elemento.dataset, valor);
        } else if (PROPIEDADES.has(nombre)) {
            elemento[nombre] = valor;
        } else if (valor === true) {
            elemento.setAttribute(nombre, "");
        } else if (valor !== false) {
            elemento.setAttribute(nombre, String(valor));
        }
    }

    agregarHijos(elemento, hijos);
    return elemento;
}

function agregarHijos(elemento, hijos) {
    for (const hijo of hijos) {
        if (hijo === null || hijo === undefined || hijo === false) {
            continue;
        }
        if (Array.isArray(hijo)) {
            agregarHijos(elemento, hijo);
        } else if (hijo instanceof Node) {
            elemento.appendChild(hijo);
        } else {
            elemento.appendChild(document.createTextNode(String(hijo)));
        }
    }
}

/** Quita todos los hijos de un elemento. Devuelve el mismo elemento. */
export function limpiar(elemento) {
    while (elemento.firstChild) {
        elemento.removeChild(elemento.firstChild);
    }
    return elemento;
}

export function $(selector, raiz = document) {
    return raiz.querySelector(selector);
}

/**
 * Campo de formulario con su <label>. El control debe tener id.
 * ayuda: texto pequeño opcional debajo del control.
 */
export function campo(textoEtiqueta, control, ayuda) {
    const idAyuda = ayuda ? `${control.id}-ayuda` : null;
    if (idAyuda) {
        control.setAttribute("aria-describedby", idAyuda);
    }
    return crear("div", { class: "campo" },
        crear("label", { class: "campo__etiqueta", for: control.id }, textoEtiqueta),
        control,
        ayuda ? crear("span", { class: "campo__ayuda", id: idAyuda }, ayuda) : null);
}

/** Desplaza la vista hasta el elemento en pantallas angostas, donde el detalle queda debajo de la lista. */
export function mostrarEnPantallaAngosta(elemento) {
    if (window.matchMedia("(max-width: 800px)").matches) {
        elemento.scrollIntoView({ behavior: "smooth", block: "start" });
    }
}

/**
 * Deshabilita un botón mientras se espera una promesa, para evitar dobles envíos.
 * Devuelve el resultado de la promesa (o vuelve a lanzar su error).
 */
export async function mientrasEspera(boton, accion) {
    if (boton) {
        boton.disabled = true;
        boton.setAttribute("aria-busy", "true");
    }
    try {
        return await accion();
    } finally {
        if (boton) {
            boton.disabled = false;
            boton.removeAttribute("aria-busy");
        }
    }
}
