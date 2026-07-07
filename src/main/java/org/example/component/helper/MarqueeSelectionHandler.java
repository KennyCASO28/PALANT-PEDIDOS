package org.example.component.helper;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import org.example.component.GraphicLayer;
import org.example.component.PrendaVisualizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Corel-style drag-box (marquee) selection.
 */
public class MarqueeSelectionHandler {

    private final PrendaVisualizer visualizer;
    private final Rectangle marqueeRect;

    private double startX;
    private double startY;
    private boolean isDragging = false;

    public MarqueeSelectionHandler(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;

        marqueeRect = new Rectangle();
        marqueeRect.setFill(Color.web("#3498db", 0.15));
        marqueeRect.setStroke(Color.web("#2980b9"));
        marqueeRect.setStrokeWidth(1.0);
        marqueeRect.getStrokeDashArray().addAll(5.0, 5.0);
        marqueeRect.setStrokeType(StrokeType.INSIDE);
        marqueeRect.setVisible(false);
        marqueeRect.setMouseTransparent(true);
        marqueeRect.setManaged(false);
    }

    public void attach() {
        // Add to the guide layer so it renders above the shapes but below the UI
        visualizer.getGuideLayer().getChildren().add(marqueeRect);

        // Listen on the design area stack (background clicks)
        visualizer.getUiOrchestrator().getDesignAreaStack().addEventFilter(MouseEvent.MOUSE_PRESSED,
                this::onMousePressed);
        visualizer.getUiOrchestrator().getDesignAreaStack().addEventFilter(MouseEvent.MOUSE_DRAGGED,
                this::onMouseDragged);
        visualizer.getUiOrchestrator().getDesignAreaStack().addEventFilter(MouseEvent.MOUSE_RELEASED,
                this::onMouseReleased);
    }

    private void onMousePressed(MouseEvent event) {
        // Only start if we clicked the background (not consuming it yet to allow other
        // features to work if needed)
        // Check if the target is the background pane (designAreaStack or guideLayer)
        Node target = (Node) event.getTarget();
        if (target != visualizer.getUiOrchestrator().getDesignAreaStack() && target != visualizer.getGuideLayer()) {
            return;
        }

        if (!event.isShiftDown()) {
            visualizer.getUserLayerManager().clearSelection();
        }

        startX = event.getX();
        startY = event.getY();

        marqueeRect.setX(startX);
        marqueeRect.setY(startY);
        marqueeRect.setWidth(0);
        marqueeRect.setHeight(0);
        marqueeRect.setVisible(true);
        isDragging = true;
    }

    private void onMouseDragged(MouseEvent event) {
        if (!isDragging)
            return;

        double currentX = event.getX();
        double currentY = event.getY();

        marqueeRect.setX(Math.min(startX, currentX));
        marqueeRect.setY(Math.min(startY, currentY));
        marqueeRect.setWidth(Math.abs(currentX - startX));
        marqueeRect.setHeight(Math.abs(currentY - startY));
    }

    private void onMouseReleased(MouseEvent event) {
        if (!isDragging)
            return;
        isDragging = false;
        marqueeRect.setVisible(false);

        // Ignore tiny clicks
        if (marqueeRect.getWidth() < 2 || marqueeRect.getHeight() < 2) {
            return;
        }

        Bounds selectionBoundsLocal = marqueeRect.getBoundsInLocal();
        Bounds selectionBoundsScene = marqueeRect.localToScene(selectionBoundsLocal);

        List<Node> nodesToSelect = new ArrayList<>();

        for (Node layer : visualizer.getUserLayerManager().getLayers()) {
            if (!layer.isVisible() || layer.isMouseTransparent() || (layer instanceof GraphicLayer && ((GraphicLayer) layer).isLocked())) {
                continue;
            }

            Bounds nodeBoundsLocal = layer.getBoundsInLocal();
            Bounds nodeBoundsScene = layer.localToScene(nodeBoundsLocal);

            // Corel Draw style: Must be FULLY enclosed
            if (selectionBoundsScene.contains(nodeBoundsScene)) {
                nodesToSelect.add(layer);
            }
        }

        for (Node n : nodesToSelect) {
            visualizer.getUserLayerManager().addToSelection(n);
        }
    }
}
