package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.model.BezierNode;
import org.example.model.ShapeType;

import java.util.ArrayList;
import java.util.List;

import static org.example.utils.GeometryUtility.format;

/**
 * Servicio encargado de la interacción avanzada con nodos Bezier.
 * Maneja la edición, inserción, eliminación y visualización de anclas y manejadores.
 */
public class BezierInteractionService {

    private final PrendaVisualizer visualizer;
    private Group handleGroup;
    
    // State
    private final javafx.beans.property.BooleanProperty nodeEditing = new javafx.beans.property.SimpleBooleanProperty(false);
    private ShapeLayer editingLayer;
    private List<BezierNode> bezierNodes;
    private BezierNode selectedBezierNode;
    private BezierNode currentDragNode;
    private String currentDragType;
    private List<BezierNode> nodesBeforeEdit;
    private ContextMenu activeNodeMenu;
    
    private javafx.event.EventHandler<MouseEvent> nodeEditDragHandler;
    private javafx.event.EventHandler<MouseEvent> nodeEditReleaseHandler;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> nodeEditKeyHandler;
    private javafx.event.EventHandler<MouseEvent> nodeInsertHandler;
    private javafx.event.EventHandler<MouseEvent> nodeHoverHandler;
    private final Runnable viewportZoomHandler = this::syncHandlePositions;

    public BezierInteractionService(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    public boolean isNodeEditing() {
        return nodeEditing.get();
    }

    public javafx.beans.property.BooleanProperty nodeEditingProperty() {
        return nodeEditing;
    }

    public ShapeLayer getEditingLayer() {
        return editingLayer;
    }

    public void enterNodeEditMode(ShapeLayer layer) {
        if (layer == null) return;

        layer.convertPrimitiveToPath();
        if (layer.getBezierNodes() == null) return;

        if (isNodeEditing() && editingLayer != layer) {
            exitNodeEditMode();
        }

        nodeEditing.set(true);
        editingLayer = layer;
        editingLayer.setSelected(true);
        editingLayer.setIsNodeEditing(true);

        this.bezierNodes = layer.getBezierNodes();
        
        layer.setOnResizeHandler((dw, dh) -> updateNodeEditHandles());

        if (handleGroup == null) {
            handleGroup = new Group();
            visualizer.getContentGroup().getChildren().add(handleGroup);
        } else if (!visualizer.getContentGroup().getChildren().contains(handleGroup)) {
            visualizer.getContentGroup().getChildren().add(handleGroup);
        }

        handleGroup.toFront();
        handleGroup.setFocusTraversable(true);
        handleGroup.requestFocus();

        updateNodeEditHandles();
        normalizeAndConvertToPath(editingLayer);
        initNodeEditHandlers();

        if (visualizer.getScene() != null) {
            visualizer.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, nodeInsertHandler);
            visualizer.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, nodeHoverHandler);
            visualizer.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, nodeEditKeyHandler);
        }
        if (visualizer.getViewportController() != null) {
            visualizer.getViewportController().addOnViewportChanged(viewportZoomHandler);
        }
    }

    public void exitNodeEditMode() {
        if (!isNodeEditing()) return;

        if (editingLayer != null) {
            editingLayer.setSelected(true);
            editingLayer.setIsNodeEditing(false);
            recalculateLayerBounds(editingLayer, true);
            editingLayer.setOnResizeHandler(null);
        }

        nodeEditing.set(false);
        editingLayer = null;
        bezierNodes = null;
        selectedBezierNode = null;

        if (handleGroup != null) {
            handleGroup.getChildren().clear();
            visualizer.getContentGroup().getChildren().remove(handleGroup);
        }

        if (visualizer.getScene() != null) {
            visualizer.getScene().removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, nodeEditKeyHandler);
            visualizer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, nodeInsertHandler);
            visualizer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, nodeHoverHandler);
            visualizer.getContentGroup().setCursor(Cursor.DEFAULT);
        }
        if (visualizer.getViewportController() != null) {
            visualizer.getViewportController().removeOnViewportChanged(viewportZoomHandler);
        }

        if (activeNodeMenu != null) {
            activeNodeMenu.hide();
            activeNodeMenu = null;
        }
    }

    private void normalizeAndConvertToPath(ShapeLayer layer) {
        if (layer == null) return;
        javafx.scene.Group sg = layer.getShapeGroup();

        boolean changed = false;
        double w = layer.getBoundsInLocal().getWidth();
        double h = layer.getBoundsInLocal().getHeight();

        if (sg.getScaleX() < 0) {
            sg.setScaleX(1);
            sg.setTranslateX(0);
            if (layer.getBezierNodes() != null) {
                for (BezierNode n : layer.getBezierNodes()) {
                    n.anchor = new Point2D(w - n.anchor.getX(), n.anchor.getY());
                    n.control1 = new Point2D(w - n.control1.getX(), n.control1.getY());
                    n.control2 = new Point2D(w - n.control2.getX(), n.control2.getY());
                }
                changed = true;
            }
        }

        if (sg.getScaleY() < 0) {
            sg.setScaleY(1);
            sg.setTranslateY(0);
            if (layer.getBezierNodes() != null) {
                for (BezierNode n : layer.getBezierNodes()) {
                    n.anchor = new Point2D(n.anchor.getX(), h - n.anchor.getY());
                    n.control1 = new Point2D(n.control1.getX(), h - n.control1.getY());
                    n.control2 = new Point2D(n.control2.getX(), h - n.control2.getY());
                }
                changed = true;
            }
        }

        if (layer.getBezierNodes() != null && !layer.getBezierNodes().isEmpty()) {
            layer.setType(ShapeType.CUSTOM_PATH);
            refreshLayerPath(layer, layer.getBezierNodes());
        }

        updateNodeEditHandles();
    }

    public void updateNodeEditHandles() {
        if (!isNodeEditing() || editingLayer == null || bezierNodes == null) return;

        if (currentDragNode != null) {
            syncHandlePositions();
            return;
        }

        handleGroup.getChildren().clear();

        for (int i = 0; i < bezierNodes.size(); i++) {
            BezierNode n = bezierNodes.get(i);
            final BezierNode nodeRef = n;
            final int nodeIndex = i;
            
            Point2D anchorScene = editingLayer.shapeLocalToScene(n.anchor);
            Point2D anchorLocal = visualizer.getContentGroup().sceneToLocal(anchorScene);

            Rectangle rect = new Rectangle(anchorLocal.getX() - 3.0, anchorLocal.getY() - 3.0, 6, 6);
            rect.setArcWidth(1);
            rect.setArcHeight(1);
            rect.setFill(n == selectedBezierNode ? Color.RED : Color.web("#0078D7"));
            rect.setStroke(Color.WHITE);
            rect.setStrokeWidth(1.5);
            rect.setCursor(Cursor.HAND);
            rect.setUserData(new Object[] { n, "ANCHOR", nodeIndex });

            rect.setOnMouseEntered(ev -> {
                rect.setEffect(new javafx.scene.effect.DropShadow(4, Color.color(0, 0, 0, 0.4)));
                if (nodeRef != selectedBezierNode) rect.setFill(Color.web("#006CC2"));
            });
            rect.setOnMouseExited(ev -> {
                rect.setEffect(null);
                rect.setFill(nodeRef == selectedBezierNode ? Color.RED : Color.web("#0078D7"));
            });

            Rectangle hitBoxAnchor = new Rectangle(anchorLocal.getX() - 7.0, anchorLocal.getY() - 7.0, 14, 14);
            hitBoxAnchor.setFill(Color.TRANSPARENT);
            hitBoxAnchor.setCursor(Cursor.HAND);
            hitBoxAnchor.setUserData(rect.getUserData());
            
            hitBoxAnchor.setOnMouseEntered(rect.getOnMouseEntered());
            hitBoxAnchor.setOnMouseExited(rect.getOnMouseExited());

            hitBoxAnchor.setOnMousePressed(e -> {
                selectedBezierNode = nodeRef;
                currentDragNode = nodeRef;
                currentDragType = "ANCHOR";
                if (editingLayer != null && editingLayer.getBezierNodes() != null) {
                    nodesBeforeEdit = new ArrayList<>();
                    for (BezierNode bn : editingLayer.getBezierNodes()) {
                        nodesBeforeEdit.add(bn.copy());
                    }
                }
            });

            hitBoxAnchor.setOnMouseReleased(e -> {
                recordUndo();
                currentDragNode = null;
                currentDragType = null;
                e.consume();
            });

            setupNodeContextMenu(hitBoxAnchor, nodeRef);
            handleGroup.getChildren().add(rect);
            handleGroup.getChildren().add(hitBoxAnchor);

            int idx = i;
            int prevIdx = (idx - 1 + bezierNodes.size()) % bezierNodes.size();
            BezierNode prevNode = bezierNodes.get(prevIdx);
            boolean isClosed = editingLayer.getIsClosed();

            boolean showC1 = (idx > 0 || isClosed) && prevNode.segmentType == BezierNode.SegmentType.CURVE;
            boolean showC2 = (idx < bezierNodes.size() - 1 || isClosed) && n.segmentType == BezierNode.SegmentType.CURVE;

            if (showC1) {
                Point2D c1Scene = editingLayer.shapeLocalToScene(n.control1);
                Point2D c1Local = visualizer.getContentGroup().sceneToLocal(c1Scene);
                drawEditHandle(n, c1Local, anchorLocal, "C1", nodeIndex);
            }
            if (showC2) {
                Point2D c2Scene = editingLayer.shapeLocalToScene(n.control2);
                Point2D c2Local = visualizer.getContentGroup().sceneToLocal(c2Scene);
                drawEditHandle(n, c2Local, anchorLocal, "C2", nodeIndex);
            }
        }
        applyAntiScaleToHandles();
    }

    private void applyAntiScaleToHandles() {
        if (handleGroup == null || visualizer.getViewportController() == null) return;
        double viewportScale = visualizer.getViewportController().getFinalScale();
        double invScale = viewportScale > 0 ? 1.0 / viewportScale : 1.0;

        for (Node child : handleGroup.getChildren()) {
            if (child instanceof Rectangle || child instanceof Circle) {
                child.setScaleX(invScale);
                child.setScaleY(invScale);
            } else if (child instanceof Line) {
                ((Line) child).setStrokeWidth(1.0 * invScale);
            }
        }
    }

    private void syncHandlePositions() {
        if (handleGroup == null || editingLayer == null) return;
        
        for (Node child : handleGroup.getChildren()) {
            Object data = child.getUserData();
            if (!(data instanceof Object[])) continue;
            Object[] tag = (Object[]) data;
            BezierNode n = (BezierNode) tag[0];
            String type = (String) tag[1];
            
            Point2D anchorScene = editingLayer.shapeLocalToScene(n.anchor);
            Point2D anchorLocal = visualizer.getContentGroup().sceneToLocal(anchorScene);
            
            if ("ANCHOR".equals(type)) {
                Rectangle r = (Rectangle) child;
                if (r.getFill() == Color.TRANSPARENT) {
                    // It's the hitbox
                    r.setX(anchorLocal.getX() - 7.0);
                    r.setY(anchorLocal.getY() - 7.0);
                } else {
                    r.setX(anchorLocal.getX() - 3.0);
                    r.setY(anchorLocal.getY() - 3.0);
                    r.setFill(n == selectedBezierNode ? Color.RED : Color.web("#0078D7"));
                }
            } else if ("C1".equals(type) || "C2".equals(type)) {
                if (child instanceof Circle) {
                    Circle c = (Circle) child;
                    Point2D controlScene = editingLayer.shapeLocalToScene("C1".equals(type) ? n.control1 : n.control2);
                    Point2D controlLocal = visualizer.getContentGroup().sceneToLocal(controlScene);
                    c.setCenterX(controlLocal.getX());
                    c.setCenterY(controlLocal.getY());
                }
            } else if ("C1LINE".equals(type) || "C2LINE".equals(type)) {
                Line l = (Line) child;
                Point2D controlScene = editingLayer.shapeLocalToScene("C1LINE".equals(type) ? n.control1 : n.control2);
                Point2D controlLocal = visualizer.getContentGroup().sceneToLocal(controlScene);
                l.setStartX(anchorLocal.getX());
                l.setStartY(anchorLocal.getY());
                l.setEndX(controlLocal.getX());
                l.setEndY(controlLocal.getY());
            }
        }
        applyAntiScaleToHandles();
    }

    private void setupNodeContextMenu(Rectangle rect, BezierNode n) {
        rect.setOnContextMenuRequested(e -> {
            e.consume();
            if (activeNodeMenu != null) activeNodeMenu.hide();
            ContextMenu cm = new ContextMenu();
            activeNodeMenu = cm;
            cm.setOnHidden(ev -> { if (activeNodeMenu == cm) activeNodeMenu = null; });

            Menu typeMenu = new Menu("Tipo de Nodo");
            CheckMenuItem cusp = new CheckMenuItem("Asimétrico (Cusp)");
            CheckMenuItem smooth = new CheckMenuItem("Uniforme (Smooth)");
            CheckMenuItem symm = new CheckMenuItem("Simétrico");
            cusp.setSelected(n.type == BezierNode.NodeType.CUSP);
            smooth.setSelected(n.type == BezierNode.NodeType.SMOOTH);
            symm.setSelected(n.type == BezierNode.NodeType.SYMMETRICAL);
            
            cusp.setOnAction(ev -> { n.type = BezierNode.NodeType.CUSP; updateNodeEditHandles(); });
            smooth.setOnAction(ev -> { n.type = BezierNode.NodeType.SMOOTH; alignHandlesCollinear(n, true); refreshLayerPath(editingLayer, bezierNodes); updateNodeEditHandles(); });
            symm.setOnAction(ev -> { n.type = BezierNode.NodeType.SYMMETRICAL; alignHandlesCollinear(n, false); refreshLayerPath(editingLayer, bezierNodes); updateNodeEditHandles(); });
            typeMenu.getItems().addAll(cusp, smooth, symm);

            Menu segMenu = new Menu("Segmento");
            CheckMenuItem lineSeg = new CheckMenuItem("A línea");
            CheckMenuItem curveSeg = new CheckMenuItem("A curva");
            lineSeg.setSelected(n.segmentType == BezierNode.SegmentType.LINE);
            curveSeg.setSelected(n.segmentType == BezierNode.SegmentType.CURVE);
            lineSeg.setOnAction(ev -> { n.segmentType = BezierNode.SegmentType.LINE; refreshLayerPath(editingLayer, bezierNodes); updateNodeEditHandles(); });
            curveSeg.setOnAction(ev -> { n.segmentType = BezierNode.SegmentType.CURVE; refreshLayerPath(editingLayer, bezierNodes); updateNodeEditHandles(); });
            segMenu.getItems().addAll(lineSeg, curveSeg);

            MenuItem delItem = new MenuItem("Eliminar Punto");
            delItem.setOnAction(ev -> { selectedBezierNode = n; deleteSelectedNode(); });

            cm.getItems().addAll(typeMenu, segMenu, new SeparatorMenuItem(), delItem);
            cm.show(rect, e.getScreenX(), e.getScreenY());
        });
    }

    private void alignHandlesCollinear(BezierNode n, boolean preserveLengths) {
        double dx = n.control1.getX() - n.anchor.getX();
        double dy = n.control1.getY() - n.anchor.getY();
        double dist1 = Math.sqrt(dx * dx + dy * dy);
        if (dist1 > 0) {
            if (preserveLengths) {
                double dx2 = n.control2.getX() - n.anchor.getX();
                double dy2 = n.control2.getY() - n.anchor.getY();
                double dist2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
                n.control2 = new Point2D(n.anchor.getX() - (dx / dist1) * dist2,
                        n.anchor.getY() - (dy / dist1) * dist2);
            } else {
                n.control2 = new Point2D(n.anchor.getX() - dx, n.anchor.getY() - dy);
            }
        }
    }

    private void drawEditHandle(BezierNode n, Point2D controlLocal, Point2D anchorLocal, String type, int nodeIndex) {
        Line handleBar = new Line(anchorLocal.getX(), anchorLocal.getY(), controlLocal.getX(), controlLocal.getY());
        handleBar.setStroke(Color.web("#0078D7", 0.6));
        handleBar.setStrokeWidth(1.0);
        handleBar.setMouseTransparent(true);
        handleBar.setUserData(new Object[] { n, type + "LINE", nodeIndex });
        handleGroup.getChildren().add(handleBar);

        Circle c = new Circle(controlLocal.getX(), controlLocal.getY(), 3.5, Color.WHITE);
        c.setStroke(Color.web("#0078D7"));
        c.setStrokeWidth(1.5);
        c.setCursor(Cursor.HAND);
        c.setUserData(new Object[] { n, type, nodeIndex });
        
        c.setOnMouseEntered(ev -> { c.setStrokeWidth(2.0); c.setRadius(4.5); });
        c.setOnMouseExited(ev -> { c.setStrokeWidth(1.5); c.setRadius(3.5); });
        
        // Improve hitbox sensitivity using a larger transparent region
        Rectangle hitBox = new Rectangle();
        hitBox.widthProperty().bind(c.radiusProperty().multiply(4));
        hitBox.heightProperty().bind(c.radiusProperty().multiply(4));
        hitBox.layoutXProperty().bind(c.centerXProperty().subtract(c.radiusProperty().multiply(2)));
        hitBox.layoutYProperty().bind(c.centerYProperty().subtract(c.radiusProperty().multiply(2)));
        hitBox.setFill(Color.TRANSPARENT);
        hitBox.setCursor(Cursor.HAND);
        hitBox.setUserData(c.getUserData());
        
        hitBox.setOnMouseEntered(c.getOnMouseEntered());
        hitBox.setOnMouseExited(c.getOnMouseExited());

        hitBox.setOnMousePressed(e -> {
            currentDragNode = n;
            selectedBezierNode = n;
            currentDragType = type;
            if (editingLayer != null && editingLayer.getBezierNodes() != null) {
                nodesBeforeEdit = new ArrayList<>();
                for (BezierNode bn : editingLayer.getBezierNodes()) {
                    nodesBeforeEdit.add(bn.copy());
                }
            }
            updateNodeEditHandles();
            e.consume();
        });

        hitBox.setOnMouseReleased(e -> {
            recordUndo();
            currentDragNode = null;
            currentDragType = null;
            e.consume();
        });

        // Add both to group (order matters for rendering vs hit detection)
        handleGroup.getChildren().add(c);
        handleGroup.getChildren().add(hitBox);
    }

    private void initNodeEditHandlers() {
        if (nodeEditDragHandler != null) return;

        nodeEditDragHandler = e -> {
            if (!isNodeEditing() || currentDragNode == null) return;
            e.consume();

            Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
            Point2D pLayer = editingLayer.sceneToLocal(pScene);
            String type = currentDragType != null ? currentDragType : "ANCHOR";

            if ("C1".equals(type)) {
                currentDragNode.control1 = pLayer;
                if (currentDragNode.type == BezierNode.NodeType.SMOOTH || currentDragNode.type == BezierNode.NodeType.SYMMETRICAL) {
                    double dx = currentDragNode.control1.getX() - currentDragNode.anchor.getX();
                    double dy = currentDragNode.control1.getY() - currentDragNode.anchor.getY();
                    double dist1 = Math.sqrt(dx * dx + dy * dy);
                    if (dist1 > 0) {
                        if (currentDragNode.type == BezierNode.NodeType.SYMMETRICAL) {
                            currentDragNode.control2 = new Point2D(currentDragNode.anchor.getX() - dx, currentDragNode.anchor.getY() - dy);
                        } else {
                            double dx2 = currentDragNode.control2.getX() - currentDragNode.anchor.getX();
                            double dy2 = currentDragNode.control2.getY() - currentDragNode.anchor.getY();
                            double dist2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
                            currentDragNode.control2 = new Point2D(currentDragNode.anchor.getX() - (dx / dist1) * dist2, currentDragNode.anchor.getY() - (dy / dist1) * dist2);
                        }
                    }
                }
            } else if ("C2".equals(type)) {
                currentDragNode.control2 = pLayer;
                if (currentDragNode.type == BezierNode.NodeType.SMOOTH || currentDragNode.type == BezierNode.NodeType.SYMMETRICAL) {
                    double dx = currentDragNode.control2.getX() - currentDragNode.anchor.getX();
                    double dy = currentDragNode.control2.getY() - currentDragNode.anchor.getY();
                    double dist2 = Math.sqrt(dx * dx + dy * dy);
                    if (dist2 > 0) {
                        if (currentDragNode.type == BezierNode.NodeType.SYMMETRICAL) {
                            currentDragNode.control1 = new Point2D(currentDragNode.anchor.getX() - dx, currentDragNode.anchor.getY() - dy);
                        } else {
                            double dx1 = currentDragNode.control1.getX() - currentDragNode.anchor.getX();
                            double dy1 = currentDragNode.control1.getY() - currentDragNode.anchor.getY();
                            double dist1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
                            currentDragNode.control1 = new Point2D(currentDragNode.anchor.getX() - (dx / dist2) * dist1, currentDragNode.anchor.getY() - (dy / dist2) * dist1);
                        }
                    }
                }
            } else {
                double dx = pLayer.getX() - currentDragNode.anchor.getX();
                double dy = pLayer.getY() - currentDragNode.anchor.getY();
                currentDragNode.anchor = pLayer;
                currentDragNode.control1 = new Point2D(currentDragNode.control1.getX() + dx, currentDragNode.control1.getY() + dy);
                currentDragNode.control2 = new Point2D(currentDragNode.control2.getX() + dx, currentDragNode.control2.getY() + dy);
            }

            refreshLayerPath(editingLayer, bezierNodes);
            // DO NOT recalculate bounds during drag to avoid "jumping" headers/handles
            // recalculateLayerBounds(editingLayer, false); 
            updateNodeEditHandles();
        };

        nodeEditReleaseHandler = e -> {
            if (isNodeEditing()) {
                // Finalize bounds and normalize coordinates on release
                if (editingLayer != null) {
                    recalculateLayerBounds(editingLayer, true); 
                    editingLayer.updateVisuals();
                }
                recordUndo();
                currentDragNode = null;
            }
        };

        visualizer.addEventHandler(MouseEvent.MOUSE_DRAGGED, nodeEditDragHandler);
        visualizer.addEventHandler(MouseEvent.MOUSE_RELEASED, nodeEditReleaseHandler);

        nodeEditKeyHandler = e -> {
            if (isNodeEditing()) {
                if (e.getCode() == javafx.scene.input.KeyCode.DELETE) {
                    deleteSelectedNode(); e.consume();
                } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    exitNodeEditMode(); e.consume();
                }
            }
        };

        nodeInsertHandler = e -> {
            if (isNodeEditing()) {
                boolean isDoubleClick = e.getClickCount() == 2 && e.isPrimaryButtonDown();
                boolean isRightClick = e.getButton() == javafx.scene.input.MouseButton.SECONDARY;
                if (!isDoubleClick && !isRightClick) return;
                if (e.getTarget() instanceof Circle || e.getTarget() instanceof Rectangle) return;

                Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
                Point2D pLayer = editingLayer.sceneToLocal(pScene);
                tryInsertNode(pLayer.getX(), pLayer.getY());
                e.consume();
            }
        };

        nodeHoverHandler = e -> {
            if (isNodeEditing() && editingLayer != null) {
                Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
                Point2D pLayer = editingLayer.sceneToLocal(pScene);
                if (isNearSegment(pLayer.getX(), pLayer.getY())) {
                    visualizer.getContentGroup().setCursor(Cursor.CROSSHAIR);
                } else {
                    visualizer.getContentGroup().setCursor(Cursor.DEFAULT);
                }
            }
        };
    }

    private void tryInsertNode(double localX, double localY) {
        if (bezierNodes == null || bezierNodes.size() < 2) return;
        Point2D p = new Point2D(localX, localY);
        double threshold = 10.0;
        int bestSegment = -1;
        double bestT = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < bezierNodes.size() - 1; i++) {
            BezierNode n1 = bezierNodes.get(i);
            BezierNode n2 = bezierNodes.get(i + 1);
            for (int step = 0; step <= 50; step++) {
                double t = step / 50.0;
                Point2D onCurve = org.example.utils.GeometryUtility.evalCubicBezier(n1.anchor, n1.control2, n2.control1, n2.anchor, t);
                double d = onCurve.distance(p);
                if (d < minDistance) { minDistance = d; bestSegment = i; bestT = t; }
            }
        }
        if (minDistance < threshold && bestSegment != -1) insertNodeAt(bestSegment, bestT);
    }

    private void insertNodeAt(int segmentIndex, double t) {
        List<BezierNode> nodesBefore = new ArrayList<>();
        if (bezierNodes != null) { for (BezierNode n : bezierNodes) nodesBefore.add(n.copy()); }

        BezierNode n1 = bezierNodes.get(segmentIndex);
        BezierNode n2 = bezierNodes.get(segmentIndex + 1);

        Point2D p0 = n1.anchor; Point2D p1 = n1.control2; Point2D p2 = n2.control1; Point2D p3 = n2.anchor;
        Point2D q0 = lerp(p0, p1, t); Point2D q1 = lerp(p1, p2, t); Point2D q2 = lerp(p2, p3, t);
        Point2D r0 = lerp(q0, q1, t); Point2D r1 = lerp(q1, q2, t);
        Point2D s0 = lerp(r0, r1, t);

        BezierNode newNode = new BezierNode(s0, r0, r1);
        n1.control2 = q0; n2.control1 = q2;
        bezierNodes.add(segmentIndex + 1, newNode);

        refreshLayerPath(editingLayer, bezierNodes);
        updateNodeEditHandles();
        currentDragNode = newNode;

        if (visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.BezierEditCommand(editingLayer, nodesBefore, bezierNodes, editingLayer.getActiveZone()));
        }
    }

    private void deleteSelectedNode() {
        BezierNode toDelete = selectedBezierNode != null ? selectedBezierNode : currentDragNode;
        if (!isNodeEditing() || toDelete == null || bezierNodes == null || editingLayer == null) return;
        if (bezierNodes.size() <= 2) return;

        List<BezierNode> nodesBefore = new ArrayList<>();
        for (BezierNode bn : bezierNodes) nodesBefore.add(bn.copy());

        bezierNodes.remove(toDelete);
        currentDragNode = null;
        selectedBezierNode = null;
        updateNodeEditHandles();
        refreshLayerPath(editingLayer, bezierNodes);

        if (visualizer.getHistoryManager() != null) {
            List<BezierNode> currentNodes = new ArrayList<>();
            for (BezierNode bn : editingLayer.getBezierNodes()) currentNodes.add(bn.copy());
            visualizer.getHistoryManager().addCommand(new org.example.pattern.BezierEditCommand(editingLayer, nodesBefore, currentNodes, editingLayer.getActiveZone()));
        }
    }

    private void recordUndo() {
        if (isNodeEditing() && editingLayer != null && nodesBeforeEdit != null) {
            List<BezierNode> currentNodes = new ArrayList<>();
            for (BezierNode bn : editingLayer.getBezierNodes()) currentNodes.add(bn.copy());
            visualizer.getHistoryManager().addCommand(new org.example.pattern.BezierEditCommand(editingLayer, nodesBeforeEdit, currentNodes, editingLayer.getActiveZone()));
            nodesBeforeEdit = null;
        }
    }

    public void refreshLayerPath(ShapeLayer layer, List<BezierNode> nodes) {
        StringBuilder sb = new StringBuilder();
        if (nodes.isEmpty()) return;
        BezierNode first = nodes.get(0);
        sb.append("M ").append(format(first.anchor.getX())).append(",").append(format(first.anchor.getY()));
        for (int i = 0; i < nodes.size() - 1; i++) {
            BezierNode n1 = nodes.get(i); BezierNode n2 = nodes.get(i + 1);
            if (n1.segmentType == BezierNode.SegmentType.LINE) {
                sb.append(" L ").append(format(n2.anchor.getX())).append(",").append(format(n2.anchor.getY()));
            } else {
                sb.append(" C ").append(format(n1.control2.getX())).append(",").append(format(n1.control2.getY())).append(" ").append(format(n2.control1.getX())).append(",").append(format(n2.control1.getY())).append(" ").append(format(n2.anchor.getX())).append(",").append(format(n2.anchor.getY()));
            }
        }
        if (layer.getIsClosed() && nodes.size() > 1) {
            BezierNode last = nodes.get(nodes.size() - 1);
            if (last.segmentType == BezierNode.SegmentType.LINE) { sb.append(" Z");
            } else { sb.append(" C ").append(format(last.control2.getX())).append(",").append(format(last.control2.getY())).append(" ").append(format(first.control1.getX())).append(",").append(format(first.control1.getY())).append(" ").append(format(first.anchor.getX())).append(",").append(format(first.anchor.getY())).append(" Z"); }
        }
        layer.setSvgPathData(sb.toString());
    }

    public void recalculateLayerBounds(ShapeLayer layer, boolean normalize) {
        if (layer == null) return;
        layer.refreshShapeVisuals();
    }

    private boolean isNearSegment(double x, double y) {
        if (bezierNodes == null || bezierNodes.size() < 2) return false;
        Point2D p = new Point2D(x, y);
        double threshold = 10.0;
        for (int i = 0; i < bezierNodes.size() - 1; i++) {
            BezierNode n1 = bezierNodes.get(i); BezierNode n2 = bezierNodes.get(i + 1);
            for (int step = 0; step <= 20; step++) {
                double t = step / 20.0;
                Point2D onCurve = org.example.utils.GeometryUtility.evalCubicBezier(n1.anchor, n1.control2, n2.control1, n2.anchor, t);
                if (onCurve.distance(p) < threshold) return true;
            }
        }
        return false;
    }

    private Point2D lerp(Point2D a, Point2D b, double t) {
        return new Point2D(a.getX() + (b.getX() - a.getX()) * t, a.getY() + (b.getY() - a.getY()) * t);
    }

    public Group getHandleGroup() {
        return handleGroup;
    }
}
