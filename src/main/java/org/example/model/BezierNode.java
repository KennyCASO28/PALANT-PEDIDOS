package org.example.model;

import javafx.geometry.Point2D;

/**
 * Representation of a Bezier Path Node.
 * Used for Custom Path editing in Vector Shapes.
 */
public class BezierNode {
    public enum NodeType {
        CUSP, SMOOTH, SYMMETRICAL
    }

    public enum SegmentType {
        LINE, CURVE
    }

    public Point2D anchor;
    public Point2D control1; // Incoming (from previous)
    public Point2D control2; // Outgoing (to next)
    public NodeType type = NodeType.CUSP;
    public SegmentType segmentType = SegmentType.CURVE; // Segment to NEXT node
    public boolean isMoveTo = false; // Indicates this node starts a new subpath

    public BezierNode(Point2D anchor) {
        this.anchor = anchor;
        this.control1 = anchor;
        this.control2 = anchor;
    }

    public BezierNode(Point2D anchor, Point2D c1, Point2D c2) {
        this.anchor = anchor;
        this.control1 = c1;
        this.control2 = c2;
    }

    public BezierNode copy() {
        BezierNode n = new BezierNode(new Point2D(anchor.getX(), anchor.getY()),
                new Point2D(control1.getX(), control1.getY()),
                new Point2D(control2.getX(), control2.getY()));
        n.type = this.type;
        n.segmentType = this.segmentType;
        n.isMoveTo = this.isMoveTo;
        return n;
    }
}

