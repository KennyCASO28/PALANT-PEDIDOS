package org.example.dto.save;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ShapeDTO.class, name = "SHAPE"),
        @JsonSubTypes.Type(value = ImageDTO.class, name = "IMAGE"),
        @JsonSubTypes.Type(value = TextDTO.class, name = "TEXT"),
        @JsonSubTypes.Type(value = GroupDTO.class, name = "GROUP")
})
public abstract class LayerDTO {
    private double x;
    private double y;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double rotation;
    private int zIndex;
    private boolean locked;
    private String activeZone; // For PowerClip
    private double width;
    private double height;

    public LayerDTO() {
    }

    public LayerDTO(double x, double y, double scaleX, double scaleY, double rotation, int zIndex) {
        this.x = x;
        this.y = y;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.rotation = rotation;
        this.zIndex = zIndex;
    }

    // Getters and Setters
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

    public double getScaleX() {
        return scaleX;
    }

    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    public double getScaleY() {
        return scaleY;
    }

    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getActiveZone() {
        return activeZone;
    }

    public void setActiveZone(String activeZone) {
        this.activeZone = activeZone;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public abstract LayerDTO deepCopy();
}

