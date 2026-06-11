package org.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the visual configuration state of a garment.
 * Pure data model, no UI components.
 */
public class PrendaState {

    // --- Enums ---
    private TipoGenero currentGenero = TipoGenero.HOMBRE;
    private TipoCorte currentCorte = TipoCorte.CUADRADO;
    private TipoCorte currentCorteShort = TipoCorte.CUADRADO;
    private TipoLargo currentLargo = TipoLargo.MANGA_CORTA;
    private TipoCuello currentCuello = TipoCuello.V;
    private String currentGarmentType = "";
    private TipoTela currentTela = TipoTela.WIN;
    private String customTela = "";
    private TipoMedias tipoMedias = null; // Default to null for auto-calc logic in roster

    // --- Visibility Booleans (Camiseta) ---
    private boolean hasShirt = true;
    private boolean hasMesh = false;
    private boolean hasCuffs = false;
    private boolean hasShirtStripe = false;
    private boolean hasPadding = false;
    private boolean telaNatural = false;

    // --- Visibility Booleans (Short) ---
    private boolean hasShorts = true;
    private boolean hasShortsStripe = false;
    private boolean hasShortsPicket = false;
    private boolean hasShortsPocket = false;
    private boolean hasShortsCuff = false;
    private boolean hasShortsCord = true;
    private boolean hasShortsLining = false;

    // --- Visibility Booleans (Medias) ---
    private boolean hasSocks = true;
    private boolean hasSocksTop = false;

    // --- Branding Config ---
    private boolean chestBrandVisible = false;
    private String chestBrandPosition = "derecha";
    private boolean shortBrandVisible = false;
    private String shortBrandPosition = "pierna";
    private boolean socksBrandVisible = false;
    private String brandTech = "Bordado";

    // --- Numbers Config ---
    private boolean chestNumberVisible = false;
    private boolean backNumberVisible = false;
    private boolean shortNumberVisible = false;

    // --- Number Values ---
    private String currentChestNumber = "9";
    private String currentBackNumber = "9";
    private String currentShortNumber = "9";

    // --- Other ---
    private boolean shortCrestVisible = false;
    private boolean sleeveCrestVisible = false;

    private String shirtCrestTech = "Bordado";
    private String shortCrestTech = "Ninguno";
    private String sleeveCrestTech = "Ninguno";

    private List<ReferenceHotspot> referenceHotspots = new ArrayList<>();
    private javafx.scene.paint.Color colorReferenciaArquero = javafx.scene.paint.Color.WHITE;
    private List<org.example.dto.save.LayerDTO> userLayers = new ArrayList<>();
    private java.util.Map<String, javafx.scene.paint.Color> colors = new java.util.HashMap<>();
    private java.util.Map<String, String> internalCodes = new java.util.HashMap<>();
    private java.util.Map<String, byte[]> customFonts = new java.util.HashMap<>();

    // --- Number Transformations (Independent for each state) ---
    private double chestNumberX = 0, chestNumberY = 0, chestNumberScale = 1.0;
    private double backNumberX = 0, backNumberY = 0, backNumberScale = 1.0;
    private double shortNumberX = 0, shortNumberY = 0, shortNumberScale = 1.0;

    // --- Number Colors Persistence ---
    private java.util.Map<Integer, javafx.scene.paint.Color> chestNumberColors = new java.util.HashMap<>();
    private java.util.Map<Integer, javafx.scene.paint.Color> backNumberColors = new java.util.HashMap<>();
    private java.util.Map<Integer, javafx.scene.paint.Color> shortNumberColors = new java.util.HashMap<>();

    public java.util.Map<Integer, javafx.scene.paint.Color> getChestNumberColors() {
        return chestNumberColors;
    }

    public void setChestNumberColors(java.util.Map<Integer, javafx.scene.paint.Color> colors) {
        this.chestNumberColors.clear();
        if (colors != null)
            this.chestNumberColors.putAll(colors);
    }

    public java.util.Map<Integer, javafx.scene.paint.Color> getBackNumberColors() {
        return backNumberColors;
    }

    public void setBackNumberColors(java.util.Map<Integer, javafx.scene.paint.Color> colors) {
        this.backNumberColors.clear();
        if (colors != null)
            this.backNumberColors.putAll(colors);
    }

    public java.util.Map<Integer, javafx.scene.paint.Color> getShortNumberColors() {
        return shortNumberColors;
    }

    public void setShortNumberColors(java.util.Map<Integer, javafx.scene.paint.Color> colors) {
        this.shortNumberColors.clear();
        if (colors != null)
            this.shortNumberColors.putAll(colors);
    }

    public java.util.Map<String, byte[]> getCustomFonts() {
        return customFonts;
    }

    public void setCustomFonts(java.util.Map<String, byte[]> fonts) {
        this.customFonts.clear();
        if (fonts != null)
            this.customFonts.putAll(fonts);
    }

    public void addCustomFont(String family, byte[] data) {
        this.customFonts.put(family, data);
    }

    // ================= GETTERS & SETTERS =================

    public TipoGenero getGenero() {
        return currentGenero;
    }

    public void setGenero(TipoGenero genero) {
        this.currentGenero = genero;
    }

    public TipoCorte getCorte() {
        return currentCorte;
    }

    public void setCorte(TipoCorte corte) {
        this.currentCorte = corte;
    }

    public boolean hasSleeves() {
        return hasShirt;
    }

    public void setHasShortsCrest(boolean v) {
        this.shortCrestVisible = v;
    }

    public void setHasSocksBrand(boolean v) {
        this.socksBrandVisible = v;
    }

    public void setChestBrandColor(String hex) {
        if (hex != null) colors.put("brand_pecho", javafx.scene.paint.Color.web(hex));
    }

    public void setShortBrandColor(String hex) {
        if (hex != null) colors.put("brand_short", javafx.scene.paint.Color.web(hex));
    }

    public void setSocksBrandColor(String hex) {
        if (hex != null) colors.put("brand_medias", javafx.scene.paint.Color.web(hex));
    }

    public boolean llevaShort() { return hasShorts; }
    public boolean llevaFranjaShort() { return hasShortsStripe; }
    public boolean llevaPiqueteShort() { return hasShortsPicket; }
    public boolean llevaBolsilloShort() { return hasShortsPocket; }
    public boolean llevaPunoShort() { return hasShortsCuff; }
    public boolean llevaPasadorShort() { return hasShortsCord; }
    public boolean llevaForroShort() { return hasShortsLining; }
    public TipoCorte getCorteShort() { return currentCorteShort; }

    public TipoCorte getCorteShortInternal() {
        return currentCorteShort;
    }

    public void setCorteShort(TipoCorte corteShort) {
        this.currentCorteShort = corteShort;
    }

    public TipoLargo getLargo() {
        return currentLargo;
    }

    public void setLargo(TipoLargo largo) {
        this.currentLargo = largo;
    }

    public TipoCuello getCuello() {
        return currentCuello;
    }

    public void setCuello(TipoCuello cuello) {
        this.currentCuello = cuello;
    }

    public String getGarmentType() {
        return currentGarmentType;
    }

    public void setGarmentType(String garmentType) {
        this.currentGarmentType = garmentType;
    }

    public boolean hasShirt() {
        return hasShirt;
    }

    public void setHasShirt(boolean hasShirt) {
        this.hasShirt = hasShirt;
    }

    public boolean hasMesh() {
        return hasMesh;
    }

    public void setHasMesh(boolean hasMesh) {
        this.hasMesh = hasMesh;
    }

    public boolean hasCuffs() {
        return hasCuffs;
    }

    public void setHasCuffs(boolean hasCuffs) {
        this.hasCuffs = hasCuffs;
    }

    public boolean hasShirtStripe() {
        return hasShirtStripe;
    }

    public void setHasShirtStripe(boolean hasShirtStripe) {
        this.hasShirtStripe = hasShirtStripe;
    }

    public boolean hasPadding() {
        return hasPadding;
    }

    public void setHasPadding(boolean hasPadding) {
        this.hasPadding = hasPadding;
    }

    public boolean isTelaNatural() {
        return telaNatural;
    }

    public void setTelaNatural(boolean telaNatural) {
        this.telaNatural = telaNatural;
    }

    public boolean hasShorts() {
        return hasShorts;
    }

    public void setHasShorts(boolean hasShorts) {
        this.hasShorts = hasShorts;
    }

    public boolean hasShortsStripe() {
        return hasShortsStripe;
    }

    public void setHasShortsStripe(boolean hasShortsStripe) {
        this.hasShortsStripe = hasShortsStripe;
    }

    public boolean hasShortsPicket() {
        return hasShortsPicket;
    }

    public void setHasShortsPicket(boolean hasShortsPicket) {
        this.hasShortsPicket = hasShortsPicket;
    }

    public boolean hasShortsPocket() {
        return hasShortsPocket;
    }

    public void setHasShortsPocket(boolean hasShortsPocket) {
        this.hasShortsPocket = hasShortsPocket;
    }

    public boolean hasShortsCuff() {
        return hasShortsCuff;
    }

    public void setHasShortsCuff(boolean hasShortsCuff) {
        this.hasShortsCuff = hasShortsCuff;
    }

    public boolean hasShortsCord() {
        return hasShortsCord;
    }

    public void setHasShortsCord(boolean hasShortsCord) {
        this.hasShortsCord = hasShortsCord;
    }

    public boolean hasShortsLining() {
        return hasShortsLining;
    }

    public void setHasShortsLining(boolean hasShortsLining) {
        this.hasShortsLining = hasShortsLining;
    }

    public boolean hasSocks() {
        return hasSocks;
    }

    public void setHasSocks(boolean hasSocks) {
        this.hasSocks = hasSocks;
    }

    public boolean hasSocksTop() {
        return hasSocksTop;
    }

    public void setHasSocksTop(boolean hasSocksTop) {
        this.hasSocksTop = hasSocksTop;
    }

    public boolean isChestBrandVisible() {
        return chestBrandVisible;
    }

    public void setChestBrandVisible(boolean chestBrandVisible) {
        this.chestBrandVisible = chestBrandVisible;
    }

    public String getChestBrandPosition() {
        return chestBrandPosition;
    }

    public void setChestBrandPosition(String chestBrandPosition) {
        this.chestBrandPosition = chestBrandPosition;
    }

    public boolean isShortBrandVisible() {
        return shortBrandVisible;
    }

    public void setShortBrandVisible(boolean shortBrandVisible) {
        this.shortBrandVisible = shortBrandVisible;
    }

    public String getShortBrandPosition() {
        return shortBrandPosition;
    }

    public void setShortBrandPosition(String shortBrandPosition) {
        this.shortBrandPosition = shortBrandPosition;
    }

    public boolean isSocksBrandVisible() {
        return socksBrandVisible;
    }

    public void setSocksBrandVisible(boolean socksBrandVisible) {
        this.socksBrandVisible = socksBrandVisible;
    }

    public String getBrandTech() {
        return brandTech;
    }

    public void setBrandTech(String brandTech) {
        this.brandTech = brandTech;
    }

    public boolean isChestNumberVisible() {
        return chestNumberVisible;
    }

    public void setChestNumberVisible(boolean chestNumberVisible) {
        this.chestNumberVisible = chestNumberVisible;
    }

    public boolean isBackNumberVisible() {
        return backNumberVisible;
    }

    public void setBackNumberVisible(boolean backNumberVisible) {
        this.backNumberVisible = backNumberVisible;
    }

    public boolean isShortNumberVisible() {
        return shortNumberVisible;
    }

    public void setShortNumberVisible(boolean shortNumberVisible) {
        this.shortNumberVisible = shortNumberVisible;
    }

    public String getCurrentChestNumber() {
        return currentChestNumber;
    }

    public void setCurrentChestNumber(String currentChestNumber) {
        this.currentChestNumber = currentChestNumber;
    }

    public String getCurrentBackNumber() {
        return currentBackNumber;
    }

    public void setCurrentBackNumber(String currentBackNumber) {
        this.currentBackNumber = currentBackNumber;
    }

    public String getCurrentShortNumber() {
        return currentShortNumber;
    }

    public void setCurrentShortNumber(String currentShortNumber) {
        this.currentShortNumber = currentShortNumber;
    }

    public boolean isShortCrestVisible() {
        return shortCrestVisible;
    }

    public void setShortCrestVisible(boolean shortCrestVisible) {
        this.shortCrestVisible = shortCrestVisible;
    }

    public TipoTela getTela() {
        return currentTela;
    }

    public void setTela(TipoTela tela) {
        this.currentTela = tela;
    }

    public String getCustomTela() {
        return customTela;
    }

    public void setCustomTela(String customTela) {
        this.customTela = customTela;
    }

    public TipoMedias getTipoMedias() {
        return tipoMedias;
    }

    public void setTipoMedias(TipoMedias tipoMedias) {
        this.tipoMedias = tipoMedias;
    }

    public boolean isSleeveCrestVisible() {
        return sleeveCrestVisible;
    }

    public void setSleeveCrestVisible(boolean sleeveCrestVisible) {
        this.sleeveCrestVisible = sleeveCrestVisible;
    }

    public String getShirtCrestTech() {
        return shirtCrestTech;
    }

    public void setShirtCrestTech(String shirtCrestTech) {
        this.shirtCrestTech = shirtCrestTech;
    }

    public String getShortCrestTech() {
        return shortCrestTech;
    }

    public void setShortCrestTech(String shortCrestTech) {
        this.shortCrestTech = shortCrestTech;
    }

    public String getSleeveCrestTech() {
        return sleeveCrestTech;
    }

    public void setSleeveCrestTech(String sleeveCrestTech) {
        this.sleeveCrestTech = sleeveCrestTech;
    }

    public List<ReferenceHotspot> getReferenceHotspots() {
        return referenceHotspots;
    }

    public void setReferenceHotspots(List<ReferenceHotspot> hotspots) {
        this.referenceHotspots = hotspots;
    }

    public javafx.scene.paint.Color getColorReferenciaArquero() {
        return colorReferenciaArquero;
    }

    public void setColorReferenciaArquero(javafx.scene.paint.Color color) {
        this.colorReferenciaArquero = color;
    }

    public List<org.example.dto.save.LayerDTO> getUserLayers() {
        return userLayers;
    }

    public void setUserLayers(List<org.example.dto.save.LayerDTO> layers) {
        this.userLayers = layers;
    }

    public java.util.Map<String, javafx.scene.paint.Color> getColors() {
        return colors;
    }

    public void setColors(java.util.Map<String, javafx.scene.paint.Color> colors) {
        this.colors = colors;
    }

    public java.util.Map<String, String> getInternalCodes() {
        return internalCodes;
    }

    public void setInternalCodes(java.util.Map<String, String> codes) {
        this.internalCodes = codes;
    }

    // --- Number Transform Getters/Setters ---
    public double getChestNumberX() {
        return chestNumberX;
    }

    public void setChestNumberX(double x) {
        this.chestNumberX = x;
    }

    public double getChestNumberY() {
        return chestNumberY;
    }

    public void setChestNumberY(double y) {
        this.chestNumberY = y;
    }

    public double getChestNumberScale() {
        return chestNumberScale;
    }

    public void setChestNumberScale(double scale) {
        this.chestNumberScale = scale;
    }

    public double getBackNumberX() {
        return backNumberX;
    }

    public void setBackNumberX(double x) {
        this.backNumberX = x;
    }

    public double getBackNumberY() {
        return backNumberY;
    }

    public void setBackNumberY(double y) {
        this.backNumberY = y;
    }

    public double getBackNumberScale() {
        return backNumberScale;
    }

    public void setBackNumberScale(double scale) {
        this.backNumberScale = scale;
    }

    public double getShortNumberX() {
        return shortNumberX;
    }

    public void setShortNumberX(double x) {
        this.shortNumberX = x;
    }

    public double getShortNumberY() {
        return shortNumberY;
    }

    public void setShortNumberY(double y) {
        this.shortNumberY = y;
    }

    public double getShortNumberScale() {
        return shortNumberScale;
    }

    public void setShortNumberScale(double scale) {
        this.shortNumberScale = scale;
    }

    public static class ReferenceHotspot {
        private double x;
        private double y;
        private String zone;
        private String label;
        private String description;
        private String imagePath;
        private byte[] imageData;

        public ReferenceHotspot() {
        }

        public ReferenceHotspot(double x, double y, String zone, String label) {
            this.x = x;
            this.y = y;
            this.zone = zone;
            this.label = label;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public byte[] getImageData() {
            return imageData;
        }

        public void setImageData(byte[] imageData) {
            this.imageData = imageData;
        }
    }

    /**
     * Deep clones customization attributes from another state.
     * This is used when a goalkeeper starts with the player's design.
     */
    public void copyFrom(PrendaState source) {
        if (source == null)
            return;

        // 1. Enums & Metadata
        this.currentGenero = source.currentGenero;
        this.currentCorte = source.currentCorte;
        this.currentCorteShort = source.currentCorteShort;
        this.currentLargo = source.currentLargo;
        this.currentCuello = source.currentCuello;
        this.currentGarmentType = source.currentGarmentType;
        this.currentTela = source.currentTela;
        this.customTela = source.customTela;
        this.tipoMedias = source.tipoMedias;

        // 2. Visibility
        this.hasShirt = source.hasShirt;
        this.hasMesh = source.hasMesh;
        this.hasCuffs = source.hasCuffs;
        this.hasShirtStripe = source.hasShirtStripe;
        this.hasPadding = source.hasPadding;
        this.telaNatural = source.telaNatural;
        this.hasShorts = source.hasShorts;
        this.hasShortsStripe = source.hasShortsStripe;
        this.hasShortsPicket = source.hasShortsPicket;
        this.hasShortsPocket = source.hasShortsPocket;
        this.hasShortsCuff = source.hasShortsCuff;
        this.hasShortsCord = source.hasShortsCord;
        this.hasShortsLining = source.hasShortsLining;
        this.hasSocks = source.hasSocks;
        this.hasSocksTop = source.hasSocksTop;

        // 3. Branding
        this.chestBrandVisible = source.chestBrandVisible;
        this.chestBrandPosition = source.chestBrandPosition;
        this.shortBrandVisible = source.shortBrandVisible;
        this.shortBrandPosition = source.shortBrandPosition;
        this.socksBrandVisible = source.socksBrandVisible;
        this.brandTech = source.brandTech;

        this.shirtCrestTech = source.shirtCrestTech;
        this.shortCrestTech = source.shortCrestTech;
        this.sleeveCrestTech = source.sleeveCrestTech;
        this.shortCrestVisible = source.shortCrestVisible;
        this.sleeveCrestVisible = source.sleeveCrestVisible;

        // 4. Numbers Transform & Values
        this.chestNumberVisible = source.chestNumberVisible;
        this.backNumberVisible = source.backNumberVisible;
        this.shortNumberVisible = source.shortNumberVisible;
        this.currentChestNumber = source.currentChestNumber;
        this.currentBackNumber = source.currentBackNumber;
        this.currentShortNumber = source.currentShortNumber;

        this.chestNumberX = source.chestNumberX;
        this.chestNumberY = source.chestNumberY;
        this.chestNumberScale = source.chestNumberScale;
        this.backNumberX = source.backNumberX;
        this.backNumberY = source.backNumberY;
        this.backNumberScale = source.backNumberScale;
        this.shortNumberX = source.shortNumberX;
        this.shortNumberY = source.shortNumberY;
        this.shortNumberScale = source.shortNumberScale;

        // 5. Deep Copy Maps (Number colors)
        setChestNumberColors(new java.util.HashMap<>(source.chestNumberColors));
        setBackNumberColors(new java.util.HashMap<>(source.backNumberColors));
        setShortNumberColors(new java.util.HashMap<>(source.shortNumberColors));

        // 6. Deep Copy Layers (CRITICAL: Independent Objects)
        this.userLayers = new java.util.ArrayList<>();
        if (source.userLayers != null) {
            for (org.example.dto.save.LayerDTO layer : source.userLayers) {
                this.userLayers.add(layer.deepCopy());
            }
        }

        // 7. Hotspots
        this.referenceHotspots = new java.util.ArrayList<>();
        if (source.referenceHotspots != null) {
            for (ReferenceHotspot rh : source.referenceHotspots) {
                ReferenceHotspot copy = new ReferenceHotspot(rh.getX(), rh.getY(), rh.getZone(), rh.getLabel());
                copy.setDescription(rh.getDescription());
                copy.setImagePath(rh.getImagePath());
                copy.setImageData(rh.getImageData());
                this.referenceHotspots.add(copy);
            }
        }

        // 8. Colors (CRITICAL for synchronization)
        if (source.colors != null) {
            this.colors.clear();
            this.colors.putAll(source.colors);
        }
        if (source.internalCodes != null) {
            this.internalCodes.clear();
            this.internalCodes.putAll(source.internalCodes);
        }

        // 9. Fonts
        if (source.customFonts != null) {
            this.customFonts.clear();
            this.customFonts.putAll(source.customFonts);
        }
    }

    /**
     * Checks if the state has been independently customized (colors, layers, or
     * number configurations).
     * Used to prevent over-aggressive synchronization from player to arquero.
     */
    public boolean hasAnyCustomization() {
        // 1. If we have ANY layer, it's customized.
        if (userLayers != null && !userLayers.isEmpty())
            return true;

        // 2. Check Number Colors (Crucial for the reported bug)
        if (chestNumberColors != null && !chestNumberColors.isEmpty())
            return true;
        if (backNumberColors != null && !backNumberColors.isEmpty())
            return true;
        if (shortNumberColors != null && !shortNumberColors.isEmpty())
            return true;

        // 3. Check Garment Colors
        if (colors != null && !colors.isEmpty()) {
            // If we have more than the basic reference colors, or they are not default
            // WHITE
            for (java.util.Map.Entry<String, javafx.scene.paint.Color> entry : colors.entrySet()) {
                javafx.scene.paint.Color c = entry.getValue();
                if (c != null && !c.equals(javafx.scene.paint.Color.WHITE)
                        && !c.equals(javafx.scene.paint.Color.TRANSPARENT)) {
                    // Ignore internal branding colors if they were inherited
                    if (!entry.getKey().startsWith("brand"))
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * Resets all number transforms (X, Y, Scale) to defaults.
     * Useful when switching genders as the garment geometry changes significantly.
     */
    public void resetNumberTransforms() {
        this.chestNumberX = 0;
        this.chestNumberY = 0;
        this.chestNumberScale = 1.0; // Faithful to export size
        
        this.backNumberX = 0;
        this.backNumberY = 0;
        this.backNumberScale = 1.0; // Faithful to export size

        this.shortNumberX = 0;
        this.shortNumberY = 0;
        this.shortNumberScale = 1.0; // Faithful to export size
    }

    /**
     * Resets all personalization visibility flags to their defaults.
     * Used when changing garments to prevent ghost elements.
     */
    public void resetPersonalizationVisibility() {
        this.chestNumberVisible = false;
        this.backNumberVisible = false;
        this.shortNumberVisible = false;
        
        this.chestBrandVisible = false;
        this.shortBrandVisible = false;
        this.socksBrandVisible = false;
        
        this.shortCrestVisible = false;
        this.sleeveCrestVisible = false;
        
        this.shirtCrestTech = "Bordado";
        this.shortCrestTech = "Ninguno";
        this.sleeveCrestTech = "Ninguno";
    }
}
