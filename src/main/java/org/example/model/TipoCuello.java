package org.example.model;

public enum TipoCuello {
    REDONDO("Redondo"),
    V("Cuello V"),
    CAMISERO("Camisero / Polo"),
    DIFUZO("Difuzo"),
    ANCHO("Ancho");

    private final String label;

    TipoCuello(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

