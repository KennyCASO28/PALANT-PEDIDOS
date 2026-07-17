package org.example.component;

import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * Common interface for all interactive layers (Shape, Image, Text).
 */
public interface GraphicLayer {
    void setSelected(boolean selected);
    boolean isSelected();
    
    void setLocked(boolean locked);
    void setUserLocked(boolean locked);
    void setSystemLocked(boolean locked);
    boolean isLocked();
    boolean isUserLocked();
    
    void setVisualizer(PrendaVisualizer visualizer);
    PrendaVisualizer getVisualizer();
    
    String getActiveZone();
    void setActiveZone(String zone);
    
    void updateVisuals();
    void render();
    
    // Group State
    void setGrouped(boolean grouped);
    boolean isGrouped();
    
    // Logical Dimensions
    default double getLogicalWidth() { return 0; }
    default double getLogicalHeight() { return 0; }
    default void setSize(double w, double h) {}
    
    // Transforms
    double getInternalRotation();
    void setInternalRotation(double angle);
    
    double getInternalScaleX();
    void setInternalScaleX(double s);
    
    double getInternalScaleY();
    void setInternalScaleY(double s);
    
    double getInternalShearX();
    void setInternalShearX(double s);

    double getInternalShearY();
    void setInternalShearY(double s);

    double getCustomPivotX();
    void setCustomPivotX(double x);

    double getCustomPivotY();
    void setCustomPivotY(double y);

    void recordUndoState();
    
    // Interaction Modes
    void setRotationMode(boolean active);
    boolean isRotationMode();
    
    // Styling
    default javafx.scene.paint.Color getFillColor() { return null; }
    default void setFillColor(javafx.scene.paint.Color color) {}
    default javafx.scene.paint.Color getStrokeColor() { return null; }
    default void setStrokeColor(javafx.scene.paint.Color color) {}
    default double getStrokeWidth() { return 0; }
    default void setStrokeWidth(double width) {}

    Node getNode();
}
