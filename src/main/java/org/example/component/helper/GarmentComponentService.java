package org.example.component.helper;

import javafx.scene.paint.Color;
import org.example.component.PrendaVisualizer;
import org.example.model.PrendaState;
import org.example.model.TipoCuello;
import org.example.model.TipoLargo;
import org.example.model.TipoCorte;
import org.example.model.TipoMedias;
import org.example.component.renderer.ShortsRenderer;
import org.example.component.renderer.SocksRenderer;

/**
 * Service to manage structural and aesthetic changes to garment components.
 * This encapsulates the logic for sleeves, collars, pockets, socks, and their specific colors.
 */
public class GarmentComponentService {

    private final PrendaVisualizer visualizer;

    public GarmentComponentService(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    public void setLargo(TipoLargo largo) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setLargo(largo);
            updateSleeveLogic();
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setCuello(TipoCuello cuello) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setCuello(cuello);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setMalla(boolean hasMesh) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasMesh(hasMesh);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setPunos(boolean hasCuffs) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasCuffs(hasCuffs);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShortsCorte(TipoCorte corte) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setCorteShort(corte);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShirtVisible(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShirt(v);
            visualizer.applyVisibility();
        }
    }

    public void setShorts(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShorts(v);
            visualizer.applyVisibility();
        }
    }

    public void setMedias(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasSocks(v);
            visualizer.applyVisibility();
        }
    }

    public void setSocksTop(boolean visible) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasSocksTop(visible);
            visualizer.applyVisibility();
        }
    }

    public void setTipoMedias(TipoMedias tm) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setTipoMedias(tm);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShortsStripe(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShortsStripe(v);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShortsPocket(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShortsPocket(v);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShortsPicket(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShortsPicket(v);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShortsCuff(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShortsCuff(v);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShortsCord(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShortsCord(v);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShortsLining(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShortsLining(v);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setShirtStripe(boolean v) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setHasShirtStripe(v);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    // --- Aesthetic / Color Logic ---
    public void setSocksTopColor(Color c) {
        visualizer.getColorManager().setSocksTopColor(c);
    }

    public void setCollarColor(Color c) {
        visualizer.getColorManager().setCollarColor(c);
    }

    public void setCuffColor(Color c) {
        visualizer.getColorManager().setCuffColor(c);
    }

    public void setShortsColor(Color c) {
        visualizer.getColorManager().setShortsColor(c);
    }

    public void setShortsStripeColor(Color c) {
        visualizer.getColorManager().setShortsStripeColor(c);
    }

    public void setShirtStripeColor(Color c) {
        visualizer.getColorManager().setShirtStripeColor(c, true);
    }

    public void setShortsPicketColor(Color c) {
        visualizer.getColorManager().setShortsPicketColor(c);
    }

    public void setShortsCuffColor(Color c) {
        visualizer.getColorManager().setShortsCuffColor(c);
    }

    public void setShortsCordColor(Color c) {
        visualizer.getColorManager().setShortsCordColor(c);
    }

    public void setSocksBaseColor(Color c) {
        visualizer.getColorManager().setSocksBaseColor(c);
    }

    public void setSocksSoleColor(Color c) {
        visualizer.getColorManager().setSocksSoleColor(c);
    }

    public void setSocksDetailColor(Color c) {
        visualizer.getColorManager().setSocksDetailColor(c);
    }

    public void setSocksBrandColor(Color c, boolean notify) {
        visualizer.getColorManager().setSocksBrandColor(c, notify);
    }

    public void setSocksBrandVisible(boolean visible) {
        PrendaState state = visualizer.getState();
        if (state != null) {
            state.setSocksBrandVisible(visible);
            visualizer.invalidateCargarCapasSignature();
            visualizer.cargarCapas();
        }
    }

    public void setSleevesColor(Color c) {
        visualizer.getColorManager().setSleevesColor(c);
    }

    public void setMeshColor(Color c) {
        visualizer.getColorManager().setMeshColor(c);
    }

    public void updateSleeveLogic() {
        PrendaState state = visualizer.getState();
        if (state == null) return;

        TipoCorte corte = state.getCorte();
        TipoLargo largo = state.getLargo();
        PrendaOverlayManager overlayManager = visualizer.getOverlayManager();

        overlayManager.setSleeveMaxThreshold(Double.MAX_VALUE);
        overlayManager.setIsFrontOnLeft(true);
        overlayManager.setIsInterleavedMapping(false);

        if (corte == TipoCorte.REDONDO) {
            if (largo == TipoLargo.MANGA_LARGA || largo == TipoLargo.MANGA_3_4)
                overlayManager.setSleeveThreshold(375.0);
            else
                overlayManager.setSleeveThreshold(443.0);
        } else {
            overlayManager.setSleeveThreshold(350.0);
            overlayManager.setIsInterleavedMapping(true);
        }
    }
}
