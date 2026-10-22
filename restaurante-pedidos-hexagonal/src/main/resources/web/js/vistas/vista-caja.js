// Vista Caja (Cajero, RF4 y RF5): totales con descuento y propina, y cobro.
// Solo aparecen las órdenes con todos sus platillos listos para entrega.
// Los totales siempre los calcula el backend; aquí solo se sugiere la propina rápida.

import { listarOrdenes, obtenerOrden, calcularTotales, cobrar } from "../api/api.js";
import { mostrarExito, mostrarError } from "../componentes/notificaciones.js";
import { insigniaEstado } from "../componentes/insignia.js";
import { crear, limpiar, campo, mientrasEspera, mostrarEnPantallaAngosta } from "../util/dom.js";
import { formatoDinero, formatoFecha, normalizarMonto, etiqueta } from "../util/formato.js";

const ESPERA_ESCRITURA_MS = 400;
const INTERVALO_LISTA_MS = 5000;
const PROPINAS_RAPIDAS = [10, 15, 20];
const METODOS = ["EFECTIVO", "TARJETA", "PASARELA_DIGITAL"];

export function montar(contenedor) {
    const estado = {
        activa: true,
        ordenSeleccionadaId: null,
        orden: null,
        totales: null,
        calculando: false,
        consultaTotales: 0,
        consultaDetalle: 0,
        temporizador: null,
        cargandoLista: false,
        recargaPendiente: false,
        firmaLista: null
    };

    const lista = crear("ul", { class: "lista-ordenes", "aria-label": "Órdenes listas para entrega" });
    const detalle = crear("section", { class: "tarjeta" });

    contenedor.appendChild(crear("section", { class: "vista" },
        crear("div", { class: "vista__encabezado" }, crear("h2", { class: "vista__titulo" }, "Caja")),
        crear("div", { class: "columnas columnas--lista-detalle" },
            crear("section", { class: "tarjeta" },
                crear("div", { class: "pila" },
                    crear("h3", { class: "tarjeta__subtitulo" }, "Listas para entrega"),
                    crear("span", { class: "texto-secundario" }, "Órdenes con todos sus platillos listos en cocina")),
                lista),
            detalle)));

    // Controles del cobro: se crean una vez y se conservan entre intentos (p. ej. tras un rechazo).
    const descuento = crear("input", { type: "text", id: "caja-descuento", inputmode: "decimal", autocomplete: "off", placeholder: "0.00", oninput: programarTotales });
    const propina = crear("input", { type: "text", id: "caja-propina", inputmode: "decimal", autocomplete: "off", placeholder: "0.00", oninput: programarTotales });
    const errorTotales = crear("p", { class: "texto-error", role: "alert", hidden: true });
    const desglose = crear("div", {});
    const metodos = crear("fieldset", { class: "metodos-pago" },
        crear("legend", {}, "Método de pago"),
        METODOS.map((metodo) => crear("label", { class: "metodo-pago", for: `caja-metodo-${metodo}` },
            crear("input", { type: "radio", name: "caja-metodo", id: `caja-metodo-${metodo}`, value: metodo, checked: metodo === "EFECTIVO" }),
            etiqueta(metodo))));
    const botonCobrar = crear("button", { type: "submit", class: "boton boton--primario boton--grande" }, "Cobrar");
    const formularioCobro = crear("form", { class: "formulario", onsubmit: cobrarOrden },
        crear("div", { class: "formulario__campos" },
            campo("Descuento", descuento, "Monto en pesos; vacío = 0"),
            campo("Propina", propina, "Vacío = 0")),
        crear("div", { class: "pila" },
            crear("span", { class: "campo__etiqueta" }, "Propina sugerida"),
            crear("div", { class: "botones-rapidos" }, PROPINAS_RAPIDAS.map((porcentaje) =>
                crear("button", { type: "button", class: "boton boton--secundario boton--pequeno", onclick: () => sugerirPropina(porcentaje) }, `${porcentaje} %`)))),
        errorTotales,
        desglose,
        metodos,
        botonCobrar);
    const encabezado = crear("div", { class: "detalle-encabezado" });
    const resultado = crear("div", { "aria-live": "polite" });
    const historial = crear("div", {});

    pintarSinSeleccion();
    cargarLista();
    // La lista se actualiza sola para que aparezcan las órdenes que la cocina va terminando.
    const intervalo = setInterval(() => cargarLista(true), INTERVALO_LISTA_MS);

    return () => {
        estado.activa = false;
        clearTimeout(estado.temporizador);
        clearInterval(intervalo);
    };

    // ------------------------------------------------------------ datos

    /** automatica: refresco periódico; si falla no se avisa con notificación para no repetir el aviso cada 5 s. */
    async function cargarLista(automatica = false) {
        if (estado.cargandoLista) {
            // Una recarga pedida por el usuario no se pierde: se repite al terminar la que está en curso.
            estado.recargaPendiente ||= !automatica;
            return;
        }
        estado.cargandoLista = true;
        if (!lista.firstChild) {
            lista.appendChild(crear("li", { class: "cargando" }, "Cargando órdenes…"));
        }
        try {
            const ordenes = (await listarOrdenes("ABIERTA")).filter(estaListaParaEntrega);
            if (!estado.activa) return;
            // Solo se vuelve a pintar si algo cambió, para no perder el foco del teclado en cada refresco.
            const firma = JSON.stringify(ordenes.map((o) => [o.id, o.totales?.total]));
            if (firma !== estado.firmaLista) {
                estado.firmaLista = firma;
                pintarLista(ordenes);
            }
        } catch (error) {
            if (!estado.activa) return;
            if (!automatica) {
                estado.firmaLista = null;
                limpiar(lista).appendChild(crear("li", { class: "texto-secundario" }, "No se pudieron cargar las órdenes."));
                mostrarError(error);
            }
        } finally {
            estado.cargandoLista = false;
            if (estado.recargaPendiente && estado.activa) {
                estado.recargaPendiente = false;
                cargarLista();
            }
        }
    }

    async function seleccionar(ordenId) {
        const consulta = ++estado.consultaDetalle;
        estado.ordenSeleccionadaId = ordenId;
        marcarSeleccionada();
        try {
            const orden = await obtenerOrden(ordenId);
            if (!estado.activa || consulta !== estado.consultaDetalle) return;
            descuento.value = "";
            propina.value = "";
            limpiar(resultado);
            pintarDetalle(orden);
            pedirTotales();
            mostrarEnPantallaAngosta(detalle);
        } catch (error) {
            if (estado.activa) mostrarError(error);
        }
    }

    function programarTotales() {
        clearTimeout(estado.temporizador);
        estado.calculando = true;
        actualizarBotonCobrar();
        estado.temporizador = setTimeout(pedirTotales, ESPERA_ESCRITURA_MS);
    }

    async function pedirTotales() {
        clearTimeout(estado.temporizador);
        if (!estado.orden || estado.orden.estado !== "ABIERTA") {
            estado.calculando = false;
            actualizarBotonCobrar();
            return;
        }
        const consulta = ++estado.consultaTotales;
        estado.calculando = true;
        actualizarBotonCobrar();
        if (!estado.totales) {
            pintarDesglose();
        }
        try {
            const totales = await calcularTotales(estado.orden.id, montosCapturados());
            if (!estado.activa || consulta !== estado.consultaTotales) return;
            estado.totales = totales;
            errorTotales.hidden = true;
            errorTotales.textContent = "";
        } catch (error) {
            if (!estado.activa || consulta !== estado.consultaTotales) return;
            estado.totales = null;
            errorTotales.textContent = error.message;
            errorTotales.hidden = false;
        }
        estado.calculando = false;
        pintarDesglose();
        actualizarBotonCobrar();
    }

    function montosCapturados() {
        return { descuento: normalizarMonto(descuento.value), propina: normalizarMonto(propina.value) };
    }

    // ------------------------------------------------------------ acciones

    /** Único cálculo en el navegador: sugiere (subtotal - descuento) × porcentaje con 2 decimales. */
    function sugerirPropina(porcentaje) {
        if (!estado.orden) return;
        const subtotal = Number(estado.orden.totales?.subtotal ?? 0);
        const descuentoCapturado = Number(normalizarMonto(descuento.value));
        const base = Math.max(subtotal - (Number.isFinite(descuentoCapturado) ? descuentoCapturado : 0), 0);
        const sugerida = Math.round(base * porcentaje + 1e-7) / 100;
        propina.value = sugerida.toFixed(2);
        pedirTotales();
    }

    function cobrarOrden(evento) {
        evento.preventDefault();
        if (!estado.orden || !estado.totales || estado.calculando) {
            return;
        }
        const orden = estado.orden;
        const metodo = metodos.querySelector("input:checked")?.value;
        mientrasEspera(botonCobrar, async () => {
            try {
                // Puede haber cambiado desde otro dispositivo (p. ej. el mesero agregó un platillo).
                const vigente = await obtenerOrden(orden.id);
                if (!estaListaParaEntrega(vigente)) {
                    mostrarError("La orden ya no está lista para entrega: tiene platillos pendientes en cocina");
                    estado.ordenSeleccionadaId = null;
                    estado.orden = null;
                    pintarSinSeleccion();
                    cargarLista();
                    return;
                }
                const pago = await cobrar(orden.id, { metodo, ...montosCapturados() });
                pintarResultado(pago);
                if (pago.estado === "APROBADO") {
                    mostrarExito(`Pago aprobado: ${formatoDinero(pago.monto)}`);
                    cargarLista();
                } else {
                    mostrarError("Pago rechazado, intenta con otro método");
                }
                const actualizada = await obtenerOrden(orden.id);
                if (!estado.activa || estado.ordenSeleccionadaId !== orden.id) return;
                estado.orden = actualizada;
                pintarEncabezado(actualizada);
                pintarHistorial(actualizada);
                if (actualizada.estado !== "ABIERTA") {
                    formularioCobro.hidden = true;
                }
            } catch (error) {
                mostrarError(error);
            }
        }).then(actualizarBotonCobrar);
    }

    // ------------------------------------------------------------ pintado

    function pintarLista(ordenes) {
        limpiar(lista);
        if (ordenes.length === 0) {
            lista.appendChild(crear("li", { class: "texto-secundario" },
                "No hay órdenes listas para entrega. Aparecen aquí cuando la cocina marca todos sus platillos como listos."));
            return;
        }
        for (const orden of ordenes) {
            lista.appendChild(crear("li", {},
                crear("button", { type: "button", class: "tarjeta-orden", dataset: { ordenId: orden.id }, onclick: () => seleccionar(orden.id) },
                    crear("span", { class: "tarjeta-orden__fila" },
                        crear("span", { class: "tarjeta-orden__referencia" }, orden.referencia),
                        crear("span", { class: "tarjeta-orden__total" }, formatoDinero(orden.totales?.total))),
                    crear("span", { class: "tarjeta-orden__fila" }, insigniaEstado(orden.tipo)))));
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
        limpiar(detalle).appendChild(crear("p", { class: "texto-vacio" }, "Selecciona una orden lista para entrega para cobrarla"));
    }

    function pintarDetalle(orden) {
        estado.orden = orden;
        estado.totales = null;
        errorTotales.hidden = true;
        formularioCobro.hidden = orden.estado !== "ABIERTA";
        limpiar(detalle);

        detalle.appendChild(encabezado);
        pintarEncabezado(orden);
        detalle.appendChild(resumenLineas(orden));
        detalle.appendChild(formularioCobro);
        detalle.appendChild(resultado);
        detalle.appendChild(historial);

        pintarDesglose();
        pintarHistorial(orden);
        actualizarBotonCobrar();
    }

    function pintarEncabezado(orden) {
        limpiar(encabezado).append(
            crear("h2", { class: "detalle-encabezado__referencia" }, orden.referencia),
            crear("div", { class: "fila" }, insigniaEstado(orden.tipo), insigniaEstado(orden.estado)));
    }

    function pintarDesglose() {
        limpiar(desglose);
        if (estado.totales) {
            desglose.appendChild(resumenTotales(estado.totales));
        } else if (estado.calculando) {
            desglose.appendChild(crear("p", { class: "cargando" }, "Calculando totales…"));
        }
    }

    function actualizarBotonCobrar() {
        const listo = Boolean(estado.totales) && !estado.calculando;
        botonCobrar.disabled = !listo;
        botonCobrar.textContent = estado.totales ? `Cobrar ${formatoDinero(estado.totales.total)}` : "Cobrar";
    }

    function pintarResultado(pago) {
        limpiar(resultado);
        const aprobado = pago.estado === "APROBADO";
        resultado.appendChild(crear("div", { class: `panel-resultado panel-resultado--${aprobado ? "exito" : "error"}` },
            crear("span", { class: "panel-resultado__titulo" }, aprobado ? "Pago aprobado" : "Pago rechazado, intenta con otro método"),
            crear("span", {}, `${etiqueta(pago.metodo)} · ${formatoDinero(pago.monto)}`),
            pago.referencia ? crear("span", {}, `Referencia: ${pago.referencia}`) : null));
    }

    function pintarHistorial(orden) {
        limpiar(historial);
        if (!orden.pagos?.length) {
            return;
        }
        historial.appendChild(crear("div", { class: "pila" },
            crear("h3", { class: "tarjeta__subtitulo" }, "Historial de pagos"),
            crear("ul", { class: "lista-pagos" }, orden.pagos.map((pago) => crear("li", {},
                insigniaEstado(pago.estado, "pago"),
                crear("span", {}, etiqueta(pago.metodo)),
                crear("span", { class: "texto-secundario" }, `${formatoFecha(pago.fecha)} · ${pago.referencia ?? ""}`),
                crear("span", { class: "lista-pagos__monto" }, formatoDinero(pago.monto)))))));
    }
}

/** Una orden pasa a caja cuando tiene platillos y todos los no cancelados están listos para entrega. */
function estaListaParaEntrega(orden) {
    const activas = orden.lineas.filter((l) => l.estadoCocina !== "CANCELADA");
    return activas.length > 0 && activas.every((l) => l.estadoCocina === "LISTO");
}

function resumenLineas(orden) {
    const activas = orden.lineas.filter((l) => l.estadoCocina !== "CANCELADA");
    if (activas.length === 0) {
        return crear("p", { class: "texto-secundario" }, "La orden no tiene líneas.");
    }
    return crear("div", { class: "tabla-contenedor" },
        crear("table", { class: "tabla" },
            crear("thead", {}, crear("tr", {},
                crear("th", { scope: "col" }, "Plato"),
                crear("th", { scope: "col", class: "numero" }, "Cantidad"),
                crear("th", { scope: "col", class: "numero" }, "Precio unitario"),
                crear("th", { scope: "col" }, "Modificadores"))),
            crear("tbody", {}, activas.map((linea) => crear("tr", {},
                crear("td", {}, linea.platoNombre),
                crear("td", { class: "numero" }, String(linea.cantidad)),
                crear("td", { class: "numero" }, formatoDinero(linea.precioUnitario)),
                crear("td", { class: "modificadores-linea" }, linea.modificadores?.length
                    ? linea.modificadores.map((m) => Number(m.costoExtra) > 0 ? `${m.nombre} (+${formatoDinero(m.costoExtra)})` : m.nombre).join(", ")
                    : "—"))))));
}

function resumenTotales(totales) {
    return crear("dl", { class: "resumen-totales" },
        crear("dt", {}, "Subtotal"), crear("dd", {}, formatoDinero(totales.subtotal)),
        crear("dt", {}, "Descuento"),
        crear("dd", {}, Number(totales.descuento) > 0 ? `−${formatoDinero(totales.descuento)}` : formatoDinero(totales.descuento)),
        crear("dt", {}, "Impuestos"), crear("dd", {}, formatoDinero(totales.impuestos)),
        crear("dt", {}, "Propina"), crear("dd", {}, formatoDinero(totales.propina)),
        crear("dt", { class: "resumen-totales__total" }, "Total"),
        crear("dd", { class: "resumen-totales__total" }, formatoDinero(totales.total)));
}
