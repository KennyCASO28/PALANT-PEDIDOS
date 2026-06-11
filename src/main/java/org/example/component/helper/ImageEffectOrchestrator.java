package org.example.component.helper;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.WritableImage;
import org.example.component.ImageLayer;
import org.example.model.ImageLayerState;

/**
 * Manages ColorAdjust effects and pixel baking for ImageLayer.
 */
public class ImageEffectOrchestrator {

    private final ImageLayer layer;
    private final ImageLayerState state;
    private final ColorAdjust colorAdjust = new ColorAdjust();

    public ImageEffectOrchestrator(ImageLayer layer, ImageLayerState state) {
        this.layer = layer;
        this.state = state;
        
        // Initial Sync
        syncFromState();
    }

    public void syncFromState() {
        colorAdjust.setBrightness(state.brightness);
        colorAdjust.setContrast(state.contrast);
        colorAdjust.setSaturation(state.saturation);
    }

    public ColorAdjust getEffect() {
        return colorAdjust;
    }

    public void setBrightness(double b) {
        state.brightness = b;
        colorAdjust.setBrightness(b);
    }

    public void setContrast(double c) {
        state.contrast = c;
        colorAdjust.setContrast(c);
    }

    public void setSaturation(double s) {
        state.saturation = s;
        colorAdjust.setSaturation(s);
    }

    public void reset() {
        setBrightness(0);
        setContrast(0);
        setSaturation(0);
    }

    public void applyAdjustmentsToPixels() {
        if (state.brightness == 0 && state.contrast == 0 && state.saturation == 0) return;

        layer.recordUndoStateContent("Aplicar Ajustes");
        WritableImage baked = layer.snapshotCanvas();
        layer.setImage(baked);
        reset();
    }
}
