package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
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
 * Servicio encargado de la creación inicial de formas (Rectángulos, Círculos, Caminos Bezier).
 * Maneja el estado de "dibujo" mientras el usuario arrastra el mouse.
 */
public class ShapeCreationService {

    private final PrendaVisualizer visualizer;

    // State
    private boolean isCreatingShape = false;
    private ShapeType creationType;
    private Color creationFill;
    private Color creationStroke;
    private double creationStrokeWidth;
    private java.util.function.Consumer<ShapeLayer> onCreationFinish;

    private ShapeLayer tempCreationLayer;
    private double creationStartX, creationStartY;

    // Bezier Creation State
    private List<BezierNode> bezierNodes;
    private javafx.scene.shape.Path creationPreviewPath;
    private Group handleGroup;
    private BezierNode currentDragNode;

    // Handlers
    private javafx.event.EventHandler<MouseEvent> creationPressHandler;
    private javafx.event.EventHandler<MouseEvent> creationDragHandler;
    private javafx.event.EventHandler<MouseEvent> creationReleaseHandler;

    public ShapeCreationService(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    public boolean isCreatingShape() {
        return isCreatingShape;
    }

    public void startShapeCreation(ShapeType type, Color fill, Color stroke, double strokeWidth,
                                    java.util.function.Consumer<ShapeLayer> onFinish) {
        cancelShapeCreation();

        this.isCreatingShape = true;
        this.creationType = type;
        this.creationFill = fill;

        if (type == ShapeType.CUSTOM_PATH && (stroke == null || stroke.equals(Color.TRANSPARENT))) {
            this.creationStroke = Color.BLACK;
        } else {
            this.creationStroke = stroke;
        }

        this.creationStrokeWidth = strokeWidth;
        this.onCreationFinish = onFinish;

        visualizer.getContentGroup().setCursor(Cursor.CROSSHAIR);
        initCreationHandlers();
    }

    public void insertDefaultShape(ShapeType type, Color fill, Color stroke, double strokeWidth,
                                    java.util.function.Consumer<ShapeLayer> onFinish) {
        cancelShapeCreation();

        ShapeLayer layer = new ShapeLayer(type, fill, stroke, strokeWidth);
        layer.setSize(100, 100);
        layer.setTranslateX(200);
        layer.setTranslateY(200);

        visualizer.getLayerFactory().addShapeLayer(layer);

        if (onFinish != null) onFinish.accept(layer);
        visualizer.notifyStateChanged();
    }

    public void cancelShapeCreation() {
        this.isCreatingShape = false;
        visualizer.getContentGroup().setCursor(Cursor.DEFAULT);
        if (tempCreationLayer != null) {
            visualizer.getLayerFactory().removeLayer(tempCreationLayer);
            tempCreationLayer = null;
        }
        
        if (creationPreviewPath != null || handleGroup != null) {
            visualizer.getContentGroup().getChildren().removeAll(creationPreviewPath, handleGroup);
            creationPreviewPath = null;
            handleGroup = null;
            bezierNodes = null;
        }
    }

    private void initCreationHandlers() {
        if (creationPressHandler != null) return;

        creationPressHandler = e -> {
            if (!isCreatingShape) return;

            if (creationType == ShapeType.CUSTOM_PATH) {
                if (e.isPrimaryButtonDown()) {
                    e.consume();
                    Point2D p = visualizer.getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());

                    if (bezierNodes == null) {
                        bezierNodes = new ArrayList<>();
                        creationPreviewPath = new javafx.scene.shape.Path();
                        creationPreviewPath.setStroke(creationStroke != null ? creationStroke : Color.web("#0078D7"));
                        creationPreviewPath.setStrokeWidth(1.2);
                        creationPreviewPath.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                        creationPreviewPath.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                        creationPreviewPath.getStrokeDashArray().addAll(6d, 4d);

                        handleGroup = new Group();
                        visualizer.getContentGroup().getChildren().addAll(creationPreviewPath, handleGroup);
                    }

                    if (bezierNodes.size() > 2 && p.distance(bezierNodes.get(0).anchor) < 10) {
                        finishBezierShape(true);
                        return;
                    }

                    BezierNode newNode = new BezierNode(p);
                    bezierNodes.add(newNode);
                    currentDragNode = newNode;

                    updateBezierPreview();

                    if (e.getClickCount() == 2) {
                        finishBezierShape(false);
                    }
                }
                return;
            }

            if (e.isPrimaryButtonDown()) {
                e.consume();
                Point2D p = visualizer.getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());
                creationStartX = p.getX();
                creationStartY = p.getY();

                tempCreationLayer = new ShapeLayer(creationType, creationFill, creationStroke, creationStrokeWidth);
                tempCreationLayer.setTranslateX(creationStartX);
                tempCreationLayer.setTranslateY(creationStartY);
                tempCreationLayer.setSize(10, 10);
                visualizer.getContentGroup().getChildren().add(tempCreationLayer);
            }
        };

        creationDragHandler = e -> {
            if (!isCreatingShape) return;

            if (creationType == ShapeType.CUSTOM_PATH && currentDragNode != null) {
                e.consume();
                Point2D p = visualizer.getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());

                currentDragNode.control2 = p;
                double dx = p.getX() - currentDragNode.anchor.getX();
                double dy = p.getY() - currentDragNode.anchor.getY();
                currentDragNode.control1 = new Point2D(currentDragNode.anchor.getX() - dx,
                        currentDragNode.anchor.getY() - dy);

                updateBezierPreview();
                return;
            }

            if (tempCreationLayer == null) return;
            e.consume();
            Point2D localPoint = visualizer.getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());
            double currentX = localPoint.getX();
            double currentY = localPoint.getY();

            double w = Math.abs(currentX - creationStartX);
            double h = Math.abs(currentY - creationStartY);
            double newX = Math.min(creationStartX, currentX);
            double newY = Math.min(creationStartY, currentY);

            tempCreationLayer.setTranslateX(newX);
            tempCreationLayer.setTranslateY(newY);
            tempCreationLayer.setSize(w, h);
        };

        creationReleaseHandler = e -> {
            if (!isCreatingShape) return;

            if (creationType == ShapeType.CUSTOM_PATH) {
                currentDragNode = null;
                return;
            }

            if (tempCreationLayer == null) return;
            e.consume();

            if (tempCreationLayer.getParent() != null) {
                if (tempCreationLayer.getParent() instanceof Pane) {
                    ((Pane) tempCreationLayer.getParent()).getChildren().remove(tempCreationLayer);
                } else if (tempCreationLayer.getParent() instanceof Group) {
                    ((Group) tempCreationLayer.getParent()).getChildren().remove(tempCreationLayer);
                }
            }

            if (tempCreationLayer.getBoundsInLocal().getWidth() > 5 || tempCreationLayer.getBoundsInLocal().getHeight() > 5) {
                visualizer.getLayerFactory().addShapeLayer(tempCreationLayer);
                if (onCreationFinish != null) onCreationFinish.accept(tempCreationLayer);
            }

            tempCreationLayer = null;
            isCreatingShape = false;
            visualizer.getContentGroup().setCursor(Cursor.DEFAULT);
        };

        visualizer.addEventHandler(MouseEvent.MOUSE_PRESSED, creationPressHandler);
        visualizer.addEventHandler(MouseEvent.MOUSE_DRAGGED, creationDragHandler);
        visualizer.addEventHandler(MouseEvent.MOUSE_RELEASED, creationReleaseHandler);
    }

    private void updateBezierPreview() {
        if (bezierNodes == null || bezierNodes.isEmpty() || creationPreviewPath == null) return;

        creationPreviewPath.getElements().clear();
        handleGroup.getChildren().clear();

        Point2D start = bezierNodes.get(0).anchor;
        creationPreviewPath.getElements().add(new javafx.scene.shape.MoveTo(start.getX(), start.getY()));

        for (int i = 0; i < bezierNodes.size() - 1; i++) {
            BezierNode n1 = bezierNodes.get(i);
            BezierNode n2 = bezierNodes.get(i + 1);
            creationPreviewPath.getElements().add(new javafx.scene.shape.CubicCurveTo(
                    n1.control2.getX(), n1.control2.getY(),
                    n2.control1.getX(), n2.control1.getY(),
                    n2.anchor.getX(), n2.anchor.getY()));
        }

        for (BezierNode n : bezierNodes) {
            Rectangle rect = new Rectangle(n.anchor.getX() - 3, n.anchor.getY() - 3, 6, 6);
            rect.setArcWidth(1.5); rect.setArcHeight(1.5);
            rect.setFill(Color.web("#0078D7")); rect.setStroke(Color.WHITE); rect.setStrokeWidth(1);
            handleGroup.getChildren().add(rect);

            if (!n.control1.equals(n.anchor)) {
                Line l = new Line(n.anchor.getX(), n.anchor.getY(), n.control1.getX(), n.control1.getY());
                l.setStroke(Color.web("#0078D7", 0.6)); l.setStrokeWidth(1);
                Circle c = new Circle(n.control1.getX(), n.control1.getY(), 2.5);
                c.setFill(Color.WHITE); c.setStroke(Color.web("#0078D7")); c.setStrokeWidth(1);
                handleGroup.getChildren().addAll(l, c);
            }
            if (!n.control2.equals(n.anchor)) {
                Line l = new Line(n.anchor.getX(), n.anchor.getY(), n.control2.getX(), n.control2.getY());
                l.setStroke(Color.web("#0078D7", 0.6)); l.setStrokeWidth(1);
                Circle c = new Circle(n.control2.getX(), n.control2.getY(), 2.5);
                c.setFill(Color.WHITE); c.setStroke(Color.web("#0078D7")); c.setStrokeWidth(1);
                handleGroup.getChildren().addAll(l, c);
            }
        }
    }

    private void finishBezierShape(boolean closed) {
        if (bezierNodes == null || bezierNodes.size() < 2) {
            cancelShapeCreation();
            return;
        }

        visualizer.getContentGroup().getChildren().removeAll(creationPreviewPath, handleGroup);
        creationPreviewPath = null;
        handleGroup = null;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (BezierNode n : bezierNodes) {
            minX = Math.min(minX, Math.min(n.anchor.getX(), Math.min(n.control1.getX(), n.control2.getX())));
            minY = Math.min(minY, Math.min(n.anchor.getY(), Math.min(n.control1.getY(), n.control2.getY())));
            maxX = Math.max(maxX, Math.max(n.anchor.getX(), Math.max(n.control1.getX(), n.control2.getX())));
            maxY = Math.max(maxY, Math.max(n.anchor.getY(), Math.max(n.control1.getY(), n.control2.getY())));
        }

        double w = Math.max(maxX - minX, 2);
        double h = Math.max(maxY - minY, 2);

        StringBuilder sb = new StringBuilder();
        Point2D start = bezierNodes.get(0).anchor;
        sb.append("M ").append(format(start.getX() - minX)).append(",").append(format(start.getY() - minY));

        for (int i = 0; i < bezierNodes.size() - 1; i++) {
            BezierNode n1 = bezierNodes.get(i); BezierNode n2 = bezierNodes.get(i + 1);
            sb.append(" C ")
                    .append(format(n1.control2.getX() - minX)).append(",").append(format(n1.control2.getY() - minY)).append(" ")
                    .append(format(n2.control1.getX() - minX)).append(",").append(format(n2.control1.getY() - minY)).append(" ")
                    .append(format(n2.anchor.getX() - minX)).append(",").append(format(n2.anchor.getY() - minY));
        }
        if (closed) sb.append(" Z");

        ShapeLayer layer = new ShapeLayer(ShapeType.CUSTOM_PATH, creationFill, creationStroke, creationStrokeWidth);
        layer.setTranslateX(minX); layer.setTranslateY(minY); layer.setSize(w, h);
        layer.setSvgPathData(sb.toString()); layer.setIsClosed(closed);

        List<BezierNode> storedNodes = new ArrayList<>();
        for (BezierNode n : bezierNodes) {
            storedNodes.add(new BezierNode(
                    new Point2D(n.anchor.getX() - minX, n.anchor.getY() - minY),
                    new Point2D(n.control1.getX() - minX, n.control1.getY() - minY),
                    new Point2D(n.control2.getX() - minX, n.control2.getY() - minY)));
        }
        layer.setBezierNodes(storedNodes);

        visualizer.getLayerFactory().addShapeLayer(layer);
        bezierNodes = null; isCreatingShape = false;
        visualizer.getContentGroup().setCursor(Cursor.DEFAULT);

        if (visualizer.getShapeManagerController() != null) {
            visualizer.getShapeManagerController().deselectAllShapeTools();
        }

        if (onCreationFinish != null) onCreationFinish.accept(layer);
    }
}
