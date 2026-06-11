package org.example.component.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.example.component.helper.ViewportController;

/**
 * A ruler component that displays ticks and labels.
 * It synchronizes with a ViewportController to show design coordinates.
 */
public class Ruler extends Canvas {
    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    private final Orientation orientation;
    private ViewportController viewportController;
    private static final double RULER_SIZE = 16.0; // Thickness of the ruler

    public Ruler(Orientation orientation) {
        this.orientation = orientation;
        if (orientation == Orientation.HORIZONTAL) {
            setHeight(RULER_SIZE);
        } else {
            setWidth(RULER_SIZE);
        }

        // Listen to size changes to redraw
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());
    }

    public void setViewportController(ViewportController vc) {
        this.viewportController = vc;
        if (vc != null) {
            vc.setOnViewportChanged(this::draw);
        }
        draw();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        super.setWidth(width);
        super.setHeight(height);
        draw();
    }

    @Override
    public double prefWidth(double height) {
        return (orientation == Orientation.HORIZONTAL) ? 0 : RULER_SIZE;
    }

    @Override
    public double prefHeight(double width) {
        return (orientation == Orientation.VERTICAL) ? 0 : RULER_SIZE;
    }

    @Override
    public double maxWidth(double height) {
        return (orientation == Orientation.HORIZONTAL) ? 10000 : RULER_SIZE;
    }

    @Override
    public double maxHeight(double width) {
        return (orientation == Orientation.VERTICAL) ? 10000 : RULER_SIZE;
    }

    public void draw() {
        if (viewportController == null)
            return;

        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0)
            return;

        gc.clearRect(0, 0, w, h);

        // Background
        gc.setFill(Color.web("#f0f0f0"));
        gc.fillRect(0, 0, w, h);

        // Border
        gc.setStroke(Color.web("#cccccc"));
        gc.setLineWidth(1);
        if (orientation == Orientation.HORIZONTAL) {
            gc.strokeLine(0, h - 0.5, w, h - 0.5);
        } else {
            gc.strokeLine(w - 0.5, 0, w - 0.5, h);
        }

        double scale = viewportController.getFinalScale();
        if (scale <= 1e-6) // Guard against 0 or negative scale loop
            return;

        double offset = (orientation == Orientation.HORIZONTAL)
                ? viewportController.getEffectiveOffsetX()
                : viewportController.getEffectiveOffsetY();

        // Interval calculation based on scale

        double[] possibleIntervals = { 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000 };
        double rawInterval = 100.0;
        for (double interval : possibleIntervals) {
            if (interval * scale >= 30) { // Aim for at least 30 pixels between major ticks
                rawInterval = interval;
                break;
            }
        }

        double startDesign = Math.floor(-offset / scale / rawInterval) * rawInterval;

        gc.setFill(Color.web("#555555"));
        gc.setStroke(Color.web("#aaaaaa"));
        gc.setFont(new Font("Segoe UI Semibold", 9)); 

        int safetyCounter = 0;
        for (double d = startDesign;; d += rawInterval) {
            if (++safetyCounter > 500) // Don't draw more than 500 ticks
                break;
            double pos = d * scale + offset;
            if (pos > (orientation == Orientation.HORIZONTAL ? w : h))
                break;

            if (orientation == Orientation.HORIZONTAL) {
                if (pos >= 0) {
                    gc.strokeLine(pos, 0, pos, h);
                    gc.fillText(String.format("%.0f", d), pos + 3, h - 5);
                }

                // Sub-ticks
                double subInterval = rawInterval / 10.0;
                if (subInterval * scale >= 3) {
                    for (int i = 1; i < 10; i++) {
                        double subPos = (d + i * subInterval) * scale + offset;
                        if (subPos >= 0 && subPos <= w) {
                            double tickLen = (i == 5) ? h * 0.5 : h * 0.3;
                            gc.strokeLine(subPos, h - tickLen, subPos, h);
                        }
                    }
                }
            } else {
                if (pos >= 0) {
                    gc.strokeLine(0, pos, w, pos);
                    gc.save();
                    gc.translate(h - 5, pos + 2);
                    gc.rotate(-90);
                    gc.fillText(String.format("%.0f", d), 0, 0);
                    gc.restore();
                }

                // Sub-ticks
                double subInterval = rawInterval / 10.0;
                if (subInterval * scale >= 3) {
                    for (int i = 1; i < 10; i++) {
                        double subPos = (d + i * subInterval) * scale + offset;
                        if (subPos >= 0 && subPos <= h) {
                            double tickLen = (i == 5) ? w * 0.5 : w * 0.3;
                            gc.strokeLine(w - tickLen, subPos, w, subPos);
                        }
                    }
                }
            }
        }
    }
}

