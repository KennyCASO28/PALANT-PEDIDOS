package org.example.component.helper;

import org.example.component.PrendaVisualizer;
import org.example.component.NumberComposition;
import org.example.model.PrendaState;
import org.example.component.renderer.ShirtRenderer;
import org.example.component.renderer.ShortsRenderer;
import org.example.component.renderer.SocksRenderer;

/**
 * Manages the visibility of all garment components (Shirt, Shorts, Socks, Numbers)
 * across different design modes (Field Player vs Goalkeeper).
 */
public class GarmentVisibilityManager {

    private final PrendaVisualizer visualizer;

    public GarmentVisibilityManager(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    /**
     * Updates the visibility of all visual components based on the current state.
     * This method handles the binary logic between Player and Goalkeeper designs.
     */
    public void applyVisibility() {
        boolean isArquero = visualizer.isEditandoArquero();
        PrendaState state = visualizer.getState();
        GarmentNumberManager numberManager = visualizer.getNumberManager();

        // --- FIELD PLAYER COMPONENTS ---
        ShirtRenderer shirtR = visualizer.getShirtRenderer();
        ShortsRenderer shortsR = visualizer.getShortsRenderer();
        SocksRenderer socksR = visualizer.getSocksRenderer();

        if (shirtR != null) shirtR.setVisible(!isArquero && state.hasShirt());
        if (shortsR != null) shortsR.setVisible(!isArquero && state.hasShorts());
        if (socksR != null) {
            socksR.setVisible(!isArquero && state.hasSocks());
            socksR.setSocksTopVisible(state.hasSocksTop());
        }

        // Field Player Numbers: ONLY if NOT in arquero mode
        updateNumberVisibility(numberManager, false, !isArquero, state);

        // Goalkeeper Components
        org.example.component.renderer.ArqueroRenderer arqShirtR = visualizer.getArqueroShirtRenderer();
        org.example.component.renderer.ShortsRenderer arqShortsR = visualizer.getArqueroShortsRenderer();
        org.example.component.renderer.SocksRenderer arqSocksR = visualizer.getArqueroSocksRenderer();

        if (arqShirtR != null) {
            arqShirtR.setVisible(isArquero && state.hasShirt());
        }
        if (arqShortsR != null) {
            arqShortsR.setVisible(isArquero && state.hasShorts());
        }
        if (arqSocksR != null) {
            arqSocksR.setVisible(isArquero && state.hasSocks());
            arqSocksR.setSocksTopVisible(state.hasSocksTop());
        }

        // Goalkeeper Numbers: ONLY if in arquero mode
        updateNumberVisibility(numberManager, true, isArquero, state);

        // --- Viewport Auto-Scale Optimization ---
        if (visualizer.getViewportController() != null && !visualizer.isTakingSnapshot()) {
            javafx.application.Platform.runLater(visualizer.getViewportController()::autoScale);
        }

        visualizer.notifyStateChanged();
    }

    private void updateNumberVisibility(GarmentNumberManager nm, boolean forArquero, boolean isAllowedMode, PrendaState state) {
        NumberComposition chest = nm.getChestNumber(forArquero);
        NumberComposition back = nm.getBackNumber(forArquero);
        NumberComposition shortNum = nm.getShortNumber(forArquero);

        if (chest != null) {
            chest.setVisible(isAllowedMode && state.hasShirt() && state.isChestNumberVisible()
                    && nm.hasNumberDigit(state.getCurrentChestNumber()));
        }
        if (back != null) {
            back.setVisible(isAllowedMode && state.hasShirt() && state.isBackNumberVisible()
                    && nm.hasNumberDigit(state.getCurrentBackNumber()));
        }
        if (shortNum != null) {
            shortNum.setVisible(isAllowedMode && state.hasShorts() && state.isShortNumberVisible()
                    && nm.hasNumberDigit(state.getCurrentShortNumber()));
        }
    }
}
