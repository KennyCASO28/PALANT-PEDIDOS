package org.example.model;

import java.util.Arrays;
import java.util.List;

public enum TipoTalla {
    XXXL, XXL, XL, L, M, S,
    T16("16"), T14("14"), T12("12"), T10("10"), T8("8"), T6("6"), T4("4"), T2("2");

    private final String label;

    TipoTalla() {
        this.label = name();
    }

    TipoTalla(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static List<String> getLabels() {
        return Arrays.stream(values())
                .map(TipoTalla::getLabel)
                .toList();
    }
}

