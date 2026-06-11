package org.example.component.helper;

import java.util.ArrayList;
import java.util.List;
import org.example.component.ShapeLayer;
import org.example.model.BezierNode;
import org.example.model.ShapeType;

public final class ShapeClipboardSupport {

    private static ShapeLayer clipboardLayer;
    private static boolean cutOperation;

    private ShapeClipboardSupport() {
    }

    public static void copy(ShapeLayer layer) {
        clipboardLayer = layer;
        cutOperation = false;
    }

    public static void cut(ShapeLayer layer) {
        clipboardLayer = layer;
        cutOperation = true;
        layer.removeFromParent();
    }

    public static boolean hasClipboard() {
        return clipboardLayer != null;
    }

    public static void clear() {
        clipboardLayer = null;
        cutOperation = false;
    }

    public static ShapeLayer getClipboardCopy() {
        if (clipboardLayer == null) {
            return null;
        }

        if (cutOperation) {
            ShapeLayer cutLayer = clipboardLayer;
            clipboardLayer = null;
            cutOperation = false;
            cutLayer.setActiveZone(null);
            cutLayer.setLocked(false);
            return cutLayer;
        }

        return cloneLayer(clipboardLayer);
    }

    private static ShapeLayer cloneLayer(ShapeLayer source) {
        ShapeLayer copy = new ShapeLayer(
                source.getType(),
                source.getFillColor(),
                source.getStrokeColor(),
                source.getStrokeWidth());

        copy.setWidth(source.getWidth());
        copy.setHeight(source.getHeight());
        copy.setArcWidth(source.getArcWidth());
        copy.setArcHeight(source.getArcHeight());
        
        copy.setTranslateX(source.getTranslateX());
        copy.setTranslateY(source.getTranslateY());
        copy.setInternalRotation(source.getInternalRotation());
        copy.setInternalScaleX(source.getInternalScaleX());
        copy.setInternalScaleY(source.getInternalScaleY());
        copy.setInternalShearX(source.getInternalShearX());
        copy.setInternalShearY(source.getInternalShearY());

        if (source.getType() == ShapeType.CUSTOM_PATH) {
            List<BezierNode> bezierNodes = source.getBezierNodes();
            if (bezierNodes != null) {
                copy.setBezierNodes(ShapePathSupport.copyNodes(bezierNodes));
            }
            copy.setIsClosed(source.getIsClosed());
        }

        String svgPathData = source.getSvgPathData();
        if (svgPathData != null) {
            copy.setSvgPathData(svgPathData);
            copy.refreshShapeVisuals();
        }

        if (source.getContourSteps() > 0) {
            copy.applyContour(source.getContourSteps(), source.getContourDistance(), source.getContourColor());
        }

        if (source.isTransparencyEnabled()) {
            copy.setTransparency(
                    true,
                    source.getTransparencyAngle(),
                    source.getTransparencyStartAlpha(),
                    source.getTransparencyEndAlpha());
            copy.setTransparencyBalance(source.getTransparencyBalance());
        }

        return copy;
    }
}
