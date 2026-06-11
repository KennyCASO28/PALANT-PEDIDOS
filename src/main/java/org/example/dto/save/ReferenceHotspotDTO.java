package org.example.dto.save;

public class ReferenceHotspotDTO {
    private double x;
    private double y;
    private String zone;
    private String label;
    private String description;
    private String base64Image;

    public ReferenceHotspotDTO() {}

    // Getters and Setters
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBase64Image() { return base64Image; }
    public void setBase64Image(String base64Image) { this.base64Image = base64Image; }
}

