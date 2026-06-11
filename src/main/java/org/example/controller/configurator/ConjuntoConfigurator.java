package org.example.controller.configurator;

import javafx.scene.layout.VBox;
import org.example.component.PrendaVisualizer;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.TipoGenero;
import org.example.model.TipoPrenda;

/**
 * Configurator for conjunto (set) garments.
 * Delegates to CamisetaConfigurator which now handles full set logic
 * efficiently.
 */
public class ConjuntoConfigurator {

    private final CamisetaConfigurator camisetaConfig;

    public void setOnConfigChanged(Runnable listener) {
        camisetaConfig.setOnConfigChanged(listener);
    }

    public ConjuntoConfigurator(VBox container, PrendaVisualizer visualizer) {
        this.camisetaConfig = new CamisetaConfigurator(container, visualizer);
    }

    /**
     * Builds the complete UI for conjunto configuration.
     */
    public void buildUI(TipoGenero genero, ConfiguracionPrendaDTO previousConfig) {
        // Prepare config for Conjunto Mode (Forces Shorts + Medias if clean start)
        if (previousConfig == null) {
            previousConfig = new ConfiguracionPrendaDTO.Builder()
                    .tipoPrenda(TipoPrenda.CONJUNTO)
                    .genero(genero)
                    .conShort(true)
                    .conMedias(true)
                    .conPasadorShort(true) // Force Pasador ON by default for Sets
                    .build();
        } else {
            // Ensure visualizer knows it's a conjunto setup
            previousConfig = new ConfiguracionPrendaDTO.Builder()
                    .from(previousConfig)
                    .tipoPrenda(TipoPrenda.CONJUNTO) // Ensure type
                    .build();
        }

        // Delegate to CamisetaConfigurator which includes the "Completer Conjunto"
        // section via ShortConfigurator
        camisetaConfig.buildUI(genero, previousConfig, TipoPrenda.CONJUNTO);
    }

    /**
     * Gets the combined configuration.
     */
    public ConfiguracionPrendaDTO getConfiguration() {
        ConfiguracionPrendaDTO config = camisetaConfig.getConfigBuilder().build();
        return new ConfiguracionPrendaDTO.Builder()
                .from(config)
                .tipoPrenda(TipoPrenda.CONJUNTO)
                .build();
    }

    public CamisetaConfigurator getCamisetaConfig() {
        return camisetaConfig;
    }
}
