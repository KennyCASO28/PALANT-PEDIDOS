package org.example.controller.uicomponent;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.component.PrendaVisualizer;
import org.example.utils.UIFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogoManagerController {

    private final PrendaVisualizer visualizer;
    private final VBox container;
    private final FlowPane gallery;
    private final List<Image> uploadedImages = new ArrayList<>();
    private VBox emptyState;
    private ScrollPane scroll;

    // Gallery selection tracking
    private Image selectedGalleryImage = null;
    private VBox selectedGalleryBox = null;

    public LogoManagerController(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
        this.container = new VBox(10);
        this.gallery = new FlowPane(10, 10);

        setupUI();
    }

    private void setupUI() {
        container.setPadding(new javafx.geometry.Insets(10));
        container.setFillWidth(true);

        Label title = UIFactory.crearTituloSeccion("Galería de Logos");

        // Compact HBox to fit all 3 buttons in one row
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnUpload = createAnimatedButton("Subir", "mdi2u-upload", "#2ecc71", "white");
        btnUpload.setTooltip(new Tooltip("Subir nuevos logos"));
        btnUpload.setOnAction(e -> uploadImage());

        Button btnDeleteSel = createAnimatedButton("Borrar", "mdi2t-trash-can-outline", "#ef4444", "white");
        btnDeleteSel.setTooltip(new Tooltip("Borrar logo seleccionado"));
        btnDeleteSel.setOnAction(e -> {
            if (selectedGalleryBox != null) {
                gallery.getChildren().remove(selectedGalleryBox);
                uploadedImages.remove(selectedGalleryImage);
                selectedGalleryBox = null;
                selectedGalleryImage = null;
                checkEmptyState();
            }
        });

        Button btnClear = createAnimatedButton("Limpiar", "mdi2e-eraser", "#f1f5f9", "#475569");
        btnClear.setStyle(btnClear.getStyle() + " -fx-border-color: #cbd5e1; -fx-border-radius: 6;");
        btnClear.setTooltip(new Tooltip("Borrar toda la galería"));
        btnClear.setOnAction(e -> clearAll());

        actions.getChildren().addAll(btnUpload, btnDeleteSel, btnClear);

        // Gallery Area (Professional Styling)
        gallery.setBackground(null);
        gallery.setPadding(new javafx.geometry.Insets(15));
        gallery.setHgap(15);
        gallery.setVgap(15);

        scroll = new ScrollPane(gallery);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefHeight(450);
        scroll.setMinHeight(300);
        // Ensure ScrollPane doesn't have its own border now to avoid double borders
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0 12 0 0;");

        // Professional Header Card (Grouping title, actions and hint)
        VBox headerCard = new VBox(12);
        headerCard.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc); " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 12; " +
                "-fx-background-radius: 12; -fx-padding: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 2);");
        headerCard.setMaxWidth(Double.MAX_VALUE);

        Label hint = new Label("Arrastra logos al diseño para usarlos:");
        hint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 500;");

        headerCard.getChildren().addAll(title, actions, hint);

        // EMPTY STATE
        emptyState = new VBox(15);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new javafx.geometry.Insets(40));
        javafx.scene.Node emptyIcon = UIFactory.crearIcono("mdi2i-image-multiple-outline", 48, "#334155");
        Label emptyLabel = new Label("Galería de Activos");
        emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label emptySub = new Label("Sube logos para visualizarlos aquí");
        emptySub.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        emptyState.getChildren().addAll(emptyIcon, emptyLabel, emptySub);

        javafx.scene.layout.StackPane scrollStack = new javafx.scene.layout.StackPane(scroll, emptyState);
        scrollStack.setStyle("-fx-background-color: #f8fafc; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        scrollStack.setClip(null);
        scrollStack.setMaxWidth(Double.MAX_VALUE);

        container.getChildren().addAll(headerCard, scrollStack);
        checkEmptyState();
    }

    private Button createAnimatedButton(String text, String icon, String bgColor, String textColor) {
        Button btn = new Button(text);
        btn.setGraphic(UIFactory.crearIcono(icon, 16, textColor));
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 12; -fx-background-radius: 8; -fx-font-size: 12px; -fx-pref-width: 105;",
                bgColor, textColor));

        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(100),
                btn);
        btn.setOnMouseEntered(e -> {
            st.stop();
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        btn.setOnMouseExited(e -> {
            st.stop();
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        return btn;
    }

    private void checkEmptyState() {
        boolean isEmpty = uploadedImages.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        scroll.setVisible(!isEmpty);
        scroll.setManaged(!isEmpty);
    }

    private void uploadImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        List<File> files = fc.showOpenMultipleDialog(container.getScene().getWindow());

        if (files != null) {
            for (File f : files) {
                try {
                    // OPTIMIZATION: Limit texture memory payload per image (max 2000px, preserve
                    // ratio, smooth scaling)
                    Image img = new Image(f.toURI().toString(), 800, 800, true, true);
                    addToGallery(img);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addToGallery(Image img) {
        uploadedImages.add(img);

        ImageView thumb = new ImageView(img);
        thumb.setFitWidth(70);
        thumb.setFitHeight(70);
        thumb.setPreserveRatio(true);
        thumb.setSmooth(true);

        VBox box = new VBox(thumb);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(90, 90); // Slightly larger for better "cuadro" container look

        // Dynamic Styles for Thumbnails - Darker Grey Grid to highlight white PNGs
        String transparencyGrid = "linear-gradient(from 0px 0px to 8px 8px, repeat, #475569 0%, #475569 25%, #334155 25%, #334155 50%, #475569 50%, #475569 75%, #334155 75%, #334155 100%)";

        String baseStyle = "-fx-background-color: " + transparencyGrid
                + "; -fx-padding: 8; -fx-background-radius: 8; -fx-border-color: #1e293b; -fx-border-width: 2; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + transparencyGrid
                + "; -fx-padding: 8; -fx-background-radius: 8; -fx-border-color: #38bdf8; -fx-border-width: 2; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(56,189,248,0.4), 10, 0, 0, 0);";
        String selectStyle = "-fx-background-color: " + transparencyGrid
                + "; -fx-padding: 8; -fx-background-radius: 8; -fx-border-color: #0ea5e9; -fx-border-width: 3.5; -fx-cursor: hand;";

        box.setStyle(baseStyle);

        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200),
                box);

        box.setOnMouseEntered(e -> {
            if (selectedGalleryBox != box) {
                box.setStyle(hoverStyle);
                st.stop();
                st.setToX(1.08);
                st.setToY(1.08);
                st.play();
            }
        });
        box.setOnMouseExited(e -> {
            if (selectedGalleryBox != box) {
                box.setStyle(baseStyle);
                st.stop();
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            }
        });

        // Click to Select / Double Click to Insert
        box.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                // Double Click: Insert directly to canvas
                org.example.component.ImageLayer il = new org.example.component.ImageLayer(img);
                visualizer.getLayerFactory().addImageLayer(il);
                e.consume();
                return;
            }

            // Deselect all others
            gallery.getChildren().forEach(node -> {
                if (node instanceof VBox) {
                    ((VBox) node).setStyle(baseStyle);
                }
            });
            // Select this one
            box.setStyle(selectStyle);
            selectedGalleryImage = img;
            selectedGalleryBox = box;
        });

        // Drag to Canvas Logic
        box.setOnDragDetected(e -> {
            Dragboard db = box.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putImage(img);
            db.setContent(content);
            e.consume();
        });

        // Context Menu
        ContextMenu cm = new ContextMenu();
        MenuItem itemRemove = new MenuItem("Eliminar de Galería");
        itemRemove.setOnAction(e -> {
            gallery.getChildren().remove(box);
            uploadedImages.remove(img);
            if (selectedGalleryBox == box) {
                selectedGalleryImage = null;
                selectedGalleryBox = null;
            }
        });
        cm.getItems().add(itemRemove);
        box.setOnContextMenuRequested(e -> cm.show(box, e.getScreenX(), e.getScreenY()));

        gallery.getChildren().add(box);
        checkEmptyState();
    }

    public void clearAll() {
        gallery.getChildren().clear();
        uploadedImages.clear();
        selectedGalleryImage = null;
        selectedGalleryBox = null;
        checkEmptyState();
    }

    public javafx.scene.Node getContainer() {
        return container;
    }

    public List<Image> getLibrary() {
        return new ArrayList<>(uploadedImages);
    }

    public void setLibrary(List<Image> images) {
        clearAll();
        if (images != null) {
            for (Image img : images) {
                addToGallery(img);
            }
        }
    }
}
