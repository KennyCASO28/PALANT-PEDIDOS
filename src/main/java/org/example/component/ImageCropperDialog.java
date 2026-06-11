package org.example.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.KeyCode;
import org.example.utils.UIFactory;

/**
 * Improved Image Cropper Dialog.
 * Fixed alignment issues using Pane and corrected Cancel button behavior.
 */
public class ImageCropperDialog extends Dialog<Image> {

    private final Image originalImage;
    private final ImageView imageView;
    private final Rectangle cropRect;
    private final Pane container; // Changed to Pane for exact absolute positioning
    
    // Handles
    private final StackPane hTopLeft, hTopRight, hBottomLeft, hBottomRight;
    
    // Logic state
    private double iw_disp, ih_disp;

    public ImageCropperDialog(Image image) {
        this.originalImage = image;
        this.imageView = new ImageView(image);
        this.imageView.setPreserveRatio(true);
        this.imageView.setSmooth(true);
        
        // --- SCALE FOR DIALOG (Optimized size) ---
        double targetW = 700;
        double targetH = 500;
        double iw_orig = image.getWidth();
        double ih_orig = image.getHeight();
        double ratio = iw_orig / ih_orig;

        if (ratio > targetW / targetH) {
            this.imageView.setFitWidth(targetW);
        } else {
            this.imageView.setFitHeight(targetH);
        }

        // --- CONTAINER PANE ---
        this.container = new Pane(); 
        this.container.setPickOnBounds(true);
        this.container.getChildren().add(imageView);
        
        // Calculate visual bounds exactly
        this.iw_disp = (ratio > targetW / targetH) ? targetW : targetH * ratio;
        this.ih_disp = (ratio > targetW / targetH) ? targetW / ratio : targetH;
        
        // Ensure container is exact size of image
        this.container.setPrefSize(iw_disp, ih_disp);
        this.container.setMaxSize(iw_disp, ih_disp);

        this.cropRect = new Rectangle(0, 0, 0, 0); 
        resetCropRect();

        this.cropRect.setFill(Color.rgb(52, 152, 219, 0.15));
        this.cropRect.setStroke(Color.web("#3498db"));
        this.cropRect.setStrokeWidth(2.0);
        this.cropRect.getStrokeDashArray().addAll(6d, 6d);
        
        // Background dimming
        Rectangle dimTop = new Rectangle();
        Rectangle dimBottom = new Rectangle();
        Rectangle dimLeft = new Rectangle();
        Rectangle dimRight = new Rectangle();
        Color dimColor = Color.rgb(0, 0, 0, 0.7); 
        dimTop.setFill(dimColor); dimBottom.setFill(dimColor); 
        dimLeft.setFill(dimColor); dimRight.setFill(dimColor);

        container.getChildren().addAll(dimTop, dimBottom, dimLeft, dimRight, cropRect);

        // Resize Handles (Absolute position nodes)
        hTopLeft = createHandle(Cursor.NW_RESIZE);
        hTopRight = createHandle(Cursor.NE_RESIZE);
        hBottomLeft = createHandle(Cursor.SW_RESIZE);
        hBottomRight = createHandle(Cursor.SE_RESIZE);
        
        container.getChildren().addAll(hTopLeft, hTopRight, hBottomLeft, hBottomRight);

        setupInteractions(dimTop, dimBottom, dimLeft, dimRight);
        updateVisuals(dimTop, dimBottom, dimLeft, dimRight);

        // --- ROOT UI ---
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-color: #dee2e6; -fx-border-radius: 12;");
        root.setAlignment(Pos.CENTER);
        
        Label lblHint = new Label("RECORTAR IMAGEN DE REFERENCIA");
        lblHint.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #2c3e50;");

        // Centering Wrapper
        StackPane scrollContent = new StackPane(container);
        scrollContent.setPadding(new Insets(30));
        scrollContent.setStyle("-fx-background-color: #2c3e50;");

        ScrollPane sp = new ScrollPane(scrollContent);
        sp.setPrefSize(780, 580);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: #bdc3c7;");

        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER);
        
        Button btnReset = new Button("RESTAURAR");
        btnReset.setGraphic(UIFactory.crearIcono("mdi2r-restore", 16, "white"));
        btnReset.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8;");
        btnReset.setOnAction(e -> {
            resetCropRect();
            updateVisuals(dimTop, dimBottom, dimLeft, dimRight);
        });

        Button btnCrop = new Button("ACEPTAR");
        btnCrop.setGraphic(UIFactory.crearIcono("mdi2c-check", 18, "white"));
        btnCrop.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 10 40; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 14px;");
        btnCrop.setOnAction(e -> confirmCrop());
        
        Button btnCancel = new Button("CANCELAR");
        btnCancel.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 14px;");
        btnCancel.setOnAction(e -> {
            setResult(null);
            this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            this.close();
        });

        footer.getChildren().addAll(btnReset, btnCrop, btnCancel);

        root.getChildren().addAll(lblHint, sp, footer);
        
        getDialogPane().setContent(root);
        UIFactory.estilizarDialogo(this);
        getDialogPane().getScene().setFill(Color.TRANSPARENT);

        // --- FIX: Standard Dialog ButtonTypes are required for ESC and X to work correctly ---
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        javafx.scene.Node closeBtn = getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setManaged(false);
            closeBtn.setVisible(false);
        }

        this.getDialogPane().getScene().setOnKeyPressed(k -> {
            if (k.getCode() == KeyCode.ENTER) confirmCrop();
            if (k.getCode() == KeyCode.ESCAPE) {
                setResult(null);
                this.close();
            }
        });

        setResultConverter(btnType -> null);
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
        // Make sure it's square to match the user's perception of "points" if they prefer, or clean circles.
        // User mentioned "puntos cuadrados", so let's stick to clean squares or well-aligned circles.
        handle.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 2.5;");
        handle.setCursor(cursor);
        return handle;
    }

    private void setupInteractions(Rectangle dt, Rectangle db, Rectangle dl, Rectangle dr) {
        final double[] startPos = new double[4]; // [sceneX, sceneY, initialRectX, initialRectY]
        cropRect.setCursor(Cursor.MOVE);
        
        cropRect.setOnMousePressed(e -> {
            startPos[0] = e.getSceneX();
            startPos[1] = e.getSceneY();
            startPos[2] = cropRect.getX();
            startPos[3] = cropRect.getY();
            e.consume();
        });

        cropRect.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - startPos[0];
            double dy = e.getSceneY() - startPos[1];
            
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
        final double[] startPos = new double[6]; // [sceneX, sceneY, initialRectX, initialRectY, initialRectW, initialRectH]

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
            double dx = e.getSceneX() - startPos[0];
            double dy = e.getSceneY() - startPos[1];
            
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

        // Position handles exactly on corners using layoutX/Y in Pane
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

