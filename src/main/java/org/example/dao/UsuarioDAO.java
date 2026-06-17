package org.example.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioDAO.class);

    public static class Usuario {
        private int id;
        private String nombreUsuario;
        private String rol;
        private String nombreCompleto;
        private boolean activo;

        public Usuario(int id, String nombreUsuario, String rol, String nombreCompleto, boolean activo) {
            this.id = id;
            this.nombreUsuario = nombreUsuario;
            this.rol = rol;
            this.nombreCompleto = nombreCompleto;
            this.activo = activo;
        }

        public int getId() { return id; }
        public String getNombreUsuario() { return nombreUsuario; }
        public String getRol() { return rol; }
        public String getNombreCompleto() { return nombreCompleto; }
        public boolean isActivo() { return activo; }
    }

    /**
     * Authenticates a user against the PostgreSQL database.
     * 
     * @param username The username
     * @param rawPassword The clear-text password
     * @return The authenticated Usuario, or null if credentials fail
     */
    public Usuario autenticar(String username, String rawPassword) {
        String hash = encriptarPassword(rawPassword);
        if (hash == null) return null;

        String sql = "SELECT id, nombre_usuario, rol, nombre_completo, activo FROM usuarios " +
                     "WHERE nombre_usuario = ? AND contrasena_encriptada = ? AND activo = TRUE";

        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        logger.info("🔑 Usuario '{}' autenticado con éxito. Rol: {}", username, rs.getString("rol"));
                        return new Usuario(
                            rs.getInt("id"),
                            rs.getString("nombre_usuario"),
                            rs.getString("rol"),
                            rs.getString("nombre_completo"),
                            rs.getBoolean("activo")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error al autenticar usuario '{}': {}", username, e.getMessage(), e);
        }
        logger.warn("⚠️ Intento de inicio de sesión fallido para usuario '{}'", username);
        return null;
    }

    /**
     * Registers a new user with SHA-256 hashed password.
     */
    public boolean registrar(String username, String rawPassword, String rol, String nombreCompleto) {
        String hash = encriptarPassword(rawPassword);
        if (hash == null) return false;

        String sql = "INSERT INTO usuarios (nombre_usuario, contrasena_encriptada, rol, nombre_completo, activo) VALUES (?, ?, ?, ?, TRUE)";

        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, rol);
                ps.setString(4, nombreCompleto);
                int rows = ps.executeUpdate();
                logger.info("Registro exitoso: Usuario '{}' creado con el rol '{}'.", username, rol);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al registrar nuevo usuario '{}': {}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * SHA-256 Password Encryption helper.
     */
    public static String encriptarPassword(String rawPassword) {
        if (rawPassword == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error al obtener instancia del algoritmo de encriptación SHA-256: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Lists all registered users (for admin management panel).
     */
    public java.util.List<Usuario> listarTodos() {
        java.util.List<Usuario> lista = new java.util.ArrayList<>();
        String sql = "SELECT id, nombre_usuario, rol, nombre_completo, activo FROM usuarios ORDER BY nombre_usuario";

        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Usuario(
                        rs.getInt("id"),
                        rs.getString("nombre_usuario"),
                        rs.getString("rol"),
                        rs.getString("nombre_completo"),
                        rs.getBoolean("activo")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al listar usuarios registrados: {}", e.getMessage(), e);
        }
        return lista;
    }

    /**
     * Updates profile full name for a specific user ID.
     */
    public boolean actualizarDatosBasicos(int id, String nombreCompleto) {
        String sql = "UPDATE usuarios SET nombre_completo = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombreCompleto);
                ps.setInt(2, id);
                int rows = ps.executeUpdate();
                logger.info("Perfil ID {} actualizado. Nuevo nombre: {}", id, nombreCompleto);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar datos básicos del usuario ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Updates an existing user's details (full name, role, and optionally password).
     */
    public boolean actualizarUsuarioCompleto(int id, String nombreCompleto, String rol, String rawPassword) {
        String sql;
        boolean updatePassword = rawPassword != null && !rawPassword.trim().isEmpty();
        if (updatePassword) {
            sql = "UPDATE usuarios SET nombre_completo = ?, rol = ?, contrasena_encriptada = ? WHERE id = ?";
        } else {
            sql = "UPDATE usuarios SET nombre_completo = ?, rol = ? WHERE id = ?";
        }

        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombreCompleto);
                ps.setString(2, rol);
                if (updatePassword) {
                    String hash = encriptarPassword(rawPassword);
                    if (hash == null) return false;
                    ps.setString(3, hash);
                    ps.setInt(4, id);
                } else {
                    ps.setInt(3, id);
                }
                int rows = ps.executeUpdate();
                logger.info("Usuario ID {} actualizado. Nombre: {}, Rol: {}", id, nombreCompleto, rol);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar usuario ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Changes active status of a user (blocking or unblocking access).
     */
    public boolean cambiarEstadoActivo(int id, boolean activo) {
        String sql = "UPDATE usuarios SET activo = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, activo);
                ps.setInt(2, id);
                int rows = ps.executeUpdate();
                logger.info("Estado activo del usuario ID {} cambiado a: {}", id, activo);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al cambiar estado activo del usuario ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deletes user credentials from system.
     */
    public boolean eliminar(int id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                logger.info("Usuario ID {} eliminado permanentemente.", id);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar usuario ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }
}
