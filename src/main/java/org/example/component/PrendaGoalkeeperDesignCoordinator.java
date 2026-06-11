package org.example.component;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.example.component.helper.PrendaColorManager;
import org.example.component.renderer.ArqueroRenderer;
import org.example.component.renderer.ShortsRenderer;
import org.example.component.renderer.SocksRenderer;
import org.example.dto.save.LayerDTO;
import org.example.model.PrendaState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class PrendaGoalkeeperDesignCoordinator {

    private PrendaGoalkeeperDesignCoordinator() {
    }

    public static GoalkeeperInitialization initializeGoalkeeper(
            Group contentGroup,
            Group userLayerGroup,
            boolean arqueroActive,
            PrendaState camisetaState,
            PrendaState arqueroState,
            Runnable onStateChanged) {
        System.out.println("DEBUG: Initializing ArqueroRenderer (independent from ShirtRenderer) on demand...");

        ArqueroRenderer arqueroShirtRenderer = new ArqueroRenderer();
        insertGoalkeeperShirt(contentGroup, userLayerGroup, arqueroShirtRenderer);

        if (arqueroState.getColors().isEmpty() && !camisetaState.getColors().isEmpty()) {
            System.out.println("DEBUG: Inheriting brand colors from Camiseta to Arquero...");
            copyBrandColors(camisetaState, arqueroState);
        }

        preloadGoalkeeperReferenceColor(arqueroState);

        ShortsRenderer arqueroShortsRenderer = new ShortsRenderer();
        SocksRenderer arqueroSocksRenderer = new SocksRenderer();
        insertGoalkeeperLowerGarments(contentGroup, userLayerGroup, arqueroShortsRenderer, arqueroSocksRenderer);

        arqueroShirtRenderer.setVisible(arqueroActive && arqueroState.hasShirt());
        arqueroShortsRenderer.setVisible(arqueroActive && arqueroState.hasShorts());
        arqueroSocksRenderer.setVisible(arqueroActive && arqueroState.hasSocks());

        PrendaColorManager arqueroColorManager = new PrendaColorManager(
                arqueroState,
                arqueroShirtRenderer,
                arqueroShortsRenderer,
                arqueroSocksRenderer,
                onStateChanged);

        return new GoalkeeperInitialization(
                arqueroShirtRenderer,
                arqueroShortsRenderer,
                arqueroSocksRenderer,
                arqueroColorManager);
    }

    public static boolean synchronizeSharedAttributes(
            PrendaState source,
            PrendaState target,
            PrendaState activeState,
            PrendaState camisetaState,
            PrendaState arqueroState,
            boolean editandoArquero,
            Supplier<List<LayerDTO>> activeLayerExtractor) {
        if (source == null || target == null) {
            return false;
        }

        List<LayerDTO> currentLayers;
        if (activeState == source || (source == camisetaState && !editandoArquero)) {
            currentLayers = activeLayerExtractor != null ? activeLayerExtractor.get() : null;
            if (currentLayers != null) {
                source.setUserLayers(new ArrayList<>(currentLayers));
            }
        } else {
            currentLayers = source.getUserLayers();
        }

        boolean targetHasCustomization = target.hasAnyCustomization();
        if (currentLayers != null && !targetHasCustomization) {
            target.setUserLayers(deepCopyLayers(currentLayers));
        }

        if (!targetHasCustomization) {
            synchronizeNumbersAndBranding(source, target, arqueroState);
        }

        if (target.getGenero() == null) {
            target.setGenero(source.getGenero());
        }
        if (target.getCorte() == null) {
            target.setCorte(source.getCorte());
        }

        return editandoArquero && target == arqueroState;
    }

    private static void insertGoalkeeperShirt(
            Group contentGroup,
            Group userLayerGroup,
            ArqueroRenderer arqueroShirtRenderer) {
        int userLayerIndex = contentGroup.getChildren().indexOf(userLayerGroup);
        if (userLayerIndex == -1) {
            arqueroShirtRenderer.addToGroup(contentGroup);
            return;
        }

        arqueroShirtRenderer.addToGroup(contentGroup);
        moveNodeToIndex(contentGroup, arqueroShirtRenderer.getGroup(), userLayerIndex);
        moveNodeToTop(contentGroup, arqueroShirtRenderer.getDetailGroup());
    }

    private static void insertGoalkeeperLowerGarments(
            Group contentGroup,
            Group userLayerGroup,
            ShortsRenderer arqueroShortsRenderer,
            SocksRenderer arqueroSocksRenderer) {
        int insertIndex = contentGroup.getChildren().indexOf(userLayerGroup);
        if (insertIndex == -1) {
            insertIndex = 0;
        }

        arqueroSocksRenderer.addToGroup(contentGroup);
        moveNodeToIndex(contentGroup, arqueroSocksRenderer.getGroup(), insertIndex);

        arqueroShortsRenderer.addToGroup(contentGroup);
        moveNodeToIndex(contentGroup, arqueroShortsRenderer.getGroup(), insertIndex + 1);

        moveNodeToTop(contentGroup, arqueroSocksRenderer.getDetailGroup());
        moveNodeToTop(contentGroup, arqueroShortsRenderer.getDetailGroup());
    }

    private static void moveNodeToIndex(Group contentGroup, Node node, int index) {
        if (node == null) {
            return;
        }
        contentGroup.getChildren().remove(node);
        contentGroup.getChildren().add(Math.min(index, contentGroup.getChildren().size()), node);
    }

    private static void moveNodeToTop(Group contentGroup, Node node) {
        if (node == null) {
            return;
        }
        contentGroup.getChildren().remove(node);
        contentGroup.getChildren().add(node);
    }

    private static void preloadGoalkeeperReferenceColor(PrendaState arqueroState) {
        Color referenceColor = arqueroState.getColorReferenciaArquero();
        if (referenceColor != null && !referenceColor.equals(Color.WHITE)) {
            arqueroState.getColors().put("body", referenceColor);
            arqueroState.getColors().put("sleeves", referenceColor);
            arqueroState.getColors().put("shorts", referenceColor);
            arqueroState.getColors().put("socks", referenceColor);
            System.out.println("DEBUG: Pre-loading Goalkeeper reference color: " + referenceColor);
            return;
        }

        arqueroState.getColors().putIfAbsent("body", Color.WHITE);
        arqueroState.getColors().putIfAbsent("sleeves", Color.WHITE);
        arqueroState.getColors().putIfAbsent("shorts", Color.WHITE);
        arqueroState.getColors().putIfAbsent("socks", Color.WHITE);
    }

    private static void copyBrandColors(PrendaState source, PrendaState target) {
        String[] brandKeys = { "brandChest", "brandShort", "brandSocks" };
        for (String key : brandKeys) {
            if (source.getColors().containsKey(key)) {
                target.getColors().put(key, source.getColors().get(key));
            }
        }
    }

    private static void synchronizeNumbersAndBranding(
            PrendaState source,
            PrendaState target,
            PrendaState arqueroState) {
        String chestNumber = source.getCurrentChestNumber();
        String backNumber = source.getCurrentBackNumber();
        String shortNumber = source.getCurrentShortNumber();

        if (target == arqueroState) {
            chestNumber = "1";
            backNumber = "1";
            shortNumber = "1";
        }

        target.setCurrentChestNumber(chestNumber);
        target.setChestNumberX(source.getChestNumberX());
        target.setChestNumberY(source.getChestNumberY());
        target.setChestNumberScale(source.getChestNumberScale());
        target.setChestNumberVisible(source.isChestNumberVisible());

        target.setCurrentBackNumber(backNumber);
        target.setBackNumberX(source.getBackNumberX());
        target.setBackNumberY(source.getBackNumberY());
        target.setBackNumberScale(source.getBackNumberScale());
        target.setBackNumberVisible(source.isBackNumberVisible());

        target.setCurrentShortNumber(shortNumber);
        target.setShortNumberX(source.getShortNumberX());
        target.setShortNumberY(source.getShortNumberY());
        target.setShortNumberScale(source.getShortNumberScale());
        target.setShortNumberVisible(source.isShortNumberVisible());

        target.setChestNumberColors(new java.util.HashMap<>(source.getChestNumberColors()));
        target.setBackNumberColors(new java.util.HashMap<>(source.getBackNumberColors()));
        target.setShortNumberColors(new java.util.HashMap<>(source.getShortNumberColors()));

        target.setChestBrandVisible(source.isChestBrandVisible());
        target.setChestBrandPosition(source.getChestBrandPosition());
        target.setBrandTech(source.getBrandTech());

        target.setShortBrandVisible(source.isShortBrandVisible());
        target.setShortBrandPosition(source.getShortBrandPosition());
        target.setSocksBrandVisible(source.isSocksBrandVisible());

        target.getColors().put("brandChest", source.getColors().getOrDefault("brandChest", Color.BLACK));
        target.getColors().put("brandShort", source.getColors().getOrDefault("brandShort", Color.BLACK));
        target.getColors().put("brandSocks", source.getColors().getOrDefault("brandSocks", Color.BLACK));
    }

    private static List<LayerDTO> deepCopyLayers(List<LayerDTO> original) {
        List<LayerDTO> copy = new ArrayList<>();
        if (original == null) {
            return copy;
        }
        for (LayerDTO layer : original) {
            copy.add(layer.deepCopy());
        }
        return copy;
    }

    public static final class GoalkeeperInitialization {
        private final ArqueroRenderer shirtRenderer;
        private final ShortsRenderer shortsRenderer;
        private final SocksRenderer socksRenderer;
        private final PrendaColorManager colorManager;

        private GoalkeeperInitialization(
                ArqueroRenderer shirtRenderer,
                ShortsRenderer shortsRenderer,
                SocksRenderer socksRenderer,
                PrendaColorManager colorManager) {
            this.shirtRenderer = shirtRenderer;
            this.shortsRenderer = shortsRenderer;
            this.socksRenderer = socksRenderer;
            this.colorManager = colorManager;
        }

        public ArqueroRenderer shirtRenderer() {
            return shirtRenderer;
        }

        public ShortsRenderer shortsRenderer() {
            return shortsRenderer;
        }

        public SocksRenderer socksRenderer() {
            return socksRenderer;
        }

        public PrendaColorManager colorManager() {
            return colorManager;
        }
    }
}
