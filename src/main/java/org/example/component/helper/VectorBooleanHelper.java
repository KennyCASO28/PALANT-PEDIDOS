package org.example.component.helper;

import javafx.scene.shape.*;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point2D;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Affine;
import javafx.scene.Node;
import org.example.component.ShapeLayer;
import org.example.component.PrendaVisualizer;
import org.example.model.ShapeType;

public class VectorBooleanHelper {

    /**
     * Une multiples ShapeLayers (o Shapes) en un solo Path de JavaFX.
     * Retorna null si la lista esta vacia.
     */
    public static Shape weldShapes(List<Shape> shapes) {
        if (shapes == null || shapes.isEmpty()) return null;
        if (shapes.size() == 1) return Shape.union(shapes.get(0), shapes.get(0));

        Shape result = shapes.get(0);
        for (int i = 1; i < shapes.size(); i++) {
            Shape next = shapes.get(i);
            if (next != null) {
                result = Shape.union(result, next);
            }
        }
        return result;
    }

    private static PathElement copyElement(PathElement element) {
        if (element instanceof MoveTo m) {
            return new MoveTo(m.getX(), m.getY());
        } else if (element instanceof LineTo l) {
            return new LineTo(l.getX(), l.getY());
        } else if (element instanceof CubicCurveTo c) {
            return new CubicCurveTo(c.getControlX1(), c.getControlY1(), c.getControlX2(), c.getControlY2(), c.getX(), c.getY());
        } else if (element instanceof QuadCurveTo q) {
            return new QuadCurveTo(q.getControlX(), q.getControlY(), q.getX(), q.getY());
        } else if (element instanceof ArcTo a) {
            return new ArcTo(a.getRadiusX(), a.getRadiusY(), a.getXAxisRotation(), a.getX(), a.getY(), a.isLargeArcFlag(), a.isSweepFlag());
        } else if (element instanceof HLineTo h) {
            return new HLineTo(h.getX());
        } else if (element instanceof VLineTo v) {
            return new VLineTo(v.getY());
        }
        return new ClosePath();
    }

    /**
     * Extrae un SVG Path string de un JavaFX Path element.
     */
    public static String convertPathToSvg(Path path) {
        if (path == null || path.getElements().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        
        for (PathElement el : path.getElements()) {
            if (el instanceof MoveTo) {
                MoveTo m = (MoveTo) el;
                sb.append("M ").append(format(m.getX())).append(" ").append(format(m.getY())).append(" ");
            } else if (el instanceof LineTo) {
                LineTo l = (LineTo) el;
                sb.append("L ").append(format(l.getX())).append(" ").append(format(l.getY())).append(" ");
            } else if (el instanceof CubicCurveTo) {
                CubicCurveTo c = (CubicCurveTo) el;
                sb.append("C ")
                  .append(format(c.getControlX1())).append(" ").append(format(c.getControlY1())).append(" ")
                  .append(format(c.getControlX2())).append(" ").append(format(c.getControlY2())).append(" ")
                  .append(format(c.getX())).append(" ").append(format(c.getY())).append(" ");
            } else if (el instanceof QuadCurveTo) {
                QuadCurveTo q = (QuadCurveTo) el;
                sb.append("Q ")
                  .append(format(q.getControlX())).append(" ").append(format(q.getControlY())).append(" ")
                  .append(format(q.getX())).append(" ").append(format(q.getY())).append(" ");
            } else if (el instanceof ClosePath) {
                sb.append("Z ");
            } else if (el instanceof ArcTo) {
                ArcTo a = (ArcTo) el;
                sb.append("A ")
                  .append(format(a.getRadiusX())).append(" ").append(format(a.getRadiusY())).append(" ")
                  .append(format(a.getXAxisRotation())).append(" ")
                  .append(a.isLargeArcFlag() ? "1 " : "0 ")
                  .append(a.isSweepFlag() ? "1 " : "0 ")
                  .append(format(a.getX())).append(" ").append(format(a.getY())).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Ensures every subpath is explicitly closed. Text outlines should behave as
     * filled closed vectors, not open strokes.
     */
    public static void ensureClosed(Path path) {
        if (path == null || path.getElements().isEmpty()) return;

        List<PathElement> closed = new ArrayList<>();
        boolean subPathOpen = false;

        for (PathElement element : path.getElements()) {
            if (element instanceof MoveTo) {
                if (subPathOpen) {
                    closed.add(new ClosePath());
                }
                subPathOpen = true;
            } else if (element instanceof ClosePath) {
                subPathOpen = false;
            }
            closed.add(copyElement(element));
        }

        if (subPathOpen) {
            closed.add(new ClosePath());
        }

        path.getElements().setAll(closed);
    }

    /**
     * Aplica un Transform a un JavaFX Path y devuelve un nuevo Path transformado.
     */
    public static Path transformPath(Path path, Transform transform) {
        Path result = new Path();
        result.setFillRule(path.getFillRule());
        
        for (PathElement el : path.getElements()) {
            if (el instanceof MoveTo) {
                MoveTo m = (MoveTo) el;
                Point2D p = transform.transform(m.getX(), m.getY());
                result.getElements().add(new MoveTo(p.getX(), p.getY()));
            } else if (el instanceof LineTo) {
                LineTo l = (LineTo) el;
                Point2D p = transform.transform(l.getX(), l.getY());
                result.getElements().add(new LineTo(p.getX(), p.getY()));
            } else if (el instanceof CubicCurveTo) {
                CubicCurveTo c = (CubicCurveTo) el;
                Point2D c1 = transform.transform(c.getControlX1(), c.getControlY1());
                Point2D c2 = transform.transform(c.getControlX2(), c.getControlY2());
                Point2D p = transform.transform(c.getX(), c.getY());
                result.getElements().add(new CubicCurveTo(c1.getX(), c1.getY(), c2.getX(), c2.getY(), p.getX(), p.getY()));
            } else if (el instanceof QuadCurveTo) {
                QuadCurveTo q = (QuadCurveTo) el;
                Point2D c1 = transform.transform(q.getControlX(), q.getControlY());
                Point2D p = transform.transform(q.getX(), q.getY());
                result.getElements().add(new QuadCurveTo(c1.getX(), c1.getY(), p.getX(), p.getY()));
            } else if (el instanceof ClosePath) {
                result.getElements().add(new ClosePath());
            } else if (el instanceof ArcTo) {
                // Arc transform is complex, fallback to converting ArcTo to curves first if needed.
                // For Text nodes, Shape.union typically returns only Curves and Lines.
                ArcTo a = (ArcTo) el;
                Point2D p = transform.transform(a.getX(), a.getY());
                ArcTo newA = new ArcTo(a.getRadiusX(), a.getRadiusY(), a.getXAxisRotation(), p.getX(), p.getY(), a.isLargeArcFlag(), a.isSweepFlag());
                result.getElements().add(newA);
            }
        }
        return result;
    }

    public static ShapeLayer createShapeLayerFromPath(Path originalPath, javafx.scene.paint.Color fill,
                                                      javafx.scene.paint.Color stroke, double strokeWidth) {
        if (originalPath == null || originalPath.getElements().isEmpty()) return null;

        ensureClosed(originalPath);
        javafx.geometry.Bounds bounds = calculateExactBounds(originalPath);
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) return null;

        Path normalizedPath = transformPath(originalPath,
                new javafx.scene.transform.Translate(-bounds.getMinX(), -bounds.getMinY()));
        normalizedPath.setFillRule(originalPath.getFillRule());
        ensureClosed(normalizedPath);

        String svgData = convertPathToSvg(normalizedPath);
        if (svgData.isEmpty()) return null;

        ShapeLayer layer = new ShapeLayer(ShapeType.CUSTOM_PATH,
                fill != null ? fill : javafx.scene.paint.Color.BLACK,
                stroke != null ? stroke : javafx.scene.paint.Color.TRANSPARENT,
                Math.max(0, strokeWidth));
        layer.getState().isClosed = true;
        layer.getState().visualMinX = 0;
        layer.getState().visualMinY = 0;
        layer.getState().width = bounds.getWidth();
        layer.getState().height = bounds.getHeight();
        layer.getState().svgPathData = svgData;
        layer.getState().bezierNodes = convertPathToBezierNodes(normalizedPath);
        layer.getState().originalBezierNodes = ShapePathSupport.copyNodes(layer.getState().bezierNodes);
        layer.setTranslateX(bounds.getMinX());
        layer.setTranslateY(bounds.getMinY());
        layer.renderShape();
        layer.getOrchestrator().recalculateGeometricBounds();
        layer.updateVisuals();
        return layer;
    }

    /**
     * Rompe un Path compuesto en múltiples Paths independientes (Desoldar).
     * El algoritmo de separación más simple agrupa los comandos hasta el siguiente MoveTo.
     */
    public static List<Path> unweldPath(Path path) {
        List<Path> separated = new ArrayList<>();
        if (path == null || path.getElements().isEmpty()) return separated;

        List<Path> rawSubPaths = new ArrayList<>();
        Path currentSubPath = null;
        for (PathElement el : path.getElements()) {
            if (el instanceof MoveTo) {
                currentSubPath = new Path();
                currentSubPath.setFillRule(path.getFillRule());
                rawSubPaths.add(currentSubPath);
                currentSubPath.getElements().add(el);
            } else if (currentSubPath != null) {
                currentSubPath.getElements().add(el);
            }
        }

        // Post-process: Group holes (like inside 'O' or 'A') with their parent shapes.
        // A subpath is a hole if its bounds are completely contained within another subpath's bounds.
        boolean[] isHole = new boolean[rawSubPaths.size()];
        int[] parentIndex = new int[rawSubPaths.size()];
        for (int i = 0; i < rawSubPaths.size(); i++) parentIndex[i] = -1;

        for (int i = 0; i < rawSubPaths.size(); i++) {
            javafx.geometry.Bounds boundsI = rawSubPaths.get(i).getBoundsInLocal();
            for (int j = 0; j < rawSubPaths.size(); j++) {
                if (i == j) continue;
                javafx.geometry.Bounds boundsJ = rawSubPaths.get(j).getBoundsInLocal();
                // If I is fully contained within J, then I is a hole of J
                if (boundsJ.contains(boundsI)) {
                    // Find the SMALLEST parent that contains it
                    if (parentIndex[i] == -1 || rawSubPaths.get(parentIndex[i]).getBoundsInLocal().contains(boundsJ)) {
                        isHole[i] = true;
                        parentIndex[i] = j;
                    }
                }
            }
        }

        // Combine parents and their holes
        for (int i = 0; i < rawSubPaths.size(); i++) {
            if (!isHole[i]) {
                Path merged = new Path();
                merged.setFillRule(path.getFillRule());
                merged.getElements().addAll(rawSubPaths.get(i).getElements());
                for (int j = 0; j < rawSubPaths.size(); j++) {
                    if (isHole[j] && parentIndex[j] == i) {
                        merged.getElements().addAll(rawSubPaths.get(j).getElements());
                    }
                }
                separated.add(merged);
            }
        }

        return separated;
    }

    private static String format(double value) {
        return org.example.utils.GeometryUtility.format(value);
    }

    /**
     * Convierte un JavaFX Path en una lista de BezierNodes para poder editarlos individualmente.
     */
    public static List<org.example.model.BezierNode> convertPathToBezierNodes(Path path) {
        List<org.example.model.BezierNode> nodes = new ArrayList<>();
        if (path == null) return nodes;

        Point2D currentAnchor = new Point2D(0, 0);

        for (PathElement el : path.getElements()) {
            if (el instanceof MoveTo) {
                MoveTo m = (MoveTo) el;
                currentAnchor = new Point2D(m.getX(), m.getY());
                org.example.model.BezierNode bn = new org.example.model.BezierNode(currentAnchor, currentAnchor, currentAnchor);
                bn.isMoveTo = true;
                nodes.add(bn);
            } else if (el instanceof LineTo) {
                LineTo l = (LineTo) el;
                currentAnchor = new Point2D(l.getX(), l.getY());
                nodes.add(new org.example.model.BezierNode(currentAnchor, currentAnchor, currentAnchor));
            } else if (el instanceof CubicCurveTo) {
                CubicCurveTo c = (CubicCurveTo) el;
                Point2D c1 = new Point2D(c.getControlX1(), c.getControlY1());
                Point2D c2 = new Point2D(c.getControlX2(), c.getControlY2());
                currentAnchor = new Point2D(c.getX(), c.getY());

                if (!nodes.isEmpty()) {
                    org.example.model.BezierNode prev = nodes.get(nodes.size() - 1);
                    prev.control2 = c1;
                    prev.segmentType = org.example.model.BezierNode.SegmentType.CURVE;
                }

                org.example.model.BezierNode n = new org.example.model.BezierNode(currentAnchor, currentAnchor, currentAnchor);
                n.control1 = c2;
                n.segmentType = org.example.model.BezierNode.SegmentType.CURVE;
                nodes.add(n);
            } else if (el instanceof QuadCurveTo) {
                QuadCurveTo q = (QuadCurveTo) el;
                Point2D prevAnchor = nodes.isEmpty() ? new Point2D(0, 0) : nodes.get(nodes.size() - 1).anchor;
                Point2D ctrl = new Point2D(q.getControlX(), q.getControlY());
                currentAnchor = new Point2D(q.getX(), q.getY());

                // Convert Quad to Cubic
                Point2D c1 = new Point2D(
                        prevAnchor.getX() + (2.0 / 3.0) * (ctrl.getX() - prevAnchor.getX()),
                        prevAnchor.getY() + (2.0 / 3.0) * (ctrl.getY() - prevAnchor.getY())
                );
                Point2D c2 = new Point2D(
                        currentAnchor.getX() + (2.0 / 3.0) * (ctrl.getX() - currentAnchor.getX()),
                        currentAnchor.getY() + (2.0 / 3.0) * (ctrl.getY() - currentAnchor.getY())
                );

                if (!nodes.isEmpty()) {
                    org.example.model.BezierNode prev = nodes.get(nodes.size() - 1);
                    prev.control2 = c1;
                    prev.segmentType = org.example.model.BezierNode.SegmentType.CURVE;
                }

                org.example.model.BezierNode n = new org.example.model.BezierNode(currentAnchor, currentAnchor, currentAnchor);
                n.control1 = c2;
                n.segmentType = org.example.model.BezierNode.SegmentType.CURVE;
                nodes.add(n);
            }
        }
        return nodes;
    }

    public static javafx.geometry.Bounds calculateExactBounds(Path path) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        boolean hasPoints = false;
        
        PathElement lastMoveTo = null;
        boolean subpathHasGeometry = false;

        for (PathElement el : path.getElements()) {
            if (el instanceof MoveTo) {
                lastMoveTo = el;
                subpathHasGeometry = false;
            } else if (el instanceof LineTo || el instanceof CubicCurveTo || el instanceof QuadCurveTo || el instanceof ArcTo) {
                if (!subpathHasGeometry && lastMoveTo != null) {
                    MoveTo m = (MoveTo) lastMoveTo;
                    minX = Math.min(minX, m.getX());
                    minY = Math.min(minY, m.getY());
                    maxX = Math.max(maxX, m.getX());
                    maxY = Math.max(maxY, m.getY());
                    subpathHasGeometry = true;
                    hasPoints = true;
                }
                
                if (el instanceof LineTo) {
                    LineTo l = (LineTo) el;
                    minX = Math.min(minX, l.getX());
                    minY = Math.min(minY, l.getY());
                    maxX = Math.max(maxX, l.getX());
                    maxY = Math.max(maxY, l.getY());
                } else if (el instanceof CubicCurveTo) {
                    CubicCurveTo c = (CubicCurveTo) el;
                    minX = Math.min(minX, Math.min(c.getX(), Math.min(c.getControlX1(), c.getControlX2())));
                    minY = Math.min(minY, Math.min(c.getY(), Math.min(c.getControlY1(), c.getControlY2())));
                    maxX = Math.max(maxX, Math.max(c.getX(), Math.max(c.getControlX1(), c.getControlX2())));
                    maxY = Math.max(maxY, Math.max(c.getY(), Math.max(c.getControlY1(), c.getControlY2())));
                    subpathHasGeometry = true;
                    hasPoints = true;
                } else if (el instanceof QuadCurveTo) {
                    QuadCurveTo q = (QuadCurveTo) el;
                    minX = Math.min(minX, Math.min(q.getX(), q.getControlX()));
                    minY = Math.min(minY, Math.min(q.getY(), q.getControlY()));
                    maxX = Math.max(maxX, Math.max(q.getX(), q.getControlX()));
                    maxY = Math.max(maxY, Math.max(q.getY(), q.getControlY()));
                    subpathHasGeometry = true;
                    hasPoints = true;
                } else if (el instanceof ArcTo) {
                    ArcTo a = (ArcTo) el;
                    minX = Math.min(minX, a.getX() - a.getRadiusX());
                    minY = Math.min(minY, a.getY() - a.getRadiusY());
                    maxX = Math.max(maxX, a.getX() + a.getRadiusX());
                    maxY = Math.max(maxY, a.getY() + a.getRadiusY());
                }
                hasPoints = true;
            }
        }
        
        if (!hasPoints) {
            return new javafx.geometry.BoundingBox(0, 0, 0, 0);
        }
        return new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    public static void weldSelectedShapes(PrendaVisualizer visualizer, List<ShapeLayer> layersToWeld) {
        if (layersToWeld == null || layersToWeld.size() < 2 || visualizer == null) return;

        javafx.scene.paint.Color fill = null;
        javafx.scene.paint.Color stroke = null;
        double strokeWidth = 0;

        for (ShapeLayer layer : layersToWeld) {
            if (fill == null || javafx.scene.paint.Color.TRANSPARENT.equals(fill)) fill = layer.getFillColor();
            if (stroke == null || javafx.scene.paint.Color.TRANSPARENT.equals(stroke)) stroke = layer.getStrokeColor();
            if (layer.getStrokeWidth() > strokeWidth) strokeWidth = layer.getStrokeWidth();
        }

        List<Shape> shapesToUnion = new ArrayList<>();

        for (ShapeLayer layer : layersToWeld) {
            List<org.example.model.BezierNode> nodes = layer.getState().bezierNodes;

            // For primitive shapes (Rectangle, Circle, etc.) that have no bezier nodes, convert first
            if (nodes == null || nodes.isEmpty()) {
                ShapePathSupport.PathConversionResult res = ShapePathSupport.convertPrimitiveToPath(
                    layer.getCurrentShapeNode(),
                    layer.getState().width, layer.getState().height,
                    layer.getState().arcWidth, layer.getState().arcHeight,
                    layer.getState().isClosed);
                if (res != null) nodes = res.getNodes();
            }

            if (nodes == null || nodes.isEmpty()) continue;

            // Transform each bezier node from layer's local space to parent space.
            javafx.scene.transform.Transform t = layer.getLocalToParentTransform();
            List<org.example.model.BezierNode> parentNodes = new ArrayList<>();
            boolean firstNodeOfThisShape = true;
            for (org.example.model.BezierNode bn : nodes) {
                javafx.geometry.Point2D anchor = t.transform(bn.anchor);
                javafx.geometry.Point2D c1 = t.transform(bn.control1 != null ? bn.control1 : bn.anchor);
                javafx.geometry.Point2D c2 = t.transform(bn.control2 != null ? bn.control2 : bn.anchor);

                org.example.model.BezierNode tbn = new org.example.model.BezierNode(anchor, c1, c2);
                tbn.segmentType = bn.segmentType;
                tbn.isMoveTo = bn.isMoveTo || firstNodeOfThisShape;
                firstNodeOfThisShape = false;
                parentNodes.add(tbn);
            }

            // Build SVG path in parent space
            String svgData = ShapePathSupport.buildSvgPath(parentNodes, true);
            if (!svgData.isEmpty()) {
                SVGPath svgPath = new SVGPath();
                svgPath.setContent(svgData);
                shapesToUnion.add(svgPath);
            }
        }

        if (shapesToUnion.isEmpty()) return;

        // Perform actual constructive solid geometry (CSG) union in parent space
        Shape unionResult = shapesToUnion.get(0);
        for (int i = 1; i < shapesToUnion.size(); i++) {
            unionResult = Shape.union(unionResult, shapesToUnion.get(i));
        }

        // Convert the unioned shape into a standard JavaFX Path
        Path unionPath = (Path) Shape.union(unionResult, new Path());

        // Create the welded ShapeLayer using the existing helper
        ShapeLayer weldedLayer = createShapeLayerFromPath(unionPath, fill, stroke, strokeWidth);
        if (weldedLayer == null) return;

        ShapeLayer firstLayer = layersToWeld.get(0);
        weldedLayer.setActiveZone(firstLayer.getActiveZone());

        javafx.scene.Group parentGroup = null;
        if (firstLayer.getParent() instanceof javafx.scene.Group) {
            parentGroup = (javafx.scene.Group) firstLayer.getParent();
        }
        int insertionIndex = parentGroup != null ? parentGroup.getChildren().indexOf(firstLayer) : -1;

        // Collect original nodes for command
        List<javafx.scene.Node> originalNodes = new ArrayList<>(layersToWeld);
        List<org.example.pattern.NodeMemento> originalStates = new ArrayList<>();
        for (ShapeLayer layer : layersToWeld) {
            originalStates.add(new org.example.pattern.NodeMemento(layer));
        }

        // Add welded layer back into the same container at the same position
        if (parentGroup != null) {
            visualizer.addShapeLayerToContainer(weldedLayer, parentGroup, insertionIndex, true);
        } else {
            visualizer.addShapeLayer(weldedLayer);
        }

        boolean wasHistory = visualizer.getUserLayerManager().isPerformingHistoryAction();
        visualizer.getUserLayerManager().setPerformingHistoryAction(true);
        try {
            // Remove old layers
            for (ShapeLayer layer : layersToWeld) {
                visualizer.getUserLayerManager().removeLayer(layer);
            }
        } finally {
            visualizer.getUserLayerManager().setPerformingHistoryAction(wasHistory);
        }

        // Sync PowerClip container item state if in a zone
        if (weldedLayer.getActiveZone() != null) {
            SmartZoneContainer zoneContainer = visualizer.getPowerClipManager().getContainer(weldedLayer.getActiveZone());
            if (zoneContainer != null) {
                zoneContainer.updateItemState(weldedLayer);
            }
        }

        visualizer.getUserLayerManager().selectNode(weldedLayer);

        if (visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.VectorBooleanCommand(
                visualizer.getUserLayerManager(),
                originalNodes,
                java.util.Collections.singletonList(weldedLayer),
                org.example.pattern.VectorBooleanCommand.ActionType.WELD,
                weldedLayer.getActiveZone(),
                originalStates
            ));
        }
    }


    public static void unweldShape(PrendaVisualizer visualizer, ShapeLayer layer) {
        if (layer == null || layer.getType() != ShapeType.CUSTOM_PATH || visualizer == null) return;

        List<org.example.model.BezierNode> nodes = layer.getState().bezierNodes;
        if (nodes == null || nodes.isEmpty()) return;

        // Split the combined bezier node list into individual subpaths (each starts with isMoveTo=true)
        List<List<org.example.model.BezierNode>> subPaths = new ArrayList<>();
        List<org.example.model.BezierNode> currentSubPath = null;
        for (int i = 0; i < nodes.size(); i++) {
            org.example.model.BezierNode bn = nodes.get(i);
            if (i == 0 || bn.isMoveTo) {
                currentSubPath = new ArrayList<>();
                subPaths.add(currentSubPath);
            }
            if (currentSubPath != null) currentSubPath.add(bn);
        }

        if (subPaths.size() <= 1) return; // Nothing to unweld

        javafx.scene.Group parentGroup = (layer.getParent() instanceof javafx.scene.Group)
            ? (javafx.scene.Group) layer.getParent() : null;
        String activeZone = layer.getActiveZone();
        javafx.scene.paint.Color srcFill = layer.getFillColor();
        javafx.scene.paint.Color srcStroke = layer.getStrokeColor();
        double srcStrokeWidth = layer.getStrokeWidth();

        // The bezierNodes are in the welded ShapeLayer's LOCAL space.
        // getLocalToParentTransform() maps them to the parent container's space.
        javafx.scene.transform.Transform t = layer.getLocalToParentTransform();

        List<ShapeLayer> newLayers = new ArrayList<>();
        for (List<org.example.model.BezierNode> subPath : subPaths) {
            if (subPath.isEmpty()) continue;

            // Transform subpath nodes to parent space
            List<org.example.model.BezierNode> transformed = new ArrayList<>();
            for (org.example.model.BezierNode bn : subPath) {
                javafx.geometry.Point2D anchor = t.transform(bn.anchor);
                javafx.geometry.Point2D c1 = t.transform(bn.control1 != null ? bn.control1 : bn.anchor);
                javafx.geometry.Point2D c2 = t.transform(bn.control2 != null ? bn.control2 : bn.anchor);
                org.example.model.BezierNode tbn = new org.example.model.BezierNode(anchor, c1, c2);
                tbn.segmentType = bn.segmentType;
                tbn.isMoveTo = false; // each new layer is a single subpath
                transformed.add(tbn);
            }

            // Calculate bounds and normalize to (0,0)
            ShapePathSupport.BoundsData sBounds = ShapePathSupport.calculateBezierBounds(transformed);
            if (sBounds == null || sBounds.getWidth() <= 0 || sBounds.getHeight() <= 0) continue;

            double ox = sBounds.getMinX();
            double oy = sBounds.getMinY();
            List<org.example.model.BezierNode> normalized = new ArrayList<>();
            for (org.example.model.BezierNode bn : transformed) {
                javafx.geometry.Point2D a  = new javafx.geometry.Point2D(bn.anchor.getX()   - ox, bn.anchor.getY()   - oy);
                javafx.geometry.Point2D c1 = new javafx.geometry.Point2D(bn.control1.getX() - ox, bn.control1.getY() - oy);
                javafx.geometry.Point2D c2 = new javafx.geometry.Point2D(bn.control2.getX() - ox, bn.control2.getY() - oy);
                org.example.model.BezierNode nbn = new org.example.model.BezierNode(a, c1, c2);
                nbn.segmentType = bn.segmentType;
                nbn.isMoveTo = false;
                normalized.add(nbn);
            }

            String svgData = ShapePathSupport.buildSvgPath(normalized, true);
            if (svgData.isEmpty()) continue;

            ShapeLayer newLayer = new ShapeLayer(ShapeType.CUSTOM_PATH, srcFill, srcStroke, srcStrokeWidth);
            newLayer.getState().isClosed = true;
            newLayer.getState().visualMinX = 0;
            newLayer.getState().visualMinY = 0;
            newLayer.getState().width = sBounds.getWidth();
            newLayer.getState().height = sBounds.getHeight();
            newLayer.getState().svgPathData = svgData;
            newLayer.getState().bezierNodes = normalized;
            newLayer.getState().originalBezierNodes = ShapePathSupport.copyNodes(normalized);
            newLayer.setActiveZone(activeZone);
            newLayer.setTranslateX(sBounds.getMinX());
            newLayer.setTranslateY(sBounds.getMinY());
            newLayer.renderShape();
            newLayer.updateVisuals();
            newLayers.add(newLayer);
        }

        if (!newLayers.isEmpty()) {
            List<javafx.scene.Node> originalNodes = java.util.Collections.singletonList(layer);
            List<org.example.pattern.NodeMemento> originalStates = java.util.Collections.singletonList(new org.example.pattern.NodeMemento(layer));
            List<javafx.scene.Node> resultNodes = new ArrayList<>();

            boolean wasHistory = visualizer.getUserLayerManager().isPerformingHistoryAction();
            visualizer.getUserLayerManager().setPerformingHistoryAction(true);
            try {
                visualizer.getUserLayerManager().removeLayer(layer);
            } finally {
                visualizer.getUserLayerManager().setPerformingHistoryAction(wasHistory);
            }
            visualizer.getUserLayerManager().clearSelection();
            for (ShapeLayer nl : newLayers) {
                if (parentGroup != null) {
                    visualizer.getUserLayerManager().addLayerToContainer(nl, parentGroup, false);
                } else {
                    visualizer.getUserLayerManager().addLayer(nl);
                }
                visualizer.getUserLayerManager().addToSelection(nl);
                resultNodes.add(nl);
            }

            if (visualizer.getHistoryManager() != null) {
                visualizer.getHistoryManager().addCommand(new org.example.pattern.VectorBooleanCommand(
                    visualizer.getUserLayerManager(),
                    originalNodes,
                    resultNodes,
                    org.example.pattern.VectorBooleanCommand.ActionType.UNWELD,
                    activeZone,
                    originalStates
                ));
            }
        }
    }
}
