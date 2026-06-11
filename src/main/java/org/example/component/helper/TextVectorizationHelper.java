package org.example.component.helper;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

public class TextVectorizationHelper {

    /**
     * Convierte un TextLayer en letras vectoriales independientes.
     * La geometria final queda horneada en coordenadas del contenedor padre para
     * preservar posicion, escala, rotacion, shear y trayectoria visual.
     */
    public static List<ShapeLayer> convertToShapeLayers(TextLayer textLayer) {
        List<ShapeLayer> vectorLayers = new ArrayList<>();
        if (textLayer == null) return vectorLayers;

        List<Path> letterPaths = new ArrayList<>();
        extractFilledLetterPaths(textLayer.getTextGroup(), textLayer.getLocalToParentTransform(), letterPaths);

        for (Path letterPath : letterPaths) {
            ShapeLayer layer = VectorBooleanHelper.createShapeLayerFromPath(
                    letterPath, textLayer.getTextColor(), Color.TRANSPARENT, 0);
            if (layer != null) {
                if (textLayer.isDropShadowEnabled()) {
                    layer.getShapeGroup().setEffect(new javafx.scene.effect.DropShadow(10, textLayer.getDropShadowColor()));
                }
                vectorLayers.add(layer);
            }
        }

        return vectorLayers;
    }

    /**
     * Compatibilidad con llamadas antiguas: devuelve una sola capa soldada.
     * El flujo de UI usa convertToShapeLayers para dejar las letras separadas.
     */
    public static ShapeLayer convertToShapeLayer(TextLayer textLayer) {
        if (textLayer == null) return null;

        List<ShapeLayer> layers = convertToShapeLayers(textLayer);
        if (layers.isEmpty()) return null;

        List<Shape> shapes = new ArrayList<>();
        for (ShapeLayer layer : layers) {
            Shape current = layer.getCurrentShapeNode();
            if (current == null) continue;
            Path raw = toPath(current);
            if (raw != null) {
                shapes.add(VectorBooleanHelper.transformPath(raw, layer.getLocalToParentTransform()));
            }
        }

        Shape unionShape = VectorBooleanHelper.weldShapes(shapes);
        if (unionShape == null) return null;

        Path masterPath = toPath(unionShape);
        return VectorBooleanHelper.createShapeLayerFromPath(masterPath, textLayer.getTextColor(), Color.TRANSPARENT, 0);
    }

    private static void extractFilledLetterPaths(Node node, Transform currentTransform, List<Path> paths) {
        if (node == null || !node.isVisible()) return;

        Transform nodeTransform = currentTransform.createConcatenation(node.getLocalToParentTransform());

        if (node instanceof Text text) {
            if (text.getFill() != null && text.getFill() != Color.TRANSPARENT) {
                paths.addAll(createIndependentGlyphPaths(text, nodeTransform));
            }
        } else if (node instanceof Group group) {
            for (Node child : group.getChildren()) {
                extractFilledLetterPaths(child, nodeTransform, paths);
            }
        }
    }

    private static List<Path> createIndependentGlyphPaths(Text source, Transform nodeTransform) {
        List<Path> paths = new ArrayList<>();
        
        // Create a pristine clone WITHOUT transforms (Scale, Translate, etc) 
        // because Shape.union applies the node's transforms automatically!
        // If we don't clone, the path gets double-scaled when we apply nodeTransform later.
        Text clone = new Text(source.getText());
        clone.setFont(source.getFont());
        clone.setX(source.getX());
        clone.setY(source.getY());
        clone.setTextOrigin(source.getTextOrigin());
        clone.setBoundsType(source.getBoundsType());
        clone.setFontSmoothingType(source.getFontSmoothingType());
        clone.setTextAlignment(source.getTextAlignment());
        clone.setLineSpacing(source.getLineSpacing());
        clone.setWrappingWidth(source.getWrappingWidth());
        
        // Remove stroke so we only get the fill geometry
        clone.setStrokeWidth(0);
        clone.setStroke(javafx.scene.paint.Color.TRANSPARENT);
        
        Path rawPath = toPath(clone);
        if (rawPath == null || rawPath.getElements().isEmpty()) return paths;

        List<Path> subPaths = new ArrayList<>();
        Path currentSubPath = null;
        List<javafx.scene.shape.PathElement> elements = new ArrayList<>(rawPath.getElements());
        rawPath.getElements().clear();

        for (javafx.scene.shape.PathElement element : elements) {
            if (element instanceof javafx.scene.shape.MoveTo) {
                if (currentSubPath != null) {
                    subPaths.add(currentSubPath);
                }
                currentSubPath = new Path();
            }
            if (currentSubPath != null) {
                currentSubPath.getElements().add(element);
            }
        }
        if (currentSubPath != null) subPaths.add(currentSubPath);

        subPaths.sort((p1, p2) -> {
            javafx.geometry.Bounds b1 = p1.getBoundsInLocal();
            javafx.geometry.Bounds b2 = p2.getBoundsInLocal();
            double area1 = b1.getWidth() * b1.getHeight();
            double area2 = b2.getWidth() * b2.getHeight();
            return Double.compare(area2, area1);
        });

        List<List<Path>> groups = new ArrayList<>();
        for (Path sub : subPaths) {
            javafx.geometry.Bounds subBounds = sub.getBoundsInLocal();
            boolean foundParent = false;
            for (int i = groups.size() - 1; i >= 0; i--) {
                List<Path> group = groups.get(i);
                Path parent = group.get(0);
                if (parent.getBoundsInLocal().contains(subBounds)) {
                    group.add(sub);
                    foundParent = true;
                    break;
                }
            }
            if (!foundParent) {
                List<Path> newGroup = new ArrayList<>();
                newGroup.add(sub);
                groups.add(newGroup);
            }
        }

        for (List<Path> group : groups) {
            Path combined = new Path();
            combined.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
            for (Path p : group) {
                combined.getElements().addAll(p.getElements());
            }
            Path transformed = VectorBooleanHelper.transformPath(combined, nodeTransform);
            VectorBooleanHelper.ensureClosed(transformed);
            paths.add(transformed);
        }

        return paths;
    }

    private static Path toPath(Shape shape) {
        if (shape == null) return null;
        Shape pathShape = Shape.union(shape, new Path());
        return (pathShape instanceof Path) ? (Path) pathShape : null;
    }

}
