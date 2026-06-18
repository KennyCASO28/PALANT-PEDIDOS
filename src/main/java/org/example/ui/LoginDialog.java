package org.example.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.dao.UsuarioDAO;

public class LoginDialog {

    private boolean authenticated = false;
    private UsuarioDAO.Usuario activeUser = null;

    public void showLogin(Stage ownerStage, Runnable onSuccess) {
        Stage dialog = new Stage();
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT); // Modern transparent window with no square borders
        dialog.setTitle("PALANT - Inicio de Sesión");

        // Main Container with premium dark gradient background
        VBox container = new VBox(20);
        container.setPadding(new Insets(30, 40, 30, 40));
        container.setAlignment(Pos.CENTER);
        container.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #1a2230, #0c1017);" +
            "-fx-border-color: #3b82f6;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;"
        );

        // Add Drop Shadow effect for premium float look
        DropShadow ds = new DropShadow();
        ds.setOffsetY(5.0);
        ds.setOffsetX(5.0);
        ds.setColor(Color.color(0, 0, 0, 0.5));
        container.setEffect(ds);

        // Header Title Bar with Close Button
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        Button btnClose = new Button("✕");
        btnClose.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #9ca3af;" +
            "-fx-font-size: 16;" +
            "-fx-cursor: hand;"
        );
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 16; -fx-cursor: hand;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 16; -fx-cursor: hand;"));
        btnClose.setOnAction(e -> {
            dialog.close();
        });
        header.getChildren().add(btnClose);

        // Logo
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(getClass().getResourceAsStream("/vectors/LOGO PALANT-barra.png"));
            logoView.setImage(logo);
            logoView.setFitHeight(50);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
            // Fallback text if logo load fails
        }

        // Title
        Label lblTitle = new Label("PALANT PEDIDOS");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblTitle.setStyle("-fx-text-fill: #f3f4f6;");

        Label lblSubtitle = new Label("Ingresa tus credenciales para continuar");
        lblSubtitle.setFont(Font.font("System", 12));
        lblSubtitle.setStyle("-fx-text-fill: #9ca3af;");

        // Form fields
        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(10);
        grid.setAlignment(Pos.CENTER);

        Label lblUser = new Label("Usuario:");
        lblUser.setFont(Font.font("System", FontWeight.MEDIUM, 14));
        lblUser.setStyle("-fx-text-fill: #d1d5db;");

        TextField txtUser = new TextField();
        txtUser.setPromptText("Ej. juan.perez");
        txtUser.setPrefWidth(240);
        txtUser.setStyle(
            "-fx-background-color: #1f2937;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #6b7280;" +
            "-fx-border-color: #4b5563;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8;"
        );

        Label lblPass = new Label("Contraseña:");
        lblPass.setFont(Font.font("System", FontWeight.MEDIUM, 14));
        lblPass.setStyle("-fx-text-fill: #d1d5db;");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("••••••••");
        txtPass.setPrefWidth(240);
        txtPass.setStyle(
            "-fx-background-color: #1f2937;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #6b7280;" +
            "-fx-border-color: #4b5563;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8;"
        );

        grid.add(lblUser, 0, 0);
        grid.add(txtUser, 1, 0);
        grid.add(lblPass, 0, 1);
        grid.add(txtPass, 1, 1);

        // Error Feedback Label
        Label lblError = new Label();
        lblError.setTextFill(Color.web("#ef4444"));
        lblError.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblError.setVisible(false);

        // Login Button
        Button btnLogin = new Button("Ingresar");
        btnLogin.setPrefWidth(260);
        btnLogin.setFont(Font.font("System", FontWeight.BOLD, 15));
        btnLogin.setStyle(
            "-fx-background-color: linear-gradient(to right, #2563eb, #1d4ed8);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10;" +
            "-fx-cursor: hand;"
        );
        
        // Hover effects
        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle(
            "-fx-background-color: linear-gradient(to right, #3b82f6, #2563eb);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10;" +
            "-fx-cursor: hand;"
        ));
        btnLogin.setOnMouseExited(e -> btnLogin.setStyle(
            "-fx-background-color: linear-gradient(to right, #2563eb, #1d4ed8);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10;" +
            "-fx-cursor: hand;"
        ));

        // Submit action
        Runnable performLogin = () -> {
            String userVal = txtUser.getText().trim();
            String passVal = txtPass.getText();

            if (userVal.isEmpty() || passVal.isEmpty()) {
                lblError.setText("Por favor, llene todos los campos.");
                lblError.setVisible(true);
                return;
            }

            btnLogin.setDisable(true);
            lblError.setVisible(false);

            // Execute auth in a small task to keep UI responsive
            UsuarioDAO dao = new UsuarioDAO();
            UsuarioDAO.Usuario authUser = dao.autenticar(userVal, passVal);

            if (authUser != null) {
                this.authenticated = true;
                this.activeUser = authUser;
                org.example.logic.SessionManager.setCurrentUser(authUser);
                dialog.close();
                onSuccess.run(); // Call main program trigger
            } else {
                btnLogin.setDisable(false);
                lblError.setText("Usuario o contraseña incorrectos.");
                lblError.setVisible(true);
            }
        };

        btnLogin.setOnAction(e -> performLogin.run());
        txtPass.setOnAction(e -> performLogin.run()); // Login on pressing Enter
        txtUser.setOnAction(e -> performLogin.run());

        // Assembly
        container.getChildren().addAll(header, logoView, lblTitle, lblSubtitle, grid, lblError, btnLogin);

        Scene scene = new Scene(container);
        scene.setFill(Color.TRANSPARENT); // Allow transparent window corners
        
        // Load fonts/css if needed
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.show();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public UsuarioDAO.Usuario getActiveUser() {
        return activeUser;
    }
}
