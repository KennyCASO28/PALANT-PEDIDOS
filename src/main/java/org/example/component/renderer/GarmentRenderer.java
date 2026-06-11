package org.example.component.renderer;

import javafx.scene.paint.Color;
import java.util.Map;

/**
 * Base interface for specialized garment renderers.
 */
public interface GarmentRenderer {
    void updateLayers(String gender, String cut, String length, String collar);

    void applyColors(Map<String, Color> colorState);

    public void setVisible(boolean visible);

    public boolean isVisible();

    public void addToGroup(javafx.scene.Group parent);

    public void setEffect(javafx.scene.effect.Effect e);

    void setOpacity(double opacity);

    // Optional methods for different renderer types
    default void setMeshVisible(boolean visible) {}
    default void setCuffsVisible(boolean visible) {}
    default void setStripeVisible(boolean visible) {}
    default void updateBranding(boolean visible, String basePath, String detailPath) {}
    
    default javafx.scene.shape.SVGPath getBody() { return null; }
    default javafx.scene.shape.SVGPath getSleeves() { return null; }
    default javafx.scene.shape.SVGPath getBrandBase() { return null; }
    default javafx.scene.shape.SVGPath getBrandDetail() { return null; }
}
