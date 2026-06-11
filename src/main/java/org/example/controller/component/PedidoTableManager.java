package org.example.controller.component;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import org.example.model.DetallePedido;
import org.example.model.TipoTalla;
import org.example.dto.ConfiguracionPrendaDTO;
import java.util.function.Supplier;

/**
 * A CLEAN builder version for 'Pedido' table.
 * Resolves centering issues by explicitly clearing text when using graphics.
 */
public class PedidoTableManager {

    public static TableView<DetallePedido> build(ObservableList<DetallePedido> data,
            Supplier<ConfiguracionPrendaDTO> configSupplier) {
        TableView<DetallePedido> table = new TableView<>();
        table.getStyleClass().add("clean-table-v3");

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setItems(data);
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 1. INDEX
        table.getColumns().add(createIndexColumn());

        // 2. NOMBRE
        TableColumn<DetallePedido, String> colNombre = createEditableTextColumn("Nombre", "nombre", 200, Pos.CENTER);
        colNombre.setOnEditCommit(e -> e.getRowValue().setNombre(e.getNewValue().toUpperCase()));
        table.getColumns().add(colNombre);

        // 3. TALLA
        TableColumn<DetallePedido, String> colTalla = createEditableTextColumn("Talla", "talla", 75, Pos.CENTER);
        colTalla.setOnEditCommit(e -> e.getRowValue().setTalla(e.getNewValue().toUpperCase()));
        table.getColumns().add(colTalla);

        // 4. NUMERO
        table.getColumns().add(createEditableTextColumn("Número", "numero", 75, Pos.CENTER));

        // 5. GENERO
        table.getColumns().add(createComboColumn("Género", "genero", 110, DetallePedido::setGenero, "HOMBRE", "MUJER"));

        // 6. MANGA
        TableColumn<DetallePedido, String> colManga = createMangaColumn(configSupplier);
        colManga.setOnEditCommit(e -> {
            e.getRowValue().setTipoManga(e.getNewValue());
            table.refresh();
        });
        table.getColumns().add(colManga);

        // 7. SHORT
        table.getColumns().add(createStrictCheckBoxColumn("Short", 80, DetallePedido::isIncludeBottom,
                DetallePedido::setIncludeBottom));

        // 8. TIPO PRENDA INF
        table.getColumns().add(createTipoBottomColumn());

        // 9. TALLA SHORT
        table.getColumns().add(createTallaShortColumn());

        // 10. MEDIAS
        table.getColumns().add(createStrictCheckBoxColumn("Medias", 100, DetallePedido::isIncludeSocks,
                DetallePedido::setIncludeSocks));

        // 10b. TIPO MEDIAS
        table.getColumns().add(createComboColumn("Tipo Medias", "tipoMedias", 120, (row, val) -> {
            row.setTipoMedias(val);
            if ("PROFESIONAL".equals(val)) {
                // Bulk apply to all rows
                for (DetallePedido p : data) {
                    p.setTipoMedias("PROFESIONAL");
                }
                table.refresh();
            }
        }, "PROFESIONAL", "ADULTO", "JUVENIL", "NIÑOS"));

        // 11. ARQUERO
        table.getColumns().add(createStrictCheckBoxColumn("¿Arquero?", 90, DetallePedido::isEsArquero, (p, v) -> {
            p.setEsArquero(v);
            javafx.application.Platform.runLater(table::refresh);
        }));

        // 12. COLOR REF
        table.getColumns().add(createColorColumn());

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setTableMenuButtonVisible(false);
        table.setPrefWidth(1200);

        for (TableColumn<DetallePedido, ?> col : table.getColumns()) {
            col.setResizable(true);
        }

        return table;
    }

    private static TableColumn<DetallePedido, Void> createIndexColumn() {
        TableColumn<DetallePedido, Void> col = new TableColumn<>("#");
        col.setPrefWidth(40);
        col.setMinWidth(40);
        col.setMaxWidth(40);
        col.setSortable(false);
        col.setCellFactory(c -> new TableCell<>() {
            {
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        col.setText(null);
        Label lblHeader = new Label("#");
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.prefWidthProperty().bind(col.widthProperty().subtract(2));
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        col.setGraphic(lblHeader);

        return col;
    }

    private static TableColumn<DetallePedido, String> createEditableTextColumn(String title, String field, double width,
            Pos align) {
        TableColumn<DetallePedido, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setMinWidth(title.equals("Nombre") ? 200 : 90);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setCellValueFactory(new PropertyValueFactory<>(field));

        col.setCellFactory(c -> new TableCell<DetallePedido, String>() {
            private final Label label = new Label();
            private final StackPane wrapper = new StackPane(label);
            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                wrapper.setAlignment(Pos.CENTER);
                setPadding(javafx.geometry.Insets.EMPTY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null); // CRITICAL: Clear text so it doesn't shift the graphic
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    label.setAlignment(align);
                    setGraphic(wrapper);
                }
            }
        });

        col.setText(null);
        Label lblHeader = new Label(title);
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.prefWidthProperty().bind(col.widthProperty().subtract(2));
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        col.setGraphic(lblHeader);

        return col;
    }

    private static TableColumn<DetallePedido, String> createComboColumn(String title, String field, double width,
            java.util.function.BiConsumer<DetallePedido, String> setter,
            String... items) {
        TableColumn<DetallePedido, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setMinWidth(90);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setCellValueFactory(new PropertyValueFactory<>(field));
        col.setCellFactory(c -> new TableCell<>() {
            private final ComboBox<String> cb = new ComboBox<>(
                    javafx.collections.FXCollections.observableArrayList(items));
            private final StackPane wrapper = new StackPane(cb);
            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                wrapper.setAlignment(Pos.CENTER);
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setOnAction(e -> {
                    DetallePedido item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        setter.accept(item, cb.getValue());
                    }
                });

                // --- BLOQUEO TOTAL DE CLICK DERECHO (EVENT FILTER) ---
                cb.addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> {
                    if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        e.consume();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null); 
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    cb.setValue(item);
                    setGraphic(wrapper);
                }
            }
        });

        col.setText(null);
        Label lblHeader = new Label(title);
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.prefWidthProperty().bind(col.widthProperty().subtract(2));
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        col.setGraphic(lblHeader);

        return col;
    }

    private static TableColumn<DetallePedido, String> createMangaColumn(
            Supplier<ConfiguracionPrendaDTO> configSupplier) {
        TableColumn<DetallePedido, String> col = new TableColumn<>("Manga");
        col.setPrefWidth(130);
        col.setMinWidth(100);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setCellValueFactory(new PropertyValueFactory<>("tipoManga"));
        col.setCellFactory(c -> new TableCell<>() {
            private final ComboBox<String> cb = new ComboBox<>();
            private final StackPane wrapper = new StackPane(cb);
            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                wrapper.setAlignment(Pos.CENTER);
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setItems(javafx.collections.FXCollections.observableArrayList("CORTA", "LARGA", "MANGA 0"));
                cb.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        getTableRow().getItem().setTipoManga(cb.getValue());
                    }
                });

                // --- BLOQUEO TOTAL DE CLICK DERECHO (EVENT FILTER) ---
                cb.addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> {
                    if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        e.consume();
                    }
                });
            }

            @Override
            public void startEdit() {
                if (configSupplier != null && configSupplier.get() != null &&
                        configSupplier.get().getCorte() == org.example.model.TipoCorte.REDONDO) {
                    if (cb.getItems().contains("MANGA 0")) {
                        cb.getItems().remove("MANGA 0");
                    }
                }
                super.startEdit();
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null); // CRITICAL
                if (empty) {
                    setGraphic(null);
                } else {
                    cb.setValue(item);
                    setGraphic(wrapper);
                }
            }
        });

        col.setText(null);
        Label lblHeader = new Label("Manga");
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.prefWidthProperty().bind(col.widthProperty().subtract(2));
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        col.setGraphic(lblHeader);

        return col;
    }

    private static TableColumn<DetallePedido, String> createTipoBottomColumn() {
        TableColumn<DetallePedido, String> col = new TableColumn<>("Tipo Inf.");
        col.setPrefWidth(120);
        col.setMinWidth(110);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setCellValueFactory(new PropertyValueFactory<>("tipoBottom"));
        col.setCellFactory(c -> new TableCell<>() {
            private final ComboBox<String> cb = new ComboBox<>(javafx.collections.FXCollections
                    .observableArrayList("Short", "Licra", "Pantaloneta", "Falda", "Bermuda"));
            private final StackPane wrapper = new StackPane(cb);
            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                wrapper.setAlignment(Pos.CENTER);
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        getTableRow().getItem().setTipoBottom(cb.getValue());
                    }
                });

                // --- BLOQUEO TOTAL DE CLICK DERECHO (EVENT FILTER) ---
                cb.addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> {
                    if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        e.consume();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null); // CRITICAL
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else if (!getTableRow().getItem().isIncludeBottom()) {
                    setGraphic(null);
                    setText("-");
                } else {
                    cb.setValue(item);
                    setGraphic(wrapper);
                }
            }
        });

        col.setText(null);
        Label lblHeader = new Label("Tipo Inf.");
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.prefWidthProperty().bind(col.widthProperty().subtract(2));
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        col.setGraphic(lblHeader);

        return col;
    }

    private static TableColumn<DetallePedido, String> createTallaShortColumn() {
        TableColumn<DetallePedido, String> col = new TableColumn<>("T. Short");
        col.setPrefWidth(100);
        col.setMinWidth(90);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setCellValueFactory(new PropertyValueFactory<>("tallaShort"));
        col.setCellFactory(c -> new TableCell<>() {
            private final ComboBox<String> cb = new ComboBox<>(
                    javafx.collections.FXCollections.observableArrayList(TipoTalla.getLabels()));
            private final StackPane wrapper = new StackPane(cb);
            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                wrapper.setAlignment(Pos.CENTER);
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        getTableRow().getItem().setTallaShort(cb.getValue());
                    }
                });

                // --- BLOQUEO TOTAL DE CLICK DERECHO (EVENT FILTER) ---
                cb.addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> {
                    if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        e.consume();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null); // CRITICAL
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else if (!getTableRow().getItem().isIncludeBottom()) {
                    setGraphic(null);
                    setText("-");
                } else {
                    cb.setValue(item);
                    setGraphic(wrapper);
                }
            }
        });

        col.setText(null);
        Label lblHeader = new Label("T. Short");
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.prefWidthProperty().bind(col.widthProperty().subtract(2));
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        col.setGraphic(lblHeader);

        return col;
    }

    private interface BooleanGetter {
        boolean get(DetallePedido p);
    }

    private interface BooleanSetter {
        void set(DetallePedido p, boolean v);
    }

    private static TableColumn<DetallePedido, Boolean> createStrictCheckBoxColumn(String title, double width,
            BooleanGetter getter, BooleanSetter setter) {
        TableColumn<DetallePedido, Boolean> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setMinWidth(title.contains("Arquero") ? 100 : 70);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setCellValueFactory(
                features -> new javafx.beans.property.SimpleBooleanProperty(getter.get(features.getValue())));

        col.setCellFactory(c -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            private final StackPane wrapper = new StackPane(cb);
            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setPadding(javafx.geometry.Insets.EMPTY);
                wrapper.setAlignment(Pos.CENTER);
                cb.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        setter.set(getTableRow().getItem(), cb.isSelected());
                    }
                });

                // --- ESTILO MANEJADO POR CSS (styles.css -> .clean-table-v3 .table-check-box)
                // ---
                cb.getStyleClass().add("table-check-box");
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(null); // CRITICAL: Stop boolean string from shifting the checkbox
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    cb.setSelected(getter.get(getTableRow().getItem()));
                    setGraphic(wrapper);
                }
            }
        });

        col.setText(null);
        Label lblHeader = new Label(title);
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.prefWidthProperty().bind(col.widthProperty().subtract(2));
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        col.setGraphic(lblHeader);

        return col;
    }

    private static TableColumn<DetallePedido, javafx.scene.paint.Color> createColorColumn() {
        TableColumn<DetallePedido, javafx.scene.paint.Color> col = new TableColumn<>("Color Ref.");
        col.setPrefWidth(110);
        col.setMinWidth(100);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setCellValueFactory(cellData -> cellData.getValue().colorArqueroProperty());
        col.setCellFactory(c -> new TableCell<>() {
            private MenuButton pickerBtn;
            private final StackPane wrapper = new StackPane();

            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setPadding(javafx.geometry.Insets.EMPTY);
                wrapper.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(javafx.scene.paint.Color item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null || !getTableRow().getItem().isEsArquero()) {
                    setGraphic(null);
                } else {
                    if (pickerBtn == null) {
                        pickerBtn = org.example.utils.UIFactory.createColorMenuButton(
                                item != null ? item : javafx.scene.paint.Color.YELLOW,
                                "Seleccionar color de referencia",
                                chosen -> {
                                    if (getTableRow() != null && getTableRow().getItem() != null) {
                                        getTableRow().getItem().setColorArquero(chosen);
                                    }
                                },
                                null // No eyedropper in table for now
                        );
                        pickerBtn.setPrefWidth(90);
                        pickerBtn.setMaxWidth(90);
                        wrapper.getChildren().setAll(pickerBtn);
                    } else {
                        org.example.utils.UIFactory.setColorMenuButtonColor(pickerBtn, item);
                    }
                    setGraphic(wrapper);
                }
            }
        });

        col.setText(null);
        Label lblHeader = new Label("Color Ref.");
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.prefWidthProperty().bind(col.widthProperty().subtract(2));
        lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        col.setGraphic(lblHeader);

        return col;
    }
}

