package org.example.component.renderer;

import javafx.scene.Node;
import org.example.model.PrendaState;
import org.example.model.TipoGenero;

/**
 * Interfaz base para cualquier renderizador de prenda (camisetas, shorts, medias, etc).
 * Facilita la adopción del principio Abierto/Cerrado (OCP) para futuras adiciones de prendas,
 * permitiendo inyectar nuevas prendas de manera agnóstica a PrendaVisualizer.
 */
public interface IGarmentRenderer {
    
    /**
     * Retorna el nodo gráfico raíz (Group, StackPane, etc.) generado por este renderizador.
     */
    Node getGraphicNode();

    /**
     * Alterna la visibilidad global de esta pieza gráfica.
     */
    void setVisible(boolean visible);

    /**
     * Sincroniza la arquitectura visual de la prenda basándose en el estado.
     */
    void updateFromState(PrendaState state);

    /**
     * Aplica la visibilidad de los complementos o partes internas.
     */
    void applyVisibilityTokens(PrendaState state);

    /**
     * Actualiza y posiciona marcas de branding.
     */
    void updateBranding(boolean hasBranding, String mainPath, String strokePath);
}
