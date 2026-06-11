package org.example.component.helper;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import org.example.component.ShapeLayer;
import org.example.model.BezierNode;
import org.example.model.ShapeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates styling and complex rendering (Contours, Gradients) for ShapeLayer.
 */
public class ShapeStyleOrchestrator {

    private final ShapeLayer layer;
    private final Group contourGroup;

    public ShapeStyleOrchestrator(ShapeLayer layer, Group contourGroup) {
        this.layer = layer;
        this.contourGroup = contourGroup;
    }

    public void updateFill(Shape currentShapeNode, Color fillColor, boolean isGradientTransparency, 
                           double transparencyAngle, double transparencyStartAlpha, 
                           double transparencyEndAlpha, double transparencyBalance) {
        if (currentShapeNode == null) return;

        if (!isGradientTransparency) {
            currentShapeNode.setFill(fillColor != null ? fillColor : Color.TRANSPARENT);
        } else {
            double rad = Math.toRadians(transparencyAngle);
            double startX = 0.5 - 0.5 * Math.cos(rad);
            double startY = 0.5 - 0.5 * Math.sin(rad);
            double endX = 0.5 + 0.5 * Math.cos(rad);
            double endY = 0.5 + 0.5 * Math.sin(rad);

            Color c1 = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), transparencyStartAlpha);
            Color c2 = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), transparencyEndAlpha);
            Color cMid = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), (transparencyStartAlpha + transparencyEndAlpha) / 2.0);

            LinearGradient lg = new LinearGradient(startX, startY, endX, endY, true, CycleMethod.NO_CYCLE,
                    new Stop(0, c1),
                    new Stop(transparencyBalance, cMid),
                    new Stop(1, c2));
            currentShapeNode.setFill(lg);
        }
    }

    public void renderContour(int steps, double distance, Color color, StrokeLineJoin join, 
                              Shape currentShapeNode, Color fillColor, Color strokeColor, double strokeWidth) {
        contourGroup.getChildren().clear();
        if (steps <= 0 || distance <= 0 || currentShapeNode == null) return;

        double baseStroke = strokeWidth > 0 ? strokeWidth : 0;
        Color baseColor = (fillColor != null && !Color.TRANSPARENT.equals(fillColor)) ? fillColor
                        : (strokeColor != null ? strokeColor : Color.TRANSPARENT);

        for (int i = steps; i >= 1; i--) {
            Shape clone = createSilhouetteClone(currentShapeNode);
            if (clone == null) continue;

            double offset = distance * i;
            Color stepColor = baseColor.interpolate(color, (double) i / steps);

            clone.setFill(stepColor);
            clone.setStroke(stepColor);
            clone.setStrokeLineJoin(join);
            clone.setStrokeMiterLimit(2.0);
            clone.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            clone.setStrokeType(StrokeType.CENTERED);
            clone.setStrokeWidth(baseStroke + offset);
            clone.setMouseTransparent(true);

            contourGroup.getChildren().add(clone);
        }
    }

    private Shape createSilhouetteClone(Shape source) {
        Shape clone = null;
        if (source instanceof Rectangle) {
            Rectangle s = (Rectangle) source;
            Rectangle r = new Rectangle(s.getX(), s.getY(), s.getWidth(), s.getHeight());
            r.setArcWidth(s.getArcWidth());
            r.setArcHeight(s.getArcHeight());
            clone = r;
        } else if (source instanceof Circle) {
            Circle s = (Circle) source;
            clone = new Circle(s.getCenterX(), s.getCenterY(), s.getRadius());
        } else if (source instanceof Polygon) {
            Polygon p = new Polygon();
            p.getPoints().addAll(((Polygon) source).getPoints());
            clone = p;
        } else if (source instanceof SVGPath) {
            SVGPath s = (SVGPath) source;
            SVGPath svg = new SVGPath();
            svg.setContent(s.getContent());
            svg.setScaleX(s.getScaleX());
            svg.setScaleY(s.getScaleY());
            svg.setTranslateX(s.getTranslateX());
            svg.setTranslateY(s.getTranslateY());
            clone = svg;
        }
        return clone;
    }

    public List<ShapeLayer> separateContours(int steps, double distance, Color color, StrokeLineJoin join,
                                            ShapeType type, double width, double height, Color fillColor, 
                                            Color strokeColor, double strokeWidth, double arcWidth, double arcHeight,
                                            String svgPathData, List<BezierNode> nodes, List<Double> points,
                                            boolean isClosed) {
        List<ShapeLayer> newLayers = new ArrayList<>();
        if (steps <= 0 || distance <= 0) return newLayers;

        double baseStroke = strokeWidth > 0 ? strokeWidth : 1;
        Color baseColor = (fillColor != null && !Color.TRANSPARENT.equals(fillColor)) ? fillColor
                        : (strokeColor != null ? strokeColor : Color.TRANSPARENT);

        for (int i = 1; i <= steps; i++) {
            ShapeLayer nl = new ShapeLayer(type, Color.TRANSPARENT, color, baseStroke);
            nl.setTranslateX(layer.getTranslateX());
            nl.setTranslateY(layer.getTranslateY());
            nl.setRotate(layer.getRotate());
            nl.setScaleX(layer.getScaleX());
            nl.setScaleY(layer.getScaleY());
            nl.setWidth(width);
            nl.setHeight(height);

            if (type == ShapeType.RECTANGLE) {
                nl.setArcWidth(arcWidth);
                nl.setArcHeight(arcHeight);
            }

            if (type == ShapeType.CUSTOM_PATH) {
                nl.setSvgPathData(svgPathData);
                if (nodes != null) {
                    List<BezierNode> copiedNodes = new ArrayList<>();
                    for (BezierNode bn : nodes) copiedNodes.add(bn.copy());
                    nl.setBezierNodes(copiedNodes);
                }
            } else if (points != null) {
                nl.setCustomPoints(new ArrayList<>(points));
            }

            nl.refreshShapeVisuals();
            Color stepColor = baseColor.interpolate(color, (double) i / steps);
            nl.setFillColor(stepColor);
            nl.setStrokeColor(stepColor);
            nl.setStrokeWidth(baseStroke + (distance * i));
            
            newLayers.add(nl);
        }
        return newLayers;
    }
}
