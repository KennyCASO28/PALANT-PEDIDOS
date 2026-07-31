package org.example.component.helper;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
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

    private static Point2D getLogicalCenter(Node n) {
        if (n instanceof ShapeLayer sl) {
            return new Point2D(sl.getVisualMinX() + sl.getLogicalWidth() / 2.0, sl.getVisualMinY() + sl.getLogicalHeight() / 2.0);
        } else if (n instanceof ImageLayer il) {
            return new Point2D(il.getLogicalWidth() / 2.0, il.getLogicalHeight() / 2.0);
        } else if (n instanceof TextLayer) {
            return new Point2D(0, 0);
        }
        Bounds b = n.getBoundsInLocal();
        return new Point2D(b.getMinX() + b.getWidth() / 2.0, b.getMinY() + b.getHeight() / 2.0);
    }

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

    public static void centerNodeInParentHorizontally(Node node) {
        if (node == null || node.getParent() == null) return;
        Bounds parentBounds = node.getParent().getBoundsInLocal();
        double parentCenterX = parentBounds.getMinX() + parentBounds.getWidth() / 2.0;

        Point2D nodeCenterInScene = getCenterInScene(node);
        Point2D parentCenterInScene = node.getParent().localToScene(parentCenterX, parentBounds.getMinY() + parentBounds.getHeight() / 2.0);

        double deltaX = parentCenterInScene.getX() - nodeCenterInScene.getX();

        Point2D p0 = node.getParent().sceneToLocal(0, 0);
        Point2D p1 = node.getParent().sceneToLocal(deltaX, 0);
        if (p0 != null && p1 != null) {
            node.setTranslateX(node.getTranslateX() + (p1.getX() - p0.getX()));
        } else {
            node.setTranslateX(node.getTranslateX() + deltaX);
        }
    }

    public static void centerNodeInParentVertically(Node node) {
        if (node == null || node.getParent() == null) return;
        Bounds parentBounds = node.getParent().getBoundsInLocal();
        double parentCenterY = parentBounds.getMinY() + parentBounds.getHeight() / 2.0;

        Point2D nodeCenterInScene = getCenterInScene(node);
        Point2D parentCenterInScene = node.getParent().localToScene(parentBounds.getMinX() + parentBounds.getWidth() / 2.0, parentCenterY);

        double deltaY = parentCenterInScene.getY() - nodeCenterInScene.getY();

        Point2D p0 = node.getParent().sceneToLocal(0, 0);
        Point2D p1 = node.getParent().sceneToLocal(0, deltaY);
        if (p0 != null && p1 != null) {
            node.setTranslateY(node.getTranslateY() + (p1.getY() - p0.getY()));
        } else {
            node.setTranslateY(node.getTranslateY() + deltaY);
        }
    }

    public static void alignCombinedCenterHorizontal(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) return;

        double sumX = 0;
        for (Node n : nodes) {
            sumX += getCenterInScene(n).getX();
        }
        double targetCenterX = sumX / nodes.size();

        for (Node n : nodes) {
            double currentCenterX = getCenterInScene(n).getX();
            double deltaX = targetCenterX - currentCenterX;

            if (n.getParent() != null) {
                Point2D p0 = n.getParent().sceneToLocal(0, 0);
                Point2D p1 = n.getParent().sceneToLocal(deltaX, 0);
                if (p0 != null && p1 != null) {
                    n.setTranslateX(n.getTranslateX() + (p1.getX() - p0.getX()));
                }
            } else {
                n.setTranslateX(n.getTranslateX() + deltaX);
            }
        }
    }

    public static void alignCombinedMiddleVertical(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) return;

        double sumY = 0;
        for (Node n : nodes) {
            sumY += getCenterInScene(n).getY();
        }
        double targetCenterY = sumY / nodes.size();

        for (Node n : nodes) {
            double currentCenterY = getCenterInScene(n).getY();
            double deltaY = targetCenterY - currentCenterY;

            if (n.getParent() != null) {
                Point2D p0 = n.getParent().sceneToLocal(0, 0);
                Point2D p1 = n.getParent().sceneToLocal(0, deltaY);
                if (p0 != null && p1 != null) {
                    n.setTranslateY(n.getTranslateY() + (p1.getY() - p0.getY()));
                }
            } else {
                n.setTranslateY(n.getTranslateY() + deltaY);
            }
        }
    }

    public static void alignCenterHorizontal(List<Node> nodes, Node anchor) {
        if (nodes == null || nodes.isEmpty()) return;
        if (nodes.size() == 1) {
            centerNodeInParentHorizontally(nodes.get(0));
            return;
        }

        double targetCenterX = anchor != null ? getCenterInScene(anchor).getX() : getCenterInScene(nodes.get(0)).getX();

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
        if (nodes == null || nodes.isEmpty()) return;
        if (nodes.size() == 1) {
            centerNodeInParentVertically(nodes.get(0));
            return;
        }

        double targetCenterY = anchor != null ? getCenterInScene(anchor).getY() : getCenterInScene(nodes.get(0)).getY();

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

