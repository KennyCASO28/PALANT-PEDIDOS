package org.example.component.helper;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import org.example.component.RenderContext;
import org.example.component.TextLayer;
import org.example.component.TextRenderStrategy;

/**
 * Optimized single-node rendering strategy for straight (linear) text.
 * Uses a single Text node instead of per-character nodes, significantly improving
 * performance for simple horizontal text layout (the most common case).
 */
public class OptimizedStraightRenderStrategy implements TextRenderStrategy {

    @Override
    public void render(TextLayer textLayer, Group textGroup, RenderContext ctx) {
        String text = ctx.getTextContent();
        if (text == null || text.isEmpty()) return;

        Font font = ctx.getFont();
        double logicalWidth = ctx.getLogicalWidth();
        double logicalHeight = ctx.getLogicalHeight();

        // Calculate natural size of the text
        Text textNode = new Text(text);
        textNode.setFont(font);
        textNode.setFill(ctx.getTextColor());
        textNode.setTextOrigin(javafx.geometry.VPos.CENTER);
        textNode.setBoundsType(javafx.scene.text.TextBoundsType.VISUAL);
        textNode.setTextAlignment(ctx.getTextAlignment());

        double naturalWidth = textNode.getLayoutBounds().getWidth();
        double scaleX = logicalWidth / Math.max(1, naturalWidth);
        double scaleY = logicalHeight / Math.max(1, font.getSize());

        // Apply optimized styling based on render context
        if (ctx.getStrokeWidth() > 0) {
            textNode.setStroke(ctx.getStrokeColor());
            textNode.setStrokeWidth(ctx.getStrokeWidth());
            textNode.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        }

        // Position text centered
        textNode.setTranslateX(0);
        textNode.setTranslateY(0);
        textNode.getTransforms().add(new Scale(scaleX, scaleY));

        textGroup.getChildren().add(textNode);
    }

    @Override
    public double calculateNaturalWidth(TextLayer textLayer, RenderContext ctx) {
        String text = ctx.getTextContent();
        if (text == null || text.isEmpty()) return 0;
        Font font = ctx.getFont();
        Text t = new Text(text);
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
    }

    @Override
    public double calculateNaturalHeight(TextLayer textLayer, RenderContext ctx) {
        Font font = ctx.getFont();
        if (font == null) return 0;
        Text t = new Text("Mg");
        t.setFont(font);
        return t.getLayoutBounds().getHeight();
    }

    @Override
    public boolean isOptimized() {
        return true;
    }
}
