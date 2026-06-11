package org.example.component;

/**
 * Strategy interface for text rendering approaches.
 * Allows different rendering strategies (e.g., per-character, full-node, optimized) to be swapped at runtime.
 */
public interface TextRenderStrategy {

    /**
     * Renders text content onto the provided textGroup.
     * Implementations should handle trajectory, scaling, and positioning.
     *
     * @param textGroup the Group on which to render
     * @param context rendering context with all necessary data
     */
    void render(org.example.component.TextLayer textLayer, javafx.scene.Group textGroup, RenderContext context);

    /**
     * Calculates the natural (unscaled) width of the rendered text.
     * Used for proper bounding box calculation.
     *
     * @param context the rendering context
     * @return the natural width of the text
     */
    double calculateNaturalWidth(org.example.component.TextLayer textLayer, RenderContext context);

    /**
     * Calculates the natural (unscaled) height of the rendered text.
     * Used for proper bounding box calculation.
     *
     * @param context the rendering context
     * @return the natural height of the text
     */
    double calculateNaturalHeight(org.example.component.TextLayer textLayer, RenderContext context);

    /**
     * Checks if this strategy is performance-optimized for general use.
     * @return true if optimized
     */
    boolean isOptimized();
}
