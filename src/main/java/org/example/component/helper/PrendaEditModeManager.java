package org.example.component.helper;

import javafx.scene.control.Button;
import org.example.component.ImageLayer;
import org.example.component.ShapeLayer;
import org.example.component.PrendaVisualizer;

/**
 * Manages the "Edit Mode" UI and state for the PrendaVisualizer.
 * Handles the "Finish Edit" button and layer locking logic.
 */
public class PrendaEditModeManager {

    private final PrendaVisualizer visualizer; // Callback reference
    private final PrendaOverlayManager overlayManager;

    private javafx.scene.Node currentEditingLayer = null;
    private String currentEditingZone = null;

    public PrendaEditModeManager(PrendaVisualizer visualizer, PrendaOverlayManager overlayManager) {
        this.visualizer = visualizer;
        this.overlayManager = overlayManager;
    }

    public Button getBtnFinishEdit() {
        if (visualizer.getVisualizerUiController() != null) {
            return visualizer.getVisualizerUiController().getFinishEditButton();
        }
        return null;
    }

    public void enterEditMode(ImageLayer layer) {
        if (currentEditingLayer != null && currentEditingLayer != layer) {
            finishEditMode();
        }
        currentEditingLayer = layer;
        this.currentEditingZone = layer.getActiveZone();
        layer.setLocked(false);
        layer.setIsBeingEdited(true); // Flag
        visualizer.getLayerManager().addLayerToZone(layer, null); // Move to global (unclip)
        layer.setSelected(true);
        visualizer.updateOverlayForZone(currentEditingZone);
        showUI();
    }

    public void enterEditMode(ShapeLayer layer) {
        if (currentEditingLayer != null && currentEditingLayer != layer) {
            finishEditMode();
        }
        currentEditingLayer = layer;
        this.currentEditingZone = layer.getActiveZone();
        layer.setLocked(false);
        layer.setIsBeingEdited(true); // Flag to block PowerClip menu
        visualizer.getLayerManager().addLayerToZone(layer, null); // Move to global (unclip)
        layer.setSelected(true);
        visualizer.updateOverlayForZone(currentEditingZone);
        showUI();
    }

    private void showUI() {
        visualizer.setEditModeVisuals(true);
        setVisible(true);
    }

    public void setZoneEditMode(boolean active) {
        visualizer.setEditModeVisuals(active);
        setVisible(active);
    }

    public void finishEditMode() {
        // 1. Single Layer Edit Mode (Legacy/Direct)
        if (currentEditingLayer != null && currentEditingZone != null) {
            // CRITICAL: Check if layer was deleted (removed from scene) during edit
            if (currentEditingLayer.getParent() == null) {
                // Layer was deleted by user. Do not resurrect it.
                currentEditingLayer = null;
                currentEditingZone = null;
                overlayManager.clearOverlay();
                visualizer.setEditModeVisuals(false);
                setVisible(false);
                return;
            }

            // Restore to Zone Bucket
            if (currentEditingLayer instanceof ImageLayer) {
                ImageLayer img = (ImageLayer) currentEditingLayer;
                visualizer.applySmartPowerClip(img, currentEditingZone, false);
                img.setLocked(true);
                img.setSelected(false);
                img.setIsBeingEdited(false); // Reset flag
                img.setVisible(true); // Safety force
            } else if (currentEditingLayer instanceof ShapeLayer) {
                ShapeLayer shape = (ShapeLayer) currentEditingLayer;
                visualizer.applySmartPowerClip(shape, currentEditingZone, false);
                shape.setLocked(true);
                shape.setSelected(false);
                shape.setIsBeingEdited(false); // Reset flag
                shape.setVisible(true); // Safety force
            }

            currentEditingLayer = null;
            currentEditingZone = null;

            overlayManager.clearOverlay();
            // Legacy cleanup just in case
            visualizer.setEditModeVisuals(false);
            setVisible(false);
            visualizer.notifyStateChanged();
            return;
        }

        // 2. Zone Edit Mode (PowerClipManager)
        if (visualizer.getPowerClipManager() != null && visualizer.getPowerClipManager().isEditing()) {
            visualizer.finishPowerClipEdit();
        }
    }

    public boolean isEditing() {
        // Also return true when PowerClipManager is in zone-edit mode.
        // This prevents the selection listener from clearing the zone overlay.
        return currentEditingLayer != null ||
               (visualizer.getPowerClipManager() != null && visualizer.getPowerClipManager().isEditing());
    }

    public javafx.scene.Node getCurrentEditingLayer() {
        return currentEditingLayer;
    }

    public String getCurrentEditingZone() {
        // First check if PowerClipManager is editing
        if (visualizer.getPowerClipManager() != null && visualizer.getPowerClipManager().isEditing()) {
            return visualizer.getPowerClipManager().getCurrentEditingZone();
        }
        // Otherwise return legacy zone
        return currentEditingZone;
    }

    public void setVisible(boolean visible) {
        // Overlay Button (Premium Animated) - the sole location of the Listo button
        if (visualizer.getVisualizerUiController() != null) {
            visualizer.getVisualizerUiController().setEditOverlayVisible(visible);
        }
        // NOTE: The sidebar backup button was removed. The overlay at TOP_CENTER
        // of the PrendaVisualizer is the only "Listo" button.
    }

    public void reset() {
        finishEditMode();
    }
}
