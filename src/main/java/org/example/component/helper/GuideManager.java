package org.example.component.helper;

import javafx.scene.layout.Pane;
import org.example.component.ui.GuideLine;
import org.example.component.helper.ViewportController;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages guide lines for the visualizer.
 */
public class GuideManager {
    private final Pane container;
    private final ViewportController viewportController;
    private final List<GuideLine> guides = new ArrayList<>();
    private GuideLine selectedGuide = null;

    public GuideManager(Pane container, ViewportController vc) {
        this.container = container;
        this.viewportController = vc;
    }

    public void addGuide(GuideLine.Orientation orientation, double designPos) {
        addGuideAndReturn(orientation, designPos);
    }

    public GuideLine addGuideAndReturn(GuideLine.Orientation orientation, double designPos) {
        GuideLine guide = new GuideLine(orientation, designPos, viewportController);
        guides.add(guide);
        container.getChildren().add(guide);

        // Setup selection and removal on click
        guide.setOnMousePressed(e -> {
            selectGuide(guide);
            e.consume(); // Prevent click from bubbling up and deselecting
        });

        // Setup removal on right-click (optional, keeping for redundancy)
        guide.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                removeGuide(guide);
            }
        });
        return guide;
    }

    public void selectGuide(GuideLine guide) {
        if (selectedGuide != null) {
            selectedGuide.setSelected(false);
        }
        selectedGuide = guide;
        if (selectedGuide != null) {
            selectedGuide.setSelected(true);
        }
    }

    public void deselectAll() {
        if (selectedGuide != null) {
            selectedGuide.setSelected(false);
            selectedGuide = null;
        }
    }

    public void deleteSelectedGuide() {
        if (selectedGuide != null) {
            removeGuide(selectedGuide);
            selectedGuide = null;
        }
    }

    public void removeGuide(GuideLine guide) {
        guides.remove(guide);
        container.getChildren().remove(guide);
    }

    public void updateGuides() {
        for (GuideLine guide : guides) {
            guide.updatePosition();
        }
    }

    public void clearGuides() {
        container.getChildren().removeAll(guides);
        guides.clear();
    }

    public List<GuideLine> getGuides() {
        return guides;
    }
}

