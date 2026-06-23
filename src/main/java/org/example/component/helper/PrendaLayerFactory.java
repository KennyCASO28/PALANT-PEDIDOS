package org.example.component.helper;

import javafx.scene.input.TransferMode;
import org.example.component.ImageLayer;
import org.example.component.TextLayer;
import org.example.component.UserLayerManager;
import org.example.component.PrendaVisualizer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
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
                org.example.component.helper.SmartZoneContainer container = visualizer.getPowerClipManager()
                        .getContainer(zone);
                if (container != null) {
                    layerManager.addLayerToContainer(node, container.getContentGroup(), true);
                } else {
                    layerManager.addLayer(node);
                }
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
        
        // Smart Insertion for Shape
        boolean insertedInPowerClip = false;
        if (visualizer.getPowerClipManager().isEditing()) {
            String zone = visualizer.getPowerClipManager().getCurrentEditingZone();
            visualizer.getPowerClipManager().addToContainer(layer, zone, true);
            recordAddInHistory(layer, zone);
            insertedInPowerClip = true;
        } else {
            if (layer.getTranslateX() == 0 && layer.getTranslateY() == 0) {
                layer.setTranslateX(250);
                layer.setTranslateY(250);
            }
            layerManager.addLayer(layer);
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
            if (event.getGestureSource() != targetNode && event.getDragboard().hasImage()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        targetNode.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasImage()) {
                ImageLayer il = new ImageLayer(event.getDragboard().getImage());

                // PARSE METADATA (Shield / Logo)
                if (event.getDragboard().hasString()) {
                    String metadata = event.getDragboard().getString();
                    if (metadata != null && metadata.startsWith("BADGE:")) {
                        // Format: "BADGE:TYPE:ZONE"
                        String[] parts = metadata.split(":");
                        if (parts.length >= 2) {
                            try {
                                il.setBadgeType(org.example.model.TipoEscudo.valueOf(parts[1]));
                            } catch (Exception e) {
                                // Fallback to BORDADO if type invalid
                                il.setBadgeType(org.example.model.TipoEscudo.BORDADO);
                            }
                        }
                    } else {
                        // If it's not explicitly a BADGE from the gallery, it's a regular logo
                        il.setBadgeType(org.example.model.TipoEscudo.NINGUNO);
                    }
                } else {
                    il.setBadgeType(org.example.model.TipoEscudo.NINGUNO);
                }

                // Detection for auto-powerclip on drop
                String zone = visualizer.getShapeHelper().detectZone(event.getSceneX(), event.getSceneY());
                if (zone != null) {
                    // DELETED: Automatic PowerClip on Drop.
                    // User wants it to stay floating ("quede afuera").
                }
                addImageLayer(il);
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

        double origScaleX = node.getScaleX();
        double origScaleY = node.getScaleY();
        double origOpacity = node.getOpacity();

        // Start collapsed and transparent
        node.setScaleX(origScaleX * 0.4);
        node.setScaleY(origScaleY * 0.4);
        node.setOpacity(0.0);

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

        FadeTransition ft = new FadeTransition(Duration.millis(140), node);
        ft.setFromValue(0.0);
        ft.setToValue(origOpacity);
        ft.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(scaleSeq, ft);
        pt.setOnFinished(e -> {
            node.setScaleX(origScaleX);
            node.setScaleY(origScaleY);
            node.setOpacity(origOpacity);
        });
        pt.play();
    }
}
