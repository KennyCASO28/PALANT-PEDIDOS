package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.example.component.ImageLayer;
import org.example.pattern.ImageContentCommand;

/**
 * Service to handle pixel-level editing for ImageLayer (Paint, Erase).
 * Alleviates ImageLayer from heavy algorithmic and undo-state logic.
 */
public class ImageEditingService {

    private final ImageLayer layer;
    private final Canvas canvas;
    private final GraphicsContext gc;

    // Undo State Holder
    private Image pendingOldImage;
    private double pendingOldW, pendingOldH;
    private double pendingOldTx, pendingOldTy;
    
    private boolean isProcessing = false;

    public ImageEditingService(ImageLayer layer, Canvas canvas) {
        this.layer = layer;
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
    }

    public void paint(double startSceneX, double startSceneY, double endSceneX, double endSceneY, double radius, Color color) {
        try {
            Point2D startLocal = canvas.sceneToLocal(startSceneX, startSceneY);
            Point2D endLocal = canvas.sceneToLocal(endSceneX, endSceneY);

            gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER);
            gc.setStroke(color);
            gc.setLineWidth(radius * 2); // Diameter
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

            gc.strokeLine(startLocal.getX(), startLocal.getY(), endLocal.getX(), endLocal.getY());
            
            layer.setModified(true);
            layer.setSnapshotDirty(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void erase(double startSceneX, double startSceneY, double endSceneX, double endSceneY, double radius) {
        try {
            Point2D startLocal = canvas.sceneToLocal(startSceneX, startSceneY);
            Point2D endLocal = canvas.sceneToLocal(endSceneX, endSceneY);

            double x0 = startLocal.getX();
            double y0 = startLocal.getY();
            double x1 = endLocal.getX();
            double y1 = endLocal.getY();

            double dx = x1 - x0;
            double dy = y1 - y0;
            double dist = Math.sqrt(dx * dx + dy * dy);

            double step = Math.max(1.0, radius / 3.0);
            int count = (int) Math.ceil(dist / step);

            PixelWriter pw = gc.getPixelWriter();
            int w = (int) canvas.getWidth();
            int h = (int) canvas.getHeight();
            int r = (int) Math.ceil(radius);
            int rSq = r * r;

            for (int i = 0; i <= count; i++) {
                double t = (count == 0) ? 0.0 : (double) i / count;
                double cx = x0 + dx * t;
                double cy = y0 + dy * t;

                int centerPixelX = (int) Math.round(cx);
                int centerPixelY = (int) Math.round(cy);

                int minX = Math.max(0, centerPixelX - r);
                int maxX = Math.min(w - 1, centerPixelX + r);
                int minY = Math.max(0, centerPixelY - r);
                int maxY = Math.min(h - 1, centerPixelY + r);

                for (int py = minY; py <= maxY; py++) {
                    for (int px = minX; px <= maxX; px++) {
                        int d2 = (px - centerPixelX) * (px - centerPixelX) + (py - centerPixelY) * (py - centerPixelY);
                        if (d2 <= rSq) {
                            pw.setColor(px, py, Color.TRANSPARENT);
                        }
                    }
                }
            }
            
            layer.setModified(true);
            layer.setSnapshotDirty(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void prepareUndoState(String actionName) {
        this.pendingOldImage = layer.snapshotCanvas();
        this.pendingOldW = layer.getCurrentWidth();
        this.pendingOldH = layer.getCurrentHeight();
        this.pendingOldTx = layer.getTranslateX();
        this.pendingOldTy = layer.getTranslateY();
    }

    public void commitAction(String actionName) {
        if (layer.getVisualizer() != null && layer.getVisualizer().getHistoryManager() != null && pendingOldImage != null) {
            Image newImg = layer.snapshotCanvas();
            double newW = layer.getCurrentWidth();
            double newH = layer.getCurrentHeight();
            double newTx = layer.getTranslateX();
            double newTy = layer.getTranslateY();

            ImageContentCommand cmd = new ImageContentCommand(layer, actionName,
                    pendingOldImage, pendingOldW, pendingOldH, pendingOldTx, pendingOldTy,
                    newImg, newW, newH, newTx, newTy);

            layer.getVisualizer().getHistoryManager().addCommand(cmd);
        }
        this.pendingOldImage = null;
    }
    
    public boolean hasPendingAction() {
        return pendingOldImage != null;
    }
    
    public void setProcessing(boolean p) { this.isProcessing = p; }
    public boolean isProcessing() { return isProcessing; }

    public void removeBackgroundByColor(Image img, Color target, double tolerance, int... seeds) {
        if (img == null || target == null || target.getOpacity() < 0.05 || isProcessing) return; 

        setProcessing(true);
        try {
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            if (w <= 0 || h <= 0) { setProcessing(false); return; }

            PixelReader pr = img.getPixelReader();
            WritableImage out = new WritableImage(w, h);
            PixelWriter pw = out.getPixelWriter();

            // Buffers temporales reutilizables (optimización de GC)
            boolean[] toRemove = new boolean[w * h];
            int[] queue = new int[w * h]; 
            int head = 0, tail = 0;

            int[] actualSeeds = (seeds != null && seeds.length > 0) ? seeds : new int[]{0, w - 1, (h - 1) * w, (h * w) - 1};
            
            for (int seed : actualSeeds) {
                if (seed < 0 || seed >= w*h) continue;
                int sx = seed % w;
                int sy = seed / w;
                if (isColorSimilar(pr.getColor(sx, sy), target, tolerance)) {
                    toRemove[seed] = true;
                    queue[tail++] = seed;
                }
            }

            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};

            while (head < tail) {
                int pos = queue[head++];
                int cx = pos % w;
                int cy = pos / w;

                for (int i = 0; i < 4; i++) {
                    int nx = cx + dx[i];
                    int ny = cy + dy[i];

                    if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                        int npos = ny * w + nx;
                        if (!toRemove[npos]) {
                            if (isColorSimilar(pr.getColor(nx, ny), target, tolerance)) {
                                toRemove[npos] = true;
                                queue[tail++] = npos;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < w * h; i++) {
                int x = i % w;
                int y = i / w;
                if (toRemove[i]) {
                    pw.setColor(x, y, Color.TRANSPARENT);
                } else {
                    pw.setColor(x, y, pr.getColor(x, y));
                }
            }

            // Ejecutar actualización de UI en el hilo principal
            javafx.application.Platform.runLater(() -> {
                gc.clearRect(0, 0, w, h);
                gc.drawImage(out, 0, 0);
                layer.setModified(true);
                layer.setSnapshotDirty(true);
                commitAction("Quitar Fondo");
                trimWhitespace();
                setProcessing(false);
            });
        } catch (Exception e) {
            e.printStackTrace();
            setProcessing(false);
        }
    }

    public void removeBackgroundByColor(Color target, double tolerance) {
        removeBackgroundByColor(layer.snapshotCanvas(), target, tolerance, (int[])null);
    }

    public void removeBackgroundAt(Image img, int x, int y, double tolerance) {
        if (img == null) return;
        PixelReader pr = img.getPixelReader();
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        Color target = pr.getColor(x, y);
        int w = (int)img.getWidth();
        int seed = y * w + x;
        removeBackgroundByColor(img, target, tolerance, seed);
    }

    public void removeBackgroundAt(int x, int y, double tolerance) {
        removeBackgroundAt(layer.snapshotCanvas(), x, y, tolerance);
    }

    public void applyCrop(double lx, double ly, double lw, double lh) {
        if (lw <= 5 || lh <= 5) return;
        
        Image img = layer.snapshotCanvas(); // HD
        double nw = img.getWidth();
        double nh = img.getHeight();
        double layerW = layer.getWidth();
        double layerH = layer.getHeight();
        
        int px = (int)Math.round((lx / layerW) * nw);
        int py = (int)Math.round((ly / layerH) * nh);
        int pw = (int)Math.round((lw / layerW) * nw);
        int ph = (int)Math.round((lh / layerH) * nh);
        
        px = Math.max(0, Math.min((int)nw-1, px));
        py = Math.max(0, Math.min((int)nh-1, py));
        pw = Math.max(2, Math.min((int)nw - px, pw));
        ph = Math.max(2, Math.min((int)nh - py, ph));

        prepareUndoState("Recortar Imagen");
        WritableImage cropped = new WritableImage(img.getPixelReader(), px, py, pw, ph);
        
        // Ajustar posición para que no salte, considerando rotación/escala
        javafx.geometry.Point2D currentPos = layer.localToParent(0, 0);
        javafx.geometry.Point2D newPos = layer.localToParent(lx, ly);
        double dx = newPos.getX() - currentPos.getX();
        double dy = newPos.getY() - currentPos.getY();

        layer.setTranslateX(layer.getTranslateX() + dx);
        layer.setTranslateY(layer.getTranslateY() + dy);
        
        layer.setImage(cropped);
        layer.setSize(lw, lh);
        commitAction("Recortar Imagen");
    }

    public void trimWhitespace() {
        Image img = layer.snapshotCanvas();
        PixelReader pr = img.getPixelReader();
        int w = (int)img.getWidth();
        int h = (int)img.getHeight();

        int top = 0, bottom = h - 1, left = 0, right = w - 1;

        // Scan from top
        topLoop: for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (pr.getColor(x, y).getOpacity() > 0.05) { top = y; break topLoop; }
            }
        }
        // Scan from bottom
        bottomLoop: for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                if (pr.getColor(x, y).getOpacity() > 0.05) { bottom = y; break bottomLoop; }
            }
        }
        // Scan from left
        leftLoop: for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (pr.getColor(x, y).getOpacity() > 0.05) { left = x; break leftLoop; }
            }
        }
        // Scan from right
        rightLoop: for (int x = w - 1; x >= 0; x--) {
            for (int y = 0; y < h; y++) {
                if (pr.getColor(x, y).getOpacity() > 0.05) { right = x; break rightLoop; }
            }
        }

        int pw = right - left + 1;
        int ph = bottom - top + 1;
        if (pw <= 0 || ph <= 0) return;

        // Map back to logical
        double lx = (left / (double)w) * layer.getWidth();
        double ly = (top / (double)h) * layer.getHeight();
        double lw = (pw / (double)w) * layer.getWidth();
        double lh = (ph / (double)h) * layer.getHeight();

        applyCrop(lx, ly, lw, lh);
    }

    private boolean isColorSimilar(Color c1, Color c2, double tolerance) {
        if (c1.getOpacity() < 0.05) return true;
        double dist = Math.sqrt(
                Math.pow(c1.getRed() - c2.getRed(), 2) +
                Math.pow(c1.getGreen() - c2.getGreen(), 2) +
                Math.pow(c1.getBlue() - c2.getBlue(), 2)
        );
        return dist <= tolerance;
    }
}
