package org.example.logic;

import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.example.model.TextShape;

/**
 * Generates SVG paths for text with various curved shapes.
 * Handles mathematical transformations to position characters along curves.
 */
public class TextPathGenerator {

    /**
     * Generate SVG path content for text with specified shape.
     */
    public static String generatePath(String text, Font font, TextShape shape, double width, double height) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return switch (shape) {
            case STRAIGHT -> generateStraightPath(text, font, width);
            case ARC_TOP -> generateArcPath(text, font, width, height, true);
            case ARC_BOTTOM -> generateArcPath(text, font, width, height, false);
            case CIRCULAR -> generateCircularPath(text, font, Math.min(width, height) / 2);
            case OVAL -> generateOvalPath(text, font, width, height);
            case WAVE -> generateWavePath(text, font, width, height);
            case BEZIER -> generateStraightPath(text, font, width);
        };
    }

    /**
     * Straight text - simple horizontal layout
     */
    private static String generateStraightPath(String text, Font font, double width) {
        Text textNode = new Text(text);
        textNode.setFont(font);

        // Center the text
        double textWidth = textNode.getLayoutBounds().getWidth();
        double startX = (width - textWidth) / 2;

        StringBuilder path = new StringBuilder();
        double x = startX;

        for (char c : text.toCharArray()) {
            Text charNode = new Text(String.valueOf(c));
            charNode.setFont(font);

            // Simple positioning
            path.append(String.format("M %.2f 0 ", x));
            path.append(createCharPath(c, font));

            x += charNode.getLayoutBounds().getWidth();
        }

        return path.toString();
    }

    /**
     * Arc path - text follows a circular arc
     */
    private static String generateArcPath(String text, Font font, double width, double height, boolean top) {
        Text textNode = new Text(text);
        textNode.setFont(font);

        double textWidth = textNode.getLayoutBounds().getWidth();
        double radius = Math.max(width, textWidth) * 0.8;

        // Calculate arc length and angle
        double arcLength = textWidth * 1.2; // Add spacing
        double angleSpan = arcLength / radius; // Radians

        StringBuilder path = new StringBuilder();
        double currentAngle = -angleSpan / 2; // Start from left

        for (char c : text.toCharArray()) {
            Text charNode = new Text(String.valueOf(c));
            charNode.setFont(font);
            double charWidth = charNode.getLayoutBounds().getWidth();

            // Calculate position on arc
            double x = width / 2 + radius * Math.sin(currentAngle);
            double y;

            if (top) {
                y = height * 0.3 + radius * (1 - Math.cos(currentAngle));
            } else {
                y = height * 0.7 - radius * (1 - Math.cos(currentAngle));
            }

            // Rotate character to follow arc tangent
            double rotation = Math.toDegrees(currentAngle);
            if (!top) {
                rotation += 180; // Flip for bottom arc
            }

            path.append(createTransformedChar(c, font, x, y, rotation));

            currentAngle += charWidth / radius;
        }

        return path.toString();
    }

    /**
     * Circular path - text follows a complete circle
     */
    private static String generateCircularPath(String text, Font font, double radius) {
        Text textNode = new Text(text);
        textNode.setFont(font);

        StringBuilder path = new StringBuilder();
        double angleStep = (2 * Math.PI) / text.length();
        double currentAngle = -Math.PI / 2; // Start at top

        for (char c : text.toCharArray()) {
            double x = radius + radius * 0.8 * Math.cos(currentAngle);
            double y = radius + radius * 0.8 * Math.sin(currentAngle);
            double rotation = Math.toDegrees(currentAngle) + 90;

            path.append(createTransformedChar(c, font, x, y, rotation));
            currentAngle += angleStep;
        }

        return path.toString();
    }

    /**
     * Oval path - text follows an elliptical curve
     */
    private static String generateOvalPath(String text, Font font, double width, double height) {
        Text textNode = new Text(text);
        textNode.setFont(font);

        double radiusX = width / 2 * 0.8;
        double radiusY = height / 2 * 0.6;

        StringBuilder path = new StringBuilder();
        double angleStep = (2 * Math.PI) / text.length();
        double currentAngle = -Math.PI / 2;

        for (char c : text.toCharArray()) {
            double x = width / 2 + radiusX * Math.cos(currentAngle);
            double y = height / 2 + radiusY * Math.sin(currentAngle);
            double rotation = Math.toDegrees(currentAngle) + 90;

            path.append(createTransformedChar(c, font, x, y, rotation));
            currentAngle += angleStep;
        }

        return path.toString();
    }

    /**
     * Wave path - text follows a sine wave
     */
    private static String generateWavePath(String text, Font font, double width, double height) {
        Text textNode = new Text(text);
        textNode.setFont(font);

        double textWidth = textNode.getLayoutBounds().getWidth();
        double startX = (width - textWidth) / 2;
        double amplitude = height * 0.2;
        double frequency = 2.0; // Number of waves

        StringBuilder path = new StringBuilder();
        double x = startX;

        for (char c : text.toCharArray()) {
            Text charNode = new Text(String.valueOf(c));
            charNode.setFont(font);
            double charWidth = charNode.getLayoutBounds().getWidth();

            // Calculate wave position
            double progress = x / textWidth;
            double y = height / 2 + amplitude * Math.sin(progress * frequency * 2 * Math.PI);

            // Slight rotation based on wave slope
            double rotation = Math.toDegrees(Math.atan(
                    amplitude * frequency * 2 * Math.PI / textWidth *
                            Math.cos(progress * frequency * 2 * Math.PI)));

            path.append(createTransformedChar(c, font, x, y, rotation));
            x += charWidth;
        }

        return path.toString();
    }

    /**
     * Create SVG path for a transformed character
     */
    private static String createTransformedChar(char c, Font font, double x, double y, double rotation) {
        // This is a simplified version - in production, you'd convert the actual glyph
        // to SVG
        Text charNode = new Text(String.valueOf(c));
        charNode.setFont(font);

        return String.format(
                "<text x=\"%.2f\" y=\"%.2f\" transform=\"rotate(%.2f,%.2f,%.2f)\" " +
                        "font-family=\"%s\" font-size=\"%.0f\" fill=\"currentColor\">%s</text>",
                x, y, rotation, x, y,
                font.getFamily(), font.getSize(), c);
    }

    /**
     * Create simple SVG path for a character (without transformation)
     */
    private static String createCharPath(char c, Font font) {
        return String.format(
                "<text font-family=\"%s\" font-size=\"%.0f\" fill=\"currentColor\">%s</text>",
                font.getFamily(), font.getSize(), c);
    }
}

