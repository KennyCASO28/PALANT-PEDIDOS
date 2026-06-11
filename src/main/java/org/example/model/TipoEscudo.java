package org.example.model;

public enum TipoEscudo {
    SUBLIMADO("Parche Sublimado"),
    BORDADO("Parche Bordado"),
    NINGUNO("Ninguno");

    private final String label;

    TipoEscudo(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TipoEscudo valueOfTech(String tech) {
        if (tech == null || tech.isBlank()) return BORDADO; // Safe check
        
        // Exact label match
        for (TipoEscudo t : values()) {
            if (t.label.equalsIgnoreCase(tech))
                return t;
        }
        
        // Exact enum name match
        for (TipoEscudo t : values()) {
            if (t.name().equalsIgnoreCase(tech))
                return t;
        }
        
        // Substring checks
        String lower = tech.toLowerCase();
        if (lower.contains("sublimado")) return SUBLIMADO;
        if (lower.contains("bordado")) return BORDADO;
        if (lower.contains("ningun")) return NINGUNO;

        return BORDADO; // Fallback
    }
}

