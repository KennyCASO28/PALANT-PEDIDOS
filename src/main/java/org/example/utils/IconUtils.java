package org.example.utils;

import org.kordamp.ikonli.javafx.FontIcon;

public class IconUtils {
    public static FontIcon crearIcono(String iconCode, int iconSize, String colorHex) {
        FontIcon icon = new FontIcon();
        try {
            icon.setIconLiteral(iconCode);
            icon.setIconSize(iconSize);
            icon.setIconColor(javafx.scene.paint.Color.web(colorHex));
        } catch (Exception e) {
            icon = new FontIcon("mdi2h-help");
            icon.setIconSize(iconSize);
            icon.setIconColor(javafx.scene.paint.Color.web(colorHex));
        }
        return icon;
    }
}
