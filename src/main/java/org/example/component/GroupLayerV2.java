package org.example.component;

import javafx.geometry.Bounds;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import org.example.component.GraphicLayer;
import org.example.component.ShapeLayer;
import org.example.component.ImageLayer;
import org.example.component.GroupLayer;
import org.example.component.TextLayer;
import java.util.List;

public class GroupLayerV2 extends AbstractGraphicLayer {

    public GroupLayerV2() {
        super();
        setId("USER_GROUP");
    }

    public void addChild(Node node) {
        if (node == null || userLayers.contains(node)) return;
        node.setCache(false);
        if (node instanceof javafx.scene.shape.Shape) {
            ((javafx.scene.shape.Shape) node).setSmooth(true);
        }
        if (node instanceof GraphicLayer) {
            ((GraphicLayer) node).setGrouped(true);
        }
        userLayers.add(node);
        contentGroup.getChildren().add(node);
        invalidateBounds();
        if (isSelected()) updateSelectionOverlay();
        syncUserLayersOrder();
    }

    public void removeChild(Node node) {
        if (userLayers.remove(node)) {
            if (node instanceof GraphicLayer) {
                ((GraphicLayer) node).setGrouped(false);
            }
            contentGroup.getChildren().remove(node);
            invalidateBounds();
            if (isSelected()) updateSelectionOverlay();
            syncUserLayersOrder();
        }
    }

    public void syncUserLayersOrder() {
        userLayers.clear();
        for (Node child : contentGroup.getChildren()) {
            userLayers.add(child);
        }
    }

    public List<Node> getUserLayers() {
        return new ArrayList<>(userLayers);
    }

    @Override
    protected javafx.scene.Node createSilhouetteNode() {
        Bounds b = calculateBounds();
        return new javafx.scene.shape.Rectangle(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
    }

    @Override
    public Bounds calculateBounds() {
        if (userLayers.isEmpty()) {
            return new javafx.geometry.BoundingBox(0, 0, 100, 100);
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        
        for (Node child : userLayers) {
            Bounds b = child.getBoundsInParent();
            if (b.getMinX() < minX) minX = b.getMinX();
            if (b.getMinY() < minY) minY = b.getMinY();
            if (b.getMaxX() > maxX) maxX = b.getMaxX();
            if (b.getMaxY() > maxY) maxY = b.getMaxY();
        }
        return new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private double logicalScaleX = 1.0;
    private double logicalScaleY = 1.0;

    @Override
    public double getInternalScaleX() {
        return Math.signum(super.getInternalScaleX()) * logicalScaleX;
    }

    @Override
    public double getInternalScaleY() {
        return Math.signum(super.getInternalScaleY()) * logicalScaleY;
    }

    @Override
    public void setInternalScaleX(double x) {
        this.logicalScaleX = Math.abs(x);
        if (this.logicalScaleX == 0) this.logicalScaleX = 1.0;
        super.setInternalScaleX(x < 0 ? -1.0 : 1.0);
    }

    @Override
    public void setInternalScaleY(double y) {
        this.logicalScaleY = Math.abs(y);
        if (this.logicalScaleY == 0) this.logicalScaleY = 1.0;
        super.setInternalScaleY(y < 0 ? -1.0 : 1.0);
    }

    @Override
    public void multiplyScale(double sx, double sy) {
        double absSx = Math.abs(sx);
        double absSy = Math.abs(sy);
        if (absSx == 0 && absSy == 0) return;
        
        // Track cumulative logical scale (sign for flip, magnitude for resize)
        setInternalScaleX(getInternalScaleX() * sx);
        setInternalScaleY(getInternalScaleY() * sy);
        
        if (absSx != 1.0 || absSy != 1.0) {
            for (Node n : userLayers) {
                if (n instanceof ShapeLayer) {
                    ShapeLayer sl = (ShapeLayer) n;
                    // Para ShapeLayer CUSTOM_PATH (SVG importado), el scaleTransform interno
                    // tiene pivot en el centro visual del ShapeLayer. Esto causa que al escalar,
                    // el nodo se desplace inesperadamente dentro del contentGroup.
                    // Solución: aplicar la escala Y compensar el desplazamiento del pivot manualmente,
                    // forzando un comportamiento equivalente a pivot (0,0).
                    if (sl.getType() == org.example.model.ShapeType.CUSTOM_PATH) {
                        // Calculamos la posición visual actual del child en el grupo antes de escalar
                        javafx.geometry.Bounds bBefore = n.getBoundsInParent();
                        double originX = bBefore.getMinX();
                        double originY = bBefore.getMinY();
                        // Aplicamos la escala
                        sl.multiplyScale(absSx, absSy);
                        // Calculamos dónde quedó la esquina del child tras escalar desde su pivot central
                        javafx.geometry.Bounds bAfter = n.getBoundsInParent();
                        double newOriginX = bAfter.getMinX();
                        double newOriginY = bAfter.getMinY();
                        // Corregimos con translate para que la esquina se mueva como si el pivot fuera (0,0)
                        n.setTranslateX(n.getTranslateX() + (originX * absSx - newOriginX));
                        n.setTranslateY(n.getTranslateY() + (originY * absSy - newOriginY));
                    } else {
                        sl.multiplyScale(absSx, absSy);
                        n.setTranslateX(n.getTranslateX() * absSx);
                        n.setTranslateY(n.getTranslateY() * absSy);
                    }
                } else if (n instanceof GraphicLayer) {
                    if (n instanceof ImageLayer) ((ImageLayer) n).multiplyScale(absSx, absSy);
                    else if (n instanceof GroupLayerV2) ((GroupLayerV2) n).multiplyScale(absSx, absSy);
                    else if (n instanceof GroupLayer) ((GroupLayer) n).multiplyScale(absSx, absSy);
                    else if (n instanceof TextLayer) ((TextLayer) n).multiplyScale(absSx, absSy);
                    n.setTranslateX(n.getTranslateX() * absSx);
                    n.setTranslateY(n.getTranslateY() * absSy);
                } else {
                    n.setScaleX(n.getScaleX() * absSx);
                    n.setScaleY(n.getScaleY() * absSy);
                    n.setTranslateX(n.getTranslateX() * absSx);
                    n.setTranslateY(n.getTranslateY() * absSy);
                }
            }
            invalidateBounds();
            if (isSelected()) updateSelectionOverlay();
        }
    }



    @Override
    public void multiplyShear(double sx, double sy) {
        // Apply to the group's shear transform
        setInternalShearX(getInternalShearX() + sx);
        setInternalShearY(getInternalShearY() + sy);
        
        // Pass down to graphic layer children if necessary, or just rely on group transform
        // For standard groups, modifying the group's shearTransform is sufficient 
        // since the children inherit the transformation.
        invalidateBounds();
        if (isSelected()) updateSelectionOverlay();
    }



    @Override
    public void setRotationMode(boolean active) {
        super.setRotationMode(active);
    }



    @Override
    public void setGrouped(boolean grouped) {
        super.setGrouped(grouped);
        if (grouped) {
            setSelected(false);
        }
    }
    
    private boolean editingMode = false;
    
    public boolean isEditingMode() {
        return editingMode;
    }
    
    public void setEditingMode(boolean editingMode) {
        this.editingMode = editingMode;
    }
}
