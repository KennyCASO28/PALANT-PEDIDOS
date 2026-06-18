package org.example.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VendedorDAO {

    public List<String> listarVendedores() {
        List<String> lista = new ArrayList<>();
        // Query active users from the 'usuarios' table (the actual sellers/staff)
        String sql = "SELECT nombre_completo FROM usuarios WHERE activo = TRUE ORDER BY nombre_completo ASC";

        Connection conn = ConexionDB.getConnection();
        if (conn == null) {
            return lista;
        }

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String nombre = rs.getString("nombre_completo");
                if (nombre != null && !nombre.trim().isEmpty()) {
                    lista.add(nombre.trim());
                }
            }

        } catch (Exception e) {
            System.out.println("Error al listar vendedores: " + e.getMessage());
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return lista;
    }
}
