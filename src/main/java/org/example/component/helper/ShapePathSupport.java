package org.example.component.helper;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.example.model.BezierNode;
import org.example.model.ShapeType;
import static org.example.utils.GeometryUtility.format;

public final class ShapePathSupport {

    private ShapePathSupport() {
    }

    public static String buildSvgPath(List<BezierNode> bezierNodes, boolean isClosed) {
        if (bezierNodes == null || bezierNodes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        BezierNode currentSubpathStart = bezierNodes.get(0);
        sb.append("M ")
                .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getX()))
                .append(",")
                .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getY()));

        for (int i = 0; i < bezierNodes.size() - 1; i++) {
            BezierNode current = bezierNodes.get(i);
            BezierNode next = bezierNodes.get(i + 1);

            if (next.isMoveTo) {
                if (isClosed) {
                    if (current.segmentType == BezierNode.SegmentType.LINE) {
                        sb.append(" Z");
                    } else {
                        sb.append(" C ")
                                .append(org.example.utils.GeometryUtility.format(current.control2.getX()))
                                .append(",")
                                .append(org.example.utils.GeometryUtility.format(current.control2.getY()))
                                .append(" ")
                                .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getX()))
                                .append(",")
                                .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getY()))
                                .append(" ")
                                .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getX()))
                                .append(",")
                                .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getY()))
                                .append(" Z");
                    }
                }
                currentSubpathStart = next;
                sb.append(" M ")
                        .append(org.example.utils.GeometryUtility.format(next.anchor.getX()))
                        .append(",")
                        .append(org.example.utils.GeometryUtility.format(next.anchor.getY()));
                continue;
            }

            if (current.segmentType == BezierNode.SegmentType.LINE) {
                sb.append(" L ")
                        .append(org.example.utils.GeometryUtility.format(next.anchor.getX()))
                        .append(",")
                        .append(org.example.utils.GeometryUtility.format(next.anchor.getY()));
            } else {
                sb.append(" C ")
                        .append(org.example.utils.GeometryUtility.format(current.control2.getX()))
                        .append(",")
                        .append(org.example.utils.GeometryUtility.format(current.control2.getY()))
                        .append(" ")
                        .append(org.example.utils.GeometryUtility.format(next.control1.getX()))
                        .append(",")
                        .append(org.example.utils.GeometryUtility.format(next.control1.getY()))
                        .append(" ")
                        .append(org.example.utils.GeometryUtility.format(next.anchor.getX()))
                        .append(",")
                        .append(org.example.utils.GeometryUtility.format(next.anchor.getY()));
            }
        }

        if (isClosed && bezierNodes.size() > 1) {
            BezierNode last = bezierNodes.get(bezierNodes.size() - 1);
            if (last.segmentType == BezierNode.SegmentType.LINE) {
                sb.append(" Z");
            } else {
                sb.append(" C ")
                        .append(org.example.utils.GeometryUtility.format(last.control2.getX()))
                        .append(",")
                        .append(org.example.utils.GeometryUtility.format(last.control2.getY()))
                        .append(" ")
                        .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getX()))
                        .append(",")
                        .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getY()))
                        .append(" ")
                        .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getX()))
                        .append(",")
                        .append(org.example.utils.GeometryUtility.format(currentSubpathStart.anchor.getY()))
                        .append(" Z");
            }
        }

        return sb.toString();
    }

     public static BoundsData calculateBezierBounds(List<BezierNode> bezierNodes) {
        if (bezierNodes == null || bezierNodes.isEmpty()) {
            return null;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        // Base points: Anchors always define the minimum bounds.
        // This also covers closing-segment endpoints for free.
        for (BezierNode node : bezierNodes) {
            minX = Math.min(minX, node.anchor.getX());
            minY = Math.min(minY, node.anchor.getY());
            maxX = Math.max(maxX, node.anchor.getX());
            maxY = Math.max(maxY, node.anchor.getY());
        }

        // Precise Curve Extrema: iterate only REAL segments within each subpath.
        for (int i = 0; i < bezierNodes.size() - 1; i++) {
            BezierNode n1 = bezierNodes.get(i);
            BezierNode n2 = bezierNodes.get(i + 1);

            // Skip phantom cross-subpath segments: if n2 starts a new subpath (isMoveTo),
            // there is NO real curve between n1 and n2. Including it inflates bounds
            // with wrong control points → incorrect resize/deformation on welded shapes.
            if (n2.isMoveTo) continue;

            double[] bounds = getSegmentBounds(n1.anchor, n1.control2, n2.control1, n2.anchor);
            minX = Math.min(minX, bounds[0]);
            minY = Math.min(minY, bounds[1]);
            maxX = Math.max(maxX, bounds[2]);
            maxY = Math.max(maxY, bounds[3]);
        }

        if (minX == Double.MAX_VALUE) return null;
        return new BoundsData(minX, minY, maxX - minX, maxY - minY);
    }


    public static Point2D normalizeNodes(List<BezierNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return Point2D.ZERO;
        BoundsData bounds = calculateBezierBounds(nodes);
        if (bounds == null) return Point2D.ZERO;
        
        double mx = bounds.getMinX();
        double my = bounds.getMinY();
        
        // Only normalize if there's a significant offset (to avoid float drift)
        if (Math.abs(mx) < 0.001 && Math.abs(my) < 0.001) return Point2D.ZERO;

        for (BezierNode n : nodes) {
            n.anchor = new Point2D(n.anchor.getX() - mx, n.anchor.getY() - my);
            n.control1 = new Point2D(n.control1.getX() - mx, n.control1.getY() - my);
            n.control2 = new Point2D(n.control2.getX() - mx, n.control2.getY() - my);
        }
        return new Point2D(mx, my);
    }

    private static double[] getSegmentBounds(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        double minX = Math.min(p0.getX(), p3.getX());
        double minY = Math.min(p0.getY(), p3.getY());
        double maxX = Math.max(p0.getX(), p3.getX());
        double maxY = Math.max(p0.getY(), p3.getY());

        // Find extrema for X and Y independently
        double[] tx = findExtrema(p0.getX(), p1.getX(), p2.getX(), p3.getX());
        double[] ty = findExtrema(p0.getY(), p1.getY(), p2.getY(), p3.getY());

        for (double t : tx) {
            if (t > 0 && t < 1) {
                double x = evalBezier(p0.getX(), p1.getX(), p2.getX(), p3.getX(), t);
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            }
        }
        for (double t : ty) {
            if (t > 0 && t < 1) {
                double y = evalBezier(p0.getY(), p1.getY(), p2.getY(), p3.getY(), t);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
        }

        return new double[]{minX, minY, maxX, maxY};
    }

    private static double[] findExtrema(double p0, double p1, double p2, double p3) {
        // Derivative of cubic Bezier is quadratic: at^2 + bt + c = 0
        double a = 3 * (-p0 + 3 * p1 - 3 * p2 + p3);
        double b = 6 * (p0 - 2 * p1 + p2);
        double c = 3 * (p1 - p0);

        if (Math.abs(a) < 1e-10) { // Practically linear/quadratic
            if (Math.abs(b) < 1e-10) return new double[0];
            return new double[]{-c / b};
        }

        double det = b * b - 4 * a * c;
        if (det < 0) return new double[0];
        if (det == 0) return new double[]{-b / (2 * a)};
        
        double sqrtDet = Math.sqrt(det);
        return new double[]{(-b + sqrtDet) / (2 * a), (-b - sqrtDet) / (2 * a)};
    }

    private static double evalBezier(double p0, double p1, double p2, double p3, double t) {
        double mt = 1 - t;
        return mt * mt * mt * p0 + 3 * mt * mt * t * p1 + 3 * mt * t * t * p2 + t * t * t * p3;
    }

    public static void updatePolygonPoints(
            Polygon poly,
            ShapeType type,
            List<Double> customPoints,
            double width,
            double height,
            double minX,
            double minY) {
        if (poly == null) {
            return;
        }

        poly.getPoints().clear();
        double cx = minX + width / 2;
        double cy = minY + height / 2;
        double rx = width / 2;
        double ry = height / 2;

        boolean needsNormalization = false;

        if (type == ShapeType.CUSTOM_PATH) {
            if (customPoints != null) {
                for (int i = 0; i < customPoints.size(); i += 2) {
                    double nx = customPoints.get(i);
                    double ny = customPoints.get(i + 1);
                    poly.getPoints().addAll(nx * width, ny * height);
                }
            }
        } else if (type == ShapeType.TRIANGLE) {
            poly.getPoints().addAll(
                    cx, minY,
                    minX + width, minY + height,
                    minX, minY + height);
        } else if (type == ShapeType.PENTAGON) {
            org.example.utils.GeometryUtility.createRegularPolygon(poly, 5, cx, cy, rx, ry);
            needsNormalization = true;
        } else if (type == ShapeType.HEXAGON) {
            org.example.utils.GeometryUtility.createRegularPolygon(poly, 6, cx, cy, rx, ry);
            needsNormalization = true;
        } else if (type == ShapeType.STAR) {
            org.example.utils.GeometryUtility.createStar(poly, 5, cx, cy, rx, ry);
            needsNormalization = true;
        }

        if (needsNormalization && !poly.getPoints().isEmpty()) {
            List<Double> points = poly.getPoints();
            double curMinX = Double.MAX_VALUE;
            double curMaxX = -Double.MAX_VALUE;
            double curMinY = Double.MAX_VALUE;
            double curMaxY = -Double.MAX_VALUE;

            for (int i = 0; i < points.size(); i += 2) {
                double x = points.get(i);
                double y = points.get(i + 1);
                if (x < curMinX) curMinX = x;
                if (x > curMaxX) curMaxX = x;
                if (y < curMinY) curMinY = y;
                if (y > curMaxY) curMaxY = y;
            }

            double currentWidth = curMaxX - curMinX;
            double currentHeight = curMaxY - curMinY;

            if (currentWidth > 0 && currentHeight > 0) {
                for (int i = 0; i < points.size(); i += 2) {
                    double x = points.get(i);
                    double y = points.get(i + 1);
                    double nx = (x - curMinX) / currentWidth;
                    double ny = (y - curMinY) / currentHeight;
                    points.set(i, nx * width + minX);
                    points.set(i + 1, ny * height + minY);
                }
            }
        }
    }

    public static PathConversionResult convertPrimitiveToPath(
            Shape currentShapeNode,
            double width,
            double height,
            double arcWidth,
            double arcHeight,
            boolean isClosed) {
        List<BezierNode> nodes = new ArrayList<>();

        if (currentShapeNode instanceof Rectangle) {
            double aw = Math.min(width / 2.0, arcWidth / 2.0);
            double ah = Math.min(height / 2.0, arcHeight / 2.0);

            if (aw > 0 && ah > 0) {
                double kappa = 0.5522847498;
                double ox = aw * kappa;
                double oy = ah * kappa;

                nodes.add(new BezierNode(new Point2D(aw, 0), new Point2D(aw - ox, 0), new Point2D(aw, 0)));
                nodes.add(new BezierNode(new Point2D(width - aw, 0), new Point2D(width - aw, 0), new Point2D(width - aw + ox, 0)));
                nodes.add(new BezierNode(new Point2D(width, ah), new Point2D(width, ah - oy), new Point2D(width, ah)));
                nodes.add(new BezierNode(
                        new Point2D(width, height - ah),
                        new Point2D(width, height - ah),
                        new Point2D(width, height - ah + oy)));
                nodes.add(new BezierNode(
                        new Point2D(width - aw, height),
                        new Point2D(width - aw + ox, height),
                        new Point2D(width - aw, height)));
                nodes.add(new BezierNode(new Point2D(aw, height), new Point2D(aw, height), new Point2D(aw - ox, height)));
                nodes.add(new BezierNode(
                        new Point2D(0, height - ah),
                        new Point2D(0, height - ah + oy),
                        new Point2D(0, height - ah)));
                nodes.add(new BezierNode(new Point2D(0, ah), new Point2D(0, ah), new Point2D(0, ah - oy)));
            } else {
                nodes.add(createLinearNode(0, 0));
                nodes.add(createLinearNode(width, 0));
                nodes.add(createLinearNode(width, height));
                nodes.add(createLinearNode(0, height));
            }
        } else if (currentShapeNode instanceof Polygon polygon) {
            List<Double> points = polygon.getPoints();
            for (int i = 0; i < points.size(); i += 2) {
                nodes.add(createLinearNode(points.get(i), points.get(i + 1)));
            }
        } else if (currentShapeNode instanceof Circle) {
            double rx = width / 2;
            double ry = height / 2;
            double cx = width / 2;
            double cy = height / 2;
            double kappa = 0.5522847498;
            double ox = rx * kappa;
            double oy = ry * kappa;

            nodes.add(new BezierNode(
                    new Point2D(cx, cy - ry),
                    new Point2D(cx - ox, cy - ry),
                    new Point2D(cx + ox, cy - ry)));
            nodes.add(new BezierNode(
                    new Point2D(cx + rx, cy),
                    new Point2D(cx + rx, cy - oy),
                    new Point2D(cx + rx, cy + oy)));
            nodes.add(new BezierNode(
                    new Point2D(cx, cy + ry),
                    new Point2D(cx + ox, cy + ry),
                    new Point2D(cx - ox, cy + ry)));
            nodes.add(new BezierNode(
                    new Point2D(cx - rx, cy),
                    new Point2D(cx - rx, cy + oy),
                    new Point2D(cx - rx, cy - oy)));
        }

        if (nodes.isEmpty()) {
            return null;
        }

        BoundsData bounds = calculateBezierBounds(nodes);
        return new PathConversionResult(
                nodes,
                copyNodes(nodes),
                buildSvgPath(nodes, isClosed),
                bounds);
    }

    public static List<BezierNode> copyNodes(List<BezierNode> source) {
        if (source == null) {
            return null;
        }
        List<BezierNode> copy = new ArrayList<>();
        for (BezierNode node : source) {
            copy.add(node.copy());
        }
        return copy;
    }

    public static void scaleNodes(List<BezierNode> bezierNodes, double drX, double drY) {
        if (bezierNodes == null) {
            return;
        }
        for (BezierNode node : bezierNodes) {
            node.anchor = new Point2D(node.anchor.getX() * drX, node.anchor.getY() * drY);
            node.control1 = new Point2D(node.control1.getX() * drX, node.control1.getY() * drY);
            node.control2 = new Point2D(node.control2.getX() * drX, node.control2.getY() * drY);
        }
    }

    private static BezierNode createLinearNode(double x, double y) {
        Point2D p = new Point2D(x, y);
        return new BezierNode(p, p, p);
    }

    public static final class BoundsData {
        private final double minX;
        private final double minY;
        private final double width;
        private final double height;

        public BoundsData(double minX, double minY, double width, double height) {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
        }

        public double getMinX() {
            return minX;
        }

        public double getMinY() {
            return minY;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }
    }

    public static final class PathConversionResult {
        private final List<BezierNode> nodes;
        private final List<BezierNode> originalNodes;
        private final String svgPathData;
        private final BoundsData bounds;

        public PathConversionResult(
                List<BezierNode> nodes,
                List<BezierNode> originalNodes,
                String svgPathData,
                BoundsData bounds) {
            this.nodes = nodes;
            this.originalNodes = originalNodes;
            this.svgPathData = svgPathData;
            this.bounds = bounds;
        }

        public List<BezierNode> getNodes() {
            return nodes;
        }

        public List<BezierNode> getOriginalNodes() {
            return originalNodes;
        }

        public String getSvgPathData() {
            return svgPathData;
        }

        public BoundsData getBounds() {
            return bounds;
        }
    }
}
