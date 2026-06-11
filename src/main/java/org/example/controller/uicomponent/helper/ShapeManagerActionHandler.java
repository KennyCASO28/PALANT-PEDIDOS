package org.example.controller.uicomponent.helper;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;
import org.example.component.ImageLayer;
import org.example.component.TextLayer;
import org.example.controller.uicomponent.ShapeManagerController;
import org.example.pattern.PropertyChangeCommand;
import org.example.pattern.CompositeCommand;
import org.example.pattern.LayerActionCommand;
import org.example.pattern.ICommand;
import org.example.utils.UIFactory;
import javafx.scene.control.Alert;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Handles the logic for actions performed in the ShapeManager.
 * Manages Undo/Redo command generation and recursive application of properties to groups.
 */
public class ShapeManagerActionHandler {

    private final PrendaVisualizer visualizer;
    private final ShapeManagerController controller;

    public ShapeManagerActionHandler(PrendaVisualizer visualizer, ShapeManagerController controller) {
        this.visualizer = visualizer;
        this.controller = controller;
    }

    public <T> void recordPropertyChange(String name, Function<ShapeLayer, T> getter,
                                         BiConsumer<ShapeLayer, T> setter, T newValue) {
        if (visualizer.getLayerManager() == null)
            return;

        Set<Node> selection = visualizer.getLayerManager().getSelectedNodes();
        if (selection.isEmpty()) {
            ShapeLayer active = controller.getActiveShapeLayer();
            if (active != null) {
                T oldValue = getter.apply(active);
                if (!Objects.equals(oldValue, newValue)) {
                    PropertyChangeCommand<T> cmd = new PropertyChangeCommand<>(name, oldValue, newValue,
                            val -> setter.accept(active, val));
                    visualizer.getHistoryManager().addCommand(cmd);
                    setter.accept(active, newValue);
                }
            }
            return;
        }

        CompositeCommand composite = new CompositeCommand(name);
        for (Node node : selection) {
            collectPropertyCommandsRecursive(node, name, getter, setter, newValue, composite);
        }

        if (!composite.isEmpty()) {
            visualizer.getHistoryManager().addCommand(composite);
            applyToSelection(layer -> setter.accept(layer, newValue));
        }
    }

    private <T> void collectPropertyCommandsRecursive(Node node, String name,
                                                       Function<ShapeLayer, T> getter, BiConsumer<ShapeLayer, T> setter,
                                                       T newValue, CompositeCommand composite) {
        if (node instanceof ShapeLayer) {
            ShapeLayer layer = (ShapeLayer) node;
            T oldValue = getter.apply(layer);
            if (!Objects.equals(oldValue, newValue)) {
                composite.addCommand(
                        new PropertyChangeCommand<>(name, oldValue, newValue, val -> setter.accept(layer, val)));
            }
        } else if (node instanceof GroupLayer) {
            for (Node child : ((GroupLayer) node).getUserLayers()) {
                collectPropertyCommandsRecursive(child, name, getter, setter, newValue, composite);
            }
        } else if (node instanceof GroupLayerV2) {
            for (Node child : ((GroupLayerV2) node).getUserLayers()) {
                collectPropertyCommandsRecursive(child, name, getter, setter, newValue, composite);
            }
        }
    }

    public void applyToSelection(Consumer<ShapeLayer> action) {
        if (visualizer.getLayerManager() == null) return;

        Set<Node> selection = visualizer.getLayerManager().getSelectedNodes();
        if (selection.isEmpty()) {
            ShapeLayer active = controller.getActiveShapeLayer();
            if (active != null) {
                action.accept(active);
            }
            return;
        }

        for (Node node : selection) {
            applyRecursive(node, action);
        }
    }

    private void applyRecursive(Node node, Consumer<ShapeLayer> action) {
        if (node instanceof ShapeLayer) {
            action.accept((ShapeLayer) node);
        } else if (node instanceof GroupLayer) {
            for (Node child : ((GroupLayer) node).getUserLayers()) {
                applyRecursive(child, action);
            }
        } else if (node instanceof GroupLayerV2) {
            for (Node child : ((GroupLayerV2) node).getUserLayers()) {
                applyRecursive(child, action);
            }
        }
    }

    public void deleteSelection() {
        Node selected = visualizer.getSelectedNode();
        if (selected == null) {
            selected = controller.getActiveShapeLayer();
        }

        if (selected != null) {
            ICommand cmd = new LayerActionCommand(
                    visualizer.getLayerManager(), selected,
                    LayerActionCommand.ActionType.REMOVE);
            cmd.execute();
            visualizer.getHistoryManager().addCommand(cmd);

            if (selected == controller.getActiveShapeLayer())
                controller.setActiveShapeLayer(null);
        } else {
            ShapeLayer.clearClipboard();
            ImageLayer.clearClipboard();
            TextLayer.clearClipboard();
            UIFactory.mostrarAlerta(Alert.AlertType.INFORMATION, "Portapapeles",
                    "Portapapeles vaciado correctamente.");
        }
    }

    public void toggleLock() {
        Node selected = visualizer.getSelectedNode();
        if (selected == null) selected = controller.getActiveShapeLayer();
        if (selected == null) return;

        boolean newState = false;
        if (selected instanceof ShapeLayer) {
            ShapeLayer sl = (ShapeLayer) selected;
            sl.setLocked(!sl.isUserLocked());
            newState = sl.isUserLocked();
        } else if (selected instanceof ImageLayer) {
            ImageLayer il = (ImageLayer) selected;
            il.setLocked(!il.isUserLocked());
            newState = il.isUserLocked();
        } else if (selected instanceof TextLayer) {
            TextLayer tl = (TextLayer) selected;
            tl.setLocked(!tl.isUserLocked());
            newState = tl.isUserLocked();
        } else if (selected instanceof GroupLayerV2) {
            GroupLayerV2 gl = (GroupLayerV2) selected;
            gl.setUserLocked(!gl.isUserLocked());
            newState = gl.isUserLocked();
        } else if (selected instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) selected;
            gl.setUserLocked(!gl.isUserLocked());
            newState = gl.isUserLocked();
        }
        
        controller.updateLockIcon();
    }

    public void weldSelectedShapes() {
        if (visualizer.getLayerManager() == null) return;

        Set<Node> selection = visualizer.getLayerManager().getSelectedNodes();
        if (selection.isEmpty()) {
            ShapeLayer active = controller.getActiveShapeLayer();
            if (active != null) {
                selection = new java.util.HashSet<>();
                selection.add(active);
            } else {
                return;
            }
        }

        java.util.List<ShapeLayer> shapeLayers = new java.util.ArrayList<>();
        for (Node node : selection) {
            if (node instanceof ShapeLayer) {
                shapeLayers.add((ShapeLayer) node);
            }
        }

        if (shapeLayers.size() < 2) {
            UIFactory.mostrarAlerta(Alert.AlertType.WARNING, "Soldar",
                    "Selecciona al menos 2 vectores para soldar.");
            return;
        }

        // Delegate to VectorBooleanHelper which works directly with bezierNodes (no Shape.union bugs)
        org.example.component.helper.VectorBooleanHelper.weldSelectedShapes(visualizer, shapeLayers);
    }


    public void unweldSelectedShape() {
        ShapeLayer active = controller.getActiveShapeLayer();
        if (active == null) return;
        // Delegate to VectorBooleanHelper which works directly with bezierNodes (no Shape.union bugs)
        org.example.component.helper.VectorBooleanHelper.unweldShape(visualizer, active);
    }
}
