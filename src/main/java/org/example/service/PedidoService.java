package org.example.service;

import org.example.dao.PedidoDAO;

/**
 * Service for managing order business logic.
 * Handles validation, processing, and persistence of orders.
 */
public class PedidoService {

    private final PedidoDAO pedidoDAO;

    public PedidoService() {
        this.pedidoDAO = new PedidoDAO();
    }

    /**
     * Gets the count of orders created today.
     * 
     * @return Number of orders today
     */
    public int contarPedidosHoy() {
        try {
            return pedidoDAO.contarPedidosHoy();
        } catch (Exception e) {
            System.err.println("❌ Error al contar pedidos: " + e.getMessage());
            return 0;
        }
    }

}

