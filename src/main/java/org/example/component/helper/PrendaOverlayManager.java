package org.example.component.helper;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import org.example.component.renderer.ShortsRenderer;
import org.example.model.TipoCorte;

/**
 * Manages the visual red dashed overlay used during editing to show garment
 * boundaries.
 * Extracts complexity from PrendaVisualizer.
 */
public class PrendaOverlayManager {

    private final Group overlayGroup;
    private SVGPath overlayStroke;
    private double sleeveThreshold = 380.0; // min X for front pieces
    private double sleeveMaxThreshold = Double.MAX_VALUE; // max X for front pieces (range mode)
    private boolean isFrontOnLeft = true;

    public void setSleeveThreshold(double threshold) {
        this.sleeveThreshold = threshold;
    }

    public void setSleeveMaxThreshold(double max) {
        this.sleeveMaxThreshold = max;
    }

    public double getSleeveThreshold() {
        return sleeveThreshold;
    }

    public void setIsFrontOnLeft(boolean isFrontOnLeft) {
        this.isFrontOnLeft = isFrontOnLeft;
    }

    private boolean isInterleavedMapping = false;

    public void setIsInterleavedMapping(boolean interleaved) {
        this.isInterleavedMapping = interleaved;
    }

    public boolean isFrontOnLeft() {
        return isFrontOnLeft;
    }

    public boolean isFrontPiece(double startX) {
        // Identificación precisa para piezas de manga cuadrada (Raglan) Varón
        // Esto permite separar el frente del brazo (24, 444) de la espalda (407, 864)
        if (isInterleavedMapping) {
            if (Math.abs(startX - 24.85) < 10)
                return true; // Delantera
            if (Math.abs(startX - 407.3) < 10)
                return false; // Trasera
            if (Math.abs(startX - 444.6) < 20)
                return true; // Delantera (inner 1)
            if (Math.abs(startX - 481.9) < 20)
                return true; // Delantera (inner 2)
            if (Math.abs(startX - 864.4) < 10)
                return false; // Trasera
        }

        // Range mode: front pieces are the CENTER pieces of the SVG.
        if (sleeveMaxThreshold < Double.MAX_VALUE) {
            return startX >= sleeveThreshold && startX <= sleeveMaxThreshold;
        }
        // Simple threshold mode (for Redondo)
        if (isFrontOnLeft) {
            return startX < sleeveThreshold;
        } else {
            return startX >= sleeveThreshold;
        }
    }


    public PrendaOverlayManager() {
        this.overlayGroup = new Group();
        this.overlayGroup.setMouseTransparent(true); // Always transparent
    }

    public Group getOverlayGroup() {
        return overlayGroup;
    }

    public void setVisible(boolean visible) {
        overlayGroup.setVisible(visible);
    }

    public void clearOverlay() {
        if (overlayStroke != null) {
            overlayGroup.getChildren().remove(overlayStroke);
            overlayStroke = null;
        }
    }

    public void updateOverlayForZone(String zone,
            boolean hasShirt,
            boolean hasShorts,
            TipoCorte currentCorteShort,
            org.example.component.renderer.GarmentRenderer shirtRenderer,
            ShortsRenderer shortsRenderer) {
        clearOverlay();
        if (zone == null || zone.isEmpty())
            return;

        String content = "";

        // Logic extracted from PrendaVisualizer
        if (hasShirt) {
            if ("PECHO".equals(zone)) {
                content = getSplitZoneContent(shirtRenderer.getBody(), true);
            } else if ("ESPALDA".equals(zone)) {
                content = getSplitZoneContent(shirtRenderer.getBody(), false);
            } else if ("MANGA_DELANTERA".equals(zone)) {
                content = getSplitZoneContent(shirtRenderer.getSleeves(), true);
            } else if ("MANGA_TRASERA".equals(zone)) {
                content = getSplitZoneContent(shirtRenderer.getSleeves(), false);
            }
        }

        if (hasShorts && currentCorteShort != TipoCorte.PANTALONETA
                && currentCorteShort != TipoCorte.LICRA) {
            if ("SHORT_FRONT".equals(zone)) {
                content = getSplitZoneContentForShorts(shortsRenderer.getShorts(), true);
            } else if ("SHORT_BACK".equals(zone)) {
                content = getSplitZoneContentForShorts(shortsRenderer.getShorts(), false);
            }
        }

        if (content != null && !content.isEmpty()) {
            overlayStroke = new SVGPath();
            overlayStroke.setContent(content);
            overlayStroke.setFill(Color.TRANSPARENT);
            overlayStroke.setStroke(Color.web("#FF3B30")); // Rojo vibrante moderno
            overlayStroke.setStrokeWidth(1.5); // Más delgado y elegante
            overlayStroke.getStrokeDashArray().setAll(6.0, 4.0); // Punteado más fino
            overlayStroke.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND); // Guiones redondeados
            overlayStroke.setMouseTransparent(true);
            overlayGroup.getChildren().add(overlayStroke);
        }
    }

    /**
     * Splits SVG path content into front/back halves using the START coordinate
     * of each subpath (the M command value), not the bounding box center.
     * This is reliable because M coordinates precisely mark where each sleeve piece
     * begins.
     * - Front sleeves (DELANTERA): startX < 380
     * - Back sleeves (TRASERA): startX >= 380
     */
    public String getSplitZoneContent(javafx.scene.shape.SVGPath sourcePath, boolean isVisualLeft) {
        String fullContent = sourcePath.getContent();
        if (fullContent == null || fullContent.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        String[] parts = fullContent.split("(?=[Mm])");
        double lastMoveX = 0.0;
        double lastMoveY = 0.0;

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty())
                continue;

            String absolutePart = org.example.utils.SVGUtils.absoluteifyPiece(part, lastMoveX, lastMoveY);
            double[] absCoords = org.example.utils.SVGUtils.parseCoordinatePair(absolutePart.substring(1));
            lastMoveX = absCoords[0];
            lastMoveY = absCoords[1];

            if (isVisualLeft) {
                if (isFrontPiece(lastMoveX))
                    sb.append(absolutePart).append(" ");
            } else {
                if (!isFrontPiece(lastMoveX))
                    sb.append(absolutePart).append(" ");
            }
        }

        return sb.toString().trim();
    }

    public String getSplitZoneContentForShorts(javafx.scene.shape.SVGPath sourcePath, boolean isFront) {
        String fullContent = sourcePath.getContent();
        if (fullContent == null || fullContent.isEmpty())
            return "";

        double boundsCenter = sourcePath.getLayoutBounds().getMinX() + (sourcePath.getLayoutBounds().getWidth() / 2.0);

        StringBuilder sb = new StringBuilder();
        String[] parts = fullContent.split("(?=[Mm])");
        double lastMoveX = 0.0;
        double lastMoveY = 0.0;

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty())
                continue;

            String absolutePart = org.example.utils.SVGUtils.absoluteifyPiece(part, lastMoveX, lastMoveY);
            double[] absCoords = org.example.utils.SVGUtils.parseCoordinatePair(absolutePart.substring(1));
            lastMoveX = absCoords[0];
            lastMoveY = absCoords[1];

            boolean isLeftPiece = lastMoveX < boundsCenter;
            boolean pieceIsFront = isFrontOnLeft ? isLeftPiece : !isLeftPiece;

            if (isFront) {
                if (pieceIsFront)
                    sb.append(absolutePart).append(" ");
            } else {
                if (!pieceIsFront)
                    sb.append(absolutePart).append(" ");
            }
        }

        return sb.toString().trim();
    }

    /**
     * Determines if a specific SVG path content (a piece) belongs to the "Front"
     * zone
     * based on its starting X coordinate.
     */
    public boolean isPieceFront(String pieceContent, double threshold) {
        String trimmed = pieceContent.trim();
        if (trimmed.isEmpty())
            return true;

        char cmd = trimmed.charAt(0);
        String rest = trimmed.length() > 1 ? trimmed.substring(1).trim() : "";
        double startX = 0;

        if (cmd == 'M' || cmd == 'm') {
            startX = org.example.utils.SVGUtils.parseLeadingDouble(rest);
        }

        return isFrontPiece(startX);
    }

}

