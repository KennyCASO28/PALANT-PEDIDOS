package org.example.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * DTO for loading garment_assets.json configuration.
 */
public class GarmentAssetConfig {

    private Map<String, String> genders;
    private Map<String, String> cuts;
    private Map<String, String> lengths;
    private Map<String, String> templates;

    public Map<String, String> getGenders() { return genders; }
    public void setGenders(Map<String, String> genders) { this.genders = genders; }

    public Map<String, String> getCuts() { return cuts; }
    public void setCuts(Map<String, String> cuts) { this.cuts = cuts; }

    public Map<String, String> getLengths() { return lengths; }
    public void setLengths(Map<String, String> lengths) { this.lengths = lengths; }

    public Map<String, String> getTemplates() { return templates; }
    public void setTemplates(Map<String, String> templates) { this.templates = templates; }

    // Helper methods for easy lookup with fallbacks
    public String getGenderFolder(String gender) {
        return genders.getOrDefault(gender != null ? gender.toUpperCase() : "HOMBRE", "varon");
    }

    public String getCutFolder(String cut) {
        return cuts.getOrDefault(cut != null ? cut.toUpperCase() : "CUADRADO", "cuadrado");
    }

    public String getLengthFolder(String length) {
        return lengths.getOrDefault(length != null ? length.toUpperCase() : "MANGA_CORTA", "corta");
    }

    public String getTemplate(String key) {
        return templates.get(key);
    }
}
