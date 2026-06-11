package org.example.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.config.ConfigLoader;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database connection manager using PostgreSQL and HikariCP connection pooling.
 * Credentials are loaded from application.properties for security.
 */
public class ConexionDB {

    private static HikariDataSource dataSource;

    static {
        try {
            // Load configuration
            String url = ConfigLoader.getDatabaseUrl();
            String user = ConfigLoader.getDatabaseUser();
            String password = ConfigLoader.getDatabasePassword();

            // Validate configuration
            if (url == null || user == null || password == null) {
                System.err.println("❌ Error: Configuración de base de datos incompleta.");
                System.err
                        .println("   Verifica que application.properties existe y tiene db.url, db.user, db.password");
            } else {
                // Configure HikariCP
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(url);
                config.setUsername(user);
                config.setPassword(password);
                config.setDriverClassName("org.postgresql.Driver");

                // Pool Settings
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setIdleTimeout(300000); // 5 minutes
                config.setConnectionTimeout(30000); // 30 seconds
                config.setMaxLifetime(1800000); // 30 minutes

                // PreparedStatement optimization
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

                dataSource = new HikariDataSource(config);
                System.out.println("✅ Pool de conexiones HikariCP inicializado correctamente.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error fatal al inicializar el pool de conexiones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Establishes and returns a database connection from the pool.
     * 
     * @return Connection object, or null if connection fails
     */
    public static Connection getConnection() {
        try {
            if (dataSource == null) {
                System.err.println("❌ Error: El DataSource no ha sido inicializado.");
                return null;
            }
            return dataSource.getConnection();
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener conexión del pool: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Closes the connection pool. Useful for clean application shutdown.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("✅ Pool de conexiones HikariCP cerrado.");
        }
    }
}
