package org.example.component.helper;

import javafx.scene.input.TransferMode;
import org.example.component.ImageLayer;
import org.example.component.TextLayer;
import org.example.component.ShapeLayer;
import org.example.component.UserLayerManager;
import javafx.geometry.Point2D;
import org.example.component.PrendaVisualizer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;

/**
 * Factory and Configurator for User Layers (Images, Text).
 * Handles initialization, event wiring (Drag, Resize, Edit), and Drag-and-Drop
 * support.
 */
public class PrendaLayerFactory {

    private final UserLayerManager layerManager;
    private final PrendaVisualizer visualizer; // Callback reference (Circle of Life)

    // We need access to check shorts state for visibility logic
    private final BooleanSupplier shortsVisibilityCondition;

    private boolean animationsSuspended = false;

    public void setAnimationsSuspended(boolean value) {
        this.animationsSuspended = value;
    }

    public PrendaLayerFactory(UserLayerManager layerManager, PrendaVisualizer visualizer,
            BooleanSupplier shortsVisibilityCondition) {
        this.layerManager = layerManager;
        this.visualizer = visualizer;
        this.shortsVisibilityCondition = shortsVisibilityCondition;
    }

    public void addLayer(ImageLayer layer) {
        addImageLayer(layer);
    }

    public void addImageLayer(ImageLayer layer) {
        layer.setVisualizer(visualizer);
        if (visualizer.getPowerClipManager().isEditing()) {
            String zone = visualizer.getPowerClipManager().getCurrentEditingZone();
            visualizer.getPowerClipManager().addToContainer(layer, zone, true);
            recordAddInHistory(layer, zone);
        } else {
            if (layer.getTranslateX() == 0 && layer.getTranslateY() == 0) {
                layer.setTranslateX(250);
                layer.setTranslateY(250);
            }
            layerManager.addLayer(layer);
        }

        layer.setAvailableZonesSupplier(() -> visualizer.getAvailableZones());
        layer.setPowerClipHandler(zone -> visualizer.applySmartPowerClip(layer, zone, false));
        layer.setInternalPowerClipHandler(target -> visualizer.applyInternalPowerClip(layer, target));
        layer.setEditContentHandler(() -> visualizer.enterEditMode(layer));
        layer.setPasteHandler(() -> {
            ImageLayer pastedLayer = ImageLayer.getClipboardCopy();
            if (pastedLayer != null) {
                if (visualizer.getPowerClipManager().isEditing()) {
                    addImageLayer(pastedLayer);
                } else {
                    pastedLayer.setActiveZone(null);
                    addImageLayer(pastedLayer);
                }
            }
        });

        layer.setShortsOptionsVisibility(shortsVisibilityCondition);

        layer.setOnDragHandler((dx, dy) -> {
            for (javafx.scene.Node node : layerManager.getSelectedNodes()) {
                boolean isParentOfLayer = false;
                if (node instanceof javafx.scene.Group) {
                    javafx.scene.Node temp = layer;
                    while (temp != null) {
                        if (temp == node) {
                            isParentOfLayer = true;
                            break;
                        }
                        temp = temp.getParent();
                    }
                }

                if (node != layer && !isParentOfLayer) {
                    boolean isNodeLocked = false;
                    if (node instanceof org.example.component.ShapeLayer) {
                        isNodeLocked = ((org.example.component.ShapeLayer) node).isLocked();
                    } else if (node instanceof org.example.component.ImageLayer) {
                        isNodeLocked = ((org.example.component.ImageLayer) node).isLocked();
                    }

                    if (!isNodeLocked) {
                        node.setTranslateX(node.getTranslateX() + dx);
                        node.setTranslateY(node.getTranslateY() + dy);
                    }
                }
            }
        });

        layerManager.selectNode(layer);
        
        if (!animationsSuspended) {
            playAppearanceAnimation(layer);
        }
    }

    public void addUserLayer(javafx.scene.Node node) {
        if (!visualizer.getPowerClipManager().isEditing()) {
            clearActiveZoneRecursively(node);
        }

        if (node instanceof ImageLayer) {
            addImageLayer((ImageLayer) node);
        } else if (node instanceof org.example.component.ShapeLayer) {
            addShapeLayer((org.example.component.ShapeLayer) node);
        } else if (node instanceof TextLayer) {
            addTextLayer((TextLayer) node);
        } else {
            if (visualizer.getPowerClipManager().isEditing()) {
                String zone = visualizer.getPowerClipManager().getCurrentEditingZone();
                visualizer.getPowerClipManager().addToContainer(node, zone, true);
                // recordAddInHistory might require GraphicLayer, but let's just let it be handled if possible or skip history here if not supported.
            } else {
                layerManager.addLayer(node);
            }
        }
    }

    public void addTextLayer(TextLayer layer) {
        layerManager.clearSelection();
        layer.setVisualizer(visualizer);

        if (visualizer.getPowerClipManager().isEditing()) {
            String zone = visualizer.getPowerClipManager().getCurrentEditingZone();
            visualizer.getPowerClipManager().addToContainer(layer, zone, true);
            recordAddInHistory(layer, zone);
        } else {
            layerManager.addLayer(layer);
        }

        layer.setEditHandler(textLayer -> {
            visualizer.selectLayer(textLayer);
        });

        layer.setAvailableZonesSupplier(() -> visualizer.getAvailableZones());
        layer.setPowerClipHandler(zone -> visualizer.applySmartPowerClip(layer, zone, false));
        layer.setDeleteHandler(() -> {
            layerManager.removeLayer(layer);
        });

        // Drag Handler (Group Move)
        layer.setOnDragHandler((dx, dy) -> {
            for (javafx.scene.Node node : layerManager.getSelectedNodes()) {
                // Fix: Don't move the node if it's the layer itself OR if it's a Group that
                // contains the layer.
                // Because ShapeLayer already handles the movement of itself or its parent
                // group.
                // The iteration here is intended for MULTI-SELECTION of SIBLINGS.
                // If 'node' is a Group and 'layer' is inside it, the Group is already moving!
                boolean isParentOfLayer = false;
                if (node instanceof javafx.scene.Group) {
                    javafx.scene.Node temp = layer;
                    while (temp != null) {
                        if (temp == node) {
                            isParentOfLayer = true;
                            break;
                        }
                        temp = temp.getParent();
                    }
                }

                if (node != layer && !isParentOfLayer) {
                    node.setTranslateX(node.getTranslateX() + dx);
                    node.setTranslateY(node.getTranslateY() + dy);
                }
            }
        });

        // Resize Handler (Group Scale)
        layer.setOnResizeHandler((scaleFactor, ratio) -> {
            double leaderX = layer.getTranslateX();
            double leaderY = layer.getTranslateY();

            for (javafx.scene.Node node : layerManager.getSelectedNodes()) {
                if (node != layer) {
                    double relX = node.getTranslateX() - leaderX;
                    double relY = node.getTranslateY() - leaderY;

                    node.setTranslateX(leaderX + relX * scaleFactor);
                    node.setTranslateY(leaderY + relY * scaleFactor);

                    if (node instanceof TextLayer) {
                        ((TextLayer) node).scale(scaleFactor);
                    }
                }
            }
        });

        layer.setPasteHandler(() -> {
            TextLayer pasted = TextLayer.getClipboardCopy(); // Assuming TextLayer has clipboard support
            if (pasted != null) {
                if (visualizer.getPowerClipManager().isEditing()) {
                    addTextLayer(pasted); // Handles insertion check
                } else {
                    pasted.setActiveZone(null);
                    addTextLayer(pasted);
                }
            }
        });

        layerManager.selectNode(layer);
        
        if (!animationsSuspended) {
            playAppearanceAnimation(layer);
        }
    }

    // --- Shape Layer Creation ---

    public void addShapeLayer(org.example.component.ShapeLayer layer) {
        layer.setVisualizer(visualizer); // Inject before adding to ensure correct viewport scale
        
        if (layer.getParent() == null) {
            // Smart Insertion for Shape
            boolean insertedInPowerClip = false;
            String zone = layer.getActiveZone();
            if (zone == null && visualizer.getPowerClipManager().isEditing()) {
                if (layer.getState().svgPathData == null || layer.getState().svgPathData.isEmpty()) {
                    zone = visualizer.getPowerClipManager().getCurrentEditingZone();
                }
            }
            if (zone != null) {
                visualizer.getPowerClipManager().addToContainer(layer, zone, false);
                recordAddInHistory(layer, zone);
                insertedInPowerClip = true;
            } else {
                if (layer.getTranslateX() == 0 && layer.getTranslateY() == 0) {
                    if (layer.getState().svgPathData == null || layer.getState().svgPathData.isEmpty()) {
                        layer.setTranslateX(250);
                        layer.setTranslateY(250);
                    }
                }
                layerManager.addLayer(layer);
            }
        } else {
            // Already has a parent (like inside a group in unweldShape), just register it
            if (!layerManager.getLayers().contains(layer)) {
                layerManager.getLayers().add(layer);
            }
        }

        // PowerClip Handler
        layer.setVisualizer(visualizer);
        layer.setAvailableZonesSupplier(() -> visualizer.getAvailableZones());
        layer.setPowerClipHandler(zone -> visualizer.applySmartPowerClip(layer, zone, false));
        layer.setInternalPowerClipHandler(target -> visualizer.applyInternalPowerClip(layer, target));
        layer.setEditContentHandler(() -> visualizer.enterEditMode(layer));
        layer.setPasteHandler(() -> {
            org.example.component.ShapeLayer pasted = org.example.component.ShapeLayer.getClipboardCopy();
            if (pasted != null) {
                if (visualizer.getPowerClipManager().isEditing()) {
                    addShapeLayer(pasted); // Handles insertion check
                } else {
                    pasted.setActiveZone(null);
                    addShapeLayer(pasted);
                }
            }
        });

        // Selection Request Handler - REMOVED: Managed centralization in
        // GarmentInputHandler
        // layer.setOnSelectionRequested(e -> {
        // if (e.isControlDown() || e.isShiftDown()) {
        // layerManager.toggleSelection(layer);
        // } else {
        // visualizer.selectLayer(layer);
        // }
        // });

        // Drag Handler (Group Move)
        layer.setOnDragHandler((dx, dy) -> {
            // Check locked state for group move
            boolean isSelfLocked = layer.isLocked();

            for (javafx.scene.Node node : layerManager.getSelectedNodes()) {
                // Prevent Double Movement: If 'node' is the Group containing 'layer', it is
                // ALREADY moved by ShapeLayer's internal logic.
                boolean isParentOfLayer = false;
                if (node instanceof javafx.scene.Group) {
                    javafx.scene.Node temp = layer;
                    while (temp != null) {
                        if (temp == node) {
                            isParentOfLayer = true;
                            break;
                        }
                        temp = temp.getParent();
                    }
                }

                if (node != layer && !isParentOfLayer) {
                    boolean isNodeLocked = false;
                    if (node instanceof org.example.component.ShapeLayer) {
                        isNodeLocked = ((org.example.component.ShapeLayer) node).isLocked();
                    } else if (node instanceof org.example.component.ImageLayer) {
                        isNodeLocked = ((org.example.component.ImageLayer) node).isLocked();
                    }

                    if (!isNodeLocked) {
                        node.setTranslateX(node.getTranslateX() + dx);
                        node.setTranslateY(node.getTranslateY() + dy);
                    }
                }
            }

            visualizer.updateOverlayForShapeDrag(layer);
        });

        layer.setOnDragReleased(() -> {
            visualizer.handleShapeDragRelease(layer);
        });

        layerManager.selectNode(layer);
        
        if (!animationsSuspended) {
            playAppearanceAnimation(layer);
        }
    }

    private void recordAddInHistory(javafx.scene.Node layer, String zone) {
        if (visualizer.getHistoryManager() != null && !layerManager.isPerformingHistoryAction()) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.LayerActionCommand(
                    layerManager, java.util.Collections.singletonList(layer),
                    org.example.pattern.LayerActionCommand.ActionType.ADD, zone));
        }
    }

    public void addShapeLayerToContainer(org.example.component.ShapeLayer layer, javafx.scene.Group container,
                                         int insertionIndex, boolean autoSelect) {
        if (container == null) {
            addShapeLayer(layer);
            return;
        }

        layerManager.addLayerToContainer(layer, container, autoSelect);
        if (insertionIndex >= 0 && layer.getParent() == container) {
            int currentIndex = container.getChildren().indexOf(layer);
            int boundedIndex = Math.min(insertionIndex, container.getChildren().size() - 1);
            if (currentIndex != -1 && currentIndex != boundedIndex) {
                container.getChildren().remove(layer);
                container.getChildren().add(boundedIndex, layer);
            }
        }

        configureShapeLayer(layer, false);

        // Sync PowerClip container item state if in a zone
        if (layer.getActiveZone() != null) {
            SmartZoneContainer zoneContainer = visualizer.getPowerClipManager().getContainer(layer.getActiveZone());
            if (zoneContainer != null) {
                zoneContainer.updateItemState(layer);
            }
        }

        if (autoSelect) {
            layerManager.selectNode(layer);
        }

        if (autoSelect && !animationsSuspended) {
            playAppearanceAnimation(layer);
        }
    }

    private void configureShapeLayer(org.example.component.ShapeLayer layer, boolean centerIfOrigin) {
        layer.setVisualizer(visualizer);

        if (centerIfOrigin && layer.getTranslateX() == 0 && layer.getTranslateY() == 0) {
            layer.setTranslateX(250);
            layer.setTranslateY(250);
        }

        // PowerClip Handler
        layer.setAvailableZonesSupplier(() -> visualizer.getAvailableZones());
        layer.setPowerClipHandler(zone -> visualizer.applySmartPowerClip(layer, zone, false));
        layer.setInternalPowerClipHandler(target -> visualizer.applyInternalPowerClip(layer, target));
        layer.setEditContentHandler(() -> visualizer.enterEditMode(layer));
        layer.setPasteHandler(() -> {
            org.example.component.ShapeLayer pasted = org.example.component.ShapeLayer.getClipboardCopy();
            if (pasted != null) {
                if (visualizer.getPowerClipManager().isEditing()) {
                    addShapeLayer(pasted);
                } else {
                    pasted.setActiveZone(null);
                    addShapeLayer(pasted);
                }
            }
        });

        layer.setOnDragHandler((dx, dy) -> {
            for (javafx.scene.Node node : layerManager.getSelectedNodes()) {
                boolean isParentOfLayer = false;
                if (node instanceof javafx.scene.Group) {
                    javafx.scene.Node temp = layer;
                    while (temp != null) {
                        if (temp == node) {
                            isParentOfLayer = true;
                            break;
                        }
                        temp = temp.getParent();
                    }
                }

                if (node != layer && !isParentOfLayer) {
                    boolean isNodeLocked = false;
                    if (node instanceof org.example.component.ShapeLayer) {
                        isNodeLocked = ((org.example.component.ShapeLayer) node).isLocked();
                    } else if (node instanceof org.example.component.ImageLayer) {
                        isNodeLocked = ((org.example.component.ImageLayer) node).isLocked();
                    }

                    if (!isNodeLocked) {
                        node.setTranslateX(node.getTranslateX() + dx);
                        node.setTranslateY(node.getTranslateY() + dy);
                    }
                }
            }

            visualizer.updateOverlayForShapeDrag(layer);
        });

        layer.setOnDragReleased(() -> visualizer.handleShapeDragRelease(layer));
    }

    // --- Drag and Drop Support ---

    public void initDragDrop(javafx.scene.Node targetNode) {
        targetNode.setOnDragOver(event -> {
            boolean hasValidFile = false;
            if (event.getDragboard().hasFiles()) {
                for (java.io.File f : event.getDragboard().getFiles()) {
                    if (f.getName().toLowerCase().endsWith(".svg")) {
                        hasValidFile = true;
                        break;
                    }
                }
            }
            if (event.getGestureSource() != targetNode && (event.getDragboard().hasImage() || hasValidFile)) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        targetNode.setOnDragDropped(event -> {
            boolean success = false;
            
            // 1. Check for SVG Files
            if (event.getDragboard().hasFiles()) {
                for (java.io.File file : event.getDragboard().getFiles()) {
                    if (file.getName().toLowerCase().endsWith(".svg")) {
                        try {
                            java.util.List<org.example.utils.SVGUtils.ParsedSVGShape> shapes = org.example.utils.SVGUtils.parseComplexSvg(file);
                            if (!shapes.isEmpty()) {
                                java.util.List<Node> newLayers = new java.util.ArrayList<>();
                                for (org.example.utils.SVGUtils.ParsedSVGShape ps : shapes) {
                                    String fillColor = ps.fill;
                                    String strokeColor = ps.stroke;
                                    String strokeW = ps.strokeWidth;
                                    String transformStr = ps.transform;
                                    String fillRuleStr = (ps.fillRule != null && !ps.fillRule.isEmpty()) ? ps.fillRule : "evenodd";

                                    boolean hasNoFill = fillColor == null || fillColor.isEmpty() || fillColor.equals("none");
                                    boolean hasNoStroke = strokeColor == null || strokeColor.isEmpty() || strokeColor.equals("none") || strokeColor.equals("null");
                                    double sw = strokeW != null && !strokeW.equals("none") && !strokeW.isEmpty() && !strokeW.equals("null") ? Double.parseDouble(strokeW.replaceAll("[a-zA-Z]", "")) : 0.0;
                                    javafx.scene.paint.Color fill = org.example.utils.SVGUtils.getSafeColor(fillColor, javafx.scene.paint.Color.BLACK);
                                    if (hasNoFill && (hasNoStroke || sw <= 0)) {
                                        fill = javafx.scene.paint.Color.BLACK;
                                    }
                                    javafx.scene.paint.Color stroke = org.example.utils.SVGUtils.getSafeColor(strokeColor, javafx.scene.paint.Color.TRANSPARENT);

                                    // Build a proper normalized ShapeLayer from SVG data
                                    javafx.scene.shape.SVGPath tempSvg = new javafx.scene.shape.SVGPath();
                                    tempSvg.setContent(ps.pathData);
                                    if ("evenodd".equalsIgnoreCase(fillRuleStr)) {
                                        tempSvg.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
                                    }
                                    if (transformStr != null && !transformStr.isEmpty()) {
                                        org.example.utils.SVGUtils.applySVGTransform(tempSvg, transformStr);
                                    }
                                    javafx.scene.shape.Path fxPath = (javafx.scene.shape.Path) javafx.scene.shape.Shape.union(tempSvg, new javafx.scene.shape.Path());
                                    if ("evenodd".equalsIgnoreCase(fillRuleStr)) {
                                        fxPath.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
                                    }

                                    ShapeLayer sl = org.example.component.helper.VectorBooleanHelper.createShapeLayerFromPath(fxPath, fill, stroke, sw);
                                    if (sl == null) continue;
                                    addShapeLayer(sl);
                                    newLayers.add(sl);
                                }
                                
                                // Group them!
                                if (newLayers.size() > 1) {
                                    layerManager.clearSelection();
                                    newLayers.forEach(layerManager::addToSelection);
                                    layerManager.groupSelected();
                                    
                                    // Get the created group and move it to cursor position
                                    Node createdGroup = layerManager.getSelectedNode();
                                    if (createdGroup != null) {
                                        Point2D localP = createdGroup.getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
                                        // Center it roughly
                                        createdGroup.setTranslateX(localP.getX() - createdGroup.getBoundsInParent().getWidth() / 2);
                                        createdGroup.setTranslateY(localP.getY() - createdGroup.getBoundsInParent().getHeight() / 2);
                                    }
                                } else if (newLayers.size() == 1) {
                                    Node single = newLayers.get(0);
                                    Point2D localP = single.getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
                                    single.setTranslateX(localP.getX() - single.getBoundsInParent().getWidth() / 2);
                                    single.setTranslateY(localP.getY() - single.getBoundsInParent().getHeight() / 2);
                                    layerManager.selectNode(single);
                                }
                                success = true;
                            }
                        } catch (Exception ex) { 
                            ex.printStackTrace(); 
                        }
                    }
                }
            }

            // 2. Check for Standard Image
            if (!success && event.getDragboard().hasImage()) {
                ImageLayer il = new ImageLayer(event.getDragboard().getImage());

                // PARSE METADATA (Shield / Logo)
                if (event.getDragboard().hasString()) {
                    String metadata = event.getDragboard().getString();
                    if (metadata != null && metadata.startsWith("BADGE:")) {
                        String[] parts = metadata.split(":");
                        if (parts.length >= 2) {
                            try {
                                il.setBadgeType(org.example.model.TipoEscudo.valueOf(parts[1]));
                            } catch (Exception e) {
                                il.setBadgeType(org.example.model.TipoEscudo.BORDADO);
                            }
                        }
                    } else {
                        il.setBadgeType(org.example.model.TipoEscudo.NINGUNO);
                    }
                } else {
                    il.setBadgeType(org.example.model.TipoEscudo.NINGUNO);
                }

                addImageLayer(il);
                
                // Position at mouse
                Point2D localP = il.getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
                il.setTranslateX(localP.getX() - il.getBoundsInParent().getWidth() / 2);
                il.setTranslateY(localP.getY() - il.getBoundsInParent().getHeight() / 2);
                
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    public void removeLayer(javafx.scene.Node node) {
        layerManager.removeLayer(node);
    }

    private void clearActiveZoneRecursively(javafx.scene.Node node) {
        if (node == null) return;
        node.setMouseTransparent(false);
        node.setOpacity(1.0);
        if (node instanceof org.example.component.ShapeLayer) {
            org.example.component.ShapeLayer sl = (org.example.component.ShapeLayer) node;
            sl.setActiveZone(null);
            sl.setSystemLocked(false);
            sl.setIsBeingEdited(false);
        } else if (node instanceof org.example.component.ImageLayer) {
            org.example.component.ImageLayer il = (org.example.component.ImageLayer) node;
            il.setActiveZone(null);
            il.setSystemLocked(false);
            il.setIsBeingEdited(false);
        } else if (node instanceof org.example.component.TextLayer) {
            org.example.component.TextLayer tl = (org.example.component.TextLayer) node;
            tl.setActiveZone(null);
            tl.setSystemLocked(false);
            tl.setBeingEdited(false);
        } else if (node instanceof org.example.component.GroupLayer) {
            org.example.component.GroupLayer gl = (org.example.component.GroupLayer) node;
            gl.setActiveZone(null);
            gl.setSystemLocked(false);
            gl.setIsBeingEdited(false);
            for (javafx.scene.Node child : gl.getUserLayers()) {
                clearActiveZoneRecursively(child);
            }
        } else if (node instanceof org.example.component.GroupLayerV2) {
            org.example.component.GroupLayerV2 glv2 = (org.example.component.GroupLayerV2) node;
            glv2.setActiveZone(null);
            glv2.setSystemLocked(false);
            for (javafx.scene.Node child : glv2.getUserLayers()) {
                clearActiveZoneRecursively(child);
            }
        } else if (node instanceof javafx.scene.Group) {
            for (javafx.scene.Node child : ((javafx.scene.Group) node).getChildren()) {
                clearActiveZoneRecursively(child);
            }
        }
    }

    private void playAppearanceAnimation(Node node) {
        if (node == null) return;

        double origOpacity = node.getOpacity();

        // Start transparent and fade in
        node.setOpacity(0.0);

        FadeTransition ft = new FadeTransition(Duration.millis(200), node);
        ft.setFromValue(0.0);
        ft.setToValue(origOpacity);
        ft.setInterpolator(Interpolator.EASE_OUT);

        ft.setOnFinished(e -> {
            node.setOpacity(origOpacity);
        });
        ft.play();
    }
}
