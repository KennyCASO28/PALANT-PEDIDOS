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

        // 1. Extract Fill paths (transformed to parent)
        List<Path> letterPaths = new ArrayList<>();
        extractFilledLetterPaths(textLayer.getTextGroup(), textLayer.getLocalToParentTransform(), letterPaths);

        // 2. Extract Stroke/Outline paths (as independent, uniform vector layers)
        List<Path> strokePaths = new ArrayList<>();
        if (textLayer.getStrokeWidth() > 0 && textLayer.getStrokeColor() != null && textLayer.getStrokeColor() != Color.TRANSPARENT) {
            double strokeWidth = textLayer.getStrokeWidth();
            for (Path letterPath : letterPaths) {
                // Convert to AWT path in parent coordinates
                java.awt.geom.Path2D.Double awtPath = fxPathToAWTPath(letterPath);
                
                // Create BasicStroke with smooth curves/joins
                java.awt.BasicStroke basicStroke = new java.awt.BasicStroke(
                        (float) strokeWidth,
                        java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND
                );
                
                java.awt.Shape strokedAWTShape = basicStroke.createStrokedShape(awtPath);
                
                // Unir la letra original y su contorno para obtener una figura cerrada y solida de fondo
                java.awt.geom.Area combinedArea = new java.awt.geom.Area(awtPath);
                combinedArea.add(new java.awt.geom.Area(strokedAWTShape));
                
                Path strokeFXPath = awtShapeToFXPath(combinedArea);
                
                if (strokeFXPath != null && !strokeFXPath.getElements().isEmpty()) {
                    VectorBooleanHelper.ensureClosed(strokeFXPath);
                    strokePaths.add(strokeFXPath);
                }
            }
        }

        // --- COMBINE AND ADD TO VECTOR LAYERS ---
        // 1. Stroke Layer (Combine into a single piece, put first so it renders in the back)
        if (!strokePaths.isEmpty()) {
            Path masterStrokePath = combinePaths(strokePaths);
            if (masterStrokePath != null) {
                ShapeLayer strokeLayer = VectorBooleanHelper.createShapeLayerFromPath(
                        masterStrokePath, textLayer.getStrokeColor(), Color.TRANSPARENT, 0);
                if (strokeLayer != null) {
                    if (textLayer.isDropShadowEnabled()) {
                        strokeLayer.getShapeGroup().setEffect(new javafx.scene.effect.DropShadow(10, textLayer.getDropShadowColor()));
                    }
                    vectorLayers.add(strokeLayer);
                }
            }
        }

        // 2. Fill Layer (Combine into a single piece, put second so it renders in the front)
        if (!letterPaths.isEmpty()) {
            Path masterFillPath = combinePaths(letterPaths);
            if (masterFillPath != null) {
                ShapeLayer fillLayer = VectorBooleanHelper.createShapeLayerFromPath(
                        masterFillPath, textLayer.getTextColor(), Color.TRANSPARENT, 0);
                if (fillLayer != null) {
                    if (textLayer.isDropShadowEnabled()) {
                        fillLayer.getShapeGroup().setEffect(new javafx.scene.effect.DropShadow(10, textLayer.getDropShadowColor()));
                    }
                    vectorLayers.add(fillLayer);
                }
            }
        }

        return vectorLayers;
    }

    /**
     * Combina una lista de Paths en un unico Path agregando todos sus elementos.
     */
    public static Path combinePaths(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return null;
        Path combined = new Path();
        combined.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        for (Path p : paths) {
            if (p != null) {
                combined.getElements().addAll(p.getElements());
            }
        }
        return combined;
    }

    public static ShapeLayer convertToShapeLayer(TextLayer textLayer) {
        if (textLayer == null) return null;

        List<ShapeLayer> layers = convertToShapeLayers(textLayer);
        if (layers.isEmpty()) return null;

        List<Path> pathsToCombine = new ArrayList<>();
        for (ShapeLayer layer : layers) {
            Shape current = layer.getCurrentShapeNode();
            if (current == null) continue;
            Path raw = toPath(current);
            if (raw != null) {
                pathsToCombine.add(VectorBooleanHelper.transformPath(raw, layer.getLocalToParentTransform()));
            }
        }

        Path masterPath = combinePaths(pathsToCombine);
        if (masterPath == null) return null;
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

    private static java.awt.geom.Path2D.Double fxPathToAWTPath(Path fxPath) {
        java.awt.geom.Path2D.Double awtPath = new java.awt.geom.Path2D.Double();
        awtPath.setWindingRule(fxPath.getFillRule() == javafx.scene.shape.FillRule.EVEN_ODD 
            ? java.awt.geom.Path2D.WIND_EVEN_ODD 
            : java.awt.geom.Path2D.WIND_NON_ZERO);

        for (javafx.scene.shape.PathElement element : fxPath.getElements()) {
            if (element instanceof javafx.scene.shape.MoveTo m) {
                awtPath.moveTo(m.getX(), m.getY());
            } else if (element instanceof javafx.scene.shape.LineTo l) {
                awtPath.lineTo(l.getX(), l.getY());
            } else if (element instanceof javafx.scene.shape.QuadCurveTo q) {
                awtPath.quadTo(q.getControlX(), q.getControlY(), q.getX(), q.getY());
            } else if (element instanceof javafx.scene.shape.CubicCurveTo c) {
                awtPath.curveTo(c.getControlX1(), c.getControlY1(), c.getControlX2(), c.getControlY2(), c.getX(), c.getY());
            } else if (element instanceof javafx.scene.shape.ClosePath) {
                awtPath.closePath();
            }
        }
        return awtPath;
    }

    private static Path awtShapeToFXPath(java.awt.Shape awtShape) {
        Path fxPath = new Path();
        fxPath.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        java.awt.geom.PathIterator iterator = awtShape.getPathIterator(null);
        float[] coords = new float[6];
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            switch (type) {
                case java.awt.geom.PathIterator.SEG_MOVETO:
                    fxPath.getElements().add(new javafx.scene.shape.MoveTo(coords[0], coords[1]));
                    break;
                case java.awt.geom.PathIterator.SEG_LINETO:
                    fxPath.getElements().add(new javafx.scene.shape.LineTo(coords[0], coords[1]));
                    break;
                case java.awt.geom.PathIterator.SEG_QUADTO:
                    fxPath.getElements().add(new javafx.scene.shape.QuadCurveTo(coords[0], coords[1], coords[2], coords[3]));
                    break;
                case java.awt.geom.PathIterator.SEG_CUBICTO:
                    fxPath.getElements().add(new javafx.scene.shape.CubicCurveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]));
                    break;
                case java.awt.geom.PathIterator.SEG_CLOSE:
                    fxPath.getElements().add(new javafx.scene.shape.ClosePath());
                    break;
            }
            iterator.next();
        }
        return fxPath;
    }

}
