package org.example.controller.uicomponent;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.component.TextLayer;
import org.example.utils.UIFactory;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * Modernized Programmatic Text Manager Controller.
 * Focal point: Quick trajectory presets and essential styling.
 * Restored compatibility with PersonalizacionDelegate (Programmatic UI).
 */
public class TextManagerController {

    private final VBox container;
    private TextField textField;
    private ComboBox<String> fontSelector;
    private MenuButton textColorPicker; // Changed from ColorPicker to MenuButton
    private FlowPane galleryContainer;
    private Slider spacingSlider;
    private MenuButton strokeColorPicker;
    private Slider strokeWidthSlider;
    private ToggleButton btnBold;
    private ToggleButton btnItalic;

    // Nuevos controles solicitados
    private Slider fontSizeSlider;
    private ToggleGroup alignmentGroup;
    private ToggleButton btnAlignLeft;
    private ToggleButton btnAlignCenter;
    private ToggleButton btnAlignRight;
    private CheckBox dropShadowCheck;
    private MenuButton shadowColorPicker;

    private final org.example.component.PrendaVisualizer visualizer;
    private final BiConsumer<Consumer<Color>, Consumer<Color>> eyedropperStarter;
    private TextLayer activeTextLayer;
    private boolean isUpdatingUI = false;
    private List<String> allFontFamilies;

    // Constructor requested by PersonalizacionDelegate
    public TextManagerController(org.example.component.PrendaVisualizer visualizer,
            BiConsumer<Consumer<Color>, Consumer<Color>> eyedropperStarter) {
        this.visualizer = visualizer;
        this.eyedropperStarter = eyedropperStarter;
        this.container = new VBox(10);
        buildUI();
        initLogic();
    }

    private void buildUI() {
        container.setPadding(new Insets(15));
        container.setSpacing(12);
        container.setFillWidth(true);
        container.getStyleClass().add("text-manager-panel");
        container.setStyle("-fx-background-color: white;");

        // --- 0. AGREGAR TEXTO ---
        Button btnAddText = new Button("AGREGAR TEXTO");
        btnAddText.setGraphic(UIFactory.crearIcono("mdi2p-plus-circle", 18, "white"));
        btnAddText.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12; -fx-background-radius: 10; -fx-font-size: 14px;");
        btnAddText.setMaxWidth(Double.MAX_VALUE);
        btnAddText.setOnAction(e -> addNewTextLayer());

        // --- 1. PROPIEDADES (Ordered Stack) ---
        VBox propBox = new VBox(10);

        // Input
        VBox inputGrp = new VBox(4);
        Label lblText = new Label("CONTENIDO:");
        lblText.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 11px;");
        textField = new TextField();
        textField.setPromptText("Escribe aquí...");
        textField.getStyleClass().add("modern-textfield");
        inputGrp.getChildren().addAll(lblText, textField);

        // Typography (Searchable)
        VBox fontGrp = new VBox(6);
        Label lblFont = new Label("TIPOGRAFÍA:");
        lblFont.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 11px;");

        fontSelector = new ComboBox<>();
        fontSelector.setPromptText("Buscar Fuente...");
        fontSelector.setEditable(true);
        fontSelector.setMaxWidth(Double.MAX_VALUE);
        fontSelector.getStyleClass().add("modern-combo-box");
        UIFactory.fixComboBoxReadability(fontSelector);

        // Style and Alignment Row below Typography
        HBox styleRow = new HBox(8);
        styleRow.setAlignment(Pos.CENTER_LEFT);

        btnBold = new ToggleButton();
        btnBold.setGraphic(UIFactory.crearIcono("mdi2f-format-bold", 16, "#475569"));
        btnBold.setStyle("-fx-cursor: hand; -fx-padding: 4 8;");

        btnItalic = new ToggleButton();
        btnItalic.setGraphic(UIFactory.crearIcono("mdi2f-format-italic", 16, "#475569"));
        btnItalic.setStyle("-fx-cursor: hand; -fx-padding: 4 8;");

        // Alignment
        alignmentGroup = new ToggleGroup();
        btnAlignLeft = new ToggleButton();
        btnAlignLeft.setGraphic(UIFactory.crearIcono("mdi2f-format-align-left", 16, "#475569"));
        btnAlignLeft.setToggleGroup(alignmentGroup);
        btnAlignLeft.setStyle("-fx-cursor: hand; -fx-padding: 4 8;");
        btnAlignLeft.setUserData(javafx.scene.text.TextAlignment.LEFT);

        btnAlignCenter = new ToggleButton();
        btnAlignCenter.setGraphic(UIFactory.crearIcono("mdi2f-format-align-center", 16, "#475569"));
        btnAlignCenter.setToggleGroup(alignmentGroup);
        btnAlignCenter.setStyle("-fx-cursor: hand; -fx-padding: 4 8;");
        btnAlignCenter.setUserData(javafx.scene.text.TextAlignment.CENTER);

        btnAlignRight = new ToggleButton();
        btnAlignRight.setGraphic(UIFactory.crearIcono("mdi2f-format-align-right", 16, "#475569"));
        btnAlignRight.setToggleGroup(alignmentGroup);
        btnAlignRight.setStyle("-fx-cursor: hand; -fx-padding: 4 8;");
        btnAlignRight.setUserData(javafx.scene.text.TextAlignment.RIGHT);

        styleRow.getChildren().addAll(btnBold, btnItalic, new Separator(javafx.geometry.Orientation.VERTICAL),
                btnAlignLeft, btnAlignCenter, btnAlignRight);

        fontGrp.getChildren().addAll(lblFont, fontSelector, styleRow);

        // Colors (Two rows or stack)
        VBox colorGrp = new VBox(8);

        HBox textClrRow = new HBox(10);
        textClrRow.setAlignment(Pos.CENTER_LEFT);
        Label lblTextColor = new Label("COLOR LETRA:");
        lblTextColor.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 11px;");
        lblTextColor.setPrefWidth(88);
        lblTextColor.setMinWidth(88);
        lblTextColor.setWrapText(true);
        textColorPicker = UIFactory.createColorMenuButton(Color.BLACK, "Elegir Color",
                color -> {
                    if (activeTextLayer != null)
                        activeTextLayer.setTextColor(color);
                }, eyedropperStarter);
        textColorPicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textColorPicker, Priority.ALWAYS);
        textClrRow.getChildren().addAll(lblTextColor, textColorPicker);

        HBox strokeClrRow = new HBox(10);
        strokeClrRow.setAlignment(Pos.CENTER_LEFT);
        Label lblStrokeColor = new Label("COLOR BORDE:");
        lblStrokeColor.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 11px;");
        lblStrokeColor.setPrefWidth(88);
        lblStrokeColor.setMinWidth(88);
        lblStrokeColor.setWrapText(true);
        strokeColorPicker = UIFactory.createColorMenuButton(Color.TRANSPARENT, "Elegir Color",
                color -> {
                    if (activeTextLayer != null)
                        activeTextLayer.setStrokeColor(color);
                }, eyedropperStarter);
        strokeColorPicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(strokeColorPicker, Priority.ALWAYS);
        strokeClrRow.getChildren().addAll(lblStrokeColor, strokeColorPicker);

        // Shadow Checkbox & ColorPicker
        HBox shadowClrRow = new HBox(10);
        shadowClrRow.setAlignment(Pos.CENTER_LEFT);
        dropShadowCheck = new CheckBox("SOMBRA:");
        dropShadowCheck.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 11px;");
        dropShadowCheck.setPrefWidth(88);
        dropShadowCheck.setMinWidth(88);
        shadowColorPicker = UIFactory.createColorMenuButton(Color.BLACK, "Color Sombra",
                color -> {
                    if (activeTextLayer != null)
                        activeTextLayer.setDropShadowColor(color);
                }, eyedropperStarter);
        shadowColorPicker.setMaxWidth(Double.MAX_VALUE);
        shadowColorPicker.setDisable(true);
        HBox.setHgrow(shadowColorPicker, Priority.ALWAYS);
        shadowClrRow.getChildren().addAll(dropShadowCheck, shadowColorPicker);

        VBox strokeBox = new VBox(2);
        HBox strokeLblBox = new HBox();
        Label lblStrokeWidth = new Label("ANCHO DE BORDE:");
        lblStrokeWidth.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 10px;");
        Label valStroke = new Label("0.0");
        valStroke.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 10px; -fx-font-weight: bold;");
        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);
        strokeLblBox.getChildren().addAll(lblStrokeWidth, sp3, valStroke);
        strokeWidthSlider = new Slider(0, 6, 0);
        strokeWidthSlider.valueProperty()
                .addListener((obs, old, val) -> valStroke.setText(String.format("%.1f", val.doubleValue())));
        strokeBox.getChildren().addAll(strokeLblBox, strokeWidthSlider);

        colorGrp.getChildren().addAll(textClrRow, strokeClrRow, strokeBox);

        // Sliders (Removed Sombra, Tamaño de fuente, Separación, Grosor contorno as requested)
        VBox sliderGrp = new VBox(8);

        VBox fontSizeBox = new VBox(2);
        HBox fontLblBox = new HBox();
        Label lblFontSize = new Label("TAMAÑO DE FUENTE:");
        lblFontSize.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 10px;");
        Label valFontSize = new Label("100%");
        valFontSize.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 10px; -fx-font-weight: bold;");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        fontLblBox.getChildren().addAll(lblFontSize, sp1, valFontSize);
        fontSizeSlider = new Slider(10, 200, 100);
        fontSizeSlider.valueProperty()
                .addListener((obs, old, val) -> valFontSize.setText(String.format("%.0f%%", val.doubleValue())));
        fontSizeBox.getChildren().addAll(fontLblBox, fontSizeSlider);

        VBox spacingBox = new VBox(2);
        HBox spacingLblBox = new HBox();
        Label lblSpacing = new Label("SEPARACIÓN LETRAS:");
        lblSpacing.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 10px;");
        Label valSpacing = new Label("0.0");
        valSpacing.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 10px; -fx-font-weight: bold;");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        spacingLblBox.getChildren().addAll(lblSpacing, sp2, valSpacing);
        spacingSlider = new Slider(-5, 20, 0);
        spacingSlider.valueProperty()
                .addListener((obs, old, val) -> valSpacing.setText(String.format("%.1f", val.doubleValue())));
        spacingBox.getChildren().addAll(spacingLblBox, spacingSlider);

        sliderGrp.getChildren().addAll(fontSizeBox, spacingBox);

        propBox.getChildren().addAll(inputGrp, fontGrp, colorGrp);
        VBox propCard = UIFactory.crearSeccionTarjeta("PROPIEDADES", "mdi2f-format-list-bulleted-type", propBox);

        // --- 2. TRAYECTORIA ---
        VBox trajBox = new VBox(10);

        Button btnOpenTrajectory = new Button("Forma de Texto");
        btnOpenTrajectory.setGraphic(UIFactory.crearIcono("mdi2f-format-text-variant-outline", 20, "white"));
        btnOpenTrajectory.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 14px;");
        btnOpenTrajectory.setMaxWidth(Double.MAX_VALUE);
        btnOpenTrajectory.setOnAction(e -> {
            if (activeTextLayer != null) {
                new org.example.component.TextTrajectoryDialog(activeTextLayer).show();
            }
        });

        Button btnConvertToVector = new Button("Convertir a Curvas");
        btnConvertToVector.setGraphic(UIFactory.crearIcono("mdi2v-vector-curve", 20, "white"));
        btnConvertToVector.setStyle(
                "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10; -fx-background-radius: 8; -fx-font-size: 14px;");
        btnConvertToVector.setMaxWidth(Double.MAX_VALUE);
        btnConvertToVector.setOnAction(e -> {
            if (activeTextLayer != null && visualizer != null) {
                List<org.example.component.ShapeLayer> vectorLayers = org.example.component.helper.TextVectorizationHelper
                        .convertToShapeLayers(activeTextLayer);
                if (!vectorLayers.isEmpty()) {
                    String activeZone = activeTextLayer.getActiveZone();
                    javafx.scene.Group parent = activeTextLayer.getParent() instanceof javafx.scene.Group
                            ? (javafx.scene.Group) activeTextLayer.getParent()
                            : null;
                    int insertionIndex = parent != null ? parent.getChildren().indexOf(activeTextLayer) : -1;

                    visualizer.getLayerManager().removeLayer(activeTextLayer);

                    for (int i = 0; i < vectorLayers.size(); i++) {
                        org.example.component.ShapeLayer vectorLayer = vectorLayers.get(i);
                        vectorLayer.setActiveZone(activeZone);
                        if (parent != null) {
                            visualizer.addShapeLayerToContainer(vectorLayer, parent, insertionIndex + i, false);
                        } else {
                            visualizer.addShapeLayer(vectorLayer);
                        }
                    }

                    visualizer.getLayerManager().clearSelection();
                    for (org.example.component.ShapeLayer vectorLayer : vectorLayers) {
                        visualizer.getLayerManager().addToSelection(vectorLayer);
                    }

                    org.example.utils.UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.INFORMATION,
                            "Vectorizacion Exitosa",
                            "El texto se convirtio a letras vectoriales independientes con relleno y sin contorno.");
                    return;
                }
                org.example.component.ShapeLayer vectorLayer = org.example.component.helper.TextVectorizationHelper
                        .convertToShapeLayer(activeTextLayer);
                if (vectorLayer != null) {
                    String activeZone = activeTextLayer.getActiveZone();
                    visualizer.getLayerManager().removeLayer(activeTextLayer);
                    vectorLayer.setActiveZone(activeZone);
                    visualizer.addShapeLayer(vectorLayer);
                    org.example.utils.UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.INFORMATION,
                            "Vectorización Exitosa",
                            "El texto se ha convertido a formas vectoriales. Ya no es editable como texto.");
                }
            }
        });

        trajBox.getChildren().addAll(btnOpenTrajectory, btnConvertToVector);
        VBox trajCard = UIFactory.crearSeccionTarjeta("FORMA Y TRAYECTORIA", "mdi2v-vector-curve", trajBox);

        // --- 3. GALLERY ---
        VBox galleryBox = new VBox(8);
        Label lblLayers = new Label("Capas en este Diseño:");
        lblLayers.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 11px;");
        galleryContainer = new FlowPane(10, 10);
        galleryContainer.setPadding(new Insets(10));
        galleryContainer.setPrefWrapLength(320);
        galleryContainer.setStyle(
                "-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-style: dashed; -fx-border-radius: 10;");
        galleryContainer.setMinHeight(80);
        galleryBox.getChildren().addAll(lblLayers, galleryContainer);

        container.getChildren().addAll(btnAddText, propCard, trajCard, galleryBox);
    }

    private void initLogic() {
        // Font Selector with DEBOUNCE for better performance
        allFontFamilies = Font.getFamilies();
        fontSelector.getItems().setAll(allFontFamilies);

        // Debounce timer for font filtering
        javafx.animation.PauseTransition fontFilterDebounce = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(150));
        fontFilterDebounce.setOnFinished(e -> {
            if (isUpdatingUI)
                return;
            String val = fontSelector.getEditor().getText();
            if (val == null || val.trim().isEmpty()) {
                if (fontSelector.getItems().size() != allFontFamilies.size()) {
                    fontSelector.getItems().setAll(allFontFamilies);
                }
            } else {
                String filter = val.toLowerCase();
                List<String> filtered = allFontFamilies.stream()
                        .filter(f -> f.toLowerCase().contains(filter))
                        .toList();
                if (fontSelector.getItems().size() != filtered.size() ||
                        !fontSelector.getItems().equals(filtered)) {
                    fontSelector.getItems().setAll(filtered);
                }
            }
        });

        // Real-time Filtering & Selection with Debounce
        fontSelector.getEditor().textProperty().addListener((obs, old, val) -> {
            if (isUpdatingUI)
                return;
            fontFilterDebounce.stop();
            fontFilterDebounce.playFromStart();
        });

        fontSelector.setOnAction(e -> {
            if (activeTextLayer != null && !isUpdatingUI) {
                String selectedFont = fontSelector.getEditor().getText();
                if (selectedFont != null && !selectedFont.trim().isEmpty()) {
                    activeTextLayer.setFont(new Font(selectedFont, activeTextLayer.getFont().getSize()));
                }
            }
        });

        // Style Logic
        btnBold.setOnAction(e -> {
            if (activeTextLayer != null && !isUpdatingUI) {
                activeTextLayer.setBold(btnBold.isSelected());
            }
        });
        btnItalic.setOnAction(e -> {
            if (activeTextLayer != null && !isUpdatingUI) {
                activeTextLayer.setItalic(btnItalic.isSelected());
            }
        });

        alignmentGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            if (activeTextLayer != null && !isUpdatingUI && val != null) {
                javafx.scene.text.TextAlignment align = (javafx.scene.text.TextAlignment) val.getUserData();
                activeTextLayer.setTextAlignment(align);
            }
        });

        dropShadowCheck.selectedProperty().addListener((obs, old, val) -> {
            shadowColorPicker.setDisable(!val);
            if (activeTextLayer != null && !isUpdatingUI) {
                activeTextLayer.setDropShadowEnabled(val);
            }
        });

        // SHORTCUT FIX: Consume Backspace/Delete events locally to prevent bubbling up
        // to design shortcuts
        fontSelector.getEditor().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE
                    || e.getCode() == javafx.scene.input.KeyCode.DELETE) {
                e.consume(); // Stay here, don't delete design objects!
            }
        });

        // Text Field
        textField.textProperty().addListener((obs, old, val) -> {
            if (activeTextLayer != null && !isUpdatingUI) {
                activeTextLayer.setTextContent(val);
            }
        });

        // Color Picker handled by MenuButton callback in buildUI

        // Spacing Slider
        spacingSlider.valueProperty().addListener((obs, old, val) -> {
            if (activeTextLayer != null && !isUpdatingUI) {
                activeTextLayer.setSpacing(val.doubleValue());
            }
        });

        // Stroke Width Slider
        strokeWidthSlider.valueProperty().addListener((obs, old, val) -> {
            if (activeTextLayer != null && !isUpdatingUI) {
                activeTextLayer.setStrokeWidth(val.doubleValue());
            }
        });

        // Font Size Slider
        fontSizeSlider.valueProperty().addListener((obs, old, val) -> {
            if (activeTextLayer != null && !isUpdatingUI) {
                activeTextLayer.setFontSizeScale(val.doubleValue());
            }
        });

        // Selection Listener
        visualizer.getLayerManager().addSelectionListener(node -> {
            if (node instanceof TextLayer) {
                loadLayerSettings((TextLayer) node);
            } else {
                activeTextLayer = null;
                updateGalleryVisuals();
            }
        });

        updateGalleryVisuals();
    }

    private void addNewTextLayer() {
        String content = textField.getText();
        if (content == null || content.trim().isEmpty()) {
            content = "NUEVO TEXTO";
        }

        TextLayer textLayer = new TextLayer(content);
        textLayer.setVisualizer(visualizer);

        // Smaller initial size for better aesthetics
        textLayer.setTextSize(200.0, 50.0);

        // Center on visualizer
        double cx = visualizer.getVisualizerWidth() / 2.0;
        double cy = visualizer.getVisualizerHeight() / 3.0; // Slightly higher than center
        textLayer.setTranslateX(cx - 100);
        textLayer.setTranslateY(cy - 25);

        // Add to active zone if editing, else Pecho
        String zone = visualizer.getPowerClipManager().getCurrentEditingZone();
        if (zone == null)
            zone = "PECHO";

        visualizer.addTextLayer(textLayer);
        visualizer.getLayerManager().selectNode(textLayer);

        textField.requestFocus();
        textField.selectAll();
    }

    private void loadLayerSettings(TextLayer layer) {
        if (isUpdatingUI)
            return;
        isUpdatingUI = true;
        activeTextLayer = layer;
        if (layer != null) {
            textField.setText(layer.getTextContent());
            fontSelector.getEditor().setText(layer.getFont().getFamily());
            UIFactory.setColorMenuButtonColor(textColorPicker, layer.getTextColor());
            spacingSlider.setValue(layer.getSpacing());

            // Update stroke controls
            strokeWidthSlider.setValue(layer.getStrokeWidth());
            UIFactory.setColorMenuButtonColor(strokeColorPicker, layer.getStrokeColor());

            // Sync Bold/Italic buttons
            btnBold.setSelected(layer.isBold());
            btnItalic.setSelected(layer.isItalic());

            // Sync Alignment buttons
            javafx.scene.text.TextAlignment align = layer.getTextAlignment();
            if (align == javafx.scene.text.TextAlignment.LEFT)
                alignmentGroup.selectToggle(btnAlignLeft);
            else if (align == javafx.scene.text.TextAlignment.RIGHT)
                alignmentGroup.selectToggle(btnAlignRight);
            else
                alignmentGroup.selectToggle(btnAlignCenter);

            // Sync Drop Shadow
            dropShadowCheck.setSelected(layer.isDropShadowEnabled());
            shadowColorPicker.setDisable(!layer.isDropShadowEnabled());
            UIFactory.setColorMenuButtonColor(shadowColorPicker, layer.getDropShadowColor());

            // Sync Font Size Slider based on logical height roughly
            double scale = (layer.getLogicalHeight() / 100.0) * 100.0;
            // clamp scale between 10 and 200 just for the slider
            scale = Math.max(10, Math.min(200, scale));
            fontSizeSlider.setValue(scale);
        } else {
            textField.clear();
        }
        isUpdatingUI = false;
        updateGalleryVisuals();
    }

    public void updateGalleryVisuals() {
        galleryContainer.getChildren().clear();
        List<TextLayer> texts = visualizer.getLayerManager().getAllTextLayers();
        for (TextLayer tl : texts) {
            HBox box = createGalleryItem(tl);
            galleryContainer.getChildren().add(box);
        }
    }

    private HBox createGalleryItem(TextLayer tl) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setCursor(Cursor.HAND);

        String idleStyle = "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 1;";
        String activeStyle = "-fx-background-color: #f0f9ff; -fx-background-radius: 8; -fx-border-color: #0ea5e9; -fx-border-width: 2;";

        box.setStyle(tl == activeTextLayer ? activeStyle : idleStyle);

        // Mini Icon to represent text
        Label icon = new Label();
        icon.setGraphic(UIFactory.crearIcono("mdi2f-format-text", 14, tl == activeTextLayer ? "#0284c7" : "#94a3b8"));

        Label lbl = new Label(tl.getTextContent().isEmpty() ? "Vacio"
                : (tl.getTextContent().length() > 12 ? tl.getTextContent().substring(0, 10) + "..."
                        : tl.getTextContent()));
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: " + (tl == activeTextLayer ? "900" : "bold")
                + "; -fx-text-fill: " + (tl == activeTextLayer ? "#0369a1" : "#475569") + ";");
        lbl.setMaxWidth(150);
        HBox.setHgrow(lbl, Priority.ALWAYS);

        box.getChildren().addAll(icon, lbl);
        box.setOnMouseClicked(e -> visualizer.getLayerManager().selectNode(tl));

        return box;
    }

    public VBox getContainer() {
        return container;
    }

    public void focusInput() {
        textField.requestFocus();
    }

    public void clearAll() {
        galleryContainer.getChildren().clear();
        textField.clear();
    }

    public void handleLayerAdded(TextLayer layer) {
        updateGalleryVisuals();
    }

    public void handleLayerRemoved(TextLayer layer) {
        updateGalleryVisuals();
    }
}
