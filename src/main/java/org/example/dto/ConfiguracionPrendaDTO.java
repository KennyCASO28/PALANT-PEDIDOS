package org.example.dto;

import org.example.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object that encapsulates all configuration for a garment.
 * Replaces 15+ boolean fields with a clean, immutable object.
 * 
 * Use the Builder pattern to create instances.
 */
public class ConfiguracionPrendaDTO {

    private final TipoPrenda tipoPrenda;
    private final TipoGenero genero;
    private final TipoCorte corte;
    private final TipoLargo largo;
    private final TipoCuello cuello;

    // Adicionales como Map para flexibilidad
    private final Map<String, Boolean> adicionales;

    // NEW: Fabric Type
    private final TipoTela tela;
    private final String customTela;
    private final boolean telaNatural;

    // Short configuration
    private final TipoCorte corteShort;

    // Shield Config
    private final TipoEscudo tipoEscudo;

    // NEW: Sock Type
    private final TipoMedias tipoMedias;

    // NEW: Detailed State for Reporting
    private final Map<String, String> colors;
    private final Map<String, String> internalCodes;

    private ConfiguracionPrendaDTO(Builder builder) {
        this.tipoPrenda = builder.tipoPrenda;
        this.genero = builder.genero;
        this.corte = builder.corte;
        this.largo = builder.largo;
        this.cuello = builder.cuello;
        this.adicionales = new HashMap<>(builder.adicionales);
        this.tela = builder.tela;
        this.customTela = builder.customTela;
        this.telaNatural = builder.telaNatural;
        this.corteShort = builder.corteShort;
        this.tipoEscudo = builder.tipoEscudo;
        this.tipoMedias = builder.tipoMedias;
        this.colors = new HashMap<>(builder.colors);
        this.internalCodes = new HashMap<>(builder.internalCodes);
    }

    // Getters
    public Map<String, String> getColors() {
        return new HashMap<>(colors);
    }

    public Map<String, String> getInternalCodes() {
        return new HashMap<>(internalCodes);
    }
    public TipoPrenda getTipoPrenda() {
        return tipoPrenda;
    }

    public TipoTela getTela() {
        return tela;
    }

    public String getCustomTela() {
        return customTela;
    }

    public boolean isTelaNatural() {
        return telaNatural;
    }

    public TipoGenero getGenero() {
        return genero;
    }

    public TipoCorte getCorte() {
        return corte;
    }

    public TipoLargo getLargo() {
        return largo;
    }

    public TipoCuello getCuello() {
        return cuello;
    }

    public TipoEscudo getTipoEscudo() {
        return tipoEscudo;
    }

    public Map<String, Boolean> getAdicionales() {
        return new HashMap<>(adicionales);
    }

    public TipoCorte getCorteShort() {
        return corteShort;
    }

    public TipoMedias getTipoMedias() {
        return tipoMedias;
    }

    // Convenience methods for adicionales
    public boolean tieneAdicional(String key) {
        return adicionales.getOrDefault(key, false);
    }

    public boolean llevaMalla() {
        return tieneAdicional("malla");
    }

    public boolean llevaPunoCamiseta() {
        return tieneAdicional("punoCamiseta");
    }

    public boolean llevaShort() {
        return tieneAdicional("short");
    }

    public boolean llevaMedias() {
        return tieneAdicional("medias");
    }

    public boolean llevaPunoShort() {
        return tieneAdicional("punoShort");
    }

    public boolean llevaFranjaShort() {
        return tieneAdicional("franjaShort");
    }

    public boolean llevaLineaShort() {
        return tieneAdicional("lineaShort");
    }

    public boolean llevaPiqueteShort() {
        return tieneAdicional("piqueteShort");
    }

    public boolean llevaBolsilloShort() {
        return tieneAdicional("bolsilloShort");
    }

    public boolean llevaPasadorShort() {
        return tieneAdicional("pasadorShort");
    }

    public boolean llevaLigaMedias() {
        return tieneAdicional("ligaMedias");
    }

    public boolean llevaForroShort() {
        return tieneAdicional("forroShort");
    }

    public boolean llevaAcolchado() {
        return tieneAdicional("acolchado");
    }

    public boolean llevaFranjaCamiseta() {
        return tieneAdicional("franjaCamiseta");
    }

    public boolean llevaLineaCamiseta() {
        return tieneAdicional("lineaCamiseta");
    }

    /**
     * Builder for ConfiguracionPrendaDTO.
     * Provides a fluent API for creating configuration objects.
     */
    public static class Builder {
        private TipoPrenda tipoPrenda;
        private TipoGenero genero = TipoGenero.HOMBRE;
        private TipoTela tela = TipoTela.WIN; // Default
        private String customTela = "";
        private boolean telaNatural = false;
        private TipoEscudo tipoEscudo = TipoEscudo.SUBLIMADO; // Default
        private TipoCorte corte = TipoCorte.CUADRADO;
        private TipoLargo largo = TipoLargo.MANGA_CORTA;
        private TipoCuello cuello = TipoCuello.V;
        private Map<String, Boolean> adicionales = new HashMap<>();
        private TipoCorte corteShort = TipoCorte.CUADRADO;
        private TipoMedias tipoMedias = TipoMedias.PROFESIONAL; // Default
        private Map<String, String> colors = new HashMap<>();
        private Map<String, String> internalCodes = new HashMap<>();

        public Builder tipoPrenda(TipoPrenda tipoPrenda) {
            this.tipoPrenda = tipoPrenda;
            return this;
        }

        public Builder colors(Map<String, String> colors) {
            if (colors != null)
                this.colors = new HashMap<>(colors);
            return this;
        }

        public Builder internalCodes(Map<String, String> codes) {
            if (codes != null)
                this.internalCodes = new HashMap<>(codes);
            return this;
        }

        public Builder genero(TipoGenero genero) {
            this.genero = genero;
            return this;
        }

        public Builder tela(TipoTela tela) {
            this.tela = tela;
            return this;
        }

        public Builder customTela(String customTela) {
            this.customTela = customTela;
            return this;
        }

        public Builder tipoEscudo(TipoEscudo tipoEscudo) {
            this.tipoEscudo = tipoEscudo;
            return this;
        }

        public Builder corte(TipoCorte corte) {
            this.corte = corte;
            return this;
        }

        public Builder largo(TipoLargo largo) {
            this.largo = largo;
            return this;
        }

        public Builder cuello(TipoCuello cuello) {
            this.cuello = cuello;
            return this;
        }

        public Builder corteShort(TipoCorte corteShort) {
            this.corteShort = corteShort;
            return this;
        }

        public Builder tipoMedias(TipoMedias tipoMedias) {
            this.tipoMedias = tipoMedias;
            return this;
        }

        public Builder adicional(String key, boolean value) {
            this.adicionales.put(key, value);
            return this;
        }

        // Convenience methods
        public Builder conMalla(boolean value) {
            return adicional("malla", value);
        }

        public Builder conPunoCamiseta(boolean value) {
            return adicional("punoCamiseta", value);
        }

        public Builder conFranjaCamiseta(boolean value) {
            return adicional("franjaCamiseta", value);
        }

        public Builder conLineaCamiseta(boolean value) {
            return adicional("lineaCamiseta", value);
        }

        public Builder conShort(boolean value) {
            return adicional("short", value);
        }

        public Builder conMedias(boolean value) {
            return adicional("medias", value);
        }

        public Builder conPunoShort(boolean value) {
            return adicional("punoShort", value);
        }

        public Builder conFranjaShort(boolean value) {
            return adicional("franjaShort", value);
        }

        public Builder conLineaShort(boolean value) {
            return adicional("lineaShort", value);
        }

        public Builder conPiqueteShort(boolean value) {
            return adicional("piqueteShort", value);
        }

        public Builder conBolsilloShort(boolean value) {
            return adicional("bolsilloShort", value);
        }

        public Builder conPasadorShort(boolean value) {
            return adicional("pasadorShort", value);
        }

        public Builder conLigaMedias(boolean value) {
            return adicional("ligaMedias", value);
        }

        public Builder conForroShort(boolean value) {
            return adicional("forroShort", value);
        }

        public Builder conAcolchado(boolean value) {
            return adicional("acolchado", value);
        }

        public Builder from(ConfiguracionPrendaDTO dto) {
            this.tipoPrenda = dto.getTipoPrenda();
            this.genero = dto.getGenero();
            this.tela = dto.getTela();
            this.customTela = dto.getCustomTela();
            this.tipoEscudo = dto.getTipoEscudo();
            this.corte = dto.getCorte();
            this.largo = dto.getLargo();
            this.cuello = dto.getCuello();
            this.adicionales = new HashMap<>(dto.getAdicionales());
            this.corteShort = dto.getCorteShort();
            this.tipoMedias = dto.getTipoMedias();
            this.colors = new HashMap<>(dto.getColors());
            this.internalCodes = new HashMap<>(dto.getInternalCodes());
            return this;
        }

        public ConfiguracionPrendaDTO build() {
            return new ConfiguracionPrendaDTO(this);
        }
    }

    @Override
    public String toString() {
        return "ConfiguracionPrendaDTO{" +
                "tipoPrenda=" + tipoPrenda +
                ", genero=" + genero +
                ", corte=" + corte +
                ", largo=" + largo +
                ", cuello=" + cuello +
                ", tela=" + tela +
                ", customTela='" + customTela + '\'' +
                ", tipoEscudo=" + tipoEscudo +
                ", adicionales=" + adicionales +
                ", corteShort=" + corteShort +
                ", tipoMedias=" + tipoMedias +
                '}';
    }
}

