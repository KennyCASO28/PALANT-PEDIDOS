package org.example.controller;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.example.dto.save.LayerDTO;
import org.example.dto.save.PrendaStateDTO;
import org.example.model.DetallePedido;
import org.example.model.PrendaState;
import org.example.model.TipoCorte;
import org.example.model.TipoGenero;
import org.example.model.TipoLargo;
import org.example.model.TipoMedias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

final class PedidoGoalkeeperCoordinator {

    private static final String PLAYER_MODE = "Jugador";
    private static final String GOALKEEPER_MODE = "Arquero";
    private static final String DEFAULT_GOALKEEPER_NUMBER = "1";

    private final PedidoController controller;

    PedidoGoalkeeperCoordinator(PedidoController controller) {
        this.controller = controller;
    }

    void updateOptionStatus() {
        if (controller.comboModoDiseno == null) {
            return;
        }

        boolean hasGoalkeepers = controller.listaJugadores.stream().anyMatch(DetallePedido::isEsArquero);
        if (!hasGoalkeepers && controller.editandoDisenoArquero) {
            changeDesignMode(false);
        }

        boolean previousSwitchingState = controller.switchingDesignMode;
        controller.switchingDesignMode = true;
        try {
            if (hasGoalkeepers) {
                if (!controller.comboModoDiseno.getItems().contains(GOALKEEPER_MODE)) {
                    controller.comboModoDiseno.getItems().add(GOALKEEPER_MODE);
                }
                controller.comboModoDiseno.setDisable(false);

                DetallePedido firstGoalkeeper = getPrimaryGoalkeeper();
                if (firstGoalkeeper != null && !controller.wizardStarted) {
                    syncDesignFromPlayer(firstGoalkeeper);
                }
                return;
            }

            if (GOALKEEPER_MODE.equalsIgnoreCase(controller.comboModoDiseno.getValue())) {
                controller.comboModoDiseno.setValue(PLAYER_MODE);
            }
            controller.comboModoDiseno.getItems().remove(GOALKEEPER_MODE);
            controller.comboModoDiseno.setDisable(true);
        } finally {
            controller.switchingDesignMode = previousSwitchingState;
        }
    }

    void attachPlayerListeners(DetallePedido player) {
        player.esArqueroProperty().addListener((obs, oldValue, isGoalkeeper) -> {
            updateOptionStatus();
            if (!isGoalkeeper) {
                return;
            }
            if (player.getArqueroOrdenMarcado() <= 0) {
                player.setArqueroOrdenMarcado(controller.nextArqueroMarkOrder++);
            }
            String sleeveType = player.getTipoManga();
            if (sleeveType != null && !sleeveType.isBlank()) {
                player.setTipoMangaArquero(sleeveType);
            }
            if (isPrimaryGoalkeeper(player)) {
                syncDesignFromPlayer(player);
            }
        });

        javafx.beans.value.ChangeListener<Object> syncListener = (obs, oldValue, newValue) -> {
            if (player.isEsArquero() && isPrimaryGoalkeeper(player)) {
                syncDesignFromPlayer(player);
            }
        };

        player.includeBottomProperty().addListener(syncListener);
        player.includeSocksProperty().addListener(syncListener);
        player.generoProperty().addListener(syncListener);
        player.tipoMangaArqueroProperty().addListener(syncListener);
        player.tipoMangaProperty().addListener(syncListener);
        player.tipoBottomProperty().addListener(syncListener);
        player.tipoMediasProperty().addListener(syncListener);
        player.colorArqueroProperty().addListener(syncListener);
    }

    void syncLiveStateFromRosterUpdate(DetallePedido player) {
        if (player == null || !player.isEsArquero() || !isPrimaryGoalkeeper(player) || controller.prendaVisualizer == null) {
            return;
        }

        PrendaState goalkeeperState = controller.prendaVisualizer.getArqueroState();
        if (goalkeeperState == null) {
            return;
        }

        if (goalkeeperState.getUserLayers() == null || goalkeeperState.getUserLayers().isEmpty()) {
            goalkeeperState.copyFrom(controller.prendaVisualizer.getCamisetaState());
        }

        applyTableSettingsToLiveState(goalkeeperState, player);

        if (controller.editandoDisenoArquero) {
            controller.prendaVisualizer.cargarCapas();
            if (controller.prendaVisualizer.getArqueroColorManager() != null) {
                controller.prendaVisualizer.getArqueroColorManager().reapplyColors();
            }
        }
    }

    void syncDesignFromPlayer(DetallePedido player) {
        DetallePedido referencePlayer = player != null ? player : new DetallePedido();
        boolean createdNow = false;

        if (controller.disenoArqueroConfig == null) {
            if (controller.disenoCampoConfig == null) {
                controller.guardarDisenoActualEnSlot();
            }
            controller.disenoArqueroConfig = controller.disenoCampoConfig != null
                    ? controller.disenoCampoConfig.deepCopy()
                    : new PrendaStateDTO();
            controller.disenoArqueroLayers = copyLayers(controller.disenoCampoLayers);
            controller.disenoArqueroConfig.setHasShirt(true);
            createdNow = true;
            if (controller.prendaVisualizer != null) {
                controller.prendaVisualizer.setArqueroDisenoPersonalizado(false);
            }
        }

        boolean autoSyncActive = controller.prendaVisualizer != null
                && !controller.prendaVisualizer.isArqueroDisenoPersonalizado()
                && !createdNow;
        if (autoSyncActive) {
            if (!controller.editandoDisenoArquero) {
                controller.guardarDisenoActualEnSlot();
            }
            controller.disenoArqueroLayers = copyLayers(controller.disenoCampoLayers);
            inheritBrandingFromFieldDesign();
        }

        ensureDefaultNumbers(controller.disenoArqueroConfig, createdNow);
        controller.disenoArqueroConfig.setHasShorts(referencePlayer.isIncludeBottom());
        controller.disenoArqueroConfig.setHasSocks(referencePlayer.isIncludeSocks());
        if (referencePlayer.isIncludeBottom()) {
            controller.disenoArqueroConfig.setCurrentCorteShort(resolveShortCut(
                    referencePlayer.getTipoBottom(),
                    controller.disenoArqueroConfig.getCurrentCorteShort()));
        }
        if (referencePlayer.isIncludeSocks()) {
            controller.disenoArqueroConfig.setCurrentTipoMedias(resolveSocksType(
                    referencePlayer.getTipoMedias(),
                    controller.disenoArqueroConfig.getCurrentTipoMedias()));
        }

        controller.disenoArqueroConfig.setHasShortsLining(
                controller.disenoCampoConfig != null && controller.disenoCampoConfig.isHasShortsLining());

        if (referencePlayer.getGenero() != null) {
            try {
                controller.disenoArqueroConfig.setCurrentGenero(
                        TipoGenero.valueOf(referencePlayer.getGenero().toUpperCase()));
            } catch (Exception ignored) {
            }
        }

        controller.disenoArqueroConfig.setCurrentLargo(resolveSleeveLength(referencePlayer));

        if (createdNow || referencePlayer.getColorArquero() != null) {
            applyReferenceColorToConfig(
                    controller.disenoArqueroConfig,
                    referencePlayer.getColorArquero(),
                    referencePlayer.isIncludeBottom(),
                    referencePlayer.isIncludeSocks());
        }

        if (controller.editandoDisenoArquero && controller.prendaVisualizer != null) {
            Platform.runLater(() -> {
                if (controller.prendaDelegate != null) {
                    controller.prendaDelegate.restoreFromState(controller.disenoArqueroConfig);
                }
                controller.restaurarDisenoSilencioso(
                        true,
                        controller.disenoArqueroConfig,
                        controller.disenoArqueroLayers);
            });
        }
    }

    void changeDesignMode(boolean toGoalkeeper) {
        if (controller.prendaVisualizer == null || controller.switchingDesignMode) {
            return;
        }

        controller.switchingDesignMode = true;
        try {
            controller.guardarDisenoActualEnSlot();

            controller.editandoDisenoArquero = toGoalkeeper;
            controller.prendaVisualizer.setActiveDesign(toGoalkeeper);

            if (controller.prendaDelegate != null) {
                controller.prendaDelegate.setArqueroMode(toGoalkeeper);
            }

            if (controller.prendaVisualizer.getColorManager() != null) {
                controller.prendaVisualizer.getColorManager().clearCache();
            }

            PrendaStateDTO targetConfig = toGoalkeeper ? controller.disenoArqueroConfig : controller.disenoCampoConfig;
            List<LayerDTO> targetLayers = toGoalkeeper ? controller.disenoArqueroLayers : controller.disenoCampoLayers;

            if (toGoalkeeper && (targetConfig == null || (targetLayers != null && targetLayers.isEmpty()))) {
                syncDesignFromPlayer(Optional.ofNullable(getPrimaryGoalkeeper()).orElse(new DetallePedido()));
                targetConfig = controller.disenoArqueroConfig;
                targetLayers = controller.disenoArqueroLayers;
            }

            if (targetConfig != null) {
                controller.restaurarDisenoSilencioso(toGoalkeeper, targetConfig, targetLayers);
            }

            if (toGoalkeeper) {
                applySpecificsFromTable();
            }

            controller.prendaVisualizer.resetView();
            controller.prendaVisualizer.cargarCapas();

            PrendaStateDTO finalConfig = targetConfig;
            Platform.runLater(() -> {
                if (controller.comboModoDiseno != null) {
                    controller.comboModoDiseno.setValue(toGoalkeeper ? GOALKEEPER_MODE : PLAYER_MODE);
                }
                if (controller.personalizacionDelegate != null) {
                    controller.personalizacionDelegate.refreshContent();
                }
                if (controller.prendaDelegate != null && finalConfig != null) {
                    controller.prendaDelegate.restoreFromState(finalConfig);
                }
            });
        } catch (Exception e) {
            System.err.println("ERROR switching design: " + e.getMessage());
            e.printStackTrace();
        } finally {
            controller.switchingDesignMode = false;
        }
    }

    void applySpecificsFromTable() {
        if (controller.prendaVisualizer == null) {
            return;
        }
        Optional.ofNullable(getPrimaryGoalkeeper()).ifPresent(player -> {
            PrendaState goalkeeperState = controller.prendaVisualizer.getArqueroState();
            if (goalkeeperState != null) {
                applyTableSettingsToLiveState(goalkeeperState, player);
            }
        });
    }

    void applySpecificsFromStoredDesign() {
        Optional.ofNullable(getPrimaryGoalkeeper()).ifPresent(this::syncDesignFromPlayer);
    }

    DetallePedido getPrimaryGoalkeeper() {
        return controller.listaJugadores.stream()
                .filter(DetallePedido::isEsArquero)
                .sorted(Comparator.comparingInt(this::getGoalkeeperSortOrder))
                .findFirst()
                .orElse(null);
    }

    boolean isPrimaryGoalkeeper(DetallePedido player) {
        if (player == null || !player.isEsArquero()) {
            return false;
        }
        return getPrimaryGoalkeeper() == player;
    }

    void recomputeNextMarkOrder() {
        int maxOrder = 0;
        for (DetallePedido player : controller.listaJugadores) {
            if (player.getArqueroOrdenMarcado() > maxOrder) {
                maxOrder = player.getArqueroOrdenMarcado();
            }
        }
        controller.nextArqueroMarkOrder = maxOrder + 1;
    }

    private int getGoalkeeperSortOrder(DetallePedido player) {
        if (player == null) {
            return Integer.MAX_VALUE;
        }
        return player.getArqueroOrdenMarcado() > 0 ? player.getArqueroOrdenMarcado() : Integer.MAX_VALUE;
    }

    private void inheritBrandingFromFieldDesign() {
        if (controller.disenoCampoConfig == null || controller.disenoArqueroConfig == null) {
            return;
        }
        controller.disenoArqueroConfig.setChestBrandVisible(controller.disenoCampoConfig.isChestBrandVisible());
        controller.disenoArqueroConfig.setChestBrandPosition(controller.disenoCampoConfig.getChestBrandPosition());
        controller.disenoArqueroConfig.setBrandTech(controller.disenoCampoConfig.getBrandTech());
        controller.disenoArqueroConfig.setShortBrandVisible(controller.disenoCampoConfig.isShortBrandVisible());
        controller.disenoArqueroConfig.setSocksBrandVisible(controller.disenoCampoConfig.isSocksBrandVisible());
    }

    private void applyTableSettingsToLiveState(PrendaState goalkeeperState, DetallePedido player) {
        applyReferenceColorToLiveState(
                goalkeeperState,
                player.getColorArquero(),
                player.isIncludeBottom(),
                player.isIncludeSocks());
        ensureDefaultNumbers(goalkeeperState);

        if (player.getGenero() != null) {
            try {
                goalkeeperState.setGenero(TipoGenero.valueOf(player.getGenero().toUpperCase()));
            } catch (Exception ignored) {
            }
        }

        goalkeeperState.setLargo(resolveSleeveLength(player));
        goalkeeperState.setHasShorts(player.isIncludeBottom());
        goalkeeperState.setHasSocks(player.isIncludeSocks());

        if (player.isIncludeBottom()) {
            goalkeeperState.setCorteShort(resolveShortCut(player.getTipoBottom(), goalkeeperState.getCorteShort()));
        }
        if (player.isIncludeSocks()) {
            goalkeeperState.setTipoMedias(resolveSocksType(player.getTipoMedias(), goalkeeperState.getTipoMedias()));
        }
    }

    private void ensureDefaultNumbers(PrendaStateDTO config, boolean createdNow) {
        if (config == null) {
            return;
        }

        forceGoalkeeperNumber(config.getCurrentBackNumber(), config::setCurrentBackNumber);
        forceGoalkeeperNumber(config.getCurrentChestNumber(), config::setCurrentChestNumber);
        forceGoalkeeperNumber(config.getCurrentShortNumber(), config::setCurrentShortNumber);

        if (createdNow || isBlank(config.getCurrentBackNumber())) {
            config.setCurrentBackNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
        if (config.isChestNumberVisible() && (createdNow || isBlank(config.getCurrentChestNumber()))) {
            config.setCurrentChestNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
        if (config.isHasShorts() && (createdNow || isBlank(config.getCurrentShortNumber()))) {
            config.setCurrentShortNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
    }

    private void ensureDefaultNumbers(PrendaState state) {
        if (state == null) {
            return;
        }
        if (isBlank(state.getCurrentBackNumber())) {
            state.setCurrentBackNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
        if (isBlank(state.getCurrentChestNumber())) {
            state.setCurrentChestNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
        if (isBlank(state.getCurrentShortNumber())) {
            state.setCurrentShortNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
    }

    private void forceGoalkeeperNumber(String currentValue, java.util.function.Consumer<String> setter) {
        if ("9".equals(currentValue)) {
            setter.accept(DEFAULT_GOALKEEPER_NUMBER);
        }
    }

    private TipoLargo resolveSleeveLength(DetallePedido player) {
        String sleeveValue = player.getTipoMangaArquero();
        if (isBlank(sleeveValue)) {
            sleeveValue = player.getTipoManga();
        }
        if (sleeveValue == null) {
            return TipoLargo.MANGA_CORTA;
        }
        String normalized = sleeveValue.toUpperCase();
        if (normalized.contains("LARGA")) {
            return TipoLargo.MANGA_LARGA;
        }
        if (normalized.contains("0") || normalized.contains("SIN")) {
            return TipoLargo.MANGA_CERO;
        }
        return TipoLargo.MANGA_CORTA;
    }

    private TipoCorte resolveShortCut(String tipoBottom, TipoCorte fallback) {
        if (isBlank(tipoBottom)) {
            return fallback != null ? fallback : TipoCorte.CUADRADO;
        }
        String normalized = tipoBottom.trim().toUpperCase();
        if (normalized.contains("PANTALONETA")) {
            return TipoCorte.PANTALONETA;
        }
        if (normalized.contains("LICRA")) {
            return TipoCorte.LICRA;
        }
        if (normalized.contains("REDON")) {
            return TipoCorte.REDONDO;
        }
        if (normalized.contains("RANGL")) {
            return TipoCorte.RANGLAN;
        }
        if (normalized.contains("NOVA")) {
            return TipoCorte.NOVA;
        }
        if (normalized.contains("CUADRAD") || normalized.contains("SHORT")) {
            return TipoCorte.CUADRADO;
        }
        return fallback != null ? fallback : TipoCorte.CUADRADO;
    }

    private TipoMedias resolveSocksType(String tipoMedias, TipoMedias fallback) {
        if (isBlank(tipoMedias)) {
            return fallback != null ? fallback : TipoMedias.PROFESIONAL;
        }
        String normalized = tipoMedias.trim().toUpperCase();
        if (normalized.contains("ADULTO")) {
            return TipoMedias.ADULTO;
        }
        if (normalized.contains("JUVEN")) {
            return TipoMedias.JUVENIL;
        }
        if (normalized.contains("NI") || normalized.contains("NIN")) {
            return TipoMedias.NINOS;
        }
        if (normalized.contains("PROF")) {
            return TipoMedias.PROFESIONAL;
        }
        return fallback != null ? fallback : TipoMedias.PROFESIONAL;
    }

    private List<LayerDTO> copyLayers(List<LayerDTO> source) {
        List<LayerDTO> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }
        for (LayerDTO layer : source) {
            if (layer != null) {
                copy.add(layer.deepCopy());
            }
        }
        return copy;
    }

    private void applyReferenceColorToConfig(
            PrendaStateDTO config,
            Color newReferenceColor,
            boolean includeBottom,
            boolean includeSocks) {
        if (config == null || newReferenceColor == null) {
            return;
        }
        if (config.getColors() == null) {
            config.setColors(new HashMap<>());
        }

        String oldReferenceHex = config.getReferenceColorArquero();
        String newReferenceHex = colorToHex(newReferenceColor);
        replaceConfigReferenceColor(config.getColors(), oldReferenceHex, newReferenceHex, "body", "sleeves",
                "shirt", "collar", "cuff", "mesh", "shirtStripe");
        if (includeBottom) {
            replaceConfigReferenceColor(config.getColors(), oldReferenceHex, newReferenceHex, "shorts",
                    "shortsStripe", "shortsPicket", "shortsCuff");
        }
        if (includeSocks) {
            replaceConfigReferenceColor(config.getColors(), oldReferenceHex, newReferenceHex, "socks",
                    "socksDetail", "socksTop");
        }
        config.setReferenceColorArquero(newReferenceHex);
    }

    private void applyReferenceColorToLiveState(
            PrendaState state,
            Color newReferenceColor,
            boolean includeBottom,
            boolean includeSocks) {
        if (state == null || newReferenceColor == null) {
            return;
        }

        Color oldReferenceColor = state.getColorReferenciaArquero();
        replaceLiveReferenceColor(state.getColors(), oldReferenceColor, newReferenceColor, "body", "sleeves",
                "collar", "cuff", "mesh", "shirtStripe");
        if (includeBottom) {
            replaceLiveReferenceColor(state.getColors(), oldReferenceColor, newReferenceColor, "shorts",
                    "shortsStripe", "shortsPicket", "shortsCuff");
        }
        if (includeSocks) {
            replaceLiveReferenceColor(state.getColors(), oldReferenceColor, newReferenceColor, "socks",
                    "socksDetail", "socksTop");
        }
        state.setColorReferenciaArquero(newReferenceColor);
    }

    private void replaceConfigReferenceColor(
            java.util.Map<String, String> colors,
            String oldReferenceHex,
            String newReferenceHex,
            String... keys) {
        for (String key : keys) {
            String current = colors.get(key);
            if (isBlank(current) || isBlank(oldReferenceHex) || sameHex(current, oldReferenceHex)) {
                colors.put(key, newReferenceHex);
            }
        }
    }

    private void replaceLiveReferenceColor(
            java.util.Map<String, Color> colors,
            Color oldReferenceColor,
            Color newReferenceColor,
            String... keys) {
        for (String key : keys) {
            Color current = colors.get(key);
            if (current == null || oldReferenceColor == null || sameColor(current, oldReferenceColor)) {
                colors.put(key, newReferenceColor);
            }
        }
    }

    private boolean sameColor(Color left, Color right) {
        return left != null && right != null && colorToHex(left).equalsIgnoreCase(colorToHex(right));
    }

    private boolean sameHex(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String colorToHex(Color color) {
        if (color == null) {
            return "#00000000";
        }
        return String.format("#%02X%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                (int) (color.getOpacity() * 255));
    }
}
