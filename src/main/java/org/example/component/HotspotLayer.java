package org.example.component;

import javafx.animation.FillTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.scene.Node;
import org.example.model.PrendaState.ReferenceHotspot;

import java.io.ByteArrayInputStream;

public class HotspotLayer extends StackPane {
    private final ReferenceHotspot data;
    private final Circle circle;
    private final Label label;
    private ScaleTransition scaleTransition;
    private FillTransition fillTransition;
    private double mouseAnchorX;
    private double mouseAnchorY;
    private Popup popup;
    private boolean isDragging = false;
    private final Runnable onPositionChanged;
    private final PrendaVisualizer visualizer;

    public HotspotLayer(ReferenceHotspot data, Runnable onPositionChanged, PrendaVisualizer visualizer) {
        this.data = data;
        this.onPositionChanged = onPositionChanged;
        this.visualizer = visualizer;
        
        this.circle = new Circle(5, Color.rgb(0, 0, 0, 0.15)); // Ghost fill
        this.circle.setStroke(Color.rgb(0, 0, 0, 0.25));
        this.circle.setStrokeWidth(0.8);
        this.circle.setEffect(null);
        
        this.label = new Label(data.getLabel());
        this.label.setStyle("-fx-text-fill: rgba(0,0,0,0.5); -fx-font-weight: bold; -fx-font-size: 8px;");
        
        this.setOnMouseEntered(e -> {
            if (isDragging) return;
            expandMarker();
            
            if (popup != null && !popup.isShowing() && !popup.getContent().isEmpty()) {
                // Show right below the cursor
                popup.show(this, e.getScreenX() + 15, e.getScreenY() + 15);
            }
        });
        
        this.setOnMouseExited(e -> {
            if (isDragging) return;
            shrinkMarker();
            
            if (popup != null && popup.isShowing()) {
                popup.hide();
            }
        });
        
        this.getChildren().addAll(circle, label);
        this.setAlignment(Pos.CENTER);
        this.setPickOnBounds(false);
        this.setTranslateX(data.getX());
        this.setTranslateY(data.getY());
        
        // Ensure cursor is hands so it's obviously interactive
        this.setCursor(javafx.scene.Cursor.HAND);

        initAnimations();
        setupDragging();
        updateVisuals();
    }

    private void initAnimations() {
        scaleTransition = new ScaleTransition(Duration.millis(250), this);
        fillTransition = new FillTransition(Duration.millis(250), this.circle);
    }

    private void expandMarker() {
        scaleTransition.stop();
        scaleTransition.setToX(1.6); // Slightly larger
        scaleTransition.setToY(1.6);
        scaleTransition.play();

        fillTransition.stop();
        fillTransition.setToValue(Color.web("#2980b9", 0.9)); // Solid blue
        fillTransition.play();

        this.circle.setStroke(Color.WHITE);
        this.circle.setStrokeWidth(1.5);
        this.circle.setEffect(new DropShadow(6, Color.web("#3498db", 0.6)));
        this.label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 8px;");
    }

    private void shrinkMarker() {
        scaleTransition.stop();
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.play();

        fillTransition.stop();
        fillTransition.setToValue(Color.rgb(0, 0, 0, 0.25));
        fillTransition.play();

        this.circle.setStroke(Color.rgb(0, 0, 0, 0.4));
        this.circle.setStrokeWidth(1.0);
        this.circle.setEffect(null);
        this.label.setStyle("-fx-text-fill: rgba(0,0,0,0.6); -fx-font-weight: bold; -fx-font-size: 8px;");
    }

    private void setupDragging() {
        this.setOnMousePressed(e -> {
            isDragging = true;
            this.setPickOnBounds(true); // Catch events even if cursor slips out of circle
            if (this.getParent() != null) {
                javafx.geometry.Point2D parentCoords = this.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                if (parentCoords != null) {
                    mouseAnchorX = parentCoords.getX() - this.getTranslateX();
                    mouseAnchorY = parentCoords.getY() - this.getTranslateY();
                }
            }
            visualizer.requestFocus();
            e.consume();
        });

        this.setOnMouseDragged(e -> {
            if (this.getParent() != null) {
                javafx.geometry.Point2D parentCoords = this.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                if (parentCoords != null) {
                    double newX = parentCoords.getX() - mouseAnchorX;
                    double newY = parentCoords.getY() - mouseAnchorY;
                    this.setTranslateX(newX);
                    this.setTranslateY(newY);
                    data.setX(newX);
                    data.setY(newY);
                }
            }
            e.consume();
            
            // Hide popup while dragging
            if (popup != null && popup.isShowing()) {
                popup.hide();
            }
        });

        this.setOnMouseReleased(e -> {
            if (isDragging) {
                isDragging = false;
                this.setPickOnBounds(false); // Restore normal behavior
                if (onPositionChanged != null) onPositionChanged.run();
            }
            if (!this.contains(e.getX(), e.getY())) {
                shrinkMarker();
            }
        });

        this.setOnMouseClicked(e -> {

            if (e.isStillSincePress()) {
                if (e.isControlDown()) {
                    // Toggle as persistent anchor for keyboard shortcuts C/E
                    if (visualizer.getActiveReferenceAnchor() == this) {
                        visualizer.setAlignmentActiveAnchor(null);
                        resetHighlight();
                    } else {
                        // Reset previous anchor highlight if it's a flag point
                        Node prevAnchor = visualizer.getActiveReferenceAnchor();
                        if (prevAnchor instanceof StackPane && !(prevAnchor instanceof HotspotLayer)) {
                            StackPane prev = (StackPane) prevAnchor;
                            if (prev.getChildren().get(0) instanceof Circle) {
                                ((Circle) prev.getChildren().get(0)).setFill(Color.web("#2ecc71", 0.3));
                                prev.setEffect(null);
                            }
                        } else if (prevAnchor instanceof HotspotLayer && prevAnchor != this) {
                            ((HotspotLayer)prevAnchor).resetHighlight();
                        }

                        visualizer.setAlignmentActiveAnchor(this);
                        circle.setFill(Color.web("#3498db"));
                        this.setEffect(new DropShadow(10, Color.web("#3498db")));
                    }
                } else if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    // Immediate Alignment on Click
                    java.util.Set<Node> selected = visualizer.getLayerManager().getSelectedNodes();
                    if (!selected.isEmpty()) {
                        String currentZone = visualizer.getPowerClipManager().getCurrentEditingZone();
                        org.example.pattern.CompositeCommand composite = new org.example.pattern.CompositeCommand("Alinear con Punto de Referencia");

                        java.util.Map<Node, org.example.pattern.NodeMemento> beforeStates = new java.util.HashMap<>();
                        for (Node n : selected) beforeStates.put(n, new org.example.pattern.NodeMemento(n));

                        java.util.List<Node> nodes = new java.util.ArrayList<>(selected);
                        nodes.add(this);

                        org.example.component.helper.AlignmentHelper.alignCenterHorizontal(nodes, this);
                        org.example.component.helper.AlignmentHelper.alignMiddleVertical(nodes, this);

                        for (Node n : selected) {
                            org.example.pattern.NodeMemento after = new org.example.pattern.NodeMemento(n);
                            composite.addCommand(new org.example.pattern.TransformCommand(n, beforeStates.get(n), after, currentZone));
                        }
                        visualizer.getHistoryManager().addCommand(composite);
                    }
                }

                if (popup != null) {
                    if (popup.isShowing()) popup.hide();
                    else if (!popup.getContent().isEmpty()) popup.show(this, e.getScreenX() + 15, e.getScreenY() + 15);
                }
            }
            e.consume();
        });
    }

    public void updateVisuals() {
        // Update label text if changed
        label.setText(data.getLabel());

        // Recreate Popup to ensure clean state
        if (popup != null && popup.isShowing()) {
            popup.hide();
        }
        popup = new Popup();
        popup.setAutoHide(true);
        popup.setConsumeAutoHidingEvents(false);

        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 4; -fx-background-radius: 4; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 0);");
        
        // Let VBox size exactly to its children
        box.setPrefSize(javafx.scene.layout.Region.USE_COMPUTED_SIZE, javafx.scene.layout.Region.USE_COMPUTED_SIZE);

        boolean hasContent = false;

        if (data.getDescription() != null && !data.getDescription().trim().isEmpty()) {
            Label descLabel = new Label(data.getDescription());
            descLabel.setWrapText(true);
            // FIX: Use setPrefWidth/setMinWidth instead of only setMaxWidth to prevent JavaFX from 
            // calculating a huge preferred height assuming zero initial width.
            descLabel.setPrefWidth(200);
            descLabel.setMinWidth(200);
            descLabel.setMaxWidth(200);
            descLabel.setAlignment(Pos.CENTER);
            descLabel.setStyle("-fx-text-fill: #34495e; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-alignment: center;");
            box.getChildren().add(descLabel);
            hasContent = true;
        }

        if ((data.getImageData() != null && data.getImageData().length > 0) || (data.getImagePath() != null && !data.getImagePath().trim().isEmpty())) {
            try {
                Image img = null;
                if (data.getImageData() != null && data.getImageData().length > 0) {
                    img = new Image(new ByteArrayInputStream(data.getImageData()));
                } else {
                    img = new Image(data.getImagePath(), 150, 150, true, true);
                }

                if (img != null && !img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(true);
                    iv.setFitWidth(150);
                    box.getChildren().add(iv);
                    hasContent = true;
                }
            } catch (Exception e) {}
        }

        if (hasContent) {
            popup.getContent().add(box);
        } else {
            Label placeholder = new Label("Referencia " + data.getLabel());
            placeholder.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
            box.getChildren().add(placeholder);
            popup.getContent().add(box);
        }
    }

    public ReferenceHotspot getData() {
        return data;
    }

    public void resetHighlight() {
        circle.setFill(Color.rgb(0,0,0,0.15));
        circle.setStroke(Color.rgb(0,0,0,0.25));
        this.setEffect(null);
    }
}

