package org.example.model;

public enum TipoGenero {
    HOMBRE("Hombre"),
    MUJER("Mujer"),
    UNISEX("Unisex");

    private final String label;

    TipoGenero(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

