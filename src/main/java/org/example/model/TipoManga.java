package org.example.model;

public enum TipoManga {
    CORTA_REDONDA("Corta (Redonda)"),
    CORTA_RAGLAN("Corta (Raglan)"),
    LARGA("Larga"),
    CERO("Cero / Musculosa"),
    CUADRADA("Cuadrada"); // For specific user request

    private final String label;

    TipoManga(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

