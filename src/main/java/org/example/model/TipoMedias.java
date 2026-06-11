package org.example.model;

/**
 * Enumeración para los tipos de medias disponibles.
 */
public enum TipoMedias {
    PROFESIONAL("Profesional"),
    ADULTO("Adulto"),
    JUVENIL("Juvenil"),
    NINOS("Niños");

    private final String label;

    TipoMedias(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}

