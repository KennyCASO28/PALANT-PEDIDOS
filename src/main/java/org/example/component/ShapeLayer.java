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
public class ShapeLayer extends Group implements GraphicLayer {

    // --- Core Delegates ---
    private final ShapeLayerState state = new ShapeLayerState();
    private final ShapeTransformManager transformManager;
    private final ShapeLayerOrchestrator orchestrator;
    private PrendaVisualizer visualizer;

    // --- UI Components ---
    private final Group shapeGroup = new Group();
    private final Group contentGroup = new Group();
    private final Group contourGroup = new Group();
    private final Group handlesGroup = new Group();
    private Shape currentShapeNode;
    public Shape getCurrentShapeNode() { return currentShapeNode; }

    // --- Transforms ---
    private final Rotate rotateTransform = new Rotate();
    private final Scale scaleTransform = new Scale();
    private final Shear shearTransform = new Shear();

    // --- State Management ---
    private final ShapeSelectionOverlaySupport.OverlayNodes overlayNodes;
    private final ShapeInteractionHandler interactionHandler;
    private final ShapeStyleOrchestrator styleOrchestrator;

    // UI Transient State
    private boolean isSelected = false;
    private boolean isRotationMode = false;
    private boolean isBeingEdited = false;
    private boolean isNodeEditing = false;
    private boolean isArcEditingMode = false;
    private boolean isGrouped = false;
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

        this.transformManager = new ShapeTransformManager(this, rotateTransform, scaleTransform, shearTransform);
        this.orchestrator = new ShapeLayerOrchestrator(this);

        initializeUI();
        this.overlayNodes = ShapeSelectionOverlaySupport.createOverlayNodes(shearTransform, handlesGroup);
        this.interactionHandler = new ShapeInteractionHandler(this, overlayNodes);
        this.styleOrchestrator = new ShapeStyleOrchestrator(this, contourGroup);

        this.interactionHandler.init();
        render();
    }

    // --- Package-private accessors for managers ---
    public ShapeTransformManager getTransformManager() { return transformManager; }
    public ShapeLayerOrchestrator getOrchestrator() { return orchestrator; }

    private void initializeUI() {
        this.setPickOnBounds(false); // Hitbox should be the actual geometry, not the rectangular bounding box
        this.shapeGroup.setPickOnBounds(false);
        this.contentGroup.setPickOnBounds(false);
        this.contourGroup.setPickOnBounds(false);
        this.handlesGroup.setManaged(false);
        this.getChildren().addAll(contourGroup, shapeGroup, contentGroup, handlesGroup);
        this.getTransforms().addAll(rotateTransform, scaleTransform);
    }

    // --- GraphicLayer Implementation ---
    @Override public boolean isSelected() { return isSelected; }
    @Override public void setLocked(boolean l) { this.state.isUserLocked = l; refreshLockState(); }
    @Override public void setUserLocked(boolean l) { this.state.isUserLocked = l; refreshLockState(); }
    @Override public void setSystemLocked(boolean l) { this.state.isLocked = l; refreshLockState(); }
    @Override public boolean isLocked() { return state.isLocked || state.isUserLocked; }
    @Override public boolean isUserLocked() { return state.isUserLocked; }
    @Override public void setRotationMode(boolean b) { this.isRotationMode = b; updateVisuals(); }
    @Override public boolean isRotationMode() { return isRotationMode; }
    @Override public void setVisualizer(PrendaVisualizer v) { this.visualizer = v; }
    @Override public PrendaVisualizer getVisualizer() { return visualizer; }
    @Override public String getActiveZone() { return state.activeZone; }
    @Override public void setActiveZone(String z) { this.state.activeZone = z; setSystemLocked(z != null); }
    @Override public Node getNode() { return this; }
    @Override public void render() { renderShape(); }

    @Override public double getCustomPivotX() { return customPivotX; }
    @Override public void setCustomPivotX(double x) { customPivotX = x; }
    @Override public double getCustomPivotY() { return customPivotY; }
    @Override public void setCustomPivotY(double y) { customPivotY = y; }

    @Override
    public void recordUndoState() {
        this.undoStartX = getTranslateX(); this.undoStartY = getTranslateY();
        this.undoStartScaleX = transformManager.getInternalScaleX();
        this.undoStartScaleY = transformManager.getInternalScaleY();
        this.undoStartRotate = transformManager.getInternalRotation();
    }

    @Override
    public void updateVisuals() {
        double viewportScale = (visualizer != null && visualizer.getViewportController() != null) ? visualizer.getViewportController().getFinalScale() : 1.0;
        ShapeSelectionOverlaySupport.updateVisuals(overlayNodes,
            new ShapeSelectionOverlaySupport.VisualState(
                state.width, state.height, state.visualMinX, state.visualMinY,
                isNodeEditing, isRotationMode, isArcEditingMode, isLocked(),
                state.type, state.arcWidth, customPivotX, customPivotY,
                rotateTransform, scaleTransform, shearTransform, viewportScale));
    }

    // --- Transforms (delegated to ShapeTransformManager) ---
    @Override public double getInternalRotation() { return transformManager.getInternalRotation(); }
    @Override public void setInternalRotation(double a) { transformManager.setInternalRotation(a); }
    @Override public double getInternalScaleX() { return transformManager.getInternalScaleX(); }
    @Override public void setInternalScaleX(double s) { transformManager.setInternalScaleX(s); }
    @Override public double getInternalScaleY() { return transformManager.getInternalScaleY(); }
    @Override public void setInternalScaleY(double s) { transformManager.setInternalScaleY(s); }
    public void setInternalShearX(double x) { transformManager.setInternalShearX(x); }
    public void setInternalShearY(double y) { transformManager.setInternalShearY(y); }

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
        Point2D parentBefore = localToParent(0, 0);
        double pxLocal = lx - state.visualMinX;
        double pyLocal = ly - state.visualMinY;
        double cx = state.width / 2.0;
        double cy = state.height / 2.0;
        if (Math.sqrt(Math.pow(pxLocal - cx, 2) + Math.pow(pyLocal - cy, 2)) < 15) {
            customPivotX = -1; customPivotY = -1;
            if (overlayNodes != null && overlayNodes.pivotHandle() != null) {
                overlayNodes.pivotHandle().setScaleX(1.4);
                overlayNodes.pivotHandle().setScaleY(1.4);
            }
        } else {
            customPivotX = pxLocal; customPivotY = pyLocal;
            if (overlayNodes != null && overlayNodes.pivotHandle() != null) {
                overlayNodes.pivotHandle().setScaleX(1.0);
                overlayNodes.pivotHandle().setScaleY(1.0);
            }
        }
        updateVisuals();
        Point2D parentAfter = localToParent(0, 0);
        setTranslateX(getTranslateX() + (parentBefore.getX() - parentAfter.getX()));
        setTranslateY(getTranslateY() + (parentBefore.getY() - parentAfter.getY()));
    }

    // --- Rendering Logic (delegated to ShapeGeometryEngine) ---
    public void renderShape() {
        if ((shearTransform.getX() != 0 || shearTransform.getY() != 0) && state.type != ShapeType.CUSTOM_PATH) {
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
        ShapeGeometryEngine.applyGeometry(shape, state.type, state.bezierNodes,
                state.width, state.height, state.visualMinX, state.visualMinY,
                state.arcWidth, state.arcHeight, state.svgPathData, shearTransform);

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
        transformManager.setSizeWithOffset(w, h, ox, oy);
    }

    public void multiplyScale(double rx, double ry) {
        transformManager.multiplyScale(rx, ry, state.width, state.height);
    }

    public void multiplyShear(double sx, double sy) {
        transformManager.multiplyShear(sx, sy);
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
        transformManager.resetTransforms();
        org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(this);
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, after, getActiveZone()));
        }
    }

    private void refreshLockState() {
        boolean effective = isLocked();
        if (effective && !isBeingEdited) {
            if (!isSelected) handlesGroup.setVisible(false);
            else handlesGroup.setVisible(true);
            shapeGroup.setCursor(Cursor.DEFAULT);
        } else {
            if (isSelected) handlesGroup.setVisible(true);
            else handlesGroup.setVisible(false);
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
    public boolean isGrouped() { return isGrouped; }
    public void setGrouped(boolean g) { this.isGrouped = g; }

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
    public double getInternalShearX() { return transformManager.getInternalShearX(); }
    public double getInternalShearY() { return transformManager.getInternalShearY(); }

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
    @Override
    public void setSelected(boolean s) {
        this.isSelected = s;
        handlesGroup.setVisible(s && !isNodeEditing); // Mostrar handles aunque esté bloqueado (Ctrl+Click)
        updateVisuals();
    }

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

        clone.setTranslateX(getTranslateX());
        clone.setTranslateY(getTranslateY());
        clone.transformManager.setInternalRotation(transformManager.getInternalRotation());
        clone.transformManager.setInternalScaleX(transformManager.getInternalScaleX());
        clone.transformManager.setInternalScaleY(transformManager.getInternalScaleY());
        clone.setInternalShearX(transformManager.getInternalShearX());
        clone.setInternalShearY(transformManager.getInternalShearY());

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
            if (oldIdx != newIdx && visualizer != null && visualizer.getHistoryManager() != null) {
                visualizer.getHistoryManager().addCommand(new org.example.pattern.ZOrderCommand(visualizer.getUserLayerManager(), this, oldIdx, newIdx));
            }
        }
    }
    public void zSendToBack() { 
        if (getParent() instanceof Group g) {
            int oldIdx = g.getChildren().indexOf(this);
            toBack(); 
            int newIdx = g.getChildren().indexOf(this);
            if (oldIdx != newIdx && visualizer != null && visualizer.getHistoryManager() != null) {
                visualizer.getHistoryManager().addCommand(new org.example.pattern.ZOrderCommand(visualizer.getUserLayerManager(), this, oldIdx, newIdx));
            }
        }
    }
    public void zBringForward() {
        if (getParent() instanceof Group g) {
            int idx = g.getChildren().indexOf(this);
            if (idx < g.getChildren().size() - 1) {
                g.getChildren().remove(this);
                g.getChildren().add(idx + 1, this);
                if (visualizer != null && visualizer.getHistoryManager() != null) {
                    visualizer.getHistoryManager().addCommand(new org.example.pattern.ZOrderCommand(visualizer.getUserLayerManager(), this, idx, idx + 1));
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
                if (visualizer != null && visualizer.getHistoryManager() != null) {
                    visualizer.getHistoryManager().addCommand(new org.example.pattern.ZOrderCommand(visualizer.getUserLayerManager(), this, idx, idx - 1));
                }
            }
        }
    }

    public ShapeLayer createClone() { return orchestrator.createClone(); }

    // --- Flip (delegated) ---
    public void flipHorizontal() { 
        org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
        transformManager.flipHorizontal(); 
        org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(this);
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, after, getActiveZone()));
        }
    }
    public void flipVertical() { 
        org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
        transformManager.flipVertical(); 
        org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(this);
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, after, getActiveZone()));
        }
    }

    public void insertIntoClip(Node n) { contentGroup.getChildren().add(n); }
    public boolean hasMoved() { return getTranslateX() != 0 || getTranslateY() != 0; }
    public void rotateBy(double d) { 
        org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
        transformManager.rotateBy(d); 
        org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(this);
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, after, getActiveZone()));
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

        if (visualizer != null && visualizer.getHistoryManager() != null && (!java.util.Objects.equals(oldColor, color) || oldSteps != steps || oldDist != dist)) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.PropertyChangeCommand<>(
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

        if (visualizer != null && visualizer.getHistoryManager() != null && (oldEnabled != enabled || oldAngle != angle || oldStart != start || oldEnd != end)) {
            visualizer.getHistoryManager().addCommand(new org.example.pattern.PropertyChangeCommand<>(
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
        if (visualizer != null)
            visualizer.getHistoryManager().addCommand(
                new org.example.pattern.TransformCommand(this,
                    otx, oty, getInternalScaleX(), getInternalScaleY(), getInternalRotation(),
                    ow, oh, omx, omy, ntx, nty, getInternalScaleX(), getInternalScaleY(),
                    getInternalRotation(), nw, nh, nmx, nmy, state.activeZone));
    }
}
