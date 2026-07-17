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
import javafx.scene.control.Alert;
import javafx.application.Platform;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import org.example.component.helper.VectorBooleanHelper;

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
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes y Vectores", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.svg", "*.pdf", "*.cdr"));
        List<File> files = fc.showOpenMultipleDialog(container.getScene().getWindow());

        if (files != null) {
            for (File f : files) {
                try {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".svg") || name.endsWith(".pdf") || name.endsWith(".cdr")) {
                        processVectorFileAsync(f);
                    } else {
                        Image img = new Image(f.toURI().toString(), 800, 800, true, true);
                        addToGallery(img, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processVectorFileAsync(File f) {
        new Thread(() -> {
            try {
                File svgFile = f;
                if (!f.getName().toLowerCase().endsWith(".svg")) {
                    svgFile = File.createTempFile("converted_vector_", ".svg");
                    svgFile.deleteOnExit();
                    
                    String inkscapePath = "inkscape";
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        // 1. Buscar versión portable empaquetada (Recomendado)
                        File bundled = new File(System.getProperty("user.dir"), "tools\\inkscape\\bin\\inkscape.exe");
                        // 2. Buscar versión de sistema
                        File pf = new File("C:\\Program Files\\Inkscape\\bin\\inkscape.exe");
                        
                        if (bundled.exists()) {
                            inkscapePath = bundled.getAbsolutePath();
                        } else if (pf.exists()) {
                            inkscapePath = pf.getAbsolutePath();
                        }
                    }
                    
                    ProcessBuilder pb = new ProcessBuilder(
                        inkscapePath, 
                        "--export-type=svg", 
                        "--export-plain-svg", 
                        "--export-filename=" + svgFile.getAbsolutePath(), 
                        f.getAbsolutePath()
                    );
                    
                    Process p;
                    try {
                        p = pb.start();
                    } catch (java.io.IOException e) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Conversor de Vectores Ausente");
                            alert.setHeaderText("Falta el motor Inkscape Portable");
                            alert.setContentText("Para que el sistema sea independiente, debes empaquetar Inkscape Portable.\nCrea una carpeta llamada 'tools' junto a tu proyecto, y dentro coloca la carpeta 'inkscape'.\nEl sistema buscará el archivo en: tools\\inkscape\\bin\\inkscape.exe");
                            alert.showAndWait();
                        });
                        return;
                    }
                    
                    int exitCode = p.waitFor();
                    if (exitCode != 0) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error de Conversión");
                            alert.setHeaderText("Fallo al convertir archivo a SVG");
                            alert.setContentText("Asegúrate de que Inkscape está instalado y en el PATH del sistema. Código: " + exitCode);
                            alert.showAndWait();
                        });
                        return;
                    }
                }
                
                // Parse the SVG to create a thumbnail
                java.util.List<org.example.utils.SVGUtils.ParsedSVGShape> shapes = org.example.utils.SVGUtils.parseComplexSvg(svgFile);
                if (shapes.isEmpty()) return;
                
                javafx.scene.Group tempGroup = new javafx.scene.Group();
                for (org.example.utils.SVGUtils.ParsedSVGShape ps : shapes) {
                    javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
                    path.setContent(ps.pathData);
                    boolean hasNoFill = ps.fill == null || ps.fill.isEmpty() || ps.fill.equals("none");
                    boolean hasNoStroke = ps.stroke == null || ps.stroke.isEmpty() || ps.stroke.equals("none") || ps.stroke.equals("null");
                    double sw = ps.strokeWidth != null && !ps.strokeWidth.equals("none") && !ps.strokeWidth.isEmpty() && !ps.strokeWidth.equals("null") ? Double.parseDouble(ps.strokeWidth.replaceAll("[a-zA-Z]", "")) : 0.0;
                    javafx.scene.paint.Color thumbFill = org.example.utils.SVGUtils.getSafeColor(ps.fill, javafx.scene.paint.Color.BLACK);
                    if (hasNoFill && (hasNoStroke || sw <= 0)) {
                        thumbFill = javafx.scene.paint.Color.BLACK;
                    }
                    path.setFill(thumbFill);
                    if (ps.stroke != null && !ps.stroke.equals("none")) {
                        path.setStroke(org.example.utils.SVGUtils.getSafeColor(ps.stroke, javafx.scene.paint.Color.TRANSPARENT));
                        path.setStrokeWidth(ps.strokeWidth != null && !ps.strokeWidth.equals("none") ? Double.parseDouble(ps.strokeWidth.replaceAll("[a-zA-Z]", "")) : 0.0);
                    }
                    String fillRuleStr = (ps.fillRule != null && !ps.fillRule.isEmpty()) ? ps.fillRule : "evenodd";
                    if ("evenodd".equalsIgnoreCase(fillRuleStr)) {
                        path.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
                    }
                    org.example.utils.SVGUtils.applySVGTransform(path, ps.transform);
                    tempGroup.getChildren().add(path);
                }
                
                final File finalSvgFile = svgFile;
                Platform.runLater(() -> {
                    javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                    params.setFill(javafx.scene.paint.Color.TRANSPARENT);
                    double maxDim = Math.max(tempGroup.getBoundsInLocal().getWidth(), tempGroup.getBoundsInLocal().getHeight());
                    if (maxDim > 0) {
                        double scale = 150 / maxDim;
                        params.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
                    }
                    Image thumbnail = tempGroup.snapshot(params, null);
                    addToGallery(thumbnail, finalSvgFile);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addToGallery(Image img, File vectorFile) {
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
                if (vectorFile != null) {
                    // Import individual shapes separately to avoid path fusion and coordinate corruption
                    java.util.List<org.example.utils.SVGUtils.ParsedSVGShape> shapes = org.example.utils.SVGUtils.parseComplexSvg(vectorFile);
                    if (!shapes.isEmpty()) {
                        java.util.List<javafx.scene.Node> newLayers = new java.util.ArrayList<>();
                        for (org.example.utils.SVGUtils.ParsedSVGShape ps : shapes) {
                            String fillColor = ps.fill;
                            String strokeColor = ps.stroke;
                            String strokeW = ps.strokeWidth;
                            String transformStr = ps.transform;
                            String fillRuleStr = (ps.fillRule != null && !ps.fillRule.isEmpty()) ? ps.fillRule : "evenodd";

                            // Bake SVG transform into geometry via Shape.union (exactamente como PrendaLayerFactory)
                            javafx.scene.shape.SVGPath tempSvg = new javafx.scene.shape.SVGPath();
                            tempSvg.setContent(ps.pathData);
                            if ("evenodd".equalsIgnoreCase(fillRuleStr)) {
                                tempSvg.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
                            }
                            if (transformStr != null && !transformStr.isEmpty()) {
                                org.example.utils.SVGUtils.applySVGTransform(tempSvg, transformStr);
                            }
                            javafx.scene.shape.Path fxPath = (javafx.scene.shape.Path) javafx.scene.shape.Shape.union(tempSvg, new javafx.scene.shape.Path());
                            if ("evenodd".equalsIgnoreCase(fillRuleStr)) {
                                fxPath.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
                            }

                            // Inkscape exporta CDR con fill:none y sin stroke; aplicar negro como default
                            boolean hasNoFill = fillColor == null || fillColor.isEmpty() || fillColor.equals("none");
                            boolean hasNoStroke = strokeColor == null || strokeColor.isEmpty() || strokeColor.equals("none") || strokeColor.equals("null");
                            double sw = strokeW != null && !strokeW.equals("none") && !strokeW.isEmpty() && !strokeW.equals("null") ? Double.parseDouble(strokeW.replaceAll("[a-zA-Z]", "")) : 0.0;
                            javafx.scene.paint.Color fill = org.example.utils.SVGUtils.getSafeColor(fillColor, javafx.scene.paint.Color.BLACK);
                            if (hasNoFill && (hasNoStroke || sw <= 0)) {
                                fill = javafx.scene.paint.Color.BLACK;
                            }
                            org.example.component.ShapeLayer sl = org.example.component.helper.VectorBooleanHelper.createShapeLayerFromPath(fxPath,
                                fill,
                                org.example.utils.SVGUtils.getSafeColor(strokeColor, javafx.scene.paint.Color.TRANSPARENT),
                                sw);

                            if (sl == null) continue;
                            visualizer.getLayerFactory().addShapeLayer(sl);
                            newLayers.add(sl);
                        }
                        visualizer.getLayerManager().clearSelection();
                        newLayers.forEach(visualizer.getLayerManager()::addToSelection);
                        visualizer.getLayerManager().groupSelected();
                    }
                } else {
                    org.example.component.ImageLayer il = new org.example.component.ImageLayer(img);
                    visualizer.getLayerFactory().addImageLayer(il);
                }
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
            if (vectorFile != null) {
                content.putFiles(java.util.Collections.singletonList(vectorFile));
            } else {
                content.putImage(img);
            }
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
                addToGallery(img, null);
            }
        }
    }
}
