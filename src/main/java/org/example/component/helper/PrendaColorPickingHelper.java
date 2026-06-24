package org.example.component.helper;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;

import java.util.List;
import java.util.function.Supplier;

public final class PrendaColorPickingHelper {

    private PrendaColorPickingHelper() {
    }

    public static Color pickDataColor(
            List<Node> userLayers,
            double sceneX,
            double sceneY,
            ShapeInteractionHelper shapeHelper,
            PrendaColorManager colorManager,
            Supplier<Color> fallbackPicker) {
        if (userLayers != null) {
            for (int i = userLayers.size() - 1; i >= 0; i--) {
                Color color = pickColorFromNodeRecursive(userLayers.get(i), sceneX, sceneY);
                if (color != null) {
                    return color;
                }
            }
        }

        if (shapeHelper != null && colorManager != null) {
            String zone = shapeHelper.detectZone(sceneX, sceneY);
            String colorKey = mapZoneToColorKey(zone);
            if (colorKey != null) {
                return colorManager.getPartColor(colorKey, Color.TRANSPARENT);
            }
        }

        return fallbackPicker != null ? fallbackPicker.get() : Color.TRANSPARENT;
    }

    private static Color pickColorFromNodeRecursive(Node node, double sceneX, double sceneY) {
        if (node == null || !node.isVisible() || node.isMouseTransparent()) {
            return null;
        }

        javafx.geometry.Point2D local = node.sceneToLocal(sceneX, sceneY);
        if (!node.contains(local)) {
            return null;
        }

        if (node instanceof ShapeLayer shapeLayer) {
            return shapeLayer.getFillColor();
        }
        if (node instanceof TextLayer textLayer) {
            return textLayer.getTextColor();
        }
        if (node instanceof GroupLayerV2 groupLayerV2) {
            for (Node child : groupLayerV2.getUserLayers()) {
                Color color = pickColorFromNodeRecursive(child, sceneX, sceneY);
                if (color != null) {
                    return color;
                }
            }
        }
        if (node instanceof GroupLayer groupLayer) {
            for (Node child : groupLayer.getUserLayers()) {
                Color color = pickColorFromNodeRecursive(child, sceneX, sceneY);
                if (color != null) {
                    return color;
                }
            }
        }
        return null;
    }

    private static String mapZoneToColorKey(String zone) {
        if (zone == null) {
            return null;
        }
        return switch (zone) {
            case "PECHO", "ESPALDA" -> "body";
            case "MANGA_DELANTERA", "MANGA_TRASERA" -> "sleeves";
            case "CUELLO" -> "collar";
            case "PUNO", "PU\u00d1O" -> "cuff";
            case "MALLA" -> "mesh";
            case "FRANJA_CAMISETA" -> "shirtStripe";
            case "LINEA_CAMISETA" -> "shirtLinea";
            case "SHORTS", "SHORT_FRONT", "SHORT_BACK" -> "shorts";
            case "FRANJA_SHORTS" -> "shortsStripe";
            case "LINEA_SHORTS" -> "shortsLinea";
            case "PIQUETE_SHORTS" -> "shortsPicket";
            case "PUNO_SHORTS", "PU\u00d1O_SHORTS" -> "shortsCuff";
            case "MEDIAS" -> "socks";
            case "MEDIAS_BORDE" -> "socksTop";
            case "DETALLE_MEDIAS" -> "socksDetail";
            default -> null;
        };
    }
}
