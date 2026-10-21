// Vista Órdenes (Mesero, RF2 y RF3): abrir órdenes; agregar, modificar o cancelar líneas; cancelar órdenes.

import {
    listarPlatos, listarModificadores, listarOrdenes, obtenerOrden, abrirOrden,
    agregarLinea, modificarLinea, cancelarLinea, cancelarOrden
} from "../api/api.js";
import { mostrarExito, mostrarError } from "../componentes/notificaciones.js";
import { abrirModal, cerrarModal, confirmar } from "../componentes/modal.js";
import { insigniaEstado } from "../componentes/insignia.js";
import { crear, limpiar, campo, mientrasEspera, mostrarEnPantallaAngosta } from "../util/dom.js";
import { formatoDinero, formatoFecha, etiqueta } from "../util/formato.js";

const FILTROS = [
    { valor: "ABIERTA", texto: "Abiertas" },
    { valor: "CERRADA", texto: "Cerradas" },
    { valor: "CANCELADA", texto: "Canceladas" },
    { valor: "", texto: "Todas" }
];

export function montar(contenedor) {
    const estado = {
        activa: true,
        filtro: "ABIERTA",
        ordenSeleccionadaId: null,
        platos: [],
        modificadores: [],
        consultaLista: 0,
        consultaDetalle: 0
    };

    // ------------------------------------------------------------ columna izquierda
    const tipoNueva = crear("select", { id: "orden-tipo" },
        ["SALA", "PARA_LLEVAR", "DELIVERY"].map((tipo) => crear("option", { value: tipo }, etiqueta(tipo))));
    const referenciaNueva = crear("input", { type: "text", id: "orden-referencia", autocomplete: "off", placeholder: "Mesa 5" });
    const botonAbrir = crear("button", { type: "submit", class: "boton boton--primario" }, "Abrir");
    const formularioNueva = crear("form", { class: "formulario", onsubmit: abrirNueva },
        crear("h3", { class: "tarjeta__subtitulo" }, "Nueva orden"),
        crear("div", { class: "formulario__campos" },
            campo("Tipo", tipoNueva),
            campo("Referencia", referenciaNueva, "Número de mesa o id del pedido externo")),
        crear("div", { class: "fila" }, botonAbrir));

    const filtro = crear("select", { id: "orden-filtro", onchange: () => { estado.filtro = filtro.value; cargarLista(); } },
        FILTROS.map((f) => crear("option", { value: f.valor, selected: f.valor === estado.filtro }, f.texto)));
    const lista = crear("ul", { class: "lista-ordenes", "aria-label": "Órdenes" });

    const detalle = crear("section", { class: "tarjeta detalle-orden", "aria-live": "polite" });

    contenedor.appendChild(crear("section", { class: "vista" },
        crear("div", { class: "vista__encabezado" }, crear("h2", { class: "vista__titulo" }, "Órdenes")),
        crear("div", { class: "columnas columnas--lista-detalle" },
            crear("div", { class: "pila" },
                crear("section", { class: "tarjeta" }, formularioNueva),
                crear("section", { class: "tarjeta" },
                    crear("div", { class: "fila" }, crear("h3", { class: "tarjeta__subtitulo" }, "Órdenes")),
                    campo("Estado", filtro),
                    lista)),
            detalle)));

    pintarSinSeleccion();
    cargarCatalogo();
    cargarLista();

    return () => { estado.activa = false; };

    // ------------------------------------------------------------ datos

    async function cargarCatalogo() {
        try {
            const [platos, modificadores] = await Promise.all([listarPlatos(), listarModificadores()]);
            if (!estado.activa) return;
            estado.platos = platos;
            estado.modificadores = modificadores;
            if (estado.ordenSeleccionadaId) {
                await recargarDetalle();
            }
        } catch (error) {
            if (estado.activa) mostrarError(error);
        }
    }

    async function cargarLista() {
        const consulta = ++estado.consultaLista;
        if (!lista.firstChild) {
            lista.appendChild(crear("li", { class: "cargando" }, "Cargando órdenes…"));
        }
        try {
            const ordenes = await listarOrdenes(estado.filtro || undefined);
            if (!estado.activa || consulta !== estado.consultaLista) return;
            pintarLista(ordenes);
        } catch (error) {
            if (!estado.activa || consulta !== estado.consultaLista) return;
            limpiar(lista).appendChild(crear("li", { class: "texto-secundario" }, "No se pudieron cargar las órdenes."));
            mostrarError(error);
        }
    }

    async function seleccionar(ordenId) {
        estado.ordenSeleccionadaId = ordenId;
        marcarSeleccionada();
        await recargarDetalle();
        mostrarEnPantallaAngosta(detalle);
    }

    async function recargarDetalle() {
        const consulta = ++estado.consultaDetalle;
        try {
            const orden = await obtenerOrden(estado.ordenSeleccionadaId);
            if (!estado.activa || consulta !== estado.consultaDetalle) return;
            pintarDetalle(orden);
        } catch (error) {
            if (estado.activa) mostrarError(error);
        }
    }

    /** Después de cada acción: pinta el OrdenDto devuelto por el backend y refresca la lista. */
    function alActualizar(orden) {
        estado.ordenSeleccionadaId = orden.id;
        estado.consultaDetalle++;
        pintarDetalle(orden);
        cargarLista();
    }

    // ------------------------------------------------------------ acciones

    function abrirNueva(evento) {
        evento.preventDefault();
        mientrasEspera(botonAbrir, async () => {
            try {
                const orden = await abrirOrden({ tipo: tipoNueva.value, referencia: referenciaNueva.value.trim() });
                mostrarExito(`Orden "${orden.referencia}" abierta`);
                referenciaNueva.value = "";
                if (estado.filtro !== "ABIERTA" && estado.filtro !== "") {
                    estado.filtro = "ABIERTA";
                    filtro.value = "ABIERTA";
                }
                alActualizar(orden);
                mostrarEnPantallaAngosta(detalle);
            } catch (error) {
                mostrarError(error);
            }
        });
    }

    function editarLinea(orden, linea) {
        const cantidad = crear("input", { type: "number", id: "editar-cantidad", min: "1", step: "1", value: String(linea.cantidad) });
        const actuales = new Set(linea.modificadores.map((m) => m.id));
        const casillas = casillasModificadores("editar-mod", estado.modificadores, actuales);

        abrirModal({
            titulo: `Editar ${linea.platoNombre}`,
            contenido: crear("form", { class: "formulario" }, campo("Cantidad", cantidad), casillas),
            acciones: [
                { texto: "Volver", tipo: "secundario" },
                {
                    texto: "Guardar cambios",
                    tipo: "primario",
                    alHacerClic: async () => {
                        try {
                            const actualizada = await modificarLinea(orden.id, linea.id, {
                                cantidad: leerEntero(cantidad.value),
                                modificadorIds: idsMarcados(casillas)
                            });
                            cerrarModal();
                            mostrarExito("Línea actualizada");
                            alActualizar(actualizada);
                        } catch (error) {
                            mostrarError(error);
                        }
                    }
                }
            ]
        });
    }

    function pedirCancelarLinea(orden, linea) {
        confirmar({
            titulo: "Cancelar línea",
            mensaje: `¿Cancelar ${linea.platoNombre} × ${linea.cantidad}? La línea quedará cancelada y ya no contará en el total.`,
            textoConfirmar: "Cancelar línea",
            alConfirmar: async () => {
                try {
                    const actualizada = await cancelarLinea(orden.id, linea.id);
                    mostrarExito("Línea cancelada");
                    alActualizar(actualizada);
                } catch (error) {
                    mostrarError(error);
                    return false;
                }
            }
        });
    }

    function pedirCancelarOrden(orden) {
        confirmar({
            titulo: "Cancelar orden",
            mensaje: `¿Cancelar la orden "${orden.referencia}"? Solo es posible si ninguna línea ha entrado a cocina.`,
            textoConfirmar: "Cancelar orden",
            alConfirmar: async () => {
                try {
                    const actualizada = await cancelarOrden(orden.id);
                    mostrarExito(`Orden "${actualizada.referencia}" cancelada`);
                    alActualizar(actualizada);
                } catch (error) {
                    mostrarError(error);
                    return false;
                }
            }
        });
    }

    // ------------------------------------------------------------ pintado

    function pintarLista(ordenes) {
        limpiar(lista);
        if (ordenes.length === 0) {
            lista.appendChild(crear("li", { class: "texto-secundario" }, "No hay órdenes con este estado."));
            return;
        }
        for (const orden of ordenes) {
            const activas = orden.lineas.filter((l) => l.estadoCocina !== "CANCELADA").length;
            lista.appendChild(crear("li", {},
                crear("button", {
                    type: "button",
                    class: "tarjeta-orden",
                    dataset: { ordenId: orden.id },
                    onclick: () => seleccionar(orden.id)
                },
                    crear("span", { class: "tarjeta-orden__fila" },
                        crear("span", { class: "tarjeta-orden__referencia" }, orden.referencia),
                        crear("span", { class: "tarjeta-orden__total" }, formatoDinero(orden.totales?.total))),
                    crear("span", { class: "tarjeta-orden__fila" },
                        crear("span", { class: "fila" }, insigniaEstado(orden.tipo), insigniaEstado(orden.estado)),
                        crear("span", { class: "texto-secundario" }, activas === 1 ? "1 línea" : `${activas} líneas`)))));
        }
        marcarSeleccionada();
    }

    function marcarSeleccionada() {
        for (const boton of lista.querySelectorAll(".tarjeta-orden")) {
            const seleccionada = boton.dataset.ordenId === estado.ordenSeleccionadaId;
            boton.classList.toggle("tarjeta-orden--seleccionada", seleccionada);
            boton.setAttribute("aria-pressed", String(seleccionada));
        }
    }

    function pintarSinSeleccion() {
        limpiar(detalle).appendChild(crear("p", { class: "texto-vacio" }, "Selecciona o abre una orden"));
    }

    function pintarDetalle(orden) {
        const abierta = orden.estado === "ABIERTA";
        limpiar(detalle);

        detalle.appendChild(crear("div", { class: "detalle-encabezado" },
            crear("div", { class: "pila" },
                crear("h2", { class: "detalle-encabezado__referencia" }, orden.referencia),
                crear("span", { class: "texto-secundario" }, `Creada el ${formatoFecha(orden.fechaCreacion)}`)),
            crear("div", { class: "fila" }, insigniaEstado(orden.tipo), insigniaEstado(orden.estado))));

        detalle.appendChild(tablaLineas(orden));

        if (abierta) {
            detalle.appendChild(crear("hr", { class: "separador" }));
            detalle.appendChild(formularioAgregar(orden));
        }

        detalle.appendChild(crear("div", { class: "pila" },
            crear("h3", { class: "tarjeta__subtitulo" }, "Totales"),
            resumenTotales(orden.totales)));

        if (orden.pagos?.length) {
            detalle.appendChild(crear("div", { class: "pila" },
                crear("h3", { class: "tarjeta__subtitulo" }, "Pagos"),
                listaPagos(orden.pagos)));
        }

        if (abierta) {
            detalle.appendChild(crear("div", { class: "fila" },
                crear("button", { type: "button", class: "boton boton--peligro", onclick: () => pedirCancelarOrden(orden) }, "Cancelar orden")));
        }
    }

    function tablaLineas(orden) {
        if (orden.lineas.length === 0) {
            return crear("p", { class: "texto-secundario" }, "La orden aún no tiene líneas.");
        }
        return crear("div", { class: "tabla-contenedor" },
            crear("table", { class: "tabla" },
                crear("thead", {}, crear("tr", {},
                    crear("th", { scope: "col" }, "Plato"),
                    crear("th", { scope: "col", class: "numero" }, "Cantidad"),
                    crear("th", { scope: "col", class: "numero" }, "Precio unitario"),
                    crear("th", { scope: "col" }, "Modificadores"),
                    crear("th", { scope: "col" }, "Estado"),
                    crear("th", { scope: "col" }, "Acciones"))),
                crear("tbody", {}, orden.lineas.map((linea) => crear("tr", { class: linea.estadoCocina === "CANCELADA" ? "linea--cancelada" : null },
                    crear("td", {}, linea.platoNombre),
                    crear("td", { class: "numero" }, String(linea.cantidad)),
                    crear("td", { class: "numero" }, formatoDinero(linea.precioUnitario)),
                    crear("td", { class: "modificadores-linea" }, textoModificadores(linea.modificadores)),
                    crear("td", { class: "celda-estado" }, insigniaEstado(linea.estadoCocina)),
                    crear("td", { class: "acciones" }, linea.editable === true
                        ? crear("div", { class: "fila" },
                            crear("button", { type: "button", class: "boton boton--secundario boton--pequeno", onclick: () => editarLinea(orden, linea) }, "Editar"),
                            crear("button", { type: "button", class: "boton boton--peligro boton--pequeno", onclick: () => pedirCancelarLinea(orden, linea) }, "Cancelar"))
                        : null))))));
    }

    function formularioAgregar(orden) {
        const titulo = crear("h3", { class: "tarjeta__subtitulo" }, "Agregar línea");
        if (estado.platos.length === 0) {
            return crear("div", { class: "pila" }, titulo,
                crear("p", { class: "texto-secundario" }, "No hay platos en el menú. Regístralos en la pestaña Menú."));
        }

        const plato = crear("select", { id: "linea-plato" },
            estado.platos.map((p) => crear("option", { value: p.id }, `${p.nombre} (${formatoDinero(p.precioBase)})`)));
        const cantidad = crear("input", { type: "number", id: "linea-cantidad", min: "1", step: "1", value: "1" });
        const casillas = casillasModificadores("linea-mod", estado.modificadores, new Set());
        const boton = crear("button", { type: "submit", class: "boton boton--primario" }, "Agregar");

        const formulario = crear("form", {
            class: "formulario",
            onsubmit: (evento) => {
                evento.preventDefault();
                mientrasEspera(boton, async () => {
                    try {
                        const actualizada = await agregarLinea(orden.id, {
                            platoId: plato.value,
                            cantidad: leerEntero(cantidad.value),
                            modificadorIds: idsMarcados(casillas)
                        });
                        mostrarExito("Línea agregada");
                        alActualizar(actualizada);
                    } catch (error) {
                        mostrarError(error);
                    }
                });
            }
        },
            titulo,
            crear("div", { class: "formulario__campos" }, campo("Plato", plato), campo("Cantidad", cantidad)),
            casillas,
            crear("div", { class: "fila" }, boton));
        return formulario;
    }
}

// ---------------------------------------------------------------- ayudantes de pintado

function casillasModificadores(prefijo, modificadores, marcados) {
    const grupo = crear("fieldset", { class: "casillas" }, crear("legend", {}, "Modificadores"));
    if (modificadores.length === 0) {
        grupo.appendChild(crear("span", { class: "texto-secundario" }, "No hay modificadores registrados."));
        return grupo;
    }
    for (const m of modificadores) {
        const id = `${prefijo}-${m.id}`;
        const detalleCosto = m.tipo === "EXCLUSION" ? " (exclusión)" : Number(m.costoExtra) > 0 ? ` (+${formatoDinero(m.costoExtra)})` : "";
        grupo.appendChild(crear("label", { class: "casilla", for: id },
            crear("input", { type: "checkbox", id, value: m.id, checked: marcados.has(m.id) }),
            `${m.nombre}${detalleCosto}`));
    }
    return grupo;
}

function idsMarcados(grupo) {
    return [...grupo.querySelectorAll("input[type=checkbox]:checked")].map((casilla) => casilla.value);
}

/** La cantidad viaja como entero; un valor vacío o inválido se manda como 0 y el backend explica el error. */
function leerEntero(texto) {
    const numero = Number(texto);
    return Number.isInteger(numero) ? numero : 0;
}

function textoModificadores(modificadores) {
    if (!modificadores?.length) {
        return "—";
    }
    return modificadores.map((m) => m.tipo === "EXCLUSION"
        ? m.nombre
        : Number(m.costoExtra) > 0 ? `${m.nombre} (+${formatoDinero(m.costoExtra)})` : m.nombre).join(", ");
}

function resumenTotales(totales) {
    if (!totales) {
        return crear("p", { class: "texto-secundario" }, "Sin totales.");
    }
    return crear("dl", { class: "resumen-totales" },
        crear("dt", {}, "Subtotal"), crear("dd", {}, formatoDinero(totales.subtotal)),
        crear("dt", {}, "Descuento"),
        crear("dd", {}, Number(totales.descuento) > 0 ? `−${formatoDinero(totales.descuento)}` : formatoDinero(totales.descuento)),
        crear("dt", {}, "Impuestos"), crear("dd", {}, formatoDinero(totales.impuestos)),
        crear("dt", {}, "Propina"), crear("dd", {}, formatoDinero(totales.propina)),
        crear("dt", { class: "resumen-totales__total" }, "Total"),
        crear("dd", { class: "resumen-totales__total" }, formatoDinero(totales.total)));
}

function listaPagos(pagos) {
    return crear("ul", { class: "lista-pagos" }, pagos.map((pago) => crear("li", {},
        insigniaEstado(pago.estado, "pago"),
        crear("span", {}, etiqueta(pago.metodo)),
        crear("span", { class: "texto-secundario" }, `${formatoFecha(pago.fecha)} · ${pago.referencia ?? ""}`),
        crear("span", { class: "lista-pagos__monto" }, formatoDinero(pago.monto)))));
}
