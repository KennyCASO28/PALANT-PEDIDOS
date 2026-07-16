package org.example.model;

import javafx.geometry.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a geometric path for text to follow.
 * Supports different types of interpolation (Linear, Curved, Circular).
 */
public class TrajectoryPath implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        STRAIGHT,
        ARC,
        CIRCLE,
        BEZIER,
        WAVE
    }

    private Type type = Type.STRAIGHT;
    private final List<Point2D> controlPoints = new ArrayList<>();
    
    // Configurable parameters
    private double curvature = 0.5; 
    private double spacing = 0.0;
    private boolean autoRotate = true;

    public TrajectoryPath(Type type) {
        this.type = type;
        initializeDefaultPoints();
    }

    private void initializeDefaultPoints() {
        controlPoints.clear();
        switch (type) {
            case STRAIGHT:
                controlPoints.add(new Point2D(-150, 0));
                controlPoints.add(new Point2D(150, 0));
                break;
            case ARC:
                controlPoints.add(new Point2D(-150, 50));
                controlPoints.add(new Point2D(0, -50)); // Control point for curvature
                controlPoints.add(new Point2D(150, 50));
                break;
            case CIRCLE:
                controlPoints.add(new Point2D(0, 0)); // Center
                controlPoints.add(new Point2D(100, 0)); // Radius handle
                break;
            case BEZIER:
                controlPoints.add(new Point2D(-150, 0));
                controlPoints.add(new Point2D(-75, -100));
                controlPoints.add(new Point2D(75, 100));
                controlPoints.add(new Point2D(150, 0));
                break;
            case WAVE:
                controlPoints.add(new Point2D(-150, 0));
                controlPoints.add(new Point2D(150, 0));
                break;
        }
    }

    public Type getType() { return type; }
    public void setType(Type type) { 
        this.type = type; 
        initializeDefaultPoints();
    }

    public List<Point2D> getControlPoints() { return controlPoints; }
    
    public double getCurvature() { return curvature; }
    public void setCurvature(double curvature) { this.curvature = curvature; }

    public double getSpacing() { return spacing; }
    public void setSpacing(double spacing) { this.spacing = spacing; }

    public boolean isAutoRotate() { return autoRotate; }
    public void setAutoRotate(boolean autoRotate) { this.autoRotate = autoRotate; }

    public Point2D getPointAt(double t) {
        if (controlPoints.isEmpty()) return Point2D.ZERO;
        
        switch (type) {
            case STRAIGHT:
                return lerp(controlPoints.get(0), controlPoints.get(1), t);
            case ARC:
                return calculateArcPoint(t);
            case CIRCLE:
                return calculateCirclePoint(t);
            case BEZIER:
                if (controlPoints.size() == 4) return calculateCubicBezier(t);
                return Point2D.ZERO;
            case WAVE:
                Point2D p0 = controlPoints.get(0);
                Point2D p1 = controlPoints.get(1);
                Point2D base = lerp(p0, p1, t);
                double waveY = Math.sin(t * 2 * Math.PI) * (curvature * 100);
                return new Point2D(base.getX(), base.getY() + waveY);
            default:
                return Point2D.ZERO;
        }
    }

    private Point2D lerp(Point2D p0, Point2D p1, double t) {
        return new Point2D(
            p0.getX() + (p1.getX() - p0.getX()) * t,
            p0.getY() + (p1.getY() - p0.getY()) * t
        );
    }

    private Point2D calculateArcPoint(double t) {
        Point2D p1 = controlPoints.get(0);
        Point2D p2 = controlPoints.get(1);
        Point2D p3 = controlPoints.get(2);

        double x1 = p1.getX(), y1 = p1.getY();
        double x2 = p2.getX(), y2 = p2.getY();
        double x3 = p3.getX(), y3 = p3.getY();

        double d = 2 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
        
        if (Math.abs(d) < 0.0001) {
            Point2D q0 = lerp(p1, p2, t);
            Point2D q1 = lerp(p2, p3, t);
            return lerp(q0, q1, t);
        }

        double ux = ((x1 * x1 + y1 * y1) * (y2 - y3) + (x2 * x2 + y2 * y2) * (y3 - y1) + (x3 * x3 + y3 * y3) * (y1 - y2)) / d;
        double uy = ((x1 * x1 + y1 * y1) * (x3 - x2) + (x2 * x2 + y2 * y2) * (x1 - x3) + (x3 * x3 + y3 * y3) * (x2 - x1)) / d;

        Point2D center = new Point2D(ux, uy);
        double radius = center.distance(p1);

        double startAngle = Math.atan2(y1 - uy, x1 - ux);
        double midAngle = Math.atan2(y2 - uy, x2 - ux);
        double endAngle = Math.atan2(y3 - uy, x3 - ux);

        double normMid = (midAngle - startAngle) % (2 * Math.PI);
        if (normMid < 0) normMid += 2 * Math.PI;
        
        double normEnd = (endAngle - startAngle) % (2 * Math.PI);
        if (normEnd < 0) normEnd += 2 * Math.PI;
        
        double sweep;
        if (normMid > normEnd) {
            sweep = normEnd - 2 * Math.PI;
        } else {
            sweep = normEnd;
        }

        double currentAngle = startAngle + t * sweep;

        return new Point2D(
                center.getX() + radius * Math.cos(currentAngle),
                center.getY() + radius * Math.sin(currentAngle)
        );
    }

    private Point2D calculateCirclePoint(double t) {
        Point2D center = controlPoints.get(0);
        Point2D radiusHandle = controlPoints.get(1);
        double radius = center.distance(radiusHandle);
        
        // Start from top (-90 degrees)
        double angle = Math.toRadians(-90 + (t * 360));
        return new Point2D(
            center.getX() + Math.cos(angle) * radius,
            center.getY() + Math.sin(angle) * radius
        );
    }

    private Point2D calculateCubicBezier(double t) {
        Point2D p0 = controlPoints.get(0);
        Point2D p1 = controlPoints.get(1);
        Point2D p2 = controlPoints.get(2);
        Point2D p3 = controlPoints.get(3);
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;
        double x = uuu * p0.getX() + 3 * uu * t * p1.getX() + 3 * u * tt * p2.getX() + ttt * p3.getX();
        double y = uuu * p0.getY() + 3 * uu * t * p1.getY() + 3 * u * tt * p2.getY() + ttt * p3.getY();
        return new Point2D(x, y);
    }

    public double getTotalLength() {
        if (controlPoints.isEmpty()) return 0;
        switch (type) {
            case STRAIGHT:
                return controlPoints.get(0).distance(controlPoints.get(1));
            case CIRCLE:
                return 2 * Math.PI * controlPoints.get(0).distance(controlPoints.get(1));
            case ARC:
            case BEZIER:
            case WAVE:
                double length = 0;
                Point2D last = getPointAt(0);
                for (double t = 0.05; t <= 1.0; t += 0.05) {
                    Point2D curr = getPointAt(t);
                    length += last.distance(curr);
                    last = curr;
                }
                return length;
            default:
                return 0;
        }
    }

    /**
     * Scales all control points proportionally.
     * @param sx Horizontal scale factor
     * @param sy Vertical scale factor
     */
    public void scalePoints(double sx, double sy) {
        for (int i = 0; i < controlPoints.size(); i++) {
            Point2D p = controlPoints.get(i);
            controlPoints.set(i, new Point2D(p.getX() * sx, p.getY() * sy));
        }
    }

    /**
     * Creates a deep copy of the trajectory state.
     */
    public TrajectoryPath copy() {
        TrajectoryPath clone = new TrajectoryPath(this.type);
        clone.setFrom(this);
        return clone;
    }

    /**
     * Restores state from another trajectory instance.
     */
    public void setFrom(TrajectoryPath other) {
        this.type = other.type;
        this.controlPoints.clear();
        for (Point2D p : other.controlPoints) {
            this.controlPoints.add(new Point2D(p.getX(), p.getY()));
        }
        this.curvature = other.curvature;
        this.spacing = other.spacing;
        this.autoRotate = other.autoRotate;
    }
}
