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
        // 1. Shirt Detail Logic (cuffs, collar, mesh, stripes, lines)
        if (visualizer.hasShirt()) {
            org.example.component.renderer.GarmentRenderer r = visualizer.getActiveShirtRenderer();
            
            // Check Collar
            javafx.scene.shape.SVGPath collar = r.getCollar();
            if (collar != null && collar.isVisible()) {
                Point2D localCollar = collar.sceneToLocal(sceneX, sceneY);
                if (collar.getBoundsInLocal().contains(localCollar) && collar.contains(localCollar)) {
                    return "CUELLO";
                }
            }

            // Check Cuffs
            javafx.scene.shape.SVGPath cuffs = r.getCuffs();
            if (cuffs != null && cuffs.isVisible()) {
                Point2D localCuffs = cuffs.sceneToLocal(sceneX, sceneY);
                if (cuffs.getBoundsInLocal().contains(localCuffs) && cuffs.contains(localCuffs)) {
                    return "PUNO";
                }
            }

            // Check Mesh
            javafx.scene.shape.SVGPath mesh = r.getMesh();
            if (mesh != null && mesh.isVisible()) {
                Point2D localMesh = mesh.sceneToLocal(sceneX, sceneY);
                if (mesh.getBoundsInLocal().contains(localMesh) && mesh.contains(localMesh)) {
                    return "MALLA";
                }
            }

            // Check Shirt Stripe
            javafx.scene.shape.SVGPath shirtStripe = r.getShirtStripe();
            if (shirtStripe != null && shirtStripe.isVisible()) {
                Point2D localStripe = shirtStripe.sceneToLocal(sceneX, sceneY);
                if (shirtStripe.getBoundsInLocal().contains(localStripe) && shirtStripe.contains(localStripe)) {
                    return "FRANJA_CAMISETA";
                }
            }

            // Check Shirt Line
            javafx.scene.shape.SVGPath shirtLinea = r.getShirtLinea();
            if (shirtLinea != null && shirtLinea.isVisible()) {
                Point2D localLinea = shirtLinea.sceneToLocal(sceneX, sceneY);
                if (shirtLinea.getBoundsInLocal().contains(localLinea) && shirtLinea.contains(localLinea)) {
                    return "LINEA_CAMISETA";
                }
            }
        }

        // 2. Shorts Detail Logic (stripes, lines, pickets, cuffs)
        if (visualizer.hasShorts()) {
            org.example.component.renderer.GarmentRenderer r = visualizer.getActiveShortsRenderer();

            // Check Shorts Stripe
            javafx.scene.shape.SVGPath shortsStripe = r.getShortsStripe();
            if (shortsStripe != null && shortsStripe.isVisible()) {
                Point2D localStripe = shortsStripe.sceneToLocal(sceneX, sceneY);
                if (shortsStripe.getBoundsInLocal().contains(localStripe) && shortsStripe.contains(localStripe)) {
                    return "FRANJA_SHORTS";
                }
            }

            // Check Shorts Line
            javafx.scene.shape.SVGPath shortsLinea = r.getShortsLinea();
            if (shortsLinea != null && shortsLinea.isVisible()) {
                Point2D localLinea = shortsLinea.sceneToLocal(sceneX, sceneY);
                if (shortsLinea.getBoundsInLocal().contains(localLinea) && shortsLinea.contains(localLinea)) {
                    return "LINEA_SHORTS";
                }
            }

            // Check Shorts Picket
            javafx.scene.shape.SVGPath shortsPicket = r.getShortsPicket();
            if (shortsPicket != null && shortsPicket.isVisible()) {
                Point2D localPicket = shortsPicket.sceneToLocal(sceneX, sceneY);
                if (shortsPicket.getBoundsInLocal().contains(localPicket) && shortsPicket.contains(localPicket)) {
                    return "PIQUETE_SHORTS";
                }
            }

            // Check Shorts Cuff
            javafx.scene.shape.SVGPath shortsCuff = r.getShortsCuff();
            if (shortsCuff != null && shortsCuff.isVisible()) {
                Point2D localCuff = shortsCuff.sceneToLocal(sceneX, sceneY);
                if (shortsCuff.getBoundsInLocal().contains(localCuff) && shortsCuff.contains(localCuff)) {
                    return "PUNO_SHORTS";
                }
            }
        }

        // 3. Socks Logic (borde/top, detalles, base)
        if (visualizer.hasSocks()) {
            org.example.component.renderer.GarmentRenderer r = visualizer.getActiveSocksRenderer();
            if (r != null) {
                // Check Socks Top
                javafx.scene.shape.SVGPath socksTop = r.getSocksTop();
                if (socksTop != null && socksTop.isVisible()) {
                    Point2D localTop = socksTop.sceneToLocal(sceneX, sceneY);
                    if (socksTop.getBoundsInLocal().contains(localTop) && socksTop.contains(localTop)) {
                        return "MEDIAS_BORDE";
                    }
                }

                // Check Socks Detail
                javafx.scene.shape.SVGPath socksDetail = r.getSocksDetail();
                if (socksDetail != null && socksDetail.isVisible()) {
                    Point2D localDetail = socksDetail.sceneToLocal(sceneX, sceneY);
                    if (socksDetail.getBoundsInLocal().contains(localDetail) && socksDetail.contains(localDetail)) {
                        return "DETALLE_MEDIAS";
                    }
                }

                // Check Socks Body
                javafx.scene.shape.SVGPath socksBody = r.getBody();
                if (socksBody != null && socksBody.isVisible()) {
                    Point2D localBody = socksBody.sceneToLocal(sceneX, sceneY);
                    if (socksBody.getBoundsInLocal().contains(localBody) && socksBody.contains(localBody)) {
                        return "MEDIAS";
                    }
                }
            }
        }

        // 4. Base Shirt Logic (body, sleeves)
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

        // 5. Base Shorts Logic
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
            return visualizer.getOverlayManager().getSplitZoneContentForShorts(visualizer.getActiveShortsRenderer().getShorts(), true);
        } else if ("SHORT_BACK".equals(zone)) {
            return visualizer.getOverlayManager().getSplitZoneContentForShorts(visualizer.getActiveShortsRenderer().getShorts(),
                    false);
        }
        return "";
    }
}
