package org.example.component.ui;

import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;
import org.example.component.helper.ViewportController;

/**
 * A dashed line that represents a guide.
 */
public class GuideLine extends Line {
    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    private final Orientation orientation;
    private double designPosition;
    private ViewportController viewportController;
    private boolean selected = false;

    public GuideLine(Orientation orientation, double designPosition, ViewportController vc) {
        this.orientation = orientation;
        this.designPosition = designPosition;
        this.viewportController = vc;

        updateStyle();
        getStrokeDashArray().addAll(5.0, 5.0);
        setCursor(Cursor.MOVE);

        // Bind the line to span the entire container
        if (orientation == Orientation.HORIZONTAL) {
            setStartX(0);
            endXProperty().bind(viewportController.getContainerWidthProperty());
        } else {
            setStartY(0);
            endYProperty().bind(viewportController.getContainerHeightProperty());
        }

        setupInteraction();
        updatePosition();
    }

    private void setupInteraction() {
        setOnMouseDragged(e -> {
            if (viewportController == null)
                return;

            // Convert scene coordinates to container coordinates if needed,
            // but screenToDesign expects screen/scene coordinates relative to the same
            // logic.
            // Actually ViewportController uses layoutOffsetX etc which are relative to the
            // container.
            // So we should use coordinates relative to the container.

            double containerX = e.getX(); // This is local to the Line, not good.
            // We need parent coordinates.

            if (getParent() != null) {
                // Scene coordinates are safest
                if (orientation == Orientation.HORIZONTAL) {
                    designPosition = viewportController.sceneToDesignY(e.getSceneY());
                } else {
                    designPosition = viewportController.sceneToDesignX(e.getSceneX());
                }
                updatePosition();
            }
        });

        // Snap to grid/labels could be added here
    }

    public void updatePosition() {
        if (viewportController == null)
            return;

        double screenPos = (orientation == Orientation.HORIZONTAL) ? viewportController.designToScreenY(designPosition)
                : viewportController.designToScreenX(designPosition);

        if (orientation == Orientation.HORIZONTAL) {
            setStartY(screenPos);
            setEndY(screenPos);
        } else {
            setStartX(screenPos);
            setEndX(screenPos);
        }
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setDesignPosition(double designPosition) {
        this.designPosition = designPosition;
    }

    public double getDesignPosition() {
        return designPosition;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateStyle();
    }

    public boolean isSelected() {
        return selected;
    }

    private void updateStyle() {
        if (selected) {
            setStroke(Color.web("#f1c40f")); // Yellow for selected
            setStrokeWidth(2);
        } else {
            setStrokeWidth(1);
            if (orientation == Orientation.HORIZONTAL) {
                setStroke(Color.web("#e74c3c")); // Red for horizontal
            } else {
                setStroke(Color.web("#3498db")); // Blue for vertical
            }
        }
    }
}

