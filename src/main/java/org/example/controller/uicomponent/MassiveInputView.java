package org.example.controller.uicomponent;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.*;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.DetallePedido;
import org.example.model.TipoGenero;
import org.example.model.TipoTalla;
import org.example.utils.UIFactory;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Componente UI para la generación masiva de jugadores (Correlativa por Talla).
 */
public class MassiveInputView {

    private final HBox view;
    private Supplier<ConfiguracionPrendaDTO> configSupplier;
    private Consumer<List<DetallePedido>> onGenerateListener;

    // UI Fields
    private ComboBox<TipoGenero> comboGenero;
    private RadioButton rbContinuous, rbPerSize;
    private TextField txtStartNumber;
    private FlowPane rowsContainer;
    private Button btnGenerate;

    public MassiveInputView(Supplier<ConfiguracionPrendaDTO> configSupplier) {
        this.configSupplier = configSupplier;
        this.view = createContent();
    }

    public Node getView() {
        return view;
    }

    public void setOnGenerate(Consumer<List<DetallePedido>> listener) {
        this.onGenerateListener = listener;
    }

    public void setConfigSupplier(Supplier<ConfiguracionPrendaDTO> supplier) {
        this.configSupplier = supplier;
    }

    private HBox createContent() {
        HBox rootWrapper = new HBox();
        rootWrapper.setAlignment(Pos.CENTER);

        // Main scroll pane for vertical scrolling on small screens/laptops
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        VBox container = new VBox(15);
        container.setPadding(new Insets(10, 15, 15, 15));
        container.setStyle("-fx-background-color: white;");
        scrollPane.setContent(container);

        // --- 1. CONFIGURATION SECTION ---
        VBox configSection = new VBox(8);
        configSection.setStyle(
                "-fx-background-color: #f8f9fa; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #ecf0f1; -fx-border-radius: 8;");

        Label lblTitle1 = new Label("1. Configuración");
        lblTitle1.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");

        GridPane gridConfig = new GridPane();
        gridConfig.setHgap(15);
        gridConfig.setVgap(10);

        VBox boxGender = new VBox(4);
        Label lblGen = new Label("Género Base:");
        lblGen.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold;");
        comboGenero = new ComboBox<>();
        comboGenero.getItems().setAll(TipoGenero.HOMBRE, TipoGenero.MUJER);
        comboGenero.setValue(TipoGenero.HOMBRE);
        comboGenero.setMaxWidth(Double.MAX_VALUE);
        comboGenero.setStyle(
                "-fx-font-size: 12px; -fx-pref-height: 35px; -fx-background-color: white; -fx-border-color: #bdc3c7; -fx-background-radius: 5; -fx-border-radius: 5;");

        comboGenero.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && txtStartNumber != null) {
                txtStartNumber.setText(newVal == TipoGenero.HOMBRE ? "2" : "1");
            }
        });
        boxGender.getChildren().addAll(lblGen, comboGenero);

        VBox boxStartNum = new VBox(4);
        Label lblStart = new Label("Número Inicial:");
        lblStart.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold;");
        txtStartNumber = new TextField("2");
        txtStartNumber.setStyle(
                "-fx-font-size: 13px; -fx-pref-height: 35px; -fx-background-color: white; -fx-border-color: #bdc3c7; -fx-background-radius: 5; -fx-border-radius: 5;");
        txtStartNumber.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtStartNumber.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        boxStartNum.getChildren().addAll(lblStart, txtStartNumber);

        VBox boxStrategy = new VBox(6);
        Label lblStrat = new Label("Numeración:");
        lblStrat.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold;");
        FlowPane stratOptions = new FlowPane(10, 5); // Responsive wrapping
        ToggleGroup grpStrat = new ToggleGroup();
        rbContinuous = new RadioButton("Global Continua");
        rbContinuous.setToggleGroup(grpStrat);
        rbContinuous.setSelected(true);
        rbContinuous.setWrapText(true);
        rbContinuous.setMaxWidth(Double.MAX_VALUE); // Allow growing/shrinking
        rbContinuous.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-cursor: hand;");

        rbPerSize = new RadioButton("Por Talla");
        rbPerSize.setToggleGroup(grpStrat);
        rbPerSize.setWrapText(true);
        rbPerSize.setMaxWidth(Double.MAX_VALUE);
        rbPerSize.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-cursor: hand;");

        stratOptions.getChildren().addAll(rbContinuous, rbPerSize);
        boxStrategy.getChildren().addAll(lblStrat, stratOptions);

        gridConfig.add(boxGender, 0, 0);
        gridConfig.add(boxStartNum, 1, 0);
        gridConfig.add(boxStrategy, 0, 1, 2, 1);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(50);
        cc1.setHgrow(Priority.ALWAYS);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(50);
        cc2.setHgrow(Priority.ALWAYS);
        gridConfig.getColumnConstraints().addAll(cc1, cc2);

        configSection.getChildren().addAll(lblTitle1, gridConfig);

        // --- 2. ACTIONS SECTION ---
        VBox actionsSection = new VBox(8);
        Label lblTitle2 = new Label("2. Acciones");
        lblTitle2.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        Button btnAddRow = new Button("Agregar Bloque de Talla");
        FontIcon iconAdd = new FontIcon("mdi2p-plus-circle");
        iconAdd.setIconColor(javafx.scene.paint.Color.WHITE);
        btnAddRow.setGraphic(iconAdd);
        btnAddRow.setMaxWidth(Double.MAX_VALUE);
        btnAddRow.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 12; -fx-font-size: 13px; -fx-background-radius: 5;");
        btnAddRow.setOnAction(e -> addSizeRow());
        actionsSection.getChildren().addAll(lblTitle2, btnAddRow);

        // --- 3. DISTRIBUTION SECTION ---
        HBox distHeader = new HBox(10);
        distHeader.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(distHeader, new Insets(15, 0, 0, 0)); // Extra margin to avoid overlap
        Label lblTitle3 = new Label("Distribución de Tallas");
        lblTitle3.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        btnGenerate = new Button("GENERAR LISTA");
        FontIcon iconDist = new FontIcon("mdi2c-check-circle");
        iconDist.setIconColor(javafx.scene.paint.Color.WHITE);
        btnGenerate.setGraphic(iconDist);
        btnGenerate.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // Prevent truncation
        btnGenerate.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand; -fx-background-radius: 5;");
        btnGenerate.setOnAction(e -> generate());
        distHeader.getChildren().addAll(lblTitle3, spacer, btnGenerate);

        rowsContainer = new FlowPane(10, 10);
        rowsContainer.setPadding(new Insets(10));
        rowsContainer.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 5;");

        container.getChildren().addAll(configSection, actionsSection, distHeader, rowsContainer);
        rootWrapper.getChildren().add(scrollPane);

        addSizeRow();
        return rootWrapper;
    }

    private void addSizeRow() {
        String nextSize = "M"; // Default if first row
        if (!rowsContainer.getChildren().isEmpty()) {
            Node lastNode = rowsContainer.getChildren().get(rowsContainer.getChildren().size() - 1);
            if (lastNode instanceof VBox card) {
                java.util.Map<String, Object> refs = (java.util.Map<String, Object>) card.getUserData();
                ComboBox<String> cb = (ComboBox<String>) refs.get("cbSize");
                if (cb != null) {
                    String current = cb.getValue();
                    List<String> labels = TipoTalla.getLabels();
                    int idx = labels.indexOf(current);
                    if (idx != -1 && idx < labels.size() - 1) {
                        nextSize = labels.get(idx + 1);
                    } else {
                        nextSize = current; // Keep same if at end of list
                    }
                }
            }
        }

        VBox card = new VBox(5);
        card.setPadding(new Insets(8));
        card.setStyle(
                "-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

        // BIND WIDTH: 4 columns proportional to container
        // Margin: 20 (padding) + 30 (3 gaps) + 10 (buffer) = 60
        card.prefWidthProperty().bind(rowsContainer.widthProperty().subtract(60).divide(4));
        card.setMinWidth(110);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_RIGHT);

        Button btnConfig = new Button();
        btnConfig.setGraphic(new FontIcon("mdi2c-cog"));
        btnConfig.getStyleClass().addAll("circular-icon-button", "btn-config-manga");
        btnConfig.setTooltip(new Tooltip("Configurar Manga/Short"));

        Button btnDel = new Button();
        btnDel.setGraphic(new FontIcon("mdi2c-close"));
        btnDel.getStyleClass().addAll("circular-icon-button", "btn-delete-card");
        btnDel.setTooltip(new Tooltip("Eliminar"));
        btnDel.setOnAction(e -> rowsContainer.getChildren().remove(card));

        header.getChildren().addAll(btnConfig, btnDel);

        VBox body = new VBox(5);
        body.setAlignment(Pos.TOP_LEFT);

        Label lblSize = new Label("Talla:");
        lblSize.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        ComboBox<String> cbSize = new ComboBox<>();
        cbSize.getItems().addAll(TipoTalla.getLabels());
        cbSize.getSelectionModel().select(nextSize);
        cbSize.setMaxWidth(Double.MAX_VALUE);

        VBox rowQty = new VBox(2);
        rowQty.setAlignment(Pos.TOP_LEFT);
        Label lblQty = new Label("Cantidad:");
        lblQty.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        TextField txtQty = new TextField("1");
        txtQty.setMaxWidth(Double.MAX_VALUE);
        txtQty.setAlignment(Pos.CENTER);
        rowQty.getChildren().addAll(lblQty, txtQty);

        body.getChildren().addAll(lblSize, cbSize, rowQty);

        // Metadata
        java.util.Map<String, Object> refs = new java.util.HashMap<>();
        refs.put("cbSize", cbSize);
        refs.put("txtQty", txtQty);
        card.setUserData(refs);
        card.getProperties().put("config", new String[] { "DEFAULT", "SAME", "DEFAULT" });

        // Config Dialog Logic
        btnConfig.setOnAction(e -> showConfigDialog(card));

        card.getChildren().addAll(header, body);
        rowsContainer.getChildren().add(card);
    }

    private void showConfigDialog(Node card) {
        String[] currentConfig = (String[]) card.getProperties().get("config");
        String curManga = currentConfig[0];
        String curTallaShort = currentConfig[1];
        String curTipoBottom = (currentConfig.length > 2) ? currentConfig[2] : "DEFAULT";

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Configurar Bloque");
        dialog.setHeaderText("Manga y Prenda Inferior");
        ButtonType applyType = new ButtonType("Aplicar", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyType, ButtonType.CANCEL);
        UIFactory.estilizarDialogo(dialog);

        GridPane g = new GridPane();
        g.setHgap(20);
        g.setVgap(15);
        g.setPadding(new Insets(30));

        // Determine visibility/disability from Global Config
        boolean disableBottom = false;
        boolean isRedondo = false;
        String defaultMangaLabel = "USAR DEFECTO";

        if (configSupplier != null && configSupplier.get() != null) {
            ConfiguracionPrendaDTO cfg = configSupplier.get();
            // Check Short logic
            if (!cfg.llevaShort() && cfg.getTipoPrenda() != org.example.model.TipoPrenda.CONJUNTO) {
                disableBottom = true;
            }
            // Check Corte logic (Redondo vs Others)
            if (cfg.getCorte() == org.example.model.TipoCorte.REDONDO) {
                isRedondo = true;
            }

            if (cfg.getLargo() != null) {
                defaultMangaLabel = "USAR DEFECTO (" + cfg.getLargo().name() + ")";
            }
        }

        ComboBox<String> cbManga = new ComboBox<>();
        cbManga.getItems().add("DEFAULT");
        cbManga.getItems().addAll("CORTA", "LARGA");
        // Only add extra options if NOT Redondo
        if (!isRedondo) {
            cbManga.getItems().addAll("MANGA 0");
        }

        // Internal value is "DEFAULT", but we want to show "USAR DEFECTO"
        String initialManga = curManga.equals("DEFAULT") ? "DEFAULT" : curManga;
        // If the stored value was "USAR DEFECTO" literal (legacy), map to "DEFAULT"
        if (curManga.equals("USAR DEFECTO"))
            initialManga = "DEFAULT";

        // If current value is invalid for this mode (e.g. was MANGA 0 but now Redondo),
        // reset to DEFAULT
        if (!cbManga.getItems().contains(initialManga)) {
            initialManga = "DEFAULT";
        }

        cbManga.setValue(initialManga);
        cbManga.setMaxWidth(Double.MAX_VALUE);
        cbManga.setPrefWidth(200);

        // Render "DEFAULT" as "USAR DEFECTO"
        final String finalDefaultLabel = defaultMangaLabel;
        javafx.util.Callback<ListView<String>, ListCell<String>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.equals("DEFAULT")) {
                    setText(finalDefaultLabel);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9;");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        };
        cbManga.setButtonCell(cellFactory.call(null));
        cbManga.setCellFactory(cellFactory);

        ComboBox<String> cbShort = new ComboBox<>();
        cbShort.getItems().add("IGUAL A CAMISETA");
        cbShort.getItems().addAll(TipoTalla.getLabels());
        cbShort.setValue(curTallaShort.equals("SAME") ? "IGUAL A CAMISETA" : curTallaShort);
        cbShort.setPrefWidth(200);

        ComboBox<String> cbTipoBottom = new ComboBox<>();
        cbTipoBottom.getItems().addAll("USAR DEFECTO", "Short", "Licra", "Pantaloneta", "Falda", "Bermuda");
        cbTipoBottom.setValue(curTipoBottom.equals("DEFAULT") ? "USAR DEFECTO" : curTipoBottom);
        cbTipoBottom.setPrefWidth(200);

        if (disableBottom) {
            cbShort.setDisable(true);
            cbShort.setValue("NO APLICA");
            cbShort.setStyle("-fx-opacity: 0.7; -fx-background-color: #eee;");

            cbTipoBottom.setDisable(true);
            cbTipoBottom.setValue("NO APLICA");
            cbTipoBottom.setStyle("-fx-opacity: 0.7; -fx-background-color: #eee;");
        }

        int row = 0;
        g.add(new Label("Tipo Manga:"), 0, row);
        g.add(cbManga, 1, row++);

        // Always show bottom fields, but disabled if needed
        g.add(new Label("Talla Inf.:"), 0, row);
        g.add(cbShort, 1, row++);
        g.add(new Label("Tipo Inf.:"), 0, row);
        g.add(cbTipoBottom, 1, row++);

        dialog.getDialogPane().setContent(g);
        dialog.setResultConverter(db -> {
            if (db == applyType) {
                String sVal = cbShort.getValue().equals("IGUAL A CAMISETA") ? "SAME" : cbShort.getValue();
                String tVal = cbTipoBottom.getValue().equals("USAR DEFECTO") ? "DEFAULT" : cbTipoBottom.getValue();
                return new String[] { cbManga.getValue(), sVal, tVal };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newC -> card.getProperties().put("config", newC));
    }

    public void generate() {
        if (onGenerateListener == null)
            return;

        TipoGenero genero = comboGenero.getValue();
        boolean continuous = rbContinuous.isSelected();
        int startVal = 1;
        try {
            String val = txtStartNumber.getText().trim();
            if (!val.isEmpty())
                startVal = Integer.parseInt(val);
        } catch (Exception e) {
        }

        final int finalStartVal = startVal;

        // Capture relevant data for the background thread
        java.util.List<RowData> rowsToProcess = new java.util.ArrayList<>();
        for (Node node : rowsContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                java.util.Map<String, Object> refs = (java.util.Map<String, Object>) card.getUserData();
                ComboBox<String> cb = (ComboBox<String>) refs.get("cbSize");
                TextField txt = (TextField) refs.get("txtQty");

                String size = cb.getValue();
                String qtyStr = txt.getText().trim();

                if (size != null && !qtyStr.isEmpty()) {
                    try {
                        int qty = Integer.parseInt(qtyStr);
                        String[] config = (String[]) card.getProperties().get("config");
                        rowsToProcess.add(new RowData(size, qty, config));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }

        if (rowsToProcess.isEmpty()) {
            UIFactory.mostrarAlerta(Alert.AlertType.WARNING, "Sin datos",
                    "No se generaron registros. Verifica las cantidades.");
            return;
        }

        // --- GENERATION IN BACKGROUND ---
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            List<DetallePedido> generated = new java.util.ArrayList<>();
            int globalCounter = finalStartVal;

            for (RowData data : rowsToProcess) {
                int currentCounter = continuous ? globalCounter : finalStartVal;
                String manga = (data.config != null) ? data.config[0] : "DEFAULT";
                String tShort = (data.config != null) ? data.config[1] : "SAME";
                String tipoBottom = (data.config != null && data.config.length > 2) ? data.config[2] : "DEFAULT";

                for (int i = 0; i < data.qty; i++) {
                    DetallePedido newItem = createItemImpl("JUGADOR", String.valueOf(currentCounter), data.size,
                            genero.name(), manga, tShort, tipoBottom);
                    generated.add(newItem);
                    currentCounter++;
                }
                if (continuous)
                    globalCounter = currentCounter;
            }
            return generated;
        }).thenAccept(generated -> {
            javafx.application.Platform.runLater(() -> {
                onGenerateListener.accept(generated);
            });
        });
    }

    private static class RowData {
        String size;
        int qty;
        String[] config;

        RowData(String size, int qty, String[] config) {
            this.size = size;
            this.qty = qty;
            this.config = config;
        }
    }

    private DetallePedido createItemImpl(String nombre, String numero, String talla, String genero,
            String overrideManga, String overrideShort, String overrideTipoBottom) {
        DetallePedido nuevo = new DetallePedido(nombre, numero, talla, genero);

        // Defaults from Config
        ConfiguracionPrendaDTO cfg = configSupplier != null ? configSupplier.get() : null;

        if (cfg != null) {
            org.example.model.TipoPrenda tipo = cfg.getTipoPrenda();
            boolean top = (tipo != org.example.model.TipoPrenda.SHORT);
            boolean bottom = (tipo == org.example.model.TipoPrenda.SHORT) || cfg.llevaShort();
            boolean socks = cfg.llevaMedias();

            nuevo.setIncludeTop(top);
            nuevo.setIncludeBottom(bottom);
            nuevo.setIncludeSocks(socks);

            // Base Manga setup
            if (cfg.getLargo() != null) {
                switch (cfg.getLargo()) {
                    case MANGA_LARGA:
                    case MANGA_3_4:
                        nuevo.setTipoManga("LARGA");
                        break;
                    case MANGA_CERO:
                        nuevo.setTipoManga("MANGA 0");
                        break;
                    default:
                        nuevo.setTipoManga("CORTA");
                        break;
                }
            }

            // Base Bottom
            if (cfg.getCorteShort() != null) {
                if (cfg.getCorteShort() == org.example.model.TipoCorte.LICRA)
                    nuevo.setTipoBottom("Licra");
                else if (cfg.getCorteShort() == org.example.model.TipoCorte.PANTALONETA)
                    nuevo.setTipoBottom("Pantaloneta");
                else
                    nuevo.setTipoBottom("Short");
            }

        } else {
            nuevo.setIncludeTop(true);
            nuevo.setIncludeBottom(true);
            nuevo.setIncludeSocks(false);
            nuevo.setTipoManga("CORTA");
            nuevo.setTipoBottom("Short");
        }

        // Overrides
        // 1. Manga: If "DEFAULT", keep the one from cfg (already set above). Only
        // override if specific.
        if (overrideManga != null && !overrideManga.equals("DEFAULT")) {
            // Map "SIN MANGA" -> "MANGA 0" logic if needed, or keep as is
            if (overrideManga.equals("SIN MANGA")) {
                nuevo.setTipoManga("MANGA 0");
            } else if (overrideManga.equals("MANGA 3/4")) { // Check exact enum string
                nuevo.setTipoManga("MANGA 3/4"); // Ensure DetallePedido supports this or maps it
            } else {
                nuevo.setTipoManga(overrideManga);
            }
        }
        if (!overrideShort.equals("SAME"))
            nuevo.setTallaShort(overrideShort);
        else
            nuevo.setTallaShort(talla);
        if (!overrideTipoBottom.matches("DEFAULT|NO APLICA"))
            nuevo.setTipoBottom(overrideTipoBottom);

        return nuevo;
    }

}
