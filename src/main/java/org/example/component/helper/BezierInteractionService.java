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
import javafx.scene.shape.Polygon;
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
    private final java.util.Set<BezierNode> selectedBezierNodes = new java.util.LinkedHashSet<>();
    private Rectangle marqueeRect;
    private Point2D marqueeStartScene;
    private boolean isMarqueeDragging = false;
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
            visualizer.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, nodeEditDragHandler);
            visualizer.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, nodeEditReleaseHandler);
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
        selectedBezierNodes.clear();

        if (handleGroup != null) {
            handleGroup.getChildren().clear();
            visualizer.getContentGroup().getChildren().remove(handleGroup);
        }

        if (visualizer.getScene() != null) {
            visualizer.getScene().removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, nodeEditKeyHandler);
            visualizer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, nodeInsertHandler);
            visualizer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, nodeHoverHandler);
            visualizer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, nodeEditDragHandler);
            visualizer.getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, nodeEditReleaseHandler);
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

            Circle anchorNode = new Circle(anchorLocal.getX(), anchorLocal.getY(), 3.0);
            anchorNode.setFill(selectedBezierNodes.contains(n) ? Color.RED : Color.web("#0078D7"));
            anchorNode.setStroke(Color.WHITE);
            anchorNode.setStrokeWidth(1.5);
            anchorNode.setCursor(Cursor.HAND);
            anchorNode.setUserData(new Object[] { n, "ANCHOR", nodeIndex });

            anchorNode.setOnMouseEntered(ev -> {
                anchorNode.setEffect(new javafx.scene.effect.DropShadow(4, Color.color(0, 0, 0, 0.4)));
                if (!selectedBezierNodes.contains(nodeRef)) anchorNode.setFill(Color.web("#006CC2"));
            });
            anchorNode.setOnMouseExited(ev -> {
                anchorNode.setEffect(null);
                anchorNode.setFill(selectedBezierNodes.contains(nodeRef) ? Color.RED : Color.web("#0078D7"));
            });

            Rectangle hitBoxAnchor = new Rectangle(anchorLocal.getX() - 7.0, anchorLocal.getY() - 7.0, 14, 14);
            hitBoxAnchor.setFill(Color.TRANSPARENT);
            hitBoxAnchor.setCursor(Cursor.HAND);
            hitBoxAnchor.setUserData(anchorNode.getUserData());
            
            hitBoxAnchor.setOnMouseEntered(anchorNode.getOnMouseEntered());
            hitBoxAnchor.setOnMouseExited(anchorNode.getOnMouseExited());

            hitBoxAnchor.setOnMousePressed(e -> {
                if (!e.isShiftDown()) {
                    if (!selectedBezierNodes.contains(nodeRef)) {
                        selectedBezierNodes.clear();
                        selectedBezierNodes.add(nodeRef);
                    }
                } else {
                    if (selectedBezierNodes.contains(nodeRef)) {
                        selectedBezierNodes.remove(nodeRef);
                    } else {
                        selectedBezierNodes.add(nodeRef);
                    }
                }
                currentDragNode = nodeRef;
                currentDragType = "ANCHOR";
                if (editingLayer != null && editingLayer.getBezierNodes() != null) {
                    nodesBeforeEdit = new ArrayList<>();
                    for (BezierNode bn : editingLayer.getBezierNodes()) {
                        nodesBeforeEdit.add(bn.copy());
                    }
                }
                updateNodeEditHandles();
                e.consume();
            });

            hitBoxAnchor.setOnMouseReleased(e -> {
                recordUndo();
                currentDragNode = null;
                currentDragType = null;
                e.consume();
            });

            setupNodeContextMenu(hitBoxAnchor, nodeRef);
            handleGroup.getChildren().add(anchorNode);
            handleGroup.getChildren().add(hitBoxAnchor);

            int idx = i;
            boolean isClosed = editingLayer.getIsClosed();

            int start = idx;
            while (start > 0 && !bezierNodes.get(start).isMoveTo) {
                start--;
            }
            int end = idx;
            while (end < bezierNodes.size() - 1 && !bezierNodes.get(end + 1).isMoveTo) {
                end++;
            }

            boolean showC1 = false;
            if (idx > start) {
                BezierNode prevNode = bezierNodes.get(idx - 1);
                showC1 = prevNode.segmentType == BezierNode.SegmentType.CURVE || (n.control1 != null && n.control1.distance(n.anchor) > 0.1);
            } else if (idx == start && isClosed) {
                BezierNode prevNode = bezierNodes.get(end);
                showC1 = prevNode.segmentType == BezierNode.SegmentType.CURVE || (n.control1 != null && n.control1.distance(n.anchor) > 0.1);
            }

            boolean showC2 = false;
            if (idx < end) {
                showC2 = n.segmentType == BezierNode.SegmentType.CURVE || (n.control2 != null && n.control2.distance(n.anchor) > 0.1);
            } else if (idx == end && isClosed) {
                showC2 = n.segmentType == BezierNode.SegmentType.CURVE || (n.control2 != null && n.control2.distance(n.anchor) > 0.1);
            }

            boolean isNSelected = selectedBezierNodes.contains(n);
            
            boolean isPrevSelected = false;
            if (idx > start) {
                isPrevSelected = selectedBezierNodes.contains(bezierNodes.get(idx - 1));
            } else if (idx == start && isClosed) {
                isPrevSelected = selectedBezierNodes.contains(bezierNodes.get(end));
            }
            
            boolean isNextSelected = false;
            if (idx < end) {
                isNextSelected = selectedBezierNodes.contains(bezierNodes.get(idx + 1));
            } else if (idx == end && isClosed) {
                isNextSelected = selectedBezierNodes.contains(bezierNodes.get(start));
            }

            if (showC1 && (isNSelected || isPrevSelected)) {
                Point2D c1Scene = editingLayer.shapeLocalToScene(n.control1);
                Point2D c1Local = visualizer.getContentGroup().sceneToLocal(c1Scene);
                drawEditHandle(n, c1Local, anchorLocal, "C1", nodeIndex);
            }
            if (showC2 && (isNSelected || isNextSelected)) {
                Point2D c2Scene = editingLayer.shapeLocalToScene(n.control2);
                Point2D c2Local = visualizer.getContentGroup().sceneToLocal(c2Scene);
                drawEditHandle(n, c2Local, anchorLocal, "C2", nodeIndex);
            }
        }
        if (marqueeRect != null && isMarqueeDragging) {
            if (!handleGroup.getChildren().contains(marqueeRect)) handleGroup.getChildren().add(marqueeRect);
            marqueeRect.toFront();
        }
        applyAntiScaleToHandles();
    }

    private void applyAntiScaleToHandles() {
        if (handleGroup == null || visualizer.getViewportController() == null) return;
        double viewportScale = visualizer.getViewportController().getFinalScale();
        double invScale = viewportScale > 0 ? 1.0 / viewportScale : 1.0;

        for (Node child : handleGroup.getChildren()) {
            if (child == marqueeRect) continue;
            if (child instanceof Rectangle || child instanceof Circle || child instanceof Polygon) {
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
                if (child instanceof Rectangle) {
                    // It's the hitbox
                    Rectangle r = (Rectangle) child;
                    r.setX(anchorLocal.getX() - 7.0);
                    r.setY(anchorLocal.getY() - 7.0);
                } else if (child instanceof Circle) {
                    Circle c = (Circle) child;
                    c.setCenterX(anchorLocal.getX());
                    c.setCenterY(anchorLocal.getY());
                    c.setFill(selectedBezierNodes.contains(n) ? Color.RED : Color.web("#0078D7"));
                }
            } else if (child == marqueeRect) {
                // ignore marqueeRect
            } else if ("C1".equals(type) || "C2".equals(type)) {
                Point2D controlScene = editingLayer.shapeLocalToScene("C1".equals(type) ? n.control1 : n.control2);
                Point2D controlLocal = visualizer.getContentGroup().sceneToLocal(controlScene);
                if (child instanceof Circle) {
                    Circle c = (Circle) child;
                    c.setCenterX(controlLocal.getX());
                    c.setCenterY(controlLocal.getY());
                } else if (child instanceof Polygon) {
                    Polygon arrowhead = (Polygon) child;
                    double len = anchorLocal.distance(controlLocal);
                    double dx = len > 0 ? (controlLocal.getX() - anchorLocal.getX()) / len : 1.0;
                    double dy = len > 0 ? (controlLocal.getY() - anchorLocal.getY()) / len : 0.0;
                    double size = 4.0;
                    double tipX = controlLocal.getX() + dx * size;
                    double tipY = controlLocal.getY() + dy * size;
                    double baseMidX = controlLocal.getX() - dx * size;
                    double baseMidY = controlLocal.getY() - dy * size;
                    double ox = -dy * (size * 0.8);
                    double oy = dx * (size * 0.8);
                    arrowhead.getPoints().setAll(
                        tipX, tipY,
                        baseMidX + ox, baseMidY + oy,
                        baseMidX - ox, baseMidY - oy
                    );
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
        if (marqueeRect != null && isMarqueeDragging) {
            if (!handleGroup.getChildren().contains(marqueeRect)) handleGroup.getChildren().add(marqueeRect);
            marqueeRect.toFront();
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
            
            cusp.setOnAction(ev -> { 
                captureUndoState();
                n.type = BezierNode.NodeType.CUSP; 
                updateNodeEditHandles(); 
                recordUndo();
            });
            smooth.setOnAction(ev -> { 
                captureUndoState();
                n.type = BezierNode.NodeType.SMOOTH; alignHandlesCollinear(n, true); refreshLayerPath(editingLayer, bezierNodes); updateNodeEditHandles(); 
                recordUndo();
            });
            symm.setOnAction(ev -> { 
                captureUndoState();
                n.type = BezierNode.NodeType.SYMMETRICAL; alignHandlesCollinear(n, false); refreshLayerPath(editingLayer, bezierNodes); updateNodeEditHandles(); 
                recordUndo();
            });
            typeMenu.getItems().addAll(cusp, smooth, symm);

            Menu segMenu = new Menu("Segmento");
            CheckMenuItem lineSeg = new CheckMenuItem("A línea");
            CheckMenuItem curveSeg = new CheckMenuItem("A curva");
            lineSeg.setSelected(n.segmentType == BezierNode.SegmentType.LINE);
            curveSeg.setSelected(n.segmentType == BezierNode.SegmentType.CURVE);
            lineSeg.setOnAction(ev -> { 
                captureUndoState();
                n.segmentType = BezierNode.SegmentType.LINE; refreshLayerPath(editingLayer, bezierNodes); updateNodeEditHandles(); 
                recordUndo();
            });
            curveSeg.setOnAction(ev -> { 
                captureUndoState();
                n.segmentType = BezierNode.SegmentType.CURVE; refreshLayerPath(editingLayer, bezierNodes); updateNodeEditHandles(); 
                recordUndo();
            });
            segMenu.getItems().addAll(lineSeg, curveSeg);

            MenuItem delItem = new MenuItem("Eliminar Punto");
            delItem.setOnAction(ev -> { if (!selectedBezierNodes.contains(n)) { selectedBezierNodes.clear(); selectedBezierNodes.add(n); } deleteSelectedNode(); });

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
        handleBar.setStroke(Color.web("#0078D7", 0.8));
        handleBar.setStrokeWidth(1.0);
        handleBar.getStrokeDashArray().addAll(3.0, 3.0); // Dotted line
        handleBar.setMouseTransparent(true);
        handleBar.setUserData(new Object[] { n, type + "LINE", nodeIndex });
        handleGroup.getChildren().add(handleBar);

        // Calculate arrowhead triangle pointing along the tangent vector
        double len = anchorLocal.distance(controlLocal);
        double dx = len > 0 ? (controlLocal.getX() - anchorLocal.getX()) / len : 1.0;
        double dy = len > 0 ? (controlLocal.getY() - anchorLocal.getY()) / len : 0.0;
        double size = 4.0;
        double tipX = controlLocal.getX() + dx * size;
        double tipY = controlLocal.getY() + dy * size;
        double baseMidX = controlLocal.getX() - dx * size;
        double baseMidY = controlLocal.getY() - dy * size;
        double ox = -dy * (size * 0.8);
        double oy = dx * (size * 0.8);

        Polygon arrowhead = new Polygon(
            tipX, tipY,
            baseMidX + ox, baseMidY + oy,
            baseMidX - ox, baseMidY - oy
        );
        arrowhead.setFill(Color.WHITE);
        arrowhead.setStroke(Color.web("#0078D7"));
        arrowhead.setStrokeWidth(1.5);
        arrowhead.setCursor(Cursor.HAND);
        arrowhead.setUserData(new Object[] { n, type, nodeIndex });

        arrowhead.setOnMouseEntered(ev -> {
            arrowhead.setFill(Color.web("#E6F2FF"));
            arrowhead.setStrokeWidth(2.0);
        });
        arrowhead.setOnMouseExited(ev -> {
            arrowhead.setFill(Color.WHITE);
            arrowhead.setStrokeWidth(1.5);
        });

        // Keep invisible Circle c for hitbox translation bindings
        Circle c = new Circle(controlLocal.getX(), controlLocal.getY(), 3.5);
        c.setVisible(false);
        c.setUserData(arrowhead.getUserData());
        
        // Improve hitbox sensitivity using a larger transparent region
        Rectangle hitBox = new Rectangle();
        hitBox.widthProperty().bind(c.radiusProperty().multiply(4));
        hitBox.heightProperty().bind(c.radiusProperty().multiply(4));
        hitBox.layoutXProperty().bind(c.centerXProperty().subtract(c.radiusProperty().multiply(2)));
        hitBox.layoutYProperty().bind(c.centerYProperty().subtract(c.radiusProperty().multiply(2)));
        hitBox.setFill(Color.TRANSPARENT);
        hitBox.setCursor(Cursor.HAND);
        hitBox.setUserData(c.getUserData());
        
        hitBox.setOnMouseEntered(arrowhead.getOnMouseEntered());
        hitBox.setOnMouseExited(arrowhead.getOnMouseExited());

        hitBox.setOnMousePressed(e -> {
            currentDragNode = n;
            if (!e.isShiftDown() && !selectedBezierNodes.contains(n)) {
                selectedBezierNodes.clear();
                selectedBezierNodes.add(n);
            } else if (e.isShiftDown()) {
                if (selectedBezierNodes.contains(n)) selectedBezierNodes.remove(n);
                else selectedBezierNodes.add(n);
            }
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
        handleGroup.getChildren().add(arrowhead);
        handleGroup.getChildren().add(hitBox);
    }

    private void initNodeEditHandlers() {
        if (nodeEditDragHandler != null) return;

        nodeEditDragHandler = e -> {
            if (!isNodeEditing()) return;
            if (isMarqueeDragging && marqueeRect != null && marqueeStartScene != null) {
                double currentX = e.getSceneX();
                double currentY = e.getSceneY();
                Point2D startLocal = visualizer.getContentGroup().sceneToLocal(marqueeStartScene);
                Point2D currentLocal = visualizer.getContentGroup().sceneToLocal(currentX, currentY);
                marqueeRect.setX(Math.min(startLocal.getX(), currentLocal.getX()));
                marqueeRect.setY(Math.min(startLocal.getY(), currentLocal.getY()));
                marqueeRect.setWidth(Math.abs(currentLocal.getX() - startLocal.getX()));
                marqueeRect.setHeight(Math.abs(currentLocal.getY() - startLocal.getY()));
                e.consume();
                return;
            }
            if (currentDragNode == null) return;
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
                java.util.Set<BezierNode> toMove = new java.util.HashSet<>();
                if (selectedBezierNodes.contains(currentDragNode)) {
                    toMove.addAll(selectedBezierNodes);
                } else {
                    toMove.add(currentDragNode);
                }
                for (BezierNode n : toMove) {
                    n.anchor = new Point2D(n.anchor.getX() + dx, n.anchor.getY() + dy);
                    n.control1 = new Point2D(n.control1.getX() + dx, n.control1.getY() + dy);
                    n.control2 = new Point2D(n.control2.getX() + dx, n.control2.getY() + dy);
                }
            }

            refreshLayerPath(editingLayer, bezierNodes);
            // DO NOT recalculate bounds during drag to avoid "jumping" headers/handles
            // recalculateLayerBounds(editingLayer, false); 
            updateNodeEditHandles();
        };

        nodeEditReleaseHandler = e -> {
            if (isNodeEditing()) {
                if (isMarqueeDragging) {
                    isMarqueeDragging = false;
                    if (marqueeRect != null) {
                        javafx.geometry.Bounds selectionBounds = marqueeRect.getBoundsInParent();
                        marqueeRect.setVisible(false);
                        if (!e.isShiftDown()) selectedBezierNodes.clear();
                        for (Node child : handleGroup.getChildren()) {
                            if (child instanceof Rectangle && child != marqueeRect) {
                                Object data = child.getUserData();
                                if (data instanceof Object[]) {
                                    Object[] tag = (Object[]) data;
                                    if ("ANCHOR".equals(tag[1])) {
                                        if (selectionBounds.intersects(child.getBoundsInParent())) {
                                            selectedBezierNodes.add((BezierNode) tag[0]);
                                        }
                                    }
                                }
                            }
                        }
                        updateNodeEditHandles();
                    }
                } else if (currentDragNode != null) {
                    if (editingLayer != null) {
                        recalculateLayerBounds(editingLayer, true); 
                        editingLayer.updateVisuals();
                    }
                    recordUndo();
                    currentDragNode = null;
                }
            }
        };

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
                boolean isSingleClick = e.getClickCount() == 1 && e.isPrimaryButtonDown();
                
                if (e.getTarget() instanceof Circle || e.getTarget() instanceof Rectangle) return;

                if (isDoubleClick) {
                    Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
                    Point2D pLayer = editingLayer.sceneToLocal(pScene);
                    tryInsertNode(pLayer.getX(), pLayer.getY(), false);
                    e.consume();
                } else if (isRightClick) {
                    // Mostrar menú contextual para elegir tipo de punto
                    Point2D pScene = new Point2D(e.getSceneX(), e.getSceneY());
                    if (bezierNodes == null || bezierNodes.size() < 2) return;
                    Point2D pLayer = editingLayer.sceneToLocal(pScene);
                    double threshold = 10.0;
                    int bestSegment = -1;
                    double bestT = -1;
                    double minDistance = Double.MAX_VALUE;
                    for (int i = 0; i < bezierNodes.size() - 1; i++) {
                        BezierNode bn1 = bezierNodes.get(i);
                        BezierNode bn2 = bezierNodes.get(i + 1);
                        if (bn2.isMoveTo) continue;
                        for (int step = 0; step <= 50; step++) {
                            double tt = step / 50.0;
                            Point2D onCurve = org.example.utils.GeometryUtility.evalCubicBezier(
                                    bn1.anchor, bn1.control2, bn2.control1, bn2.anchor, tt);
                            double d = onCurve.distance(pLayer);
                            if (d < minDistance) { minDistance = d; bestSegment = i; bestT = tt; }
                        }
                    }
                    if (minDistance >= threshold || bestSegment == -1) { e.consume(); return; }
                    final int segIdx = bestSegment;
                    final double tVal = bestT;
                    ContextMenu cm = new ContextMenu();
                    MenuItem addLine = new MenuItem("A\u00f1adir punto lineal");
                    addLine.setOnAction(ev -> insertNodeAt(segIdx, tVal, true));
                    MenuItem addCurve = new MenuItem("A\u00f1adir punto curvo");
                    addCurve.setOnAction(ev -> insertNodeAt(segIdx, tVal, false));
                    cm.getItems().addAll(addLine, addCurve);
                    cm.show(editingLayer, e.getScreenX(), e.getScreenY());
                    if (activeNodeMenu != null) activeNodeMenu.hide();
                    activeNodeMenu = cm;
                    cm.setOnHidden(ev -> { if (activeNodeMenu == cm) activeNodeMenu = null; });
                    e.consume();
                } else if (isSingleClick) {
                    marqueeStartScene = new Point2D(e.getSceneX(), e.getSceneY());
                    isMarqueeDragging = true;
                    if (marqueeRect == null) {
                        marqueeRect = new Rectangle();
                        marqueeRect.setFill(Color.web("#3498db", 0.15));
                        marqueeRect.setStroke(Color.web("#2980b9"));
                        marqueeRect.setStrokeWidth(1.0);
                        marqueeRect.setMouseTransparent(true);
                    }
                    if (!handleGroup.getChildren().contains(marqueeRect)) {
                        handleGroup.getChildren().add(marqueeRect);
                    }
                    Point2D localStart = visualizer.getContentGroup().sceneToLocal(marqueeStartScene);
                    marqueeRect.setX(localStart.getX());
                    marqueeRect.setY(localStart.getY());
                    marqueeRect.setWidth(0);
                    marqueeRect.setHeight(0);
                    marqueeRect.setVisible(true);
                    if (!e.isShiftDown()) {
                        selectedBezierNodes.clear();
                        updateNodeEditHandles();
                    }
                    e.consume();
                }
            }
        };

        nodeHoverHandler = e -> {
            // Eliminar o comentar para evitar el cursor de cruz molesto
        };
    }

    private void tryInsertNode(double localX, double localY, boolean isLinear) {
        if (bezierNodes == null || bezierNodes.size() < 2) return;
        Point2D p = new Point2D(localX, localY);
        double threshold = 10.0;
        int bestSegment = -1;
        double bestT = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < bezierNodes.size() - 1; i++) {
            BezierNode n1 = bezierNodes.get(i);
            BezierNode n2 = bezierNodes.get(i + 1);
            if (n2.isMoveTo) continue;
            for (int step = 0; step <= 50; step++) {
                double t = step / 50.0;
                Point2D onCurve = org.example.utils.GeometryUtility.evalCubicBezier(n1.anchor, n1.control2, n2.control1, n2.anchor, t);
                double d = onCurve.distance(p);
                if (d < minDistance) { minDistance = d; bestSegment = i; bestT = t; }
            }
        }
        if (minDistance < threshold && bestSegment != -1) insertNodeAt(bestSegment, bestT, isLinear);
    }

    private void insertNodeAt(int segmentIndex, double t, boolean isLinear) {
        List<BezierNode> nodesBefore = new ArrayList<>();
        if (bezierNodes != null) { for (BezierNode n : bezierNodes) nodesBefore.add(n.copy()); }

        BezierNode n1 = bezierNodes.get(segmentIndex);
        BezierNode n2 = bezierNodes.get(segmentIndex + 1);

        Point2D p0 = n1.anchor; Point2D p1 = n1.control2; Point2D p2 = n2.control1; Point2D p3 = n2.anchor;
        Point2D q0 = lerp(p0, p1, t); Point2D q1 = lerp(p1, p2, t); Point2D q2 = lerp(p2, p3, t);
        Point2D r0 = lerp(q0, q1, t); Point2D r1 = lerp(q1, q2, t);
        Point2D s0 = lerp(r0, r1, t);

        BezierNode newNode;
        if (isLinear) {
            // Punto lineal: ambos controles en el ancla, segmento tipo LINE
            newNode = new BezierNode(s0, s0, s0);
            newNode.segmentType = BezierNode.SegmentType.LINE;
            // Ajustar segmento anterior a LINE también
            n1.segmentType = BezierNode.SegmentType.LINE;
        } else {
            newNode = new BezierNode(s0, r0, r1);
            newNode.segmentType = BezierNode.SegmentType.CURVE;
        }
        n1.control2 = q0; n2.control1 = q2;
        bezierNodes.add(segmentIndex + 1, newNode);

        refreshLayerPath(editingLayer, bezierNodes);
        updateNodeEditHandles();
        currentDragNode = newNode;

        if (visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.BezierEditCommand(
                editingLayer, nodesBefore, bezierNodes, editingLayer.getActiveZone(), () -> {
                    if (editingLayer != null) this.bezierNodes = editingLayer.getBezierNodes();
                    updateNodeEditHandles();
                }));
        }
    }

    private void deleteSelectedNode() {
        if (!isNodeEditing() || bezierNodes == null || editingLayer == null) return;
        java.util.Set<BezierNode> toDelete = new java.util.HashSet<>(selectedBezierNodes);
        if (toDelete.isEmpty() && currentDragNode != null) {
            toDelete.add(currentDragNode);
        }
        if (toDelete.isEmpty() || bezierNodes.size() - toDelete.size() < 2) return;

        List<BezierNode> nodesBefore = new ArrayList<>();
        for (BezierNode bn : bezierNodes) nodesBefore.add(bn.copy());

        bezierNodes.removeAll(toDelete);
        currentDragNode = null;
        selectedBezierNodes.clear();
        updateNodeEditHandles();
        refreshLayerPath(editingLayer, bezierNodes);

        if (visualizer.getHistoryManager() != null) {
            List<BezierNode> currentNodes = new ArrayList<>();
            for (BezierNode bn : editingLayer.getBezierNodes()) currentNodes.add(bn.copy());
            visualizer.getHistoryManager().addCommand(new org.example.pattern.BezierEditCommand(
                editingLayer, nodesBefore, currentNodes, editingLayer.getActiveZone(), () -> {
                    if (editingLayer != null) this.bezierNodes = editingLayer.getBezierNodes();
                    updateNodeEditHandles();
                }));
        }
    }

    private void captureUndoState() {
        if (isNodeEditing() && editingLayer != null && editingLayer.getBezierNodes() != null) {
            nodesBeforeEdit = new ArrayList<>();
            for (BezierNode bn : editingLayer.getBezierNodes()) {
                nodesBeforeEdit.add(bn.copy());
            }
        }
    }

    private void recordUndo() {
        if (isNodeEditing() && editingLayer != null && nodesBeforeEdit != null) {
            List<BezierNode> currentNodes = new ArrayList<>();
            for (BezierNode bn : editingLayer.getBezierNodes()) currentNodes.add(bn.copy());
            
            boolean changed = false;
            if (nodesBeforeEdit.size() != currentNodes.size()) {
                changed = true;
            } else {
                for (int i = 0; i < nodesBeforeEdit.size(); i++) {
                    BezierNode o = nodesBeforeEdit.get(i);
                    BezierNode n = currentNodes.get(i);
                    if (o.type != n.type || o.segmentType != n.segmentType) { changed = true; break; }
                    if (o.anchor.distance(n.anchor) > 0.01) { changed = true; break; }
                    if ((o.control1 == null && n.control1 != null) || (o.control1 != null && n.control1 == null) || (o.control1 != null && n.control1 != null && o.control1.distance(n.control1) > 0.01)) { changed = true; break; }
                    if ((o.control2 == null && n.control2 != null) || (o.control2 != null && n.control2 == null) || (o.control2 != null && n.control2 != null && o.control2.distance(n.control2) > 0.01)) { changed = true; break; }
                }
            }

            if (changed && visualizer != null && visualizer.getHistoryManager() != null) {
                visualizer.getHistoryManager().addCommand(new org.example.pattern.BezierEditCommand(
                    editingLayer, nodesBeforeEdit, currentNodes, editingLayer.getActiveZone(), () -> {
                        // Re-sync the service's bezierNodes reference in case undo replaced the list object
                        if (editingLayer != null) {
                            this.bezierNodes = editingLayer.getBezierNodes();
                        }
                        updateNodeEditHandles();
                    }
                ));
            }
            nodesBeforeEdit = null;
        }
    }

    public void refreshLayerPath(ShapeLayer layer, List<BezierNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        String svgPath = ShapePathSupport.buildSvgPath(nodes, layer.getIsClosed());
        layer.fastUpdateSvgPathData(svgPath);
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
            if (n2.isMoveTo) continue;
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
