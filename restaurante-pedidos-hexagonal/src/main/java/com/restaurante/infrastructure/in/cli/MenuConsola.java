package com.restaurante.infrastructure.in.cli;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.restaurante.application.port.in.AdministrarMenuUseCase;
import com.restaurante.application.port.in.CobrarOrdenUseCase;
import com.restaurante.application.port.in.GestionarCocinaUseCase;
import com.restaurante.application.port.in.GestionarCocinaUseCase.LineaEnCocina;
import com.restaurante.application.port.in.GestionarOrdenUseCase;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.MetodoPago;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.ModificadorAplicado;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Plato;
import com.restaurante.domain.ReglaDeNegocioException;
import com.restaurante.domain.TipoModificador;
import com.restaurante.domain.TipoOrden;
import com.restaurante.domain.Totales;

/**
 * Adaptador de entrada (consola interactiva).
 *
 * Solo conoce los PUERTOS DE ENTRADA (casos de uso) y el dominio; nunca los servicios
 * concretos ni los repositorios. Quien lo arma y le pasa los casos de uso es {@link Main}.
 * Las opciones se agrupan por actor: administrador, mesero, cocinero y cajero.
 */
public class MenuConsola {

    private final AdministrarMenuUseCase administrarMenu;
    private final GestionarOrdenUseCase gestionarOrden;
    private final GestionarCocinaUseCase gestionarCocina;
    private final CobrarOrdenUseCase cobrarOrden;
    private final Scanner entrada;
    private final PrintStream salida;

    public MenuConsola(AdministrarMenuUseCase administrarMenu,
                       GestionarOrdenUseCase gestionarOrden,
                       GestionarCocinaUseCase gestionarCocina,
                       CobrarOrdenUseCase cobrarOrden,
                       Scanner entrada,
                       PrintStream salida) {
        this.administrarMenu = administrarMenu;
        this.gestionarOrden = gestionarOrden;
        this.gestionarCocina = gestionarCocina;
        this.cobrarOrden = cobrarOrden;
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
        salida.println("------------------- MENÚ -------------------");
        salida.println(" Administrador");
        salida.println("   1. Registrar un plato");
        salida.println("   2. Registrar un modificador (extra o exclusión)");
        salida.println("   3. Consultar el menú");
        salida.println(" Mesero");
        salida.println("   4. Abrir una nueva orden");
        salida.println("   5. Agregar una línea a una orden");
        salida.println("   6. Modificar una línea de una orden");
        salida.println("   7. Cancelar una línea de una orden");
        salida.println("   8. Cancelar una orden completa");
        salida.println("   9. Ver el detalle de una orden");
        salida.println(" Cocinero");
        salida.println("  10. Ver pedidos pendientes y actualizar su estado");
        salida.println(" Cajero");
        salida.println("  11. Consultar los totales de una orden");
        salida.println("  12. Cerrar una orden y pagar");
        salida.println();
        salida.println("   0. Salir");
        salida.println("--------------------------------------------");
    }

    /** @return false cuando hay que salir. */
    private boolean ejecutar(String opcion) {
        try {
            switch (opcion) {
                case "1" -> registrarPlato();
                case "2" -> registrarModificador();
                case "3" -> consultarMenu();
                case "4" -> abrirNuevaOrden();
                case "5" -> agregarLineaAOrden();
                case "6" -> modificarLineaDeOrden();
                case "7" -> cancelarLineaDeOrden();
                case "8" -> cancelarOrdenCompleta();
                case "9" -> verDetalleDeOrden();
                case "10" -> gestionarCocina();
                case "11" -> consultarTotales();
                case "12" -> cobrarYCerrarOrden();
                case "0" -> {
                    return false;
                }
                default -> salida.println("Opción no válida. Escribe un número del 0 al 12.");
            }
        } catch (ReglaDeNegocioException e) {
            salida.println("No se pudo completar la operación: " + e.getMessage());
        }
        return true;
    }

    // ================================================================ ADMINISTRADOR

    private void registrarPlato() {
        salida.println();
        salida.println("-- Registrar plato --");
        String id = leerLinea("Id del plato (ej. P-001): ");
        String nombre = leerLinea("Nombre: ");
        BigDecimal precio = leerDecimal("Precio base (ej. 85.50): ");
        if (precio == null) {
            return; // ya se informó el error de formato
        }
        BigDecimal tasa = leerDecimal("Tasa de impuesto como fracción (0.16 = 16 %): ");
        if (tasa == null) {
            return;
        }

        Plato registrado = administrarMenu.registrarPlato(new Plato(id, nombre, precio, tasa));
        salida.println("Plato registrado: " + registrado.id() + " - " + registrado.nombre());
    }

    private void registrarModificador() {
        salida.println();
        salida.println("-- Registrar modificador --");
        String id = leerLinea("Id del modificador (ej. M-001): ");
        String nombre = leerLinea("Nombre (ej. Queso extra, Sin cebolla): ");
        salida.println("Tipo:");
        salida.println("  1. EXTRA (ingrediente adicional)");
        salida.println("  2. EXCLUSION (quitar un ingrediente, sin costo)");
        TipoModificador tipo = switch (leerLinea("Elige el tipo: ")) {
            case "1" -> TipoModificador.EXTRA;
            case "2" -> TipoModificador.EXCLUSION;
            default -> null;
        };
        if (tipo == null) {
            salida.println("Tipo de modificador no válido.");
            return;
        }
        BigDecimal costo = BigDecimal.ZERO;
        if (tipo == TipoModificador.EXTRA) {
            costo = leerDecimalOpcional("Costo extra (Enter para 0): ");
            if (costo == null) {
                return;
            }
        }

        Modificador registrado = administrarMenu.registrarModificador(new Modificador(id, nombre, tipo, costo));
        salida.println("Modificador registrado: " + registrado.id() + " - " + registrado.nombre()
                + " (" + registrado.tipo() + ")");
    }

    private void consultarMenu() {
        List<Plato> platos = administrarMenu.listarPlatos();
        salida.println();
        salida.println("-- Platos --");
        if (platos.isEmpty()) {
            salida.println("(No hay platos. Usa la opción 1 para registrarlos.)");
        } else {
            salida.printf("%-10s %-30s %12s %10s%n", "ID", "NOMBRE", "PRECIO", "IMPUESTO");
            for (Plato p : platos) {
                salida.printf("%-10s %-30s %12s %9s%%%n",
                        p.id(), p.nombre(), p.precioBase().toPlainString(),
                        p.tasaImpuesto().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
            }
        }

        List<Modificador> modificadores = administrarMenu.listarModificadores();
        salida.println();
        salida.println("-- Modificadores --");
        if (modificadores.isEmpty()) {
            salida.println("(No hay modificadores. Usa la opción 2 para registrarlos.)");
        } else {
            salida.printf("%-10s %-30s %-10s %12s%n", "ID", "NOMBRE", "TIPO", "COSTO EXTRA");
            for (Modificador m : modificadores) {
                salida.printf("%-10s %-30s %-10s %12s%n",
                        m.id(), m.nombre(), m.tipo(), m.costoExtra().toPlainString());
            }
        }
    }

    // ================================================================ MESERO

    private void abrirNuevaOrden() {
        salida.println();
        salida.println("-- Abrir orden --");
        salida.println("Tipo de orden:");
        salida.println("  1. SALA");
        salida.println("  2. PARA_LLEVAR");
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

        Orden orden = gestionarOrden.abrir(tipo, referencia);
        salida.println("Orden abierta.");
        salida.println("  Id:         " + orden.getId());
        salida.println("  Tipo:       " + orden.getTipo());
        salida.println("  Referencia: " + orden.getReferencia());
        salida.println("  Estado:     " + orden.getEstado());
    }

    private void agregarLineaAOrden() {
        salida.println();
        salida.println("-- Agregar línea a una orden --");
        List<Plato> platos = administrarMenu.listarPlatos();
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
        List<String> modificadorIds = elegirModificadores();
        if (modificadorIds == null) {
            return;
        }

        Orden orden = gestionarOrden.agregarLinea(ordenId, platos.get(numeroPlato - 1).id(), cantidad, modificadorIds);
        salida.println("Línea agregada.");
        imprimirLineas(orden);
    }

    private void modificarLineaDeOrden() {
        salida.println();
        salida.println("-- Modificar una línea --");
        String ordenId = elegirOrden();
        if (ordenId == null) {
            return;
        }
        LineaPedido linea = elegirLineaEditable(gestionarOrden.consultar(ordenId), "modificar");
        if (linea == null) {
            return;
        }
        Integer cantidad = leerEntero("Nueva cantidad (actual " + linea.getCantidad() + "): ");
        if (cantidad == null) {
            return;
        }
        salida.println("Los modificadores elegidos reemplazan a los actuales ("
                + describirModificadores(linea.getModificadores()) + ").");
        List<String> modificadorIds = elegirModificadores();
        if (modificadorIds == null) {
            return;
        }

        Orden orden = gestionarOrden.modificarLinea(ordenId, linea.getId(), cantidad, modificadorIds);
        salida.println("Línea modificada.");
        imprimirLineas(orden);
    }

    private void cancelarLineaDeOrden() {
        salida.println();
        salida.println("-- Cancelar una línea --");
        String ordenId = elegirOrden();
        if (ordenId == null) {
            return;
        }
        LineaPedido linea = elegirLineaEditable(gestionarOrden.consultar(ordenId), "cancelar");
        if (linea == null) {
            return;
        }

        Orden orden = gestionarOrden.cancelarLinea(ordenId, linea.getId());
        salida.println("Línea cancelada: " + linea.getPlato().nombre() + " x" + linea.getCantidad() + ".");
        imprimirLineas(orden);
    }

    private void cancelarOrdenCompleta() {
        salida.println();
        salida.println("-- Cancelar una orden completa --");
        String ordenId = elegirOrden();
        if (ordenId == null) {
            return;
        }
        String confirmacion = leerLinea("¿Seguro que quieres cancelar la orden? (s/n): ");
        if (!confirmacion.equalsIgnoreCase("s")) {
            salida.println("No se canceló la orden.");
            return;
        }
        Orden orden = gestionarOrden.cancelarOrden(ordenId);
        salida.println("Orden " + orden.getReferencia() + " cancelada.");
    }

    private void verDetalleDeOrden() {
        salida.println();
        salida.println("-- Detalle de una orden --");
        String ordenId = elegirOrden(gestionarOrden.listar(), "Órdenes");
        if (ordenId == null) {
            return;
        }
        Orden orden = gestionarOrden.consultar(ordenId);
        salida.println("  Id:         " + orden.getId());
        salida.println("  Tipo:       " + orden.getTipo());
        salida.println("  Referencia: " + orden.getReferencia());
        salida.println("  Creada:     " + orden.getFechaCreacion().withNano(0));
        salida.println("  Estado:     " + orden.getEstado());
        imprimirLineas(orden);
        imprimirTotales(orden.getTotales().orElseGet(orden::calcularTotales));
        if (!orden.getPagos().isEmpty()) {
            salida.println("  Pagos:");
            for (Pago p : orden.getPagos()) {
                salida.println("    - " + p.metodo() + " " + p.monto().toPlainString() + " [" + p.estado() + "] ref "
                        + p.referencia());
            }
        }
    }

    // ================================================================ COCINERO

    private void gestionarCocina() {
        while (true) {
            List<LineaEnCocina> pendientes = gestionarCocina.listarPendientes();
            salida.println();
            salida.println("-- Pedidos pendientes en cocina --");
            if (pendientes.isEmpty()) {
                salida.println("No hay líneas pendientes.");
                return;
            }
            salida.printf("%-3s %-14s %-22s %-5s %-11s %s%n", "#", "ORDEN", "PLATO", "CANT", "ESTADO", "MODIFICADORES");
            for (int i = 0; i < pendientes.size(); i++) {
                LineaEnCocina l = pendientes.get(i);
                salida.printf("%-3d %-14s %-22s %-5d %-11s %s%n",
                        i + 1, l.referenciaOrden(), l.plato(), l.cantidad(), l.estado(),
                        l.modificadores().isEmpty() ? "-" : String.join(", ", l.modificadores()));
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
            LineaEnCocina elegida = pendientes.get(numero - 1);
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

    // ================================================================ CAJERO

    private void consultarTotales() {
        salida.println();
        salida.println("-- Totales de una orden --");
        String ordenId = elegirOrden();
        if (ordenId == null) {
            return;
        }
        BigDecimal descuento = leerDecimalOpcional("Descuento en monto (Enter para 0): ");
        if (descuento == null) {
            return;
        }
        BigDecimal propina = leerDecimalOpcional("Propina (Enter para 0): ");
        if (propina == null) {
            return;
        }

        imprimirTotales(cobrarOrden.calcularTotales(ordenId, descuento, propina));
    }

    private void cobrarYCerrarOrden() {
        salida.println();
        salida.println("-- Cerrar orden y pagar --");
        String ordenId = elegirOrden();
        if (ordenId == null) {
            return;
        }
        BigDecimal descuento = leerDecimalOpcional("Descuento en monto (Enter para 0): ");
        if (descuento == null) {
            return;
        }
        BigDecimal propina = leerDecimalOpcional("Propina (Enter para 0): ");
        if (propina == null) {
            return;
        }
        imprimirTotales(cobrarOrden.calcularTotales(ordenId, descuento, propina));

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

        Pago pago = cobrarOrden.cobrar(ordenId, metodo, descuento, propina);
        if (pago.esAprobado()) {
            salida.println("Pago APROBADO (" + pago.metodo() + ") por " + pago.monto().toPlainString()
                    + ", referencia " + pago.referencia() + ". La orden quedó CERRADA.");
        } else {
            salida.println("Pago " + pago.estado() + " (" + pago.metodo() + ") por " + pago.monto().toPlainString()
                    + ". La orden sigue ABIERTA; intenta con otro método.");
        }
    }

    // ================================================================ utilidades de selección

    /** Permite escoger una orden abierta por número, o pegar un id completo. */
    private String elegirOrden() {
        return elegirOrden(gestionarOrden.listarAbiertas(), "Órdenes abiertas");
    }

    private String elegirOrden(List<Orden> ordenes, String titulo) {
        if (ordenes.isEmpty()) {
            salida.println("No hay órdenes para mostrar. Puedes pegar el id de una orden manualmente.");
        } else {
            salida.println(titulo + ":");
            for (int i = 0; i < ordenes.size(); i++) {
                Orden o = ordenes.get(i);
                salida.println("  " + (i + 1) + ". " + o.getReferencia() + " (" + o.getTipo() + ", "
                        + o.getLineasActivas().size() + " líneas, " + o.getEstado() + ") - " + o.getId());
            }
        }
        String texto = leerLinea("Número de la lista o id de la orden: ");
        if (texto.isEmpty()) {
            salida.println("No se indicó ninguna orden.");
            return null;
        }
        try {
            int indice = Integer.parseInt(texto);
            if (indice >= 1 && indice <= ordenes.size()) {
                return ordenes.get(indice - 1).getId();
            }
        } catch (NumberFormatException e) {
            // no era un número: se toma como id completo
        }
        return texto;
    }

    /** Muestra las líneas PENDIENTES (las únicas que se pueden modificar o cancelar) y deja escoger una. */
    private LineaPedido elegirLineaEditable(Orden orden, String accion) {
        List<LineaPedido> editables = orden.getLineasActivas().stream().filter(LineaPedido::esEditable).toList();
        if (editables.isEmpty()) {
            salida.println("La orden no tiene líneas que se puedan " + accion
                    + " (solo se permite con líneas PENDIENTES en cocina).");
            return null;
        }
        salida.println("Líneas que se pueden " + accion + ":");
        for (int i = 0; i < editables.size(); i++) {
            LineaPedido l = editables.get(i);
            salida.println("  " + (i + 1) + ". " + l.getPlato().nombre() + " x" + l.getCantidad()
                    + " [" + describirModificadores(l.getModificadores()) + "]");
        }
        Integer numero = leerEntero("Número de la línea: ");
        if (numero == null) {
            return null;
        }
        if (numero < 1 || numero > editables.size()) {
            salida.println("Número de línea no válido.");
            return null;
        }
        return editables.get(numero - 1);
    }

    /** @return ids elegidos (vacío si no se elige ninguno), o null si la entrada no es válida. */
    private List<String> elegirModificadores() {
        List<Modificador> modificadores = administrarMenu.listarModificadores();
        if (modificadores.isEmpty()) {
            return List.of();
        }
        salida.println("Modificadores disponibles:");
        for (int i = 0; i < modificadores.size(); i++) {
            Modificador m = modificadores.get(i);
            salida.println("  " + (i + 1) + ". " + m.nombre() + " (" + m.tipo() + ", +"
                    + m.costoExtra().toPlainString() + ")");
        }
        String texto = leerLinea("Números separados por coma (ej. 1,3) o Enter para ninguno: ");
        if (texto.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String parte : texto.split(",")) {
            try {
                int numero = Integer.parseInt(parte.trim());
                if (numero < 1 || numero > modificadores.size()) {
                    salida.println("Número de modificador no válido: " + numero + ".");
                    return null;
                }
                ids.add(modificadores.get(numero - 1).id());
            } catch (NumberFormatException e) {
                salida.println("Número no válido: \"" + parte.trim() + "\".");
                return null;
            }
        }
        return ids;
    }

    // ================================================================ utilidades de impresión

    private void imprimirLineas(Orden orden) {
        salida.println("  Líneas de la orden " + orden.getReferencia() + ":");
        if (orden.getLineas().isEmpty()) {
            salida.println("    (sin líneas)");
            return;
        }
        for (LineaPedido l : orden.getLineas()) {
            salida.println("    - " + l.getPlato().nombre() + " x" + l.getCantidad()
                    + " @ " + l.getPrecioUnitario().toPlainString()
                    + " [" + describirModificadores(l.getModificadores()) + "]"
                    + " " + l.getEstadoCocina());
        }
    }

    private void imprimirTotales(Totales t) {
        salida.printf("  %-10s %12s%n", "Subtotal:", t.subtotal().toPlainString());
        salida.printf("  %-10s %12s%n", "Descuento:", t.descuento().negate().toPlainString());
        salida.printf("  %-10s %12s%n", "Impuestos:", t.impuestos().toPlainString());
        salida.printf("  %-10s %12s%n", "Propina:", t.propina().toPlainString());
        salida.printf("  %-10s %12s%n", "Total:", t.total().toPlainString());
    }

    private static String describirModificadores(List<ModificadorAplicado> modificadores) {
        if (modificadores.isEmpty()) {
            return "sin modificadores";
        }
        return modificadores.stream()
                .map(m -> m.nombre() + (m.costoExtraAplicado().signum() > 0
                        ? " +" + m.costoExtraAplicado().toPlainString() : ""))
                .collect(Collectors.joining(", "));
    }

    // ================================================================ utilidades de lectura

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
