package org.example.logic;

import org.example.model.TipoGenero;
import org.example.model.TipoCorte;
import org.example.model.TipoLargo;
import org.example.config.GarmentAssetConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;

/**
 * Systematized Asset Manager.
 * Loads configuration from garment_assets.json to avoid hardcoded switch logic.
 */
public class GarmentAssetManager {

    private static GarmentAssetConfig config;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = GarmentAssetManager.class.getResourceAsStream("/config/garment_assets.json");
            if (is != null) {
                config = mapper.readValue(is, GarmentAssetConfig.class);
            } else {
                System.err.println("CRITICAL: garment_assets.json not found in resources!");
                // Minimal fallback to prevent crash could be added here
            }
        } catch (Exception e) {
            System.err.println("ERROR loading garment_assets.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // === PUBLIC API ===

    public static String getNumberBasePath(TipoGenero genero, String location, TipoCorte corte, String number) {
        String locFolder = location.toLowerCase();
        if (locFolder.equals("chest")) locFolder = "pecho";
        if (locFolder.equals("back")) locFolder = "espalda";

        return config.getTemplate("number")
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null))
                .replace("{location}", locFolder)
                .replace("{cut}", config.getCutFolder(corte != null ? corte.name() : null))
                .replace("{number}", number);
    }

    public static String getShortsCrestPath(TipoGenero genero, TipoCorte corte) {
        return config.getTemplate("crest_shorts")
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null))
                .replace("{cut}", config.getCutFolder(corte != null ? corte.name() : null));
    }

    public static String[] getBrandPaths(TipoGenero genero, String location, TipoCorte corte, String position) {
        String gFolder = config.getGenderFolder(genero != null ? genero.name() : null);
        String cFolder = config.getCutFolder(corte != null ? corte.name() : null);

        // Special handling for Licra/Pantaloneta (Marca root changes)
        String locFolder = location;
        String finalCutFolder = cFolder;

        if (location.equalsIgnoreCase("short")) {
            if (corte == TipoCorte.LICRA) {
                locFolder = "licra";
                finalCutFolder = "cuadrado";
            } else if (corte == TipoCorte.PANTALONETA) {
                locFolder = "pantaloneta";
                finalCutFolder = "cuadrado";
            }
        }

        String base = config.getTemplate("brand_base")
                .replace("{gender}", gFolder)
                .replace("{location}", locFolder)
                .replace("{cut}", finalCutFolder)
                .replace("{position}", position);

        String cut = config.getTemplate("brand_cut")
                .replace("{gender}", gFolder)
                .replace("{location}", locFolder)
                .replace("{cut}", finalCutFolder)
                .replace("{position}", position);

        return new String[] { base, cut };
    }

    public static String getShirtPath(TipoGenero genero, TipoCorte corte, TipoLargo largo, boolean isArquero) {
        return config.getTemplate("shirt")
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null))
                .replace("{cut}", config.getCutFolder(corte != null ? corte.name() : null))
                .replace("{length}", config.getLengthFolder(largo != null ? largo.name() : null));
    }

    public static String getShirtPath(TipoGenero genero, TipoCorte corte, TipoLargo largo) {
        return getShirtPath(genero, corte, largo, false);
    }

    public static String getBaseRedondoPath(TipoGenero genero, TipoCorte corte) {
        String template = config.getTemplate("base_redondo");
        if (template == null) return null;
        return template
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null))
                .replace("{cut}", config.getCutFolder(corte != null ? corte.name() : null));
    }

    public static String getMeshPath(TipoGenero genero, TipoCorte corte) {
        return config.getTemplate("mesh")
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null))
                .replace("{cut}", config.getCutFolder(corte != null ? corte.name() : null));
    }

    public static String getCuffsPath(TipoGenero genero, TipoCorte corte, TipoLargo largo) {
        return config.getTemplate("cuffs")
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null))
                .replace("{cut}", config.getCutFolder(corte != null ? corte.name() : null))
                .replace("{length}", config.getLengthFolder(largo != null ? largo.name() : null));
    }

    public static String getShortsPath(TipoGenero genero, TipoCorte corteShort) {
        return config.getTemplate("shorts")
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null))
                .replace("{cut}", config.getCutFolder(corteShort != null ? corteShort.name() : null));
    }

    public static String getSocksPath(TipoGenero genero) {
        return config.getTemplate("socks")
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null));
    }

    public static String getSocksTopPath(TipoGenero genero) {
        return config.getTemplate("socks_top")
                .replace("{gender}", config.getGenderFolder(genero != null ? genero.name() : null));
    }

    // Deprecated helpers, kept for compatibility if needed elsewhere, but logic moved to DTO
    private static String resolveGenderFolder(TipoGenero genero) { return config.getGenderFolder(genero != null ? genero.name() : null); }
    private static String resolveCorteFolder(TipoCorte corte) { return config.getCutFolder(corte != null ? corte.name() : null); }
}
