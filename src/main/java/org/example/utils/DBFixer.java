package org.example.utils;

import org.example.dao.ConexionDB;
import java.sql.Connection;
import java.sql.Statement;

public class DBFixer {
    public static void main(String[] args) {
        System.out.println("🔧 Iniciando reparación de la base de datos...");

        String sql1 = "ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS vendedor TEXT;";
        String sql2 = "ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS codigo_pedido TEXT;";
        String sql3 = "ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;";

        try (Connection conn = ConexionDB.getConnection();
                Statement stmt = conn.createStatement()) {

            if (conn != null) {
                stmt.execute(sql1);
                System.out.println("✅ Columna 'vendedor' agregada/verificada.");

                stmt.execute(sql2);
                System.out.println("✅ Columna 'codigo_pedido' agregada/verificada.");

                stmt.execute(sql3);
                System.out.println("✅ Columna 'created_at' agregada/verificada.");

                System.out.println("🎉 ¡Base de datos actualizada con éxito!");
            }

        } catch (Exception e) {
            System.out.println("❌ Error reparando la BD: " + e.getMessage());
        }
    }
}

