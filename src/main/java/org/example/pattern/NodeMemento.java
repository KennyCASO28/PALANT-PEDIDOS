package org.example.pattern;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.example.component.ImageLayer;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;
import org.example.component.GraphicLayer;
import org.example.component.helper.SmartZoneContainer;
import org.example.model.ShapeType;
import org.example.model.TrajectoryPath;

/**
 * Captures the exact state (parent, index, and transformations) of a JavaFX Node.
 * Used for precise Undo/Redo operations without relying on layout calculations.
 * 
 * ENHANCED VERSION with improved index handling and safety checks to prevent
 * nodes from "disappearing" during undo/redo operations.
 */
public class NodeMemento {
    private final Node node;
    private final Parent parent;
    private final int index;
    private final boolean parentWasGroup;
    
    // Transform state
    private double tx, ty, sx, sy, rot;
    private double isx, isy, irot;
    private double shx, shy;
    private double cpx, cpy;
    private Double logicalWidth = null;
    private Double logicalHeight = null;
    private Double visualMinX = null;
    private Double visualMinY = null;
    private boolean visible;
    private String activeZone = null;
    private ShapeType shapeType = null;
    private Double arcWidth = null;
    private Double arcHeight = null;
    private TrajectoryPath trajectoryPath = null;
    private java.util.List<org.example.model.BezierNode> bezierNodes = null;
    private String svgPathData = null;
    private java.util.List<NodeMemento> childMementos = null;

    public NodeMemento(Node node) {
        this.node = node;
        this.parent = node.getParent();

        int idx = -1;
        this.parentWasGroup = (this.parent instanceof Group);
        if (this.parent instanceof Group) {
            idx = ((Group) this.parent).getChildren().indexOf(node);
        } else if (this.parent != null) {
            idx = this.parent.getChildrenUnmodifiable().indexOf(node);
        }
        this.index = Math.max(idx, 0); // Ensure non-negative index

        this.tx = node.getTranslateX();
        this.ty = node.getTranslateY();
        this.sx = node.getScaleX();
        this.sy = node.getScaleY();
        this.rot = node.getRotate();
        this.visible = node.isVisible();
        if (node instanceof GraphicLayer) {
            this.activeZone = ((GraphicLayer) node).getActiveZone();
        }

        // Capture internal transforms based on type
        if (node instanceof ShapeLayer) {
            ShapeLayer sl = (ShapeLayer) node;
            this.shapeType = sl.getType();
            this.isx = sl.getInternalScaleX();
            this.isy = sl.getInternalScaleY();
            this.irot = sl.getInternalRotation();
            this.shx = sl.getInternalShearX();
            this.shy = sl.getInternalShearY();
            this.cpx = sl.getInternalPivotX();
            this.cpy = sl.getInternalPivotY();
            this.logicalWidth = sl.getLogicalWidth();
            this.logicalHeight = sl.getLogicalHeight();
            this.visualMinX = sl.getVisualMinX();
            this.visualMinY = sl.getVisualMinY();
            this.arcWidth = sl.getArcWidth();
            this.arcHeight = sl.getArcHeight();
            this.svgPathData = sl.getSvgPathData();
            if (sl.getBezierNodes() != null && !sl.getBezierNodes().isEmpty()) {
                this.bezierNodes = new java.util.ArrayList<>();
                for (org.example.model.BezierNode bn : sl.getBezierNodes()) {
                    this.bezierNodes.add(bn.copy());
                }
            }
        } else if (node instanceof ImageLayer) {
            ImageLayer il = (ImageLayer) node;
            this.isx = il.getInternalScaleX();
            this.isy = il.getInternalScaleY();
            this.irot = il.getInternalRotation();
            this.shx = il.getShearTransform().getX();
            this.shy = il.getShearTransform().getY();
            this.cpx = il.getRotateTransform().getPivotX();
            this.cpy = il.getRotateTransform().getPivotY();
            this.logicalWidth = il.getCurrentWidth();
            this.logicalHeight = il.getCurrentHeight();
        } else if (node instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) node;
            this.isx = gl.getInternalScaleX();
            this.isy = gl.getInternalScaleY();
            this.irot = gl.getInternalRotation();
            this.childMementos = new java.util.ArrayList<>();
            for (Node child : gl.getUserLayers()) {
                this.childMementos.add(new NodeMemento(child));
            }
        } else if (node instanceof GroupLayerV2) {
            GroupLayerV2 gl20 = (GroupLayerV2) node;
            this.isx = gl20.getInternalScaleX();
            this.isy = gl20.getInternalScaleY();
            this.irot = gl20.getInternalRotation();
            this.shx = gl20.getInternalShearX();
            this.shy = gl20.getInternalShearY();
            this.childMementos = new java.util.ArrayList<>();
            for (Node child : gl20.getUserLayers()) {
                this.childMementos.add(new NodeMemento(child));
            }
        } else if (node instanceof TextLayer) {
            TextLayer tl = (TextLayer) node;
            this.isx = tl.getInternalScaleX();
            this.isy = tl.getInternalScaleY();
            this.irot = tl.getInternalRotation();
            this.shx = tl.getShearX();
            this.shy = tl.getShearY();
            this.cpx = tl.getCustomPivotX();
            this.cpy = tl.getCustomPivotY();
            this.logicalWidth = tl.getLogicalWidth();
            this.logicalHeight = tl.getLogicalHeight();
            this.trajectoryPath = tl.getTrajectory() != null ? tl.getTrajectory().copy() : null;
        } else {
            this.isx = 1.0;
            this.isy = 1.0;
            this.irot = 0.0;
            this.shx = 0.0;
            this.shy = 0.0;
            this.cpx = -1.0;
            this.cpy = -1.0;
        }
    }

    public double getTx() { return tx; }
    public double getTy() { return ty; }

    public void overrideTransforms(double x, double y, double sX, double sY, double r,
            Double w, Double h, Double mx, Double my) {
        this.tx = x;
        this.ty = y;

        if (this.node instanceof org.example.component.GroupLayer
                || this.node instanceof org.example.component.GroupLayerV2
                || this.node instanceof org.example.component.ShapeLayer
                || this.node instanceof org.example.component.ImageLayer
                || this.node instanceof org.example.component.TextLayer) {
            this.isx = sX;
            this.isy = sY;
            this.irot = r;
            this.sx = 1.0;
            this.sy = 1.0;
            this.rot = 0.0;
        } else {
            this.sx = sX;
            this.sy = sY;
            this.rot = r;
        }

        if (w != null)
            this.logicalWidth = w;
        if (h != null)
            this.logicalHeight = h;
        if (mx != null)
            this.visualMinX = mx;
        if (my != null)
            this.visualMinY = my;
    }

    /**
     * Restores the node to its captured parent and index.
     * IMPROVED with safety checks to prevent nodes from disappearing.
     */
    public void restore() {
        if (node == null) return;
        
        try {
            System.out.println("DEBUG NodeMemento.restore: node=" + node.getClass().getSimpleName());
            restoreParentAndIndex();
            applyTransforms();
            syncContainerState();
        } catch (Exception e) {
            System.err.println("NodeMemento ERROR during restore: " + e.getMessage());
            e.printStackTrace();
            try { applyTransforms(); } catch (Exception ignored) {}
        }
    }

    public void restoreTransformsOnly() {
        if (node == null) return;
        applyTransforms();
        syncContainerState();
    }

    private void restoreParentAndIndex() {
        Parent currentParent = node.getParent();

        if (parent == null) {
            if (currentParent instanceof Group) {
                ((Group) currentParent).getChildren().remove(node);
                if (currentParent.getParent() instanceof org.example.component.GroupLayerV2) {
                    ((org.example.component.GroupLayerV2) currentParent.getParent()).removeChild(node);
                } else if (currentParent instanceof org.example.component.GroupLayerV2) {
                    ((org.example.component.GroupLayerV2) currentParent).removeChild(node);
                }
            }
            return;
        }

        if (!(parent instanceof Group)) {
            return;
        }

        Group targetParent = (Group) parent;
        if (currentParent instanceof Group && currentParent != targetParent) {
            ((Group) currentParent).getChildren().remove(node);
            if (currentParent.getParent() instanceof org.example.component.GroupLayerV2) {
                ((org.example.component.GroupLayerV2) currentParent.getParent()).removeChild(node);
            } else if (currentParent instanceof org.example.component.GroupLayerV2) {
                ((org.example.component.GroupLayerV2) currentParent).removeChild(node);
            }
        }

        if (targetParent.getChildren().contains(node)) {
            targetParent.getChildren().remove(node);
        }

        int boundedIndex = Math.max(0, Math.min(index, targetParent.getChildren().size()));

        if (targetParent.getParent() instanceof org.example.component.GroupLayerV2) {
            org.example.component.GroupLayerV2 glv2 = (org.example.component.GroupLayerV2) targetParent.getParent();
            glv2.addChild(node);
            if (targetParent.getChildren().contains(node)) {
                targetParent.getChildren().remove(node);
            }
            targetParent.getChildren().add(boundedIndex, node);
            glv2.syncUserLayersOrder();
        } else if (targetParent instanceof org.example.component.GroupLayerV2) {
            org.example.component.GroupLayerV2 glv2 = (org.example.component.GroupLayerV2) targetParent;
            glv2.addChild(node);
            if (targetParent.getChildren().contains(node)) {
                targetParent.getChildren().remove(node);
            }
            targetParent.getChildren().add(boundedIndex, node);
            glv2.syncUserLayersOrder();
        } else {
            targetParent.getChildren().add(boundedIndex, node);
        }
    }

    private void applyTransforms() {
        node.setTranslateX(tx);
        node.setTranslateY(ty);
        node.setScaleX(sx);
        node.setScaleY(sy);
        node.setRotate(rot);
        node.setVisible(visible);

        if (node instanceof ShapeLayer) {
            ShapeLayer sl = (ShapeLayer) node;
            if (shapeType != null && sl.getType() != shapeType) {
                sl.setType(shapeType);
            }
            sl.setInternalScaleX(isx);
            sl.setInternalScaleY(isy);
            sl.setInternalRotation(irot);
            sl.setInternalShearX(shx);
            sl.setInternalShearY(shy);
            sl.updatePivot(cpx, cpy);
            if (logicalWidth != null && logicalHeight != null) {
                if (visualMinX != null && visualMinY != null) {
                    sl.setSizeWithOffset(logicalWidth, logicalHeight, visualMinX, visualMinY);
                } else {
                    sl.setSize(logicalWidth, logicalHeight);
                }
            }
            if (this.svgPathData != null) {
                sl.setSvgPathData(this.svgPathData);
            }
            if (this.bezierNodes != null) {
                java.util.List<org.example.model.BezierNode> restoredNodes = new java.util.ArrayList<>();
                for (org.example.model.BezierNode bn : this.bezierNodes) {
                    restoredNodes.add(bn.copy());
                }
                sl.setBezierNodes(restoredNodes);
                sl.refreshPath();
            }
            if (arcWidth != null) {
                sl.setArcWidth(arcWidth);
            }
            if (arcHeight != null) {
                sl.setArcHeight(arcHeight);
            }
            // DON'T call refreshShapeVisuals() here - it might override our transforms!
        } else if (node instanceof ImageLayer) {
            ImageLayer il = (ImageLayer) node;
            il.setInternalScaleX(isx);
            il.setInternalScaleY(isy);
            il.setInternalRotation(irot);
            il.getShearTransform().setX(shx);
            il.getShearTransform().setY(shy);
            il.setCustomPivot(cpx, cpy);
            if (logicalWidth != null && logicalHeight != null) {
                il.resize(logicalWidth, logicalHeight);
            }
        } else if (node instanceof GroupLayer) {
            GroupLayer gl = (GroupLayer) node;
            gl.setInternalScaleX(isx);
            gl.setInternalScaleY(isy);
            gl.setInternalRotation(irot);
            if (this.childMementos != null) {
                for (NodeMemento cm : this.childMementos) {
                    cm.restore();
                }
            }
        } else if (node instanceof GroupLayerV2) {
            GroupLayerV2 glv2 = (GroupLayerV2) node;
            glv2.setInternalScaleX(isx);
            glv2.setInternalScaleY(isy);
            glv2.setInternalRotation(irot);
            glv2.setInternalShear(shx, shy);
            if (this.childMementos != null) {
                // CRITICAL FIX: We must use addChild() (not raw cm.restore()) to keep
                // userLayers in sync with contentGroup.getChildren(). If we just call
                // cm.restore(), the child is added to contentGroup.getChildren() directly
                // but userLayers stays empty — so getUserLayers() (used by ungroup) returns
                // nothing, causing a "group disappears on second ungroup" bug.
                //
                // Clear existing children first to avoid duplicates from repeated restores.
                java.util.List<Node> existingChildren = new java.util.ArrayList<>(glv2.getUserLayers());
                for (Node existing : existingChildren) {
                    glv2.removeChild(existing);
                }
                for (NodeMemento cm : this.childMementos) {
                    Node childNode = cm.node;
                    // Remove from any current parent to avoid JavaFX "already has parent" error
                    if (childNode.getParent() instanceof Group) {
                        ((Group) childNode.getParent()).getChildren().remove(childNode);
                    }
                    glv2.addChild(childNode);
                    // Now restore transforms (position/scale/rotation) without re-parenting
                    cm.restoreTransformsOnly();
                }
                glv2.recalculateBounds();

            }
        } else if (node instanceof TextLayer) {
            TextLayer tl = (TextLayer) node;
            tl.setInternalScaleX(isx);
            tl.setInternalScaleY(isy);
            tl.setInternalRotation(irot);
            tl.setShearX(shx);
            tl.setShearY(shy);
            tl.setCustomPivotX(cpx);
            tl.setCustomPivotY(cpy);
            if (logicalWidth != null && logicalHeight != null) {
                tl.setTextSizeSilently(logicalWidth, logicalHeight);
            }
            if (trajectoryPath != null && tl.getTrajectory() != null) {
                tl.getTrajectory().setFrom(trajectoryPath);
                tl.renderText();
            }
        }

        if (node instanceof GraphicLayer) {
            ((GraphicLayer) node).setActiveZone(activeZone);
        }
    }

    private void syncContainerState() {
        Parent p = node.getParent();
        if (p instanceof Group && ((Group) p).getParent() instanceof SmartZoneContainer) {
            SmartZoneContainer container = (SmartZoneContainer) ((Group) p).getParent();
            container.updateItemState(node);
        }
    }

    private boolean isRootContainer(Group g) {
        return g.getId() != null && (g.getId().equals("LAYER_CONTAINER") || g.getId().equals("contentGroup"));
    }

    public boolean isEquivalentTo(NodeMemento other) {
        if (other == null) return false;
        return parent == other.parent
                && index == other.index
                && close(tx, other.tx)
                && close(ty, other.ty)
                && close(sx, other.sx)
                && close(sy, other.sy)
                && close(rot, other.rot)
                && close(isx, other.isx)
                && close(isy, other.isy)
                && close(irot, other.irot)
                && close(shx, other.shx)
                && close(shy, other.shy)
                && close(cpx, other.cpx)
                && close(cpy, other.cpy)
                && nullableClose(logicalWidth, other.logicalWidth)
                && nullableClose(logicalHeight, other.logicalHeight)
                && nullableClose(visualMinX, other.visualMinX)
                && nullableClose(visualMinY, other.visualMinY)
                && nullableClose(arcWidth, other.arcWidth)
                && nullableClose(arcHeight, other.arcHeight)
                && visible == other.visible
                && java.util.Objects.equals(activeZone, other.activeZone)
                && shapeType == other.shapeType
                && java.util.Objects.equals(svgPathData, other.svgPathData)
                && trajectoryEquivalent(trajectoryPath, other.trajectoryPath)
                && bezierEquivalent(bezierNodes, other.bezierNodes);
    }

    private boolean close(double a, double b) {
        return Math.abs(a - b) < 0.0001;
    }

    private boolean nullableClose(Double a, Double b) {
        if (a == null || b == null) return a == b;
        return close(a, b);
    }

    private boolean trajectoryEquivalent(TrajectoryPath a, TrajectoryPath b) {
        if (a == null || b == null) return a == b;
        if (a.getType() != b.getType()) return false;
        if (!close(a.getCurvature(), b.getCurvature())) return false;
        if (!close(a.getSpacing(), b.getSpacing())) return false;
        if (a.isAutoRotate() != b.isAutoRotate()) return false;
        java.util.List<javafx.geometry.Point2D> ap = a.getControlPoints();
        java.util.List<javafx.geometry.Point2D> bp = b.getControlPoints();
        if (ap.size() != bp.size()) return false;
        for (int i = 0; i < ap.size(); i++) {
            if (!close(ap.get(i).getX(), bp.get(i).getX()) || !close(ap.get(i).getY(), bp.get(i).getY())) {
                return false;
            }
        }
        return true;
    }

    private boolean bezierEquivalent(java.util.List<org.example.model.BezierNode> a,
            java.util.List<org.example.model.BezierNode> b) {
        if (a == null || b == null) return a == b;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            org.example.model.BezierNode an = a.get(i);
            org.example.model.BezierNode bn = b.get(i);
            if (!pointClose(an.anchor, bn.anchor)
                    || !pointClose(an.control1, bn.control1)
                    || !pointClose(an.control2, bn.control2)
                    || an.isMoveTo != bn.isMoveTo) {
                return false;
            }
        }
        return true;
    }

    private boolean pointClose(javafx.geometry.Point2D a, javafx.geometry.Point2D b) {
        if (a == null || b == null) return a == b;
        return close(a.getX(), b.getX()) && close(a.getY(), b.getY());
    }
}
