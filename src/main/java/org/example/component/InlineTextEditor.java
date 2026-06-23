package org.example.component;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * Inline text editor that appears directly on the canvas.
 *
 * KEY DESIGN: The TextField is added as a child of the TextLayer Group itself,
 * NOT of the parent container. This means it automatically inherits every
 * transform that the TextLayer has — rotation, scale, viewport zoom, shear —
 * without any manual coordinate conversion.  The result is zero position jump.
 *
 * The TextField is placed in the TextLayer's local coordinate space at
 * (-logicalWidth/2, -logicalHeight/2) with size logicalWidth × logicalHeight,
 * which is the same box that the textGroup occupies.
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
    private Consumer<String> onCommit;
    private Runnable onCancel;
    private boolean committed = false;

    public InlineTextEditor() {}

    /**
     * Starts inline editing on the given TextLayer.
     *
     * @param layer    the layer to edit (TextField is injected into this layer's children)
     * @param parent   unused – kept for API compatibility; pass null if desired
     * @param onCommit callback invoked with the final text on Enter / focus-loss
     * @param onCancel callback invoked when Escape is pressed
     */
    public void start(TextLayer layer, javafx.scene.Group parent,
                      Consumer<String> onCommit, Runnable onCancel) {
        this.targetLayer = layer;
        this.onCommit    = onCommit;
        this.onCancel    = onCancel;

        textField = new TextField(layer.getTextContent());
        textField.getStyleClass().add("inline-text-editor");

        // ----------------------------------------------------------------
        // POSITION: layer-local coordinates, same box as textGroup.
        // The TextLayer renders text centered at its local origin (0,0),
        // so the top-left of the text box is at (-lw/2, -lh/2).
        // Since the TextField is a child of the TextLayer, it automatically
        // inherits rotation, scale, and viewport zoom — no manual math needed.
        // ----------------------------------------------------------------
        double lw = layer.getLogicalWidth();
        double lh = layer.getLogicalHeight();

        textField.setLayoutX(-lw / 2.0);
        textField.setLayoutY(-lh / 2.0);
        textField.setPrefWidth(lw);
        textField.setPrefHeight(lh);
        textField.setMinWidth(lw);
        textField.setMinHeight(lh);

        // ----------------------------------------------------------------
        // STYLE: completely transparent so only the text cursor is visible.
        // ----------------------------------------------------------------
        Font layerFont = layer.getFont();
        textField.setFont(layerFont);
        textField.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"     +
            "-fx-border-width: 0;"               +
            "-fx-background-insets: 0;"          +
            "-fx-padding: 0 0 0 0;"              +
            "-fx-font-size: " + layerFont.getSize() + "px;" +
            "-fx-text-fill: " + toWebColor(layer.getTextColor()) + ";" +
            "-fx-alignment: center;"
        );

        // ----------------------------------------------------------------
        // KEYBOARD HANDLING
        // ----------------------------------------------------------------
        textField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                commit();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                cancel();
                e.consume();
            }
        });

        // Commit on focus loss (e.g. clicking elsewhere on canvas)
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !committed) {
                Platform.runLater(this::commit);
            }
        });

        // Inject into the TextLayer itself — inherits all transforms automatically
        layer.getChildren().add(textField);
        textField.requestFocus();
        textField.selectAll();

        // Hide the original textGroup to avoid double-text rendering
        layer.getTextGroup().setVisible(false);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private void commit() {
        if (committed) return;
        committed = true;
        if (onCommit != null) {
            try {
                onCommit.accept(textField.getText());
            } catch (Exception e) {
                System.err.println("InlineTextEditor commit error: " + e.getMessage());
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
                System.err.println("InlineTextEditor cancel error: " + e.getMessage());
            }
        }
        finish();
    }

    private void finish() {
        // Restore textGroup visibility
        if (targetLayer != null && targetLayer.getTextGroup() != null) {
            targetLayer.getTextGroup().setVisible(true);
        }
        // Remove the TextField from wherever it was parented
        if (textField != null && textField.getParent() != null) {
            ((javafx.scene.Group) textField.getParent()).getChildren().remove(textField);
        }
        textField   = null;
        targetLayer = null;
    }

    private static String toWebColor(Color color) {
        if (color == null) return "#000000";
        return String.format("#%02X%02X%02X",
                (int) (color.getRed()   * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue()  * 255));
    }
}
