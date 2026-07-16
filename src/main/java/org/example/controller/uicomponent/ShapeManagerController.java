package org.example.controller.uicomponent;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.example.component.ImageEditorDialog;
import org.example.component.ImageLayer;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.model.ShapeType;
import org.example.utils.UIFactory;
import org.example.controller.uicomponent.helper.ShapeManagerActionHandler;
import org.example.controller.uicomponent.helper.ShapeManagerUIOrchestrator;
import org.example.controller.uicomponent.helper.ShapeManagerSyncHelper;
import org.example.controller.uicomponent.helper.ShapeButtonFactory;
import java.util.function.Consumer;

public class ShapeManagerController implements org.example.component.helper.DrawingToolContext {

    private final PrendaVisualizer visualizer;
    private final ShapeManagerActionHandler actionHandler;
    private final ShapeManagerUIOrchestrator uiOrchestrator;
    private final ShapeManagerSyncHelper syncHelper;
    private final ShapeButtonFactory buttonFactory;

    // UI Controls
    private MenuButton btnTargetScope;
    private MenuButton fillPicker;
    private MenuButton strokePicker;
    private Slider strokeWidthSlider;
    private Label strokeWidthLabel;
    private ToggleButton btnBrush;
    private ToggleButton btnEraser;
    private Button btnImageEditor;
    private ToggleButton btnPencil;
    private Button btnTransform;
    private Button btnStroke;
    private Button btnContour;
    private Button btnTrans;
    private Button btnDelete;
    private Button btnLock;
    private Button btnFinish;
    private ColorPicker systemColorPicker;
    private ToggleButton btnNodeEdit;
    private ToggleButton btnSelect;
    private ToggleButton btnCurrentShape;
    private Button btnWeld;
    private Button btnUnweld;
    private Button btnCut;

    // State
    private org.example.component.GraphicLayer activeGraphicLayer = null;
    private boolean isUpdatingUI = false;
    private ShapeType currentShapeType = ShapeType.RECTANGLE;
    private final javafx.beans.property.DoubleProperty brushSize = new javafx.beans.property.SimpleDoubleProperty(10);
    private final javafx.beans.property.DoubleProperty eraserSize = new javafx.beans.property.SimpleDoubleProperty(10);

    public ShapeManagerController(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
        this.uiOrchestrator = new ShapeManagerUIOrchestrator(visualizer, this);
        this.buttonFactory = new ShapeButtonFactory(uiOrchestrator);
        this.actionHandler = new ShapeManagerActionHandler(visualizer, this);
        this.syncHelper = new ShapeManagerSyncHelper(visualizer, this);
        
        initSelectionListener();
        visualizer.setShapeManagerController(this);
        visualizer.setDrawingToolContext(this);
    }

    // --- HELPERS ---
    public ShapeManagerActionHandler getActionHandler() { return actionHandler; }
    public ShapeManagerUIOrchestrator getUiOrchestrator() { return uiOrchestrator; }
    public ShapeManagerSyncHelper getSyncHelper() { return syncHelper; }

    // --- GETTERS & SETTERS ---
    public org.example.component.GraphicLayer getActiveGraphicLayer() { return activeGraphicLayer; }
    public ShapeLayer getActiveShapeLayer() { 
        return (activeGraphicLayer instanceof ShapeLayer) ? (ShapeLayer) activeGraphicLayer : null; 
    }
    public void setActiveShapeLayer(ShapeLayer layer) { this.activeGraphicLayer = layer; }
    public void setActiveGraphicLayer(org.example.component.GraphicLayer layer) { this.activeGraphicLayer = layer; }
    public boolean isUpdatingUI() { return isUpdatingUI; }
    public void setIsUpdatingUI(boolean val) { this.isUpdatingUI = val; }

    public Node getRightToolbar() { return uiOrchestrator.buildRightToolbar(); }
    
    public Node getAngleBox() { 
        return uiOrchestrator.getAngleBox(); 
    }
    
    public MenuButton getTargetScopeButton() {
        if (btnTargetScope == null) {
            btnTargetScope = new MenuButton();
            btnTargetScope.setGraphic(UIFactory.crearIcono("mdi2v-view-grid-plus", 20, "#2c3e50"));
            btnTargetScope.setTooltip(new Tooltip("Seleccionar Contenedor Destino"));
            btnTargetScope.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-mark-color: transparent;");
            btnTargetScope.setUserData("GLOBAL");
            btnTargetScope.setOnShowing(e -> refreshScopeItems());
        }
        return btnTargetScope;
    }

    private void refreshScopeItems() {
        btnTargetScope.getItems().clear();
        createScopeMenuItem("Lienzo (Global)", "mdi2v-view-dashboard", "GLOBAL");
        if (visualizer.hasShirt()) {
            createScopeMenuItem("Pecho", "mdi2t-tshirt-crew", "PECHO");
            createScopeMenuItem("Espalda", "mdi2t-tshirt-crew-outline", "ESPALDA");
            createScopeMenuItem("Manga Delantera", "mdi2a-arm-flex", "MANGA_DELANTERA");
            createScopeMenuItem("Manga Trasera", "mdi2a-arm-flex-outline", "MANGA_TRASERA");
        }
        if (visualizer.hasShorts()) {
            createScopeMenuItem("Short Frente", "mdi2f-format-vertical-align-bottom", "SHORT_FRONT");
            createScopeMenuItem("Short Atrás", "mdi2f-format-vertical-align-top", "SHORT_BACK");
        }
    }

    private void createScopeMenuItem(String text, String icon, String dataId) {
        MenuItem item = new MenuItem(text);
        item.setGraphic(UIFactory.crearIcono(icon, 16, "#555"));
        item.setOnAction(e -> {
            btnTargetScope.setUserData(dataId);
            btnTargetScope.setGraphic(UIFactory.crearIcono(icon, 20, "#2980b9"));
            btnTargetScope.setTooltip(new Tooltip("Insertar en: " + text));
        });
        btnTargetScope.getItems().add(item);
    }

    public MenuButton getFillPicker() {
        if (fillPicker == null) {
            fillPicker = buttonFactory.createColorMenuButton("mdi2f-format-color-fill", Color.web("#E31B23"), "Color de Relleno",
                c -> { if (!isUpdatingUI) actionHandler.recordPropertyChange("Fill Color", ShapeLayer::getFillColor, ShapeLayer::setFillColor, c); },
                c -> buttonFactory.updatePickerGraphic(fillPicker, c),
                () -> activateEyedropper(c -> { buttonFactory.updatePickerGraphic(fillPicker, c); actionHandler.recordPropertyChange("Fill Color", ShapeLayer::getFillColor, ShapeLayer::setFillColor, c); }),
                () -> UIFactory.showColorSelector(fillPicker.getScene().getWindow(), (Color)fillPicker.getUserData(),
                    c -> { buttonFactory.updatePickerGraphic(fillPicker, c); actionHandler.recordPropertyChange("Fill Color", ShapeLayer::getFillColor, ShapeLayer::setFillColor, c); },
                    c -> { buttonFactory.updatePickerGraphic(fillPicker, c); actionHandler.recordPropertyChange("Fill Color", ShapeLayer::getFillColor, ShapeLayer::setFillColor, c); }));
            fillPicker.setOnContextMenuRequested(e -> {
                if (!isUpdatingUI) {
                    actionHandler.recordPropertyChange("Set No Fill", ShapeLayer::getFillColor, ShapeLayer::setFillColor, Color.TRANSPARENT);
                    buttonFactory.updatePickerGraphic(fillPicker, Color.TRANSPARENT);
                }
            });
        }
        return fillPicker;
    }

    public MenuButton getStrokePicker() {
        if (strokePicker == null) {
            strokePicker = buttonFactory.createColorMenuButton("mdi2b-border-color", Color.BLACK, "Color de Borde",
                c -> { if (!isUpdatingUI) actionHandler.recordPropertyChange("Stroke Color", ShapeLayer::getStrokeColor, ShapeLayer::setStrokeColor, c); },
                c -> buttonFactory.updatePickerGraphic(strokePicker, c),
                () -> activateEyedropper(c -> { buttonFactory.updatePickerGraphic(strokePicker, c); actionHandler.recordPropertyChange("Stroke Color", ShapeLayer::getStrokeColor, ShapeLayer::setStrokeColor, c); }),
                () -> UIFactory.showColorSelector(strokePicker.getScene().getWindow(), (Color)strokePicker.getUserData(),
                    c -> { buttonFactory.updatePickerGraphic(strokePicker, c); actionHandler.recordPropertyChange("Stroke Color", ShapeLayer::getStrokeColor, ShapeLayer::setStrokeColor, c); },
                    c -> { buttonFactory.updatePickerGraphic(strokePicker, c); actionHandler.recordPropertyChange("Stroke Color", ShapeLayer::getStrokeColor, ShapeLayer::setStrokeColor, c); }));
            strokePicker.setOnContextMenuRequested(e -> {
                if (!isUpdatingUI) {
                    actionHandler.recordPropertyChange("Remove Stroke", ShapeLayer::getStrokeColor, ShapeLayer::setStrokeColor, Color.TRANSPARENT);
                    buttonFactory.updatePickerGraphic(strokePicker, Color.TRANSPARENT);
                }
            });
        }
        return strokePicker;
    }

    public Label getStrokeWidthLabel() {
        if (strokeWidthLabel == null) strokeWidthLabel = new Label("2px");
        return strokeWidthLabel;
    }

    public Slider getStrokeWidthSlider() {
        if (strokeWidthSlider == null) {
            strokeWidthSlider = new Slider(0, 20, 2);
            strokeWidthSlider.setOrientation(javafx.geometry.Orientation.VERTICAL);
            strokeWidthSlider.setPrefHeight(100);
            strokeWidthSlider.valueProperty().addListener((obs, old, val) -> {
                if (strokeWidthLabel != null) strokeWidthLabel.setText(String.format("%.0fpx", val.doubleValue()));
                if (!isUpdatingUI) actionHandler.recordPropertyChange("Stroke Width", ShapeLayer::getStrokeWidth, ShapeLayer::setStrokeWidth, val.doubleValue());
            });
        }
        return strokeWidthSlider;
    }

    public ToggleButton getSelectButton() {
        if (btnSelect == null) {
            btnSelect = buttonFactory.createVerticalToolButton("mdi2c-cursor-default", "Seleccionar");
            btnSelect.setOnAction(e -> {
                visualizer.cancelShapeCreation();
                visualizer.getShapeHelper().exitNodeEditMode();
            });
        }
        return btnSelect;
    }

    public ToggleButton getCurrentShapeButton() {
        if (btnCurrentShape == null) {
            btnCurrentShape = buttonFactory.createVerticalToolButton(buttonFactory.getIconForShape(currentShapeType), "Herramienta de Formas (Click derecho para cambiar)");
            ContextMenu shapeMenu = new ContextMenu();
            for (ShapeType type : ShapeType.values()) {
                if (type == ShapeType.CUSTOM_PATH) continue;
                MenuItem item = new MenuItem(type.getDisplayName());
                item.setGraphic(UIFactory.crearIcono(buttonFactory.getIconForShape(type), 16, "#555"));
                item.setOnAction(ev -> {
                    currentShapeType = type;
                    updateCurrentShapeButton();
                    if (btnCurrentShape.isSelected()) activateCurrentShape();
                    else btnCurrentShape.setSelected(true);
                });
                shapeMenu.getItems().add(item);
            }
            btnCurrentShape.setOnContextMenuRequested(e -> shapeMenu.show(btnCurrentShape, e.getScreenX(), e.getScreenY()));
            btnCurrentShape.setOnAction(e -> {
                visualizer.getShapeHelper().exitNodeEditMode();
                if (btnCurrentShape.isSelected()) activateCurrentShape();
                else { visualizer.cancelShapeCreation(); visualizer.setCursor(javafx.scene.Cursor.DEFAULT); }
            });
        }
        return btnCurrentShape;
    }

    public ToggleButton getPencilButton() {
        if (btnPencil == null) {
            btnPencil = buttonFactory.createVerticalToolButton("mdi2p-pen", "Lápiz (Dibujo Libre / Bezier)");
            btnPencil.setUserData(ShapeType.CUSTOM_PATH);
            btnPencil.setOnAction(e -> {
                visualizer.getShapeHelper().exitNodeEditMode();
                if (btnPencil.isSelected()) {
                    visualizer.startShapeCreationInternal(ShapeType.CUSTOM_PATH, getFillColor(), getStrokeColor(), getStrokeWidthSlider().getValue(), (layer) -> { if (layer != null) syncHelper.syncUIWithSelection(layer); });
                    visualizer.getContentGroup().setCursor(javafx.scene.Cursor.CROSSHAIR);
                } else { visualizer.cancelShapeCreation(); visualizer.setCursor(javafx.scene.Cursor.DEFAULT); }
            });
        }
        return btnPencil;
    }

    public ToggleButton getBrushButton() {
        if (btnBrush == null) {
            btnBrush = new ToggleButton();
            buttonFactory.configureAsToolButton(btnBrush, "mdi2b-brush", "Pincel (Pintar encima)", "TOOL_BRUSH");
            btnBrush.setContextMenu(buttonFactory.createBrushSizeMenu(brushSize, "Tamaño Pincel (px):"));
            btnBrush.setOnAction(e -> {
                visualizer.cancelShapeCreation();
                visualizer.getShapeHelper().exitNodeEditMode();
                if (btnBrush.isSelected()) {
                    visualizer.setCursor(javafx.scene.Cursor.CROSSHAIR);
                } else {
                    visualizer.setCursor(javafx.scene.Cursor.DEFAULT);
                }
            });
        }
        return btnBrush;
    }

    public Button getImageEditorButton() {
        if (btnImageEditor == null) {
            btnImageEditor = new Button();
            btnImageEditor.setGraphic(UIFactory.crearIcono("mdi2i-image-filter-hdr", 20, "#2c3e50"));
            btnImageEditor.setTooltip(new Tooltip("Editar imagen (Pintar / Borrar)"));
            btnImageEditor.setMinSize(28, 28);
            btnImageEditor.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4;");
            btnImageEditor.setOnAction(e -> {
                Node sel = visualizer.getSelectedNode();
                if (sel instanceof ImageLayer) {
                    ImageLayer il = (ImageLayer) sel;
                    ImageEditorDialog editor = new ImageEditorDialog(il.getImage());
                    Stage owner = (Stage) visualizer.getScene().getWindow();
                    Image result = editor.showAndWait(owner);
                    if (result != null) il.setImage(result);
                }
            });
            visualizer.addSelectionListener(n -> {
                btnImageEditor.setVisible(n instanceof ImageLayer);
                btnImageEditor.setManaged(n instanceof ImageLayer);
            });
            btnImageEditor.setVisible(false);
            btnImageEditor.setManaged(false);
        }
        return btnImageEditor;
    }

    public ToggleButton getEraserButton() {
        if (btnEraser == null) {
            btnEraser = new ToggleButton();
            buttonFactory.configureAsToolButton(btnEraser, "mdi2e-eraser", "Borrador (Ocultar)", "TOOL_ERASER");
            btnEraser.setContextMenu(buttonFactory.createBrushSizeMenu(eraserSize, "Tamaño Borrador (px):"));
            btnEraser.setOnAction(e -> {
                visualizer.cancelShapeCreation();
                visualizer.getShapeHelper().exitNodeEditMode();
                if (btnEraser.isSelected()) {
                    visualizer.setCursor(javafx.scene.Cursor.CROSSHAIR);
                } else {
                    visualizer.setCursor(javafx.scene.Cursor.DEFAULT);
                }
            });
        }
        return btnEraser;
    }


    public Button getTransformButton() {
        if (btnTransform == null) {
            btnTransform = new Button("", UIFactory.crearIcono("mdi2r-resize", 20, "#555"));
            btnTransform.setTooltip(new Tooltip("Transformar (Tamaño/Rotación)"));
            buttonFactory.styleToolButton(btnTransform);
            btnTransform.setOnAction(e -> uiOrchestrator.showTransformPopup(btnTransform));
            visualizer.addSelectionListener(n -> updateTransformButtonState());
        }
        return btnTransform;
    }

    public void updateTransformButtonState() {
        if (btnTransform == null) return;
        ShapeLayer layer = getActiveShapeLayer();
        boolean active = layer != null && (Math.abs(layer.getRotate()) > 0.1 || Math.abs(layer.getScaleX() - 1.0) > 0.01);
        btnTransform.setGraphic(UIFactory.crearIcono("mdi2r-resize", 20, active ? "#3498db" : "#555"));
        btnTransform.setStyle(active ? "-fx-background-color: #d6eaf8; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 4; -fx-background-insets: 0;" : "-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 4; -fx-background-insets: 0;");
    }

    public Button getStrokeButton() {
        if (btnStroke == null) {
            btnStroke = new Button("", UIFactory.crearIcono("mdi2f-format-line-weight", 20, "#555"));
            btnStroke.setTooltip(new Tooltip("Grosor de Borde"));
            buttonFactory.styleToolButton(btnStroke);
            btnStroke.setOnAction(e -> {
                uiOrchestrator.showStrokePopup(btnStroke);
                btnStroke.setGraphic(UIFactory.crearIcono("mdi2f-format-line-weight", 20, "#3498db"));
                btnStroke.setStyle("-fx-background-color: #d6eaf8; -fx-background-radius: 4; -fx-padding: 4;");
            });
        }
        return btnStroke;
    }

    public Button getContourButton() {
        if (btnContour == null) {
            btnContour = new Button("", UIFactory.crearIcono("mdi2b-border-outside", 20, "#555"));
            btnContour.setTooltip(new Tooltip("Contorno / Silueta"));
            buttonFactory.styleToolButton(btnContour);
            btnContour.setOnAction(e -> uiOrchestrator.showContourPopup(btnContour));
        }
        return btnContour;
    }

    public Button getTransButton() {
        if (btnTrans == null) {
            btnTrans = new Button("", UIFactory.crearIcono("mdi2g-gradient", 20, "#555"));
            btnTrans.setTooltip(new Tooltip("Transparencia y Degradado"));
            buttonFactory.styleToolButton(btnTrans);
            btnTrans.setOnAction(e -> uiOrchestrator.showTransPopup(btnTrans));
        }
        return btnTrans;
    }

    public Button getDeleteButton() {
        if (btnDelete == null) {
            btnDelete = new Button("", UIFactory.crearIcono("mdi2d-delete", 20, "#c0392b"));
            btnDelete.setTooltip(new Tooltip("Eliminar"));
            buttonFactory.styleToolButton(btnDelete); // Ensure it also has the 32x32 size
            btnDelete.setOnAction(e -> actionHandler.deleteSelection());
        }
        return btnDelete;
    }

    public Button getWeldButton() {
        if (btnWeld == null) {
            btnWeld = new Button("", UIFactory.crearIcono("mdi2l-link", 20, "#555"));
            btnWeld.setTooltip(new Tooltip("Soldar Vectores (Unir)"));
            buttonFactory.styleToolButton(btnWeld);
            btnWeld.setOnAction(e -> actionHandler.weldSelectedShapes());
            visualizer.addSelectionListener(n -> updateWeldButtonState());
            updateWeldButtonState();
        }
        return btnWeld;
    }

    public Button getUnweldButton() {
        if (btnUnweld == null) {
            btnUnweld = new Button("", UIFactory.crearIcono("mdi2l-link-off", 20, "#555"));
            btnUnweld.setTooltip(new Tooltip("Desoldar Vectores (Separar)"));
            buttonFactory.styleToolButton(btnUnweld);
            btnUnweld.setOnAction(e -> actionHandler.unweldSelectedShape());
            visualizer.addSelectionListener(n -> updateWeldButtonState());
        }
        return btnUnweld;
    }

    public Button getCutButton() {
        if (btnCut == null) {
            btnCut = new Button("", UIFactory.crearIcono("mdi2c-content-cut", 20, "#555"));
            btnCut.setTooltip(new Tooltip("Cortar Vectores (Restar)"));
            buttonFactory.styleToolButton(btnCut);
            btnCut.setOnAction(e -> actionHandler.cutSelectedShapes());
            visualizer.addSelectionListener(n -> updateWeldButtonState());
            updateWeldButtonState();
        }
        return btnCut;
    }

    private void updateWeldButtonState() {
        if (btnWeld == null || btnUnweld == null) return;
        boolean hasShape = visualizer.getLayerManager().getSelectedNodes().stream()
            .anyMatch(n -> n instanceof ShapeLayer || (n instanceof org.example.component.GraphicLayer && ((org.example.component.GraphicLayer)n).getNode() instanceof ShapeLayer));
        btnWeld.setDisable(!hasShape);
        btnWeld.setOpacity(hasShape ? 1.0 : 0.5);
        btnUnweld.setDisable(!hasShape);
        btnUnweld.setOpacity(hasShape ? 1.0 : 0.5);
        if (btnCut != null) {
            btnCut.setDisable(!hasShape);
            btnCut.setOpacity(hasShape ? 1.0 : 0.5);
        }
    }

    public Button getLockButton() {
        if (btnLock == null) {
            btnLock = new Button();
            btnLock.setTooltip(new Tooltip("Bloquear Posición (Ctrl + B)"));
            buttonFactory.styleToolButton(btnLock);
            btnLock.setOnAction(e -> actionHandler.toggleLock());
            visualizer.addSelectionListener(n -> updateLockIcon());
            updateLockIcon();
        }
        return btnLock;
    }

    public void updateLockIcon() {
        if (btnLock == null) return;
        boolean isLocked = false;
        Node selected = visualizer.getSelectedNode();
        
        if (selected == null && activeGraphicLayer != null) {
            selected = activeGraphicLayer.getNode();
        }
        
        if (selected instanceof org.example.component.GraphicLayer) {
            isLocked = ((org.example.component.GraphicLayer) selected).isUserLocked();
        } else if (selected instanceof org.example.component.GroupLayerV2) {
            isLocked = ((org.example.component.GroupLayerV2) selected).isUserLocked();
        } else if (selected instanceof org.example.component.GroupLayer) {
            isLocked = ((org.example.component.GroupLayer) selected).isUserLocked();
        }

        btnLock.setGraphic(UIFactory.crearIcono(isLocked ? "mdi2l-lock" : "mdi2l-lock-open-variant", 20, isLocked ? "#e74c3c" : "#555"));
        btnLock.setStyle(isLocked ? "-fx-background-color: #fadbd8; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 4;" : "-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
    }

    public Button getFinishButton() {
        if (btnFinish == null) {
            btnFinish = new Button("LISTO");
            btnFinish.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 8; -fx-cursor: hand;");
            btnFinish.setMaxWidth(Double.MAX_VALUE);
            btnFinish.setVisible(false);
            btnFinish.setManaged(false);
            btnFinish.setOnAction(e -> visualizer.finishEditMode());
        }
        return btnFinish;
    }

    public void setFinishButtonVisible(boolean visible) {
        if (btnFinish != null) {
            btnFinish.setVisible(visible);
            btnFinish.setManaged(visible);
        }
    }

    public ColorPicker getSystemColorPicker() {
        if (systemColorPicker == null) {
            systemColorPicker = new ColorPicker();
            systemColorPicker.setManaged(false);
            systemColorPicker.setOpacity(0);
            systemColorPicker.setPrefSize(1, 1);
        }
        return systemColorPicker;
    }

    private void initSelectionListener() {
        visualizer.addSelectionListener(layer -> {
            if (layer instanceof org.example.component.GraphicLayer) {
                syncHelper.syncUIWithSelection((org.example.component.GraphicLayer) layer);
            } else if (layer instanceof org.example.component.GroupLayer || layer instanceof org.example.component.GroupLayerV2) {
                syncHelper.syncUIWithSelection(null);
            } else {
                activeGraphicLayer = null;
                uiOrchestrator.closePopup();
            }
        });
    }

    private void updateCurrentShapeButton() {
        buttonFactory.updateCurrentShapeButton(btnCurrentShape, currentShapeType);
    }

    public boolean isEraserActive() { return btnEraser != null && btnEraser.isSelected(); }
    public double getEraserSize() { return eraserSize.get(); }
    public void setEraserSize(double s) { eraserSize.set(s); }
    public boolean isBrushActive() { return btnBrush != null && btnBrush.isSelected(); }
    public double getBrushSize() { return brushSize.get(); }
    public void setBrushSize(double s) { brushSize.set(s); }

    public void updateAngleUI(double angle) {
        if (uiOrchestrator != null && uiOrchestrator.getTxtToolbarAngle() != null) {
            uiOrchestrator.getTxtToolbarAngle().setText(String.format("%.1f°", angle));
        }
    }

    public void deselectAllShapeTools() {
        ToggleGroup tg = buttonFactory.getOrCreateToolsGroup();
        if (tg.getSelectedToggle() != null) {
            tg.getSelectedToggle().setSelected(false);
        }
        visualizer.cancelShapeCreation();
        visualizer.setCursor(javafx.scene.Cursor.DEFAULT);
    }
    
    public void hideFloatingToolbar() {
        // Delegate or implementation
    }
    
    public javafx.scene.control.ToggleGroup getToolsGroup() {
        return buttonFactory.getOrCreateToolsGroup();
    }
    
    public javafx.scene.control.ToggleButton getNodeEditButton() {
        if (btnNodeEdit == null) {
            btnNodeEdit = buttonFactory.createVerticalToolButton("mdi2v-vector-point", "Editar Nodos / Vectores (N)");
            btnNodeEdit.setOnAction(e -> {
                if (btnNodeEdit.isSelected()) {
                    // Enter node edit for currently active shape
                    ShapeLayer active = getActiveShapeLayer();
                    if (active != null) {
                        visualizer.getShapeHelper().enterNodeEditMode(active);
                    } else {
                        // Nothing to edit — deselect immediately
                        btnNodeEdit.setSelected(false);
                    }
                } else {
                    visualizer.getShapeHelper().exitNodeEditMode();
                }
            });
            // Auto-enter node edit when a shape is selected while the button is active
            visualizer.addSelectionListener(layer -> {
                if (btnNodeEdit != null && btnNodeEdit.isSelected() && layer instanceof ShapeLayer) {
                    visualizer.getShapeHelper().enterNodeEditMode((ShapeLayer) layer);
                }
            });
            // Auto-deselect button when node edit mode exits programmatically
            visualizer.getShapeHelper().nodeEditingProperty().addListener((obs, old, val) -> {
                if (!val && btnNodeEdit != null && btnNodeEdit.isSelected()) {
                    btnNodeEdit.setSelected(false);
                }
            });
        }
        return btnNodeEdit;
    }

    public void updateToolSelection(ShapeType type) {
        this.currentShapeType = type;
        updateCurrentShapeButton();
    }

    private void activateCurrentShape() {
        String targetZone = (String) getTargetScopeButton().getUserData();
        visualizer.startShapeCreationInternal(currentShapeType, getFillColor(), getStrokeColor(), getStrokeWidthSlider().getValue(), (layer) -> {
            if (layer != null) {
                syncHelper.syncUIWithSelection(layer);
                if (targetZone != null && !targetZone.equals("GLOBAL")) visualizer.getShapeHelper().applySmartPowerClip(layer, targetZone, true);
            }
        });
    }

    public Color getFillColor() { return (fillPicker != null && fillPicker.getUserData() instanceof Color) ? (Color) fillPicker.getUserData() : Color.web("#E31B23"); }
    public Color getStrokeColor() { return (strokePicker != null && strokePicker.getUserData() instanceof Color) ? (Color) strokePicker.getUserData() : Color.BLACK; }



    public void activateEyedropper(Consumer<Color> onColorPicked) {
        uiOrchestrator.activateEyedropper(onColorPicked, null);
    }
    
    public void activateEyedropper(Consumer<Color> onColorPicked, Consumer<Color> onHover) {
        uiOrchestrator.activateEyedropper(onColorPicked, onHover);
    }

    public void setContourUI(boolean active, int steps, double dist) {}

    public void setTransparencyUI(boolean active, double start, double end, double angle, double bal) {}

    public ShapeButtonFactory getButtonFactory() { return buttonFactory; }
}
