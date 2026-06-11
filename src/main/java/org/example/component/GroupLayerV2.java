package org.example.component;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.layout.StackPane;
import javafx.scene.DepthTest;
import org.example.pattern.TransformCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * GroupLayerV2 - FIXED ADDCHILD & RESIZE
 * - Soluciona: Glitch visual (DepthTest Disable)
 * - Soluciona: Estirado (Scale Transform)
 * - Soluciona: Agrupado fallido (Respetar coordenadas originales)
 */
public class GroupLayerV2 extends Group {

    // ============================================
    // ESTRUCTURA
    // ============================================
    private final Group contentGroup;
    private final List<Node> userLayers = new ArrayList<>();

    // UI de Selección
    private final Group selectionOverlay;
    private final Rectangle border;
    private final StackPane topLeft, topRight, bottomLeft, bottomRight;
    private final StackPane topCenter, bottomCenter, leftCenter, rightCenter;
    private final javafx.scene.Group pivotHandle;
    private final StackPane rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight;
    private final StackPane shearTop, shearBottom, shearLeft, shearRight;
    private final Rectangle selectionHitArea;

    // TRANSFORMACIONES (Aplicadas al contentGroup)
    private final Scale scaleTransform = new Scale(1, 1);
    private final Rotate rotateTransform = new Rotate(0);
    private final Shear shearTransform = new Shear(0, 0);

    // TRANSFORMACIONES ESPEJO (Para selectionOverlay)
    private final Scale overlayScaleTransform = new Scale(1, 1);
    private final Rotate overlayRotateTransform = new Rotate(0);
    private final Shear overlayShearTransform = new Shear(0, 0);

    // ESTADO
    private boolean isSelected = false;
    private boolean isSystemLocked = false;
    private boolean isUserLocked = false;
    private boolean isRotationMode = false;
    private PrendaVisualizer visualizer;

    // CACHÉ DE BOUNDS
    private Bounds cachedBounds;
    private boolean boundsDirty = true;

    private String activeZone = null;

    public void setActiveZone(String activeZone) {
        this.activeZone = activeZone;
        setSystemLocked(activeZone != null);
    }

    public String getActiveZone() {
        return activeZone;
    }

    public GroupLayerV2() {
        setId("USER_GROUP");

        // 1. CONFIGURACIÓN ANTI-GLITCH (CRÍTICO)
        // Forzamos renderizado plano para evitar Z-Fighting y artefactos visuales
        setDepthTest(DepthTest.DISABLE);
        setCache(false);
        setPickOnBounds(true);

        // 2. Content Group (Donde van tus figuras)
        contentGroup = new Group();
        contentGroup.setDepthTest(DepthTest.DISABLE);
        contentGroup.setCache(false);

        // IMPORTANTE: Transformaciones para permitir rotación y sesgo
        contentGroup.getTransforms().addAll(rotateTransform, shearTransform);

        // 3. Selection Overlay
        selectionOverlay = new Group();
        selectionOverlay.setVisible(false);
        selectionOverlay.setManaged(false);
        selectionOverlay.setDepthTest(DepthTest.DISABLE);
        selectionOverlay.setCache(false);

        // Borde
        border = new Rectangle();
        border.setFill(null);
        border.setStroke(Color.web("#0047AB"));
        border.setStrokeWidth(1);
        border.setMouseTransparent(true);
        border.setDepthTest(DepthTest.DISABLE);

        // Handles
        topLeft = createHandle(Cursor.NW_RESIZE);
        topRight = createHandle(Cursor.NE_RESIZE);
        bottomLeft = createHandle(Cursor.SW_RESIZE);
        bottomRight = createHandle(Cursor.SE_RESIZE);
        topCenter = createHandle(Cursor.N_RESIZE);
        bottomCenter = createHandle(Cursor.S_RESIZE);
        leftCenter = createHandle(Cursor.W_RESIZE);
        rightCenter = createHandle(Cursor.E_RESIZE);

        // Advanced Handles
        rotTopLeft = createRotHandle();
        rotTopRight = createRotHandle();
        rotBottomLeft = createRotHandle();
        rotBottomRight = createRotHandle();

        shearTop = createShearHandle(Cursor.H_RESIZE, true);
        shearBottom = createShearHandle(Cursor.H_RESIZE, true);
        shearLeft = createShearHandle(Cursor.V_RESIZE, false);
        shearRight = createShearHandle(Cursor.V_RESIZE, false);

        selectionOverlay.getChildren().addAll(border, topLeft, topRight, bottomLeft, bottomRight,
                topCenter, bottomCenter, leftCenter, rightCenter,
                rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight,
                shearTop, shearBottom, shearLeft, shearRight);

        selectionHitArea = new Rectangle();
        selectionHitArea.setFill(Color.web("#ffffff", 0.01)); // Translucid
        selectionHitArea.setPickOnBounds(true);
        selectionHitArea.setDepthTest(DepthTest.DISABLE);
        selectionHitArea.setCache(false);

        // Bind overlay transforms securely
        overlayScaleTransform.xProperty().bind(scaleTransform.xProperty());
        overlayScaleTransform.yProperty().bind(scaleTransform.yProperty());
        overlayRotateTransform.angleProperty().bind(rotateTransform.angleProperty());
        overlayShearTransform.xProperty().bind(shearTransform.xProperty());
        overlayShearTransform.yProperty().bind(shearTransform.yProperty());

        // CRITICAL: We DO NOT add overlayScaleTransform to
        // selectionOverlay.getTransforms().
        // This ensures handles stay circular regardless of logical scale.
        selectionOverlay.getTransforms().addAll(overlayRotateTransform, overlayShearTransform);

        getChildren().addAll(selectionHitArea, contentGroup, selectionOverlay);

        // Pivot Handle
        pivotHandle = createPivotHandle();
        selectionOverlay.getChildren().add(pivotHandle);

        // Shear handles
        initShearHandles();

        // --- SCALE ABSORPTION (PREVENT SQUASHED HANDLES) ---
        // Final protection: if root scale is changed (e.g. by undo memento), absorb
        // into geometry.
        scaleXProperty().addListener((obs, oldValue, newValue) -> {
            double val = newValue.doubleValue();
            if (Math.abs(val - 1.0) > 0.001) {
                javafx.application.Platform.runLater(() -> {
                    Point2D ref = contentGroup.localToScene(0, 0);
                    multiplyScale(val, 1.0);
                    compensateScalingPosition(ref);
                    setScaleX(1.0);
                });
            }
        });
        scaleYProperty().addListener((obs, oldValue, newValue) -> {
            double val = newValue.doubleValue();
            if (Math.abs(val - 1.0) > 0.001) {
                javafx.application.Platform.runLater(() -> {
                    Point2D ref = contentGroup.localToScene(0, 0);
                    multiplyScale(1.0, val);
                    compensateScalingPosition(ref);
                    setScaleY(1.0);
                });
            }
        });

        setupDragHandlers();
        setupResizeHandlers();
        setupRotateHandler();
        setupShearHandlers();
        setupPivotHandler();


    }

    public void setInternalScale(double sx, double sy) {
        scaleTransform.setX(sx);
        scaleTransform.setY(sy);
        invalidateBounds();
        updateSelectionOverlay();
    }

    private StackPane createHandle(Cursor cursor) {
        // Selection handle: White square with Blue border, size 4
        StackPane handle = org.example.utils.UIFactory.crearSquareHandle(null, 4, "#0047AB",
                "#ffffff", cursor);
        org.example.utils.GeometryUtility.applyAntiShear(handle, shearTransform, 2, 2);
        return handle;
    }

    private javafx.scene.Group createPivotHandle() {
        javafx.scene.Group handle = org.example.utils.UIFactory.crearPivotHandle();
        handle.setVisible(false);
        handle.setManaged(false);
        org.example.utils.GeometryUtility.applyAntiShear(handle, shearTransform, 8, 8);
        return handle;
    }

    private StackPane createRotHandle() {
        StackPane handle = org.example.utils.UIFactory.crearIconHandle("mdi2r-rotate-right", 16,
                "#e8a020", Cursor.HAND);
        handle.setVisible(false);
        org.example.utils.GeometryUtility.applyAntiShear(handle, shearTransform, 8, 8);
        return handle;
    }

    private StackPane createShearHandle(Cursor cursor, boolean horizontal) {
        String iconName = horizontal ? "mdi2a-arrow-left-right" : "mdi2a-arrow-up-down";
        StackPane handle = org.example.utils.UIFactory.crearIconHandle(iconName, 16, "#16a085", cursor);
        handle.setVisible(false);
        org.example.utils.GeometryUtility.applyAntiShear(handle, shearTransform, 8, 8);
        return handle;
    }

    private void initShearHandles() {
        // These are added to selectionOverlay in the constructor
    }

    // ============================================
    // GESTIÓN DE HIJOS (CORREGIDO)
    // ============================================

    public void addChild(Node node) {
        if (node == null || userLayers.contains(node))
            return;

        // --- CORRECCIÓN CRÍTICA ---
        // NO reseteamos TranslateX/Y aquí. Dejamos que UserLayerManager
        // coloque el nodo donde debe ir (restándole el minX/minY del grupo).

        node.setCache(false);
        if (node instanceof javafx.scene.shape.Shape) {
            ((javafx.scene.shape.Shape) node).setSmooth(true);
        }
        
        if (node instanceof org.example.component.ShapeLayer) {
            ((org.example.component.ShapeLayer) node).setGrouped(true);
        }

        userLayers.add(node);
        contentGroup.getChildren().add(node);

        invalidateBounds();
        updateSelectionOverlay();
    }

    public void removeChild(Node node) {
        if (userLayers.remove(node)) {
            if (node instanceof org.example.component.ShapeLayer) {
                ((org.example.component.ShapeLayer) node).setGrouped(false);
            }
            contentGroup.getChildren().remove(node);
            invalidateBounds();
            updateSelectionOverlay();
        }
    }

    public List<Node> getUserLayers() {
        return new ArrayList<>(userLayers);
    }

    public Group getContentGroup() {
        return contentGroup;
    }

    // --- INTERNAL TRANSFORM ACCESSORS (FOR PERSISTENCE) ---
    public double getInternalScaleX() {
        return scaleTransform.getX();
    }

    public void setInternalScaleX(double x) {
        scaleTransform.setX(x);
        updateSelectionOverlay();
    }

    public double getInternalScaleY() {
        return scaleTransform.getY();
    }

    public void setInternalScaleY(double y) {
        scaleTransform.setY(y);
        updateSelectionOverlay();
    }

    public double getInternalRotation() {
        return rotateTransform.getAngle();
    }

    public void setInternalRotation(double angle) {
        rotateTransform.setAngle(angle);
        updateSelectionOverlay();
    }

    public void addRotation(double delta) {
        setInternalRotation(getInternalRotation() + delta);
    }

    public double getInternalShearX() {
        return shearTransform.getX();
    }

    public double getInternalShearY() {
        return shearTransform.getY();
    }

    public void setInternalShear(double x, double y) {
        shearTransform.setX(x);
        shearTransform.setY(y);
        invalidateBounds();
        updateSelectionOverlay();
    }

    public void multiplyShear(double sx, double sy) {
        shearTransform.setX(shearTransform.getX() + sx);
        shearTransform.setY(shearTransform.getY() + sy);
        updateSelectionOverlay();
    }

    public void setRotationMode(boolean rotationMode) {
        this.isRotationMode = rotationMode;
        updateSelectionOverlay();
    }

    public boolean isRotationMode() {
        return isRotationMode;
    }

    // ============================================
    // GEOMETRIC SCALING (FIX FOR SQUASHED CONTOURS)
    // ============================================

    public void multiplyScale(double sx, double sy) {
        if (Math.abs(sx - 1.0) < 0.0001 && Math.abs(sy - 1.0) < 0.0001)
            return;

        for (Node n : userLayers) {
            // 1. Scale child geometry recursively
            if (n instanceof ShapeLayer) {
                ((ShapeLayer) n).multiplyScale(sx, sy);
            } else if (n instanceof ImageLayer) {
                ((ImageLayer) n).multiplyScale(sx, sy);
            } else if (n instanceof GroupLayerV2) {
                ((GroupLayerV2) n).multiplyScale(sx, sy);
            } else if (n instanceof GroupLayer) {
                ((GroupLayer) n).multiplyScale(sx, sy);
            } else if (n instanceof TextLayer) {
                ((TextLayer) n).multiplyScale(sx, sy);
            } else {
                n.setScaleX(n.getScaleX() * sx);
                n.setScaleY(n.getScaleY() * sy);
            }

            // 2. Align child post-scale relative to group (0,0)
            n.setTranslateX(n.getTranslateX() * sx);
            n.setTranslateY(n.getTranslateY() * sy);
        }

        invalidateBounds();
        updateSelectionOverlay();
    }

    /**
     * Compensates world position to keep a specific scene-point stationary 
     * while scaling children geometrically.
     */
    private void compensateScalingPosition(Point2D refStartScene) {
        Point2D refEnd = contentGroup.localToScene(0, 0);
        if (refStartScene != null && refEnd != null) {
            Point2D parentStart = getParent().sceneToLocal(refStartScene);
            Point2D parentEnd = getParent().sceneToLocal(refEnd);
            Point2D parentDelta = parentStart.subtract(parentEnd);
            setTranslateX(getTranslateX() + parentDelta.getX());
            setTranslateY(getTranslateY() + parentDelta.getY());
        }
    }

    // ============================================
    // BOUNDS
    // ============================================

    public Bounds calculateBounds() {
        if (!boundsDirty && cachedBounds != null) {
            return cachedBounds;
        }

        if (userLayers.isEmpty()) {
            return new BoundingBox(0, 0, 10, 10);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (Node child : userLayers) {
            Bounds b;
            if (child instanceof ShapeLayer sl) {
                b = sl.localToParent(new javafx.geometry.BoundingBox(sl.getVisualMinX(), sl.getVisualMinY(), sl.getLogicalWidth(), sl.getLogicalHeight()));
            } else if (child instanceof TextLayer tl) {
                b = tl.localToParent(new javafx.geometry.BoundingBox(-tl.getLogicalWidth()/2.0, -tl.getLogicalHeight()/2.0, tl.getLogicalWidth(), tl.getLogicalHeight()));
            } else if (child instanceof GroupLayer gl) {
                b = gl.localToParent(new javafx.geometry.BoundingBox(gl.getBoundsMinX(), gl.getBoundsMinY(), gl.getLogicalWidth(), gl.getLogicalHeight()));
            } else if (child instanceof GroupLayerV2 g2) {
                Bounds cb = g2.calculateBounds();
                b = g2.localToParent(new javafx.geometry.BoundingBox(cb.getMinX(), cb.getMinY(), cb.getWidth(), cb.getHeight()));
            } else {
                b = child.getBoundsInParent();
            }
            if (b.getWidth() == 0 && b.getHeight() == 0)
                continue;
            minX = Math.min(minX, b.getMinX());
            minY = Math.min(minY, b.getMinY());
            maxX = Math.max(maxX, b.getMaxX());
            maxY = Math.max(maxY, b.getMaxY());
        }

        if (minX == Double.MAX_VALUE) {
            minX = 0;
            minY = 0;
            maxX = 10;
            maxY = 10;
        }

        cachedBounds = new BoundingBox(minX, minY, maxX - minX, maxY - minY);
        boundsDirty = false;
        return cachedBounds;
    }

    public void invalidateBounds() {
        boundsDirty = true;
    }

    public void recalculateBounds() {
        invalidateBounds();
        updateSelectionOverlay();
    }

    // ============================================
    // SELECCIÓN
    // ============================================

    public void setSelected(boolean selected) {
        if (this.isSelected == selected)
            return;
        this.isSelected = selected;

        if (selected) {
            updateSelectionOverlay();
            selectionOverlay.setVisible(true); // Always visible if selected, handle logic is inside update
        } else {
            selectionOverlay.setVisible(false);
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void updateSelectionOverlay() {
        if (userLayers.isEmpty()) {
            selectionOverlay.setVisible(false);
            return;
        }

        Bounds rawBounds = calculateBounds();
        double x = rawBounds.getMinX();
        double y = rawBounds.getMinY();
        double w = rawBounds.getWidth();
        double h = rawBounds.getHeight();

        border.setX(x);
        border.setY(y);
        border.setWidth(w);
        border.setHeight(h);

        selectionHitArea.setX(x);
        selectionHitArea.setY(y);
        selectionHitArea.setWidth(w);
        selectionHitArea.setHeight(h);

        boolean isEffectiveLocked = isLocked();
        boolean showRotate = isSelected && isRotationMode && !isEffectiveLocked;
        boolean showResize = isSelected && !isRotationMode && !isEffectiveLocked;

        // Resize Handles
        topLeft.setVisible(showResize);
        topRight.setVisible(showResize);
        bottomLeft.setVisible(showResize);
        bottomRight.setVisible(showResize);
        topCenter.setVisible(showResize);
        bottomCenter.setVisible(showResize);
        leftCenter.setVisible(showResize);
        rightCenter.setVisible(showResize);

        double hs = 12.0; // Correct offset for 24x24 handle containers to be centered at (0,0)
        positionNode(topLeft, x - hs, y - hs);
        positionNode(topRight, x + w - hs, y - hs);
        positionNode(bottomLeft, x - hs, y + h - hs);
        positionNode(bottomRight, x + w - hs, y + h - hs);
        positionNode(topCenter, x + w / 2 - hs, y - hs);
        positionNode(bottomCenter, x + w / 2 - hs, y + h - hs);
        positionNode(leftCenter, x - hs, y + h / 2 - hs);
        positionNode(rightCenter, x + w - hs, y + h / 2 - hs);

        // Rotation Mode Handles
        rotTopLeft.setVisible(showRotate);
        rotTopRight.setVisible(showRotate);
        rotBottomLeft.setVisible(showRotate);
        rotBottomRight.setVisible(showRotate);

        double rs = 16 / 2.0;
        positionNode(rotTopLeft, x - rs, y - rs);
        positionNode(rotTopRight, x + w - rs, y - rs);
        positionNode(rotBottomLeft, x - rs, y + h - rs);
        positionNode(rotBottomRight, x + w - rs, y + h - rs);

        shearTop.setVisible(showRotate);
        shearBottom.setVisible(showRotate);
        shearLeft.setVisible(showRotate);
        shearRight.setVisible(showRotate);

        positionNode(shearTop, x + w / 2 - rs, y - rs);
        positionNode(shearBottom, x + w / 2 - rs, y + h - rs);
        positionNode(shearLeft, x - rs, y + h / 2 - rs);
        positionNode(shearRight, x + w - rs, y + h / 2 - rs);

        // Pivot: default center + custom offset
        double pivotX = x + w / 2.0 + pivotOffsetX;
        double pivotY = y + h / 2.0 + pivotOffsetY;

        // ONLY update pivot if it has moved significantly to avoid floating-point 'jitter'
        if (Math.abs(rotateTransform.getPivotX() - pivotX) > 0.001 || Math.abs(rotateTransform.getPivotY() - pivotY) > 0.001) {
            rotateTransform.setPivotX(pivotX);
            rotateTransform.setPivotY(pivotY);
            shearTransform.setPivotX(pivotX);
            shearTransform.setPivotY(pivotY);
            
            overlayRotateTransform.setPivotX(pivotX);
            overlayRotateTransform.setPivotY(pivotY);
            overlayShearTransform.setPivotX(pivotX);
            overlayShearTransform.setPivotY(pivotY);
        }

        pivotHandle.setVisible(showRotate);
        positionNode(pivotHandle, pivotX - 8, pivotY - 8);

        // --- OPTIMIZATION: If we are only rotating, handles are already moved by overlayRotateTransform ---
        // This prevents the 'vibration' caused by repositioning 16 nodes per frame.
        if (isRotationMode && !boundsDirty) {
             // Anti-shear still needs to be updated if shear is active, but usually it's stable
             return;
        }

        // --- ANTI-SHEAR (MANDATORY TO AVOID STRETCHING) ---
        org.example.utils.GeometryUtility.applyAntiShear(topLeft, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(topRight, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(bottomLeft, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(bottomRight, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(topCenter, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(bottomCenter, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(leftCenter, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(rightCenter, shearTransform, 8, 8);

        org.example.utils.GeometryUtility.applyAntiShear(rotTopLeft, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(rotTopRight, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(rotBottomLeft, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(rotBottomRight, shearTransform, 8, 8);

        org.example.utils.GeometryUtility.applyAntiShear(shearTop, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(shearBottom, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(shearLeft, shearTransform, 8, 8);
        org.example.utils.GeometryUtility.applyAntiShear(shearRight, shearTransform, 8, 8);

        org.example.utils.GeometryUtility.applyAntiShear(pivotHandle, shearTransform, 8, 8);
    }

    public boolean isLocked() {
        return isSystemLocked || isUserLocked;
    }

    private void positionNode(Node node, double x, double y) {
        node.setLayoutX(x);
        node.setLayoutY(y);
    }

    private boolean isHandle(Node node) {
        return node == topLeft || node == topRight || node == bottomLeft || node == bottomRight ||
                node == topCenter || node == bottomCenter || node == leftCenter || node == rightCenter ||
                node == rotTopLeft || node == rotTopRight || node == rotBottomLeft || node == rotBottomRight ||
                node == shearTop || node == shearBottom || node == shearLeft || node == shearRight ||
                node == pivotHandle;
    }

    // ============================================
    // DRAG (MOVIMIENTO)
    // ============================================

    private void setupDragHandlers() {
        final class DragContext {
            double startX, startY, initX, initY;
        }
        final DragContext ctx = new DragContext();
        final boolean[] dropCopy = {false};
        final org.example.pattern.NodeMemento[] startMemento = new org.example.pattern.NodeMemento[1];
        @SuppressWarnings("unchecked")
        final javafx.event.EventHandler<javafx.scene.input.MouseEvent>[] rightClickFilter = new javafx.event.EventHandler[1];

        final long[] lastClickTime = {0};
        setOnMousePressed(e -> {
            if (isLocked())
                return;
                
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime[0] < 500 && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                setRotationMode(!isRotationMode());
                e.consume();
                return;
            }
            lastClickTime[0] = currentTime;

            if (!isSelected)
                return;

            if (e.getTarget() instanceof Node && isHandle((Node) e.getTarget()))
                return;
                
            dropCopy[0] = false;
            startMemento[0] = new org.example.pattern.NodeMemento(this);
            
            // Add global right-click detector
            rightClickFilter[0] = ev -> {
                if (ev.getButton() == javafx.scene.input.MouseButton.SECONDARY && ev.getEventType() == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
                    if (!dropCopy[0]) {
                        dropCopy[0] = true;
                        setCursor(Cursor.CROSSHAIR);
                    }
                    ev.consume();
                }
            };
            if (getScene() != null) {
                getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
            }

            javafx.geometry.Point2D startLocal = this.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            ctx.startX = startLocal.getX();
            ctx.startY = startLocal.getY();
            ctx.initX = getTranslateX();
            ctx.initY = getTranslateY();

            setCursor(Cursor.MOVE);
            e.consume();
        });

        setOnMouseDragged(e -> {
            if (isLocked() || !isSelected)
                return;

            javafx.geometry.Point2D currLocal = this.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            double dx = currLocal.getX() - ctx.startX;
            double dy = currLocal.getY() - ctx.startY;

            setTranslateX(ctx.initX + dx);
            setTranslateY(ctx.initY + dy);
            e.consume();
        });

        setOnMouseReleased(e -> {
            if (rightClickFilter[0] != null && getScene() != null) {
                getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
                rightClickFilter[0] = null;
            }
            setCursor(Cursor.DEFAULT);
            if (isSelected && !isLocked()) {
                updateSelectionOverlay();
                
                if (dropCopy[0] && visualizer != null) {
                    visualizer.copySelectedLayer();
                    visualizer.pasteLayer();
                    
                    if (startMemento[0] != null) {
                        startMemento[0].restore();
                    } else {
                        setTranslateX(ctx.initX);
                        setTranslateY(ctx.initY);
                    }
                } else {
                    // Record History
                    if (visualizer != null && visualizer.getHistoryManager() != null && startMemento[0] != null
                            && (ctx.initX != getTranslateX() || ctx.initY != getTranslateY())) {

                        TransformCommand cmd = new TransformCommand(this, startMemento[0], new org.example.pattern.NodeMemento(this), getActiveZone());
                        visualizer.getHistoryManager().addCommand(cmd);
                    }
                }
            }
        });
    }

    // ============================================
    // RESIZE (ESTIRAR) - IMPLEMENTACIÓN REAL
    // ============================================

    private void setupResizeHandlers() {
        setupResizeHandle(topLeft, -1, -1);
        setupResizeHandle(topRight, 1, -1);
        setupResizeHandle(bottomLeft, -1, 1);
        setupResizeHandle(bottomRight, 1, 1);
        setupResizeHandle(topCenter, 0, -1);
        setupResizeHandle(bottomCenter, 0, 1);
        setupResizeHandle(leftCenter, -1, 0);
        setupResizeHandle(rightCenter, 1, 0);
    }

    private void setupResizeHandle(Node handle, double dirX, double dirY) {
        final class ResizeCtx {
            double startX, startY;
            double startBoundsW, startBoundsH;
            double startTx, startTy;
            Point2D startAnchorWorld;
            double ax, ay;
            double appliedSoFarX = 1.0; // Local tracker for incremental scaling
            double appliedSoFarY = 1.0;
        }
        final ResizeCtx ctx = new ResizeCtx();
        final boolean[] dropCopy = {false};
        final org.example.pattern.NodeMemento[] startMemento = new org.example.pattern.NodeMemento[1];
        @SuppressWarnings("unchecked")
        final javafx.event.EventHandler<javafx.scene.input.MouseEvent>[] rightClickFilter = new javafx.event.EventHandler[1];

        handle.setOnMousePressed(e -> {
            if (isLocked())
                return;
            e.consume();
            
            dropCopy[0] = false;
            startMemento[0] = new org.example.pattern.NodeMemento(this);
            
            rightClickFilter[0] = ev -> {
                if (ev.getButton() == javafx.scene.input.MouseButton.SECONDARY && ev.getEventType() == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
                    if (!dropCopy[0]) {
                        dropCopy[0] = true;
                        setCursor(Cursor.CROSSHAIR);
                    }
                    ev.consume();
                }
            };
            if (getScene() != null) {
                getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
            }

            ctx.startX = e.getSceneX();
            ctx.startY = e.getSceneY();
            ctx.startTx = getTranslateX();
            ctx.startTy = getTranslateY();

            Bounds b = calculateBounds();
            ctx.startBoundsW = b.getWidth();
            ctx.startBoundsH = b.getHeight();

            // Calculate Anchor Point in Local Coordinates (RELATIVE TO START)
            ctx.ax = (dirX == -1) ? b.getMaxX() : (dirX == 1) ? b.getMinX() : b.getCenterX();
            ctx.ay = (dirY == -1) ? b.getMaxY() : (dirY == 1) ? b.getMinY() : b.getCenterY();

            // Store stable World Anchor
            ctx.startAnchorWorld = this.localToParent(new Point2D(ctx.ax, ctx.ay));
        });

        handle.setOnMouseDragged(e -> {
            if (isLocked())
                return;
            e.consume();

            // USE PARENT COORDINATES for stable delta calculation
            Point2D currParent = this.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            Point2D startParent = this.getParent().sceneToLocal(ctx.startX, ctx.startY);

            double parentDx = currParent.getX() - startParent.getX();
            double parentDy = currParent.getY() - startParent.getY();

            // --- CORRECTION: Project Parent Delta into the Group's Rotated Local Space ---
            double angleRad = Math.toRadians(getRotate());
            double cos = Math.cos(angleRad);
            double sin = Math.sin(angleRad);

            // Delta in unrotated local space of the group
            double localDx = parentDx * cos + parentDy * sin;
            double localDy = -parentDx * sin + parentDy * cos;

            double safeW = Math.max(0.1, ctx.startBoundsW);
            double safeH = Math.max(0.1, ctx.startBoundsH);

            double proposedW = safeW + localDx * dirX;
            double proposedH = safeH + localDy * dirY;

            // Snap logic (Ctrl modifier)
            if (e.isControlDown()) {
                if (dirX != 0 && Math.abs(proposedW) > 0) {
                    long m = Math.round(proposedW / safeW);
                    if (m == 0) m = (proposedW < 0) ? -1 : 1;
                    proposedW = m * safeW;
                }
                if (dirY != 0 && Math.abs(proposedH) > 0) {
                    long m = Math.round(proposedH / safeH);
                    if (m == 0) m = (proposedH < 0) ? -1 : 1;
                    proposedH = m * safeH;
                }
            }

            double totalRatioX = (dirX != 0) ? proposedW / safeW : 1.0;
            double totalRatioY = (dirY != 0) ? proposedH / safeH : 1.0;

            // Allow negative scaling (mirroring), but prevent 0
            if (Math.abs(totalRatioX) < 0.01) totalRatioX = 0.01 * (totalRatioX >= 0 ? 1 : -1);
            if (Math.abs(totalRatioY) < 0.01) totalRatioY = 0.01 * (totalRatioY >= 0 ? 1 : -1);

            // Increment multiplier to reach totalRatioX relative to start
            double finalMsx = totalRatioX / ctx.appliedSoFarX;
            double finalMsy = totalRatioY / ctx.appliedSoFarY;
            
            ctx.appliedSoFarX = totalRatioX;
            ctx.appliedSoFarY = totalRatioY;

            // 1. Geometric Scale
            multiplyScale(finalMsx, finalMsy);

            // 2. Compensation: Calculate current position of anchor in parent space
            Point2D currentAnchorLocal = new Point2D(ctx.ax * totalRatioX, ctx.ay * totalRatioY);
            Point2D currentAnchorParent = localToParent(currentAnchorLocal);

            double dx = ctx.startAnchorWorld.getX() - currentAnchorParent.getX();
            double dy = ctx.startAnchorWorld.getY() - currentAnchorParent.getY();

            // 3. Translate group to keep anchor stationary
            setTranslateX(getTranslateX() + dx);
            setTranslateY(getTranslateY() + dy);

            updateSelectionOverlay();
        });

        handle.setOnMouseReleased(e -> {
            if (rightClickFilter[0] != null && getScene() != null) {
                getScene().removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, rightClickFilter[0]);
                rightClickFilter[0] = null;
            }
            
            boolean changed = Math.abs(ctx.appliedSoFarX - 1.0) > 0.001
                    || Math.abs(ctx.appliedSoFarY - 1.0) > 0.001;

            if (changed) {
                if (dropCopy[0] && visualizer != null) {
                    visualizer.copySelectedLayer();
                    visualizer.pasteLayer();
                    
                    if (startMemento[0] != null) {
                        // REVERT geometry of children before restoring translation
                        multiplyScale(1.0 / ctx.appliedSoFarX, 1.0 / ctx.appliedSoFarY);
                        startMemento[0].restore();
                    }
                } else if (visualizer != null && visualizer.getHistoryManager() != null && startMemento[0] != null) {
                    // Using NodeMemento since GroupLayerV2 has deep properties that can change geometrically
                    TransformCommand cmd = new TransformCommand(this, startMemento[0], new org.example.pattern.NodeMemento(this), getActiveZone());
                    visualizer.getHistoryManager().addCommand(cmd);
                }
            }
            e.consume();
        });
    }

    // ============================================
    // ROTACIÓN
    // ============================================

    private void setupRotateHandler() {
        final class RotCtx {
            double startAngle;
            double startMouseAngle;
            Point2D pivot;
        }
        final RotCtx ctx = new RotCtx();

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> press = e -> {
            if (isLocked())
                return;
            Bounds b = border.getBoundsInLocal();
            // Origin for rotation is the pivot (currently center of group)
            ctx.pivot = localToScene(b.getCenterX(), b.getCenterY());
            ctx.startMouseAngle = Math
                    .toDegrees(Math.atan2(e.getSceneY() - ctx.pivot.getY(), e.getSceneX() - ctx.pivot.getX()));
            ctx.startAngle = rotateTransform.getAngle();
            e.consume();
        };

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> drag = e -> {
            if (isLocked())
                return;
            double currentMouseAngle = Math
                    .toDegrees(Math.atan2(e.getSceneY() - ctx.pivot.getY(), e.getSceneX() - ctx.pivot.getX()));
            rotateTransform.setAngle(ctx.startAngle + (currentMouseAngle - ctx.startMouseAngle));
            updateSelectionOverlay();
            e.consume();
        };

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> release = e -> {
            if (visualizer != null && visualizer.getHistoryManager() != null
                    && ctx.startAngle != rotateTransform.getAngle()) {
                TransformCommand cmd = new TransformCommand(this,
                        getTranslateX(), getTranslateY(),
                        1.0, 1.0,
                        ctx.startAngle,
                        null, null,
                        getTranslateX(), getTranslateY(),
                        1.0, 1.0,
                        rotateTransform.getAngle(),
                        null, null,
                        null);
                visualizer.getHistoryManager().addCommand(cmd);
            }
            e.consume();
        };

        // Bind to all rotation handles
        rotTopLeft.setOnMousePressed(press);
        rotTopLeft.setOnMouseDragged(drag);
        rotTopLeft.setOnMouseReleased(release);

        rotTopRight.setOnMousePressed(press);
        rotTopRight.setOnMouseDragged(drag);
        rotTopRight.setOnMouseReleased(release);

        rotBottomLeft.setOnMousePressed(press);
        rotBottomLeft.setOnMouseDragged(drag);
        rotBottomLeft.setOnMouseReleased(release);

        rotBottomRight.setOnMousePressed(press);
        rotBottomRight.setOnMouseDragged(drag);
        rotBottomRight.setOnMouseReleased(release);
    }

    private void setupShearHandlers() {
        final class ShearCtx {
            double startX, startY;
            double initShearX, initShearY;
            org.example.pattern.NodeMemento beforeMemento;
        }
        final ShearCtx ctx = new ShearCtx();

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> press = e -> {
            if (isLocked())
                return;
            ctx.beforeMemento = new org.example.pattern.NodeMemento(this);
            ctx.startX = e.getSceneX();
            ctx.startY = e.getSceneY();
            ctx.initShearX = shearTransform.getX();
            ctx.initShearY = shearTransform.getY();
            e.consume();
        };

        java.util.function.BiFunction<Boolean, Boolean, javafx.event.EventHandler<javafx.scene.input.MouseEvent>> createDrag = (
                isHorizontal, invert) -> e -> {
                    if (isLocked())
                        return;
                    Point2D localDelta = screenToUnrotatedLocalDelta(e.getSceneX() - ctx.startX,
                            e.getSceneY() - ctx.startY);
                    double factor = 0.005; // Sensitivity
                    if (invert)
                        factor = -factor;

                    if (isHorizontal) {
                        shearTransform.setX(ctx.initShearX + (localDelta.getX() * factor));
                    } else {
                        shearTransform.setY(ctx.initShearY + (localDelta.getY() * factor));
                    }
                    updateSelectionOverlay();
                    e.consume();
                };

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> release = e -> {
            if (visualizer != null && visualizer.getHistoryManager() != null
                    && (ctx.initShearX != shearTransform.getX() || ctx.initShearY != shearTransform.getY())) {
                org.example.pattern.NodeMemento afterMemento = new org.example.pattern.NodeMemento(this);
                TransformCommand cmd = new TransformCommand(this, ctx.beforeMemento, afterMemento, null);
                visualizer.getHistoryManager().addCommand(cmd);
            }
            e.consume();
        };

        shearTop.setOnMousePressed(press);
        shearTop.setOnMouseDragged(createDrag.apply(true, true));
        shearTop.setOnMouseReleased(release);

        shearBottom.setOnMousePressed(press);
        shearBottom.setOnMouseDragged(createDrag.apply(true, false));
        shearBottom.setOnMouseReleased(release);

        shearLeft.setOnMousePressed(press);
        shearLeft.setOnMouseDragged(createDrag.apply(false, true));
        shearLeft.setOnMouseReleased(release);

        shearRight.setOnMousePressed(press);
        shearRight.setOnMouseDragged(createDrag.apply(false, false));
        shearRight.setOnMouseReleased(release);
    }

    // Custom pivot offset relative to the center of bounds (0,0 = default center)
    private double pivotOffsetX = 0;
    private double pivotOffsetY = 0;

    private void setupPivotHandler() {
        pivotHandle.setOnMousePressed(e -> {
            if (isLocked())
                return;
            e.consume();
        });

        pivotHandle.setOnMouseDragged(e -> {
            if (isLocked())
                return;

            // Stable local space reference
            Point2D groupLocal = this.sceneToLocal(e.getSceneX(), e.getSceneY());

            // Current bounds center
            Bounds b = calculateBounds();
            double centerX = b.getMinX() + b.getWidth() / 2.0;
            double centerY = b.getMinY() + b.getHeight() / 2.0;

            // Snap threshold: 15px
            double SNAP_RADIUS = 15.0;
            double dx = groupLocal.getX() - centerX;
            double dy = groupLocal.getY() - centerY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist <= SNAP_RADIUS) {
                pivotOffsetX = 0;
                pivotOffsetY = 0;
                pivotHandle.setOpacity(0.6);
            } else {
                pivotOffsetX = dx;
                pivotOffsetY = dy;
                pivotHandle.setOpacity(1.0);
            }

            updateSelectionOverlay();
            e.consume();
        });

        // Double-click resets pivot to center
        pivotHandle.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                pivotOffsetX = 0;
                pivotOffsetY = 0;
                updateSelectionOverlay();
                e.consume();
            }
        });
    }

    private Point2D screenToUnrotatedLocalDelta(double dx, double dy) {
        double angleRad = Math.toRadians(-rotateTransform.getAngle());
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return new Point2D(dx * cos - dy * sin, dx * sin + dy * cos);
    }

    // ============================================
    // COLORES (Recursivo)
    // ============================================

    public Color getFillColor() {
        return getCommonColor(true);
    }

    public Color getStrokeColor() {
        return getCommonColor(false);
    }

    private Color getCommonColor(boolean isFill) {
        Color common = null;
        boolean first = true;

        for (Node node : userLayers) {
            Color c = null;
            if (node instanceof ShapeLayer) {
                c = isFill ? ((ShapeLayer) node).getFillColor() : ((ShapeLayer) node).getStrokeColor();
            } else if (node instanceof GroupLayer) {
                c = isFill ? ((GroupLayer) node).getFillColor() : ((GroupLayer) node).getStrokeColor();
            } else if (node instanceof GroupLayerV2) {
                c = isFill ? ((GroupLayerV2) node).getFillColor() : ((GroupLayerV2) node).getStrokeColor();
            } else {
                continue; // Skip images, etc.
            }

            if (first) {
                common = c;
                first = false;
            } else {
                if (!isColorEqual(common, c))
                    return null;
            }
        }
        return common;
    }

    private boolean isColorEqual(Color c1, Color c2) {
        if (c1 == null && c2 == null)
            return true;
        if (c1 == null || c2 == null)
            return false;
        return c1.toString().equals(c2.toString());
    }

    public void setSystemLocked(boolean locked) {
        this.isSystemLocked = locked;
        if (isSelected)
            updateSelectionOverlay();
    }

    public void setUserLocked(boolean locked) {
        this.isUserLocked = locked;
        if (isSelected)
            updateSelectionOverlay();
    }

    public boolean isUserLocked() {
        return isUserLocked;
    }

    public boolean isSystemLocked() {
        return isSystemLocked;
    }

    public void setVisualizer(PrendaVisualizer v) {
        this.visualizer = v;
    }

}
