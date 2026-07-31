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
        currentPopup.setAutoHide(false);

        VBox root = new VBox(5);
        root.setPadding(new javafx.geometry.Insets(10));
        root.setStyle(
                "-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0); -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-background-radius: 6;");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(lblTitle, javafx.scene.layout.Priority.ALWAYS);
        lblTitle.setMaxWidth(Double.MAX_VALUE);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4;");
        btnClose.setOnAction(e -> {
            if (currentPopup != null) currentPopup.hide();
        });

        header.getChildren().addAll(lblTitle, btnClose);
        root.getChildren().addAll(header, new Separator(), content);

        currentPopup.getContent().add(root);

        javafx.scene.Scene scene = (anchor != null && anchor.getScene() != null) ? anchor.getScene() : (visualizer != null ? visualizer.getScene() : null);
        javafx.stage.Window window = (scene != null) ? scene.getWindow() : null;

        javafx.geometry.Point2D p = (anchor != null && anchor.getScene() != null) ? anchor.localToScreen(0, 0) : null;

        javafx.application.Platform.runLater(() -> {
            javafx.stage.Window targetWindow = window;
            if (targetWindow == null && visualizer != null && visualizer.getScene() != null) {
                targetWindow = visualizer.getScene().getWindow();
            }
            if (targetWindow != null) {
                currentPopup.show(targetWindow);
                double w = root.getWidth() > 0 ? root.getWidth() : 250;
                double windowX = targetWindow.getX();
                double targetX = (p != null) ? p.getX() - w - 10 : windowX + 200;
                if (targetX < windowX + 10) {
                    double anchorWidth = (anchor != null && anchor.getBoundsInParent() != null) ? anchor.getBoundsInParent().getWidth() : 50;
                    targetX = (p != null) ? p.getX() + anchorWidth + 10 : windowX + 200;
                }
                if (targetX < windowX + 10) {
                    targetX = windowX + 100;
                }
                double targetY = (p != null) ? p.getY() - 50 : targetWindow.getY() + 150;
                targetY = Math.max(targetWindow.getY() + 50, targetY);
                currentPopup.setX(targetX);
                currentPopup.setY(targetY);

                javafx.application.Platform.runLater(() -> {
                    if (currentPopup != null) currentPopup.setAutoHide(true);
                });
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
                    if (clone instanceof ShapeLayer) {
                        ShapeLayer sl = (ShapeLayer) clone;
                        double shirtCenter = 450.0;
                        if (visualizer.getOverlayManager() != null) {
                            shirtCenter = visualizer.getOverlayManager().getSleeveThreshold();
                        }
                        sl.flipHorizontalSymmetric(shirtCenter);
                    } else if (clone instanceof GroupLayerV2) {
                        ((GroupLayerV2) clone).flipHorizontal();
                        double offsetX = node.getBoundsInParent().getWidth();
                        clone.setTranslateX(node.getTranslateX() + offsetX);
                    } else if (clone instanceof org.example.component.AbstractGraphicLayer) {
                        ((org.example.component.AbstractGraphicLayer) clone).flipHorizontal();
                        double offsetX = node.getBoundsInParent().getWidth();
                        clone.setTranslateX(node.getTranslateX() + offsetX);
                    }
                    
                    if (node.getParent() instanceof javafx.scene.Group) {
                        visualizer.getUserLayerManager().addLayerToContainer(clone, (javafx.scene.Group)node.getParent(), false);
                    } else {
                        visualizer.getUserLayerManager().addLayer(clone);
                    }
                    visualizer.getUserLayerManager().selectNode(clone);
                }
            }
        });
        mirrorBox.getChildren().addAll(btnFlipH, btnFlipV, btnDuplicateOpposite);

        box.getChildren().addAll(new HBox(10, lblRot, txtRot), slRot, new Separator(), new Label("Invertir:"),
                mirrorBox, new Separator(), new HBox(10, lblScale, txtScale), slScale);
        showPopup(anchor, box, "TRANSFORMAR");
    }

    public void showStrokePopup(Node anchor) {
        VBox box = new VBox(8);
        box.setMinWidth(230);

        ShapeLayer active = controller.getActiveShapeLayer();
        double currentWidth = (active != null) ? active.getStrokeWidth() : 2.0;

        // ── Header label ──────────────────────────────────────────────
        Label lblWidth = new Label("Grosor del Borde:");
        lblWidth.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #2c3e50;");

        // ── TextField + spinner arrows ─────────────────────────────────
        // Converts raw slider position [0..1] ↔ real px using a log-like scale
        // that spends ~60% of the slider on [0..5] px (the most-used range).
        final double MAX_PX = 50.0;

        // px → slider pos [0..1]
        java.util.function.DoubleUnaryOperator toSlider = px -> {
            double clamped = Math.max(0, Math.min(MAX_PX, px));
            // split: first 60% of slider covers 0-5px, remaining 40% covers 5-50px
            if (clamped <= 5.0) return (clamped / 5.0) * 0.60;
            else return 0.60 + ((clamped - 5.0) / 45.0) * 0.40;
        };
        // slider pos [0..1] → px
        java.util.function.DoubleUnaryOperator toPx = pos -> {
            double clamped = Math.max(0, Math.min(1.0, pos));
            if (clamped <= 0.60) return (clamped / 0.60) * 5.0;
            else return 5.0 + ((clamped - 0.60) / 0.40) * 45.0;
        };

        Slider slWidth = new Slider(0, 1, toSlider.applyAsDouble(currentWidth));
        slWidth.setShowTickMarks(false);
        slWidth.setShowTickLabels(false);
        slWidth.setPrefWidth(200);
        slWidth.setStyle("-fx-padding: 4 0;");

        TextField tfVal = new TextField(String.format("%.1f", currentWidth));
        tfVal.setPrefWidth(60);
        tfVal.setAlignment(Pos.CENTER_RIGHT);
        tfVal.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 4; " +
                "-fx-border-color: #bdc3c7; -fx-border-radius: 4; -fx-padding: 2 6;");

        Label lblPx = new Label("px");
        lblPx.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        final boolean[] updatingFromSlider = {false};
        final boolean[] updatingFromField  = {false};
        final Double[]  widthStart         = {currentWidth};

        // Helper: apply value to shapes and sync both controls
        Consumer<Double> applyValue = px -> {
            double v = Math.max(0, Math.min(MAX_PX, px));
            if (!updatingFromSlider[0]) {
                updatingFromSlider[0] = true;
                slWidth.setValue(toSlider.applyAsDouble(v));
                updatingFromSlider[0] = false;
            }
            if (!updatingFromField[0]) {
                updatingFromField[0] = true;
                tfVal.setText(String.format("%.1f", v));
                updatingFromField[0] = false;
            }
            if (!controller.isUpdatingUI()) {
                controller.getActionHandler().applyToSelection(layer -> layer.setStrokeWidth(v));
                controller.getStrokeWidthSlider().setValue(v);
            }
        };

        // Slider changes
        slWidth.setOnMousePressed(e -> widthStart[0] = toPx.applyAsDouble(slWidth.getValue()));
        slWidth.setOnMouseReleased(e -> {
            double newVal = toPx.applyAsDouble(slWidth.getValue());
            if (!Objects.equals(widthStart[0], newVal)) {
                Double capturedStart = widthStart[0];
                controller.getActionHandler().recordPropertyChange("Stroke Width",
                        layer -> capturedStart, ShapeLayer::setStrokeWidth, newVal);
            }
        });
        slWidth.valueProperty().addListener((o, old, v) -> {
            if (!updatingFromSlider[0]) {
                updatingFromSlider[0] = true;
                applyValue.accept(toPx.applyAsDouble(v.doubleValue()));
                updatingFromSlider[0] = false;
            }
        });

        // TextField: commit on Enter or focus loss
        Runnable commitField = () -> {
            try {
                double v = Double.parseDouble(tfVal.getText().replace(",", ".").trim());
                Double capturedStart = widthStart[0];
                applyValue.accept(v);
                if (!Objects.equals(capturedStart, v)) {
                    controller.getActionHandler().recordPropertyChange("Stroke Width",
                            layer -> capturedStart, ShapeLayer::setStrokeWidth, v);
                    widthStart[0] = v;
                }
            } catch (NumberFormatException ignored) {
                tfVal.setText(String.format("%.1f", toPx.applyAsDouble(slWidth.getValue())));
            }
        };
        tfVal.setOnAction(e -> commitField.run());
        tfVal.focusedProperty().addListener((o, old, focused) -> { if (!focused) commitField.run(); });

        // Keyboard arrows on TextField:  ↑/↓ = ±0.1 | Shift = ±1 | Ctrl = ±5
        tfVal.setOnKeyPressed(e -> {
            double delta = 0;
            switch (e.getCode()) {
                case UP:   delta = e.isControlDown() ? 5.0 : e.isShiftDown() ? 1.0 : 0.1; break;
                case DOWN: delta = e.isControlDown() ? -5.0 : e.isShiftDown() ? -1.0 : -0.1; break;
                default: return;
            }
            e.consume();
            double cur = toPx.applyAsDouble(slWidth.getValue());
            applyValue.accept(Math.round((cur + delta) * 10.0) / 10.0);
        });

        HBox inputRow = new HBox(6, slWidth, tfVal, lblPx);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        // ── Presets row ────────────────────────────────────────────────
        double[] presets = {0, 0.1, 0.5, 1, 2, 3, 5, 10, 20};
        FlowPane presetsPane = new FlowPane(4, 4);
        presetsPane.setPrefWrapLength(225);
        String PRESET_STYLE = "-fx-background-radius: 3; -fx-padding: 2 6; -fx-cursor: hand; " +
                "-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-radius: 3; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #34495e;";
        String PRESET_HOVER = "-fx-background-radius: 3; -fx-padding: 2 6; -fx-cursor: hand; " +
                "-fx-background-color: #d6eaf8; -fx-border-color: #3498db; -fx-border-radius: 3; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563eb;";

        for (double p : presets) {
            String label = (p == (int) p) ? ((int) p + " px") : (p + " px");
            Button btn = new Button(label);
            btn.setStyle(PRESET_STYLE);
            btn.setOnMouseEntered(e -> btn.setStyle(PRESET_HOVER));
            btn.setOnMouseExited(e -> btn.setStyle(PRESET_STYLE));
            btn.setOnAction(e -> {
                Double capturedStart = toPx.applyAsDouble(slWidth.getValue());
                applyValue.accept(p);
                controller.getActionHandler().recordPropertyChange("Stroke Width",
                        layer -> capturedStart, ShapeLayer::setStrokeWidth, p);
                widthStart[0] = p;
            });
            presetsPane.getChildren().add(btn);
        }

        // ── Alineación del Borde ───────────────────────────────────────
        Label lblType = new Label("Alineación del Borde:");
        lblType.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #2c3e50;");

        ToggleButton btnInside   = new ToggleButton("Interior");
        ToggleButton btnCentered = new ToggleButton("Compartido");
        ToggleButton btnOutside  = new ToggleButton("Exterior");

        String TOGGLE_STYLE    = "-fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;";
        String TOGGLE_SELECTED = "-fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-background-color: #d6eaf8; -fx-border-color: #3498db; -fx-border-radius: 4; -fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #2563eb;";

        btnInside.setStyle(TOGGLE_STYLE); btnCentered.setStyle(TOGGLE_STYLE); btnOutside.setStyle(TOGGLE_STYLE);

        ToggleGroup typeGroup = new ToggleGroup();
        btnInside.setToggleGroup(typeGroup); btnCentered.setToggleGroup(typeGroup); btnOutside.setToggleGroup(typeGroup);

        javafx.scene.shape.StrokeType currentType = (active != null) ? active.getStrokeType() : javafx.scene.shape.StrokeType.CENTERED;
        if      (currentType == javafx.scene.shape.StrokeType.INSIDE)   { btnInside.setSelected(true);   btnInside.setStyle(TOGGLE_SELECTED); }
        else if (currentType == javafx.scene.shape.StrokeType.OUTSIDE)  { btnOutside.setSelected(true);  btnOutside.setStyle(TOGGLE_SELECTED); }
        else                                                             { btnCentered.setSelected(true); btnCentered.setStyle(TOGGLE_SELECTED); }

        typeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) { typeGroup.selectToggle(oldVal); return; }
            javafx.scene.shape.StrokeType type = javafx.scene.shape.StrokeType.CENTERED;
            if (newVal == btnInside)  type = javafx.scene.shape.StrokeType.INSIDE;
            else if (newVal == btnOutside) type = javafx.scene.shape.StrokeType.OUTSIDE;
            btnInside.setStyle(newVal == btnInside ? TOGGLE_SELECTED : TOGGLE_STYLE);
            btnCentered.setStyle(newVal == btnCentered ? TOGGLE_SELECTED : TOGGLE_STYLE);
            btnOutside.setStyle(newVal == btnOutside ? TOGGLE_SELECTED : TOGGLE_STYLE);
            if (!controller.isUpdatingUI()) {
                javafx.scene.shape.StrokeType finalType = type;
                controller.getActionHandler().recordPropertyChange("Alineación del Borde",
                        ShapeLayer::getStrokeType, ShapeLayer::setStrokeType, finalType);
            }
        });

        HBox typeBox = new HBox(5, btnInside, btnCentered, btnOutside);
        typeBox.setAlignment(Pos.CENTER);

        box.getChildren().addAll(
                lblWidth,
                inputRow,
                presetsPane,
                new Separator(),
                lblType,
                typeBox
        );
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
        ShapeLayer active = null;
        if (visualizer != null && visualizer.getLayerManager() != null) {
            Node sel = visualizer.getLayerManager().getSelectedNode();
            if (sel instanceof ShapeLayer) {
                active = (ShapeLayer) sel;
                controller.setActiveShapeLayer(active);
            }
        }
        if (active == null) {
            active = controller.getActiveShapeLayer();
        }
        final ShapeLayer currentActive = active;

        boolean hasContour = (currentActive != null && currentActive.getContourSteps() > 0);
        CheckBox chk = new CheckBox("Silueta");
        chk.setSelected(hasContour);

        double initialSteps = (currentActive != null && currentActive.getContourSteps() > 0) ? currentActive.getContourSteps() : 3;
        double initialDist = (currentActive != null && currentActive.getContourDistance() > 0) ? currentActive.getContourDistance() : 5;
        Color initialColor = (currentActive != null && currentActive.getContourColor() != null && !Color.TRANSPARENT.equals(currentActive.getContourColor())) 
                ? currentActive.getContourColor() 
                : Color.web("#e74c3c");

        Slider slSteps = new Slider(1, 10, initialSteps);
        Slider slDist = new Slider(1, 40, initialDist);

        final MenuButton[] btnColorRef = new MenuButton[1];
        MenuButton btnColor = controller.getButtonFactory().createColorMenuButton("mdi2b-border-outside", initialColor, "Color de Silueta",
                c -> {
                    btnColorRef[0].setUserData(c);
                    if (!controller.isUpdatingUI()) {
                        controller.getActionHandler().recordPropertyChange("Contour Color", ShapeLayer::getContourColor,
                                (layer, val) -> {
                                    layer.applyContour((int) slSteps.getValue(), slDist.getValue(), val);
                                }, c);
                    }
                },
                c -> {
                    btnColorRef[0].setUserData(c);
                    controller.getButtonFactory().updatePickerGraphic(btnColorRef[0], c);
                },
                () -> controller.activateEyedropper(c -> {
                    btnColorRef[0].setUserData(c);
                    controller.getButtonFactory().updatePickerGraphic(btnColorRef[0], c);
                    controller.getActionHandler().recordPropertyChange("Contour Color", ShapeLayer::getContourColor, (layer, val) -> { layer.applyContour((int) slSteps.getValue(), slDist.getValue(), val); }, c);
                }),
                () -> org.example.utils.UIFactory.showColorSelector(btnColorRef[0].getScene().getWindow(), (Color)btnColorRef[0].getUserData(), c -> {
                    btnColorRef[0].setUserData(c);
                    controller.getButtonFactory().updatePickerGraphic(btnColorRef[0], c);
                    controller.getActionHandler().recordPropertyChange("Contour Color", ShapeLayer::getContourColor, (layer, val) -> { layer.applyContour((int) slSteps.getValue(), slDist.getValue(), val); }, c);
                }, c -> {
                    btnColorRef[0].setUserData(c);
                    controller.getButtonFactory().updatePickerGraphic(btnColorRef[0], c);
                    controller.getActionHandler().recordPropertyChange("Contour Color", ShapeLayer::getContourColor, (layer, val) -> { layer.applyContour((int) slSteps.getValue(), slDist.getValue(), val); }, c);
                }));
        btnColorRef[0] = btnColor;
        btnColor.setUserData(initialColor);

        chk.selectedProperty().addListener(o -> {
            if (!controller.isUpdatingUI()) {
                controller.getActionHandler().recordPropertyChange("Toggle Contour",
                        layer -> layer.getContourSteps() > 0,
                        (layer, val) -> {
                            Color currentColor = (Color) btnColorRef[0].getUserData();
                            if (currentColor == null)
                                currentColor = initialColor;
                            if (val)
                                layer.applyContour((int) slSteps.getValue(), slDist.getValue(), currentColor);
                            else
                                layer.applyContour(0, 0, Color.TRANSPARENT);
                        }, chk.isSelected());
            }
        });

        // Sliders with transactional undo and real-time canvas update
        setupTransactionalSlider(slSteps, "Contour Steps", v -> (currentActive != null ? (double) currentActive.getContourSteps() : 3.0), (layer, val) -> {
            Color currentColor = (Color) btnColorRef[0].getUserData();
            layer.applyContour(val.intValue(), slDist.getValue(), (currentColor != null ? currentColor : initialColor));
            if (visualizer != null) visualizer.notifyStateChanged();
        });

        setupTransactionalSlider(slDist, "Contour Distance", v -> (currentActive != null ? currentActive.getContourDistance() : 5.0), (layer, val) -> {
            Color currentColor = (Color) btnColorRef[0].getUserData();
            layer.applyContour((int) slSteps.getValue(), val, (currentColor != null ? currentColor : initialColor));
            if (visualizer != null) visualizer.notifyStateChanged();
        });

        TextField txtSteps = createValueField(slSteps, "");
        TextField txtDist = createValueField(slDist, "");

        // Corner Style Logic
        HBox styleBox = createCornerStyleSelector();

        Button btnRemove = new Button("Eliminar Silueta");
        btnRemove.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnRemove.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnRemove, javafx.scene.layout.Priority.ALWAYS);
        btnRemove.setOnAction(e -> {
            if (!controller.isUpdatingUI()) {
                ShapeLayer targetLayer = currentActive != null ? currentActive : controller.getActiveShapeLayer();
                if (targetLayer != null && targetLayer.getActiveZone() != null && targetLayer.getActiveZone().startsWith("CUELLO")) {
                    if (visualizer != null) {
                        visualizer.getPowerClipManager().extractFromContainer(targetLayer);
                        visualizer.removeUserLayer(targetLayer);
                        visualizer.getUserLayerManager().clearSelection();
                        if (visualizer.getVisualizerUiController() != null && visualizer.getVisualizerUiController().getBtnAddSilhouette() != null) {
                            visualizer.getVisualizerUiController().getBtnAddSilhouette().setText("+ AÑADIR SILUETA");
                        }
                        if (currentPopup != null) currentPopup.hide();
                        visualizer.notifyStateChanged();
                    }
                    return;
                }
                if (currentActive != null) {
                    controller.getActionHandler().applyToSelection(layer -> layer.applyContour(0, 0, Color.TRANSPARENT));
                    controller.setIsUpdatingUI(true);
                    chk.setSelected(false);
                    slSteps.setValue(1);
                    controller.setIsUpdatingUI(false);
                    if (visualizer != null) visualizer.notifyStateChanged();
                }
            }
        });

        Button btnSeparate = new Button("Separar Contornos");
        btnSeparate.setStyle(
                "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnSeparate.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnSeparate, javafx.scene.layout.Priority.ALWAYS);
        btnSeparate.setOnAction(e -> {
            ShapeLayer targetLayer = currentActive != null ? currentActive : controller.getActiveShapeLayer();
            if (targetLayer != null) {
                String activeZone = targetLayer.getActiveZone();
                java.util.List<ShapeLayer> newLayers = targetLayer.separateContours();
                Node prev = targetLayer;
                java.util.List<Node> allNodes = new java.util.ArrayList<>();
                allNodes.add(targetLayer);
                for (ShapeLayer nl : newLayers) {
                    if (activeZone != null && activeZone.startsWith("CUELLO")) {
                        visualizer.getPowerClipManager().restoreToContainer(nl, activeZone);
                        visualizer.getLayerManager().moveNodeBehind(nl, prev);
                        // Apply collar boundary clip so separated layers don't bleed outside the collar shape
                        String collarBoundary = visualizer.getCollarVectorContent(activeZone);
                        if (collarBoundary != null && !collarBoundary.trim().isEmpty()) {
                            nl.setContourClip(collarBoundary);
                        }
                    } else {
                        visualizer.addShapeLayer(nl);
                        visualizer.getLayerManager().moveNodeBehind(nl, prev);
                    }
                    allNodes.add(nl);
                    prev = nl;
                }
                controller.setIsUpdatingUI(true);
                chk.setSelected(false);
                slSteps.setValue(1);
                controller.setIsUpdatingUI(false);
                visualizer.getLayerManager().clearSelection();
                for (Node n : allNodes) {
                    visualizer.getLayerManager().addToSelection(n);
                }
                if (visualizer != null) visualizer.notifyStateChanged();
            }
        });

        box.getChildren().addAll(chk,
                new HBox(10, new Label("Pasos"), txtSteps), slSteps,
                new HBox(10, new Label("Distancia"), txtDist), slDist,
                new HBox(10, new Label("Color"), btnColor),
                new HBox(10, new Label("Bordes"), styleBox),
                new Separator(),
                new HBox(10, btnRemove, btnSeparate));

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

