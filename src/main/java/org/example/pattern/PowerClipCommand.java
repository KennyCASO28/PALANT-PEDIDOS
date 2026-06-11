package org.example.pattern;

import javafx.scene.Node;
import org.example.component.ShapeLayer;
import org.example.component.ImageLayer;
import org.example.component.TextLayer;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;
import org.example.component.PrendaVisualizer;

public class PowerClipCommand implements ICommand {

    private final PrendaVisualizer visualizer;
    private final Node target;
    private final String oldZone;
    private final String newZone;
    private final boolean isInitialPlacement;
    private final NodeMemento beforeState;
    private NodeMemento afterState;

    public PowerClipCommand(PrendaVisualizer visualizer, Node target, String oldZone, String newZone,
            boolean isInitialPlacement) {
        this.visualizer = visualizer;
        this.target = findTopMostUserGroup(target);
        this.oldZone = oldZone;
        this.newZone = newZone;
        this.isInitialPlacement = isInitialPlacement;
        this.beforeState = this.target != null ? new NodeMemento(this.target) : null;
    }

    @Override
    public void execute() {
        apply(newZone);
        if (target != null) {
            afterState = new NodeMemento(target);
        }
    }

    @Override
    public void redo() {
        if (afterState != null) {
            restoreState(afterState, newZone);
        } else {
            execute();
        }
    }

    @Override
    public String getName() {
        return (newZone != null) ? "Insertar en PowerClip" : "Extraer de PowerClip";
    }

    @Override
    public void undo() {
        if (beforeState != null) {
            restoreState(beforeState, oldZone);
        } else {
            apply(oldZone);
        }
    }

    private void apply(String zone) {
        if (target == null)
            return;

        boolean wasHistory = visualizer.getLayerManager().isPerformingHistoryAction();
        visualizer.getLayerManager().setPerformingHistoryAction(true);
        try {
        if (zone == null) {
            // Extraction
            // Extraction
            // 1. Calculate Scene Coordinates of the target BEFORE removing from container
            javafx.geometry.Point2D centerLocal = getLogicalCenter(target);
            javafx.geometry.Point2D centerScene = target.localToScene(centerLocal);

            // 2. Add to LayerManager (Root) - Auto removes from Container
            visualizer.getLayerManager().addLayer(target);

            // Find Top-Most USER_GROUP starting from 'target' (in case it WAS grouped, but
            // here checking re-parenting)
            // After adding to LayerManager, target's parent is now the visualizer's layer
            // group.

            // 3. Convert Scene Coordinates to New Parent Local
            if (target.getParent() != null && target.getScene() != null) {
                try {
                    javafx.geometry.Point2D newLocal = target.getParent().sceneToLocal(centerScene);

                    // 4. Update Translate to match visual position
                    target.setTranslateX(newLocal.getX() - centerLocal.getX());
                    target.setTranslateY(newLocal.getY() - centerLocal.getY());
                } catch (Exception e) {
                    // Fallback to 0,0 if transform fails (e.g. non-invertible or detached)
                    System.err.println("PowerClip Extraction Undo: Transform error, resetting to default pos.");
                }
            }

            setZoneRecursively(target, null);
            setLockedRecursively(target, false);
            setBeingEditedRecursively(target, false);
            target.setMouseTransparent(false);
        } else {
            // Insertion
            // Determine Target (Child or Group)
            Node actualTarget = target;
            Node temp = target.getParent();
            while (temp != null && "USER_GROUP".equals(temp.getId())) {
                actualTarget = temp;
                temp = temp.getParent();
            }
            visualizer.getPowerClipManager().addToContainer(actualTarget, zone, isInitialPlacement);
            setZoneRecursively(target, zone);
        }
        } finally {
            visualizer.getLayerManager().setPerformingHistoryAction(wasHistory);
        }
    }

    private void restoreState(NodeMemento state, String zone) {
        boolean wasHistory = visualizer.getLayerManager().isPerformingHistoryAction();
        visualizer.getLayerManager().setPerformingHistoryAction(true);
        try {
            state.restore();
            setZoneRecursively(target, zone);
            if (zone == null) {
                setLockedRecursively(target, false);
                setBeingEditedRecursively(target, false);
                target.setMouseTransparent(false);
            } else {
                visualizer.getPowerClipManager().refreshZoneClip(zone);
                org.example.component.helper.SmartZoneContainer container = visualizer.getPowerClipManager()
                        .getContainer(zone);
                container.updateItemState(target);
            }
            visualizer.getLayerManager().trackRestoredLayer(target);
            visualizer.getLayerManager().selectNode(target);
        } finally {
            visualizer.getLayerManager().setPerformingHistoryAction(wasHistory);
        }
    }

    private Node findTopMostUserGroup(Node start) {
        Node actualTarget = start;
        Node temp = start != null ? start.getParent() : null;
        while (temp != null) {
            if ("USER_GROUP".equals(temp.getId()) || temp instanceof GroupLayer || temp instanceof GroupLayerV2) {
                actualTarget = temp;
            }
            temp = temp.getParent();
        }
        return actualTarget;
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
            GroupLayerV2 glv2 = (GroupLayerV2) node;
            glv2.setActiveZone(zone);
            for (Node child : glv2.getUserLayers()) {
                setZoneRecursively(child, zone);
            }
        } else if (node instanceof javafx.scene.Group) {
            for (Node child : ((javafx.scene.Group) node).getChildren()) {
                setZoneRecursively(child, zone);
            }
        }
    }

    private void setLockedRecursively(Node node, boolean locked) {
        node.setMouseTransparent(false); // Ensure it's not ghosted anymore
        if (node instanceof ShapeLayer) {
            ((ShapeLayer) node).setLocked(locked);
        } else if (node instanceof ImageLayer) {
            ((ImageLayer) node).setLocked(locked);
        } else if (node instanceof TextLayer) {
            ((TextLayer) node).setLocked(locked);
        } else if (node instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) node;
            gl.setUserLocked(locked);
            for (Node child : gl.getUserLayers()) {
                setLockedRecursively(child, locked);
            }
        } else if (node instanceof GroupLayerV2) {
            GroupLayerV2 glv2 = (GroupLayerV2) node;
            glv2.setUserLocked(locked);
            for (Node child : glv2.getUserLayers()) {
                setLockedRecursively(child, locked);
            }
        } else if (node instanceof javafx.scene.Group) {
            for (Node child : ((javafx.scene.Group) node).getChildren()) {
                setLockedRecursively(child, locked);
            }
        }
    }

    private javafx.geometry.Point2D getLogicalCenter(Node layer) {
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
            javafx.geometry.Bounds b = glv2.calculateBounds();
            cx = b.getMinX() + b.getWidth() / 2.0;
            cy = b.getMinY() + b.getHeight() / 2.0;
        } else {
            javafx.geometry.Bounds b = layer.getBoundsInLocal();
            cx = b.getMinX() + b.getWidth() / 2.0;
            cy = b.getMinY() + b.getHeight() / 2.0;
        }
        return new javafx.geometry.Point2D(cx, cy);
    }

    private void setBeingEditedRecursively(Node node, boolean editing) {
        if (node instanceof ShapeLayer) {
            ((ShapeLayer) node).setIsBeingEdited(editing);
        } else if (node instanceof ImageLayer) {
            ((ImageLayer) node).setIsBeingEdited(editing);
        } else if (node instanceof TextLayer) {
            ((TextLayer) node).setBeingEdited(editing);
        } else if (node instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) node;
            gl.setIsBeingEdited(editing);
            for (Node child : gl.getUserLayers()) {
                setBeingEditedRecursively(child, editing);
            }
        } else if (node instanceof GroupLayerV2) {
            GroupLayerV2 glv2 = (GroupLayerV2) node;
            for (Node child : glv2.getUserLayers()) {
                setBeingEditedRecursively(child, editing);
            }
        } else if (node instanceof javafx.scene.Group) {
            for (Node child : ((javafx.scene.Group) node).getChildren()) {
                setBeingEditedRecursively(child, editing);
            }
        }
    }
}
