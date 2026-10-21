// Acceso a la API real (mismo origen, prefijo /api).

const PREFIJO = "/api";

export class ErrorApi extends Error {
    estadoHttp;

    constructor(estadoHttp, mensaje) {
        super(mensaje);
        this.name = "ErrorApi";
        this.estadoHttp = estadoHttp;
    }
}

/**
 * Hace una petición a la API.
 * - ruta relativa a "/api", p. ej. peticion("GET", "/platos")
 * - 204 → null; 2xx → el JSON de la respuesta
 * - si no es 2xx → ErrorApi con el campo "error" de la respuesta (o "Error de conexión")
 * - si el servidor no responde → ErrorApi(0, "No se pudo conectar con el servidor")
 */
export async function peticion(metodo, ruta, cuerpo = undefined) {
    const opciones = { method: metodo, headers: { Accept: "application/json" } };
    if (cuerpo !== undefined) {
        opciones.headers["Content-Type"] = "application/json";
        opciones.body = JSON.stringify(cuerpo);
    }

    let respuesta;
    try {
        respuesta = await fetch(PREFIJO + ruta, opciones);
    } catch {
        throw new ErrorApi(0, "No se pudo conectar con el servidor");
    }

    if (respuesta.status === 204) {
        return null;
    }

    const datos = await leerJson(respuesta);
    if (!respuesta.ok) {
        const mensaje = datos && typeof datos.error === "string" && datos.error ? datos.error : "Error de conexión";
        throw new ErrorApi(respuesta.status, mensaje);
    }
    return datos;
}

async function leerJson(respuesta) {
    const texto = await respuesta.text();
    if (!texto) {
        return null;
    }
    try {
        return JSON.parse(texto);
    } catch {
        return null;
    }
}
