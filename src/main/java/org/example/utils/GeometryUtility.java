package org.example.utils;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import java.util.List;
import java.util.Locale;

/**
 * Utility for geometric calculations and path generation.
 */
public class GeometryUtility {

    /**
     * Generates points for a regular polygon (Triangle, Pentagon, Hexagon, etc.)
     */
    public static void createRegularPolygon(Polygon poly, int sides, double cx, double cy, double rx, double ry) {
        double angleStep = 2 * Math.PI / sides;
        // Start from top (-PI/2)
        double startAngle = -Math.PI / 2;
        for (int i = 0; i < sides; i++) {
            double angle = startAngle + i * angleStep;
            double x = cx + rx * Math.cos(angle);
            double y = cy + ry * Math.sin(angle);
            poly.getPoints().addAll(x, y);
        }
    }

    /**
     * Generates points for a star shape.
     */
    public static void createStar(Polygon poly, int points, double cx, double cy, double rx, double ry) {
        double innerRx = rx * 0.4; // Inner radius
        double innerRy = ry * 0.4;
        double angleStep = Math.PI / points; // Half step for inner points
        double startAngle = -Math.PI / 2;

        for (int i = 0; i < points * 2; i++) {
            double rOuterX = (i % 2 == 0) ? rx : innerRx;
            double rOuterY = (i % 2 == 0) ? ry : innerRy;
            double angle = startAngle + i * angleStep;
            double x = cx + rOuterX * Math.cos(angle);
            double y = cy + rOuterY * Math.sin(angle);
            poly.getPoints().addAll(x, y);
        }
    }

    /**
     * Generates a smooth SVG path string from a list of points using Quadratic
     * curves.
     */
    public static String generateSmoothPath(List<Point2D> points) {
        if (points == null || points.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();

        // 1. Move to start
        Point2D start = points.get(0);
        sb.append("M ").append(format(start.getX())).append(",").append(format(start.getY()));

        if (points.size() < 3) {
            for (int i = 1; i < points.size(); i++) {
                sb.append(" L ").append(format(points.get(i).getX())).append(",").append(format(points.get(i).getY()));
            }
            return sb.toString();
        }

        // 2. Quadratic curve smoothing between midpoints
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D p1 = points.get(i);
            Point2D p2 = points.get(i + 1);
            double midX = (p1.getX() + p2.getX()) / 2;
            double midY = (p1.getY() + p2.getY()) / 2;

            if (i == 0) {
                // First point to first midpoint
                sb.append(" L ").append(format(midX)).append(",").append(format(midY));
            } else {
                // Quadratic curve from previous midpoint to current midpoint using p1 as
                // control
                sb.append(" Q ").append(format(p1.getX())).append(",").append(format(p1.getY()))
                        .append(" ").append(format(midX)).append(",").append(format(midY));
            }
        }

        // 3. Last segment to end
        Point2D last = points.get(points.size() - 1);
        sb.append(" L ").append(format(last.getX())).append(",").append(format(last.getY()));

        return sb.toString();
    }

    /**
     * Formats a double value for SVG (2 decimal places, US Locale).
     */
    public static String format(double val) {
        return String.format(Locale.US, "%.2f", val);
    }

    /**
     * Evaluates a point on a cubic bezier curve at parameter t.
     */
    public static Point2D evalCubicBezier(Point2D p0, Point2D p1, Point2D p2, Point2D p3, double t) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;

        double x = uuu * p0.getX() + 3 * uu * t * p1.getX() + 3 * u * tt * p2.getX() + ttt * p3.getX();
        double y = uuu * p0.getY() + 3 * uu * t * p1.getY() + 3 * u * tt * p2.getY() + ttt * p3.getY();

        return new Point2D(x, y);
    }

    /**
     * Re-scales a raw SVG Path String by a given factor to avoid affine transform
     * distortions on strokes.
     */
    public static String scaleSvgPathData(String path, double scaleX, double scaleY) {
        if (path == null || path.trim().isEmpty() || (scaleX == 1.0 && scaleY == 1.0)) {
            return path;
        }

        StringBuilder result = new StringBuilder();
        StringBuilder numberToken = new StringBuilder();
        boolean isX = true; // X comes first, then Y

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);

            // Is part of a number?
            if (Character.isDigit(c) || c == '.' || c == '-' || c == 'e' || c == 'E') {
                numberToken.append(c);
            } else {
                // If we finished reading a number, convert and scale it
                if (numberToken.length() > 0) {
                    try {
                        double val = Double.parseDouble(numberToken.toString());
                        // Apply Scale
                        double scaledVal = isX ? val * scaleX : val * scaleY;
                        result.append(format(scaledVal));
                        isX = !isX; // Alternate between X and Y
                    } catch (NumberFormatException ignored) {
                        result.append(numberToken.toString());
                    }
                    numberToken.setLength(0);
                }

                // If this characterizes a command letter, reset X/Y alternator
                if (Character.isLetter(c) && c != 'e' && c != 'E') {
                    isX = true; // Every new command usually starts expecting an X
                    // Commands like A (Arc) have multiple non-xy parameters, but for standard
                    // basic shapes M, L, C, Q, Z, coordinates travel in pairs.
                }

                result.append(c); // Append the space, comma, or letter
            }
        }

        // Flush last number if end of string
        if (numberToken.length() > 0) {
            try {
                double val = Double.parseDouble(numberToken.toString());
                double scaledVal = isX ? val * scaleX : val * scaleY;
                result.append(format(scaledVal));
            } catch (NumberFormatException ignored) {
                result.append(numberToken.toString());
            }
        }

        return result.toString();
    }

    /**
     * Counters the effect of shear on handles and UI elements to keep their
     * original shape.
     */
    public static void applyAntiShear(javafx.scene.Node node, javafx.scene.transform.Shear shear, double centerX,
            double centerY) {
        if (shear == null || (shear.getX() == 0 && shear.getY() == 0)) {
            node.getTransforms().removeIf(t -> t instanceof javafx.scene.transform.Shear || t instanceof javafx.scene.transform.Affine);
            return;
        }

        javafx.scene.transform.Affine antiShear = new javafx.scene.transform.Affine();
        try {
            javafx.scene.transform.Transform inverse = shear.createInverse();
            antiShear.appendTranslation(centerX, centerY);
            antiShear.append(
                inverse.getMxx(), inverse.getMxy(), 0,
                inverse.getMyx(), inverse.getMyy(), 0
            );
            antiShear.appendTranslation(-centerX, -centerY);
        } catch (Exception e) {
            return;
        }

        node.getTransforms().removeIf(t -> t instanceof javafx.scene.transform.Shear || t instanceof javafx.scene.transform.Affine);
        node.getTransforms().add(antiShear);
    }
}

