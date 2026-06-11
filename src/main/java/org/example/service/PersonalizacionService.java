package org.example.service;

import javafx.scene.paint.Color;
import org.example.component.PrendaVisualizer;
import org.example.dto.PersonalizacionDTO;
import org.example.model.ComponentePrenda;

/**
 * Service for managing garment personalization features.
 * Handles colors, sponsors, and embroidery application.
 */
public class PersonalizacionService {

    /**
     * Applies a color configuration to a visualizer.
     * 
     * @param visualizer      The garment visualizer
     * @param personalizacion Personalization configuration
     */
    public void aplicarColores(PrendaVisualizer visualizer, PersonalizacionDTO personalizacion) {
        if (visualizer == null || personalizacion == null) {
            return;
        }

        // Apply colors to each component
        Color colorCuerpo = personalizacion.getColor(ComponentePrenda.CUERPO);
        if (colorCuerpo != null) {
            visualizer.setColorBase(colorCuerpo);
        }

        Color colorCuello = personalizacion.getColor(ComponentePrenda.CUELLO);
        if (colorCuello != null) {
            visualizer.setCollarColor(colorCuello);
        }

        Color colorPunos = personalizacion.getColor(ComponentePrenda.PUNOS);
        if (colorPunos != null) {
            visualizer.setCuffColor(colorPunos);
        }

        Color colorShort = personalizacion.getColor(ComponentePrenda.SHORT);
        if (colorShort != null) {
            visualizer.setShortsColor(colorShort);
        }

        Color colorFranjaShort = personalizacion.getColor(ComponentePrenda.FRANJA_SHORT);
        if (colorFranjaShort != null) {
            visualizer.setShortsStripeColor(colorFranjaShort);
        }

        Color colorPunoShort = personalizacion.getColor(ComponentePrenda.PUNO_SHORT);
        if (colorPunoShort != null) {
            visualizer.setShortsCuffColor(colorPunoShort);
        }

        Color colorCordonShort = personalizacion.getColor(ComponentePrenda.CORDON_SHORT);
        if (colorCordonShort != null) {
            visualizer.setShortsCordColor(colorCordonShort);
        }

        System.out.println("✅ Colores aplicados: " + personalizacion.getColores().size() + " componentes");
    }

    /**
     * Creates a default color scheme for a garment.
     * 
     * @return Default personalization with standard colors
     */
    public PersonalizacionDTO crearEsquemaColorDefault() {
        return new PersonalizacionDTO.Builder()
                .color(ComponentePrenda.CUERPO, Color.web("#3498db"))
                .color(ComponentePrenda.CUELLO, Color.web("#2c3e50"))
                .color(ComponentePrenda.PUNOS, Color.web("#2c3e50"))
                .color(ComponentePrenda.SHORT, Color.web("#2c3e50"))
                .color(ComponentePrenda.MEDIAS, Color.web("#ecf0f1"))
                .build();
    }

    /**
     * Validates a color configuration.
     * 
     * @param personalizacion Personalization to validate
     * @return Validation error message, or null if valid
     */
    public String validarPersonalizacion(PersonalizacionDTO personalizacion) {
        if (personalizacion == null) {
            return "Personalización no puede ser nula";
        }

        // Validate that at least one color is defined
        if (personalizacion.getColores().isEmpty()) {
            return "Debe definir al menos un color";
        }

        // Additional validations can be added here
        // e.g., contrast validation, sponsor size limits, etc.

        return null; // Valid
    }

    /**
     * Applies sponsors/logos to a garment (placeholder for future implementation).
     * 
     * @param visualizer      The garment visualizer
     * @param personalizacion Personalization configuration
     */
    public void aplicarSponsors(PrendaVisualizer visualizer, PersonalizacionDTO personalizacion) {
        // TODO: Implement sponsor application logic
        System.out.println("ℹ️  Sponsors: " + personalizacion.getSponsors().size() + " configurados");
    }

    /**
     * Applies embroidery to a garment (placeholder for future implementation).
     * 
     * @param visualizer      The garment visualizer
     * @param personalizacion Personalization configuration
     */
    public void aplicarBordados(PrendaVisualizer visualizer, PersonalizacionDTO personalizacion) {
        // TODO: Implement embroidery application logic
        System.out.println("ℹ️  Bordados: " + personalizacion.getBordados().size() + " configurados");
    }
}

