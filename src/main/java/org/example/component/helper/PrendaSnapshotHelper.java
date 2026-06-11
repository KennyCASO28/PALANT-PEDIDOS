package org.example.component.helper;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Affine;

public final class PrendaSnapshotHelper {

    private static final double LONG_SLEEVE_SHIFT = 350.0;
    private static final double LONG_SLEEVE_PADDING = 1.50;
    private static final double DEFAULT_PADDING = 1.15;
    private static final double BACK_HALF_THRESHOLD = 400.0;
    private static final double MAX_SNAPSHOT_PART_WIDTH = 900.0;

    private PrendaSnapshotHelper() {
    }

    public static WritableImage captureCenteredSnapshot(Pane sandbox, Group contentGroup, boolean isLongSleeve) {
        VisibleScanner scanner = new VisibleScanner(isLongSleeve);
        scanner.scan(contentGroup);

        if (!scanner.found) {
            return new WritableImage(1, 1);
        }

        boolean shifted = false;
        try {
            if (isLongSleeve) {
                applyDeepShift(contentGroup, LONG_SLEEVE_SHIFT);
                sandbox.layout();
                shifted = true;
            }

            double boundsWidth = scanner.maxX - scanner.minX;
            double boundsHeight = scanner.maxY - scanner.minY;
            double padding = isLongSleeve ? LONG_SLEEVE_PADDING : DEFAULT_PADDING;
            double viewportWidth = boundsWidth * padding;
            double viewportHeight = boundsHeight * padding;

            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);

            Affine transform = new Affine();
            transform.appendScale(2, 2);
            transform.appendTranslation(
                    -scanner.minX + (viewportWidth - boundsWidth) / 2.0,
                    -scanner.minY + (viewportHeight - boundsHeight) / 2.0);

            parameters.setTransform(transform);
            parameters.setViewport(new javafx.geometry.Rectangle2D(0, 0, viewportWidth * 2, viewportHeight * 2));
            return sandbox.snapshot(parameters, null);
        } finally {
            if (shifted) {
                applyDeepShift(contentGroup, -LONG_SLEEVE_SHIFT);
            }
        }
    }

    public static Color pickColorFromCache(
            WritableImage cachedSnapshot,
            double nodeWidth,
            double nodeHeight,
            Point2D localPoint) {
        if (cachedSnapshot == null || localPoint == null || nodeWidth <= 0 || nodeHeight <= 0) {
            return Color.TRANSPARENT;
        }

        double scaleX = cachedSnapshot.getWidth() / nodeWidth;
        double scaleY = cachedSnapshot.getHeight() / nodeHeight;

        int x = (int) (localPoint.getX() * scaleX);
        int y = (int) (localPoint.getY() * scaleY);

        if (x >= 0 && x < cachedSnapshot.getWidth() && y >= 0 && y < cachedSnapshot.getHeight()) {
            return cachedSnapshot.getPixelReader().getColor(x, y);
        }
        return Color.TRANSPARENT;
    }

    public static Image getRegionFromCache(
            WritableImage cachedSnapshot,
            double nodeWidth,
            double nodeHeight,
            Point2D localPoint,
            int width,
            int height) {
        if (cachedSnapshot == null || localPoint == null || nodeWidth <= 0 || nodeHeight <= 0) {
            return new WritableImage(Math.max(1, width), Math.max(1, height));
        }

        double scaleX = cachedSnapshot.getWidth() / nodeWidth;
        double scaleY = cachedSnapshot.getHeight() / nodeHeight;

        int centerX = (int) (localPoint.getX() * scaleX);
        int centerY = (int) (localPoint.getY() * scaleY);
        int pixelWidth = Math.max(1, (int) (width * scaleX));
        int pixelHeight = Math.max(1, (int) (height * scaleY));
        int startX = centerX - pixelWidth / 2;
        int startY = centerY - pixelHeight / 2;

        WritableImage region = new WritableImage(pixelWidth, pixelHeight);
        javafx.scene.image.PixelWriter writer = region.getPixelWriter();
        javafx.scene.image.PixelReader reader = cachedSnapshot.getPixelReader();
        int maxWidth = (int) cachedSnapshot.getWidth();
        int maxHeight = (int) cachedSnapshot.getHeight();

        for (int x = 0; x < pixelWidth; x++) {
            for (int y = 0; y < pixelHeight; y++) {
                int sourceX = startX + x;
                int sourceY = startY + y;
                if (sourceX >= 0 && sourceX < maxWidth && sourceY >= 0 && sourceY < maxHeight) {
                    writer.setArgb(x, y, reader.getArgb(sourceX, sourceY));
                } else {
                    writer.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }

        return region;
    }

    private static void applyDeepShift(Node node, double shift) {
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child.localToParent(child.getBoundsInLocal()).getMinX() > BACK_HALF_THRESHOLD) {
                    child.setTranslateX(child.getTranslateX() + shift);
                } else if (child instanceof Parent) {
                    applyDeepShift(child, shift);
                }
            }
        }
    }

    private static final class VisibleScanner {
        private final boolean longSleeve;
        private double minX = Double.MAX_VALUE;
        private double minY = Double.MAX_VALUE;
        private double maxX = -Double.MAX_VALUE;
        private double maxY = -Double.MAX_VALUE;
        private boolean found = false;

        private VisibleScanner(boolean longSleeve) {
            this.longSleeve = longSleeve;
        }

        private void scan(Node node) {
            if (!node.isVisible() || node.getOpacity() <= 0.01) {
                return;
            }

            if (node instanceof SVGPath svgPath) {
                if (svgPath.getContent() == null || svgPath.getContent().isEmpty()) {
                    return;
                }

                javafx.geometry.Bounds bounds = node.localToParent(svgPath.getBoundsInLocal());
                if (bounds.getWidth() > MAX_SNAPSHOT_PART_WIDTH) {
                    return;
                }

                double effectiveMinX = bounds.getMinX();
                double effectiveMaxX = bounds.getMaxX();
                if (longSleeve && bounds.getMinX() > BACK_HALF_THRESHOLD) {
                    effectiveMinX += LONG_SLEEVE_SHIFT;
                    effectiveMaxX += LONG_SLEEVE_SHIFT;
                }

                minX = Math.min(minX, effectiveMinX);
                minY = Math.min(minY, bounds.getMinY());
                maxX = Math.max(maxX, effectiveMaxX);
                maxY = Math.max(maxY, bounds.getMaxY());
                found = true;
                return;
            }

            if (node instanceof Parent parent) {
                for (Node child : parent.getChildrenUnmodifiable()) {
                    scan(child);
                }
            }
        }
    }
}
