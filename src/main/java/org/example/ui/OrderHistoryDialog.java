package org.example.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.controller.ShellController;
import org.example.dao.PedidoDAO;
import org.example.dao.PedidoDAO.PedidoRecord;
import org.example.model.DetallePedido;
import org.example.logic.SessionManager;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class OrderHistoryDialog {

    private static final Logger logger = LoggerFactory.getLogger(OrderHistoryDialog.class);
    private final ShellController shell;
    private Stage dialogStage;
    private TableView<PedidoRecord> table;
    private ObservableList<PedidoRecord> masterData = FXCollections.observableArrayList();
    private FilteredList<PedidoRecord> filteredData;
    private double xOffset = 0;
    private double yOffset = 0;

    public OrderHistoryDialog(ShellController shell) {
        this.shell = shell;
    }

    public void show(Stage ownerStage) {
        dialogStage = new Stage();
        if (ownerStage != null) {
            dialogStage.initOwner(ownerStage);
        }
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Historial de Pedidos");

        VBox mainContainer = new VBox();
        mainContainer.setStyle(
            "-fx-background-color: #0B213E; " +
            "-fx-border-color: #2ecc71; " +
            "-fx-border-width: 1.5; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 20;"
        );
        mainContainer.setSpacing(15);
        mainContainer.setPrefSize(780, 520);

        // Draggable
        mainContainer.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        mainContainer.setOnMouseDragged(event -> {
            dialogStage.setX(event.getScreenX() - xOffset);
            dialogStage.setY(event.getScreenY() - yOffset);
        });

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("HISTORIAL DE PEDIDOS");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(new FontIcon("mdi2c-close"));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        ((FontIcon) closeBtn.getGraphic()).setIconColor(Color.WHITE);
        ((FontIcon) closeBtn.getGraphic()).setIconSize(18);
        closeBtn.setOnAction(e -> dialogStage.close());

        header.getChildren().addAll(title, spacer, closeBtn);
        mainContainer.getChildren().add(header);

        // Search panel
        HBox searchBar = new HBox(8);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Buscar por cliente, prenda o código...");
        txtSearch.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06); " +
            "-fx-text-fill: white; " +
            "-fx-border-color: rgba(255,255,255,0.15); " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8; " +
            "-fx-pref-width: 320;"
        );
        
        Label lblSearchIcon = new Label();
        lblSearchIcon.setGraphic(new FontIcon("mdi2m-magnify"));
        ((FontIcon) lblSearchIcon.getGraphic()).setIconColor(Color.web("#94A3B8"));
        ((FontIcon) lblSearchIcon.getGraphic()).setIconSize(18);
        
        searchBar.getChildren().addAll(lblSearchIcon, txtSearch);
        mainContainer.getChildren().add(searchBar);

        // Table initialization
        table = new TableView<>();
        table.getStyleClass().add("dark-table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PedidoRecord, String> colCode = new TableColumn<>("Código");
        colCode.setCellValueFactory(new PropertyValueFactory<>("codigoPedido"));
        colCode.setPrefWidth(90);

        TableColumn<PedidoRecord, String> colClient = new TableColumn<>("Cliente");
        colClient.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));
        colClient.setPrefWidth(180);

        TableColumn<PedidoRecord, String> colGarment = new TableColumn<>("Prenda");
        colGarment.setCellValueFactory(new PropertyValueFactory<>("tipoPrenda"));
        colGarment.setPrefWidth(100);

        TableColumn<PedidoRecord, String> colPriority = new TableColumn<>("Prioridad");
        colPriority.setCellValueFactory(new PropertyValueFactory<>("prioridad"));
        colPriority.setPrefWidth(90);

        TableColumn<PedidoRecord, LocalDate> colDelivery = new TableColumn<>("Entrega");
        colDelivery.setCellValueFactory(new PropertyValueFactory<>("fechaEntrega"));
        colDelivery.setPrefWidth(100);

        TableColumn<PedidoRecord, String> colState = new TableColumn<>("Estado");
        colState.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colState.setPrefWidth(110);

        TableColumn<PedidoRecord, String> colSeller = new TableColumn<>("Vendedor");
        colSeller.setCellValueFactory(new PropertyValueFactory<>("vendedor"));
        colSeller.setPrefWidth(120);

        table.getColumns().addAll(colCode, colClient, colGarment, colPriority, colDelivery, colState, colSeller);
        mainContainer.getChildren().add(table);

        // Load History Data from Database
        loadData();

        // Bind filter to search box
        filteredData = new FilteredList<>(masterData, p -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(record -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lower = newValue.toLowerCase().trim();
                if (record.getCodigoPedido() != null && record.getCodigoPedido().toLowerCase().contains(lower)) return true;
                if (record.getClienteNombre() != null && record.getClienteNombre().toLowerCase().contains(lower)) return true;
                if (record.getTipoPrenda() != null && record.getTipoPrenda().toLowerCase().contains(lower)) return true;
                if (record.getVendedor() != null && record.getVendedor().toLowerCase().contains(lower)) return true;
                if (record.getEstado() != null && record.getEstado().toLowerCase().contains(lower)) return true;
                return false;
            });
        });
        table.setItems(filteredData);

        // Bottom panel for state changes and workspace loads
        HBox bottomPanel = new HBox(15);
        bottomPanel.setAlignment(Pos.CENTER_LEFT);

        Label lblState = new Label("Actualizar Estado:");
        lblState.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");

        ComboBox<String> comboEstado = new ComboBox<>();
        comboEstado.getItems().addAll("Pendiente", "Diseño", "Sublimación", "Confección", "Terminado");
        comboEstado.getStyleClass().add("dark-combo-box");
        comboEstado.setPrefWidth(140);

        Button btnChangeState = new Button("Actualizar");
        btnChangeState.setGraphic(new FontIcon("mdi2p-pencil"));
        btnChangeState.setStyle(
            "-fx-background-color: #2ecc71; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 7 14; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) btnChangeState.getGraphic()).setIconColor(Color.WHITE);
        btnChangeState.setOnAction(e -> {
            PedidoRecord selected = table.getSelectionModel().getSelectedItem();
            String newState = comboEstado.getValue();
            if (selected != null && newState != null) {
                PedidoDAO dao = new PedidoDAO();
                if (dao.actualizarEstadoPedido(selected.getId(), newState)) {
                    loadData(); // Reload table
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo actualizar el estado en base de datos.");
                    alert.showAndWait();
                }
            }
        });

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, javafx.scene.layout.Priority.ALWAYS);

        Button btnLoad = new Button("Cargar en Editor");
        btnLoad.setGraphic(new FontIcon("mdi2u-upload"));
        btnLoad.setStyle(
            "-fx-background-color: #2980b9; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 7 14; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) btnLoad.getGraphic()).setIconColor(Color.WHITE);
        btnLoad.setOnAction(e -> {
            PedidoRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                PedidoDAO dao = new PedidoDAO();
                List<DetallePedido> players = dao.obtenerDetalleJugadores(selected.getId());
                if (shell.getActiveTabController() != null) {
                    shell.getActiveTabController().importarDatosDePedido(selected, players);
                    dialogStage.close();
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "No hay ningún editor activo donde cargar el pedido.");
                    alert.showAndWait();
                }
            }
        });

        bottomPanel.getChildren().addAll(lblState, comboEstado, btnChangeState, spacerBottom, btnLoad);
        mainContainer.getChildren().add(bottomPanel);

        Scene scene = new Scene(mainContainer);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void loadData() {
        PedidoDAO dao = new PedidoDAO();
        List<PedidoRecord> list;
        if (SessionManager.isLoggedIn()) {
            // All logged-in users see the full order history.
            // Access control is handled at the action level (e.g., only Jefe can manage users).
            list = dao.obtenerHistorialCompleto();
        } else {
            // Guest users see no orders
            list = new java.util.ArrayList<>();
        }
        masterData.setAll(list);
    }
}
