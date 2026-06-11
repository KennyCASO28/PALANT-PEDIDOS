package org.example.logic;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.LinkedList;
import java.util.Queue;

public class BackgroundRemover {

    public static Image removeBackground(Image inputImage, Color targetColor, double tolerance) {
        int width = (int) inputImage.getWidth();
        int height = (int) inputImage.getHeight();
        WritableImage outputImage = new WritableImage(width, height);
        PixelReader reader = inputImage.getPixelReader();
        PixelWriter writer = outputImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (isSimilar(color, targetColor, tolerance)) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                } else {
                    writer.setColor(x, y, color);
                }
            }
        }
        return outputImage;
    }

    /**
     * Smart removal: Attempts to remove the background starting from the perimeter.
     * Uses a flood-fill algorithm seeded from every pixel on the edge of the image.
     */
    public static Image removeSmartBackground(Image inputImage, double tolerance) {
        int width = (int) inputImage.getWidth();
        int height = (int) inputImage.getHeight();

        // 1. Initial Robustness Check
        if (width < 2 || height < 2) return inputImage;

        WritableImage outputImage = new WritableImage(width, height);
        PixelReader reader = inputImage.getPixelReader();
        PixelWriter writer = outputImage.getPixelWriter();

        // Copy image first
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, reader.getColor(x, y));
            }
        }

        boolean[][] visited = new boolean[width][height];

        // --- ENHANCED SEEDING: Sample the entire perimeter ---
        // This handles cases where the background doesn't touch the corners
        // but touches some other edge part (common in cropped images).

        // Top and Bottom edges
        for (int x = 0; x < width; x++) {
            floodFillTransparent(reader, writer, visited, x, 0, tolerance, width, height);
            floodFillTransparent(reader, writer, visited, x, height - 1, tolerance, width, height);
        }
        // Left and Right edges
        for (int y = 0; y < height; y++) {
            floodFillTransparent(reader, writer, visited, 0, y, tolerance, width, height);
            floodFillTransparent(reader, writer, visited, width - 1, y, tolerance, width, height);
        }

        // --- Post-Processing: SOFT EDGES & ANTI-HALO ---
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!visited[x][y]) {
                    // Check neighbors to detect boundary
                    boolean nearEdge = false;
                    int visitedNeighbors = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height && visited[nx][ny]) {
                                nearEdge = true;
                                visitedNeighbors++;
                            }
                        }
                    }
                    if (nearEdge) {
                        Color original = reader.getColor(x, y);
                        // SMARTER BLENDING: Calculate alpha based on neighbor count (smoothing)
                        // This reduces "jagged" artifacts at the boundary.
                        double alpha = 1.0 - (visitedNeighbors / 9.0) * 0.8;
                        writer.setColor(x, y,
                                new Color(original.getRed(), original.getGreen(), original.getBlue(),
                                        Math.max(0, Math.min(original.getOpacity(), alpha))));
                    }
                }
            }
        }

        return outputImage;
    }

    public static void floodFillTransparent(PixelReader reader, PixelWriter writer, boolean[][] visited,
            int startX, int startY, double tolerance, int w, int h) {
        if (startX < 0 || startX >= w || startY < 0 || startY >= h)
            return;
        if (visited[startX][startY])
            return;

        Color targetColor = reader.getColor(startX, startY);
        
        // If the start pixel is already transparent or too dark to be "background white", skip?
        // Actually, we want to be generous with "white" background.
        // If the user requested "White Background removal", we should check if targetColor is close to white.
        // But the generic algorithm uses the seed color.
        
        // Optimization: if the pixel is already mostly transparent, don't use it as a seed
        if (targetColor.getOpacity() < 0.1) {
            visited[startX][startY] = true;
            return;
        }

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            writer.setColor(p.x, p.y, Color.TRANSPARENT);

            int[] dx = { 1, -1, 0, 0 };
            int[] dy = { 0, 0, 1, -1 };

            for (int i = 0; i < 4; i++) {
                int nx = p.x + dx[i];
                int ny = p.y + dy[i];

                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[nx][ny]) {
                    Color neighborColor = reader.getColor(nx, ny);
                    if (isSimilar(neighborColor, targetColor, tolerance)) {
                        visited[nx][ny] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }
    }

    private static boolean isSimilar(Color c1, Color c2, double tolerance) {
        double dist = Math.sqrt(
                Math.pow(c1.getRed() - c2.getRed(), 2) +
                        Math.pow(c1.getGreen() - c2.getGreen(), 2) +
                        Math.pow(c1.getBlue() - c2.getBlue(), 2));
        return dist < tolerance;
    }

    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}

