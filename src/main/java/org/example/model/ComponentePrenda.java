package org.example.model;

/**
 * Enum representing the different components of a garment
 * that can be customized with colors.
 */
public enum ComponentePrenda {
    CUERPO("Cuerpo Principal"),
    CUELLO("Cuello"),
    MANGAS("Mangas"),
    PUNOS("Puños"),
    MALLA("Malla Lateral"),
    SHORT("Short"),
    FRANJA_SHORT("Franja del Short"),
    PUNO_SHORT("Puño del Short"),
    CORDON_SHORT("Cordón del Short"),
    MEDIAS("Medias"),
    BOLSILLO("Bolsillo");

    private final String label;

    ComponentePrenda(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

