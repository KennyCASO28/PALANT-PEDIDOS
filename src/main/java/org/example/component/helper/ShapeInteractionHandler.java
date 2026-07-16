package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.example.component.ShapeLayer;
import org.example.component.UserLayerManager;
import org.example.model.BezierNode;
import org.example.model.ShapeType;
import org.example.pattern.NodeMemento;
import org.example.pattern.TransformCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Handles all mouse interactions for ShapeLayer.
 * Decouples Drag, Resize, Rotate, Shear and Arc events from the main layer.
 */
public class ShapeInteractionHandler {

    private final ShapeLayer layer;
    private final ShapeSelectionOverlaySupport.OverlayNodes overlay;

    // Drag State
    private final double[] dragDelta = new double[2];
    private org.example.pattern.NodeMemento dragStartMemento; // Full snapshot for undo (includes parent+index)

    // Multi-drag support
    private boolean isMultiDrag = false;
    private List<Node> multiDragNodes = new ArrayList<>();
    private Map<Node, NodeMemento> multiDragMementos = new HashMap<>();
    private Point2D multiDragReferencePoint;

    public ShapeInteractionHandler(ShapeLayer layer, ShapeSelectionOverlaySupport.OverlayNodes overlay) {
        this.layer = layer;
        this.overlay = overlay;
    }

    public void init() {
        initDragEvents();
        initResizeEvents();
        initRotateEvents();
        initShearEvents();
        initArcEvents();
    }

    private boolean isEffectivelyLocked() {
        return layer.isUserLocked() || (layer.isLocked() && !layer.isBeingEdited()) || layer.isGrouped();
    }

    private void initDragEvents() {
        layer.setOnMouseEntered(e -> {
            if (layer.isGrouped()) {
                layer.setCursor(Cursor.DEFAULT);
            } else if (!layer.isLocked()) {
                layer.setCursor(Cursor.MOVE);
            }
        });

        final boolean[] dropCopy = { false };
        @SuppressWarnings("unchecked")
        final javafx.event.EventHandler<javafx.scene.input.MouseEvent>[] rightClickFilter = new javafx.event.EventHandler[1];

    }

    private void advanceEditMode() {
        if (!layer.isRotationMode() && !layer.isArcEditingMode() && !layer.isNodeEditing()) {
            // Currently Resize -> Go to Rotate
            layer.setRotationMode(true);
            layer.setArcEditingMode(false);
            layer.setIsNodeEditing(false);
        } else if (layer.isRotationMode()) {
            // Currently Rotate -> Go to Edit
            layer.setRotationMode(false);
            if (layer.getType() == ShapeType.RECTANGLE) {
                layer.setArcEditingMode(true);
            } else if (layer.getType() == ShapeType.CUSTOM_PATH) {
                layer.setIsNodeEditing(true);
            }
        } else {
            // Currently Edit -> Go to Resize
            layer.setRotationMode(false);
            layer.setArcEditingMode(false);
            layer.setIsNodeEditing(false);
        }
        layer.refreshShapeVisuals();
    }

    private Node findTopMostUserGroup(Node startNode) {
        Node target = startNode;
        Node temp = startNode.getParent();
        while (temp != null) {
            if ("USER_GROUP".equals(temp.getId())) {
                target = temp;
            }
            temp = temp.getParent();
        }
        return target;
    }

    // --- Resize Logic ---
    private void initResizeEvents() {
        setupResizeHandler(overlay.topLeft, -1, -1);
        setupResizeHandler(overlay.topRight, 1, -1);
        setupResizeHandler(overlay.bottomLeft, -1, 1);
        setupResizeHandler(overlay.bottomRight, 1, 1);
        setupResizeHandler(overlay.topCenter, 0, -1);
        setupResizeHandler(overlay.bottomCenter, 0, 1);
        setupResizeHandler(overlay.leftCenter, -1, 0);
        setupResizeHandler(overlay.rightCenter, 1, 0);
    }

    private void setupResizeHandler(Node handle, double dW, double dH) {
        final double[] ctx = new double[10]; // [startW, startH, sceneX, sceneY, tx, ty, mx, startScaleX, startScaleY,
                                             // startRotation]
        final Point2D[] anchor = new Point2D[1];
        final Point2D[] anchorCenter = new Point2D[1];
        final boolean[] dropCopy = { false };
        final NodeMemento[] startMemento = new NodeMemento[1];
        @SuppressWarnings("unchecked")
        final javafx.event.EventHandler<javafx.scene.input.MouseEvent>[] rightClickFilter = new javafx.event.EventHandler[1];

        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked())
                return;
            dropCopy[0] = false;
            startMemento[0] = new NodeMemento(layer);

            // Add global right-click detector
            rightClickFilter[0] = ev -> {
                if (ev.getButton() == javafx.scene.input.MouseButton.SECONDARY
                        && ev.getEventType() == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
                    if (!dropCopy[0]) {
                        dropCopy[0] = true;
                        layer.getVisualizer().setCursor(Cursor.CROSSHAIR);
                    }
                    ev.consume();
                }
            };
            if (layer.getScene() != null) {
                layer.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
            }

            ctx[0] = layer.getLogicalWidth();
            ctx[1] = layer.getLogicalHeight();

            // Use parent space coordinates to account for workspace zoom
            Node refNode = (layer.getVisualizer() != null && layer.getVisualizer().getContentGroup() != null)
                    ? layer.getVisualizer().getContentGroup()
                    : layer.getParent();
            if (refNode == null)
                refNode = layer;
            Point2D parentMouse = refNode.sceneToLocal(e.getSceneX(), e.getSceneY());
            ctx[2] = parentMouse.getX();
            ctx[3] = parentMouse.getY();

            ctx[4] = layer.getTranslateX();
            ctx[5] = layer.getTranslateY();
            ctx[6] = layer.getVisualMinX();

            ctx[7] = layer.getInternalScaleX();
            ctx[8] = layer.getInternalScaleY();
            ctx[9] = layer.getInternalRotation();

            double my = layer.getVisualMinY();

            double anchorX = (dW == -1) ? (layer.getVisualMinX() + ctx[0])
                    : (dW == 1) ? layer.getVisualMinX() : (layer.getVisualMinX() + ctx[0] / 2.0);
            double anchorY = (dH == -1) ? (my + ctx[1]) : (dH == 1) ? my : (my + ctx[1] / 2.0);
            anchor[0] = layer.localToParent(layer.getContentGroup().localToParent(new Point2D(anchorX, anchorY)));

            double centerAnchorX = layer.getVisualMinX() + ctx[0] / 2.0;
            double centerAnchorY = layer.getVisualMinY() + ctx[1] / 2.0;
            anchorCenter[0] = layer
                    .localToParent(layer.getContentGroup().localToParent(new Point2D(centerAnchorX, centerAnchorY)));

            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked())
                return;

            Node refNode = (layer.getVisualizer() != null && layer.getVisualizer().getContentGroup() != null)
                    ? layer.getVisualizer().getContentGroup()
                    : layer.getParent();
            if (refNode == null)
                refNode = layer;
            Point2D parentMouse = refNode.sceneToLocal(e.getSceneX(), e.getSceneY());
            InteractionMath.ResizeContext rCtx = new InteractionMath.ResizeContext();
            rCtx.startW = ctx[0];
            rCtx.startH = ctx[1];
            rCtx.startScaleX = ctx[7];
            rCtx.startScaleY = ctx[8];
            rCtx.startRotation = ctx[9];
            rCtx.localDx = (parentMouse.getX() - ctx[2]);
            rCtx.localDy = (parentMouse.getY() - ctx[3]);
            rCtx.dW = dW;
            rCtx.dH = dH;
            rCtx.isShiftDown = e.isShiftDown();
            rCtx.isControlDown = e.isControlDown();
            rCtx.aspect = ctx[0] / ctx[1];

            InteractionMath.ResizeResult res = InteractionMath.calculateResize(rCtx);

            layer.setSize(res.newW, res.newH);
            layer.setInternalRotation(ctx[9]); // ensure rotation stays
            layer.setInternalScaleX(res.newScaleX);
            layer.setInternalScaleY(res.newScaleY);

            // ANCHOR CALCULATION
            double anchorLocalX, anchorLocalY;
            if (res.useCenterAnchor) {
                // Symmetric scaling: anchor is always the center of the starting shape
                anchorLocalX = layer.getVisualMinX() + res.newW / 2.0;
                anchorLocalY = layer.getVisualMinY() + res.newH / 2.0;
            } else {
                anchorLocalX = (dW == -1) ? (layer.getVisualMinX() + res.newW)
                        : (dW == 1) ? layer.getVisualMinX() : (layer.getVisualMinX() + res.newW / 2.0);
                anchorLocalY = (dH == -1) ? (layer.getVisualMinY() + res.newH)
                        : (dH == 1) ? layer.getVisualMinY() : (layer.getVisualMinY() + res.newH / 2.0);
            }

            Point2D currentAnchor = layer
                    .localToParent(layer.getContentGroup().localToParent(new Point2D(anchorLocalX, anchorLocalY)));

            if (anchor[0] != null && currentAnchor != null) {
                if (res.useCenterAnchor && anchorCenter[0] != null) {
                    layer.setTranslateX(layer.getTranslateX() + (anchorCenter[0].getX() - currentAnchor.getX()));
                    layer.setTranslateY(layer.getTranslateY() + (anchorCenter[0].getY() - currentAnchor.getY()));
                } else {
                    layer.setTranslateX(layer.getTranslateX() + (anchor[0].getX() - currentAnchor.getX()));
                    layer.setTranslateY(layer.getTranslateY() + (anchor[0].getY() - currentAnchor.getY()));
                }
            }
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            System.out.println("DEBUG SHAPE: handle released! dropCopy=" + dropCopy[0]);
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY)
                return;
            if (rightClickFilter[0] != null && layer.getScene() != null) {
                layer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
                rightClickFilter[0] = null;
            }
            if (ctx[0] > 0) {
                double newW = layer.getLogicalWidth();
                double newH = layer.getLogicalHeight();
                double startW = ctx[0];
                double startH = ctx[1];
                double signX = Math.signum(layer.getInternalScaleX() / (ctx[7] == 0 ? 1 : ctx[7]));
                double signY = Math.signum(layer.getInternalScaleY() / (ctx[8] == 0 ? 1 : ctx[8]));
                double ratioX = (newW / startW) * signX;
                double ratioY = (newH / startH) * signY;

                if (dropCopy[0]) {
                    // 1. Create a copy of the CURRENT layer (with mirrored/scaled settings)
                    ShapeClipboardSupport.copy(layer);
                    ShapeLayer clone = ShapeClipboardSupport.getClipboardCopy();
                    if (clone != null) {
                        double targetX = layer.getTranslateX();
                        double targetY = layer.getTranslateY();
                        layer.getVisualizer().addShapeLayer(clone);
                        if (layer.getVisualizer().getPowerClipManager() != null
                                && layer.getVisualizer().getPowerClipManager().isEditing()) {
                            layer.getVisualizer().applySmartPowerClip(clone,
                                    layer.getVisualizer().getPowerClipManager().getCurrentEditingZone(), false);
                        }
                        clone.setTranslateX(targetX);
                        clone.setTranslateY(targetY);
                        System.out.println("DEBUG SHAPE: Clone created! tx=" + targetX + ", scaleX="
                                + clone.getInternalScaleX() + ", width=" + clone.getLogicalWidth());
                    }

                    org.example.pattern.RepeatActionRecorder.recordTransform(startMemento[0], new NodeMemento(layer),
                            true);

                    // 2. Revert the ORIGINAL layer to its starting state
                    if (startMemento[0] != null) {
                        startMemento[0].restore();
                    } else {
                        layer.setSize(ctx[0], ctx[1]);
                        layer.setInternalScaleX(ctx[7]);
                        layer.setInternalScaleY(ctx[8]);
                        layer.setTranslateX(ctx[4]);
                        layer.setTranslateY(ctx[5]);
                    }
                    layer.getVisualizer().setCursor(Cursor.DEFAULT);
                } else {
                    if (layer.getVisualizer() != null && layer.getVisualizer().getHistoryManager() != null
                            && startMemento[0] != null) {
                        NodeMemento after = new NodeMemento(layer);
                        layer.getVisualizer().getHistoryManager()
                                .addCommand(new TransformCommand(layer, startMemento[0], after, layer.getActiveZone()));
                        org.example.pattern.RepeatActionRecorder.recordTransform(startMemento[0], after, false);
                    }
                }
                ctx[0] = 0;
            }
            e.consume();
        });
    }

    private void initRotateEvents() {
        setupRotateHandler(overlay.rotTopLeft);
        setupRotateHandler(overlay.rotTopRight);
        setupRotateHandler(overlay.rotBottomLeft);
        setupRotateHandler(overlay.rotBottomRight);

        final NodeMemento[] startPivotMemento = new NodeMemento[1];

        overlay.pivotHandle.setOnMousePressed(e -> {
            if (isEffectivelyLocked())
                return;
            startPivotMemento[0] = new NodeMemento(layer);
            e.consume(); // Prevent fall-through to layer drag which hides handles
        });

        overlay.pivotHandle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked())
                return;
            Point2D local = layer.sceneToLocal(e.getSceneX(), e.getSceneY());
            layer.updatePivot(local.getX(), local.getY());
            e.consume();
        });

        overlay.pivotHandle.setOnMouseReleased(e -> {
            if (isEffectivelyLocked())
                return;
            // Reset visual pop effect if any
            overlay.pivotHandle.setScaleX(1.0);
            overlay.pivotHandle.setScaleY(1.0);
            if (startPivotMemento[0] != null) {
                NodeMemento after = new NodeMemento(layer);
                layer.getVisualizer().getHistoryManager()
                        .addCommand(new TransformCommand(layer, startPivotMemento[0], after, layer.getActiveZone()));
                org.example.pattern.RepeatActionRecorder.recordTransform(startPivotMemento[0], after, false);
                startPivotMemento[0] = null;
            }
            e.consume();
        });
    }

    private void setupRotateHandler(Node handle) {
        final double[] startMouseAngle = new double[1];
        final double[] startRotation = new double[1];
        final Point2D[] center = new Point2D[1];
        final NodeMemento[] memento = new NodeMemento[1];

        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked())
                return;
            memento[0] = new NodeMemento(layer);
            center[0] = layer.localToScene(layer.getInternalPivotX(), layer.getInternalPivotY());
            startMouseAngle[0] = Math
                    .toDegrees(Math.atan2(e.getSceneY() - center[0].getY(), e.getSceneX() - center[0].getX()));
            startRotation[0] = layer.getInternalRotation();
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked())
                return;
            double currentMouseAngle = Math
                    .toDegrees(Math.atan2(e.getSceneY() - center[0].getY(), e.getSceneX() - center[0].getX()));

            double deltaAngle = currentMouseAngle - startMouseAngle[0];
            while (deltaAngle > 180)
                deltaAngle -= 360;
            while (deltaAngle <= -180)
                deltaAngle += 360;

            double newAngle = startRotation[0] + deltaAngle * 0.5; // Reduced sensitivity

            if (e.isShiftDown()) {
                newAngle = Math.round(newAngle / 15.0) * 15.0;
            }

            layer.setInternalRotation(newAngle);
            if (layer.getVisualizer() != null && layer.getVisualizer().getShapeManagerController() != null) {
                layer.getVisualizer().getShapeManagerController().updateAngleUI(newAngle);
            }
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isEffectivelyLocked())
                return;
            if (layer.getVisualizer() != null && memento[0] != null) {
                NodeMemento after = new NodeMemento(layer);
                TransformCommand cmd = new TransformCommand(layer, memento[0], after, layer.getActiveZone());
                layer.getVisualizer().getHistoryManager().addCommand(cmd);

                org.example.pattern.RepeatActionRecorder.recordTransform(memento[0], after, false);
            }
            e.consume();
        });
    }

    private void initShearEvents() {
        setupShearHandler(overlay.shearTop, true, true);
        setupShearHandler(overlay.shearBottom, true, false);
        setupShearHandler(overlay.shearLeft, false, true);
        setupShearHandler(overlay.shearRight, false, false);
    }

    private void setupShearHandler(Node handle, boolean horizontal, boolean invert) {
        final double[] start = new double[6]; // sceneX, sceneY, shearX, shearY, anchorParentX, anchorParentY
        final NodeMemento[] memento = new NodeMemento[1];

        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked())
                return;
            layer.convertPrimitiveToPath();
            memento[0] = new NodeMemento(layer);
            start[0] = e.getSceneX();
            start[1] = e.getSceneY();
            start[2] = layer.getInternalShearX();
            start[3] = layer.getInternalShearY();
            layer.setShearing(true);

            // Determine Anchor in Local coordinates
            double logW = Math.max(1.0, layer.getLogicalWidth());
            double logH = Math.max(1.0, layer.getLogicalHeight());
            double localAnchorX = 0;
            double localAnchorY = 0;

            if (horizontal) {
                localAnchorY = invert ? (logH / 2) : (-logH / 2);
            } else {
                localAnchorX = invert ? (logW / 2) : (-logW / 2);
            }

            // Get anchor in Parent space
            javafx.geometry.Point2D anchorParent = layer
                    .localToParent(new javafx.geometry.Point2D(localAnchorX, localAnchorY));
            start[4] = anchorParent.getX();
            start[5] = anchorParent.getY();

            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked())
                return;
            double dx = e.getSceneX() - start[0];
            double dy = e.getSceneY() - start[1];

            double angle = Math.toRadians(layer.getInternalRotation());
            double rotDx = dx * Math.cos(angle) + dy * Math.sin(angle);
            double rotDy = -dx * Math.sin(angle) + dy * Math.cos(angle);

            double logW = Math.max(1.0, layer.getLogicalWidth());
            double logH = Math.max(1.0, layer.getLogicalHeight());
            double amount = horizontal ? (rotDx / logH) : (rotDy / logW);
            amount *= 1.2; // Increased sensitivity for smooth shearing

            if (invert)
                amount = -amount;

            if (horizontal)
                layer.setInternalShearX(start[2] + amount);
            else
                layer.setInternalShearY(start[3] + amount);

            // Ocultar borde después del shear (evita ver el margen estirarse)
            layer.setBorderVisible(false);

            // Fix Anchor Position
            double localAnchorX = 0;
            double localAnchorY = 0;
            if (horizontal) {
                localAnchorY = invert ? (logH / 2) : (-logH / 2);
            } else {
                localAnchorX = invert ? (logW / 2) : (-logW / 2);
            }

            javafx.geometry.Point2D currentAnchorParent = layer
                    .localToParent(new javafx.geometry.Point2D(localAnchorX, localAnchorY));

            double parentDx = start[4] - currentAnchorParent.getX();
            double parentDy = start[5] - currentAnchorParent.getY();

            layer.setTranslateX(layer.getTranslateX() + parentDx);
            layer.setTranslateY(layer.getTranslateY() + parentDy);

            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isEffectivelyLocked())
                return;
            if (layer.getVisualizer() != null && memento[0] != null) {
                TransformCommand cmd = new TransformCommand(layer, memento[0], new NodeMemento(layer),
                        layer.getActiveZone());
                layer.getVisualizer().getHistoryManager().addCommand(cmd);
            }
            layer.setShearing(false);
            layer.invalidateBounds();
            layer.updateVisuals();
            layer.setBorderVisible(true);
            e.consume();
        });
    }

    private void initArcEvents() {
        setupArcHandler(overlay.arcTopLeft, 1, 1);
        setupArcHandler(overlay.arcTopRight, -1, 1);
        setupArcHandler(overlay.arcBottomLeft, 1, -1);
        setupArcHandler(overlay.arcBottomRight, -1, -1);
    }

    private void setupArcHandler(Node handle, double dX, double dY) {
        final NodeMemento[] memento = new NodeMemento[1];
        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked())
                return;
            memento[0] = new NodeMemento(layer);
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked())
                return;
            Point2D local = layer.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (local == null)
                return;

            // Calculate distance to the closest corner to allow dragging from stacked
            // center in any direction
            double clampedX = Math.max(layer.getVisualMinX(),
                    Math.min(local.getX(), layer.getVisualMinX() + layer.getLogicalWidth()));
            double clampedY = Math.max(layer.getVisualMinY(),
                    Math.min(local.getY(), layer.getVisualMinY() + layer.getLogicalHeight()));

            double minDx = Math.min(clampedX - layer.getVisualMinX(),
                    (layer.getVisualMinX() + layer.getLogicalWidth()) - clampedX);
            double minDy = Math.min(clampedY - layer.getVisualMinY(),
                    (layer.getVisualMinY() + layer.getLogicalHeight()) - clampedY);

            double rawRadius = Math.max(minDx, minDy);

            // TACTILE MAGNET: Snap to 0 (sharp corner) with hysteresis
            double newArc;
            if (rawRadius < 12) {
                newArc = 0;
            } else {
                newArc = rawRadius * 2.0;
            }

            // CLAMPING: Prevent arc from exceeding geometry
            double maxArc = Math.min(layer.getLogicalWidth(), layer.getLogicalHeight());

            // Circle magnet at the end
            if (Math.abs(newArc - maxArc) < 15) {
                newArc = maxArc;
            }

            newArc = Math.max(0, Math.min(newArc, maxArc));

            layer.setArcWidth(newArc);
            layer.setArcHeight(newArc);
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isEffectivelyLocked())
                return;
            if (layer.getVisualizer() != null && memento[0] != null) {
                layer.getVisualizer().getHistoryManager().addCommand(
                        new TransformCommand(layer, memento[0], new NodeMemento(layer), layer.getActiveZone()));
            }
            e.consume();
        });
    }

    private double normalizeAngle(double angle) {
        while (angle > 180)
            angle -= 360;
        while (angle <= -180)
            angle += 360;
        return angle;
    }
}
