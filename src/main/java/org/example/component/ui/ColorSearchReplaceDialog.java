package org.example.component.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.component.PrendaVisualizer;
import org.example.utils.UIFactory;

public class ColorSearchReplaceDialog {

    private final Stage stage;
    private final PrendaVisualizer visualizer;
    
    private final ObjectProperty<Color> sourceColor = new SimpleObjectProperty<>(Color.web("#3498db"));
    private final ObjectProperty<Color> targetColor = new SimpleObjectProperty<>(Color.web("#e91e63"));
    private double tolerance = 0.15;

    public ColorSearchReplaceDialog(Stage owner, PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
        this.stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("Buscar y Reemplazar Colores (Ctrl+F)");

        setupUI();
    }

    private void setupUI() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #ffffff;");
        root.setPrefWidth(380);

        // --- HEADER ---
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 25, 15, 25));
        header.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        
        Label title = new Label("Buscador de Colores");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(UIFactory.crearIcono("mdi2p-palette", 22, "#2563eb"), title, spacer);

        // --- CONTENT ---
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));

        // --- COLORS SECTION ---
        VBox colorsContainer = new VBox(15);
        
        // SOURCE
        VBox sourceBox = createColorSection("Color a buscar:", sourceColor);
        
        // SWAP BUTTON
        HBox swapContainer = new HBox();
        swapContainer.setAlignment(Pos.CENTER);
        Button btnSwap = new Button();
        btnSwap.setGraphic(UIFactory.crearIcono("mdi2s-swap-vertical", 20, "#64748b"));
        btnSwap.setTooltip(new Tooltip("Intercambiar Colores"));
        btnSwap.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 20; -fx-padding: 5; -fx-cursor: hand; -fx-border-color: #cbd5e1; -fx-border-radius: 20;");
        btnSwap.setOnAction(e -> {
            Color temp = sourceColor.get();
            sourceColor.set(targetColor.get());
            targetColor.set(temp);
        });
        swapContainer.getChildren().add(btnSwap);

        // TARGET
        VBox targetBox = createColorSection("Reemplazar por:", targetColor);

        colorsContainer.getChildren().addAll(sourceBox, swapContainer, targetBox);

        // --- TOLERANCE ---
        VBox tolBox = new VBox(8);
        Label lblTol = new Label("Tolerancia / Sensibilidad:");
        lblTol.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 12px;");
        
        Slider sliderTol = new Slider(0, 0.5, 0.15);
        sliderTol.setShowTickMarks(true);
        sliderTol.setMajorTickUnit(0.1);
        sliderTol.valueProperty().addListener((obs, old, val) -> this.tolerance = val.doubleValue());
        
        Label lblTolDesc = new Label("Ajuste si el color a buscar tiene sombras o degradados leves.");
        lblTolDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        lblTolDesc.setWrapText(true);

        tolBox.getChildren().addAll(lblTol, sliderTol, lblTolDesc);

        content.getChildren().addAll(colorsContainer, tolBox);

        // --- ACTIONS ---
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(0, 25, 25, 25));

        Button btnReplaceAll = new Button("Reemplazar Todo");
        btnReplaceAll.setGraphic(UIFactory.crearIcono("mdi2p-palette", 18, "white"));
        btnReplaceAll.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.2), 10, 0, 0, 3);");
        btnReplaceAll.setOnAction(e -> executeReplacement());

        Button btnClose = new Button("Cerrar");
        btnClose.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-border-width: 0; -fx-cursor: hand;");
        btnClose.setOnAction(e -> stage.close());

        actions.getChildren().addAll(btnClose, btnReplaceAll);

        root.getChildren().addAll(header, content, actions);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private VBox createColorSection(String labelText, ObjectProperty<Color> colorProp) {
        VBox box = new VBox(8);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-size: 13px;");
        
        HBox hb = new HBox(10);
        hb.setAlignment(Pos.CENTER_LEFT);
        
        MenuButton btnColor = UIFactory.createColorMenuButton(colorProp.get(), "", color -> {
            colorProp.set(color);
        }, (commit, preview) -> {
            visualizer.getShapeHelper().startEyedropperSession(true, color -> {
                commit.accept(color);
                colorProp.set(color);
            }, preview);
        });
        
        // Connect property
        colorProp.addListener((obs, old, val) -> {
             Object prop = btnColor.getProperties().get("colorProperty");
             if (prop instanceof ObjectProperty) {
                 ((ObjectProperty<Color>) prop).set(val);
             }
        });
        
        btnColor.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnColor, Priority.ALWAYS);
        
        Button btnEyedropper = new Button();
        btnEyedropper.setGraphic(UIFactory.crearIcono("mdi2e-eyedropper", 16, "#475569"));
        btnEyedropper.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10; -fx-border-color: #cbd5e1; -fx-border-radius: 8;");
        btnEyedropper.setOnAction(e -> {
            visualizer.getShapeHelper().startEyedropperSession(true, color -> {
                colorProp.set(color);
            }, null);
        });
        
        hb.getChildren().addAll(btnColor, btnEyedropper);
        box.getChildren().addAll(lbl, hb);
        return box;
    }

    private void executeReplacement() {
        int count = visualizer.replaceAllColors(sourceColor.get(), targetColor.get(), tolerance);
        
        if (count > 0) {
            UIFactory.mostrarAlerta(Alert.AlertType.INFORMATION, "Reemplazo Exitoso", 
                "Se han actualizado " + count + " elementos del diseño.");
            visualizer.reapplyColors();
        } else {
            UIFactory.mostrarAlerta(Alert.AlertType.WARNING, "Sin Coincidencias", 
                "No se encontró el color buscado en la prenda.\n" +
                "Pruebe:\n" +
                "1. Aumentar la 'Tolerancia'.\n" + 
                "2. Asegurarse de capturar el color con el gotero.");
        }
    }

    public void show() {
        if (!stage.isShowing()) {
            stage.show();
        } else {
            stage.toFront();
        }
    }
}
