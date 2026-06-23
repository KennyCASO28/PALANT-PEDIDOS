package org.example.component.helper;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import org.example.component.renderer.*;
import org.example.logic.GarmentAssetManager;
import org.example.model.PrendaState;
import java.util.Map;

/**
 * Orchestrates the SVG renderers for the visualizer.
 * Handles lazy loading of goalkeeper renderers and layer updates.
 */
public class VisualizerRenderOrchestrator {

    private final ShirtRenderer shirtRenderer;
    private ArqueroRenderer arqueroShirtRenderer;
    private final ShortsRenderer shortsRenderer;
    private ShortsRenderer arqueroShortsRenderer;
    private final SocksRenderer socksRenderer;
    private SocksRenderer arqueroSocksRenderer;

    private final Group contentGroup;

    public VisualizerRenderOrchestrator(Group contentGroup) {
        this.contentGroup = contentGroup;
        
        this.shirtRenderer = new ShirtRenderer();
        this.shortsRenderer = new ShortsRenderer();
        this.socksRenderer = new SocksRenderer();

        // Add main renderers to content group
        this.socksRenderer.addToGroup(contentGroup);
        this.shortsRenderer.addToGroup(contentGroup);
        this.shirtRenderer.addToGroup(contentGroup);
    }

    public void ensureArqueroInitialized() {
        if (arqueroShirtRenderer == null) {
            arqueroShirtRenderer = new ArqueroRenderer();
            insertRendererProperly(arqueroShirtRenderer, shirtRenderer);
        }
        if (arqueroShortsRenderer == null) {
            arqueroShortsRenderer = new ShortsRenderer();
            insertRendererProperly(arqueroShortsRenderer, shortsRenderer);
        }
        if (arqueroSocksRenderer == null) {
            arqueroSocksRenderer = new SocksRenderer();
            insertRendererProperly(arqueroSocksRenderer, socksRenderer);
        }
    }

    private void insertRendererProperly(org.example.component.renderer.BaseGarmentRenderer newRenderer, org.example.component.renderer.BaseGarmentRenderer referenceRenderer) {
        int index = contentGroup.getChildren().indexOf(referenceRenderer.getGroup());
        if (index >= 0) {
            contentGroup.getChildren().add(index, newRenderer.getGroup());
        } else {
            newRenderer.addToGroup(contentGroup);
        }
        
        int detailIndex = contentGroup.getChildren().indexOf(referenceRenderer.getDetailGroup());
        if (detailIndex >= 0) {
            contentGroup.getChildren().add(detailIndex, newRenderer.getDetailGroup());
        } else {
            contentGroup.getChildren().add(newRenderer.getDetailGroup());
        }
        newRenderer.getDetailGroup().setMouseTransparent(true);
    }

    public BaseGarmentRenderer getActiveShirtRenderer(boolean editandoArquero) {
        if (editandoArquero) {
            ensureArqueroInitialized();
            return arqueroShirtRenderer;
        }
        return shirtRenderer;
    }

    public ShortsRenderer getActiveShortsRenderer(boolean editandoArquero) {
        if (editandoArquero) {
            ensureArqueroInitialized();
            return arqueroShortsRenderer;
        }
        return shortsRenderer;
    }

    public SocksRenderer getActiveSocksRenderer(boolean editandoArquero) {
        if (editandoArquero) {
            ensureArqueroInitialized();
            return arqueroSocksRenderer;
        }
        return socksRenderer;
    }

    public void updateLayers(PrendaState state, boolean editandoArquero) {
        BaseGarmentRenderer activeShirtR = getActiveShirtRenderer(editandoArquero);
        ShortsRenderer activeShortsR = getActiveShortsRenderer(editandoArquero);
        SocksRenderer activeSocksR = getActiveSocksRenderer(editandoArquero);

        if (editandoArquero && activeShirtR instanceof ArqueroRenderer) {
            ((ArqueroRenderer) activeShirtR).updateArqueroLayers(state.getGenero(), state.getCorte(), state.getLargo(), state.getCuello().name());
        } else {
            activeShirtR.updateLayers(state.getGenero().name(), state.getCorte().name(), state.getLargo().name(), state.getCuello().name());
        }

        activeShirtR.setMeshVisible(state.hasMesh());
        activeShirtR.setCuffsVisible(state.hasCuffs());
        activeShirtR.setStripeVisible(state.hasShirtStripe());
        activeShirtR.setShirtLinea(state.hasShirtLinea());

        activeShortsR.updateShorts(state.getGenero(), state.getCorteShort(), state.hasShortsStripe(),
                state.hasShortsLinea(), state.hasShortsPicket(), state.hasShortsCuff(),
                state.hasShortsCord(), state.hasShortsPocket());
        
        activeSocksR.updateSocks(state.getGenero());
        activeSocksR.setSocksTopVisible(state.hasSocksTop());
    }

    public void applyVisibility(PrendaState state, boolean editandoArquero) {
        boolean mainActive = !editandoArquero;
        boolean arqActive = editandoArquero;

        shirtRenderer.getGroup().setVisible(mainActive && state.hasShirt());
        shirtRenderer.getDetailGroup().setVisible(mainActive && state.hasShirt());
        shortsRenderer.getGroup().setVisible(mainActive && state.hasShorts());
        shortsRenderer.getDetailGroup().setVisible(mainActive && state.hasShorts());
        socksRenderer.getGroup().setVisible(mainActive && state.hasSocks());
        socksRenderer.getDetailGroup().setVisible(mainActive && state.hasSocks());

        if (arqueroShirtRenderer != null) {
            arqueroShirtRenderer.getGroup().setVisible(arqActive && state.hasShirt());
            arqueroShirtRenderer.getDetailGroup().setVisible(arqActive && state.hasShirt());
        }
        if (arqueroShortsRenderer != null) {
            arqueroShortsRenderer.getGroup().setVisible(arqActive && state.hasShorts());
            arqueroShortsRenderer.getDetailGroup().setVisible(arqActive && state.hasShorts());
        }
        if (arqueroSocksRenderer != null) {
            arqueroSocksRenderer.getGroup().setVisible(arqActive && state.hasSocks());
            arqueroSocksRenderer.getDetailGroup().setVisible(arqActive && state.hasSocks());
        }
    }

    public void updateBranding(PrendaState state, boolean editandoArquero) {
        BaseGarmentRenderer activeShirtR = getActiveShirtRenderer(editandoArquero);
        ShortsRenderer activeShortsR = getActiveShortsRenderer(editandoArquero);
        SocksRenderer activeSocksR = getActiveSocksRenderer(editandoArquero);

        if (state.hasShirt()) {
            String[] paths = GarmentAssetManager.getBrandPaths(state.getGenero(), "pecho", state.getCorte(), state.getChestBrandPosition());
            activeShirtR.updateBranding(state.isChestBrandVisible(), paths[0].toLowerCase(), paths[1].toLowerCase());
        }
        if (state.hasShorts()) {
            String[] paths = GarmentAssetManager.getBrandPaths(state.getGenero(), "short", state.getCorteShort(), state.getShortBrandPosition());
            activeShortsR.updateBranding(state.isShortBrandVisible(), paths[0].toLowerCase(), paths[1].toLowerCase());
        }
        if (state.hasSocks()) {
            String[] paths = GarmentAssetManager.getBrandPaths(state.getGenero(), "medias", null, "medias");
            activeSocksR.updateBranding(state.isSocksBrandVisible(), paths[0].toLowerCase(), paths[1].toLowerCase());
        }
    }

    public void updateAllBranding(PrendaState state, boolean editandoArquero) {
        updateBranding(state, editandoArquero);
    }

    public void setRenderersOpacity(double opacity) {
        shirtRenderer.getBody().setOpacity(opacity);
        shirtRenderer.getSleeves().setOpacity(opacity);
        shortsRenderer.getShorts().setOpacity(opacity);
        if (socksRenderer.getSocks() != null) socksRenderer.getSocks().setOpacity(opacity);

        // También opacar renderers del arquero (que son los activos en modo arquero)
        if (arqueroShirtRenderer != null) {
            arqueroShirtRenderer.getBody().setOpacity(opacity);
            arqueroShirtRenderer.getSleeves().setOpacity(opacity);
        }
        if (arqueroShortsRenderer != null) {
            arqueroShortsRenderer.getShorts().setOpacity(opacity);
        }
        if (arqueroSocksRenderer != null && arqueroSocksRenderer.getSocks() != null) {
            arqueroSocksRenderer.getSocks().setOpacity(opacity);
        }
    }

    public Group getContentGroup() {
        return contentGroup;
    }

    public ShirtRenderer getShirtRenderer() { return shirtRenderer; }
    public ShortsRenderer getShortsRenderer() { return shortsRenderer; }
    public SocksRenderer getSocksRenderer() { return socksRenderer; }
    
    public ArqueroRenderer getArqueroShirtRenderer() { return arqueroShirtRenderer; }
    public ShortsRenderer getArqueroShortsRenderer() { return arqueroShortsRenderer; }
    public SocksRenderer getArqueroSocksRenderer() { return arqueroSocksRenderer; }
}
