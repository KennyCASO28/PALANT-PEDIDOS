package org.example.dto.save;

import java.util.ArrayList;
import java.util.List;

public class ProjectState {
    private String version = "1.0";
    private long timestamp;

    // Order Data
    private List<DetallePedidoDTO> orderDetails = new ArrayList<>();

    // Shipping / Client Metadata
    private org.example.dto.DatosEnvioDTO shippingInfo;

    // Garment Configuration
    private PrendaStateDTO garmentConfig;

    // Arquero Design (optional)
    private PrendaStateDTO arqueroGarmentConfig;
    private List<LayerDTO> arqueroLayers = new ArrayList<>();
    private List<GoalkeeperDesignDTO> goalkeeperDesigns = new ArrayList<>();
    private String selectedGoalkeeperDesignId;

    private boolean arqueroPersonalizado;
    private List<LayerDTO> layers = new ArrayList<>();

    // Image Libraries (Base64)
    private List<String> logoLibrary = new ArrayList<>();
    private List<String> shieldLibrary = new ArrayList<>();
    private List<String> shieldLibraryShirt = new ArrayList<>();
    private List<String> shieldLibraryShort = new ArrayList<>();
    private List<String> shieldLibrarySleeve = new ArrayList<>();
    private List<String> referenceLibrary = new ArrayList<>();
    private List<FontLibraryDTO> fontLibrary = new ArrayList<>();

    // Additional Metadata could go here (e.g., Client Name, Date)

    public ProjectState() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<DetallePedidoDTO> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(List<DetallePedidoDTO> orderDetails) {
        this.orderDetails = orderDetails;
    }

    public PrendaStateDTO getGarmentConfig() {
        return garmentConfig;
    }

    public void setGarmentConfig(PrendaStateDTO garmentConfig) {
        this.garmentConfig = garmentConfig;
    }

    public PrendaStateDTO getArqueroGarmentConfig() {
        return arqueroGarmentConfig;
    }

    public void setArqueroGarmentConfig(PrendaStateDTO arqueroGarmentConfig) {
        this.arqueroGarmentConfig = arqueroGarmentConfig;
    }

    public List<LayerDTO> getArqueroLayers() {
        return arqueroLayers;
    }

    public void setArqueroLayers(List<LayerDTO> arqueroLayers) {
        this.arqueroLayers = (arqueroLayers != null) ? arqueroLayers : new ArrayList<>();
    }

    public List<GoalkeeperDesignDTO> getGoalkeeperDesigns() {
        return goalkeeperDesigns;
    }

    public void setGoalkeeperDesigns(List<GoalkeeperDesignDTO> goalkeeperDesigns) {
        this.goalkeeperDesigns = goalkeeperDesigns != null ? goalkeeperDesigns : new ArrayList<>();
    }

    public String getSelectedGoalkeeperDesignId() {
        return selectedGoalkeeperDesignId;
    }

    public void setSelectedGoalkeeperDesignId(String selectedGoalkeeperDesignId) {
        this.selectedGoalkeeperDesignId = selectedGoalkeeperDesignId;
    }

    public List<LayerDTO> getLayers() {
        return layers;
    }

    public void setLayers(List<LayerDTO> layers) {
        this.layers = layers;
    }

    public void addLayer(LayerDTO layer) {
        this.layers.add(layer);
    }

    public List<String> getLogoLibrary() {
        return logoLibrary;
    }

    public void setLogoLibrary(List<String> logoLibrary) {
        this.logoLibrary = logoLibrary;
    }

    public List<String> getShieldLibrary() {
        return shieldLibrary;
    }

    public void setShieldLibrary(List<String> shieldLibrary) {
        this.shieldLibrary = shieldLibrary;
    }

    public List<String> getReferenceLibrary() {
        return referenceLibrary;
    }

    public void setReferenceLibrary(List<String> referenceLibrary) {
        this.referenceLibrary = referenceLibrary;
    }

    public List<String> getShieldLibraryShirt() {
        return shieldLibraryShirt;
    }

    public void setShieldLibraryShirt(List<String> shieldLibraryShirt) {
        this.shieldLibraryShirt = shieldLibraryShirt;
    }

    public List<String> getShieldLibraryShort() {
        return shieldLibraryShort;
    }

    public void setShieldLibraryShort(List<String> shieldLibraryShort) {
        this.shieldLibraryShort = shieldLibraryShort;
    }

    public List<String> getShieldLibrarySleeve() {
        return shieldLibrarySleeve;
    }

    public void setShieldLibrarySleeve(List<String> shieldLibrarySleeve) {
        this.shieldLibrarySleeve = shieldLibrarySleeve;
    }

    public org.example.dto.DatosEnvioDTO getShippingInfo() {
        return shippingInfo;
    }

    public void setShippingInfo(org.example.dto.DatosEnvioDTO shippingInfo) {
        this.shippingInfo = shippingInfo;
    }

    public boolean isArqueroPersonalizado() {
        return arqueroPersonalizado;
    }

    public void setArqueroPersonalizado(boolean arqueroPersonalizado) {
        this.arqueroPersonalizado = arqueroPersonalizado;
    }

    public List<FontLibraryDTO> getFontLibrary() {
        return fontLibrary;
    }

    public void setFontLibrary(List<FontLibraryDTO> fontLibrary) {
        this.fontLibrary = fontLibrary;
    }
}

