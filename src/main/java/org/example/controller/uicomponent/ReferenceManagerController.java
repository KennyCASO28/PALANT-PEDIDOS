package org.example.controller.uicomponent;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.example.component.HotspotLayer;
import org.example.component.PrendaVisualizer;
import org.example.model.PrendaState.ReferenceHotspot;
import org.example.utils.UIFactory;
import org.example.component.ImageCropperDialog;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;

public class ReferenceManagerController {
    private final PrendaVisualizer visualizer;
    private final VBox container;
    private final VBox listContainer;
    private final List<ReferenceItemUI> items = new ArrayList<>();
    private Runnable onItemsChanged;
    
    private boolean isPlacing = false;
    private EventHandler<MouseEvent> placementHandler;
    private Button btnAdd;

    public ReferenceManagerController(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
        this.container = new VBox(10);
        this.container.setPadding(new Insets(10));
        this.container.setFillWidth(true);
        this.container.setMaxWidth(Double.MAX_VALUE);
        this.listContainer = new VBox(12); // Increased spacing between items
        this.listContainer.setPadding(new Insets(5));
        this.listContainer.setFillWidth(true);
        this.listContainer.setMaxWidth(Double.MAX_VALUE);
        
        setupUI();
    }

    private void setupUI() {
        Label title = new Label("Referencias Interactivas");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        btnAdd = new Button("Anclar Referencia +");
        btnAdd.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 6;");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> togglePlacementMode());

        container.getChildren().addAll(title, btnAdd, listContainer);
    }

    private void togglePlacementMode() {
        if (isPlacing) {
            cancelPlacement();
        } else {
            startPlacement();
        }
    }

    private void startPlacement() {
        isPlacing = true;
        btnAdd.setText("Cancelar Colocación (Clic en prenda)");
        btnAdd.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 6;");
        
        visualizer.setCursor(Cursor.CROSSHAIR);
        
        placementHandler = e -> {
            if (isPlacing && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                javafx.geometry.Point2D local = visualizer.getHotspotLayer().sceneToLocal(e.getSceneX(), e.getSceneY());
                if (local != null) {
                    addHotspotAt(local.getX(), local.getY());
                }
                cancelPlacement();
                e.consume(); // Prevent click from triggering other UI
            }
        };
        visualizer.addEventFilter(MouseEvent.MOUSE_CLICKED, placementHandler);
    }

    private void cancelPlacement() {
        isPlacing = false;
        btnAdd.setText("Anclar Referencia +");
        btnAdd.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 6;");
        
        visualizer.setCursor(Cursor.DEFAULT);
        
        if (placementHandler != null) {
            visualizer.removeEventFilter(MouseEvent.MOUSE_CLICKED, placementHandler);
            placementHandler = null;
        }
    }

    private void addHotspotAt(double x, double y) {
        ReferenceHotspot data = new ReferenceHotspot(x, y, "BODY", String.valueOf(items.size() + 1));
        visualizer.getState().getReferenceHotspots().add(data);
        
        org.example.component.HotspotLayer hl = new org.example.component.HotspotLayer(data, visualizer::notifyStateChanged, visualizer);
        visualizer.addHotspot(hl);
        
        ReferenceItemUI ui = new ReferenceItemUI(data, hl);
        items.add(ui);
        listContainer.getChildren().add(ui);
        
        visualizer.notifyStateChanged();
        if (onItemsChanged != null) onItemsChanged.run();
    }

    public void syncFromState() {
        listContainer.getChildren().clear();
        items.clear();
        visualizer.clearHotspots();
        
        for (ReferenceHotspot data : visualizer.getState().getReferenceHotspots()) {
            org.example.component.HotspotLayer hl = new org.example.component.HotspotLayer(data, visualizer::notifyStateChanged, visualizer);
            visualizer.addHotspot(hl);
            ReferenceItemUI ui = new ReferenceItemUI(data, hl);
            items.add(ui);
            listContainer.getChildren().add(ui);
        }
        if (onItemsChanged != null) onItemsChanged.run();
    }

    public void setOnItemsChanged(Runnable listener) {
        this.onItemsChanged = listener;
    }

    public List<ReferenceItemUI> getItems() {
        return items;
    }

    public VBox getContainer() {
        return container;
    }

    public class ReferenceItemUI extends VBox {
        private final ReferenceHotspot data;
        private final org.example.component.HotspotLayer layer;

        public ReferenceItemUI(ReferenceHotspot data, org.example.component.HotspotLayer layer) {
            this.data = data;
            this.layer = layer;
            setupUI();
        }

        public ReferenceHotspot getData() { return data; }
        public HotspotLayer getLayer() { return layer; }

        private void setupUI() {
            this.setSpacing(10);
            this.setPadding(new Insets(12));
            this.setMaxWidth(Double.MAX_VALUE);
            this.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setMaxWidth(Double.MAX_VALUE);
            Label lblNum = new Label("Ref:");
            lblNum.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
            
            TextField txtLabel = new TextField(data.getLabel());
            txtLabel.setPrefWidth(56);
            txtLabel.textProperty().addListener((obs, old, val) -> {
                data.setLabel(val);
                layer.updateVisuals();
                visualizer.notifyStateChanged();
            });

            TextField txtZone = new TextField(data.getZone());
            txtZone.setPromptText("Zona...");
            txtZone.setPrefWidth(100);
            txtZone.setStyle("-fx-font-weight: bold; -fx-background-color: #f8f9fa; -fx-border-color: #dcdde1; -fx-border-radius: 4;");
            txtZone.textProperty().addListener((obs, old, val) -> {
                data.setZone(val != null ? val.trim() : "");
                visualizer.notifyStateChanged();
            });

            Button btnRemove = new Button();
            try {
                btnRemove.setGraphic(UIFactory.crearIcono("mdi2d-delete", 16, "white"));
            } catch (Exception e) {
                btnRemove.setText("X");
            }
            btnRemove.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4 8;");
            btnRemove.setOnAction(e -> {
                visualizer.getState().getReferenceHotspots().remove(data);
                visualizer.removeHotspot(layer);
                listContainer.getChildren().remove(this);
                items.remove(this);
                visualizer.notifyStateChanged();
                if (onItemsChanged != null) onItemsChanged.run();
            });

            // Hide Zone field as per USER request (prioritize details)
            Label lblZonePrefix = new Label(" Zona:");
            lblZonePrefix.setManaged(false);
            lblZonePrefix.setVisible(false);
            txtZone.setManaged(false);
            txtZone.setVisible(false);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(lblNum, txtLabel, lblZonePrefix, txtZone, spacer, btnRemove);

            TextArea txtDesc = new TextArea(data.getDescription());
            txtDesc.setPrefRowCount(2);
            txtDesc.setWrapText(true);
            txtDesc.setPromptText("Detalles técnicos de esta referencia...");
            txtDesc.setMaxWidth(Double.MAX_VALUE);
            // Remove JavaFX default background shadow and specify clean borders
            txtDesc.setStyle("-fx-background-color: transparent; -fx-control-inner-background: white; -fx-border-color: #bdc3c7; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 2;");
            // OPTIMIZATION: Use focus-out instead of immediate text listener to avoid typing lag
            txtDesc.focusedProperty().addListener((obs, old, focused) -> {
                if (!focused) {
                    data.setDescription(txtDesc.getText());
                    layer.updateVisuals();
                    visualizer.notifyStateChanged();
                }
            });

            // IMPROVEMENT: Auto-select text on click to speed up rewriting/editing
            txtDesc.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) txtDesc.selectAll();
            });

            Button btnImg = new Button(" Cargar Imagen");
            try {
                btnImg.setGraphic(UIFactory.crearIcono("mdi2i-image-plus", 16, "white"));
            } catch (Exception e) {}
            btnImg.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 6;");
            
            // Set initial state based on if there's an image
            if (data.getImagePath() != null && !data.getImagePath().isEmpty()) {
                btnImg.setText(" Imagen Cargada");
                btnImg.setStyle("-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 6;");
                try {
                    btnImg.setGraphic(UIFactory.crearIcono("mdi2c-check-circle", 16, "white"));
                } catch (Exception e) {}
            }
            
            btnImg.setMaxWidth(Double.MAX_VALUE);
            Button btnCrop = new Button(" Recortar");
            try {
                btnCrop.setGraphic(UIFactory.crearIcono("mdi2c-crop", 14, "white"));
            } catch (Exception e) {}
            btnCrop.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 6;");
            btnCrop.setMaxWidth(Double.MAX_VALUE);
            btnCrop.setManaged(data.getImagePath() != null || data.getImageData() != null);
            btnCrop.setVisible(data.getImagePath() != null || data.getImageData() != null);

            btnCrop.setOnAction(e -> {
                try {
                    Image currentImg = null;
                    if (data.getImageData() != null && data.getImageData().length > 0) {
                        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data.getImageData())) {
                            currentImg = new Image(bais);
                        }
                    } else if (data.getImagePath() != null) {
                        currentImg = new Image(data.getImagePath());
                    }

                    if (currentImg != null) {
                        ImageCropperDialog cropper = new ImageCropperDialog(currentImg);
                        cropper.initOwner(btnCrop.getScene().getWindow());
                        java.util.Optional<Image> result = cropper.showAndWait();
                        if (result.isPresent()) {
                            Image cropped = result.get();
                            // Convert to byte[]
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(SwingFXUtils.fromFXImage(cropped, null), "png", baos);
                            data.setImageData(baos.toByteArray());
                            layer.updateVisuals();
                            visualizer.notifyStateChanged();
                            if (onItemsChanged != null) onItemsChanged.run();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    UIFactory.mostrarAlerta(Alert.AlertType.ERROR, "Error al recortar", "No se pudo procesar el recorte de la imagen.");
                }
            });

            btnImg.setOnAction(e -> {
                javafx.application.Platform.runLater(() -> {
                    try {
                        FileChooser fc = new FileChooser();
                        fc.setTitle("Seleccionar Imagen de Referencia");
                        fc.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                            new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
                        );
                        
                        File initialDir = new File(System.getProperty("user.home"));
                        if (initialDir.exists()) {
                            fc.setInitialDirectory(initialDir);
                        }

                        javafx.stage.Window owner = btnImg.getScene().getWindow();
                        File f = fc.showOpenDialog(owner);
                        if (f != null) {
                            String path = f.toURI().toString();
                            data.setImagePath(path);
                            
                            // Visual indication
                            btnImg.setText(" Imagen Cargada");
                            btnImg.setStyle("-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 6;");
                            try {
                                btnImg.setGraphic(UIFactory.crearIcono("mdi2c-check-circle", 16, "white"));
                            } catch (Exception ex2) {}
                            
                            btnImg.setTooltip(new Tooltip(f.getName()));

                            // Enable crop button
                            btnCrop.setVisible(true);
                            btnCrop.setManaged(true);

                            // AUTO-TRIGGER CROPPER
                            Image loadedImg = new Image(path);
                            ImageCropperDialog cropper = new ImageCropperDialog(loadedImg);
                            cropper.initOwner(owner);
                            java.util.Optional<Image> result = cropper.showAndWait();
                            if (result.isPresent()) {
                                Image cropped = result.get();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(SwingFXUtils.fromFXImage(cropped, null), "png", baos);
                                data.setImageData(baos.toByteArray());
                            }
                            
                            layer.updateVisuals();
                            visualizer.notifyStateChanged();
                            if (onItemsChanged != null) onItemsChanged.run();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            });

            GridPane actionGrid = new GridPane();
            actionGrid.setHgap(8);
            actionGrid.setMaxWidth(Double.MAX_VALUE);
            ColumnConstraints actionCol1 = new ColumnConstraints();
            actionCol1.setPercentWidth(50);
            actionCol1.setFillWidth(true);
            actionCol1.setHgrow(Priority.ALWAYS);
            ColumnConstraints actionCol2 = new ColumnConstraints();
            actionCol2.setPercentWidth(50);
            actionCol2.setFillWidth(true);
            actionCol2.setHgrow(Priority.ALWAYS);
            actionGrid.getColumnConstraints().addAll(actionCol1, actionCol2);
            actionGrid.add(btnImg, 0, 0);
            actionGrid.add(btnCrop, 1, 0);

            this.getChildren().addAll(header, txtDesc, actionGrid);
        }
    }
}
