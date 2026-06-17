package org.example.service.save;

import org.example.dao.ConexionDB;
import org.example.dao.PedidoDAO;
import org.example.model.DetallePedido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderSaveAndUpdateIntegrationTest {

    private static final String TEST_CODE = "TEST-SAVE-UPDATE-999";

    @AfterEach
    public void tearDown() {
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM detalle_nombres WHERE pedido_id IN (SELECT id FROM pedidos WHERE codigo_pedido = ?)")) {
                    ps.setString(1, TEST_CODE);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM pedidos WHERE codigo_pedido = ?")) {
                    ps.setString(1, TEST_CODE);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up integration test order: " + e.getMessage());
        }
    }

    @Test
    public void testInsertAndThenUpdateOrderDirectly() {
        System.out.println("=== PROBANDO GUARDAR PEDIDO (INSERT Y LUEGO UPDATE) ===");
        PedidoDAO dao = new PedidoDAO();

        // 1. Initial Insert
        List<DetallePedido> players1 = new ArrayList<>();
        players1.add(new DetallePedido("Player A", "10", "M"));
        players1.add(new DetallePedido("Player B", "7", "L"));

        boolean insertResult = dao.guardarPedidoCompleto(
                "Cliente Test S.A.C.",
                "admin",
                TEST_CODE,
                "Camiseta",
                "HOMBRE",
                "Manga Corta",
                "Cuello V",
                false,
                false,
                false,
                false,
                LocalDate.now().plusDays(10),
                "Normal",
                players1
        );

        assertTrue(insertResult, "The initial order INSERT should be successful");
        System.out.println("✅ Inserción inicial exitosa!");

        // 2. Update existing order with the same code
        List<DetallePedido> players2 = new ArrayList<>();
        players2.add(new DetallePedido("Player Updated", "99", "XL"));

        boolean updateResult = dao.guardarPedidoCompleto(
                "Cliente Test SAC - Modificado",
                "admin",
                TEST_CODE,
                "Camiseta",
                "HOMBRE",
                "Manga Larga",
                "Cuello V",
                true,
                true,
                true,
                true,
                LocalDate.now().plusDays(12),
                "Urgente",
                players2
        );

        assertTrue(updateResult, "The order UPDATE should be successful when saving with the same order code");
        System.out.println("✅ Actualización exitosa sobre el mismo código!");
    }
}
