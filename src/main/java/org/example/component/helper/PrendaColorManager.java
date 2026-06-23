package org.example.component.helper;

import javafx.scene.paint.Color;

import org.example.component.renderer.BaseGarmentRenderer;
import org.example.component.renderer.ShortsRenderer;
import org.example.component.renderer.SocksRenderer;
import org.example.model.PrendaState;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages color state and application for the PrendaVisualizer.
 * Extracts color logic from the main visualizer class.
 * Now delegates state to PrendaState to allow decoupled camiseta/arquero configs.
 */
public class PrendaColorManager {

    private PrendaState state;
    private final BaseGarmentRenderer shirtRenderer;
    private final ShortsRenderer shortsRenderer;
    private final SocksRenderer socksRenderer;

    private final Map<String, Color> tempPreviewColors = new HashMap<>();
    private final Map<String, Color> lastKnownColors = new HashMap<>();

    // Callback to notify parent of state changes
    private final Runnable onStateChangedCallback;

    private boolean notificationsSuspended = false;
    private boolean locked = false;

    public PrendaColorManager(PrendaState state, BaseGarmentRenderer shirt, ShortsRenderer shorts, SocksRenderer socks,
            Runnable onStateChanged) {
        this.state = state;
        this.shirtRenderer = shirt;
        this.shortsRenderer = shorts;
        this.socksRenderer = socks;
        this.onStateChangedCallback = onStateChanged;

        // Initial Defaults if empty
        if (state != null && (state.getColors() == null || state.getColors().isEmpty())) {
            resetColors();
        } else {
            if (state != null) lastKnownColors.putAll(state.getColors());
            reapplyColors();
        }
    }

    public void setNotificationsSuspended(boolean suspended) {
        this.notificationsSuspended = suspended;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setState(PrendaState newState) {
        this.state = newState;
        reapplyColors();
    }

    public void setInternalCode(String partName, String code) {
        if (state != null) state.getInternalCodes().put(partName, code);
        notifyChanged();
    }

    public String getInternalCode(String partName) {
        return state != null ? state.getInternalCodes().getOrDefault(partName, "") : "";
    }

    public void resetColors() {
        if (state == null) return;
        state.getColors().clear();
        state.getInternalCodes().clear();
        tempPreviewColors.clear();
        
        Map<String, Color> colorState = state.getColors();
        colorState.put("body", Color.WHITE);
        colorState.put("sleeves", Color.WHITE);
        colorState.put("collar", Color.web("#95a5a6"));
        colorState.put("cuff", Color.web("#95a5a6"));
        colorState.put("mesh", Color.web("#95a5a6"));
        colorState.put("shorts", Color.WHITE);
        colorState.put("shortsStripe", Color.web("#95a5a6"));
        colorState.put("shortsLinea", Color.web("#95a5a6"));
        colorState.put("shortsPicket", Color.web("#95a5a6"));
        colorState.put("shortsCuff", Color.web("#95a5a6"));
        colorState.put("shortsCord", Color.WHITE);
        colorState.put("socks", Color.WHITE);
        colorState.put("socksSole", Color.web("#95a5a6"));
        colorState.put("socksDetail", Color.BLACK);
        colorState.put("brandSocks", Color.BLACK);
        colorState.put("brandChest", Color.web("#333333"));
        colorState.put("brandShort", Color.web("#333333"));
        
        lastKnownColors.clear();
        lastKnownColors.putAll(colorState);

        reapplyColors();
    }

    public void setPreviewColor(String partName, Color c) {
        if (c == null) tempPreviewColors.remove(partName);
        else tempPreviewColors.put(partName, c);
        reapplyColors();
    }

    public void clearPreviewColors() {
        tempPreviewColors.clear();
        reapplyColors();
        clearCache(); // Force fresh render to clear any "ghost" colors or opacity effects
    }

    public void reapplyColors() {
        if (state == null || notificationsSuspended) return;
        
        // Base colors from persistent state
        Map<String, Color> colorState = new HashMap<>(state.getColors());
        
        // --- SMART FALLBACKS: Use last known color if missing in current state ---
        colorState.putIfAbsent("brandChest", lastKnownColors.getOrDefault("brandChest", Color.web("#333333")));
        colorState.putIfAbsent("brandShort", lastKnownColors.getOrDefault("brandShort", Color.web("#333333")));
        colorState.putIfAbsent("brandSocks", lastKnownColors.getOrDefault("brandSocks", Color.BLACK));
        colorState.putIfAbsent("body", lastKnownColors.getOrDefault("body", Color.WHITE));
        colorState.putIfAbsent("sleeves", lastKnownColors.getOrDefault("sleeves", Color.WHITE));
        colorState.putIfAbsent("collar", lastKnownColors.getOrDefault("collar", Color.web("#95a5a6")));
        colorState.putIfAbsent("cuff", lastKnownColors.getOrDefault("cuff", Color.web("#95a5a6")));
        colorState.putIfAbsent("mesh", lastKnownColors.getOrDefault("mesh", Color.web("#95a5a6")));
        colorState.putIfAbsent("shirtStripe", lastKnownColors.getOrDefault("shirtStripe", Color.web("#95a5a6")));
        colorState.putIfAbsent("shirtLinea", lastKnownColors.getOrDefault("shirtLinea", Color.web("#95a5a6")));
        colorState.putIfAbsent("shorts", lastKnownColors.getOrDefault("shorts", Color.WHITE));
        colorState.putIfAbsent("shortsStripe", lastKnownColors.getOrDefault("shortsStripe", Color.web("#95a5a6")));
        colorState.putIfAbsent("shortsLinea", lastKnownColors.getOrDefault("shortsLinea", Color.web("#95a5a6")));
        colorState.putIfAbsent("shortsPicket", lastKnownColors.getOrDefault("shortsPicket", Color.web("#95a5a6")));
        colorState.putIfAbsent("shortsCuff", lastKnownColors.getOrDefault("shortsCuff", Color.web("#95a5a6")));
        colorState.putIfAbsent("shortsCord", lastKnownColors.getOrDefault("shortsCord", Color.WHITE));
        colorState.putIfAbsent("socks", lastKnownColors.getOrDefault("socks", Color.WHITE));
        colorState.putIfAbsent("socksSole", lastKnownColors.getOrDefault("socksSole", Color.web("#95a5a6")));
        colorState.putIfAbsent("socksDetail", lastKnownColors.getOrDefault("socksDetail", Color.BLACK));
        colorState.putIfAbsent("socksTop", lastKnownColors.getOrDefault("socksTop", Color.web("#95a5a6")));

        // Overlay temporary preview colors (these are NOT saved to state/model)
        colorState.putAll(tempPreviewColors);

        // Apply colors to all managed renderers regardless of visibility to ensure they are ready when shown
        if (shirtRenderer != null && shirtRenderer.getGroup() != null)
            shirtRenderer.applyColors(colorState);
        
        if (shortsRenderer != null && shortsRenderer.getGroup() != null)
            shortsRenderer.applyColors(colorState);
        
        if (socksRenderer != null && socksRenderer.getGroup() != null)
            socksRenderer.applyColors(colorState);
    }

    // --- Setters (Persistent) --

    public void setPartColor(String key, Color c) {
        if (locked) return;
        if (state != null) {
            state.getColors().put(key, c);
            lastKnownColors.put(key, c);
        }
        reapplyColors();
        notifyChanged();
    }

    public void setColorBase(Color c) { setPartColor("body", c); }
    public void setSleevesColor(Color c) { setPartColor("sleeves", c); }
    public void setCollarColor(Color c) { setPartColor("collar", c); }
    public void setCuffColor(Color c) { setPartColor("cuff", c); }
    public void setMeshColor(Color c) { setPartColor("mesh", c); }
    public void setShortsColor(Color c) { setPartColor("shorts", c); }
    public void setShortsStripeColor(Color c) { setPartColor("shortsStripe", c); }
    public void setShortsPicketColor(Color c) { setPartColor("shortsPicket", c); }
    public void setShortsCuffColor(Color c) { setPartColor("shortsCuff", c); }
    public void setShortsCordColor(Color c) { setPartColor("shortsCord", c); }
    public void setSocksColor(Color c) { setPartColor("socks", c); }
    public void setSocksSoleColor(Color c) { setPartColor("socksSole", c); }
    public void setSocksDetailColor(Color c) { setPartColor("socksDetail", c); }
    public void setSocksTopColor(Color c) { setPartColor("socksTop", c); }
    
    public void setChestBrandColor(Color c) { setPartColor("brandChest", c); }
    public void setChestBrandColor(Color c, boolean notify) {
        if (locked) return;
        if (state != null) state.getColors().put("brandChest", c);
        reapplyColors();
        if (notify) notifyChanged();
    }

    public void setShirtStripeColor(Color c, boolean notify) {
        if (locked) return;
        if (state != null) {
            state.getColors().put("shirtStripe", c);
            lastKnownColors.put("shirtStripe", c);
        }
        reapplyColors();
        if (notify) notifyChanged();
    }

    public void setShirtLineaColor(Color c, boolean notify) {
        if (locked) return;
        if (state != null) {
            state.getColors().put("shirtLinea", c);
            lastKnownColors.put("shirtLinea", c);
        }
        reapplyColors();
        if (notify) notifyChanged();
    }

    public void setShortsLineaColor(Color c, boolean notify) {
        if (locked) return;
        if (state != null) {
            state.getColors().put("shortsLinea", c);
            lastKnownColors.put("shortsLinea", c);
        }
        reapplyColors();
        if (notify) notifyChanged();
    }
    
    public void setShortBrandColor(Color c) { setPartColor("brandShort", c); }
    public void setShortBrandColor(Color c, boolean notify) {
        if (locked) return;
        if (state != null) state.getColors().put("brandShort", c);
        reapplyColors();
        if (notify) notifyChanged();
    }
    
    public void setSocksBrandColor(Color c) { setPartColor("brandSocks", c); }
    public void setSocksBrandColor(Color c, boolean notify) {
        if (locked) return;
        if (state != null) {
            state.getColors().put("brandSocks", c);
            lastKnownColors.put("brandSocks", c);
        }
        reapplyColors();
        if (notify) notifyChanged();
    }

    public void setSocksBaseColor(Color c) { setPartColor("socks", c); }

    /**
     * Quickly applies a single reference color to ALL parts of the garment 
     * (Body, Sleeves, Collar, Cuffs, Mesh, Stripe).
     * Used mainly for Goalkeepers who use a single reference color from table.
     */
    public void applyFullGarmentColor(Color c) {
        if (locked) return;
        if (c == null || state == null) return;
        state.getColors().put("body", c);
        state.getColors().put("sleeves", c);
        state.getColors().put("collar", c);
        state.getColors().put("cuff", c);
        state.getColors().put("mesh", c);
        state.getColors().put("shirtStripe", c);
        state.getColors().put("shorts", c);
        state.getColors().put("shortsStripe", c);
        state.getColors().put("shortsPicket", c);
        state.getColors().put("shortsCuff", c);
        state.getColors().put("socks", c);
        state.getColors().put("socksDetail", c);
        state.getColors().put("socksTop", c);
        reapplyColors();
    }

    // --- Getters ---

    public Color getPartColor(String partName, Color defaultColor) {
        return (state != null && state.getColors() != null) ? state.getColors().getOrDefault(partName, defaultColor) : defaultColor;
    }

    public Map<String, Color> getColorState() {
        return state != null ? state.getColors() : new HashMap<>();
    }

    public Map<String, String> getInternalColorCodes() {
        return state != null ? state.getInternalCodes() : new HashMap<>();
    }

    private void notifyChanged() {
        if (onStateChangedCallback != null) onStateChangedCallback.run();
    }

    /**
     * Clears the JavaFX node cache to force a fresh render on the GPU.
     * Prevents 'ghost colors' from previous design modes.
     */
    public void clearCache() {
        if (shirtRenderer != null && shirtRenderer.getGroup() != null) {
            shirtRenderer.getGroup().setCache(false);
            shirtRenderer.getGroup().setCache(true); // Cycle to clear stale buffers
        }
        if (shortsRenderer != null && shortsRenderer.getGroup() != null) {
            shortsRenderer.getGroup().setCache(false);
            shortsRenderer.getGroup().setCache(true);
        }
        if (socksRenderer != null && socksRenderer.getGroup() != null) {
            socksRenderer.getGroup().setCache(false);
            socksRenderer.getGroup().setCache(true);
        }
    }
}
