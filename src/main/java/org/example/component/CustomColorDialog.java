package org.example.component;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.example.utils.ColorUtils;
import org.example.utils.UIFactory;

public class CustomColorDialog extends Dialog<Color> {

    private Color currentColor;
    private Color newColor;

    private final Rectangle rectCurrent = new Rectangle(60, 40);
    private final Rectangle rectNew = new Rectangle(60, 40);

    private double opacity = 1.0;
    private java.util.function.Consumer<Color> onColorChanged;
    private final TabPane tabs = new TabPane();

    public void setOnColorChanged(java.util.function.Consumer<Color> onColorChanged) {
        this.onColorChanged = onColorChanged;
    }

    // --- HSB Properties for the Square and Bar ---
    private final DoubleProperty hue = new SimpleDoubleProperty(0);
    private final DoubleProperty saturation = new SimpleDoubleProperty(100);
    private final DoubleProperty brightness = new SimpleDoubleProperty(100);

    // --- SLIDERS ---
    private final Slider sliderC = createSlider(0, 100);
    private final Slider sliderM = createSlider(0, 100);
    private final Slider sliderY = createSlider(0, 100);
    private final Slider sliderK = createSlider(0, 100);
    private final Slider sliderR = createSlider(0, 255);
    private final Slider sliderG = createSlider(0, 255);
    private final Slider sliderB = createSlider(0, 255);
    private final Slider sliderH = createSlider(0, 360);
    private final Slider sliderS = createSlider(0, 100);
    private final Slider sliderBr = createSlider(0, 100);
    private final Slider sliderOpacity = createSlider(0, 100);

    // --- TEXT FIELDS (Added for manual input) ---
    private final TextField txtH = createSliderTextField();
    private final TextField txtS = createSliderTextField();
    private final TextField txtBr = createSliderTextField();
    private final TextField txtR = createSliderTextField();
    private final TextField txtG = createSliderTextField();
    private final TextField txtB = createSliderTextField();
    private final TextField txtC = createSliderTextField();
    private final TextField txtM = createSliderTextField();
    private final TextField txtY = createSliderTextField();
    private final TextField txtK = createSliderTextField();
    private final TextField txtOpacity = createSliderTextField();

    // --- WEB ---
    private final TextField txtWeb = new TextField();

    private boolean isSyncing = false;

    public CustomColorDialog(Color initialColor) {
        initStyle(javafx.stage.StageStyle.TRANSPARENT); // Cambio a TRANSPARENT para evitar marcos blancos

        this.currentColor = initialColor != null ? initialColor : Color.WHITE;
        this.newColor = this.currentColor;
        this.opacity = this.currentColor.getOpacity();

        // Initial HSB values
        this.hue.set(currentColor.getHue());
        this.saturation.set(currentColor.getSaturation() * 100);
        this.brightness.set(currentColor.getBrightness() * 100);

        setTitle("Personalizar color");
        setHeaderText(null); // Removed redundant header as requested

        // Create custom buttons to center them under the manipulation area
        Button btnOk = new Button("Usar");
        btnOk.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 100; -fx-padding: 8 20; -fx-cursor: hand;");
        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 100; -fx-padding: 8 20; -fx-cursor: hand;");

        btnOk.setOnAction(e -> {
            if (newColor == null) {
                // Last ditch effort to capture state if something went wrong
                newColor = Color.hsb(hue.get(), saturation.get() / 100.0, brightness.get() / 100.0, opacity);
            }
            setResult(newColor);
            close();
        });
        btnCancel.setOnAction(e -> {
            setResult(null);
            if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
                getDialogPane().getScene().getWindow().hide();
            }
        });

        UIFactory.estilizarDialogo(this);

        // --- CLEAN DIALOG PANE ---
        getDialogPane().setHeader(null);
        getDialogPane().setGraphic(null);
        getDialogPane().getButtonTypes().clear();
        getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-width: 0;");
        getDialogPane().setPadding(Insets.EMPTY);

        // Ensure scene is transparent
        javafx.application.Platform.runLater(() -> {
            if (getDialogPane().getScene() != null) {
                getDialogPane().getScene().setFill(Color.TRANSPARENT);
                getDialogPane().getScene().getWindow().sizeToScene();
            }
        });

        // Main Container (VBox)
        VBox rootLayout = new VBox(0);
        rootLayout.setStyle(
                "-fx-background-color: #f0f4f8; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
        rootLayout.setPadding(Insets.EMPTY);

        // --- CUSTOM HEADER ---
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 15, 10, 15));
        header.setStyle("-fx-background-color: #0d1b2a; -fx-background-radius: 8 8 0 0;"); // Match root radius
                                                                                           // esquinas superiores

        Label lblTitle = new Label("Personalizar color");
        lblTitle.setStyle(
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-font-family: 'Segoe UI';");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCloseHeader = new Button("✕");
        btnCloseHeader.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 8; -fx-background-radius: 4;");
        
        btnCloseHeader.setOnMouseEntered(e -> btnCloseHeader.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 8; -fx-background-radius: 4;"));
        btnCloseHeader.setOnMouseExited(e -> btnCloseHeader.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 8; -fx-background-radius: 0;"));

        btnCloseHeader.setOnAction(e -> {
            setResult(null);
            if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
                getDialogPane().getScene().getWindow().hide();
            }
        });

        javafx.scene.image.Image icon = UIFactory.getAppLogo();
        if (icon != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(icon);
            iv.setFitWidth(20);
            iv.setFitHeight(20);
            iv.setSmooth(true);
            header.getChildren().add(iv);
        }
        header.getChildren().addAll(lblTitle, spacer, btnCloseHeader);

        // DRAG FUNCTIONALITY
        final double[] xOffset = new double[1];
        final double[] yOffset = new double[1];
        header.setOnMousePressed(e -> {
            xOffset[0] = e.getSceneX();
            yOffset[0] = e.getSceneY();
        });
        header.setOnMouseDragged(e -> {
            getDialogPane().getScene().getWindow().setX(e.getScreenX() - xOffset[0]);
            getDialogPane().getScene().getWindow().setY(e.getScreenY() - yOffset[0]);
        });

        VBox contentContainer = new VBox(0);
        contentContainer.setPadding(new Insets(12)); // Pequeño margen interno general
        contentContainer.setAlignment(Pos.CENTER);
        contentContainer.setSpacing(12);

        // Blocks container
        HBox blocksContainer = new HBox(12);
        blocksContainer.setAlignment(Pos.CENTER);

        // --- LEFT BLOCK ---
        VBox leftBlock = new VBox(10);
        leftBlock.setAlignment(Pos.TOP_CENTER);
        leftBlock.setStyle(
                "-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-border-width: 1;");

        HBox selectorArea = new HBox(12);
        selectorArea.setAlignment(Pos.CENTER);
        Pane colorSquare = createColorSquare();
        Pane hueBar = createHueBar();
        selectorArea.getChildren().addAll(colorSquare, hueBar);

        VBox paletteSection = createRecentPalette();
        leftBlock.getChildren().addAll(selectorArea, paletteSection);

        // --- RIGHT BLOCK ---
        VBox rightBlock = new VBox(8);
        rightBlock.setStyle(
                "-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-border-width: 1;");

        // Thin Horizontal Preview Area
        HBox previewBar = new HBox(0);
        previewBar.setAlignment(Pos.CENTER);
        previewBar.setPrefHeight(25);
        previewBar.setMinHeight(25);

        rectCurrent.setWidth(180);
        rectCurrent.setHeight(20);
        rectNew.setWidth(180);
        rectNew.setHeight(20);
        rectCurrent.setArcWidth(0);
        rectCurrent.setArcHeight(0);
        rectNew.setArcWidth(0);
        rectNew.setArcHeight(0);

        VBox currentLabelBox = new VBox(2, new Label("Color Actual"), rectCurrent);
        currentLabelBox.setAlignment(Pos.CENTER);
        VBox newLabelBox = new VBox(2, new Label("Nuevo Color"), rectNew);
        newLabelBox.setAlignment(Pos.CENTER);

        previewBar.getChildren().addAll(currentLabelBox, newLabelBox);

        // Tabs
        tabs.getStyleClass().add("floating");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefHeight(240);

        tabs.getTabs().addAll(
                new Tab("HSB", createHsbPane()),
                new Tab("RGB", createRgbPane()),
                new Tab("CMYK", createCmykPane()),
                new Tab("WEB", createWebPane()));
        tabs.getSelectionModel().select(1);

        // Opacity
        VBox opacityBox = new VBox(5);
        HBox opacityControl = new HBox(8, new Label("Opacidad:"), sliderOpacity, txtOpacity);
        opacityControl.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sliderOpacity, Priority.ALWAYS);
        opacityBox.getChildren().addAll(opacityControl);

        // Buttons INTEGRATED and CENTERED
        HBox hBoxButtons = new HBox(20, btnOk, btnCancel);
        hBoxButtons.setAlignment(Pos.CENTER);
        hBoxButtons.setPadding(new Insets(5, 0, 0, 0));

        rightBlock.getChildren().addAll(previewBar, tabs, opacityBox, hBoxButtons);

        // IMPORTANTE: Añadir los bloques al contenedor
        blocksContainer.getChildren().addAll(leftBlock, rightBlock);

        contentContainer.getChildren().addAll(blocksContainer);
        rootLayout.getChildren().addAll(header, contentContainer);

        getDialogPane().setContent(rootLayout);

        // Force sync window size to our layout strictly
        rootLayout.heightProperty().addListener((obs, oldV, newV) -> {
            if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
                getDialogPane().getScene().getWindow().setHeight(newV.doubleValue());
            }
        });
        rootLayout.widthProperty().addListener((obs, oldV, newV) -> {
            if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
                getDialogPane().getScene().getWindow().setWidth(newV.doubleValue());
            }
        });

        syncAllComponents(newColor);

        sliderOpacity.valueProperty().addListener((obs, oldV, newV) -> {
            if (isSyncing)
                return;
            opacity = newV.doubleValue() / 100.0;
            txtOpacity.setText(String.format("%.0f", newV.doubleValue()));
            updateFinalColor(Color.hsb(hue.get(), saturation.get() / 100.0, brightness.get() / 100.0, opacity));
        });
        setupTextFieldSync(txtOpacity, sliderOpacity);

        setResultConverter(dialogButton -> (dialogButton == ButtonType.OK) ? newColor : null);
    }

    private VBox createRecentPalette() {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: white; -fx-padding: 8; -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-radius: 6;");
        box.setAlignment(Pos.CENTER);
        Label lblPalette = new Label("Paleta de Colores");
        lblPalette.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 10px; -fx-text-transform: uppercase;");

        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setAlignment(Pos.CENTER);

        // 1. Row 0: Greyscale steps (12 colors)
        for (int i = 0; i < 12; i++) {
            double v = 1.0 - (i / 11.0);
            grid.add(createColorSquareMini(Color.color(v, v, v)), i, 0);
        }

        // Diverse 3-Row Palette: Distinct Gammas
        double[] redHues = {0, 25, 45, 60, 90, 120, 160, 195, 225, 260, 290, 325};
        
        // 1. Vivid Gamma (Bright Primaries/Secondaries)
        for (int i = 0; i < 12; i++) {
            grid.add(createColorSquareMini(Color.hsb(redHues[i], 1.0, 1.0)), i, 1);
        }

        // 2. Pastel/Muted Gamma (Soft Design Tones)
        for (int i = 0; i < 12; i++) {
            grid.add(createColorSquareMini(Color.hsb(redHues[i], 0.4, 0.9)), i, 2);
        }

        box.getChildren().addAll(lblPalette, grid);
        return box;
    }

    private Node createColorSquareMini(Color c) {
        Rectangle r = new Rectangle(18, 18, c);
        r.setStroke(Color.web("#e2e8f0"));
        r.setStrokeWidth(0.5);
        r.setCursor(Cursor.HAND);
        r.setOnMousePressed(e -> {
            syncAllComponents(c);
            e.consume();
        });

        // Hover Effect: Zoom and Lift
        r.setOnMouseEntered(e -> {
            r.setStroke(Color.web("#94a3b8"));
            r.setScaleX(1.3);
            r.setScaleY(1.3);
            r.setEffect(new javafx.scene.effect.DropShadow(5, Color.BLACK));
            r.toFront(); // Ensure it zooms over neighbors
        });
        r.setOnMouseExited(e -> {
            r.setStroke(Color.web("#e2e8f0"));
            r.setScaleX(1.0);
            r.setScaleY(1.0);
            r.setEffect(null);
        });
        return r;
    }

    private Pane createColorSquare() {
        Pane square = new Pane();
        square.setPrefSize(256, 300); // Increased height to fill space
        square.setMinSize(256, 300);
        square.setCursor(Cursor.DEFAULT); // NO MORE CROSSHAIR

        // Background Color (Hue only)
        square.backgroundProperty()
                .bind(Bindings.createObjectBinding(
                        () -> new Background(
                                new BackgroundFill(Color.hsb(hue.get(), 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)),
                        hue));

        // Gradients
        Region whiteGrad = new Region();
        whiteGrad.setPrefSize(256, 300);
        whiteGrad.setStyle("-fx-background-color: linear-gradient(to right, white, transparent);");

        Region blackGrad = new Region();
        blackGrad.setPrefSize(256, 300);
        blackGrad.setStyle("-fx-background-color: linear-gradient(to bottom, transparent, black);");

        // Elegant Circular Handle
        Circle handle = new Circle(8, Color.TRANSPARENT);
        handle.setStroke(Color.WHITE);
        handle.setStrokeWidth(2.5);
        handle.setEffect(new javafx.scene.effect.DropShadow(4, Color.BLACK));

        handle.setMouseTransparent(true);

        // Dynamic stroke color for visibility
        hue.addListener((obs, old, val) -> updateHandleStroke(handle));
        saturation.addListener((obs, old, val) -> updateHandleStroke(handle));
        brightness.addListener((obs, old, val) -> updateHandleStroke(handle));

        // Bind position
        handle.translateXProperty().bind(saturation.multiply(2.56));
        handle.translateYProperty()
                .bind(Bindings.createDoubleBinding(() -> (100 - brightness.get()) * 3.0, brightness));

        square.getChildren().addAll(whiteGrad, blackGrad, handle);

        square.setOnMousePressed(e -> updateFromSquare(e.getX(), e.getY()));
        square.setOnMouseDragged(e -> updateFromSquare(e.getX(), e.getY()));

        return square;
    }

    private void updateFromSquare(double x, double y) {
        double s = Math.max(0, Math.min(100, x / 2.56));
        double b = Math.max(0, Math.min(100, 100 - (y / 3.0)));

        isSyncing = true;
        this.saturation.set(s);
        this.brightness.set(b);
        isSyncing = false;

        syncAllComponents(Color.hsb(hue.get(), s / 100.0, b / 100.0, opacity));
    }

    private Pane createHueBar() {
        Pane bar = new Pane();
        bar.setPrefSize(25, 300); // Increased height to match square
        bar.setMinSize(25, 300);
        bar.setCursor(Cursor.HAND);
        bar.setStyle("-fx-border-color: #cbd5e1; -fx-border-radius: 4;");

        // Hue Gradient
        StringBuilder sb = new StringBuilder("linear-gradient(to bottom, ");
        for (int i = 0; i <= 360; i += 30) {
            sb.append(String.format("hsb(%d, 100%%, 100%%)", i));
            if (i < 360)
                sb.append(", ");
        }
        sb.append(")");
        bar.setStyle("-fx-background-color: " + sb.toString() + "; -fx-background-radius: 4;");

        // Sleek Tab Handle
        Rectangle handle = new Rectangle(35, 8, Color.WHITE);
        handle.setStroke(Color.web("#475569"));
        handle.setStrokeWidth(1.5);
        handle.setArcWidth(4);
        handle.setArcHeight(4);
        handle.setX(-5);
        handle.setEffect(new javafx.scene.effect.DropShadow(3, Color.rgb(0, 0, 0, 0.5)));

        handle.translateYProperty().bind(hue.multiply(300.0 / 360.0).subtract(4));

        bar.getChildren().add(handle);

        bar.setOnMousePressed(e -> updateFromHue(e.getY()));
        bar.setOnMouseDragged(e -> updateFromHue(e.getY()));

        return bar;
    }

    private void updateFromHue(double y) {
        double h = Math.max(0, Math.min(360, y * 360.0 / 300.0));

        isSyncing = true;
        this.hue.set(h);
        isSyncing = false;

        syncAllComponents(Color.hsb(h, saturation.get() / 100.0, brightness.get() / 100.0, opacity));
    }

    private Pane createHsbPane() {
        GridPane grid = createGrid();
        addSliderRow(grid, 0, "Matiz:", sliderH, txtH, "°", 360);
        addSliderRow(grid, 1, "Saturación:", sliderS, txtS, "%", 100);
        addSliderRow(grid, 2, "Brillo:", sliderBr, txtBr, "%", 100);

        sliderH.valueProperty().addListener(o -> syncFromHSB());
        sliderS.valueProperty().addListener(o -> syncFromHSB());
        sliderBr.valueProperty().addListener(o -> syncFromHSB());

        sliderOpacity.valueProperty().addListener(o -> syncFromSliderToText(sliderOpacity, txtOpacity));
        setupTextFieldSync(txtH, sliderH);
        setupTextFieldSync(txtS, sliderS);
        setupTextFieldSync(txtBr, sliderBr);
        setupTextFieldSync(txtOpacity, sliderOpacity);

        return grid;
    }

    private Pane createRgbPane() {
        GridPane grid = createGrid();
        addSliderRow(grid, 0, "Rojo:", sliderR, txtR, "", 255);
        addSliderRow(grid, 1, "Verde:", sliderG, txtG, "", 255);
        addSliderRow(grid, 2, "Azul:", sliderB, txtB, "", 255);

        sliderR.valueProperty().addListener(o -> syncFromRGB());
        sliderG.valueProperty().addListener(o -> syncFromRGB());
        sliderB.valueProperty().addListener(o -> syncFromRGB());

        setupTextFieldSync(txtR, sliderR);
        setupTextFieldSync(txtG, sliderG);
        setupTextFieldSync(txtB, sliderB);

        return grid;
    }

    private Pane createCmykPane() {
        GridPane grid = createGrid();
        addSliderRow(grid, 0, "Cyan:", sliderC, txtC, "%", 100);
        addSliderRow(grid, 1, "Magenta:", sliderM, txtM, "%", 100);
        addSliderRow(grid, 2, "Yellow:", sliderY, txtY, "%", 100);
        addSliderRow(grid, 3, "Black (K):", sliderK, txtK, "%", 100);

        sliderC.valueProperty().addListener(o -> syncFromCMYK());
        sliderM.valueProperty().addListener(o -> syncFromCMYK());
        sliderY.valueProperty().addListener(o -> syncFromCMYK());
        sliderK.valueProperty().addListener(o -> syncFromCMYK());

        setupTextFieldSync(txtC, sliderC);
        setupTextFieldSync(txtM, sliderM);
        setupTextFieldSync(txtY, sliderY);
        setupTextFieldSync(txtK, sliderK);

        return grid;
    }

    private Pane createWebPane() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);

        Label lblHex = new Label("Hexadecimal:");
        lblHex.setStyle("-fx-font-weight: bold;");

        txtWeb.setPrefWidth(140);
        txtWeb.setStyle(
                "-fx-font-family: 'Consolas', monospace; -fx-font-size: 16px; -fx-alignment: center; -fx-padding: 8;");

        txtWeb.textProperty().addListener((obs, old, newVal) -> {
            if (isSyncing)
                return;
            try {
                String hex = newVal.startsWith("#") ? newVal : "#" + newVal;
                Color col = Color.web(hex);
                syncAllComponents(col);
            } catch (Exception e) {
            }
        });

        box.getChildren().addAll(lblHex, txtWeb);
        return box;
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        return grid;
    }

    private void updateHandleStroke(Circle handle) {
        Color c = Color.hsb(hue.get(), saturation.get() / 100.0, brightness.get() / 100.0);
        Color stroke = c.getBrightness() > 0.6 ? Color.BLACK : Color.WHITE;
        handle.setStroke(stroke);
    }

    private void addSliderRow(GridPane grid, int row, String label, Slider slider, TextField textField, String unit,
            double max) {
        Label lblTitle = new Label(label);
        lblTitle.setMinWidth(70);
        lblTitle.setStyle("-fx-font-size: 11px;");
        grid.add(lblTitle, 0, row);
        grid.add(slider, 1, row);
        grid.add(textField, 2, row);
        Label lblUnit = new Label(unit);
        lblUnit.setMinWidth(15);
        grid.add(lblUnit, 3, row);
        GridPane.setHgrow(slider, Priority.ALWAYS);
    }

    private TextField createSliderTextField() {
        TextField tf = new TextField();
        tf.setPrefWidth(45);
        tf.setMaxWidth(45);
        tf.setStyle("-fx-padding: 2 4; -fx-font-size: 11px; -fx-alignment: center;");
        return tf;
    }

    private void setupTextFieldSync(TextField tf, Slider slider) {
        tf.setOnAction(e -> {
            try {
                double val = Double.parseDouble(tf.getText());
                slider.setValue(Math.max(slider.getMin(), Math.min(slider.getMax(), val)));
            } catch (Exception ex) {
                tf.setText(String.format("%.0f", slider.getValue()));
            }
        });
        slider.valueProperty().addListener(o -> syncFromSliderToText(slider, tf));
    }

    private void syncFromSliderToText(Slider slider, TextField tf) {
        if (!tf.isFocused()) {
            tf.setText(String.format("%.0f", slider.getValue()));
        }
    }

    private void syncFromHSB() {
        if (isSyncing)
            return;
        Color col = Color.hsb(sliderH.getValue(), sliderS.getValue() / 100.0, sliderBr.getValue() / 100.0, opacity);
        syncAllComponents(col);
    }

    private void syncFromRGB() {
        if (isSyncing)
            return;
        Color col = Color.rgb((int) sliderR.getValue(), (int) sliderG.getValue(), (int) sliderB.getValue(), opacity);
        syncAllComponents(col);
    }

    private void syncFromCMYK() {
        if (isSyncing)
            return;
        Color col = ColorUtils.fromCmyk(sliderC.getValue() / 100.0, sliderM.getValue() / 100.0,
                sliderY.getValue() / 100.0, sliderK.getValue() / 100.0, opacity);
        syncAllComponents(col);
    }

    private void syncAllComponents(Color col) {
        if (isSyncing)
            return;
        isSyncing = true;
        try {
            this.newColor = col;
            rectNew.setFill(col);

            // Update Properties (Crucial for HSB square and "Usar" button)
            this.hue.set(col.getHue());
            this.saturation.set(col.getSaturation() * 100);
            this.brightness.set(col.getBrightness() * 100);

            // Update HSB
            if (sliderH.getValue() != col.getHue())
                sliderH.setValue(col.getHue());
            if (sliderS.getValue() != col.getSaturation() * 100)
                sliderS.setValue(col.getSaturation() * 100);
            if (sliderBr.getValue() != col.getBrightness() * 100)
                sliderBr.setValue(col.getBrightness() * 100);
            syncFromSliderToText(sliderH, txtH);
            syncFromSliderToText(sliderS, txtS);
            syncFromSliderToText(sliderBr, txtBr);

            // Update RGB
            double r = col.getRed() * 255;
            double g = col.getGreen() * 255;
            double b = col.getBlue() * 255;
            if (Math.abs(sliderR.getValue() - r) > 0.5) sliderR.setValue(r);
            if (Math.abs(sliderG.getValue() - g) > 0.5) sliderG.setValue(g);
            if (Math.abs(sliderB.getValue() - b) > 0.5) sliderB.setValue(b);
            syncFromSliderToText(sliderR, txtR);
            syncFromSliderToText(sliderG, txtG);
            syncFromSliderToText(sliderB, txtB);

            // Update Web
            String hex = String.format("#%02X%02X%02X", (int) Math.round(r), (int) Math.round(g), (int) Math.round(b));
            if (!txtWeb.getText().equals(hex))
                txtWeb.setText(hex);

            // Update CMYK - ONLY if not currently in the CMYK tab to prevent "jumping"
            // This allows the user to manually set specific CMYK values without the auto-optimal-GCR 
            // logic overriding their choices immediately.
            boolean inCmykTab = tabs.getSelectionModel().getSelectedIndex() == 2;
            if (!inCmykTab) {
                double[] cmyk = ColorUtils.toCmyk(col);
                if (Math.abs(sliderC.getValue() - cmyk[0] * 100) > 0.5) sliderC.setValue(cmyk[0] * 100);
                if (Math.abs(sliderM.getValue() - cmyk[1] * 100) > 0.5) sliderM.setValue(cmyk[1] * 100);
                if (Math.abs(sliderY.getValue() - cmyk[2] * 100) > 0.5) sliderY.setValue(cmyk[2] * 100);
                if (Math.abs(sliderK.getValue() - cmyk[3] * 100) > 0.5) sliderK.setValue(cmyk[3] * 100);
                syncFromSliderToText(sliderC, txtC);
                syncFromSliderToText(sliderM, txtM);
                syncFromSliderToText(sliderY, txtY);
                syncFromSliderToText(sliderK, txtK);
            }

            // Opacity
            if (sliderOpacity.getValue() != col.getOpacity() * 100)
                sliderOpacity.setValue(col.getOpacity() * 100);
            syncFromSliderToText(sliderOpacity, txtOpacity);
            opacity = col.getOpacity();

            updateFinalColor(col);
        } finally {
            isSyncing = false;
        }
    }

    private void updateFinalColor(Color col) {
        newColor = col;
        rectNew.setFill(newColor);
        if (onColorChanged != null) {
            onColorChanged.accept(newColor);
        }
    }

    private Slider createSlider(double min, double max) {
        Slider slider = new Slider(min, max, min);
        slider.setPrefWidth(125);
        // Modern Premium Styling for Track and Thumb
        slider.setStyle(
                "-fx-control-inner-background: #e2e8f0; " +
                        "-fx-background-radius: 5; " +
                        "-fx-padding: 5 0; ");
        return slider;
    }
}

