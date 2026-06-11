package org.example.dao;

import org.example.model.DetallePedido;
import java.sql.*;
import java.util.List;

public class PedidoDAO {

    public PedidoDAO() {
        checkAndMigrateSchema();
    }

    private void checkAndMigrateSchema() {
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return;
            try (Statement stmt = conn.createStatement()) {
                // 1. Check if 'fecha_entrega' exists
                try {
                    stmt.executeQuery("SELECT fecha_entrega FROM pedidos LIMIT 1");
                } catch (SQLException e) {
                    System.out.println("⚠️ Migrating DB: Adding 'fecha_entrega' column...");
                    stmt.executeUpdate("ALTER TABLE pedidos ADD COLUMN fecha_entrega DATE");
                }

                // 2. Check if 'prioridad' exists
                try {
                    stmt.executeQuery("SELECT prioridad FROM pedidos LIMIT 1");
                } catch (SQLException e) {
                    System.out.println("⚠️ Migrating DB: Adding 'prioridad' column...");
                    stmt.executeUpdate("ALTER TABLE pedidos ADD COLUMN prioridad VARCHAR(50)");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método PRINCIPAL: Guarda Pedido + Lista de Jugadores (ACTUALIZADO FASE 1)
    public boolean guardarPedidoCompleto(String cliente, String vendedor, String codigo,
            String tipoPrenda, String genero, String manga,
            String cuello, boolean llevaMalla,
            boolean tieneMedias, boolean punoCamiseta, boolean punoShort,
            java.time.LocalDate fechaEntrega, String prioridad,
            List<DetallePedido> listaJugadores) {

        String sqlPedido = "INSERT INTO pedidos (cliente_nombre, vendedor, codigo_pedido, tipo_prenda, genero_corte, tipo_manga, codigo_cuello, tiene_malla, tiene_medias, puno_camiseta, puno_short, fecha_entrega, prioridad, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pendiente') RETURNING id";
        String sqlDetalle = "INSERT INTO detalle_nombres (pedido_id, nombre, numero, talla) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement psPedido = null;
        PreparedStatement psDetalle = null;

        try {
            conn = ConexionDB.getConnection();
            if (conn == null)
                return false;

            // 1. INICIAR TRANSACCIÓN (Desactivar guardado automático)
            conn.setAutoCommit(false);

            // 2. GUARDAR CABECERA DEL PEDIDO
            psPedido = conn.prepareStatement(sqlPedido);
            psPedido.setString(1, cliente);
            psPedido.setString(2, vendedor);
            psPedido.setString(3, codigo);
            psPedido.setString(4, tipoPrenda);
            psPedido.setString(5, genero);
            psPedido.setString(6, manga);
            psPedido.setString(7, cuello);
            psPedido.setBoolean(8, llevaMalla);
            // Nuevos seteos
            psPedido.setBoolean(9, tieneMedias);
            psPedido.setBoolean(10, punoCamiseta);
            psPedido.setBoolean(11, punoShort);
            psPedido.setObject(12, fechaEntrega);
            psPedido.setString(13, prioridad);

            ResultSet rs = psPedido.executeQuery();

            if (rs.next()) {
                int idPedidoGenerado = rs.getInt(1); // ¡Aquí capturamos el ID nuevo!

                // 3. GUARDAR LA LISTA DE JUGADORES
                psDetalle = conn.prepareStatement(sqlDetalle);

                for (DetallePedido jugador : listaJugadores) {
                    psDetalle.setInt(1, idPedidoGenerado); // Vinculamos al padre
                    psDetalle.setString(2, jugador.getNombre());
                    psDetalle.setString(3, jugador.getNumero());
                    psDetalle.setString(4, jugador.getTalla());
                    psDetalle.addBatch(); // Añadir al lote
                }

                psDetalle.executeBatch(); // Ejecutar todo el lote de una

                // 4. CONFIRMAR TODO (COMMIT)
                conn.commit();
                System.out.println(
                        "✅ Pedido #" + idPedidoGenerado + " guardado con " + listaJugadores.size() + " items.");
                return true;
            }

        } catch (SQLException e) {
            System.out.println("❌ Error CRÍTICO (Rolling back): " + e.getMessage());
            try {
                if (conn != null)
                    conn.rollback(); // Si falla algo, deshacemos todo
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            // Cerrar recursos manualmente
            try {
                if (psPedido != null)
                    psPedido.close();
                if (psDetalle != null)
                    psDetalle.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // Tu método auxiliar para contar (sin cambios)
    public int contarPedidosHoy() {
        String sql = "SELECT COUNT(*) FROM pedidos WHERE DATE(fecha_creacion) = CURRENT_DATE";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return 0;
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Info: No pude contar pedidos (" + e.getMessage() + ")");
        }
        return 0;
    }

    public int contarPedidosPorFechaEntrega(java.time.LocalDate fecha) {
        String sql = "SELECT COUNT(*) FROM pedidos WHERE date(fecha_entrega) = ?";
        Connection conn = ConexionDB.getConnection();
        if (conn == null) {
            return 0;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, fecha);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Info: Error al contar pedidos por fecha entrega (" + e.getMessage() + ")");
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
        return 0;
    }
}
