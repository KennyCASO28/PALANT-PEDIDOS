package org.example.utils;

import org.example.dao.ConexionDB;
import java.sql.Connection;
import java.sql.Statement;

public class DBFixerTables {
    public static void main(String[] args) {
        System.out.println("🔧 Actualizando esquema de Base de Datos...");

        // 1. Crear tabla detalle_nombres si no existe
        String sqlTablaDetalle = "CREATE TABLE IF NOT EXISTS detalle_nombres (" +
                "id SERIAL PRIMARY KEY, " +
                "pedido_id INT NOT NULL, " +
                "nombre TEXT, " +
                "numero TEXT, " +
                "talla TEXT, " +
                "CONSTRAINT fk_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id)" +
                ");";

        // 2. Asegurar columna fecha_creacion en pedidos (El código Java nuevo usa
        // 'fecha_creacion' en lugar de 'created_at')
        // Intentamos renombrar si existe created_at, o crear fecha_creacion si no.
        // 2. Asegurar columna fecha_creacion en pedidos
        // Lógica mejorada: Solo renombramos si NO existe 'fecha_creacion' y SI existe
        // 'created_at'.
        String sqlRename = "DO $$ " +
                "BEGIN " +
                "  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='fecha_creacion') THEN "
                +
                "      IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='created_at') THEN "
                +
                "          ALTER TABLE pedidos RENAME COLUMN created_at TO fecha_creacion; " +
                "      ELSE " +
                "          ALTER TABLE pedidos ADD COLUMN fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP; " +
                "      END IF; " +
                "  END IF; " +
                "END $$;";

        // 3. Crear tabla VENDEDORES y poblar datos iniciales
        String sqlTablaVendedores = "CREATE TABLE IF NOT EXISTS vendedores (" +
                "id SERIAL PRIMARY KEY, " +
                "nombre TEXT NOT NULL, " +
                "activo BOOLEAN DEFAULT TRUE" +
                ");";

        String sqlSeedVendedores = "INSERT INTO vendedores (nombre) SELECT 'Juan Pérez' WHERE NOT EXISTS (SELECT 1 FROM vendedores); "
                +
                "INSERT INTO vendedores (nombre) SELECT 'Maria Lopez' WHERE NOT EXISTS (SELECT 1 FROM vendedores WHERE nombre='Maria Lopez'); "
                +
                "INSERT INTO vendedores (nombre) SELECT 'Carlos Ruiz' WHERE NOT EXISTS (SELECT 1 FROM vendedores WHERE nombre='Carlos Ruiz');";

        // 4. Agregar columnas EXTRA (Fase 1)
        // Usamos DO $$ para chequeo condicional en PostgreSQL
        String sqlAddColumns = "DO $$ " +
                "BEGIN " +
                "  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='tiene_medias') THEN "
                + "      ALTER TABLE pedidos ADD COLUMN tiene_medias BOOLEAN DEFAULT FALSE; " + "  END IF; " +

                "  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='puno_camiseta') THEN "
                + "      ALTER TABLE pedidos ADD COLUMN puno_camiseta BOOLEAN DEFAULT FALSE; " + "  END IF; " +

                "  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='puno_short') THEN "
                + "      ALTER TABLE pedidos ADD COLUMN puno_short BOOLEAN DEFAULT FALSE; " + "  END IF; " +

                "  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='tiene_bordado') THEN "
                + "      ALTER TABLE pedidos ADD COLUMN tiene_bordado BOOLEAN DEFAULT FALSE; " + "  END IF; " +

                "  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='tiene_insignia') THEN "
                + "      ALTER TABLE pedidos ADD COLUMN tiene_insignia BOOLEAN DEFAULT FALSE; " + "  END IF; " +

                "  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='color_hilo') THEN "
                + "      ALTER TABLE pedidos ADD COLUMN color_hilo TEXT; " + "  END IF; " +

                "  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='pedidos' AND column_name='borde_insignia') THEN "
                + "      ALTER TABLE pedidos ADD COLUMN borde_insignia TEXT; " + "  END IF; " +
                "END $$;";

        try (Connection conn = ConexionDB.getConnection();
                Statement stmt = conn.createStatement()) {

            if (conn != null) {
                // Ejecutar creación de tabla detalle
                stmt.execute(sqlTablaDetalle);

                // Ejecutar ajuste de columnas
                stmt.execute(sqlRename);

                // Ejecutar tabla vendedores
                stmt.execute(sqlTablaVendedores);
                stmt.execute(sqlSeedVendedores);

                // Ejecutar nuevas columnas
                stmt.execute(sqlAddColumns);

                System.out.println("✅ Tablas 'detalle_nombres' y 'vendedores' (con datos) verificadas.");
                System.out.println("✅ Columnas Extras (Medias, Puños, Bordados) verificadas.");
                System.out.println("🎉 ¡Base de datos lista para Fase 1 Visual!");
            }

        } catch (Exception e) {
            System.out.println("❌ Error actualizando BD: " + e.getMessage());
        }
    }
}

