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
 * Componente UI para la generación específica de jugadores (Números por Talla).
 */
public class SpecificInputView {

    private final VBox view;
    private Supplier<ConfiguracionPrendaDTO> configSupplier;
    private Consumer<List<DetallePedido>> onGenerateListener;

    private ComboBox<TipoGenero> comboGenero;
    private FlowPane rowsContainer;
    private Button btnGenerate;

    public SpecificInputView(Supplier<ConfiguracionPrendaDTO> configSupplier) {
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

    private VBox createContent() {
        VBox rootWrapper = new VBox(0);
        rootWrapper.setStyle("-fx-background-color: transparent;");

        // Main scroll pane for vertical scrolling
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox container = new VBox(15);
        container.setPadding(new Insets(10, 15, 15, 15));
        container.setStyle("-fx-background-color: white;");
        scrollPane.setContent(container);

        // --- 1. CONFIGURATION SECTION ---
        VBox configGreyBox = new VBox(8);
        configGreyBox.setPadding(new Insets(12));
        configGreyBox.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #ecf0f1; -fx-border-radius: 8;");

        Label lblTopTitle = new Label("1. Configuración de Carga Específica");
        lblTopTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        lblTopTitle.setMaxWidth(Double.MAX_VALUE);
        lblTopTitle.setAlignment(Pos.CENTER_LEFT);

        HBox configRow = new HBox(25);
        configRow.setAlignment(Pos.CENTER_LEFT);
        VBox boxGender = new VBox(4);
        Label lblGen = new Label("Género Base:");
        lblGen.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-weight: bold;");
        comboGenero = new ComboBox<>();
        comboGenero.getItems().setAll(TipoGenero.HOMBRE, TipoGenero.MUJER);
        comboGenero.setValue(TipoGenero.HOMBRE);
        comboGenero.setPrefWidth(160);
        comboGenero.setStyle(
                "-fx-font-size: 13px; -fx-pref-height: 32px; -fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        boxGender.getChildren().addAll(lblGen, comboGenero);
        configRow.getChildren().add(boxGender);
        configGreyBox.getChildren().addAll(lblTopTitle, configRow);

        // --- 2. ACTIONS SECTION ---
        VBox actionsBox = new VBox(8);
        Label lblActionsTitle = new Label("2. Acciones");
        lblActionsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        HBox actionsRow = new HBox(15);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        Button btnAddRow = new Button("Agregar Bloque de Talla");
        FontIcon iconAdd = new FontIcon("mdi2p-plus-circle");
        iconAdd.setIconColor(javafx.scene.paint.Color.WHITE);
        btnAddRow.setGraphic(iconAdd);
        btnAddRow.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 12; -fx-font-size: 13px; -fx-background-radius: 5;");
        btnAddRow.setOnAction(e -> addSizeRow());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblNote = new Label("Nota: Utilice TAB para moverse rápido.");
        lblNote.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px; -fx-font-style: italic;"); // Softer grey

        actionsRow.getChildren().addAll(btnAddRow, spacer, lblNote);
        actionsBox.getChildren().addAll(lblActionsTitle, actionsRow);

        // --- 3. DISTRIBUTION SECTION ---
        HBox distributionHeader = new HBox(15);
        distributionHeader.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(distributionHeader, new Insets(15, 0, 0, 0)); // Extra margin to avoid overlap
        Label lblDistTitle = new Label("Distribución de Tallas (Específica)");
        lblDistTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        lblDistTitle.setMaxWidth(Double.MAX_VALUE);
        lblDistTitle.setAlignment(Pos.CENTER_LEFT);
        Region distSpacer = new Region();
        HBox.setHgrow(distSpacer, Priority.ALWAYS);
        btnGenerate = new Button("GENERAR LISTA");
        FontIcon iconGen = new FontIcon("mdi2c-check-circle");
        iconGen.setIconColor(javafx.scene.paint.Color.WHITE);
        btnGenerate.setGraphic(iconGen);
        btnGenerate.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // Prevent truncation
        btnGenerate.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand; -fx-background-radius: 5;");
        btnGenerate.setOnAction(e -> generate());
        distributionHeader.getChildren().addAll(lblDistTitle, distSpacer, btnGenerate);

        rowsContainer = new FlowPane(15, 15);
        rowsContainer.setPadding(new Insets(15));
        rowsContainer.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 8;");

        container.getChildren().addAll(configGreyBox, actionsBox, distributionHeader, rowsContainer);
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
        // Margin: 40 (padding) + 45 (3 gaps) + 5 (buffer) = 90
        card.prefWidthProperty().bind(rowsContainer.widthProperty().subtract(90).divide(4));
        card.setMinWidth(110); // Safe minimum for very small panels

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

        // Quantity acts as Number of Input Fields in Specific Mode
        VBox rowQty = new VBox(2);
        rowQty.setAlignment(Pos.TOP_LEFT);
        Label lblQty = new Label("Cantidad:");
        lblQty.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        TextField txtQty = new TextField("1");
        txtQty.setMaxWidth(Double.MAX_VALUE);
        txtQty.setAlignment(Pos.CENTER);
        rowQty.getChildren().addAll(lblQty, txtQty);

        body.getChildren().addAll(lblSize, cbSize, rowQty);

        // Container for specific numbers
        FlowPane fpNumbers = new FlowPane(5, 5);
        fpNumbers.setPrefWrapLength(140);
        card.getChildren().add(fpNumbers);

        // Wiring listeners
        txtQty.textProperty().addListener((obs, ov, nv) -> updateNumberFields(fpNumbers, nv));

        // Initial init
        updateNumberFields(fpNumbers, "1");

        // Metadata
        java.util.Map<String, Object> refs = new java.util.HashMap<>();
        refs.put("cbSize", cbSize);
        refs.put("txtQty", txtQty);
        refs.put("fpNumbers", fpNumbers);
        card.setUserData(refs);
        card.getProperties().put("config", new String[] { "DEFAULT", "SAME", "DEFAULT" });

        btnConfig.setOnAction(e -> showConfigDialog(card));

        card.getChildren().addAll(header, body); // body includes the number flowpane via add? No, added separately
        // Wait, body only has size and qty. fpNumbers added to card.
        // Re-check order in original code.
        // Original: card.getChildren().addAll(header, body). Then if specific,
        // card.getChildren().add(fpNumbers).
        // My code above: card.getChildren().add(fpNumbers) AFTER body setup.
        // Then card.getChildren().addAll(header, body) ?? No, this adds them again?
        // Ah, `card` is VBox. I added fpNumbers to card already.
        // Then `card.getChildren().addAll(header, body)` adds them to the end?
        // No, `addAll` appends.
        // So order will be: fpNumbers, header, body. WRONG.

        // Logic fix:
        card.getChildren().clear();
        card.getChildren().addAll(header, body, fpNumbers);

        rowsContainer.getChildren().add(card);
    }

    private void updateNumberFields(FlowPane container, String qtyStr) {
        container.getChildren().clear();
        if (qtyStr == null || qtyStr.trim().isEmpty())
            return;
        try {
            int qty = Integer.parseInt(qtyStr.trim());
            if (qty > 100)
                qty = 100;
            for (int i = 0; i < qty; i++) {
                TextField tf = new TextField();
                tf.setPromptText("#");
                tf.setPrefWidth(35);
                tf.setAlignment(Pos.CENTER);
                tf.setStyle("-fx-font-size: 10px; -fx-background-color: #fff8e1; -fx-border-color: #ffe0b2;");
                container.getChildren().add(tf);
            }
        } catch (Exception e) {
        }
    }

    private void showConfigDialog(Node card) {
        // Same logic as MassiveInputView - duplications are acceptable for now to
        // decouple
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

        String initialManga = curManga;
        if (curManga.equals("DEFAULT") || curManga.equals("USAR DEFECTO"))
            initialManga = "DEFAULT";

        cbManga.setValue(initialManga);
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

        java.util.List<RowData> rowsToProcess = new java.util.ArrayList<>();
        TipoGenero genero = comboGenero.getValue();

        for (Node node : rowsContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                try {
                    java.util.Map<String, Object> refs = (java.util.Map<String, Object>) card.getUserData();
                    ComboBox<String> cb = (ComboBox<String>) refs.get("cbSize");
                    FlowPane fp = (FlowPane) refs.get("fpNumbers");

                    if (cb != null && fp != null) {
                        String size = cb.getValue();
                        if (size != null) {
                            String[] config = (String[]) card.getProperties().get("config");
                            java.util.List<String> nums = new java.util.ArrayList<>();
                            for (Node child : fp.getChildren()) {
                                if (child instanceof TextField) {
                                    String num = ((TextField) child).getText().trim();
                                    if (!num.isEmpty()) {
                                        nums.add(num);
                                    }
                                }
                            }
                            if (!nums.isEmpty()) {
                                rowsToProcess.add(new RowData(size, nums, config));
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }

        if (rowsToProcess.isEmpty()) {
            UIFactory.mostrarAlerta(Alert.AlertType.WARNING, "Sin datos", "No se encontraron números ingresados.");
            return;
        }

        // --- GENERATION IN BACKGROUND ---
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            List<DetallePedido> generated = new java.util.ArrayList<>();
            for (RowData data : rowsToProcess) {
                String manga = (data.config != null) ? data.config[0] : "DEFAULT";
                String tShort = (data.config != null) ? data.config[1] : "SAME";
                String tipoBottom = (data.config != null && data.config.length > 2) ? data.config[2] : "DEFAULT";

                for (String num : data.numbers) {
                    DetallePedido newItem = createItemImpl("JUGADOR", num, data.size, genero.name(),
                            manga, tShort, tipoBottom);
                    generated.add(newItem);
                }
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
        java.util.List<String> numbers;
        String[] config;

        RowData(String size, java.util.List<String> numbers, String[] config) {
            this.size = size;
            this.numbers = numbers;
            this.config = config;
        }
    }

    // Duplicated helper to avoid dependency hell for now
    private DetallePedido createItemImpl(String nombre, String numero, String talla, String genero,
            String overrideManga, String overrideShort, String overrideTipoBottom) {
        DetallePedido nuevo = new DetallePedido(nombre, numero, talla, genero);

        ConfiguracionPrendaDTO cfg = configSupplier != null ? configSupplier.get() : null;
        if (cfg != null) {
            org.example.model.TipoPrenda tipo = cfg.getTipoPrenda();
            boolean top = (tipo != org.example.model.TipoPrenda.SHORT);
            boolean bottom = (tipo == org.example.model.TipoPrenda.SHORT) || cfg.llevaShort();
            boolean socks = cfg.llevaMedias();

            nuevo.setIncludeTop(top);
            nuevo.setIncludeBottom(bottom);
            nuevo.setIncludeSocks(socks);

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

        if (overrideManga != null && !overrideManga.equals("DEFAULT")) {
            if (overrideManga.equals("SIN MANGA")) {
                nuevo.setTipoManga("MANGA 0");
            } else if (overrideManga.equals("MANGA 3/4")) {
                nuevo.setTipoManga("MANGA 3/4");
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

