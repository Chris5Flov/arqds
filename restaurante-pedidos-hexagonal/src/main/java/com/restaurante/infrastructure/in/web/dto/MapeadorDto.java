package com.restaurante.infrastructure.in.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.restaurante.application.port.in.GestionarCocinaUseCase.LineaEnCocina;
import com.restaurante.domain.LineaPedido;
import com.restaurante.domain.Modificador;
import com.restaurante.domain.ModificadorAplicado;
import com.restaurante.domain.Orden;
import com.restaurante.domain.Pago;
import com.restaurante.domain.Plato;
import com.restaurante.domain.Totales;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.LineaCocinaDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.LineaDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.ModificadorDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.OrdenDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.PagoDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.PlatoDto;
import com.restaurante.infrastructure.in.web.dto.RespuestasDto.TotalesDto;

/** Convierte objetos del dominio en los DTO del contrato de la API. */
public final class MapeadorDto {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private MapeadorDto() {
    }

    public static PlatoDto aDto(Plato plato) {
        return new PlatoDto(plato.id(), plato.nombre(), dinero(plato.precioBase()), tasa(plato.tasaImpuesto()));
    }

    public static ModificadorDto aDto(Modificador modificador) {
        return new ModificadorDto(modificador.id(), modificador.nombre(), nombre(modificador.tipo()),
                dinero(modificador.costoExtra()));
    }

    /** Un modificador dentro de una línea lleva el costo extra con el que se aplicó, no el del catálogo. */
    public static ModificadorDto aDto(ModificadorAplicado aplicado) {
        Modificador m = aplicado.modificador();
        return new ModificadorDto(m.id(), m.nombre(), nombre(m.tipo()), dinero(aplicado.costoExtraAplicado()));
    }

    public static TotalesDto aDto(Totales t) {
        return new TotalesDto(dinero(t.subtotal()), dinero(t.descuento()), dinero(t.impuestos()),
                dinero(t.propina()), dinero(t.total()));
    }

    public static PagoDto aDto(Pago pago) {
        return new PagoDto(pago.id(), pago.ordenId(), fecha(pago.fecha()), nombre(pago.estado()),
                nombre(pago.metodo()), dinero(pago.monto()), pago.referencia());
    }

    public static LineaDto aDto(LineaPedido linea) {
        return new LineaDto(linea.getId(), linea.getPlato().id(), linea.getPlato().nombre(), linea.getCantidad(),
                dinero(linea.getPrecioUnitario()), tasa(linea.getTasaImpuestoAplicada()),
                linea.getModificadores().stream().map(MapeadorDto::aDto).toList(),
                nombre(linea.getEstadoCocina()), linea.esEditable());
    }

    /** Si la orden está cerrada lleva los totales cobrados; si no, los actuales sin descuento ni propina. */
    public static OrdenDto aDto(Orden orden) {
        Totales totales = orden.getTotales().orElseGet(orden::calcularTotales);
        return new OrdenDto(orden.getId(), nombre(orden.getTipo()), orden.getReferencia(), nombre(orden.getEstado()),
                fecha(orden.getFechaCreacion()),
                orden.getLineas().stream().map(MapeadorDto::aDto).toList(),
                aDto(totales),
                orden.getPagos().stream().map(MapeadorDto::aDto).toList());
    }

    public static LineaCocinaDto aDto(LineaEnCocina linea) {
        return new LineaCocinaDto(linea.ordenId(), linea.lineaId(), linea.referenciaOrden(), linea.plato(),
                linea.cantidad(), List.copyOf(linea.modificadores()), nombre(linea.estado()));
    }

    public static List<OrdenDto> ordenes(List<Orden> ordenes) {
        return ordenes.stream().map(MapeadorDto::aDto).toList();
    }

    // ---------------------------------------------------------------- formatos

    /** Montos siempre con 2 decimales: "85.00". */
    static String dinero(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Tasas como fracción sin ceros sobrantes: "0.16", "0". */
    static String tasa(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        return valor.signum() == 0 ? "0" : valor.stripTrailingZeros().toPlainString();
    }

    /** ISO local sin zona y siempre con segundos: "2026-10-20T13:45:00". */
    static String fecha(LocalDateTime valor) {
        return valor == null ? null : valor.format(FECHA);
    }

    private static String nombre(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }
}
