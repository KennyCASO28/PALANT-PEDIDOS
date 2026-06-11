package org.example.component.helper;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import org.example.component.PrendaVisualizer;
import org.example.utils.UIFactory;

public class VisualizerUiController {

    private final PrendaVisualizer visualizer;

    private ToggleButton btnLockBg;
    private ToggleButton btnToggleRefPoints;
    private Button btnFinishEditOverlay;

    public VisualizerUiController(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
        initButtons();
    }

    private void initButtons() {
        initLockBackgroundButton();
        initReferencePointsButton();
        initFinishEditButton();
    }

    public void attachTo(StackPane container) {
        if (btnLockBg != null && !container.getChildren().contains(btnLockBg)) {
            container.getChildren().add(btnLockBg);
            StackPane.setAlignment(btnLockBg, Pos.TOP_LEFT);
            StackPane.setMargin(btnLockBg, new Insets(20));
        }
        if (btnToggleRefPoints != null && !container.getChildren().contains(btnToggleRefPoints)) {
            container.getChildren().add(btnToggleRefPoints);
            StackPane.setAlignment(btnToggleRefPoints, Pos.BOTTOM_LEFT);
            StackPane.setMargin(btnToggleRefPoints, new Insets(20));
        }
        if (btnFinishEditOverlay != null && !container.getChildren().contains(btnFinishEditOverlay)) {
            container.getChildren().add(btnFinishEditOverlay);
            StackPane.setAlignment(btnFinishEditOverlay, Pos.TOP_CENTER);
            StackPane.setMargin(btnFinishEditOverlay, new Insets(60, 0, 0, 0));
        }
    }

    private void initLockBackgroundButton() {
        this.btnLockBg = new ToggleButton(""); // No text
        try {
            btnLockBg.setGraphic(UIFactory.crearIcono("mdi2l-lock-open-variant", 18, "white"));
        } catch(Exception e) {}
        btnLockBg.setTooltip(new Tooltip("Bloquear Fondo"));

        String BTN_STYLE_OVAL = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 50%; -fx-padding: 8; -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px; "
                + "-fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 3, 0, 0, 1); -fx-border-color: transparent; -fx-alignment: center;";

        String BTN_STYLE_LOCKED = "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 50%; -fx-padding: 8; -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px; "
                + "-fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 3, 0, 0, 1); -fx-border-color: transparent; -fx-alignment: center;";

        btnLockBg.setStyle(BTN_STYLE_OVAL);

        btnLockBg.selectedProperty().addListener((obs, old, val) -> {
            visualizer.setViewportPanningEnabled(!val);
            if (val) {
                btnLockBg.setTooltip(new Tooltip("Fondo Bloqueado\n(Click para desbloquear)"));
                try{ btnLockBg.setGraphic(UIFactory.crearIcono("mdi2l-lock", 18, "white")); }catch(Exception e){}
                btnLockBg.setStyle(BTN_STYLE_LOCKED);
            } else {
                btnLockBg.setTooltip(new Tooltip("Fondo Desbloqueado\n(Click para bloquear)"));
                try{ btnLockBg.setGraphic(UIFactory.crearIcono("mdi2l-lock-open-variant", 18, "white")); }catch(Exception e){}
                btnLockBg.setStyle(BTN_STYLE_OVAL);
            }
        });

        btnLockBg.setSelected(true);
        btnLockBg.setOnAction(e -> {
            visualizer.setViewportPanningEnabled(!btnLockBg.isSelected());
        });

        btnLockBg.setOnAction(e -> {
            visualizer.setViewportPanningEnabled(!btnLockBg.isSelected());
        });
    }

    private void initReferencePointsButton() {
        this.btnToggleRefPoints = new ToggleButton("");
        try{ btnToggleRefPoints.setGraphic(UIFactory.crearIcono("mdi2f-flag-variant", 20, "white")); }catch(Exception e){}
        btnToggleRefPoints.setTooltip(new Tooltip("Puntos de Referencia (Centros de Prendas)"));

        String REFP_BASE = "-fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40; "
                + "-fx-padding: 0; -fx-cursor: hand; -fx-background-insets: 0; -fx-border-width: 0; -fx-alignment: center;";
        String REFP_OFF = REFP_BASE + "-fx-background-color: #27ae60;";
        String REFP_ON = REFP_BASE
                + "-fx-background-color: #2ecc71; -fx-effect: dropshadow(three-pass-box, rgba(46,204,113,0.5), 10, 0, 0, 0);";

        btnToggleRefPoints.setStyle(REFP_OFF);

        btnToggleRefPoints.selectedProperty().addListener((obs, old, val) -> {
            btnToggleRefPoints.setStyle(val ? REFP_ON : REFP_OFF);
            if (visualizer.getReferenceLayer() != null) {
                visualizer.getReferenceLayer().setVisible(val);
            }
            visualizer.updateReferencePoints();
        });
    }

    private void initFinishEditButton() {
        btnFinishEditOverlay = new Button("LISTO");
        btnFinishEditOverlay.setAlignment(Pos.CENTER);
        try {
            btnFinishEditOverlay.setGraphic(UIFactory.crearIcono("mdi2c-check-bold", 18, "white"));
            btnFinishEditOverlay.setGraphicTextGap(10);
            btnFinishEditOverlay.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        } catch (Exception e) {}

        String STYLE_NORMAL = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 12; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 4);";
        btnFinishEditOverlay.setStyle(STYLE_NORMAL);
        String STYLE_HOVER = "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 12; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.5); -fx-border-radius: 12; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 6);";
        String STYLE_PRESSED = "-fx-background-color: #1e8449; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 12; -fx-cursor: hand; -fx-border-color: transparent;";

        final boolean[] isBtnPressed = { false };

        ScaleTransition pressAnim = new ScaleTransition(Duration.millis(50), btnFinishEditOverlay);
        pressAnim.setToX(0.90);
        pressAnim.setToY(0.90);

        ScaleTransition releaseAnim = new ScaleTransition(Duration.millis(100), btnFinishEditOverlay);
        releaseAnim.setToX(1.05);
        releaseAnim.setToY(1.05);

        ScaleTransition hoverEnter = new ScaleTransition(Duration.millis(100), btnFinishEditOverlay);
        hoverEnter.setToX(1.05);
        hoverEnter.setToY(1.05);

        ScaleTransition hoverExit = new ScaleTransition(Duration.millis(100), btnFinishEditOverlay);
        hoverExit.setToX(1.0);
        hoverExit.setToY(1.0);

        btnFinishEditOverlay.setOnMouseEntered(e -> {
            if (!isBtnPressed[0]) {
                btnFinishEditOverlay.setStyle(STYLE_HOVER);
                hoverEnter.playFromStart();
            }
        });

        btnFinishEditOverlay.setOnMouseExited(e -> {
            if (!isBtnPressed[0]) {
                btnFinishEditOverlay.setStyle(STYLE_NORMAL);
                hoverExit.playFromStart();
            }
        });

        btnFinishEditOverlay.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            isBtnPressed[0] = true;
            btnFinishEditOverlay.setStyle(STYLE_PRESSED);
            pressAnim.playFromStart();
            e.consume();
        });

        btnFinishEditOverlay.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            isBtnPressed[0] = false;
            btnFinishEditOverlay.setStyle(STYLE_HOVER);
            releaseAnim.playFromStart();
        });

        btnFinishEditOverlay.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            e.consume();
            visualizer.finishEditMode();
        });

        btnFinishEditOverlay.setVisible(false);
        btnFinishEditOverlay.setId("btnFinishEditOverlay");
        // NOTE: managed stays TRUE so StackPane always applies TOP_CENTER alignment.
        // Visibility is controlled exclusively via setVisible(boolean).
        btnFinishEditOverlay.setMouseTransparent(false);
        btnFinishEditOverlay.setPickOnBounds(true);
        btnFinishEditOverlay.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        btnFinishEditOverlay.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);
    }

    public void setEditOverlayVisible(boolean visible) {
        if (btnFinishEditOverlay != null) {
            btnFinishEditOverlay.setVisible(visible);
            if (visible) {
                btnFinishEditOverlay.toFront();
            }
        }
    }

    public void setAllOverlaysVisible(boolean visible) {
        if (btnLockBg != null) {
            btnLockBg.setVisible(visible);
            btnLockBg.setManaged(visible);
        }
        if (btnToggleRefPoints != null) {
            btnToggleRefPoints.setVisible(visible);
            btnToggleRefPoints.setManaged(visible);
        }
        setEditOverlayVisible(visible && isEditOverlayVisible()); // Only restore if it was actually active
    }

    public boolean isLockBgVisible() {
        return btnLockBg != null && btnLockBg.isVisible();
    }

    public boolean isRefPointsVisible() {
        return btnToggleRefPoints != null && btnToggleRefPoints.isVisible();
    }

    public boolean isEditOverlayVisible() {
        return btnFinishEditOverlay != null && btnFinishEditOverlay.isVisible();
    }

    public Button getFinishEditButton() {
        return btnFinishEditOverlay;
    }

    public ToggleButton getLockBgButton() {
        return btnLockBg;
    }

    public ToggleButton getRefPointsButton() {
        return btnToggleRefPoints;
    }

    public void setEditModeVisuals(boolean active) {
        if (active) {
            // Get the current editing zone to determine which objects to dim
            String editingZone = visualizer.getPowerClipManager().getCurrentEditingZone();

            // Apply dimming to garment base layers (darker: 20% instead of 40%)
            visualizer.getRenderOrchestrator().setRenderersOpacity(0.2);

            // Apply dimming ONLY to user objects (layers) OUTSIDE the editing zone
            for (javafx.scene.Node n : visualizer.getLayerManager().getLayerGroup().getChildren()) {
                if (n instanceof org.example.component.helper.SmartZoneContainer) {
                    org.example.component.helper.SmartZoneContainer container = (org.example.component.helper.SmartZoneContainer) n;
                    String containerZone = container.getZoneId();

                    if (editingZone != null && editingZone.equals(containerZone)) {
                        for (javafx.scene.Node child : container.getContentGroup().getChildren()) {
                            child.setOpacity(1.0);
                        }
                    } else {
                        for (javafx.scene.Node child : container.getContentGroup().getChildren()) {
                            child.setOpacity(0.15);
                        }
                    }
                    continue;
                }

                // Logic for individual layers outside containers
                String nodeZone = null;
                if (n instanceof org.example.component.ImageLayer) {
                    nodeZone = ((org.example.component.ImageLayer) n).getActiveZone();
                } else if (n instanceof org.example.component.ShapeLayer) {
                    nodeZone = ((org.example.component.ShapeLayer) n).getActiveZone();
                }

                if (editingZone != null && !editingZone.equals(nodeZone)) {
                    n.setOpacity(0.15);
                } else {
                    n.setOpacity(1.0);
                }
            }

            visualizer.requestLayout();
            setEditOverlayVisible(true);

        } else {
            // Restore normal appearance
            visualizer.getRenderOrchestrator().setRenderersOpacity(1.0);
            for (javafx.scene.Node n : visualizer.getLayerManager().getLayerGroup().getChildren()) {
                n.setOpacity(1.0);
                if (n instanceof org.example.component.helper.SmartZoneContainer) {
                    for (javafx.scene.Node child : ((org.example.component.helper.SmartZoneContainer) n).getContentGroup().getChildren()) {
                        child.setOpacity(1.0);
                    }
                }
            }
            setEditOverlayVisible(false);
        }
    }
}
