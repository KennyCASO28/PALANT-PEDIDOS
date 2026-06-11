package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.example.component.PrendaVisualizer;

public class VisualizerEyedropperService {

    private final PrendaVisualizer visualizer;
    private javafx.event.EventHandler<MouseEvent> eyedropperMouseHandler;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> eyedropperEscHandler;
    private java.util.function.Consumer<Color> currentOnPreview;

    private boolean wasOverlayVisibleBeforeEyedropper = false;
    private boolean eyedropperUseDataPick = false;

    // Loupe State
    private Group loupeGroup;
    private ImageView loupeView;
    private Label loupeColorLabel;
    private Circle loupeContentClip;
    private Circle loupeRing;

    public VisualizerEyedropperService(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    public void startEyedropperSession(java.util.function.Consumer<Color> onPicked) {
        startEyedropperSession(onPicked, null);
    }

    public void startEyedropperSession(java.util.function.Consumer<Color> onPicked,
            java.util.function.Consumer<Color> onPreview) {
        startEyedropperSession(false, onPicked, onPreview);
    }

    public void startEyedropperSession(boolean useDataPick, java.util.function.Consumer<Color> onPicked,
            java.util.function.Consumer<Color> onPreview) {
        this.eyedropperUseDataPick = useDataPick;
        this.currentOnPreview = onPreview;

        if (visualizer.getOverlayManager() != null) {
            wasOverlayVisibleBeforeEyedropper = visualizer.getOverlayManager().getOverlayGroup().isVisible();
            visualizer.getOverlayManager().getOverlayGroup().setVisible(true);
            visualizer.getOverlayManager().getOverlayGroup().toFront();
        }

        visualizer.startSnapshotSession();
        initLoupe();
        if (loupeGroup != null) {
            loupeGroup.toFront();
        }

        eyedropperMouseHandler = new javafx.event.EventHandler<MouseEvent>() {
            private Color lastPreviewColor = null;
            private long lastUpdateTime = 0;
            private double lastX = -1, lastY = -1;

            @Override
            public void handle(MouseEvent e) {
                if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
                    e.consume();
                    Color color = eyedropperUseDataPick ? visualizer.pickDataColor(e.getSceneX(), e.getSceneY())
                                                        : visualizer.pickColor(e.getSceneX(), e.getSceneY());
                    if (onPicked != null)
                        onPicked.accept(color);
                    cleanupEyedropper();
                } else if (e.getEventType() == MouseEvent.MOUSE_MOVED) {
                    long now = System.currentTimeMillis();
                    double sx = e.getSceneX();
                    double sy = e.getSceneY();

                    updateLoupePosition(sx, sy);

                    if (now - lastUpdateTime > 16 || lastX == -1) {
                        Color hoverColor = updateLoupeContent(sx, sy);
                        
                        if (eyedropperUseDataPick) {
                            hoverColor = visualizer.pickDataColor(sx, sy);
                        }
                        
                        if (onPreview != null && (lastPreviewColor == null || !lastPreviewColor.equals(hoverColor))) {
                            onPreview.accept(hoverColor);
                            lastPreviewColor = hoverColor;
                        }
                        lastUpdateTime = now;
                        lastX = sx;
                        lastY = sy;
                    }
                }
            }
        };

        eyedropperEscHandler = e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cleanupEyedropper();
                e.consume();
            }
        };

        visualizer.addEventFilter(MouseEvent.MOUSE_PRESSED, eyedropperMouseHandler);
        visualizer.addEventFilter(MouseEvent.MOUSE_MOVED, eyedropperMouseHandler);
        
        if (visualizer.getScene() != null) {
            visualizer.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, eyedropperEscHandler);
        }
        
        loupeGroup.setVisible(true);
        visualizer.setCursor(Cursor.NONE);
    }

    public boolean isEyedropperActive() {
        return eyedropperMouseHandler != null;
    }

    private void cleanupEyedropper() {
        if (currentOnPreview != null) {
            currentOnPreview.accept(null);
        }
        currentOnPreview = null;

        if (eyedropperMouseHandler != null) {
            visualizer.removeEventFilter(MouseEvent.MOUSE_PRESSED, eyedropperMouseHandler);
            visualizer.removeEventFilter(MouseEvent.MOUSE_MOVED, eyedropperMouseHandler);
            eyedropperMouseHandler = null;
        }
        
        if (eyedropperEscHandler != null) {
            if (visualizer.getScene() != null) {
                visualizer.getScene().removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, eyedropperEscHandler);
            }
            eyedropperEscHandler = null;
        }

        if (loupeGroup != null) {
            loupeGroup.setVisible(false);
        }

        visualizer.setCursor(Cursor.DEFAULT);
        visualizer.endSnapshotSession();
        
        visualizer.getColorManager().clearPreviewColors();
        visualizer.getColorManager().clearCache();
        visualizer.requestLayout();

        if (visualizer.getOverlayManager() != null) {
            visualizer.getOverlayManager().getOverlayGroup().setVisible(wasOverlayVisibleBeforeEyedropper);
        }
    }

    private void initLoupe() {
        if (loupeGroup == null) {
            loupeGroup = new Group();
            loupeGroup.setMouseTransparent(true);
            loupeGroup.setManaged(false);

            loupeView = new ImageView();
            loupeView.setFitWidth(120);
            loupeView.setFitHeight(120);
            loupeView.setPreserveRatio(true);
            loupeView.setSmooth(false);

            loupeContentClip = new Circle(60, 60, 60);
            loupeView.setClip(loupeContentClip);

            loupeRing = new Circle(60, 60, 62);
            loupeRing.setFill(null);
            loupeRing.setStroke(Color.WHITE);
            loupeRing.setStrokeWidth(3);

            Line vLine = new Line(60, 50, 60, 70);
            vLine.setStroke(Color.RED);
            Line hLine = new Line(50, 60, 70, 60);
            hLine.setStroke(Color.RED);

            loupeColorLabel = new Label("#FFFFFF");
            loupeColorLabel.setStyle(
                    "-fx-background-color: rgba(30, 41, 59, 0.9); " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 3 8; " +
                    "-fx-font-family: 'Consolas', 'Monospace'; " +
                    "-fx-font-size: 11px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-radius: 4; " +
                    "-fx-border-color: rgba(255,255,255,0.2); " +
                    "-fx-border-radius: 4;");
            loupeColorLabel.setLayoutX(20);
            loupeColorLabel.setLayoutY(130);

            loupeGroup.setEffect(new javafx.scene.effect.DropShadow(15, Color.rgb(0,0,0,0.3)));
            loupeGroup.getChildren().addAll(loupeRing, loupeView, vLine, hLine, loupeColorLabel);
        }

        if (!visualizer.getOverlayManager().getOverlayGroup().getChildren().contains(loupeGroup)) {
            visualizer.getOverlayManager().getOverlayGroup().getChildren().add(loupeGroup);
        }
    }
    
    private void updateLoupePosition(double sceneX, double sceneY) {
        if (loupeGroup == null) return;
        Point2D local = visualizer.getOverlayManager().getOverlayGroup().sceneToLocal(sceneX, sceneY);
        loupeGroup.setLayoutX(local.getX() - 60);
        loupeGroup.setLayoutY(local.getY() - 60);
    }

    private Color updateLoupeContent(double sceneX, double sceneY) {
        if (loupeGroup == null)
            return null;

        WritableImage cachedImg = visualizer.getCachedSnapshot();
        if (cachedImg != null) {
            if (loupeView.getImage() != cachedImg) {
                loupeView.setImage(cachedImg);
            }

            double scaleX = cachedImg.getWidth() / visualizer.getWidth();
            double scaleY = cachedImg.getHeight() / visualizer.getHeight();

            Point2D viewLocal = visualizer.sceneToLocal(sceneX, sceneY);
            
            double pixelW = 30 * scaleX;
            double pixelH = 30 * scaleY;
            
            double centerX = viewLocal.getX() * scaleX;
            double centerY = viewLocal.getY() * scaleY;

            loupeView.setViewport(new javafx.geometry.Rectangle2D(
                    centerX - pixelW/2,
                    centerY - pixelH/2,
                    pixelW,
                    pixelH
            ));
        }

        Color c = visualizer.pickColor(sceneX, sceneY);
        String hex = String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
        loupeColorLabel.setText(hex);
        
        loupeRing.setStroke(c.invert()); 
        return c;
    }
}
