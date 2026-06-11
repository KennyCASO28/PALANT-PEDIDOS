package org.example.component.helper;

import javafx.scene.Group;
import javafx.scene.Node;
import org.example.component.renderer.IGarmentRenderer;
import org.example.model.PrendaState;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor dinámico de Renderizadores.
 * Permite registrar múltiples prendas ("Camiseta", "Short", "Medias", "Buzo", etc.)
 * y sincronizarlas de una sola vez, en lugar de invocar a cada una manualmente en el Visualizador.
 */
public class GarmentRendererManager {
    
    private final List<IGarmentRenderer> renderers = new ArrayList<>();
    private final Group container;

    public GarmentRendererManager(Group container) {
        this.container = container;
    }

    public void registerRenderer(IGarmentRenderer renderer) {
        if (!renderers.contains(renderer)) {
            renderers.add(renderer);
            Node graphic = renderer.getGraphicNode();
            if (graphic != null && !container.getChildren().contains(graphic)) {
                container.getChildren().add(graphic);
            }
        }
    }

    public void removeRenderer(IGarmentRenderer renderer) {
        if (renderers.remove(renderer)) {
            Node graphic = renderer.getGraphicNode();
            if (graphic != null) {
                container.getChildren().remove(graphic);
            }
        }
    }

    /**
     * Sincroniza todas las prendas registradas con el estado actual.
     */
    public void syncAllWithState(PrendaState state) {
        for (IGarmentRenderer r : renderers) {
            r.updateFromState(state);
            r.applyVisibilityTokens(state);
        }
    }

    /**
     * Alterna la visibilidad general.
     */
    public void setAllVisible(boolean visible) {
        for (IGarmentRenderer r : renderers) {
            r.setVisible(visible);
        }
    }
}
