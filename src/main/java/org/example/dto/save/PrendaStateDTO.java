package org.example.dto.save;

import org.example.model.TipoGenero;
import org.example.model.TipoCorte;
import org.example.model.TipoLargo;
import org.example.model.TipoCuello;
import org.example.model.TipoTela;
import org.example.model.TipoMedias;


public class PrendaStateDTO {
    // Enums
    private TipoGenero currentGenero;
    private TipoCorte currentCorte;
    private TipoCorte currentCorteShort;
    private TipoLargo currentLargo;
    private TipoCuello currentCuello;
    private TipoTela currentTela;
    private String customTela = "";
    private TipoMedias currentTipoMedias;

    private String currentGarmentType;
    private String referenceColorArquero;
    private java.util.Map<String, String> colors = new java.util.HashMap<>();
    private java.util.Map<String, String> internalCodes = new java.util.HashMap<>();

    // Visibility Booleans (Camiseta)
    private boolean hasShirt;
    private boolean hasMesh;
    private boolean hasCuffs;
    private boolean hasShirtStripe;

    // Visibility Booleans (Short)
    private boolean hasShorts;
    private boolean hasShortsStripe;
    private boolean hasShortsPicket;
    private boolean hasShortsPocket;
    private boolean hasShortsCuff;
    private boolean hasShortsCord;
    private boolean hasShortsLining;

    // Visibility Booleans (Medias)
    private boolean hasSocks;
    private boolean hasSocksTop;
    private boolean hasPadding;

    // Branding Config
    private boolean chestBrandVisible;
    private String chestBrandPosition;
    private boolean shortBrandVisible;
    private String shortBrandPosition;
    private boolean socksBrandVisible;
    private String brandTech;
    private boolean shortCrestVisible;
    private boolean sleeveCrestVisible;
    private String shirtCrestTech;
    private String shortCrestTech;
    private String sleeveCrestTech;

    // Numbers Config
    private boolean chestNumberVisible;
    private boolean backNumberVisible;
    private boolean shortNumberVisible;

    // Number Values
    private String currentChestNumber;
    private String currentBackNumber;
    private String currentShortNumber;

    // Advanced Persistence
    private java.util.List<ReferenceHotspotDTO> hotspots = new java.util.ArrayList<>();
    private java.util.Map<String, java.util.List<String>> numberColors = new java.util.HashMap<>();
    
    // Position/Scale persistence
    private double chestNumberX, chestNumberY, chestNumberScale = 1.0;
    private double backNumberX, backNumberY, backNumberScale = 1.0;
    private double shortNumberX, shortNumberY, shortNumberScale = 1.0;

    public PrendaStateDTO() {
    }

    public PrendaStateDTO deepCopy() {
        PrendaStateDTO copy = new PrendaStateDTO();
        copy.currentGenero = currentGenero;
        copy.currentCorte = currentCorte;
        copy.currentCorteShort = currentCorteShort;
        copy.currentLargo = currentLargo;
        copy.currentCuello = currentCuello;
        copy.currentTela = currentTela;
        copy.customTela = customTela;
        copy.currentTipoMedias = currentTipoMedias;
        copy.currentGarmentType = currentGarmentType;
        copy.referenceColorArquero = referenceColorArquero;
        copy.colors = new java.util.HashMap<>(colors);
        copy.internalCodes = new java.util.HashMap<>(internalCodes);
        copy.hasShirt = hasShirt;
        copy.hasMesh = hasMesh;
        copy.hasCuffs = hasCuffs;
        copy.hasShirtStripe = hasShirtStripe;
        copy.hasShorts = hasShorts;
        copy.hasShortsStripe = hasShortsStripe;
        copy.hasShortsPicket = hasShortsPicket;
        copy.hasShortsPocket = hasShortsPocket;
        copy.hasShortsCuff = hasShortsCuff;
        copy.hasShortsCord = hasShortsCord;
        copy.hasShortsLining = hasShortsLining;
        copy.hasSocks = hasSocks;
        copy.hasSocksTop = hasSocksTop;
        copy.hasPadding = hasPadding;
        copy.chestBrandVisible = chestBrandVisible;
        copy.chestBrandPosition = chestBrandPosition;
        copy.shortBrandVisible = shortBrandVisible;
        copy.shortBrandPosition = shortBrandPosition;
        copy.socksBrandVisible = socksBrandVisible;
        copy.brandTech = brandTech;
        copy.shortCrestVisible = shortCrestVisible;
        copy.sleeveCrestVisible = sleeveCrestVisible;
        copy.shirtCrestTech = shirtCrestTech;
        copy.shortCrestTech = shortCrestTech;
        copy.sleeveCrestTech = sleeveCrestTech;
        copy.chestNumberVisible = chestNumberVisible;
        copy.backNumberVisible = backNumberVisible;
        copy.shortNumberVisible = shortNumberVisible;
        copy.currentChestNumber = currentChestNumber;
        copy.currentBackNumber = currentBackNumber;
        copy.currentShortNumber = currentShortNumber;
        copy.chestNumberX = chestNumberX;
        copy.chestNumberY = chestNumberY;
        copy.chestNumberScale = chestNumberScale;
        copy.backNumberX = backNumberX;
        copy.backNumberY = backNumberY;
        copy.backNumberScale = backNumberScale;
        copy.shortNumberX = shortNumberX;
        copy.shortNumberY = shortNumberY;
        copy.shortNumberScale = shortNumberScale;
        copy.hotspots = new java.util.ArrayList<>();
        for (ReferenceHotspotDTO hotspot : hotspots) {
            if (hotspot == null) {
                continue;
            }
            ReferenceHotspotDTO hotspotCopy = new ReferenceHotspotDTO();
            hotspotCopy.setX(hotspot.getX());
            hotspotCopy.setY(hotspot.getY());
            hotspotCopy.setZone(hotspot.getZone());
            hotspotCopy.setLabel(hotspot.getLabel());
            hotspotCopy.setDescription(hotspot.getDescription());
            hotspotCopy.setBase64Image(hotspot.getBase64Image());
            copy.hotspots.add(hotspotCopy);
        }
        copy.numberColors = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, java.util.List<String>> entry : numberColors.entrySet()) {
            copy.numberColors.put(entry.getKey(),
                    entry.getValue() != null ? new java.util.ArrayList<>(entry.getValue()) : null);
        }
        return copy;
    }

    // --- Backwards Compatibility for Old Project Files ---
    @com.fasterxml.jackson.annotation.JsonSetter("genero")
    public void setOldGenero(TipoGenero genero) {
        if (this.currentGenero == null) this.currentGenero = genero;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("corte")
    public void setOldCorte(TipoCorte corte) {
        if (this.currentCorte == null) this.currentCorte = corte;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("corteShort")
    public void setOldCorteShort(TipoCorte corteShort) {
        if (this.currentCorteShort == null) this.currentCorteShort = corteShort;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("largo")
    public void setOldLargo(TipoLargo largo) {
        if (this.currentLargo == null) this.currentLargo = largo;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("cuello")
    public void setOldCuello(TipoCuello cuello) {
        if (this.currentCuello == null) this.currentCuello = cuello;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("tela")
    public void setOldTela(TipoTela tela) {
        if (this.currentTela == null) this.currentTela = tela;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("tipoMedias")
    public void setOldTipoMedias(TipoMedias tipoMedias) {
        if (this.currentTipoMedias == null) this.currentTipoMedias = tipoMedias;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("tipoPrenda")
    public void setOldTipoPrenda(String tipoPrenda) {
        if (this.currentGarmentType == null) this.currentGarmentType = tipoPrenda;
        
        // Old versions always had the shirt enabled implicitly by having a Garment Config.
        this.hasShirt = true;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("tipoEscudo")
    public void setOldTipoEscudo(String tipoEscudo) {
        if (this.shirtCrestTech == null) this.shirtCrestTech = tipoEscudo;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("adicionales")
    public void setOldAdicionales(java.util.Map<String, Boolean> adicionales) {
        if (adicionales != null) {
            if (adicionales.containsKey("malla")) this.hasMesh = adicionales.get("malla");
            if (adicionales.containsKey("punoCamiseta")) this.hasCuffs = adicionales.get("punoCamiseta");
            if (adicionales.containsKey("franjaCamiseta")) this.hasShirtStripe = adicionales.get("franjaCamiseta");
            if (adicionales.containsKey("short")) this.hasShorts = adicionales.get("short");
            if (adicionales.containsKey("medias")) this.hasSocks = adicionales.get("medias");
            if (adicionales.containsKey("punoShort")) this.hasShortsCuff = adicionales.get("punoShort");
            if (adicionales.containsKey("franjaShort")) this.hasShortsStripe = adicionales.get("franjaShort");
            if (adicionales.containsKey("piqueteShort")) this.hasShortsPicket = adicionales.get("piqueteShort");
            if (adicionales.containsKey("bolsilloShort")) this.hasShortsPocket = adicionales.get("bolsilloShort");
            if (adicionales.containsKey("pasadorShort")) this.hasShortsCord = adicionales.get("pasadorShort");
            if (adicionales.containsKey("forroShort")) this.hasShortsLining = adicionales.get("forroShort");
            if (adicionales.containsKey("ligaMedias")) this.hasSocksTop = adicionales.get("ligaMedias");
            if (adicionales.containsKey("acolchado")) this.hasPadding = adicionales.get("acolchado");
        }
    }
    // ---------------------------------------------------

    // Getters and Setters
    public TipoGenero getCurrentGenero() {
        return currentGenero;
    }

    public void setCurrentGenero(TipoGenero currentGenero) {
        this.currentGenero = currentGenero;
    }

    public TipoCorte getCurrentCorte() {
        return currentCorte;
    }

    public void setCurrentCorte(TipoCorte currentCorte) {
        this.currentCorte = currentCorte;
    }

    public TipoCorte getCurrentCorteShort() {
        return currentCorteShort;
    }

    public void setCurrentCorteShort(TipoCorte currentCorteShort) {
        this.currentCorteShort = currentCorteShort;
    }

    public TipoLargo getCurrentLargo() {
        return currentLargo;
    }

    public void setCurrentLargo(TipoLargo currentLargo) {
        this.currentLargo = currentLargo;
    }

    public TipoCuello getCurrentCuello() {
        return currentCuello;
    }

    public void setCurrentCuello(TipoCuello currentCuello) {
        this.currentCuello = currentCuello;
    }

    public String getCurrentGarmentType() {
        return currentGarmentType;
    }

    public void setCurrentGarmentType(String currentGarmentType) {
        this.currentGarmentType = currentGarmentType;
    }

    public boolean isHasShirt() {
        return hasShirt;
    }

    public void setHasShirt(boolean hasShirt) {
        this.hasShirt = hasShirt;
    }

    public boolean isHasMesh() {
        return hasMesh;
    }

    public void setHasMesh(boolean hasMesh) {
        this.hasMesh = hasMesh;
    }

    public boolean isHasCuffs() {
        return hasCuffs;
    }

    public void setHasCuffs(boolean hasCuffs) {
        this.hasCuffs = hasCuffs;
    }

    public boolean isHasShirtStripe() {
        return hasShirtStripe;
    }

    public void setHasShirtStripe(boolean hasShirtStripe) {
        this.hasShirtStripe = hasShirtStripe;
    }

    public boolean isHasShorts() {
        return hasShorts;
    }

    public void setHasShorts(boolean hasShorts) {
        this.hasShorts = hasShorts;
    }

    public boolean isHasShortsStripe() {
        return hasShortsStripe;
    }

    public void setHasShortsStripe(boolean hasShortsStripe) {
        this.hasShortsStripe = hasShortsStripe;
    }

    public boolean isHasShortsPicket() {
        return hasShortsPicket;
    }

    public void setHasShortsPicket(boolean hasShortsPicket) {
        this.hasShortsPicket = hasShortsPicket;
    }

    public boolean isHasShortsPocket() {
        return hasShortsPocket;
    }

    public void setHasShortsPocket(boolean hasShortsPocket) {
        this.hasShortsPocket = hasShortsPocket;
    }

    public boolean isHasShortsCuff() {
        return hasShortsCuff;
    }

    public void setHasShortsCuff(boolean hasShortsCuff) {
        this.hasShortsCuff = hasShortsCuff;
    }

    public boolean isHasShortsCord() {
        return hasShortsCord;
    }

    public void setHasShortsCord(boolean hasShortsCord) {
        this.hasShortsCord = hasShortsCord;
    }

    public boolean isHasShortsLining() {
        return hasShortsLining;
    }

    public void setHasShortsLining(boolean hasShortsLining) {
        this.hasShortsLining = hasShortsLining;
    }

    public boolean isHasSocks() {
        return hasSocks;
    }

    public void setHasSocks(boolean hasSocks) {
        this.hasSocks = hasSocks;
    }

    public boolean isHasSocksTop() {
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

    public boolean isShortCrestVisible() {
        return shortCrestVisible;
    }

    public void setShortCrestVisible(boolean shortCrestVisible) {
        this.shortCrestVisible = shortCrestVisible;
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

    public java.util.Map<String, String> getColors() {
        return colors;
    }

    public void setColors(java.util.Map<String, String> colors) {
        this.colors = colors;
    }

    public String getReferenceColorArquero() {
        return referenceColorArquero;
    }

    public void setReferenceColorArquero(String referenceColorArquero) {
        this.referenceColorArquero = referenceColorArquero;
    }

    public java.util.Map<String, String> getInternalCodes() {
        return internalCodes;
    }

    public void setInternalCodes(java.util.Map<String, String> internalCodes) {
        this.internalCodes = internalCodes;
    }

    public TipoTela getCurrentTela() {
        return currentTela;
    }

    public void setCurrentTela(TipoTela currentTela) {
        this.currentTela = currentTela;
    }

    public String getCustomTela() {
        return customTela;
    }

    public void setCustomTela(String customTela) {
        this.customTela = customTela;
    }

    public TipoMedias getCurrentTipoMedias() {
        return currentTipoMedias;
    }

    public void setCurrentTipoMedias(TipoMedias currentTipoMedias) {
        this.currentTipoMedias = currentTipoMedias;
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

    public java.util.List<ReferenceHotspotDTO> getHotspots() {
        return hotspots;
    }

    public void setHotspots(java.util.List<ReferenceHotspotDTO> hotspots) {
        this.hotspots = hotspots;
    }

    public java.util.Map<String, java.util.List<String>> getNumberColors() {
        return numberColors;
    }

    public void setNumberColors(java.util.Map<String, java.util.List<String>> numberColors) {
        this.numberColors = numberColors;
    }

    public double getChestNumberX() { return chestNumberX; }
    public void setChestNumberX(double v) { this.chestNumberX = v; }
    public double getChestNumberY() { return chestNumberY; }
    public void setChestNumberY(double v) { this.chestNumberY = v; }
    public double getChestNumberScale() { return chestNumberScale; }
    public void setChestNumberScale(double v) { this.chestNumberScale = v; }

    public double getBackNumberX() { return backNumberX; }
    public void setBackNumberX(double v) { this.backNumberX = v; }
    public double getBackNumberY() { return backNumberY; }
    public void setBackNumberY(double v) { this.backNumberY = v; }
    public double getBackNumberScale() { return backNumberScale; }
    public void setBackNumberScale(double v) { this.backNumberScale = v; }

    public double getShortNumberX() { return shortNumberX; }
    public void setShortNumberX(double v) { this.shortNumberX = v; }
    public double getShortNumberY() { return shortNumberY; }
    public void setShortNumberY(double v) { this.shortNumberY = v; }
    public double getShortNumberScale() { return shortNumberScale; }
    public void setShortNumberScale(double v) { this.shortNumberScale = v; }

    public boolean isHasPadding() { return hasPadding; }
    public void setHasPadding(boolean hasPadding) { this.hasPadding = hasPadding; }
}
