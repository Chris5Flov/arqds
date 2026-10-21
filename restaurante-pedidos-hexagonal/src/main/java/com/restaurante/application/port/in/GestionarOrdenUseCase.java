package com.restaurante.application.port.in;

import com.restaurante.domain.Orden;
import com.restaurante.domain.TipoOrden;
import java.util.List;

/** RF2 y RF3: el mesero abre órdenes y agrega, modifica o cancela sus líneas. */
public interface GestionarOrdenUseCase {

    /** RF2 */
    Orden abrir(TipoOrden tipo, String referencia);

    /** RF3: los modificadores se indican por id y se buscan en el catálogo. */
    Orden agregarLinea(String ordenId, String platoId, int cantidad, List<String> modificadorIds);

    /** RF3: solo si la línea sigue PENDIENTE en cocina. */
    Orden modificarLinea(String ordenId, String lineaId, int cantidad, List<String> modificadorIds);

    /** RF3: solo si la línea sigue PENDIENTE en cocina. */
    Orden cancelarLinea(String ordenId, String lineaId);

    /** Cancela la orden completa si ninguna línea ha entrado a cocina. */
    Orden cancelarOrden(String ordenId);

    Orden consultar(String ordenId);

    /** Todas las órdenes (abiertas, cerradas y canceladas), de la más antigua a la más reciente. */
    List<Orden> listar();

    List<Orden> listarAbiertas();
}
