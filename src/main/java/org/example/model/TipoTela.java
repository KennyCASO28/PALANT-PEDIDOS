package org.example.model;

public enum TipoTela {
    WIN("Win"),
    YACART("Yacart"),
    ESPIGA("Espiga"),
    DRAY("Dray"),
    OTRO("Otro");

    private final String label;

    TipoTela(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

