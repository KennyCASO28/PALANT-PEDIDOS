package org.example.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.dao.UsuarioDAO;
import org.example.dao.UsuarioDAO.Usuario;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserManagementDialog {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementDialog.class);
    private Stage dialogStage;
    private TableView<Usuario> table;
    private ObservableList<Usuario> dataList = FXCollections.observableArrayList();
    private double xOffset = 0;
    private double yOffset = 0;

    public void show(Stage ownerStage) {
        dialogStage = new Stage();
        if (ownerStage != null) {
            dialogStage.initOwner(ownerStage);
        }
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Gestión de Usuarios");

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
        mainContainer.setPrefSize(720, 520);

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
        Label title = new Label("GESTIÓN DE VENDEDORES / USUARIOS");
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

        // Split Layout: Left is Form to Create, Right is Table of Existing
        HBox splitBox = new HBox(20);
        splitBox.setAlignment(Pos.TOP_LEFT);

        // Left Form (Create/Edit)
        VBox form = new VBox(10);
        form.setPrefWidth(240);
        form.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 8;");
        
        Label lblFormTitle = new Label("Nuevo Vendedor");
        lblFormTitle.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 13px;");

        TextField txtUser = new TextField();
        txtUser.setPromptText("Usuario");
        txtUser.setStyle(styleInput());

        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Nombre Completo");
        txtFullName.setStyle(styleInput());

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Contraseña");
        txtPass.setStyle(styleInput());

        ComboBox<String> comboRole = new ComboBox<>();
        comboRole.getItems().addAll("Vendedor", "Jefe");
        comboRole.setValue("Vendedor");
        comboRole.getStyleClass().add("dark-combo-box");
        comboRole.setMaxWidth(Double.MAX_VALUE);

        Button btnCreate = new Button("Crear Cuenta");
        btnCreate.setGraphic(new FontIcon("mdi2a-account-plus"));
        btnCreate.setStyle(
            "-fx-background-color: #2ecc71; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) btnCreate.getGraphic()).setIconColor(Color.WHITE);
        btnCreate.setMaxWidth(Double.MAX_VALUE);

        Button btnCancelEdit = new Button("Limpiar Selección");
        btnCancelEdit.setGraphic(new FontIcon("mdi2c-close-circle-outline"));
        btnCancelEdit.setStyle(
            "-fx-background-color: rgba(255,255,255,0.1); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) btnCancelEdit.getGraphic()).setIconColor(Color.WHITE);
        btnCancelEdit.setMaxWidth(Double.MAX_VALUE);
        btnCancelEdit.setVisible(false);
        btnCancelEdit.setManaged(false);

        btnCreate.setOnAction(e -> {
            String u = txtUser.getText().trim();
            String n = txtFullName.getText().trim();
            String p = txtPass.getText();
            String r = comboRole.getValue(); // Capitalized first letter to match DB check constraint!

            if (n.isEmpty() || (table.getSelectionModel().getSelectedItem() == null && (u.isEmpty() || p.isEmpty()))) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Por favor complete todos los campos requeridos.");
                alert.showAndWait();
                return;
            }

            UsuarioDAO dao = new UsuarioDAO();
            Usuario selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Edit Mode
                if (dao.actualizarUsuarioCompleto(selected.getId(), n, r, p)) {
                    table.getSelectionModel().clearSelection();
                    txtUser.setDisable(false);
                    txtUser.clear();
                    txtFullName.clear();
                    txtPass.clear();
                    txtPass.setPromptText("Contraseña");
                    lblFormTitle.setText("Nuevo Vendedor");
                    btnCreate.setText("Crear Cuenta");
                    btnCreate.setGraphic(new FontIcon("mdi2a-account-plus"));
                    ((FontIcon) btnCreate.getGraphic()).setIconColor(Color.WHITE);
                    btnCancelEdit.setVisible(false);
                    btnCancelEdit.setManaged(false);
                    loadUsers();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo actualizar el usuario.");
                    alert.showAndWait();
                }
            } else {
                // Create Mode
                if (dao.registrar(u, p, r, n)) {
                    txtUser.clear();
                    txtFullName.clear();
                    txtPass.clear();
                    loadUsers();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo registrar el usuario. El nombre de usuario podría estar en uso.");
                    alert.showAndWait();
                }
            }
        });

        btnCancelEdit.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            txtUser.setDisable(false);
            txtUser.clear();
            txtFullName.clear();
            txtPass.clear();
            txtPass.setPromptText("Contraseña");
            lblFormTitle.setText("Nuevo Vendedor");
            btnCreate.setText("Crear Cuenta");
            btnCreate.setGraphic(new FontIcon("mdi2a-account-plus"));
            ((FontIcon) btnCreate.getGraphic()).setIconColor(Color.WHITE);
            btnCancelEdit.setVisible(false);
            btnCancelEdit.setManaged(false);
        });

        form.getChildren().addAll(lblFormTitle, txtUser, txtFullName, txtPass, comboRole, btnCreate, btnCancelEdit);

        // Right Table
        VBox tableBox = new VBox(10);
        tableBox.setPrefWidth(420);
        HBox.setHgrow(tableBox, javafx.scene.layout.Priority.ALWAYS);

        table = new TableView<>();
        table.getStyleClass().add("dark-table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Usuario, String> colUser = new TableColumn<>("Usuario");
        colUser.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));
        colUser.setPrefWidth(100);

        TableColumn<Usuario, String> colName = new TableColumn<>("Nombre Completo");
        colName.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colName.setPrefWidth(160);

        TableColumn<Usuario, String> colRole = new TableColumn<>("Rol");
        colRole.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colRole.setPrefWidth(80);

        TableColumn<Usuario, Boolean> colActive = new TableColumn<>("Activo");
        colActive.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colActive.setPrefWidth(70);

        table.getColumns().addAll(colUser, colName, colRole, colActive);
        tableBox.getChildren().add(table);

        // Selection listener to load fields for editing
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                lblFormTitle.setText("Editar Usuario");
                txtUser.setText(newSelection.getNombreUsuario());
                txtUser.setDisable(true);
                txtFullName.setText(newSelection.getNombreCompleto());
                txtPass.clear();
                txtPass.setPromptText("Nueva Contraseña (opcional)");
                
                String roleVal = "Vendedor";
                if ("Jefe".equalsIgnoreCase(newSelection.getRol())) {
                    roleVal = "Jefe";
                }
                comboRole.setValue(roleVal);
                
                btnCreate.setText("Actualizar Cuenta");
                btnCreate.setGraphic(new FontIcon("mdi2a-account-edit"));
                ((FontIcon) btnCreate.getGraphic()).setIconColor(Color.WHITE);
                btnCancelEdit.setVisible(true);
                btnCancelEdit.setManaged(true);
            }
        });

        splitBox.getChildren().addAll(form, tableBox);
        mainContainer.getChildren().add(splitBox);

        // Load users from DB
        loadUsers();
        table.setItems(dataList);

        // Bottom control row for activation / deletions
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_RIGHT);

        Button btnToggleActive = new Button("Activar/Desactivar");
        btnToggleActive.setGraphic(new FontIcon("mdi2a-account-switch"));
        btnToggleActive.setStyle(
            "-fx-background-color: #f39c12; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8 16; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) btnToggleActive.getGraphic()).setIconColor(Color.WHITE);
        btnToggleActive.setOnAction(e -> {
            Usuario selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                UsuarioDAO dao = new UsuarioDAO();
                if (dao.cambiarEstadoActivo(selected.getId(), !selected.isActivo())) {
                    loadUsers();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo cambiar el estado del usuario.");
                    alert.showAndWait();
                }
            }
        });

        Button btnDelete = new Button("Eliminar");
        btnDelete.setGraphic(new FontIcon("mdi2d-delete"));
        btnDelete.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8 16; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) btnDelete.getGraphic()).setIconColor(Color.WHITE);
        btnDelete.setOnAction(e -> {
            Usuario selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Está seguro que desea eliminar a @" + selected.getNombreUsuario() + "? Esta acción no se puede deshacer.", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        UsuarioDAO dao = new UsuarioDAO();
                        if (dao.eliminar(selected.getId())) {
                            loadUsers();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el usuario.");
                            alert.showAndWait();
                        }
                    }
                });
            }
        });

        controls.getChildren().addAll(btnToggleActive, btnDelete);
        mainContainer.getChildren().add(controls);

        Scene scene = new Scene(mainContainer);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void loadUsers() {
        UsuarioDAO dao = new UsuarioDAO();
        List<Usuario> users = dao.listarTodos();
        dataList.setAll(users);
    }

    private String styleInput() {
        return "-fx-background-color: rgba(255,255,255,0.06); " +
               "-fx-text-fill: white; " +
               "-fx-border-color: rgba(255,255,255,0.15); " +
               "-fx-border-radius: 6; " +
               "-fx-background-radius: 6; " +
               "-fx-padding: 8;";
    }
}
