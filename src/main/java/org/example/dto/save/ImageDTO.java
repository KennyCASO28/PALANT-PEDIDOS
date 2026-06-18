package org.example.dto.save;

public class ImageDTO extends LayerDTO {
    private String base64Content; // Embedded image
    private String imagePath; // External link (optional backup)
    private double cropX, cropY, cropW, cropH;
    private boolean isCropMode;
    private String badgeType; // For persistence of Shield/Patch type

    public ImageDTO() {
    }

    // Getters and Setters
    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double getCropX() {
        return cropX;
    }

    public void setCropX(double cropX) {
        this.cropX = cropX;
    }

    public double getCropY() {
        return cropY;
    }

    public void setCropY(double cropY) {
        this.cropY = cropY;
    }

    public double getCropW() {
        return cropW;
    }

    public void setCropW(double cropW) {
        this.cropW = cropW;
    }

    public double getCropH() {
        return cropH;
    }

    public void setCropH(double cropH) {
        this.cropH = cropH;
    }

    public boolean isCropMode() {
        return isCropMode;
    }

    public void setCropMode(boolean cropMode) {
        isCropMode = cropMode;
    }

    public String getBadgeType() {
        return badgeType;
    }

    public void setBadgeType(String badgeType) {
        this.badgeType = badgeType;
    }

    @Override
    public LayerDTO deepCopy() {
        ImageDTO copy = new ImageDTO();
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

        copy.setBase64Content(this.base64Content);
        copy.setImagePath(this.imagePath);
        copy.setCropX(this.cropX);
        copy.setCropY(this.cropY);
        copy.setCropW(this.cropW);
        copy.setCropH(this.cropH);
        copy.setCropMode(this.isCropMode);
        copy.setBadgeType(this.badgeType);
        return copy;
    }
}

