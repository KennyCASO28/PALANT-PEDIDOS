package org.example.component.helper;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import org.example.component.ui.Ruler;
import org.example.component.ui.GuideLine;

/**
 * Orchestrates the UI structure of the visualizer.
 * Handles rulers, guide lines, and viewport transformations.
 */
public class VisualizerUIOrchestrator {

    private final Pane designAreaStack;
    private final BorderPane mainBorderPane;
    private final HBox topBox;
    private final Ruler horizontalRuler;
    private final Ruler verticalRuler;
    private final Region rulerCorner;
    private final Pane guideLayer;
    
    private final Group contentGroup;
    private final Group contourGroup = new Group();
    private final Group handlesGroup = new Group();
    private ViewportController viewportController;
    private GuideManager guideManager;

    public VisualizerUIOrchestrator(Group contentGroup) {
        this.contentGroup = contentGroup;
        
        // Initialize Rulers
        this.horizontalRuler = new Ruler(Ruler.Orientation.HORIZONTAL);
        this.verticalRuler = new Ruler(Ruler.Orientation.VERTICAL);
        
        this.rulerCorner = new Region();
        this.rulerCorner.setPrefSize(16, 16);
        this.rulerCorner.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 0 1 1 0;");
        
        this.topBox = new HBox(rulerCorner, horizontalRuler);
        HBox.setHgrow(horizontalRuler, Priority.ALWAYS);
        
        this.guideLayer = new Pane();
        this.guideLayer.setMouseTransparent(false);
        this.guideLayer.setPickOnBounds(false);

        this.designAreaStack = new Pane();
        // FIXED SIZE: Prevent the container from resizing based on content bounds
        // which triggers autoScale() and creates 'zoom' effects during rotation.
        this.designAreaStack.setPrefSize(2000, 2000); 
        this.designAreaStack.getChildren().addAll(contentGroup, guideLayer, contourGroup, handlesGroup);
        
        this.mainBorderPane = new BorderPane();
        this.mainBorderPane.setTop(topBox);
        this.mainBorderPane.setLeft(verticalRuler);
        this.mainBorderPane.setCenter(this.designAreaStack);
        
        setupClipping();
    }

    private void setupClipping() {
        Rectangle designClip = new Rectangle();
        designClip.widthProperty().bind(this.designAreaStack.widthProperty());
        designClip.heightProperty().bind(this.designAreaStack.heightProperty());
        this.designAreaStack.setClip(designClip);
    }

    public void initializeViewportAndGuides() {
        this.viewportController = new ViewportController(this.designAreaStack, contentGroup);
        
        horizontalRuler.setViewportController(viewportController);
        verticalRuler.setViewportController(viewportController);
        
        this.guideManager = new GuideManager(guideLayer, viewportController);
        
        viewportController.setOnViewportChanged(() -> {
            guideManager.updateGuides();
            horizontalRuler.draw();
            verticalRuler.draw();
        });

        setupRulerInteractions();
    }

    private GuideLine currentlyDraggedGuide = null;

    private void setupRulerInteractions() {
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

    public Pane getDesignAreaStack() {
        return designAreaStack;
    }

    public BorderPane getMainBorderPane() {
        return mainBorderPane;
    }

    public ViewportController getViewportController() {
        return viewportController;
    }

    public GuideManager getGuideManager() {
        return guideManager;
    }

    public Ruler getHorizontalRuler() {
        return horizontalRuler;
    }

    public Ruler getVerticalRuler() {
        return verticalRuler;
    }

    public Group getContourGroup() { return contourGroup; }
    public Group getHandlesGroup() { return handlesGroup; }
    public Pane getGuideLayer() { return guideLayer; }

    public void autoScale() {
        if (viewportController != null) {
            javafx.application.Platform.runLater(viewportController::autoScale);
        }
    }
}
