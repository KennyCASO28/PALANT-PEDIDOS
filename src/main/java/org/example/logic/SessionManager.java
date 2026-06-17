package org.example.logic;

import org.example.dao.UsuarioDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    
    private static UsuarioDAO.Usuario currentUser = null;
    private static final String SESSION_DIR = ".session";
    private static final String PROPERTIES_FILE = SESSION_DIR + "/session.properties";
    private static final Properties properties = new Properties();

    static {
        try {
            Path dirPath = Paths.get(SESSION_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path propPath = Paths.get(PROPERTIES_FILE);
            if (Files.exists(propPath)) {
                try (InputStream in = new FileInputStream(propPath.toFile())) {
                    properties.load(in);
                }
            }
        } catch (IOException e) {
            logger.error("Error al inicializar el almacenamiento de sesión local: {}", e.getMessage());
        }
    }

    public static UsuarioDAO.Usuario getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(UsuarioDAO.Usuario user) {
        currentUser = user;
        if (user != null) {
            rememberUsername(user.getNombreUsuario());
        }
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }

    public static String getRememberedUsername() {
        return properties.getProperty("remembered.username", "");
    }

    public static void rememberUsername(String username) {
        properties.setProperty("remembered.username", username);
        saveProperties();
    }

    public static String getCustomAvatarPath(String username) {
        return properties.getProperty("avatar." + username, "");
    }

    public static void setCustomAvatarPath(String username, String path) {
        properties.setProperty("avatar." + username, path);
        saveProperties();
    }

    /**
     * Copies a selected file locally to the session folder to persist as the user's avatar.
     * 
     * @param username The owner username
     * @param sourceFile The source image file
     * @return The absolute path of the locally saved copy, or null if copy fails
     */
    public static String saveAvatarLocally(String username, File sourceFile) {
        try {
            Path destDir = Paths.get(SESSION_DIR, "profile_pics");
            if (!Files.exists(destDir)) {
                Files.createDirectories(destDir);
            }
            
            // Extract extension
            String ext = "";
            String name = sourceFile.getName();
            int idx = name.lastIndexOf('.');
            if (idx > 0) {
                ext = name.substring(idx);
            }
            
            Path destPath = destDir.resolve(username + ext);
            Files.copy(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
            String savedPath = destPath.toAbsolutePath().toString();
            setCustomAvatarPath(username, savedPath);
            logger.info("Avatar para '{}' guardado localmente en: {}", username, savedPath);
            return savedPath;
        } catch (IOException e) {
            logger.error("Error al guardar la imagen de avatar de forma local: {}", e.getMessage(), e);
            return null;
        }
    }

    private static void saveProperties() {
        try (OutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
            properties.store(out, "Palant Pedidos Local Session Configuration");
        } catch (IOException e) {
            logger.error("Error al guardar propiedades de sesión en archivo local: {}", e.getMessage());
        }
    }
}
