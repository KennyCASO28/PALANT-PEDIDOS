package org.example.utils;

import javafx.geometry.Insets;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.model.TipoGenero;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import org.example.controller.uicomponent.ColorPalettePopup;
import java.util.function.BiConsumer;
import javafx.scene.Cursor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Clase utilitaria para la creación estandarizada de componentes UI
 * y diálogos, descargando responsabilidad del controlador.
 */
public class UIFactory {

    public static final int RECOMPILE_VERSION = 2; // Final sync
    // --- CACHE ---
    private static javafx.scene.image.Image cachedLogo;

    public static javafx.scene.image.Image getAppLogo() {
        if (cachedLogo == null) {
            try {
                // DEBUG: Print loading attempt
                System.out.println("UIFactory: Loading App Logo...");
                // Use optimized smaller logo (512px)
                cachedLogo = new javafx.scene.image.Image(
                        UIFactory.class.getResourceAsStream("/vectors/logo_small.png"));

                if (cachedLogo.isError()) {
                    System.err.println("UIFactory: Error loading logo: " + cachedLogo.getException());
                } else {
                    System.out.println("UIFactory: Logo loaded successfully. Width: " + cachedLogo.getWidth());
                }
            } catch (Exception e) {
                System.err.println("UIFactory: Could not load app logo: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return cachedLogo;
    }

    // ... (Skipping unaffected methods) ...

    public static Label crearTituloSeccion(String texto) {
        return crearTituloSeccion(texto, null);
    }

    public static Label crearTituloSeccion(String texto, String iconCode) {
        Label l = new Label(texto.toUpperCase());
        if (iconCode != null && !iconCode.isEmpty()) {
            FontIcon icon = crearIcono(iconCode, 16, "#2c3e50"); // Icon color same as text
            l.setGraphic(icon);
            l.setGraphicTextGap(10);
        }
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER_LEFT);
        l.getStyleClass().add("section-title-label");
        // Reduced margin as it will likely be inside a card now
        VBox.setMargin(l, new Insets(5, 0, 10, 0));
        return l;
    }

    /**
     * Creates a styled 'Card' section (Gray Box) as requested.
     */
    public static VBox crearSeccionTarjeta(String titulo, String iconCode, Node content) {
        VBox card = new VBox(5);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 10; -fx-border-color: #ecf0f1; -fx-border-radius: 8;");

        Label lbl = crearTituloSeccion(titulo, iconCode);
        VBox.setMargin(lbl, new Insets(0, 0, 8, 0)); // Tighter margin inside card

        card.getChildren().addAll(lbl, content);
        return card;
    }

    public static ToggleButton crearBotonOpcion(String texto, Node grafico) {
        ToggleButton btn = new ToggleButton(texto);
        btn.setContentDisplay(ContentDisplay.TOP);
        btn.setAlignment(Pos.CENTER);
        btn.setMinWidth(0); // Permitir compresión
        btn.setMinHeight(72);
        btn.setPrefHeight(74); // Wider visual weight for card-like selectors
        btn.setGraphicTextGap(6);
        btn.setWrapText(true);
        btn.getStyleClass().add("toggle-button");
        btn.setStyle("-fx-cursor: hand; -fx-text-alignment: center; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 10;");
        btn.setGraphic(grafico);

        // Add selection listener for styling
        btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            updateButtonStyle(btn);
        });

        // Initial style
        updateButtonStyle(btn);

        return btn;
    }

    public static ToggleButton crearBotonOpcion(String texto, String iconCode, int iconSize) {
        FontIcon icon = crearIcono(iconCode, iconSize, "#2c3e50");
        return crearBotonOpcion(texto, icon);
    }

    public static FontIcon crearIcono(String iconCode, int iconSize, String colorHex) {
        return IconUtils.crearIcono(iconCode, iconSize, colorHex);
    }

    /**
     * Crea un manipulador circular moderno con icono.
     */
    public static StackPane crearStackHandle(String iconCode, int size, String iconColor, String bgColor,
            Cursor cursor) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);

        Circle bg = new Circle(size / 2.0);
        bg.setFill(Color.web(bgColor));
        bg.setStroke(Color.WHITE);
        bg.setStrokeWidth(1.0); // Sutil

        FontIcon icon = null;
        if (iconCode != null && !iconCode.isEmpty()) {
            icon = crearIcono(iconCode, (int) (size * 0.65), iconColor);
            pane.getChildren().addAll(bg, icon);
        } else {
            pane.getChildren().add(bg);
        }

        pane.setCursor(cursor);
        // Efecto de sombra suave
        pane.setEffect(new DropShadow(2, 0, 1, Color.rgb(0, 0, 0, 0.25)));

        // --- HOVER EFFECTS ---
        pane.setOnMouseEntered(e -> {
            bg.setStroke(Color.ORANGE);
            bg.setStrokeWidth(2.0);
            pane.setEffect(new DropShadow(5, 0, 1, Color.rgb(0, 0, 0, 0.45)));
        });
        pane.setOnMouseExited(e -> {
            bg.setStroke(Color.WHITE);
            bg.setStrokeWidth(1.0);
            pane.setEffect(new DropShadow(2, 0, 1, Color.rgb(0, 0, 0, 0.25)));
        });

        // Mantener tamaño fijo
        pane.setMinWidth(size);
        pane.setMaxWidth(size);
        pane.setMinHeight(size);
        pane.setMaxHeight(size);

        return pane;
    }

    /**
     * Crea un manipulador cuadrado moderno.
     */
    public static StackPane crearSquareHandle(String iconCode, int size, String iconColor, String bgColor,
            Cursor cursor) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);

        // TRANSPARENT HIT AREA (Garantiza que el clic siempre gane sobre el fondo)
        // Reducido a 12 para que no estorbe la selección de otras figuras cercanas
        Rectangle hitArea = new Rectangle(12, 12);
        hitArea.setFill(Color.TRANSPARENT);
        hitArea.setMouseTransparent(false);

        Rectangle bg = new Rectangle(size, size);
        bg.setArcWidth(2);
        bg.setArcHeight(2);
        bg.setFill(Color.web(bgColor));
        bg.setStroke(Color.web(iconColor));
        bg.setStrokeWidth(1.0);

        FontIcon icon = null;
        if (iconCode != null && !iconCode.isEmpty()) {
            icon = crearIcono(iconCode, (int) (size * 0.65), iconColor);
            pane.getChildren().addAll(hitArea, bg, icon);
        } else {
            pane.getChildren().addAll(hitArea, bg);
        }

        pane.setCursor(cursor);
        pane.setEffect(new DropShadow(2, 0, 1, Color.rgb(0, 0, 0, 0.25)));

        // --- HOVER EFFECTS ---
        pane.setOnMouseEntered(e -> {
            bg.setStroke(Color.ORANGE);
            bg.setStrokeWidth(2.0);
            pane.setEffect(new DropShadow(5, 0, 1, Color.rgb(0, 0, 0, 0.45)));
        });
        pane.setOnMouseExited(e -> {
            bg.setStroke(Color.web(iconColor));
            bg.setStrokeWidth(1.0);
            pane.setEffect(new DropShadow(2, 0, 1, Color.rgb(0, 0, 0, 0.25)));
        });

        pane.setMinWidth(12);
        pane.setMaxWidth(12);
        pane.setMinHeight(12);
        pane.setMaxHeight(12);

        return pane;
    }

    /**
     * ICON HANDLE: Ícono limpio sin fondo circular.
     * 
     * 
     * 
     * 
     * Para rotación y sesgo — sutil, pequeño y legible.
     */
    public static StackPane crearIconHandle(String iconCode, int size, String color, Cursor cursor) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);

        // Transparent hit area for easier clicking
        Circle hitArea = new Circle(size / 1.2);
        hitArea.setFill(Color.TRANSPARENT);

        FontIcon icon = crearIcono(iconCode, size, color);
        // White outline for contrast on any background
        icon.setStroke(Color.web(color, 0.25));
        icon.setStrokeWidth(0.8);

        pane.getChildren().addAll(hitArea, icon);
        pane.setCursor(cursor);

        // Subtle shadow so icon is readable on any background
        pane.setEffect(new DropShadow(3, 0, 1, Color.rgb(0, 0, 0, 0.45)));

        // --- HOVER EFFECTS ---
        pane.setOnMouseEntered(e -> {
            icon.setIconColor(Color.ORANGE);
            pane.setEffect(new DropShadow(6, 0, 1, Color.rgb(0, 0, 0, 0.65)));
        });
        pane.setOnMouseExited(e -> {
            icon.setIconColor(Color.web(color));
            pane.setEffect(new DropShadow(3, 0, 1, Color.rgb(0, 0, 0, 0.45)));
        });

        pane.setMinWidth(size);
        pane.setMaxWidth(size);
        pane.setMinHeight(size);
        pane.setMaxHeight(size);
        return pane;
    }

    /**
     * PIVOT HANDLE: Eje central de rotación.
     * Círculo limpio con punto central — igual en todos los tipos de capa.
     */
    public static javafx.scene.Group crearPivotHandle() {
        javafx.scene.Group pane = new javafx.scene.Group();

        // White halo for contrast on both light and dark backgrounds
        Circle halo = new Circle(5);
        halo.setFill(Color.rgb(255, 255, 255, 0.55));
        halo.setStroke(Color.TRANSPARENT);

        // Outer ring — radius 5 so natural pref size = 10px (exact for -5 offset)
        Circle ring = new Circle(5);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.rgb(0, 0, 0, 0.80));
        ring.setStrokeWidth(1.2);

        // Center dot
        Circle dot = new Circle(1.5);
        dot.setFill(Color.rgb(0, 0, 0, 0.80));

        // Group places children at (0,0) by default which is the center of the circles
        // We set their centers exactly to 5,5 locally so the Group is exactly 10x10
        halo.setCenterX(5);
        halo.setCenterY(5);
        ring.setCenterX(5);
        ring.setCenterY(5);
        dot.setCenterX(5);
        dot.setCenterY(5);

        pane.getChildren().addAll(halo, ring, dot);
        pane.setCursor(Cursor.MOVE);
        pane.setEffect(new DropShadow(4, 0, 1, Color.rgb(0, 0, 0, 0.35)));

        // --- HOVER EFFECTS ---
        pane.setOnMouseEntered(e -> {
            ring.setStroke(Color.ORANGE);
            ring.setStrokeWidth(2.0);
            dot.setFill(Color.ORANGE);
        });
        pane.setOnMouseExited(e -> {
            ring.setStroke(Color.rgb(0, 0, 0, 0.80));
            ring.setStrokeWidth(1.2);
            dot.setFill(Color.rgb(0, 0, 0, 0.80));
        });

        return pane;
    }

    /**
     * Updates the visual style of a toggle button based on its selection state.
     * Respects "themeColor" property if set.
     */
    public static void updateButtonStyle(ToggleButton btn) {
        String mainColor = "#3498db";
        String subtleBg = "#eff9ff"; // Default subtle blue
        String borderColor = "#2980b9";

        // 1. Check for explicit gender button
        if (btn.getUserData() instanceof TipoGenero) {
            TipoGenero g = (TipoGenero) btn.getUserData();
            if (g == TipoGenero.MUJER) {
                mainColor = "#e91e63";
                subtleBg = "#fce4ec"; // Pink 50
                borderColor = "#e91e63";
            } else if (g == TipoGenero.HOMBRE) {
                mainColor = "#0288D1";
                subtleBg = "#e1f5fe"; // Light Blue 50
                borderColor = "#0288D1";
            }
        }
        // 2. Check for assigned Theme Property
        else if (btn.getProperties().containsKey("themeColor")) {
            mainColor = (String) btn.getProperties().get("themeColor");
            subtleBg = deriveSubtleColor(mainColor);
        }

        String fontSize = (String) btn.getProperties().getOrDefault("customFontSize", "13px");
        String padding = (String) btn.getProperties().getOrDefault("customPadding", "8 12");
        String common = "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: " + padding + "; -fx-cursor: hand; -fx-font-size: " + fontSize + "; ";

        if (btn.isSelected()) {
            btn.setStyle(common +
                    "-fx-background-color: " + subtleBg + "; " +
                    "-fx-text-fill: " + mainColor + "; " +
                    "-fx-border-color: " + mainColor + "; " +
                    "-fx-border-width: 1; " +
                    "-fx-font-weight: bold; " +
                    "-fx-accent: " + mainColor + ";");
        } else {
            btn.setStyle(common +
                    "-fx-background-color: white; " +
                    "-fx-text-fill: #546e7a; " +
                    "-fx-border-color: #cfd8dc; " +
                    "-fx-border-width: 1; " +
                    "-fx-accent: " + mainColor + ";");
        }
    }

    /**
     * Applies gender-based theme color to a control.
     */
    public static void applyGenderTheme(javafx.scene.control.Control node, org.example.model.TipoGenero gender) {
        String color = (gender == org.example.model.TipoGenero.MUJER) ? "#e91e63" : "#0288D1";
        node.getProperties().put("themeColor", color);

        if (node instanceof javafx.scene.control.ToggleButton) {
            updateButtonStyle((javafx.scene.control.ToggleButton) node);
        }
        // We leave CheckBox standard styling to be handled explicitly or by generic
        // accent
        else if (node instanceof javafx.scene.control.CheckBox || node instanceof javafx.scene.control.RadioButton) {
            String currentStyle = node.getStyle() != null ? node.getStyle() : "";
            if (!currentStyle.contains("-fx-accent")) {
                node.setStyle(currentStyle + "; -fx-accent: " + color + ";");
            }
        }
    }

    // Updated helper name and logic
    private static String deriveSubtleColor(String hexColor) {
        if (hexColor.equals("#e91e63"))
            return "#fce4ec"; // Pink 50
        if (hexColor.equals("#0288D1"))
            return "#e1f5fe"; // Light Blue 50
        if (hexColor.equals("#3498db"))
            return "#eff9ff";
        return "#f5f5f5";
    }

    public static void mostrarAlerta(javafx.scene.control.Alert.AlertType tipo, String titulo, String mensaje) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(tipo);
        alert.setTitle("Palant - Información"); // Título genérico de ventana
        alert.setHeaderText(titulo); // Título destacado dentro del diálogo
        alert.setContentText(mensaje);

        estilizarDialogo(alert);
        alert.showAndWait();
    }

    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Palant - Confirmación");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);

        estilizarDialogo(alert);
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
    }

    public static void estilizarDialogo(javafx.scene.control.Dialog<?> dialog) {
        javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
        dialogPane.getStyleClass().add("custom-dialog-pane");

        boolean isSaveRelated = false;
        if (dialog.getTitle() != null) {
            String titleLower = dialog.getTitle().toLowerCase();
            if (titleLower.contains("guardar") || titleLower.contains("cambios") || titleLower.contains("salvar")) {
                isSaveRelated = true;
            }
        }

        if (dialog instanceof javafx.scene.control.Alert) {
            javafx.scene.control.Alert alert = (javafx.scene.control.Alert) dialog;
            if (alert.getHeaderText() == null || alert.getHeaderText().trim().isEmpty()) {
                switch (alert.getAlertType()) {
                    case WARNING -> alert.setHeaderText("Advertencia");
                    case CONFIRMATION -> {
                        if (isSaveRelated) {
                            alert.setHeaderText("Guardar Cambios");
                        } else {
                            alert.setHeaderText("Confirmación");
                        }
                    }
                    case ERROR -> alert.setHeaderText("Error");
                    case INFORMATION -> alert.setHeaderText("Información");
                    default -> alert.setHeaderText("Mensaje");
                }
            }
        }

        if (dialog.getHeaderText() != null) {
            String headerLower = dialog.getHeaderText().toLowerCase();
            if (headerLower.contains("guardar") || headerLower.contains("cambios") || headerLower.contains("salvar")) {
                isSaveRelated = true;
            }
        }

        if (dialog instanceof javafx.scene.control.Alert) {
            javafx.scene.control.Alert alert = (javafx.scene.control.Alert) dialog;
            int width = 480;
            if (alert.getAlertType() == javafx.scene.control.Alert.AlertType.CONFIRMATION || isSaveRelated) {
                width = 540; // Expand width to prevent button text truncation when multiple options are present
            }
            dialogPane.setMaxWidth(width);
            dialogPane.setPrefWidth(width);
            if (alert.getAlertType() != null) {
                switch (alert.getAlertType()) {
                    case WARNING -> dialogPane.getStyleClass().add("dialog-warning");
                    case CONFIRMATION -> {
                        if (isSaveRelated) {
                            dialogPane.getStyleClass().add("dialog-save");
                        } else {
                            dialogPane.getStyleClass().add("dialog-confirmation");
                        }
                    }
                    case ERROR -> dialogPane.getStyleClass().add("dialog-error");
                    case INFORMATION -> dialogPane.getStyleClass().add("dialog-information");
                    default -> {}
                }
            }
        } else {
            dialogPane.setMaxWidth(500);
            dialogPane.setPrefWidth(500);
            if (isSaveRelated) {
                dialogPane.getStyleClass().add("dialog-save");
            } else {
                dialogPane.getStyleClass().add("dialog-confirmation");
            }
        }

        // Auto-Center dialog on active window
        javafx.stage.Window activeWindow = javafx.stage.Window.getWindows().stream()
                .filter(javafx.stage.Window::isFocused).findFirst().orElse(null);
        if (activeWindow != null && dialog.getOwner() == null) {
            try {
                dialog.initOwner(activeWindow);
            } catch (Exception e) {
                // Ignore if owner cannot be set at this point
            }
        }

        // Apply Stylesheet
        try {
            dialogPane.getStylesheets().add(UIFactory.class.getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load styles.css for dialog: " + e.getMessage());
        }

        // Apply Icon - PARANOID STRATEGY (Listeners + OnShowing + RunLater)
        javafx.scene.image.Image logo = getAppLogo();
        if (logo != null) {
            // Strategy 1: Listener on Scene -> Window
            dialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obsW, oldWindow, newWindow) -> {
                        if (newWindow instanceof javafx.stage.Stage) {
                            javafx.stage.Stage stage = (javafx.stage.Stage) newWindow;
                            if (!stage.getIcons().contains(logo)) {
                                stage.getIcons().add(logo);
                            }
                        }
                    });
                }
            });

            // Strategy 2: OnShowing Event with RunLater (Catch-all)
            dialog.setOnShowing(e -> javafx.application.Platform.runLater(() -> {
                try {
                    if (dialogPane.getScene() != null
                            && dialogPane.getScene().getWindow() instanceof javafx.stage.Stage) {
                        javafx.stage.Stage stage = (javafx.stage.Stage) dialogPane.getScene().getWindow();
                        if (!stage.getIcons().contains(logo)) {
                            stage.getIcons().add(logo);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }));

            // Strategy 3: Immediate Attempt (If cached/reused)
            if (dialogPane.getScene() != null && dialogPane.getScene().getWindow() instanceof javafx.stage.Stage) {
                ((javafx.stage.Stage) dialogPane.getScene().getWindow()).getIcons().add(logo);
            }
        }

        // ULTIMATE RELIABILITY: Style only when the dialog is actually shown
        dialog.setOnShown(event -> {
            dialogPane.getButtonTypes().forEach(btnType -> {
                javafx.scene.control.Button btn = (javafx.scene.control.Button) dialogPane.lookupButton(btnType);
                if (btn != null) {
                    btn.getStyleClass().clear();
                    btn.getStyleClass().add("button");
                    btn.setFocusTraversable(false);

                    javafx.scene.control.ButtonBar.ButtonData data = btnType.getButtonData();
                    String btnText = (btn.getText() != null) ? btn.getText().toLowerCase() : "";
                    String finalColor = "#3498db"; // Default Blue

                    // Classification by Data AND Text Fallback (supports "Guardar Cambios", "No Guardar", etc)
                    if (data == javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE || btnText.contains("cancel")) {
                        finalColor = "#7f8c8d"; // Plomo
                        btn.getStyleClass().add("dialog-button-cancel");
                    } else if (data == javafx.scene.control.ButtonBar.ButtonData.NO || btnText.contains("no guardar") || btnText.equals("no")) {
                        finalColor = "#e74c3c"; // Rojo
                        btn.getStyleClass().add("dialog-button-danger");
                    } else if (data == javafx.scene.control.ButtonBar.ButtonData.YES || data == javafx.scene.control.ButtonBar.ButtonData.OK_DONE
                            || data == javafx.scene.control.ButtonBar.ButtonData.APPLY || btnText.contains("guardar") 
                            || btnText.contains("aceptar") || btnText.contains("si")) {
                        finalColor = "#27ae60"; // Verde
                        btn.getStyleClass().add("dialog-button-success");
                    } else {
                        btn.getStyleClass().add("dialog-button");
                    }

                    // Force the color and kill the oval, with clean compact sizing
                    btn.setStyle("-fx-background-color: " + finalColor + " !important; " +
                                 "-fx-text-fill: white !important; " +
                                 "-fx-font-weight: bold !important; " +
                                 "-fx-background-insets: 0 !important; " +
                                 "-fx-focus-color: transparent !important; " +
                                 "-fx-faint-focus-color: transparent !important; " +
                                 "-fx-border-width: 0 !important; " +
                                 "-fx-effect: null !important; " +
                                 "-fx-min-width: 135px !important; " +
                                 "-fx-padding: 8px 16px !important; " +
                                 "-fx-cursor: hand !important; " +
                                 "-fx-background-radius: 6px !important;");
                }
            });
        });
    }

    /**
     * Creates a ColorPicker with a restricted palette of common colors for quick
     * selection.
     */
    public static javafx.scene.control.ColorPicker crearSelectorColorRapido(javafx.scene.paint.Color initialColor) {
        javafx.scene.control.ColorPicker cp = new javafx.scene.control.ColorPicker(initialColor);
        cp.getStyleClass().add("button"); // Look like a button

        // CSS inyectable para forzar que el rectángulo interno o "picker-color" sea
        // mucho más grande y ancho
        cp.setStyle(
                "-fx-color-label-visible: false; " +
                        "-fx-background-insets: 0; " +
                        "-fx-pref-width: 80; " + // Ancho duplicado para mayor impacto visual
                        "-fx-pref-height: 25; " +
                        "-fx-padding: 2; " + // Padding minúsculo para que el color ocupe todo el botón
                        "-fx-border-color: #bdc3c7; -fx-border-radius: 4; -fx-background-radius: 4;");

        // El verdadero recuadro de color interior se llama .picker-color
        // No es posible inyectar sub-clases por setStyle, pero agrandar el botón y
        // reducir el padding
        // de la caja principal obliga a JavaFX a expandir el recuadro interior
        // automáticamente.

        // Standard Palette
        cp.getCustomColors().addAll(
                javafx.scene.paint.Color.BLACK,
                javafx.scene.paint.Color.WHITE,
                javafx.scene.paint.Color.web("#e74c3c"), // Red
                javafx.scene.paint.Color.web("#3498db"), // Blue
                javafx.scene.paint.Color.web("#f1c40f"), // Yellow
                javafx.scene.paint.Color.web("#2ecc71"), // Green
                javafx.scene.paint.Color.web("#e67e22"), // Orange
                javafx.scene.paint.Color.web("#9b59b6"), // Purple
                javafx.scene.paint.Color.web("#34495e"), // Navy
                javafx.scene.paint.Color.web("#95a5a6") // Grey
        );
        return cp;
    }

    /**
     * Fixes ComboBox text readability by forcing dark text color.
     * This is needed for themes where the button cell text could be unreadable.
     */
    public static <T> void fixComboBoxReadability(ComboBox<T> combo) {
        // Enforce style on the Combo itself (Button Area)
        combo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-opacity: 1.0;");

        // Use a Custom Button Cell to guarantee text visibility
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: rgb(30, 41, 59); -fx-font-weight: bold;");
                }
            }
        });
    }

    /**
     * Creates a row with a toggle switch and label for boolean options.
     */
    public static HBox crearFilaOpcion(String text, boolean selected, TipoGenero genero, Consumer<Boolean> action) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle("-fx-padding: 10 14; -fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 4, 0, 0, 1);");
        CheckBox check = crearToggleSwitch(selected);
        check.selectedProperty().addListener((obs, old, val) -> action.accept(val));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E293B; -fx-font-weight: bold;");
        row.getChildren().addAll(check, lbl);
        HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
        return row;
    }

    /**
     * Compact version of option row, with smaller styling.
     */
    public static HBox crearFilaOpcionCompacta(String text, boolean selected, TipoGenero genero, Consumer<Boolean> action) {
        HBox row = crearFilaOpcion(text, selected, genero, action);
        row.setStyle("-fx-padding: 2 0; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");
        return row;
    }

    /**
     * Creates a styled toggle switch (CheckBox with toggle appearance).
     */
    public static CheckBox crearToggleSwitch(boolean selected) {
        CheckBox cb = new CheckBox();
        cb.setSelected(selected);
        cb.getStyleClass().add("toggle-switch");
        cb.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E293B;");
        return cb;
    }

    /**
     * Opens a file chooser dialog to select a file for opening.
     */
    public static File seleccionarArchivoAbrir(Window owner, String title, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        String desc = extension.substring(extension.lastIndexOf(".") + 1).toUpperCase() + " Files";
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, extension));
        return fileChooser.showOpenDialog(owner);
    }

    /**
     * Opens a file chooser dialog to select a file for saving.
     */
    public static File seleccionarArchivoGuardar(Window owner, String title, String extension, String description) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extension));
        return fileChooser.showSaveDialog(owner);
    }

    /**
     * Shows the advanced color selector with CMYK support.
     */
    public static void showColorSelector(Window owner, javafx.scene.paint.Color initial,
            java.util.function.Consumer<javafx.scene.paint.Color> onResult) {
        showColorSelector(owner, initial, onResult, null);
    }

    public static void showColorSelector(Window owner, javafx.scene.paint.Color initial,
            java.util.function.Consumer<javafx.scene.paint.Color> onResult,
            java.util.function.Consumer<javafx.scene.paint.Color> onLiveUpdate) {
        org.example.component.CustomColorDialog dialog = new org.example.component.CustomColorDialog(initial);
        dialog.initOwner(owner);
        if (onLiveUpdate != null) {
            dialog.setOnColorChanged(onLiveUpdate);
        }
        Optional<javafx.scene.paint.Color> result = dialog.showAndWait();
        if (result.isPresent()) {
            onResult.accept(result.get());
        } else {
            // Restore initial if cancelled and we had live updates
            if (onLiveUpdate != null) {
                onLiveUpdate.accept(initial);
            }
        }
    }

    /**
     * Creates a MenuButton that behaves like a ColorPicker but with a palette
     * first.
     */
    public static MenuButton createColorMenuButton(Color initialColor, String tooltip,
            Consumer<Color> onColor,
            BiConsumer<Consumer<Color>, Consumer<Color>> triggerEyedropper) {

        MenuButton btn = new MenuButton();
        if (tooltip != null)
            btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle(
                "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-padding: 3; -fx-alignment: CENTER; -fx-pref-width: 120; -fx-pref-height: 34; -fx-cursor: hand;");

        Runnable updateIcon = () -> {
            Color c = (Color) btn.getUserData();
            if (c == null)
                c = initialColor;
            javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(65, 14, c);
            r.setArcWidth(12);
            r.setArcHeight(12);
            r.setStroke(Color.web("#cbd5e1", 0.5));
            r.setStrokeWidth(1);
            btn.setGraphic(r);
        };

        btn.setUserData(initialColor);
        updateIcon.run();

        ObjectProperty<Color> colorProp = new SimpleObjectProperty<>(initialColor);
        btn.getProperties().put("colorProperty", colorProp);
        colorProp.addListener((obs, old, newVal) -> {
            if (newVal != null) {
                btn.setUserData(newVal);
                updateIcon.run();
            }
        });

        btn.setOnShowing(e -> {
            if (btn.getItems().isEmpty()) {
                ColorPalettePopup popupContent = new ColorPalettePopup(
                        (Color) btn.getUserData(),
                        (c) -> {
                            btn.setUserData(c);
                            updateIcon.run();
                            onColor.accept(c);
                            btn.hide();
                        },
                        () -> {
                            btn.hide();
                            if (triggerEyedropper != null) {
                                triggerEyedropper.accept(
                                        color -> {
                                            btn.setUserData(color);
                                            updateIcon.run();
                                            onColor.accept(color);
                                        },
                                        color -> {
                                            if (color != null) onColor.accept(color);
                                        });
                            }
                        },
                        () -> {
                            Color current = (Color) btn.getUserData();
                            if (current == null)
                                current = initialColor;

                            btn.hide();

                            showColorSelector(btn.getScene().getWindow(), current,
                                    (chosen) -> {
                                        btn.setUserData(chosen);
                                        updateIcon.run();
                                        onColor.accept(chosen);
                                    },
                                    (liveColor) -> {
                                        btn.setUserData(liveColor);
                                        updateIcon.run();
                                        onColor.accept(liveColor);
                                    });
                        });

                CustomMenuItem item = new CustomMenuItem();
                item.setHideOnClick(false);
                item.setContent(popupContent);
                btn.getItems().add(item);
            }
        });

        return btn;
    }

    /**
     * Safely updates the color of a MenuButton created via createColorMenuButton.
     */
    public static void setColorMenuButtonColor(MenuButton btn, Color color) {
        if (btn == null || color == null)
            return;
        Object prop = btn.getProperties().get("colorProperty");
        if (prop instanceof ObjectProperty) {
            ((ObjectProperty<Color>) prop).set(color);
        } else {
            // Fallback
            btn.setUserData(color);
        }
    }

    public static String toHexString(Color color) {
        if (color == null) return "#000000";
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}

