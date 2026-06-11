package org.example.component.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;

import org.example.component.ImageLayer;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;
import org.example.component.UserLayerManager;
import org.example.pattern.LayerActionCommand;

public class GarmentClipboardService {

    private final UserLayerManager layerManager;
    private final PrendaLayerFactory layerFactory;
    private final PrendaHistoryManager historyManager;
    private final PowerClipManager powerClipManager;

    // Centralized Clipboard for Multi-Selection
    private static List<Node> centralClipboard = null;

    public GarmentClipboardService(UserLayerManager layerManager, PrendaLayerFactory layerFactory, 
                                   PrendaHistoryManager historyManager, PowerClipManager powerClipManager) {
        this.layerManager = layerManager;
        this.layerFactory = layerFactory;
        this.historyManager = historyManager;
        this.powerClipManager = powerClipManager;
    }

    public void copySelectedLayer() {
        // Clear all legacy clipboards
        ImageLayer.clearClipboard();
        ShapeLayer.clearClipboard();
        TextLayer.clearClipboard();
        centralClipboard = null;

        // Get ALL selected nodes
        Set<Node> selectedNodes = layerManager.getSelectedNodes();
        if (selectedNodes.isEmpty()) {
            return;
        }

        // Clone all selected items into the central clipboard
        centralClipboard = new ArrayList<>();
        for (Node node : selectedNodes) {
            Node clone = cloneNode(node);
            if (clone != null) {
                centralClipboard.add(clone);
            }
        }
    }

    private Node cloneNode(Node node) {
        if (node instanceof ImageLayer) {
            ((ImageLayer) node).copyToClipboard();
            return ImageLayer.getClipboardCopy();
        } else if (node instanceof ShapeLayer) {
            ((ShapeLayer) node).copyToClipboard();
            return ShapeLayer.getClipboardCopy();
        } else if (node instanceof TextLayer) {
            ((TextLayer) node).copyToClipboard();
            return TextLayer.getClipboardCopy();
        } else if (node instanceof GroupLayerV2) {
            GroupLayerV2 originalGroup = (GroupLayerV2) node;
            GroupLayerV2 newGroup = new GroupLayerV2();
            newGroup.setId("USER_GROUP");

            for (Node child : originalGroup.getUserLayers()) {
                Node childClone = cloneNode(child);
                if (childClone != null) {
                    newGroup.addChild(childClone);
                }
            }

            newGroup.setTranslateX(originalGroup.getTranslateX());
            newGroup.setTranslateY(originalGroup.getTranslateY());
            newGroup.setInternalRotation(originalGroup.getInternalRotation());
            newGroup.setInternalScale(originalGroup.getInternalScaleX(), originalGroup.getInternalScaleY());
            newGroup.setInternalShear(originalGroup.getInternalShearX(), originalGroup.getInternalShearY());

            newGroup.recalculateBounds();
            return newGroup;
        } else if (node instanceof GroupLayer) {
            // Clone the group structure with all its children (Legacy)
            GroupLayer originalGroup = (GroupLayer) node;
            GroupLayer newGroup = new GroupLayer();
            newGroup.setId("USER_GROUP");

            // Clone all children recursively
            for (Node child : originalGroup.getUserLayers()) {
                Node childClone = cloneNode(child);
                if (childClone != null) {
                    // Preserve relative positions
                    childClone.setTranslateX(child.getTranslateX());
                    childClone.setTranslateY(child.getTranslateY());
                    newGroup.getContentGroup().getChildren().add(childClone);
                }
            }

            // Copy group transforms
            newGroup.setTranslateX(originalGroup.getTranslateX());
            newGroup.setTranslateY(originalGroup.getTranslateY());

            // CRITICAL: Copy internal transforms
            newGroup.getRotateTransform().setAngle(originalGroup.getRotateTransform().getAngle());
            newGroup.getScaleTransform().setX(originalGroup.getScaleTransform().getX());
            newGroup.getScaleTransform().setY(originalGroup.getScaleTransform().getY());

            newGroup.recalculateBounds();
            return newGroup;
        }
        return null;
    }

    public void cutSelectedLayer() {
        copySelectedLayer(); // Copy first
        deleteSelectedLayer(); // Then delete
    }

    public void pasteLayer() {
        if (centralClipboard == null || centralClipboard.isEmpty()) {
            return; // Nothing to paste
        }

        // Re-clone all items from clipboard (to support multiple pastes)
        List<Node> freshClones = new ArrayList<>();
        for (Node stored : centralClipboard) {
            Node freshClone = cloneNode(stored);
            if (freshClone != null) {
                freshClones.add(freshClone);
            }
        }

        // Context-aware insertion
        for (Node node : freshClones) {
            // Use Factory to ensure ALL Handlers (PowerClip, etc) are wired correctly.
            // Factory internally handles the "If editing PowerClip" insertion logic.
            layerManager.setPerformingHistoryAction(true); // Stop individual history recording
            layerFactory.addUserLayer(node);
            layerManager.setPerformingHistoryAction(false);
        }
        
        // Record History as ONE action
        if (historyManager != null && !freshClones.isEmpty()) {
            String zone = (powerClipManager != null && powerClipManager.isEditing()) 
                          ? powerClipManager.getCurrentEditingZone() : null;
            layerManager.setPerformingHistoryAction(true); // Temporarily stop individual recordings
            LayerActionCommand batchCmd = new LayerActionCommand(
                    layerManager, new ArrayList<>(freshClones),
                    LayerActionCommand.ActionType.ADD, zone);
            historyManager.addCommand(batchCmd);
            layerManager.setPerformingHistoryAction(false);
        }

        // Select all pasted items
        if (!freshClones.isEmpty()) {
            layerManager.clearSelection();
            for (Node node : freshClones) {
                layerManager.addToSelection(node);
            }
        }
    }

    // Delete all selected layers
    public void deleteSelectedLayer() {
        List<Node> toDelete = new ArrayList<>(layerManager.getSelectedNodes());
        if (toDelete.isEmpty()) return;

        // Record History
        if (historyManager != null) {
            String zone = (powerClipManager != null && powerClipManager.isEditing()) 
                          ? powerClipManager.getCurrentEditingZone() : null;
            layerManager.setPerformingHistoryAction(true);
            LayerActionCommand batchCmd = new LayerActionCommand(
                    layerManager, toDelete,
                    LayerActionCommand.ActionType.REMOVE, zone);
            historyManager.addCommand(batchCmd);
            layerManager.setPerformingHistoryAction(false);
        }

        // Remove all selected nodes
        for (Node node : toDelete) {
            layerManager.removeLayer(node);
        }
    }
}
