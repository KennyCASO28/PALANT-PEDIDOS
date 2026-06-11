package org.example.model;

/**
 * Defines the available shapes for text rendering.
 * Used by TextLayer to determine how text should be curved/arranged.
 */
public enum TextShape {
    STRAIGHT("Recto"),
    ARC_TOP("Arco Superior"),
    ARC_BOTTOM("Arco Inferior"),
    CIRCULAR("Circular"),
    OVAL("Óvalo"),
    WAVE("Ondulado"),
    BEZIER("Personalizado");

    private final String displayName;

    TextShape(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
