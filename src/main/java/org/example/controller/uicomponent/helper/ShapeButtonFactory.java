package org.example.controller.uicomponent.helper;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.controller.uicomponent.ColorPalettePopup;
import org.example.model.ShapeType;
import org.example.utils.UIFactory;

import java.util.function.Consumer;

/**
 * Factory for creating UI controls used by ShapeManagerController.
 * Extracts button creation, color pickers, and menu building into reusable methods.
 */
public class ShapeButtonFactory {

    private final ShapeManagerUIOrchestrator uiOrchestrator;
    private ToggleGroup toolsGroup;

    public ShapeButtonFactory(ShapeManagerUIOrchestrator uiOrchestrator) {
        this.uiOrchestrator = uiOrchestrator;
    }

    public ToggleGroup getOrCreateToolsGroup() {
        if (toolsGroup == null) toolsGroup = new ToggleGroup();
        return toolsGroup;
    }

    public void updatePickerGraphic(MenuButton picker, Color c) {
        if (picker == null) return;
        picker.setUserData(c);
        String iconName = (String) picker.getProperties().get("iconName");
        HBox graphic = new HBox(4);
        graphic.setAlignment(Pos.CENTER_LEFT);
        Node icon = UIFactory.crearIcono(iconName != null ? iconName : "mdi2f-format-color-fill", 16, "#2c3e50");
        Node colorRect = uiOrchestrator.createColorGraphic(c);
        graphic.getChildren().addAll(icon, colorRect);
        picker.setGraphic(graphic);
    }

    public MenuButton createColorMenuButton(String icon, Color initial, String tooltip,
                                             Consumer<Color> onColor, Consumer<Color> onPickerUpdate,
                                             Runnable onEyedropper, Runnable onAdvanced) {
        MenuButton btn = new MenuButton();
        btn.getProperties().put("iconName", icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-mark-color: transparent;");
        btn.setUserData(initial);
        updatePickerGraphic(btn, initial);
        CustomMenuItem item = new CustomMenuItem(
            new ColorPalettePopup(initial,
                c -> { updatePickerGraphic(btn, c); onColor.accept(c); btn.hide(); },
                () -> onEyedropper.run(),
                () -> { btn.hide(); onAdvanced.run(); }
            ));
        item.setHideOnClick(false);
        btn.getItems().add(item);
        return btn;
    }

    public ToggleButton createVerticalToolButton(String icon, String tooltip) {
        ToggleButton btn = new ToggleButton();
        configureAsToolButton(btn, icon, tooltip, null);
        return btn;
    }

    public void configureAsToolButton(ToggleButton btn, String icon, String tooltip, String userData) {
        btn.setGraphic(UIFactory.crearIcono(icon, 16, "#2c3e50"));
        btn.setTooltip(new Tooltip(tooltip));
        btn.setToggleGroup(getOrCreateToolsGroup());
        if (userData != null) btn.setUserData(userData);
        btn.setMinSize(28, 28);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4;");
        btn.selectedProperty().addListener((obs, old, val) -> {
            btn.setStyle(val
                ? "-fx-background-color: #d6eaf8; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4;"
                : "-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4;");
            btn.setGraphic(UIFactory.crearIcono(icon, 16, val ? "#3498db" : "#2c3e50"));
        });
    }

    public ContextMenu createBrushSizeMenu(javafx.beans.property.DoubleProperty property, String label) {
        ContextMenu menu = new ContextMenu();
        VBox box = new VBox(5);
        box.setPadding(new javafx.geometry.Insets(5));
        Slider sl = new Slider(1, 100, property.get());
        Label val = new Label(String.format("%.0f", sl.getValue()));
        sl.valueProperty().bindBidirectional(property);
        sl.valueProperty().addListener((o, old, v) -> val.setText(String.format("%.0f", v.doubleValue())));
        box.getChildren().addAll(new Label(label), sl, val);
        CustomMenuItem item = new CustomMenuItem(box);
        item.setHideOnClick(false);
        menu.getItems().add(item);
        return menu;
    }

    public String getIconForShape(ShapeType type) {
        return switch (type) {
            case RECTANGLE -> "mdi2r-rectangle-outline";
            case CIRCLE -> "mdi2c-circle-outline";
            case TRIANGLE -> "mdi2t-triangle-outline";
            case STAR -> "mdi2s-star-outline";
            case PENTAGON -> "mdi2p-pentagon-outline";
            case HEXAGON -> "mdi2h-hexagon-outline";
            case CUSTOM_PATH -> "mdi2p-pen";
            default -> "mdi2s-shape-outline";
        };
    }

    public void updateCurrentShapeButton(ToggleButton btnCurrentShape, ShapeType currentShapeType) {
        if (btnCurrentShape != null) {
            btnCurrentShape.setGraphic(UIFactory.crearIcono(
                getIconForShape(currentShapeType), 16,
                btnCurrentShape.isSelected() ? "#3498db" : "#2c3e50"));
            btnCurrentShape.setTooltip(new Tooltip(
                "Forma: " + currentShapeType.getDisplayName() + " (Click derecho para cambiar)"));
            btnCurrentShape.setUserData(currentShapeType);
        }
    }

    public void styleToolButton(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 4; -fx-background-insets: 0;");
        btn.setMinSize(28, 28);
    }
}
