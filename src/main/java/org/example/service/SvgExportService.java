package org.example.service;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.transform.Transform;
import javafx.scene.text.Text;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SvgExportService {

    private StringBuilder svgBody;
    private StringBuilder svgDefs;
    private int idCounter = 0;
    private Map<Node, String> clipMap = new HashMap<>();

    public void exportarVector(org.example.dto.ExportDataDTO data) {
        exportarVector(data.getTargetFile(), data.getVisualizer(), data.getOrderCode());
    }

    /**
     * High-level orchestration for SVG export.
     * Handles file selection (optional if pre-selected), actual generation, and
     * user feedback.
     */
    public void exportarVector(File file, org.example.component.PrendaVisualizer visualizer, String codigo) {
        if (visualizer == null || file == null)
            return;

        try {
            // We export the content group to avoid UI overlays and zoom artifacts
            exportToSvg(visualizer.getContentGroup(), file);

            org.example.utils.UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Vector Exportado",
                    "Diseño guardóado correctamente en: " + file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            org.example.utils.UIFactory.mostrarAlerta(javafx.scene.control.Alert.AlertType.ERROR,
                    "Error Exportando",
                    "No se pudo guardóar el archivo SVG: " + e.getMessage());
        }
    }

    public void exportToSvg(Node rootNode, File file) throws IOException {
        svgBody = new StringBuilder();
        svgDefs = new StringBuilder();
        clipMap.clear();
        idCounter = 0;

        // Traverse
        convertNode(rootNode, svgBody, 0);

        // Build Final SVG
        StringBuilder finalSvg = new StringBuilder();
        finalSvg.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        // Calculate Bounds Manually to ensure everything is included
        javafx.geometry.Bounds bounds = calculateRecursiveBounds(rootNode, Transform.affine(1, 0, 0, 1, 0, 0));

        if (bounds == null) {
            // Fallback
            bounds = rootNode.getBoundsInLocal();
        }

        double minX = bounds.getMinX();
        double minY = bounds.getMinY();
        double width = bounds.getWidth();
        double height = bounds.getHeight();

        // Add padding
        // Add padding
        double padding = 50;
        minX -= padding;
        minY -= padding;
        width += padding * 2;
        height += padding * 2;

        finalSvg.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"%.2f\" height=\"%.2f\" viewBox=\"%.2f %.2f %.2f %.2f\">\n",
                width, height, minX, minY, width, height));

        if (svgDefs.length() > 0) {
            finalSvg.append("<defs>\n");
            finalSvg.append(svgDefs);
            finalSvg.append("</defs>\n");
        }

        finalSvg.append(svgBody);
        finalSvg.append("</svg>");

        // Write
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.write(finalSvg.toString());
        }
    }

    private javafx.geometry.Bounds calculateRecursiveBounds(Node node, Transform transform) {
        if (!node.isVisible())
            return null;

        // Combine current node transform
        Transform nodeTransform = transform.createConcatenation(node.getLocalToParentTransform());

        javafx.geometry.Bounds totalBounds = null;

        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                javafx.geometry.Bounds childBounds = calculateRecursiveBounds(child, nodeTransform);
                if (childBounds != null) {
                    if (totalBounds == null) {
                        totalBounds = childBounds;
                    } else {
                        // Manual union to avoid internal JavaFX issues if any
                        double minX = Math.min(totalBounds.getMinX(), childBounds.getMinX());
                        double minY = Math.min(totalBounds.getMinY(), childBounds.getMinY());
                        double maxX = Math.max(totalBounds.getMaxX(), childBounds.getMaxX());
                        double maxY = Math.max(totalBounds.getMaxY(), childBounds.getMaxY());
                        totalBounds = new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
                    }
                }
            }
            // Check if Parent itself has content (like direct Shape subclass? No, Parent is
            // abstract or Group/Pane)
            // usually Parents handle bounds via children.
        }

        // Leaf nodes (Shape, ImageView)
        if (node instanceof Shape || node instanceof ImageView) {
            javafx.geometry.Bounds local = node.getBoundsInLocal(); // Untransformed local bounds
            // Transform bounds to Root Space
            javafx.geometry.Bounds transformed = nodeTransform.transform(local);

            if (totalBounds == null) {
                totalBounds = transformed;
            } else {
                double minX = Math.min(totalBounds.getMinX(), transformed.getMinX());
                double minY = Math.min(totalBounds.getMinY(), transformed.getMinY());
                double maxX = Math.max(totalBounds.getMaxX(), transformed.getMaxX());
                double maxY = Math.max(totalBounds.getMaxY(), transformed.getMaxY());
                totalBounds = new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
            }
        }

        return totalBounds;
    }

    private void convertNode(Node node, StringBuilder sb, int indent) {
        if (!node.isVisible())
            return;

        // Indentation
        String tab = "  ".repeat(indent);

        // 1. Check Transforms
        StringBuilder transformAttr = new StringBuilder();
        if (!node.getTransforms().isEmpty() || node.getTranslateX() != 0 || node.getTranslateY() != 0
                || node.getScaleX() != 1 || node.getScaleY() != 1) {
            transformAttr.append(" transform=\"");

            // Translate
            if (node.getTranslateX() != 0 || node.getTranslateY() != 0) {
                transformAttr.append(
                        String.format(Locale.US, "translate(%.2f, %.2f) ", node.getTranslateX(), node.getTranslateY()));
            }
            // Scale
            if (node.getScaleX() != 1 || node.getScaleY() != 1) {
                transformAttr
                        .append(String.format(Locale.US, "scale(%.2f, %.2f) ", node.getScaleX(), node.getScaleY()));
            }

            // Explicit transforms
            for (Transform t : node.getTransforms()) {
                transformAttr.append(String.format(Locale.US, "matrix(%.4f, %.4f, %.4f, %.4f, %.4f, %.4f) ",
                        t.getMxx(), t.getMyx(), t.getMxy(), t.getMyy(), t.getTx(), t.getTy()));
            }
            transformAttr.append("\"");
        }

        // 2. Check Clipping (Powerclip)
        String clipAttr = "";
        if (node.getClip() != null) {
            String clipId = getOrCreateClip(node.getClip());
            clipAttr = " clip-path=\"url(#" + clipId + ")\"";
        }

        // 3. Node Type Handling
        if (node instanceof Shape) {
            convertShape((Shape) node, sb, tab, transformAttr.toString(), clipAttr);
        } else if (node instanceof ImageView) {
            convertImageView((ImageView) node, sb, tab, transformAttr.toString(), clipAttr);
        } else if (node instanceof Parent) {
            sb.append(tab).append("<g").append(transformAttr).append(clipAttr).append(">\n");
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                convertNode(child, sb, indent + 1);
            }
            sb.append(tab).append("</g>\n");
        }
    }

    private String getOrCreateClip(Node clipNode) {
        if (clipMap.containsKey(clipNode)) {
            return clipMap.get(clipNode);
        }

        String id = "clip_" + (++idCounter);
        clipMap.put(clipNode, id);

        StringBuilder clipContent = new StringBuilder();
        // Recursively convert the clip node structure, but we only care about geometry
        // (paths)
        // Usually clips are Shapes.
        convertNode(clipNode, clipContent, 2);

        // Wrap in clipPath tag
        svgDefs.append("  <clipPath id=\"").append(id).append("\">\n");
        svgDefs.append(clipContent);
        svgDefs.append("  </clipPath>\n");

        return id;
    }

    private void convertShape(Shape shape, StringBuilder sb, String tab, String transform, String clip) {
        String fill = colorToHex(shape.getFill());
        String stroke = colorToHex(shape.getStroke());
        String strokeWidth = String.format(Locale.US, "%.2f", shape.getStrokeWidth());

        // Only output stroke if width > 0
        String style = "";
        if (shape.getStroke() == null || shape.getStrokeWidth() == 0) {
            style = String.format("fill=\"%s\" stroke=\"none\"", fill);
        } else {
            style = String.format("fill=\"%s\" stroke=\"%s\" stroke-width=\"%s\"", fill, stroke, strokeWidth);
        }

        if (shape.getStyle().contains("-fx-fill: transparent") || shape.getFill() == Color.TRANSPARENT) {
            // Keep it if it's a stroke, otherwise invisible fill
            if (shape.getStroke() == null || shape.getStroke() == Color.TRANSPARENT) {
                return; // Completely invisible
            }
        }

        if (shape instanceof SVGPath) {
            SVGPath p = (SVGPath) shape;
            sb.append(tab)
                    .append(String.format("<path d=\"%s\" %s %s %s />\n", p.getContent(), style, transform, clip));
        } else if (shape instanceof Rectangle) {
            Rectangle r = (Rectangle) shape;
            sb.append(tab).append(String.format(Locale.US,
                    "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.2f\" ry=\"%.2f\" %s %s %s />\n",
                    r.getX(), r.getY(), r.getWidth(), r.getHeight(), r.getArcWidth(), r.getArcHeight(), style,
                    transform, clip));
        } else if (shape instanceof Circle) {
            Circle c = (Circle) shape;
            sb.append(tab).append(String.format(Locale.US, "<circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" %s %s %s />\n",
                    c.getCenterX(), c.getCenterY(), c.getRadius(), style, transform, clip));
        } else if (shape instanceof Line) {
            Line l = (Line) shape;
            sb.append(tab)
                    .append(String.format(Locale.US,
                            "<line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" %s %s %s />\n",
                            l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY(), style, transform, clip));
        } else if (shape instanceof Text) {
            Text t = (Text) shape;
            // Enhanced text support: Include stroke if present
            String textStyle = style;
            if (t.getStroke() != null && t.getStrokeWidth() > 0) {
                // Ensure text specific stroke-width if needed or use generic
            }

            sb.append(tab).append(String.format(Locale.US,
                    "<text x=\"%.2f\" y=\"%.2f\" font-family=\"%s\" font-size=\"%.2f\" %s %s %s>%s</text>\n",
                    t.getX(), t.getY(), t.getFont().getFamily(), t.getFont().getSize(), textStyle, transform, clip,
                    t.getText()));
        }
        // Add other shapes as needed (Path, etc.)
        // Fallback for generic Path (from Shape parent) is harder without PathElement
        // access easily mapped to d-string.
    }

    private void convertImageView(ImageView iv, StringBuilder sb, String tab, String transform, String clip) {
        if (iv.getImage() == null)
            return;

        String base64Obj = imageToBase64(iv.getImage());
        double x = iv.getX();
        double y = iv.getY();
        double w = iv.getFitWidth() > 0 ? iv.getFitWidth() : iv.getImage().getWidth();
        double h = iv.getFitHeight() > 0 ? iv.getFitHeight() : iv.getImage().getHeight();

        sb.append(tab).append(String.format(Locale.US,
                "<image x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" xlink:href=\"data:image/png;base64,%s\" %s %s />\n",
                x, y, w, h, base64Obj, transform, clip));
    }

    private String colorToHex(Paint paint) {
        if (paint instanceof Color) {
            Color c = (Color) paint;
            if (c.getOpacity() == 0)
                return "none";
            return String.format("#%02X%02X%02X",
                    (int) (c.getRed() * 255),
                    (int) (c.getGreen() * 255),
                    (int) (c.getBlue() * 255));
        }
        return "black"; // fallback
    }

    private String imageToBase64(Image fxImage) {
        try {
            BufferedImage bImage = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImage, null);
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            ImageIO.write(bImage, "png", s);
            byte[] res = s.toByteArray();
            return Base64.getEncoder().encodeToString(res);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}

