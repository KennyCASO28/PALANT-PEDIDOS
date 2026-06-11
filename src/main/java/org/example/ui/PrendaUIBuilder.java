package org.example.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Builder for creating UI components with consistent styling.
 * Eliminates duplicated code for button creation and layout configuration.
 */
public class PrendaUIBuilder {

    /**
     * Creates a toggle button with icon and responsive layout.
     * 
     * @param text     Button text
     * @param iconCode Material Design icon code
     * @param iconSize Icon size in pixels
     * @return Configured ToggleButton
     */
    public static ToggleButton createResponsiveToggleButton(String text, String iconCode, int iconSize) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(iconSize);

        ToggleButton btn = new ToggleButton(text, icon);
        btn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");

        // Responsive configuration
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefWidth(10); // Allow shrinking

        return btn;
    }

    /**
     * Creates a toggle button with SVG icon and responsive layout.
     * 
     * @param text    Button text
     * @param svgPath SVG path content
     * @return Configured ToggleButton
     */
    public static ToggleButton createResponsiveToggleButton(String text, SVGPath svgPath) {
        ToggleButton btn = new ToggleButton(text, svgPath);
        btn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");

        // Responsive configuration
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefWidth(10);

        return btn;
    }

    /**
     * Creates a section title label with consistent styling.
     * 
     * @param text Title text
     * @return Styled Label
     */
    public static Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-padding: 10 0 5 0;");
        return label;
    }

    /**
     * Creates an info label with consistent styling.
     * 
     * @param text Label text
     * @return Styled Label
     */
    public static Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 5 0 0 0;");
        return label;
    }

    /**
     * Creates a responsive HBox container with standard spacing.
     * 
     * @param spacing Spacing between children
     * @return Configured HBox
     */
    public static HBox createResponsiveHBox(double spacing) {
        HBox hbox = new HBox(spacing);
        hbox.setAlignment(Pos.CENTER_LEFT);
        return hbox;
    }

    /**
     * Applies responsive layout properties to a ToggleButton.
     * 
     * @param button Button to configure
     */
    public static void makeResponsive(ToggleButton button) {
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefWidth(10);
    }

    /**
     * Creates an SVGPath with standard configuration.
     * 
     * @param content SVG path content
     * @param fill    Fill color
     * @param scaleX  X scale factor
     * @param scaleY  Y scale factor
     * @return Configured SVGPath
     */
    public static SVGPath createSVGIcon(String content, Color fill, double scaleX, double scaleY) {
        SVGPath svg = new SVGPath();
        svg.setContent(content);
        svg.setFill(fill);
        svg.setScaleX(scaleX);
        svg.setScaleY(scaleY);
        return svg;
    }
}

