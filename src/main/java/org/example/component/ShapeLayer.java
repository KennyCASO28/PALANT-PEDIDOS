package org.example.component;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import org.example.model.BezierNode;
import org.example.model.ShapeLayerState;
import org.example.model.ShapeType;
import org.example.component.helper.*;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Interactive layer for Vector Shapes.
 * Desmonolitized: delegates geometry, transforms, contours, and cloning to specialized managers.
 */
public class ShapeLayer extends AbstractGraphicLayer {

    @Override
    public javafx.geometry.Bounds calculateBounds() {
        javafx.geometry.Bounds b = contentGroup.getBoundsInLocal();
        if (b.isEmpty() || b.getWidth() <= 0 || b.getHeight() <= 0) {
            return new javafx.geometry.BoundingBox(0, 0, 8, 8);
        }
        return new javafx.geometry.BoundingBox(
                b.getMinX(),
                b.getMinY(),
                b.getWidth(),
                b.getHeight()
        );
    }

    @Override
    protected javafx.scene.Node createSilhouetteNode() {
        if (currentShapeNode instanceof javafx.scene.shape.SVGPath) {
            javafx.scene.shape.SVGPath p = new javafx.scene.shape.SVGPath();
            p.setContent(((javafx.scene.shape.SVGPath)currentShapeNode).getContent());
            return p;
        } else if (currentShapeNode instanceof javafx.scene.shape.Path) {
            javafx.scene.shape.Path p = new javafx.scene.shape.Path();
            p.getElements().addAll(((javafx.scene.shape.Path)currentShapeNode).getElements());
            return p;
        } else if (currentShapeNode instanceof javafx.scene.shape.Rectangle) {
            javafx.scene.shape.Rectangle r = (javafx.scene.shape.Rectangle) currentShapeNode;
            return new javafx.scene.shape.Rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        } else if (currentShapeNode instanceof javafx.scene.shape.Ellipse) {
            javafx.scene.shape.Ellipse e = (javafx.scene.shape.Ellipse) currentShapeNode;
            return new javafx.scene.shape.Ellipse(e.getCenterX(), e.getCenterY(), e.getRadiusX(), e.getRadiusY());
        }
        javafx.geometry.Bounds b = calculateBounds();
        return new javafx.scene.shape.Rectangle(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
    }

    @Override
    public void multiplyShear(double sx, double sy) {
        setInternalShearX(getInternalShearX() + sx);
        setInternalShearY(getInternalShearY() + sy);
    }

    // --- Core Delegates ---
    private final ShapeLayerState state = new ShapeLayerState();
    private final ShapeLayerOrchestrator orchestrator;
    
    // --- UI Components ---
    private final Group shapeGroup = new Group();
        private final Group contourGroup = new Group();
    private final Group handlesGroup = new Group();
    private Shape currentShapeNode;
    public Shape getCurrentShapeNode() { return currentShapeNode; }

    // --- Transforms ---
    // (Utilizamos rotateTransform, scaleTransform y shearTransform heredados de AbstractGraphicLayer)

    // --- State Management ---
    private final ShapeStyleOrchestrator styleOrchestrator;

    // UI Transient State
        private boolean isRotationMode = false;
    private boolean isBeingEdited = false;
    private boolean isNodeEditing = false;
    private boolean isArcEditingMode = false;
        private double customPivotX = -1;
    private double customPivotY = -1;

    // Undo Buffer
    private double undoStartX, undoStartY, undoStartScaleX, undoStartScaleY, undoStartRotate;

    // Callbacks
    private Consumer<MouseEvent> onSelectionRequested;
    private BiConsumer<Double, Double> onDragHandler;
    private BiConsumer<Double, Double> onResizeHandler;
    private Runnable onDragReleased;
    private Consumer<ShapeLayer> editHandler;
    private Consumer<String> powerClipHandler;
    private Consumer<Node> internalPowerClipHandler;
    private Runnable editContentHandler;
    private Runnable pasteHandler;
    private Supplier<List<String>> availableZonesSupplier;

    public ShapeLayer(ShapeType type, Color fill, Color stroke, double strokeWidth) {
        this.state.type = type;
        this.state.fillColor = fill;
        this.state.strokeColor = stroke;
        this.state.strokeWidth = strokeWidth;

        this.orchestrator = new ShapeLayerOrchestrator(this);

        initializeUI();
        this.styleOrchestrator = new ShapeStyleOrchestrator(this, contourGroup);

        render();
    }

    // --- Package-private accessors for managers ---
    public ShapeLayerOrchestrator getOrchestrator() { return orchestrator; }

    private void initializeUI() {
        this.setPickOnBounds(false); 
        this.shapeGroup.setPickOnBounds(false);
        this.contentGroup.setPickOnBounds(false);
        this.contourGroup.setPickOnBounds(false);
        // Add specific children to contentGroup instead of this directly to inherit transforms
        this.contentGroup.getChildren().addAll(contourGroup, shapeGroup);
    }

    // --- GraphicLayer Implementation ---
























    // --- Transforms (delegated to ShapeTransformManager) ---

    // --- Transforms ---

    @Override
    public void setInternalScaleX(double s) {
        // ShapeLayer uses geometric scaling (setSize) for magnitude to prevent stroke distortion.
        // scaleTransform is only used for mirroring.
        double sign = Math.signum(s);
        super.setInternalScaleX(sign == 0 ? 1 : sign);
    }

    @Override
    public void setInternalScaleY(double s) {
        double sign = Math.signum(s);
        super.setInternalScaleY(sign == 0 ? 1 : sign);
    }

    @Override
    public void setInternalRotation(double angle) {
        super.setInternalRotation(angle);
    }

    public void setInternalShearX(double x) {
        super.setInternalShearX(x);
    }
    public void setInternalShearY(double y) {
        super.setInternalShearY(y);
    }

    // --- Geometry Management ---
    public double getWidth() { return state.width; }
    public void setWidth(double w) { this.state.width = w; refreshShapeVisuals(); }
    public double getHeight() { return state.height; }
    public void setHeight(double h) { this.state.height = h; refreshShapeVisuals(); }
    public void setPrefSize(double w, double h) { this.state.width = w; this.state.height = h; refreshShapeVisuals(); }
    public double getLogicalWidth() { return state.width; }
    public double getLogicalHeight() { return state.height; }
    public double getVisualMinX() { return state.visualMinX; }
    public double getVisualMinY() { return state.visualMinY; }
    public ShapeType getType() { return state.type; }
    public void setType(ShapeType t) { this.state.type = t; renderShape(); }
    public javafx.scene.shape.StrokeType getStrokeType() { return state.strokeType; }
    public void setStrokeType(javafx.scene.shape.StrokeType t) { this.state.strokeType = t; renderShape(); }

    public void updatePivot(double lx, double ly) {
        double pxLocal = lx - state.visualMinX;
        double pyLocal = ly - state.visualMinY;
        double cx = state.width / 2.0;
        double cy = state.height / 2.0;
        
        Point2D centerScene = localToScene(cx + state.visualMinX, cy + state.visualMinY);
        Point2D pScene = localToScene(lx, ly);
        double distScene = pScene.distance(centerScene);
        double SNAP_RADIUS_SCENE = 10.0; // 10 physical screen pixels

        if (distScene <= SNAP_RADIUS_SCENE) {
            updatePivotWithCompensation(0, 0);
        } else {
            updatePivotWithCompensation(pxLocal - cx, pyLocal - cy);
        }
    }

    // --- Rendering Logic (delegated to ShapeGeometryEngine) ---
    public void renderShape() {
        if ((getInternalShearX() != 0 || getInternalShearY() != 0) && state.type != ShapeType.CUSTOM_PATH) {
            convertPrimitiveToPath();
        }

        boolean reuseNode = false;
        if (currentShapeNode != null) {
            Shape expectedShape = ShapeGeometryEngine.createShapeNode(state.type);
            if (currentShapeNode.getClass().equals(expectedShape.getClass())) {
                reuseNode = true;
            }
        }

        Shape shape;
        if (reuseNode) {
            shape = currentShapeNode;
        } else {
            shapeGroup.getChildren().clear();
            shape = ShapeGeometryEngine.createShapeNode(state.type);
            currentShapeNode = shape;
        }

        updateFill();
        // Do not pass shearTransform to ShapeGeometryEngine anymore! It is handled by contentGroup!
        ShapeGeometryEngine.applyGeometry(shape, state.type, state.bezierNodes,
                state.width, state.height, state.visualMinX, state.visualMinY,
                state.arcWidth, state.arcHeight, state.svgPathData, null);

        shape.setStroke(state.strokeColor != null ? state.strokeColor : Color.TRANSPARENT);
        shape.setStrokeWidth(Math.max(0.001, state.strokeWidth));
        shape.setStrokeType(state.strokeType != null ? state.strokeType : StrokeType.CENTERED);
        shape.setStrokeLineJoin(state.strokeLineJoin);
        shape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        shape.setPickOnBounds(true);

        if (!reuseNode) {
            shapeGroup.getChildren().add(shape);
        }
        renderContour();
    }

    public void updateFill() {
        styleOrchestrator.updateFill(currentShapeNode, state.fillColor, state.isGradientTransparency,
                state.transparencyAngle, state.transparencyStartAlpha, state.transparencyEndAlpha, state.transparencyBalance);
    }

    public void renderContour() {
        styleOrchestrator.renderContour(state.contourSteps, state.contourDistance, state.contourColor,
                state.contourLineJoin, currentShapeNode, state.fillColor, state.strokeColor, state.strokeWidth);
    }

    public void refreshShapeVisuals() {
        orchestrator.recalculateGeometricBounds();
        renderShape();
        updateVisuals();
    }

    // --- Transformation Utilities (delegated to ShapeTransformManager) ---
    public void setSize(double nw, double nh) {
        double drX = (state.width > 0) ? (nw / state.width) : 1.0;
        double drY = (state.height > 0) ? (nh / state.height) : 1.0;
        if (state.bezierNodes != null) {
            ShapePathSupport.scaleNodes(state.bezierNodes, drX, drY);
            state.svgPathData = ShapePathSupport.buildSvgPath(state.bezierNodes, state.isClosed);
        }
        this.state.width = nw; this.state.height = nh;
        refreshShapeVisuals();
    }

    public void setSizeWithOffset(Double w, Double h, Double ox, Double oy) {
        if (w != null) state.width = w;
        if (h != null) state.height = h;
        if (ox != null) state.visualMinX = ox;
        if (oy != null) state.visualMinY = oy;
        refreshShapeVisuals();
    }

    public void multiplyScale(double rx, double ry) {
        if (rx < 0 || ry < 0) {
            double signX = Math.signum(rx);
            double signY = Math.signum(ry);
            setInternalScaleX(getInternalScaleX() * signX);
            setInternalScaleY(getInternalScaleY() * signY);
            rx = Math.abs(rx);
            ry = Math.abs(ry);
        }
        double newW = state.width * rx;
        double newH = state.height * ry;
        setSize(newW, newH);
    }

    

    public void convertPrimitiveToPath() {
        ShapePathSupport.PathConversionResult res = ShapePathSupport.convertPrimitiveToPath(
                currentShapeNode, state.width, state.height, state.arcWidth, state.arcHeight, state.isClosed);
        if (res != null) {
            this.state.bezierNodes = res.getNodes();
            this.state.originalBezierNodes = res.getOriginalNodes();
            this.state.type = ShapeType.CUSTOM_PATH;
            this.state.svgPathData = res.getSvgPathData();
            refreshShapeVisuals();
        }
    }

    public void resetTransforms() {
        org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
        setInternalRotation(0);
        setInternalScaleX(1);
        setInternalScaleY(1);
        setInternalShearX(0);
        setInternalShearY(0);
        org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(this);
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            getVisualizer().getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, after, getActiveZone()));
        }
    }

    private void refreshLockState() {
        boolean effective = isLocked();
        boolean showHandles = isSelected() && !isNodeEditing && !isArcEditingMode;
        
        if (effective && !isBeingEdited) {
            handlesGroup.setVisible(showHandles);
            shapeGroup.setCursor(Cursor.DEFAULT);
        } else {
            handlesGroup.setVisible(showHandles);
            shapeGroup.setCursor(Cursor.MOVE);
        }
        updateVisuals();
    }

    // --- Getters & Setters Delegation ---
    public ShapeLayerState getState() { return state; }
    public void setFillColor(Color c) { this.state.fillColor = c; updateFill(); }
    public Color getFillColor() { return state.fillColor; }
    public void setStrokeColor(Color c) { this.state.strokeColor = c; if (currentShapeNode != null) currentShapeNode.setStroke(c); }
    public Color getStrokeColor() { return state.strokeColor; }
    public void setStrokeWidth(double w) { this.state.strokeWidth = w; if (currentShapeNode != null) currentShapeNode.setStrokeWidth(w); }
    public double getStrokeWidth() { return state.strokeWidth; }
    public void setArcWidth(double w) { this.state.arcWidth = w; renderShape(); updateVisuals(); }
    public double getArcWidth() { return state.arcWidth; }
    public void setArcHeight(double h) { this.state.arcHeight = h; renderShape(); updateVisuals(); }
    public double getArcHeight() { return state.arcHeight; }

    public List<BezierNode> getBezierNodes() { return state.bezierNodes; }
    public void setBezierNodes(List<BezierNode> b) {
        this.state.bezierNodes = b;
        this.state.type = ShapeType.CUSTOM_PATH;
        // Rebuild the SVG path string from the restored nodes so renderShape() uses the correct path.
        if (b != null && !b.isEmpty()) {
            this.state.svgPathData = org.example.component.helper.ShapePathSupport.buildSvgPath(b, this.state.isClosed);
        }
        refreshShapeVisuals();
    }
    public void setSvgPathData(String d) { this.state.svgPathData = d; renderShape(); updateVisuals(); }
    public void fastUpdateSvgPathData(String d) {
        this.state.svgPathData = d;
        if (state.type == ShapeType.CUSTOM_PATH && currentShapeNode instanceof javafx.scene.shape.SVGPath) {
            ((javafx.scene.shape.SVGPath) currentShapeNode).setContent(d);
            for (Node child : contourGroup.getChildren()) {
                if (child instanceof javafx.scene.shape.SVGPath) {
                    ((javafx.scene.shape.SVGPath) child).setContent(d);
                }
            }
        } else {
            renderShape();
        }
        updateVisuals();
    }
    public String getSvgPathData() { return state.svgPathData; }
    public String getShapeContent() { return getSvgPathData(); }

    public boolean isArcEditingMode() { return isArcEditingMode; }
    public void setArcEditingMode(boolean m) { this.isArcEditingMode = m; updateVisuals(); }
    public void setIsBeingEdited(boolean b) { this.isBeingEdited = b; refreshLockState(); }
    public boolean isBeingEdited() { return isBeingEdited; }
    public void setIsNodeEditing(boolean b) { this.isNodeEditing = b; setSelected(isSelected); updateVisuals(); }
    public boolean isNodeEditing() { return isNodeEditing; }

    // --- Compatibility Methods ---
    public Group getShapeGroup() { return shapeGroup; }
    public Group getHandlesGroup() { return handlesGroup; }
    public void hideHandlesGroup() { handlesGroup.setVisible(false); }
    public void showHandlesGroup() { handlesGroup.setVisible(true); }

    public double getUndoStartX() { return undoStartX; }
    public double getUndoStartY() { return undoStartY; }
    public double getUndoStartScaleX() { return undoStartScaleX; }
    public double getUndoStartScaleY() { return undoStartScaleY; }
    public double getUndoStartRotate() { return undoStartRotate; }

    public double getInternalPivotX() { return rotateTransform.getPivotX(); }
    public double getInternalPivotY() { return rotateTransform.getPivotY(); }


    public Point2D getStableCenter() {
        Bounds b = getBoundsInLocal();
        return new Point2D((b.getMinX() + b.getMaxX()) / 2.0, (b.getMinY() + b.getMaxY()) / 2.0);
    }

    public void addRotation(double angle) { rotateBy(angle); }
    public void refreshPath() { refreshShapeVisuals(); }
    public Point2D shapeLocalToScene(Point2D p) { return localToScene(p); }

    public void updateBezierBounds(double x, double y, double w, double h) {
        this.state.visualMinX = x; this.state.visualMinY = y;
        this.state.width = w; this.state.height = h;
        updateVisuals();
    }
    public void updateBezierBounds(int x, int y, double w, double h) {
        updateBezierBounds((double) x, (double) y, w, h);
    }

    public ShapeType getShapeType() { return getType(); }
    public void setCustomPoints(java.util.ArrayList<Double> pts) { this.state.customPoints = pts; refreshShapeVisuals(); }

    public StrokeLineJoin getContourLineJoin() { return state.contourLineJoin; }
    public void setContourLineJoin(StrokeLineJoin sj) { this.state.contourLineJoin = sj; renderContour(); }

    public List<ShapeLayer> separateContours() { return orchestrator.separateContours(); }

    // --- Selection & Lifecycle ---


    public void removeFromParent() {
        if (getParent() instanceof Group g) g.getChildren().remove(this);
    }

    // --- Clipboard ---
    public static void clearClipboard() { ShapeClipboardSupport.clear(); }
    public static boolean hasClipboard() { return ShapeClipboardSupport.hasClipboard(); }
    public void copyToClipboard() { ShapeClipboardSupport.copy(this); }
    public void cutToClipboard() { ShapeClipboardSupport.cut(this); }
    public static ShapeLayer getClipboardCopy() { return ShapeClipboardSupport.getClipboardCopy(); }

    /**
     * Creates a deep clone preserving ALL state including contours, transforms, and transparency.
     * This is more robust than using the clipboard support for cloning.
     */
    public ShapeLayer createDeepClone() {
        ShapeLayer clone = new ShapeLayer(state.type, state.fillColor, state.strokeColor, state.strokeWidth);

        clone.state.width = state.width;
        clone.state.height = state.height;
        clone.state.visualMinX = state.visualMinX;
        clone.state.visualMinY = state.visualMinY;
        clone.state.arcWidth = state.arcWidth;
        clone.state.arcHeight = state.arcHeight;
        clone.state.isClosed = state.isClosed;
        clone.state.strokeLineJoin = state.strokeLineJoin;
        clone.state.strokeType = state.strokeType;
        clone.state.svgPathData = state.svgPathData;
        clone.state.bezierNodes = state.bezierNodes != null ? ShapePathSupport.copyNodes(state.bezierNodes) : null;
        clone.state.originalBezierNodes = state.originalBezierNodes != null ? ShapePathSupport.copyNodes(state.originalBezierNodes) : null;
        clone.state.customPoints = state.customPoints != null ? new java.util.ArrayList<>(state.customPoints) : null;
        clone.state.contourSteps = state.contourSteps;
        clone.state.contourDistance = state.contourDistance;
        clone.state.contourColor = state.contourColor;
        clone.state.contourLineJoin = state.contourLineJoin;
        clone.state.isGradientTransparency = state.isGradientTransparency;
        clone.state.transparencyAngle = state.transparencyAngle;
        clone.state.transparencyStartAlpha = state.transparencyStartAlpha;
        clone.state.transparencyEndAlpha = state.transparencyEndAlpha;
        clone.state.transparencyBalance = state.transparencyBalance;
        clone.state.activeZone = state.activeZone;
        clone.state.isLocked = state.isLocked;
        clone.state.isUserLocked = state.isUserLocked;

        clone.setVisualizer(visualizer);
        clone.renderShape();

        // Copy pivot offset before copying translations, so the compensation gets overwritten by the exact translation
        clone.updatePivotWithCompensation(getCustomPivotX(), getCustomPivotY());

        clone.setTranslateX(getTranslateX());
        clone.setTranslateY(getTranslateY());
        clone.setInternalRotation(getInternalRotation());
        clone.setInternalScaleX(getInternalScaleX());
        clone.setInternalScaleY(getInternalScaleY());
        clone.setInternalShearX(getInternalShearX());
        clone.setInternalShearY(getInternalShearY());

        if (state.contourSteps > 0) {
            clone.applyContour(state.contourSteps, state.contourDistance, state.contourColor);
        }

        if (state.isGradientTransparency) {
            clone.setTransparency(true, state.transparencyAngle, state.transparencyStartAlpha, state.transparencyEndAlpha);
            clone.setTransparencyBalance(state.transparencyBalance);
        }

        return clone;
    }

    // --- Z-Order ---
    public void zBringToFront() { 
        if (getParent() instanceof Group g) {
            int oldIdx = g.getChildren().indexOf(this);
            toFront(); 
            int newIdx = g.getChildren().indexOf(this);
            if (oldIdx != newIdx && getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
                getVisualizer().getHistoryManager().addCommand(new org.example.pattern.ZOrderCommand(getVisualizer().getUserLayerManager(), this, oldIdx, newIdx));
            }
        }
    }
    public void zSendToBack() { 
        if (getParent() instanceof Group g) {
            int oldIdx = g.getChildren().indexOf(this);
            toBack(); 
            int newIdx = g.getChildren().indexOf(this);
            if (oldIdx != newIdx && getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
                getVisualizer().getHistoryManager().addCommand(new org.example.pattern.ZOrderCommand(getVisualizer().getUserLayerManager(), this, oldIdx, newIdx));
            }
        }
    }
    public void zBringForward() {
        if (getParent() instanceof Group g) {
            int idx = g.getChildren().indexOf(this);
            if (idx < g.getChildren().size() - 1) {
                g.getChildren().remove(this);
                g.getChildren().add(idx + 1, this);
                if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
                    getVisualizer().getHistoryManager().addCommand(new org.example.pattern.ZOrderCommand(getVisualizer().getUserLayerManager(), this, idx, idx + 1));
                }
            }
        }
    }
    public void zSendBackward() {
        if (getParent() instanceof Group g) {
            int idx = g.getChildren().indexOf(this);
            if (idx > 0) {
                g.getChildren().remove(this);
                g.getChildren().add(idx - 1, this);
                if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
                    getVisualizer().getHistoryManager().addCommand(new org.example.pattern.ZOrderCommand(getVisualizer().getUserLayerManager(), this, idx, idx - 1));
                }
            }
        }
    }

    public ShapeLayer createClone() { return orchestrator.createClone(); }

    // --- Flip ---

    public void insertIntoClip(Node n) { contentGroup.getChildren().add(n); }
    public boolean hasMoved() { return getTranslateX() != 0 || getTranslateY() != 0; }
    public void rotateBy(double d) { 
        org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
        setInternalRotation(getInternalRotation() + d);
        org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(this);
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            getVisualizer().getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, after, getActiveZone()));
        }
    }

    // Callbacks Accessors
    public void setOnSelectionRequested(Consumer<MouseEvent> c) { this.onSelectionRequested = c; }
    public Consumer<MouseEvent> getOnSelectionRequested() { return onSelectionRequested; }
    public void setOnDragHandler(BiConsumer<Double, Double> c) { this.onDragHandler = c; }
    public BiConsumer<Double, Double> getOnDragHandler() { return onDragHandler; }
    public void setOnResizeHandler(BiConsumer<Double, Double> c) { this.onResizeHandler = c; }
    public BiConsumer<Double, Double> getOnResizeHandler() { return onResizeHandler; }
    public void setOnDragReleased(Runnable r) { this.onDragReleased = r; }
    public Runnable getOnDragReleased() { return onDragReleased; }
    public void setEditContentHandler(Runnable r) { this.editContentHandler = r; }
    public Runnable getEditContentHandler() { return editContentHandler; }
    public void setPowerClipHandler(Consumer<String> c) { this.powerClipHandler = c; }
    public Consumer<String> getPowerClipHandler() { return powerClipHandler; }
    public void setInternalPowerClipHandler(Consumer<Node> c) { this.internalPowerClipHandler = c; }
    public void setAvailableZonesSupplier(Supplier<List<String>> s) { this.availableZonesSupplier = s; }
    public void setPasteHandler(Runnable r) { this.pasteHandler = r; }
    public Runnable getPasteHandler() { return pasteHandler; }

    // Specialized state getters for Clipboard
    public boolean getIsClosed() { return state.isClosed; }
    public void setIsClosed(boolean b) { this.state.isClosed = b; }
    public int getContourSteps() { return state.contourSteps; }
    public double getContourDistance() { return state.contourDistance; }
    public Color getContourColor() { return state.contourColor; }
    public void applyContour(int steps, double dist, Color color) {
        int oldSteps = this.state.contourSteps;
        double oldDist = this.state.contourDistance;
        Color oldColor = this.state.contourColor;

        this.state.contourSteps = steps;
        this.state.contourDistance = dist;
        this.state.contourColor = color;
        renderContour();

        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && (!java.util.Objects.equals(oldColor, color) || oldSteps != steps || oldDist != dist)) {
            getVisualizer().getHistoryManager().addCommand(new org.example.pattern.PropertyChangeCommand<>(
                "Aplicar Contorno",
                new Object[]{oldSteps, oldDist, oldColor},
                new Object[]{steps, dist, color},
                (val) -> {
                    Object[] arr = (Object[]) val;
                    this.state.contourSteps = (Integer) arr[0];
                    this.state.contourDistance = (Double) arr[1];
                    this.state.contourColor = (Color) arr[2];
                    renderContour();
                }
            ));
        }
    }
    public boolean isTransparencyEnabled() { return state.isGradientTransparency; }
    public double getTransparencyAngle() { return state.transparencyAngle; }
    public double getTransparencyStartAlpha() { return state.transparencyStartAlpha; }
    public double getTransparencyEndAlpha() { return state.transparencyEndAlpha; }
    public double getTransparencyBalance() { return state.transparencyBalance; }
    public void setTransparency(boolean enabled, double angle, double start, double end) {
        boolean oldEnabled = this.state.isGradientTransparency;
        double oldAngle = this.state.transparencyAngle;
        double oldStart = this.state.transparencyStartAlpha;
        double oldEnd = this.state.transparencyEndAlpha;

        this.state.isGradientTransparency = enabled;
        this.state.transparencyAngle = angle;
        this.state.transparencyStartAlpha = start;
        this.state.transparencyEndAlpha = end;
        updateFill();

        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && (oldEnabled != enabled || oldAngle != angle || oldStart != start || oldEnd != end)) {
            getVisualizer().getHistoryManager().addCommand(new org.example.pattern.PropertyChangeCommand<>(
                "Aplicar Transparencia",
                new Object[]{oldEnabled, oldAngle, oldStart, oldEnd},
                new Object[]{enabled, angle, start, end},
                (val) -> {
                    Object[] arr = (Object[]) val;
                    this.state.isGradientTransparency = (Boolean) arr[0];
                    this.state.transparencyAngle = (Double) arr[1];
                    this.state.transparencyStartAlpha = (Double) arr[2];
                    this.state.transparencyEndAlpha = (Double) arr[3];
                    updateFill();
                }
            ));
        }
    }
    public void setTransparencyBalance(double b) { this.state.transparencyBalance = b; updateFill(); }

    public void recordResizeUndo(double ow, double oh, double omx, double omy,
                                  double nw, double nh, double nmx, double nmy,
                                  double otx, double oty, double ntx, double nty) {
        if (getVisualizer() != null)
            getVisualizer().getHistoryManager().addCommand(
                new org.example.pattern.TransformCommand(this,
                    otx, oty, getInternalScaleX(), getInternalScaleY(), getInternalRotation(),
                    ow, oh, omx, omy, ntx, nty, getInternalScaleX(), getInternalScaleY(),
                    getInternalRotation(), nw, nh, nmx, nmy, state.activeZone));
    }
}
