package org.example.utils;

import javafx.scene.paint.Color;

public class ColorUtils {
    public static Color darker(Color c) {
        return c.deriveColor(0, 1, 0.7, 1);
    }

    public static Color lighter(Color c) {
        return c.deriveColor(0, 1, 1.4, 1);
    }

    public static boolean isDark(Color c) {
        return c.getBrightness() < 0.6;
    }

    public static Color getSmartCombination(Color base) {
        double brightness = base.getBrightness();
        if (brightness < 0.15) {
            // Very dark (like Black) -> Make it significantly lighter (greyish/light shade)
            return base.deriveColor(0, 1.0, 1.0, 1.0).deriveColor(0, 0, 0.4, 1.0); 
        } else if (brightness < 0.5) {
            // Dark color -> Make it lighter
            return base.deriveColor(0, 0.8, 1.5, 1.0);
        } else if (brightness > 0.85) {
            // Very light (like White) -> Make it significantly darker
            return base.deriveColor(0, 1.0, 0.5, 1.0);
        } else {
            // Light/Medium color -> Make it darker (approx 2 tones)
            return base.deriveColor(0, 1.0, 0.7, 1.0);
        }
    }

    public static double[] toCmyk(Color c) {
        double r = c.getRed();
        double g = c.getGreen();
        double b = c.getBlue();
        double k = 1.0 - Math.max(r, Math.max(g, b));
        double cyan = (k == 1.0) ? 0 : (1.0 - r - k) / (1.0 - k);
        double magenta = (k == 1.0) ? 0 : (1.0 - g - k) / (1.0 - k);
        double yellow = (k == 1.0) ? 0 : (1.0 - b - k) / (1.0 - k);
        return new double[] { cyan, magenta, yellow, k };
    }

    public static Color fromCmyk(double c, double m, double y, double k, double opacity) {
        double r = (1.0 - c) * (1.0 - k);
        double g = (1.0 - m) * (1.0 - k);
        double b = (1.0 - y) * (1.0 - k);
        return Color.color(
                Math.max(0, Math.min(1, r)),
                Math.max(0, Math.min(1, g)),
                Math.max(0, Math.min(1, b)),
                opacity);
    }

    public static boolean areColorsSimilar(Color c1, Color c2, double tolerance) {
        if (c1 == null || c2 == null) return false;
        double rDiff = c1.getRed() - c2.getRed();
        double gDiff = c1.getGreen() - c2.getGreen();
        double bDiff = c1.getBlue() - c2.getBlue();
        double distance = Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
        return distance <= tolerance;
    }

    public static String toRGBCode(Color color) {
        if (color == null) return "#FFFFFF";
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
