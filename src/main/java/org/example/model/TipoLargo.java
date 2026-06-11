package org.example.model;

public enum TipoLargo {
    MANGA_CORTA("Manga Corta"),
    MANGA_LARGA("Manga Larga"),
    MANGA_3_4("Manga 3/4"),
    MANGA_CERO("Sin Mangas (0)");

    private final String label;

    TipoLargo(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

