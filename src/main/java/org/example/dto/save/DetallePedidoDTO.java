package org.example.dto.save;

public class DetallePedidoDTO {
    private String nombre;
    private String numero;
    private String talla;
    private boolean includeTop;
    private boolean includeBottom;
    private boolean includeSocks;
    private String genero;

    // Arquero
    private boolean esArquero;
    private int arqueroOrdenMarcado;
    private String tipoMangaArquero;
    private String colorArquero; // Hex
    private String tipoManga;

    // Shorts
    private String tipoBottom;
    private String tallaShort;

    public DetallePedidoDTO() {
    }

    // Getters and Setters...
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public boolean isIncludeTop() {
        return includeTop;
    }

    public void setIncludeTop(boolean includeTop) {
        this.includeTop = includeTop;
    }

    public boolean isIncludeBottom() {
        return includeBottom;
    }

    public void setIncludeBottom(boolean includeBottom) {
        this.includeBottom = includeBottom;
    }

    public boolean isIncludeSocks() {
        return includeSocks;
    }

    public void setIncludeSocks(boolean includeSocks) {
        this.includeSocks = includeSocks;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public boolean isEsArquero() {
        return esArquero;
    }

    public void setEsArquero(boolean esArquero) {
        this.esArquero = esArquero;
    }

    public int getArqueroOrdenMarcado() {
        return arqueroOrdenMarcado;
    }

    public void setArqueroOrdenMarcado(int arqueroOrdenMarcado) {
        this.arqueroOrdenMarcado = arqueroOrdenMarcado;
    }

    public String getTipoMangaArquero() {
        return tipoMangaArquero;
    }

    public void setTipoMangaArquero(String tipoMangaArquero) {
        this.tipoMangaArquero = tipoMangaArquero;
    }

    public String getColorArquero() {
        return colorArquero;
    }

    public void setColorArquero(String colorArquero) {
        this.colorArquero = colorArquero;
    }

    public String getTipoManga() {
        return tipoManga;
    }

    public void setTipoManga(String tipoManga) {
        this.tipoManga = tipoManga;
    }

    public String getTipoBottom() {
        return tipoBottom;
    }

    public void setTipoBottom(String tipoBottom) {
        this.tipoBottom = tipoBottom;
    }

    public String getTallaShort() {
        return tallaShort;
    }

    public void setTallaShort(String tallaShort) {
        this.tallaShort = tallaShort;
    }
}

