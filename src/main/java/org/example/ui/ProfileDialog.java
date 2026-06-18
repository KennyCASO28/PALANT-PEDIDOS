package org.example.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.controller.ShellController;
import org.example.dao.UsuarioDAO;
import org.example.logic.SessionManager;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ProfileDialog {

    private static final Logger logger = LoggerFactory.getLogger(ProfileDialog.class);
    private final ShellController shell;
    private Stage dialogStage;
    private double xOffset = 0;
    private double yOffset = 0;

    private Stage ownerStage;

    public ProfileDialog(ShellController shell) {
        this.shell = shell;
    }

    public void show(Stage ownerStage) {
        this.ownerStage = ownerStage;
        dialogStage = new Stage();
        if (ownerStage != null) {
            dialogStage.initOwner(ownerStage);
        }
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Perfil de Usuario");

        VBox mainContainer = new VBox();
        mainContainer.setStyle(
            "-fx-background-color: #0B213E; " +
            "-fx-border-color: #2ecc71; " +
            "-fx-border-width: 1.5; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 20;"
        );
        mainContainer.setSpacing(20);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPrefWidth(380);

        // Make draggable
        mainContainer.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        mainContainer.setOnMouseDragged(event -> {
            dialogStage.setX(event.getScreenX() - xOffset);
            dialogStage.setY(event.getScreenY() - yOffset);
        });

        // Header Title
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("PERFIL DE USUARIO");
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

        if (!SessionManager.isLoggedIn()) {
            buildGuestView(mainContainer);
        } else {
            buildProfileView(mainContainer, SessionManager.getCurrentUser());
        }

        Scene scene = new Scene(mainContainer);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void buildGuestView(VBox container) {
        ImageView guestIcon = new ImageView(new Image(getClass().getResourceAsStream("/vectors/logo_small.png")));
        guestIcon.setFitHeight(70);
        guestIcon.setFitWidth(70);

        Label subtitle = new Label("Modo Invitado");
        subtitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label desc = new Label("Inicia sesión para registrar pedidos con tu firma digital, tener un historial de producción y acceder a herramientas avanzadas.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 12px; -fx-text-alignment: center; -fx-padding: 0 10;");

        Button loginBtn = new Button("Iniciar Sesión");
        loginBtn.setGraphic(new FontIcon("mdi2l-login"));
        loginBtn.setStyle(
            "-fx-background-color: #2ecc71; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 20; " +
            "-fx-cursor: hand; " +
            "-fx-font-size: 13px;"
        );
        ((FontIcon) loginBtn.getGraphic()).setIconColor(Color.WHITE);
        loginBtn.setOnAction(e -> {
            dialogStage.close();
            LoginDialog login = new LoginDialog();
            login.showLogin(ownerStage, () -> {
                shell.refreshProfileUI();
                // Show profile again after login safely
                javafx.application.Platform.runLater(() -> new ProfileDialog(shell).show(ownerStage));
            });
        });

        container.getChildren().addAll(guestIcon, subtitle, desc, loginBtn);
    }

    private void buildProfileView(VBox container, UsuarioDAO.Usuario user) {
        // Avatar circular
        ImageView avatar = new ImageView();
        avatar.setFitHeight(80);
        avatar.setFitWidth(80);
        
        Circle clip = new Circle(40, 40, 40);
        avatar.setClip(clip);

        String customPath = SessionManager.getCustomAvatarPath(user.getNombreUsuario());
        if (!customPath.isEmpty() && new File(customPath).exists()) {
            avatar.setImage(new Image(new File(customPath).toURI().toString()));
        } else {
            avatar.setImage(new Image(getClass().getResourceAsStream("/vectors/logo_small.png")));
        }

        Button changePicBtn = new Button("Cambiar Foto");
        changePicBtn.setGraphic(new FontIcon("mdi2c-camera"));
        changePicBtn.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.1); " +
            "-fx-text-fill: #E2E8F0; " +
            "-fx-font-size: 11px; " +
            "-fx-background-radius: 15; " +
            "-fx-padding: 4 12; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) changePicBtn.getGraphic()).setIconColor(Color.WHITE);
        changePicBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar Foto de Perfil");
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
            );
            File file = chooser.showOpenDialog(dialogStage);
            if (file != null) {
                String saved = SessionManager.saveAvatarLocally(user.getNombreUsuario(), file);
                if (saved != null) {
                    avatar.setImage(new Image(new File(saved).toURI().toString()));
                    shell.refreshProfileUI();
                }
            }
        });

        // User info details
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER);
        
        Label lblUsername = new Label("@" + user.getNombreUsuario());
        lblUsername.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-font-style: italic;");
        
        Label lblRolBadge = new Label(user.getRol().toUpperCase());
        lblRolBadge.setStyle(
            "-fx-background-color: #1E293B; " +
            "-fx-text-fill: #2ecc71; " +
            "-fx-font-size: 10px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 3 8; " +
            "-fx-background-radius: 4;"
        );
        
        infoBox.getChildren().addAll(lblUsername, lblRolBadge);

        // Edit form
        VBox form = new VBox(6);
        form.setAlignment(Pos.CENTER_LEFT);
        
        Label lblName = new Label("Nombre Completo");
        lblName.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        TextField txtName = new TextField(user.getNombreCompleto());
        txtName.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06); " +
            "-fx-text-fill: white; " +
            "-fx-border-color: rgba(255,255,255,0.15); " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8;"
        );
        
        form.getChildren().addAll(lblName, txtName);

        // Save & Logout Actions
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        
        Button saveBtn = new Button("Guardar");
        saveBtn.setGraphic(new FontIcon("mdi2c-content-save"));
        saveBtn.setStyle(
            "-fx-background-color: #27ae60; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8 16; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) saveBtn.getGraphic()).setIconColor(Color.WHITE);
        saveBtn.setOnAction(e -> {
            String newName = txtName.getText().trim();
            if (!newName.isEmpty()) {
                UsuarioDAO dao = new UsuarioDAO();
                if (dao.actualizarDatosBasicos(user.getId(), newName)) {
                    // Refresh session user memory
                    SessionManager.setCurrentUser(new UsuarioDAO.Usuario(
                        user.getId(),
                        user.getNombreUsuario(),
                        user.getRol(),
                        newName,
                        user.isActivo()
                    ));
                    shell.refreshProfileUI();
                    dialogStage.close();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudieron actualizar los datos en la base de datos.");
                    alert.showAndWait();
                }
            }
        });

        Button logoutBtn = new Button("Cerrar Sesión");
        logoutBtn.setGraphic(new FontIcon("mdi2l-logout"));
        logoutBtn.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8 16; " +
            "-fx-cursor: hand;"
        );
        ((FontIcon) logoutBtn.getGraphic()).setIconColor(Color.WHITE);
        logoutBtn.setOnAction(e -> {
            SessionManager.logout();
            shell.refreshProfileUI();
            dialogStage.close();
        });
 
        actions.getChildren().addAll(saveBtn, logoutBtn);

        // Admin/History Navigation Actions
        VBox adminActions = new VBox(10);
        adminActions.setAlignment(Pos.CENTER);
        adminActions.setPadding(new Insets(10, 0, 10, 0));

        Button btnHistory = new Button("Historial de Pedidos");
        btnHistory.setGraphic(new FontIcon("mdi2h-history"));
        btnHistory.setStyle(
            "-fx-background-color: #2980b9; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8; " +
            "-fx-cursor: hand;" +
            "-fx-pref-width: 230;"
        );
        ((FontIcon) btnHistory.getGraphic()).setIconColor(Color.WHITE);
        btnHistory.setOnAction(e -> {
            dialogStage.close();
            javafx.application.Platform.runLater(() -> new OrderHistoryDialog(shell).show(ownerStage));
        });
        adminActions.getChildren().add(btnHistory);
 
        if ("Jefe".equalsIgnoreCase(user.getRol())) {
            Button btnManageUsers = new Button("Gestionar Vendedores");
            btnManageUsers.setGraphic(new FontIcon("mdi2a-account-cog"));
            btnManageUsers.setStyle(
                "-fx-background-color: #f39c12; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 6; " +
                "-fx-padding: 8; " +
                "-fx-cursor: hand;" +
                "-fx-pref-width: 230;"
            );
            ((FontIcon) btnManageUsers.getGraphic()).setIconColor(Color.WHITE);
            btnManageUsers.setOnAction(e -> {
                dialogStage.close();
                javafx.application.Platform.runLater(() -> new UserManagementDialog().show(ownerStage));
            });
            adminActions.getChildren().add(btnManageUsers);
        }
 
        container.getChildren().addAll(avatar, changePicBtn, infoBox, form, adminActions, actions);
    }
}
