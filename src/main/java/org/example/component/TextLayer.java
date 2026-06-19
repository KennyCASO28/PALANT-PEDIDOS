package org.example.component;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.effect.DropShadow;
import org.example.model.TextShape;
import org.example.model.TrajectoryPath;
import org.example.pattern.NodeMemento;
import org.example.pattern.PropertyChangeCommand;
import org.example.pattern.TransformCommand;
import org.example.utils.UIFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Professional Text Layer Component.
 * Optimized for high-performance rendering and consistent interaction.
 */
public class TextLayer extends Group implements GraphicLayer {

    private final Group textGroup = new Group();

    public Group getTextGroup() {
        return textGroup;
    }

    private final Rectangle border = new Rectangle();
    private final Group handlesGroup = new Group();
    private final Group trajectoryEditingGroup = new Group();
    private InlineTextEditor inlineEditor;

    // Interactive Handles
    private final StackPane nw, ne, sw, se;
    private final StackPane n, s, e, w;
    private final StackPane rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight;
    private final StackPane shearTop, shearBottom, shearLeft, shearRight;
    private final Group pivotHandle;

    // Transforms
    private final Rotate rotateTransform = new Rotate();
    private final Scale scaleTransform = new Scale();
    private final Shear shearTransform = new Shear();

    // Clipboard Static State
    private static TextLayer clipboardLayer = null;

    // Model Properties
    private String textContent;
    private Font font;
    private double fontSize = 24.0;
    private Color textColor;
    private Color strokeColor = Color.TRANSPARENT;
    private double strokeWidth = 0;
    private boolean isBold = false;
    private boolean isItalic = false;
    private javafx.scene.text.TextAlignment textAlignment = javafx.scene.text.TextAlignment.CENTER;
    private boolean dropShadowEnabled = false;
    private Color dropShadowColor = Color.BLACK;
    private TrajectoryPath trajectory = new TrajectoryPath(TrajectoryPath.Type.STRAIGHT);
    private TrajectoryPath trajectorySnapshot; // For Undo/Redo

    // Logical Dimensions
    private double logicalWidth = 300;
    private double logicalHeight = 100;
    private double spacing = 0;

    // State
    private boolean isSelected = false;
    private boolean isRotationMode = false;
    private double customPivotX = -1, customPivotY = -1;
    private boolean isLocked = false;
    private boolean isUserLocked = false;
    private boolean isTrajectoryEditMode = false;
    private boolean isUpdating = false;
    private boolean isBeingEdited = false;
    private boolean isInitialized = false;
    private boolean isDraggingTrajectoryHandle = false;
    private String activeZone;

    // Contour
    private int contourSteps = 0;
    private double contourDistance = 1.0;
    private Color contourColor = Color.TRANSPARENT;

    // Callbacks
    private Consumer<MouseEvent> onSelectionRequested;
    private BiConsumer<Double, Double> onDragHandler;
    private BiConsumer<Double, Double> onResizeHandler;
    private Consumer<TextLayer> editHandler;
    private Runnable deleteHandler;
    private Consumer<String> powerClipHandler;
    private Supplier<List<String>> availableZonesSupplier;
    private Runnable pasteHandler;
    private final List<Runnable> visualChangeListeners = new ArrayList<>();
    private org.example.component.PrendaVisualizer visualizer;

    // Tooltip for hover
    private final Tooltip hoverTooltip = new Tooltip();

    public TextLayer(String text) {
        this(text, Font.font("Arial", FontWeight.BOLD, 24), Color.BLACK);
    }

    public TextLayer(String text, Font font, Color color) {
        this(text, font, color, TextShape.STRAIGHT);
    }

    public TextLayer(String text, Font font, Color color, TextShape shape) {
        this.textContent = text;
        this.font = font;
        this.textColor = color;
        if (font != null) {
            String style = font.getStyle().toLowerCase();
            this.isBold = style.contains("bold");
            this.isItalic = style.contains("italic");
        }

        // Initialize Handles
        int hSize = 4; // Standardized square handle size
        int iSize = 12; // Standardized icon handle size
        // Set icon to null to remove "?" icons - clean squares look more professional
        this.nw = UIFactory.crearSquareHandle(null, hSize, "#0047AB", "#fff", Cursor.NW_RESIZE);
        this.ne = UIFactory.crearSquareHandle(null, hSize, "#0047AB", "#fff", Cursor.NE_RESIZE);
        this.sw = UIFactory.crearSquareHandle(null, hSize, "#0047AB", "#fff", Cursor.SW_RESIZE);
        this.se = UIFactory.crearSquareHandle(null, hSize, "#0047AB", "#fff", Cursor.SE_RESIZE);
        this.n = UIFactory.crearSquareHandle(null, hSize, "#0047AB", "#fff", Cursor.V_RESIZE);
        this.s = UIFactory.crearSquareHandle(null, hSize, "#0047AB", "#fff", Cursor.V_RESIZE);
        this.e = UIFactory.crearSquareHandle(null, hSize, "#0047AB", "#fff", Cursor.H_RESIZE);
        this.w = UIFactory.crearSquareHandle(null, hSize, "#0047AB", "#fff", Cursor.H_RESIZE);

        this.rotTopLeft = UIFactory.crearIconHandle("mdi2r-rotate-right", iSize, "#e67e22", Cursor.HAND);
        this.rotTopRight = UIFactory.crearIconHandle("mdi2r-rotate-right", iSize, "#e67e22", Cursor.HAND);
        this.rotBottomLeft = UIFactory.crearIconHandle("mdi2r-rotate-right", iSize, "#e67e22", Cursor.HAND);
        this.rotBottomRight = UIFactory.crearIconHandle("mdi2r-rotate-right", iSize, "#e67e22", Cursor.HAND);

        this.shearTop = UIFactory.crearIconHandle("mdi2a-arrow-expand-horizontal", iSize, "#16a085", Cursor.H_RESIZE);
        this.shearBottom = UIFactory.crearIconHandle("mdi2a-arrow-expand-horizontal", iSize, "#16a085",
                Cursor.H_RESIZE);
        this.shearLeft = UIFactory.crearIconHandle("mdi2a-arrow-expand-vertical", iSize, "#16a085", Cursor.V_RESIZE);
        this.shearRight = UIFactory.crearIconHandle("mdi2a-arrow-expand-vertical", iSize, "#16a085", Cursor.V_RESIZE);

        this.pivotHandle = UIFactory.crearPivotHandle();

        // Setup Transforms
        this.getTransforms().addAll(rotateTransform, scaleTransform, shearTransform);

        // Styling Border
        border.setFill(null);
        border.setStroke(Color.web("#0047AB"));
        border.setStrokeWidth(1);
        border.getStrokeDashArray().addAll(4.0, 4.0);
        border.setVisible(false);

        // Assembly
        handlesGroup.getChildren().addAll(nw, ne, sw, se, n, s, e, w,
                rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight,
                shearTop, shearBottom, shearLeft, shearRight,
                pivotHandle);

        // Reset all layouts to ensure TranslateX/Y is the only driver
        handlesGroup.getChildren().forEach(node -> {
            node.setLayoutX(0);
            node.setLayoutY(0);
        });

        handlesGroup.setVisible(false);
        trajectoryEditingGroup.setPickOnBounds(false);

        this.getChildren().addAll(textGroup, border, handlesGroup, trajectoryEditingGroup);

        // Ensure Group picking is reliable
        this.setPickOnBounds(false);
        textGroup.setPickOnBounds(true);
        border.setMouseTransparent(true); // Don't block character selection

        // Initialize hover tooltip
        updateHoverTooltip();
        this.setOnMouseEntered(e -> {
            if (!isSelected) {
                updateHoverTooltip();
            }
        });

        initInteraction();
        renderText();
        this.isInitialized = true;
    }

    private void updateHoverTooltip() {
        String zoneInfo = (activeZone != null && !activeZone.isEmpty()) ? "\nZona: " + activeZone : "";
        String display = (textContent != null && !textContent.isEmpty()) ? textContent : "(Vacío)";
        hoverTooltip.setText("Texto: \"" + display + "\"" + zoneInfo);
        Tooltip.install(this, hoverTooltip);
    }

    private void initInteraction() {
        this.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                // Double-click on text content starts inline editing
                if (!isHandle(e.getTarget()) && !isLocked()) {
                    startInlineEdit();
                    e.consume();
                }
            }
        });

        setupResizeHandle(nw, -1, -1);
        setupResizeHandle(ne, 1, -1);
        setupResizeHandle(sw, -1, 1);
        setupResizeHandle(se, 1, 1);
        setupResizeHandle(n, 0, -1);
        setupResizeHandle(s, 0, 1);
        setupResizeHandle(e, 1, 0);
        setupResizeHandle(w, -1, 0);

        setupRotationHandler();
        setupShearHandlers();
        setupPivotHandler();
        final double[] start = new double[2];
        final NodeMemento[] dragStartMemento = new NodeMemento[1];
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (isLocked() || isHandle(e.getTarget()))
                return;
            if (onSelectionRequested != null)
                onSelectionRequested.accept(e);

            Point2D localMouse = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            start[0] = localMouse.getX() - getTranslateX();
            start[1] = localMouse.getY() - getTranslateY();
            dragStartMemento[0] = new NodeMemento(this);
            setCursor(Cursor.MOVE);
            e.consume();
        });

        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (isLocked() || isHandle(e.getTarget()))
                return;
            Point2D localMouse = getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
            double newX = localMouse.getX() - start[0];
            double newY = localMouse.getY() - start[1];
            double dx = newX - getTranslateX();
            double dy = newY - getTranslateY();
            setTranslateX(newX);
            setTranslateY(newY);
            if (onDragHandler != null)
                onDragHandler.accept(dx, dy);
            e.consume();
        });

        this.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            setCursor(Cursor.DEFAULT);
            recordTransformUndo(dragStartMemento[0]);
            dragStartMemento[0] = null;
        });

        // --- LAYER HOVER FEEDBACK ---
        this.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (!isSelected && !isLocked()) {
                border.setVisible(true);
                border.setStroke(Color.web("#3498db", 0.5));
                border.setStrokeWidth(1.5);
                border.getStrokeDashArray().setAll(5.0, 5.0);
            }
        });
        this.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (!isSelected) {
                border.setVisible(false);
                border.setStroke(Color.web("#0047AB"));
                border.setStrokeWidth(1.0);
                border.getStrokeDashArray().clear();
            }
        });
    }

    private void setupResizeHandle(Node handle, int sx, int sy) {
        final double[] initialDim = new double[2];
        final double[] initialMouse = new double[2];
        final double[] initialScale = new double[2];
        final Point2D[] anchor = new Point2D[1];
        final NodeMemento[] startMemento = new NodeMemento[1];

        handle.setOnMousePressed(e -> {
            if (isLocked())
                return;
            startMemento[0] = new NodeMemento(this);
            initialDim[0] = logicalWidth;
            initialDim[1] = logicalHeight;
            initialScale[0] = scaleTransform.getX();
            initialScale[1] = scaleTransform.getY();

            // Use parent space coordinates to account for workspace zoom
            Point2D parentMouse = visualizer.getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());
            initialMouse[0] = parentMouse.getX();
            initialMouse[1] = parentMouse.getY();

            // Anchor at the opposite side of the dragged handle (centered logic)
            double ax = -sx * (logicalWidth / 2.0);
            double ay = -sy * (logicalHeight / 2.0);
            anchor[0] = localToParent(ax, ay);
            e.consume();
        });

        handle.setOnMouseDragged(e -> {
            if (isLocked())
                return;

            Point2D parentMouse = visualizer.getContentGroup().sceneToLocal(e.getSceneX(), e.getSceneY());
            double dx = parentMouse.getX() - initialMouse[0];
            double dy = parentMouse.getY() - initialMouse[1];

            // Convert parent delta to local delta taking rotation into account
            double rad = Math.toRadians(rotateTransform.getAngle());
            // Standard projection for screen space (Y-down)
            double localDx = (dx * Math.cos(-rad) + dy * Math.sin(-rad)) * sx;
            double localDy = (-dx * Math.sin(-rad) + dy * Math.cos(-rad)) * sy;

            // Adjust by scale to avoid aggressive jumping
            double curSx = Math.abs(initialScale[0]);
            double curSy = Math.abs(initialScale[1]);
            localDx /= (curSx > 0.01 ? curSx : 1.0);
            localDy /= (curSy > 0.01 ? curSy : 1.0);

            double proposedW = initialDim[0] + localDx;
            double proposedH = initialDim[1] + localDy;

            boolean flipX = proposedW < 0;
            boolean flipY = proposedH < 0;

            double newW = Math.max(20, Math.abs(proposedW));
            double newH = Math.max(10, Math.abs(proposedH));

            double factorX = newW / Math.max(1, logicalWidth);
            double factorY = newH / Math.max(1, logicalHeight);

            logicalWidth = newW;
            logicalHeight = newH;

            setInternalScaleX(initialScale[0] * (flipX ? -1 : 1));
            setInternalScaleY(initialScale[1] * (flipY ? -1 : 1));

            if (trajectory != null) {
                trajectory.scalePoints(factorX, factorY);
            }

            renderText();

            // Re-align based on the opposite anchor to keep that point fixed in scene
            double ax = -sx * (logicalWidth / 2.0);
            double ay = -sy * (logicalHeight / 2.0);
            Point2D currentAnchor = localToParent(ax, ay);

            if (anchor[0] != null && currentAnchor != null) {
                setTranslateX(getTranslateX() + (anchor[0].getX() - currentAnchor.getX()));
                setTranslateY(getTranslateY() + (anchor[0].getY() - currentAnchor.getY()));
            }
            e.consume();
        });

        handle.setOnMouseReleased(e -> {
            recordTransformUndo(startMemento[0]);
            startMemento[0] = null;
            e.consume();
        });
    }

    public void renderText() {
        if (isUpdating)
            return;
        isUpdating = true;
        textGroup.getChildren().clear();
        if (textContent == null || textContent.isEmpty()) {
            isUpdating = false;
            return;
        }

        // Build immutable render context
        RenderContext ctx = new RenderContext(
                textContent, font, textColor, strokeColor, strokeWidth,
                isBold, isItalic, trajectory, logicalWidth, logicalHeight, spacing,
                textAlignment, fontSize, dropShadowColor, 0, 0, dropShadowEnabled ? 10 : 0, false, textColor,
                textColor);

        // Select optimal strategy based on trajectory type
        TextRenderStrategy strategy;
        if (trajectory.getType() == TrajectoryPath.Type.STRAIGHT && spacing == 0) {
            strategy = new org.example.component.helper.OptimizedStraightRenderStrategy();
        } else {
            strategy = new org.example.component.helper.PerCharRenderStrategy();
        }

        // Delegate rendering to the strategy
        strategy.render(this, textGroup, ctx);

        if (dropShadowEnabled) {
            textGroup.setEffect(new javafx.scene.effect.DropShadow(10, dropShadowColor));
        } else {
            textGroup.setEffect(null);
        }

        // --- FINAL FIT: Ensure the text group fits within logical bounds ---
        textGroup.setScaleX(1.0);
        textGroup.setScaleY(1.0);
        Bounds b = textGroup.getBoundsInLocal();
        if (b.getWidth() > 0 && b.getHeight() > 0) {
            double fitX = logicalWidth / b.getWidth();
            double fitY = logicalHeight / b.getHeight();
            // Only apply final stretch if it's straight text to avoid deforming curves
            if (trajectory.getType() == TrajectoryPath.Type.STRAIGHT) {
                textGroup.setScaleX(fitX);
                textGroup.setScaleY(fitY);
            }
            // Center the group visually
            textGroup.setTranslateX(-b.getCenterX() * textGroup.getScaleX());
            textGroup.setTranslateY(-b.getCenterY() * textGroup.getScaleY());
        }

        updateUI();
        if (isInitialized)
            notifyVisualChange();
        isUpdating = false;
    }

    private Group createCharStack(char c, double sx, double sy) {
        Group g = new Group();
        Text fill = new Text(String.valueOf(c));
        fill.setFont(font);
        fill.setFill(textColor);
        fill.setTextOrigin(javafx.geometry.VPos.CENTER); // Vertical centering
        fill.setBoundsType(javafx.scene.text.TextBoundsType.VISUAL);
        fill.getTransforms().add(new Scale(sx, sy, 0, 0));
        if (strokeWidth > 0) {
            Text outer = new Text(String.valueOf(c));
            outer.setFont(font);
            outer.setFill(null);
            outer.setStroke(strokeColor);
            outer.setStrokeType(StrokeType.OUTSIDE);
            outer.setStrokeWidth(strokeWidth);
            outer.setTextOrigin(javafx.geometry.VPos.CENTER); // Vertical centering
            outer.setBoundsType(javafx.scene.text.TextBoundsType.VISUAL);
            outer.getTransforms().add(new Scale(sx, sy, 0, 0));
            g.getChildren().add(outer);
        }
        g.getChildren().add(fill);
        return g;
    }

    private void updateUI() {
        double base_w = logicalWidth;
        double base_h = logicalHeight;
        double base_x = -base_w / 2.0;
        double base_y = -base_h / 2.0;

        border.setX(0); // Use translation for consistent alignment
        border.setY(0);
        border.setWidth(base_w);
        border.setHeight(base_h);

        double viewportScale = (visualizer != null && visualizer.getViewportController() != null)
                ? visualizer.getViewportController().getFinalScale()
                : 1.0;
        double sx = Math.abs(scaleTransform.getX()) * viewportScale;
        double sy = Math.abs(scaleTransform.getY()) * viewportScale;
        double maxScale = Math.max(sx, sy);
        border.setStrokeWidth((maxScale > 0) ? 1.0 / maxScale : 1.0);

        place(border, base_x, base_y);

        boolean showRotate = isSelected && isRotationMode && !isLocked;
        boolean showResize = isSelected && !isRotationMode && !isLocked;

        handlesGroup.getChildren().forEach(n -> n.setVisible(false));
        if (showResize) {
            nw.setVisible(true);
            ne.setVisible(true);
            sw.setVisible(true);
            se.setVisible(true);
            n.setVisible(true);
            s.setVisible(true);
            e.setVisible(true);
            w.setVisible(true);

            place(nw, base_x, base_y);
            place(ne, base_x + base_w, base_y);
            place(sw, base_x, base_y + base_h);
            place(se, base_x + base_w, base_y + base_h);
            place(n, 0, base_y);
            place(s, 0, base_y + base_h);
            place(e, base_x + base_w, 0);
            place(w, base_x, 0);
        } else if (showRotate) {
            rotTopLeft.setVisible(true);
            rotTopRight.setVisible(true);
            rotBottomLeft.setVisible(true);
            rotBottomRight.setVisible(true);
            shearTop.setVisible(true);
            shearBottom.setVisible(true);
            shearLeft.setVisible(true);
            shearRight.setVisible(true);
            pivotHandle.setVisible(true);

            place(rotTopLeft, base_x, base_y);
            place(rotTopRight, base_x + base_w, base_y);
            place(rotBottomLeft, base_x, base_y + base_h);
            place(rotBottomRight, base_x + base_w, base_y + base_h);
            place(shearTop, 0, base_y);
            place(shearBottom, 0, base_y + base_h);
            place(shearLeft, base_x, 0);
            place(shearRight, base_x + base_w, 0);
            positionNode(pivotHandle, rotateTransform.getPivotX(), rotateTransform.getPivotY());
        }

        if (!isRotationMode) {
            setPivotSmoothly(base_x + base_w / 2, base_y + base_h / 2);
        } else if (customPivotX != -1) {
            setPivotSmoothly(customPivotX, customPivotY);
        }

        // Preserve handles during drag to avoid event interruption and 'blind editing'
        if (!isDraggingTrajectoryHandle) {
            trajectoryEditingGroup.getChildren().clear();
            trajectoryEditingGroup.setVisible(isTrajectoryEditMode);
            if (isSelected && isTrajectoryEditMode)
                renderTrajectoryHandles();
        } else {
            // Update the guide line points in real-time during drag
            renderCurveGuide();
        }
    }

    private void renderCurveGuide() {
        // Find existing polyline and update it
        for (Node n : trajectoryEditingGroup.getChildren()) {
            if (n instanceof Polyline) {
                Polyline line = (Polyline) n;
                line.getPoints().clear();
                int samples = 30;
                for (int i = 0; i <= samples; i++) {
                    double t = (double) i / samples;
                    Point2D p = trajectory.getPointAt(t);
                    line.getPoints().addAll(p.getX(), p.getY());
                }
            }
        }
    }

    private void renderTrajectoryHandles() {
        List<Point2D> pts = trajectory.getControlPoints();

        // Add a smooth guide line connecting the path the text actually follows
        if (pts.size() > 1) {
            javafx.scene.shape.Polyline guideLine = new javafx.scene.shape.Polyline();
            // Sample the actual curve at 30 points for high fidelity
            int samples = 30;
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / samples;
                Point2D p = trajectory.getPointAt(t);
                guideLine.getPoints().addAll(p.getX(), p.getY());
            }
            guideLine.setStroke(Color.web("#0ea5e9")); // Brighter blue
            guideLine.setStrokeWidth(1.2);
            guideLine.getStrokeDashArray().addAll(6.0, 4.0);
            guideLine.setOpacity(0.8);
            guideLine.setMouseTransparent(true);
            trajectoryEditingGroup.getChildren().add(guideLine);
        }

        for (int i = 0; i < pts.size(); i++) {
            final int idx = i;
            Point2D p = pts.get(i);
            // Slightly larger handle for better hit detection
            Circle h = new Circle(p.getX(), p.getY(), 8, Color.WHITE);
            h.setStroke(Color.web("#3498db"));
            h.setStrokeWidth(2);
            h.getStyleClass().add("trajectory-handle");
            h.setPickOnBounds(true);
            h.setCursor(Cursor.HAND);

            // Hover effects
            h.setOnMouseEntered(e -> h.setStroke(Color.ORANGE));
            h.setOnMouseExited(e -> h.setStroke(Color.web("#3498db")));

            h.setOnMousePressed(e -> {
                isDraggingTrajectoryHandle = true;
                trajectorySnapshot = trajectory.copy();
                e.consume();
            });
            h.setOnMouseReleased(e -> {
                isDraggingTrajectoryHandle = false;
                if (trajectorySnapshot != null && visualizer.getHistoryManager() != null) {
                    visualizer.getHistoryManager().addCommand(
                            new org.example.pattern.TrajectoryCommand(this, trajectorySnapshot, trajectory));
                }
                updateUI(); // Final refresh
                e.consume();
            });
            h.setOnMouseDragged(e -> {
                if (isLocked())
                    return;
                Point2D local = sceneToLocal(e.getSceneX(), e.getSceneY());
                trajectory.getControlPoints().set(idx, local);

                // Update circle position immediately for smooth movement
                h.setCenterX(local.getX());
                h.setCenterY(local.getY());

                renderText(); // This will trigger updateTrajectoryGuideOnly
                e.consume();
            });
            trajectoryEditingGroup.getChildren().add(h);
        }
    }

    private void setupRotationHandler() {
        final double[] startAngle = new double[1];
        final double[] initialAngle = new double[1];
        final NodeMemento[] startMemento = new NodeMemento[1];
        for (Node h : new Node[] { rotTopLeft, rotTopRight, rotBottomLeft, rotBottomRight }) {
            h.setOnMousePressed(e -> {
                if (isLocked())
                    return;
                startMemento[0] = new NodeMemento(this);
                // Get pivot in scene coordinates to measure angle correctly
                Point2D pivotScene = localToScene(rotateTransform.getPivotX(), rotateTransform.getPivotY());
                startAngle[0] = Math
                        .toDegrees(Math.atan2(e.getSceneY() - pivotScene.getY(), e.getSceneX() - pivotScene.getX()));
                initialAngle[0] = rotateTransform.getAngle();
                e.consume();
            });
            h.setOnMouseDragged(e -> {
                Point2D pivotScene = localToScene(rotateTransform.getPivotX(), rotateTransform.getPivotY());
                double curr = Math
                        .toDegrees(Math.atan2(e.getSceneY() - pivotScene.getY(), e.getSceneX() - pivotScene.getX()));
                double diff = curr - startAngle[0];
                double newAngle = initialAngle[0] + diff;
                rotateTransform.setAngle(newAngle);
                if (visualizer != null && visualizer.getShapeManagerController() != null) {
                    visualizer.getShapeManagerController().updateAngleUI(newAngle);
                }
                e.consume();
            });
            h.setOnMouseReleased(e -> {
                recordTransformUndo(startMemento[0]);
                startMemento[0] = null;
                e.consume();
            });
        }
    }

    private void setupShearHandlers() {
        final double[] start = new double[2];
        final double[] initS = new double[2];
        final NodeMemento[] startMemento = new NodeMemento[1];
        for (Node h : new Node[] { shearTop, shearBottom }) {
            h.setOnMousePressed(e -> {
                if (isLocked())
                    return;
                startMemento[0] = new NodeMemento(this);
                start[0] = e.getSceneX();
                initS[0] = shearTransform.getX();
                e.consume();
            });
            h.setOnMouseDragged(e -> {
                if (isLocked())
                    return;
                shearTransform.setX(initS[0] + (e.getSceneX() - start[0]) / 100.0);
                e.consume();
            });
            h.setOnMouseReleased(e -> {
                recordTransformUndo(startMemento[0]);
                startMemento[0] = null;
                e.consume();
            });
        }
        for (Node h : new Node[] { shearLeft, shearRight }) {
            h.setOnMousePressed(e -> {
                if (isLocked())
                    return;
                startMemento[0] = new NodeMemento(this);
                start[1] = e.getSceneY();
                initS[1] = shearTransform.getY();
                e.consume();
            });
            h.setOnMouseDragged(e -> {
                if (isLocked())
                    return;
                shearTransform.setY(initS[1] + (e.getSceneY() - start[1]) / 100.0);
                e.consume();
            });
            h.setOnMouseReleased(e -> {
                recordTransformUndo(startMemento[0]);
                startMemento[0] = null;
                e.consume();
            });
        }
    }

    private void setupPivotHandler() {
        final NodeMemento[] startMemento = new NodeMemento[1];
        pivotHandle.setOnMousePressed(e -> {
            if (isLocked())
                return;
            startMemento[0] = new NodeMemento(this);
            e.consume();
        });
        pivotHandle.setOnMouseDragged(e -> {
            if (isLocked())
                return;
            Point2D local = sceneToLocal(e.getSceneX(), e.getSceneY());
            customPivotX = local.getX();
            customPivotY = local.getY();
            setPivotSmoothly(customPivotX, customPivotY);
            updateUI();
            e.consume();
        });
        pivotHandle.setOnMouseReleased(e -> {
            recordTransformUndo(startMemento[0]);
            startMemento[0] = null;
            e.consume();
        });
    }

    private void recordTransformUndo(NodeMemento before) {
        if (before == null || visualizer == null || visualizer.getHistoryManager() == null)
            return;

        NodeMemento after = new NodeMemento(this);
        if (!before.isEquivalentTo(after)) {
            visualizer.getHistoryManager().addCommand(new TransformCommand(this, before, after, activeZone));
        }
    }

    /**
     * Updates the pivot of all transforms without moving the node visually.
     * This prevents the "jumping" effect when entering/exiting rotation mode.
     */
    private void setPivotSmoothly(double nx, double ny) {
        if (nx == rotateTransform.getPivotX() && ny == rotateTransform.getPivotY())
            return;

        // Capture current visual position of local (0,0) in parent coordinates
        Point2D oldPos = localToParent(0, 0);

        // Update pivots for all active transforms
        rotateTransform.setPivotX(nx);
        rotateTransform.setPivotY(ny);
        scaleTransform.setPivotX(nx);
        scaleTransform.setPivotY(ny);
        shearTransform.setPivotX(nx);
        shearTransform.setPivotY(ny);

        // Capture new visual position of local (0,0) after pivot change
        Point2D newPos = localToParent(0, 0);

        // Compensate translation to keep the node exactly where it was
        setTranslateX(getTranslateX() + (oldPos.getX() - newPos.getX()));
        setTranslateY(getTranslateY() + (oldPos.getY() - newPos.getY()));
    }

    private void place(javafx.scene.Node n, double x, double y) {
        // UIFactory handles are 24x24 StackPanes. To center them on (x,y), we offset by
        // 6.0
        double offset = (n instanceof javafx.scene.layout.StackPane) ? 6.0 : 0.0;

        double viewportScale = (visualizer != null && visualizer.getViewportController() != null)
                ? visualizer.getViewportController().getFinalScale()
                : 1.0;
        double sx = Math.abs(scaleTransform.getX()) * viewportScale;
        double sy = Math.abs(scaleTransform.getY()) * viewportScale;

        // Anti-scale handles
        if (n != border && sx > 0 && sy > 0) {
            n.setScaleX(1.0 / sx);
            n.setScaleY(1.0 / sy);
            // Adjust offset for scale to keep them perfectly centered
            offset = offset / sx;
        }

        n.setTranslateX(x - offset);
        n.setTranslateY(y - offset);
    }

    private void positionNode(Node n, double x, double y) {
        double offset = (n instanceof javafx.scene.layout.StackPane) ? 6.0 : 0.0;

        double viewportScale = (visualizer != null && visualizer.getViewportController() != null)
                ? visualizer.getViewportController().getFinalScale()
                : 1.0;
        double sx = Math.abs(scaleTransform.getX()) * viewportScale;
        double sy = Math.abs(scaleTransform.getY()) * viewportScale;

        if (n != border && sx > 0 && sy > 0) {
            n.setScaleX(1.0 / sx);
            n.setScaleY(1.0 / sy);
            offset = offset / sx;
        }

        n.setTranslateX(x - offset);
        n.setTranslateY(y - offset);
    }

    private boolean isHandle(Object t) {
        if (!(t instanceof Node))
            return false;
        Node n = (Node) t;
        return n.getStyleClass().contains("handle") || n.getStyleClass().contains("trajectory-handle")
                || n.getParent() instanceof StackPane;
    }

    // --- API & Persistence ---
    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String t) {
        String old = this.textContent;
        this.textContent = t;
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null && !old.equals(t)) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Texto", old, t, val -> this.textContent = val, this::renderText));
        }
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font f) {
        Font old = this.font;
        this.font = Font.font(
                f.getFamily(),
                isBold ? FontWeight.BOLD : FontWeight.NORMAL,
                isItalic ? FontPosture.ITALIC : FontPosture.REGULAR,
                f.getSize());
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null && !old.equals(this.font)) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Fuente", old, this.font, val -> {
                        this.font = val;
                        this.renderText();
                    }, null));
        }
    }

    public void setBold(boolean bold) {
        if (this.isBold == bold)
            return;
        Font oldFont = this.font;
        this.isBold = bold;
        updateStyledFont();
        if (visualizer != null && visualizer.getHistoryManager() != null && oldFont != this.font) {
            Font finalOldFont = oldFont;
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Negrita", finalOldFont, this.font, val -> {
                        this.font = val;
                        this.isBold = (val.getStyle().contains("bold"));
                        this.renderText();
                    }, null));
        }
    }

    public void setItalic(boolean italic) {
        if (this.isItalic == italic)
            return;
        Font oldFont = this.font;
        this.isItalic = italic;
        updateStyledFont();
        if (visualizer != null && visualizer.getHistoryManager() != null && oldFont != this.font) {
            Font finalOldFont = oldFont;
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Cursiva", finalOldFont, this.font, val -> {
                        this.font = val;
                        this.isItalic = (val.getStyle().contains("italic"));
                        this.renderText();
                    }, null));
        }
    }

    private void updateStyledFont() {
        if (this.font != null) {
            this.font = Font.font(
                    this.font.getFamily(),
                    isBold ? FontWeight.BOLD : FontWeight.NORMAL,
                    isItalic ? FontPosture.ITALIC : FontPosture.REGULAR,
                    this.font.getSize());
            renderText();
        }
    }

    public boolean isBold() {
        return isBold;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color c) {
        Color old = this.textColor;
        this.textColor = c;
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null && !old.equals(c)) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Color de Texto", old, c, val -> {
                        this.textColor = val;
                        this.renderText();
                    }, null));
        }
    }

    public double getSpacing() {
        return spacing;
    }

    public void setSpacing(double s) {
        if (this.spacing == s)
            return;
        double old = this.spacing;
        this.spacing = s;
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Espaciado", old, s, val -> {
                        this.spacing = val;
                        this.renderText();
                    }, null));
        }
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color c) {
        Color old = this.strokeColor;
        this.strokeColor = c;
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null && !old.equals(c)) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Color de Contorno", old, c, val -> {
                        this.strokeColor = val;
                        this.renderText();
                    }, null));
        }
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(double w) {
        this.strokeWidth = w;
        renderText();
    }

    public javafx.scene.text.TextAlignment getTextAlignment() {
        return textAlignment;
    }

    public void setTextAlignment(javafx.scene.text.TextAlignment ta) {
        if (this.textAlignment == ta)
            return;
        javafx.scene.text.TextAlignment old = this.textAlignment;
        this.textAlignment = ta;
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Alineación", old, ta, val -> {
                        this.textAlignment = val;
                        this.renderText();
                    }, null));
        }
    }

    public boolean isDropShadowEnabled() {
        return dropShadowEnabled;
    }

    public void setDropShadowEnabled(boolean enabled) {
        if (this.dropShadowEnabled == enabled)
            return;
        boolean old = this.dropShadowEnabled;
        this.dropShadowEnabled = enabled;
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Sombra", old, enabled, val -> {
                        this.dropShadowEnabled = val;
                        this.renderText();
                    }, null));
        }
    }

    public Color getDropShadowColor() {
        return dropShadowColor;
    }

    public void setDropShadowColor(Color c) {
        Color old = this.dropShadowColor;
        this.dropShadowColor = c;
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null && !old.equals(c)) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Color de Sombra", old, c, val -> {
                        this.dropShadowColor = val;
                        this.renderText();
                    }, null));
        }
    }

    public void setFontSizeScale(double scaleFactor) {
        if (this.logicalWidth == 300.0 * (scaleFactor / 100.0) && this.logicalHeight == 100.0 * (scaleFactor / 100.0))
            return;
        double oldW = this.logicalWidth;
        double oldH = this.logicalHeight;
        this.logicalWidth = 300.0 * (scaleFactor / 100.0);
        this.logicalHeight = 100.0 * (scaleFactor / 100.0);
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            // Use a composite-like approach: restore both w and h together
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Tamaño de Texto", oldW, this.logicalWidth, val -> {
                        this.logicalWidth = val;
                        this.logicalHeight = oldH * (val / oldW);
                        this.renderText();
                    }, null));
        }
    }

    public void setShape(TextShape s) {
        switch (s) {
            case STRAIGHT:
                setTrajectoryType(TrajectoryPath.Type.STRAIGHT);
                break;
            case ARC_TOP:
            case ARC_BOTTOM:
                setTrajectoryType(TrajectoryPath.Type.ARC);
                break;
            case WAVE:
                setTrajectoryType(TrajectoryPath.Type.WAVE);
                break;
            default:
                setTrajectoryType(TrajectoryPath.Type.BEZIER);
                break;
        }
    }

    public TextShape getShape() {
        switch (trajectory.getType()) {
            case STRAIGHT:
                return TextShape.STRAIGHT;
            case ARC:
                return TextShape.ARC_TOP;
            case WAVE:
                return TextShape.WAVE;
            case CIRCLE:
                return TextShape.CIRCULAR;
            default:
                return TextShape.BEZIER;
        }
    }

    public double getArcFactor() {
        return trajectory.getCurvature();
    }

    public void setArcFactor(double f) {
        trajectory.setCurvature(f);
        renderText();
    }

    public TrajectoryPath getTrajectory() {
        return trajectory;
    }

    public void setTrajectoryType(TrajectoryPath.Type t) {
        TrajectoryPath before = trajectory.copy();
        trajectory.setType(t);
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(
                    new org.example.pattern.TrajectoryCommand(this, before, trajectory));
        }
        renderText();
    }

    public double getLogicalWidth() {
        return logicalWidth;
    }

    public double getLogicalHeight() {
        return logicalHeight;
    }

    public void setTextSize(double w, double h) {
        double oldW = this.logicalWidth;
        double oldH = this.logicalHeight;
        if (oldW == w && oldH == h)
            return;
        this.logicalWidth = w;
        this.logicalHeight = h;
        renderText();
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            visualizer.getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Redimensionar Texto", oldW, w, val -> {
                        this.logicalWidth = val;
                        this.logicalHeight = oldH * (val / oldW);
                        this.renderText();
                    }, null));
        }
    }

    public void setTextSize(Double w, Double h) {
        if (w != null && h != null)
            setTextSize(w.doubleValue(), h.doubleValue());
    }

    public void setTextSizeSilently(double w, double h) {
        this.logicalWidth = w;
        this.logicalHeight = h;
        renderText();
    }

    /**
     * Recursively finds a Group ancestor starting from a Parent node.
     * Used to locate the visualizer's content group for inline editing.
     * 
     * @param node the node to start searching from
     * @return the nearest Group ancestor, or null if none found
     */
    private javafx.scene.Group findGroupParent(javafx.scene.Parent node) {
        while (node != null) {
            if (node instanceof javafx.scene.Group) {
                return (javafx.scene.Group) node;
            }
            if (node.getParent() != null) {
                node = node.getParent();
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * Starts inline editing of this text layer.
     * Creates an InlineTextEditor positioned over the layer.
     */
    public void startInlineEdit() {
        if (isLocked() || isBeingEdited)
            return;

        // Find the parent Group where we can inject the inline editor
        // Search up the hierarchy in case this TextLayer is nested inside another group
        javafx.scene.Group parent = findGroupParent(this);
        if (parent == null)
            return;

        isBeingEdited = true;
        inlineEditor = new InlineTextEditor();
        inlineEditor.start(this, parent,
                newText -> {
                    // Commit: update the text and mark as not editing
                    if (newText != null && !newText.equals(textContent)) {
                        setTextContent(newText);
                    }
                    isBeingEdited = false;
                    inlineEditor = null;
                },
                () -> {
                    // Cancel: just mark as not editing
                    isBeingEdited = false;
                    inlineEditor = null;
                });
    }

    public boolean isBeingEdited() {
        return isBeingEdited;
    }

    public void setBeingEdited(boolean beingEdited) {
        this.isBeingEdited = beingEdited;
    }

    public void setTrajectoryEditMode(boolean en) {
        this.isTrajectoryEditMode = en;
        updateUI();
    }

    public boolean getTrajectoryEditMode() {
        return isTrajectoryEditMode;
    }

    public double getInternalRotation() {
        return rotateTransform.getAngle();
    }

    public void setInternalRotation(double r) {
        rotateTransform.setAngle(r);
    }

    public double getInternalScaleX() {
        return scaleTransform.getX();
    }

    public void setInternalScaleX(double s) {
        if ((scaleTransform.getX() > 0 && s < 0) || (scaleTransform.getX() < 0 && s > 0)) {
            shearTransform.setY(shearTransform.getY() * -1);
        }
        scaleTransform.setX(s);
    }

    public double getInternalScaleY() {
        return scaleTransform.getY();
    }

    public void setInternalScaleY(double s) {
        if ((scaleTransform.getY() > 0 && s < 0) || (scaleTransform.getY() < 0 && s > 0)) {
            shearTransform.setX(shearTransform.getX() * -1);
        }
        scaleTransform.setY(s);
    }

    public double getShearX() {
        return shearTransform.getX();
    }

    public void setShearX(double x) {
        shearTransform.setX(x);
    }

    public double getShearY() {
        return shearTransform.getY();
    }

    public void setShearY(double y) {
        shearTransform.setY(y);
    }

    public double getCustomPivotX() {
        return rotateTransform.getPivotX();
    }

    public double getCustomPivotY() {
        return rotateTransform.getPivotY();
    }

    public void setCustomPivotX(double x) {
        rotateTransform.setPivotX(x);
        scaleTransform.setPivotX(x);
        shearTransform.setPivotX(x);
        this.customPivotX = x;
    }

    public void setCustomPivotY(double y) {
        rotateTransform.setPivotY(y);
        scaleTransform.setPivotY(y);
        shearTransform.setPivotY(y);
        this.customPivotY = y;
    }

    public void addRotation(double angle) {
        rotateTransform.setAngle(rotateTransform.getAngle() + angle);
    }

    public Point2D getStableCenter() {
        Bounds b = textGroup.getBoundsInLocal();
        return new Point2D((b.getMinX() + b.getMaxX()) / 2.0, (b.getMinY() + b.getMaxY()) / 2.0);
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
        logicalWidth *= rx;
        logicalHeight *= ry;
        renderText();
    }

    public void scale(double factor) {
        multiplyScale(factor, factor);
    }

    public void scale(Double factor) {
        if (factor != null)
            scale(factor.doubleValue());
    }

    public void applyContour(int steps, double distance, Color color) {
        this.contourSteps = steps;
        this.contourDistance = distance;
        this.contourColor = color;
        renderText();
    }

    // --- Clipboard & Extra Interactions ---
    public static void clearClipboard() {
        clipboardLayer = null;
    }

    public static boolean hasClipboard() {
        return clipboardLayer != null;
    }

    public static TextLayer getClipboardCopy() {
        return (clipboardLayer != null) ? clipboardLayer.createClone() : null;
    }

    public void copyToClipboard() {
        clipboardLayer = this.createClone();
    }

    public void cutToClipboard() {
        copyToClipboard();
        if (visualizer != null)
            visualizer.getLayerManager().removeLayer(this);
    }

    public void flipHorizontal() {
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
            rotateTransform.setAngle(-rotateTransform.getAngle());
            shearTransform.setX(-shearTransform.getX());
            setInternalScaleX(scaleTransform.getX() * -1);
            visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before,
                    new org.example.pattern.NodeMemento(this), activeZone));
        } else {
            rotateTransform.setAngle(-rotateTransform.getAngle());
            shearTransform.setX(-shearTransform.getX());
            setInternalScaleX(scaleTransform.getX() * -1);
        }
    }

    public void flipVertical() {
        if (visualizer != null && visualizer.getHistoryManager() != null) {
            org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
            rotateTransform.setAngle(-rotateTransform.getAngle());
            shearTransform.setY(-shearTransform.getY());
            setInternalScaleY(scaleTransform.getY() * -1);
            visualizer.getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before,
                    new org.example.pattern.NodeMemento(this), activeZone));
        } else {
            rotateTransform.setAngle(-rotateTransform.getAngle());
            shearTransform.setY(-shearTransform.getY());
            setInternalScaleY(scaleTransform.getY() * -1);
        }
    }

    // Compatibility for StateMapper
    public double getWidthScale() {
        return 1.0;
    }

    public double getHeightScale() {
        return 1.0;
    }

    public void setWidthScale(double s) {
    }

    public void setHeightScale(double s) {
    }

    // Contour
    public int getContourSteps() {
        return contourSteps;
    }

    public void setContourSteps(int s) {
        this.contourSteps = s;
        renderText();
    }

    public double getContourDistance() {
        return contourDistance;
    }

    public void setContourDistance(double d) {
        this.contourDistance = d;
        renderText();
    }

    public Color getContourColor() {
        return contourColor;
    }

    public void setContourColor(Color c) {
        this.contourColor = c;
        renderText();
    }

    public void setOnSelectionRequested(Consumer<MouseEvent> h) {
        this.onSelectionRequested = h;
    }

    public void setOnDragHandler(BiConsumer<Double, Double> h) {
        this.onDragHandler = h;
    }

    public void setOnResizeHandler(BiConsumer<Double, Double> h) {
        this.onResizeHandler = h;
    }

    public void setEditHandler(Consumer<TextLayer> h) {
        this.editHandler = h;
    }

    public void setDeleteHandler(Runnable h) {
        this.deleteHandler = h;
    }

    public void setPowerClipHandler(Consumer<String> h) {
        this.powerClipHandler = h;
    }

    public void setAvailableZonesSupplier(Supplier<List<String>> s) {
        this.availableZonesSupplier = s;
    }

    public void setPasteHandler(Runnable h) {
        this.pasteHandler = h;
    }

    public void addOnVisualChangeListener(Runnable r) {
        visualChangeListeners.add(r);
    }

    private void notifyVisualChange() {
        visualChangeListeners.forEach(r -> {
            try {
                r.run();
            } catch (Exception e) {
            }
        });
    }

    public TextLayer createClone() {
        TextLayer clone = new TextLayer(textContent, font, textColor);
        clone.setTranslateX(getTranslateX());
        clone.setTranslateY(getTranslateY());
        clone.setTrajectoryType(trajectory.getType());
        clone.setTextSize(logicalWidth, logicalHeight);
        clone.setInternalRotation(getInternalRotation());
        clone.setCustomPivotX(getCustomPivotX());
        clone.setCustomPivotY(getCustomPivotY());
        clone.setInternalScaleX(getInternalScaleX());
        clone.setInternalScaleY(getInternalScaleY());
        clone.setActiveZone(activeZone);
        return clone;
    }

    // --- GraphicLayer Implementation ---
    @Override
    public void setSelected(boolean sel) {
        this.isSelected = sel;
        border.setVisible(sel);
        handlesGroup.setVisible(sel);
        updateUI();
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public void setLocked(boolean l) {
        setUserLocked(l);
    }

    @Override
    public void setUserLocked(boolean l) {
        this.isUserLocked = l;
        updateUI();
    }

    @Override
    public void setSystemLocked(boolean l) {
        this.isLocked = l;
        updateUI();
    }

    @Override
    public boolean isLocked() {
        return isLocked || isUserLocked;
    }

    @Override
    public boolean isUserLocked() {
        return isUserLocked;
    }

    @Override
    public void setVisualizer(PrendaVisualizer v) {
        this.visualizer = v;
    }

    @Override
    public PrendaVisualizer getVisualizer() {
        return visualizer;
    }

    @Override
    public String getActiveZone() {
        return activeZone;
    }

    @Override
    public void setActiveZone(String z) {
        this.activeZone = z;
        setSystemLocked(z != null);
    }

    @Override
    public void updateVisuals() {
        updateUI();
    }

    @Override
    public void render() {
        renderText();
    }

    @Override public double getInternalShearX() { return shearTransform.getX(); }
    @Override public void setInternalShearX(double s) { shearTransform.setX(s); }
    @Override public double getInternalShearY() { return shearTransform.getY(); }
    @Override public void setInternalShearY(double s) { shearTransform.setY(s); }

    @Override
    public void recordUndoState() {
        /* Future memento integration */ }

    @Override
    public void setRotationMode(boolean active) {
        this.isRotationMode = active;
        updateUI();
    }

    @Override
    public boolean isRotationMode() {
        return isRotationMode;
    }

    @Override
    public Node getNode() {
        return this;
    }
}
