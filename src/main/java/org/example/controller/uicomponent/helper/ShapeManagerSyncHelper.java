package org.example.controller.uicomponent.helper;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.example.component.ShapeLayer;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;
import org.example.controller.uicomponent.ShapeManagerController;
import org.example.model.ShapeType;
import org.example.component.PrendaVisualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Synchronizes the ShapeManager UI state with the currently selected layer(s).
 */
public class ShapeManagerSyncHelper {

    private final PrendaVisualizer visualizer;
    private final ShapeManagerController controller;

    public ShapeManagerSyncHelper(PrendaVisualizer visualizer, ShapeManagerController controller) {
        this.visualizer = visualizer;
        this.controller = controller;
    }

    public void syncUIWithSelection(org.example.component.GraphicLayer layer) {
        controller.setIsUpdatingUI(true);
        try {
            controller.setActiveGraphicLayer(layer);

            Set<Node> selectedNodes = visualizer.getLayerManager().getSelectedNodes();
            List<ShapeLayer> selectedShapes = new ArrayList<>();

            GroupLayer selectedGroupV1 = null;
            GroupLayerV2 selectedGroupV2 = null;

            for (Node node : selectedNodes) {
                if (node instanceof GroupLayerV2) {
                    selectedGroupV2 = (GroupLayerV2) node;
                } else if (node instanceof GroupLayer) {
                    selectedGroupV1 = (GroupLayer) node;
                }
                collectShapeLayers(node, selectedShapes);
            }

            boolean multipleSelected = selectedShapes.size() > 1;

            if (selectedGroupV2 != null) {
                loadGroupLayerV2Settings(selectedGroupV2);
            } else if (selectedGroupV1 != null) {
                loadGroupLayerSettings(selectedGroupV1);
            } else if (layer != null && !multipleSelected) {
                if (layer instanceof ShapeLayer) {
                    loadSingleLayerSettings((ShapeLayer) layer);
                } else {
                    loadGraphicLayerSettings(layer);
                }
            } else if (multipleSelected) {
                loadMultipleLayerSettings(selectedShapes);
            }
            
            controller.updateTransformButtonState();
            controller.updateLockIcon();

        } finally {
            controller.setIsUpdatingUI(false);
        }
    }

    private void collectShapeLayers(Node node, List<ShapeLayer> result) {
        if (node instanceof ShapeLayer) {
            result.add((ShapeLayer) node);
        } else if (node instanceof GroupLayerV2) {
            for (Node child : ((GroupLayerV2) node).getUserLayers()) {
                collectShapeLayers(child, result);
            }
        } else if (node instanceof GroupLayer) {
            for (Node child : ((GroupLayer) node).getUserLayers()) {
                collectShapeLayers(child, result);
            }
        }
    }

    private void loadGraphicLayerSettings(org.example.component.GraphicLayer layer) {
        if (controller.getUiOrchestrator() != null && controller.getUiOrchestrator().getTxtToolbarAngle() != null) {
            controller.getUiOrchestrator().getTxtToolbarAngle().setText(String.format("%.1f°", layer.getInternalRotation()));
        }
    }

    private void loadSingleLayerSettings(ShapeLayer layer) {
        controller.updateToolSelection(layer.getShapeType());
controller.getButtonFactory().updatePickerGraphic(controller.getFillPicker(), (Color) layer.getFillColor());

        controller.getButtonFactory().updatePickerGraphic(controller.getStrokePicker(), (Color) layer.getStrokeColor());
        
        if (controller.getStrokeWidthSlider() != null) {
            controller.getStrokeWidthSlider().setValue(layer.getStrokeWidth());
        }

        loadGraphicLayerSettings(layer);
        
        // Sync Contour
        boolean contourActive = layer.getContourSteps() > 0;
        controller.setContourUI(contourActive, layer.getContourSteps(), layer.getContourDistance());

        // Sync Transparency
        controller.setTransparencyUI(layer.isTransparencyEnabled(), 
                                   layer.getTransparencyStartAlpha(), 
                                   layer.getTransparencyEndAlpha(), 
                                   layer.getTransparencyAngle(), 
                                   layer.getTransparencyBalance());
    }

    private void loadMultipleLayerSettings(List<ShapeLayer> layers) {
        if (layers.isEmpty()) return;

        Color firstFillColor = (Color) layers.get(0).getFillColor();
        Color firstStrokeColor = (Color) layers.get(0).getStrokeColor();
        boolean fillUniform = true;
        boolean strokeUniform = true;
        for (ShapeLayer layer : layers) {
            if (!colorsEqual(firstFillColor, (Color) layer.getFillColor())) {
                fillUniform = false;
            }
            if (!colorsEqual(firstStrokeColor, (Color) layer.getStrokeColor())) {
                strokeUniform = false;
            }
        }
        controller.getButtonFactory().updatePickerGraphic(controller.getFillPicker(), fillUniform ? firstFillColor : null);
        controller.getButtonFactory().updatePickerGraphic(controller.getStrokePicker(), strokeUniform ? firstStrokeColor : null);
    }

    private void loadGroupLayerSettings(GroupLayer group) {
        controller.getButtonFactory().updatePickerGraphic(controller.getFillPicker(), group.getFillColor());
        controller.getButtonFactory().updatePickerGraphic(controller.getStrokePicker(), group.getStrokeColor());
    }

    private void loadGroupLayerV2Settings(GroupLayerV2 group) {
        controller.getButtonFactory().updatePickerGraphic(controller.getFillPicker(), group.getFillColor());
        controller.getButtonFactory().updatePickerGraphic(controller.getStrokePicker(), group.getStrokeColor());
    }

    private boolean colorsEqual(Color c1, Color c2) {
        if (c1 == null && c2 == null) return true;
        if (c1 == null || c2 == null) return false;
        return Math.abs(c1.getRed() - c2.getRed()) < 0.01 &&
                Math.abs(c1.getGreen() - c2.getGreen()) < 0.01 &&
                Math.abs(c1.getBlue() - c2.getBlue()) < 0.01 &&
                Math.abs(c1.getOpacity() - c2.getOpacity()) < 0.01;
    }
}
