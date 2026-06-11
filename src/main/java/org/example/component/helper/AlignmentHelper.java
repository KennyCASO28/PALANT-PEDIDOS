package org.example.component.helper;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import org.example.component.ShapeLayer;
import org.example.component.ImageLayer;
import org.example.component.TextLayer;

import java.util.List;

/**
 * Helper class to align multiple nodes relative to each other.
 * Supports: Left, Center (Horz), Right, Top, Middle (Vert), Bottom.
 */
public class AlignmentHelper {

    // --- Horizontal Alignment ---

    // --- Horizontal Alignment ---

    public static void alignLeft(List<Node> nodes, Node anchor) {
        if (nodes == null || nodes.size() < 2)
            return;

        double targetMinX;

        if (anchor != null) {
            Bounds b = anchor.getBoundsInParent();
            targetMinX = b.getMinX();
        } else {
            // Classic behavior: Find min X of selection
            double minX = Double.MAX_VALUE;
            for (Node n : nodes) {
                Bounds b = n.getBoundsInParent();
                if (b.getMinX() < minX)
                    minX = b.getMinX();
            }
            targetMinX = minX;
        }

        for (Node n : nodes) {
            if (n == anchor)
                continue; // Don't move the anchor
            Bounds b = n.getBoundsInParent();
            double offset = targetMinX - b.getMinX();
            n.setTranslateX(n.getTranslateX() + offset);
        }
    }

    public static void alignCenterHorizontal(List<Node> nodes, Node anchor) {
        if (nodes == null || nodes.size() < 2 || anchor == null)
            return;

        // Get target center in Scene coordinates
        javafx.geometry.Bounds anchorBounds = anchor.localToScene(anchor.getBoundsInLocal());
        double targetCenterX = anchorBounds.getMinX() + anchorBounds.getWidth() / 2.0;

        for (Node n : nodes) {
            if (n == anchor) continue;
            
            // Get current center in Scene coordinates
            javafx.geometry.Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());
            double currentCenterX = nodeBounds.getMinX() + nodeBounds.getWidth() / 2.0;
            
            // Calculate delta in Scene space
            double deltaX = targetCenterX - currentCenterX;
            
            // Convert delta to Node's Parent space
            if (n.getParent() != null) {
                // To get the equivalent delta in parent space, we project a line
                javafx.geometry.Point2D p0 = n.getParent().sceneToLocal(0, 0);
                javafx.geometry.Point2D p1 = n.getParent().sceneToLocal(deltaX, 0);
                if (p0 != null && p1 != null) {
                    double parentDeltaX = p1.getX() - p0.getX();
                    n.setTranslateX(n.getTranslateX() + parentDeltaX);
                }
            } else {
                n.setTranslateX(n.getTranslateX() + deltaX);
            }
        }
    }

    public static void alignMiddleVertical(List<Node> nodes, Node anchor) {
        if (nodes == null || nodes.size() < 2 || anchor == null)
            return;

        // Get target center in Scene coordinates
        javafx.geometry.Bounds anchorBounds = anchor.localToScene(anchor.getBoundsInLocal());
        double targetCenterY = anchorBounds.getMinY() + anchorBounds.getHeight() / 2.0;

        for (Node n : nodes) {
            if (n == anchor) continue;
            
            // Get current center in Scene coordinates
            javafx.geometry.Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());
            double currentCenterY = nodeBounds.getMinY() + nodeBounds.getHeight() / 2.0;
            
            // Calculate delta in Scene space
            double deltaY = targetCenterY - currentCenterY;
            
            // Convert delta to Node's Parent space
            if (n.getParent() != null) {
                javafx.geometry.Point2D p0 = n.getParent().sceneToLocal(0, 0);
                javafx.geometry.Point2D p1 = n.getParent().sceneToLocal(0, deltaY);
                if (p0 != null && p1 != null) {
                    double parentDeltaY = p1.getY() - p0.getY();
                    n.setTranslateY(n.getTranslateY() + parentDeltaY);
                }
            } else {
                n.setTranslateY(n.getTranslateY() + deltaY);
            }
        }
    }
}

