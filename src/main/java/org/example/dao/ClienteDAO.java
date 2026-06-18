package org.example.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private static final Logger logger = LoggerFactory.getLogger(ClienteDAO.class);

    public static class Cliente {
        private int id;
        private String nombreInstitucion;
        private String telefono;
        private String ciudad;

        public Cliente(int id, String nombreInstitucion, String telefono, String ciudad) {
            this.id = id;
            this.nombreInstitucion = nombreInstitucion;
            this.telefono = telefono;
            this.ciudad = ciudad;
        }

        public int getId() { return id; }
        public String getNombreInstitucion() { return nombreInstitucion; }
        public String getTelefono() { return telefono; }
        public String getCiudad() { return ciudad; }
    }

    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id, nombre_institucion, telefono, ciudad FROM clientes ORDER BY nombre_institucion";
        
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Cliente(
                        rs.getInt("id"),
                        rs.getString("nombre_institucion"),
                        rs.getString("telefono"),
                        rs.getString("ciudad")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al listar clientes de la base de datos: {}", e.getMessage(), e);
        }
        return lista;
    }

    public boolean crear(String nombreInstitucion, String telefono, String ciudad) {
        String sql = "INSERT INTO clientes (nombre_institucion, telefono, ciudad) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombreInstitucion);
                ps.setString(2, telefono);
                ps.setString(3, ciudad);
                int rows = ps.executeUpdate();
                logger.info("Cliente '{}' registrado con éxito.", nombreInstitucion);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al registrar nuevo cliente: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean actualizar(int id, String nombreInstitucion, String telefono, String ciudad) {
        String sql = "UPDATE clientes SET nombre_institucion = ?, telefono = ?, ciudad = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombreInstitucion);
                ps.setString(2, telefono);
                ps.setString(3, ciudad);
                ps.setInt(4, id);
                int rows = ps.executeUpdate();
                logger.info("Cliente ID {} actualizado con éxito.", id);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar cliente ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                logger.info("Cliente ID {} eliminado con éxito.", id);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar cliente ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }
}
