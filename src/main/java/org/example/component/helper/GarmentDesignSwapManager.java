package org.example.component.helper;

import java.util.ArrayList;
import java.util.List;
import org.example.component.PrendaVisualizer;
import org.example.model.PrendaState;
import org.example.service.save.StateMapper;
import org.example.component.HotspotLayer;

public class GarmentDesignSwapManager {

    private final PrendaVisualizer visualizer;

    public GarmentDesignSwapManager(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    /**
     * Synchronizes shared attributes from one design to another.
     * Traditionally used to inherit player design into the goalkeeper.
     */
    public void sincronizarAtributosCompartidos(PrendaState source, PrendaState target) {
        boolean shouldReloadVisibleTarget = org.example.component.PrendaGoalkeeperDesignCoordinator.synchronizeSharedAttributes(
                source,
                target,
                visualizer.getState(),
                visualizer.getCamisetaState(),
                visualizer.getArqueroState(),
                visualizer.isEditandoArquero(),
                () -> StateMapper.extractUserLayers(visualizer));

        if (shouldReloadVisibleTarget) {
            cargarEstadoDesdeCero(target);
        }
    }

    /**
     * Rebuilds the visual state from scratch based on a specific PrendaState.
     */
    public void cargarEstadoDesdeCero(PrendaState state) {
        visualizer.setNotificationsSuspended(true);
        try {
            visualizer.setStateDirectly(state);

            // Re-render components
            visualizer.cargarCapas();
            visualizer.getColorManager().reapplyColors();

            // Clear history so old garments' items don't return on undo
            if (visualizer.getHistoryManager() != null) {
                visualizer.getHistoryManager().clearHistory();
            }

            // Re-load user graphics
            visualizer.clearUserLayers();
            if (state.getUserLayers() != null) {
                StateMapper.restoreDesign(visualizer, null, state.getUserLayers());
            }

            // Load numbers
            visualizer.getNumberManager().restoreNumbersFromState(state, visualizer.isSwappingDesign(), visualizer.isEditandoArquero());

            visualizer.applyVisibility();
            visualizer.notifyStateChanged();
        } finally {
            visualizer.setNotificationsSuspended(false);
        }
    }

    public void setActiveDesign(boolean isArquero) {
        // Always flush current UI state before checking early return to ensure
        // latest work is captured before design swap or snapshot return
        visualizer.flushUIStateToDataModel();

        if (visualizer.isEditandoArquero() == isArquero)
            return;

        visualizer.setSwappingDesign(true);
        visualizer.setNotificationsSuspended(true);
        try {
            // Hide previous designs' numbers
            GarmentNumberManager nm = visualizer.getNumberManager();
            if (nm.getChestNumber(visualizer.isEditandoArquero()) != null)
                nm.getChestNumber(visualizer.isEditandoArquero()).setVisible(false);
            if (nm.getBackNumber(visualizer.isEditandoArquero()) != null)
                nm.getBackNumber(visualizer.isEditandoArquero()).setVisible(false);
            if (nm.getShortNumber(visualizer.isEditandoArquero()) != null)
                nm.getShortNumber(visualizer.isEditandoArquero()).setVisible(false);

            // --- INDEPENDENT LAYERS ---
            if (visualizer.getPowerClipManager() != null)
                visualizer.getPowerClipManager().reset();
            visualizer.getLayerManager().clearAll();

            // --- INDEPENDENT HOTSPOTS (REFERENCES) ---
            PrendaState currentState = visualizer.getState();
            if (currentState != null) {
                // Only save hotspots from UI to state during NORMAL design swap.
                if (!visualizer.isSwappingDesign()) {
                    List<PrendaState.ReferenceHotspot> currentHotspots = new ArrayList<>();
                    for (javafx.scene.Node n : visualizer.getHotspotLayer().getChildren()) {
                        if (n instanceof HotspotLayer) {
                            currentHotspots.add(((HotspotLayer) n).getData());
                        }
                    }
                    currentState.setReferenceHotspots(currentHotspots);
                }
            }
            visualizer.getHotspotLayer().getChildren().clear();

            // NOW change the state/flag
            visualizer.setEditandoArquero(isArquero);
            PrendaState newState = isArquero ? visualizer.getArqueroState() : visualizer.getCamisetaState();
            visualizer.setStateDirectly(newState);

            // --- CRITICAL SYNC: Ensure ColorManager is using the active state ---
            if (visualizer.getColorManager() != null) {
                visualizer.getColorManager().setState(newState);
            }

            // --- RESTORE GEOMETRY & ASSETS ---
            visualizer.updateSleeveLogic();
            if (isArquero)
                visualizer.ensureArqueroInitialized();
            visualizer.cargarCapas();

            // Load new layers from NEW state (AFTER garment loading to ensure valid
            // PowerClip masks)
            List<org.example.dto.save.LayerDTO> newLayers = newState.getUserLayers();
            if (newLayers != null && !newLayers.isEmpty()) {
                StateMapper.restoreDesign(visualizer, null, newLayers);
            }

            // Load hotspots from NEW state
            if (newState.getReferenceHotspots() != null) {
                for (PrendaState.ReferenceHotspot h : newState.getReferenceHotspots()) {
                    org.example.component.HotspotLayer hl = new org.example.component.HotspotLayer(h, null, visualizer);
                    visualizer.addHotspot(hl);
                }
            }

            // Restore numbers visibility and values for the NEW active state
            visualizer.getNumberManager().restoreNumbersFromState(newState, visualizer.isSwappingDesign(), isArquero);

            // Re-apply visibility for everything
            visualizer.applyVisibility();

            visualizer.updateReferencePoints();
            
            visualizer.notifyStateChanged();
        } finally {
            visualizer.setSwappingDesign(false);
            visualizer.setNotificationsSuspended(false);
        }
    }
}
