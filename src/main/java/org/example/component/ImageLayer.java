package org.example.component;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import org.example.component.helper.*;
import org.example.model.ImageLayerState;
import org.example.model.TipoEscudo;
import org.example.pattern.ICommand;
import org.example.pattern.PropertyChangeCommand;
import org.example.utils.GeometryUtility;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Coordinated Image Layer.
 * Modularized version that delegates logic to Specialized Handlers.
 */
public class ImageLayer extends Group implements GraphicLayer {

    private final ImageLayerState state;
    private final ImageInteractionHandler interactionHandler;
    private final ImageEffectOrchestrator effectOrchestrator;
    private final ImageEditingService editingService;

    private final Canvas canvas = new Canvas();
    private final GraphicsContext gc = canvas.getGraphicsContext2D();

    // Transforms
    private final Rotate rotateTransform = new Rotate();
    private final Scale scaleTransform = new Scale();
    private final Scale inverseScaleTransform = new Scale();
    private final Shear shearTransform = new Shear();
    private final Scale canvasScaleTransform = new Scale();

    private PrendaVisualizer visualizer;

    // Undo State Helper
    private double undoStartX, undoStartY, undoStartScaleX, undoStartScaleY, undoStartRotate;
    private boolean isBeingEditedField = false;
    private boolean isSelected = false;

    // Callbacks
    private Consumer<Boolean> onSelectionChanged;
    private Consumer<String> powerClipHandler;
    private Consumer<ShapeLayer> internalPowerClipHandler;
    private Supplier<List<String>> availableZonesSupplier;
    private Runnable editContentHandler;
    private Runnable pasteHandler;
    private BooleanSupplier shouldShowShortsOptions;
    private java.util.function.BiConsumer<Double, Double> onDragHandler;

    public ImageLayer(Image image) {
        this.state = new ImageLayerState();
        this.state.originalImage = image;
        this.state.currentImage = image;

        this.editingService = new ImageEditingService(this, canvas);
        this.effectOrchestrator = new ImageEffectOrchestrator(this, state);
        this.interactionHandler = new ImageInteractionHandler(this, state);

        setupCanvas(image);
        setupUI();
        interactionHandler.init();
        
        applyLockStyle();
    }

    private void setupCanvas(Image image) {
        if (image != null) {
            double nw = image.getWidth();
            double nh = image.getHeight();
            canvas.setWidth(nw);
            canvas.setHeight(nh);
            gc.drawImage(image, 0, 0);

            state.width = 150;
            state.height = 150 / (nw / nh);
        }

        canvas.getTransforms().add(canvasScaleTransform);
        canvas.setEffect(effectOrchestrator.getEffect());
        canvas.setPickOnBounds(true);
        this.getChildren().add(canvas);
        updateCanvasScale();
    }

    private void setupUI() {
        this.setDepthTest(javafx.scene.DepthTest.DISABLE);
        this.setCache(false);
        this.getTransforms().addAll(rotateTransform, scaleTransform, shearTransform);
        
        ImageInteractionHandler.OverlayNodes n = interactionHandler.getOverlay();
        this.getChildren().add(n.border);
        this.getChildren().add(n.handlesGroup);
        
        // Register inverse scale to all handles
        registerInverseScale(n);
        
        updateVisuals();
    }

    private void registerInverseScale(ImageInteractionHandler.OverlayNodes n) {
        n.topLeft.getTransforms().add(inverseScaleTransform);
        n.topRight.getTransforms().add(inverseScaleTransform);
        n.bottomLeft.getTransforms().add(inverseScaleTransform);
        n.bottomRight.getTransforms().add(inverseScaleTransform);
        n.top.getTransforms().add(inverseScaleTransform);
        n.bottom.getTransforms().add(inverseScaleTransform);
        n.left.getTransforms().add(inverseScaleTransform);
        n.right.getTransforms().add(inverseScaleTransform);
        n.rotTopLeft.getTransforms().add(inverseScaleTransform);
        n.rotTopRight.getTransforms().add(inverseScaleTransform);
        n.rotBottomLeft.getTransforms().add(inverseScaleTransform);
        n.rotBottomRight.getTransforms().add(inverseScaleTransform);
        n.shearTop.getTransforms().add(inverseScaleTransform);
        n.shearBottom.getTransforms().add(inverseScaleTransform);
        n.shearLeft.getTransforms().add(inverseScaleTransform);
        n.shearRight.getTransforms().add(inverseScaleTransform);
        n.pivotHandle.getTransforms().add(inverseScaleTransform);
    }

    // --- GraphicLayer Implementation ---

    @Override
    public void setSelected(boolean s) {
        this.isSelected = s;
        if (!s) setCropMode(false);
        
        interactionHandler.getOverlay().border.setVisible(s);
        interactionHandler.getOverlay().handlesGroup.setVisible(s); // Mostrar handles aunque esté bloqueado

        if (s) {
            this.getChildren().remove(interactionHandler.getOverlay().handlesGroup);
            this.getChildren().add(interactionHandler.getOverlay().handlesGroup);
        }

        if (onSelectionChanged != null) onSelectionChanged.accept(s);
        applyLockStyle();
        updateVisuals();
    }

    @Override
    public boolean isSelected() { return isSelected; }

    @Override
    public void setLocked(boolean l) { setUserLocked(l); }

    @Override
    public void setRotationMode(boolean b) {
        state.isRotationMode = b;
        updateVisuals();
    }

    public void toggleRotationMode() {
        if (state.isCropMode) return; // Prevent rotation while cropping
        setRotationMode(!state.isRotationMode);
    }

    @Override
    public void setUserLocked(boolean l) { 
        state.isUserLocked = l;
        applyLockStyle();
    }

    @Override
    public void setSystemLocked(boolean l) {
        state.isLocked = l;
        applyLockStyle();
    }

    private void applyLockStyle() {
        boolean locked = isLocked();
        interactionHandler.getOverlay().handlesGroup.setVisible(isSelected() && !locked);
        canvas.setCursor(locked ? Cursor.DEFAULT : Cursor.MOVE);
    }

    @Override
    public boolean isLocked() { return state.isLocked || state.isUserLocked; }

    @Override
    public boolean isRotationMode() { return state.isRotationMode; }

    @Override
    public boolean isUserLocked() { return state.isUserLocked; }

    @Override
    public void setVisualizer(PrendaVisualizer v) { this.visualizer = v; }

    @Override
    public PrendaVisualizer getVisualizer() { return visualizer; }

    @Override
    public String getActiveZone() { return state.activeZone; }

    @Override
    public void setActiveZone(String z) {
        state.activeZone = z;
        setSystemLocked(z != null);
    }

    @Override
    public void updateVisuals() {
        double vw = state.width;
        double vh = state.height;

        double hx = state.isCropMode ? state.cropX : 0;
        double hy = state.isCropMode ? state.cropY : 0;
        double hw = state.isCropMode ? state.cropW : vw;
        double hh = state.isCropMode ? state.cropH : vh;

        interactionHandler.getOverlay().border.setX(hx);
        interactionHandler.getOverlay().border.setY(hy);
        interactionHandler.getOverlay().border.setWidth(hw);
        interactionHandler.getOverlay().border.setHeight(hh);
        interactionHandler.getOverlay().border.setVisible(isSelected() || state.isCropMode);
        
        double viewportScale = (visualizer != null && visualizer.getViewportController() != null) ? visualizer.getViewportController().getFinalScale() : 1.0;
        interactionHandler.getOverlay().border.setStrokeWidth(1.0 / viewportScale);

        // Position handles
        ImageInteractionHandler.OverlayNodes n = interactionHandler.getOverlay();
        
        // DISABLE HANDLES INTERFERENCE DURING DRAWING (Avoid "Magnet" effect)
        DrawingToolContext ctx = visualizer != null ? visualizer.getDrawingToolContext() : null;
        boolean drawingToolActive = ctx != null && (ctx.isBrushActive() || ctx.isEraserActive());
        n.handlesGroup.setMouseTransparent(drawingToolActive);
        n.handlesGroup.setOpacity(drawingToolActive ? 0.3 : 1.0); // Visual feedback
        
        boolean small = hw < 20 || hh < 20;
        boolean ultraSmall = hw < 10 || hh < 10;
        boolean selected = isSelected();

        // 1. Logic for corners
        n.topLeft.setVisible(!ultraSmall && selected);
        n.topRight.setVisible(!ultraSmall && selected);
        n.bottomLeft.setVisible(!ultraSmall && selected);
        n.bottomRight.setVisible(selected); // Always keep at least one for resizing

        positionHandle(n.topLeft, hx, hy);
        positionHandle(n.topRight, hx + hw, hy);
        positionHandle(n.bottomLeft, hx, hy + hh);
        positionHandle(n.bottomRight, hx + hw, hy + hh);

        // 2. Logic for Mid-handles (already implemented, now with UltraSmall check)
        n.top.setVisible(!small && !ultraSmall && selected);
        n.right.setVisible(!small && !ultraSmall && selected);
        n.bottom.setVisible(!small && !ultraSmall && selected);
        n.left.setVisible(!small && !ultraSmall && selected);
        
        positionHandle(n.top, hx + hw / 2, hy);
        positionHandle(n.right, hx + hw, hy + hh / 2);
        positionHandle(n.bottom, hx + hw / 2, hy + hh);
        positionHandle(n.left, hx, hy + hh / 2);

        boolean showRotate = state.isRotationMode;
        // In UltraSmall, common rotation handles can also be annoying, but keep them if user forced mode
        n.rotTopLeft.setVisible(showRotate && !ultraSmall);
        n.rotTopRight.setVisible(showRotate && !ultraSmall);
        n.rotBottomLeft.setVisible(showRotate && !ultraSmall);
        n.rotBottomRight.setVisible(showRotate); // Keep one
        positionHandle(n.rotTopLeft, hx, hy);
        positionHandle(n.rotTopRight, hx + hw, hy);
        positionHandle(n.rotBottomLeft, hx, hy + hh);
        positionHandle(n.rotBottomRight, hx + hw, hy + hh);

        n.shearTop.setVisible(showRotate);
        n.shearBottom.setVisible(showRotate);
        n.shearLeft.setVisible(showRotate);
        n.shearRight.setVisible(showRotate);
        positionHandle(n.shearTop, hx + hw / 2, hy);
        positionHandle(n.shearBottom, hx + hw / 2, hy + hh);
        positionHandle(n.shearLeft, hx, hy + hh / 2);
        positionHandle(n.shearRight, hx + hw, hy + hh / 2);

        if (showRotate) {
            n.pivotHandle.setVisible(true);
            double pivotX = (state.pivotX == -1) ? vw / 2 : state.pivotX;
            double pivotY = (state.pivotY == -1) ? vh / 2 : state.pivotY;
            n.pivotHandle.setLayoutX(pivotX - 5);
            n.pivotHandle.setLayoutY(pivotY - 5);

            rotateTransform.setPivotX(pivotX);
            rotateTransform.setPivotY(pivotY);
            scaleTransform.setPivotX(pivotX);
            scaleTransform.setPivotY(pivotY);
            shearTransform.setPivotX(pivotX);
            shearTransform.setPivotY(pivotY);
        } else {
            n.pivotHandle.setVisible(false);
            rotateTransform.setPivotX(vw / 2);
            rotateTransform.setPivotY(vh / 2);
            scaleTransform.setPivotX(vw / 2);
            scaleTransform.setPivotY(vh / 2);
            shearTransform.setPivotX(vw / 2);
        }
        if (state.isCropMode) {
            n.cropOverlay.setX(state.cropX);
            n.cropOverlay.setY(state.cropY);
            n.cropOverlay.setWidth(state.cropW);
            n.cropOverlay.setHeight(state.cropH);
            n.cropOverlay.setVisible(true);
        } else {
            n.cropOverlay.setVisible(false);
        }

        // UPDATE INVERSE SCALE FOR HANDLES (Scale Neutrality)
        // viewportScale already computed above
        viewportScale = (visualizer != null && visualizer.getViewportController() != null) ? visualizer.getViewportController().getFinalScale() : 1.0;
        double sx = Math.abs(scaleTransform.getX()) * viewportScale;
        double sy = Math.abs(scaleTransform.getY()) * viewportScale;
        if (sx > 0 && sy > 0) {
            inverseScaleTransform.setX(1.0 / sx);
            inverseScaleTransform.setY(1.0 / sy);
        }
        
        applyAntiShearToAll();
    }

    private void applyAntiShearToAll() {
        ImageInteractionHandler.OverlayNodes n = interactionHandler.getOverlay();
        GeometryUtility.applyAntiShear(n.topLeft, shearTransform, 2, 2);
        GeometryUtility.applyAntiShear(n.topRight, shearTransform, 2, 2);
        GeometryUtility.applyAntiShear(n.bottomLeft, shearTransform, 2, 2);
        GeometryUtility.applyAntiShear(n.bottomRight, shearTransform, 2, 2);
        GeometryUtility.applyAntiShear(n.top, shearTransform, 2, 2);
        GeometryUtility.applyAntiShear(n.bottom, shearTransform, 2, 2);
        GeometryUtility.applyAntiShear(n.left, shearTransform, 2, 2);
        GeometryUtility.applyAntiShear(n.right, shearTransform, 2, 2);
        GeometryUtility.applyAntiShear(n.rotTopLeft, shearTransform, 5, 5);
        GeometryUtility.applyAntiShear(n.rotTopRight, shearTransform, 5, 5);
        GeometryUtility.applyAntiShear(n.rotBottomLeft, shearTransform, 5, 5);
        GeometryUtility.applyAntiShear(n.rotBottomRight, shearTransform, 5, 5);
        GeometryUtility.applyAntiShear(n.shearTop, shearTransform, 5, 5);
        GeometryUtility.applyAntiShear(n.shearBottom, shearTransform, 5, 5);
        GeometryUtility.applyAntiShear(n.shearLeft, shearTransform, 5, 5);
        GeometryUtility.applyAntiShear(n.shearRight, shearTransform, 5, 5);
        GeometryUtility.applyAntiShear(n.pivotHandle, shearTransform, 5, 5);
    }

    private void positionHandle(Node n, double x, double y) {
        // Since n has inverseScaleTransform, 1 unit in layout is 1 unit in parent
        // But the internal size (8px) is scale-neutral.
        // To center it, WE MUST offset it by 4 screen pixels in local units.
        // Local Offset = 4 * (1/inverseScale) = 4 * parentScale.
        // Wait, NO. Layout is in parent units. 
        // If we want to move it 4 screen pixels to the left:
        // OffsetInParent = 4 / parentScale.
        double viewportScale = (visualizer != null && visualizer.getViewportController() != null) ? visualizer.getViewportController().getFinalScale() : 1.0;
        double sx = Math.abs(scaleTransform.getX()) * viewportScale;
        double sy = Math.abs(scaleTransform.getY()) * viewportScale;
        double offsetX = (sx > 0) ? (6.0 / sx) : 6.0;
        double offsetY = (sy > 0) ? (6.0 / sy) : 6.0;
        
        n.setLayoutX(x - offsetX);
        n.setLayoutY(y - offsetY);
    }

    @Override
    public void render() { updateVisuals(); }

    @Override
    public Node getNode() { return this; }

    @Override
    public void recordUndoState() {
        undoStartX = getTranslateX();
        undoStartY = getTranslateY();
        undoStartScaleX = getScaleX();
        undoStartScaleY = getScaleY();
        undoStartRotate = getRotate();
    }
    
    public void recordUndoStateContent(String actionName) {
        editingService.prepareUndoState(actionName);
    }

    @Override
    public double getInternalRotation() { return rotateTransform.getAngle(); }
    @Override
    public void setInternalRotation(double angle) { rotateTransform.setAngle(angle); updateVisuals(); }
    @Override
    public void setInternalScaleX(double s) { 
        scaleTransform.setX(s); 
        updateVisuals(); 
    }
    @Override
    public double getInternalScaleY() { return scaleTransform.getY(); }
    @Override
    public void setInternalScaleY(double s) { 
        scaleTransform.setY(s); 
        updateVisuals(); 
    }

    // --- Image API ---

    public void setImage(Image img) {
        if (state.originalImage == null) state.originalImage = img;
        else if (img != state.originalImage) state.isModified = true;
        
        state.currentImage = img;
        state.snapshotDirty = false;
        state.base64Content = null;
        
        double nw = img.getWidth();
        double nh = img.getHeight();
        canvas.setWidth(nw);
        canvas.setHeight(nh);
        gc.clearRect(0, 0, nw, nh);
        gc.drawImage(img, 0, 0);
        
        state.height = state.width / (nw / nh);
        updateCanvasScale();
        updateVisuals();
    }

    public Image getImage() {
        if (state.snapshotDirty) refreshSnapshot();
        return state.currentImage != null ? state.currentImage : state.originalImage;
    }

    public void setSize(double w, double h) {
        state.width = w;
        state.height = h;
        updateCanvasScale();
        updateVisuals();
    }

    public void resize(double w, double h) {
        setSize(w, h);
    }

    private void updateCanvasScale() {
        canvasScaleTransform.setX(state.width / canvas.getWidth());
        canvasScaleTransform.setY(state.height / canvas.getHeight());
    }

    public WritableImage snapshotCanvas() {
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        
        // Al capturar, neutralizamos la escala visual para obtener los píxeles reales (HD)
        double sx = canvasScaleTransform.getX();
        double sy = canvasScaleTransform.getY();
        if (sx > 0 && sy > 0) {
            params.setTransform(javafx.scene.transform.Transform.scale(1.0/sx, 1.0/sy));
        }

        return canvas.snapshot(params, null);
    }

    private void refreshSnapshot() {
        state.currentImage = snapshotCanvas();
        state.snapshotDirty = false;
        state.base64Content = null;
    }

    // --- Legacy / Compatibility ---
    public double getWidth() { return state.width; }
    public double getHeight() { return state.height; }
    public double getLogicalWidth() { return state.width; }
    public double getLogicalHeight() { return state.height; }
    public double getCurrentWidth() { return state.width; }
    public double getCurrentHeight() { return state.height; }

    public void setModified(boolean b) { state.isModified = b; }
    public void setSnapshotDirty(boolean b) { state.snapshotDirty = b; }
    public void setCustomPivot(double x, double y) { state.pivotX = x; state.pivotY = y; }
    public Rotate getRotateTransform() { return rotateTransform; }
    public Shear getShearTransform() { return shearTransform; }
    
    @Override public double getInternalShearX() { return shearTransform.getX(); }
    @Override public void setInternalShearX(double s) { shearTransform.setX(s); }
    @Override public double getInternalShearY() { return shearTransform.getY(); }
    @Override public void setInternalShearY(double s) { shearTransform.setY(s); }
    @Override public double getCustomPivotX() { return state.pivotX; }
    @Override public void setCustomPivotX(double x) { state.pivotX = x; }
    @Override public double getCustomPivotY() { return state.pivotY; }
    @Override public void setCustomPivotY(double y) { state.pivotY = y; }
    public Canvas getCanvas() { return canvas; }
    public ImageEditingService editingService() { return editingService; }
    public boolean isProcessing() { return editingService.isProcessing(); }

    public TipoEscudo getBadgeType() { return state.badgeType; }
    public void setBadgeType(TipoEscudo t) { state.badgeType = t; }

    public void setCropMode(boolean b) {
        state.isCropMode = b;
        if (b) setRotationMode(false); // Exclusivity
        interactionHandler.getOverlay().cropOverlay.setVisible(b);
        if (b) {
            state.cropX = 0; state.cropY = 0;
            state.cropW = state.width; state.cropH = state.height;
        }
        updateVisuals();
    }
    public boolean isCropMode() { return state.isCropMode; }
    public void resetCrop() { 
        if (state.originalImage != null && state.currentImage != null) {
            // Calculate current visual scale (how many logical units per pixel)
            double currentScale = state.width / state.currentImage.getWidth();
            
            // Revert image
            setImage(state.originalImage);
            
            // Restore visual size based on original pixels and maintained scale
            setSize(state.originalImage.getWidth() * currentScale, 
                    state.originalImage.getHeight() * currentScale);
            
            state.isModified = false;
        }
        setCropMode(false); 
    }
    public void commitIfInCropMode() { 
        if (isCropMode()) {
            editingService.applyCrop(state.cropX, state.cropY, state.cropW, state.cropH);
            setCropMode(false); 
        }
    }

    public void resetTransforms() {
        setTranslateX(250); setTranslateY(250); // Default reset pos
        setRotate(0); setScaleX(1); setScaleY(1);
        rotateTransform.setAngle(0);
        scaleTransform.setX(1); scaleTransform.setY(1);
        shearTransform.setX(0); shearTransform.setY(0);
        updateVisuals();
    }

    public double getUndoStartX() { return undoStartX; }
    public double getUndoStartY() { return undoStartY; }
    public double getUndoStartScaleX() { return undoStartScaleX; }
    public double getUndoStartScaleY() { return undoStartScaleY; }
    public double getUndoStartRotate() { return undoStartRotate; }

    public void setOnSelectionChanged(Consumer<Boolean> c) { this.onSelectionChanged = c; }
    public void setPowerClipHandler(Consumer<String> h) { this.powerClipHandler = h; }
    public void setInternalPowerClipHandler(Consumer<ShapeLayer> h) { this.internalPowerClipHandler = h; }
    public void setAvailableZonesSupplier(Supplier<List<String>> s) { this.availableZonesSupplier = s; }
    public void setEditContentHandler(Runnable r) { this.editContentHandler = r; }
    public void setPasteHandler(Runnable r) { this.pasteHandler = r; }
    public void setShortsOptionsVisibility(BooleanSupplier s) { this.shouldShowShortsOptions = s; }
    public void setOnDragHandler(java.util.function.BiConsumer<Double, Double> h) { this.onDragHandler = h; }
    public java.util.function.BiConsumer<Double, Double> getOnDragHandler() { return onDragHandler; }

    public void setIsBeingEdited(boolean b) { this.isBeingEditedField = b; applyLockStyle(); }

    public double getBrightness() { return state.brightness; }
    public void setBrightness(double b) { effectOrchestrator.setBrightness(b); }
    public double getContrast() { return state.contrast; }
    public void setContrast(double c) { effectOrchestrator.setContrast(c); }
    public double getSaturation() { return state.saturation; }
    public void setSaturation(double s) { effectOrchestrator.setSaturation(s); }
    public void resetAdjustments() { effectOrchestrator.reset(); }
    public void applyAdjustmentsToPixels() { effectOrchestrator.applyAdjustmentsToPixels(); }

    public void flipHorizontal() {
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
            setInternalScaleX(scaleTransform.getX() * -1);
            visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, new org.example.pattern.NodeMemento(this), state.activeZone));
        } else {
            setInternalScaleX(scaleTransform.getX() * -1);
        }
    }

    public void flipVertical() {
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
            setInternalScaleY(scaleTransform.getY() * -1);
            visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, new org.example.pattern.NodeMemento(this), state.activeZone));
        } else {
            setInternalScaleY(scaleTransform.getY() * -1);
        }
    }

    public void multiplyScale(double sx, double sy) {
        scaleTransform.setX(scaleTransform.getX() * sx);
        scaleTransform.setY(scaleTransform.getY() * sy);
        updateVisuals();
    }

    @Override
    public double getInternalScaleX() { return scaleTransform.getX(); }

    public void addRotation(double angle) {
        rotateTransform.setAngle(rotateTransform.getAngle() + angle);
        updateVisuals();
    }

    public void rotateBy(int degrees) { addRotation(degrees); }

    public Point2D getStableCenter() {
        return localToParent(state.width / 2.0, state.height / 2.0);
    }

    public void setDimensions(double w, double h) { setSize(w, h); }

    public double getMagicWandTolerance() { return state.magicWandTolerance; }
    public void setMagicWandTolerance(Double t) { if (t != null) state.magicWandTolerance = t; }

    public boolean isBeingEdited() { 
        return isBeingEditedField || state.isRotationMode || state.isCropMode; 
    }

    public void removeWhiteBackground(Image img) { editingService.removeBackgroundByColor(img, Color.WHITE, state.magicWandTolerance); }
    public void removeWhiteBackground() { editingService.removeBackgroundByColor(Color.WHITE, state.magicWandTolerance); }
    public void removeBlackBackground(Image img) { editingService.removeBackgroundByColor(img, Color.BLACK, state.magicWandTolerance); }
    public void removeBlackBackground() { editingService.removeBackgroundByColor(Color.BLACK, state.magicWandTolerance); }
    public void removeBackgroundAt(Image img, int x, int y, double tolerance) { editingService.removeBackgroundAt(img, x, y, tolerance); }
    public void removeBackgroundAt(int x, int y, double tolerance) { editingService.removeBackgroundAt(x, y, tolerance); }

    public boolean isPartOfLayer(Node node) {
        if (node == this || node == canvas) return true;
        Group handles = interactionHandler.getOverlay().handlesGroup;
        
        // Búsqueda recursiva: el clic puede ser en un hijo del manejador (ej: hitArea)
        Node temp = node;
        while (temp != null) {
            if (temp == handles) return true;
            temp = temp.getParent();
        }
        return false;
    }
    
    // Clipboard Bridge
    public void copyToClipboard() { ImageClipboardSupport.copy(this); }
    public void cutToClipboard() { ImageClipboardSupport.cut(this); }
    public static boolean hasClipboard() { return ImageClipboardSupport.hasClipboard(); }
    public static void clearClipboard() { ImageClipboardSupport.clear(); }
    public static ImageLayer getClipboardCopy() { return ImageClipboardSupport.getClipboardCopy(); }

    // Logic Bridge
    public void trimWhitespace() {
        editingService.trimWhitespace();
    }

    public String getBase64Content() { return state.base64Content; }
    public void setBase64Content(String b64) { state.base64Content = b64; }
}
