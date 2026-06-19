package org.example.controller.uicomponent;

import javafx.geometry.Insets;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.component.PrendaVisualizer;
import org.example.utils.UIFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GarmentColorController {

    private final PrendaVisualizer visualizer;
    private final TitledPane pane;
    private final VBox colorsContainer;
    private final ToggleGroup typeGroup;
    private final BiConsumer<Consumer<Color>, Consumer<Color>> eyedropperCallback; // Callback to launch eyedropper

    private ToggleButton btnShirt;
    private ToggleButton btnShort;
    private ToggleButton btnSocks;
    private boolean isUpdatingUI = false;

    public GarmentColorController(PrendaVisualizer visualizer,
            BiConsumer<Consumer<Color>, Consumer<Color>> eyedropperCallback) {
        this.visualizer = visualizer;
        this.eyedropperCallback = eyedropperCallback;

        this.colorsContainer = new VBox(6);
        this.typeGroup = new ToggleGroup();
        this.pane = createPane();
    }

    public TitledPane getPane() {
        return pane;
    }

    private TitledPane createPane() {
        TitledPane pane = new TitledPane();
        pane.setText("1. Colores de la Prenda");
        pane.setGraphic(UIFactory.crearIcono("mdi2p-palette", 18, "#2c3e50"));
        pane.getStyleClass().add("modern-pane");

        // Segmented Selector
        HBox segmentedBox = new HBox(8);
        segmentedBox.setAlignment(javafx.geometry.Pos.CENTER);
        segmentedBox.setMaxWidth(Double.MAX_VALUE);

        this.btnShirt = UIFactory.crearBotonOpcion("Camiseta", "mdi2t-tshirt-crew", 20);
        this.btnShort = UIFactory.crearBotonOpcion("Short", "mdi2v-view-column", 20);
        this.btnSocks = UIFactory.crearBotonOpcion("Medias", "mdi2f-foot-print", 20);

        // Adjust for Segmented Look (Smaller height than default)
        configureSegmentedButton(btnShirt, "Camiseta");
        configureSegmentedButton(btnShort, "Short");
        configureSegmentedButton(btnSocks, "Medias");

        btnShirt.setSelected(true);
        segmentedBox.getChildren().addAll(btnShirt, btnShort, btnSocks);
        HBox.setHgrow(btnShirt, Priority.ALWAYS);
        HBox.setHgrow(btnShort, Priority.ALWAYS);
        HBox.setHgrow(btnSocks, Priority.ALWAYS);

        typeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true); // Don't allow deselection
            } else {
                updateUI();
            }
        });

        // Use a VBox for rows so they can flex properly without overflowing
        colorsContainer.setPadding(new Insets(15, 0, 0, 0));
        colorsContainer.setStyle("-fx-background-color: white;");
        colorsContainer.setFillWidth(true);

        VBox container = new VBox(10);
        container.setPadding(new Insets(10, 15, 10, 15)); // Added side padding to prevent clipping
        container.setStyle("-fx-background-color: white;");
        container.setFillWidth(true);
        container.getChildren().addAll(segmentedBox, colorsContainer);

        pane.setContent(container);

        // Auto-update when expanded
        pane.expandedProperty().addListener((o, old, expanded) -> {
            if (expanded)
                updateUI();
        });

        return pane;
    }

    private void configureSegmentedButton(ToggleButton btn, String id) {
        btn.setToggleGroup(typeGroup);
        btn.setUserData(id);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setWrapText(true);
        btn.getProperties().put("customFontSize", "12px");
        UIFactory.updateButtonStyle(btn); // Apply immediately
    }

    // Flag to prevent rebuilding UI when the change comes from the picker itself
    private boolean isSelfTriggered = false;

    public void updateUI() {
        if (isUpdatingUI)
            return; // Prevent recursion during build

        isUpdatingUI = true;
        try {
            doUpdateUI();
        } finally {
            isUpdatingUI = false;
        }
    }

    private void doUpdateUI() {
        if (isSelfTriggered) return; // Keep current controls to avoid losing focus/popup state during drag
        
        // --- Smart Visibility Logic ---
        boolean hasShirt = visualizer.hasShirt();
        boolean hasShort = visualizer.hasShorts();
        boolean hasSocks = visualizer.hasSocks();

        btnShirt.setVisible(hasShirt);
        btnShirt.setManaged(hasShirt);
        btnShort.setVisible(hasShort);
        btnShort.setManaged(hasShort);
        btnSocks.setVisible(hasSocks);
        btnSocks.setManaged(hasSocks);

        // Auto-selection if current is hidden
        ToggleButton current = (ToggleButton) typeGroup.getSelectedToggle();
        if (current != null && !current.isVisible()) {
            if (hasShirt)
                btnShirt.setSelected(true);
            else if (hasShort)
                btnShort.setSelected(true);
            else if (hasSocks)
                btnSocks.setSelected(true);
        }

        colorsContainer.getChildren().clear();
        int row = 0;

        // Update Dynamic Labels
        String shortLabel = "Short"; // Default
        if (hasShort && btnShort != null) {
            if (visualizer.getGenero() == org.example.model.TipoGenero.MUJER) {
                org.example.model.TipoCorte shortType = visualizer.getCurrentCorteShort();
                if (shortType != null) {
                    if (shortType == org.example.model.TipoCorte.LICRA) {
                        shortLabel = "Licra";
                    } else if (shortType == org.example.model.TipoCorte.PANTALONETA) {
                        shortLabel = "Pantaloneta";
                    }
                }
            }
            btnShort.setText(shortLabel);
        }

        ToggleButton selected = (ToggleButton) typeGroup.getSelectedToggle();
        String selection = (selected != null) ? (String) selected.getUserData() : "Camiseta";

        if ("Camiseta".equals(selection) && visualizer.hasShirt()) {
            addColorControl(row++, "Cuerpo", "body", visualizer.getPartColor("body", Color.WHITE),
                    visualizer::setColorBase);
            if (visualizer.hasSleeves()) {
                addColorControl(row++, "Mangas", "sleeves", visualizer.getPartColor("sleeves", Color.WHITE),
                        visualizer::setSleevesColor);
            }
            addColorControl(row++, "Cuello", "collar", visualizer.getPartColor("collar", Color.web("#95a5a6")),
                    visualizer::setCollarColor);

            if (visualizer.hasCuffs()) {
                addColorControl(row++, "Puños", "cuff", visualizer.getPartColor("cuff", Color.web("#95a5a6")),
                        visualizer::setCuffColor);
            }
            if (visualizer.hasMesh()) {
                addColorControl(row++, "Malla", "mesh", visualizer.getPartColor("mesh", Color.web("#95a5a6")),
                        visualizer::setMeshColor);
            }
            // ADDED: Shirt Stripe Control
            if (visualizer.hasShirtStripe()) {
                addColorControl(row++, "Franja", "shirtStripe", visualizer.getPartColor("shirtStripe", Color.web("#95a5a6")),
                        visualizer::setShirtStripeColor);
            }
            // ADDED: Shirt Linea Control
            if (visualizer.hasShirtLinea()) {
                addColorControl(row++, "Líneas", "shirtLinea", visualizer.getPartColor("shirtLinea", Color.web("#95a5a6")),
                        visualizer::setShirtLineaColor);
            }
        }

        if ("Short".equals(selection) && visualizer.hasShorts()) {
            // Use Dynamic Label
            addColorControl(row++, shortLabel + " Base", "shorts", visualizer.getPartColor("shorts", Color.WHITE),
                    visualizer::setShortsColor);
            if (visualizer.hasShortsStripe()) {
                addColorControl(row++, "Franjas", "shortsStripe",
                        visualizer.getPartColor("shortsStripe", Color.web("#95a5a6")),
                        visualizer::setShortsStripeColor);
            }
            if (visualizer.hasShortsPicket()) {
                addColorControl(row++, "Piquetes", "shortsPicket",
                        visualizer.getPartColor("shortsPicket", Color.web("#95a5a6")),
                        visualizer::setShortsPicketColor);
            }
            if (visualizer.hasShortsCuff()) {
                addColorControl(row++, "Puños " + shortLabel, "shortsCuff",
                        visualizer.getPartColor("shortsCuff", Color.web("#95a5a6")),
                        visualizer::setShortsCuffColor);
            }
            // ADDED: Short Linea Control
            if (visualizer.hasShortsLinea()) {
                addColorControl(row++, "Líneas", "shortsLinea",
                        visualizer.getPartColor("shortsLinea", Color.web("#95a5a6")),
                        visualizer::setShortsLineaColor);
            }
        }

        if ("Medias".equals(selection) && visualizer.hasSocks()) {
            addColorControl(row++, "Medias", "socks", visualizer.getPartColor("socks", Color.WHITE),
                    visualizer::setSocksBaseColor);

            if (visualizer.hasSocksTop()) {
                addColorControl(row++, "Borde Superior", "socksTop",
                        visualizer.getPartColor("socksTop", Color.web("#ecf0f1")),
                        visualizer::setSocksTopColor);
            }

            addColorControl(row++, "Detalles", "socksDetail", visualizer.getPartColor("socksDetail", Color.BLACK),
                    visualizer::setSocksDetailColor);
        }
    }

    private void addColorControl(int row, String label, String key, Color initialColor, Consumer<Color> onColorChange) {
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        lbl.setPrefWidth(72);
        lbl.setMinWidth(72);
        lbl.setMaxWidth(72);

        final MenuButton[] pickerBtnArr = new MenuButton[1];
        MenuButton pickerBtn = UIFactory.createColorMenuButton(initialColor, "Seleccionar color",
                c -> {
                    if (!isUpdatingUI) {
                        Color oldColor = visualizer.getPartColor(key, Color.WHITE);
                        if (c != null && !c.equals(oldColor)) {
                            // Record History
                            visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                                    label + " Change", oldColor, c,
                                    newVal -> {
                                        UIFactory.setColorMenuButtonColor(pickerBtnArr[0], newVal);
                                        onColorChange.accept(newVal);
                                        updateUI();
                                    }));
                            
                            try {
                                onColorChange.accept(c);
                            } finally {
                                // Rebuild UI to ensure ComboBox shows the finalized (and potentially sanitized) color
                                updateUI();
                            }
                        }
                    }
                },
                (onCommit, onPreview) -> {
                    eyedropperCallback.accept(
                        color -> {
                            if (color == null) {
                                visualizer.clearPreviewColors();
                                return;
                            }
                            visualizer.clearPreviewColors();
                            
                            Color oldColor = visualizer.getPartColor(key, Color.WHITE);
                            if (!color.equals(oldColor)) {
                                // Record History
                                visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                                        label + " Change", oldColor, color,
                                        newVal -> {
                                            UIFactory.setColorMenuButtonColor(pickerBtnArr[0], newVal);
                                            onColorChange.accept(newVal);
                                            updateUI();
                                        }));
                                onCommit.accept(color);
                            }
                        },
                        color -> {
                            if (color == null) {
                                visualizer.clearPreviewColors();
                            } else {
                                visualizer.setPreviewColor(key, color);
                            }
                        }
                    );
                });
        pickerBtnArr[0] = pickerBtn;
        pickerBtn.setMaxWidth(Double.MAX_VALUE);
        pickerBtn.setMinWidth(30); // Allow shrinking safely

        Button btnCode = new Button();
        String existingCode = visualizer.getInternalCode(key);
        if (existingCode != null && !existingCode.isEmpty()) {
            btnCode.setText(existingCode);
            btnCode.setStyle("-fx-background-color: #dff9fb; -fx-text-fill: #2c3e50; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 3; -fx-border-color: #7f8c8d; -fx-border-radius: 3; -fx-font-weight: bold;");
        } else {
            btnCode.setGraphic(UIFactory.crearIcono("mdi2t-tag-text-outline", 14, "#95a5a6"));
            btnCode.setStyle("-fx-background-color: #ecf0f1; -fx-cursor: hand; -fx-padding: 4 6; -fx-background-radius: 3; -fx-border-color: #bdc3c7; -fx-border-radius: 3;");
        }

        btnCode.setTooltip(new javafx.scene.control.Tooltip("Asignar código interno (ej. 095)"));
        btnCode.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(visualizer.getInternalCode(key));
            dialog.setTitle("Código Interno");
            dialog.setHeaderText("Código para: " + label);
            dialog.setContentText("Ingrese el código (ej. 095):");
            UIFactory.estilizarDialogo(dialog);

            dialog.showAndWait().ifPresent(code -> {
                visualizer.setInternalCode(key, code);
                if (code != null && !code.trim().isEmpty()) {
                    btnCode.setText(code);
                    btnCode.setGraphic(null);
                    btnCode.setStyle("-fx-background-color: #dff9fb; -fx-text-fill: #2c3e50; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 3; -fx-border-color: #7f8c8d; -fx-border-radius: 3; -fx-font-weight: bold;");
                } else {
                    btnCode.setText("");
                    btnCode.setGraphic(UIFactory.crearIcono("mdi2t-tag-text-outline", 14, "#95a5a6"));
                    btnCode.setStyle("-fx-background-color: #ecf0f1; -fx-cursor: hand; -fx-padding: 4 6; -fx-background-radius: 3; -fx-border-color: #bdc3c7; -fx-border-radius: 3;");
                }
            });
        });

        HBox tools = new HBox(2, btnCode);
        tools.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        tools.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);

        HBox rowBox = new HBox(10); // Increased spacing for breathing room
        rowBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        rowBox.setStyle("-fx-padding: 4 0; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");

        HBox.setHgrow(pickerBtn, Priority.ALWAYS);

        lbl.setWrapText(true);
        
        rowBox.getChildren().addAll(lbl, pickerBtn, tools);
        colorsContainer.getChildren().add(rowBox);
    }
}
