package org.example.component.renderer;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.example.utils.SVGCache;
import org.example.component.renderer.GarmentRenderer;

/**
 * Abstract base for garment renderers providing common utilities.
 */
public abstract class BaseGarmentRenderer implements GarmentRenderer {
    protected final Group group = new Group();
    protected final Group detailGroup = new Group();
    protected boolean visible = true;
    protected boolean hasLinea = false;

    public abstract void setShirtLinea(boolean hasLinea);

    // NEW: Speed optimization for color application. Maps SVG Zone ID to actual path objects.
    protected final java.util.Map<String, java.util.List<SVGPath>> zoneMap = new java.util.HashMap<>();

    public java.util.Map<String, java.util.List<SVGPath>> getZoneMap() { return zoneMap; }
    
    protected void trackZoneNode(String zone, SVGPath path) {
        zoneMap.computeIfAbsent(zone, k -> new java.util.ArrayList<>()).add(path);
    }

    // All state is now delegated to 'state' object.
    private Runnable onStateChanged;

    private boolean notificationsSuspended = false;

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        group.setVisible(visible);
        detailGroup.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void addToGroup(Group parentGroup) {
        parentGroup.getChildren().add(group);
        // We do NOT add detailGroup here automatically,
        // because PrendaVisualizer needs to add it manually ON TOP of user layers.
    }

    public Group getDetailGroup() {
        return detailGroup;
    }

    public Group getGroup() {
        return group;
    }

    protected void configureLayer(SVGPath path, Color fill, Color stroke) {
        path.setFill(fill);
        path.setStroke(stroke);
        path.setStrokeWidth(1);
    }

    protected void configureShadowLayer(SVGPath path) {
        path.setFill(Color.web("#000000", 0.1));
        path.setMouseTransparent(true);
    }

    protected void configureDetailLayer(SVGPath path, Color color) {
        path.setFill(color);
        path.setMouseTransparent(true);
    }

    protected void configureOutlineLayer(SVGPath path, Color strokeColor) {
        path.setFill(javafx.scene.paint.Color.TRANSPARENT);
        path.setStroke(strokeColor);
        path.setStrokeWidth(1);
        path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setMouseTransparent(true);
    }

    protected Color sanitizeFillColor(Color input) {
        if (input == null)
            return null;
        // Restored per user request: Force deep blacks to be lighter (#111111)
        // so they don't blend with the strict Black outline, but keeping it "Intense".
        double brightness = input.getBrightness();
        if (brightness < 0.12) {
            return Color.web("#111111"); // Intense Textile Black (Negro Fuerte)
        }
        return input;
    }

    protected Color getContrastStroke(Color fill) {
        // Enforce Strict Black Outline as per user/company requirement.
        if (fill == null) return Color.BLACK;
        return Color.BLACK;
    }

    protected void loadLayerWithExtras(SVGPath baseNode, SVGPath shadowNode, SVGPath detailNode, String basePath) {
        String content = SVGCache.loadPath(basePath);
        safeSetContent(baseNode, content);

        if (!content.isEmpty()) {
            String shadowPath = basePath.replace(".svg", "_sombra.svg");
            String detailPath = basePath.replace(".svg", "_detalle.svg");
            safeSetContent(shadowNode, SVGCache.loadOptionalPath(shadowPath));
            safeSetContent(detailNode, SVGCache.loadOptionalPath(detailPath));
        } else {
            safeSetContent(shadowNode, "");
            safeSetContent(detailNode, "");
        }
    }

    public void setEffect(javafx.scene.effect.Effect e) {
        group.setEffect(e);
        detailGroup.setEffect(e);
    }

    @Override
    public void setOpacity(double opacity) {
        group.setOpacity(opacity);
        detailGroup.setOpacity(opacity);
    }

    /**
     * Updates path content only if it has changed to prevent micro-flickering in JavaFX.
     */
    protected void safeSetContent(javafx.scene.shape.SVGPath path, String content) {
        if (path == null) return;
        String current = path.getContent();
        if (content == null) content = "";
        if (!content.equals(current)) {
            path.setContent(content);
        }
    }
}

