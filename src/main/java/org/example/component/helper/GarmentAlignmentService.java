package org.example.component.helper;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;

import org.example.component.PrendaVisualizer;
import org.example.component.UserLayerManager;
import org.example.pattern.CompositeCommand;
import org.example.pattern.NodeMemento;
import org.example.pattern.TransformCommand;
import org.example.utils.UIFactory;
import org.example.component.renderer.GarmentRenderer;
import org.example.component.renderer.ShortsRenderer;

public class GarmentAlignmentService {

    private final PrendaVisualizer visualizer;
    private final UserLayerManager layerManager;
    private final PowerClipManager powerClipManager;
    private final PrendaHistoryManager historyManager;
    private final PrendaOverlayManager overlayManager;
    private final Pane referenceLayer;

    private Node activeReferenceAnchor;

    public GarmentAlignmentService(PrendaVisualizer visualizer, UserLayerManager layerManager, 
                                   PowerClipManager powerClipManager, PrendaHistoryManager historyManager,
                                   PrendaOverlayManager overlayManager, Pane referenceLayer) {
        this.visualizer = visualizer;
        this.layerManager = layerManager;
        this.powerClipManager = powerClipManager;
        this.historyManager = historyManager;
        this.overlayManager = overlayManager;
        this.referenceLayer = referenceLayer;
    }

    public Node getActiveReferenceAnchor() {
        return activeReferenceAnchor;
    }

    public void setActiveReferenceAnchor(Node anchor) {
        this.activeReferenceAnchor = anchor;
    }

    public void alignSelected(String type) {
        Set<Node> selectedSet = layerManager.getSelectedNodes();
        if (selectedSet.isEmpty())
            return;

        List<Node> nodes = new ArrayList<>(selectedSet);
        Node anchor;

        // Determine anchor: prioritize explicitly selected reference point if one was active
        if (activeReferenceAnchor != null) {
            anchor = activeReferenceAnchor;
            // The anchor itself shouldn't move, but we include it in the nodes list for AlignmentHelper calculations
            if (!nodes.contains(anchor)) {
                nodes.add(anchor);
            }
        } else {
            anchor = layerManager.getSelectedNode();
        }

        if (anchor == null || historyManager == null)
            return;

        // --- UNDO HISTORY SUPPORT ---
        String currentZone = powerClipManager != null ? powerClipManager.getCurrentEditingZone() : null;
        String commandName = "C".equals(type) ? "Centrar Horizontal" : "Alinear Vertical";
        CompositeCommand composite = new CompositeCommand(commandName);

        // 1. Capture "Before" states (Only for nodes that ARE NOT the anchor)
        Map<Node, NodeMemento> beforeStates = new HashMap<>();
        for (Node n : selectedSet) {
            if (n != anchor) {
                beforeStates.put(n, new NodeMemento(n));
            }
        }

        // 2. Perform Alignment
        if ("C".equals(type)) {
            AlignmentHelper.alignCenterHorizontal(nodes, anchor);
        } else if ("E".equals(type)) {
            AlignmentHelper.alignMiddleVertical(nodes, anchor);
        }

        // 3. Capture "After" states and create commands
        for (Node n : selectedSet) {
            if (n != anchor) {
                NodeMemento after = new NodeMemento(n);
                composite.addCommand(new TransformCommand(n, beforeStates.get(n), after, currentZone));
            }
        }

        // 4. Record in History
        if (!composite.isEmpty()) {
            historyManager.addCommand(composite);
        }
    }

    public void updateReferencePoints() {
        referenceLayer.getChildren().clear();
        if (visualizer.getBtnToggleRefPoints() == null || !visualizer.getBtnToggleRefPoints().isSelected()) {
            activeReferenceAnchor = null; // Reset anchor if tool disabled
            return;
        }

        // SMART FILTERING: Only show points for the active editing zone if one is selected
        String editingZone = powerClipManager != null ? powerClipManager.getCurrentEditingZone() : null;

        String[] zones = { "PECHO", "ESPALDA", "MANGA_DELANTERA", "MANGA_TRASERA", "SHORT_FRONT", "SHORT_BACK" };
        for (String zone : zones) {
            // Only show points for the zone being edited IF one is selected.
            // If none is selected, show ALL points so the user can see where they are.
            if (editingZone != null && !editingZone.equals(zone)) {
                continue;
            }

            String content = "";
            if (visualizer.hasShirt()) {
                GarmentRenderer activeShirt = visualizer.getActiveShirtRenderer();
                if ("PECHO".equals(zone))
                    content = overlayManager.getSplitZoneContent(activeShirt.getBody(), true);
                else if ("ESPALDA".equals(zone))
                    content = overlayManager.getSplitZoneContent(activeShirt.getBody(), false);
                else if ("MANGA_DELANTERA".equals(zone))
                    content = overlayManager.getSplitZoneContent(activeShirt.getSleeves(), true);
                else if ("MANGA_TRASERA".equals(zone))
                    content = overlayManager.getSplitZoneContent(activeShirt.getSleeves(), false);
            }
            if (visualizer.hasShorts()) {
                ShortsRenderer activeShorts = visualizer.getActiveShortsRenderer();
                if ("SHORT_FRONT".equals(zone))
                    content = overlayManager.getSplitZoneContent(activeShorts.getShorts(), true);
                else if ("SHORT_BACK".equals(zone))
                    content = overlayManager.getSplitZoneContent(activeShorts.getShorts(), false);
            }

            if (content != null && !content.isEmpty()) {
                // Split content into individual subpaths to handle multiple pieces
                String[] subpaths = content.split("(?=[Mm])");
                for (String subpath : subpaths) {
                    if (subpath.trim().isEmpty())
                        continue;

                    SVGPath temp = new SVGPath();
                    temp.setContent(subpath);
                    javafx.geometry.Bounds bounds = temp.getLayoutBounds();
                    // Ignore tiny artifacts
                    if (bounds.getWidth() > 10 && bounds.getHeight() > 10) {
                        double cx = bounds.getMinX() + bounds.getWidth() / 2;
                        double cy = bounds.getMinY() + bounds.getHeight() / 2;
                        createReferencePoint(cx, cy, zone);
                    }
                }
            }
        }
    }

    private void createReferencePoint(double x, double y, String zone) {
        StackPane point = new StackPane();
        point.setPrefSize(14, 14);
        point.setLayoutX(x - 7);
        point.setLayoutY(y - 7);

        // Marker Background: Ghost Green (Transparent & Small)
        Circle bg = new Circle(7, Color.web("#2ecc71", 0.3));
        bg.setStroke(Color.rgb(0, 0, 0, 0.2));
        bg.setStrokeWidth(0.8);
        bg.setEffect(null); // No shadow for "ghost" look

        // Flag icon (Small & Greyish)
        Node flagIconNode = UIFactory.crearIcono("mdi2f-flag", 9, "rgba(0,0,0,0.4)");

        point.getChildren().addAll(bg, flagIconNode);
        point.setStyle("-fx-cursor: hand;");

        // --- HOVER EFFECTS ---
        point.setOnMouseEntered(e -> {
            if (activeReferenceAnchor == point) return;
            bg.setFill(Color.web("#2ecc71", 0.8));
            bg.setStroke(Color.WHITE);
            bg.setStrokeWidth(1.2);
            bg.setEffect(new javafx.scene.effect.DropShadow(5, Color.web("#2ecc71")));
            ((org.kordamp.ikonli.javafx.FontIcon)flagIconNode).setIconColor(Color.WHITE);
        });

        point.setOnMouseExited(e -> {
            if (activeReferenceAnchor == point) return;
            bg.setFill(Color.web("#2ecc71", 0.3));
            bg.setStroke(Color.rgb(0, 0, 0, 0.2));
            bg.setStrokeWidth(0.8);
            bg.setEffect(null);
            ((org.kordamp.ikonli.javafx.FontIcon)flagIconNode).setIconColor(Color.web("rgba(0,0,0,0.4)"));
        });

        Tooltip.install(point, new Tooltip(
                "Centro de " + zone + "\nClick: Alinear seleccionado aquí\nCtrl + Click: Usar como ancla para E/C"));

        point.setOnMousePressed(e -> {
            visualizer.requestFocus();
            e.consume();
        });

        point.setOnMouseClicked(e -> {

            if (e.isControlDown()) {
                // Toggle as persistent anchor for keyboard shortcuts C/E
                if (activeReferenceAnchor == point) {
                    activeReferenceAnchor = null;
                    bg.setFill(Color.web("#2ecc71", 0.3));
                    bg.setEffect(null);
                } else {
                    // Reset previous anchor highlight if it's a flag point
                    if (activeReferenceAnchor instanceof StackPane) {
                        StackPane prev = (StackPane) activeReferenceAnchor;
                        if (prev.getChildren().get(0) instanceof Circle) {
                            ((Circle) prev.getChildren().get(0)).setFill(Color.web("#2ecc71", 0.3));
                            prev.setEffect(null);
                        }
                    } else if (activeReferenceAnchor instanceof org.example.component.HotspotLayer) {
                        // Also reset manual hotspot if it was the anchor
                        ((org.example.component.HotspotLayer)activeReferenceAnchor).resetHighlight();
                    }

                    activeReferenceAnchor = point;
                    bg.setFill(Color.web("#3498db")); // Blue highlight for active anchor
                    bg.setEffect(new javafx.scene.effect.DropShadow(10, Color.web("#3498db")));
                }
            } else {
                // Immediate Alignment on Click
                Set<Node> selected = layerManager.getSelectedNodes();
                if (!selected.isEmpty() && historyManager != null) {
                    // --- UNDO HISTORY SUPPORT ---
                    String currentZone = powerClipManager != null ? powerClipManager.getCurrentEditingZone() : null;
                    CompositeCommand composite = new CompositeCommand("Alinear con Punto de Referencia");

                    // 1. Capture "Before" states
                    Map<Node, org.example.pattern.NodeMemento> beforeStates = new HashMap<>();
                    for (Node n : selected) {
                        beforeStates.put(n, new org.example.pattern.NodeMemento(n));
                    }

                    // 2. Perform Alignment
                    List<Node> nodes = new ArrayList<>(selected);
                    nodes.add(point);
                    AlignmentHelper.alignCenterHorizontal(nodes, point);
                    AlignmentHelper.alignMiddleVertical(nodes, point);

                    // 3. Capture "After" states and create commands
                    for (Node n : selected) {
                        org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(n);
                        composite.addCommand(new org.example.pattern.TransformCommand(n, beforeStates.get(n), after, currentZone));
                    }

                    // 4. Record in History
                    historyManager.addCommand(composite);
                }
            }
            e.consume();
        });

        referenceLayer.getChildren().add(point);
    }
    public void removeHotspot(Node h) {
        referenceLayer.getChildren().remove(h);
    }

    public void clearHotspots() {
        referenceLayer.getChildren().clear();
        activeReferenceAnchor = null;
    }

    public void resetNumberToFactory(org.example.component.NumberComposition nc) {
        if (nc == null) return;
        nc.setPosition(0, 0, 1.0);
        visualizer.notifyStateChanged();
    }
}
