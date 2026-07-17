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

    // --- Safe Bounds Extractors ---
    private static javafx.geometry.Point2D getCenterInScene(Node n) {
        if (n instanceof org.example.component.GroupLayerV2) {
            org.example.component.GroupLayerV2 g2 = (org.example.component.GroupLayerV2) n;
            Bounds cb = g2.calculateBounds();
            double cx = cb.getMinX() + cb.getWidth() / 2.0;
            double cy = cb.getMinY() + cb.getHeight() / 2.0;
            // calculateBounds() usa coordenadas del contentGroup (no del GroupLayerV2 mismo).
            // Debemos convertir desde contentGroup.localToScene para que la rotación/escala
            // del contentGroup (rotate/scale/shear transforms) sea correctamente considerada.
            return g2.getContentGroup().localToScene(cx, cy);
        } else if (n instanceof org.example.component.GroupLayer) {
            org.example.component.GroupLayer gl = (org.example.component.GroupLayer) n;
            double cx = gl.getBoundsMinX() + gl.getLogicalWidth() / 2.0;
            double cy = gl.getBoundsMinY() + gl.getLogicalHeight() / 2.0;
            return gl.localToScene(cx, cy);
        } else if (n instanceof org.example.component.ShapeLayer) {
            org.example.component.ShapeLayer sl = (org.example.component.ShapeLayer) n;
            double cx = sl.getVisualMinX() + sl.getLogicalWidth() / 2.0;
            double cy = sl.getVisualMinY() + sl.getLogicalHeight() / 2.0;
            return sl.localToScene(cx, cy);
        } else if (n instanceof org.example.component.ImageLayer) {
            org.example.component.ImageLayer il = (org.example.component.ImageLayer) n;
            double cx = il.getLogicalWidth() / 2.0;
            double cy = il.getLogicalHeight() / 2.0;
            return il.localToScene(cx, cy);
        } else if (n instanceof org.example.component.TextLayer) {
            org.example.component.TextLayer tl = (org.example.component.TextLayer) n;
            // TextLayer logical bounds are centered at 0,0 locally
            return tl.localToScene(0, 0);
        } else {
            Bounds b = n.getBoundsInLocal();
            double cx = b.getMinX() + b.getWidth() / 2.0;
            double cy = b.getMinY() + b.getHeight() / 2.0;
            return n.localToScene(cx, cy);
        }
    }

    private static javafx.geometry.Point2D getMinXYInScene(Node n) {
        if (n instanceof org.example.component.GroupLayerV2) {
            org.example.component.GroupLayerV2 g2 = (org.example.component.GroupLayerV2) n;
            Bounds cb = g2.calculateBounds();
            // calculateBounds() usa coordenadas del contentGroup — usar contentGroup.localToScene
            return g2.getContentGroup().localToScene(cb.getMinX(), cb.getMinY());
        } else if (n instanceof org.example.component.GroupLayer) {
            org.example.component.GroupLayer gl = (org.example.component.GroupLayer) n;
            return gl.localToScene(gl.getBoundsMinX(), gl.getBoundsMinY());
        } else if (n instanceof org.example.component.ShapeLayer) {
            org.example.component.ShapeLayer sl = (org.example.component.ShapeLayer) n;
            return sl.localToScene(sl.getVisualMinX(), sl.getVisualMinY());
        } else {
            Bounds b = n.getBoundsInLocal();
            return n.localToScene(b.getMinX(), b.getMinY());
        }
    }

    public static void alignLeft(List<Node> nodes, Node anchor) {
        if (nodes == null || nodes.size() < 2)
            return;

        double targetMinX;

        if (anchor != null) {
            targetMinX = getMinXYInScene(anchor).getX();
        } else {
            double minX = Double.MAX_VALUE;
            for (Node n : nodes) {
                double nodeMinX = getMinXYInScene(n).getX();
                if (nodeMinX < minX)
                    minX = nodeMinX;
            }
            targetMinX = minX;
        }

        for (Node n : nodes) {
            if (n == anchor) continue;
            double currentMinX = getMinXYInScene(n).getX();
            double deltaX = targetMinX - currentMinX;

            if (n.getParent() != null) {
                javafx.geometry.Point2D p0 = n.getParent().sceneToLocal(0, 0);
                javafx.geometry.Point2D p1 = n.getParent().sceneToLocal(deltaX, 0);
                if (p0 != null && p1 != null) {
                    n.setTranslateX(n.getTranslateX() + (p1.getX() - p0.getX()));
                }
            } else {
                n.setTranslateX(n.getTranslateX() + deltaX);
            }
        }
    }

    public static void alignCenterHorizontal(List<Node> nodes, Node anchor) {
        if (nodes == null || nodes.size() < 2 || anchor == null)
            return;

        double targetCenterX = getCenterInScene(anchor).getX();

        for (Node n : nodes) {
            if (n == anchor) continue;
            
            double currentCenterX = getCenterInScene(n).getX();
            double deltaX = targetCenterX - currentCenterX;
            
            if (n.getParent() != null) {
                javafx.geometry.Point2D p0 = n.getParent().sceneToLocal(0, 0);
                javafx.geometry.Point2D p1 = n.getParent().sceneToLocal(deltaX, 0);
                if (p0 != null && p1 != null) {
                    n.setTranslateX(n.getTranslateX() + (p1.getX() - p0.getX()));
                }
            } else {
                n.setTranslateX(n.getTranslateX() + deltaX);
            }
        }
    }

    public static void alignMiddleVertical(List<Node> nodes, Node anchor) {
        if (nodes == null || nodes.size() < 2 || anchor == null)
            return;

        double targetCenterY = getCenterInScene(anchor).getY();

        for (Node n : nodes) {
            if (n == anchor) continue;
            
            double currentCenterY = getCenterInScene(n).getY();
            double deltaY = targetCenterY - currentCenterY;
            
            if (n.getParent() != null) {
                javafx.geometry.Point2D p0 = n.getParent().sceneToLocal(0, 0);
                javafx.geometry.Point2D p1 = n.getParent().sceneToLocal(0, deltaY);
                if (p0 != null && p1 != null) {
                    n.setTranslateY(n.getTranslateY() + (p1.getY() - p0.getY()));
                }
            } else {
                n.setTranslateY(n.getTranslateY() + deltaY);
            }
        }
    }
}

