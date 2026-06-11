package org.example.component.helper;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.model.ShapeType;

/**
 * Orchestrator for all shape-related interactions.
 * Delegates specific tasks to specialized services (Creation, Bezier Editing, Zone Detection).
 */
public class ShapeInteractionHelper {

    private final PrendaVisualizer visualizer;
    
    // Specialized Services
    private final ShapeCreationService creationService;
    private final BezierInteractionService bezierService;
    private final GarmentZoneService zoneService;
    private final VisualizerEyedropperService eyedropperService;

    public ShapeInteractionHelper(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
        this.creationService = new ShapeCreationService(visualizer);
        this.bezierService = new BezierInteractionService(visualizer);
        this.zoneService = new GarmentZoneService(visualizer);
        this.eyedropperService = new VisualizerEyedropperService(visualizer);

        // Fix Ghost Nodes: Cleanup if the edited layer is removed from the scene
        this.visualizer.addRemovalListener(node -> {
            if (node == bezierService.getEditingLayer()) {
                 bezierService.exitNodeEditMode();
            }
        });
    }

    // --- Delegation: Creation ---

    public void startShapeCreation(ShapeType type, Color fill, Color stroke, double strokeWidth,
            java.util.function.Consumer<ShapeLayer> onFinish) {
        bezierService.exitNodeEditMode();
        creationService.startShapeCreation(type, fill, stroke, strokeWidth, onFinish);
    }

    public void insertDefaultShape(ShapeType type, Color fill, Color stroke, double strokeWidth,
            java.util.function.Consumer<ShapeLayer> onFinish) {
        bezierService.exitNodeEditMode();
        creationService.insertDefaultShape(type, fill, stroke, strokeWidth, onFinish);
    }

    public void cancelShapeCreation() {
        creationService.cancelShapeCreation();
    }

    public boolean isCreatingShape() {
        return creationService.isCreatingShape();
    }

    // --- Delegation: Bezier Editing ---

    public boolean isNodeEditing() {
        return bezierService.isNodeEditing();
    }

    public javafx.beans.property.BooleanProperty nodeEditingProperty() {
        return bezierService.nodeEditingProperty();
    }

    public void enterNodeEditMode(ShapeLayer layer) {
        creationService.cancelShapeCreation();
        bezierService.enterNodeEditMode(layer);
    }

    public void exitNodeEditMode() {
        bezierService.exitNodeEditMode();
    }

    public boolean isNodeEditHandle(Node target) {
        if (bezierService.getHandleGroup() == null) return false;
        Node temp = target;
        while (temp != null) {
            if (temp == bezierService.getHandleGroup()) return true;
            temp = temp.getParent();
        }
        return false;
    }

    // --- Delegation: Zones ---

    public String detectZone(double sceneX, double sceneY) {
        return zoneService.detectZone(sceneX, sceneY);
    }

    public javafx.scene.Node getZoneNode(String zone) {
        return zoneService.getZoneNode(zone);
    }

    public String getZoneSvgContent(String zone) {
        return zoneService.getZoneSvgContent(zone);
    }

    public void applySmartPowerClip(javafx.scene.Node layer, String zone, boolean isInitialPlacement) {
        visualizer.applySmartPowerClip(layer, zone, isInitialPlacement);
    }

    // --- Common Controls ---

    public void reset() {
        creationService.cancelShapeCreation();
        bezierService.exitNodeEditMode();
    }

    // --- Specialized Modes (Eyedropper) ---

    public void startEyedropperSession(java.util.function.Consumer<javafx.scene.paint.Color> onPicked) {
        eyedropperService.startEyedropperSession(onPicked);
    }

    public void startEyedropperSession(java.util.function.Consumer<javafx.scene.paint.Color> onPicked, java.util.function.Consumer<javafx.scene.paint.Color> onPreview) {
        eyedropperService.startEyedropperSession(onPicked, onPreview);
    }

    public void startEyedropperSession(boolean useDataPick, java.util.function.Consumer<javafx.scene.paint.Color> onPicked, java.util.function.Consumer<javafx.scene.paint.Color> onPreview) {
        eyedropperService.startEyedropperSession(useDataPick, onPicked, onPreview);
    }

    public boolean isEyedropperActive() {
        return eyedropperService.isEyedropperActive();
    }

    // --- PowerClip Target Picker ---

    private ShapeLayer pickingTargetFor;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> pickerClickHandler;

    public void startPowerClipTargetPicker(ShapeLayer source) {
        if (source == null) return;
        this.pickingTargetFor = source;
        visualizer.getContentGroup().setCursor(javafx.scene.Cursor.CROSSHAIR);

        pickerClickHandler = e -> {
            javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
            String zone = detectZone(e.getSceneX(), e.getSceneY());
            if (zone != null) {
                applySmartPowerClip(pickingTargetFor, zone, true);
                cancelPowerClipTargetPicker();
                e.consume();
                return;
            }

            javafx.scene.Node temp = target;
            ShapeLayer targetShape = null;
            while (temp != null && temp != visualizer) {
                if (temp instanceof ShapeLayer && temp != pickingTargetFor) {
                    targetShape = (ShapeLayer) temp;
                    break;
                }
                temp = temp.getParent();
            }

            if (targetShape != null) {
                visualizer.applyInternalPowerClip(pickingTargetFor, targetShape);
                cancelPowerClipTargetPicker();
                e.consume();
                return;
            }
            cancelPowerClipTargetPicker();
        };
        visualizer.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, pickerClickHandler);
    }

    private void cancelPowerClipTargetPicker() {
        if (pickerClickHandler != null) {
            visualizer.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, pickerClickHandler);
            pickerClickHandler = null;
        }
        visualizer.getContentGroup().setCursor(javafx.scene.Cursor.DEFAULT);
    }

    public boolean isPowerClipPickerActive() {
        return pickerClickHandler != null;
    }
}
