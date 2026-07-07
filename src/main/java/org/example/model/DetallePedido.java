package org.example.model;

import javafx.beans.property.*;
import javafx.scene.paint.Color;

public class DetallePedido {
    public static java.util.function.Supplier<String> defaultSocksTypeProvider = () -> null;

    private final StringProperty nombre = new SimpleStringProperty("");
    private final StringProperty numero = new SimpleStringProperty("");
    private final StringProperty talla = new SimpleStringProperty("");

    private final BooleanProperty includeTop = new SimpleBooleanProperty(true);
    private final BooleanProperty includeBottom = new SimpleBooleanProperty(true);
    private final BooleanProperty includeSocks = new SimpleBooleanProperty(true);

    private final StringProperty genero = new SimpleStringProperty("HOMBRE");

    private final BooleanProperty esArquero = new SimpleBooleanProperty(false);
    private final StringProperty tipoMangaArquero = new SimpleStringProperty("CORTA");
    private final StringProperty tipoManga = new SimpleStringProperty("CORTA");
    private final ObjectProperty<Color> colorArquero = new SimpleObjectProperty<>(Color.YELLOW);

    private final StringProperty tipoBottom = new SimpleStringProperty("Short");
    private final StringProperty tallaShort = new SimpleStringProperty("");
    private final StringProperty tipoMedias = new SimpleStringProperty("PROFESIONAL");
    private final IntegerProperty arqueroOrdenMarcado = new SimpleIntegerProperty(0);
    private final StringProperty arqueroDesignId = new SimpleStringProperty("");

    public DetallePedido() {
        setTalla(""); // Initialize with default logic
    }

    public DetallePedido(String nombre, String numero, String talla) {
        setNombre(nombre);
        this.numero.set(numero);
        setTalla(talla);
        this.tallaShort.set(talla);
    }

    public DetallePedido(String nombre, String numero, String talla, String genero) {
        setNombre(nombre);
        this.numero.set(numero);
        setTalla(talla);
        this.tallaShort.set(talla);
        this.genero.set(genero);
    }

    // --- GETTERS / SETTERS / PROPERTIES ---

    public String getNombre() { return nombre.get(); }
    public void setNombre(String v) { 
        this.nombre.set(v); 
        if (v != null) {
            String upper = v.trim().toUpperCase();
            if (upper.equals("ARQUERO") || upper.startsWith("ARQUERO ") || upper.startsWith("ARQUERO-") || upper.startsWith("ARQUERO_")) {
                setEsArquero(true);
            }
        }
    }
    public StringProperty nombreProperty() { return nombre; }

    public String getNumero() { return numero.get(); }
    public void setNumero(String v) { this.numero.set(v); }
    public StringProperty numeroProperty() { return numero; }

    public String getTalla() { return talla.get(); }
    public void setTalla(String v) { 
        this.talla.set(v); 
        // Force auto-calculation of socks type or use default bulk category if active
        String providerDefault = defaultSocksTypeProvider != null ? defaultSocksTypeProvider.get() : null;
        if (providerDefault != null) {
            setTipoMedias(providerDefault);
        } else {
            setTipoMedias(resolveAutoSocksType(v));
        }
    }
    public StringProperty tallaProperty() { return talla; }

    public boolean isIncludeTop() { return includeTop.get(); }
    public void setIncludeTop(boolean v) { this.includeTop.set(v); }
    public BooleanProperty includeTopProperty() { return includeTop; }

    public boolean isIncludeBottom() { return includeBottom.get(); }
    public void setIncludeBottom(boolean v) { this.includeBottom.set(v); }
    public BooleanProperty includeBottomProperty() { return includeBottom; }

    public boolean isIncludeSocks() { return includeSocks.get(); }
    public void setIncludeSocks(boolean v) { this.includeSocks.set(v); }
    public BooleanProperty includeSocksProperty() { return includeSocks; }

    public String getGenero() { return genero.get(); }
    public void setGenero(String v) { this.genero.set(v); }
    public StringProperty generoProperty() { return genero; }

    public boolean isEsArquero() { return esArquero.get(); }
    public void setEsArquero(boolean v) {
        this.esArquero.set(v);
        if (v) {
            ensureArqueroDesignId();
            String mangaActual = getTipoManga();
            if (mangaActual != null && !mangaActual.isBlank()) {
                this.tipoMangaArquero.set(mangaActual);
            } else if (this.tipoMangaArquero.get() == null || this.tipoMangaArquero.get().isBlank()) {
                this.tipoMangaArquero.set("CORTA");
            }
        }
    }
    public BooleanProperty esArqueroProperty() { return esArquero; }

    public String getTipoMangaArquero() { return tipoMangaArquero.get(); }
    public void setTipoMangaArquero(String v) {
        this.tipoMangaArquero.set(v);
        this.tipoManga.set(v); // Keep in sync for the roster table
    }
    public StringProperty tipoMangaArqueroProperty() { return tipoMangaArquero; }

    public String getTipoManga() { return tipoManga.get(); }
    public void setTipoManga(String v) {
        this.tipoManga.set(v);
        if (isEsArquero()) {
            this.tipoMangaArquero.set(v); // Keep in sync for goalkeeper config
        }
    }
    public StringProperty tipoMangaProperty() { return tipoManga; }

    public Color getColorArquero() { return colorArquero.get(); }
    public void setColorArquero(Color v) { this.colorArquero.set(v); }
    public ObjectProperty<Color> colorArqueroProperty() { return colorArquero; }

    public String getTipoBottom() { return tipoBottom.get(); }
    public void setTipoBottom(String v) { this.tipoBottom.set(v); }
    public StringProperty tipoBottomProperty() { return tipoBottom; }

    public String getTallaShort() { return tallaShort.get(); }
    public void setTallaShort(String v) { this.tallaShort.set(v); }
    public StringProperty tallaShortProperty() { return tallaShort; }

    public String getTipoMedias() { return tipoMedias.get(); }
    public void setTipoMedias(String v) { this.tipoMedias.set(v); }
    public StringProperty tipoMediasProperty() { return tipoMedias; }

    public int getArqueroOrdenMarcado() { return arqueroOrdenMarcado.get(); }
    public void setArqueroOrdenMarcado(int v) { this.arqueroOrdenMarcado.set(v); }
    public IntegerProperty arqueroOrdenMarcadoProperty() { return arqueroOrdenMarcado; }

    public String getArqueroDesignId() { return arqueroDesignId.get(); }
    public void setArqueroDesignId(String v) { this.arqueroDesignId.set(v != null ? v : ""); }
    public StringProperty arqueroDesignIdProperty() { return arqueroDesignId; }

    public String ensureArqueroDesignId() {
        if (getArqueroDesignId() == null || getArqueroDesignId().isBlank()) {
            setArqueroDesignId("arq-" + java.util.UUID.randomUUID());
        }
        return getArqueroDesignId();
    }

    private String resolveAutoSocksType(String t) {
        if (t == null) return "PROFESIONAL";
        String tag = t.toUpperCase().trim();
        // Categorizar segun talla
        if (tag.matches("S|M|L|XL|XXS|XXXS|XXL|3XL|4XL|5XL|G|XG|EG|EEG")) return "ADULTO";
        if (tag.matches("10|12|14|16")) return "JUVENIL";
        if (tag.matches("2|4|6|8")) return "NIÑOS";
        return "PROFESIONAL";
    }
}
