package org.example.component;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
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
import javafx.scene.transform.Transform;
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
public class TextLayer extends AbstractGraphicLayer {

    private final Group textGroup = new Group();
    private final Group strokeGroup = new Group();

    public Group getTextGroup() {
        return textGroup;
    }

    
    
    private final Group trajectoryEditingGroup = new Group();
    private InlineTextEditor inlineEditor;

    // Interactive Handles
    
    
    
    
    

    // Transforms
    
    
    

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
    
    
    
    
    
    private boolean isTrajectoryEditMode = false;
    private boolean verticalLayout = false;
    private boolean isUpdating = false;
    private boolean isBeingEdited = false;
    private boolean isInitialized = false;
    private boolean isDraggingTrajectoryHandle = false;
    

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
    

    // Tooltip for hover
    private final Tooltip hoverTooltip = new Tooltip();

    
    @Override
    public Bounds calculateBounds() {
        return new javafx.geometry.BoundingBox(-logicalWidth / 2.0, -logicalHeight / 2.0, logicalWidth, logicalHeight);
    }

    private Group cloneGroup(Group source) {
        Group clone = new Group();
        clone.getTransforms().addAll(source.getTransforms());
        clone.setTranslateX(source.getTranslateX());
        clone.setTranslateY(source.getTranslateY());
        clone.setLayoutX(source.getLayoutX());
        clone.setLayoutY(source.getLayoutY());

        for (Node child : source.getChildren()) {
            if (child instanceof Text) {
                Text t = (Text) child;
                Text cloneText = new Text(t.getText());
                cloneText.setFont(t.getFont());
                cloneText.setX(t.getX());
                cloneText.setY(t.getY());
                cloneText.setTextOrigin(t.getTextOrigin());
                cloneText.setBoundsType(t.getBoundsType());
                cloneText.setTranslateX(t.getTranslateX());
                cloneText.setTranslateY(t.getTranslateY());
                cloneText.setRotate(t.getRotate());
                cloneText.getTransforms().addAll(t.getTransforms());
                clone.getChildren().add(cloneText);
            } else if (child instanceof Group) {
                clone.getChildren().add(cloneGroup((Group) child));
            }
        }
        return clone;
    }

    @Override
    protected javafx.scene.Node createSilhouetteNode() {
        if (textGroup != null) {
            return cloneGroup(textGroup);
        }
        return new javafx.scene.shape.Rectangle(-logicalWidth / 2.0, -logicalHeight / 2.0, logicalWidth, logicalHeight);
    }
    
    @Override
    public void multiplyShear(double sx, double sy) {
        setInternalShearX(getInternalShearX() + sx);
        setInternalShearY(getInternalShearY() + sy);
    }

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

        // Setup Transforms
        

        
        

        // Assembly
        

         
        

        
        trajectoryEditingGroup.setPickOnBounds(false);
        contentGroup.setPickOnBounds(false);

        contentGroup.getChildren().addAll(strokeGroup, textGroup, trajectoryEditingGroup);

        // Ensure Group picking is reliable
        this.setPickOnBounds(false);
        textGroup.setPickOnBounds(false);
        border.setMouseTransparent(true); // Don't block character selection

        // Initialize hover tooltip
        updateHoverTooltip();
        this.setOnMouseEntered(e -> {
            if (!isSelected()) {
                updateHoverTooltip();
            }
        });

        initInteraction();
        renderText();
        this.isInitialized = true;
    }

    private void updateHoverTooltip() {
        String zoneInfo = (getActiveZone() != null && !getActiveZone().isEmpty()) ? "\nZona: " + getActiveZone() : "";
        String display = (textContent != null && !textContent.isEmpty()) ? textContent : "(Vacío)";
        hoverTooltip.setText("Texto: \"" + display + "\"" + zoneInfo);
        Tooltip.install(this, hoverTooltip);
    }

    private void initInteraction() {
        this.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                if (!isLocked()) {
                    startInlineEdit();
                    e.consume();
                }
            }
        });
    }




    public void renderText() {
        if (isUpdating)
            return;
        isUpdating = true;
        textGroup.getChildren().clear();
        strokeGroup.getChildren().clear();
        
        // Reset transforms that may have been applied by other layout modes
        textGroup.setScaleX(1.0);
        textGroup.setScaleY(1.0);
        textGroup.setTranslateX(0);
        textGroup.setTranslateY(0);
        strokeGroup.setScaleX(1.0);
        strokeGroup.setScaleY(1.0);
        strokeGroup.setTranslateX(0);
        strokeGroup.setTranslateY(0);
        if (textContent == null || textContent.isEmpty()) {
            isUpdating = false;
            return;
        }

        // Build immutable render context - PASS TRANSPARENT STROKE to the strategy
        // so that the text nodes themselves are rendered clean and fill-only,
        // preventing double/distorted borders on screen.
        RenderContext ctx = new RenderContext(
                textContent, font, textColor, Color.TRANSPARENT, 0,
                isBold, isItalic, trajectory, logicalWidth, logicalHeight, spacing,
                textAlignment, fontSize, dropShadowColor, 0, 0, dropShadowEnabled ? 10 : 0, false, textColor,
                textColor);

        // Vertical layout: characters top-to-bottom
        if (verticalLayout) {
            char[] chars = textContent.toCharArray();
            Font useFont = font;
            double totalTextHeight = 0;
            double maxCharWidth = 0;
            double[] charHeights = new double[chars.length];
            double[] charWidths = new double[chars.length];
            for (int i = 0; i < chars.length; i++) {
                Text t = new Text(String.valueOf(chars[i]));
                t.setFont(useFont);
                Bounds tb = t.getLayoutBounds();
                charWidths[i] = tb.getWidth();
                charHeights[i] = tb.getHeight();
                totalTextHeight += charHeights[i];
                if (charWidths[i] > maxCharWidth) maxCharWidth = charWidths[i];
            }
            double scaleY = logicalHeight / Math.max(1, totalTextHeight + (chars.length - 1) * spacing);
            double scaleX = logicalWidth / Math.max(1, maxCharWidth);
            double startY = -(totalTextHeight * scaleY + (chars.length - 1) * spacing) / 2.0;
            for (int i = 0; i < chars.length; i++) {
                Text txt = new Text(String.valueOf(chars[i]));
                txt.setFont(useFont);
                txt.setFill(textColor);
                txt.setTextOrigin(VPos.CENTER);
                txt.getTransforms().add(new Scale(scaleX, scaleY, 0, 0));
                txt.setTranslateY(startY + charHeights[i] * scaleY / 2.0);
                txt.setTranslateX(- (charWidths[i] * scaleX) / 2.0);
                textGroup.getChildren().add(txt);
                startY += charHeights[i] * scaleY + spacing;
            }
            
            if (strokeWidth > 0 && strokeColor != null && !strokeColor.equals(Color.TRANSPARENT)) {
                for (Node child : textGroup.getChildren()) {
                    if (child instanceof Text textNode) {
                        Path fillPath = getPristinePath(textNode);
                        if (fillPath != null) {
                            Transform localToParent = textNode.getLocalToParentTransform();
                            Path strokePath = org.example.component.helper.VectorBooleanHelper.transformPath(fillPath, localToParent);
                            strokePath.setFill(Color.TRANSPARENT);
                            strokePath.setStroke(strokeColor);
                            strokePath.setStrokeWidth(strokeWidth);
                            strokePath.setStrokeType(StrokeType.OUTSIDE);
                            strokePath.setStrokeLineJoin(StrokeLineJoin.ROUND);
                            strokePath.setStrokeLineCap(StrokeLineCap.ROUND);
                            strokeGroup.getChildren().add(strokePath);
                        }
                    }
                }
            }
            invalidateBounds();
            if (isInitialized) notifyVisualChange();
            isUpdating = false;
            return;
        }

        // Select optimal strategy based on trajectory type
        TextRenderStrategy strategy;
        if (trajectory.getType() == TrajectoryPath.Type.STRAIGHT && spacing == 0) {
            strategy = new org.example.component.helper.OptimizedStraightRenderStrategy();
        } else {
            strategy = new org.example.component.helper.PerCharRenderStrategy();
        }

        // Delegate rendering to the strategy
        strategy.render(this, textGroup, ctx);

        // --- FINAL FIT: Ensure the text group fits within logical bounds ---
        textGroup.setScaleX(1.0);
        textGroup.setScaleY(1.0);
        textGroup.setTranslateX(0);
        textGroup.setTranslateY(0);
        
        double fitX = 1.0;
        double fitY = 1.0;
        Bounds b = textGroup.getBoundsInLocal();
        if (b.getWidth() > 0 && b.getHeight() > 0) {
            fitX = logicalWidth / b.getWidth();
            fitY = logicalHeight / b.getHeight();
            // Only apply final stretch if it's straight text to avoid deforming curves
            if (trajectory.getType() == TrajectoryPath.Type.STRAIGHT) {
                textGroup.setScaleX(fitX);
                textGroup.setScaleY(fitY);
            }
            Bounds parentBounds = textGroup.getBoundsInParent();
            // Center the group visually based on its bounds in parent coordinate space
            textGroup.setTranslateX(-parentBounds.getCenterX());
            textGroup.setTranslateY(-parentBounds.getCenterY());
        }

        // Draw the stroke in strokeGroup behind textGroup (ignoring parent scale distortion)
        if (strokeWidth > 0 && strokeColor != null && strokeColor != Color.TRANSPARENT) {
            Scale scale = new Scale(
                trajectory.getType() == TrajectoryPath.Type.STRAIGHT ? fitX : 1.0,
                trajectory.getType() == TrajectoryPath.Type.STRAIGHT ? fitY : 1.0,
                b.getCenterX(),
                b.getCenterY()
            );
            
            for (Node child : textGroup.getChildren()) {
                if (child instanceof Text textNode) {
                    Path fillPath = getPristinePath(textNode);
                    if (fillPath != null) {
                        Transform localToParent = textNode.getLocalToParentTransform();
                        Transform combined = scale.createConcatenation(localToParent);
                        
                        Path strokePath = org.example.component.helper.VectorBooleanHelper.transformPath(fillPath, combined);
                        strokePath.setFill(Color.TRANSPARENT);
                        strokePath.setStroke(strokeColor);
                        strokePath.setStrokeWidth(strokeWidth);
                        strokePath.setStrokeType(StrokeType.OUTSIDE);
                        strokePath.setStrokeLineJoin(StrokeLineJoin.ROUND);
                        strokePath.setStrokeLineCap(StrokeLineCap.ROUND);
                        
                        strokeGroup.getChildren().add(strokePath);
                    }
                } else if (child instanceof Group subGrp) {
                    for (Node subChild : subGrp.getChildren()) {
                        if (subChild instanceof Text textNode) {
                            Path fillPath = getPristinePath(textNode);
                            if (fillPath != null) {
                                Transform subGrpTransform = subGrp.getLocalToParentTransform();
                                Transform textTransform = textNode.getLocalToParentTransform();
                                Transform localToParent = subGrpTransform.createConcatenation(textTransform);
                                Transform combined = scale.createConcatenation(localToParent);
                                
                                Path strokePath = org.example.component.helper.VectorBooleanHelper.transformPath(fillPath, combined);
                                strokePath.setFill(Color.TRANSPARENT);
                                strokePath.setStroke(strokeColor);
                                strokePath.setStrokeWidth(strokeWidth);
                                strokePath.setStrokeType(StrokeType.OUTSIDE);
                                strokePath.setStrokeLineJoin(StrokeLineJoin.ROUND);
                                strokePath.setStrokeLineCap(StrokeLineCap.ROUND);
                                
                                strokeGroup.getChildren().add(strokePath);
                            }
                        }
                    }
                }
            }
            
            // Align strokeGroup exactly with textGroup position
            strokeGroup.setTranslateX(textGroup.getTranslateX());
            strokeGroup.setTranslateY(textGroup.getTranslateY());
        }

        if (dropShadowEnabled) {
            textGroup.setEffect(new javafx.scene.effect.DropShadow(10, dropShadowColor));
        } else {
            textGroup.setEffect(null);
        }

        if (isTrajectoryEditMode) {
            renderCurveGuide();
        }
        invalidateBounds();
        if (isInitialized)
            notifyVisualChange();
        isUpdating = false;
    }

    private Path getPristinePath(Text source) {
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
        
        clone.setStrokeWidth(0);
        clone.setStroke(javafx.scene.paint.Color.TRANSPARENT);
        
        return toPath(clone);
    }

    private Path toPath(Shape shape) {
        if (shape == null) return null;
        Shape pathShape = Shape.union(shape, new Path());
        return (pathShape instanceof Path) ? (Path) pathShape : null;
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
                if (trajectorySnapshot != null && getVisualizer().getHistoryManager() != null) {
                    getVisualizer().getHistoryManager().addCommand(
                            new org.example.pattern.TrajectoryCommand(this, trajectorySnapshot, trajectory));
                }
                invalidateBounds(); // Final refresh
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







    private void recordTransformUndo(NodeMemento before) {
        if (before == null || getVisualizer() == null || getVisualizer().getHistoryManager() == null)
            return;

        NodeMemento after = new NodeMemento(this);
        if (!before.isEquivalentTo(after)) {
            getVisualizer().getHistoryManager().addCommand(new TransformCommand(this, before, after, getActiveZone()));
        }
    }

    /**
     * Updates the pivot of all transforms without moving the node visually.
     * This prevents the "jumping" effect when entering/exiting rotation mode.
     */






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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && !old.equals(t)) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && !old.equals(this.font)) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && oldFont != this.font) {
            Font finalOldFont = oldFont;
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && oldFont != this.font) {
            Font finalOldFont = oldFont;
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && !old.equals(c)) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
                    "Cambiar Color de Texto", old, c, val -> {
                        this.textColor = val;
                        this.renderText();
                    }, null));
        }
    }

    public double getSpacing() {
        return spacing;
    }

    public boolean isVerticalLayout() {
        return verticalLayout;
    }

    public void setVerticalLayout(boolean vl) {
        this.verticalLayout = vl;
        renderText();
    }

    public void setSpacing(double s) {
        if (this.spacing == s)
            return;
        double old = this.spacing;
        this.spacing = s;
        renderText();
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && !old.equals(c)) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null && !old.equals(c)) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            // Use a composite-like approach: restore both w and h together
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            getVisualizer().getHistoryManager().addCommand(
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
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            getVisualizer().getHistoryManager().addCommand(new PropertyChangeCommand<>(
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

        // Snapshot the text before editing so we can restore it on cancel or empty-commit
        final String textBefore = this.textContent;

        isBeingEdited = true;
        inlineEditor = new InlineTextEditor();
        inlineEditor.start(this, parent,
                newText -> {
                    isBeingEdited = false;
                    inlineEditor = null;

                    // If the user cleared all text, treat it as a cancel — restore original
                    if (newText == null || newText.trim().isEmpty()) {
                        // Restore the original text without touching history
                        this.textContent = textBefore;
                        renderText();
                        return;
                    }

                    // Only record to history when text actually changed
                    if (!newText.equals(textBefore)) {
                        // Update text content and push a single undo command for the full edit
                        this.textContent = newText;
                        renderText();
                        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
                            final String capturedBefore = textBefore;
                            final String capturedAfter = newText;
                            getVisualizer().getHistoryManager().addCommand(
                                new org.example.pattern.PropertyChangeCommand<>(
                                    "Editar Texto",
                                    capturedBefore,
                                    capturedAfter,
                                    val -> { this.textContent = val; renderText(); }));
                        }
                    }
                },
                () -> {
                    // Cancel: restore original text without any history change
                    isBeingEdited = false;
                    inlineEditor = null;
                    if (!textBefore.equals(this.textContent)) {
                        this.textContent = textBefore;
                        renderText();
                    }
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
        trajectoryEditingGroup.getChildren().clear();
        if (en) {
            renderTrajectoryHandles();
        }
        trajectoryEditingGroup.setVisible(en);
        invalidateBounds();
    }

    public boolean getTrajectoryEditMode() {
        return isTrajectoryEditMode;
    }













    public double getShearX() {
        return getInternalShearX();
    }

    public void setShearX(double x) {
        setInternalShearX(x);
    }

    public double getShearY() {
        return getInternalShearY();
    }

    public void setShearY(double y) {
        setInternalShearY(y);
    }









    public void addRotation(double angle) {
        setInternalRotation(getInternalRotation() + angle);
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
        invalidateBounds();
        updateSelectionOverlay();
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
        if (getVisualizer() != null)
            getVisualizer().getLayerManager().removeLayer(this);
    }

    public void flipHorizontal() {
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
            double currentRotation = getInternalRotation();
            setInternalScaleX(getInternalScaleX() * -1);
            setInternalRotation(-currentRotation);
            getVisualizer().getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before,
                    new org.example.pattern.NodeMemento(this), getActiveZone()));
        } else {
            double currentRotation = getInternalRotation();
            setInternalScaleX(getInternalScaleX() * -1);
            setInternalRotation(-currentRotation);
        }
    }

    public void flipVertical() {
        if (getVisualizer() != null && getVisualizer().getHistoryManager() != null) {
            org.example.pattern.NodeMemento before = new org.example.pattern.NodeMemento(this);
            double currentRotation = getInternalRotation();
            setInternalScaleY(getInternalScaleY() * -1);
            setInternalRotation(normalizeAngle(180 + currentRotation));
            getVisualizer().getHistoryManager().addCommand(new org.example.pattern.TransformCommand(this, before,
                    new org.example.pattern.NodeMemento(this), getActiveZone()));
        } else {
            double currentRotation = getInternalRotation();
            setInternalScaleY(getInternalScaleY() * -1);
            setInternalRotation(normalizeAngle(180 + currentRotation));
        }
    }

    private double normalizeAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle <= -180) angle += 360;
        return angle;
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
        clone.copyTransformsFrom(this);
        clone.setActiveZone(getActiveZone());
        return clone;
    }

    // --- GraphicLayer Implementation ---
























    @Override
    public void render() {
        renderText();
    }













}
