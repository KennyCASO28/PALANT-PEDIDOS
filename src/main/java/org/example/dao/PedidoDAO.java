package org.example.dao;

import org.example.model.DetallePedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

public class PedidoDAO {

    private static final Logger logger = LoggerFactory.getLogger(PedidoDAO.class);

    public PedidoDAO() {
        checkAndMigrateSchema();
    }

    private void checkAndMigrateSchema() {
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return;
            try (Statement stmt = conn.createStatement()) {
                // 1. Crear tabla CLIENTES si no existe
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS clientes (" +
                    "    id SERIAL PRIMARY KEY," +
                    "    nombre_institucion VARCHAR(150) NOT NULL UNIQUE," +
                    "    telefono VARCHAR(15)," +
                    "    ciudad VARCHAR(50) NOT NULL" +
                    ")"
                );

                // 2. Crear tabla USUARIOS si no existe
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS usuarios (" +
                    "    id SERIAL PRIMARY KEY," +
                    "    nombre_usuario VARCHAR(80) NOT NULL UNIQUE," +
                    "    contrasena_encriptada VARCHAR(255) NOT NULL," +
                    "    rol VARCHAR(20) NOT NULL CHECK (rol IN ('Vendedor', 'Diseñador', 'Jefe'))," +
                    "    nombre_completo VARCHAR(100) NOT NULL," +
                    "    activo BOOLEAN DEFAULT TRUE" +
                    ")"
                );

                // Insertar usuarios iniciales por defecto si está vacía
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.executeUpdate(
                            "INSERT INTO usuarios (nombre_usuario, contrasena_encriptada, rol, nombre_completo) VALUES " +
                            "('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Jefe', 'Administrador Principal'), " +
                            "('juan.perez', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Vendedor', 'Juan Pérez')"
                        );
                        logger.info("Inserted default admin and sales users into 'usuarios' table.");
                    } else {
                        // Actualizar a admin123 si tenían el hash antiguo ('admin')
                        stmt.executeUpdate(
                            "UPDATE usuarios SET contrasena_encriptada = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9' " +
                            "WHERE contrasena_encriptada = '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918'"
                        );
                    }
                }

                // 3. Crear tabla FICHA_TECNICA si no existe
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS ficha_tecnica (" +
                    "    id SERIAL PRIMARY KEY," +
                    "    pedido_id INTEGER REFERENCES pedidos(id) ON DELETE CASCADE UNIQUE," +
                    "    ruta_archivo_vectorial VARCHAR(500)," +
                    "    observaciones_sublimacion TEXT," +
                    "    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")"
                );

                // 4. Migrar tabla PEDIDOS si faltan las nuevas columnas
                // Check 'fecha_entrega'
                try {
                    stmt.executeQuery("SELECT fecha_entrega FROM pedidos LIMIT 1");
                } catch (SQLException e) {
                    logger.info("⚠️ Migrating DB: Adding 'fecha_entrega' column...");
                    stmt.executeUpdate("ALTER TABLE pedidos ADD COLUMN fecha_entrega DATE");
                }

                // Check 'prioridad'
                try {
                    stmt.executeQuery("SELECT prioridad FROM pedidos LIMIT 1");
                } catch (SQLException e) {
                    logger.info("⚠️ Migrating DB: Adding 'prioridad' column...");
                    stmt.executeUpdate("ALTER TABLE pedidos ADD COLUMN prioridad VARCHAR(50)");
                }

                // Check 'cliente_id'
                try {
                    stmt.executeQuery("SELECT cliente_id FROM pedidos LIMIT 1");
                } catch (SQLException e) {
                    logger.info("⚠️ Migrating DB: Adding 'cliente_id' column linked to 'clientes'...");
                    stmt.executeUpdate("ALTER TABLE pedidos ADD COLUMN cliente_id INTEGER REFERENCES clientes(id) ON DELETE SET NULL");
                }

                // Check 'vendedor_id'
                try {
                    stmt.executeQuery("SELECT vendedor_id FROM pedidos LIMIT 1");
                } catch (SQLException e) {
                    logger.info("⚠️ Migrating DB: Adding 'vendedor_id' column linked to 'usuarios'...");
                    stmt.executeUpdate("ALTER TABLE pedidos ADD COLUMN vendedor_id INTEGER REFERENCES usuarios(id) ON DELETE SET NULL");
                }
            }
        } catch (SQLException e) {
            logger.error("❌ Error durante la migración del esquema de base de datos: {}", e.getMessage(), e);
        }
    }

    // Método PRINCIPAL: Guarda Pedido + Lista de Jugadores (ACTUALIZADO FASE 1 & NORMALIZADO FASE 2)
    public boolean guardarPedidoCompleto(String cliente, String vendedor, String codigo,
            String tipoPrenda, String genero, String manga,
            String cuello, boolean llevaMalla,
            boolean tieneMedias, boolean punoCamiseta, boolean punoShort,
            java.time.LocalDate fechaEntrega, String prioridad,
            List<DetallePedido> listaJugadores) {

        String sqlInsert = "INSERT INTO pedidos (cliente_nombre, cliente_id, id_vendedor, vendedor_id, codigo_pedido, tipo_prenda, genero_corte, tipo_manga, codigo_cuello, tiene_malla, tiene_medias, puno_camiseta, puno_short, fecha_entrega, prioridad, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pendiente') RETURNING id";
        String sqlUpdate = "UPDATE pedidos SET cliente_nombre = ?, cliente_id = ?, id_vendedor = ?, vendedor_id = ?, tipo_prenda = ?, genero_corte = ?, tipo_manga = ?, codigo_cuello = ?, tiene_malla = ?, tiene_medias = ?, puno_camiseta = ?, puno_short = ?, fecha_entrega = ?, prioridad = ? WHERE id = ?";
        String sqlDetalle = "INSERT INTO detalle_nombres (pedido_id, nombre, numero, talla) VALUES (?, ?, ?, ?)";
        String sqlDeleteDetalle = "DELETE FROM detalle_nombres WHERE pedido_id = ?";
        String sqlCheck = "SELECT id FROM pedidos WHERE codigo_pedido = ?";

        Connection conn = null;
        PreparedStatement psPedido = null;
        PreparedStatement psDetalle = null;

        try {
            conn = ConexionDB.getConnection();
            if (conn == null)
                return false;

            // 1. INICIAR TRANSACCIÓN
            conn.setAutoCommit(false);

            // 2. RESOLVER RELACIONES (Clientes y Usuarios)
            Integer clienteId = null;
            if (cliente != null && !cliente.trim().isEmpty()) {
                clienteId = obtenerOInsertarCliente(conn, cliente.trim());
            }

            Integer vendedorId = null;

            // Priority 1: If a user is currently logged in, use their ID directly (most accurate)
            if (org.example.logic.SessionManager.isLoggedIn()) {
                org.example.dao.UsuarioDAO.Usuario sessionUser = org.example.logic.SessionManager.getCurrentUser();
                if (sessionUser != null) {
                    vendedorId = sessionUser.getId();
                }
            }

            // Priority 2: If no session user, try to find by vendor name string
            if (vendedorId == null && vendedor != null && !vendedor.trim().isEmpty()) {
                vendedorId = obtenerIdVendedor(conn, vendedor.trim());
            }

            // Priority 3: Fallback to ID=1 (admin) to satisfy NOT NULL constraint
            if (vendedorId == null) {
                vendedorId = 1;
            }

            // Check if order code already exists
            Integer existingOrderId = null;
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                psCheck.setString(1, codigo);
                try (ResultSet rsCheck = psCheck.executeQuery()) {
                    if (rsCheck.next()) {
                        existingOrderId = rsCheck.getInt("id");
                    }
                }
            }

            int idPedidoGenerado;

            if (existingOrderId != null) {
                // UPDATE
                idPedidoGenerado = existingOrderId;
                psPedido = conn.prepareStatement(sqlUpdate);
                psPedido.setString(1, cliente);
                if (clienteId != null) {
                    psPedido.setInt(2, clienteId);
                } else {
                    psPedido.setNull(2, java.sql.Types.INTEGER);
                }
                if (vendedorId != null) {
                    psPedido.setInt(3, vendedorId);
                    psPedido.setInt(4, vendedorId);
                } else {
                    psPedido.setNull(3, java.sql.Types.INTEGER);
                    psPedido.setNull(4, java.sql.Types.INTEGER);
                }
                psPedido.setString(5, tipoPrenda);
                psPedido.setString(6, genero);
                psPedido.setString(7, manga);
                psPedido.setString(8, cuello);
                psPedido.setBoolean(9, llevaMalla);
                psPedido.setBoolean(10, tieneMedias);
                psPedido.setBoolean(11, punoCamiseta);
                psPedido.setBoolean(12, punoShort);
                psPedido.setObject(13, fechaEntrega);
                psPedido.setString(14, prioridad);
                psPedido.setInt(15, idPedidoGenerado);
                psPedido.executeUpdate();

                // Delete old player details
                try (PreparedStatement psDel = conn.prepareStatement(sqlDeleteDetalle)) {
                    psDel.setInt(1, idPedidoGenerado);
                    psDel.executeUpdate();
                }
            } else {
                // INSERT
                psPedido = conn.prepareStatement(sqlInsert);
                psPedido.setString(1, cliente);
                if (clienteId != null) {
                    psPedido.setInt(2, clienteId);
                } else {
                    psPedido.setNull(2, java.sql.Types.INTEGER);
                }
                if (vendedorId != null) {
                    psPedido.setInt(3, vendedorId);
                    psPedido.setInt(4, vendedorId);
                } else {
                    psPedido.setNull(3, java.sql.Types.INTEGER);
                    psPedido.setNull(4, java.sql.Types.INTEGER);
                }
                psPedido.setString(5, codigo);
                psPedido.setString(6, tipoPrenda);
                psPedido.setString(7, genero);
                psPedido.setString(8, manga);
                psPedido.setString(9, cuello);
                psPedido.setBoolean(10, llevaMalla);
                psPedido.setBoolean(11, tieneMedias);
                psPedido.setBoolean(12, punoCamiseta);
                psPedido.setBoolean(13, punoShort);
                psPedido.setObject(14, fechaEntrega);
                psPedido.setString(15, prioridad);

                try (ResultSet rs = psPedido.executeQuery()) {
                    if (rs.next()) {
                        idPedidoGenerado = rs.getInt(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID del pedido generado.");
                    }
                }
            }

            // 4. GUARDAR LA LISTA DE JUGADORES
            psDetalle = conn.prepareStatement(sqlDetalle);

            for (DetallePedido jugador : listaJugadores) {
                psDetalle.setInt(1, idPedidoGenerado);
                psDetalle.setString(2, jugador.getNombre());
                psDetalle.setString(3, jugador.getNumero());
                psDetalle.setString(4, jugador.getTalla());
                psDetalle.addBatch();
            }

            psDetalle.executeBatch();

            // 5. CONFIRMAR TODO
            conn.commit();
            logger.info("✅ Pedido #{} guardado/actualizado con {} jugadores. Relación Clientes ID: {}, Vendedores ID: {}", 
                idPedidoGenerado, listaJugadores.size(), clienteId, vendedorId);
            return true;

        } catch (SQLException e) {
            logger.error("❌ Error crítico en guardarPedidoCompleto (Rolling back): {}", e.getMessage(), e);
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                logger.error("Error al realizar rollback de la transacción: {}", ex.getMessage(), ex);
            }
            return false;
        } finally {
            try {
                if (psPedido != null) psPedido.close();
                if (psDetalle != null) psDetalle.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.error("Error al cerrar recursos en guardarPedidoCompleto: {}", e.getMessage());
            }
        }
    }

    private Integer obtenerOInsertarCliente(Connection conn, String nombreInstitucion) throws SQLException {
        String querySelect = "SELECT id FROM clientes WHERE nombre_institucion = ?";
        try (PreparedStatement ps = conn.prepareStatement(querySelect)) {
            ps.setString(1, nombreInstitucion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertClient = "INSERT INTO clientes (nombre_institucion, telefono, ciudad) VALUES (?, 'S/N', 'Por definir') RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(insertClient)) {
            ps.setString(1, nombreInstitucion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    private Integer obtenerIdVendedor(Connection conn, String vendedorStr) throws SQLException {
        String querySelect = "SELECT id FROM usuarios WHERE LOWER(nombre_completo) = LOWER(?) OR LOWER(nombre_usuario) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(querySelect)) {
            ps.setString(1, vendedorStr.trim());
            ps.setString(2, vendedorStr.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    public int contarPedidosHoy() {
        String sql = "SELECT COUNT(*) FROM pedidos WHERE DATE(fecha_creacion) = CURRENT_DATE";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return 0;
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.info("Info: No pude contar pedidos ({})", e.getMessage());
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
            logger.info("Info: Error al contar pedidos por fecha entrega ({})", e.getMessage());
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
                logger.error("Error al cerrar conexión: {}", e.getMessage(), e);
            }
        }
        return 0;
    }

    public static class PedidoRecord {
        private final int id;
        private final String codigoPedido;
        private final String clienteNombre;
        private final String vendedor;
        private final String tipoPrenda;
        private final String prioridad;
        private final String estado;
        private final java.time.LocalDate fechaEntrega;

        public PedidoRecord(int id, String codigoPedido, String clienteNombre, String vendedor,
                            String tipoPrenda, String prioridad, String estado, java.time.LocalDate fechaEntrega) {
            this.id = id;
            this.codigoPedido = codigoPedido;
            this.clienteNombre = clienteNombre;
            this.vendedor = vendedor;
            this.tipoPrenda = tipoPrenda;
            this.prioridad = prioridad;
            this.estado = estado;
            this.fechaEntrega = fechaEntrega;
        }

        public int getId() { return id; }
        public String getCodigoPedido() { return codigoPedido; }
        public String getClienteNombre() { return clienteNombre; }
        public String getVendedor() { return vendedor; }
        public String getTipoPrenda() { return tipoPrenda; }
        public String getPrioridad() { return prioridad; }
        public String getEstado() { return estado; }
        public java.time.LocalDate getFechaEntrega() { return fechaEntrega; }
    }

    public java.util.List<PedidoRecord> obtenerHistorialPorVendedor(int vendedorId) {
        java.util.List<PedidoRecord> lista = new java.util.ArrayList<>();
        String sql = "SELECT p.id, p.codigo_pedido, p.cliente_nombre, u.nombre_completo AS vendedor_nombre, " +
                     "p.tipo_prenda, p.prioridad, p.estado, p.fecha_entrega " +
                     "FROM pedidos p " +
                     "LEFT JOIN usuarios u ON p.id_vendedor = u.id " +
                     "WHERE p.id_vendedor = ? ORDER BY p.fecha_creacion DESC";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, vendedorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.sql.Date fEntrega = rs.getDate("fecha_entrega");
                        lista.add(new PedidoRecord(
                            rs.getInt("id"),
                            rs.getString("codigo_pedido"),
                            rs.getString("cliente_nombre"),
                            rs.getString("vendedor_nombre"),
                            rs.getString("tipo_prenda"),
                            rs.getString("prioridad"),
                            rs.getString("estado"),
                            fEntrega != null ? fEntrega.toLocalDate() : null
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener historial de pedidos del vendedor ID {}: {}", vendedorId, e.getMessage(), e);
        }
        return lista;
    }

    public java.util.List<PedidoRecord> obtenerHistorialCompleto() {
        java.util.List<PedidoRecord> lista = new java.util.ArrayList<>();
        String sql = "SELECT p.id, p.codigo_pedido, p.cliente_nombre, u.nombre_completo AS vendedor_nombre, " +
                     "p.tipo_prenda, p.prioridad, p.estado, p.fecha_entrega " +
                     "FROM pedidos p " +
                     "LEFT JOIN usuarios u ON p.id_vendedor = u.id " +
                     "ORDER BY p.fecha_creacion DESC";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date fEntrega = rs.getDate("fecha_entrega");
                    lista.add(new PedidoRecord(
                        rs.getInt("id"),
                        rs.getString("codigo_pedido"),
                        rs.getString("cliente_nombre"),
                        rs.getString("vendedor_nombre"),
                        rs.getString("tipo_prenda"),
                        rs.getString("prioridad"),
                        rs.getString("estado"),
                        fEntrega != null ? fEntrega.toLocalDate() : null
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener historial completo de pedidos: {}", e.getMessage(), e);
        }
        return lista;
    }

    public boolean actualizarEstadoPedido(int pedidoId, String nuevoEstado) {
        String sql = "UPDATE pedidos SET estado = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nuevoEstado);
                ps.setInt(2, pedidoId);
                int rows = ps.executeUpdate();
                logger.info("Estado del pedido ID {} actualizado a: {}", pedidoId, nuevoEstado);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar estado del pedido ID {}: {}", pedidoId, e.getMessage(), e);
            return false;
        }
    }

    public java.util.List<org.example.model.DetallePedido> obtenerDetalleJugadores(int pedidoId) {
        java.util.List<org.example.model.DetallePedido> lista = new java.util.ArrayList<>();
        String sql = "SELECT nombre, numero, talla FROM detalle_nombres WHERE pedido_id = ? ORDER BY id";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, pedidoId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new org.example.model.DetallePedido(
                            rs.getString("nombre"),
                            rs.getString("numero"),
                            rs.getString("talla")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener detalle de jugadores para el pedido ID {}: {}", pedidoId, e.getMessage(), e);
        }
        return lista;
    }
}
