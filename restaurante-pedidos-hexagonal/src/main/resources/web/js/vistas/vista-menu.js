// Vista Menú (Administrador, RF1): registrar y consultar platos y modificadores.

import { listarPlatos, registrarPlato, listarModificadores, registrarModificador } from "../api/api.js";
import { mostrarExito, mostrarError } from "../componentes/notificaciones.js";
import { insigniaEstado } from "../componentes/insignia.js";
import { crear, limpiar, campo, mientrasEspera } from "../util/dom.js";
import { formatoDinero, formatoPorcentaje, normalizarMonto } from "../util/formato.js";

export function montar(contenedor) {
    let activa = true;

    // ------------------------------------------------------------ platos
    const cuerpoPlatos = crear("tbody");
    const idPlato = crear("input", { type: "text", id: "plato-id", autocomplete: "off", placeholder: "P-001" });
    const nombrePlato = crear("input", { type: "text", id: "plato-nombre", autocomplete: "off", placeholder: "Tacos al pastor" });
    const precioPlato = crear("input", { type: "text", id: "plato-precio", inputmode: "decimal", autocomplete: "off", placeholder: "85.50" });
    const tasaPlato = crear("input", { type: "text", id: "plato-tasa", inputmode: "decimal", autocomplete: "off", placeholder: "0.16" });
    const botonPlato = crear("button", { type: "submit", class: "boton boton--primario" }, "Guardar plato");

    const formularioPlato = crear("form", { class: "formulario", onsubmit: guardarPlato },
        crear("h3", { class: "tarjeta__subtitulo" }, "Registrar plato"),
        crear("div", { class: "formulario__campos" },
            campo("ID", idPlato),
            campo("Nombre", nombrePlato),
            campo("Precio base", precioPlato),
            campo("Tasa de impuesto", tasaPlato, "fracción: 0.16 = 16 %")),
        crear("div", { class: "fila" }, botonPlato));

    // ------------------------------------------------------------ modificadores
    const cuerpoModificadores = crear("tbody");
    const idModificador = crear("input", { type: "text", id: "modificador-id", autocomplete: "off", placeholder: "M-001" });
    const nombreModificador = crear("input", { type: "text", id: "modificador-nombre", autocomplete: "off", placeholder: "Queso extra" });
    const tipoModificador = crear("select", { id: "modificador-tipo", onchange: actualizarCosto },
        crear("option", { value: "EXTRA" }, "Extra"),
        crear("option", { value: "EXCLUSION" }, "Exclusión"));
    const costoModificador = crear("input", { type: "text", id: "modificador-costo", inputmode: "decimal", autocomplete: "off", placeholder: "10.00" });
    const botonModificador = crear("button", { type: "submit", class: "boton boton--primario" }, "Guardar modificador");

    const formularioModificador = crear("form", { class: "formulario", onsubmit: guardarModificador },
        crear("h3", { class: "tarjeta__subtitulo" }, "Registrar modificador"),
        crear("div", { class: "formulario__campos" },
            campo("ID", idModificador),
            campo("Nombre", nombreModificador),
            campo("Tipo", tipoModificador),
            campo("Costo extra", costoModificador, "Una exclusión no tiene costo")),
        crear("div", { class: "fila" }, botonModificador));

    contenedor.appendChild(crear("section", { class: "vista" },
        crear("div", { class: "vista__encabezado" }, crear("h2", { class: "vista__titulo" }, "Menú")),
        crear("div", { class: "columnas" },
            crear("section", { class: "tarjeta" },
                crear("h2", { class: "tarjeta__titulo" }, "Platos"),
                tabla(["ID", "Nombre", "Precio", "Impuesto"], [2, 3], cuerpoPlatos),
                crear("hr", { class: "separador" }),
                formularioPlato),
            crear("section", { class: "tarjeta" },
                crear("h2", { class: "tarjeta__titulo" }, "Modificadores"),
                tabla(["ID", "Nombre", "Tipo", "Costo extra"], [3], cuerpoModificadores),
                crear("hr", { class: "separador" }),
                formularioModificador))));

    cargarPlatos();
    cargarModificadores();

    return () => { activa = false; };

    // ------------------------------------------------------------ acciones

    async function cargarPlatos() {
        filaMensaje(cuerpoPlatos, 4, "Cargando platos…", "cargando");
        try {
            const platos = await listarPlatos();
            if (!activa) return;
            limpiar(cuerpoPlatos);
            if (platos.length === 0) {
                filaMensaje(cuerpoPlatos, 4, "Aún no hay platos registrados.");
                return;
            }
            for (const plato of platos) {
                cuerpoPlatos.appendChild(crear("tr", {},
                    crear("td", {}, plato.id),
                    crear("td", {}, plato.nombre),
                    crear("td", { class: "numero" }, formatoDinero(plato.precioBase)),
                    crear("td", { class: "numero" }, formatoPorcentaje(plato.tasaImpuesto))));
            }
        } catch (error) {
            if (!activa) return;
            filaMensaje(cuerpoPlatos, 4, "No se pudieron cargar los platos.");
            mostrarError(error);
        }
    }

    async function cargarModificadores() {
        filaMensaje(cuerpoModificadores, 4, "Cargando modificadores…", "cargando");
        try {
            const modificadores = await listarModificadores();
            if (!activa) return;
            limpiar(cuerpoModificadores);
            if (modificadores.length === 0) {
                filaMensaje(cuerpoModificadores, 4, "Aún no hay modificadores registrados.");
                return;
            }
            for (const modificador of modificadores) {
                cuerpoModificadores.appendChild(crear("tr", {},
                    crear("td", {}, modificador.id),
                    crear("td", {}, modificador.nombre),
                    crear("td", {}, insigniaEstado(modificador.tipo)),
                    crear("td", { class: "numero" }, formatoDinero(modificador.costoExtra))));
            }
        } catch (error) {
            if (!activa) return;
            filaMensaje(cuerpoModificadores, 4, "No se pudieron cargar los modificadores.");
            mostrarError(error);
        }
    }

    function guardarPlato(evento) {
        evento.preventDefault();
        const nombre = nombrePlato.value.trim();
        mientrasEspera(botonPlato, async () => {
            try {
                await registrarPlato({
                    id: idPlato.value.trim(),
                    nombre,
                    precioBase: normalizarMonto(precioPlato.value),
                    tasaImpuesto: normalizarMonto(tasaPlato.value)
                });
                mostrarExito(`Plato "${nombre}" registrado`);
                formularioPlato.reset();
                idPlato.focus();
                await cargarPlatos();
            } catch (error) {
                mostrarError(error);
            }
        });
    }

    function guardarModificador(evento) {
        evento.preventDefault();
        const nombre = nombreModificador.value.trim();
        const tipo = tipoModificador.value;
        mientrasEspera(botonModificador, async () => {
            try {
                await registrarModificador({
                    id: idModificador.value.trim(),
                    nombre,
                    tipo,
                    costoExtra: tipo === "EXCLUSION" ? "0" : normalizarMonto(costoModificador.value)
                });
                mostrarExito(`Modificador "${nombre}" registrado`);
                formularioModificador.reset();
                actualizarCosto();
                idModificador.focus();
                await cargarModificadores();
            } catch (error) {
                mostrarError(error);
            }
        });
    }

    /** Si el tipo es EXCLUSION, el costo se deshabilita y se fija en "0". */
    function actualizarCosto() {
        const esExclusion = tipoModificador.value === "EXCLUSION";
        if (esExclusion) {
            costoModificador.value = "0";
        } else if (costoModificador.disabled) {
            costoModificador.value = "";
        }
        costoModificador.disabled = esExclusion;
    }
}

function tabla(encabezados, columnasNumericas, cuerpo) {
    return crear("div", { class: "tabla-contenedor" },
        crear("table", { class: "tabla" },
            crear("thead", {}, crear("tr", {},
                encabezados.map((texto, i) => crear("th", { scope: "col", class: columnasNumericas.includes(i) ? "numero" : null }, texto)))),
            cuerpo));
}

function filaMensaje(cuerpo, columnas, mensaje, clase = "texto-secundario") {
    limpiar(cuerpo).appendChild(crear("tr", {}, crear("td", { colspan: columnas, class: clase }, mensaje)));
}
