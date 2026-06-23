package org.example.controller.delegate;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;

import javafx.scene.layout.*;
// import javafx.scene.Cursor; // REMOVED unused
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.DetallePedido;
import org.example.model.TipoGenero;
import org.example.model.TipoTalla;
import org.example.utils.ExcelImporter;
import org.example.utils.UIFactory;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.Optional;

// Forced recompile check
public class JugadoresDelegate {

    private final TableView<DetallePedido> tabla;
    private TableColumn<DetallePedido, Boolean> colBottom; // Promoted to field for direct access
    // Old references kept to hide them if possible or just ignore.
    // We will build our own inputs for the "Manual" tab.
    private TextField txtNombreManual;
    private TextField txtNumeroManual;
    private ComboBox<String> comboTallaManual;
    private ComboBox<String> comboGeneroManual; // New Field

    // View Fields for Global Access
    private org.example.controller.uicomponent.MassiveInputView massiveView;
    private org.example.controller.uicomponent.SpecificInputView specificView;
    private TabPane tabPane;
    private Tab tabMassive;
    private Tab tabSpecific;
    private Tab tabSimple;
    private Tab tabManual; // New Field
    private FlowPane simpleCardsContainer;

    private Pane contentParent; // New Field
    private javafx.scene.control.SplitPane mainSplitPane;
    private VBox manualLayoutVBox;
    private StackPane tableContainer;
    private HBox toolbarBottom;
    private Node manualFormNode; // New Field
    private StackPane tabPaneContainer; // New Field

    private final Label lblTotal;

    private final ObservableList<DetallePedido> listaJugadores;

    // Use Configuration Supplier for robust logic
    private Supplier<org.example.dto.ConfiguracionPrendaDTO> configSupplier;

    public void setConfigSupplier(Supplier<org.example.dto.ConfiguracionPrendaDTO> supplier) {
        this.configSupplier = supplier;
        if (massiveView != null)
            massiveView.setConfigSupplier(supplier);
        if (specificView != null)
            specificView.setConfigSupplier(supplier);
    }

    // Legacy support if needed, but configSupplier replaces it
    public void setGeneroSupplier(Supplier<TipoGenero> supplier) {
        // No-op or map if needed, but we prefer configSupplier
    }

    // UI Components for Massive Mode (Restored)
    // UI Components for Massive Mode

    public JugadoresDelegate(TableView<DetallePedido> tabla,
            TextField txtOldNombre,
            TextField txtOldNumero,
            ComboBox<String> comboOldTalla,
            Label lblTotal,
            ObservableList<DetallePedido> listaCompartida) {
        this.tabla = tabla;
        this.lblTotal = lblTotal;
        this.listaJugadores = listaCompartida;

        // Hide the old inputs passed from FXML as we are replacing the UI
        if (txtOldNombre != null && txtOldNombre.getParent() != null) {
            txtOldNombre.getParent().setVisible(false);
            txtOldNombre.getParent().setManaged(false);
        }

        inicializarTabla();
        setupRedesignedUI();
    }

    // --- Field Cleaned ---

    private TableColumn<DetallePedido, Boolean> colTop; // Promoted to field for direct access
    private TableColumn<DetallePedido, Boolean> colSocks; // Promoted to field for direct access

    private void inicializarTabla() {
        // --- Table Styling (Robust CSS Injection) ---
        // Styles are in styles.css

        // Apply row factory for hover and selection effects
        tabla.setRowFactory(tv -> {
            TableRow<DetallePedido> row = new TableRow<>();
            row.styleProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
                String baseStyle = "-fx-border-width: 0 0 0 4; ";
                if (row.isSelected()) {
                    return baseStyle
                            + "-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-text-fill: black; -fx-padding: 0;";
                } else if (row.isHover()) {
                    return baseStyle + "-fx-background-color: #f1f4f8; -fx-border-color: transparent; -fx-padding: 0;";
                } else if (row.getIndex() % 2 == 0) {
                    return baseStyle + "-fx-background-color: #ffffff; -fx-border-color: transparent; -fx-padding: 0;";
                } else {
                    return baseStyle + "-fx-background-color: #fafbfc; -fx-border-color: transparent; -fx-padding: 0;";
                }
            }, row.selectedProperty(), row.hoverProperty(), row.indexProperty()));
            return row;
        });

        // Context Menu
        setupContextMenu();

        tabla.setEditable(true);
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // --- COLUMNS INITIALIZATION ---
        // DELEGATE TO PedidoTableManager (Source of Truth)
        // We just need to bind our internal fields to the existing columns for logic
        // reference.

        if (tabla.getColumns().size() > 9) {
            try {
                // Short is index 6
                if ("Short".equalsIgnoreCase(tabla.getColumns().get(6).getText()) ||
                        "Include Bottom".equals(tabla.getColumns().get(6).getText())) {
                    colBottom = (TableColumn<DetallePedido, Boolean>) tabla.getColumns().get(6);
                }

                // Socks match index 9
                if ("Medias".equalsIgnoreCase(tabla.getColumns().get(9).getText())) {
                    colSocks = (TableColumn<DetallePedido, Boolean>) tabla.getColumns().get(9);
                }

            } catch (Exception e) {
                System.err.println("Error binding Delegate columns: " + e.getMessage());
            }
        } else {
            // Fallback if table is empty (should not happen with Builder)
            System.out.println("JugadoresDelegate: Table has no columns, assuming Builder hasn't run yet or failed.");
        }

        // NO CLEARING, NO ADDING. JUST SORT POLICY.
        setupSmartSort();

        // PedidoTableManager now handles the initial policy, ensuring scrollbars are
        // possible
        // CRITICAL: Use UNCONSTRAINED policy to ensure horizontal scrollbar appears
        // when needed
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tabla.setTableMenuButtonVisible(false);
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEditar = new MenuItem("Editar Individuo");
        itemEditar.setGraphic(UIFactory.crearIcono("mdi2p-pencil", 16, "#3498db"));
        itemEditar.setOnAction(event -> editarJugadorSeleccionado());

        MenuItem itemBatchManga = new MenuItem("Cambiar Manga");
        itemBatchManga.setGraphic(UIFactory.crearIcono("mdi2t-tshirt-crew", 16, "#e67e22"));
        itemBatchManga.setOnAction(e -> {
            List<DetallePedido> seleccion = tabla.getSelectionModel().getSelectedItems();
            if (seleccion.isEmpty())
                return;
            ChoiceDialog<String> dialog = new ChoiceDialog<>("CORTA", "CORTA", "LARGA", "MANGA 0");
            dialog.setTitle("Cambio de Manga");
            dialog.setHeaderText("Cambiar manga para " + seleccion.size() + " jugadores seleccionados");
            UIFactory.estilizarDialogo(dialog);
            dialog.showAndWait().ifPresent(nuevaManga -> {
                seleccion.forEach(p -> p.setTipoManga(nuevaManga));
                tabla.refresh();
            });
        });

        MenuItem itemBatchGenero = new MenuItem("Cambiar Corte/Género");
        itemBatchGenero.setGraphic(UIFactory.crearIcono("mdi2a-account-edit", 16, "#e91e63"));
        itemBatchGenero.setOnAction(e -> {
            List<DetallePedido> seleccion = tabla.getSelectionModel().getSelectedItems();
            if (seleccion.isEmpty())
                return;
            ChoiceDialog<String> dialog = new ChoiceDialog<>("HOMBRE", "HOMBRE", "MUJER");
            dialog.setTitle("Cambio de Corte");
            UIFactory.estilizarDialogo(dialog);
            dialog.showAndWait().ifPresent(nuevoGen -> {
                seleccion.forEach(p -> p.setGenero(nuevoGen));
                tabla.refresh();
            });
        });

        MenuItem itemBatchShort = new MenuItem("Cambiar Talla Inferior");
        itemBatchShort.setGraphic(UIFactory.crearIcono("mdi2r-ruler", 16, "#2ecc71"));
        itemBatchShort.setOnAction(e -> {
            List<DetallePedido> seleccion = tabla.getSelectionModel().getSelectedItems();
            if (seleccion.isEmpty())
                return;
            ChoiceDialog<String> dialog = new ChoiceDialog<>("IGUAL A CAMISETA");
            dialog.getItems().addAll(TipoTalla.getLabels());
            dialog.setTitle("Cambio de Talla Inferior");
            UIFactory.estilizarDialogo(dialog);
            dialog.showAndWait().ifPresent(nuevaTalla -> {
                String val = nuevaTalla.equals("IGUAL A CAMISETA") ? "SAME" : nuevaTalla;
                seleccion.forEach(p -> p.setTallaShort(val));
                tabla.refresh();
            });
        });

        MenuItem itemEliminar = new MenuItem("Eliminar Seleccionados");
        itemEliminar.setGraphic(UIFactory.crearIcono("mdi2t-trash-can", 16, "#e74c3c"));
        itemEliminar.setOnAction(event -> eliminarJugadorSeleccionado());

        contextMenu.getItems().addAll(itemEditar, new SeparatorMenuItem(), itemBatchManga, itemBatchGenero,
                itemBatchShort, new SeparatorMenuItem(), itemEliminar);
        tabla.setContextMenu(contextMenu);
    }

    private void setupSmartSort() {
        tabla.setSortPolicy(tv -> {
            TableColumn<DetallePedido, ?> sortCol = null;
            if (!tv.getSortOrder().isEmpty()) {
                sortCol = tv.getSortOrder().get(0);
            }

            if (sortCol != null && "TALLA".equalsIgnoreCase(sortCol.getText())) {
                boolean isAsc = sortCol.getSortType() == TableColumn.SortType.ASCENDING;

                // Delegate to RosterSorter
                java.util.List<DetallePedido> sorted = org.example.logic.RosterSorter.sort(tabla.getItems(), isAsc);
                tabla.getItems().setAll(sorted);

                return true;
            } else {
                try {
                    javafx.collections.FXCollections.sort(tv.getItems(), tv.getComparator());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    // Sorting logic moved to org.example.logic.RosterSorter

    public void updateGranularColumnsVisibility(boolean showTop, boolean showBottom, boolean showSocks) {
        if (colTop != null)
            colTop.setVisible(showTop);
        // FORCE VISIBLE: User wants to be able to add Short/Socks optionally even if
        // config is Jersey Only
        if (colBottom != null)
            colBottom.setVisible(true);
        if (colSocks != null)
            colSocks.setVisible(true);
    }

    public void updateColumnLabels(String bottomLabel) {
        // Find existing column and update header
        for (TableColumn<DetallePedido, ?> col : tabla.getColumns()) {
            String txt = col.getText();
            Label graphicLabel = null;

            // Handle PedidoTableManager (Null Text, Label Graphic)
            if (txt == null && col.getGraphic() instanceof Label) {
                graphicLabel = (Label) col.getGraphic();
                txt = graphicLabel.getText();
            }

            if (txt == null)
                continue;

            if (txt.equalsIgnoreCase("Prenda Inf.") ||
                    txt.equalsIgnoreCase("Short") ||
                    txt.equalsIgnoreCase("Licra") ||
                    txt.equalsIgnoreCase("Pantaloneta") ||
                    txt.equalsIgnoreCase("Falda") ||
                    txt.equalsIgnoreCase("Tipo Inf.")) {

                String newLabel = bottomLabel != null ? bottomLabel : "Prenda Inf.";

                if (graphicLabel != null) {
                    graphicLabel.setText(newLabel);
                } else {
                    col.setText(newLabel);
                }
            }
        }
    }

    private void setupRedesignedUI() {
        if (tabla.getParent() instanceof Pane) {
            Pane parent = (Pane) tabla.getParent();

            // We want to replace the entire content of this Tab's AnchorPane
            // because the FXML has old inputs we don't want.

            // Guard: If the parent has already been redesigned, stop here to avoid duplication
            if (parent.getProperties().containsKey("jugadores_redesigned") || parent instanceof StackPane) {
                return;
            }

            // 1. Create the new Layout (SPLIT PANE)
            javafx.scene.control.SplitPane mainContainer = new javafx.scene.control.SplitPane();
            mainContainer.setPadding(new Insets(10));
            mainContainer.setStyle("-fx-background-color: #f4f6f9; -fx-padding: 10;");

            // --- LEFT PANEL (Inputs & Config) ---
            VBox leftPanel = new VBox(10);
            leftPanel.setMinWidth(320);

            VBox.setVgrow(leftPanel, Priority.ALWAYS); // Grow vertically

            // HEADER / TABS
            tabPane = new TabPane();
            tabPane.setStyle("-fx-background-color: transparent;");
            VBox.setVgrow(tabPane, Priority.ALWAYS);

            // TAB 1: GENERACIÓN MASIVA
            tabMassive = new Tab("Masiva"); // Shortened title
            tabMassive.setClosable(false);
            massiveView = new org.example.controller.uicomponent.MassiveInputView(
                    configSupplier);

            massiveView.setOnGenerate(items -> {
                listaJugadores.addAll(items);
                actualizarContador();
                org.example.utils.UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.INFORMATION,
                        "Lista Actualizada",
                        "Se añadieron " + items.size() + " prendas a la lista.");
            });
            tabMassive.setContent(massiveView.getView());

            // TAB 2: NUMERACIÓN ESPECÍFICA
            tabSpecific = new Tab("Específica"); // Shortened title
            tabSpecific.setClosable(false);
            specificView = new org.example.controller.uicomponent.SpecificInputView(
                    configSupplier);

            specificView.setOnGenerate(items -> {
                listaJugadores.addAll(items);
                actualizarContador();
                org.example.utils.UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.INFORMATION,
                        "Lista Actualizada",
                        "Se añadieron " + items.size() + " prendas a la lista.");
            });
            tabSpecific.setContent(specificView.getView());

            // TAB 3: CANTIDADES SIMPLES (NEW)
            tabSimple = new Tab("Simples"); // Shortened title
            tabSimple.setClosable(false);
            tabSimple.setContent(createSimpleContent());

            // TAB 4: INGRESO MANUAL
            tabManual = new Tab("Manual");
            tabManual.setClosable(false);
            // Manual tab content is now just the top form
            tabManual.setContent(new Region()); // Tab content is empty, we handle the layout externally

            tabPane.getTabs().addAll(tabMassive, tabSpecific, tabSimple, tabManual);

            // Wrap TabPane in Styled Container
            tabPaneContainer = createStyledContainer(tabPane);
            tabPaneContainer.setMinHeight(60.0); // CRITICAL: Prevent header collapse
            VBox.setVgrow(tabPaneContainer, Priority.ALWAYS);

            // Add Card to Left Panel
            leftPanel.getChildren().add(tabPaneContainer);

            // --- RIGHT PANEL (Table & Actions) ---
            VBox rightPanel = new VBox(10);
            rightPanel.setPadding(new Insets(0, 0, 0, 10));

            // TOOLBAR (Bottom controls)
            toolbarBottom = new HBox(10);
            toolbarBottom.setAlignment(Pos.CENTER_RIGHT);
            toolbarBottom.setPadding(new Insets(5, 0, 0, 0));

            Button btnClearNames = new Button("Borrar Nombres");
            FontIcon iconName = new FontIcon("mdi2a-account-remove");
            iconName.setIconColor(javafx.scene.paint.Color.WHITE);
            btnClearNames.setGraphic(iconName);
            btnClearNames.setStyle(
                    "-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 15 8 15;");

            Button btnClearNumbers = new Button("Borrar Números");
            FontIcon iconNum = new FontIcon("mdi2f-format-list-numbered");
            iconNum.setIconColor(javafx.scene.paint.Color.WHITE);
            btnClearNumbers.setGraphic(iconNum);
            btnClearNumbers.setStyle(
                    "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 15 8 15;");

            Button btnLimpiar = new Button("VACIAR TODO");
            FontIcon iconBroom = new FontIcon("mdi2b-broom");
            iconBroom.setIconColor(javafx.scene.paint.Color.WHITE);
            btnLimpiar.setGraphic(iconBroom);
            btnLimpiar.setStyle(
                    "-fx-background-color: #ff4757; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 15 8 15;");
            btnLimpiar.setOnAction(e -> limpiarTodo());

            btnClearNames.setOnAction(e -> clearColumn("names"));
            btnClearNumbers.setOnAction(e -> clearColumn("numbers"));

            toolbarBottom.getChildren().addAll(btnClearNames, btnClearNumbers, btnLimpiar);

            // TABLE Container
            parent.getChildren().remove(tabla);
            tableContainer = createStyledContainer(tabla);
            tabla.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            VBox.setVgrow(tableContainer, Priority.ALWAYS);

            rightPanel.getChildren().addAll(tableContainer, toolbarBottom);

            // Assemble Split Pane
            mainSplitPane = mainContainer;
            mainSplitPane.getItems().addAll(leftPanel, rightPanel);
            mainSplitPane.setDividerPositions(0.40);

            // Ensure Tabs fit
            leftPanel.setMinWidth(320.0);

            // Remove rigid bindings to allow SplitPane to work normally and prevent
            // squashing
            // leftPanel.maxWidthProperty().bind(...) // REMOVED
            // leftPanel.minWidthProperty().bind(...) // REMOVED

            manualLayoutVBox = new VBox(5);
            manualLayoutVBox.setPadding(new Insets(15));
            manualLayoutVBox.setStyle("-fx-background-color: white;"); // Container is now white
            VBox.setVgrow(manualLayoutVBox, Priority.ALWAYS);
            manualFormNode = createManualForm();

            // --- SELECTION LISTENER ---
            contentParent = parent;
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                refreshLayout(newTab);
            });

            // Initial Layout
            contentParent.getChildren().clear();
            contentParent.getChildren().add(mainSplitPane);
            contentParent.getProperties().put("jugadores_redesigned", true);

            // 3. Anchor logic if parent is AnchorPane
            if (parent instanceof AnchorPane) {
                AnchorPane.setTopAnchor(mainContainer, 0.0);
                AnchorPane.setBottomAnchor(mainContainer, 0.0);
                AnchorPane.setLeftAnchor(mainContainer, 0.0);
                AnchorPane.setRightAnchor(mainContainer, 0.0);
            }

            // Fix: Add Grow Priority for VBox/HBox parents
            VBox.setVgrow(mainContainer, Priority.ALWAYS);
            HBox.setHgrow(mainContainer, Priority.ALWAYS);

            // Allow shrinking (Table handles internal resize policy)
            mainContainer.setMinSize(0, 0);
        }
    }

    private void refreshLayout(Tab selectedTab) {
        if (selectedTab == tabManual) {
            // Remove TabPane from Split Layout
            VBox leftPanel = (VBox) mainSplitPane.getItems().get(0);
            leftPanel.getChildren().clear();

            // Re-parent Table and TabPane to Manual Layout
            // We want Tab Headers at top, then Form, then Table
            manualLayoutVBox.setSpacing(0);
            manualLayoutVBox.setPadding(new Insets(2, 15, 5, 15)); // Minimal top padding

            manualLayoutVBox.getChildren().setAll(tabPaneContainer, manualFormNode, tableContainer, toolbarBottom);

            // Add Margin to Form to show white container background
            // Align Left/Right (8px) to match TabPaneContainer padding so they are flush
            VBox.setMargin(manualFormNode, new Insets(0, 8, 5, 8));

            // Limit TabPane height in Manual view to just headers
            // Reduce internal padding of the container (ShadowPane is tabPaneContainer)
            tabPaneContainer.setPadding(new Insets(0, 8, 0, 8));

            tabPaneContainer.setMinHeight(55); // Increased to 55 for selection indicator
            tabPaneContainer.setMaxHeight(55);
            VBox.setVgrow(tabPaneContainer, Priority.NEVER);

            contentParent.getChildren().setAll(manualLayoutVBox);
            applyAnchors(manualLayoutVBox);
        } else {
            // Restore TabPane to Left Panel
            VBox leftPanel = (VBox) mainSplitPane.getItems().get(0);
            if (!leftPanel.getChildren().contains(tabPaneContainer)) {
                tabPaneContainer.setMaxHeight(Double.MAX_VALUE);
                VBox.setVgrow(tabPaneContainer, Priority.ALWAYS);
                leftPanel.getChildren().setAll(tabPaneContainer);
            }

            // Restore Table to Split Pane (Right Panel)
            VBox rightPanel = (VBox) mainSplitPane.getItems().get(1);
            if (!rightPanel.getChildren().contains(tableContainer)) {
                rightPanel.getChildren().setAll(tableContainer, toolbarBottom);
            }
            contentParent.getChildren().setAll(mainSplitPane);
            applyAnchors(mainSplitPane);
        }
    }

    private void applyAnchors(Node node) {
        if (contentParent instanceof AnchorPane) {
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);
        }
    }

    private Node createManualForm() {
        HBox form = new HBox(10); // Changed to HBox for single line control
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(10));
        // Using matching grey background from SpecificInputView
        form.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #bdc3c7; -fx-background-radius: 8; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        // Fields
        txtNombreManual = new TextField();
        txtNombreManual.setPromptText("Nombre");
        txtNombreManual.setPrefWidth(180); // Compact

        txtNumeroManual = new TextField();
        txtNumeroManual.setPromptText("#");
        txtNumeroManual.setPrefWidth(60); // Compact
        txtNumeroManual.setAlignment(Pos.CENTER);

        comboTallaManual = new ComboBox<>();
        comboTallaManual.getItems().addAll(org.example.model.TipoTalla.getLabels());
        comboTallaManual.setPromptText("Talla");
        comboTallaManual.setPrefWidth(110); // Increased from 90 for readability

        comboGeneroManual = new ComboBox<>();
        comboGeneroManual.getItems().addAll("HOMBRE", "MUJER");
        comboGeneroManual.getSelectionModel().selectFirst();
        comboGeneroManual.setPrefWidth(140); // Increased from 110 for readability

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("AGREGAR");
        FontIcon iconAdd = new FontIcon("mdi2p-plus-circle");
        iconAdd.setIconColor(javafx.scene.paint.Color.WHITE);
        btnAdd.setGraphic(iconAdd);
        btnAdd.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-size: 13px; -fx-background-radius: 5;");
        btnAdd.setOnAction(e -> agregarJugadorManual());

        Button btnImport = new Button("Excel");
        FontIcon iconExcel = new FontIcon("mdi2f-file-excel");
        iconExcel.setIconColor(javafx.scene.paint.Color.WHITE);
        btnImport.setGraphic(iconExcel);
        btnImport.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-size: 13px; -fx-background-radius: 5;");
        btnImport.setOnAction(e -> importarExcelManual());

        form.getChildren().addAll(
                new Label("1. Ingreso:"),
                txtNombreManual, txtNumeroManual, comboTallaManual, comboGeneroManual,
                spacer, // Added Spacer
                btnAdd, btnImport);

        // Returning Form directly implies it will take natural height.
        // Parent VBox should handle it.
        form.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE); // CRITICAL: Prevent collapse
        return form;
    }

    private void importarExcelManual() {
        if (contentParent.getScene() != null) {
            importarExcel((javafx.stage.Stage) contentParent.getScene().getWindow());
        }
    }

    // --- HELPER CREATION LOGIC ---

    private DetallePedido crearItem(String nombre, String numero, String talla, String genero) {
        DetallePedido nuevo = new DetallePedido(nombre, numero, talla, genero);

        // Logic based on Active Configuration
        if (configSupplier != null && configSupplier.get() != null) {
            org.example.dto.ConfiguracionPrendaDTO cfg = configSupplier.get();
            org.example.model.TipoPrenda tipo = cfg.getTipoPrenda();

            // 1. TOP (Camiseta)
            // Included unless it's ONLY Short
            boolean top = (tipo != org.example.model.TipoPrenda.SHORT);
            nuevo.setIncludeTop(top);

            // 2. BOTTOM (Short)
            // Included if Short-only OR Conjunto/Camiseta explicitly has it
            boolean bottom = (tipo == org.example.model.TipoPrenda.SHORT) || cfg.llevaShort();
            nuevo.setIncludeBottom(bottom);

            // 3. SOCKS (Medias)
            // Included only if explicitly configured
            boolean socks = cfg.llevaMedias();
            nuevo.setIncludeSocks(socks);

            // 4. MANGA (Sleeve)
            if (cfg.getLargo() != null) {
                switch (cfg.getLargo()) {
                    case MANGA_LARGA:
                    case MANGA_3_4:
                        nuevo.setTipoManga("LARGA");
                        break;
                    case MANGA_CERO:
                        nuevo.setTipoManga("MANGA 0");
                        break;
                    case MANGA_CORTA:
                    default:
                        nuevo.setTipoManga("CORTA");
                        break;
                }
            }

            // 5. TIPO BOTTOM (Short/Licra/...)
            if (cfg.getCorteShort() != null) {
                org.example.model.TipoCorte c = cfg.getCorteShort();
                if (c == org.example.model.TipoCorte.LICRA)
                    nuevo.setTipoBottom("Licra");
                else if (c == org.example.model.TipoCorte.PANTALONETA)
                    nuevo.setTipoBottom("Pantaloneta");
                else
                    nuevo.setTipoBottom("Short");
            } else {
                nuevo.setTipoBottom("Short");
            }

        } else {
            // Fallback to UI columns if config is missing (legacy safety)
            if (colTop != null)
                nuevo.setIncludeTop(colTop.isVisible());
            if (colBottom != null)
                nuevo.setIncludeBottom(colBottom.isVisible());
            if (colSocks != null)
                nuevo.setIncludeSocks(colSocks.isVisible());
            nuevo.setTipoBottom("Short");
        }

        return nuevo;
    }

    public void agregarJugadorManual() {
        String nombre = txtNombreManual.getText().trim();
        String numero = txtNumeroManual.getText().trim();
        String talla = comboTallaManual.getValue();

        if (talla == null) {
            UIFactory.mostrarAlerta(Alert.AlertType.WARNING, "Faltan datos", "Selecciona Talla.");
            return;
        }

        String genero = comboGeneroManual.getValue();
        if (genero == null)
            genero = "HOMBRE";

        DetallePedido nuevo = crearItem(nombre, numero, talla, genero);
        listaJugadores.add(nuevo);

        txtNombreManual.clear();
        txtNumeroManual.clear();
        actualizarContador();
    }

    // Compatibility method for PedidoController
    public void agregarJugador() {
        agregarJugadorManual();
    }

    /**
     * Refresca la tabla y el contador de jugadores.
     * Invocado por el controlador para asegurar que la UI refleje el estado más reciente.
     */
    public void cargarListaJugadores() {
        if (tabla != null) {
            tabla.refresh();
        }
        actualizarContador();
    }

    public void importarExcel(Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Excel de Jugadores");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        File file = fileChooser.showOpenDialog(ownerStage);
        if (file != null) {
            try {
                // ExcelImporter loads data but doesn't know about UI state.
                // We need to post-process the imported items or update ExcelImporter.
                // Let's post-process here.
                List<DetallePedido> importados = ExcelImporter.importarJugadores(file);
                if (!importados.isEmpty()) {
                    // Update flags
                    if (configSupplier != null && configSupplier.get() != null) {
                        org.example.dto.ConfiguracionPrendaDTO cfg = configSupplier.get();
                        org.example.model.TipoPrenda tipo = cfg.getTipoPrenda();

                        boolean top = (tipo != org.example.model.TipoPrenda.SHORT);
                        boolean bottom = (tipo == org.example.model.TipoPrenda.SHORT) || cfg.llevaShort();
                        boolean socks = cfg.llevaMedias();

                        for (DetallePedido p : importados) {
                            p.setIncludeTop(top);
                            p.setIncludeBottom(bottom);
                            p.setIncludeSocks(socks);

                            // Apply Sleeve Logic from Config
                            if (cfg.getLargo() != null) {
                                switch (cfg.getLargo()) {
                                    case MANGA_LARGA:
                                    case MANGA_3_4:
                                        p.setTipoManga("LARGA");
                                        break;
                                    case MANGA_CERO:
                                        p.setTipoManga("MANGA 0");
                                        break;
                                    case MANGA_CORTA:
                                    default:
                                        p.setTipoManga("CORTA");
                                        break;
                                }
                            }
                        }
                    } else {
                        // Fallback to column visibility if config missing
                        boolean top = (colTop != null && colTop.isVisible());
                        boolean bottom = (colBottom != null && colBottom.isVisible());
                        boolean socks = (colSocks != null && colSocks.isVisible());

                        for (DetallePedido p : importados) {
                            p.setIncludeTop(top);
                            p.setIncludeBottom(bottom);
                            p.setIncludeSocks(socks);
                            p.setTipoManga("CORTA"); // Default fallback
                        }
                    }

                    listaJugadores.addAll(importados);
                    actualizarContador();
                    UIFactory.mostrarAlerta(Alert.AlertType.INFORMATION, "Importación",
                            "Se cargaron " + importados.size());
                }
            } catch (IOException e) {
                UIFactory.mostrarAlerta(Alert.AlertType.ERROR, "Error", e.getMessage());
            }
        }
    }

    private void eliminarJugadorSeleccionado() {
        DetallePedido seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            listaJugadores.remove(seleccionado);
            actualizarContador();
        }
    }

    private void editarJugadorSeleccionado() {
        DetallePedido seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            // Fill manual inputs
            tabla.getSelectionModel().clearSelection(); // Deselect to avoid confusion
            // Switch to manual Tab if possible? accessing tabPane is hard here without ref

            // Simple fallback: Show manual dialog or fill inputs
            txtNombreManual.setText(seleccionado.getNombre());
            txtNumeroManual.setText(seleccionado.getNumero());
            comboTallaManual.setValue(seleccionado.getTalla());

            listaJugadores.remove(seleccionado);
            actualizarContador();

            UIFactory.mostrarAlerta(Alert.AlertType.INFORMATION, "Editando",
                    "Los datos se pasaron al formulario manual.");
        }
    }

    public void limpiarTodo() {
        listaJugadores.clear();
        actualizarContador();
    }

    private void actualizarContador() {
        if (lblTotal != null)
            lblTotal.setText("Total Prendas: " + listaJugadores.size());
    }

    private void clearColumn(String type) {
        if (listaJugadores.isEmpty())
            return;
        boolean confirm = UIFactory.mostrarConfirmacion("¿Borrar columna?",
                "¿Seguro de borrar los " + (type.equals("names") ? "nombres" : "números") + "?");
        if (!confirm)
            return;
        for (DetallePedido p : listaJugadores) {
            if (type.equals("names"))
                p.setNombre("");
            else
                p.setNumero("");
        }
        tabla.refresh();
    }

    private Node createSimpleContent() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblInfo = new Label("Distribución de Tallas (Tarjetas)");
        lblInfo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        simpleCardsContainer = new FlowPane(10, 10);
        simpleCardsContainer.setPadding(new Insets(10));
        simpleCardsContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");
        simpleCardsContainer.setPrefWrapLength(600); // Reasonable width

        Button btnAddCard = new Button("Añadir Bloque");
        FontIcon iconAdd = new FontIcon("mdi2p-plus-circle");
        iconAdd.setIconColor(javafx.scene.paint.Color.WHITE);
        btnAddCard.setGraphic(iconAdd);
        btnAddCard.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        btnAddCard.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20; -fx-font-size: 13px; -fx-background-radius: 5;");
        btnAddCard.setOnAction(e -> addSimpleCard(simpleCardsContainer));

        Button btnProcess = new Button("GENERAR LISTA");
        FontIcon iconCheck = new FontIcon("mdi2c-check-circle");
        iconCheck.setIconColor(javafx.scene.paint.Color.WHITE);
        btnProcess.setGraphic(iconCheck);
        btnProcess.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        btnProcess.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 5;");
        btnProcess.setOnAction(e -> processSimpleCards(simpleCardsContainer));

        header.getChildren().addAll(lblInfo, spacer, btnProcess, btnAddCard);

        // Add initial card
        addSimpleCard(simpleCardsContainer);

        // Scrollable Area for Cards
        ScrollPane scroll = new ScrollPane(simpleCardsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");

        // Remove Vgrow so it doesn't push footer to bottom when empty
        VBox.setVgrow(scroll, Priority.NEVER);

        // Footer Actions - REMOVED local button
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        // btnProcess REMOVED - uses Global Button

        root.getChildren().addAll(header, scroll, footer);

        return root;
    }

    private void addSimpleCard(FlowPane container) {
        String nextSize = "M";
        if (!container.getChildren().isEmpty()) {
            Node lastNode = container.getChildren().get(container.getChildren().size() - 1);
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
                        nextSize = current;
                    }
                }
            }
        }
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        // BIND WIDTH: 4 columns proportional to container
        // Margin: 20 (padding) + 30 (3 gaps) + 10 (buffer) = 60
        card.prefWidthProperty().bind(container.widthProperty().subtract(60).divide(4));
        card.setMinWidth(110);

        // Header: "Talla" + Close
        HBox cardHeader = new HBox(5);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblTalla = new Label("Talla");
        lblTalla.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close"));
        btnClose.getStyleClass().addAll("circular-icon-button", "btn-delete-card");
        btnClose.setTooltip(new Tooltip("Eliminar"));
        btnClose.setOnAction(e -> container.getChildren().remove(card));

        // CONFIG BUTTON (Gear/List) - DUPLICATED LOGIC adapted for Properties
        Button btnConfig = new Button();
        btnConfig.setGraphic(new FontIcon("mdi2c-cog"));
        btnConfig.getStyleClass().addAll("circular-icon-button", "btn-config-manga");
        btnConfig.setTooltip(new Tooltip("Configurar Manga/Short"));

        // Default Config
        card.getProperties().put("config", new String[] { "DEFAULT", "SAME" });

        btnConfig.setOnAction(e -> {
            String[] currentConfig = (String[]) card.getProperties().get("config");

            Dialog<String[]> dialog = new Dialog<>();
            dialog.setTitle("Configurar Bloque");
            dialog.setHeaderText("Manga y Short");

            ButtonType applyType = new ButtonType("Aplicar", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(applyType, ButtonType.CANCEL);

            // Stylize AFTER adding buttons
            UIFactory.estilizarDialogo(dialog);

            // Manual styling
            Node btnApply = dialog.getDialogPane().lookupButton(applyType);
            if (btnApply != null)
                btnApply.setStyle(
                        "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");

            Node btnCancel = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (btnCancel != null)
                btnCancel.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");

            GridPane grid = new GridPane();
            grid.setHgap(20);
            grid.setVgap(15);
            grid.setPadding(new Insets(30));

            ComboBox<String> cbManga = new ComboBox<>();

            // Logic for Redondo vs Cuadrado (Sleeves)
            boolean isRedondo = false;
            if (configSupplier != null && configSupplier.get() != null) {
                if (configSupplier.get().getCorte() == org.example.model.TipoCorte.REDONDO) {
                    isRedondo = true;
                }
            }

            cbManga.getItems().add("DEFAULT");
            cbManga.getItems().addAll("CORTA", "LARGA");
            if (!isRedondo) {
                cbManga.getItems().addAll("MANGA 0");
            }

            // Validate current value
            String curManga = currentConfig[0];
            if (curManga.equals("DEFAULT") || curManga.equals("USAR DEFECTO"))
                curManga = "DEFAULT";

            if (!cbManga.getItems().contains(curManga)) {
                curManga = "DEFAULT"; // Fallback to DEFAULT instead of CORTA
            }

            cbManga.setValue(curManga);
            cbManga.setPrefWidth(200);

            // Custom Cell Factory for USAR DEFECTO
            javafx.util.Callback<ListView<String>, ListCell<String>> cellFactory = lv -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else if (item.equals("DEFAULT")) {
                        setText("USAR DEFECTO");
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
            cbShort.setValue(currentConfig[1].equals("SAME") ? "IGUAL A CAMISETA" : currentConfig[1]);
            cbShort.setPrefWidth(200);
            UIFactory.fixComboBoxReadability(cbShort);

            // Conditional Logic: Disable/Hide Short selector if Config says NO Short
            if (configSupplier != null && configSupplier.get() != null) {
                boolean llevaShort = configSupplier.get().llevaShort();
                if (configSupplier.get().getTipoPrenda() == org.example.model.TipoPrenda.SHORT) {
                    llevaShort = true;
                }
                if (!llevaShort) {
                    cbShort.setDisable(true);
                    cbShort.setValue("NO APLICA");
                }
            }

            Label l1 = new Label("Tipo Manga:");
            l1.setGraphic(new FontIcon("mdi2t-tshirt-crew"));
            l1.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
            l1.setGraphicTextGap(10);

            Label l2 = new Label("Talla Short:");
            l2.setGraphic(new FontIcon("mdi2r-ruler"));
            l2.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
            l2.setGraphicTextGap(10);

            grid.add(l1, 0, 0);
            grid.add(cbManga, 1, 0);
            grid.add(l2, 0, 1);
            grid.add(cbShort, 1, 1);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == applyType) {
                    String sVal = cbShort.getValue().equals("IGUAL A CAMISETA") ? "SAME" : cbShort.getValue();
                    if (cbShort.isDisabled() || cbShort.getValue().equals("NO APLICA"))
                        sVal = "SAME";
                    return new String[] { cbManga.getValue(), sVal };
                }
                return null;
            });

            Optional<String[]> result = dialog.showAndWait();
            result.ifPresent(newConfig -> {
                card.getProperties().put("config", newConfig);
                // Highlight button if config is not default
                if (!newConfig[0].equals("CORTA")
                        || (!newConfig[1].equals("SAME") && !newConfig[1].equals("NO APLICA"))) {
                    btnConfig.setStyle("-fx-border-color: #3498db;");
                    ((FontIcon) btnConfig.getGraphic()).setIconColor(javafx.scene.paint.Color.web("#3498db"));
                } else {
                    btnConfig.setStyle("");
                    ((FontIcon) btnConfig.getGraphic()).setIconColor(javafx.scene.paint.Color.web("#7f8c8d"));
                }
            });
        });

        cardHeader.getChildren().addAll(lblTalla, sp, btnConfig, btnClose);

        // Content
        ComboBox<String> comboSize = new ComboBox<>();
        comboSize.getItems().addAll(org.example.model.TipoTalla.getLabels());
        comboSize.setValue(nextSize);
        comboSize.setMaxWidth(Double.MAX_VALUE);
        comboSize.setStyle("");

        ComboBox<String> comboGen = new ComboBox<>();
        comboGen.getItems().addAll("HOMBRE", "MUJER");
        comboGen.setValue("HOMBRE");
        comboGen.setMaxWidth(Double.MAX_VALUE);
        comboGen.setStyle("-fx-font-size: 10px;");

        TextField txtQty = new TextField("1");
        txtQty.setPromptText("Cant.");
        txtQty.setAlignment(Pos.CENTER);
        // Numeric filter
        txtQty.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtQty.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        Separator sep = new Separator();
        sep.setOpacity(0.3); // Make it subtle

        card.getChildren().addAll(cardHeader, comboSize, comboGen, sep, new Label("Cantidad:"), txtQty);

        // Ensure inputs span full width
        comboSize.setMaxWidth(Double.MAX_VALUE);
        comboGen.setMaxWidth(Double.MAX_VALUE);
        txtQty.setMaxWidth(Double.MAX_VALUE);

        // Align card itself to left
        card.setAlignment(Pos.TOP_LEFT);

        // Store references in Map for robust retrieval
        java.util.Map<String, Object> refs = new java.util.HashMap<>();
        refs.put("cbSize", comboSize);
        refs.put("txtQty", txtQty);
        refs.put("cbGen", comboGen);
        card.setUserData(refs);

        container.getChildren().add(card);
    }

    private void processSimpleCards(FlowPane container) {
        System.out.println("DEBUG: processSimpleCards() called");
        int totalAdded = 0;
        int cardsFound = 0;

        for (Node node : container.getChildren()) {
            if (node instanceof VBox) {
                cardsFound++;
                Object userData = node.getUserData();
                System.out.println("DEBUG: Card " + cardsFound + " - UserData type: "
                        + (userData != null ? userData.getClass().getName() : "null"));

                if (userData instanceof java.util.Map) {
                    java.util.Map<String, Object> refs = (java.util.Map<String, Object>) userData;
                    ComboBox<String> combo = (ComboBox<String>) refs.get("cbSize");
                    TextField txt = (TextField) refs.get("txtQty");
                    ComboBox<String> comboGen = (ComboBox<String>) refs.get("cbGen");

                    String size = (combo != null) ? combo.getValue() : null;
                    String qtyStr = (txt != null) ? txt.getText() : "";
                    String gender = (comboGen != null) ? comboGen.getValue() : "HOMBRE";

                    System.out.println(
                            "DEBUG: Card " + cardsFound + " - Size=" + size + ", Qty=" + qtyStr + ", Gender=" + gender);

                    // READ CONFIG PROPERTY
                    String[] config = (String[]) node.getProperties().get("config");
                    String manga = (config != null) ? config[0] : "CORTA";
                    String tShort = (config != null) ? config[1] : "SAME";
                    String tipoBottom = (config != null && config.length > 2) ? config[2] : "DEFAULT";

                    if (size != null && !qtyStr.isEmpty()) {
                        try {
                            int qty = Integer.parseInt(qtyStr);
                            if (qty > 0) {
                                for (int i = 0; i < qty; i++) {
                                    DetallePedido nuevo = crearItem("", "", size, gender);

                                    // Apply Config
                                    if (config != null) {
                                        // Resolve DEFAULT Manga
                                        String resolvedManga = manga;
                                        if (manga.equals("DEFAULT") || manga.equals("USAR DEFECTO")) {
                                            if (configSupplier != null && configSupplier.get() != null
                                                    && configSupplier.get().getLargo() != null) {
                                                switch (configSupplier.get().getLargo()) {
                                                    case MANGA_LARGA:
                                                        resolvedManga = "LARGA";
                                                        break;
                                                    case MANGA_CORTA:
                                                        resolvedManga = "CORTA";
                                                        break;
                                                    case MANGA_CERO:
                                                        resolvedManga = "MANGA 0";
                                                        break;
                                                    case MANGA_3_4:
                                                        resolvedManga = "MANGA 3/4";
                                                        break;
                                                    default:
                                                        resolvedManga = "CORTA";
                                                        break;
                                                }
                                            } else {
                                                resolvedManga = "CORTA"; // Fallback
                                            }
                                        }

                                        if (!resolvedManga.equals("CORTA"))
                                            nuevo.setTipoManga(resolvedManga);

                                        if (!tShort.equals("SAME"))
                                            nuevo.setTallaShort(tShort);
                                        else
                                            nuevo.setTallaShort(size);

                                        // Override Tipo Bottom
                                        if (!tipoBottom.equals("DEFAULT") && !tipoBottom.equals("NO APLICA")) {
                                            nuevo.setTipoBottom(tipoBottom);
                                        }
                                    }

                                    listaJugadores.add(nuevo);
                                }
                                totalAdded += qty;
                                System.out.println("DEBUG: Added " + qty + " items from card " + cardsFound);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("DEBUG: Invalid quantity format in card " + cardsFound + ": " + qtyStr);
                        }
                    } else {
                        System.out.println("DEBUG: Card " + cardsFound + " skipped - missing size or quantity");
                    }
                } else {
                    System.out.println("DEBUG: Card " + cardsFound + " has invalid UserData format");
                }
            }
        }

        System.out.println("DEBUG: Total cards found=" + cardsFound + ", Total items added=" + totalAdded);

        if (totalAdded > 0) {
            UIFactory.mostrarAlerta(Alert.AlertType.INFORMATION, "Proceso Completado",
                    "Se han añadido " + totalAdded + " prendas a la lista.");
            actualizarContador();
        } else {
            UIFactory.mostrarAlerta(Alert.AlertType.WARNING, "Sin datos",
                    "No se encontraron bloques válidos para agregar.\n\n" +
                            "Asegúrate de:\n" +
                            "1. Tener al menos una tarjeta agregada\n" +
                            "2. Seleccionar una talla en cada tarjeta\n" +
                            "3. Ingresar una cantidad válida (mayor a 0)");
        }
    }

    private StackPane createStyledContainer(Node content) {
        // 1. Outer Container for Drop Shadow
        StackPane shadowContainer = new StackPane();
        shadowContainer.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        shadowContainer.setPadding(new Insets(8));
        // shadowContainer.setMinSize(0, 0); // REMOVED to allow content to dictate size

        // 2. Border/Background Container
        StackPane borderContainer = new StackPane();
        borderContainer.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-color: #e0e0e0; " + // Lighter border
                        "-fx-border-width: 1;");
        // borderContainer.setMinSize(0, 0); // REMOVED

        // 3. Inner Container to Clip
        StackPane contentClip = new StackPane();
        contentClip.setStyle("-fx-background-color: white; -fx-background-radius: 9;");
        contentClip.setPadding(new Insets(0)); // No inner padding, let content decide
        // contentClip.setMinSize(0, 0); // REMOVED

        // Clip shape
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        clip.widthProperty().bind(contentClip.widthProperty());
        clip.heightProperty().bind(contentClip.heightProperty());
        contentClip.setClip(clip);

        contentClip.getChildren().add(content);
        borderContainer.getChildren().add(contentClip);
        shadowContainer.getChildren().add(borderContainer);

        return shadowContainer;
    }
}

