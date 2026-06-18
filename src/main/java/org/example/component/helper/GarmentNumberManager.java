package org.example.component.helper;

import java.util.Map;
import javafx.scene.Group;
import javafx.scene.paint.Color;

import org.example.component.NumberComposition;
import org.example.component.PrendaVisualizer;
import org.example.logic.GarmentAssetManager;
import org.example.model.PrendaState;
import org.example.utils.SVGCache;

public class GarmentNumberManager {

    private final PrendaVisualizer visualizer;

    private NumberComposition chestNumber;
    private NumberComposition backNumber;
    private NumberComposition shortNumber;

    // Independent Goalkeeper numbers
    private NumberComposition arqueroChestNumber;
    private NumberComposition arqueroBackNumber;
    private NumberComposition arqueroShortNumber;
    
    // Per-node signature cache to prevent flickering on redundant reloads
    private final java.util.Map<NumberComposition, String> lastNodeSignatures = new java.util.HashMap<>();

    public GarmentNumberManager(PrendaVisualizer visualizer) {
        this.visualizer = visualizer;
        
        chestNumber = new NumberComposition();
        backNumber = new NumberComposition();
        shortNumber = new NumberComposition();

        arqueroChestNumber = new NumberComposition();
        arqueroBackNumber = new NumberComposition();
        arqueroShortNumber = new NumberComposition();
    }

    public void addToGroup(Group contentGroup) {
        contentGroup.getChildren().addAll(
                chestNumber.getRoot(), backNumber.getRoot(), shortNumber.getRoot(),
                arqueroChestNumber.getRoot(), arqueroBackNumber.getRoot(), arqueroShortNumber.getRoot());
    }

    public NumberComposition getChestNumber(boolean isArquero) {
        return isArquero ? arqueroChestNumber : chestNumber;
    }

    public NumberComposition getBackNumber(boolean isArquero) {
        return isArquero ? arqueroBackNumber : backNumber;
    }

    public NumberComposition getShortNumber(boolean isArquero) {
        return isArquero ? arqueroShortNumber : shortNumber;
    }

    public void reapplyNumberColors(PrendaState camisetaState, PrendaState arqueroState) {
        restoreNumberColorsFromState(camisetaState, "pecho", chestNumber);
        restoreNumberColorsFromState(camisetaState, "espalda", backNumber);
        restoreNumberColorsFromState(camisetaState, "short", shortNumber);

        if (arqueroState != null) {
            restoreNumberColorsFromState(arqueroState, "pecho", arqueroChestNumber);
            restoreNumberColorsFromState(arqueroState, "espalda", arqueroBackNumber);
            restoreNumberColorsFromState(arqueroState, "short", arqueroShortNumber);
        }
    }

    public void reloadActiveNumbers(PrendaState state, boolean isArquero) {
        reloadActiveNumber(getChestNumber(isArquero), "pecho", state);
        reloadActiveNumber(getBackNumber(isArquero), "espalda", state);
        reloadActiveNumber(getShortNumber(isArquero), "short", state);
    }

    public boolean hasNumberDigit(String digit) {
        return digit != null && !digit.isBlank();
    }

    public void reloadAllNumbersAcrossDesigns(PrendaState camisetaState, PrendaState arqueroState) {
        // Recargar conjunto de CAMPO usando camisetaState
        if (camisetaState != null) {
            reloadActiveNumber(chestNumber, "pecho", camisetaState);
            reloadActiveNumber(backNumber, "espalda", camisetaState);
            reloadActiveNumber(shortNumber, "short", camisetaState);
        }
        // Recargar conjunto de ARQUERO usando arqueroState
        if (arqueroState != null) {
            reloadActiveNumber(arqueroChestNumber, "pecho", arqueroState);
            reloadActiveNumber(arqueroBackNumber, "espalda", arqueroState);
            reloadActiveNumber(arqueroShortNumber, "short", arqueroState);
        }
    }

    private void reloadActiveNumber(NumberComposition nc, String location, PrendaState st) {
        if (st == null || nc == null) return;
        String digit = null;
        double x = 0, y = 0, scale = 1.0;
        boolean visible = false;

        if (location.equalsIgnoreCase("pecho")) {
            digit = st.getCurrentChestNumber();
            x = st.getChestNumberX();
            y = st.getChestNumberY();
            scale = st.getChestNumberScale();
            visible = st.isChestNumberVisible();
        } else if (location.equalsIgnoreCase("espalda")) {
            digit = st.getCurrentBackNumber();
            x = st.getBackNumberX();
            y = st.getBackNumberY();
            scale = st.getBackNumberScale();
            visible = st.isBackNumberVisible();
        } else if (location.startsWith("short")) {
            digit = st.getCurrentShortNumber();
            x = st.getShortNumberX();
            y = st.getShortNumberY();
            scale = st.getShortNumberScale();
            visible = st.isShortNumberVisible();
            if (st.isShortCrestVisible()) location = "short_escudo";
        }

        // SMART SKIP: Use a comprehensive signature of ALL visual properties for this specific node.
        // If nothing has changed for 'Pecho', it should NOT be touched even when adding 'Espalda'.
        int colorHash = (location.equalsIgnoreCase("pecho") ? st.getChestNumberColors() : 
                        (location.equalsIgnoreCase("espalda") ? st.getBackNumberColors() : st.getShortNumberColors())).hashCode();
        
        String nodeSig = st.getGenero() + "|" + (location.startsWith("short") ? st.getCorteShort() : st.getCorte()) 
                + "|" + location + "|" + digit + "|v:" + visible + "|x:" + x + "|y:" + y + "|s:" + scale + "|c:" + colorHash;

        if (nodeSig.equals(lastNodeSignatures.get(nc))) {
            return; // ABORT: Zero visual change detected for this component.
        }

        if (hasNumberDigit(digit)) {
            // Load paths only if the vector source (Gender/Cut/Digit) changed
            String vectorSig = st.getGenero() + "|" + (location.startsWith("short") ? st.getCorteShort() : st.getCorte()) + "|" + location + "|" + digit;
            // We store the vector sig in the node's user data for efficiency
            if (!vectorSig.equals(nc.getRoot().getUserData())) {
                loadNumberVector(nc, location, digit, st);
                nc.getRoot().setUserData(vectorSig);
            }
            restoreNumberColorsFromState(st, location, nc);
            nc.setPosition(x, y, scale);
        }
        
        nc.setVisible(visible && hasNumberDigit(digit));
        lastNodeSignatures.put(nc, nodeSig);
    }

    public void loadNumberVector(NumberComposition nc, String location, String number, PrendaState st) {
        if (number == null || number.isEmpty() || nc == null) return;

        if (location.equalsIgnoreCase("short") || location.equalsIgnoreCase("short_escudo")) {
            st.setCurrentShortNumber(number);
            if (st.isShortCrestVisible()) location = "short_escudo";
        }

        String basePath = GarmentAssetManager.getNumberBasePath(st.getGenero(), location,
                (location.equalsIgnoreCase("short") || location.equalsIgnoreCase("short_escudo"))
                        ? st.getCorteShort() : st.getCorte(),
                number).toLowerCase();

        nc.setLayerPath(0, SVGCache.loadPath(basePath + "base.svg"));
        nc.setLayerPath(1, SVGCache.loadPath(basePath + "combo.svg"));
        nc.setLayerPath(2, SVGCache.loadPath(basePath + "borde.svg"));
        nc.setLayerPath(3, ""); // lineas and sombra feature removed
        nc.setLayerPath(4, SVGCache.loadPath(basePath + "marca.svg"));
    }

    public void setGlobalNumberDigit(String digit, PrendaState state, boolean hasShorts, boolean isArquero) {
        if (digit == null || digit.isEmpty()) return;

        // Update all states
        state.setCurrentChestNumber(digit);
        state.setCurrentBackNumber(digit);
        state.setCurrentShortNumber(digit);

        // Reload vectors for visible/active parts using the correct DESIGN-SPECIFIC nodes
        loadNumberVector(getChestNumber(isArquero), "pecho", digit, state);
        loadNumberVector(getBackNumber(isArquero), "espalda", digit, state);
        if (hasShorts) {
            loadNumberVector(getShortNumber(isArquero), "short", digit, state);
        }
    }

    public void saveNumberColorsToState(PrendaState s, String loc, NumberComposition comp) {
        Map<Integer, Color> map = loc.equals("chest") ? s.getChestNumberColors()
                : (loc.equals("back") ? s.getBackNumberColors() : s.getShortNumberColors());
        if (map == null) return;
        map.clear();
        for (int i = 0; i < 5; i++) {
            Color c = comp.getLayerColor(i);
            if (c != null) map.put(i, c);
        }
    }

    public void restoreNumberColorsFromState(PrendaState s, String loc, NumberComposition comp) {
        Map<Integer, Color> map = loc.equalsIgnoreCase("pecho") || loc.equalsIgnoreCase("chest")
                ? s.getChestNumberColors()
                : (loc.equalsIgnoreCase("espalda") || loc.equalsIgnoreCase("back") ? s.getBackNumberColors()
                        : s.getShortNumberColors());
        if (map == null || map.isEmpty()) return;
        for (Map.Entry<Integer, Color> entry : map.entrySet()) {
            comp.setLayerColor(entry.getKey(), entry.getValue());
        }
    }

    public void restoreNumbersFromState(PrendaState s, boolean isSwappingDesign, boolean isArquero) {
        NumberComposition activeChest = getChestNumber(isArquero);
        NumberComposition activeBack = getBackNumber(isArquero);
        NumberComposition activeShort = getShortNumber(isArquero);

        activeChest.setPosition(s.getChestNumberX(), s.getChestNumberY(), s.getChestNumberScale());
        loadNumberVector(activeChest, "pecho", s.getCurrentChestNumber(), s);
        restoreNumberColorsFromState(s, "pecho", activeChest);
        activeChest.setVisible(s.isChestNumberVisible() && hasNumberDigit(s.getCurrentChestNumber()) && !isSwappingDesign);

        activeBack.setPosition(s.getBackNumberX(), s.getBackNumberY(), s.getBackNumberScale());
        loadNumberVector(activeBack, "espalda", s.getCurrentBackNumber(), s);
        restoreNumberColorsFromState(s, "espalda", activeBack);
        activeBack.setVisible(s.isBackNumberVisible() && hasNumberDigit(s.getCurrentBackNumber()) && !isSwappingDesign);

        activeShort.setPosition(s.getShortNumberX(), s.getShortNumberY(), s.getShortNumberScale());
        loadNumberVector(activeShort, "short", s.getCurrentShortNumber(), s);
        restoreNumberColorsFromState(s, "short", activeShort);
        activeShort.setVisible(s.isShortNumberVisible() && hasNumberDigit(s.getCurrentShortNumber()) && !isSwappingDesign);
    }
    
    public void resetColorsToDefault() {
        if (chestNumber != null) chestNumber.resetColorsToDefault();
        if (backNumber != null) backNumber.resetColorsToDefault();
        if (shortNumber != null) shortNumber.resetColorsToDefault();
    }

    public void clearAll() {
        if (chestNumber != null) chestNumber.clear();
        if (backNumber != null) backNumber.clear();
        if (shortNumber != null) shortNumber.clear();
    }

    public void deselectAllNames() {
        if (chestNumber != null) chestNumber.setSelected(false);
        if (backNumber != null) backNumber.setSelected(false);
        if (shortNumber != null) shortNumber.setSelected(false);
    }

    public boolean isNumberRoot(javafx.scene.Node n) {
        return n != null && (n == chestNumber.getRoot() || n == backNumber.getRoot() || n == shortNumber.getRoot() ||
               n == arqueroChestNumber.getRoot() || n == arqueroBackNumber.getRoot() || n == arqueroShortNumber.getRoot());
    }

    public NumberComposition getNumberCompositionFromRoot(javafx.scene.Node n) {
        if (n == null) return null;
        if (n == chestNumber.getRoot()) return chestNumber;
        if (n == backNumber.getRoot()) return backNumber;
        if (n == shortNumber.getRoot()) return shortNumber;
        if (n == arqueroChestNumber.getRoot()) return arqueroChestNumber;
        if (n == arqueroBackNumber.getRoot()) return arqueroBackNumber;
        if (n == arqueroShortNumber.getRoot()) return arqueroShortNumber;
        return null;
    }
}
