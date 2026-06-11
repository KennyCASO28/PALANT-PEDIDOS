package org.example.model;

import org.example.utils.IconosPrenda;

public enum TipoPrenda {
    CAMISETA("Camiseta", IconosPrenda.ICON_CAMISETA),
    SHORT("Short", IconosPrenda.ICON_SHORT),
    CONJUNTO("Conjunto", IconosPrenda.ICON_CONJUNTO),
    CHALECO("Chaleco", IconosPrenda.ICON_CHALECO),
    CASACA("Casaca", IconosPrenda.ICON_CASACA),
    PANTALON("Pantalón", IconosPrenda.ICON_PANTALON),
    BUZO("Buzo", IconosPrenda.ICON_BUZO),
    MOCHILA("Mochila", IconosPrenda.ICON_MOCHILA);

    private final String label;
    private final String iconPath;

    TipoPrenda(String label, String iconPath) {
        this.label = label;
        this.iconPath = iconPath;
    }

    public String getLabel() {
        return label;
    }

    public String getIconPath() {
        return iconPath;
    }
}

