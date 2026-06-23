package org.example.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.BaseFont;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.embed.swing.SwingFXUtils;
import org.example.component.PrendaVisualizer;
import org.example.controller.uicomponent.FichaTecnicaView;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.DetallePedido;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PdfExportService {

    public static List<ShieldEntry> collectShields(PrendaVisualizer visualizer) {
        List<ShieldEntry> list = new ArrayList<>();
        if (visualizer == null || visualizer.getContentGroup() == null)
            return list;

        findShieldsRecursive(visualizer.getContentGroup(), list);
        return list;
    }

    private static void findShieldsRecursive(Node node, List<ShieldEntry> list) {
        if (node instanceof org.example.component.ImageLayer) {
            org.example.component.ImageLayer layer = (org.example.component.ImageLayer) node;
            if (layer.getBadgeType() != null && layer.getBadgeType() != org.example.model.TipoEscudo.NINGUNO) {
                list.add(new ShieldEntry(layer.getImage(), layer.getBadgeType().getLabel(), layer.getActiveZone()));
            }
        } else if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                findShieldsRecursive(child, list);
            }
        }
    }

    public void exportarFicha(org.example.dto.ExportDataDTO data) {
        exportarFicha(data.getTargetFile(), data.getVisualizer(), data.getConfig(), data.getOrderCode(),
                data.getClientName(), data.getSellerName(), data.getRoster(), data.getDeliveryDate(),
                data.getPriority(), data.getShortType(), data.getReferenceImages(), data.getShippingInfo(),
                data.getArqueroSnapshotHombre(), data.getArqueroSnapshotMujer(), data.getArqueroConfig(), data.getMainGarmentSnapshot(), data.getShields(), data.getDisenosArquero());
    }

    public void exportarFicha(File file, PrendaVisualizer visualizer, ConfiguracionPrendaDTO config, String codigo,
            String cliente, String vendedor, List<DetallePedido> roster, java.time.LocalDate fechaEntrega,
            String prioridad,
            String shortType, List<javafx.scene.image.Image> referencias, org.example.dto.DatosEnvioDTO shippingInfo,
            javafx.scene.image.Image arqSnapshotHombre, javafx.scene.image.Image arqSnapshotMujer, ConfiguracionPrendaDTO arqueroConfig,
            javafx.scene.image.Image mainSnapshot, List<ShieldEntry> shields, java.util.Map<String, org.example.dto.save.GoalkeeperDesignDTO> disenosArquero) {
        try {
            // 1. Build the view (Offscreen)
            // shields and mainSnapshot are now passed as arguments from PedidoController
            // to ensure state isolation during the export process.
            // Extract layers for color palette scanning in the report
            List<org.example.dto.save.LayerDTO> mainLayers = (visualizer != null
                    && visualizer.getCamisetaState() != null)
                            ? org.example.service.save.StateMapper.extractUserLayers(visualizer.getCamisetaState())
                            : new ArrayList<>();

            Region fullView = FichaTecnicaView.build(
                    config,
                    mainSnapshot,
                    mainLayers,
                    visualizer,
                    roster,
                    cliente,
                    codigo,
                    shields,
                    fechaEntrega,
                    prioridad,
                    shortType,
                    vendedor,
                    referencias,
                    shippingInfo,
                    arqSnapshotHombre,
                    arqSnapshotMujer,
                    arqueroConfig,
                    disenosArquero);

            // 2. Prepare for Snapshotting
            // Set explicit size in proporción A4 para layout exacto.
            Scene dummyScene = new Scene(new StackPane(fullView), 794, 1123);
            java.net.URL cssUrl = getClass().getResource("/styles.css");

            // Aseguramos flujo de visualizaciones en modo real
            dummyScene.setFill(javafx.scene.paint.Color.WHITE);
            if (cssUrl != null)
                dummyScene.getStylesheets().add(cssUrl.toExternalForm());

            fullView.applyCss();
            fullView.layout();

            // 3. Hybrid PDF Generation (Snapshots + Transparent Text Overlay)
            // This ensures the PDF looks EXACTLY like the UI but has selectable text.
            exportarFichaTecnicaHybrid(file, fullView);

            // 4. Cleanup
            fullView = null;
            System.gc(); // Hint for GC on low-RAM systems after heavy UI snapshotting

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error en orquestación de PDF: " + e.getMessage());
        }
    }

    private void exportarFichaTecnicaHybrid(File file, Region fullView) {
        // Prepare Document
        Document document = new Document(PageSize.A4, 0, 0, 0, 0); // No margins, we use snapshots
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // The fullView is a StackPane(VBox(pages))
            VBox pagesContainer = null;
            if (fullView instanceof StackPane) {
                Node content = ((StackPane) fullView).getChildren().get(0);
                if (content instanceof VBox)
                    pagesContainer = (VBox) content;
            } else if (fullView instanceof VBox) {
                pagesContainer = (VBox) fullView;
            }

            if (pagesContainer != null) {
                for (Node pageNode : pagesContainer.getChildren()) {
                    if (pageNode instanceof Region) {
                        addPageFromSnapshot(document, writer, (Region) pageNode);
                    }
                }
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addPageFromSnapshot(Document document, PdfWriter writer, Region pageNode) throws Exception {
        document.newPage();
        PdfContentByte cb = writer.getDirectContent();

        // 1. Snapshot the high-res image (excluding vector-tagged text)
        List<Node> hiddenNodes = new java.util.ArrayList<>();
        collectAndHideText(pageNode, hiddenNodes);
        javafx.scene.image.Image snapshot = captureSnapshot(pageNode);
        // Restore visibility
        for (Node n : hiddenNodes) n.setOpacity(1.0);

        // 2. Add high-res image to the PDF
        java.io.ByteArrayOutputStream byteStream = new java.io.ByteArrayOutputStream();
        java.awt.image.BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
        java.awt.image.BufferedImage rgbImage = new java.awt.image.BufferedImage(
            bufferedImage.getWidth(),
            bufferedImage.getHeight(),
            java.awt.image.BufferedImage.TYPE_INT_RGB
        );
        java.awt.Graphics2D g = rgbImage.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            g.drawImage(bufferedImage, 0, 0, null);
        } finally {
            g.dispose();
        }

        java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO.getImageWritersByFormatName("jpg");
        if (writers.hasNext()) {
            javax.imageio.ImageWriter imgWriter = writers.next();
            try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(byteStream)) {
                imgWriter.setOutput(ios);
                javax.imageio.ImageWriteParam param = imgWriter.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionType("JPEG");
                    param.setCompressionQuality(0.98f);
                }
                imgWriter.write(null, new javax.imageio.IIOImage(rgbImage, null, null), param);
            } finally {
                imgWriter.dispose();
            }
        } else {
            javax.imageio.ImageIO.write(rgbImage, "png", byteStream);
        }
        com.lowagie.text.Image pdfImg = com.lowagie.text.Image.getInstance(byteStream.toByteArray());

        // A4 precise dimensions: 595.27 x 841.89 points
        float a4W = 595.27f;
        float a4H = 841.89f;

        // Calculate proportional scaling to fit within A4 while maintaining aspect ratio
        float imgW = pdfImg.getWidth();
        float imgH = pdfImg.getHeight();
        System.out.println("DEBUG PDF: original imgW=" + imgW + " imgH=" + imgH);
        float scaleX = a4W / imgW;
        float scaleY = a4H / imgH;
        float scale = Math.min(scaleX, scaleY) * 100.0f;
        System.out.println("DEBUG PDF: scaleX=" + scaleX + " scaleY=" + scaleY + " scale%=" + scale);

        // Calculate actual displayed dimensions after scaling
        float scaledPercent = scale / 100.0f;
        float finalW = imgW * scaledPercent;
        float finalH = imgH * scaledPercent;
        System.out.println("DEBUG PDF: Final PDF display size = " + finalW + "x" + finalH + " points");

        pdfImg.scalePercent(scale);

        // Center the image on the page
        float scaledW = pdfImg.getScaledWidth();
        float scaledH = pdfImg.getScaledHeight();
        float xImg = (a4W - scaledW) / 2.0f;
        float yImg = (a4H - scaledH) / 2.0f;
        pdfImg.setAbsolutePosition(xImg, yImg);
        cb.addImage(pdfImg);

        // 3. Overlay Vector Text with DYNAMIC SCALE (Match image precisely)
        float imgScaledHeight = pdfImg.getScaledHeight();
        float actualScale = (float)(imgScaledHeight / 1123.0);
        float topAnchorY = (float)(yImg + imgScaledHeight);

        // Cleanup heavy snapshot memory as early as possible
        snapshot = null;
        bufferedImage = null;
        rgbImage = null;
        pdfImg = null;
        byteStream = null;
        System.gc(); // Force release of native/heap memory before next page

        BaseFont bfn = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED);
 
        overlayTaggedNodes(cb, bfn, bfBold, pageNode, actualScale, topAnchorY, xImg, a4H);
    }
 
    private WritableImage captureSnapshot(Region node) {
        javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
        sp.setFill(javafx.scene.paint.Color.WHITE);
        // Escala 2.5x para mantener alta calidad sin exceder la memoria de video (VRAM/D3D)
        sp.setTransform(javafx.scene.transform.Transform.scale(2.5, 2.5));
        return node.snapshot(sp, null);
    }

    private void collectAndHideText(Node node, List<Node> hiddenNodes) {
        if (node instanceof Label || node instanceof javafx.scene.text.Text) {
            // ONLY hide nodes intended for vector copy-paste (roster data, etc)
            if (node.getStyleClass().contains("selectable-text") && node.getOpacity() > 0) {
                hiddenNodes.add(node);
                node.setOpacity(0.0);
            }
        } else if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                collectAndHideText(child, hiddenNodes);
            }
        }
    }

    private void overlayTaggedNodes(PdfContentByte cb, BaseFont bfn, BaseFont bfBold, Region pageNode, float scale, float topAnchorY, float lAnchorX, float a4H) {
        // Use lookupAll to find all tagged nodes regardless of nesting complexity
        Set<Node> tagged = pageNode.lookupAll(".selectable-text");
        
        for (Node node : tagged) {
            if (!node.isVisible()) continue;

            // --- INTELLIGENT CELL-CENTER MAPPING ---
            // Use sceneToLocal to transform the center point of the cell directly into 
            // the page-relative coordinate space. This is more robust against parent transforms.
            double cellW = 0, cellH = 0;
            double x = 0, y = 0;
            Node parent = node.getParent();
            if (parent instanceof Region) {
                Region parentReg = (Region) parent;
                cellW = parentReg.getWidth();
                cellH = parentReg.getHeight();
                
                // Get cell midpoint in scene
                javafx.geometry.Point2D cellCenterScene = parentReg.localToScene(cellW / 2.0, cellH / 2.0);
                // Transform to local page coordinates (Relative to top-left 0,0 of pageNode)
                javafx.geometry.Point2D cellCenterLocal = pageNode.sceneToLocal(cellCenterScene);
                
                x = cellCenterLocal.getX();
                y = cellCenterLocal.getY();
            }

            if (node instanceof Label) {
                Label lbl = (Label) node;
                javafx.scene.paint.Color color = javafx.scene.paint.Color.BLACK;
                
                boolean isBold = lbl.getStyle().contains("bold") || (lbl.getFont().getStyle().toLowerCase().contains("bold"));
                BaseFont actualFont = isBold ? bfBold : bfn;
                
                // Pass cell-center coordinates and use CENTERED logic
                renderTextToPdf(cb, actualFont, lbl.getText(), x, y, 
                        (float)cellW, (float)cellH, (float)lbl.getFont().getSize(), color, scale, topAnchorY, lAnchorX, a4H);
            } else if (node instanceof javafx.scene.text.Text) {
                javafx.scene.text.Text txt = (javafx.scene.text.Text) node;
                javafx.geometry.Point2D tCenter = pageNode.sceneToLocal(node.localToScene(0, 0));
                renderTextToPdf(cb, bfn, txt.getText(), tCenter.getX(), tCenter.getY(), 0, 0, (float)txt.getFont().getSize(), javafx.scene.paint.Color.BLACK, scale, topAnchorY, lAnchorX, a4H);
            }
        }
    }

    private void renderTextToPdf(PdfContentByte cb, BaseFont bf, String text, double centerX, double centerY, float fxW, float fxH, float fontSize, javafx.scene.paint.Color color, float scale, float topAnchorY, float lAnchorX, float a4H) {
        if (text == null || text.trim().isEmpty()) return;

        // Since centerX/Y are already the MIDPOINT of the cell in scene coords:
        float pdfX = (float)(lAnchorX + (centerX * scale));
        
        // Horizontal: ALIGN_CENTER (since pdfX is at the cell center)
        int pdfAlign = Element.ALIGN_CENTER;

        // Vertical: topAnchorY minus (centerY * scale) gives the center of the cell in PDF space.
        // We subtract a baseline correction (~40% of font size) to account for optical vertical centering.
        float pdfY = (float)(topAnchorY - (centerY * scale) - (fontSize * scale * 0.40));

        cb.beginText();
        cb.setFontAndSize(bf, fontSize * scale);
        
        if (color != null) {
            cb.setRGBColorFill((int)(color.getRed() * 255), (int)(color.getGreen() * 255), (int)(color.getBlue() * 255));
        }

        cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL);
        cb.showTextAligned(pdfAlign, text, pdfX, pdfY, 0);
        cb.endText();
    }


    public static class ShieldEntry {
        public final javafx.scene.image.Image image;
        public final String label;
        public final String zone;

        public ShieldEntry(javafx.scene.image.Image image, String label, String zone) {
            this.image = image;
            this.label = label;
            this.zone = zone;
        }

        public javafx.scene.image.Image getImage() {
            return image;
        }

        public String getLabel() {
            return label;
        }

        public String getZone() {
            return zone;
        }
    }

}
