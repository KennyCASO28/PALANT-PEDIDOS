package org.example.service;

import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.*;

/**
 * Service for managing garment configuration business logic.
 * Handles validation and compatibility rules between different garment options.
 */
public class ConfiguracionPrendaService {

    /**
     * Validates if a sleeve length is compatible with a cut type and gender.
     * 
     * @param largo  Sleeve length
     * @param corte  Cut type
     * @param genero Gender
     * @return true if compatible, false otherwise
     */
    public boolean esLargoCompatible(TipoLargo largo, TipoCorte corte, TipoGenero genero) {
        // Rule 1: Cuadrado cut does not support 3/4 sleeve
        if (corte == TipoCorte.CUADRADO && largo == TipoLargo.MANGA_3_4) {
            return false;
        }

        // Rule 2: Non-Cuadrado cuts do not support sleeveless (Sisa)
        if (corte != TipoCorte.CUADRADO && largo == TipoLargo.MANGA_CERO) {
            return false;
        }

        // Rule 3: Redondo cut for women does not support 3/4 sleeve
        if (corte == TipoCorte.REDONDO && genero == TipoGenero.MUJER && largo == TipoLargo.MANGA_3_4) {
            return false;
        }

        return true;
    }

    /**
     * Validates if a short cut is compatible with a gender.
     * 
     * @param corteShort Short cut type
     * @param genero     Gender
     * @return true if compatible, false otherwise
     */
    public boolean esCorteShortCompatible(TipoCorte corteShort, TipoGenero genero) {
        if (genero == TipoGenero.HOMBRE) {
            // Men: LICRA and PANTALONETA not allowed
            return corteShort != TipoCorte.LICRA && corteShort != TipoCorte.PANTALONETA;
        } else if (genero == TipoGenero.MUJER) {
            // Women: NOVA not allowed
            return corteShort != TipoCorte.NOVA;
        }
        return true;
    }

    /**
     * Gets the default sleeve length for a given cut and gender.
     * 
     * @param corte  Cut type
     * @param genero Gender
     * @return Default sleeve length
     */
    public TipoLargo getLargoDefault(TipoCorte corte, TipoGenero genero) {
        return TipoLargo.MANGA_CORTA; // Safe default for all combinations
    }

    /**
     * Validates if short options are available for a given cut.
     * 
     * @param corteShort Short cut type
     * @return true if options like stripes, pockets are available
     */
    public boolean tieneOpcionesShort(TipoCorte corteShort) {
        // LICRA and PANTALONETA have limited options
        return corteShort != TipoCorte.LICRA && corteShort != TipoCorte.PANTALONETA;
    }

    /**
     * Creates a default configuration for a garment type.
     * 
     * @param tipoPrenda Garment type
     * @return Default configuration
     */
    public ConfiguracionPrendaDTO crearConfiguracionDefault(TipoPrenda tipoPrenda) {
        ConfiguracionPrendaDTO.Builder builder = new ConfiguracionPrendaDTO.Builder()
                .tipoPrenda(tipoPrenda)
                .genero(TipoGenero.HOMBRE)
                .corte(TipoCorte.CUADRADO)
                .largo(TipoLargo.MANGA_CORTA)
                .cuello(TipoCuello.V);

        // Special handling for Conjunto
        if (tipoPrenda == TipoPrenda.CONJUNTO) {
            builder.conShort(true)
                    .conMedias(true);
        }

        return builder.build();
    }

    /**
     * Validates a complete configuration for consistency.
     * 
     * @param config Configuration to validate
     * @return Validation error message, or null if valid
     */
    public String validarConfiguracion(ConfiguracionPrendaDTO config) {
        // Validate sleeve compatibility
        if (!esLargoCompatible(config.getLargo(), config.getCorte(), config.getGenero())) {
            return "La manga seleccionada no es compatible con el corte y género.";
        }

        // Validate short cut if present
        if (config.llevaShort() && config.getCorteShort() != null) {
            if (!esCorteShortCompatible(config.getCorteShort(), config.getGenero())) {
                return "El corte de short seleccionado no es compatible con el género.";
            }
        }

        // Validate conjunto requirements
        if (config.getTipoPrenda() == TipoPrenda.CONJUNTO) {
            if (!config.llevaShort()) {
                return "Un conjunto debe incluir short.";
            }
        }

        return null; // Valid
    }
}

