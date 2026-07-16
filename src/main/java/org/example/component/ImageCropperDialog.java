package org.example.component;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.KeyCode;
import org.example.utils.UIFactory;

public class ImageCropperDialog extends Dialog<Image> {

    private final Image originalImage;
    private final ImageView imageView;
    private final Rectangle cropRect;
    private final Pane container;
    private final Group zoomGroup;
    private final Pane scrollContent;
    private final ScrollPane sp;


    private final StackPane hTopLeft, hTopRight, hBottomLeft, hBottomRight;

    private double iw_disp, ih_disp;
    private double zoom = 1.0;

    public ImageCropperDialog(Image image) {
        this.originalImage = image;
        this.imageView = new ImageView(image);
        this.imageView.setPreserveRatio(true);
        this.imageView.setSmooth(true);

        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        double targetW = Math.min(700, screen.getWidth()  * 0.65);
        double targetH = Math.min(500, screen.getHeight() * 0.65);
        double iw_orig = image.getWidth();
        double ih_orig = image.getHeight();
        double ratio = (iw_orig > 0 && ih_orig > 0) ? iw_orig / ih_orig : 1.0;

        if (ratio > targetW / targetH) {
            this.imageView.setFitWidth(targetW);
        } else {
            this.imageView.setFitHeight(targetH);
        }

        this.container = new Pane();
        this.container.setPickOnBounds(true);
        this.container.getChildren().add(imageView);

        this.iw_disp = (ratio > targetW / targetH) ? targetW : targetH * ratio;
        this.ih_disp = (ratio > targetW / targetH) ? targetW / ratio : targetH;

        if (iw_disp < 1) iw_disp = 1;
        if (ih_disp < 1) ih_disp = 1;

        this.container.setPrefSize(iw_disp, ih_disp);
        this.container.setMaxSize(iw_disp, ih_disp);

        this.cropRect = new Rectangle(0, 0, 0, 0);
        resetCropRect();

        this.cropRect.setFill(Color.rgb(52, 152, 219, 0.15));
        this.cropRect.setStroke(Color.web("#3498db"));
        this.cropRect.setStrokeWidth(2.0);
        this.cropRect.getStrokeDashArray().addAll(6d, 6d);

        Rectangle dimTop = new Rectangle();
        Rectangle dimBottom = new Rectangle();
        Rectangle dimLeft = new Rectangle();
        Rectangle dimRight = new Rectangle();
        Color dimColor = Color.rgb(0, 0, 0, 0.7);
        dimTop.setFill(dimColor); dimBottom.setFill(dimColor);
        dimLeft.setFill(dimColor); dimRight.setFill(dimColor);

        container.getChildren().addAll(dimTop, dimBottom, dimLeft, dimRight, cropRect);

        hTopLeft = createHandle(Cursor.NW_RESIZE);
        hTopRight = createHandle(Cursor.NE_RESIZE);
        hBottomLeft = createHandle(Cursor.SW_RESIZE);
        hBottomRight = createHandle(Cursor.SE_RESIZE);

        container.getChildren().addAll(hTopLeft, hTopRight, hBottomLeft, hBottomRight);

        setupInteractions(dimTop, dimBottom, dimLeft, dimRight);
        updateVisuals(dimTop, dimBottom, dimLeft, dimRight);

        // --- Zoom Group ---
        this.zoomGroup = new Group(container);

        // --- Scroll Content ---
        this.scrollContent = new Pane(zoomGroup);
        scrollContent.setStyle("-fx-background-color: #2c3e50;");

        this.sp = new ScrollPane(scrollContent);
        double spW = Math.min(780, screen.getWidth()  * 0.72);
        double spH = Math.min(580, screen.getHeight() * 0.68);
        sp.setPrefSize(spW, spH);
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: #bdc3c7;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        // --- Zoom: mouse wheel ---
        scrollContent.setOnScroll(e -> {
            double f = e.getDeltaY() > 0 ? 1.15 : 0.87;
            double nz = Math.max(0.5, Math.min(5.0, zoom * f));
            if (nz == zoom) return;

            double mx = e.getX();
            double my = e.getY();
            double imgX = mx / zoom;
            double imgY = my / zoom;

            zoom = nz;
            applyZoom();

            Platform.runLater(() -> {
                double vw = sp.getViewportBounds().getWidth();
                double vh = sp.getViewportBounds().getHeight();
                double cw = iw_disp * zoom;
                double ch = ih_disp * zoom;

                if (cw > vw) {
                    double hTarget = (imgX * zoom - mx) / (cw - vw);
                    sp.setHvalue(Math.max(0, Math.min(1, hTarget)));
                }
                if (ch > vh) {
                    double vTarget = (imgY * zoom - my) / (ch - vh);
                    sp.setVvalue(Math.max(0, Math.min(1, vTarget)));
                }
            });

            e.consume();
        });

        // --- ROOT UI ---
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-color: #dee2e6; -fx-border-radius: 12;");
        root.setAlignment(Pos.CENTER);

        Label lblHint = new Label("RECORTAR IMAGEN DE REFERENCIA");
        lblHint.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #2c3e50;");

        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER);

        String btnFooterStyle = "-fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 14px;";

        Button btnReset = new Button("RESTAURAR");
        btnReset.setGraphic(UIFactory.crearIcono("mdi2r-restore", 16, "white"));
        btnReset.setMinWidth(180);
        btnReset.setPrefWidth(180);
        btnReset.setMaxWidth(180);
        btnReset.setStyle("-fx-background-color: #c0392b;" + btnFooterStyle);
        btnReset.setOnAction(e -> {
            resetCropRect();
            updateVisuals(dimTop, dimBottom, dimLeft, dimRight);
        });

        Button btnCrop = new Button("ACEPTAR");
        btnCrop.setGraphic(UIFactory.crearIcono("mdi2c-check", 18, "white"));
        btnCrop.setMinWidth(180);
        btnCrop.setPrefWidth(180);
        btnCrop.setMaxWidth(180);
        btnCrop.setStyle("-fx-background-color: #27ae60; -fx-font-weight: 900;" + btnFooterStyle);
        btnCrop.setOnAction(e -> confirmCrop());

        Button btnCancel = new Button("CANCELAR");
        btnCancel.setMinWidth(180);
        btnCancel.setPrefWidth(180);
        btnCancel.setMaxWidth(180);
        btnCancel.setStyle("-fx-background-color: #7f8c8d;" + btnFooterStyle);
        btnCancel.setOnAction(e -> {
            setResult(null);
            this.close();
        });

        footer.getChildren().addAll(btnReset, btnCrop, btnCancel);

        root.getChildren().addAll(lblHint, sp, footer);

        getDialogPane().setContent(root);
        UIFactory.estilizarDialogo(this);
        getDialogPane().getScene().setFill(Color.TRANSPARENT);

        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        javafx.scene.Node closeBtn = getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setManaged(false);
            closeBtn.setVisible(false);
        }

        this.getDialogPane().getScene().setOnKeyPressed(k -> {
            boolean ctrl = k.isControlDown() || k.isShortcutDown();
            if (k.getCode() == KeyCode.ENTER) confirmCrop();
            else if (k.getCode() == KeyCode.ESCAPE) {
                setResult(null);
                this.close();
            }
            else if (k.getCode() == KeyCode.EQUALS && ctrl) { zoom = Math.min(5.0, zoom * 1.3); applyZoom(); }
            else if (k.getCode() == KeyCode.MINUS && ctrl) { zoom = Math.max(0.5, zoom / 1.3); applyZoom(); }
            else if (k.getCode() == KeyCode.DIGIT0 && ctrl) resetZoom();
        });

        setResultConverter(btnType -> null);

        Platform.runLater(this::applyZoom);
    }

    private void resetZoom() {
        zoom = 1.0;
        applyZoom();
    }

    private void applyZoom() {
        zoomGroup.getTransforms().setAll(new javafx.scene.transform.Scale(zoom, zoom));

        double vw = sp.getViewportBounds().getWidth();
        double vh = sp.getViewportBounds().getHeight();

        double cw = iw_disp * zoom;
        double ch = ih_disp * zoom;

        if (vw > 0) cw = Math.max(vw, cw);
        if (vh > 0) ch = Math.max(vh, ch);

        scrollContent.setPrefSize(cw, ch);

        zoomGroup.setLayoutX((cw - iw_disp * zoom) / 2);
        zoomGroup.setLayoutY((ch - ih_disp * zoom) / 2);
    }

    private void closeDialog() {
        this.close();
    }

    private void resetCropRect() {
        this.cropRect.setX(iw_disp * 0.1);
        this.cropRect.setY(ih_disp * 0.1);
        this.cropRect.setWidth(iw_disp * 0.8);
        this.cropRect.setHeight(ih_disp * 0.8);
    }

    private StackPane createHandle(Cursor cursor) {
        StackPane handle = new StackPane();
        handle.setPrefSize(16, 16);
        handle.setMaxSize(16, 16);
        handle.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 2.5;");
        handle.setCursor(cursor);
        return handle;
    }

    private void setupInteractions(Rectangle dt, Rectangle db, Rectangle dl, Rectangle dr) {
        final double[] startPos = new double[4];
        cropRect.setCursor(Cursor.MOVE);

        cropRect.setOnMousePressed(e -> {
            startPos[0] = e.getSceneX();
            startPos[1] = e.getSceneY();
            startPos[2] = cropRect.getX();
            startPos[3] = cropRect.getY();
            e.consume();
        });

        cropRect.setOnMouseDragged(e -> {
            double dx = (e.getSceneX() - startPos[0]) / zoom;
            double dy = (e.getSceneY() - startPos[1]) / zoom;

            double nx = Math.max(0, Math.min(iw_disp - cropRect.getWidth(), startPos[2] + dx));
            double ny = Math.max(0, Math.min(ih_disp - cropRect.getHeight(), startPos[3] + dy));

            cropRect.setX(nx);
            cropRect.setY(ny);
            updateVisuals(dt, db, dl, dr);
            e.consume();
        });

        setupHandle(hTopLeft, true, true, dt, db, dl, dr);
        setupHandle(hTopRight, false, true, dt, db, dl, dr);
        setupHandle(hBottomLeft, true, false, dt, db, dl, dr);
        setupHandle(hBottomRight, false, false, dt, db, dl, dr);
    }

    private void setupHandle(StackPane h, boolean left, boolean top, Rectangle dt, Rectangle db, Rectangle dl, Rectangle dr) {
        final double[] startPos = new double[6];

        h.setOnMousePressed(e -> {
            startPos[0] = e.getSceneX();
            startPos[1] = e.getSceneY();
            startPos[2] = cropRect.getX();
            startPos[3] = cropRect.getY();
            startPos[4] = cropRect.getWidth();
            startPos[5] = cropRect.getHeight();
            e.consume();
        });

        h.setOnMouseDragged(e -> {
            double dx = (e.getSceneX() - startPos[0]) / zoom;
            double dy = (e.getSceneY() - startPos[1]) / zoom;

            double nx = startPos[2];
            double ny = startPos[3];
            double nw = startPos[4];
            double nh = startPos[5];

            if (left) {
                double safeDx = Math.max(-nx, Math.min(nw - 20, dx));
                cropRect.setX(nx + safeDx);
                cropRect.setWidth(nw - safeDx);
            } else {
                double safeDx = Math.max(20 - nw, Math.min(iw_disp - (nx + nw), dx));
                cropRect.setWidth(nw + safeDx);
            }

            if (top) {
                double safeDy = Math.max(-ny, Math.min(nh - 20, dy));
                cropRect.setY(ny + safeDy);
                cropRect.setHeight(nh - safeDy);
            } else {
                double safeDy = Math.max(20 - nh, Math.min(ih_disp - (ny + nh), dy));
                cropRect.setHeight(nh + safeDy);
            }

            updateVisuals(dt, db, dl, dr);
            e.consume();
        });
    }

    private void updateVisuals(Rectangle dt, Rectangle db, Rectangle dl, Rectangle dr) {
        double x = cropRect.getX();
        double y = cropRect.getY();
        double w = cropRect.getWidth();
        double h = cropRect.getHeight();

        dt.setX(0); dt.setY(0); dt.setWidth(iw_disp); dt.setHeight(y);
        db.setX(0); db.setY(y + h); db.setWidth(iw_disp); db.setHeight(ih_disp - (y + h));
        dl.setX(0); dl.setY(y); dl.setWidth(x); dl.setHeight(h);
        dr.setX(x + w); dr.setY(y); dr.setWidth(iw_disp - (x + w)); dr.setHeight(h);

        hTopLeft.setLayoutX(x - 8); hTopLeft.setLayoutY(y - 8);
        hTopRight.setLayoutX(x + w - 8); hTopRight.setLayoutY(y - 8);
        hBottomLeft.setLayoutX(x - 8); hBottomLeft.setLayoutY(y + h - 8);
        hBottomRight.setLayoutX(x + w - 8); hBottomRight.setLayoutY(y + h - 8);
    }

    private void confirmCrop() {
        double iw_img = originalImage.getWidth();
        double ih_img = originalImage.getHeight();

        double scaleX = iw_img / iw_disp;
        double scaleY = ih_img / ih_disp;

        int x = (int) Math.round(cropRect.getX() * scaleX);
        int y = (int) Math.round(cropRect.getY() * scaleY);
        int w = (int) Math.round(cropRect.getWidth() * scaleX);
        int h = (int) Math.round(cropRect.getHeight() * scaleY);

        x = Math.max(0, Math.min((int)iw_img - 1, x));
        y = Math.max(0, Math.min((int)ih_img - 1, y));
        w = Math.max(1, Math.min((int)iw_img - x, w));
        h = Math.max(1, Math.min((int)ih_img - y, h));

        try {
            PixelReader reader = originalImage.getPixelReader();
            setResult(new WritableImage(reader, x, y, w, h));
            closeDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
