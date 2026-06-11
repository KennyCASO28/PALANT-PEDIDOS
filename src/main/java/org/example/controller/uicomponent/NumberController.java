package org.example.controller.uicomponent;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.example.component.NumberComposition;
import org.example.component.PrendaVisualizer;
import org.example.utils.UIFactory;
import org.example.pattern.PropertyChangeCommand;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NumberController {

    private final PrendaVisualizer visualizer;
    private final BiConsumer<Consumer<Color>, Consumer<Color>> eyedropperCallback;
    private final java.util.Map<Integer, MenuButton> pickerButtons = new java.util.HashMap<>();
    private VBox mainContainer;

    public NumberController(PrendaVisualizer visualizer,
            BiConsumer<Consumer<Color>, Consumer<Color>> eyedropperCallback) {
        this.visualizer = visualizer;
        this.eyedropperCallback = eyedropperCallback;
    }

    public void addToContainer(VBox container) {
        this.mainContainer = container;
        this.mainContainer.setFillWidth(true);
        updateUI();
    }

    public void updateUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();

        VBox content = new VBox(10);
        content.setPadding(new Insets(5, 10, 5, 10));
        content.getStyleClass().add("number-content-box");
        content.setFillWidth(true);

        mainContainer.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);

        Label lblNumbers = new Label("Numeración (Vectores):");
        lblNumbers.getStyleClass().add("number-section-header");
        content.getChildren().add(lblNumbers);

        if (visualizer.hasShirt()) {
            addNumberConfigSection(content, "Pecho", visualizer.getChestNumber());
            addNumberConfigSection(content, "Espalda", visualizer.getBackNumber());
        }

        if (visualizer.hasShorts()) {
            org.example.model.TipoCorte cut = visualizer.getCurrentCorteShort();
            if (cut != org.example.model.TipoCorte.LICRA && cut != org.example.model.TipoCorte.PANTALONETA) {
                addNumberConfigSection(content, "Short", visualizer.getShortNumber());
            }
        }
    }

    private void addNumberConfigSection(VBox parent, String label, NumberComposition composition) {
        VBox sectionBox = new VBox(10);
        sectionBox.getStyleClass().add("number-section-box");
        sectionBox.setMaxWidth(Double.MAX_VALUE);

        HBox hbActive = UIFactory.crearFilaOpcion("Número " + label, composition.isVisible(), visualizer.getGenero(),
                selected -> {
                    boolean oldActive = !selected;
                    PropertyChangeCommand<Boolean> cmd = new PropertyChangeCommand<>("Toggle Number: " + label, oldActive, selected, val -> {
                        if (label.equalsIgnoreCase("Pecho")) visualizer.setChestNumberVisible(val);
                        else if (label.equalsIgnoreCase("Espalda")) visualizer.setBackNumberVisible(val);
                        else visualizer.setShortNumberVisible(val);
                        updateUI();
                    });
                    visualizer.getHistoryManager().addCommand(cmd);
                    if (label.equalsIgnoreCase("Pecho")) visualizer.setChestNumberVisible(selected);
                    else if (label.equalsIgnoreCase("Espalda")) visualizer.setBackNumberVisible(selected);
                    else visualizer.setShortNumberVisible(selected);
                });
        
        hbActive.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hbActive, Priority.ALWAYS);
        CheckBox chkActive = (CheckBox) hbActive.getChildren().get(0);

        VBox configBox = new VBox(10);
        configBox.setPadding(new Insets(5, 0, 0, 10));
        configBox.setMaxWidth(Double.MAX_VALUE);
        configBox.visibleProperty().bind(chkActive.selectedProperty());
        configBox.managedProperty().bind(chkActive.selectedProperty());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.setMaxWidth(Double.MAX_VALUE);
        
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.NEVER);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        Label lblDigit = new Label("Dígito:");
        lblDigit.getStyleClass().add("number-label-digit");

        ComboBox<String> cmbNumber = new ComboBox<>();
        cmbNumber.getItems().addAll("9", "1");

        String initialValue = "9";
        if (label.equalsIgnoreCase("Pecho")) initialValue = visualizer.getCurrentChestNumberStr();
        else if (label.equalsIgnoreCase("Espalda")) initialValue = visualizer.getCurrentBackNumberStr();
        else initialValue = visualizer.getCurrentShortNumberStr();

        cmbNumber.setValue(initialValue != null ? initialValue : "9");
        cmbNumber.setMaxWidth(Double.MAX_VALUE);
        UIFactory.fixComboBoxReadability(cmbNumber);

        cmbNumber.setOnAction(e -> {
            String newVal = cmbNumber.getValue();
            visualizer.setGlobalNumberDigit(newVal);
            updateUI();
        });

        VBox digitField = new VBox(4, lblDigit, cmbNumber);
        digitField.setAlignment(Pos.CENTER_LEFT);
        digitField.setMaxWidth(Double.MAX_VALUE);
        configBox.getChildren().add(digitField);
        sectionBox.getChildren().addAll(hbActive, configBox);
        addColorsToSection(configBox, composition, label);
        parent.getChildren().add(sectionBox);
    }

    private void addColorsToSection(VBox configBox, NumberComposition composition, String label) {
        boolean isSelected = composition.getLayerColor(4) != null && !composition.getLayerColor(4).equals(Color.TRANSPARENT);
        CheckBox chkIncludeBrand = UIFactory.crearToggleSwitch(isSelected);
        chkIncludeBrand.setText("Incluir Marca");
        chkIncludeBrand.getStyleClass().add("number-check-brand");
        chkIncludeBrand.setOnAction(e -> {
            boolean active = chkIncludeBrand.isSelected();
            composition.setLayerColor(4, active ? Color.WHITE : Color.TRANSPARENT);
        });
        configBox.getChildren().add(chkIncludeBrand);

        // Manual Alignment Controls removed as requested

        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(8);
        colorGrid.setVgap(10);
        colorGrid.setMaxWidth(Double.MAX_VALUE);
        
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50); col1.setFillWidth(true); col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50); col2.setFillWidth(true); col2.setHgrow(Priority.ALWAYS);
        colorGrid.getColumnConstraints().addAll(col1, col2);

        addColorControl(colorGrid, 0, 0, "Base", composition, 0, Color.BLACK, label);
        addColorControl(colorGrid, 0, 1, "Combinación", composition, 1, Color.web("#333333"), label);
        addColorControl(colorGrid, 1, 0, "Contorno", composition, 2, Color.WHITE, label);
        addColorControl(colorGrid, 1, 1, "Marca", composition, 4, Color.WHITE, label);

        configBox.getChildren().add(colorGrid);
    }

    private void addColorControl(GridPane grid, int row, int col, String label, NumberComposition comp, int layerIndex,
            Color defaultColor, String sectionLabel) {
        VBox cell = new VBox(2);
        cell.setMaxWidth(Double.MAX_VALUE);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("number-color-label");
        lbl.setWrapText(true);

        HBox controls = new HBox(5);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setMaxWidth(Double.MAX_VALUE);

        Color initial = comp.getLayerColor(layerIndex);
        if (initial == null) {
            initial = defaultColor;
            comp.setLayerColor(layerIndex, initial);
        }

        final String partName = "number_" + label.toLowerCase() + "_" + layerIndex;
        // Key for identifying this picker globally within this section
        final String pickerKey = sectionLabel + "_" + layerIndex;

        MenuButton pickerBtn = UIFactory.createColorMenuButton(initial, label + " Color",
                newColor -> {
                    if (newColor != null) {
                        applyNumberColor(comp, layerIndex, newColor, label, pickerKey, true);
                    }
                },
                (onCommit, onPreview) -> {
                    eyedropperCallback.accept(
                        color -> {
                            if (color == null) { visualizer.clearPreviewColors(); return; }
                            visualizer.clearPreviewColors();
                            applyNumberColor(comp, layerIndex, color, label, pickerKey, true);
                            onCommit.accept(color); 
                        },
                        color -> {
                            if (color == null) visualizer.clearPreviewColors();
                            else { 
                                visualizer.setPreviewColor(partName, color); 
                                comp.setLayerColor(layerIndex, color); 
                                handleSmartPreview(comp, layerIndex, color, sectionLabel);
                            }
                            onPreview.accept(color);
                        }
                    );
                });

        pickerButtons.put(comp.hashCode() + layerIndex, pickerBtn);
        pickerBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pickerBtn, Priority.ALWAYS);

        controls.getChildren().add(pickerBtn);

        cell.getChildren().addAll(lbl, controls);
        grid.add(cell, col, row);
        GridPane.setHgrow(cell, Priority.ALWAYS);
    }

    private void handleSmartPreview(NumberComposition comp, int changedIndex, Color newColor, String sectionLabel) {
        if (changedIndex == 0) {
            Color smartCombo = org.example.utils.ColorUtils.getSmartCombination(newColor);
            comp.setLayerColor(1, smartCombo);
            visualizer.setPreviewColor("number_combinación_1", smartCombo);
        }
        if (changedIndex == 2) {
            comp.setLayerColor(4, newColor);
            visualizer.setPreviewColor("number_marca_4", newColor);
        }
    }

    private void applyNumberColor(NumberComposition comp, int layerIndex, Color color, String label, String pickerKey, boolean triggerSmart) {
        Color startColor = comp.getLayerColor(layerIndex);
        if (color != null && !color.equals(startColor)) {
            comp.setLayerColor(layerIndex, color);
            
            // Sync Picker Button Color
            MenuButton btn = pickerButtons.get(comp.hashCode() + layerIndex);
            if (btn != null) UIFactory.setColorMenuButtonColor(btn, color);

            // Record history
            visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                    label + " Color", startColor, color,
                    c -> { 
                        comp.setLayerColor(layerIndex, c);
                        MenuButton b = pickerButtons.get(comp.hashCode() + layerIndex);
                        if (b != null) UIFactory.setColorMenuButtonColor(b, c);
                        // We might need to refresh dependents on undo too
                        if (triggerSmart) handleSmartUpdates(comp, layerIndex, c);
                    }));

            // TRIGGER SMART UPDATES
            if (triggerSmart) handleSmartUpdates(comp, layerIndex, color);
        }
    }

    private void handleSmartUpdates(NumberComposition comp, int changedIndex, Color newColor) {
        if (changedIndex == 0) {
            // Smart Combo adaptation based on Base color (index 0 -> target index 1)
            Color smartCombo = org.example.utils.ColorUtils.getSmartCombination(newColor);
            updateDependentColor(comp, 1, smartCombo, "Smart Combo");
        }
        if (changedIndex == 2) {
            // Contorno affects Marca (index 2 -> target index 4)
            updateDependentColor(comp, 4, newColor, "Smart Brand");
        }
    }

    private void updateDependentColor(NumberComposition comp, int targetIndex, Color newColor, String reason) {
        Color oldColor = comp.getLayerColor(targetIndex);
        if (oldColor == null || !oldColor.equals(newColor)) {
            // Apply color to composition
            comp.setLayerColor(targetIndex, newColor);
            
            // Sync Picker Button Color immediately without full updateUI
            MenuButton btn = pickerButtons.get(comp.hashCode() + targetIndex);
            if (btn != null) {
                UIFactory.setColorMenuButtonColor(btn, newColor);
            }

            // Record history (Independent logic: undoing a smart change should restore old color)
            visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                    reason, oldColor != null ? oldColor : Color.TRANSPARENT, newColor,
                    c -> { 
                        comp.setLayerColor(targetIndex, c); 
                        MenuButton b = pickerButtons.get(comp.hashCode() + targetIndex);
                        if (b != null) UIFactory.setColorMenuButtonColor(b, c);
                    }));
        }
    }
}
