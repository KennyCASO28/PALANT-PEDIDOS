package org.example.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database connection manager using PostgreSQL and HikariCP connection pooling.
 * Credentials are loaded from application.properties for security.
 */
public class ConexionDB {

    private static final Logger logger = LoggerFactory.getLogger(ConexionDB.class);
    private static HikariDataSource dataSource;

    static {
        try {
            // Load configuration
            String url = ConfigLoader.getDatabaseUrl();
            String user = ConfigLoader.getDatabaseUser();
            String password = ConfigLoader.getDatabasePassword();

            // Validate configuration
            if (url == null || user == null || password == null) {
                logger.error("❌ Error: Configuración de base de datos incompleta en application.properties");
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
                logger.info("✅ Pool de conexiones HikariCP inicializado correctamente.");
            }
        } catch (Exception e) {
            logger.error("❌ Error fatal al inicializar el pool de conexiones: {}", e.getMessage(), e);
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
                logger.error("❌ Error: El DataSource no ha sido inicializado.");
                return null;
            }
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("❌ Error al obtener conexión del pool: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Closes the connection pool. Useful for clean application shutdown.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("✅ Pool de conexiones HikariCP cerrado.");
        }
    }
}
