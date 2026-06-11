package org.example.dto;

import javafx.scene.paint.Color;
import org.example.model.ComponentePrenda;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object for garment personalization features.
 * Encapsulates colors, sponsors, and embroidery configurations.
 */
public class PersonalizacionDTO {

    private final Map<ComponentePrenda, Color> colores;
    private final Map<String, SponsorConfig> sponsors;
    private final Map<String, BordadoConfig> bordados;

    private PersonalizacionDTO(Builder builder) {
        this.colores = new HashMap<>(builder.colores);
        this.sponsors = new HashMap<>(builder.sponsors);
        this.bordados = new HashMap<>(builder.bordados);
    }

    public Map<ComponentePrenda, Color> getColores() {
        return new HashMap<>(colores);
    }

    public Color getColor(ComponentePrenda componente) {
        return colores.get(componente);
    }

    public Map<String, SponsorConfig> getSponsors() {
        return new HashMap<>(sponsors);
    }

    public Map<String, BordadoConfig> getBordados() {
        return new HashMap<>(bordados);
    }

    /**
     * Configuration for a sponsor/logo.
     */
    public static class SponsorConfig {
        private final String nombre;
        private final String rutaImagen;
        private final String posicion; // "PECHO", "ESPALDA", "MANGA", etc.
        private final double escala;

        public SponsorConfig(String nombre, String rutaImagen, String posicion, double escala) {
            this.nombre = nombre;
            this.rutaImagen = rutaImagen;
            this.posicion = posicion;
            this.escala = escala;
        }

        public String getNombre() {
            return nombre;
        }

        public String getRutaImagen() {
            return rutaImagen;
        }

        public String getPosicion() {
            return posicion;
        }

        public double getEscala() {
            return escala;
        }
    }

    /**
     * Configuration for embroidery.
     */
    public static class BordadoConfig {
        private final String texto;
        private final String fuente;
        private final Color color;
        private final String posicion;

        public BordadoConfig(String texto, String fuente, Color color, String posicion) {
            this.texto = texto;
            this.fuente = fuente;
            this.color = color;
            this.posicion = posicion;
        }

        public String getTexto() {
            return texto;
        }

        public String getFuente() {
            return fuente;
        }

        public Color getColor() {
            return color;
        }

        public String getPosicion() {
            return posicion;
        }
    }

    /**
     * Builder for PersonalizacionDTO.
     */
    public static class Builder {
        private Map<ComponentePrenda, Color> colores = new HashMap<>();
        private Map<String, SponsorConfig> sponsors = new HashMap<>();
        private Map<String, BordadoConfig> bordados = new HashMap<>();

        public Builder color(ComponentePrenda componente, Color color) {
            this.colores.put(componente, color);
            return this;
        }

        public Builder sponsor(String id, SponsorConfig config) {
            this.sponsors.put(id, config);
            return this;
        }

        public Builder bordado(String id, BordadoConfig config) {
            this.bordados.put(id, config);
            return this;
        }

        public PersonalizacionDTO build() {
            return new PersonalizacionDTO(this);
        }
    }

    @Override
    public String toString() {
        return "PersonalizacionDTO{" +
                "colores=" + colores.size() + " componentes" +
                ", sponsors=" + sponsors.size() +
                ", bordados=" + bordados.size() +
                '}';
    }
}

