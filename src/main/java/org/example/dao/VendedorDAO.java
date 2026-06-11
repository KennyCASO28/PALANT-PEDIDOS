package org.example.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VendedorDAO {

    public List<String> listarVendedores() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM vendedores WHERE activo = TRUE ORDER BY nombre ASC";

        Connection conn = ConexionDB.getConnection();
        if (conn == null) {
            return lista;
        }

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }

        } catch (Exception e) {
            System.out.println("❌ Error al listar vendedores: " + e.getMessage());
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

