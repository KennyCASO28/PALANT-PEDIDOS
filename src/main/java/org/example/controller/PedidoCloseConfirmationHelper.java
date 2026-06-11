package org.example.controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.utils.UIFactory;

import java.util.Optional;
import java.util.function.Supplier;

final class PedidoCloseConfirmationHelper {

    private PedidoCloseConfirmationHelper() {
    }

    static boolean confirmExit(boolean hasUnsavedChanges, Supplier<Boolean> saveAction) {
        if (!hasUnsavedChanges) {
            Platform.exit();
            return true;
        }

        Decision decision = showUnsavedChangesDialog(
                "Desea guardar los cambios antes de salir?",
                "Se han detectado modificaciones que se perderan si no las guarda.",
                "Guardar y Salir",
                "Salir sin guardar");

        if (decision == Decision.SAVE) {
            if (saveAction.get()) {
                Platform.exit();
                return true;
            }
            return false;
        }
        if (decision == Decision.DISCARD) {
            Platform.exit();
            return true;
        }
        return false;
    }

    static boolean confirmWindowClose(boolean projectDirty, Supplier<Boolean> saveAction) {
        if (!projectDirty) {
            return true;
        }

        Decision decision = showUnsavedChangesDialog(
                "Desea guardar los cambios antes de cerrar la ventana?",
                "Se han detectado modificaciones que se perderan si no las guarda.",
                "Guardar y Cerrar",
                "Cerrar sin guardar");

        if (decision == Decision.SAVE) {
            return saveAction.get();
        }
        return decision == Decision.DISCARD;
    }

    static boolean confirmProjectClose(boolean projectDirty, Supplier<Boolean> saveAction) {
        if (!projectDirty) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cambios sin guardar");
        alert.setHeaderText("Que desea hacer con los cambios en este proyecto?");
        alert.setContentText("Hay cambios pendientes de guardar.");
        applyAppGraphic(alert);

        ButtonType saveButton = new ButtonType("Guardar");
        ButtonType discardButton = new ButtonType("No Guardar");
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        UIFactory.estilizarDialogo(alert);
        styleProjectCloseButtons(alert, saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty()) {
            return false;
        }
        if (result.get() == saveButton) {
            return saveAction.get();
        }
        return result.get() == discardButton;
    }

    private static Decision showUnsavedChangesDialog(
            String headerText,
            String contentText,
            String saveLabel,
            String discardLabel) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Palant - Cambios sin guardar");
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        applyAppGraphic(alert);

        ButtonType saveButton = new ButtonType(saveLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType discardButton = new ButtonType(discardLabel, ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        UIFactory.estilizarDialogo(alert);
        alert.setOnShown(event -> styleButtons(alert, saveButton, discardButton, cancelButton));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty()) {
            return Decision.CANCEL;
        }
        if (result.get() == saveButton) {
            return Decision.SAVE;
        }
        if (result.get() == discardButton) {
            return Decision.DISCARD;
        }
        return Decision.CANCEL;
    }

    private static void applyAppGraphic(Alert alert) {
        try {
            Image logo = UIFactory.getAppLogo();
            if (logo == null) {
                return;
            }
            ImageView logoView = new ImageView(logo);
            logoView.setFitHeight(48);
            logoView.setFitWidth(48);
            alert.setGraphic(logoView);
        } catch (Exception ignored) {
        }
    }

    private static void styleButtons(
            Alert alert,
            ButtonType saveButton,
            ButtonType discardButton,
            ButtonType cancelButton) {
        addStyleClass((Button) alert.getDialogPane().lookupButton(saveButton), "dialog-button-success");
        addStyleClass((Button) alert.getDialogPane().lookupButton(discardButton), "dialog-button-danger");
        addStyleClass((Button) alert.getDialogPane().lookupButton(cancelButton), "dialog-button-cancel");
    }

    private static void addStyleClass(Button button, String styleClass) {
        if (button != null) {
            button.getStyleClass().add(styleClass);
        }
    }

    private static void styleProjectCloseButtons(
            Alert alert,
            ButtonType saveButton,
            ButtonType discardButton,
            ButtonType cancelButton) {
        styleButton((Button) alert.getDialogPane().lookupButton(saveButton),
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; "
                        + "-fx-min-width: 150; -fx-padding: 8 15; -fx-cursor: hand;",
                "Guardar Cambios");
        styleButton((Button) alert.getDialogPane().lookupButton(discardButton),
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; "
                        + "-fx-min-width: 150; -fx-padding: 8 15; -fx-cursor: hand;",
                "No Guardar");
        styleButton((Button) alert.getDialogPane().lookupButton(cancelButton),
                "-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; "
                        + "-fx-min-width: 150; -fx-padding: 8 15; -fx-cursor: hand;",
                null);
    }

    private static void styleButton(Button button, String style, String text) {
        if (button == null) {
            return;
        }
        button.setStyle(style);
        if (text != null) {
            button.setText(text);
        }
    }

    private enum Decision {
        SAVE,
        DISCARD,
        CANCEL
    }
}
