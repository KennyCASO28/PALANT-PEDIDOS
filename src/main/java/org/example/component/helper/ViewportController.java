package org.example.component.helper;

import javafx.scene.Group;
import javafx.scene.layout.Region;

/**
 * Handles Zoom, Pan, and Auto-scaling logic for the PrendaVisualizer.
 * Respects SVG's natural positioning (viewBox).
 */
public class ViewportController {

    private final Region container;
    private final Group contentGroup;

    private double zoomFactor = 1.0;
    private double translateX = 0;
    private double translateY = 0;

    // Derived states for Rulers/Guides
    private double finalScale = 1.0;
    private double layoutOffsetX = 0;
    private double layoutOffsetY = 0;
    private double effectiveOffsetX = 0;
    private double effectiveOffsetY = 0;

    private final java.util.List<Runnable> onViewportChangedListeners = new java.util.ArrayList<>();

    public void setOnViewportChanged(Runnable callback) {
        // Legacy support
        this.onViewportChangedListeners.clear();
        this.onViewportChangedListeners.add(callback);
    }

    public void addOnViewportChanged(Runnable callback) {
        this.onViewportChangedListeners.add(callback);
    }

    public void removeOnViewportChanged(Runnable callback) {
        this.onViewportChangedListeners.remove(callback);
    }

    private void notifyViewportChanged() {
        for (Runnable r : onViewportChangedListeners) {
            if (r != null)
                r.run();
        }
    }

    // Zoom limits
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 8.0;

    // Pan state
    private double lastX, lastY;
    private boolean panningEnabled = true;

    // Zoom follows cursor mode
    private boolean zoomFollowsCursor = true;
    private double zoomAnchorSceneX = -1;
    private double zoomAnchorSceneY = -1;
    private double zoomBeforeScale = 1.0;
    private double layoutOffsetX_old = 0;
    private double layoutOffsetY_old = 0;
    private double translateX_old = 0;
    private double translateY_old = 0;

    public void setPanningEnabled(boolean enabled) {
        this.panningEnabled = enabled;
        if (!enabled) {
            container.setCursor(javafx.scene.Cursor.DEFAULT);
            // Restore mouse interaction for design layers when NOT panning
            contentGroup.setMouseTransparent(false);
        } else {
            container.setCursor(javafx.scene.Cursor.OPEN_HAND);
            // Disable mouse interaction for design layers while panning
            // so that locked patches/shields cannot be accidentally selected or moved
            contentGroup.setMouseTransparent(true);
        }
    }

    public boolean isPanningEnabled() {
        return panningEnabled;
    }

    public void setZoomFollowsCursor(boolean follows) {
        this.zoomFollowsCursor = follows;
    }

    public boolean isZoomFollowsCursor() {
        return zoomFollowsCursor;
    }

    public double getFinalScale() {
        return finalScale;
    }

    public javafx.beans.property.ReadOnlyDoubleProperty getContainerWidthProperty() {
        return container.widthProperty();
    }

    public javafx.beans.property.ReadOnlyDoubleProperty getContainerHeightProperty() {
        return container.heightProperty();
    }

    public double getLayoutOffsetX() {
        return layoutOffsetX;
    }

    public double getLayoutOffsetY() {
        return layoutOffsetY;
    }

    public double getEffectiveOffsetX() {
        return effectiveOffsetX;
    }

    public double getEffectiveOffsetY() {
        return effectiveOffsetY;
    }

    public double getTranslateX() {
        return translateX;
    }

    public double getTranslateY() {
        return translateY;
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void setViewportState(double zoom, double tx, double ty) {
        this.zoomFactor = zoom;
        this.translateX = tx;
        this.translateY = ty;
        autoScale();
    }

    public double screenToDesignX(double screenX) {
        return (screenX - effectiveOffsetX) / finalScale;
    }

    public double screenToDesignY(double screenY) {
        return (screenY - effectiveOffsetY) / finalScale;
    }

    public double designToScreenX(double designX) {
        return (designX * finalScale) + effectiveOffsetX;
    }

    public double designToScreenY(double designY) {
        return (designY * finalScale) + effectiveOffsetY;
    }

    public double sceneToDesignX(double sceneX) {
        javafx.geometry.Point2D local = container.sceneToLocal(sceneX, 0);
        return screenToDesignX(local.getX());
    }

    public double sceneToDesignY(double sceneY) {
        javafx.geometry.Point2D local = container.sceneToLocal(0, sceneY);
        return screenToDesignY(local.getY());
    }

    private javafx.animation.PauseTransition zoomCacheTimer;
    private boolean isZoomPending = false;

    public ViewportController(Region container, Group contentGroup) {
        this.container = container;
        this.contentGroup = contentGroup;
        this.contentGroup.setCache(false);
        this.zoomCacheTimer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(16));
        this.zoomCacheTimer.setOnFinished(e -> {
            isZoomPending = false;
            autoScale();
        });
        setupHandlers();
    }

    private void setupHandlers() {
        // Zoom (Scroll)
        container.setOnScroll(event -> {
            if (event.isControlDown() || true) {
                event.consume();

                double delta = event.getDeltaY();
                double scaleChange = (delta > 0) ? 1.1 : 0.9;

                // Cursor-aware zoom: solo si la pantalla está desbloqueada (panningEnabled)
                if (panningEnabled) {
                    zoomAnchorSceneX = event.getSceneX();
                    zoomAnchorSceneY = event.getSceneY();
                    zoomBeforeScale = finalScale;
                    layoutOffsetX_old = layoutOffsetX;
                    layoutOffsetY_old = layoutOffsetY;
                    translateX_old = translateX;
                    translateY_old = translateY;
                } else {
                    zoomAnchorSceneX = -1;
                    zoomAnchorSceneY = -1;
                }

                zoomFactor = zoomFactor * scaleChange;

                // Clamp Zoom - UNIFIED TO MAX_ZOOM (User Request)
                if (zoomFactor < MIN_ZOOM)
                    zoomFactor = MIN_ZOOM;
                if (zoomFactor > MAX_ZOOM)
                    zoomFactor = MAX_ZOOM;

                // Debounce zoom updates to prevent UI freeze during rapid scrolling
                if (!isZoomPending) {
                    isZoomPending = true;
                    zoomCacheTimer.playFromStart();
                }
            }
        });

        // Pan (Drag)
        container.setOnMousePressed(event -> {
            if (!panningEnabled || !event.isPrimaryButtonDown())
                return;
            lastX = event.getSceneX();
            lastY = event.getSceneY();
            container.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        });

        container.setOnMouseDragged(event -> {
            if (!panningEnabled || !event.isPrimaryButtonDown())
                return;
            double deltaX = (event.getSceneX() - lastX) * 0.7;
            double deltaY = (event.getSceneY() - lastY) * 0.7;

            translateX += deltaX;
            translateY += deltaY;

            lastX = event.getSceneX();
            lastY = event.getSceneY();

            autoScale();
        });

        container.setOnMouseReleased(event -> {
            if (!panningEnabled)
                return;
            container.setCursor(javafx.scene.Cursor.OPEN_HAND);

            // Clean up cache state just in case
            contentGroup.setCache(false);
        });

        container.setOnMouseEntered(event -> {
            if (panningEnabled) {
                container.setCursor(javafx.scene.Cursor.OPEN_HAND);
            }
        });

        container.setOnMouseExited(event -> {
            container.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        // Listen to resize
        container.widthProperty().addListener((o, v, n) -> javafx.application.Platform.runLater(this::autoScale));
        container.heightProperty().addListener((o, v, n) -> javafx.application.Platform.runLater(this::autoScale));
    }

    public void resetView() {
        this.zoomFactor = 1.0;
        this.translateX = 0;
        this.translateY = 0;
        autoScale();
    }

    /**
     * Forces a neutral, centered view using a VIRTUAL container size.
     * Used mainly for snapshots when the actual UI container might be hidden or
     * have no size.
     */
    public void autoScaleNeutral(double virtualWidth, double virtualHeight) {
        this.zoomFactor = 1.0;
        this.translateX = 0;
        this.translateY = 0;
        // Use Studio Mode (Pure Fit, no UI-specific logic)
        autoScaleInternal(virtualWidth, virtualHeight, true);
    }

    public void autoScale() {
        if (container != null) {
            autoScaleInternal(container.getWidth(), container.getHeight(), false);
        }
    }

    public void autoScale(double w, double h) {
        autoScaleInternal(w, h, false);
    }

    /**
     * Scales content to fit viewport while respecting SVG's natural positioning.
     * Does NOT try to center - respects the SVG's built-in viewBox/frame.
     */

    /**
     * Scales content to fit viewport.
     * DISTINCT LOGIC for Laptop vs Desktop to ensure usability on both.
     */
    /**
     * Scales content to fit viewport.
     * DISTINCT INITIAL SCALING for Laptop vs Desktop, but UNIFIED FREEDOM.
     */
    private void autoScaleInternal(double availableWidth, double availableHeight, boolean studioMode) {
        try {
            // OPTIMIZACIÓN EXTREMA: Delegamos todo cálculo de geometría al núcleo C++ de
            // JavaFX
            // usando "getBoundsInParent" pero solo para los hijos VISIBLES de primer nivel.
            // Así ignoramos prendas desactivadas (ej. la camiseta cuando solo hay short)
            // sin
            // ahogar la CPU con revisión recursiva profunda.
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            boolean hasVisibleChild = false;

            for (javafx.scene.Node child : contentGroup.getChildrenUnmodifiable()) {
                if (child.isVisible()) {
                    javafx.geometry.Bounds b = child.getBoundsInParent();
                    if (!b.isEmpty() && b.getWidth() > 0 && b.getHeight() > 0) {
                        if (b.getMinX() < minX)
                            minX = b.getMinX();
                        if (b.getMinY() < minY)
                            minY = b.getMinY();
                        if (b.getMaxX() > maxX)
                            maxX = b.getMaxX();
                        if (b.getMaxY() > maxY)
                            maxY = b.getMaxY();
                        hasVisibleChild = true;
                    }
                }
            }

            double contentWidth, contentHeight, contentMinX, contentMinY;
            if (hasVisibleChild) {
                contentMinX = minX;
                contentMinY = minY;
                contentWidth = maxX - minX;
                contentHeight = maxY - minY;
            } else {
                javafx.geometry.Bounds contentBounds = contentGroup.getBoundsInLocal();
                contentWidth = contentBounds.getWidth();
                contentHeight = contentBounds.getHeight();
                contentMinX = contentBounds.getMinX();
                contentMinY = contentBounds.getMinY();
            }

            if (availableWidth <= 0 || availableHeight <= 0 || contentWidth <= 0 || contentHeight <= 0)
                return;

            // --- 0. Detect Mode ---
            // boolean isLaptop = (availableHeight < 720); // Threshold for typical laptop
            // screens

            // --- 1. Base Scaling Strategy ---
            double scaleX = availableWidth / contentWidth;
            double scaleY = availableHeight / contentHeight;
            double baseFitScale = Math.min(scaleX, scaleY);

            double contentRatio = contentWidth / contentHeight;
            double finalBaseScale;

            // UNIFIED FREEDOM: Both modes get deep zoom and generous panning.
            double maxZoomLimit = MAX_ZOOM; // 8.0x - Requested by user: "Como en PC"
            // double panFreedomFactor = 0.6; // Requested: "Allow move" even on PC.

            if (studioMode) {
                // STUDIO MODE: Pure mathematical fit to 90% of frame
                finalBaseScale = baseFitScale * 0.90;
            } else {
                // UI MODE: Distinct favors for Laptop/Desktop
                boolean isLaptop = (availableHeight < 720); // Threshold for typical laptop screens
                if (isLaptop) {
                    // === LAPTOP INITIALIZATION (Safe) ===
                    double laptopFactor = 0.95; // Capped to 0.95 to never overflow by default

                    if (contentRatio > 1.2)
                        laptopFactor *= 0.90; // Wide Safety (Long Sleeves)
                    if (contentRatio < 0.9)
                        laptopFactor *= 0.90; // Tall Safety (Shorts)

                    finalBaseScale = baseFitScale * laptopFactor;

                } else {
                    // === DESKTOP INITIALIZATION (Immersive) ===
                    double desktopFactor = 0.95; // Reduced to 0.95 (User: "Un zoom menos de actual")

                    if (contentRatio > 1.5)
                        desktopFactor *= 0.95;

                    finalBaseScale = baseFitScale * desktopFactor;
                }
            }

            // --- 2. Apply Zoom Limits ---
            if (zoomFactor > maxZoomLimit)
                zoomFactor = maxZoomLimit;
            if (zoomFactor < MIN_ZOOM)
                zoomFactor = MIN_ZOOM;

            double finalScale = finalBaseScale * zoomFactor;

            // --- 3. Dynamic Pan Limits (UNIFIED GENEROUS PANNING) ---
            double actualContentWidth = contentWidth * finalScale;
            double actualContentHeight = contentHeight * finalScale;

            double panFreedomFactor = 0.6;
            double maxPanX = Math.max(availableWidth * 0.3, actualContentWidth * panFreedomFactor);
            double maxPanY = Math.max(availableHeight * 0.3, actualContentHeight * panFreedomFactor);

            if (translateX > maxPanX)
                translateX = maxPanX;
            if (translateX < -maxPanX)
                translateX = -maxPanX;
            if (translateY > maxPanY)
                translateY = maxPanY;
            if (translateY < -maxPanY)
                translateY = -maxPanY;

            // --- 4. Centering Logic (Robust Center-to-Center) ---
            // We align the Geometric Center of the VISIBLE Content to the Center of the
            // Viewport.

            double contentCenterX = contentMinX + (contentWidth / 2.0);
            double contentCenterY = contentMinY + (contentHeight / 2.0);

            double viewportCenterX = availableWidth / 2.0;
            double viewportCenterY = availableHeight / 2.0;

            // JavaFX pivots scaling around the Node's geometric center by default.
            double pivotX = contentGroup.getBoundsInLocal().getMinX()
                    + (contentGroup.getBoundsInLocal().getWidth() / 2.0);
            double pivotY = contentGroup.getBoundsInLocal().getMinY()
                    + (contentGroup.getBoundsInLocal().getHeight() / 2.0);

            // Offset needed to bring Content Center to Viewport Center, accounting for
            // scale pivot shift
            double layoutOffsetX = viewportCenterX - (pivotX + (contentCenterX - pivotX) * finalScale);
            double layoutOffsetY = viewportCenterY - (pivotY + (contentCenterY - pivotY) * finalScale);

            // --- 5. Ajuste por zoom hacia el cursor ---
            if (panningEnabled && zoomAnchorSceneX >= 0 && zoomBeforeScale > 0) {
                // Punto del contenido que estaba bajo el ratón ANTES del zoom
                double oldEffectiveX = zoomAnchorSceneX;
                double oldEffectiveY = zoomAnchorSceneY;
                // Convertir a coordenadas de contenido antes del zoom
                double contentX = (oldEffectiveX - layoutOffsetX_old - translateX_old) / zoomBeforeScale;
                double contentY = (oldEffectiveY - layoutOffsetY_old - translateY_old) / zoomBeforeScale;
                // Dónde debería estar ese punto después del zoom
                double expectedScreenX = contentX * finalScale + layoutOffsetX + translateX;
                double expectedScreenY = contentY * finalScale + layoutOffsetY + translateY;
                // Ajustar translate para que coincida
                translateX += (oldEffectiveX - expectedScreenX);
                translateY += (oldEffectiveY - expectedScreenY);
                zoomAnchorSceneX = -1;
            }

            // --- 6. Apply Transforms ---
            contentGroup.setScaleX(finalScale);
            contentGroup.setScaleY(finalScale);

            // Static Centering
            contentGroup.setLayoutX(layoutOffsetX);
            contentGroup.setLayoutY(layoutOffsetY);

            // Dynamic User Panning
            contentGroup.setTranslateX(translateX);
            contentGroup.setTranslateY(translateY);

            // JavaFX pivots scaling around the Node's geometric center by default.
            // Effective offset incorporates layout, pivot shift due to scale, and
            // translation.
            // The effective offset formula: Pivot - (Pivot * Scale) + Layout + Translation
            this.effectiveOffsetX = layoutOffsetX + translateX + pivotX - (pivotX * finalScale);
            this.effectiveOffsetY = layoutOffsetY + translateY + pivotY - (pivotY * finalScale);

            // Store for Rulers
            this.finalScale = finalScale;
            this.layoutOffsetX = layoutOffsetX;
            this.layoutOffsetY = layoutOffsetY;

            notifyViewportChanged();
        } catch (Exception e) {
            // Ignore internal JavaFX bounds errors during initialization
        }
    }

}
