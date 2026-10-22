// Vista Menú (Administrador, RF1): registrar, consultar, editar y eliminar platos y modificadores.

import {
    listarPlatos, registrarPlato, actualizarPlato, eliminarPlato,
    listarModificadores, registrarModificador, actualizarModificador, eliminarModificador
} from "../api/api.js";
import { mostrarExito, mostrarError } from "../componentes/notificaciones.js";
import { abrirModal, cerrarModal, confirmar } from "../componentes/modal.js";
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
                tabla(["ID", "Nombre", "Precio", "Impuesto", "Acciones"], [2, 3], cuerpoPlatos),
                crear("hr", { class: "separador" }),
                formularioPlato),
            crear("section", { class: "tarjeta" },
                crear("h2", { class: "tarjeta__titulo" }, "Modificadores"),
                tabla(["ID", "Nombre", "Tipo", "Costo extra", "Acciones"], [3], cuerpoModificadores),
                crear("hr", { class: "separador" }),
                formularioModificador))));

    cargarPlatos();
    cargarModificadores();

    return () => { activa = false; };

    // ------------------------------------------------------------ acciones

    async function cargarPlatos() {
        if (!cuerpoPlatos.firstChild) {
            filaMensaje(cuerpoPlatos, 5, "Cargando platos…", "cargando");
        }
        try {
            const platos = await listarPlatos();
            if (!activa) return;
            limpiar(cuerpoPlatos);
            if (platos.length === 0) {
                filaMensaje(cuerpoPlatos, 5, "Aún no hay platos registrados.");
                return;
            }
            for (const plato of platos) {
                cuerpoPlatos.appendChild(crear("tr", {},
                    crear("td", {}, plato.id),
                    crear("td", {}, plato.nombre),
                    crear("td", { class: "numero" }, formatoDinero(plato.precioBase)),
                    crear("td", { class: "numero" }, formatoPorcentaje(plato.tasaImpuesto)),
                    crear("td", { class: "acciones" }, botonesFila(() => editarPlato(plato), () => pedirEliminarPlato(plato)))));
            }
        } catch (error) {
            if (!activa) return;
            filaMensaje(cuerpoPlatos, 5, "No se pudieron cargar los platos.");
            mostrarError(error);
        }
    }

    async function cargarModificadores() {
        if (!cuerpoModificadores.firstChild) {
            filaMensaje(cuerpoModificadores, 5, "Cargando modificadores…", "cargando");
        }
        try {
            const modificadores = await listarModificadores();
            if (!activa) return;
            limpiar(cuerpoModificadores);
            if (modificadores.length === 0) {
                filaMensaje(cuerpoModificadores, 5, "Aún no hay modificadores registrados.");
                return;
            }
            for (const modificador of modificadores) {
                cuerpoModificadores.appendChild(crear("tr", {},
                    crear("td", {}, modificador.id),
                    crear("td", {}, modificador.nombre),
                    crear("td", {}, insigniaEstado(modificador.tipo)),
                    crear("td", { class: "numero" }, formatoDinero(modificador.costoExtra)),
                    crear("td", { class: "acciones" },
                        botonesFila(() => editarModificador(modificador), () => pedirEliminarModificador(modificador)))));
            }
        } catch (error) {
            if (!activa) return;
            filaMensaje(cuerpoModificadores, 5, "No se pudieron cargar los modificadores.");
            mostrarError(error);
        }
    }

    // ------------------------------------------------------------ editar y eliminar

    function editarPlato(plato) {
        const nombre = crear("input", { type: "text", id: "editar-plato-nombre", autocomplete: "off", value: plato.nombre });
        const precio = crear("input", { type: "text", id: "editar-plato-precio", inputmode: "decimal", autocomplete: "off", value: plato.precioBase });
        const tasa = crear("input", { type: "text", id: "editar-plato-tasa", inputmode: "decimal", autocomplete: "off", value: plato.tasaImpuesto });

        abrirModal({
            titulo: `Editar plato ${plato.id}`,
            contenido: crear("form", { class: "formulario" },
                campo("Nombre", nombre),
                crear("div", { class: "formulario__campos" },
                    campo("Precio base", precio),
                    campo("Tasa de impuesto", tasa, "fracción: 0.16 = 16 %")),
                crear("p", { class: "texto-secundario" }, "Las órdenes que ya tienen este plato conservan su precio.")),
            acciones: [
                { texto: "Volver", tipo: "secundario" },
                {
                    texto: "Guardar cambios",
                    tipo: "primario",
                    alHacerClic: async () => {
                        try {
                            const actualizado = await actualizarPlato(plato.id, {
                                nombre: nombre.value.trim(),
                                precioBase: normalizarMonto(precio.value),
                                tasaImpuesto: normalizarMonto(tasa.value)
                            });
                            cerrarModal();
                            mostrarExito(`Plato "${actualizado.nombre}" actualizado`);
                            await cargarPlatos();
                        } catch (error) {
                            mostrarError(error);
                        }
                    }
                }
            ]
        });
    }

    function pedirEliminarPlato(plato) {
        confirmar({
            titulo: "Eliminar plato",
            mensaje: `¿Eliminar "${plato.nombre}" del menú? Ya no se podrá agregar a órdenes nuevas; las órdenes que ya lo tienen no cambian.`,
            textoConfirmar: "Eliminar",
            alConfirmar: async () => {
                try {
                    await eliminarPlato(plato.id);
                    mostrarExito(`Plato "${plato.nombre}" eliminado`);
                    cargarPlatos();
                } catch (error) {
                    mostrarError(error);
                    return false;
                }
            }
        });
    }

    function editarModificador(modificador) {
        const nombre = crear("input", { type: "text", id: "editar-mod-nombre", autocomplete: "off", value: modificador.nombre });
        const costo = crear("input", { type: "text", id: "editar-mod-costo", inputmode: "decimal", autocomplete: "off", value: modificador.costoExtra });
        const tipo = crear("select", { id: "editar-mod-tipo", onchange: () => fijarCostoSegunTipo(tipo, costo) },
            crear("option", { value: "EXTRA", selected: modificador.tipo === "EXTRA" }, "Extra"),
            crear("option", { value: "EXCLUSION", selected: modificador.tipo === "EXCLUSION" }, "Exclusión"));
        fijarCostoSegunTipo(tipo, costo);

        abrirModal({
            titulo: `Editar modificador ${modificador.id}`,
            contenido: crear("form", { class: "formulario" },
                campo("Nombre", nombre),
                crear("div", { class: "formulario__campos" },
                    campo("Tipo", tipo),
                    campo("Costo extra", costo, "Una exclusión no tiene costo")),
                crear("p", { class: "texto-secundario" }, "Las órdenes que ya lo tienen conservan su costo.")),
            acciones: [
                { texto: "Volver", tipo: "secundario" },
                {
                    texto: "Guardar cambios",
                    tipo: "primario",
                    alHacerClic: async () => {
                        try {
                            const actualizado = await actualizarModificador(modificador.id, {
                                nombre: nombre.value.trim(),
                                tipo: tipo.value,
                                costoExtra: tipo.value === "EXCLUSION" ? "0" : normalizarMonto(costo.value)
                            });
                            cerrarModal();
                            mostrarExito(`Modificador "${actualizado.nombre}" actualizado`);
                            await cargarModificadores();
                        } catch (error) {
                            mostrarError(error);
                        }
                    }
                }
            ]
        });
    }

    function pedirEliminarModificador(modificador) {
        confirmar({
            titulo: "Eliminar modificador",
            mensaje: `¿Eliminar "${modificador.nombre}" del menú? Ya no se podrá elegir en órdenes nuevas; las órdenes que ya lo tienen no cambian.`,
            textoConfirmar: "Eliminar",
            alConfirmar: async () => {
                try {
                    await eliminarModificador(modificador.id);
                    mostrarExito(`Modificador "${modificador.nombre}" eliminado`);
                    cargarModificadores();
                } catch (error) {
                    mostrarError(error);
                    return false;
                }
            }
        });
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

    function actualizarCosto() {
        fijarCostoSegunTipo(tipoModificador, costoModificador);
    }
}

/** Si el tipo es EXCLUSION, el costo se deshabilita y se fija en "0". */
function fijarCostoSegunTipo(tipo, costo) {
    const esExclusion = tipo.value === "EXCLUSION";
    if (esExclusion) {
        costo.value = "0";
    } else if (costo.disabled) {
        costo.value = "";
    }
    costo.disabled = esExclusion;
}

function botonesFila(alEditar, alEliminar) {
    return crear("div", { class: "fila" },
        crear("button", { type: "button", class: "boton boton--secundario boton--pequeno", onclick: alEditar }, "Editar"),
        crear("button", { type: "button", class: "boton boton--peligro boton--pequeno", onclick: alEliminar }, "Eliminar"));
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
