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

        double baseStroke = strokeWidth > 0 ? strokeWidth : 1.5;
        Color baseCol = (color != null && !Color.TRANSPARENT.equals(color)) ? color : (strokeColor != null ? strokeColor : Color.web("#e74c3c"));

        Color[] stepPalette = new Color[]{
            baseCol,
            Color.web("#f1c40f"), // Gold / Yellow
            Color.web("#3498db"), // Blue
            Color.web("#e67e22"), // Orange
            Color.web("#2ecc71"), // Green
            Color.web("#9b59b6"), // Purple
            Color.WHITE,
            Color.BLACK
        };

        for (int i = steps; i >= 1; i--) {
            Shape clone = createSilhouetteClone(currentShapeNode);
            if (clone == null) continue;

            double offset = distance * i * 0.5;

            // Apply distinct high-contrast color per step so lines never unify
            Color stepCol = (steps > 1) ? stepPalette[(i - 1) % stepPalette.length] : baseCol;

            // ÚNICAMENTE TRAZO (sin relleno interior) con remate recto (BUTT) sin punta ovalada
            clone.setFill(null);
            clone.setStroke(stepCol);
            clone.setStrokeLineJoin(join != null ? join : StrokeLineJoin.MITER);
            clone.setStrokeMiterLimit(10.0);
            clone.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
            clone.setStrokeType(StrokeType.OUTSIDE);
            clone.setStrokeWidth(offset);
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
            svg.setFillRule(s.getFillRule());
            clone = svg;
        }
        if (clone != null && source != null) {
            clone.setLayoutX(source.getLayoutX());
            clone.setLayoutY(source.getLayoutY());
            clone.setTranslateX(source.getTranslateX());
            clone.setTranslateY(source.getTranslateY());
            clone.setScaleX(source.getScaleX());
            clone.setScaleY(source.getScaleY());
            clone.setRotate(source.getRotate());
            if (!source.getTransforms().isEmpty()) {
                clone.getTransforms().setAll(source.getTransforms());
            }
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

        Color baseCol = (color != null && !Color.TRANSPARENT.equals(color)) ? color
                      : (fillColor != null && !Color.TRANSPARENT.equals(fillColor)) ? fillColor
                      : (strokeColor != null ? strokeColor : Color.web("#e74c3c"));

        // Same palette as renderContour so visual matches exactly
        Color[] stepPalette = new Color[]{
            baseCol,
            Color.web("#f1c40f"), // Gold / Yellow
            Color.web("#3498db"), // Blue
            Color.web("#e67e22"), // Orange
            Color.web("#2ecc71"), // Green
            Color.web("#9b59b6"), // Purple
            Color.WHITE,
            Color.BLACK
        };

        // Create from outermost (step N) to innermost (step 1) so stacking order is correct
        for (int i = steps; i >= 1; i--) {
            Color stepColor = (steps > 1) ? stepPalette[(i - 1) % stepPalette.length] : baseCol;
            double stepDist = distance * i * 0.5;

            // Real solid vector — fill + OUTSIDE stroke, no contour overlay.
            // This makes each step behave as a normal editable vector shape.
            ShapeLayer nl = new ShapeLayer(type, stepColor, stepColor, stepDist);
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
            nl.setFillColor(stepColor);
            nl.setStrokeColor(stepColor);
            nl.setStrokeWidth(stepDist);
            nl.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
            nl.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
            nl.setContourLineJoin(join != null ? join : StrokeLineJoin.MITER);
            // NO applyContour — the shape IS the vector, not a contour overlay
            nl.applyContour(0, 0, Color.TRANSPARENT);
            nl.setUserLocked(false);

            newLayers.add(nl);
        }
        return newLayers;
    }
}
