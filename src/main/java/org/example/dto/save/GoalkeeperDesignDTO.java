package org.example.dto.save;

import java.util.ArrayList;
import java.util.List;

public class GoalkeeperDesignDTO {
    private String designId;
    private int order;
    private String label;
    private boolean personalized;
    private boolean selectedForTechnicalSheet;
    private PrendaStateDTO garmentConfig;
    private List<LayerDTO> layers = new ArrayList<>();

    public GoalkeeperDesignDTO() {
    }

    public GoalkeeperDesignDTO deepCopy() {
        GoalkeeperDesignDTO copy = new GoalkeeperDesignDTO();
        copy.designId = designId;
        copy.order = order;
        copy.label = label;
        copy.personalized = personalized;
        copy.selectedForTechnicalSheet = selectedForTechnicalSheet;
        copy.garmentConfig = garmentConfig != null ? garmentConfig.deepCopy() : null;
        copy.layers = new ArrayList<>();
        if (layers != null) {
            for (LayerDTO layer : layers) {
                if (layer != null) {
                    copy.layers.add(layer.deepCopy());
                }
            }
        }
        return copy;
    }

    public String getDesignId() {
        return designId;
    }

    public void setDesignId(String designId) {
        this.designId = designId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isPersonalized() {
        return personalized;
    }

    public void setPersonalized(boolean personalized) {
        this.personalized = personalized;
    }

    public boolean isSelectedForTechnicalSheet() {
        return selectedForTechnicalSheet;
    }

    public void setSelectedForTechnicalSheet(boolean selectedForTechnicalSheet) {
        this.selectedForTechnicalSheet = selectedForTechnicalSheet;
    }

    public PrendaStateDTO getGarmentConfig() {
        return garmentConfig;
    }

    public void setGarmentConfig(PrendaStateDTO garmentConfig) {
        this.garmentConfig = garmentConfig;
    }

    public List<LayerDTO> getLayers() {
        return layers;
    }

    public void setLayers(List<LayerDTO> layers) {
        this.layers = layers != null ? layers : new ArrayList<>();
    }
}
