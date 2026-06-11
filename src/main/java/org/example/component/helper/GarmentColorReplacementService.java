package org.example.component.helper;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.example.component.PrendaVisualizer;
import org.example.component.ShapeLayer;
import org.example.component.NumberComposition;
import org.example.model.PrendaState;
import org.example.pattern.CompositeCommand;

public class GarmentColorReplacementService {

    private final PrendaVisualizer visualizer;

    public GarmentColorReplacementService(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    /**
     * Replaces colors globally in the design (garment parts and all layers).
     *
     * @return Number of parts/layers updated.
     */
    public int replaceAllColors(Color src, Color target, double tolerance) {
        PrendaState state = visualizer.getState();
        if (state == null)
            return 0;
            
        double effectiveTolerance = Math.max(tolerance, 0.15);

        CompositeCommand masterCmd = new CompositeCommand(
                "Reemplazo de Color (" + (visualizer.isEditandoArquero() ? "Arquero" : "Jugador") + ")") {
            @Override
            public void undo() {
                super.undo();
                visualizer.reapplyColors();
                visualizer.reloadActiveNumbers();
                visualizer.updateAllBranding();
                visualizer.notifyStateChanged();
                visualizer.notifyUIUpdates();
            }

            @Override
            public void redo() {
                super.redo();
                visualizer.reapplyColors();
                visualizer.reloadActiveNumbers();
                visualizer.updateAllBranding();
                visualizer.notifyStateChanged();
                visualizer.notifyUIUpdates();
            }
        };

        // 1. Replace in current active state only
        replaceInState(state, src, target, effectiveTolerance, masterCmd);

        // 2. Replace in current layers (swapping designs already swaps layers)
        if (visualizer.getLayerManager() != null) {
            for (Node node : visualizer.getLayerManager().getLayers()) {
                replaceInNodeRecursive(node, src, target, effectiveTolerance, masterCmd);
            }
        }

        // 3. Replace in active numbers
        GarmentNumberManager nm = visualizer.getNumberManager();
        if (nm.getChestNumber(visualizer.isEditandoArquero()) != null)
            replaceInNodeRecursive(nm.getChestNumber(visualizer.isEditandoArquero()).getRoot(), src, target, effectiveTolerance, masterCmd);
        if (nm.getBackNumber(visualizer.isEditandoArquero()) != null)
            replaceInNodeRecursive(nm.getBackNumber(visualizer.isEditandoArquero()).getRoot(), src, target, effectiveTolerance, masterCmd);
        if (nm.getShortNumber(visualizer.isEditandoArquero()) != null)
            replaceInNodeRecursive(nm.getShortNumber(visualizer.isEditandoArquero()).getRoot(), src, target, effectiveTolerance, masterCmd);

        // 4. Branding and renderers for the ACTIVE design only
        if (visualizer.isEditandoArquero()) {
            if (visualizer.getArqueroShirtRenderer() != null)
                replaceInNodeRecursive(visualizer.getArqueroShirtRenderer().getGroup(), src, target, effectiveTolerance, masterCmd);
            if (visualizer.getArqueroShortsRenderer() != null)
                replaceInNodeRecursive(visualizer.getArqueroShortsRenderer().getGroup(), src, target, effectiveTolerance, masterCmd);
            if (visualizer.getArqueroSocksRenderer() != null)
                replaceInNodeRecursive(visualizer.getArqueroSocksRenderer().getGroup(), src, target, effectiveTolerance, masterCmd);
        } else {
            if (visualizer.getShirtRenderer() != null)
                replaceInNodeRecursive(visualizer.getShirtRenderer().getGroup(), src, target, effectiveTolerance, masterCmd);
            if (visualizer.getShortsRenderer() != null)
                replaceInNodeRecursive(visualizer.getShortsRenderer().getGroup(), src, target, effectiveTolerance, masterCmd);
            if (visualizer.getSocksRenderer() != null)
                replaceInNodeRecursive(visualizer.getSocksRenderer().getGroup(), src, target, effectiveTolerance, masterCmd);
        }

        if (!masterCmd.isEmpty()) {
            masterCmd.execute();
            if (visualizer.getHistoryManager() != null)
                visualizer.getHistoryManager().addCommand(masterCmd);

            // Unified Refresh
            visualizer.reapplyColors();
            visualizer.reloadActiveNumbers();
            visualizer.updateAllBranding();
            visualizer.notifyStateChanged();
            visualizer.notifyUIUpdates(); // SYNC PANELS
        }
        return masterCmd.getCommandCount();
    }

    private void replaceInState(PrendaState s, Color src, Color target, double tolerance, CompositeCommand master) {
        // 1. Base Colors Map
        for (String partName : new java.util.ArrayList<>(s.getColors().keySet())) {
            // USER REQUEST: Exclude protected parts from global replacement
            if (partName.equals("shortsCord") || partName.equals("socksSole"))
                continue;

            Color currentColor = s.getColors().get(partName);
            if (org.example.utils.ColorUtils.areColorsSimilar(currentColor, src, tolerance)) {
                Color oldColor = currentColor;
                master.addCommand(
                        new org.example.pattern.ColorChangeCommand("Color " + partName, oldColor, target, c -> {
                            s.getColors().put(partName, c);
                        }));
            }
        }

        // 2. Number Colors Maps (Chest, Back, Short)
        replaceInNumberColorMap(s.getChestNumberColors(), src, target, tolerance, master, "Color Núm. Pecho");
        replaceInNumberColorMap(s.getBackNumberColors(), src, target, tolerance, master, "Color Núm. Espalda");
        replaceInNumberColorMap(s.getShortNumberColors(), src, target, tolerance, master, "Color Núm. Short");
    }

    private void replaceInNumberColorMap(java.util.Map<Integer, Color> map, Color src, Color target, double tolerance,
            CompositeCommand master, String label) {
        for (Integer layerIdx : new java.util.ArrayList<>(map.keySet())) {
            Color currentColor = map.get(layerIdx);
            if (org.example.utils.ColorUtils.areColorsSimilar(currentColor, src, tolerance)) {
                Color oldColor = currentColor;
                master.addCommand(new org.example.pattern.ColorChangeCommand(label, oldColor, target, c -> {
                    map.put(layerIdx, c);
                }));

                // USER REQUEST: Trigger "Smart Combo" if base is changed
                if (layerIdx == 0) {
                    Color smartCombo = org.example.utils.ColorUtils.getSmartCombination(target);
                    Color oldCombo = map.get(1);
                    if (oldCombo != null) {
                        master.addCommand(
                                new org.example.pattern.ColorChangeCommand("Smart Combo", oldCombo, smartCombo, c -> {
                                    map.put(1, c);
                                }));
                    }
                }
            }
        }
    }

    private void replaceInNodeRecursive(Node node, Color src, Color target, double tolerance,
            CompositeCommand master) {
        if (node == null)
            return;

        if (node instanceof ShapeLayer) {
            ShapeLayer sl = (ShapeLayer) node;
            if (org.example.utils.ColorUtils.areColorsSimilar(sl.getFillColor(), src, tolerance)) {
                Color old = sl.getFillColor();
                master.addCommand(new org.example.pattern.PropertyChangeCommand<>("Fill Shape", old, target,
                        c -> sl.setFillColor(c)));
            }
        } else if (visualizer.isNumberRoot(node)) {
            NumberComposition nc = visualizer.getNumberCompositionFromRoot(node);
            if (nc != null) {
                // Determine which persistent map in the state to update
                GarmentNumberManager nm = visualizer.getNumberManager();
                PrendaState statusOwner = (nc == nm.getChestNumber(false) || nc == nm.getBackNumber(false) || nc == nm.getShortNumber(false)) ? visualizer.getCamisetaState()
                        : ((nc == nm.getChestNumber(true) || nc == nm.getBackNumber(true) || nc == nm.getShortNumber(true))
                                ? visualizer.getArqueroState()
                                : visualizer.getState());

                java.util.Map<Integer, Color> stateMap = null;
                if (nc == nm.getChestNumber(false) || nc == nm.getChestNumber(true))
                    stateMap = statusOwner.getChestNumberColors();
                else if (nc == nm.getBackNumber(false) || nc == nm.getBackNumber(true))
                    stateMap = statusOwner.getBackNumberColors();
                else if (nc == nm.getShortNumber(false) || nc == nm.getShortNumber(true))
                    stateMap = statusOwner.getShortNumberColors();

                final java.util.Map<Integer, Color> finalMap = stateMap;

                for (int i = 0; i < 5; i++) {
                    Color current = nc.getLayerColor(i);
                    if (current != null && org.example.utils.ColorUtils.areColorsSimilar(current, src, tolerance)) {
                        Color old = current;
                        final int finalI = i;
                        master.addCommand(
                                new org.example.pattern.PropertyChangeCommand<>("Color Número", old, target, c -> {
                                    nc.setLayerColor(finalI, c);
                                    if (finalMap != null)
                                        finalMap.put(finalI, c);
                                }));

                        // USER REQUEST: Smart Combo Logic for UI replacement too
                        if (finalI == 0) {
                            Color smartCombo = org.example.utils.ColorUtils.getSmartCombination(target);
                            Color oldCombo = nc.getLayerColor(1);
                            if (oldCombo != null) {
                                master.addCommand(new org.example.pattern.PropertyChangeCommand<>("Smart Combo UI",
                                        oldCombo, smartCombo, c -> {
                                            nc.setLayerColor(1, c);
                                            if (finalMap != null)
                                                finalMap.put(1, c);
                                        }));
                            }
                        }
                    }
                }
            }
        } else if (node instanceof javafx.scene.Group) {
            for (Node child : ((javafx.scene.Group) node).getChildren()) {
                replaceInNodeRecursive(child, src, target, tolerance, master);
            }
        } else if (node instanceof javafx.scene.layout.Pane) {
            // Includes StackPane, VBox, HBox etc. which often host logos/branding
            for (Node child : ((javafx.scene.layout.Pane) node).getChildren()) {
                replaceInNodeRecursive(child, src, target, tolerance, master);
            }
        } else if (node instanceof javafx.scene.image.ImageView) {
            // LOGO REPLACEMENT: Simplified Colorization for logos
            javafx.scene.image.ImageView iv = (javafx.scene.image.ImageView) node;
            master.addCommand(new org.example.pattern.PropertyChangeCommand<>("Tint Logo", null, target, c -> {
                javafx.scene.effect.ColorAdjust adj = new javafx.scene.effect.ColorAdjust();
                adj.setHue(target.getHue() / 360.0);
                adj.setSaturation(target.getSaturation());
                iv.setEffect(adj);
            }));
        } else if (node instanceof javafx.scene.shape.Shape) {
            javafx.scene.shape.Shape s = (javafx.scene.shape.Shape) node;

            // USER REQUEST: Exclude pasador from visual replacement
            if (isProtectedNode(s))
                return;

            if (s.getFill() instanceof Color) {
                Color fill = (Color) s.getFill();
                if (org.example.utils.ColorUtils.areColorsSimilar(fill, src, tolerance)) {
                    Color old = fill;
                    master.addCommand(new org.example.pattern.PropertyChangeCommand<>("Color Shape", old, target, c -> {
                        s.setFill(c);
                        syncNodeToState(s, c);
                    }));
                }
            }
        }
    }

    private void syncNodeToState(Node node, Color c) {
        if (node == null || c == null)
            return;

        // Base states
        PrendaState camisetaState = visualizer.getCamisetaState();
        PrendaState arqueroState = visualizer.getArqueroState();

        // Fetched renderers
        var shirtRenderer = visualizer.getShirtRenderer();
        var arqueroShirtRenderer = visualizer.getArqueroShirtRenderer();
        var shortsRenderer = visualizer.getShortsRenderer();
        var arqueroShortsRenderer = visualizer.getArqueroShortsRenderer();
        var socksRenderer = visualizer.getSocksRenderer();
        var arqueroSocksRenderer = visualizer.getArqueroSocksRenderer();

        // Shirt Branding
        if (shirtRenderer != null && (shirtRenderer.getBrandBase() == node || shirtRenderer.getBrandDetail() == node)) {
            camisetaState.getColors().put("brandChest", c);
        }
        if (arqueroShirtRenderer != null
                && (arqueroShirtRenderer.getBrandBase() == node || arqueroShirtRenderer.getBrandDetail() == node)) {
            arqueroState.getColors().put("brandChest", c);
        }

        // Shorts Branding
        if (shortsRenderer != null
                && (shortsRenderer.getBrandBase() == node || shortsRenderer.getBrandDetail() == node)) {
            camisetaState.getColors().put("brandShort", c);
        }
        if (arqueroShortsRenderer != null
                && (arqueroShortsRenderer.getBrandBase() == node || arqueroShortsRenderer.getBrandDetail() == node)) {
            arqueroState.getColors().put("brandShort", c);
        }

        // Socks Branding
        if (socksRenderer != null && (socksRenderer.getBrandBase() == node || socksRenderer.getBrandDetail() == node)) {
            camisetaState.getColors().put("brandSocks", c);
        }
        if (arqueroSocksRenderer != null
                && (arqueroSocksRenderer.getBrandBase() == node || arqueroSocksRenderer.getBrandDetail() == node)) {
            arqueroState.getColors().put("brandSocks", c);
        }

        // Also check if it's a base garment layer to keep maps in sync
        if (shirtRenderer != null) {
            if (shirtRenderer.getBody() == node)
                camisetaState.getColors().put("body", c);
            if (shirtRenderer.getSleeves() == node)
                camisetaState.getColors().put("sleeves", c);
        }
        if (shortsRenderer != null && shortsRenderer.getBody() == node) {
            camisetaState.getColors().put("shorts", c);
        }
        if (arqueroShirtRenderer != null) {
            if (arqueroShirtRenderer.getBody() == node)
                arqueroState.getColors().put("body", c);
            if (arqueroShirtRenderer.getSleeves() == node)
                arqueroState.getColors().put("sleeves", c);
        }
        if (arqueroShortsRenderer != null && arqueroShortsRenderer.getBody() == node) {
            arqueroState.getColors().put("shorts", c);
        }
    }

    private boolean isProtectedNode(Node node) {
        if (node == null)
            return false;

        var shortsRenderer = visualizer.getShortsRenderer();
        var arqueroShortsRenderer = visualizer.getArqueroShortsRenderer();
        var socksRenderer = visualizer.getSocksRenderer();
        var arqueroSocksRenderer = visualizer.getArqueroSocksRenderer();

        // PROTECT SHORTS CORD (Pasador)
        if (shortsRenderer instanceof org.example.component.renderer.ShortsRenderer) {
            if (((org.example.component.renderer.ShortsRenderer) shortsRenderer).getShortsCord() == node)
                return true;
        }
        if (arqueroShortsRenderer instanceof org.example.component.renderer.ShortsRenderer) {
            if (((org.example.component.renderer.ShortsRenderer) arqueroShortsRenderer).getShortsCord() == node)
                return true;
        }

        // PROTECT SOCKS SOLE (Suela)
        if (socksRenderer instanceof org.example.component.renderer.SocksRenderer) {
            if (((org.example.component.renderer.SocksRenderer) socksRenderer).getSocksSole() == node)
                return true;
        }
        if (arqueroSocksRenderer instanceof org.example.component.renderer.SocksRenderer) {
            if (((org.example.component.renderer.SocksRenderer) arqueroSocksRenderer).getSocksSole() == node)
                return true;
        }

        return false;
    }
}
