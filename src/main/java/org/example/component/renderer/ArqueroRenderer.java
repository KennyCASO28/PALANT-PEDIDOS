package org.example.component.renderer;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.example.logic.GarmentAssetManager;
import org.example.model.TipoGenero;
import org.example.model.TipoCorte;
import org.example.model.TipoLargo;
import org.example.utils.SVGCache;
import java.util.Map;

/**
 * Specialized renderer for the Arquero (Goalkeeper).
 * Completely independent from ShirtRenderer, similar to ShortsRenderer.
 * Loads vectors from /vectors/arquero/ directory.
 */
public class ArqueroRenderer extends BaseGarmentRenderer {
    private final SVGPath body = new SVGPath();
    private final SVGPath bodyOutline = new SVGPath();
    private final SVGPath sleeves = new SVGPath();
    private final SVGPath sleevesOutline = new SVGPath();

    private final SVGPath backingLayer = new SVGPath();
    private final SVGPath bodyShadow = new SVGPath();
    private final SVGPath bodyDetail = new SVGPath();

    private final SVGPath sleevesShadow = new SVGPath();
    private final SVGPath sleevesDetail = new SVGPath();

    private final SVGPath collar = new SVGPath();
    private final SVGPath collarShadow = new SVGPath();
    private final SVGPath collarDetail = new SVGPath();

    private final SVGPath mesh = new SVGPath();
    private final SVGPath cuffs = new SVGPath();
    private final SVGPath cuffsShadow = new SVGPath();
    private final SVGPath cuffsDetail = new SVGPath();

    private final SVGPath brandBase = new SVGPath();
    private final SVGPath brandDetail = new SVGPath();

    private boolean telaNatural = false;
    private String currentCollarType = "V";

    public ArqueroRenderer() {
        Color baseColor = Color.WHITE;
        Color strokeColor = Color.web("#2c3e50");

        configureLayer(backingLayer, Color.WHITE, null);
        backingLayer.setStrokeWidth(0);

        configureLayer(body, baseColor, strokeColor);
        body.setStrokeWidth(0);
        configureOutlineLayer(bodyOutline, strokeColor);

        configureLayer(sleeves, baseColor, strokeColor);
        sleeves.setStrokeWidth(0);
        configureOutlineLayer(sleevesOutline, strokeColor);

        configureShadowLayer(bodyShadow);
        configureShadowLayer(sleevesShadow);

        configureDetailLayer(bodyDetail, strokeColor);
        configureDetailLayer(sleevesDetail, strokeColor);
        configureDetailLayer(collarDetail, Color.WHITE);

        configureLayer(collar, Color.WHITE, strokeColor);
        collar.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        collar.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        configureShadowLayer(collarShadow);
        configureDetailLayer(collarDetail, Color.WHITE);

        configureLayer(mesh, Color.web("#bdc3c7"), Color.BLACK);

        configureLayer(cuffs, Color.web("#7f8c8d"), Color.BLACK);
        cuffs.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        cuffs.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        configureShadowLayer(cuffsShadow);
        configureDetailLayer(cuffsDetail, strokeColor);

        // Branding
        configureLayer(brandBase, Color.BLACK, null);
        brandBase.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        configureDetailLayer(brandDetail, Color.WHITE);
        brandDetail.setVisible(false);

        // GROUP 1: BASE LAYERS (Behind User Images)
        group.getChildren().addAll(backingLayer, body, sleeves);

        // GROUP 2: DETAILS (On Top of User Images)
        detailGroup.getChildren().addAll(
                bodyShadow, sleevesShadow,
                bodyOutline, sleevesOutline,
                bodyDetail, sleevesDetail,
                mesh,
                collarShadow, collar, collarDetail,
                cuffsShadow, cuffs, cuffsDetail,
                brandBase, brandDetail);
    }

    @Override
    public void updateLayers(String gender, String cut, String length, String collarType) {
        updateArqueroLayers(TipoGenero.valueOf(gender.toUpperCase()), 
                           TipoCorte.valueOf(cut.toUpperCase()), 
                           TipoLargo.valueOf(length.toUpperCase()), 
                           collarType);
    }

    /**
     * Updates all layers for Arquero with vectors from /vectors/arquero/ path.
     * Completely independent from standard shirt vectors.
     */
    public void updateArqueroLayers(TipoGenero genero, TipoCorte corte, TipoLargo largo, String collarType) {
        this.currentCollarType = collarType;
        // Body & Sleeves from ARQUERO-SPECIFIC path
        String arqueroShirtPath = GarmentAssetManager.getShirtPath(genero, corte, largo, true);
        applyCategorizedLayer(body, sleeves, bodyShadow, sleevesShadow, bodyDetail, sleevesDetail, arqueroShirtPath);

        // Backing Layer
        backingLayer.setContent(body.getContent() + " " + sleeves.getContent());

        // Mesh
        String meshPath = GarmentAssetManager.getMeshPath(genero, corte);
        mesh.setContent(SVGCache.loadPath(meshPath));

        // Cuffs
        String cuffsPath = GarmentAssetManager.getCuffsPath(genero, corte, largo);
        loadLayerWithExtras(cuffs, cuffsShadow, cuffsDetail, cuffsPath);

        // Collar
        String genderFolder = genero == TipoGenero.MUJER ? "mujer" : "varon";
        String cutFolder = corte.name().toLowerCase();
        String collarFilename = collarType.replace(" ", "_").toLowerCase() + ".svg";
        String collarPath = ("/vectors/" + genderFolder + "/cuellos/" + cutFolder + "/" + collarFilename).toLowerCase();
        loadLayerWithExtras(collar, collarShadow, collarDetail, collarPath);
    }

    private void applyCategorizedLayer(SVGPath body, SVGPath sleeves, SVGPath bodyShadow, SVGPath sleevesShadow,
            SVGPath bodyDetail, SVGPath sleevesDetail, String path) {

        Map<String, String> basePaths = org.example.utils.SVGCache.loadCategorizedPaths(path);

        // PRIORITY: Always try to load separate sleeves file first (same as ShirtRenderer)
        String separateSleevesPath = path.replace(".svg", "_mangas.svg");
        String externalSleevesContent = SVGCache.loadPath(separateSleevesPath);

        if (!externalSleevesContent.isEmpty()) {
            // Found separate sleeves file
            body.setContent(basePaths.getOrDefault("BODY", "") + " " + basePaths.getOrDefault("SLEEVES", ""));
            sleeves.setContent(externalSleevesContent);
        } else {
            // No separate sleeves, use categorized paths
            body.setContent(basePaths.getOrDefault("BODY", basePaths.getOrDefault("cuerpo", "")));
            sleeves.setContent(basePaths.getOrDefault("SLEEVES", basePaths.getOrDefault("mangas", "")));
        }

        // SYNC OUTLINES
        bodyOutline.setContent(body.getContent());
        sleevesOutline.setContent(sleeves.getContent());

        if (!body.getContent().isEmpty() || !sleeves.getContent().isEmpty()) {
            // Shadow
            Map<String, String> shadowPaths = org.example.utils.SVGCache
                    .loadCategorizedPaths(path.replace(".svg", "_sombra.svg"));

            String separateSleevesShadowPath = path.replace(".svg", "_mangas_sombra.svg");
            String externalSShadowCont = SVGCache.loadOptionalPath(separateSleevesShadowPath);

            if (!externalSShadowCont.isEmpty()) {
                bodyShadow.setContent(shadowPaths.getOrDefault("BODY", "") + " " + shadowPaths.getOrDefault("SLEEVES", ""));
                sleevesShadow.setContent(externalSShadowCont);
            } else {
                bodyShadow.setContent(shadowPaths.getOrDefault("BODY", ""));
                sleevesShadow.setContent(shadowPaths.getOrDefault("SLEEVES", ""));
            }

            // Detail
            Map<String, String> detailPaths = org.example.utils.SVGCache
                    .loadCategorizedPaths(path.replace(".svg", "_detalle.svg"));

            String separateSleevesDetailPath = path.replace(".svg", "_mangas_detalle.svg");
            String externalSDetailCont = SVGCache.loadOptionalPath(separateSleevesDetailPath);

            if (!externalSDetailCont.isEmpty()) {
                bodyDetail.setContent(detailPaths.getOrDefault("BODY", "") + " " + detailPaths.getOrDefault("SLEEVES", ""));
                sleevesDetail.setContent(externalSDetailCont);
            } else {
                bodyDetail.setContent(detailPaths.getOrDefault("BODY", ""));
                sleevesDetail.setContent(detailPaths.getOrDefault("SLEEVES", ""));
            }
        } else {
            bodyShadow.setContent("");
            sleevesShadow.setContent("");
            bodyDetail.setContent("");
            sleevesDetail.setContent("");
        }
    }

    protected void loadLayerWithExtras(SVGPath layer, SVGPath shadow, SVGPath detail, String basePath) {
        Map<String, String> paths = org.example.utils.SVGCache.loadCategorizedPaths(basePath);

        layer.setContent(paths.getOrDefault("base", SVGCache.loadPath(basePath)));
        shadow.setContent(paths.getOrDefault("sombra", ""));
        detail.setContent(paths.getOrDefault("detalle", ""));
    }

    /**
     * Control de visibilidad de capas - Compatible con ShirtRenderer
     */
    public void setMeshVisible(boolean visible) {
        mesh.setVisible(visible);
    }

    public void setCuffsVisible(boolean visible) {
        cuffs.setVisible(visible);
    }

    public void setStripeVisible(boolean visible) {
        // ArqueroRenderer no tiene stripe, pero mantiene la interfaz compatible
    }

    /**
     * Métodos de branding - Heredados de BaseGarmentRenderer
     */
    public void updateBranding(boolean visible, String basePath, String detailPath) {
        brandBase.setVisible(visible);
        // brandDetail is kept invisible, its content is merged into brandBase to create true transparent holes
        if (visible) {
            String baseContent = SVGCache.loadPath(basePath.toLowerCase());
            String detailContent = (detailPath != null && !detailPath.isEmpty()) 
                                   ? SVGCache.loadPath(detailPath.toLowerCase()) 
                                   : "";

            // Merge paths to create true holes using EVEN_ODD fill rule (set in constructor)
            brandBase.setContent(baseContent + " " + detailContent);

            // Respect factory alignment (ViewBox)
            brandBase.setLayoutX(0);
            brandBase.setLayoutY(0);
            brandBase.setScaleX(1.0);
            brandBase.setScaleY(1.0);
            brandDetail.setLayoutX(0);
            brandDetail.setLayoutY(0);
            brandDetail.setScaleX(1.0);
            brandDetail.setScaleY(1.0);
            brandDetail.setVisible(false);
        }
    }

    /**
     * Aplica colores al arquero - Similar a ShirtRenderer
     */
    public void applyColors(Map<String, Color> colorState) {
        Color strokeColor = Color.web("#2c3e50");

        if (colorState.containsKey("body")) {
            Color c = colorState.get("body");
            Color sanitized = sanitizeFillColor(c);
            body.setFill(sanitized);

            strokeColor = getContrastStroke(sanitized);
            body.setStroke(strokeColor);
            bodyOutline.setStroke(strokeColor);

            backingLayer.setVisible(true);
            backingLayer.setFill(Color.WHITE);
        }

        if (colorState.containsKey("brandChest")) {
            brandBase.setFill(sanitizeFillColor(colorState.get("brandChest")));
        }

        if (colorState.containsKey("sleeves")) {
            Color c = colorState.get("sleeves");
            Color sanitized = sanitizeFillColor(c);
            sleeves.setFill(sanitized);
            Color sleeveStroke = getContrastStroke(sanitized);
            sleeves.setStroke(sleeveStroke);
            sleevesOutline.setStroke(sleeveStroke);
        }

        if (colorState.containsKey("collar")) {
            Color c = colorState.get("collar");
            collar.setFill(sanitizeFillColor(c));
            collar.setStroke(getContrastStroke(c));
        }

        if (colorState.containsKey("cuff")) {
            Color c = colorState.get("cuff");
            cuffs.setFill(sanitizeFillColor(c));
            cuffs.setStroke(getContrastStroke(c));
        }

        if (colorState.containsKey("mesh")) {
            mesh.setFill(sanitizeFillColor(colorState.get("mesh")));
            mesh.setStroke(Color.BLACK);
        }

        boolean isVOrRedondo = "V".equalsIgnoreCase(currentCollarType) || "REDONDO".equalsIgnoreCase(currentCollarType);
        if (telaNatural && isVOrRedondo && colorState.containsKey("body")) {
            Color bodyColor = colorState.get("body");
            if (bodyColor != null && !isDarkColor(bodyColor)) {
                collarDetail.setFill(sanitizeFillColor(bodyColor));
            } else {
                collarDetail.setFill(Color.WHITE);
            }
        } else {
            collarDetail.setFill(Color.WHITE);
        }
    }

    public void setTelaNatural(boolean telaNatural) {
        this.telaNatural = telaNatural;
    }

private boolean isDarkColor(Color c) {
        if (c == null) return false;
        double brightness = c.getBrightness();
        return brightness < 0.2;
    }

    private boolean isBlackColor(Color c) {
        if (c == null) return false;
        double brightness = c.getBrightness();
        return brightness < 0.05;
    }

    /**
     * Aplica color de referencia para el arquero
     */
    public void applyReferenceColor(Color color) {
        if (color == null || color.equals(Color.WHITE)) return;

        Color sanitized = sanitizeFillColor(color);
        body.setFill(sanitized);
        sleeves.setFill(sanitized);

        Color stroke = getContrastStroke(sanitized);
        body.setStroke(stroke);
        sleeves.setStroke(stroke);
    }

    public javafx.scene.shape.SVGPath getBody() {
        return body;
    }

    public javafx.scene.shape.SVGPath getSleeves() {
        return sleeves;
    }
}

