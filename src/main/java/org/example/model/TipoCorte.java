package org.example.model;

public enum TipoCorte {
    CUADRADO("Cuadrado"),
    REDONDO("Redondo"),
    RANGLAN("Ranglan"),
    LICRA("Licra"),
    NOVA("NOVA (Deportivo/Vestir)"),
    PANTALONETA("Pantaloneta"); // Kept just in case, though user emphasized Cuadrado/Redondo

    private final String label;

    TipoCorte(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

