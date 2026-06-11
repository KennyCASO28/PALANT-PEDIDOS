package org.example.controller.uicomponent;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.utils.IconUtils;

import java.util.function.Consumer;

/**
 * A custom popup content for selecting colors, including standard palette,
 * eyedropper, and custom color dialog.
 */
public class ColorPalettePopup extends VBox {

    private final Consumer<Color> onColorSelected;
    private final Runnable onEyedropperRequest;
    private final Runnable onCustomColorRequest;


    public ColorPalettePopup(Color initialColor, Consumer<Color> onColorSelected, Runnable onEyedropperRequest,
            Runnable onCustomColorRequest) {
        this.onColorSelected = onColorSelected;
        this.onEyedropperRequest = onEyedropperRequest;
        this.onCustomColorRequest = onCustomColorRequest;

        this.setSpacing(8);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-background-radius: 4; -fx-border-radius: 4; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        // 1. Full Professional Palette Grid
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setAlignment(Pos.CENTER);

        // A. Greyscale Row
        for (int i = 0; i < 12; i++) {
            double v = 1.0 - (i / 11.0);
            grid.add(createColorButton(Color.color(v, v, v)), i, 0);
        }

        // B. Organized Hues Matrix (Full Range for Combo/Popup)
        double[] hues = {0, 25, 45, 60, 90, 120, 160, 195, 225, 260, 290, 325};
        double[] sats = {0.9, 0.85, 0.8, 0.9, 0.8, 0.85, 0.8, 0.85, 0.9, 0.85, 0.8, 0.9};
        double[] brights = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
        
        for (int row = 0; row < brights.length; row++) {
            for (int col = 0; col < hues.length; col++) {
                grid.add(createColorButton(Color.hsb(hues[col], sats[col], brights[row])), col, row + 1);
            }
        }

        this.getChildren().add(grid);
        this.getChildren().add(new Separator());

        // 2. Action Tools (Eyedropper & Custom)
        HBox toolsBox = new HBox(8);
        toolsBox.setAlignment(Pos.CENTER);

        // Eyedropper Button
        Button btnEyedropper = new Button("Copiar");
        btnEyedropper.setGraphic(IconUtils.crearIcono("mdi2e-eyedropper", 16, "#475569"));
        btnEyedropper.setTooltip(new Tooltip("Copiar color de la pantalla (Cuentagotas)"));
        btnEyedropper.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-background-radius: 6; -fx-padding: 6 12;");
        btnEyedropper.setOnAction(e -> {
            if (onEyedropperRequest != null)
                onEyedropperRequest.run();
        });

        Button btnCustom = new Button("Más...");
        btnCustom.setGraphic(IconUtils.crearIcono("mdi2p-palette", 16, "#475569"));
        btnCustom.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-background-radius: 6; -fx-padding: 6 12;");
        btnCustom.setOnAction(e -> {
            if (onCustomColorRequest != null) {
                onCustomColorRequest.run();
            }
        });

        toolsBox.getChildren().addAll(btnEyedropper, btnCustom);
        this.getChildren().add(toolsBox);
    }

    // Pre-create shared effect for performance (prevents GC lag during rapid movement)
    private static final javafx.scene.effect.DropShadow SHARED_SHADOW = new javafx.scene.effect.DropShadow(5, Color.BLACK);

    private Button createColorButton(Color c) {
        Button btn = new Button();
        btn.setMinSize(18, 18);
        btn.setMaxSize(18, 18);
        btn.setPadding(Insets.EMPTY);
        
        Rectangle r = new Rectangle(18, 18, c);
        r.setStroke(Color.web("#e2e8f0"));
        r.setStrokeWidth(0.5);
        
        btn.setGraphic(r);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        
        btn.setOnAction(e -> {
            if (onColorSelected != null)
                onColorSelected.accept(c);
        });

        // Hover Effect: Zoom and Lift
        btn.setOnMouseEntered(ev -> {
            r.setStroke(Color.web("#94a3b8"));
            btn.setScaleX(1.3);
            btn.setScaleY(1.3);
            btn.setEffect(SHARED_SHADOW);
            btn.toFront(); // Key to overlapping neighbors
        });
        btn.setOnMouseExited(ev -> {
            r.setStroke(Color.web("#e2e8f0"));
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
            btn.setEffect(null);
        });

        return btn;
    }
}

