package org.example.controller;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.example.dto.save.GoalkeeperDesignDTO;
import org.example.dto.save.LayerDTO;
import org.example.dto.save.TextDTO;
import org.example.dto.save.PrendaStateDTO;
import org.example.dto.save.ProjectState;
import org.example.model.DetallePedido;
import org.example.model.PrendaState;
import org.example.model.TipoCorte;
import org.example.model.TipoGenero;
import org.example.model.TipoLargo;
import org.example.model.TipoMedias;
import org.example.component.PrendaVisualizer;

import java.util.*;
import java.util.stream.Collectors;

final class PedidoGoalkeeperCoordinator {

    private static final String PLAYER_MODE = "Jugador";
    private static final String DEFAULT_GOALKEEPER_NUMBER = "1";

    private final PedidoController controller;
    private boolean isSyncing = false;

    PedidoGoalkeeperCoordinator(PedidoController controller) {
        this.controller = controller;
    }

    void restoreGoalkeeperSlotsFromProject(ProjectState state) {
        clearDebounceTimers();
        controller.disenosArquero.clear();
        controller.comboArqueroDesignIds.clear();
        if (state.getGoalkeeperDesigns() != null) {
            for (GoalkeeperDesignDTO dto : state.getGoalkeeperDesigns()) {
                GoalkeeperDesignDTO copy = dto.deepCopy();
                copy.setPersonalized(true);
                controller.disenosArquero.put(dto.getDesignId(), copy);
                if (dto.getGarmentConfig() != null) {
                    System.out.println("DEBUG: Loading goalkeeper design slot: " + dto.getDesignId() 
                        + ", isPersonalized: " + dto.isPersonalized());
                    System.out.println("  - Gender: " + dto.getGarmentConfig().getCurrentGenero()
                        + ", Cut: " + dto.getGarmentConfig().getCurrentCorte()
                        + ", Largo: " + dto.getGarmentConfig().getCurrentLargo()
                        + ", HasShorts: " + dto.getGarmentConfig().isHasShorts()
                        + ", HasSocks: " + dto.getGarmentConfig().isHasSocks());
                } else {
                    System.out.println("DEBUG: Loading goalkeeper design slot: " + dto.getDesignId() 
                        + ", isPersonalized: " + dto.isPersonalized() + " (No garment config)");
                }
            }
        }
        updateOptionStatus();
    }

    String getDesignIdForComboLabel(String label) {
        return controller.comboArqueroDesignIds.get(label);
    }

    void updateOptionStatus() {
        if (controller.comboModoDiseno == null) {
            return;
        }

        List<DetallePedido> goalkeepers = controller.listaJugadores.stream()
                .filter(DetallePedido::isEsArquero)
                .sorted(Comparator.comparingInt(p -> p.getArqueroOrdenMarcado() > 0 ? p.getArqueroOrdenMarcado() : Integer.MAX_VALUE))
                .collect(Collectors.toList());

        boolean previousSwitchingState = controller.switchingDesignMode;
        controller.switchingDesignMode = true;
        try {
            // Guardar el estado actual antes de reconstruir el combo
            String currentComboValue = controller.comboModoDiseno.getValue();

            // Reconstruir lista del combo
            controller.comboModoDiseno.getItems().clear();
            controller.comboModoDiseno.getItems().add(PLAYER_MODE);
            controller.comboArqueroDesignIds.clear();

            if (goalkeepers.isEmpty()) {
                if (controller.editandoDisenoArquero) {
                    changeDesignMode(false, null);
                }
                controller.comboModoDiseno.setValue(PLAYER_MODE);
                controller.comboModoDiseno.setDisable(true);
                return;
            }

            controller.comboModoDiseno.setDisable(false);

            // Poblar diseños de arqueros basados en los items únicos
            Set<String> processedDesignIds = new HashSet<>();
            int order = 1;

            for (DetallePedido gk : goalkeepers) {
                String designId = gk.ensureArqueroDesignId();
                if (processedDesignIds.add(designId)) {
                    GoalkeeperDesignDTO design = controller.disenosArquero.computeIfAbsent(designId, id -> {
                        GoalkeeperDesignDTO newDesign = new GoalkeeperDesignDTO();
                        newDesign.setDesignId(id);
                        newDesign.setOrder(gk.getArqueroOrdenMarcado());
                        return newDesign;
                    });

                    // Generar label (Ej: Arquero 1 (Rojo))
                    String colorName = "Config " + order;
                    if (gk.getColorArquero() != null) {
                        colorName = colorToHex(gk.getColorArquero()); 
                    }
                    String label = "Arquero " + order;
                    design.setLabel(label);
                    
                    controller.comboModoDiseno.getItems().add(label);
                    controller.comboArqueroDesignIds.put(label, designId);
                    
                    // Asegurar que exista un diseño base
                    if (design.getGarmentConfig() == null) {
                        syncDesignFromPlayer(gk);
                    }
                    
                    order++;
                }
            }

            // Set default Ficha Técnica si no hay
            if (controller.arqueroFichaDesignId == null || !controller.disenosArquero.containsKey(controller.arqueroFichaDesignId)) {
                if (!goalkeepers.isEmpty() && goalkeepers.get(0) != null) {
                    controller.arqueroFichaDesignId = goalkeepers.get(0).getArqueroDesignId();
                }
            }

            // Restaurar selección del combo
            if (currentComboValue != null && controller.comboModoDiseno.getItems().contains(currentComboValue)) {
                controller.comboModoDiseno.setValue(currentComboValue);
            } else if (controller.editandoDisenoArquero && controller.arqueroActivoDesignId != null) {
                // Encontrar el label para el designId activo
                String labelForActive = null;
                for (Map.Entry<String, String> entry : controller.comboArqueroDesignIds.entrySet()) {
                    if (entry.getValue().equals(controller.arqueroActivoDesignId)) {
                        labelForActive = entry.getKey();
                        break;
                    }
                }
                if (labelForActive != null) {
                    controller.comboModoDiseno.setValue(labelForActive);
                } else {
                    controller.comboModoDiseno.setValue(PLAYER_MODE);
                    changeDesignMode(false, null);
                }
            } else {
                controller.comboModoDiseno.setValue(PLAYER_MODE);
            }

        } finally {
            controller.switchingDesignMode = previousSwitchingState;
        }
    }

    void setupVisualizerListener(PrendaVisualizer visualizer) {
        visualizer.addOnStateChanged(() -> {
            if (isSyncing || controller.isGeneratingFicha || controller.isRestoringDesign || controller.switchingDesignMode) return;
            if (controller.editandoDisenoArquero && controller.arqueroActivoDesignId != null) {
                isSyncing = true;
                try {
                    // Guardar el diseño actual en el slot del arquero activo (mantiene disenosArquero en sincronía)
                    controller.guardarDisenoActualEnSlot();

                    PrendaState activeState = visualizer.getArqueroState();
                    if (activeState != null) {
                        // Al realizar cambios manuales, forzamos que se considere personalizado
                        visualizer.setArqueroDisenoPersonalizado(true);
                        
                        // Guardar la personalización también en el DTO
                        GoalkeeperDesignDTO design = controller.disenosArquero.get(controller.arqueroActivoDesignId);
                        if (design != null) {
                            design.setPersonalized(true);
                        }

                        // Sincronizar hacia atrás al roster (listaJugadores)
                        for (DetallePedido player : controller.listaJugadores) {
                            if (player.isEsArquero() && controller.arqueroActivoDesignId.equals(player.getArqueroDesignId())) {
                                
                                // Género
                                if (activeState.getGenero() != null) {
                                    String genderStr = activeState.getGenero().name();
                                    if (!genderStr.equals(player.getGenero())) {
                                        player.setGenero(genderStr);
                                    }
                                }

                                // Manga
                                if (activeState.getLargo() != null) {
                                    String mappedSleeve;
                                    switch (activeState.getLargo()) {
                                        case MANGA_LARGA:
                                            mappedSleeve = "LARGA";
                                            break;
                                        case MANGA_CERO:
                                            mappedSleeve = "MANGA 0";
                                            break;
                                        case MANGA_3_4:
                                            mappedSleeve = "MANGA 3/4";
                                            break;
                                        case MANGA_CORTA:
                                        default:
                                            mappedSleeve = "CORTA";
                                            break;
                                    }
                                    if (!mappedSleeve.equals(player.getTipoMangaArquero())) {
                                        player.setTipoMangaArquero(mappedSleeve);
                                    }
                                    if (!mappedSleeve.equals(player.getTipoManga())) {
                                        player.setTipoManga(mappedSleeve);
                                    }
                                }

                                // Shorts (includeBottom)
                                if (player.isIncludeBottom() != activeState.hasShorts()) {
                                    player.setIncludeBottom(activeState.hasShorts());
                                }

                                // Medias (includeSocks)
                                if (player.isIncludeSocks() != activeState.hasSocks()) {
                                    player.setIncludeSocks(activeState.hasSocks());
                                }

                                // Tipo de short / bottom
                                if (activeState.getCorteShort() != null) {
                                    String bottomStr;
                                    switch (activeState.getCorteShort()) {
                                        case LICRA:
                                            bottomStr = "Licra";
                                            break;
                                        case PANTALONETA:
                                            bottomStr = "Pantaloneta";
                                            break;
                                        default:
                                            bottomStr = "Short";
                                            break;
                                    }
                                    if (!bottomStr.equals(player.getTipoBottom())) {
                                        player.setTipoBottom(bottomStr);
                                    }
                                }

                                // Tipo Medias
                                if (activeState.getTipoMedias() != null) {
                                    String mediaStr = activeState.getTipoMedias().name();
                                    if (!mediaStr.equals(player.getTipoMedias())) {
                                        player.setTipoMedias(mediaStr);
                                    }
                                }
                            }
                        }
                        
                        // Refrescar tabla del roster para reflejar cambios de inmediato
                        if (controller.jugadoresDelegate != null) {
                            controller.jugadoresDelegate.cargarListaJugadores();
                        } else if (controller.tablaDetalles != null) {
                            controller.tablaDetalles.refresh();
                        }
                    }
                } finally {
                    isSyncing = false;
                }
            }
        });
    }

    private final java.util.Map<String, javafx.animation.PauseTransition> debounceTimers = new java.util.HashMap<>();

    private void debounceSync(DetallePedido player) {
        String designId = player.getArqueroDesignId();
        if (designId == null) return;
        
        javafx.animation.PauseTransition existing = debounceTimers.get(designId);
        if (existing != null) {
            existing.stop();
        }
        
        javafx.animation.PauseTransition timer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
        timer.setOnFinished(e -> {
            debounceTimers.remove(designId);
            if (player.isEsArquero()) {
                syncDesignFromPlayer(player);
                if (controller.editandoDisenoArquero && designId.equals(controller.arqueroActivoDesignId)) {
                    syncLiveStateFromRosterUpdate(player);
                }
            }
        });
        debounceTimers.put(designId, timer);
        timer.play();
    }

    public void clearDebounceTimers() {
        for (javafx.animation.PauseTransition timer : debounceTimers.values()) {
            timer.stop();
        }
        debounceTimers.clear();
    }

    void attachPlayerListeners(DetallePedido player) {
        player.esArqueroProperty().addListener((obs, oldValue, isGoalkeeper) -> {
            if (isSyncing || controller.isRestoringDesign || controller.switchingDesignMode) return;
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
            syncDesignFromPlayer(player);
        });

        javafx.beans.value.ChangeListener<Object> syncListener = (obs, oldValue, newValue) -> {
            if (isSyncing || controller.isRestoringDesign || controller.switchingDesignMode) return;
            debounceSync(player);
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
        if (player == null || !player.isEsArquero() || controller.prendaVisualizer == null) {
            return;
        }

        PrendaState goalkeeperState = controller.prendaVisualizer.getArqueroState();
        if (goalkeeperState == null) {
            return;
        }

        applyTableSettingsToLiveState(goalkeeperState, player);

        if (player.getArqueroDesignId() != null) {
            org.example.dto.save.GoalkeeperDesignDTO design = controller.disenosArquero.get(player.getArqueroDesignId());
            if (design != null && design.getGarmentConfig() != null) {
                applyReferenceColorToConfig(design.getGarmentConfig(), colorToHex(player.getColorArquero()), player.isIncludeBottom(), player.isIncludeSocks());
            }
        }

        if (controller.editandoDisenoArquero && player.getArqueroDesignId().equals(controller.arqueroActivoDesignId)) {
            // Optimization: Apply UI changes directly without full reload unless necessary
            if (controller.prendaVisualizer.getArqueroColorManager() != null) {
                controller.prendaVisualizer.getArqueroColorManager().reapplyColors();
            }
            // cargarCapas es inteligente y solo recarga si hay cambios estructurales (hasShorts, etc)
            controller.prendaVisualizer.cargarCapas();
        }
    }

    void syncDesignFromPlayer(DetallePedido player) {
        if (player == null || !player.isEsArquero()) return;

        String designId = player.ensureArqueroDesignId();
        GoalkeeperDesignDTO design = controller.disenosArquero.computeIfAbsent(designId, id -> new GoalkeeperDesignDTO());
        design.setDesignId(designId);

        boolean createdNow = false;

        if (design.getGarmentConfig() == null) {
            if (controller.disenoCampoConfig == null) {
                controller.guardarDisenoActualEnSlot();
            }
            design.setGarmentConfig(controller.disenoCampoConfig != null
                    ? controller.disenoCampoConfig.deepCopy()
                    : new PrendaStateDTO());
            
            // Prevent goalkeepers from inheriting the player's reference images
            if (design.getGarmentConfig().getHotspots() != null) {
                design.getGarmentConfig().getHotspots().clear();
            }
            
            // COPIA INTELIGENTE DE CAPAS (UNA SOLA VEZ AL CREAR)
            design.setLayers(copyLayersIntelligently(controller.disenoCampoLayers, design.getGarmentConfig(), player));
            
            design.getGarmentConfig().setHasShirt(true);
            createdNow = true;
            design.setPersonalized(true);
            if (controller.prendaVisualizer != null && designId.equals(controller.arqueroActivoDesignId)) {
                controller.prendaVisualizer.setArqueroDisenoPersonalizado(true);
            }
        }

        // Siempre omitir la sincronización automática posterior para mantener la independencia
        System.out.println("[GoalkeeperDesign] Bypassing syncDesignFromPlayer layer/branding inheritance for designId: " + designId + " (Always personalized)");
        
        // Herencia de emblemas y marcas corporativas al crear por primera vez
        if (createdNow) {
            inheritBrandingFromFieldDesign(design.getGarmentConfig());
        }

        PrendaStateDTO config = design.getGarmentConfig();
        ensureDefaultNumbers(config, createdNow);
        
        // Reset number positions for goalkeeper so they don't overlap with field player's positions
        // Goalkeeper inherits style (colors, fonts) but not position or value
        if (createdNow) {
            config.setChestNumberX(0);
            config.setChestNumberY(0);
            config.setChestNumberScale(1.0);
            config.setBackNumberX(0);
            config.setBackNumberY(0);
            config.setBackNumberScale(1.0);
            config.setShortNumberX(0);
            config.setShortNumberY(0);
            config.setShortNumberScale(1.0);
        }
        
        // Always sync structural properties from the table to the design config DTO
        config.setHasShorts(player.isIncludeBottom());
        config.setHasSocks(player.isIncludeSocks());
        if (player.isIncludeBottom()) {
            config.setCurrentCorteShort(resolveShortCut(player.getTipoBottom(), config.getCurrentCorteShort()));
        }
        if (player.isIncludeSocks()) {
            config.setCurrentTipoMedias(resolveSocksType(player.getTipoMedias(), config.getCurrentTipoMedias()));
        }
        config.setHasShortsLining(controller.disenoCampoConfig != null && controller.disenoCampoConfig.isHasShortsLining());
        if (player.getGenero() != null) {
            try {
                config.setCurrentGenero(TipoGenero.valueOf(player.getGenero().toUpperCase()));
            } catch (Exception ignored) {}
        }
        config.setCurrentLargo(resolveSleeveLength(player));

        if (player.getColorArquero() != null) {
            applyReferenceColorToConfig(config, colorToHex(player.getColorArquero()), player.isIncludeBottom(), player.isIncludeSocks());
        }

        // Skip async restore during ficha generation to avoid redundant hotspot image reloads
        if (!controller.isGeneratingFicha && controller.editandoDisenoArquero 
                && designId.equals(controller.arqueroActivoDesignId) && controller.prendaVisualizer != null) {
            Platform.runLater(() -> {
                boolean prevSwitching = controller.switchingDesignMode;
                controller.switchingDesignMode = true;
                try {
                    controller.restaurarDisenoSilencioso(true, config, design.getLayers());
                    if (controller.prendaDelegate != null) {
                        controller.prendaDelegate.restoreFromState(config);
                    }
                } finally {
                    controller.switchingDesignMode = prevSwitching;
                }
            });
        }
    }

    /**
     * Copia las capas, pero si la prenda de arquero NO tiene la pieza requerida,
     * la capa se extrae del PowerClip (activeZone = null) para que se dibuje encima del lienzo.
     */
    private List<LayerDTO> copyLayersIntelligently(List<LayerDTO> source, PrendaStateDTO goalieConfig, DetallePedido player) {
        List<LayerDTO> copy = new ArrayList<>();
        if (source == null) return copy;
        
        TipoLargo largo = resolveSleeveLength(player);
        boolean hasSleeves = largo != TipoLargo.MANGA_CERO;
        boolean hasShorts = player.isIncludeBottom();
        boolean hasSocks = player.isIncludeSocks();

        for (LayerDTO layer : source) {
            if (layer != null) {
                LayerDTO cloned = layer.deepCopy();
                String zone = cloned.getActiveZone();
                
                if (zone != null) {
                    if (zone.contains("manga") && !hasSleeves) {
                        cloned.setActiveZone(null); // Soltar en lienzo
                    } else if (zone.contains("short") && !hasShorts) {
                        cloned.setActiveZone(null); // Soltar en lienzo
                    } else if (zone.contains("media") && !hasSocks) {
                        cloned.setActiveZone(null); // Soltar en lienzo
                    }
                }
                
                // Dorsal 9 -> 1 for goalkeepers
                if (cloned instanceof TextDTO) {
                    TextDTO textDto = (TextDTO) cloned;
                    if ("9".equals(textDto.getText())) {
                        textDto.setText("1");
                    }
                }
                
                copy.add(cloned);
            }
        }
        return copy;
    }

    void changeDesignMode(boolean toGoalkeeper, String targetDesignId) {
        if (controller.prendaVisualizer == null || controller.switchingDesignMode) {
            return;
        }

        controller.switchingDesignMode = true;
        try {
            controller.guardarDisenoActualEnSlot();

            controller.editandoDisenoArquero = toGoalkeeper;
            controller.arqueroActivoDesignId = targetDesignId;
            controller.prendaVisualizer.setActiveDesign(toGoalkeeper);

            String label = null;
            if (toGoalkeeper && targetDesignId != null) {
                GoalkeeperDesignDTO design = controller.disenosArquero.get(targetDesignId);
                if (design != null) {
                    label = design.getLabel();
                }
            }
            if (controller.prendaVisualizer.getUiController() != null) {
                controller.prendaVisualizer.getUiController().updateModeIndicatorVisuals(toGoalkeeper, label);
            }

            if (controller.prendaDelegate != null) {
                controller.prendaDelegate.setArqueroMode(toGoalkeeper);
            }

            if (controller.prendaVisualizer.getColorManager() != null) {
                controller.prendaVisualizer.getColorManager().clearCache();
            }

            PrendaStateDTO targetConfig = null;
            List<LayerDTO> targetLayers = null;

            if (toGoalkeeper && targetDesignId != null) {
                GoalkeeperDesignDTO design = controller.disenosArquero.get(targetDesignId);
                if (design == null || design.getGarmentConfig() == null) {
                    // Try to sync from table
                    DetallePedido gk = getPlayerByDesignId(targetDesignId);
                    if (gk != null) {
                        syncDesignFromPlayer(gk);
                        design = controller.disenosArquero.get(targetDesignId);
                    }
                }
                if (design != null) {
                    // Omitir la sincronización automática de capas/branding al cambiar de modo para mantener la independencia
                    targetConfig = design.getGarmentConfig();
                    targetLayers = design.getLayers();
                    controller.prendaVisualizer.setArqueroDisenoPersonalizado(true);
                }
            } else {
                targetConfig = controller.disenoCampoConfig;
                targetLayers = controller.disenoCampoLayers;
            }

            if (targetConfig != null) {
                controller.restaurarDisenoSilencioso(toGoalkeeper, targetConfig, targetLayers);
            }

            if (toGoalkeeper) {
                applySpecificsFromTable();
            }

            controller.prendaVisualizer.resetView();
            // Clear signature cache so goalkeeper numbers are NOT skipped by the smart-diff check
            controller.prendaVisualizer.getNumberManager().clearNodeSignatures();
            controller.prendaVisualizer.cargarCapas();

            PrendaStateDTO finalConfig = targetConfig;
            if (!controller.isGeneratingFicha) {
                Platform.runLater(() -> {
                    boolean prevSwitching = controller.switchingDesignMode;
                    controller.switchingDesignMode = true;
                    try {
                        if (controller.personalizacionDelegate != null) {
                            controller.personalizacionDelegate.refreshContent();
                        }
                        if (controller.prendaDelegate != null && finalConfig != null) {
                            controller.prendaDelegate.restoreFromState(finalConfig);
                        }
                        
                        // Actualizar botón de Ficha Técnica si existe en UI
                        updateFichaTecnicaUI(targetDesignId);
                    } finally {
                        controller.switchingDesignMode = prevSwitching;
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("ERROR switching design: " + e.getMessage());
            e.printStackTrace();
        } finally {
            controller.switchingDesignMode = false;
        }
    }

    void changeDesignMode(boolean toGoalkeeper) {
        changeDesignMode(toGoalkeeper, null);
    }

    private void updateFichaTecnicaUI(String activeDesignId) {
        // Podría controlarse un ToggleButton o Label en PedidoController aquí
        // Por ejemplo, si controller tiene un ToggleButton `btnFichaArquero`:
        // if (controller.btnFichaArquero != null) {
        //    controller.btnFichaArquero.setVisible(controller.editandoDisenoArquero);
        //    controller.btnFichaArquero.setSelected(activeDesignId != null && activeDesignId.equals(controller.arqueroFichaDesignId));
        // }
    }

    public void setAsTechnicalSheetDesign() {
        if (controller.editandoDisenoArquero && controller.arqueroActivoDesignId != null) {
            controller.arqueroFichaDesignId = controller.arqueroActivoDesignId;
            System.out.println("Set as Technical Sheet Design: " + controller.arqueroFichaDesignId);
            updateFichaTecnicaUI(controller.arqueroActivoDesignId);
            controller.fichaDirty = true;
            controller.markProjectDirty();
        }
    }

    void applySpecificsFromTable() {
        if (controller.prendaVisualizer == null || controller.arqueroActivoDesignId == null) {
            return;
        }
        DetallePedido player = getPlayerByDesignId(controller.arqueroActivoDesignId);
        if (player != null) {
            PrendaState goalkeeperState = controller.prendaVisualizer.getArqueroState();
            if (goalkeeperState != null) {
                applyTableSettingsToLiveState(goalkeeperState, player);
            }
        }
    }

    void applySpecificsFromStoredDesign() {
        for (DetallePedido player : controller.listaJugadores) {
            if (player.isEsArquero()) {
                syncDesignFromPlayer(player);
            }
        }
    }

    DetallePedido getPrimaryGoalkeeper() {
        return controller.listaJugadores.stream()
                .filter(DetallePedido::isEsArquero)
                .min(Comparator.comparingInt(p -> p.getArqueroOrdenMarcado() > 0 ? p.getArqueroOrdenMarcado() : Integer.MAX_VALUE))
                .orElse(null);
    }
    
    DetallePedido getPlayerByDesignId(String designId) {
        if (designId == null) return null;
        return controller.listaJugadores.stream()
                .filter(p -> designId.equals(p.getArqueroDesignId()))
                .findFirst()
                .orElse(null);
    }

    DetallePedido getTechnicalSheetGoalkeeper() {
        if (controller.arqueroFichaDesignId != null) {
            DetallePedido gk = getPlayerByDesignId(controller.arqueroFichaDesignId);
            if (gk != null) return gk;
        }
        return getPrimaryGoalkeeper();
    }

    String getTechnicalSheetGoalkeeperDesignId() {
        return controller.arqueroFichaDesignId;
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

    private void inheritBrandingFromFieldDesign(PrendaStateDTO config) {
        if (controller.disenoCampoConfig == null || config == null) {
            return;
        }
        config.setChestBrandVisible(controller.disenoCampoConfig.isChestBrandVisible());
        config.setChestBrandPosition(controller.disenoCampoConfig.getChestBrandPosition());
        config.setBrandTech(controller.disenoCampoConfig.getBrandTech());
        config.setShortBrandVisible(controller.disenoCampoConfig.isShortBrandVisible());
        config.setSocksBrandVisible(controller.disenoCampoConfig.isSocksBrandVisible());
        
        // No hay propiedades de texto en PrendaStateDTO, son guardadas como UserLayers (TextDTO)
        // La copia de TextDTO ya se realiza en copyLayersIntelligently
    }

    private void applyTableSettingsToLiveState(PrendaState goalkeeperState, DetallePedido player) {
        if (player.getColorArquero() != null) {
            applyReferenceColorToLiveState(
                    goalkeeperState,
                    colorToHex(player.getColorArquero()),
                    player.isIncludeBottom(),
                    player.isIncludeSocks());
        }

        if (player.getGenero() != null) {
            try {
                goalkeeperState.setGenero(TipoGenero.valueOf(player.getGenero().toUpperCase()));
            } catch (Exception ignored) {}
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
        ensureDefaultNumbers(goalkeeperState);
    }

    private void ensureDefaultNumbers(PrendaStateDTO config, boolean createdNow) {
        if (config == null) return;
        // Only assign the default when the design is brand-new OR the field is blank.
        // This prevents field-player numbers from bleeding into existing goalkeeper numbers.
        if (createdNow || isBlank(config.getCurrentBackNumber())) {
            config.setCurrentBackNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
        if (createdNow || isBlank(config.getCurrentChestNumber())) {
            config.setCurrentChestNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
        if (createdNow || isBlank(config.getCurrentShortNumber())) {
            config.setCurrentShortNumber(DEFAULT_GOALKEEPER_NUMBER);
        }
    }

    private void ensureDefaultNumbers(PrendaState state) {
        if (state == null) return;
        // Only assign default if the field is blank — never overwrite existing goalkeeper numbers.
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

    private TipoLargo resolveSleeveLength(DetallePedido player) {
        String sleeveValue = player.getTipoMangaArquero();
        if (isBlank(sleeveValue)) {
            sleeveValue = player.getTipoManga();
        }
        if (sleeveValue == null) {
            return TipoLargo.MANGA_CORTA;
        }
        String normalized = sleeveValue.toUpperCase();
        if (normalized.contains("LARGA")) return TipoLargo.MANGA_LARGA;
        if (normalized.contains("0") || normalized.contains("SIN")) return TipoLargo.MANGA_CERO;
        return TipoLargo.MANGA_CORTA;
    }

    private TipoCorte resolveShortCut(String tipoBottom, TipoCorte fallback) {
        if (isBlank(tipoBottom)) return fallback != null ? fallback : TipoCorte.CUADRADO;
        String normalized = tipoBottom.trim().toUpperCase();
        if (normalized.contains("PANTALONETA")) return TipoCorte.PANTALONETA;
        if (normalized.contains("LICRA")) return TipoCorte.LICRA;
        if (normalized.contains("REDON")) return TipoCorte.REDONDO;
        if (normalized.contains("RANGL")) return TipoCorte.RANGLAN;
        if (normalized.contains("NOVA")) return TipoCorte.NOVA;
        if (normalized.contains("CUADRAD") || normalized.contains("SHORT")) return TipoCorte.CUADRADO;
        return fallback != null ? fallback : TipoCorte.CUADRADO;
    }

    private TipoMedias resolveSocksType(String tipoMedias, TipoMedias fallback) {
        if (isBlank(tipoMedias)) return fallback != null ? fallback : TipoMedias.PROFESIONAL;
        String normalized = tipoMedias.trim().toUpperCase();
        if (normalized.contains("ADULTO")) return TipoMedias.ADULTO;
        if (normalized.contains("JUVEN")) return TipoMedias.JUVENIL;
        if (normalized.contains("NI") || normalized.contains("NIN")) return TipoMedias.NINOS;
        if (normalized.contains("PROF")) return TipoMedias.PROFESIONAL;
        return fallback != null ? fallback : TipoMedias.PROFESIONAL;
    }

    private void applyReferenceColorToConfig(PrendaStateDTO config, String newReferenceHex, boolean includeBottom, boolean includeSocks) {
        if (config == null || newReferenceHex == null || newReferenceHex.isBlank()) return;
        if (config.getColors() == null) config.setColors(new HashMap<>());

        String oldReferenceHex = config.getReferenceColorArquero();
        
        replaceConfigReferenceColor(config.getColors(), oldReferenceHex, newReferenceHex, "body", "sleeves", "shirt", "cuff", "mesh", "shirtStripe");
        if (includeBottom) {
            replaceConfigReferenceColor(config.getColors(), oldReferenceHex, newReferenceHex, "shorts", "shortsStripe", "shortsCuff", "shortsPicket");
        }
        if (includeSocks) {
            replaceConfigReferenceColor(config.getColors(), oldReferenceHex, newReferenceHex, "socks");
        }
        config.setReferenceColorArquero(newReferenceHex);
    }

    private void applyReferenceColorToLiveState(PrendaState state, String newReferenceHex, boolean includeBottom, boolean includeSocks) {
        if (state == null || newReferenceHex == null || newReferenceHex.isBlank()) return;
        
        Color newReferenceColor;
        try {
            newReferenceColor = Color.web(newReferenceHex);
        } catch (Exception e) {
            return;
        }

        Color oldReferenceColor = state.getColorReferenciaArquero();
        
        replaceLiveReferenceColor(state.getColors(), oldReferenceColor, newReferenceColor, "body", "sleeves", "shirt", "cuff", "mesh", "shirtStripe");
        if (includeBottom) {
            replaceLiveReferenceColor(state.getColors(), oldReferenceColor, newReferenceColor, "shorts", "shortsStripe", "shortsCuff", "shortsPicket");
        }
        if (includeSocks) {
            replaceLiveReferenceColor(state.getColors(), oldReferenceColor, newReferenceColor, "socks");
        }
        state.setColorReferenciaArquero(newReferenceColor);
    }

    private void replaceConfigReferenceColor(java.util.Map<String, String> colors, String oldReferenceHex, String newReferenceHex, String... keys) {
        for (String key : keys) {
            String current = colors.get(key);
            if (isBlank(current) || isBlank(oldReferenceHex) || sameHex(current, oldReferenceHex)) {
                colors.put(key, newReferenceHex);
            }
        }
    }

    private void replaceLiveReferenceColor(java.util.Map<String, Color> colors, Color oldReferenceColor, Color newReferenceColor, String... keys) {
        for (String key : keys) {
            Color current = colors.get(key);
            if (current == null || oldReferenceColor == null || sameColor(current, oldReferenceColor)) {
                colors.put(key, newReferenceColor);
            }
        }
    }

    private boolean sameColor(Color left, Color right) {
        if (left == null || right == null) return false;
        return colorToHex(left).equalsIgnoreCase(colorToHex(right));
    }

    private boolean sameHex(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String colorToHex(Color color) {
        if (color == null) return "#00000000";
        return String.format("#%02X%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                (int) (color.getOpacity() * 255));
    }
}
