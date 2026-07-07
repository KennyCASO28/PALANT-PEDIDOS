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
import org.example.model.ImageLayerState;
import org.example.pattern.ICommand;
import org.example.pattern.NodeMemento;
import org.example.pattern.PropertyChangeCommand;
import org.example.pattern.TransformCommand;
import org.example.utils.GeometryUtility;
import org.example.utils.UIFactory;

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

    public ImageInteractionHandler(ImageLayer layer, ImageLayerState state) {
        this.layer = layer;
        this.state = state;
        this.overlay = createOverlayNodes();
    }

    private boolean isEffectivelyLocked() {
        return layer.isUserLocked() || (layer.isLocked() && !layer.isBeingEdited());
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

        layer.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                if (isEffectivelyLocked())
                    return;

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

                Node target = findTopMostUserGroup(layer);
                if (target.getParent() == null)
                    return;

                Point2D p = target.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                dragDelta[0] = p.getX() - target.getTranslateX();
                dragDelta[1] = p.getY() - target.getTranslateY();

                // CRITICAL FIX: Capture full snapshot (parent, index, transforms)
                dragStartMemento = new NodeMemento(target);
                e.consume();
            }
        });

        layer.setOnMouseDragged(e -> {
            if (!e.isPrimaryButtonDown() || state.isCropMode || isEffectivelyLocked())
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

            Node moveTarget = findTopMostUserGroup(layer);
            if (moveTarget.getParent() != null) {
                Point2D p = moveTarget.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                double newX = p.getX() - dragDelta[0];
                double newY = p.getY() - dragDelta[1];

                moveTarget.setTranslateX(newX);
                moveTarget.setTranslateY(newY);

                e.consume();
            }
        });

        layer.setOnMouseReleased(e -> {
            if (isEffectivelyLocked())
                return;

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

            Node target = findTopMostUserGroup(layer);
            // CRITICAL FIX: Use full NodeMemento (captured at mousePressed)
            if (layer.getVisualizer() != null && dragStartMemento != null) {
                NodeMemento afterMemento = new NodeMemento(target);
                if (dragStartMemento.getTx() != afterMemento.getTx()
                        || dragStartMemento.getTy() != afterMemento.getTy()) {
                    layer.getVisualizer().getHistoryManager().addCommand(new TransformCommand(
                            target, dragStartMemento, afterMemento, layer.getActiveZone()));
                }
                dragStartMemento = null; // Clear for next drag
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
        final double[] ctx = new double[12]; // [startW, startH, sceneX, sceneY, tx, ty, cropX, cropY, cropW, cropH, startScaleX, startScaleY]
        final Point2D[] anchor = new Point2D[1];
        final NodeMemento[] startMemento = new NodeMemento[1];

        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked())
                return;
            startMemento[0] = new NodeMemento(layer);
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

            double anchorX = (dW == -1) ? ctx[0] : (dW == 1) ? 0 : ctx[0] / 2.0;
            double anchorY = (dH == -1) ? ctx[1] : (dH == 1) ? 0 : ctx[1] / 2.0;
            anchor[0] = layer.localToParent(anchorX, anchorY);
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
            dxL /= (sX != 0 ? sX : 1);
            dyL /= (sY != 0 ? sY : 1);

            if (state.isCropMode) {
                // In Crop Mode, use internal state deltas
                if (dW == -1) {
                    state.cropX = Math.max(0, Math.min(ctx[6] + ctx[8] - 5, ctx[6] + dxL));
                    state.cropW = ctx[8] - (state.cropX - ctx[6]);
                } else if (dW == 1) {
                    state.cropW = Math.max(5, Math.min(layer.getWidth() - ctx[6], ctx[8] + dxL));
                }

                if (dH == -1) {
                    state.cropY = Math.max(0, Math.min(ctx[7] + ctx[9] - 5, ctx[7] + dyL));
                    state.cropH = ctx[9] - (state.cropY - ctx[7]);
                } else if (dH == 1) {
                    state.cropH = Math.max(5, Math.min(layer.getHeight() - ctx[7], ctx[9] + dyL));
                }
                layer.updateVisuals();
                e.consume();
                return;
            }

// --- PREMIUM RESIZING ---
            double proposedW = ctx[0] + dxL * dW;
            double proposedH = ctx[1] + dyL * dH;

            boolean flipX = proposedW < 0;
            boolean flipY = proposedH < 0;

            double newW = Math.max(5, Math.abs(proposedW));
            double newH = Math.max(5, Math.abs(proposedH));

            boolean isCornerHandle = (dW != 0 && dH != 0);
            boolean isLeftHandle = (dW == -1);
            boolean isRightHandle = (dW == 1);
            boolean isTopHandle = (dH == -1);
            boolean isBottomHandle = (dH == 1);

            // CORNERS: proportional by default, Shift cancels
            boolean proportional = isCornerHandle && !e.isShiftDown();

            if (proportional) {
                double ratio = ctx[0] / ctx[1];
                if (newW / newH > ratio)
                    newW = newH * ratio;
                else
                    newH = newW / ratio;
            }

            layer.setSize(newW, newH);

            // REAL FLIP: Mirror in SCREEN coordinates
            // For horizontal mirror (flip along Y axis in screen): newRot = -oldRot
            // For vertical mirror (flip along X axis in screen): newRot = 180 - oldRot
            double currentRotation = layer.getRotate();
            double newScaleX = ctx[10];
            double newScaleY = ctx[11];

            // Check if we crossed zero (flipping from positive to negative scale means flip occurred)
            boolean crossedFlipX = (ctx[10] > 0 && flipX) || (ctx[10] < 0 && !flipX);
            boolean crossedFlipY = (ctx[11] > 0 && flipY) || (ctx[11] < 0 && !flipY);

            // Apply true mirror flip using rotation transformation
            if (crossedFlipX && isLeftHandle) {
                // LATERAL FLIP (left handle crossed center): this is horizontal mirror
                double newRot = -currentRotation;
                layer.getRotateTransform().setAngle(newRot);
                newScaleX = Math.abs(ctx[10]);
            } else if (crossedFlipX && isRightHandle) {
                // LATERAL FLIP (right handle crossed center): this is horizontal mirror
                double newRot = -currentRotation;
                layer.getRotateTransform().setAngle(newRot);
                newScaleX = Math.abs(ctx[10]);
            } else if (crossedFlipY && isTopHandle) {
                // VERTICAL FLIP (top handle crossed center): this is vertical mirror
                double newRot = 180 + currentRotation;
                layer.getRotateTransform().setAngle(normalizeAngle(newRot));
                newScaleY = Math.abs(ctx[11]);
            } else if (crossedFlipY && isBottomHandle) {
                // VERTICAL FLIP (bottom handle crossed center): this is vertical mirror
                double newRot = 180 + currentRotation;
                layer.getRotateTransform().setAngle(normalizeAngle(newRot));
                newScaleY = Math.abs(ctx[11]);
            } else if (crossedFlipX || crossedFlipY) {
                // Corner handle flip - apply both flips
                double newRot = currentRotation;
                if (crossedFlipX) newRot = -newRot;
                if (crossedFlipY) newRot = 180 + newRot;
                layer.getRotateTransform().setAngle(normalizeAngle(newRot));
                if (crossedFlipX) newScaleX = Math.abs(ctx[10]);
                if (crossedFlipY) newScaleY = Math.abs(ctx[11]);
            }

            layer.setInternalScaleX(newScaleX);
            layer.setInternalScaleY(newScaleY);

            // ANCHOR STABILIZATION: Keep the opposite corner/edge fixed
            double anchorLocalX = 0;
            double anchorLocalY = 0;

            if (dW == -1) {
                anchorLocalX = newW;
            } else if (dW == 1) {
                anchorLocalX = 0;
            } else {
                anchorLocalX = newW / 2.0;
            }

            if (dH == -1) {
                anchorLocalY = newH;
            } else if (dH == 1) {
                anchorLocalY = 0;
            } else {
                anchorLocalY = newH / 2.0;
            }

            Point2D newAnchorWorld = layer.localToParent(anchorLocalX, anchorLocalY);

            if (anchor[0] != null) {
                layer.setTranslateX(layer.getTranslateX() + (anchor[0].getX() - newAnchorWorld.getX()));
                layer.setTranslateY(layer.getTranslateY() + (anchor[0].getY() - newAnchorWorld.getY()));
            }
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (ctx[0] > 0) {
                recordTransformUndo(startMemento[0]);
                startMemento[0] = null;
                ctx[0] = 0;
            }
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
            double SNAP_RADIUS = 15.0;
            double centerX = layer.getWidth() / 2.0;
            double centerY = layer.getHeight() / 2.0;
            double dist = p.distance(centerX, centerY);

            if (dist <= SNAP_RADIUS) {
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
        border.setStrokeWidth(1);
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
        while (angle > 180) angle -= 360;
        while (angle <= -180) angle += 360;
        return angle;
    }
}
