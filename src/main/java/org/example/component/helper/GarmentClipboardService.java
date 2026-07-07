package org.example.component.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;

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
    private static boolean isCutOperation = false;

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
        isCutOperation = false;

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
                playCopyAnimation(node);
            }
        }
    }

    private Node cloneNode(Node node) {
        if (node instanceof ImageLayer) {
            ((ImageLayer) node).copyToClipboard();
            return ImageLayer.getClipboardCopy();
        } else if (node instanceof ShapeLayer) {
            return ((ShapeLayer) node).createDeepClone();
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
            newGroup.setActiveZone(originalGroup.getActiveZone());
            
            // Try to extract visualizer if possible
            if (historyManager != null && historyManager.getVisualizer() != null) {
                newGroup.setVisualizer(historyManager.getVisualizer());
            }

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
        isCutOperation = true; // Mark as cut operation
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
                resetOpacityRecursively(freshClone, 1.0);
                freshClones.add(freshClone);
            }
        }

        // Context-aware insertion
        layerFactory.setAnimationsSuspended(true);
        try {
            for (int i = 0; i < freshClones.size(); i++) {
                Node node = freshClones.get(i);
                Node originalNode = centralClipboard.get(i);
                // Save the exact translation from the clone (which matches original)
                double targetX = node.getTranslateX();
                double targetY = node.getTranslateY();

                // Get zones to check if crossing boundaries
                String originZone = getZoneOfNode(originalNode);
                String destZone = (powerClipManager != null && powerClipManager.isEditing()) 
                                ? powerClipManager.getCurrentEditingZone() : null;
                                
                boolean crossBoundary = false;
                if (originZone == null && destZone != null) crossBoundary = true;
                if (originZone != null && destZone == null) crossBoundary = true;
                if (originZone != null && destZone != null && !originZone.equals(destZone)) crossBoundary = true;

                if (!crossBoundary && !isCutOperation) {
                    // Same boundary, just offset slightly from original
                    targetX += 15;
                    targetY += 15;
                }

                layerManager.setPerformingHistoryAction(true); // Stop individual history recording
                layerFactory.addUserLayer(node);
                layerManager.setPerformingHistoryAction(false);
                
                // Restore the intended translation (overriding any auto-centering by LayerFactory)
                node.setTranslateX(targetX);
                node.setTranslateY(targetY);
                
                // Play spring pop-in animation
                playPasteAnimation(node);
            }
        } finally {
            layerFactory.setAnimationsSuspended(false);
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

    private String getZoneOfNode(Node node) {
        if (node instanceof org.example.component.GraphicLayer) {
            return ((org.example.component.GraphicLayer) node).getActiveZone();
        } else if (node instanceof GroupLayerV2) {
            return ((GroupLayerV2) node).getActiveZone();
        } else if (node instanceof GroupLayer) {
            return ((GroupLayer) node).getActiveZone();
        }
        return null;
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

    private void playCopyAnimation(Node node) {
        if (node == null) return;
        
        double origScaleX = node.getScaleX();
        double origScaleY = node.getScaleY();
        double origOpacity = node.getOpacity();

        ParallelTransition pt = new ParallelTransition();
        
        if (!(node instanceof GroupLayerV2) && !(node instanceof GroupLayer)) {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
            st.setFromX(origScaleX);
            st.setFromY(origScaleY);
            st.setToX(origScaleX * 1.06);
            st.setToY(origScaleY * 1.06);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            pt.getChildren().add(st);
        }

        FadeTransition ft = new FadeTransition(Duration.millis(120), node);
        ft.setFromValue(origOpacity);
        ft.setToValue(Math.max(0.4, origOpacity - 0.3));
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        pt.getChildren().add(ft);

        pt.setOnFinished(e -> {
            if (!(node instanceof GroupLayerV2) && !(node instanceof GroupLayer)) {
                node.setScaleX(origScaleX);
                node.setScaleY(origScaleY);
            }
            node.setOpacity(origOpacity);
        });
        pt.play();
    }

    private void playPasteAnimation(Node node) {
        if (node == null) return;

        double origScaleX = node.getScaleX();
        double origScaleY = node.getScaleY();
        double origOpacity = node.getOpacity();

        ParallelTransition pt = new ParallelTransition();

        if (!(node instanceof GroupLayerV2) && !(node instanceof GroupLayer)) {
            // Start state: small and transparent
            node.setScaleX(origScaleX * 0.4);
            node.setScaleY(origScaleY * 0.4);

            ScaleTransition st1 = new ScaleTransition(Duration.millis(140), node);
            st1.setFromX(origScaleX * 0.4);
            st1.setFromY(origScaleY * 0.4);
            st1.setToX(origScaleX * 1.15);
            st1.setToY(origScaleY * 1.15);
            st1.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition st2 = new ScaleTransition(Duration.millis(90), node);
            st2.setFromX(origScaleX * 1.15);
            st2.setFromY(origScaleY * 1.15);
            st2.setToX(origScaleX);
            st2.setToY(origScaleY);
            st2.setInterpolator(Interpolator.EASE_IN);

            SequentialTransition scaleSeq = new SequentialTransition(st1, st2);
            pt.getChildren().add(scaleSeq);
        }

        node.setOpacity(0.0);

        FadeTransition ft = new FadeTransition(Duration.millis(140), node);
        ft.setFromValue(0.0);
        ft.setToValue(origOpacity);
        ft.setInterpolator(Interpolator.EASE_OUT);
        pt.getChildren().add(ft);

        pt.setOnFinished(e -> {
            if (!(node instanceof GroupLayerV2) && !(node instanceof GroupLayer)) {
                node.setScaleX(origScaleX);
                node.setScaleY(origScaleY);
            }
            node.setOpacity(origOpacity);
        });
        pt.play();
    }

    private void resetOpacityRecursively(Node node, double val) {
        if (node == null) return;
        node.setOpacity(val);
        if (node instanceof javafx.scene.Group) {
            for (Node child : ((javafx.scene.Group) node).getChildren()) {
                resetOpacityRecursively(child, val);
            }
        }
    }
}
