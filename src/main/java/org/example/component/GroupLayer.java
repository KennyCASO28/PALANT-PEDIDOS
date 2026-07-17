package org.example.component;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.DepthTest;
import org.example.pattern.TransformCommand;

/**
 * A wrapper for grouped nodes that treats them as a single interactive unit.
 * Provides selection handles, rotation, and resizing for the entire group.
 */
public class GroupLayer extends Group {

    // Structure:
    // This Group (Root)
    // -> contentGroup (User Objects + Transforms)
    // -> handlesGroup (UI Handles)

    private final Group contentGroup;

    // Interaction UI
    private final Rectangle border;
    private final Rectangle selectionHitArea; // Invisible background for easier selection
    private final Group handlesGroup;
    private final Circle topLeft, topRight, bottomLeft, bottomRight;
    private final Circle topCenter, bottomCenter, leftCenter, rightCenter;

    // Rotation Mode UI
    private final javafx.scene.layout.StackPane rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight;
    private final javafx.scene.layout.StackPane shearTop, shearBottom, shearLeft, shearRight;
    private final javafx.scene.Group pivotHandle;
    private boolean isRotationMode = false;
    private double customPivotX = -1;
    private double customPivotY = -1;

    // Transforms (Applied to contentGroup)
    private final Rotate rotateTransform = new Rotate();
    private final Scale scaleTransform = new Scale();
    private final javafx.scene.transform.Shear shearTransform = new javafx.scene.transform.Shear();

    // State
    private boolean isSelected = false;
    private boolean isSystemLocked = false;
    private boolean isUserLocked = false;
    private boolean isBeingEdited = false;
    private PrendaVisualizer visualizer; // Optional, for undo

    // Bounds tracking (for resizing logic)
    private double currentWidth = 100;
    private double currentHeight = 100;

    // OPTIMIZATION: Cache bounds to avoid recalculating every frame
    private double boundsMinX = 0;
    private double boundsMinY = 0;

    private String activeZone = null;

    public void setActiveZone(String activeZone) {
        this.activeZone = activeZone;
        setSystemLocked(activeZone != null);
    }

    public String getActiveZone() {
        return activeZone;
    }

    public GroupLayer() {
        setId("USER_GROUP"); // Keep ID for compatibility

        // 1. CONFIGURACIÓN ANTI-GLITCH (CRÍTICO)
        // Forzamos renderizado plano para evitar Z-Fighting y artefactos visuales
        setDepthTest(DepthTest.DISABLE);
        setCache(false);
        setPickOnBounds(true); // Permitir selección por área total

        // 1.1 ÁREA DE CLIC (SENSIVILIDAD)
        selectionHitArea = new Rectangle();
        selectionHitArea.setFill(Color.web("#ffffff", 0.01)); // Casi invisible pero captable por ratón
        selectionHitArea.setDepthTest(DepthTest.DISABLE);
        selectionHitArea.setCache(false);
        selectionHitArea.setMouseTransparent(true);

        // 2. Content Group (Donde van tus figuras)
        contentGroup = new Group();
        contentGroup.setDepthTest(DepthTest.DISABLE);
        contentGroup.setCache(false);
        // Scale: Applied to Content ONLY (Handles stay constant size)
        contentGroup.getTransforms().add(scaleTransform);

        // 3. UI Initialization
        border = new Rectangle();
        border.setFill(null);
        border.setStroke(Color.web("#0047AB"));
        border.setStrokeWidth(1);
        border.setMouseTransparent(true);
        border.setVisible(false);
        border.setCache(false);
        border.setDepthTest(DepthTest.DISABLE);

        handlesGroup = new Group();
        handlesGroup.setVisible(false);
        // Fix: Prevent handles from interfering with rendering of objects underneath
        handlesGroup.setPickOnBounds(false);
        handlesGroup.setMouseTransparent(false); // Handles need to be interactive
        handlesGroup.setManaged(false); // Exclude from layout calculations
        handlesGroup.setCache(false);
        handlesGroup.setDepthTest(DepthTest.DISABLE);

        topLeft = createHandle(Cursor.NW_RESIZE);
        topRight = createHandle(Cursor.NE_RESIZE);
        bottomLeft = createHandle(Cursor.SW_RESIZE);
        bottomRight = createHandle(Cursor.SE_RESIZE);
        topCenter = createHandle(Cursor.N_RESIZE);
        bottomCenter = createHandle(Cursor.S_RESIZE);
        leftCenter = createHandle(Cursor.W_RESIZE);
        rightCenter = createHandle(Cursor.E_RESIZE);

        rotTopLeft = createRotHandle();
        rotTopRight = createRotHandle();
        rotBottomLeft = createRotHandle();
        rotBottomRight = createRotHandle();

        shearTop = createShearHandle(Cursor.H_RESIZE, true);
        shearBottom = createShearHandle(Cursor.H_RESIZE, true);
        shearLeft = createShearHandle(Cursor.V_RESIZE, false);
        shearRight = createShearHandle(Cursor.V_RESIZE, false);

        pivotHandle = org.example.utils.UIFactory.crearPivotHandle();
        pivotHandle.setCursor(Cursor.CROSSHAIR);
        pivotHandle.setVisible(false);

        handlesGroup.getChildren().addAll(topLeft, topRight, bottomLeft, bottomRight,
                topCenter, bottomCenter, leftCenter, rightCenter,
                rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight,
                shearTop, shearBottom, shearLeft, shearRight, pivotHandle);

        this.getChildren().addAll(selectionHitArea, contentGroup, border, handlesGroup);

        // Rotate & Shear: Applied to ROOT (Everything rotates together, including
        // handles)
        this.getTransforms().addAll(rotateTransform, shearTransform);

        // CRITICAL: We REMOVE scaleTransform from contentGroup.getTransforms().
        // GroupLayer (V1) now uses Pure Geometric Scaling (direct child modification).
        // The scaleTransform object is kept ONLY for logical tracking/history.
        // contentGroup.getTransforms().add(scaleTransform); // REMOVED to avoid
        // double-scaling

        // Initialize Events
        initDragEvents();
        initResizeEvents();
        initRotateEvents();
        initShearEvents();

        // --- SCALE ABSORPTION (PREVENT SQUASHED HANDLES) ---
        // If anything (like an Undo command) tries to scale the Root Group node,
        // we absorb it into children geometry and reset our scale to 1.0.
        scaleXProperty().addListener((obs, oldVal, newVal) -> {
            double val = newVal.doubleValue();
            if (Math.abs(val - 1.0) > 0.001) {
                javafx.application.Platform.runLater(() -> {
                    multiplyScale(val, 1.0);
                    setScaleX(1.0);
                });
            }
        });
        scaleYProperty().addListener((obs, oldVal, newVal) -> {
            double val = newVal.doubleValue();
            if (Math.abs(val - 1.0) > 0.001) {
                javafx.application.Platform.runLater(() -> {
                    multiplyScale(1.0, val);
                    setScaleY(1.0);
                });
            }
        });

        // Ensure initially unscaled
        setScaleX(1.0);
        setScaleY(1.0);
    }

    public Group getContentGroup() {
        return contentGroup;
    }

    public Rotate getRotateTransform() {
        return rotateTransform;
    }

    public Scale getScaleTransform() {
        return scaleTransform;
    }

    // --- INTERNAL TRANSFORM ACCESSORS (FOR PERSISTENCE) ---
    public double getInternalShearX() { return shearTransform.getX(); }
    public void setInternalShearX(double s) { shearTransform.setX(s); }
    public double getInternalShearY() { return shearTransform.getY(); }
    public void setInternalShearY(double s) { shearTransform.setY(s); }

    public void multiplyShear(double sx, double sy) {
        shearTransform.setX(shearTransform.getX() + sx);
        shearTransform.setY(shearTransform.getY() + sy);
        updateVisuals();
    }

    public double getCustomPivotX() { return customPivotX; }
    public void setCustomPivotX(double x) { customPivotX = x; }
    public double getCustomPivotY() { return customPivotY; }
    public void setCustomPivotY(double y) { customPivotY = y; }

    public double getInternalScaleX() {
        return scaleTransform.getX();
    }

    public void setInternalScaleX(double x) {
        double sign = Math.signum(x);
        scaleTransform.setX(sign == 0 ? 1 : sign);
        updateVisuals();
    }

    public double getInternalScaleY() {
        return scaleTransform.getY();
    }

    public void setInternalScaleY(double y) {
        double sign = Math.signum(y);
        scaleTransform.setY(sign == 0 ? 1 : sign);
        updateVisuals();
    }

    public double getInternalRotation() {
        return rotateTransform.getAngle();
    }

    public void setInternalRotation(double angle) {
        rotateTransform.setAngle(angle);
        updateVisuals();
    }

    public void setVisualizer(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    public void setIsBeingEdited(boolean editing) {
        this.isBeingEdited = editing;
        refreshLockState();
    }

    public boolean isBeingEdited() {
        return isBeingEdited;
    }

    public void setSystemLocked(boolean locked) {
        this.isSystemLocked = locked;
        refreshLockState();
    }

    public boolean isLocked() {
        return isSystemLocked || isUserLocked;
    }

    private void refreshLockState() {
        boolean effective = isLocked();
        // Border: Always visible if selected
        border.setVisible(isSelected);
 
        // Group handles should only be visible if NOT locked AND selected
        handlesGroup.setVisible(isSelected && !effective);
 
        // Update cursor
        if (effective && !isBeingEdited) {
            this.setCursor(Cursor.DEFAULT);
        } else {
            this.setCursor(Cursor.MOVE);
        }
    }

    public double getLogicalWidth() {
        return currentWidth;
    }

    public double getLogicalHeight() {
        return currentHeight;
    }

    public double getBoundsMinX() {
        return boundsMinX;
    }

    public double getBoundsMinY() {
        return boundsMinY;
    }

    public javafx.geometry.Point2D getStableCenter() {
        // CRITICAL: Project from contentGroup to GroupLayer space to include
        // scaleTransform
        return contentGroup.localToParent(boundsMinX + currentWidth / 2.0, boundsMinY + currentHeight / 2.0);
    }

    public void addRotation(double delta) {
        this.rotateTransform.setAngle(this.rotateTransform.getAngle() + delta);
        updateVisuals();
    }

    public void multiplyScale(double sX, double sY) {
        if (Math.abs(sX - 1.0) < 0.0001 && Math.abs(sY - 1.0) < 0.0001)
            return;

        for (Node n : getUserLayers()) {
            if (n instanceof ShapeLayer) {
                ((ShapeLayer) n).multiplyScale(sX, sY);
            } else if (n instanceof ImageLayer) {
                ((ImageLayer) n).multiplyScale(sX, sY);
            } else if (n instanceof GroupLayerV2) {
                ((GroupLayerV2) n).multiplyScale(sX, sY);
            } else if (n instanceof GroupLayer) {
                ((GroupLayer) n).multiplyScale(sX, sY);
            } else if (n instanceof TextLayer) {
                ((TextLayer) n).multiplyScale(sX, sY);
            } else {
                n.setScaleX(n.getScaleX() * sX);
                n.setScaleY(n.getScaleY() * sY);
            }

            // Align child post-scale relative to group (0,0)
            n.setTranslateX(n.getTranslateX() * sX);
            n.setTranslateY(n.getTranslateY() * sY);
        }

        recalculateBounds();
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        border.setVisible(selected);
        refreshLockState();
        if (selected) {
            // OPTIMIZED: Only reorder if needed to prevent frame flicker
            int lastIdx = getChildren().size() - 1;
            if (getChildren().indexOf(handlesGroup) != lastIdx) {
                getChildren().remove(handlesGroup);
                getChildren().add(handlesGroup);
            }
            updateVisuals();
        } else {
            handlesGroup.setVisible(false); // Ensure hidden
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setUserLocked(boolean locked) {
        this.isUserLocked = locked;
        // Propagar bloqueo a hijos recursivamente usando systemLocked
        for (Node child : getUserLayers()) {
            if (child instanceof ShapeLayer) {
                ((ShapeLayer) child).setSystemLocked(locked);
            } else if (child instanceof ImageLayer) {
                ((ImageLayer) child).setSystemLocked(locked);
            } else if (child instanceof TextLayer) {
                ((TextLayer) child).setSystemLocked(locked);
            } else if (child instanceof GroupLayerV2) {
                ((GroupLayerV2) child).setSystemLocked(locked);
            } else if (child instanceof GroupLayer) {
                ((GroupLayer) child).setSystemLocked(locked);
            }
        }
        refreshLockState();
    }

    public boolean isUserLocked() {
        return isUserLocked;
    }

    private Circle createHandle(Cursor cursor) {
        Circle c = new Circle(3); // Radius reduced from 4.0
        c.setFill(Color.WHITE);
        c.setStroke(Color.web("#0047AB")); // Unified blue stroke
        c.setStrokeWidth(0.8); // Reduced from 1.0
        c.setCursor(cursor);
        c.setEffect(new javafx.scene.effect.DropShadow(2, Color.web("#000000", 0.3)));
        org.example.utils.GeometryUtility.applyAntiShear(c, shearTransform, 0, 0);
        return c;
    }

    /**
     * Get the fill color of grouped objects.
     * Returns the color if all objects have the same color, null if mixed colors.
     */
    public Color getFillColor() {
        Color reportedColor = null;
        boolean foundFirst = false;

        for (Node n : contentGroup.getChildren()) {
            Color nodeColor = null;

            if (n instanceof ShapeLayer) {
                nodeColor = ((ShapeLayer) n).getFillColor();
            } else if (n instanceof GroupLayer) {
                nodeColor = ((GroupLayer) n).getFillColor();
            } else if (n instanceof GroupLayerV2) {
                nodeColor = ((GroupLayerV2) n).getFillColor();
            } else {
                continue; // Skip ImageLayer and others without fill
            }

            if (!foundFirst) {
                reportedColor = nodeColor;
                foundFirst = true;
            } else {
                if (!isColorEqual(reportedColor, nodeColor)) {
                    return null; // Mixed
                }
            }
        }
        return reportedColor;
    }

    public Color getStrokeColor() {
        Color reportedColor = null;
        boolean foundFirst = false;

        for (Node n : contentGroup.getChildren()) {
            Color nodeColor = null;

            if (n instanceof ShapeLayer) {
                nodeColor = ((ShapeLayer) n).getStrokeColor();
            } else if (n instanceof GroupLayer) {
                nodeColor = ((GroupLayer) n).getStrokeColor();
            } else if (n instanceof GroupLayerV2) {
                nodeColor = ((GroupLayerV2) n).getStrokeColor();
            } else {
                continue;
            }

            if (!foundFirst) {
                reportedColor = nodeColor;
                foundFirst = true;
            } else {
                if (!isColorEqual(reportedColor, nodeColor)) {
                    return null; // Mixed
                }
            }
        }
        return reportedColor;
    }

    private boolean isColorEqual(Color c1, Color c2) {
        if (c1 == null && c2 == null)
            return true;
        if (c1 == null || c2 == null)
            return false;
        return c1.toString().equals(c2.toString());
    }

    public void recalculateBounds() {
        // Recalculate size based on contentGroup children
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        boolean hasContent = false;

        for (Node n : contentGroup.getChildren()) {
            Bounds b;
            if (n instanceof ShapeLayer sl) {
                b = sl.localToParent(new javafx.geometry.BoundingBox(sl.getVisualMinX(), sl.getVisualMinY(), sl.getLogicalWidth(), sl.getLogicalHeight()));
            } else if (n instanceof TextLayer tl) {
                b = tl.localToParent(new javafx.geometry.BoundingBox(-tl.getLogicalWidth()/2.0, -tl.getLogicalHeight()/2.0, tl.getLogicalWidth(), tl.getLogicalHeight()));
            } else if (n instanceof GroupLayer gl) {
                b = gl.localToParent(new javafx.geometry.BoundingBox(gl.getBoundsMinX(), gl.getBoundsMinY(), gl.getLogicalWidth(), gl.getLogicalHeight()));
            } else {
                b = n.getBoundsInParent();
            }
            if (b.getMinX() < minX)
                minX = b.getMinX();
            if (b.getMinY() < minY)
                minY = b.getMinY();
            if (b.getMaxX() > maxX)
                maxX = b.getMaxX();
            if (b.getMaxY() > maxY)
                maxY = b.getMaxY();
            hasContent = true;
        }

        if (hasContent) {
            this.currentWidth = maxX - minX;
            this.currentHeight = maxY - minY;

            // Actualizar área de selección para que cubra todo el grupo
            selectionHitArea.setX(minX);
            selectionHitArea.setY(minY);
            selectionHitArea.setWidth(currentWidth);
            selectionHitArea.setHeight(currentHeight);

            this.boundsMinX = minX;
            this.boundsMinY = minY;
        }
        updateVisuals();
        refreshLockState();

        // FIX: Ensure group is visible and layout is refreshed after bounds update
        this.setVisible(true);

        // SAFEGUARD: Run layout update later to avoid recursive Parent.updateBounds
        // crash
        javafx.application.Platform.runLater(() -> {
            this.requestLayout();
            if (getParent() != null)
                getParent().requestLayout();
        });
    }

    private void updateVisuals() {
        // CRITICAL OPTIMIZATION: Skip if not needed
        if (contentGroup.getChildren().isEmpty()) {
            handlesGroup.setVisible(false);
            return;
        }

        if (!isSelected && !handlesGroup.isVisible()) {
            return;
        }

        // Use CACHED bounds - eliminates expensive child iteration!
        double lx = this.boundsMinX;
        double ly = this.boundsMinY;
        double lw = currentWidth;
        double lh = currentHeight;

        // In Pure Geometric Mode, visual dimensions ARE the logical dimensions.
        double visW = lw;
        double visH = lh;
        double visX = lx;
        double visY = ly;

        double unscaledCx = lx + lw / 2.0;
        double unscaledCy = ly + lh / 2.0;

        double pivotX = customPivotX != -1 ? lx + customPivotX : unscaledCx;
        double pivotY = customPivotY != -1 ? ly + customPivotY : unscaledCy;

        // Update Pivots (Logical Center or Custom)
        scaleTransform.setPivotX(unscaledCx); // Scale always from true center
        scaleTransform.setPivotY(unscaledCy);
        rotateTransform.setPivotX(pivotX);
        rotateTransform.setPivotY(pivotY);
        shearTransform.setPivotX(pivotX);
        shearTransform.setPivotY(pivotY); // Rotate visual box around pivot

        // Update UI
        border.setX(visX);
        border.setY(visY);
        border.setWidth(visW);
        border.setHeight(visH);

        place(topLeft, visX, visY);
        place(topRight, visX + visW, visY);
        place(bottomLeft, visX, visY + visH);
        place(bottomRight, visX + visW, visY + visH);
        place(topCenter, visX + visW / 2, visY);
        place(bottomCenter, visX + visW / 2, visY + visH);
        place(leftCenter, visX, visY + visH / 2);
        place(rightCenter, visX + visW, visY + visH / 2);

        rotTopLeft.setLayoutX(visX - 10);
        rotTopLeft.setLayoutY(visY - 10);
        rotTopRight.setLayoutX(visX + visW - 10);
        rotTopRight.setLayoutY(visY - 10);
        rotBottomLeft.setLayoutX(visX - 10);
        rotBottomLeft.setLayoutY(visY + visH - 10);
        rotBottomRight.setLayoutX(visX + visW - 10);
        rotBottomRight.setLayoutY(visY + visH - 10);

        shearTop.setLayoutX(visX + visW / 2 - 10);
        shearTop.setLayoutY(visY - 10);
        shearBottom.setLayoutX(visX + visW / 2 - 10);
        shearBottom.setLayoutY(visY + visH - 10);
        shearLeft.setLayoutX(visX - 10);
        shearLeft.setLayoutY(visY + visH / 2 - 10);
        shearRight.setLayoutX(visX + visW - 10);
        shearRight.setLayoutY(visY + visH / 2 - 10);

        if (isRotationMode) {
            topLeft.setVisible(false);
            topRight.setVisible(false);
            bottomLeft.setVisible(false);
            bottomRight.setVisible(false);
            topCenter.setVisible(false);
            bottomCenter.setVisible(false);
            leftCenter.setVisible(false);
            rightCenter.setVisible(false);

            rotTopLeft.setVisible(true);
            rotTopRight.setVisible(true);
            rotBottomLeft.setVisible(true);
            rotBottomRight.setVisible(true);
            shearTop.setVisible(true);
            shearBottom.setVisible(true);
            shearLeft.setVisible(true);
            shearRight.setVisible(true);
            pivotHandle.setVisible(true);

            double pX = customPivotX != -1 ? customPivotX : lw / 2.0;
            double pY = customPivotY != -1 ? customPivotY : lh / 2.0;
            pivotHandle.setLayoutX(visX + pX - 8);
            pivotHandle.setLayoutY(visY + pY - 8);
        } else {
            topLeft.setVisible(true);
            topRight.setVisible(true);
            bottomLeft.setVisible(true);
            bottomRight.setVisible(true);
            topCenter.setVisible(true);
            bottomCenter.setVisible(true);
            leftCenter.setVisible(true);
            rightCenter.setVisible(true);

            rotTopLeft.setVisible(false);
            rotTopRight.setVisible(false);
            rotBottomLeft.setVisible(false);
            rotBottomRight.setVisible(false);
            shearTop.setVisible(false);
            shearBottom.setVisible(false);
            shearLeft.setVisible(false);
            shearRight.setVisible(false);
            pivotHandle.setVisible(false);
        }

        // --- ANTI-SHEAR (MANDATORY) ---
        // Repel shear from handles so they don't look squashed
        org.example.utils.GeometryUtility.applyAntiShear(topLeft, shearTransform, 3, 3);
        org.example.utils.GeometryUtility.applyAntiShear(topRight, shearTransform, 3, 3);
        org.example.utils.GeometryUtility.applyAntiShear(bottomLeft, shearTransform, 3, 3);
        org.example.utils.GeometryUtility.applyAntiShear(bottomRight, shearTransform, 3, 3);
        org.example.utils.GeometryUtility.applyAntiShear(topCenter, shearTransform, 3, 3);
        org.example.utils.GeometryUtility.applyAntiShear(bottomCenter, shearTransform, 3, 3);
        org.example.utils.GeometryUtility.applyAntiShear(leftCenter, shearTransform, 3, 3);
        org.example.utils.GeometryUtility.applyAntiShear(rightCenter, shearTransform, 3, 3);
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

    private void place(Circle c, double x, double y) {
        c.setTranslateX(x);
        c.setTranslateY(y);
    }

    public java.util.List<Node> getUserLayers() {
        return contentGroup.getChildren();
    }

    private void initDragEvents() {
        final class DragContext {
            double startParentX, startParentY;
            double startTranslateX, startTranslateY;
        }
        DragContext ctx = new DragContext();

        this.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown() && !isLocked()) {
                if (e.getClickCount() == 2) {
                    isRotationMode = !isRotationMode;
                    updateVisuals();
                    e.consume();
                    return;
                }
                if (this.getParent() != null) {
                    Point2D pInParent = this.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                    ctx.startParentX = pInParent.getX();
                    ctx.startParentY = pInParent.getY();
                    ctx.startTranslateX = this.getTranslateX();
                    ctx.startTranslateY = this.getTranslateY();

                    // CRITICAL FIX: Hide handles during drag to prevent visual glitch and improve
                    // performance
                    if (isSelected) {
                        handlesGroup.setVisible(false);
                    }

                    e.consume();
                    // REMOVED: updateVisuals() - Causes flickering and performance issues
                }
            }
        });

        this.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown() && !isLocked()) {
                if (this.getParent() != null) {
                    Point2D pCurrent = this.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                    double dx = pCurrent.getX() - ctx.startParentX;
                    double dy = pCurrent.getY() - ctx.startParentY;
                    this.setTranslateX(ctx.startTranslateX + dx);
                    this.setTranslateY(ctx.startTranslateY + dy);
                }
                e.consume();
            }
        });

        this.setOnMouseReleased(e -> {
            if (!isLocked()) {
                // CRITICAL FIX: Show handles again after drag completes
                if (isSelected) {
                    handlesGroup.setVisible(!isLocked());
                    updateVisuals(); // Update ONCE after drag completes
                }

                // Record History
                if (visualizer != null && visualizer.getHistoryManager() != null
                        && (ctx.startTranslateX != this.getTranslateX()
                                || ctx.startTranslateY != this.getTranslateY())) {

                    TransformCommand cmd = new TransformCommand(this,
                            ctx.startTranslateX, ctx.startTranslateY,
                            scaleTransform.getX(), scaleTransform.getY(),
                            rotateTransform.getAngle(),
                            null, null,
                            this.getTranslateX(), this.getTranslateY(),
                            scaleTransform.getX(), scaleTransform.getY(),
                            rotateTransform.getAngle(),
                            null, null,
                            null); // Groups don't have activeZone
                    visualizer.getHistoryManager().addCommand(cmd);
                }
            }
        });
    }

    private void initResizeEvents() {
        setupResize(topLeft, -1, -1);
        setupResize(topRight, 1, -1);
        setupResize(bottomLeft, -1, 1);
        setupResize(bottomRight, 1, 1);
        setupResize(topCenter, 0, -1);
        setupResize(bottomCenter, 0, 1);
        setupResize(leftCenter, -1, 0);
        setupResize(rightCenter, 1, 0);
    }

    private void setupResize(Circle handle, double dX, double dY) {
        class ChildState {
            Node node;
            double startX, startY;
            double startScaleX, startScaleY;
            double startW, startH;
            // Capture transform state for Undo
            double undoStartX, undoStartY;
            double undoStartScaleX, undoStartScaleY;
            double undoStartRotate;
        }

        final class ResizeCtx {
            double startWidth, startHeight;
            Point2D anchorInParent;
            java.util.List<ChildState> children = new java.util.ArrayList<>();
            // Undo State for Group itself
            double groupUndoStartX, groupUndoStartY;
            double groupUndoStartRotate;
            double appliedSoFarX = 1.0; // Local tracker for incremental scaling
            double appliedSoFarY = 1.0;
        }
        ResizeCtx ctx = new ResizeCtx();

        handle.setOnMousePressed(e -> {
            if (isLocked()) {
                e.consume();
                return;
            }
            ctx.startWidth = currentWidth;
            ctx.startHeight = currentHeight;

            // Capture Group Undo State
            ctx.groupUndoStartX = getTranslateX();
            ctx.groupUndoStartY = getTranslateY();
            ctx.groupUndoStartRotate = rotateTransform.getAngle();

            ctx.children.clear();
            for (Node n : contentGroup.getChildren()) {
                ChildState cs = new ChildState();
                cs.node = n;
                cs.startX = n.getTranslateX();
                cs.startY = n.getTranslateY();
                cs.startScaleX = n.getScaleX();
                cs.startScaleY = n.getScaleY();

                // Capture Child Undo State
                cs.undoStartX = n.getTranslateX();
                cs.undoStartY = n.getTranslateY();
                cs.undoStartScaleX = n.getScaleX();
                cs.undoStartScaleY = n.getScaleY();
                cs.undoStartRotate = n.getRotate(); // Or internal rotate if feasible

                if (n instanceof ShapeLayer) {
                    cs.startW = ((ShapeLayer) n).getWidth();
                    cs.startH = ((ShapeLayer) n).getHeight();
                }
                ctx.children.add(cs);
            }

            // Logical current minX/minY
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            for (Node n : contentGroup.getChildren()) {
                Bounds b = n.getBoundsInParent();
                if (b.getMinX() < minX)
                    minX = b.getMinX();
                if (b.getMinY() < minY)
                    minY = b.getMinY();
            }
            if (minX == Double.MAX_VALUE) {
                minX = 0;
                minY = 0;
            }

            double w = currentWidth;
            double h = currentHeight;
            double anchorLx = (dX == -1) ? (minX + w) : (dX == 1) ? minX : (minX + w / 2);
            double anchorLy = (dY == -1) ? (minY + h) : (dY == 1) ? minY : (minY + h / 2);

            Point2D anchorLocalNode = new Point2D(anchorLx, anchorLy);
            Point2D anchorInGroup = contentGroup.localToParent(anchorLocalNode);
            ctx.anchorInParent = this.localToParent(anchorInGroup);
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isLocked()) {
                e.consume();
                return;
            }
            Point2D mouseScene = new Point2D(e.getSceneX(), e.getSceneY());
            Point2D mouseParent = (this.getParent() != null) ? this.getParent().sceneToLocal(mouseScene) : mouseScene;

            double diffX = mouseParent.getX() - ctx.anchorInParent.getX();
            double diffY = mouseParent.getY() - ctx.anchorInParent.getY();

            // Project diff into unrotated local space
            double angleRad = Math.toRadians(-rotateTransform.getAngle());
            double localDiffX = diffX * Math.cos(angleRad) - diffY * Math.sin(angleRad);
            double localDiffY = diffX * Math.sin(angleRad) + diffY * Math.cos(angleRad);

            // Calculate total ratio relative to start
            // Safeguardó against division by zero (extreme sensitivity)
            double safeW = Math.max(0.1, ctx.startWidth);
            double safeH = Math.max(0.1, ctx.startHeight);

            double totalRatioX = (dX != 0) ? (safeW + localDiffX * dX) / safeW : 1.0;
            double totalRatioY = (dY != 0) ? (safeH + localDiffY * dY) / safeH : 1.0;

            if (totalRatioX < 0.01)
                totalRatioX = 0.01;
            if (totalRatioY < 0.01)
                totalRatioY = 0.01;

            // Increment multiplier relative to what we've already applied in this drag
            double msx = (dX != 0) ? totalRatioX / ctx.appliedSoFarX : 1.0;
            double msy = (dY != 0) ? totalRatioY / ctx.appliedSoFarY : 1.0;

            ctx.appliedSoFarX = totalRatioX; // Update local trackers
            ctx.appliedSoFarY = totalRatioY;

            // Apply to logical tracker? NO. We keep it 1:1.
            // scaleTransform.setX(ctx.groupUndoStartScaleX * totalRatioX);
            // scaleTransform.setY(ctx.groupUndoStartScaleY * totalRatioY);

            // Apply geometric scaling to child translations and sizes
            for (ChildState cs : ctx.children) {
                cs.node.setTranslateX(cs.startX * totalRatioX);
                cs.node.setTranslateY(cs.startY * totalRatioY);
                if (cs.node instanceof ShapeLayer) {
                    ((ShapeLayer) cs.node).setSize(cs.startW * totalRatioX, cs.startH * totalRatioY);
                } else if (cs.node instanceof GroupLayerV2) {
                    ((GroupLayerV2) cs.node).multiplyScale(msx, msy);
                } else if (cs.node instanceof GroupLayer) {
                    ((GroupLayer) cs.node).multiplyScale(msx, msy);
                } else {
                    cs.node.setScaleX(cs.startScaleX * totalRatioX);
                    cs.node.setScaleY(cs.startScaleY * totalRatioY);
                }
            }

            this.currentWidth = ctx.startWidth * totalRatioX;
            this.currentHeight = ctx.startHeight * totalRatioY;
            recalculateBounds();

            // POSITIONAL COMPENSATION (KEEP ANCHOR STABLE)
            // Re-find current minX/minY post-scale
            double newMinX = Double.MAX_VALUE;
            double newMinY = Double.MAX_VALUE;
            for (Node n : contentGroup.getChildren()) {
                Bounds b = n.getBoundsInParent();
                if (b.getMinX() < newMinX)
                    newMinX = b.getMinX();
                if (b.getMinY() < newMinY)
                    newMinY = b.getMinY();
            }
            if (newMinX == Double.MAX_VALUE) {
                newMinX = 0;
                newMinY = 0;
            }

            double anchorLx = (dX == -1) ? (newMinX + currentWidth)
                    : (dX == 1) ? newMinX : (newMinX + currentWidth / 2);
            double anchorLy = (dY == -1) ? (newMinY + currentHeight)
                    : (dY == 1) ? newMinY : (newMinY + currentHeight / 2);

            Point2D anchorInGroupNew = contentGroup.localToParent(new Point2D(anchorLx, anchorLy));
            Point2D currentAnchorWorld = this.localToParent(anchorInGroupNew);

            double shiftX = ctx.anchorInParent.getX() - currentAnchorWorld.getX();
            double shiftY = ctx.anchorInParent.getY() - currentAnchorWorld.getY();
            this.setTranslateX(this.getTranslateX() + shiftX);
            this.setTranslateY(this.getTranslateY() + shiftY);

            updateVisuals();
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isLocked())
                return;

            // Record History
            if (visualizer != null && visualizer.getHistoryManager() != null) {
                boolean changed = Math.abs(ctx.appliedSoFarX - 1.0) > 0.001
                        || Math.abs(ctx.appliedSoFarY - 1.0) > 0.001;

                if (changed) {
                    org.example.pattern.GroupUndoCommand groupCmd = new org.example.pattern.GroupUndoCommand(
                            "Redimensionar Grupo", null);

                    // 1. Add Group Self Transform - Use 1.0 -> appliedSoFar to trigger absorption
                    org.example.pattern.TransformCommand selfCmd = new org.example.pattern.TransformCommand(this,
                            ctx.groupUndoStartX, ctx.groupUndoStartY,
                            1.0, 1.0,
                            ctx.groupUndoStartRotate,
                            null, null,
                            getTranslateX(), getTranslateY(),
                            ctx.appliedSoFarX, ctx.appliedSoFarY,
                            rotateTransform.getAngle(),
                            null, null,
                            null);
                    groupCmd.addCommand(selfCmd);

                    // 2. Add Children Transforms
                    for (ChildState cs : ctx.children) {
                        org.example.pattern.TransformCommand childCmd = new org.example.pattern.TransformCommand(
                                cs.node,
                                cs.undoStartX, cs.undoStartY,
                                cs.undoStartScaleX, cs.undoStartScaleY,
                                cs.undoStartRotate,
                                null, null,
                                cs.node.getTranslateX(), cs.node.getTranslateY(),
                                cs.node.getScaleX(), cs.node.getScaleY(),
                                cs.node.getRotate(),
                                null, null,
                                null);

                        // Special handling for ShapeLayer W/H
                        // While we recorded startW/H in context, TransformCommand supports W/H.
                        // But we didn't pass W/H to constructor above.
                        // Let's assume TransformCommand handles standard properties.
                        // Wait, TransformCommand constructor I used has defaults for W/H as null.
                        // Ideally, we should capture W/H for shapes.
                        // But for now, let's rely on Scale or if we need to extend TransformCommand for
                        // W/H (it does support it).

                        groupCmd.addCommand(childCmd);
                    }

                    visualizer.getHistoryManager().addCommand(groupCmd);
                }
            }
            e.consume();
        });

    }

    private void setupCornerRotateHandler(javafx.scene.layout.StackPane handle) {
        final class RotCtx {
            double startAng, startMouseAngle;
            javafx.geometry.Point2D centerScene;
            org.example.pattern.NodeMemento beforeMemento;
        }
        RotCtx ctx = new RotCtx();

        handle.setOnMousePressed(e -> {
            if (isLocked())
                return;
            ctx.beforeMemento = new org.example.pattern.NodeMemento(this);
            ctx.centerScene = this.localToScene(rotateTransform.getPivotX(), rotateTransform.getPivotY());
            ctx.startMouseAngle = Math
                    .toDegrees(
                            Math.atan2(e.getSceneY() - ctx.centerScene.getY(), e.getSceneX() - ctx.centerScene.getX()));
            ctx.startAng = rotateTransform.getAngle();
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isLocked())
                return;

            double currentMouseAngle = Math
                    .toDegrees(
                            Math.atan2(e.getSceneY() - ctx.centerScene.getY(), e.getSceneX() - ctx.centerScene.getX()));
            rotateTransform.setAngle(ctx.startAng + (currentMouseAngle - ctx.startMouseAngle));
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isLocked())
                return;
            if (visualizer != null && visualizer.getHistoryManager() != null && ctx.beforeMemento != null) {
                org.example.pattern.NodeMemento afterMemento = new org.example.pattern.NodeMemento(this);
                org.example.pattern.TransformCommand cmd = new org.example.pattern.TransformCommand(this,
                        ctx.beforeMemento, afterMemento, activeZone);
                visualizer.getHistoryManager().addCommand(cmd);
            }
            e.consume();
        });
    }

    private void initShearEvents() {
        setupShearHandler(shearTop, true, true);
        setupShearHandler(shearBottom, true, false);
        setupShearHandler(shearLeft, false, true);
        setupShearHandler(shearRight, false, false);
    }

    private void setupShearHandler(javafx.scene.layout.StackPane handle, boolean isHorizontal,
            boolean invertDirection) {
        final class ShearCtx {
            double startSceneX, startSceneY;
            double initialShearX, initialShearY;
            double startTx, startTy;
            javafx.geometry.Point2D startPivotWorld;
            org.example.pattern.NodeMemento beforeMemento;
        }
        ShearCtx ctx = new ShearCtx();

        handle.setOnMousePressed(e -> {
            if (isLocked())
                return;

            ctx.beforeMemento = new org.example.pattern.NodeMemento(this);
            ctx.startSceneX = e.getSceneX();
            ctx.startSceneY = e.getSceneY();
            ctx.initialShearX = shearTransform.getX();
            ctx.initialShearY = shearTransform.getY();
            ctx.startTx = getTranslateX();
            ctx.startTy = getTranslateY();

            ctx.startPivotWorld = this.localToParent(shearTransform.getPivotX(), shearTransform.getPivotY());
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isLocked())
                return;

            double deltaX = e.getSceneX() - ctx.startSceneX;
            double deltaY = e.getSceneY() - ctx.startSceneY;

            double angleRad = Math.toRadians(rotateTransform.getAngle());
            double cos = Math.cos(angleRad);
            double sin = Math.sin(angleRad);

            double rotatedDeltaX = deltaX * cos + deltaY * sin;
            double rotatedDeltaY = -deltaX * sin + deltaY * cos;

            double shearAmount = 0;
            if (isHorizontal) {
                shearAmount = rotatedDeltaX / currentHeight;
                if (invertDirection)
                    shearAmount = -shearAmount;
                shearTransform.setX(ctx.initialShearX + shearAmount);
            } else {
                shearAmount = rotatedDeltaY / currentWidth;
                if (invertDirection)
                    shearAmount = -shearAmount;
                shearTransform.setY(ctx.initialShearY + shearAmount);
            }

            javafx.geometry.Point2D newPivotWorld = this.localToParent(shearTransform.getPivotX(),
                    shearTransform.getPivotY());
            double transDx = ctx.startPivotWorld.getX() - newPivotWorld.getX();
            double transDy = ctx.startPivotWorld.getY() - newPivotWorld.getY();

            this.setTranslateX(ctx.startTx + transDx);
            this.setTranslateY(ctx.startTy + transDy);

            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            if (isLocked())
                return;

            if (visualizer != null && visualizer.getHistoryManager() != null && ctx.beforeMemento != null) {
                org.example.pattern.NodeMemento afterMemento = new org.example.pattern.NodeMemento(this);
                org.example.pattern.TransformCommand cmd = new org.example.pattern.TransformCommand(this,
                        ctx.beforeMemento, afterMemento, activeZone);
                visualizer.getHistoryManager().addCommand(cmd);
            }
            e.consume();
        });
    }

    private void initRotateEvents() {
        setupCornerRotateHandler(rotTopLeft);
        setupCornerRotateHandler(rotTopRight);
        setupCornerRotateHandler(rotBottomLeft);
        setupCornerRotateHandler(rotBottomRight);

        pivotHandle.setOnMousePressed(e -> {
            e.consume();
        });

        pivotHandle.setOnMouseDragged(e -> {
            if (isLocked() && !isBeingEdited)
                return;
            double oldPx = rotateTransform.getPivotX();
            double oldPy = rotateTransform.getPivotY();

            javafx.geometry.Point2D currLocal = this.sceneToLocal(e.getSceneX(), e.getSceneY());
            double pxLocal = currLocal.getX() - boundsMinX;
            double pyLocal = currLocal.getY() - boundsMinY;

            // IMÁN: snap al centro si está dentro de 15px
            double SNAP_RADIUS = 15.0;
            double centerX = currentWidth / 2.0;
            double centerY = currentHeight / 2.0;
            double distX = pxLocal - centerX;
            double distY = pyLocal - centerY;
            double dist = Math.sqrt(distX * distX + distY * distY);

            if (dist <= SNAP_RADIUS) {
                customPivotX = -1;
                customPivotY = -1;
                pivotHandle.setOpacity(0.6);
            } else {
                customPivotX = pxLocal;
                customPivotY = pyLocal;
                pivotHandle.setOpacity(1.0);
            }
            updateVisuals();

            double newPx = rotateTransform.getPivotX();
            double newPy = rotateTransform.getPivotY();
            double dx = newPx - oldPx;
            double dy = newPy - oldPy;
            double angle = rotateTransform.getAngle();
            if (angle != 0 && (dx != 0 || dy != 0)) {
                double rad = Math.toRadians(angle);
                double cos = Math.cos(rad);
                double sin = Math.sin(rad);
                double rx = dx * cos - dy * sin;
                double ry = dx * sin + dy * cos;
                this.setTranslateX(this.getTranslateX() + (rx - dx));
                this.setTranslateY(this.getTranslateY() + (ry - dy));
            }

            e.consume();
        });

        pivotHandle.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                double oldPx = rotateTransform.getPivotX();
                double oldPy = rotateTransform.getPivotY();

                customPivotX = -1;
                customPivotY = -1;
                pivotHandle.setOpacity(1.0);
                updateVisuals();

                double newPx = rotateTransform.getPivotX();
                double newPy = rotateTransform.getPivotY();
                double dx = newPx - oldPx;
                double dy = newPy - oldPy;
                double angle = rotateTransform.getAngle();
                if (angle != 0 && (dx != 0 || dy != 0)) {
                    double rad = Math.toRadians(angle);
                    double cos = Math.cos(rad);
                    double sin = Math.sin(rad);
                    double rx = dx * cos - dy * sin;
                    double ry = dx * sin + dy * cos;
                    this.setTranslateX(this.getTranslateX() + (rx - dx));
                    this.setTranslateY(this.getTranslateY() + (ry - dy));
                }

                e.consume();
            }
        });
    }

    private javafx.scene.layout.StackPane createRotHandle() {
        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(8);
        bg.setFill(javafx.scene.paint.Color.web("#e67e22"));
        bg.setStroke(javafx.scene.paint.Color.WHITE);
        bg.setStrokeWidth(2);

        org.kordamp.ikonli.javafx.FontIcon icon = org.example.utils.UIFactory.crearIcono("mdi2s-sync", 10, "#ffffff");

        javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(bg, icon);
        pane.setCursor(Cursor.OPEN_HAND);

        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.web("#000000", 0.3));
        shadow.setRadius(2);
        pane.setEffect(shadow);

        org.example.utils.GeometryUtility.applyAntiShear(pane, shearTransform, 8, 8);

        return pane;
    }

    private javafx.scene.layout.StackPane createShearHandle(Cursor cursor, boolean isHorizontal) {
        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(8);
        bg.setFill(javafx.scene.paint.Color.web("#e67e22"));
        bg.setStroke(javafx.scene.paint.Color.WHITE);
        bg.setStrokeWidth(2);

        String iconString = isHorizontal ? "mdi2a-arrow-left-right" : "mdi2a-arrow-up-down";
        org.kordamp.ikonli.javafx.FontIcon icon = org.example.utils.UIFactory.crearIcono(iconString, 12, "#ffffff");

        javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(bg, icon);
        pane.setCursor(cursor);

        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setColor(javafx.scene.paint.Color.web("#000000", 0.3));
        shadow.setRadius(2);
        pane.setEffect(shadow);

        org.example.utils.GeometryUtility.applyAntiShear(pane, shearTransform, 8, 8);

        return pane;
    }
}

