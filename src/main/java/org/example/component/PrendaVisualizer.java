package org.example.component;

import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;
import org.example.model.*;
import org.example.pattern.CompositeCommand;
import org.example.pattern.NodeMemento;
import org.example.component.helper.*;
import org.example.component.helper.VisualizerRenderOrchestrator;
import org.example.component.renderer.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Coordinated visualizer for garments.
 * Refactored to use modular Orchestrators (State, UI, Render).
 */
public class PrendaVisualizer extends StackPane {

    // Orchestrators
    private final VisualizerStateManager stateManager;
    private final VisualizerUIOrchestrator uiOrchestrator;
    private final VisualizerRenderOrchestrator renderOrchestrator;

    // Legacy/Service References (Maintained for API compatibility)
    private final Group contentGroup = new Group();
    private final Group userLayerGroup;
    private final Pane hotspotLayer = new Pane();
    private final Pane referenceLayer = new Pane();
    
    // Managers
    private final UserLayerManager layerManager;
    private final PrendaOverlayManager overlayManager;
    private final PrendaEditModeManager editManager;
    private final PrendaColorManager colorManager;
    private PrendaColorManager arqueroColorManager;
    private final PrendaLayerFactory layerFactory;
    private final PowerClipManager powerClipManager;
    private final GarmentClipboardService clipboardService;
    private final VisualizerSnapshotService snapshotService;
    private final GarmentAlignmentService alignmentService;
    private final GarmentNumberManager numberManager;
    private final GarmentColorReplacementService colorReplacementService;
    private final GarmentDesignSwapManager designSwapManager;
    private final PrendaHistoryManager historyManager;
    private final GarmentVisibilityManager visibilityManager;
    private final GarmentComponentService componentService;
    private final VisualizerUiController uiController;
    private final ShapeInteractionHelper shapeHelper;
    private org.example.component.helper.ActionContextRecorder actionContextRecorder;
    private ToggleButton btnToggleRefPoints; // UI compatibility
    private javafx.scene.layout.StackPane editModeContainer;

    private org.example.controller.uicomponent.ShapeManagerController shapeManagerController;
    private String lastCargarCapasSignature = "";
    private String lastColorSignature = "";
    private String lastNumberSignature = "";
    private String lastHotspotSignature = "";

    public PrendaVisualizer() {
        this.setMinSize(0, 0);
        this.setFocusTraversable(true);
        
        // 1. Initialize Orchestrators
        this.stateManager = new VisualizerStateManager();
        this.renderOrchestrator = new VisualizerRenderOrchestrator(contentGroup);
        this.uiOrchestrator = new VisualizerUIOrchestrator(contentGroup);
        this.uiOrchestrator.initializeViewportAndGuides();
        
        // 2. Initialize Layer Managers
        this.layerManager = new UserLayerManager();
        this.userLayerGroup = layerManager.getLayerGroup();
        this.contentGroup.getChildren().add(userLayerGroup);
        
        // 3. Initialize Core Managers
        this.overlayManager = new PrendaOverlayManager();
        this.visibilityManager = new GarmentVisibilityManager(this);
        this.componentService = new GarmentComponentService(this);
        this.powerClipManager = new PowerClipManager(this, layerManager, null);
        this.editManager = new PrendaEditModeManager(this, overlayManager);
        this.powerClipManager.setUIManager(editManager);

        this.colorManager = new PrendaColorManager(stateManager.getCamisetaState(), 
                renderOrchestrator.getShirtRenderer(), renderOrchestrator.getShortsRenderer(), renderOrchestrator.getSocksRenderer(),
                this::notifyStateChanged);

        this.historyManager = new PrendaHistoryManager();
        this.historyManager.setVisualizer(this);
        this.historyManager.setMaxHistory(50); // Increased from 8 to prevent data loss
        this.layerManager.setHistoryManager(historyManager);
        
        // Initialize the new ActionContextRecorder for consistent undo snapshots
        this.actionContextRecorder = new ActionContextRecorder(historyManager);

        this.layerFactory = new PrendaLayerFactory(layerManager, this,
                () -> stateManager.getActiveState().hasShorts() && stateManager.getActiveState().getCorteShort() != TipoCorte.PANTALONETA);
        this.clipboardService = new GarmentClipboardService(layerManager, layerFactory, historyManager, powerClipManager);
        this.snapshotService = new VisualizerSnapshotService(this, overlayManager, editManager, powerClipManager);
        this.alignmentService = new GarmentAlignmentService(this, layerManager, powerClipManager, historyManager, overlayManager, referenceLayer);
        this.numberManager = new GarmentNumberManager(this);
        this.colorReplacementService = new GarmentColorReplacementService(this);
        this.designSwapManager = new GarmentDesignSwapManager(this);
        this.uiController = new VisualizerUiController(this);
        this.shapeHelper = new ShapeInteractionHelper(this);

        // 4. Setup Input
        new org.example.component.input.GarmentInputHandler(this, layerManager, editManager, layerFactory);
        
        // 5. Build Final UI
        initVisualizerUI();
        
        // 6. Listeners
        layerManager.addAdditionListener(node -> {
            if (!stateManager.isNotificationsSuspended() && !stateManager.isEditandoArquero() && !stateManager.isTakingSnapshot()) {
                if (stateManager.getArqueroState().getUserLayers() == null || stateManager.getArqueroState().getUserLayers().isEmpty()) {
                    stateManager.sincronizarAtributosCompartidos(stateManager.getCamisetaState(), stateManager.getArqueroState());
                }
            }
        });

        layerManager.addSelectionListener(this::updateOverlayForSelection);
        
        cargarCapas();
    }

    private void initVisualizerUI() {
        contentGroup.setManaged(false);
        // CRITICAL FIX: Disable caching on contentGroup. 
        // Caching here was overwriting ViewportController settings and causing the "slicing" glitches.
        contentGroup.setCache(false);

        numberManager.addToGroup(contentGroup);
        contentGroup.getChildren().addAll(
                renderOrchestrator.getSocksRenderer().getDetailGroup(),
                renderOrchestrator.getShortsRenderer().getDetailGroup(),
                renderOrchestrator.getShirtRenderer().getDetailGroup(),
                overlayManager.getOverlayGroup(),
                hotspotLayer,
                referenceLayer
        );
        // HotspotLayer (user reference points) visible by default
        hotspotLayer.setVisible(true);
        hotspotLayer.setPickOnBounds(false);
        hotspotLayer.setMouseTransparent(false);
        referenceLayer.setVisible(false);
        referenceLayer.setPickOnBounds(false);
        referenceLayer.setMouseTransparent(false);

        getChildren().add(uiOrchestrator.getMainBorderPane());
        this.btnToggleRefPoints = uiController.getRefPointsButton();

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(this.widthProperty());
        clip.heightProperty().bind(this.heightProperty());
        this.setClip(clip);

        setViewportPanningEnabled(false);
        layerFactory.initDragDrop(this);
        
        getViewportController().addOnViewportChanged(() -> {
            layerManager.getSelectedNodes().forEach(node -> {
                if (node instanceof org.example.component.GraphicLayer) {
                    ((org.example.component.GraphicLayer) node).updateVisuals();
                }
            });
        });
        
        org.example.component.helper.MarqueeSelectionHandler marqueeHandler = new org.example.component.helper.MarqueeSelectionHandler(this);
        marqueeHandler.attach();
        
        // Ensure UI buttons are added LAST (on top of everything)
        uiController.attachTo(this);
    }

    public void cargarCapas() {
        if (stateManager.isNotificationsSuspended()) return;

        PrendaState state = stateManager.getActiveState();
        String signature = state.getGenero() + "|" + state.getCorte() + "|" + state.getLargo() + "|" + state.getCuello() + "|" + stateManager.isEditandoArquero() 
                + "|" + state.hasShirt() + "|" + state.hasShorts() + "|" + state.hasSocks()
                + "|" + state.hasMesh() + "|" + state.hasCuffs() + "|" + state.hasPadding() + "|" + state.hasShirtStripe()
                + "|" + state.hasShirtLinea()
                + "|" + state.hasShortsStripe() + "|" + state.hasShortsPicket() + "|" + state.hasShortsPocket() + "|" + state.hasShortsCuff()
                + "|" + state.hasShortsCord() + "|" + state.hasShortsLining() + "|" + state.hasShortsLinea()
                + "|" + state.hasSocksTop() + "|" + state.getCorteShort()
                + "|b:" + state.isChestBrandVisible() + state.getChestBrandPosition() 
                + "|" + state.isShortBrandVisible() + state.getShortBrandPosition()
                + "|" + state.isSocksBrandVisible();
        
        boolean structuralChange = !signature.equals(lastCargarCapasSignature);
        lastCargarCapasSignature = signature;

        if (structuralChange) {
            renderOrchestrator.updateLayers(state, stateManager.isEditandoArquero());
            applyVisibility();
            powerClipManager.extractOrphanContent();
            syncZoneBuckets();
            updateAllBranding();
            updateShortsCrest();
        }

        // --- SMART NUMBER SYNC ---
        // We only reload number VECTORS if the digit, visibility or anatomy (Gender/Cut) actually changes.
        // Adding decorative details like cuffs, mesh or stripes should NOT flicker the numbers.
        String numSig = state.getGenero() + "|" + state.getCorte() + "|" + state.getCorteShort() + "|" + stateManager.isEditandoArquero()
                + "|" + state.getCurrentChestNumber() + "|" + state.getCurrentBackNumber() + "|" + state.getCurrentShortNumber()
                + "|v:" + state.isChestNumberVisible() + "|" + state.isBackNumberVisible() + "|" + state.isShortNumberVisible();
        
        if (!numSig.equals(lastNumberSignature)) {
            numberManager.reloadAllNumbersAcrossDesigns(stateManager.getCamisetaState(), stateManager.getArqueroState());
            lastNumberSignature = numSig;
        }

        // Skip hotspot reload if the same state slot with identical hotspot data
        PrendaState hotspotState = stateManager.getActiveState();
        String hotspotSig = System.identityHashCode(hotspotState)
                + "|" + (hotspotState != null ? hotspotState.getReferenceHotspots().size() : 0);
        if (!hotspotSig.equals(lastHotspotSignature)) {
            refreshHotspotsFromState();
            lastHotspotSignature = hotspotSig;
        }
        reapplyColors();
        syncZoneBuckets();
        uiOrchestrator.autoScale();
        notifyStateChanged();
    }

    public Group getGroup() { return contentGroup; }
    public Group getContentGroup() { return contentGroup; }
    public Pane getHotspotLayer() { return hotspotLayer; }
    public Pane getReferenceLayer() { return referenceLayer; }
    public VisualizerRenderOrchestrator getRenderOrchestrator() { return renderOrchestrator; }
    public PrendaLayerFactory getLayerFactory() { return layerFactory; }
    public PrendaOverlayManager getOverlayManager() { return overlayManager; }

    public List<String> getAvailableZones() {
        List<String> zones = new ArrayList<>();
        if (stateManager.getActiveState().hasShirt()) {
            zones.add("PECHO");
            zones.add("ESPALDA");
            zones.add("MANGA_DELANTERA");
            zones.add("MANGA_TRASERA");
        }
        if (stateManager.getActiveState().hasShorts()) {
            zones.add("SHORT_FRONT");
            zones.add("SHORT_BACK");
        }
        return zones;
    }

    public void applySmartPowerClip(Node layer, String zone, boolean center) {
        if (layer == null) {
            return;
        }

        String oldZone = getLayerActiveZone(layer);
        if (java.util.Objects.equals(oldZone, zone) && !center) {
            return;
        }

        org.example.pattern.PowerClipCommand cmd = new org.example.pattern.PowerClipCommand(
                this, layer, oldZone, zone, center);
        cmd.execute();
        if (historyManager != null) {
            historyManager.addCommand(cmd);
        }
        notifyStateChanged();
    }

    private String getLayerActiveZone(Node layer) {
        if (layer instanceof ShapeLayer) return ((ShapeLayer) layer).getActiveZone();
        if (layer instanceof ImageLayer) return ((ImageLayer) layer).getActiveZone();
        if (layer instanceof TextLayer) return ((TextLayer) layer).getActiveZone();
        if (layer instanceof GroupLayer) return ((GroupLayer) layer).getActiveZone();
        if (layer instanceof GroupLayerV2) return ((GroupLayerV2) layer).getActiveZone();
        return null;
    }

    public void applyInternalPowerClip(Node layer, Node target) {
        if (target instanceof ShapeLayer) {
            ((ShapeLayer) target).insertIntoClip(layer);
        }
    }

    public VisualizerStateManager getStateManager() { return stateManager; }
    public VisualizerUIOrchestrator getUiOrchestrator() { return uiOrchestrator; }


    public void setNotificationsSuspended(boolean b) { 
        stateManager.setNotificationsSuspended(b); 
        if (!b) cargarCapas();
    }
    public void notifyStateChanged() { stateManager.notifyStateChanged(); }
    public void addOnStateChanged(Runnable r) { stateManager.addOnStateChanged(r); }
    public Runnable getOnStateChanged() { return stateManager.getOnStateChanged(); }
    public void setOnStateChanged(Runnable r) { stateManager.setOnStateChanged(r); }
    public boolean isPendingNotification() { return stateManager.isPendingNotification(); }

    // --- Bridge: State & Manager Accessors (CRITICAL FOR STATEMAPPER) ---
    public PrendaState getState() { return stateManager.getActiveState(); }
    public PrendaState getCamisetaState() { return stateManager.getCamisetaState(); }
    public PrendaState getArqueroState() { return stateManager.getArqueroState(); }
    public PrendaColorManager getColorManager() { return stateManager.isEditandoArquero() ? arqueroColorManager : colorManager; }
    public PrendaColorManager getCamisetaColorManager() { return colorManager; }
    
    public PrendaColorManager getArqueroColorManager() { 
        ensureArqueroInitialized();
        return arqueroColorManager; 
    }
    
    public void ensureArqueroInitialized() {
        if (arqueroColorManager == null) {
            renderOrchestrator.ensureArqueroInitialized();
            arqueroColorManager = new PrendaColorManager(stateManager.getArqueroState(), 
                    (BaseGarmentRenderer)renderOrchestrator.getActiveShirtRenderer(true), 
                    renderOrchestrator.getActiveShortsRenderer(true), 
                    renderOrchestrator.getActiveSocksRenderer(true), this::notifyStateChanged);
        }
    }

    public UserLayerManager getLayerManager() { return layerManager; }
    public PowerClipManager getPowerClipManager() { return powerClipManager; }
    public GarmentNumberManager getNumberManager() { return numberManager; }
    public PrendaHistoryManager getHistoryManager() { return historyManager; }
    public org.example.component.helper.ActionContextRecorder getActionContextRecorder() { return actionContextRecorder; }
    public PrendaEditModeManager getEditManager() { return editManager; }
    public Group getUserLayerGroup() { return layerManager.getLayerGroup(); }

    public boolean isEditandoArquero() { return stateManager.isEditandoArquero(); }
    public void setEditandoArquero(boolean b) { 
        stateManager.setEditandoArquero(b); 
        ensureArqueroInitialized();
        cargarCapas();
        if (uiController != null) {
            uiController.updateModeIndicatorVisuals(b, null);
        }
    }
    public void setActiveDesign(boolean b) { setEditandoArquero(b); }
    public boolean isArqueroDisenoPersonalizado() { return stateManager.isArqueroDisenoPersonalizado(); }
    public void setArqueroDisenoPersonalizado(boolean v) { stateManager.setArqueroDisenoPersonalizado(v); }

    // --- UI Delegation ---
    public ViewportController getViewportController() { return uiOrchestrator.getViewportController(); }
    public void setViewportPanningEnabled(boolean b) {
        uiOrchestrator.getViewportController().setPanningEnabled(b);
        if (b) {
            // Clear selection when panning starts so no layer remains
            // visually selected while mouse interaction is disabled
            clearGlobalSelection();
        }
    }
    public void resetView() { uiOrchestrator.getViewportController().resetView(); }
    public void setRulersVisible(boolean v) {
        uiOrchestrator.getHorizontalRuler().setVisible(v);
        uiOrchestrator.getVerticalRuler().setVisible(v);
        uiOrchestrator.getDesignAreaStack().getChildren().stream().filter(n -> n instanceof Pane).forEach(n -> n.setVisible(v));
    }

    // --- Render Delegation ---
    public void applyVisibility() { renderOrchestrator.applyVisibility(stateManager.getActiveState(), stateManager.isEditandoArquero()); }
    public void updateAllBranding() { renderOrchestrator.updateAllBranding(stateManager.getActiveState(), stateManager.isEditandoArquero()); }
    public void setRenderersOpacity(double o) { renderOrchestrator.setRenderersOpacity(o); }
    public BaseGarmentRenderer getActiveShirtRenderer() { return renderOrchestrator.getActiveShirtRenderer(stateManager.isEditandoArquero()); }
    public ShortsRenderer getActiveShortsRenderer() { return renderOrchestrator.getActiveShortsRenderer(stateManager.isEditandoArquero()); }
    public SocksRenderer getActiveSocksRenderer() { return renderOrchestrator.getActiveSocksRenderer(stateManager.isEditandoArquero()); }
    public ShirtRenderer getShirtRenderer() { return renderOrchestrator.getShirtRenderer(); }
    public ShortsRenderer getShortsRenderer() { return renderOrchestrator.getShortsRenderer(); }
    public SocksRenderer getSocksRenderer() { return renderOrchestrator.getSocksRenderer(); }
    public ArqueroRenderer getArqueroShirtRenderer() { return renderOrchestrator.getArqueroShirtRenderer(); }
    public ShortsRenderer getArqueroShortsRenderer() { return renderOrchestrator.getArqueroShortsRenderer(); }
    public SocksRenderer getArqueroSocksRenderer() { return renderOrchestrator.getArqueroSocksRenderer(); }

    // --- Bridge: UI & Global Delegation ---
    public VisualizerUiController getUiController() { return uiController; }
    public VisualizerUiController getVisualizerUiController() { return uiController; }
    public ToggleButton getBtnLockBg() { return uiController.getLockBgButton(); }
    public void addUIUpdateListener(Runnable r) { addOnStateChanged(r); }
    public void notifyUIUpdates() { notifyStateChanged(); }
    public void reloadActiveNumbers() { numberManager.reloadActiveNumbers(stateManager.getActiveState(), stateManager.isEditandoArquero()); }

    // --- Legacy Bridge: Attributes (StateManager/State) ---
    public TipoGenero getGenero() { return stateManager.getActiveState().getGenero(); }
    public TipoCorte getCurrentCorteShort() { return stateManager.getActiveState().getCorteShort(); }
    public void setGenero(TipoGenero g) { stateManager.getActiveState().setGenero(g); cargarCapas(); }
    public void setCorte(TipoCorte c) { stateManager.getActiveState().setCorte(c); cargarCapas(); }
    public void setLargo(TipoLargo l) { stateManager.getActiveState().setLargo(l); cargarCapas(); }
    public void setCuello(TipoCuello c) { stateManager.getActiveState().setCuello(c); cargarCapas(); }
    public void setTela(TipoTela t) { stateManager.getActiveState().setTela(t); cargarCapas(); }
    public void setPunos(boolean b) { stateManager.getActiveState().setHasCuffs(b); cargarCapas(); }
    public void setMalla(boolean b) { stateManager.getActiveState().setHasMesh(b); cargarCapas(); }
    public void setShirtStripe(boolean b) { stateManager.getActiveState().setHasShirtStripe(b); cargarCapas(); }
    public void setShirtLinea(boolean b) { stateManager.getActiveState().setHasShirtLinea(b); cargarCapas(); }
    public void setPadding(boolean b) { stateManager.getActiveState().setHasPadding(b); cargarCapas(); }
    
    // --- Internal Service Bridge ---
    public boolean isNotificationsSuspended() { return stateManager.isNotificationsSuspended(); }
    public void invalidateCargarCapasSignature() { this.lastCargarCapasSignature = ""; }
    public void invalidateSignatures() {
        this.lastCargarCapasSignature = "";
        this.lastNumberSignature = "";
        if (numberManager != null) {
            numberManager.clearNodeSignatures();
        }
    }
    public void flushUIStateToDataModel() { stateManager.flushUIStateToDataModel(); }
    public void setEditModeVisuals(boolean v) { 
        uiController.setEditModeVisuals(v); 
        updateEditModeContainerStyle(v);
    }
    
    public void setEditModeContainer(javafx.scene.layout.StackPane container) {
        this.editModeContainer = container;
    }
    
    private void updateEditModeContainerStyle(boolean editMode) {
        if (editModeContainer != null) {
            if (editMode) {
                // Se oscurece ligeramente el fondo para que las prendas blancas resalten, sin usar el borde azul grueso
                editModeContainer.setStyle("-fx-background-color: #d1d8e0; -fx-border-color: #bdc3c7; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 2; -fx-effect: innershadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 0);");
            } else {
                editModeContainer.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 2; -fx-effect: none;");
            }
        }
    }
    public void setToolsVisible(boolean v) { 
        setRulersVisible(v); 
        if (v) {
            boolean isEditing = (editManager != null && editManager.getCurrentEditingLayer() != null) || (powerClipManager != null && powerClipManager.isEditing());
            uiController.setEditOverlayVisible(isEditing);
        } else {
            uiController.setEditOverlayVisible(false);
        }
    }
    public void beginAtomicSnapshotMode() { stateManager.beginAtomicSnapshotMode(); }
    public void restoreAtomicSnapshotMode(boolean s, boolean p) { stateManager.restoreAtomicSnapshotMode(s, p); }
    public boolean isTakingSnapshot() { return stateManager.isTakingSnapshot(); }
    public boolean isSwappingDesign() { return stateManager.isSwappingDesign(); }
    public void setSwappingDesign(boolean b) { stateManager.setSwappingDesign(b); }
    public ShapeInteractionHelper getShapeHelper() { return shapeHelper; }
    public Group getContourGroup() { return uiOrchestrator.getContourGroup(); }
    public Group getHandlesGroup() { return uiOrchestrator.getHandlesGroup(); }
    public Pane getGuideLayer() { return uiOrchestrator.getGuideLayer(); }
    public boolean hasShirt() { return stateManager.getActiveState().hasShirt(); }
    public boolean hasShorts() { return stateManager.getActiveState().hasShorts(); }
    public boolean hasSocks() { return stateManager.getActiveState().hasSocks(); }
    public void startSnapshotSession() { snapshotService.startSnapshotSession(); }
    public void endSnapshotSession() { snapshotService.endSnapshotSession(); }
    public WritableImage getCachedSnapshot() { return snapshotService.getCachedSnapshot(); }
    public Color pickColor(double x, double y) { return snapshotService.pickColor(x, y); }
    public Color pickDataColor(double x, double y) { return snapshotService.pickDataColor(x, y); }
    public void setStateDirectly(PrendaState s) { stateManager.setEditandoArquero(s == stateManager.getArqueroState()); }
    public UserLayerManager getUserLayerManager() { return layerManager; }

    public boolean hasSleeves() { return stateManager.getActiveState().hasSleeves(); }
    public boolean hasCuffs() { return stateManager.getActiveState().hasCuffs(); }
    public boolean hasMesh() { return stateManager.getActiveState().hasMesh(); }
    public boolean hasShirtStripe() { return stateManager.getActiveState().hasShirtStripe(); }
    public boolean hasShirtLinea() { return stateManager.getActiveState().hasShirtLinea(); }
    public boolean hasShortsStripe() { return stateManager.getActiveState().hasShortsStripe(); }
    public boolean hasShortsLinea() { return stateManager.getActiveState().hasShortsLinea(); }
    public boolean hasShortsPicket() { return stateManager.getActiveState().hasShortsPicket(); }
    public boolean hasShortsCuff() { return stateManager.getActiveState().hasShortsCuff(); }
    public boolean hasSocksTop() { return stateManager.getActiveState().hasSocksTop(); }

    public void setShorts(boolean b) { stateManager.getActiveState().setHasShorts(b); cargarCapas(); }
    public void setShortsStripe(boolean b) { stateManager.getActiveState().setHasShortsStripe(b); cargarCapas(); }
    public void setShortsLinea(boolean b) { stateManager.getActiveState().setHasShortsLinea(b); cargarCapas(); }
    public void setShortsPicket(boolean b) { stateManager.getActiveState().setHasShortsPicket(b); cargarCapas(); }
    public void setShortsPocket(boolean b) { stateManager.getActiveState().setHasShortsPocket(b); cargarCapas(); }
    public void setShortsCuff(boolean b) { stateManager.getActiveState().setHasShortsCuff(b); cargarCapas(); }
    public void setShortsCord(boolean b) { stateManager.getActiveState().setHasShortsCord(b); cargarCapas(); }

    public void setMedias(boolean b) { stateManager.getActiveState().setHasSocks(b); cargarCapas(); }
    public void setSocksTop(Boolean b) { stateManager.getActiveState().setHasSocksTop(b != null && b); cargarCapas(); }
    public void setTipoMedias(TipoMedias t) { stateManager.getActiveState().setTipoMedias(t); cargarCapas(); }
    
    // Branding & Crest
    public boolean isChestBrandVisible() { return stateManager.getActiveState().isChestBrandVisible(); }
    public boolean isShortBrandVisible() { return stateManager.getActiveState().isShortBrandVisible(); }
    public boolean isSocksBrandVisible() { return stateManager.getActiveState().isSocksBrandVisible(); }
    public String getBrandTech() { return stateManager.getActiveState().getBrandTech(); }
    public void setBrandTech(String tech) { stateManager.getActiveState().setBrandTech(tech); cargarCapas(); }
    public boolean isShortCrestVisible() { return stateManager.getActiveState().isShortCrestVisible(); }
    public void setShortCrestVisible(boolean v) { 
        stateManager.getActiveState().setHasShortsCrest(v); 
        reloadActiveNumbers();
        cargarCapas(); 
    }
    public void setSocksBrandVisible(boolean v) { stateManager.getActiveState().setHasSocksBrand(v); cargarCapas(); }

    // Number Management Bridge
    public void setChestNumberVisible(boolean v) {
        stateManager.getActiveState().setChestNumberVisible(v);
        // If enabling visibility but no digit set yet, default to "9" so vector loads immediately
        if (v && numberManager.hasNumberDigit(stateManager.getActiveState().getCurrentChestNumber()) == false) {
            numberManager.setGlobalNumberDigit("9", stateManager.getActiveState(),
                stateManager.getActiveState().hasShorts(), stateManager.isEditandoArquero());
            numberManager.clearNodeSignatures();
        }
        cargarCapas();
    }
    public void setBackNumberVisible(boolean v) {
        stateManager.getActiveState().setBackNumberVisible(v);
        if (v && numberManager.hasNumberDigit(stateManager.getActiveState().getCurrentBackNumber()) == false) {
            numberManager.setGlobalNumberDigit("9", stateManager.getActiveState(),
                stateManager.getActiveState().hasShorts(), stateManager.isEditandoArquero());
            numberManager.clearNodeSignatures();
        }
        cargarCapas();
    }
    public void setShortNumberVisible(boolean v) {
        stateManager.getActiveState().setShortNumberVisible(v);
        if (v && numberManager.hasNumberDigit(stateManager.getActiveState().getCurrentShortNumber()) == false) {
            numberManager.setGlobalNumberDigit("9", stateManager.getActiveState(),
                stateManager.getActiveState().hasShorts(), stateManager.isEditandoArquero());
            numberManager.clearNodeSignatures();
        }
        cargarCapas();
    }
    public String getCurrentChestNumberStr() { return stateManager.getActiveState().getCurrentChestNumber(); }
    public String getCurrentBackNumberStr() { return stateManager.getActiveState().getCurrentBackNumber(); }
    public String getCurrentShortNumberStr() { return stateManager.getActiveState().getCurrentShortNumber(); }
    public boolean isChestNumberVisible() { return stateManager.getActiveState().isChestNumberVisible(); }
    public boolean isBackNumberVisible() { return stateManager.getActiveState().isBackNumberVisible(); }
    public boolean isShortNumberVisible() { return stateManager.getActiveState().isShortNumberVisible(); }
    public NumberComposition getChestNumber() { return numberManager.getChestNumber(stateManager.isEditandoArquero()); }
    public NumberComposition getBackNumber() { return numberManager.getBackNumber(stateManager.isEditandoArquero()); }
    public NumberComposition getShortNumber() { return numberManager.getShortNumber(stateManager.isEditandoArquero()); }
    
    public void setGlobalNumberDigit(String digit) {
        numberManager.setGlobalNumberDigit(digit, stateManager.getActiveState(), 
            stateManager.getActiveState().hasShorts(), stateManager.isEditandoArquero());
        if (stateManager.getArqueroState() != null) {
             numberManager.setGlobalNumberDigit(digit, stateManager.getArqueroState(), 
                stateManager.getArqueroState().hasShorts(), true);
        }
        cargarCapas();
    }
    
    public void resetNumberToFactory(NumberComposition nc) { alignmentService.resetNumberToFactory(nc); }
    public void alignSelected(String direction) { alignmentService.alignSelected(direction); }
    
    // Short Configuration Bridge
    public void setShortsCorte(TipoCorte c) { stateManager.getActiveState().setCorteShort(c); cargarCapas(); }
    public void setShortsLining(boolean v) { stateManager.getActiveState().setHasShortsLining(v); cargarCapas(); }
    
    public void setChestBrandColor(Color c, boolean notify) { 
        getColorManager().setChestBrandColor(c, notify);
        stateManager.getActiveState().setChestBrandColor(org.example.utils.UIFactory.toHexString(c));
    }
    public void setShortBrandColor(Color c, boolean notify) { 
        getColorManager().setShortBrandColor(c, notify);
        stateManager.getActiveState().setShortBrandColor(org.example.utils.UIFactory.toHexString(c));
    }
    public void setSocksBrandColor(Color c, boolean notify) { 
        getColorManager().setSocksBrandColor(c, notify);
        stateManager.getActiveState().setSocksBrandColor(org.example.utils.UIFactory.toHexString(c));
    }
    
    public void loadBrandVector(String zone, String filename) {
        boolean visible = filename != null && !filename.isEmpty();
        if ("pecho".equalsIgnoreCase(zone)) {
            stateManager.getActiveState().setChestBrandPosition(filename != null ? filename : "");
            stateManager.getActiveState().setChestBrandVisible(visible);
        } else if ("short".equalsIgnoreCase(zone)) {
            stateManager.getActiveState().setShortBrandPosition(filename != null ? filename : "");
            stateManager.getActiveState().setShortBrandVisible(visible);
        }
        // CRITICAL: updateAllBranding() is inside the structuralChange guard in cargarCapas().
        // Brand visibility changes are NOT structural, so we must call it directly here.
        updateAllBranding();
        cargarCapas();
    }
    
    public void resetState() { 
        colorManager.resetColors(); 
        if (arqueroColorManager != null) arqueroColorManager.resetColors(); 

        // Limpiar el "buffer" de personalizaciones sueltas
        if (numberManager != null) numberManager.clearAll();
        if (layerManager != null) layerManager.clearAll();
        if (powerClipManager != null) powerClipManager.reset();

        if (stateManager != null && stateManager.getActiveState() != null) {
            stateManager.getActiveState().resetPersonalizationVisibility();
            stateManager.getActiveState().resetNumberTransforms();
        }

        cargarCapas(); 
    }
    public void resetColors() { colorManager.resetColors(); }
    public void updateActiveLayers(String zone) { cargarCapas(); }

    // Colors Delegate
    public void setColorBase(Color c) { getColorManager().setColorBase(c); }
    public void setSleevesColor(Color c) { getColorManager().setSleevesColor(c); }
    public void setCollarColor(Color c) { getColorManager().setCollarColor(c); }
    public void setCuffColor(Color c) { setPunos(true); getColorManager().setCuffColor(c); }
    public void setMeshColor(Color c) { setMalla(true); getColorManager().setMeshColor(c); }
    public void setShirtStripeColor(Color c) { setShirtStripe(true); getColorManager().setShirtStripeColor(c, true); }
    public void setShirtLineaColor(Color c) { setShirtLinea(true); getColorManager().setShirtLineaColor(c, true); }
    public void setShortsColor(Color c) { getColorManager().setShortsColor(c); }
    public void setShortsStripeColor(Color c) { setShortsStripe(true); getColorManager().setShortsStripeColor(c); }
    public void setShortsLineaColor(Color c) { setShortsLinea(true); getColorManager().setShortsLineaColor(c, true); }
    public void setShortsPicketColor(Color c) { setShortsPicket(true); getColorManager().setShortsPicketColor(c); }
    public void setShortsCuffColor(Color c) { setShortsCuff(true); getColorManager().setShortsCuffColor(c); }
    public void setSocksBaseColor(Color c) { getColorManager().setSocksBaseColor(c); }
    public void setSocksTopColor(Color c) { setSocksTop(true); getColorManager().setSocksTopColor(c); }
    public void setSocksDetailColor(Color c) { getColorManager().setSocksDetailColor(c); }
    public void setShortsCordColor(Color c) { getColorManager().setShortsCordColor(c); }
    public int replaceAllColors(Color src, Color target, double tolerance) { return colorReplacementService.replaceAllColors(src, target, tolerance); }
    
    public Color getPartColor(String part, Color def) { return getColorManager().getPartColor(part, def); }
    public String getInternalCode(String part) { return getColorManager().getInternalCode(part); }
    public void setInternalCode(String part, String code) { getColorManager().setInternalCode(part, code); }
    public void clearPreviewColors() { getColorManager().clearPreviewColors(); }
    public void setPreviewColor(String part, Color c) { getColorManager().setPreviewColor(part, c); }

    // --- Bridge: Layer Management (CRITICAL FOR TEXTMANAGER & CLIPBOARD) ---
    public Node getSelectedNode() { return layerManager.getSelectedNode(); }
    public void copySelectedLayer() { clipboardService.copySelectedLayer(); }
    public void cutSelectedLayer() { clipboardService.cutSelectedLayer(); }
    public void pasteLayer() { clipboardService.pasteLayer(); }
    public void deleteSelectedLayer() { clipboardService.deleteSelectedLayer(); }
    public void deselectAllNames() { layerManager.clearSelection(); }
    public void clearGlobalSelection() { layerManager.clearSelection(); }
    public void addTextLayer(TextLayer layer) { layerFactory.addUserLayer(layer); }
    public void removeTextLayer(TextLayer layer) { layerManager.removeLayer(layer); notifyStateChanged(); }
    public void clearTextLayers() { 
        List<Node> toRemove = new ArrayList<>();
        layerManager.getLayers().forEach(n -> { if (n instanceof TextLayer) toRemove.add(n); });
        toRemove.forEach(layerManager::removeLayer);
        notifyStateChanged();
    }
    public void addImageLayer(ImageLayer layer) { layerFactory.addLayer(layer); }
    public void addUserLayer(Node n) { layerFactory.addUserLayer(n); }
    public void addShapeLayer(ShapeLayer l) { layerFactory.addShapeLayer(l); }
    public void addShapeLayerToContainer(ShapeLayer l, Group container, int insertionIndex, boolean autoSelect) {
        layerFactory.addShapeLayerToContainer(l, container, insertionIndex, autoSelect);
    }
    public void removeUserLayer(Node n) { layerManager.removeLayer(n); notifyStateChanged(); }
    public void clearUserLayers() { layerManager.clearAll(); powerClipManager.reset(); notifyStateChanged(); }
    public void addSelectionListener(java.util.function.Consumer<Node> listener) { layerManager.addSelectionListener(listener); }
    public void setUserLockedOnSelected(boolean locked) {
        layerManager.getSelectedNodes().forEach(node -> {
             if (node instanceof ShapeLayer) ((ShapeLayer) node).setUserLocked(locked);
             else if (node instanceof ImageLayer) ((ImageLayer) node).setUserLocked(locked);
             else if (node instanceof TextLayer) ((TextLayer) node).setUserLocked(locked);
        });
    }

    // --- Legacy Bridge: Design Tools (Alignment/Snapshot/Etc) ---
    public void clearHotspots() { 
        alignmentService.clearHotspots(); 
        hotspotLayer.getChildren().clear();
    }
    public void removeHotspot(Node h) { 
        alignmentService.removeHotspot(h);
        hotspotLayer.getChildren().remove(h);
    }
    public Node getActiveReferenceAnchor() { return alignmentService.getActiveReferenceAnchor(); }
    public void setAlignmentActiveAnchor(Node anchor) { alignmentService.setActiveReferenceAnchor(anchor); }
    public void toggleBackgroundLock() { if (uiController.getLockBgButton() != null) uiController.getLockBgButton().setSelected(!uiController.getLockBgButton().isSelected()); }
    public void cancelShapeCreation() { shapeHelper.cancelShapeCreation(); }
    public void startShapeCreationInternal(org.example.model.ShapeType type, Color fill, Color stroke, double width, java.util.function.Consumer<ShapeLayer> onFinish) {
        shapeHelper.startShapeCreation(type, fill, stroke, width, onFinish);
    }
    public WritableImage takeSafeSnapshot(boolean forArquero) { return snapshotService.takeSafeSnapshot(forArquero, 2.0); }
    public WritableImage takeSafeSnapshot(boolean forArquero, double scale) { return snapshotService.takeSafeSnapshot(forArquero, scale); }

    public org.example.controller.uicomponent.ShapeManagerController getShapeManagerController() { return shapeManagerController; }
    private org.example.component.helper.DrawingToolContext drawingToolContext;
    public void setDrawingToolContext(org.example.component.helper.DrawingToolContext ctx) { this.drawingToolContext = ctx; }
    public org.example.component.helper.DrawingToolContext getDrawingToolContext() { return drawingToolContext; }
    public NumberComposition getNumberCompositionFromRoot(Node n) { return numberManager.getNumberCompositionFromRoot(n); }
    public boolean isNumberRoot(Node n) { return numberManager.isNumberRoot(n); }
    public ToggleButton getBtnToggleRefPoints() { return btnToggleRefPoints; }
    public void setBtnToggleRefPoints(ToggleButton b) { this.btnToggleRefPoints = b; }

    public void selectLayer(Node n) { layerManager.selectNode(n); }
    public void addRemovalListener(java.util.function.Consumer<Node> l) { layerManager.addRemovalListener(l); }
    
    public void enterEditMode(Node n) { 
        if (n instanceof ImageLayer) editManager.enterEditMode((ImageLayer)n);
        else if (n instanceof ShapeLayer) editManager.enterEditMode((ShapeLayer)n);
    }
    public void enterEditMode(String zone) { powerClipManager.enterEditMode(zone); }
    public void finishEditMode() { powerClipManager.finishEditMode(); }
    public void finishPowerClipEdit() { powerClipManager.finishEditMode(); }
    
    public void updateOverlayForZone(String zone) {
        overlayManager.updateOverlayForZone(zone, 
            stateManager.getActiveState().hasShirt(), 
            stateManager.getActiveState().hasShorts(), 
            stateManager.getActiveState().getCorteShort(),
            (org.example.component.renderer.GarmentRenderer)renderOrchestrator.getActiveShirtRenderer(stateManager.isEditandoArquero()),
            renderOrchestrator.getActiveShortsRenderer(stateManager.isEditandoArquero()));
    }
    
    public void updateOverlayForShapeDrag(ShapeLayer l) {
        updateOverlayForZone(l.getActiveZone());
    }
    
    public void handleShapeDragStart(ShapeLayer l) { 
        l.recordUndoState();
        // Only store if not already stored (avoid overwriting if called multiple times)
        if (!dragStartMementos.containsKey(l)) {
            dragStartMementos.put(l, new NodeMemento(l));
        }
    }
    public void handleShapeDragEnd(ShapeLayer l) {}

    private final java.util.Map<javafx.scene.Node, org.example.pattern.NodeMemento> dragStartMementos = new java.util.IdentityHashMap<>();

    public void handleShapeDragRelease(ShapeLayer l) {
        if (l.isGrouped()) {
            return;
        }
        
        // Try to get the start memento (from handleShapeDragStart, which may not be called)
        // If not found, this is probably a drag that bypassed the normal flow
        NodeMemento before = dragStartMementos.remove(l);
        NodeMemento after = new NodeMemento(l);
        
        // If no before memento was captured (handleShapeDragStart wasn't called),
        // use the current position as both before and after (no-op for undo)
        if (before == null) {
            return; // Cannot do proper undo without before state
        }
        
        if (before.getTx() != after.getTx() || before.getTy() != after.getTy()) {
            historyManager.addCommand(new org.example.pattern.TransformCommand(l, before, after, l.getActiveZone()));
        }
    }

    // --- Hotspot & Reference Management ---
    public void updateReferencePoints() { 
        refreshHotspotsFromState(); 
        alignmentService.updateReferencePoints();
    }

    public void addHotspot(HotspotLayer hl) { 
        hotspotLayer.getChildren().add(hl); 
        hotspotLayer.setVisible(true);
    }
    public void cargarEstadoDesdeCero(PrendaState s) { cargarCapas(); }
    public void refreshHotspotsFromState() {
        hotspotLayer.getChildren().clear();
        for (PrendaState.ReferenceHotspot hs : stateManager.getActiveState().getReferenceHotspots()) {
            hotspotLayer.getChildren().add(new HotspotLayer(hs, this::notifyStateChanged, this));
        }
        hotspotLayer.setVisible(true);
    }

    // --- Color Management ---
    public void reapplyColors() {
        getColorManager().reapplyColors();
        numberManager.reapplyNumberColors(stateManager.getCamisetaState(), stateManager.getArqueroState());
    }

    // Overlays
    private void updateOverlayForSelection(Object node) {
        if (editManager.isEditing()) return;
        overlayManager.clearOverlay();
        if (node instanceof ImageLayer) {
            overlayManager.updateOverlayForZone(((ImageLayer)node).getActiveZone(), true, true, TipoCorte.CUADRADO, 
                renderOrchestrator.getActiveShirtRenderer(stateManager.isEditandoArquero()), 
                renderOrchestrator.getActiveShortsRenderer(stateManager.isEditandoArquero()));
        }
    }

    public void syncZoneBuckets() {
        updateSleeveLogic();
        for (String zone : List.of("PECHO", "ESPALDA", "MANGA_DELANTERA", "MANGA_TRASERA", "SHORT_FRONT", "SHORT_BACK")) {
            powerClipManager.refreshZoneClip(zone);
        }
    }

    // Legacy compatibility methods
    public void updateSleeveLogic() { componentService.updateSleeveLogic(); }
    public void setShapeManagerController(org.example.controller.uicomponent.ShapeManagerController c) { this.shapeManagerController = c; }
    public void updateShortsCrest() {
        ShortsRenderer r = renderOrchestrator.getActiveShortsRenderer(stateManager.isEditandoArquero());
        if (stateManager.getActiveState().isShortCrestVisible()) {
            r.setCrest(org.example.utils.SVGCache.loadOptionalPath(org.example.logic.GarmentAssetManager.getShortsCrestPath(stateManager.getActiveState().getGenero(), stateManager.getActiveState().getCorteShort())));
        } else r.setCrest("");
    }
    
    public void sincronizarAtributosCompartidos(PrendaState source, PrendaState target) {
        stateManager.sincronizarAtributosCompartidos(source, target);
    }
    
    public String getChestNumberValue() { return stateManager.getActiveState().getCurrentChestNumber(); }
    public String getBackNumberValue() { return stateManager.getActiveState().getCurrentBackNumber(); }
    public String getShortNumberValue() { return stateManager.getActiveState().getCurrentShortNumber(); }

    // --- Dimension API ---
    public double getVisualizerWidth() {
        double w = uiOrchestrator.getDesignAreaStack().getWidth();
        return (w > 0) ? w : 800; // Fallback to 800 if not laid out
    }

    public double getVisualizerHeight() {
        double h = uiOrchestrator.getDesignAreaStack().getHeight();
        return (h > 0) ? h : 1000; // Fallback to 1000 if not laid out
    }
}
