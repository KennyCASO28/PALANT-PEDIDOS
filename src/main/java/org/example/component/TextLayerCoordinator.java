package org.example.component;

import javafx.geometry.Point2D;
import org.example.model.TrajectoryPath;

import java.util.function.Consumer;

/**
 * Interface for coordinating interactions between TextLayer and external systems.
 * Implements the Mediator pattern to reduce coupling between TextLayer, PrendaVisualizer,
 * and other components.
 */
public interface TextLayerCoordinator {

    /**
     * Notifies the coordinator that the text layer's content has changed.
     * @param layer the layer whose content changed
     * @param newContent the new text content
     */
    void onTextContentChanged(TextLayer layer, String newContent);

    /**
     * Notifies the coordinator that a double-click (edit request) occurred on the layer.
     * @param layer the layer that was double-clicked
     * @param sceneX the X coordinate in scene space
     * @param sceneY the Y coordinate in scene space
     */
    void onEditRequested(TextLayer layer, double sceneX, double sceneY);

    /**
     * Notifies the coordinator that the layer's trajectory was modified.
     * @param layer the layer whose trajectory changed
     * @param newTrajectory the new trajectory
     */
    void onTrajectoryChanged(TextLayer layer, TrajectoryPath newTrajectory);

    /**
     * Notifies the coordinator that the layer was selected.
     * @param layer the layer that was selected
     */
    void onLayerSelected(TextLayer layer);

    /**
     * Requests the coordinator to convert a point from scene to local coordinates.
     * @param sceneX the X coordinate in scene space
     * @param sceneY the Y coordinate in scene space
     * @return the point in local coordinates
     */
    Point2D sceneToLocal(double sceneX, double sceneY);

    /**
     * Checks if the user is currently in a drag operation (to avoid triggering edit on accidental double-click during drag).
     * @return true if a drag is in progress
     */
    boolean isDragging();

    /**
     * Gets the node's x translation (used for inline edit field positioning).
     * @return the translate X value
     */
    double getNodeTranslateX(TextLayer layer);

    /**
     * Gets the node's y translation (used for inline edit field positioning).
     * @return the translate Y value
     */
    double getNodeTranslateY(TextLayer layer);
}
