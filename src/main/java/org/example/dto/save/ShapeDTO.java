package org.example.dto.save;

import org.example.model.ShapeType;

public class ShapeDTO extends LayerDTO {
    private ShapeType shapeType;
    private String fillColor; // Hex or Gradient definition
    private String strokeColor; // Hex
    private double strokeWidth;
    private String strokeType; // Inside, Outside, Centered/Shared
    private boolean isClosed; // For paths
    // Support for Custom Paths
    private String svgContent;
    private java.util.List<BezierNodeDTO> bezierNodes;

    // Rounded Corners Support
    private double arcWidth;
    private double arcHeight;

    // Contour (Silhouette) Support
    private int contourSteps;
    private double contourDistance;
    private String contourColor; // Hex
    private String contourLineJoin; // Enum name

    // Gradient Transparency Support
    private boolean isGradientTransparency;
    private double transparencyAngle;
    private double transparencyStartAlpha;
    private double transparencyEndAlpha;
    private double transparencyBalance;

    public ShapeDTO() {
    }

    // Getters and Setters
    public ShapeType getShapeType() {
        return shapeType;
    }

    public void setShapeType(ShapeType shapeType) {
        this.shapeType = shapeType;
    }

    public String getFillColor() {
        return fillColor;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
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

    public String getStrokeType() {
        return strokeType;
    }

    public void setStrokeType(String strokeType) {
        this.strokeType = strokeType;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public String getSvgContent() {
        return svgContent;
    }

    public void setSvgContent(String svgContent) {
        this.svgContent = svgContent;
    }

    public java.util.List<BezierNodeDTO> getBezierNodes() {
        return bezierNodes;
    }

    public void setBezierNodes(java.util.List<BezierNodeDTO> bezierNodes) {
        this.bezierNodes = bezierNodes;
    }

    public boolean isGradientTransparency() {
        return isGradientTransparency;
    }

    public void setGradientTransparency(boolean gradientTransparency) {
        isGradientTransparency = gradientTransparency;
    }

    public double getTransparencyAngle() {
        return transparencyAngle;
    }

    public void setTransparencyAngle(double transparencyAngle) {
        this.transparencyAngle = transparencyAngle;
    }

    public double getTransparencyStartAlpha() {
        return transparencyStartAlpha;
    }

    public void setTransparencyStartAlpha(double transparencyStartAlpha) {
        this.transparencyStartAlpha = transparencyStartAlpha;
    }

    public double getTransparencyEndAlpha() {
        return transparencyEndAlpha;
    }

    public void setTransparencyEndAlpha(double transparencyEndAlpha) {
        this.transparencyEndAlpha = transparencyEndAlpha;
    }

    public double getTransparencyBalance() {
        return transparencyBalance;
    }

    public void setTransparencyBalance(double transparencyBalance) {
        this.transparencyBalance = transparencyBalance;
    }

    public int getContourSteps() {
        return contourSteps;
    }

    public void setContourSteps(int contourSteps) {
        this.contourSteps = contourSteps;
    }

    public double getContourDistance() {
        return contourDistance;
    }

    public void setContourDistance(double contourDistance) {
        this.contourDistance = contourDistance;
    }

    public String getContourColor() {
        return contourColor;
    }

    public void setContourColor(String contourColor) {
        this.contourColor = contourColor;
    }

    public String getContourLineJoin() {
        return contourLineJoin;
    }

    public void setContourLineJoin(String contourLineJoin) {
        this.contourLineJoin = contourLineJoin;
    }

    public double getArcWidth() {
        return arcWidth;
    }

    public void setArcWidth(double arcWidth) {
        this.arcWidth = arcWidth;
    }

    public double getArcHeight() {
        return arcHeight;
    }

    public void setArcHeight(double arcHeight) {
        this.arcHeight = arcHeight;
    }

    @Override
    public LayerDTO deepCopy() {
        ShapeDTO copy = new ShapeDTO();
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

        copy.setShapeType(this.shapeType);
        copy.setFillColor(this.fillColor);
        copy.setStrokeColor(this.strokeColor);
        copy.setStrokeWidth(this.strokeWidth);
        copy.setStrokeType(this.strokeType);
        copy.setClosed(this.isClosed);
        copy.setSvgContent(this.svgContent);
        if (this.bezierNodes != null) {
            java.util.List<BezierNodeDTO> copiedNodes = new java.util.ArrayList<>();
            for (BezierNodeDTO n : this.bezierNodes) {
                BezierNodeDTO nc = new BezierNodeDTO();
                nc.setAnchorX(n.getAnchorX());
                nc.setAnchorY(n.getAnchorY());
                nc.setControl1X(n.getControl1X());
                nc.setControl1Y(n.getControl1Y());
                nc.setControl2X(n.getControl2X());
                nc.setControl2Y(n.getControl2Y());
                nc.setHasControl1(n.isHasControl1());
                nc.setHasControl2(n.isHasControl2());
                nc.setMoveTo(n.isMoveTo());
                nc.setType(n.getType());
                nc.setSegmentType(n.getSegmentType());
                copiedNodes.add(nc);
            }
            copy.setBezierNodes(copiedNodes);
        }
        copy.setArcWidth(this.arcWidth);
        copy.setArcHeight(this.arcHeight);
        copy.setContourSteps(this.contourSteps);
        copy.setContourDistance(this.contourDistance);
        copy.setContourColor(this.contourColor);
        copy.setContourLineJoin(this.contourLineJoin);
        copy.setGradientTransparency(this.isGradientTransparency);
        copy.setTransparencyAngle(this.transparencyAngle);
        copy.setTransparencyStartAlpha(this.transparencyStartAlpha);
        copy.setTransparencyEndAlpha(this.transparencyEndAlpha);
        copy.setTransparencyBalance(this.transparencyBalance);
        return copy;
    }
}

