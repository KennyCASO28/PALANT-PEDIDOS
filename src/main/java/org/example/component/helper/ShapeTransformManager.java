package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import org.example.model.BezierNode;
import org.example.pattern.NodeMemento;
import org.example.pattern.TransformCommand;
import org.example.component.ShapeLayer;

import java.util.List;

/**
 * Manages all transformations (Scale, Rotate, Shear, Flip, Size) for a ShapeLayer.
 * Handles the undo logic for transformations as well.
 */
public class ShapeTransformManager {

    private final ShapeLayer layer;
    private final Rotate rotateTransform;
    private final Scale scaleTransform;
    private final Shear shearTransform;

    public ShapeTransformManager(ShapeLayer layer, Rotate rotate, Scale scale, Shear shear) {
        this.layer = layer;
        this.rotateTransform = rotate;
        this.scaleTransform = scale;
        this.shearTransform = shear;
    }

    public double getInternalRotation() { 
        return rotateTransform.getAngle(); 
    }

    public void setInternalRotation(double a) { 
        rotateTransform.setAngle(a); 
        layer.updateVisuals(); 
    }

    public double getInternalScaleX() { 
        return scaleTransform.getX(); 
    }

    public void setInternalScaleX(double s) { 
        // If we cross the zero boundary (flip), we must invert ShearY to maintain visual symmetry
        if ((scaleTransform.getX() > 0 && s < 0) || (scaleTransform.getX() < 0 && s > 0)) {
            setInternalShearY(getInternalShearY() * -1);
        }
        scaleTransform.setX(s); 
        layer.updateVisuals(); 
    }

    public double getInternalScaleY() { 
        return scaleTransform.getY(); 
    }

    public void setInternalScaleY(double s) { 
        // If we cross the zero boundary (flip), we must invert ShearX to maintain visual symmetry
        if ((scaleTransform.getY() > 0 && s < 0) || (scaleTransform.getY() < 0 && s > 0)) {
            setInternalShearX(getInternalShearX() * -1);
        }
        scaleTransform.setY(s); 
        layer.updateVisuals(); 
    }

    public void setInternalShearX(double x) { 
        shearTransform.setX(x); 
        layer.renderShape(); 
        layer.updateVisuals(); 
    }

    public void setInternalShearY(double y) { 
        shearTransform.setY(y); 
        layer.renderShape(); 
        layer.updateVisuals(); 
    }

    public double getInternalShearX() { 
        return shearTransform.getX(); 
    }

    public double getInternalShearY() { 
        return shearTransform.getY(); 
    }

    public void flipHorizontal() {
        NodeMemento before = new NodeMemento(layer);
        rotateTransform.setAngle(-rotateTransform.getAngle());
        double oldShearX = shearTransform.getX();
        setInternalShearX(-oldShearX);
        setInternalScaleX(scaleTransform.getX() * -1);
        addTransformUndo(before);
    }

    public void flipVertical() {
        NodeMemento before = new NodeMemento(layer);
        rotateTransform.setAngle(-rotateTransform.getAngle());
        double oldShearY = shearTransform.getY();
        setInternalShearY(-oldShearY);
        setInternalScaleY(scaleTransform.getY() * -1);
        addTransformUndo(before);
    }

    public void setSize(double width, double height, double visualMinX, double visualMinY, List<BezierNode> bezierNodes, 
                        double currentWidth, double currentHeight) {
        if (layer.getState().bezierNodes != null) {
            double deltaRatioX = (currentWidth > 0) ? (width / currentWidth) : 1.0;
            double deltaRatioY = (currentHeight > 0) ? (height / currentHeight) : 1.0;
            ShapePathSupport.scaleNodes(bezierNodes, deltaRatioX, deltaRatioY);
            layer.getState().svgPathData = ShapePathSupport.buildSvgPath(bezierNodes, layer.getState().isClosed);
        }
        layer.refreshShapeVisuals();
    }

    public void setSizeWithOffset(Double w, Double h, Double ox, Double oy) {
        if (w != null) layer.getState().width = w;
        if (h != null) layer.getState().height = h;
        if (ox != null) layer.getState().visualMinX = ox;
        if (oy != null) layer.getState().visualMinY = oy;
        layer.refreshShapeVisuals();
    }

    public void multiplyScale(double rx, double ry, double currentWidth, double currentHeight) {
        if (rx < 0 || ry < 0) {
            double signX = Math.signum(rx);
            double signY = Math.signum(ry);
            setInternalScaleX(getInternalScaleX() * signX);
            setInternalScaleY(getInternalScaleY() * signY);
            rx = Math.abs(rx);
            ry = Math.abs(ry);
        }
        
        double newWidth = currentWidth * rx;
        double newHeight = currentHeight * ry;
        double newVisualMinX = layer.getState().visualMinX * rx;
        double newVisualMinY = layer.getState().visualMinY * ry;
        
        if (layer.getState().bezierNodes != null) {
            double deltaRatioX = (currentWidth > 0) ? (newWidth / currentWidth) : 1.0;
            double deltaRatioY = (currentHeight > 0) ? (newHeight / currentHeight) : 1.0;
            ShapePathSupport.scaleNodes(layer.getState().bezierNodes, deltaRatioX, deltaRatioY);
            layer.getState().svgPathData = ShapePathSupport.buildSvgPath(layer.getState().bezierNodes, layer.getState().isClosed);
        }
        
        setSizeWithOffset(newWidth, newHeight, newVisualMinX, newVisualMinY);
    }

    public void multiplyShear(double sx, double sy) {
        if (sx == 0 && sy == 0) return;
        layer.convertPrimitiveToPath();
        List<BezierNode> nodes = layer.getState().bezierNodes;
        if (nodes != null) {
            double px = layer.getState().width / 2.0 + layer.getState().visualMinX; 
            double py = layer.getState().height / 2.0 + layer.getState().visualMinY;
            for (BezierNode n : nodes) {
                n.anchor = applyShearPoint(n.anchor, sx, sy, px, py);
                if (n.control1 != null) n.control1 = applyShearPoint(n.control1, sx, sy, px, py);
                if (n.control2 != null) n.control2 = applyShearPoint(n.control2, sx, sy, px, py);
            }
            layer.getState().svgPathData = ShapePathSupport.buildSvgPath(nodes, layer.getState().isClosed);
        }
        layer.refreshShapeVisuals();
    }

    public void rotateBy(double angle) { 
        rotateTransform.setAngle(rotateTransform.getAngle() + angle); 
        layer.updateVisuals(); 
    }

    public void resetTransforms() {
        NodeMemento before = new NodeMemento(layer);
        rotateTransform.setAngle(0); 
        scaleTransform.setX(1); 
        scaleTransform.setY(1); 
        shearTransform.setX(0); 
        shearTransform.setY(0);
        if (layer.getState().originalBezierNodes != null) { 
            layer.getState().bezierNodes = ShapePathSupport.copyNodes(layer.getState().originalBezierNodes); 
        }
        layer.refreshShapeVisuals();
        addTransformUndo(before);
    }

    private Point2D applyShearPoint(Point2D p, double sx, double sy, double px, double py) {
        double newX = p.getX() + sx * (p.getY() - py);
        double newY = p.getY() + sy * (p.getX() - px);
        return new Point2D(newX, newY);
    }

    private void addTransformUndo(NodeMemento before) {
        if (layer.getVisualizer() != null) {
            layer.getVisualizer().getHistoryManager().addCommand(new TransformCommand(layer, 
                before, new NodeMemento(layer), layer.getState().activeZone));
        }
    }
}
