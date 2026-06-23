package org.example.component.helper;

import org.example.model.PrendaState;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the state synchronization and lifecycle for PrendaVisualizer.
 * Centralizes Field Player vs Goalkeeper state transitions.
 */
public class VisualizerStateManager {

    private final PrendaState camisetaState = new PrendaState();
    private final PrendaState arqueroState = new PrendaState();
    private PrendaState activeState;

    private boolean editandoArquero = false;
    private boolean arqueroDisenoPersonalizado = false;
    private boolean takingSnapshot = false;
    private boolean swappingDesign = false;

    private boolean notificationsSuspended = false;
    private boolean pendingNotification = false;

    private final List<Runnable> stateListeners = new ArrayList<>();
    private Runnable onStateChangedCallback;

    public VisualizerStateManager() {
        this.activeState = camisetaState;
    }

    public PrendaState getActiveState() {
        return activeState;
    }

    public PrendaState getCamisetaState() {
        return camisetaState;
    }

    public PrendaState getArqueroState() {
        return arqueroState;
    }

    public boolean isEditandoArquero() {
        return editandoArquero;
    }

    public void setEditandoArquero(boolean editando) {
        this.editandoArquero = editando;
        this.activeState = editando ? arqueroState : camisetaState;
    }

    public boolean isArqueroDisenoPersonalizado() {
        return arqueroDisenoPersonalizado;
    }

    public void setArqueroDisenoPersonalizado(boolean v) {
        this.arqueroDisenoPersonalizado = v;
    }

    public boolean isTakingSnapshot() {
        return takingSnapshot;
    }

    public void setTakingSnapshot(boolean taking) {
        this.takingSnapshot = taking;
    }

    public boolean isSwappingDesign() {
        return swappingDesign;
    }

    public void setSwappingDesign(boolean swapping) {
        this.swappingDesign = swapping;
    }

    public boolean isNotificationsSuspended() {
        return notificationsSuspended;
    }

    public void setNotificationsSuspended(boolean suspended) {
        this.notificationsSuspended = suspended;
    }

    public boolean isPendingNotification() {
        return pendingNotification;
    }

    public void setPendingNotification(boolean pending) {
        this.pendingNotification = pending;
    }

    public void addOnStateChanged(Runnable listener) {
        this.stateListeners.add(listener);
    }

    public Runnable getOnStateChanged() {
        return onStateChangedCallback;
    }

    public void setOnStateChanged(Runnable callback) {
        this.onStateChangedCallback = callback;
    }

    public void notifyStateChanged() {
        if (notificationsSuspended) {
            pendingNotification = true;
            return;
        }

        for (Runnable r : stateListeners) {
            try { r.run(); } catch (Exception ignored) {}
        }

        if (onStateChangedCallback != null) {
            javafx.application.Platform.runLater(onStateChangedCallback);
        }
    }

    public void beginAtomicSnapshotMode() {
        this.takingSnapshot = true;
        this.notificationsSuspended = true;
        this.pendingNotification = false;
    }

    public void restoreAtomicSnapshotMode(boolean prevSuspended, boolean prevPending) {
        this.takingSnapshot = false;
        this.pendingNotification = prevPending;
        this.notificationsSuspended = prevSuspended;
    }

    /**
     * Synchronizes shared attributes from player to goalkeeper if it's not custom yet.
     */
    public void sincronizarAtributosCompartidos(PrendaState source, PrendaState target) {
        if (source == null || target == null || arqueroDisenoPersonalizado) return;
        
        // Only sync if target is "clean" or if we force it
        boolean targetHasCustomLayers = target.getUserLayers() != null && !target.getUserLayers().isEmpty();
        if (targetHasCustomLayers && !arqueroDisenoPersonalizado) {
            // Logic can be added here if needed to force sync
        }

        target.setGenero(source.getGenero());
        target.setCorte(source.getCorte());
        target.setLargo(source.getLargo());
        target.setCuello(source.getCuello());
        target.setTela(source.getTela());
        target.setHasCuffs(source.hasCuffs());
        target.setHasMesh(source.hasMesh());
        target.setHasShirt(source.hasShirt());
        target.setHasShorts(source.hasShorts());
        target.setHasSocks(source.hasSocks());
        target.setCorteShort(source.getCorteShort());
        
        // Sync flags only if not personalized
        if (!arqueroDisenoPersonalizado) {
            target.setHasShortsStripe(source.hasShortsStripe());
            target.setHasShortsPicket(source.hasShortsPicket());
            target.setHasShortsPocket(source.hasShortsPocket());
            target.setHasShortsCuff(source.hasShortsCuff());
            target.setHasShortsCord(source.hasShortsCord());
        }
    }

    public void flushUIStateToDataModel() {
        notifyStateChanged();
    }
}
