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

        ButtonType saveButton = new ButtonType("Guardar Cambios", ButtonBar.ButtonData.YES);
        ButtonType discardButton = new ButtonType("No Guardar", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        UIFactory.estilizarDialogo(alert);

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

        ButtonType saveButton = new ButtonType(saveLabel, ButtonBar.ButtonData.YES);
        ButtonType discardButton = new ButtonType(discardLabel, ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        UIFactory.estilizarDialogo(alert);

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
            logoView.setFitHeight(40);
            logoView.setFitWidth(40);
            alert.setGraphic(logoView);
        } catch (Exception ignored) {
        }
    }

    private enum Decision {
        SAVE,
        DISCARD,
        CANCEL
    }
}
