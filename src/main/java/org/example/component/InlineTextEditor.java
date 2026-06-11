package org.example.component;

import javafx.geometry.Point2D;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * Inline text editor that appears directly on the canvas.
 * Allows users to edit text in-place without looking at the sidebar.
 * 
 * Usage:
 *   InlineTextEditor editor = new InlineTextEditor();
 *   editor.start(textLayer, parentGroup, newText -> { ... }, () -> { ... });
 *
 * Press Enter to commit, Escape to cancel.
 */
public class InlineTextEditor {

    private TextField textField;
    private TextLayer targetLayer;
    private javafx.scene.Group parentGroup;
    private Consumer<String> onCommit;
    private Runnable onCancel;
    private boolean committed = false;
    private javafx.animation.Timeline pulseAnimation;

    public InlineTextEditor() {}

    /**
     * Starts inline editing on the given TextLayer.
     *
     * @param layer        the layer to edit
     * @param parent       the parent Group where the editor will be added
     * @param onCommit     callback with the new text value
     * @param onCancel     callback when editing is cancelled
     */
    public void start(TextLayer layer, javafx.scene.Group parent, Consumer<String> onCommit, Runnable onCancel) {
        this.targetLayer = layer;
        this.parentGroup = parent;
        this.onCommit = onCommit;
        this.onCancel = onCancel;

        textField = new TextField(layer.getTextContent());
        textField.getStyleClass().add("inline-text-editor");

        // Style: BORDE VERDE NEON PULSANTE (Option B)
        Font layerFont = layer.getFont();
        textField.setFont(layerFont);
        textField.setStyle(
            "-fx-background-color: rgba(255,255,255,0.95);" +
            "-fx-border-color: #39ff14;" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 6;" +
            "-fx-font-size: " + layerFont.getSize() + "px;" +
            "-fx-padding: 6 10;" +
            "-fx-text-fill: " + toWebColor(layer.getTextColor()) + ";" +
            "-fx-effect: dropshadow(gaussian, rgba(57, 255, 20, 0.6), 8, 0, 0, 0);"
        );

        // Position over the layer in scene coordinates
        Point2D scenePos = layer.localToScene(-layer.getLogicalWidth() / 2, -layer.getLogicalHeight() / 2);
        Point2D parentPos = parent.sceneToLocal(scenePos);

        textField.setLayoutX(parentPos.getX());
        textField.setLayoutY(parentPos.getY());
        textField.setPrefWidth(Math.max(80, layer.getLogicalWidth() + 40)); // Give some extra room for typing
        textField.setPrefHeight(Math.max(30, layer.getLogicalHeight()));

        // NO ROTATION during editing: always horizontal for easy typing
        textField.setRotate(0);

        // Commit on Enter, Cancel on Escape
        textField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                commit();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                cancel();
            }
        });

        // Commit on focus lost
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !committed) {
                Platform.runLater(() -> commit());
            }
        });

        parent.getChildren().add(textField);
        textField.requestFocus();
        textField.selectAll();

        // Start NEON GREEN PULSE ANIMATION
        startNeonPulse();
    }

    private void startNeonPulse() {
        javafx.scene.paint.Color brightGreen = javafx.scene.paint.Color.web("#39ff14");
        javafx.scene.paint.Color darkGreen = javafx.scene.paint.Color.web("#16a085");

        pulseAnimation = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                new javafx.animation.KeyValue(textField.borderProperty(), createBorder(brightGreen, 3))),
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(800),
                new javafx.animation.KeyValue(textField.borderProperty(), createBorder(darkGreen, 2))),
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(1600),
                new javafx.animation.KeyValue(textField.borderProperty(), createBorder(brightGreen, 3)))
        );
        pulseAnimation.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulseAnimation.play();
    }

    private javafx.scene.layout.Border createBorder(javafx.scene.paint.Color color, double width) {
        javafx.scene.layout.BorderStroke stroke = new javafx.scene.layout.BorderStroke(
            color, javafx.scene.layout.BorderStrokeStyle.SOLID,
            new javafx.scene.layout.CornerRadii(6),
            new javafx.scene.layout.BorderWidths(width)
        );
        return new javafx.scene.layout.Border(stroke);
    }

    private void commit() {
        if (committed) return;
        committed = true;
        if (onCommit != null) {
            try {
                onCommit.accept(textField.getText());
            } catch (Exception e) {
                System.err.println("InlineTextEditor commit callback error: " + e.getMessage());
            }
        }
        finish();
    }

    private void cancel() {
        if (committed) return;
        committed = true;
        if (onCancel != null) {
            try {
                onCancel.run();
            } catch (Exception e) {
                System.err.println("InlineTextEditor cancel callback error: " + e.getMessage());
            }
        }
        finish();
    }

    private void finish() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }
        if (textField != null && textField.getParent() != null) {
            ((javafx.scene.Group) textField.getParent()).getChildren().remove(textField);
        }
        textField = null;
        targetLayer = null;
        parentGroup = null;
    }

    private String toWebColor(Color color) {
        if (color == null) return "#000000";
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
