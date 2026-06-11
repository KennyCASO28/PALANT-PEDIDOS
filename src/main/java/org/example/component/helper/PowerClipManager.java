package org.example.component.helper;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import org.example.component.ImageLayer;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;
import org.example.component.UserLayerManager;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the "PowerClip" (Container) logic for the garment.
 * Responsibilities:
 * - Define Zones (buckets).
 * - Handle "Insert into Container" (Clip).
 * - Handle "Edit Content" (Unclip temporarily).
 * - Manage the "Finish Edit" UI interactions.
 */
public class PowerClipManager {

    private final PrendaVisualizer visualizer;
    private final UserLayerManager layerManager;
    private PrendaEditModeManager uiManager; // For the "Finish" button

    private final StringProperty currentEditingZone = new SimpleStringProperty(null);
    private final Map<String, SmartZoneContainer> containers = new HashMap<>();

    public PowerClipManager(PrendaVisualizer visualizer, UserLayerManager layerManager,
            PrendaEditModeManager uiManager) {
        this.visualizer = visualizer;
        this.layerManager = layerManager;
        this.uiManager = uiManager;
    }

    public void setUIManager(PrendaEditModeManager uiManager) {
        this.uiManager = uiManager;
    }

    public SmartZoneContainer getContainer(String zone) {
        return containers.computeIfAbsent(zone, z -> {
            SmartZoneContainer container = new SmartZoneContainer(z);
            // Add container to LayerManager's layer group if not already there?
            // Current LayerManager expects raw nodes.
            // We need to ensure this container is added to the scene.
            // Delegate this to layerManager?
            // Or add directly.
            // For now, let's assume LayerManager handles the root group.
            layerManager.getLayerGroup().getChildren().add(container);
            return container;
        });
    }

    public String getCurrentEditingZone() {
        return currentEditingZone.get();
    }

    /**
     * Inserts a layer into a specific zone container.
     * 
     * @param layer        The node to insert (Shape or Image).
     * @param zoneName     The target zone (PECHO, ESPALDA, etc).
     * @param centerInZone If true, centers the object. If false, preserves visual
     *                     position.
     */
    public void addToContainer(Node layer, String zoneName, boolean centerInZone) {
        if (layer == null || zoneName == null)
            return;

        SmartZoneContainer container = getContainer(zoneName);
        Point2D sceneCenter = null;
        if (!centerInZone && layer.getParent() != null) {
            Point2D localCenter = getLogicalCenter(layer);
            sceneCenter = layer.localToScene(localCenter);
        }

        // 2. Add to Container (Change Parent)
        layerManager.addLayerToContainer(layer, container.getContentGroup(), false);
        container.updateItemState(layer);

        // 3. Update Position: Use the node's bounds center to preserve visual position
        // even if rotated
        if (!centerInZone && sceneCenter != null) {
            Point2D containerLocalCenter = container.getContentGroup().sceneToLocal(sceneCenter);

            // We need to set TranslateX/Y such that the center matches containerLocalCenter
            // localToParent(0,0) gives the top-left in parent coords before translate if we
            // are not careful.
            // But TranslateX/Y ARE the parent coords of the local 0,0.
            // So: NewTranslate = ContainerLocalCenter - (LocalCenterOfNode)
            Point2D localCenter = getLogicalCenter(layer);

            layer.setTranslateX(containerLocalCenter.getX() - localCenter.getX());
            layer.setTranslateY(containerLocalCenter.getY() - localCenter.getY());
        } else {
            centerInZone(layer, zoneName);
        }

        // 4. Ensure Geometry
        // Since container is new, it might not have the path yet if not refreshed.
        refreshZoneClip(zoneName);

        // 5. Mark as internally clipped to prevent selection from outside
        setZoneRecursively(layer, zoneName);

        // 6. CRITICAL FIX: If we are currently EDITING this zone, the item must be
        // UNLOCKED
        // setZoneRecursively locked it (System Lock), so we must unlock it to allow
        // interaction.
        if (getContainer(zoneName).isEditing()) {
            if (layer instanceof ShapeLayer)
                ((ShapeLayer) layer).setSystemLocked(false);
            else if (layer instanceof ImageLayer)
                ((ImageLayer) layer).setSystemLocked(false);
            else if (layer instanceof org.example.component.TextLayer)
                ((org.example.component.TextLayer) layer).setSystemLocked(false);

            // Also ensure it's not mouse transparent (extra safety)
            layer.setMouseTransparent(false);
        }
    }

    public void restoreToContainer(Node layer, String zoneName) {
        if (layer == null || zoneName == null)
            return;

        SmartZoneContainer container = getContainer(zoneName);
        layerManager.addLayerToContainer(layer, container.getContentGroup(), false);
        container.updateItemState(layer);

        refreshZoneClip(zoneName);
        setZoneRecursively(layer, zoneName);

        if (getContainer(zoneName).isEditing()) {
            if (layer instanceof ShapeLayer)
                ((ShapeLayer) layer).setSystemLocked(false);
            else if (layer instanceof ImageLayer)
                ((ImageLayer) layer).setSystemLocked(false);
            else if (layer instanceof org.example.component.TextLayer)
                ((org.example.component.TextLayer) layer).setSystemLocked(false);

            layer.setMouseTransparent(false);
        }
    }

    private void setZoneRecursively(Node node, String zone) {
        if (node instanceof ShapeLayer) {
            ((ShapeLayer) node).setActiveZone(zone);
        } else if (node instanceof ImageLayer) {
            ((ImageLayer) node).setActiveZone(zone);
        } else if (node instanceof TextLayer) {
            ((TextLayer) node).setActiveZone(zone);
        } else if (node instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) node;
            gl.setActiveZone(zone);
            for (Node child : gl.getUserLayers()) {
                setZoneRecursively(child, zone);
            }
        } else if (node instanceof GroupLayerV2) {
            GroupLayerV2 gv2 = (GroupLayerV2) node;
            gv2.setActiveZone(zone);
            for (Node child : gv2.getUserLayers()) {
                setZoneRecursively(child, zone);
            }
        } else if (node instanceof Group) {
            for (Node child : ((Group) node).getChildren()) {
                setZoneRecursively(child, zone);
            }
        }
    }

    /**
     * Enters "Edit Content" mode for a specific Zone.
     * This unlocks the container (disables clip) and allows moving items.
     */
    public void enterEditMode(String zoneName) {
        if (zoneName == null)
            return;

        if (currentEditingZone != null && !currentEditingZone.equals(zoneName)) {
            finishEditMode();
        }

        this.currentEditingZone.set(zoneName);

        // CRITICAL: Clear selection to avoid "phantom" states where outside items look
        // selected
        // Use runLater to ensure it happens AFTER any pending mouse events (like
        // Click/Release)
        Platform.runLater(() -> layerManager.clearSelection());

        // 1. Activate Container Edit Mode
        SmartZoneContainer container = getContainer(zoneName);

        // USER REQUEST: Ensure the border is visible even if the container is empty.
        // We must refresh the geometry path because it might not have been set yet.
        refreshZoneClip(zoneName);

        container.setEditMode(true);

        // 2. Visual Feedback (Dim others) - NEW APPROACH
        visualizer.setEditModeVisuals(true);
        // Schedule AFTER clearSelection() to ensure it is not wiped by the selection listener
        final String zoneForOverlay = zoneName;
        Platform.runLater(() -> visualizer.updateOverlayForZone(zoneForOverlay));

        // 3. UI Manger update
        uiManager.setZoneEditMode(true);

        // 4. Ghosting: Make outside objects MouseTransparent to prevent cursor
        // changes/hover effects
        // Helper variable 'container' already defined above
        for (Node node : layerManager.getLayers()) {
            boolean isInside = false;
            // Check ancestry to support Groups/Nesting
            if (container != null) {
                Node temp = node;
                while (temp != null) {
                    if (temp == container.getContentGroup()) {
                        isInside = true;
                        break;
                    }
                    temp = temp.getParent();
                }
            }

            if (!isInside) {
                setGhostRecursively(node, true);
            }
        }
    }

    private void setGhostRecursively(Node node, boolean ghost) {
        if (node instanceof ShapeLayer || node instanceof ImageLayer
                || node instanceof org.example.component.TextLayer) {
            node.setMouseTransparent(ghost);
            // Visual dim: outside-container elements fade to 25% opacity while in edit mode
            node.setOpacity(ghost ? 0.25 : 1.0);
        } else if (node instanceof Group) {
            for (Node child : ((Group) node).getChildren()) {
                setGhostRecursively(child, ghost);
            }
        }
    }

    public void finishEditMode() {
        if (currentEditingZone.get() == null)
            return;

        // CRITICAL: Clear selection before locking, so we don't leave things visually
        // selected
        layerManager.clearSelection();

        SmartZoneContainer container = getContainer(currentEditingZone.get());
        container.setEditMode(false); // Re-locks and Clips

        // Restore UI - NEW APPROACH
        visualizer.setEditModeVisuals(false);
        visualizer.getOverlayManager().clearOverlay();

        currentEditingZone.set(null);
        uiManager.setZoneEditMode(false);

        // 4. Unghost: Restore interactivity and visual opacity
        for (Node node : layerManager.getLayers()) {
            setGhostRecursively(node, false);
        }
    }

    public boolean isZoneEmpty(String zoneName) {
        if (zoneName == null)
            return true;
        SmartZoneContainer container = containers.get(zoneName);
        return container == null || container.getContentGroup().getChildren().isEmpty();
    }

    public boolean isEditing() {
        return currentEditingZone.get() != null;
    }

    public StringProperty editingZoneProperty() {
        return currentEditingZone;
    }

    // --- Helper Methods ---

    public void refreshZoneClip(String zoneName) {
        String pathContent = visualizer.getShapeHelper().getZoneSvgContent(zoneName);
        if (pathContent != null) {
            getContainer(zoneName).setZonePathContent(pathContent);
        }
    }

    private void setLayerLocked(Node node, boolean locked) {
        if (node instanceof ShapeLayer) {
            ((ShapeLayer) node).setSystemLocked(locked);
        } else if (node instanceof ImageLayer) {
            ((ImageLayer) node).setSystemLocked(locked);
        } else if (node instanceof TextLayer) {
            ((TextLayer) node).setSystemLocked(locked);
        } else if (node instanceof GroupLayer) {
            ((GroupLayer) node).setSystemLocked(locked);
        } else if (node instanceof GroupLayerV2) {
            ((GroupLayerV2) node).setSystemLocked(locked);
        }
    }

    private void centerInZone(Node layer, String zone) {
        Node zoneNode = getZoneNode(zone);
        if (zoneNode != null) {
            // 1. Get Scene Center of the Zone
            Bounds b = zoneNode.getBoundsInLocal();
            double factorX = 0.5;
            double factorY = 0.5;

            // Specific offsets for sleeves/split zones (matches ShapeInteractionHelper)
            boolean isFrontOnLeft = visualizer.getOverlayManager().isFrontOnLeft();

            if ("MANGA_DELANTERA".equals(zone))
                factorX = isFrontOnLeft ? 0.25 : 0.75;
            else if ("MANGA_TRASERA".equals(zone))
                factorX = isFrontOnLeft ? 0.64 : 0.25; // 0.64 brings it closer to the back mannequin in interleaved
                                                       // layout
            else if ("PECHO".equals(zone))
                factorX = isFrontOnLeft ? 0.25 : 0.75;
            else if ("ESPALDA".equals(zone))
                factorX = isFrontOnLeft ? 0.75 : 0.25;
            else if ("SHORT_FRONT".equals(zone))
                factorX = isFrontOnLeft ? 0.25 : 0.75;
            else if ("SHORT_BACK".equals(zone))
                factorX = isFrontOnLeft ? 0.75 : 0.25;

            double sceneCx = b.getMinX() + b.getWidth() * factorX;
            double sceneCy = b.getMinY() + b.getHeight() * factorY;
            Point2D sceneP = zoneNode.localToScene(sceneCx, sceneCy);

            // 2. Convert Scene Point to Container Local
            SmartZoneContainer container = getContainer(zone);
            Point2D localP = container.getContentGroup().sceneToLocal(sceneP);

            // 3. Center Layer at that point
            Point2D localCenter = getLogicalCenter(layer);

            layer.setTranslateX(localP.getX() - localCenter.getX());
            layer.setTranslateY(localP.getY() - localCenter.getY());
        }
    }

    private Node getZoneNode(String zone) {
        if (visualizer.hasShirt()) {
            if ("PECHO".equals(zone) || "ESPALDA".equals(zone))
                return visualizer.getActiveShirtRenderer().getBody();
            if (zone.startsWith("MANGA"))
                return visualizer.getActiveShirtRenderer().getSleeves();
        }
        if (visualizer.hasShorts()) {
            if (zone.startsWith("SHORT"))
                return visualizer.getActiveShortsRenderer().getShorts();
        }
        return null;
    }

    public void forceConsistency() {
        // Ensure all container items are locked and transparent if not editing
        if (currentEditingZone.get() != null)
            return;

        for (Node node : layerManager.getLayerGroup().getChildren()) {
            // Logic to enforce state if needed
        }
    }

    public void reset() {
        // CRITICAL: Remove all smart containers from the scene graph before clearing the map
        if (layerManager != null && layerManager.getLayerGroup() != null) {
            containers.values().forEach(container -> {
                layerManager.getLayerGroup().getChildren().remove(container);
            });
        }
        containers.clear();
        currentEditingZone.set(null);
        if (uiManager != null) {
            uiManager.setZoneEditMode(false);
        }
    }

    private Point2D getLogicalCenter(Node layer) {
        double cx = 0;
        double cy = 0;
        if (layer instanceof ShapeLayer) {
            ShapeLayer sl = (ShapeLayer) layer;
            cx = sl.getVisualMinX() + sl.getLogicalWidth() / 2.0;
            cy = sl.getVisualMinY() + sl.getLogicalHeight() / 2.0;
        } else if (layer instanceof ImageLayer) {
            ImageLayer il = (ImageLayer) layer;
            cx = il.getLogicalWidth() / 2.0;
            cy = il.getLogicalHeight() / 2.0;
        } else if (layer instanceof TextLayer) {
            cx = 0;
            cy = 0;
        } else if (layer instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) layer;
            cx = gl.getBoundsMinX() + gl.getLogicalWidth() / 2.0;
            cy = gl.getBoundsMinY() + gl.getLogicalHeight() / 2.0;
        } else if (layer instanceof GroupLayerV2) {
            GroupLayerV2 glv2 = (GroupLayerV2) layer;
            Bounds b = glv2.calculateBounds();
            cx = b.getMinX() + b.getWidth() / 2.0;
            cy = b.getMinY() + b.getHeight() / 2.0;
        } else {
            Bounds b = layer.getBoundsInLocal();
            cx = b.getMinX() + b.getWidth() / 2.0;
            cy = b.getMinY() + b.getHeight() / 2.0;
        }
        return new Point2D(cx, cy);
    }
}
