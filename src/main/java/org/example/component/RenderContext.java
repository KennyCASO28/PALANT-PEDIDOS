package org.example.component;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.example.model.TrajectoryPath;

/**
 * Immutable value object holding all data needed to render a TextLayer.
 * Used by TextRenderStrategy implementations.
 */
public class RenderContext {

    private final String textContent;
    private final Font font;
    private final Color textColor;
    private final Color strokeColor;
    private final double strokeWidth;
    private final boolean isBold;
    private final boolean isItalic;
    private final TrajectoryPath trajectory;
    private final double logicalWidth;
    private final double logicalHeight;
    private final double spacing;
    // --- NEW PROPERTIES ---
    private final javafx.scene.text.TextAlignment textAlignment;
    private final double fontSize;
    private final Color shadowColor;
    private final double shadowOffsetX;
    private final double shadowOffsetY;
    private final double shadowRadius;
    private final boolean useGradient;
    private final Color gradientStartColor;
    private final Color gradientEndColor;

    public RenderContext(String textContent, Font font, Color textColor, Color strokeColor, double strokeWidth,
                         boolean isBold, boolean isItalic, TrajectoryPath trajectory,
                         double logicalWidth, double logicalHeight, double spacing) {
        this(textContent, font, textColor, strokeColor, strokeWidth, isBold, isItalic, trajectory,
             logicalWidth, logicalHeight, spacing, javafx.scene.text.TextAlignment.CENTER, 24,
             Color.TRANSPARENT, 0, 0, 0, false, textColor, textColor);
    }

    public RenderContext(String textContent, Font font, Color textColor, Color strokeColor, double strokeWidth,
                         boolean isBold, boolean isItalic, TrajectoryPath trajectory,
                         double logicalWidth, double logicalHeight, double spacing,
                         javafx.scene.text.TextAlignment textAlignment, double fontSize,
                         Color shadowColor, double shadowOffsetX, double shadowOffsetY, double shadowRadius,
                         boolean useGradient, Color gradientStartColor, Color gradientEndColor) {
        this.textContent = textContent;
        this.font = font;
        this.textColor = textColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.isBold = isBold;
        this.isItalic = isItalic;
        this.trajectory = trajectory;
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.spacing = spacing;
        this.textAlignment = textAlignment;
        this.fontSize = fontSize;
        this.shadowColor = shadowColor;
        this.shadowOffsetX = shadowOffsetX;
        this.shadowOffsetY = shadowOffsetY;
        this.shadowRadius = shadowRadius;
        this.useGradient = useGradient;
        this.gradientStartColor = gradientStartColor;
        this.gradientEndColor = gradientEndColor;
    }

    public String getTextContent() { return textContent; }
    public Font getFont() { return font; }
    public Color getTextColor() { return textColor; }
    public Color getStrokeColor() { return strokeColor; }
    public double getStrokeWidth() { return strokeWidth; }
    public boolean isBold() { return isBold; }
    public boolean isItalic() { return isItalic; }
    public TrajectoryPath getTrajectory() { return trajectory; }
    public double getLogicalWidth() { return logicalWidth; }
    public double getLogicalHeight() { return logicalHeight; }
    public double getSpacing() { return spacing; }
    // --- NEW GETTERS ---
    public javafx.scene.text.TextAlignment getTextAlignment() { return textAlignment; }
    public double getFontSize() { return fontSize; }
    public Color getShadowColor() { return shadowColor; }
    public double getShadowOffsetX() { return shadowOffsetX; }
    public double getShadowOffsetY() { return shadowOffsetY; }
    public double getShadowRadius() { return shadowRadius; }
    public boolean isUseGradient() { return useGradient; }
    public Color getGradientStartColor() { return gradientStartColor; }
    public Color getGradientEndColor() { return gradientEndColor; }
}
