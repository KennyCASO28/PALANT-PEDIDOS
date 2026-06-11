package org.example.service;

import org.example.dao.PedidoDAO;
import org.example.dao.VendedorDAO;
import org.example.model.DetallePedido;
import org.example.utils.GeneradorCodigo;
// import org.example.dto.ConfiguracionPrendaDTO; // If needed, or pass individual fields if DTO not fully adopted yet

import java.util.List;

public class OrderService {

    private final PedidoDAO pedidoDAO;
    private final VendedorDAO vendedorDAO;

    public OrderService() {
        this(new PedidoDAO(), new VendedorDAO());
    }

    // Constructor for testing (Dependency Injection)
    public OrderService(PedidoDAO pedidoDAO, VendedorDAO vendedorDAO) {
        this.pedidoDAO = pedidoDAO;
        this.vendedorDAO = vendedorDAO;
    }

    /**
     * Lists all available sellers.
     */
    public List<String> getSellers() {
        return vendedorDAO.listarVendedores();
    }

    /**
     * Generates the next unique order code based on today's sequence.
     */
    public String generateNextOrderCode() {
        int secuenciaHoy = pedidoDAO.contarPedidosHoy() + 1;
        return GeneradorCodigo.generarCodigo(secuenciaHoy);
    }

    public boolean saveOrder(org.example.dto.OrderRequestDTO request) {
        return saveOrder(
                request.getClientName(),
                request.getSellerName(),
                request.getOrderCode(),
                request.getTipoPrenda() != null ? request.getTipoPrenda().getLabel() : null,
                request.getGender(),
                request.getSleeveInfo(),
                request.getNeckInfo(),
                request.isHasMesh(),
                request.isHasSocks(),
                request.isHasShirtCuffs(),
                request.isHasShortCuffs(),
                request.getDeliveryDate(),
                request.getPriority(),
                request.getRoster());
    }

    /**
     * Saves a complete order to the database.
     * returns true if successful, false otherwise.
     */
    public boolean saveOrder(String cliente, String vendedor, String codigo,
            String tipoPrendaLabel, String genero,
            String mangaCombinada, String cuello,
            boolean llevaMalla, boolean llevaMedias,
            boolean llevaPunoCamiseta, boolean llevaPunoShort,
            java.time.LocalDate fechaEntrega, String prioridad,
            List<DetallePedido> detalles) {

        // Business Validation
        if (cliente == null || cliente.trim().isEmpty())
            return false;
        if (vendedor == null || vendedor.trim().isEmpty())
            return false;
        if (codigo == null || codigo.trim().isEmpty())
            return false;
        if (tipoPrendaLabel == null || tipoPrendaLabel.isEmpty())
            return false;
        if (fechaEntrega == null)
            return false;

        return pedidoDAO.guardarPedidoCompleto(
                cliente,
                vendedor,
                codigo,
                tipoPrendaLabel,
                genero,
                mangaCombinada,
                cuello,
                llevaMalla,
                llevaMedias,
                llevaPunoCamiseta,
                llevaPunoShort,
                fechaEntrega,
                prioridad,
                detalles);
    }
}

