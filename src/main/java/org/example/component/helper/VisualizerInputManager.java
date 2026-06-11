package org.example.component.helper;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.example.component.PrendaVisualizer;
import org.example.component.ui.GuideLine;
import org.example.component.ui.Ruler;

public class VisualizerInputManager {

    private final PrendaVisualizer visualizer;
    private final GuideManager guideManager;
    private final ViewportController viewportController;
    private GuideLine currentlyDraggedGuide = null;

    public VisualizerInputManager(PrendaVisualizer visualizer, GuideManager guideManager, ViewportController viewportController) {
        this.visualizer = visualizer;
        this.guideManager = guideManager;
        this.viewportController = viewportController;
        initEventHandlers();
    }

    private void initEventHandlers() {
        // Keyboard handler for shortcuts
        visualizer.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DELETE) {
                if (guideManager != null) {
                    guideManager.deleteSelectedGuide();
                }
            }
        });

        // Click outside guides to deselect
        visualizer.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!(e.getTarget() instanceof GuideLine) && guideManager != null) {
                guideManager.deselectAll();
            }
        });
    }

    public void setupRulerInteractions(Ruler horizontalRuler, Ruler verticalRuler) {
        if (horizontalRuler != null) {
            horizontalRuler.setOnMousePressed(e -> {
                double initialDesignY = viewportController.sceneToDesignY(e.getSceneY());
                currentlyDraggedGuide = guideManager.addGuideAndReturn(GuideLine.Orientation.HORIZONTAL, initialDesignY);
            });
            horizontalRuler.setOnMouseDragged(e -> {
                if (currentlyDraggedGuide != null) {
                    currentlyDraggedGuide.setDesignPosition(viewportController.sceneToDesignY(e.getSceneY()));
                    currentlyDraggedGuide.updatePosition();
                }
            });
            horizontalRuler.setOnMouseReleased(e -> {
                currentlyDraggedGuide = null;
            });
        }

        if (verticalRuler != null) {
            verticalRuler.setOnMousePressed(e -> {
                double initialDesignX = viewportController.sceneToDesignX(e.getSceneX());
                currentlyDraggedGuide = guideManager.addGuideAndReturn(GuideLine.Orientation.VERTICAL, initialDesignX);
            });
            verticalRuler.setOnMouseDragged(e -> {
                if (currentlyDraggedGuide != null) {
                    currentlyDraggedGuide.setDesignPosition(viewportController.sceneToDesignX(e.getSceneX()));
                    currentlyDraggedGuide.updatePosition();
                }
            });
            verticalRuler.setOnMouseReleased(e -> {
                currentlyDraggedGuide = null;
            });
        }
    }
}
