package org.example.component.helper;

import java.util.ArrayList;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import org.example.component.NumberComposition;
import org.example.component.PrendaVisualizer;
import org.example.model.PrendaState;

public class VisualizerSnapshotService {

    private final PrendaVisualizer visualizer;
    private final PrendaOverlayManager overlayManager;
    private final PrendaEditModeManager editManager;
    private final PowerClipManager powerClipManager;

    private WritableImage cachedSnapshot;

    public VisualizerSnapshotService(PrendaVisualizer visualizer, PrendaOverlayManager overlayManager, 
                                     PrendaEditModeManager editManager, PowerClipManager powerClipManager) {
        this.visualizer = visualizer;
        this.overlayManager = overlayManager;
        this.editManager = editManager;
        this.powerClipManager = powerClipManager;
    }

    public void startSnapshotSession() {
        // Hide overlays for the master snapshot
        boolean wasOverlayVisible = overlayManager.getOverlayGroup().isVisible();

        // If we are in edit mode, the garment is dimmed (alpha 0.5 + ColorAdjust).
        // The eyedropper should pick REAL colors.
        boolean wasEditing = (editManager.getCurrentEditingLayer() != null)
                || (powerClipManager != null && powerClipManager.isEditing());
        if (wasEditing) {
            visualizer.setEditModeVisuals(false);
            visualizer.applyCss();
            visualizer.layout();
        }

        overlayManager.getOverlayGroup().setVisible(false);

        // Hide Contours, Handles, and Effects
        boolean contourVisible = visualizer.getContourGroup().isVisible();
        boolean handlesVisible = visualizer.getHandlesGroup().isVisible();
        javafx.scene.effect.Effect oldEffect = visualizer.getEffect();

        visualizer.getContourGroup().setVisible(false);
        visualizer.getHandlesGroup().setVisible(false);
        visualizer.setEffect(null);

        // Take full scale snapshot (Captures node in its current visual state)
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        this.cachedSnapshot = visualizer.snapshot(params, null);

        // Restore UI State
        visualizer.getContourGroup().setVisible(contourVisible);
        visualizer.getHandlesGroup().setVisible(handlesVisible);
        visualizer.setEffect(oldEffect);

        overlayManager.getOverlayGroup().setVisible(wasOverlayVisible);
        if (wasEditing) {
            visualizer.setEditModeVisuals(true);
        }
    }

    public void endSnapshotSession() {
        this.cachedSnapshot = null;
    }

    public WritableImage getCachedSnapshot() {
        return cachedSnapshot;
    }

    

    /**
     * ATOMIC SNAPSHOT: Ensures perfectly centered, uncropped rendering by
     * isolating state and resetting the viewport temporarily.
     */
    public WritableImage takeSafeSnapshot(boolean forArquero) {
        boolean wasArquero = visualizer.isEditandoArquero();
        boolean previousNotificationsSuspended = visualizer.isNotificationsSuspended();
        boolean previousPendingNotification = visualizer.isPendingNotification();
        double oldZoom = 1.0;
        double oldTX = 0;
        double oldTY = 0;
        boolean hasViewport = visualizer.getViewportController() != null;
        if (hasViewport) {
            oldZoom = visualizer.getViewportController().getZoomFactor();
            oldTX = visualizer.getViewportController().getTranslateX();
            oldTY = visualizer.getViewportController().getTranslateY();
        }

        visualizer.flushUIStateToDataModel();

        PrendaState savedCamisetaState = copyState(visualizer.getCamisetaState());
        PrendaState savedArqueroState = copyState(visualizer.getArqueroState());

        visualizer.beginAtomicSnapshotMode();
        try {
            return captureAtomicSnapshot(
                    forArquero,
                    wasArquero,
                    previousNotificationsSuspended,
                    previousPendingNotification,
                    savedCamisetaState,
                    savedArqueroState);
        } finally {
            if (hasViewport) {
                visualizer.getViewportController().setViewportState(oldZoom, oldTX, oldTY);
            }
        }
    }

    /**
     * Tries to pick the actual DATA color of a garment part or user layer at the given scene coordinates.
     * This is much more accurate than pickColor() because it ignores shadows, highlights, and overlays.
     */
    public Color pickDataColor(double sceneX, double sceneY) {
        return PrendaColorPickingHelper.pickDataColor(
                new ArrayList<>(visualizer.getUserLayerManager().getLayers()),
                sceneX,
                sceneY,
                visualizer.getShapeHelper(),
                visualizer.getColorManager(),
                () -> pickColor(sceneX, sceneY));
    }

    public Color pickColor(double sceneX, double sceneY) {
        // Use cache if available
        if (cachedSnapshot != null) {
            Point2D local = visualizer.sceneToLocal(sceneX, sceneY);
            return PrendaSnapshotHelper.pickColorFromCache(cachedSnapshot, visualizer.getWidth(), visualizer.getHeight(), local);
        }

        // Fallback to old slow method
        boolean wasOverlayVisible = overlayManager.getOverlayGroup().isVisible();
        overlayManager.getOverlayGroup().setVisible(false);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        Point2D localPoint = visualizer.sceneToLocal(sceneX, sceneY);
        javafx.geometry.Rectangle2D viewport = new javafx.geometry.Rectangle2D(localPoint.getX(), localPoint.getY(), 1, 1);
        params.setViewport(viewport);
        WritableImage snap = new WritableImage(1, 1);
        visualizer.snapshot(params, snap);

        overlayManager.getOverlayGroup().setVisible(wasOverlayVisible);
        return snap.getPixelReader().getColor(0, 0);
    }

    /**
     * Captures a zoomed region for the Eyedropper magnifier.
     */
    public Image getRegionSnapshot(double sceneX, double sceneY, int width, int height) {
        if (cachedSnapshot != null) {
            Point2D local = visualizer.sceneToLocal(sceneX, sceneY);
            return PrendaSnapshotHelper.getRegionFromCache(
                    cachedSnapshot,
                    visualizer.getWidth(),
                    visualizer.getHeight(),
                    local,
                    width,
                    height);
        }

        // Anti-pattern fallback
        boolean wasOverlayVisible = overlayManager.getOverlayGroup().isVisible();
        overlayManager.getOverlayGroup().setVisible(false);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        Point2D localPoint = visualizer.sceneToLocal(sceneX, sceneY);
        double halfW = width / 2.0;
        double halfH = height / 2.0;

        javafx.geometry.Rectangle2D viewport = new javafx.geometry.Rectangle2D(
                localPoint.getX() - halfW,
                localPoint.getY() - halfH,
                width, height);
        params.setViewport(viewport);

        WritableImage snap = new WritableImage(width, height);
        visualizer.snapshot(params, snap);

        overlayManager.getOverlayGroup().setVisible(wasOverlayVisible);
        return snap;
    }

    private WritableImage captureAtomicSnapshot(
            boolean forArquero,
            boolean wasArquero,
            boolean previousNotificationsSuspended,
            boolean previousPendingNotification,
            PrendaState savedCamisetaState,
            PrendaState savedArqueroState) {
        Pane sandbox = new Pane();
        sandbox.setPrefWidth(2400);
        sandbox.setPrefHeight(1800);

        try {
            sandbox.getStylesheets().add(visualizer.getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            // Non-critical: styles might not load in all environments.
        }

        Parent originalParent = visualizer.getContentGroup().getParent();
        int originalIndex = detachContentGroup(originalParent);
        sandbox.getChildren().add(visualizer.getContentGroup());

        try {
            prepareSnapshotTarget(forArquero);

            PrendaState targetState = forArquero ? visualizer.getArqueroState() : visualizer.getCamisetaState();
            applySnapshotVisibility(targetState, forArquero);
            resetSnapshotLayout(sandbox);

            boolean isMangaLarga = "LARGA".equals(targetState.getLargo());
            return PrendaSnapshotHelper.captureCenteredSnapshot(
                    sandbox,
                    visualizer.getContentGroup(),
                    isMangaLarga);
        } catch (Exception e) {
            e.printStackTrace();
            return new WritableImage(1, 1);
        } finally {
            sandbox.getChildren().remove(visualizer.getContentGroup());
            reattachContentGroup(originalParent, originalIndex);
            restoreLiveEditorState(
                    wasArquero,
                    previousNotificationsSuspended,
                    previousPendingNotification,
                    savedCamisetaState,
                    savedArqueroState);
        }
    }

    private void prepareSnapshotTarget(boolean forArquero) {
        visualizer.setActiveDesign(forArquero);
        visualizer.invalidateCargarCapasSignature();
        visualizer.cargarCapas();
    }

    private void applySnapshotVisibility(PrendaState targetState, boolean forArquero) {
        visualizer.getShirtRenderer().setVisible(!forArquero && targetState.hasShirt());
        visualizer.getShortsRenderer().setVisible(!forArquero && targetState.hasShorts());
        visualizer.getSocksRenderer().setVisible(!forArquero && targetState.hasSocks());

        if (visualizer.getArqueroShirtRenderer() != null) {
            visualizer.getArqueroShirtRenderer().setVisible(forArquero && targetState.hasShirt());
        }
        if (visualizer.getArqueroShortsRenderer() != null) {
            visualizer.getArqueroShortsRenderer().setVisible(forArquero && targetState.hasShorts());
        }
        if (visualizer.getArqueroSocksRenderer() != null) {
            visualizer.getArqueroSocksRenderer().setVisible(forArquero && targetState.hasSocks());
        }

        setNumberVisibility(
                visualizer.getNumberManager().getChestNumber(false),
                !forArquero
                        && targetState.hasShirt()
                        && targetState.isChestNumberVisible()
                        && visualizer.getNumberManager().hasNumberDigit(targetState.getCurrentChestNumber()));
        setNumberVisibility(
                visualizer.getNumberManager().getBackNumber(false),
                !forArquero
                        && targetState.hasShirt()
                        && targetState.isBackNumberVisible()
                        && visualizer.getNumberManager().hasNumberDigit(targetState.getCurrentBackNumber()));
        setNumberVisibility(
                visualizer.getNumberManager().getShortNumber(false),
                !forArquero
                        && targetState.hasShorts()
                        && targetState.isShortNumberVisible()
                        && visualizer.getNumberManager().hasNumberDigit(targetState.getCurrentShortNumber()));
        setNumberVisibility(
                visualizer.getNumberManager().getChestNumber(true),
                forArquero
                        && targetState.hasShirt()
                        && targetState.isChestNumberVisible()
                        && visualizer.getNumberManager().hasNumberDigit(targetState.getCurrentChestNumber()));
        setNumberVisibility(
                visualizer.getNumberManager().getBackNumber(true),
                forArquero
                        && targetState.hasShirt()
                        && targetState.isBackNumberVisible()
                        && visualizer.getNumberManager().hasNumberDigit(targetState.getCurrentBackNumber()));
        setNumberVisibility(
                visualizer.getNumberManager().getShortNumber(true),
                forArquero
                        && targetState.hasShorts()
                        && targetState.isShortNumberVisible()
                        && visualizer.getNumberManager().hasNumberDigit(targetState.getCurrentShortNumber()));

        if (visualizer.getGuideLayer() != null) {
            visualizer.getGuideLayer().setVisible(false);
        }
        if (visualizer.getReferenceLayer() != null) {
            visualizer.getReferenceLayer().setVisible(false);
        }
        if (visualizer.getHotspotLayer() != null) {
            visualizer.getHotspotLayer().setVisible(!forArquero);
        }

        visualizer.setToolsVisible(false);
    }

    private void resetSnapshotLayout(Pane sandbox) {
        visualizer.getContentGroup().setLayoutX(0);
        visualizer.getContentGroup().setLayoutY(0);
        visualizer.getContentGroup().setTranslateX(0);
        visualizer.getContentGroup().setTranslateY(0);
        visualizer.getContentGroup().setScaleX(1.0);
        visualizer.getContentGroup().setScaleY(1.0);
        sandbox.layout();
    }

    private void restoreLiveEditorState(
            boolean wasArquero,
            boolean previousNotificationsSuspended,
            boolean previousPendingNotification,
            PrendaState savedCamisetaState,
            PrendaState savedArqueroState) {
        visualizer.setActiveDesign(wasArquero);
        visualizer.applyVisibility();

        if (visualizer.getGuideLayer() != null) {
            visualizer.getGuideLayer().setVisible(true);
            visualizer.getGuideLayer().setManaged(true);
        }

        boolean refsActive = visualizer.getBtnToggleRefPoints() == null
                || visualizer.getBtnToggleRefPoints().isSelected();
        if (visualizer.getReferenceLayer() != null) {
            visualizer.getReferenceLayer().setVisible(refsActive);
            visualizer.getReferenceLayer().setManaged(true);
        }

        if (visualizer.getHotspotLayer() != null) {
            visualizer.getHotspotLayer().setVisible(true);
            visualizer.getHotspotLayer().setManaged(true);
        }

        visualizer.setToolsVisible(true);

        restoreStateCopy(savedCamisetaState, visualizer.getCamisetaState());
        restoreStateCopy(savedArqueroState, visualizer.getArqueroState());
        visualizer.setStateDirectly(wasArquero ? visualizer.getArqueroState() : visualizer.getCamisetaState());
        visualizer.cargarEstadoDesdeCero(visualizer.getState());

        visualizer.restoreAtomicSnapshotMode(previousNotificationsSuspended, previousPendingNotification);

        javafx.application.Platform.runLater(() -> {
            if (visualizer.getViewportController() != null) {
                visualizer.getViewportController().autoScale();
            }
        });
    }

    private int detachContentGroup(Parent originalParent) {
        int originalIndex = -1;
        if (originalParent instanceof javafx.scene.Group parentGroup) {
            originalIndex = parentGroup.getChildren().indexOf(visualizer.getContentGroup());
            parentGroup.getChildren().remove(visualizer.getContentGroup());
        } else if (originalParent instanceof Pane parentPane) {
            originalIndex = parentPane.getChildren().indexOf(visualizer.getContentGroup());
            parentPane.getChildren().remove(visualizer.getContentGroup());
        }
        return originalIndex;
    }

    private void reattachContentGroup(Parent originalParent, int originalIndex) {
        if (originalParent instanceof javafx.scene.Group parentGroup) {
            int safeIndex = originalIndex < 0
                    ? parentGroup.getChildren().size()
                    : Math.min(originalIndex, parentGroup.getChildren().size());
            parentGroup.getChildren().add(
                    safeIndex,
                    visualizer.getContentGroup());
        } else if (originalParent instanceof Pane parentPane) {
            int safeIndex = originalIndex < 0
                    ? parentPane.getChildren().size()
                    : Math.min(originalIndex, parentPane.getChildren().size());
            parentPane.getChildren().add(
                    safeIndex,
                    visualizer.getContentGroup());
        }
    }

    private void setNumberVisibility(NumberComposition number, boolean visible) {
        if (number != null) {
            number.setVisible(visible);
        }
    }

    private PrendaState copyState(PrendaState source) {
        if (source == null) {
            return null;
        }
        PrendaState snapshot = new PrendaState();
        snapshot.copyFrom(source);
        return snapshot;
    }

    private void restoreStateCopy(PrendaState savedState, PrendaState liveState) {
        if (savedState != null && liveState != null) {
            liveState.copyFrom(savedState);
        }
    }
}
