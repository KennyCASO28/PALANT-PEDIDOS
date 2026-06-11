package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.example.component.ShapeLayer;
import org.example.model.BezierNode;
import org.example.model.ShapeType;
import org.example.pattern.NodeMemento;
import org.example.pattern.TransformCommand;

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
        return layer.isUserLocked() || (layer.isLocked() && !layer.isBeingEdited());
    }

    private void initDragEvents() {
        layer.setOnMouseEntered(e -> {
            if (layer.isGrouped()) {
                layer.setCursor(Cursor.DEFAULT);
            } else if (!layer.isLocked()) {
                layer.setCursor(Cursor.MOVE);
            }
        });

        layer.setOnMousePressed(e -> {
            if (layer.isGrouped()) return;

            if (e.isPrimaryButtonDown()) {
                if (isEffectivelyLocked() || layer.isNodeEditing())
                    return;

                if (e.getClickCount() >= 2) {
                    advanceEditMode();
                    e.consume();
                    return;
                }

                layer.getShapeGroup().setCursor(Cursor.CLOSED_HAND);
                layer.hideHandlesGroup();

                Node target = findTopMostUserGroup(layer);
                if (target.getParent() == null) return;

                Point2D p = target.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                dragDelta[0] = p.getX() - target.getTranslateX();
                dragDelta[1] = p.getY() - target.getTranslateY();

                // CRITICAL FIX: Capture full snapshot (parent, index, transforms) instead of just transform values.
                // This prevents the node from 'disappearing' if the parent changes during the drag.
                dragStartMemento = new org.example.pattern.NodeMemento(target);

                if (layer.getOnSelectionRequested() != null) {
                    layer.getOnSelectionRequested().accept(e);
                }
                e.consume();
            }
        });

        layer.setOnMouseDragged(e -> {
            if (layer.isGrouped()) return;
            if (!e.isPrimaryButtonDown() || isEffectivelyLocked() || layer.isNodeEditing())
                return;

            Node moveTarget = findTopMostUserGroup(layer);
            if (moveTarget.getParent() != null) {
                Point2D p = moveTarget.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                double newX = p.getX() - dragDelta[0];
                double newY = p.getY() - dragDelta[1];

                double dx = newX - moveTarget.getTranslateX();
                double dy = newY - moveTarget.getTranslateY();

                moveTarget.setTranslateX(newX);
                moveTarget.setTranslateY(newY);

                if (layer.getOnDragHandler() != null)
                    layer.getOnDragHandler().accept(dx, dy);
                e.consume();
            }
        });

        layer.setOnMouseReleased(e -> {
            if (layer.isGrouped() || isEffectivelyLocked()) return;
            
            layer.setCursor(Cursor.MOVE);
            if (layer.isSelected()) {
                layer.showHandlesGroup();
                layer.refreshShapeVisuals();
            }

            Node target = findTopMostUserGroup(layer);
            boolean isInsideGroup = (target != layer);
            
            // FIX: When a ShapeLayer is inside a GroupLayerV2, its drag/move events are handled
            // by the group, not by the individual layer. Recording a TransformCommand here using
            // the layer's individual undo coordinates vs the group's translate coordinates would
            // mix coordinate spaces (local vs container) leading to invalid undo state and
            // "jumping" behavior on undo/redo. Skip history for grouped layers; Group handles its own.
            if (isInsideGroup) {
                return; // Group handles its own history
            }
            
            // CRITICAL FIX: Use full NodeMemento (captured at mousePressed) instead of individual
            // undoStart values. This properly restores parent, index AND transforms - preventing
            // the "disappearing layer" bug during undo.
            if (layer.getVisualizer() != null && dragStartMemento != null) {
                NodeMemento afterMemento = new NodeMemento(target);
                // Only add command if something actually changed
                if (dragStartMemento.getTx() != afterMemento.getTx() || dragStartMemento.getTy() != afterMemento.getTy()) {
                    TransformCommand cmd = new TransformCommand(target, dragStartMemento, afterMemento, layer.getActiveZone());
                    layer.getVisualizer().getHistoryManager().addCommand(cmd);
                }
                dragStartMemento = null; // Clear for next drag
            }
        });
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
        final double[] ctx = new double[9]; // [startW, startH, sceneX, sceneY, tx, ty, mx, startScaleX, startScaleY]
        final Point2D[] anchor = new Point2D[1];
        final boolean[] dropCopy = {false};
        final NodeMemento[] startMemento = new NodeMemento[1];
        @SuppressWarnings("unchecked")
        final javafx.event.EventHandler<javafx.scene.input.MouseEvent>[] rightClickFilter = new javafx.event.EventHandler[1];

        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked()) return;
            dropCopy[0] = false;
            startMemento[0] = new NodeMemento(layer);
            
            // Add global right-click detector
            rightClickFilter[0] = ev -> {
                if (ev.getButton() == javafx.scene.input.MouseButton.SECONDARY && ev.getEventType() == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
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
            Node refNode = (layer.getVisualizer() != null && layer.getVisualizer().getContentGroup() != null) ? layer.getVisualizer().getContentGroup() : layer.getParent();
            if (refNode == null) refNode = layer;
            Point2D parentMouse = refNode.sceneToLocal(e.getSceneX(), e.getSceneY());
            ctx[2] = parentMouse.getX();
            ctx[3] = parentMouse.getY();
            
            ctx[4] = layer.getTranslateX();
            ctx[5] = layer.getTranslateY();
            ctx[6] = layer.getVisualMinX();
            
            ctx[7] = layer.getInternalScaleX();
            ctx[8] = layer.getInternalScaleY();

            double my = layer.getVisualMinY();

            double anchorX = (dW == -1) ? (layer.getVisualMinX() + ctx[0]) : (dW == 1) ? layer.getVisualMinX() : (layer.getVisualMinX() + ctx[0] / 2.0);
            double anchorY = (dH == -1) ? (my + ctx[1]) : (dH == 1) ? my : (my + ctx[1] / 2.0);
            anchor[0] = layer.localToParent(new Point2D(anchorX, anchorY));
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked()) return;
            
            Node refNode = (layer.getVisualizer() != null && layer.getVisualizer().getContentGroup() != null) ? layer.getVisualizer().getContentGroup() : layer.getParent();
            if (refNode == null) refNode = layer;
            Point2D parentMouse = refNode.sceneToLocal(e.getSceneX(), e.getSceneY());
            double localDx = (parentMouse.getX() - ctx[2]);
            double localDy = (parentMouse.getY() - ctx[3]);

            double angleRad = Math.toRadians(layer.getInternalRotation());
            double cos = Math.cos(angleRad);
            double sin = Math.sin(angleRad);
            
            // Standard projection for screen space (Y-down)
            double unrotDx = localDx * cos + localDy * sin;
            double unrotDy = -localDx * sin + localDy * cos;

            double sX = ctx[7]; // use starting scale
            double sY = ctx[8];
            unrotDx /= (sX != 0 ? Math.abs(sX) : 1);
            unrotDy /= (sY != 0 ? Math.abs(sY) : 1);

            double proposedW = ctx[0] + unrotDx * dW;
            double proposedH = ctx[1] + unrotDy * dH;

            // Handle Ctrl modifier for perfect mirror / snaps
            if (e.isControlDown()) {
                if (dW != 0 && Math.abs(proposedW) > 0) {
                    long m = Math.round(proposedW / ctx[0]);
                    if (m == 0) m = (proposedW < 0) ? -1 : 1;
                    proposedW = m * ctx[0];
                }
                if (dH != 0 && Math.abs(proposedH) > 0) {
                    long m = Math.round(proposedH / ctx[1]);
                    if (m == 0) m = (proposedH < 0) ? -1 : 1;
                    proposedH = m * ctx[1];
                }
            }

            boolean flipX = proposedW < 0;
            boolean flipY = proposedH < 0;

            double newW = Math.max(2, Math.abs(proposedW));
            double newH = Math.max(2, Math.abs(proposedH));

            layer.setSize(newW, newH);
            layer.setInternalScaleX(ctx[7] * (flipX ? -1 : 1));
            layer.setInternalScaleY(ctx[8] * (flipY ? -1 : 1));

            double anchorX = (dW == -1) ? (layer.getVisualMinX() + newW) : (dW == 1) ? layer.getVisualMinX() : (layer.getVisualMinX() + newW / 2.0);
            double anchorY = (dH == -1) ? (layer.getVisualMinY() + newH) : (dH == 1) ? layer.getVisualMinY() : (layer.getVisualMinY() + newH / 2.0);
            Point2D currentAnchor = layer.localToParent(new Point2D(anchorX, anchorY));

            if (anchor[0] != null && currentAnchor != null) {
                layer.setTranslateX(layer.getTranslateX() + (anchor[0].getX() - currentAnchor.getX()));
                layer.setTranslateY(layer.getTranslateY() + (anchor[0].getY() - currentAnchor.getY()));
            }
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (rightClickFilter[0] != null && layer.getScene() != null) {
                layer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
                rightClickFilter[0] = null;
            }
            if (ctx[0] > 0) {
                if (dropCopy[0]) {
                    // 1. Create a copy of the CURRENT layer (with mirrored/scaled settings)
                    ShapeClipboardSupport.copy(layer);
                    ShapeLayer clone = ShapeClipboardSupport.getClipboardCopy();
                    if (clone != null) {
                        layer.getVisualizer().addShapeLayer(clone);
                        if (layer.getVisualizer().getPowerClipManager() != null && layer.getVisualizer().getPowerClipManager().isEditing()) {
                            layer.getVisualizer().applySmartPowerClip(clone, layer.getVisualizer().getPowerClipManager().getCurrentEditingZone(), false);
                        }
                    }
                    
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
                    if (layer.getVisualizer() != null && layer.getVisualizer().getHistoryManager() != null && startMemento[0] != null) {
                        layer.getVisualizer().getHistoryManager().addCommand(new TransformCommand(layer, startMemento[0], new NodeMemento(layer), layer.getActiveZone()));
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

        overlay.pivotHandle.setOnMousePressed(e -> {
            if (isEffectivelyLocked()) return;
            e.consume(); // Prevent fall-through to layer drag which hides handles
        });

        overlay.pivotHandle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked()) return;
            Point2D local = layer.sceneToLocal(e.getSceneX(), e.getSceneY());
            layer.updatePivot(local.getX(), local.getY());
            e.consume();
        });
        
        overlay.pivotHandle.setOnMouseReleased(e -> {
            if (isEffectivelyLocked()) return;
            // Reset visual pop effect if any
            overlay.pivotHandle.setScaleX(1.0);
            overlay.pivotHandle.setScaleY(1.0);
            e.consume();
        });
    }

    private void setupRotateHandler(Node handle) {
        final double[] startAngle = new double[1];
        final Point2D[] center = new Point2D[1];
        final NodeMemento[] memento = new NodeMemento[1];

        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked()) return;
            memento[0] = new NodeMemento(layer);
            center[0] = layer.localToScene(layer.getInternalPivotX(), layer.getInternalPivotY());
            startAngle[0] = Math.toDegrees(Math.atan2(e.getSceneY() - center[0].getY(), e.getSceneX() - center[0].getX())) - layer.getInternalRotation();
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked()) return;
            double angle = Math.toDegrees(Math.atan2(e.getSceneY() - center[0].getY(), e.getSceneX() - center[0].getX()));
            double newAngle = angle - startAngle[0];
            layer.setInternalRotation(newAngle);
            if (layer.getVisualizer() != null && layer.getVisualizer().getShapeManagerController() != null) {
                layer.getVisualizer().getShapeManagerController().updateAngleUI(newAngle);
            }
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isEffectivelyLocked()) return;
            if (layer.getVisualizer() != null && memento[0] != null) {
                TransformCommand cmd = new TransformCommand(layer, memento[0], new NodeMemento(layer), layer.getActiveZone());
                layer.getVisualizer().getHistoryManager().addCommand(cmd);
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
        final double[] start = new double[4]; // sceneX, sceneY, shearX, shearY
        final NodeMemento[] memento = new NodeMemento[1];

        handle.setOnMousePressed(e -> {
            if (isEffectivelyLocked()) return;
            layer.convertPrimitiveToPath();
            memento[0] = new NodeMemento(layer);
            start[0] = e.getSceneX();
            start[1] = e.getSceneY();
            start[2] = layer.getInternalShearX();
            start[3] = layer.getInternalShearY();
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked()) return;
            double dx = e.getSceneX() - start[0];
            double dy = e.getSceneY() - start[1];
            
            double angle = Math.toRadians(layer.getInternalRotation());
            double rotDx = dx * Math.cos(angle) + dy * Math.sin(angle);
            double rotDy = -dx * Math.sin(angle) + dy * Math.cos(angle);

            double amount = horizontal ? (rotDx / layer.getLogicalHeight()) : (rotDy / layer.getLogicalWidth());
            if (invert) amount = -amount;

            if (horizontal) layer.setInternalShearX(start[2] + amount);
            else layer.setInternalShearY(start[3] + amount);
            
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isEffectivelyLocked()) return;
            if (layer.getVisualizer() != null && memento[0] != null) {
                TransformCommand cmd = new TransformCommand(layer, memento[0], new NodeMemento(layer), layer.getActiveZone());
                layer.getVisualizer().getHistoryManager().addCommand(cmd);
            }
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
            if (isEffectivelyLocked()) return;
            memento[0] = new NodeMemento(layer);
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isEffectivelyLocked()) return;
            Point2D local = layer.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (local == null) return;

            // Calculate distance to the closest corner to allow dragging from stacked center in any direction
            double clampedX = Math.max(layer.getVisualMinX(), Math.min(local.getX(), layer.getVisualMinX() + layer.getLogicalWidth()));
            double clampedY = Math.max(layer.getVisualMinY(), Math.min(local.getY(), layer.getVisualMinY() + layer.getLogicalHeight()));

            double minDx = Math.min(clampedX - layer.getVisualMinX(), (layer.getVisualMinX() + layer.getLogicalWidth()) - clampedX);
            double minDy = Math.min(clampedY - layer.getVisualMinY(), (layer.getVisualMinY() + layer.getLogicalHeight()) - clampedY);

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
            if (isEffectivelyLocked()) return;
            if (layer.getVisualizer() != null && memento[0] != null) {
                layer.getVisualizer().getHistoryManager().addCommand(new TransformCommand(layer, memento[0], new NodeMemento(layer), layer.getActiveZone()));
            }
            e.consume();
        });
    }
}
