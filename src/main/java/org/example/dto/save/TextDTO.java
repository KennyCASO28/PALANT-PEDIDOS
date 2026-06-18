package org.example.dto.save;

public class TextDTO extends LayerDTO {
    private String text;
    private String fontFamily;
    private double fontSize;
    private String color; // Hex
    private boolean bold;
    private boolean italic;
    private String shapeType; // For TextShape enum (ARC, etc.)

    // New Fields for advanced text restoration
    private String strokeColor; // Hex
    private double strokeWidth;
    private double arcFactor;
    private double heightScale;
    private double widthScale;
    private double currentWidth;
    private double currentHeight;

    // Contour (Professional silhouette)
    private int contourSteps;
    private double contourDistance;
    private String contourColor; // Hex

    public TextDTO() {
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public String getShapeType() {
        return shapeType;
    }

    public void setShapeType(String shapeType) {
        this.shapeType = shapeType;
    }

    public String getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(String strokeColor) {
        this.strokeColor = strokeColor;
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public double getArcFactor() {
        return arcFactor;
    }

    public void setArcFactor(double arcFactor) {
        this.arcFactor = arcFactor;
    }

    public double getHeightScale() {
        return heightScale;
    }

    public void setHeightScale(double heightScale) {
        this.heightScale = heightScale;
    }

    public double getWidthScale() {
        return widthScale;
    }

    public void setWidthScale(double widthScale) {
        this.widthScale = widthScale;
    }

    public double getCurrentWidth() {
        return currentWidth;
    }

    public void setCurrentWidth(double currentWidth) {
        this.currentWidth = currentWidth;
    }

    public double getCurrentHeight() {
        return currentHeight;
    }

    public void setCurrentHeight(double currentHeight) {
        this.currentHeight = currentHeight;
    }

    public int getContourSteps() { return contourSteps; }
    public void setContourSteps(int steps) { this.contourSteps = steps; }
    public double getContourDistance() { return contourDistance; }
    public void setContourDistance(double dist) { this.contourDistance = dist; }
    public String getContourColor() { return contourColor; }
    public void setContourColor(String color) { this.contourColor = color; }

    @Override
    public LayerDTO deepCopy() {
        TextDTO copy = new TextDTO();
        copy.setX(getX());
        copy.setY(getY());
        copy.setScaleX(getScaleX());
        copy.setScaleY(getScaleY());
        copy.setShearX(getShearX());
        copy.setShearY(getShearY());
        copy.setCustomPivotX(getCustomPivotX());
        copy.setCustomPivotY(getCustomPivotY());
        copy.setRotation(getRotation());
        copy.setZIndex(getZIndex());
        copy.setLocked(isLocked());
        copy.setActiveZone(getActiveZone());
        copy.setWidth(getWidth());
        copy.setHeight(getHeight());

        copy.setText(this.text);
        copy.setFontFamily(this.fontFamily);
        copy.setFontSize(this.fontSize);
        copy.setColor(this.color);
        copy.setBold(this.bold);
        copy.setItalic(this.italic);
        copy.setShapeType(this.shapeType);
        copy.setStrokeColor(this.strokeColor);
        copy.setStrokeWidth(this.strokeWidth);
        copy.setArcFactor(this.arcFactor);
        copy.setHeightScale(this.heightScale);
        copy.setWidthScale(this.widthScale);
        copy.setCurrentWidth(this.currentWidth);
        copy.setCurrentHeight(this.currentHeight);
        copy.setContourSteps(this.contourSteps);
        copy.setContourDistance(this.contourDistance);
        copy.setContourColor(this.contourColor);
        return copy;
    }
}

