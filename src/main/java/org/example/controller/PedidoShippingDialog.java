package org.example.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.example.dto.DatosEnvioDTO;
import org.example.utils.UIFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class PedidoShippingDialog {

    private PedidoShippingDialog() {
    }

    static Optional<DatosEnvioDTO> show(Window owner, DatosEnvioDTO currentData, List<String> sellers) {
        Dialog<DatosEnvioDTO> dialog = new Dialog<>();
        dialog.setTitle("Datos de Envio");
        dialog.setHeaderText(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        UIFactory.estilizarDialogo(dialog);

        ButtonType saveButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(saveButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(680);
        dialog.getDialogPane().setPrefHeight(440);
        dialog.getDialogPane().setMinHeight(440);

        ShippingForm form = ShippingForm.newForm(currentData, sellers);
        dialog.getDialogPane().setContent(form.content());

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        if (saveButton != null) {
            saveButton.setStyle(
                    "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; "
                            + "-fx-background-radius: 8; -fx-padding: 10 18; -fx-cursor: hand;");
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Optional<String> validationError = form.validate();
                if (validationError.isPresent()) {
                    UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.WARNING,
                            "Datos incompletos",
                            validationError.get());
                    event.consume();
                }
            });
        }

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setStyle(
                    "-fx-background-color: #e2e8f0; -fx-text-fill: #0f172a; -fx-font-weight: bold; "
                            + "-fx-background-radius: 8; -fx-padding: 10 18; -fx-cursor: hand;");
        }

        dialog.setResultConverter(button -> saveButtonType.equals(button) ? form.toDto() : null);
        return dialog.showAndWait();
    }

    private record ShippingForm(
            VBox content,
            TextField nombresField,
            TextField apellidosField,
            TextField dniField,
            TextField celularField,
            TextField lugarEnvioField,
            ComboBox<String> vendedorCombo) {

        private static ShippingForm newForm(DatosEnvioDTO currentData, List<String> sellers) {
            TextField nombresField = createField("Nombres");
            TextField apellidosField = createField("Apellidos");
            TextField dniField = createField("DNI");
            TextField celularField = createField("Celular (opcional)");
            TextField lugarEnvioField = createField("Lugar de envio");
            ComboBox<String> vendedorCombo = new ComboBox<>();
            vendedorCombo.setPromptText("Vendedor");
            vendedorCombo.setStyle(fieldStyle());
            vendedorCombo.getItems().setAll(sellers != null ? sellers : new ArrayList<>());

            DatosEnvioDTO current = currentData != null ? currentData : new DatosEnvioDTO();
            if (current.getNombres() != null) {
                nombresField.setText(current.getNombres());
            }
            if (current.getApellidos() != null) {
                apellidosField.setText(current.getApellidos());
            }
            if (current.getDni() != null) {
                dniField.setText(current.getDni());
            }
            if (current.getCelular() != null) {
                celularField.setText(current.getCelular());
            }
            if (current.getLugarEnvio() != null) {
                lugarEnvioField.setText(current.getLugarEnvio());
            }
            if (current.getVendedorAtiende() != null) {
                vendedorCombo.setValue(current.getVendedorAtiende());
            }

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10, 14, 14, 14));
            addRow(grid, 0, "Nombres*", nombresField);
            addRow(grid, 1, "Apellidos*", apellidosField);
            addRow(grid, 2, "DNI*", dniField);
            addRow(grid, 3, "Celular", celularField);
            addRow(grid, 4, "Lugar de envio*", lugarEnvioField);
            addRow(grid, 5, "Vendedor*", vendedorCombo);

            ColumnConstraints labelColumn = new ColumnConstraints();
            labelColumn.setMinWidth(130);
            ColumnConstraints fieldColumn = new ColumnConstraints();
            fieldColumn.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().setAll(labelColumn, fieldColumn);

            VBox content = new VBox(10, createHeader(), grid);
            content.setStyle("-fx-background-color: white;");
            return new ShippingForm(content, nombresField, apellidosField, dniField, celularField, lugarEnvioField,
                    vendedorCombo);
        }

        private Optional<String> validate() {
            String nombres = valueOf(nombresField);
            String apellidos = valueOf(apellidosField);
            String dni = valueOf(dniField);
            String lugar = valueOf(lugarEnvioField);
            String vendedor = vendedorCombo.getValue() != null ? vendedorCombo.getValue().trim() : "";

            if (nombres.isEmpty() || apellidos.isEmpty() || dni.isEmpty() || lugar.isEmpty() || vendedor.isEmpty()) {
                return Optional.of("Complete los campos obligatorios (*).");
            }
            if (!dni.matches("\\d{8}")) {
                return Optional.of("El DNI debe tener 8 digitos.");
            }
            return Optional.empty();
        }

        private DatosEnvioDTO toDto() {
            DatosEnvioDTO dto = new DatosEnvioDTO();
            dto.setNombres(valueOf(nombresField));
            dto.setApellidos(valueOf(apellidosField));
            dto.setDni(valueOf(dniField));

            String celular = valueOf(celularField);
            dto.setCelular(celular.isEmpty() ? null : celular);
            dto.setLugarEnvio(valueOf(lugarEnvioField));
            dto.setVendedorAtiende(vendedorCombo.getValue() != null ? vendedorCombo.getValue().trim() : null);
            return dto;
        }

        private static VBox createHeader() {
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(10, 10, 0, 10));

            try {
                Image logo = UIFactory.getAppLogo();
                if (logo != null) {
                    ImageView imageView = new ImageView(logo);
                    imageView.setFitWidth(42);
                    imageView.setFitHeight(42);
                    imageView.setPreserveRatio(true);
                    header.getChildren().add(imageView);
                }
            } catch (Exception ignored) {
            }

            VBox headerText = new VBox(2);
            Label title = new Label("DATOS DE ENVIO");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0B213E;");
            Label subtitle = new Label("Esta informacion se imprime en la Ficha Tecnica.");
            subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            headerText.getChildren().addAll(title, subtitle);
            header.getChildren().add(headerText);
            return new VBox(header);
        }

        private static void addRow(GridPane grid, int row, String labelText, javafx.scene.Node field) {
            Label label = new Label(labelText);
            label.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f172a; -fx-font-weight: bold;");
            grid.add(label, 0, row);
            grid.add(field, 1, row);
        }

        private static TextField createField(String prompt) {
            TextField field = new TextField();
            field.setPromptText(prompt);
            field.setStyle(fieldStyle());
            return field;
        }

        private static String fieldStyle() {
            return "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 6; "
                    + "-fx-background-radius: 6; -fx-padding: 8 10;";
        }

        private static String valueOf(TextField field) {
            return field.getText() != null ? field.getText().trim() : "";
        }
    }

}
