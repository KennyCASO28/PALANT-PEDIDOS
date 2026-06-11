package org.example.model;

public enum ShapeType {
    RECTANGLE("Rectángulo"),
    CIRCLE("Círculo"),
    TRIANGLE("Triángulo"),
    STAR("Estrella"),
    PENTAGON("Pentágono"),
    HEXAGON("Hexágono"),
    CUSTOM_PATH("Lápiz");

    private final String displayName;

    ShapeType(String displayName) {
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

