package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import org.example.model.BezierNode;
import org.example.model.ShapeType;

import java.util.List;

/**
 * Responsible for creating JavaFX shape nodes and applying geometry.
 * Handles the mapping between ShapeType enum and actual JavaFX Shape instances.
 */
public final class ShapeGeometryEngine {

    private ShapeGeometryEngine() {} // Utility class

    /**
     * Factory method: creates the appropriate JavaFX shape based on the type.
     */
    public static Shape createShapeNode(ShapeType type) {
        return switch (type) {
            case RECTANGLE -> new Rectangle();
            case CIRCLE -> new Ellipse();
            case TRIANGLE, STAR, PENTAGON, HEXAGON -> new Polygon();
            case CUSTOM_PATH -> new SVGPath();
            default -> new Rectangle();
        };
    }

    /**
     * Applies all the geometric properties (width, height, arc, etc.) to a Shape node.
     */
    public static void applyGeometry(Shape shape, ShapeType type, List<BezierNode> bezierNodes, 
                                       double width, double height, double visualMinX, double visualMinY,
                                       double arcWidth, double arcHeight, String svgPathData,
                                       Shear shearTransform) {
        if (shape instanceof Rectangle r) {
            r.setX(visualMinX); 
            r.setY(visualMinY); 
            r.setWidth(width); 
            r.setHeight(height);
            r.setArcWidth(arcWidth); 
            r.setArcHeight(arcHeight);
        } else if (shape instanceof Ellipse e) {
            e.setCenterX(visualMinX + width / 2.0);
            e.setCenterY(visualMinY + height / 2.0);
            e.setRadiusX(width / 2.0);
            e.setRadiusY(height / 2.0);
        } else if (shape instanceof Circle c) {
            double radius = Math.min(width, height) / 2;
            c.setCenterX(visualMinX + width / 2); 
            c.setCenterY(visualMinY + height / 2); 
            c.setRadius(radius);
        } else if (shape instanceof Polygon p) {
            ShapePathSupport.updatePolygonPoints(p, type, null, width, height, visualMinX, visualMinY);
        } else if (shape instanceof SVGPath svg) {
            double shX = shearTransform.getX();
            double shY = shearTransform.getY();
            if (shX != 0 || shY != 0) {
                double px = shearTransform.getPivotX();
                double py = shearTransform.getPivotY();
                svg.setContent(buildShearedSvgPath(bezierNodes, true, shX, shY, px, py));
            } else {
                if (svgPathData != null) svg.setContent(svgPathData);
            }
            svg.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
            svg.setScaleX(1); 
            svg.setScaleY(1);
        }
    }

    private static String buildShearedSvgPath(List<BezierNode> nodes, boolean isClosed, double shX, double shY, double px, double py) {
        if (nodes == null || nodes.isEmpty()) return "";
        List<BezierNode> shearedNodes = new java.util.ArrayList<>();
        for (BezierNode n : nodes) {
            BezierNode sn = n.copy();
            sn.anchor = shearPoint(sn.anchor, shX, shY, px, py);
            if (sn.control1 != null) sn.control1 = shearPoint(sn.control1, shX, shY, px, py);
            if (sn.control2 != null) sn.control2 = shearPoint(sn.control2, shX, shY, px, py);
            shearedNodes.add(sn);
        }
        return ShapePathSupport.buildSvgPath(shearedNodes, isClosed);
    }

    private static Point2D shearPoint(Point2D p, double shX, double shY, double px, double py) {
        double newX = p.getX() + shX * (p.getY() - py);
        double newY = p.getY() + shY * (p.getX() - px);
        return new Point2D(newX, newY);
    }
}
