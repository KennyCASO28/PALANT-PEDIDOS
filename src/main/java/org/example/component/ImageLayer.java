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
import org.example.pattern.RepeatActionRecorder;
import org.example.utils.GeometryUtility;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Coordinated Image Layer.
 * Modularized version that delegates logic to Specialized Handlers.
 */
public class ImageLayer extends AbstractGraphicLayer {

    @Override
    public javafx.geometry.Bounds calculateBounds() {
        return new javafx.geometry.BoundingBox(0, 0, getLogicalWidth(), getLogicalHeight());
    }

    @Override
    protected javafx.scene.Node createSilhouetteNode() {
        return new javafx.scene.shape.Rectangle(0, 0, getLogicalWidth(), getLogicalHeight());
    }

    @Override
    public void multiplyShear(double sx, double sy) {
        setInternalShearX(getInternalShearX() + sx);
        setInternalShearY(getInternalShearY() + sy);
    }

    private final ImageLayerState state;
    private final ImageEffectOrchestrator effectOrchestrator;
    private final ImageEditingService editingService;

    private final Canvas canvas = new Canvas();
    private final GraphicsContext gc = canvas.getGraphicsContext2D();

    // Transforms
    private final Scale inverseScaleTransform = new Scale();
    private final Scale canvasScaleTransform = new Scale();

    
    // Undo State Helper
    private double undoStartX, undoStartY, undoStartScaleX, undoStartScaleY, undoStartRotate;
    private boolean isBeingEditedField = false;
    
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

        setupCanvas(image);
        setupUI();
        
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
        updateCanvasScale();
    }

    private void setupUI() {
        this.setDepthTest(javafx.scene.DepthTest.DISABLE);
        this.setCache(false);
        this.setPickOnBounds(true);
        
        // Add canvas to contentGroup so it inherits transforms correctly from AbstractGraphicLayer
        contentGroup.getChildren().add(canvas);
        
        updateVisuals();
    }

    // --- GraphicLayer Implementation ---









    public void toggleRotationMode() {
        if (state.isCropMode) return; // Prevent rotation while cropping
        setRotationMode(!state.isRotationMode);
    }





    private void applyLockStyle() {
        boolean locked = isLocked();
        canvas.setCursor(locked ? Cursor.DEFAULT : Cursor.MOVE);
    }





    @Override
    public void setGrouped(boolean grouped) {
        state.isGrouped = grouped;
        if (grouped) {
            setSelected(false);
        }
    }

    @Override
    public boolean isGrouped() {
        return state.isGrouped;
    }













    private void applyAntiShearToAll() {
    }

    private void positionHandle(Node n, double x, double y) {
        // Since n has inverseScaleTransform, 1 unit in layout is 1 unit in parent
        // But the internal size (8px) is scale-neutral.
        // To center it, WE MUST offset it by 4 screen pixels in local units.
        // Local Offset = 4 * (1/inverseScale) = 4 * parentScale.
        // Wait, NO. Layout is in parent units. 
        // If we want to move it 4 screen pixels to the left:
        // OffsetInParent = 4 / parentScale.
        double viewportScale = (getVisualizer() != null && getVisualizer().getViewportController() != null) ? getVisualizer().getViewportController().getFinalScale() : 1.0;
        double sx = Math.abs(getInternalScaleX()) * viewportScale;
        double sy = Math.abs(getInternalScaleY()) * viewportScale;
        double offsetX = (sx > 0) ? (6.0 / sx) : 6.0;
        double offsetY = (sy > 0) ? (6.0 / sy) : 6.0;
        
        n.setLayoutX(x - offsetX);
        n.setLayoutY(y - offsetY);
    }






    
    public void recordUndoStateContent(String actionName) {
        editingService.prepareUndoState(actionName);
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
    public void setCustomPivot(double x, double y) { 
        state.pivotX = x; 
        state.pivotY = y; 
        if (x == -1 && y == -1) {
            setCustomPivotX(0);
            setCustomPivotY(0);
        } else {
            setCustomPivotX(x - getWidth() / 2.0);
            setCustomPivotY(y - getHeight() / 2.0);
        }
    }
    public Rotate getRotateTransform() { return rotateTransform; }
    public Shear getShearTransform() { return shearTransform; }
    
    @Override
    public double getInternalRotation() { return rotateTransform.getAngle(); }
    @Override
    public void setInternalRotation(double angle) {
        rotateTransform.setAngle(angle);
        super.setInternalRotation(angle);
    }
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
    
    @Override public double getInternalShearX() { return shearTransform.getX(); }
    @Override public void setInternalShearX(double s) {
        shearTransform.setX(s);
        super.setInternalShearX(s);
    }
    @Override public double getInternalShearY() { return shearTransform.getY(); }
    @Override public void setInternalShearY(double s) {
        shearTransform.setY(s);
        super.setInternalShearY(s);
    }




    public Canvas getCanvas() { return canvas; }
    public ImageEditingService editingService() { return editingService; }
    public boolean isProcessing() { return editingService.isProcessing(); }

    public TipoEscudo getBadgeType() { return state.badgeType; }
    public void setBadgeType(TipoEscudo t) { state.badgeType = t; }

    public void setCropMode(boolean b) {
        state.isCropMode = b;
        if (b) setRotationMode(false); // Exclusivity
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
        setInternalRotation(0);
        setInternalScaleX(1); setInternalScaleY(1);
        setInternalShearX(0); setInternalShearY(0);
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
            setInternalScaleX(getInternalScaleX() * -1);
            org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(this);
            getVisualizer().getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, after, state.activeZone));
            org.example.pattern.RepeatActionRecorder.recordTransform(before, after, false);
        } else {
            setInternalScaleX(getInternalScaleX() * -1);
        }
    }

    public void flipVertical() {
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
            setInternalScaleY(getInternalScaleY() * -1);
            org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(this);
            getVisualizer().getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before, after, state.activeZone));
            org.example.pattern.RepeatActionRecorder.recordTransform(before, after, false);
        } else {
            setInternalScaleY(getInternalScaleY() * -1);
        }
    }

    public void multiplyScale(double sx, double sy) {
        double currentVisW = state.width * Math.abs(getInternalScaleX());
        double currentVisH = state.height * Math.abs(getInternalScaleY());
        state.width = currentVisW * Math.abs(sx);
        state.height = currentVisH * Math.abs(sy);
        setInternalScaleX(Math.signum(getInternalScaleX() * sx));
        setInternalScaleY(Math.signum(getInternalScaleY() * sy));
        updateCanvasScale();
        updateVisuals();
    }



    public void addRotation(double angle) {
        setInternalRotation(getInternalRotation() + angle);
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
        
        // Búsqueda recursiva: el clic puede ser en un hijo del manejador (ej: hitArea)
        Node temp = node;
        while (temp != null) {
            if (temp == this) return true;
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
