package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import org.example.component.ShapeLayer;
import org.example.model.BezierNode;
import org.example.model.ShapeType;
import org.example.pattern.NodeMemento;
import org.example.pattern.TransformCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the complete lifecycle of a ShapeLayer from creation to cloning.
 * Handles contour generation and cloning logic that previously lived directly
 * in ShapeLayer.
 */
public class ShapeLayerOrchestrator {

    private final ShapeLayer layer;

    public ShapeLayerOrchestrator(ShapeLayer layer) {
        this.layer = layer;
    }

    /**
     * Generates multiple contour layers based on the configured contour settings.
     */
    public List<ShapeLayer> separateContours() {
        List<ShapeLayer> newLayers = new ArrayList<>();
        if (layer.getState().contourSteps <= 0)
            return newLayers;

        int steps = layer.getState().contourSteps;
        double dist = layer.getState().contourDistance;
        Color c = layer.getState().contourColor;

        Color baseColor = (layer.getState().fillColor != null && !Color.TRANSPARENT.equals(layer.getState().fillColor))
                ? layer.getState().fillColor
                : (layer.getState().strokeColor != null ? layer.getState().strokeColor : Color.BLACK);

        double origTranslateX = layer.getTranslateX();
        double origTranslateY = layer.getTranslateY();
        double origRotate = layer.getRotate();
        double origScaleX = layer.getScaleX();
        double origScaleY = layer.getScaleY();

        for (int i = 1; i <= steps; i++) {
            ShapeLayer nl = layer.createDeepClone();
            nl.getState().contourSteps = 0;

            Color stepColor = baseColor.interpolate(c, (double) i / steps);
            nl.getState().fillColor = stepColor;
            nl.getState().strokeColor = stepColor;
            nl.getState().strokeWidth = 0.5;

            double offset = i * dist;

            nl.getState().visualMinX = 0;
            nl.getState().visualMinY = 0;
            nl.getState().width = layer.getState().width + (offset * 2.0);
            nl.getState().height = layer.getState().height + (offset * 2.0);

            double expansion = (layer.getState().contourLineJoin == StrokeLineJoin.ROUND) ? (offset * 2.0) : 0;
            nl.getState().arcWidth = layer.getState().arcWidth + expansion;
            nl.getState().arcHeight = layer.getState().arcHeight + expansion;

            nl.renderShape();

            double worldOffsetX = offset * Math.abs(origScaleX);
            double worldOffsetY = offset * Math.abs(origScaleY);

            nl.setTranslateX(origTranslateX + worldOffsetX);
            nl.setTranslateY(origTranslateY + worldOffsetY);
            nl.setRotate(origRotate);
            nl.setScaleX(1.0);
            nl.setScaleY(1.0);

            newLayers.add(nl);
        }

        layer.applyContour(0, 0, Color.TRANSPARENT);
        return newLayers;
    }

    /**
     * Deep clone a ShapeLayer with all its state and transforms.
     */
    public ShapeLayer createClone() {
        ShapeLayer clone = new ShapeLayer(layer.getState().type, layer.getState().fillColor,
                layer.getState().strokeColor, layer.getState().strokeWidth);

        clone.getState().type = layer.getState().type;
        clone.getState().width = layer.getState().width;
        clone.getState().height = layer.getState().height;
        clone.getState().visualMinX = layer.getState().visualMinX;
        clone.getState().visualMinY = layer.getState().visualMinY;
        clone.getState().fillColor = layer.getState().fillColor;
        clone.getState().strokeColor = layer.getState().strokeColor;
        clone.getState().strokeWidth = layer.getState().strokeWidth;
        clone.getState().arcWidth = layer.getState().arcWidth;
        clone.getState().arcHeight = layer.getState().arcHeight;
        clone.getState().svgPathData = layer.getState().svgPathData;
        if (layer.getState().bezierNodes != null) {
            clone.getState().bezierNodes = ShapePathSupport.copyNodes(layer.getState().bezierNodes);
        }
        if (layer.getState().originalBezierNodes != null) {
            clone.getState().originalBezierNodes = ShapePathSupport.copyNodes(layer.getState().originalBezierNodes);
        }

        clone.setVisualizer(layer.getVisualizer());
        clone.renderShape();

        // Copy pivot offset before copying translations, so the compensation gets overwritten by the exact translation
        clone.updatePivotWithCompensation(layer.getCustomPivotX(), layer.getCustomPivotY());

        clone.setTranslateX(layer.getTranslateX());
        clone.setTranslateY(layer.getTranslateY());
        clone.setInternalRotation(layer.getInternalRotation());
        clone.setInternalScaleX(layer.getInternalScaleX());
        clone.setInternalScaleY(layer.getInternalScaleY());
        clone.setInternalShearX(layer.getInternalShearX());
        clone.setInternalShearY(layer.getInternalShearY());

        return clone;
    }

    /**
     * Normalizes bezier node positions and fixes the position accordingly.
     */
    public void recalculateGeometricBounds() {
        if (layer.getState().bezierNodes == null || layer.getState().bezierNodes.isEmpty()) {
            if (layer.getState().type == ShapeType.CUSTOM_PATH && layer.getState().svgPathData != null) {
                List<BezierNode> parsed = ShapePathSupport.parseSvgPath(layer.getState().svgPathData);
                if (parsed != null && !parsed.isEmpty()) {
                    layer.getState().bezierNodes = parsed;
                }
            }
        }

        if (layer.getState().bezierNodes == null || layer.getState().bezierNodes.isEmpty()) {
            layer.getState().visualMinX = 0;
            layer.getState().visualMinY = 0;
            return;
        }

        Point2D offset = ShapePathSupport.normalizeNodes(layer.getState().bezierNodes);
        if (offset.getX() != 0 || offset.getY() != 0) {
            Point2D parentOffset = layer.localToParent(offset.getX(), offset.getY());
            Point2D parentZero = layer.localToParent(0, 0);
            layer.setTranslateX(layer.getTranslateX() + (parentOffset.getX() - parentZero.getX()));
            layer.setTranslateY(layer.getTranslateY() + (parentOffset.getY() - parentZero.getY()));
            layer.getState().svgPathData = ShapePathSupport.buildSvgPath(layer.getState().bezierNodes,
                    layer.getState().isClosed);
        }

        ShapePathSupport.BoundsData bounds = ShapePathSupport.calculateBezierBounds(layer.getState().bezierNodes);
        if (bounds != null) {
            layer.getState().visualMinX = bounds.getMinX();
            layer.getState().visualMinY = bounds.getMinY();
            layer.getState().width = bounds.getWidth();
            layer.getState().height = bounds.getHeight();
        }
    }
}
