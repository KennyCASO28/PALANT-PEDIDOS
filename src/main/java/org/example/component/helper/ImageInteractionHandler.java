package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import org.example.component.ImageLayer;
import org.example.component.UserLayerManager;
import org.example.model.ImageLayerState;
import org.example.pattern.ICommand;
import org.example.pattern.NodeMemento;
import org.example.pattern.PropertyChangeCommand;
import org.example.pattern.TransformCommand;
import org.example.utils.GeometryUtility;
import org.example.utils.UIFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Handles all mouse interactions for ImageLayer.
 * Decouples Drag, Resize, Rotate, Shear and Pivot events.
 */
public class ImageInteractionHandler {

    private final ImageLayer layer;
    private final ImageLayerState state;
    private final OverlayNodes overlay;

    // Drag State
    private final double[] dragDelta = new double[2];
    private double lastSceneX, lastSceneY;
    private boolean isDrawing = false;
    private NodeMemento dragStartMemento; // Full snapshot for undo (includes parent+index)

    // Multi-drag support
    private boolean isMultiDrag = false;
    private List<Node> multiDragNodes = new ArrayList<>();
    private Map<Node, NodeMemento> multiDragMementos = new HashMap<>();
    private Point2D multiDragReferencePoint;

    public ImageInteractionHandler(ImageLayer layer, ImageLayerState state) {
        this.layer = layer;
        this.state = state;
        this.overlay = createOverlayNodes();
    }

    private boolean isEffectivelyLocked() {
        return layer.isUserLocked() || (layer.isLocked() && !layer.isBeingEdited()) || layer.isGrouped();
    }

    public void init() {
        initDragEvents();
        setupResizeEvents();
        setupRotationHandler();
        setupShearHandlers();
        setupPivotHandler();
        setupInteractionModes();
    }

    public OverlayNodes getOverlay() {
        return overlay;
    }

    private void initDragEvents() {
        layer.setOnMouseReleased(e -> {
            layer.setCursor(Cursor.DEFAULT);
            if (layer.isSelected() && !isEffectivelyLocked()) {
                overlay.handlesGroup.setVisible(true);
            }
            e.consume();
        });

        final boolean[] dropCopy = { false };
        @SuppressWarnings("unchecked")
        final javafx.event.EventHandler<javafx.scene.input.MouseEvent>[] rightClickFilter = new javafx.event.EventHandler[1];

        layer.setOnMousePressed(e -> {
            boolean isLeft = e.getButton() == javafx.scene.input.MouseButton.PRIMARY;
            boolean isRight = e.getButton() == javafx.scene.input.MouseButton.SECONDARY;
            if (isLeft || isRight) {
                if (isEffectivelyLocked())
                    return;

                dropCopy[0] = isRight; // right-click = copy mode
                rightClickFilter[0] = ev -> {
                    if (ev.getButton() == javafx.scene.input.MouseButton.SECONDARY
                            && ev.getEventType() == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
                        if (!dropCopy[0]) {
                            dropCopy[0] = true;
                            if (layer.getVisualizer() != null)
                                layer.getVisualizer().setCursor(Cursor.CROSSHAIR);
                        }
                        ev.consume();
                    }
                };
                if (layer.getScene() != null) {
                    layer.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
                }

                // Check Drawing Tools
                DrawingToolContext ctx = layer.getVisualizer() != null ? layer.getVisualizer().getDrawingToolContext()
                        : null;
                if (ctx != null && (ctx.isBrushActive() || ctx.isEraserActive())) {
                    isDrawing = true;
                    lastSceneX = e.getSceneX();
                    lastSceneY = e.getSceneY();
                    layer.recordUndoStateContent(ctx.isBrushActive() ? "Pintar" : "Borrar");
                    e.consume();
                    return;
                }

                if (e.getClickCount() == 2) {
                    toggleRotationMode();
                    e.consume();
                    return;
                }

                layer.setCursor(Cursor.CLOSED_HAND);
                if (!layer.isBeingEdited() && !state.isCropMode) {
                    overlay.handlesGroup.setVisible(false);
                }

                // CHECK MULTI-DRAG
                isMultiDrag = false;
                multiDragNodes.clear();
                multiDragMementos.clear();

                UserLayerManager ulm = null;
                if (layer.getVisualizer() != null) {
                    ulm = layer.getVisualizer().getUserLayerManager();
                }

                if (ulm != null && ulm.getSelectedNodes().size() > 1) {
                    java.util.Set<Node> selectedSet = ulm.getSelectedNodes();
                    Node thisTarget = findTopMostUserGroup(layer);

                    boolean allSameParent = true;
                    Node commonParent = thisTarget.getParent();
                    for (Node n : selectedSet) {
                        Node nTarget = findTopMostUserGroup(n);
                        if (nTarget.getParent() != commonParent) {
                            allSameParent = false;
                            break;
                        }
                    }

                    if (allSameParent && selectedSet.contains(thisTarget)) {
                        isMultiDrag = true;
                        multiDragNodes.addAll(selectedSet);
                        multiDragReferencePoint = commonParent.sceneToLocal(e.getSceneX(), e.getSceneY());

                        for (Node n : multiDragNodes) {
                            multiDragMementos.put(n, new NodeMemento(n));
                        }

                        e.consume();
                        return;
                    }
                }

                // SINGLE DRAG MODE
                Node target = findTopMostUserGroup(layer);
                if (target.getParent() == null)
                    return;

                Point2D p = target.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                dragDelta[0] = p.getX() - target.getTranslateX();
                dragDelta[1] = p.getY() - target.getTranslateY();

                dragStartMemento = new NodeMemento(target);
                e.consume();
            }
        });

        layer.setOnMouseDragged(e -> {
            if ((!e.isPrimaryButtonDown() && !e.isSecondaryButtonDown()) || state.isCropMode || isEffectivelyLocked())
                return;

            DrawingToolContext ctx = layer.getVisualizer() != null ? layer.getVisualizer().getDrawingToolContext()
                    : null;
            if (isDrawing && ctx != null) {
                double r = ctx.isBrushActive() ? ctx.getBrushSize() : ctx.getEraserSize();
                if (ctx.isBrushActive()) {
                    layer.editingService().paint(lastSceneX, lastSceneY, e.getSceneX(), e.getSceneY(), r,
                            ctx.getFillColor());
                } else {
                    layer.editingService().erase(lastSceneX, lastSceneY, e.getSceneX(), e.getSceneY(), r);
                }
                lastSceneX = e.getSceneX();
                lastSceneY = e.getSceneY();
                e.consume();
                return;
            }

            // MULTI-DRAG MODE
            if (isMultiDrag && !multiDragNodes.isEmpty() && multiDragReferencePoint != null) {
                Node firstNode = multiDragNodes.get(0);
                Node commonParent = findTopMostUserGroup(firstNode).getParent();
                if (commonParent == null)
                    return;

                Point2D currentPoint = commonParent.sceneToLocal(e.getSceneX(), e.getSceneY());
                double dx = currentPoint.getX() - multiDragReferencePoint.getX();
                double dy = currentPoint.getY() - multiDragReferencePoint.getY();
                if (e.isShiftDown()) {
                    if (Math.abs(dx) >= Math.abs(dy))
                        dy = 0;
                    else
                        dx = 0;
                }

                for (Node n : multiDragNodes) {
                    NodeMemento before = multiDragMementos.get(n);
                    if (before != null) {
                        n.setTranslateX(before.getTx() + dx);
                        n.setTranslateY(before.getTy() + dy);
                    }
                }

                e.consume();
                return;
            }

            // SINGLE DRAG MODE
            Node moveTarget = findTopMostUserGroup(layer);
            if (moveTarget.getParent() != null) {
                Point2D p = moveTarget.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                double newX = p.getX() - dragDelta[0];
                double newY = p.getY() - dragDelta[1];
                if (e.isShiftDown()) {
                    double dx = newX - moveTarget.getTranslateX();
                    double dy = newY - moveTarget.getTranslateY();
                    if (Math.abs(dx) >= Math.abs(dy))
                        newY = moveTarget.getTranslateY();
                    else
                        newX = moveTarget.getTranslateX();
                }

                moveTarget.setTranslateX(newX);
                moveTarget.setTranslateY(newY);

                e.consume();
            }
        });

        layer.setOnMouseReleased(e -> {
            if (isEffectivelyLocked())
                return;

            if (rightClickFilter[0] != null && layer.getScene() != null) {
                layer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
                rightClickFilter[0] = null;
            }

            if (isDrawing) {
                isDrawing = false;
                layer.editingService().commitAction(
                        layer.getVisualizer().getDrawingToolContext().isBrushActive() ? "Pintar" : "Borrar");
                e.consume();
                return;
            }

            layer.setCursor(Cursor.MOVE);
            if (layer.isSelected()) {
                overlay.handlesGroup.setVisible(true);
                layer.updateVisuals();
            }

            // MULTI-DRAG MODE
            if (isMultiDrag && !multiDragNodes.isEmpty() && layer.getVisualizer() != null) {
                for (Node n : multiDragNodes) {
                    NodeMemento before = multiDragMementos.get(n);
                    if (before != null) {
                        NodeMemento after = new NodeMemento(n);
                        if (before.getTx() != after.getTx() || before.getTy() != after.getTy()) {
                            String zone = null;
                            if (n instanceof org.example.component.GraphicLayer) {
                                zone = ((org.example.component.GraphicLayer) n).getActiveZone();
                            }
                            TransformCommand cmd = new TransformCommand(n, before, after, zone);
                            layer.getVisualizer().getHistoryManager().addCommand(cmd);
                            org.example.pattern.RepeatActionRecorder.recordTransform(before, after, false);
                        }
                    }
                }

                isMultiDrag = false;
                multiDragNodes.clear();
                multiDragMementos.clear();
                multiDragReferencePoint = null;
                e.consume();
                return;
            }

            // SINGLE DRAG MODE
            Node target = findTopMostUserGroup(layer);
            if (layer.getVisualizer() != null && dragStartMemento != null) {
                NodeMemento afterMemento = new NodeMemento(target);
                if (dropCopy[0]) {
                    layer.copyToClipboard();
                    Node clone = org.example.component.ImageLayer.getClipboardCopy();
                    if (clone != null) {
                        double targetX = layer.getTranslateX();
                        double targetY = layer.getTranslateY();
                        layer.getVisualizer().getLayerFactory().addUserLayer(clone);
                        clone.setTranslateX(targetX);
                        clone.setTranslateY(targetY);
                    }

                    org.example.pattern.RepeatActionRecorder.recordTransform(dragStartMemento, afterMemento, true);
                    dragStartMemento.restore();
                } else {
                    if (dragStartMemento.getTx() != afterMemento.getTx()
                            || dragStartMemento.getTy() != afterMemento.getTy()) {
                        layer.getVisualizer().getHistoryManager().addCommand(new TransformCommand(
                                target, dragStartMemento, afterMemento, layer.getActiveZone()));
                        org.example.pattern.RepeatActionRecorder.recordTransform(dragStartMemento, afterMemento, false);
                    }
                }
                dragStartMemento = null;
            }
        });
    }

    private void setupResizeEvents() {
        setupResizeHandler(overlay.topLeft, -1, -1);
        setupResizeHandler(overlay.topRight, 1, -1);
        setupResizeHandler(overlay.bottomLeft, -1, 1);
        setupResizeHandler(overlay.bottomRight, 1, 1);
        setupResizeHandler(overlay.top, 0, -1);
        setupResizeHandler(overlay.bottom, 0, 1);
        setupResizeHandler(overlay.left, -1, 0);
        setupResizeHandler(overlay.right, 1, 0);
    }

    private void setupResizeHandler(Node handle, double dW, double dH) {
        final double[] ctx = new double[14]; // [startW, startH, sceneX, sceneY, tx, ty, cropX, cropY, cropW, cropH,
                                             // startScaleX, startScaleY, dynamicDW, dynamicDH]
        final Point2D[] anchor = new Point2D[1];
        final Point2D[] anchorCenter = new Point2D[1];
        final NodeMemento[] startMemento = new NodeMemento[1];

        final boolean[] dropCopy = { false };
        @SuppressWarnings("unchecked")
        final javafx.event.EventHandler<javafx.scene.input.MouseEvent>[] rightClickFilter = new javafx.event.EventHandler[1];

        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked())
                return;

            dropCopy[0] = false;
            startMemento[0] = new NodeMemento(layer);

            rightClickFilter[0] = ev -> {
                if (ev.getButton() == javafx.scene.input.MouseButton.SECONDARY &&
                        ev.getEventType() == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
                    dropCopy[0] = true;
                    layer.setCursor(Cursor.CROSSHAIR);
                    ev.consume();
                }
            };
            if (layer.getScene() != null) {
                layer.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
            }

            ctx[0] = layer.getWidth();
            ctx[1] = layer.getHeight();
            ctx[4] = layer.getTranslateX();
            ctx[5] = layer.getTranslateY();
            ctx[6] = state.cropX;
            ctx[7] = state.cropY;
            ctx[8] = state.cropW;
            ctx[9] = state.cropH;
            ctx[10] = layer.getInternalScaleX();
            ctx[11] = layer.getInternalScaleY();

            // Use Parent Space (Visualizer) as a stable reference to avoid feedback loops
            Point2D parentMouse = layer.getVisualizer().getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());
            ctx[2] = parentMouse.getX();
            ctx[3] = parentMouse.getY();

            double dynamicDW = dW;
            double dynamicDH = dH;
            if (ctx[10] < 0)
                dynamicDW = -dynamicDW;
            if (ctx[11] < 0)
                dynamicDH = -dynamicDH;
            ctx[12] = dynamicDW;
            ctx[13] = dynamicDH;

            double anchorX = (dynamicDW == -1) ? ctx[0] : (dynamicDW == 1) ? 0 : ctx[0] / 2.0;
            double anchorY = (dynamicDH == -1) ? ctx[1] : (dynamicDH == 1) ? 0 : ctx[1] / 2.0;
            anchor[0] = layer.localToParent(layer.getContentGroup().localToParent(new Point2D(anchorX, anchorY)));

            double centerAnchorX = ctx[0] / 2.0;
            double centerAnchorY = ctx[1] / 2.0;
            anchorCenter[0] = layer
                    .localToParent(layer.getContentGroup().localToParent(new Point2D(centerAnchorX, centerAnchorY)));
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked())
                return;

            // 1. Get current mouse in stable Parent space
            Point2D parentMouse = layer.getVisualizer().getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());
            if (parentMouse == null)
                return;

            // 2. Linear delta in parent space
            double dxp = parentMouse.getX() - ctx[2];
            double dyp = parentMouse.getY() - ctx[3];

            // 3. Project delta onto the layer's unrotated internal axes (X and Y)
            double angleRad = Math.toRadians(-layer.getRotate());
            double dxL = dxp * Math.cos(angleRad) - dyp * Math.sin(angleRad);
            double dyL = dxp * Math.sin(angleRad) + dyp * Math.cos(angleRad);

            // 4. Adjust by current scale to avoid "aggressive" resizing
            double sX = ctx[10];
            double sY = ctx[11];
            double signX = Math.signum(sX);
            double signY = Math.signum(sY);
            if (signX == 0)
                signX = 1;
            if (signY == 0)
                signY = 1;

            dxL = (dxL / Math.abs(sX)) * signX;
            dyL = (dyL / Math.abs(sY)) * signY;

            // 5. Special logic for Crop mode vs Size mode
            if (state.isCropMode) {
                // In Crop Mode, use internal state deltas
                if (ctx[12] == -1) {
                    state.cropX = Math.max(0, Math.min(ctx[6] + ctx[8] - 5, ctx[6] + dxL));
                    state.cropW = ctx[8] - (state.cropX - ctx[6]);
                } else if (ctx[12] == 1) {
                    state.cropW = Math.max(5, Math.min(layer.getWidth() - ctx[6], ctx[8] + dxL));
                }

                if (ctx[13] == -1) {
                    state.cropY = Math.max(0, Math.min(ctx[7] + ctx[9] - 5, ctx[7] + dyL));
                    state.cropH = ctx[9] - (state.cropY - ctx[7]);
                } else if (ctx[13] == 1) {
                    state.cropH = Math.max(5, Math.min(layer.getHeight() - ctx[7], ctx[9] + dyL));
                }
                layer.updateVisuals();
                e.consume();
                return;
            }

            // --- PREMIUM RESIZING USING InteractionMath ---
            InteractionMath.ResizeContext rCtx = new InteractionMath.ResizeContext();
            rCtx.startW = ctx[0];
            rCtx.startH = ctx[1];
            rCtx.startScaleX = ctx[10];
            rCtx.startScaleY = ctx[11];
            rCtx.startRotation = layer.getRotate(); // Or internal rotation
            rCtx.localDx = dxp;
            rCtx.localDy = dyp;
            rCtx.dW = (int) ctx[12];
            rCtx.dH = (int) ctx[13];
            rCtx.isShiftDown = e.isShiftDown();
            rCtx.isControlDown = e.isControlDown();
            rCtx.aspect = ctx[0] / ctx[1];
            rCtx.minW = 5;
            rCtx.minH = 5;

            InteractionMath.ResizeResult res = InteractionMath.calculateResize(rCtx);

            // layer.setSize(res.newW, res.newH); // Removed to prevent double-scaling and
            // broken clones
            layer.setInternalScaleX(res.newScaleX);
            layer.setInternalScaleY(res.newScaleY);

            // ANCHOR STABILIZATION
            double anchorLocalX;
            double anchorLocalY;
            if (res.useCenterAnchor) {
                anchorLocalX = ctx[0] / 2.0;
                anchorLocalY = ctx[1] / 2.0;
            } else {
                anchorLocalX = (ctx[12] == -1) ? ctx[0] : (ctx[12] == 1) ? 0 : ctx[0] / 2.0;
                anchorLocalY = (ctx[13] == -1) ? ctx[1] : (ctx[13] == 1) ? 0 : ctx[1] / 2.0;
            }

            Point2D newAnchorWorld = layer
                    .localToParent(layer.getContentGroup().localToParent(new Point2D(anchorLocalX, anchorLocalY)));

            if (res.useCenterAnchor && anchorCenter[0] != null) {
                layer.setTranslateX(layer.getTranslateX() + (anchorCenter[0].getX() - newAnchorWorld.getX()));
                layer.setTranslateY(layer.getTranslateY() + (anchorCenter[0].getY() - newAnchorWorld.getY()));
            } else if (anchor[0] != null) {
                layer.setTranslateX(layer.getTranslateX() + (anchor[0].getX() - newAnchorWorld.getX()));
                layer.setTranslateY(layer.getTranslateY() + (anchor[0].getY() - newAnchorWorld.getY()));
            }
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            System.out.println("DEBUG IMAGE: handle released! dropCopy=" + dropCopy[0]);
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY)
                return;

            if (rightClickFilter[0] != null && layer.getScene() != null) {
                layer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
                rightClickFilter[0] = null;
            }
            layer.setCursor(Cursor.DEFAULT);

            if (dropCopy[0] && layer.getVisualizer() != null) {
                layer.copyToClipboard();
                Node clone = org.example.component.ImageLayer.getClipboardCopy();
                if (clone != null) {
                    double targetX = layer.getTranslateX();
                    double targetY = layer.getTranslateY();
                    layer.getVisualizer().getLayerFactory().addUserLayer(clone);
                    clone.setTranslateX(targetX);
                    clone.setTranslateY(targetY);

                    System.out.println("DEBUG IMAGE: Clone created! tx=" + targetX + ", scaleX="
                            + ((org.example.component.ImageLayer) clone).getInternalScaleX() + ", width="
                            + ((org.example.component.ImageLayer) clone).getLogicalWidth());
                }

                if (startMemento[0] != null) {
                    org.example.pattern.RepeatActionRecorder.recordTransform(startMemento[0], new NodeMemento(layer),
                            true);
                    startMemento[0].restore();
                }
            } else {
                if (ctx[0] > 0) {
                    recordTransformUndo(startMemento[0]);

                    double newW = layer.getWidth();
                    double newH = layer.getHeight();
                    org.example.pattern.RepeatActionRecorder.recordTransform(startMemento[0], new NodeMemento(layer),
                            false);
                }
            }

            startMemento[0] = null;
            ctx[0] = 0;
            dropCopy[0] = false;
            e.consume();
        });
    }

    private void setupRotationHandler() {
        final double[] ctx = new double[3]; // [startAngle, startMouseAngle, initialRotate]
        final NodeMemento[] startMemento = new NodeMemento[1];

        Consumer<MouseEvent> startRotation = e -> {
            if (isEffectivelyLocked())
                return;
            startMemento[0] = new NodeMemento(layer);
            double px = layer.getRotateTransform().getPivotX();
            double py = layer.getRotateTransform().getPivotY();
            Point2D pivotParent = layer.localToParent(px, py);
            Point2D mouseParent = layer.getVisualizer().getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());

            ctx[1] = Math.toDegrees(
                    Math.atan2(mouseParent.getY() - pivotParent.getY(), mouseParent.getX() - pivotParent.getX()));
            ctx[2] = layer.getRotateTransform().getAngle();
            ctx[0] = layer.getRotateTransform().getAngle();
            e.consume();
        };

        Consumer<MouseEvent> doRotation = e -> {
            if (isEffectivelyLocked() || !e.isPrimaryButtonDown())
                return;

            double px = layer.getRotateTransform().getPivotX();
            double py = layer.getRotateTransform().getPivotY();
            Point2D pivotParent = layer.localToParent(px, py);
            Point2D mouseParent = layer.getVisualizer().getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());

            double currAngle = Math.toDegrees(
                    Math.atan2(mouseParent.getY() - pivotParent.getY(), mouseParent.getX() - pivotParent.getX()));
            double delta = currAngle - ctx[1];

            double newAngle = ctx[2] + delta;
            if (e.isShiftDown())
                newAngle = Math.round(newAngle / 45.0) * 45.0;

            layer.getRotateTransform().setAngle(newAngle);
            if (layer.getVisualizer() != null && layer.getVisualizer().getShapeManagerController() != null) {
                layer.getVisualizer().getShapeManagerController().updateAngleUI(newAngle);
            }
            e.consume();
        };

        Node[] rotHandles = { overlay.rotTopLeft, overlay.rotTopRight, overlay.rotBottomLeft, overlay.rotBottomRight };
        for (Node h : rotHandles) {
            h.setOnMousePressed(startRotation::accept);
            h.setOnMouseDragged(doRotation::accept);
            h.setOnMouseReleased(e -> {
                recordTransformUndo(startMemento[0]);
                if (startMemento[0] != null) {
                    NodeMemento after = new NodeMemento(layer);
                    org.example.pattern.RepeatActionRecorder.recordTransform(startMemento[0], after, false);
                }
                startMemento[0] = null;
                e.consume();
            });
        }
    }

    private void setupShearHandlers() {
        final double[] ctx = new double[4]; // [startX, startY, initSX, initSY]
        final NodeMemento[] startMemento = new NodeMemento[1];

        Node[] hHandles = { overlay.shearTop, overlay.shearBottom };
        for (Node h : hHandles) {
            h.setOnMousePressed(e -> {
                if (isEffectivelyLocked())
                    return;
                startMemento[0] = new NodeMemento(layer);
                ctx[0] = e.getSceneX();
                ctx[2] = layer.getShearTransform().getX();
                e.consume();
            });
            h.setOnMouseDragged(e -> {
                if (isEffectivelyLocked())
                    return;
                double delta = (e.getSceneX() - ctx[0]) / 100.0;
                layer.getShearTransform().setX(ctx[2] + delta);
                layer.updateVisuals();
                e.consume();
            });
            h.setOnMouseReleased(e -> {
                recordTransformUndo(startMemento[0]);
                startMemento[0] = null;
                e.consume();
            });
        }

        Node[] vHandles = { overlay.shearLeft, overlay.shearRight };
        for (Node h : vHandles) {
            h.setOnMousePressed(e -> {
                if (isEffectivelyLocked())
                    return;
                startMemento[0] = new NodeMemento(layer);
                ctx[1] = e.getSceneY();
                ctx[3] = layer.getShearTransform().getY();
                e.consume();
            });
            h.setOnMouseDragged(e -> {
                if (isEffectivelyLocked())
                    return;
                double delta = (e.getSceneY() - ctx[1]) / 100.0;
                layer.getShearTransform().setY(ctx[3] + delta);
                layer.updateVisuals();
                e.consume();
            });
            h.setOnMouseReleased(e -> {
                recordTransformUndo(startMemento[0]);
                startMemento[0] = null;
                e.consume();
            });
        }
    }

    private void setupPivotHandler() {
        final NodeMemento[] startMemento = new NodeMemento[1];
        overlay.pivotHandle.setOnMousePressed(e -> {
            if (isEffectivelyLocked())
                return;
            startMemento[0] = new NodeMemento(layer);
            e.consume();
        });
        overlay.pivotHandle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked())
                return;
            double oldPx = layer.getRotateTransform().getPivotX();
            double oldPy = layer.getRotateTransform().getPivotY();

            Point2D p = layer.sceneToLocal(e.getSceneX(), e.getSceneY());
            Point2D centerLocal = new Point2D(layer.getWidth() / 2.0, layer.getHeight() / 2.0);
            Point2D centerScene = layer.localToScene(centerLocal);
            Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());

            double SNAP_RADIUS_SCENE = 10.0; // 10 physical screen pixels
            double distScene = pScene.distance(centerScene);

            if (distScene <= SNAP_RADIUS_SCENE) {
                layer.setCustomPivot(-1, -1);
                overlay.pivotHandle.setOpacity(0.6);
            } else {
                layer.setCustomPivot(p.getX(), p.getY());
                overlay.pivotHandle.setOpacity(1.0);
            }
            layer.updateVisuals();

            // POSITIONAL COMPENSATION
            compensatePivotChange(oldPx, oldPy);
            e.consume();
        });
        overlay.pivotHandle.setOnMouseReleased(e -> {
            recordTransformUndo(startMemento[0]);
            startMemento[0] = null;
            e.consume();
        });

        overlay.pivotHandle.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                NodeMemento before = new NodeMemento(layer);
                double oldPx = layer.getRotateTransform().getPivotX();
                double oldPy = layer.getRotateTransform().getPivotY();
                layer.setCustomPivot(-1, -1);
                overlay.pivotHandle.setOpacity(1.0);
                layer.updateVisuals();
                compensatePivotChange(oldPx, oldPy);
                recordTransformUndo(before);
                e.consume();
            }
        });
    }

    private void compensatePivotChange(double oldPx, double oldPy) {
        double newPx = layer.getRotateTransform().getPivotX();
        double newPy = layer.getRotateTransform().getPivotY();
        double dx = newPx - oldPx;
        double dy = newPy - oldPy;
        double angle = layer.getRotateTransform().getAngle();
        if (angle != 0 && (dx != 0 || dy != 0)) {
            double rad = Math.toRadians(angle);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double rx = dx * cos - dy * sin;
            double ry = dx * sin + dy * cos;
            layer.setTranslateX(layer.getTranslateX() + (rx - dx));
            layer.setTranslateY(layer.getTranslateY() + (ry - dy));
        }
    }

    private void setupInteractionModes() {
        // Double-click is already handled in initDragEvents (setOnMousePressed)
    }

    private void toggleRotationMode() {
        state.isRotationMode = !state.isRotationMode;
        layer.updateVisuals();
    }

    private Node findTopMostUserGroup(Node startNode) {
        Node target = startNode;
        Node temp = startNode.getParent();
        while (temp != null) {
            if ("USER_GROUP".equals(temp.getId()))
                target = temp;
            temp = temp.getParent();
        }
        return target;
    }

    private void recordResizeUndo(double oldW, double oldH, double oldTx, double oldTy, double newW, double newH,
            double newTx, double newTy) {
        if (layer.getVisualizer() == null || layer.getVisualizer().getHistoryManager() == null)
            return;
        layer.getVisualizer().getHistoryManager().addCommand(new TransformCommand(layer,
                oldTx, oldTy, layer.getScaleX(), layer.getScaleY(), layer.getRotate(), oldW, oldH, null, null,
                newTx, newTy, layer.getScaleX(), layer.getScaleY(), layer.getRotate(), newW, newH, null, null,
                layer.getActiveZone()));
    }

    private void recordPropertyUndo(String name, double oldV, double newV, Consumer<Double> setter) {
        if (layer.getVisualizer() != null && layer.getVisualizer().getHistoryManager() != null) {
            layer.getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(name, oldV, newV, setter));
        }
    }

    private void recordTransformUndo(NodeMemento before) {
        if (before == null || layer.getVisualizer() == null || layer.getVisualizer().getHistoryManager() == null)
            return;

        NodeMemento after = new NodeMemento(layer);
        if (!before.isEquivalentTo(after)) {
            layer.getVisualizer().getHistoryManager().addCommand(
                    new TransformCommand(layer, before, after, layer.getActiveZone()));
        }
    }

    // --- Overlay UI Creation ---

    private OverlayNodes createOverlayNodes() {
        Rectangle border = new Rectangle();
        border.setStroke(Color.web("#0047AB"));
        border.setStrokeWidth(1.5);
        border.setFill(null);
        border.setMouseTransparent(true);
        border.setVisible(false);

        Rectangle cropOverlay = new Rectangle();
        cropOverlay.setFill(Color.rgb(255, 165, 0, 0.3));
        cropOverlay.setStroke(Color.ORANGE);
        cropOverlay.getStrokeDashArray().addAll(5d, 5d);
        cropOverlay.setMouseTransparent(true);
        cropOverlay.setVisible(false);

        StackPane topLeft = createHandle(Cursor.NW_RESIZE);
        StackPane topRight = createHandle(Cursor.NE_RESIZE);
        StackPane bottomLeft = createHandle(Cursor.SW_RESIZE);
        StackPane bottomRight = createHandle(Cursor.SE_RESIZE);
        StackPane top = createMidHandle(Cursor.N_RESIZE);
        StackPane right = createMidHandle(Cursor.E_RESIZE);
        StackPane bottom = createMidHandle(Cursor.S_RESIZE);
        StackPane left = createMidHandle(Cursor.W_RESIZE);

        StackPane rotTopLeft = createRotHandle();
        StackPane rotTopRight = createRotHandle();
        StackPane rotBottomLeft = createRotHandle();
        StackPane rotBottomRight = createRotHandle();

        StackPane shearTop = createShearHandle(Cursor.H_RESIZE, true);
        StackPane shearBottom = createShearHandle(Cursor.H_RESIZE, true);
        StackPane shearLeft = createShearHandle(Cursor.V_RESIZE, false);
        StackPane shearRight = createShearHandle(Cursor.V_RESIZE, false);

        Group pivotHandle = UIFactory.crearPivotHandle();

        Group handlesGroup = new Group();
        handlesGroup.getChildren().addAll(cropOverlay, topLeft, topRight, bottomLeft, bottomRight, top, right, bottom,
                left,
                rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight,
                shearTop, shearBottom, shearLeft, shearRight, pivotHandle);
        handlesGroup.setManaged(false);
        handlesGroup.setVisible(false);

        return new OverlayNodes(border, cropOverlay, handlesGroup, topLeft, topRight, bottomLeft, bottomRight, top,
                right, bottom, left,
                rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight,
                shearTop, shearBottom, shearLeft, shearRight, pivotHandle);
    }

    private StackPane createHandle(Cursor c) {
        StackPane h = UIFactory.crearSquareHandle(null, 4, "#0047AB", "#ffffff", c);
        h.getStyleClass().add("resize-handle");
        return h;
    }

    private StackPane createMidHandle(Cursor c) {
        StackPane h = UIFactory.crearSquareHandle(null, 4, "#0047AB", "#ffffff", c);
        h.getStyleClass().add("resize-handle");
        return h;
    }

    private StackPane createRotHandle() {
        return UIFactory.crearIconHandle("mdi2r-rotate-right", 16, "#e8a020", Cursor.OPEN_HAND);
    }

    private StackPane createShearHandle(Cursor c, boolean h) {
        return UIFactory.crearIconHandle(h ? "mdi2a-arrow-left-right" : "mdi2a-arrow-up-down", 16, "#16a085", c);
    }

    public static class OverlayNodes {
        public final Rectangle border, cropOverlay;
        public final Group handlesGroup;
        public final StackPane topLeft, topRight, bottomLeft, bottomRight;
        public final StackPane top, right, bottom, left;
        public final StackPane rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight;
        public final StackPane shearTop, shearBottom, shearLeft, shearRight;
        public final Group pivotHandle;

        public OverlayNodes(Rectangle border, Rectangle cropOverlay, Group handlesGroup, StackPane topLeft,
                StackPane topRight, StackPane bottomLeft, StackPane bottomRight,
                StackPane top, StackPane right, StackPane bottom, StackPane left,
                StackPane rotTopLeft, StackPane rotTopRight, StackPane rotBottomLeft, StackPane rotBottomRight,
                StackPane shearTop, StackPane shearBottom, StackPane shearLeft, StackPane shearRight,
                Group pivotHandle) {
            this.border = border;
            this.cropOverlay = cropOverlay;
            this.handlesGroup = handlesGroup;
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomLeft = bottomLeft;
            this.bottomRight = bottomRight;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.left = left;
            this.rotTopLeft = rotTopLeft;
            this.rotTopRight = rotTopRight;
            this.rotBottomLeft = rotBottomLeft;
            this.rotBottomRight = rotBottomRight;
            this.shearTop = shearTop;
            this.shearBottom = shearBottom;
            this.shearLeft = shearLeft;
            this.shearRight = shearRight;
            this.pivotHandle = pivotHandle;
        }
    }

    private double normalizeAngle(double angle) {
        while (angle > 180)
            angle -= 360;
        while (angle <= -180)
            angle += 360;
        return angle;
    }
}
