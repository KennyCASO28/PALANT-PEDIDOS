package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading application configuration from properties files.
 * Provides centralized access to configuration values.
 */
public class ConfigLoader {

    private static final String CONFIG_FILE = "application.properties";
    private static Properties properties;

    static {
        loadProperties();
    }

    /**
     * Loads properties from the application.properties file.
     * If the file is not found, prints an error and initializes empty properties.
     */
    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("⚠️  No se encontró " + CONFIG_FILE + ". Usando valores por defecto.");
                System.err.println("   Copia application.properties.example a application.properties");
                return;
            }
            properties.load(input);
            System.out.println("✅ Configuración cargada desde " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("❌ Error al cargar configuración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets a configuration property value.
     * 
     * @param key The property key
     * @return The property value, or null if not found
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Gets a configuration property value with a default fallback.
     * 
     * @param key          The property key
     * @param defaultValue The default value if property is not found
     * @return The property value, or defaultValue if not found
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Gets the database URL from configuration.
     * 
     * @return The database URL
     */
    public static String getDatabaseUrl() {
        return getProperty("db.url");
    }

    /**
     * Gets the database user from configuration.
     * 
     * @return The database user
     */
    public static String getDatabaseUser() {
        return getProperty("db.user");
    }

    /**
     * Gets the database password from configuration.
     * 
     * @return The database password
     */
    public static String getDatabasePassword() {
        return getProperty("db.password");
    }
}

