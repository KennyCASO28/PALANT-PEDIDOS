package org.example.component.helper;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import org.example.component.PrendaVisualizer;

/**
 * Servicio encargado de la detección de zonas en las prendas (SVG Hit Testing).
 * Desacoplado de la lógica de interacción para facilitar su reutilización.
 */
public class GarmentZoneService {

    private final PrendaVisualizer visualizer;

    public GarmentZoneService(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    /**
     * Detecta en qué zona de la prenda se encuentran las coordenadas de la escena proporcionadas.
     * @param sceneX Coordenada X en la escena.
     * @param sceneY Coordenada Y en la escena.
     * @return El nombre de la zona ("PECHO", "ESPALDA", "MANGA_DELANTERA", etc.) o null si no hay coincidencia.
     */
    public String detectZone(double sceneX, double sceneY) {
        // Shirt Logic
        if (visualizer.hasShirt()) {
            javafx.scene.shape.SVGPath body = visualizer.getActiveShirtRenderer().getBody();
            Point2D localBody = body.sceneToLocal(sceneX, sceneY);
            
            if (body.getBoundsInLocal().contains(localBody)) {
                if (body.contains(localBody)) {
                    Bounds b = body.getLayoutBounds();
                    boolean isLeft = localBody.getX() < b.getMinX() + b.getWidth() / 2;
                    boolean isFrontOnLeft = visualizer.getOverlayManager().isFrontOnLeft();
                    if (isFrontOnLeft)
                        return isLeft ? "PECHO" : "ESPALDA";
                    else
                        return isLeft ? "ESPALDA" : "PECHO";
                }
            }

            javafx.scene.shape.SVGPath sleeves = visualizer.getActiveShirtRenderer().getSleeves();
            Point2D localSleeves = sleeves.sceneToLocal(sceneX, sceneY);
            if (sleeves.getBoundsInLocal().contains(localSleeves) && sleeves.contains(localSleeves)) {
                // Piece-aware Sleeve Detection
                String content = sleeves.getContent();
                if (content != null) {
                    String[] parts = content.split("(?=[Mm])");
                    double lastX = 0;
                    double lastY = 0;
                    for (String piece : parts) {
                        if (piece.trim().isEmpty())
                            continue;

                        javafx.scene.shape.SVGPath piecePath = new javafx.scene.shape.SVGPath();
                        String absoluteContent = org.example.utils.SVGUtils.absoluteifyPiece(piece, lastX, lastY);
                        piecePath.setContent(absoluteContent);

                        double[] absCoords = org.example.utils.SVGUtils.parseCoordinatePair(absoluteContent.substring(1));
                        lastX = absCoords[0];
                        lastY = absCoords[1];

                        if (piecePath.contains(localSleeves)) {
                            if (visualizer.getOverlayManager().isFrontPiece(lastX)) {
                                return "MANGA_DELANTERA";
                            } else {
                                return "MANGA_TRASERA";
                            }
                        }
                    }
                }

                // Fallback
                double threshold = visualizer.getOverlayManager().getSleeveThreshold();
                boolean isLeftClick = localSleeves.getX() < threshold;
                boolean isFrontOnLeft = visualizer.getOverlayManager().isFrontOnLeft();
                if (isFrontOnLeft)
                    return isLeftClick ? "MANGA_DELANTERA" : "MANGA_TRASERA";
                else
                    return isLeftClick ? "MANGA_TRASERA" : "MANGA_DELANTERA";
            }
        }

        // Shorts Logic
        if (visualizer.hasShorts()) {
            javafx.scene.shape.SVGPath shorts = visualizer.getActiveShortsRenderer().getShorts();
            Point2D localShorts = shorts.sceneToLocal(sceneX, sceneY);
            if (shorts.getBoundsInLocal().contains(localShorts) && shorts.contains(localShorts)) {
                Bounds b = shorts.getLayoutBounds();
                boolean isLeft = localShorts.getX() < b.getMinX() + b.getWidth() / 2;
                boolean isFrontOnLeft = visualizer.getOverlayManager().isFrontOnLeft();
                if (isFrontOnLeft)
                    return isLeft ? "SHORT_FRONT" : "SHORT_BACK";
                else
                    return isLeft ? "SHORT_BACK" : "SHORT_FRONT";
            }
        }
        return null;
    }

    public javafx.scene.Node getZoneNode(String zone) {
        if (visualizer.hasShirt()) {
            if ("PECHO".equals(zone) || "ESPALDA".equals(zone))
                return visualizer.getActiveShirtRenderer().getBody();
            if ("MANGA_DELANTERA".equals(zone) || "MANGA_TRASERA".equals(zone))
                return visualizer.getActiveShirtRenderer().getSleeves();
        }
        if (visualizer.hasShorts()) {
            if ("SHORT_FRONT".equals(zone) || "SHORT_BACK".equals(zone))
                return visualizer.getActiveShortsRenderer().getShorts();
        }
        return null; // Fallback
    }

    public String getZoneSvgContent(String zone) {
        if (visualizer.getOverlayManager() == null)
            return "";

        if (zone.startsWith("MANGA")) {
            return visualizer.getOverlayManager().getSplitZoneContent(visualizer.getActiveShirtRenderer().getSleeves(),
                    "MANGA_DELANTERA".equals(zone));
        } else if ("PECHO".equals(zone)) {
            return visualizer.getOverlayManager().getSplitZoneContent(visualizer.getActiveShirtRenderer().getBody(), true);
        } else if ("ESPALDA".equals(zone)) {
            return visualizer.getOverlayManager().getSplitZoneContent(visualizer.getActiveShirtRenderer().getBody(), false);
        } else if ("SHORT_FRONT".equals(zone)) {
            return visualizer.getOverlayManager().getSplitZoneContent(visualizer.getActiveShortsRenderer().getShorts(), true);
        } else if ("SHORT_BACK".equals(zone)) {
            return visualizer.getOverlayManager().getSplitZoneContent(visualizer.getActiveShortsRenderer().getShorts(),
                    false);
        }
        return "";
    }
}
