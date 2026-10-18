package com.restaurante.infrastructure.in.cli;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.restaurante.application.port.in.AbrirOrdenUseCase;
import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.application.port.in.AgregarLineaUseCase;
import com.restaurante.application.port.in.CancelarLineaUseCase;
import com.restaurante.application.port.in.CerrarOrdenUseCase;
import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoOrden;

/**
 * Adaptador de entrada (consola interactiva).
 *
 * Solo conoce los PUERTOS DE ENTRADA (casos de uso) y el dominio; nunca los servicios
 * concretos ni los repositorios. Quien lo arma y le pasa los casos de uso es {@link Main}.
 */
public class MenuConsola {

    private final AdministrarMenuUseCase administrarMenu;
    private final AbrirOrdenUseCase abrirOrden;
    private final AgregarLineaUseCase agregarLinea;
    private final CancelarLineaUseCase cancelarLinea;
    private final GestionarCocinaUseCase gestionarCocina;
    private final CerrarOrdenUseCase cerrarOrden;
    private final Scanner entrada;
    private final PrintStream salida;

    /** Ids de las órdenes abiertas en esta sesión, solo para facilitar elegirlas al cerrar. */
    private final List<String> ordenesDeLaSesion = new ArrayList<>();

    public MenuConsola(AdministrarMenuUseCase administrarMenu,
                       AbrirOrdenUseCase abrirOrden,
                       AgregarLineaUseCase agregarLinea,
                       CancelarLineaUseCase cancelarLinea,
                       GestionarCocinaUseCase gestionarCocina,
                       CerrarOrdenUseCase cerrarOrden,
                       Scanner entrada,
                       PrintStream salida) {
        this.administrarMenu = administrarMenu;
        this.abrirOrden = abrirOrden;
        this.agregarLinea = agregarLinea;
        this.cancelarLinea = cancelarLinea;
        this.gestionarCocina = gestionarCocina;
        this.cerrarOrden = cerrarOrden;
        this.entrada = entrada;
        this.salida = salida;
    }

    /** Bucle principal: se repite hasta que el usuario elige "Salir" o se acaba la entrada. */
    public void iniciar() {
        salida.println("=== Sistema de Pedidos de Restaurante ===");
        try {
            boolean continuar = true;
            while (continuar) {
                mostrarOpciones();
                String opcion = leerLinea("Elige una opción: ");
                continuar = ejecutar(opcion);
            }
        } catch (NoSuchElementException e) {
            // Se cerró la entrada (Ctrl+D / Ctrl+Z): terminamos sin mostrar error.
            salida.println();
        }
        salida.println("Hasta luego.");
    }

    private void mostrarOpciones() {
        salida.println();
        salida.println("--------------- MENÚ ---------------");
        salida.println("1. Registrar un plato en el menú");
        salida.println("2. Consultar el menú disponible");
        salida.println("3. Abrir una nueva orden");
        salida.println("4. Agregar una línea a una orden");
        salida.println("5. Cancelar una línea de una orden");
        salida.println("6. Cerrar una orden (procesar pago)");
        salida.println("7. Cocina: ver líneas pendientes");
        salida.println("8. Salir");
        salida.println("------------------------------------");
    }

    /** @return false cuando hay que salir. */
    private boolean ejecutar(String opcion) {
        try {
            switch (opcion) {
                case "1" -> registrarPlato();
                case "2" -> consultarMenu();
                case "3" -> abrirNuevaOrden();
                case "4" -> agregarLineaAOrden();
                case "5" -> cancelarLineaDeOrden();
                case "6" -> cerrarOrdenConPago();
                case "7" -> gestionarCocina();
                case "8" -> {
                    return false;
                }
                default -> salida.println("Opción no válida. Escribe un número del 1 al 8.");
            }
        } catch (ReglaDeNegocioException e) {
            salida.println("No se pudo completar la operación: " + e.getMessage());
        } catch (UnsupportedOperationException e) {
            // Funcionalidad pendiente en el dominio (RF3 / RF4): se informa y la app sigue viva.
            salida.println("Funcionalidad pendiente de implementar: " + e.getMessage());
        }
        return true;
    }

    // ---------------------------------------------------------------- 1. Registrar plato
    private void registrarPlato() {
        salida.println();
        salida.println("-- Registrar plato --");
        String id = leerLinea("Id del plato (ej. P-001): ");
        String nombre = leerLinea("Nombre: ");
        BigDecimal precio = leerDecimal("Precio base (ej. 85.50): ");
        BigDecimal tasa = leerDecimal("Tasa de impuesto como fracción (0.16 = 16 %): ");
        if (precio == null || tasa == null) {
            return; // ya se informó el error de formato
        }

        Plato registrado = administrarMenu.registrar(new Plato(id, nombre, precio, tasa));
        salida.println("Plato registrado: " + registrado.id() + " - " + registrado.nombre());
    }

    // ---------------------------------------------------------------- 2. Consultar menú
    private void consultarMenu() {
        List<Plato> platos = administrarMenu.listar();
        salida.println();
        salida.println("-- Menú disponible --");
        if (platos.isEmpty()) {
            salida.println("(El menú está vacío. Usa la opción 1 para registrar platos.)");
            return;
        }
        salida.printf("%-10s %-30s %12s %10s%n", "ID", "NOMBRE", "PRECIO", "IMPUESTO");
        for (Plato p : platos) {
            salida.printf("%-10s %-30s %12s %9s%%%n",
                    p.id(), p.nombre(), p.precioBase().toPlainString(),
                    p.tasaImpuesto().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
        }
    }

    // ---------------------------------------------------------------- 3. Abrir orden
    private void abrirNuevaOrden() {
        salida.println();
        salida.println("-- Abrir orden --");
        salida.println("Tipo de orden:");
        salida.println("  1. SALA");
        salida.println("  2. LLEVAR");
        salida.println("  3. DELIVERY");
        TipoOrden tipo = switch (leerLinea("Elige el tipo: ")) {
            case "1" -> TipoOrden.SALA;
            case "2" -> TipoOrden.PARA_LLEVAR;
            case "3" -> TipoOrden.DELIVERY;
            default -> null;
        };
        if (tipo == null) {
            salida.println("Tipo de orden no válido.");
            return;
        }
        String referencia = leerLinea("Referencia (número de mesa o id de pedido externo): ");

        Orden orden = abrirOrden.abrir(new AbrirOrdenUseCase.Comando(tipo, referencia));
        ordenesDeLaSesion.add(orden.getId());
        salida.println("Orden abierta.");
        salida.println("  Id:         " + orden.getId());
        salida.println("  Tipo:       " + orden.getTipo());
        salida.println("  Referencia: " + orden.getReferencia());
        salida.println("  Estado:     " + orden.getEstado());
    }

        // ---------------------------------------------------------------- 4. Agregar línea
    private void agregarLineaAOrden() {
        salida.println();
        salida.println("-- Agregar línea a una orden --");
        List<Plato> platos = administrarMenu.listar();
        if (platos.isEmpty()) {
            salida.println("El menú está vacío. Usa la opción 1 para registrar platos primero.");
            return;
        }
        String ordenId = elegirOrden();
        if (ordenId == null) {
            return;
        }

        salida.println("Platos disponibles:");
        for (int i = 0; i < platos.size(); i++) {
            Plato p = platos.get(i);
            salida.println("  " + (i + 1) + ". " + p.nombre() + " (" + p.precioBase().toPlainString() + ")");
        }
        Integer numeroPlato = leerEntero("Número del plato: ");
        if (numeroPlato == null) {
            return;
        }
        if (numeroPlato < 1 || numeroPlato > platos.size()) {
            salida.println("Número de plato no válido.");
            return;
        }
        Integer cantidad = leerEntero("Cantidad: ");
        if (cantidad == null) {
            return;
        }

        // Los modificadores aún no se pueden administrar (RF1): la línea se crea sin ellos.
        Orden orden = agregarLinea.agregar(new AgregarLineaUseCase.Comando(
                ordenId, platos.get(numeroPlato - 1).id(), cantidad, List.of()));

        salida.println("Línea agregada. Líneas de la orden " + orden.getReferencia() + ":");
        for (LineaPedido l : orden.getLineas()) {
            salida.println("  - " + l.getPlato().nombre() + " x" + l.getCantidad()
                    + " [" + l.getEstadoCocina() + "]");
        }
    }

    // ---------------------------------------------------------------- 5. Cancelar línea
    private void cancelarLineaDeOrden() {
        salida.println();
        salida.println("-- Cancelar una línea de una orden --");
        String ordenId = elegirOrden();
        if (ordenId == null) {
            return;
        }
        // Se reutiliza la vista de cocina: muestra las líneas de la orden que aún no están LISTO.
        List<GestionarCocinaUseCase.LineaEnCocina> lineas = gestionarCocina.listarPendientes().stream()
                .filter(l -> l.ordenId().equals(ordenId))
                .toList();
        if (lineas.isEmpty()) {
            salida.println("La orden no tiene líneas por cancelar (no tiene líneas o ya están listas).");
            return;
        }
        salida.println("Líneas de la orden:");
        for (int i = 0; i < lineas.size(); i++) {
            GestionarCocinaUseCase.LineaEnCocina l = lineas.get(i);
            salida.println("  " + (i + 1) + ". " + l.plato() + " x" + l.cantidad() + " [" + l.estado() + "]");
        }
        Integer numero = leerEntero("Número de la línea a cancelar: ");
        if (numero == null) {
            return;
        }
        if (numero < 1 || numero > lineas.size()) {
            salida.println("Número de línea no válido.");
            return;
        }
        GestionarCocinaUseCase.LineaEnCocina elegida = lineas.get(numero - 1);
        cancelarLinea.cancelar(ordenId, elegida.lineaId());
        salida.println("Línea cancelada: " + elegida.plato() + " x" + elegida.cantidad() + ".");
    }
    // ---------------------------------------------------------------- 6. Cerrar orden
    private void cerrarOrdenConPago() {
        salida.println();
        salida.println("-- Cerrar orden --");
        String ordenId = elegirOrden();
        if (ordenId == null) {
            return;
        }

        salida.println("Método de pago:");
        salida.println("  1. EFECTIVO");
        salida.println("  2. TARJETA");
        salida.println("  3. PASARELA_DIGITAL");
        MetodoPago metodo = switch (leerLinea("Elige el método: ")) {
            case "1" -> MetodoPago.EFECTIVO;
            case "2" -> MetodoPago.TARJETA;
            case "3" -> MetodoPago.PASARELA_DIGITAL;
            default -> null;
        };
        if (metodo == null) {
            salida.println("Método de pago no válido.");
            return;
        }

        BigDecimal propina = leerDecimalOpcional("Propina (Enter para 0): ");
        if (propina == null) {
            return;
        }

        Pago pago = cerrarOrden.cerrar(ordenId, metodo, propina);
        salida.println("Orden cerrada. Pago " + pago.id() + " por " + pago.monto().toPlainString()
                + " (" + pago.metodo() + ")");
    }

    // ---------------------------------------------------------------- 7. Cocina
    private void gestionarCocina() {
        while (true) {
            List<GestionarCocinaUseCase.LineaEnCocina> pendientes = gestionarCocina.listarPendientes();
            salida.println();
            salida.println("-- Líneas pendientes en cocina --");
            if (pendientes.isEmpty()) {
                salida.println("No hay líneas pendientes.");
                return;
            }
            salida.printf("%-3s %-16s %-22s %-5s %s%n", "#", "ORDEN", "PLATO", "CANT", "ESTADO");
            for (int i = 0; i < pendientes.size(); i++) {
                GestionarCocinaUseCase.LineaEnCocina l = pendientes.get(i);
                salida.printf("%-3d %-16s %-22s %-5d %s%n",
                        i + 1, l.referenciaOrden(), l.plato(), l.cantidad(), l.estado());
            }
            salida.println();
            salida.println("1. Iniciar cocción");
            salida.println("2. Marcar como lista");
            salida.println("3. Volver al menú principal");
            String opcion = leerLinea("Elige una opción: ");
            if (opcion.equals("3")) {
                return;
            }
            if (!opcion.equals("1") && !opcion.equals("2")) {
                salida.println("Opción no válida. Escribe 1, 2 o 3.");
                continue;
            }
            Integer numero = leerEntero("Número de la línea: ");
            if (numero == null) {
                continue;
            }
            if (numero < 1 || numero > pendientes.size()) {
                salida.println("Número de línea no válido.");
                continue;
            }
            GestionarCocinaUseCase.LineaEnCocina elegida = pendientes.get(numero - 1);
            try {
                if (opcion.equals("1")) {
                    gestionarCocina.iniciarCoccion(elegida.ordenId(), elegida.lineaId());
                    salida.println("Cocción iniciada: " + elegida.plato() + " (" + elegida.referenciaOrden() + ").");
                } else {
                    gestionarCocina.marcarLista(elegida.ordenId(), elegida.lineaId());
                    salida.println("Línea lista: " + elegida.plato() + " (" + elegida.referenciaOrden() + ").");
                }
            } catch (ReglaDeNegocioException e) {
                salida.println("No se pudo completar la operación: " + e.getMessage());
            }
        }
    }
    /** Permite escoger una orden abierta en esta sesión por número, o pegar un id completo. */
    private String elegirOrden() {
        if (ordenesDeLaSesion.isEmpty()) {
            salida.println("Aún no has abierto órdenes en esta sesión. Puedes pegar un id manualmente.");
        } else {
            salida.println("Órdenes abiertas en esta sesión:");
            for (int i = 0; i < ordenesDeLaSesion.size(); i++) {
                salida.println("  " + (i + 1) + ". " + ordenesDeLaSesion.get(i));
            }
        }
        String texto = leerLinea("Número de la lista o id de la orden: ");
        if (texto.isEmpty()) {
            salida.println("No se indicó ninguna orden.");
            return null;
        }
        try {
            int indice = Integer.parseInt(texto);
            if (indice >= 1 && indice <= ordenesDeLaSesion.size()) {
                return ordenesDeLaSesion.get(indice - 1);
            }
        } catch (NumberFormatException e) {
            // no era un número: se toma como id completo
        }
        return texto;
    }


    // ---------------------------------------------------------------- utilidades de lectura
    private String leerLinea(String mensaje) {
        salida.print(mensaje);
        salida.flush();
        return entrada.nextLine().trim();
    }

    /** @return el número leído, o null si el formato es inválido (ya se avisó al usuario). */
    private BigDecimal leerDecimal(String mensaje) {
        String texto = leerLinea(mensaje).replace(',', '.');
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            salida.println("Número no válido: \"" + texto + "\".");
            return null;
        }
    }

    /** @return el entero leído, o null si el formato es inválido (ya se avisó al usuario). */
    private Integer leerEntero(String mensaje) {
        String texto = leerLinea(mensaje);
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException e) {
            salida.println("Número entero no válido: \"" + texto + "\".");
            return null;
        }
    }

    /** Igual que leerDecimal, pero Enter vacío significa 0. */
    private BigDecimal leerDecimalOpcional(String mensaje) {
        String texto = leerLinea(mensaje).replace(',', '.');
        if (texto.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            salida.println("Número no válido: \"" + texto + "\".");
            return null;
        }
    }
}
