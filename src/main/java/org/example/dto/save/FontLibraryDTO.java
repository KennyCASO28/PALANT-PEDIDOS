package org.example.dto.save;

/**
 * Data Transfer Object for embedding font files in project saves.
 */
public class FontLibraryDTO {
    private String familyName;
    private String base64Content;
    private String extension; // ttf, otf, etc.

    public FontLibraryDTO() {}

    public FontLibraryDTO(String familyName, String base64Content, String extension) {
        this.familyName = familyName;
        this.base64Content = base64Content;
        this.extension = extension;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
