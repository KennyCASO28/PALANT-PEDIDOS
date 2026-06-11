package org.example.model;

import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineJoin;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the complete state of a ShapeLayer.
 * Facilitates serialization, undo/redo, and scalability.
 */
public class ShapeLayerState {
    // Basic Geometry
    public ShapeType type = ShapeType.RECTANGLE;
    public double width = 100;
    public double height = 100;
    public double visualMinX = 0;
    public double visualMinY = 0;
    public double arcWidth = 0;
    public double arcHeight = 0;
    public boolean isClosed = true;

    // Fill & Stroke
    public Color fillColor = Color.RED;
    public Color strokeColor = Color.BLACK;
    public double strokeWidth = 1.0;
    public StrokeLineJoin strokeLineJoin = StrokeLineJoin.ROUND;

    // Custom Geometry
    public String svgPathData;
    public List<BezierNode> bezierNodes = new ArrayList<>();
    public List<BezierNode> originalBezierNodes = new ArrayList<>();
    public List<Double> customPoints = new ArrayList<>();

    // Contour / Outer Glow
    public int contourSteps = 0;
    public double contourDistance = 0;
    public Color contourColor = Color.BLACK;
    public StrokeLineJoin contourLineJoin = StrokeLineJoin.MITER;

    // Transparency & Gradients
    public boolean isGradientTransparency = false;
    public double transparencyAngle = 0;
    public double transparencyStartAlpha = 1.0;
    public double transparencyEndAlpha = 1.0;
    public double transparencyBalance = 0.5;

    // Logic & Placement
    public String activeZone;
    public boolean isLocked = false;
    public boolean isUserLocked = false;

    public ShapeLayerState copy() {
        ShapeLayerState copy = new ShapeLayerState();
        copy.type = this.type;
        copy.width = this.width;
        copy.height = this.height;
        copy.visualMinX = this.visualMinX;
        copy.visualMinY = this.visualMinY;
        copy.arcWidth = this.arcWidth;
        copy.arcHeight = this.arcHeight;
        copy.isClosed = this.isClosed;
        copy.fillColor = this.fillColor;
        copy.strokeColor = this.strokeColor;
        copy.strokeWidth = this.strokeWidth;
        copy.strokeLineJoin = this.strokeLineJoin;
        copy.svgPathData = this.svgPathData;
        if (this.bezierNodes != null) copy.bezierNodes = new ArrayList<>(this.bezierNodes);
        if (this.originalBezierNodes != null) copy.originalBezierNodes = new ArrayList<>(this.originalBezierNodes);
        if (this.customPoints != null) copy.customPoints = new ArrayList<>(this.customPoints);
        copy.contourSteps = this.contourSteps;
        copy.contourDistance = this.contourDistance;
        copy.contourColor = this.contourColor;
        copy.contourLineJoin = this.contourLineJoin;
        copy.isGradientTransparency = this.isGradientTransparency;
        copy.transparencyAngle = this.transparencyAngle;
        copy.transparencyStartAlpha = this.transparencyStartAlpha;
        copy.transparencyEndAlpha = this.transparencyEndAlpha;
        copy.transparencyBalance = this.transparencyBalance;
        copy.activeZone = this.activeZone;
        copy.isLocked = this.isLocked;
        copy.isUserLocked = this.isUserLocked;
        return copy;
    }
}
