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
import java.util.concurrent.atomic.AtomicBoolean;

public class NumberController {

    private final PrendaVisualizer visualizer;
    private final BiConsumer<Consumer<Color>, Consumer<Color>> eyedropperCallback;
    private final java.util.Map<Integer, MenuButton> pickerButtons = new java.util.HashMap<>();
    private VBox mainContainer;
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private boolean hasInitializedVectors = false;

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
        if (mainContainer == null || !isUpdating.compareAndSet(false, true)) return;
        try {
            pickerButtons.values().forEach(btn -> btn.getItems().clear());
            pickerButtons.clear();
            mainContainer.getChildren().clear();

            VBox content = new VBox(20); // More spacing between sections
            content.setPadding(new Insets(10, 10, 15, 10));
            content.getStyleClass().add("number-content-box");
            content.setFillWidth(true);

            mainContainer.getChildren().add(content);
            VBox.setVgrow(content, Priority.ALWAYS);

            Label lblNumbers = new Label("Numeración (Vectores):");
            lblNumbers.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            
            // --- GLOBAL DIGIT SELECTOR ---
            HBox globalDigitBox = new HBox(10);
            globalDigitBox.setAlignment(Pos.CENTER_LEFT);
            Label lblDigit = new Label("Dígito Global:");
            lblDigit.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
            
            ComboBox<String> cmbNumber = new ComboBox<>();
            cmbNumber.getItems().addAll("9", "1");
            
            String initialValue = visualizer.getCurrentChestNumberStr();
            if (initialValue == null || initialValue.isEmpty()) {
                initialValue = "9";
            }
            cmbNumber.setValue(initialValue);
            
            if (!hasInitializedVectors) {
                visualizer.setGlobalNumberDigit(initialValue); // INITIALIZE VECTOR STATE ONLY ONCE
                hasInitializedVectors = true;
            }
            
            UIFactory.fixComboBoxReadability(cmbNumber);
            cmbNumber.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cmbNumber, Priority.ALWAYS);

            cmbNumber.setOnAction(e -> {
                String newVal = cmbNumber.getValue();
                visualizer.setGlobalNumberDigit(newVal);
            });
            globalDigitBox.getChildren().addAll(lblDigit, cmbNumber);
            
            VBox headerBox = new VBox(10, lblNumbers, globalDigitBox);
            headerBox.setPadding(new Insets(0, 0, 5, 0));
            content.getChildren().add(headerBox);

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
        } finally { isUpdating.set(false); }
    }

    private void addNumberConfigSection(VBox parent, String label, NumberComposition composition) {
        VBox sectionBox = new VBox(15);
        // CARD STYLING
        sectionBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 15;");
        sectionBox.setMaxWidth(Double.MAX_VALUE);

        // Read visibility from the authoritative PrendaState, not the JavaFX node.
        // This fixes the bug where opening a project showed the switch OFF despite the saved state being ON.
        boolean isCurrentlyVisible;
        if (label.equalsIgnoreCase("Pecho")) isCurrentlyVisible = visualizer.isChestNumberVisible();
        else if (label.equalsIgnoreCase("Espalda")) isCurrentlyVisible = visualizer.isBackNumberVisible();
        else isCurrentlyVisible = visualizer.isShortNumberVisible();

        HBox hbActive = UIFactory.crearFilaOpcion("Número " + label, isCurrentlyVisible, visualizer.getGenero(),
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
        chkActive.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;"); // Emphasize header

        VBox configBox = new VBox(15);
        configBox.setMaxWidth(Double.MAX_VALUE);
        configBox.visibleProperty().bind(chkActive.selectedProperty());
        configBox.managedProperty().bind(chkActive.selectedProperty());

        addColorsToSection(configBox, composition, label);

        sectionBox.getChildren().addAll(hbActive, configBox);
        parent.getChildren().add(sectionBox);
    }

    private void addColorsToSection(VBox configBox, NumberComposition composition, String label) {
        boolean isSelected = composition.getLayerColor(4) != null && !composition.getLayerColor(4).equals(Color.TRANSPARENT);
        CheckBox chkIncludeBrand = new CheckBox("Incluir Marca");
        chkIncludeBrand.setSelected(isSelected);
        chkIncludeBrand.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: bold;");

        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(15);
        colorGrid.setVgap(15);
        colorGrid.setMaxWidth(Double.MAX_VALUE);
        
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50); col1.setFillWidth(true); col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50); col2.setFillWidth(true); col2.setHgrow(Priority.ALWAYS);
        colorGrid.getColumnConstraints().addAll(col1, col2);

        VBox cellBase = createColorControlCell("Base", composition, 0, Color.BLACK, label);
        VBox cellComb = createColorControlCell("Combinación", composition, 1, Color.web("#333333"), label);
        VBox cellCont = createColorControlCell("Contorno", composition, 2, Color.WHITE, label);
        VBox cellMarca = createColorControlCell("Color de Marca", composition, 4, Color.WHITE, label);

        // Replace the "Color de Marca" label (index 0) with the CheckBox to align perfectly!
        cellMarca.getChildren().set(0, chkIncludeBrand);
        
        // Hide the color MenuButton (index 1) when toggle is off
        javafx.scene.Node marcaColorBtn = cellMarca.getChildren().get(1);
        marcaColorBtn.visibleProperty().bind(chkIncludeBrand.selectedProperty());
        marcaColorBtn.managedProperty().bind(chkIncludeBrand.selectedProperty());

        chkIncludeBrand.setOnAction(e -> {
            boolean active = chkIncludeBrand.isSelected();
            composition.setLayerColor(4, active ? Color.WHITE : Color.TRANSPARENT);
            // Updating immediately to show the change in preview
            visualizer.setPreviewColor("number_marca_4", active ? Color.WHITE : Color.TRANSPARENT);
            saveNumberColorsToState(composition, label);
        });

        colorGrid.add(cellBase, 0, 0);
        colorGrid.add(cellComb, 1, 0);
        colorGrid.add(cellCont, 0, 1);
        colorGrid.add(cellMarca, 1, 1);

        GridPane.setHgrow(cellBase, Priority.ALWAYS);
        GridPane.setHgrow(cellComb, Priority.ALWAYS);
        GridPane.setHgrow(cellCont, Priority.ALWAYS);
        GridPane.setHgrow(cellMarca, Priority.ALWAYS);

        configBox.getChildren().add(colorGrid);
    }

    private VBox createColorControlCell(String label, NumberComposition comp, int layerIndex, Color defaultColor, String sectionLabel) {
        VBox cell = new VBox(5);
        cell.setMaxWidth(Double.MAX_VALUE);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        lbl.setWrapText(true);

        Color initial = comp.getLayerColor(layerIndex);
        if (initial == null) {
            initial = defaultColor;
            comp.setLayerColor(layerIndex, initial);
            saveNumberColorsToState(comp, sectionLabel);
        }

        final String partName = "number_" + label.toLowerCase() + "_" + layerIndex;
        final String pickerKey = sectionLabel + "_" + layerIndex;

        MenuButton pickerBtn = UIFactory.createColorMenuButton(initial, label,
                newColor -> {
                    if (newColor != null) {
                        applyNumberColor(comp, layerIndex, newColor, label, pickerKey, true, sectionLabel);
                    }
                },
                (onCommit, onPreview) -> {
                    eyedropperCallback.accept(
                        color -> {
                            if (color == null) { visualizer.clearPreviewColors(); return; }
                            visualizer.clearPreviewColors();
                            applyNumberColor(comp, layerIndex, color, label, pickerKey, true, sectionLabel);
                            onCommit.accept(color); 
                        },
                        color -> {
                            if (color == null) visualizer.clearPreviewColors();
                            else { 
                                visualizer.setPreviewColor(partName, color); 
                                comp.setLayerColor(layerIndex, color); 
                                handleSmartPreview(comp, layerIndex, color, sectionLabel);
                            }
                        }
                    );
                });

        pickerButtons.put(comp.hashCode() + layerIndex, pickerBtn);
        pickerBtn.setMaxWidth(Double.MAX_VALUE);

        cell.getChildren().addAll(lbl, pickerBtn);
        return cell;
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

    private void applyNumberColor(NumberComposition comp, int layerIndex, Color color, String label, String pickerKey, boolean triggerSmart, String sectionLabel) {
        Color startColor = comp.getLayerColor(layerIndex);
        if (color != null && !color.equals(startColor)) {
            comp.setLayerColor(layerIndex, color);
            saveNumberColorsToState(comp, sectionLabel);
            
            // Sync Picker Button Color
            MenuButton btn = pickerButtons.get(comp.hashCode() + layerIndex);
            if (btn != null) UIFactory.setColorMenuButtonColor(btn, color);

            // Record history
            visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                    label + " Color", startColor, color,
                    c -> { 
                        comp.setLayerColor(layerIndex, c);
                        saveNumberColorsToState(comp, sectionLabel);
                        MenuButton b = pickerButtons.get(comp.hashCode() + layerIndex);
                        if (b != null) UIFactory.setColorMenuButtonColor(b, c);
                        // We might need to refresh dependents on undo too
                        if (triggerSmart) handleSmartUpdates(comp, layerIndex, c, sectionLabel);
                    }));

            // TRIGGER SMART UPDATES
            if (triggerSmart) handleSmartUpdates(comp, layerIndex, color, sectionLabel);
        }
    }

    private void handleSmartUpdates(NumberComposition comp, int changedIndex, Color newColor, String sectionLabel) {
        if (changedIndex == 0) {
            // Smart Combo adaptation based on Base color (index 0 -> target index 1)
            Color smartCombo = org.example.utils.ColorUtils.getSmartCombination(newColor);
            updateDependentColor(comp, 1, smartCombo, "Smart Combo", sectionLabel);
        }
        if (changedIndex == 2) {
            // Contorno affects Marca (index 2 -> target index 4)
            updateDependentColor(comp, 4, newColor, "Smart Brand", sectionLabel);
        }
    }

    private void updateDependentColor(NumberComposition comp, int targetIndex, Color newColor, String reason, String sectionLabel) {
        Color oldColor = comp.getLayerColor(targetIndex);
        if (oldColor == null || !oldColor.equals(newColor)) {
            // Apply color to composition
            comp.setLayerColor(targetIndex, newColor);
            saveNumberColorsToState(comp, sectionLabel);
            
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
                        saveNumberColorsToState(comp, sectionLabel);
                        MenuButton b = pickerButtons.get(comp.hashCode() + targetIndex);
                        if (b != null) UIFactory.setColorMenuButtonColor(b, c);
                    }));
        }
    }

    private void saveNumberColorsToState(NumberComposition comp, String sectionLabel) {
        String loc = "short";
        if (sectionLabel.equalsIgnoreCase("pecho")) {
            loc = "chest";
        } else if (sectionLabel.equalsIgnoreCase("espalda")) {
            loc = "back";
        }
        visualizer.getNumberManager().saveNumberColorsToState(visualizer.getState(), loc, comp);
    }
}
