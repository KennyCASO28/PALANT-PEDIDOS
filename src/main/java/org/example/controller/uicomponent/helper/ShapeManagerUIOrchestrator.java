package org.example.controller.uicomponent.helper;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Popup;
import javafx.util.Duration;

import org.example.component.GroupLayerV2;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.controller.uicomponent.ShapeManagerController;
import org.example.controller.uicomponent.ColorPalettePopup;
import org.example.model.ShapeType;
import org.example.utils.UIFactory;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Orchestrates the UI construction for ShapeManager, including the toolbar and
 * property popups.
 */
public class ShapeManagerUIOrchestrator {

    private final PrendaVisualizer visualizer;
    private final ShapeManagerController controller;
    private Popup currentPopup;

    public ShapeManagerUIOrchestrator(PrendaVisualizer visualizer, ShapeManagerController controller) {
        this.visualizer = visualizer;
        this.controller = controller;
    }

    private TextField txtToolbarAngle;
    private HBox angleBox;
    
    public TextField getTxtToolbarAngle() {
        return txtToolbarAngle;
    }
    
    public Node getAngleBox() {
        if (angleBox == null) {
            initAngleControls();
        }
        return angleBox;
    }

    public VBox buildRightToolbar() {
        VBox toolbar = new VBox(8);
        toolbar.setPadding(new javafx.geometry.Insets(10, 5, 10, 5));
        toolbar.setAlignment(Pos.TOP_CENTER);
        toolbar.setStyle(
                "-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 0 0 0 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        toolbar.setMinWidth(50);
        toolbar.setPrefWidth(50);
        toolbar.setMaxWidth(50);

        toolbar.getChildren().add(controller.getSystemColorPicker());
        toolbar.getChildren().add(controller.getTargetScopeButton());
        toolbar.getChildren().add(new Separator());
        toolbar.getChildren().addAll(controller.getWeldButton(), controller.getUnweldButton(), controller.getCutButton(), controller.getCombineButton(), new Separator());

        toolbar.getChildren().addAll(controller.getFillPicker(), controller.getStrokePicker());

        toolbar.getChildren().addAll(new Separator(), controller.getStrokeWidthLabel(),
                controller.getStrokeWidthSlider(), new Separator());

        toolbar.getChildren().addAll(controller.getSelectButton(), new Separator());
        toolbar.getChildren().addAll(controller.getCurrentShapeButton(), controller.getPencilButton());
        toolbar.getChildren().add(new Separator());
        toolbar.getChildren().addAll(controller.getImageEditorButton(), new Separator());

        if (angleBox == null) {
            initAngleControls();
        }

        toolbar.getChildren().addAll(angleBox, controller.getTransformButton(), controller.getStrokeButton(),
                controller.getContourButton(), controller.getTransButton());
        toolbar.getChildren().add(new Separator());
        toolbar.getChildren().addAll(controller.getDeleteButton(), controller.getLockButton());
        // NOTE: "Listo" button is NOT in this toolbar.
        // It lives as a floating overlay at TOP_CENTER of PrendaVisualizer.

        return toolbar;
    }

    private void initAngleControls() {
        txtToolbarAngle = new TextField("0°");
        txtToolbarAngle.setPrefWidth(48);
        txtToolbarAngle.setMinHeight(32);
        txtToolbarAngle.setMaxHeight(32);
        txtToolbarAngle.setStyle("-fx-font-size: 12px; -fx-padding: 3; -fx-alignment: center; -fx-font-weight: bold;");
        txtToolbarAngle.setTooltip(new Tooltip("Ángulo de Rotación"));

        angleBox = new HBox(4, UIFactory.crearIcono("mdi2r-rotate-right", 20, "#555"), txtToolbarAngle);
        angleBox.setAlignment(Pos.CENTER);

        txtToolbarAngle.setOnAction(e -> {
            try {
                double val = Double.parseDouble(txtToolbarAngle.getText().replace("°", "").trim());
                if (!controller.isUpdatingUI()) {
                    // Start atomic operation for history
                    visualizer.getLayerManager().setPerformingHistoryAction(true);

                    for (Node node : visualizer.getLayerManager().getSelectedNodes()) {
                        double oldVal = (node instanceof org.example.component.GraphicLayer)
                                ? ((org.example.component.GraphicLayer) node).getInternalRotation()
                                : node.getRotate();

                        if (node instanceof org.example.component.GraphicLayer) {
                            ((org.example.component.GraphicLayer) node).setInternalRotation(val);
                        } else {
                            node.setRotate(val);
                        }

                        if (visualizer.getHistoryManager() != null) {
                            visualizer.getHistoryManager().addCommand(
                                    new org.example.pattern.TransformCommand(node,
                                            node.getTranslateX(), node.getTranslateY(), node.getScaleX(),
                                            node.getScaleY(), oldVal, null, null,
                                            node.getTranslateX(), node.getTranslateY(), node.getScaleX(),
                                            node.getScaleY(), val, null, null, null));
                        }
                        if (node instanceof GroupLayerV2)
                            ((GroupLayerV2) node).updateSelectionOverlay();

                        if (node instanceof org.example.component.GraphicLayer) {
                            ((org.example.component.GraphicLayer) node).updateVisuals();
                        }
                    }
                    visualizer.getLayerManager().setPerformingHistoryAction(false);
                    txtToolbarAngle.setText(String.format("%.1f°", val));
                }
            } catch (Exception ex) {
                // Ignore invalid input
            }
        });
    }

    public void showPopup(Node anchor, Node content, String title) {
        if (currentPopup != null) {
            currentPopup.hide();
        }
        currentPopup = new Popup();
        currentPopup.setAutoHide(true);

        VBox root = new VBox(5);
        root.setPadding(new javafx.geometry.Insets(10));
        root.setStyle(
                "-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0); -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-background-radius: 4;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #95a5a6;");
        root.getChildren().addAll(lblTitle, new Separator(), content);

        currentPopup.getContent().add(root);

        javafx.geometry.Point2D p = anchor.localToScreen(0, 0);
        currentPopup.show(anchor.getScene().getWindow());

        javafx.application.Platform.runLater(() -> {
            if (currentPopup.isShowing()) {
                double w = root.getWidth();
                currentPopup.setX(p.getX() - w - 10);
                currentPopup.setY(p.getY());
            }
        });
    }

    public void showTransformPopup(Node anchor) {
        VBox box = new VBox(10);
        box.setMinWidth(200);

        Node activeNode = null;
        if (visualizer.getLayerManager() != null && !visualizer.getLayerManager().getSelectedNodes().isEmpty()) {
            activeNode = visualizer.getLayerManager().getSelectedNodes().iterator().next();
        } else {
            ShapeLayer active = controller.getActiveShapeLayer();
            if (active != null) activeNode = active;
        }

        Label lblRot = new Label("Rotación:");
        Slider slRot = new Slider(-180, 180, (activeNode != null) ? activeNode.getRotate() : 0);
        TextField txtRot = createValueField(slRot, "°");

        final Double[] rotStart = { 0.0 };
        slRot.setOnMousePressed(e -> rotStart[0] = slRot.getValue());
        slRot.setOnMouseReleased(e -> {
            double newVal = slRot.getValue();
            if (!Objects.equals(rotStart[0], newVal)) {
                Double capturedStart = rotStart[0];
                controller.getActionHandler().recordNodePropertyChange("Rotate", layer -> capturedStart,
                        Node::setRotate, newVal);
            }
        });

        slRot.valueProperty().addListener((o, old, v) -> {
            if (!controller.isUpdatingUI()) {
                if (visualizer.getLayerManager() != null && !visualizer.getLayerManager().getSelectedNodes().isEmpty()) {
                    for (Node n : visualizer.getLayerManager().getSelectedNodes()) {
                        n.setRotate(v.doubleValue());
                    }
                } else {
                    ShapeLayer act = controller.getActiveShapeLayer();
                    if (act != null) act.setRotate(v.doubleValue());
                }
            }
        });

        Label lblScale = new Label("Escala:");
        Slider slScale = new Slider(0.1, 5.0, (activeNode != null) ? activeNode.getScaleX() : 1.0);
        TextField txtScale = createValueField(slScale, "x");
        txtScale.setPrefWidth(48);
        txtScale.setMinHeight(32);
        txtScale.setMaxHeight(32);
        txtScale.setStyle("-fx-font-size: 12px; -fx-padding: 3; -fx-alignment: center; -fx-font-weight: bold;");

        final Double[] scaleStart = { 0.0 };
        slScale.setOnMousePressed(e -> scaleStart[0] = slScale.getValue());
        slScale.setOnMouseReleased(e -> {
            double newVal = slScale.getValue();
            if (!Objects.equals(scaleStart[0], newVal)) {
                Double capturedStart = scaleStart[0];
                controller.getActionHandler().recordNodePropertyChange("Scale", layer -> capturedStart, (layer, val) -> {
                    layer.setScaleX(val);
                    layer.setScaleY(val);
                }, newVal);
            }
        });

        slScale.valueProperty().addListener((o, old, v) -> {
            if (!controller.isUpdatingUI()) {
                if (visualizer.getLayerManager() != null && !visualizer.getLayerManager().getSelectedNodes().isEmpty()) {
                    for (Node n : visualizer.getLayerManager().getSelectedNodes()) {
                        n.setScaleX(v.doubleValue());
                        n.setScaleY(v.doubleValue());
                    }
                } else {
                    ShapeLayer act = controller.getActiveShapeLayer();
                    if (act != null) {
                        act.setScaleX(v.doubleValue());
                        act.setScaleY(v.doubleValue());
                    }
                }
            }
        });

        // Mirror Button Box
        HBox mirrorBox = new HBox(10);
        mirrorBox.setAlignment(Pos.CENTER);

        Button btnFlipH = new Button("Espejo H", UIFactory.crearIcono("mdi2f-flip-horizontal", 18, "#2c3e50"));
        Button btnFlipV = new Button("Espejo V", UIFactory.crearIcono("mdi2f-flip-vertical", 18, "#2c3e50"));
        styleTransformActionButton(btnFlipH);
        styleTransformActionButton(btnFlipV);

        btnFlipH.setOnAction(e -> {
            for (Node node : visualizer.getLayerManager().getSelectedNodes()) {
                if (node instanceof GroupLayerV2) {
                    ((GroupLayerV2) node).flipHorizontal();
                } else if (node instanceof org.example.component.AbstractGraphicLayer) {
                    ((org.example.component.AbstractGraphicLayer) node).flipHorizontal();
                }
            }
        });
        btnFlipV.setOnAction(e -> {
            for (Node node : visualizer.getLayerManager().getSelectedNodes()) {
                if (node instanceof GroupLayerV2) {
                    ((GroupLayerV2) node).flipVertical();
                } else if (node instanceof org.example.component.AbstractGraphicLayer) {
                    ((org.example.component.AbstractGraphicLayer) node).flipVertical();
                }
            }
        });

        // New button: Duplicate to Opposite Side (preserves visual inclination, no angle inversion)
        Button btnDuplicateOpposite = new Button("Duplicar Lado Opuesto", UIFactory.crearIcono("mdi2c-content-copy", 18, "#2c3e50"));
        styleTransformActionButton(btnDuplicateOpposite);
        btnDuplicateOpposite.setStyle(btnDuplicateOpposite.getStyle() + "; -fx-base: #27ae60;");
        btnDuplicateOpposite.setOnAction(e -> {
            java.util.Set<Node> selection = visualizer.getLayerManager().getSelectedNodes();
            if (selection.isEmpty()) {
                ShapeLayer activeLayer = controller.getActiveShapeLayer();
                if (activeLayer != null) selection = java.util.Collections.<Node>singleton(activeLayer);
            }
            
            for (Node node : selection) {
                Node clone = null;
                if (visualizer.getClipboardService() != null) {
                    clone = visualizer.getClipboardService().cloneNode(node);
                }
                
                if (clone != null) {
                    double offsetX = node.getBoundsInParent().getWidth();
                    clone.setTranslateX(node.getTranslateX() + offsetX);
                    clone.setTranslateY(node.getTranslateY());
                    
                    if (node.getParent() instanceof javafx.scene.Group) {
                        visualizer.getUserLayerManager().addLayerToContainer(clone, (javafx.scene.Group)node.getParent(), false);
                    } else {
                        visualizer.getUserLayerManager().addLayer(clone);
                    }
                }
            }
        });
        mirrorBox.getChildren().addAll(btnFlipH, btnFlipV, btnDuplicateOpposite);

        box.getChildren().addAll(new HBox(10, lblRot, txtRot), slRot, new Separator(), new Label("Invertir:"),
                mirrorBox, new Separator(), new HBox(10, lblScale, txtScale), slScale);
        showPopup(anchor, box, "TRANSFORMAR");
    }

    public void showStrokePopup(Node anchor) {
        VBox box = new VBox(10);
        box.setMinWidth(200);

        ShapeLayer active = controller.getActiveShapeLayer();
        Label lblWidth = new Label("Grosor del Borde:");
        Slider slWidth = new Slider(0, 50, (active != null) ? active.getStrokeWidth() : 2);
        Label lblVal = new Label(String.format("%.1f px", slWidth.getValue()));

        final Double[] widthStart = { 0.0 };
        slWidth.setOnMousePressed(e -> widthStart[0] = slWidth.getValue());
        slWidth.setOnMouseReleased(e -> {
            double newVal = slWidth.getValue();
            if (!Objects.equals(widthStart[0], newVal)) {
                Double capturedStart = widthStart[0];
                controller.getActionHandler().recordPropertyChange("Stroke Width", layer -> capturedStart,
                        ShapeLayer::setStrokeWidth, newVal);
            }
        });

        slWidth.valueProperty().addListener((o, old, v) -> {
            lblVal.setText(String.format("%.1f px", v.doubleValue()));
            if (!controller.isUpdatingUI()) {
                controller.getActionHandler().applyToSelection(layer -> layer.setStrokeWidth(v.doubleValue()));
                controller.getStrokeWidthSlider().setValue(v.doubleValue());
            }
        });

        Label lblType = new Label("Alineación del Borde:");
        lblType.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #2c3e50;");

        ToggleButton btnInside = new ToggleButton("Interior");
        ToggleButton btnCentered = new ToggleButton("Compartido");
        ToggleButton btnOutside = new ToggleButton("Exterior");

        String TOGGLE_STYLE = "-fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;";
        String TOGGLE_SELECTED = "-fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-background-color: #d6eaf8; -fx-border-color: #3498db; -fx-border-radius: 4; -fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #2563eb;";

        btnInside.setStyle(TOGGLE_STYLE);
        btnCentered.setStyle(TOGGLE_STYLE);
        btnOutside.setStyle(TOGGLE_STYLE);

        ToggleGroup typeGroup = new ToggleGroup();
        btnInside.setToggleGroup(typeGroup);
        btnCentered.setToggleGroup(typeGroup);
        btnOutside.setToggleGroup(typeGroup);

        javafx.scene.shape.StrokeType currentType = (active != null) ? active.getStrokeType() : javafx.scene.shape.StrokeType.CENTERED;
        if (currentType == javafx.scene.shape.StrokeType.INSIDE) {
            btnInside.setSelected(true);
            btnInside.setStyle(TOGGLE_SELECTED);
        } else if (currentType == javafx.scene.shape.StrokeType.OUTSIDE) {
            btnOutside.setSelected(true);
            btnOutside.setStyle(TOGGLE_SELECTED);
        } else {
            btnCentered.setSelected(true);
            btnCentered.setStyle(TOGGLE_SELECTED);
        }

        typeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                typeGroup.selectToggle(oldVal);
                return;
            }
            javafx.scene.shape.StrokeType type = javafx.scene.shape.StrokeType.CENTERED;
            if (newVal == btnInside) type = javafx.scene.shape.StrokeType.INSIDE;
            else if (newVal == btnOutside) type = javafx.scene.shape.StrokeType.OUTSIDE;

            btnInside.setStyle(newVal == btnInside ? TOGGLE_SELECTED : TOGGLE_STYLE);
            btnCentered.setStyle(newVal == btnCentered ? TOGGLE_SELECTED : TOGGLE_STYLE);
            btnOutside.setStyle(newVal == btnOutside ? TOGGLE_SELECTED : TOGGLE_STYLE);

            if (!controller.isUpdatingUI()) {
                javafx.scene.shape.StrokeType finalType = type;
                controller.getActionHandler().recordPropertyChange("Alineación del Borde", ShapeLayer::getStrokeType, ShapeLayer::setStrokeType, finalType);
            }
        });

        HBox typeBox = new HBox(5, btnInside, btnCentered, btnOutside);
        typeBox.setAlignment(Pos.CENTER);

        box.getChildren().addAll(lblWidth, slWidth, lblVal, new Separator(), lblType, typeBox);
        showPopup(anchor, box, "BORDE / TRAZO");
    }

    public Node createColorGraphic(Color c) {
        if (c == null) {
            StackPane stack = new StackPane();
            Rectangle bg = new Rectangle(24, 24, Color.WHITE);
            bg.setStroke(Color.GRAY);
            bg.setStrokeWidth(1);

            Line l1 = new Line(2, 2, 22, 22);
            l1.setStroke(Color.web("#95a5a6"));
            l1.setStrokeWidth(1.5);
            Line l2 = new Line(22, 2, 2, 22);
            l2.setStroke(Color.web("#95a5a6"));
            l2.setStrokeWidth(1.5);

            stack.getChildren().addAll(bg, l1, l2);
            return stack;
        }

        if (Color.TRANSPARENT.equals(c)) {
            StackPane stack = new StackPane();
            Rectangle bg = new Rectangle(24, 24, Color.WHITE);
            bg.setStroke(Color.GRAY);
            bg.setStrokeWidth(1);

            GridPane grid = new GridPane();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    Rectangle sub = new Rectangle(12, 12);
                    sub.setFill((i + j) % 2 == 0 ? Color.WHITE : Color.web("#f0f0f0"));
                    grid.add(sub, i, j);
                }
            }
            grid.setMouseTransparent(true);

            Line slash = new Line(0, 24, 24, 0);
            slash.setStroke(Color.web("#E31B23"));
            slash.setStrokeWidth(2.5);

            stack.getChildren().addAll(bg, grid, slash);
            return stack;
        }

        Rectangle r = new Rectangle(24, 24, c);
        r.setStroke(Color.GRAY);
        r.setStrokeWidth(1);
        return r;
    }

    public void styleToolButton(Button btn) {
        btn.setMinSize(36, 36);
        btn.setPrefSize(36, 36);
        btn.setMaxSize(36, 36);
        btn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 6; -fx-background-insets: 0;");
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("#d6eaf8"))
                btn.setStyle(
                        "-fx-background-color: #ecf0f1; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8; -fx-background-insets: 0;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("#d6eaf8"))
                btn.setStyle(
                        "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 8; -fx-background-insets: 0;");
        });
    }

    public void showContourPopup(Node anchor) {
        VBox box = new VBox(10);
        ShapeLayer active = controller.getActiveShapeLayer();

        CheckBox chk = new CheckBox("Silueta");
        chk.setSelected(active != null && active.getContourSteps() > 0);

        Slider slSteps = new Slider(1, 10, (active != null) ? Math.max(1, active.getContourSteps()) : 3);
        Slider slDist = new Slider(1, 40, (active != null) ? Math.max(1, active.getContourDistance()) : 5);
        Color contourColor = (active != null) ? active.getContourColor() : Color.GRAY;

        final MenuButton[] btnColorRef = new MenuButton[1];
        MenuButton btnColor = controller.getButtonFactory().createColorMenuButton("mdi2b-border-outside", contourColor, "Color de Silueta",
                c -> {
                    if (!controller.isUpdatingUI()) {
                        controller.getActionHandler().recordPropertyChange("Contour Color", ShapeLayer::getContourColor,
                                (layer, val) -> {
                                    layer.applyContour((int) slSteps.getValue(), slDist.getValue(), val);
                                }, c);
                    }
                },
                c -> controller.getButtonFactory().updatePickerGraphic(btnColorRef[0], c),
                () -> controller.activateEyedropper(c -> { controller.getButtonFactory().updatePickerGraphic(btnColorRef[0], c); controller.getActionHandler().recordPropertyChange("Contour Color", ShapeLayer::getContourColor, (layer, val) -> { layer.applyContour((int) slSteps.getValue(), slDist.getValue(), val); }, c); }),
                () -> org.example.utils.UIFactory.showColorSelector(btnColorRef[0].getScene().getWindow(), (Color)btnColorRef[0].getUserData(), c -> { controller.getButtonFactory().updatePickerGraphic(btnColorRef[0], c); controller.getActionHandler().recordPropertyChange("Contour Color", ShapeLayer::getContourColor, (layer, val) -> { layer.applyContour((int) slSteps.getValue(), slDist.getValue(), val); }, c); }, c -> { controller.getButtonFactory().updatePickerGraphic(btnColorRef[0], c); controller.getActionHandler().recordPropertyChange("Contour Color", ShapeLayer::getContourColor, (layer, val) -> { layer.applyContour((int) slSteps.getValue(), slDist.getValue(), val); }, c); }));
        btnColorRef[0] = btnColor;

        chk.selectedProperty().addListener(o -> {
            if (!controller.isUpdatingUI()) {
                controller.getActionHandler().recordPropertyChange("Toggle Contour",
                        layer -> layer.getContourSteps() > 0,
                        (layer, val) -> {
                            Color currentColor = (Color) btnColorRef[0].getUserData();
                            if (currentColor == null)
                                currentColor = contourColor;
                            if (val)
                                layer.applyContour((int) slSteps.getValue(), slDist.getValue(), currentColor);
                            else
                                layer.applyContour(0, 0, Color.TRANSPARENT);
                        }, chk.isSelected());
            }
        });

        // Sliders with transactional undo
        setupTransactionalSlider(slSteps, "Contour Steps", val -> (double) active.getContourSteps(), (layer, val) -> {
            Color currentColor = (Color) btnColor.getUserData();
            layer.applyContour(val.intValue(), slDist.getValue(), (currentColor != null ? currentColor : contourColor));
        });

        setupTransactionalSlider(slDist, "Contour Distance", val -> active.getContourDistance(), (layer, val) -> {
            Color currentColor = (Color) btnColor.getUserData();
            layer.applyContour((int) slSteps.getValue(), val, (currentColor != null ? currentColor : contourColor));
        });

        TextField txtSteps = createValueField(slSteps, "");
        TextField txtDist = createValueField(slDist, "");

        // Corner Style Logic
        HBox styleBox = createCornerStyleSelector();

        Button btnSeparate = new Button("Separar Contornos");
        btnSeparate.setStyle(
                "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSeparate.setMaxWidth(Double.MAX_VALUE);
        btnSeparate.setOnAction(e -> {
            if (active != null) {
                java.util.List<ShapeLayer> newLayers = active.separateContours();
                Node prev = active;
                java.util.List<Node> allNodes = new java.util.ArrayList<>();
                allNodes.add(active);
                for (ShapeLayer nl : newLayers) {
                    visualizer.addShapeLayer(nl);
                    visualizer.getLayerManager().moveNodeBehind(nl, prev);
                    allNodes.add(nl);
                    prev = nl;
                }
                controller.setIsUpdatingUI(true);
                slSteps.setValue(0);
                controller.setIsUpdatingUI(false);
                visualizer.getLayerManager().clearSelection();
                for (Node n : allNodes) {
                    visualizer.getLayerManager().addToSelection(n);
                }
            }
        });

        box.getChildren().addAll(chk,
                new HBox(10, new Label("Pasos"), txtSteps), slSteps,
                new HBox(10, new Label("Distancia"), txtDist), slDist,
                new HBox(10, new Label("Color"), btnColor),
                new HBox(10, new Label("Bordes"), styleBox),
                new Separator(), btnSeparate);

        showPopup(anchor, box, "SILUETA / CONTORNO");
    }

    private void setupTransactionalSlider(Slider slider, String name,
            java.util.function.Function<Double, Double> getter,
            java.util.function.BiConsumer<ShapeLayer, Double> setter) {
        final Double[] startVal = { 0.0 };
        slider.setOnMousePressed(e -> startVal[0] = slider.getValue());
        slider.setOnMouseReleased(e -> {
            double newVal = slider.getValue();
            if (!Objects.equals(startVal[0], newVal)) {
                controller.getActionHandler().recordPropertyChange(name, layer -> startVal[0], setter, newVal);
            }
        });
        slider.valueProperty().addListener((o, old, v) -> {
            if (!controller.isUpdatingUI()) {
                controller.getActionHandler().applyToSelection(layer -> setter.accept(layer, v.doubleValue()));
            }
        });
    }

    private HBox createCornerStyleSelector() {
        ToggleButton btnSquare = controller.getButtonFactory().createVerticalToolButton("mdi2a-angle-right", "Bordes Cuadrados");
        ToggleButton btnRound = controller.getButtonFactory().createVerticalToolButton("mdi2c-circle-medium", "Bordes Redondos");

        String TOGGLE_STYLE = "-fx-background-radius: 4; -fx-padding: 4; -fx-cursor: hand; -fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 4;";
        String TOGGLE_SELECTED = "-fx-background-radius: 4; -fx-padding: 4; -fx-cursor: hand; -fx-background-color: #d6eaf8; -fx-border-color: #3498db; -fx-border-radius: 4;";

        btnSquare.setStyle(TOGGLE_STYLE);
        btnRound.setStyle(TOGGLE_STYLE);

        ToggleGroup styleGroup = new ToggleGroup();
        btnSquare.setToggleGroup(styleGroup);
        btnRound.setToggleGroup(styleGroup);

        ShapeLayer active = controller.getActiveShapeLayer();
        StrokeLineJoin currentJoin = (active != null) ? active.getContourLineJoin() : StrokeLineJoin.MITER;
        if (currentJoin == StrokeLineJoin.ROUND) {
            btnRound.setSelected(true);
            btnRound.setStyle(TOGGLE_SELECTED);
        } else {
            btnSquare.setSelected(true);
            btnSquare.setStyle(TOGGLE_SELECTED);
        }

        styleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                styleGroup.selectToggle(oldVal);
                return;
            }
            StrokeLineJoin join = (newVal == btnRound) ? StrokeLineJoin.ROUND : StrokeLineJoin.MITER;
            btnSquare.setStyle(newVal == btnSquare ? TOGGLE_SELECTED : TOGGLE_STYLE);
            btnRound.setStyle(newVal == btnRound ? TOGGLE_SELECTED : TOGGLE_STYLE);
            if (!controller.isUpdatingUI()) {
                controller.getActionHandler().applyToSelection(layer -> layer.setContourLineJoin(join));
            }
        });

        return new HBox(5, btnSquare, btnRound);
    }

    public void showTransPopup(Node anchor) {
        VBox box = new VBox(10);
        ShapeLayer active = controller.getActiveShapeLayer();

        CheckBox chk = new CheckBox("Activar Transparencia");
        chk.setSelected(active != null && active.isTransparencyEnabled());

        Slider slStart = new Slider(0, 1, (active != null) ? active.getTransparencyStartAlpha() : 1);
        Slider slEnd = new Slider(0, 1, (active != null) ? active.getTransparencyEndAlpha() : 0);
        Slider slAngle = new Slider(0, 360, (active != null) ? active.getTransparencyAngle() : 0);
        Slider slBal = new Slider(0, 1, (active != null) ? active.getTransparencyBalance() : 0.5);

        chk.selectedProperty().addListener(o -> {
            if (!controller.isUpdatingUI()) {
                controller.getActionHandler().recordPropertyChange("Toggle Transparency",
                        ShapeLayer::isTransparencyEnabled,
                        (layer, val) -> {
                            layer.setTransparency(val, slAngle.getValue(), slStart.getValue(), slEnd.getValue());
                            layer.setTransparencyBalance(slBal.getValue());
                        }, chk.isSelected());
            }
        });

        setupTransactionalSlider(slStart, "Transparency Start", v -> active.getTransparencyStartAlpha(),
                (layer, v) -> layer.setTransparency(chk.isSelected(), slAngle.getValue(), v, slEnd.getValue()));
        setupTransactionalSlider(slEnd, "Transparency End", v -> active.getTransparencyEndAlpha(),
                (layer, v) -> layer.setTransparency(chk.isSelected(), slAngle.getValue(), slStart.getValue(), v));
        setupTransactionalSlider(slAngle, "Transparency Angle", v -> active.getTransparencyAngle(),
                (layer, v) -> layer.setTransparency(chk.isSelected(), v, slStart.getValue(), slEnd.getValue()));
        setupTransactionalSlider(slBal, "Transparency Balance", v -> active.getTransparencyBalance(),
                ShapeLayer::setTransparencyBalance);

        box.getChildren().addAll(chk,
                new HBox(10, new Label("Inicio Opacidad"), createValueField(slStart, " %")), slStart,
                new HBox(10, new Label("Fin Opacidad"), createValueField(slEnd, " %")), slEnd,
                new HBox(10, new Label("Ángulo"), createValueField(slAngle, "°")), slAngle,
                new HBox(10, new Label("Balance"), createValueField(slBal, " %")), slBal);

        showPopup(anchor, box, "TRANSPARENCIA / DEGRADE");
    }

    public TextField createValueField(Slider slider, String suffix) {
        TextField tf = new TextField();
        tf.setPrefWidth(50);
        tf.setAlignment(Pos.CENTER_RIGHT);
        tf.setStyle("-fx-font-size: 10px; -fx-padding: 2 4;");
        updateTextField(tf, slider.getValue(), suffix);
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!tf.isFocused())
                updateTextField(tf, newVal.doubleValue(), suffix);
        });
        tf.setOnAction(e -> commitTextFieldValue(tf, slider, suffix));
        tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal)
                commitTextFieldValue(tf, slider, suffix);
        });
        return tf;
    }

    private void updateTextField(TextField tf, double value, String suffix) {
        if (suffix.contains("%"))
            tf.setText(String.format("%.0f%s", value * 100, suffix.trim()));
        else
            tf.setText(String.format("%.0f%s", value, suffix));
    }

    private void commitTextFieldValue(TextField tf, Slider slider, String suffix) {
        String text = tf.getText().replace(suffix.trim(), "").replace("°", "").replace("%", "").trim();
        try {
            double val = Double.parseDouble(text);
            if (suffix.contains("%"))
                val = val / 100.0;
            if (val < slider.getMin())
                val = slider.getMin();
            if (val > slider.getMax())
                val = slider.getMax();
            slider.setValue(val);
        } catch (NumberFormatException e) {
            updateTextField(tf, slider.getValue(), suffix);
        }
        updateTextField(tf, slider.getValue(), suffix);
    }

    public void closePopup() {
        if (currentPopup != null)
            currentPopup.hide();
    }

    public void activateEyedropper(Consumer<Color> onColorPicked, Consumer<Color> onHover) {
        visualizer.cancelShapeCreation();
        visualizer.getShapeHelper().exitNodeEditMode();
        visualizer.setCursor(javafx.scene.Cursor.CROSSHAIR);
        visualizer.getShapeHelper().startEyedropperSession(color -> {
            if (onColorPicked != null)
                onColorPicked.accept(color);
            visualizer.setCursor(javafx.scene.Cursor.DEFAULT);
        }, color -> {
            if (onHover != null)
                onHover.accept(color);
        });
    }

    private void styleTransformActionButton(Button btn) {
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setStyle(
                "-fx-background-color: #ecf0f1; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8; -fx-font-size: 10px; -fx-text-fill: #2c3e50;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #d6eaf8; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8; -fx-font-size: 10px; -fx-text-fill: #2980b9;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #ecf0f1; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8; -fx-font-size: 10px; -fx-text-fill: #2c3e50;"));
    }
}

