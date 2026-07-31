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
    private final SVGPath canezuLayer = new SVGPath();
    private final javafx.scene.Group collarStripeGroup = new javafx.scene.Group();

    private final SVGPath mesh = new SVGPath();
    private final SVGPath cuffs = new SVGPath();
    private final SVGPath cuffsShadow = new SVGPath();
    private final SVGPath cuffsDetail = new SVGPath();

    private final SVGPath brandBase = new SVGPath();
    private final SVGPath brandDetail = new SVGPath();

    // Linea decorativa en pecho (espejo de ShirtRenderer)
    private final SVGPath shirtLinea = new SVGPath();
    private final SVGPath shirtLineaClip = new SVGPath();
    private boolean hasLinea = false;

    private boolean telaNatural = false;
    private String currentCollarType = "V";
    private org.example.model.CollarDesignConfig collarDesignConfig;

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
        configureDetailLayer(canezuLayer, Color.web("#4a4a4a"));
        canezuLayer.setVisible(false);
        canezuLayer.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);

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

        // Linea decorativa (igual que ShirtRenderer)
        configureLayer(shirtLinea, Color.web("#7f8c8d"), null);
        shirtLinea.setStrokeWidth(0);
        shirtLinea.setClip(shirtLineaClip);

        // GROUP 1: BASE LAYERS (Behind User Images)
        group.getChildren().addAll(backingLayer, body, sleeves, shirtLinea);

        // GROUP 2: DETAILS (On Top of User Images)
        // Jerarquía del cuello (de abajo hacia arriba): collarDetail (_detalle), canezuLayer (canesú), collarShadow (_reforsado), collar (v o redondo)
        detailGroup.getChildren().addAll(
                bodyShadow, sleevesShadow,
                bodyOutline, sleevesOutline,
                bodyDetail, sleevesDetail,
                mesh,
                collarDetail, canezuLayer, collarShadow, collar, collarStripeGroup,
                brandBase, brandDetail);

        // GROUP 3: CUFFS / PUÑOS (Always in front of user drawings)
        cuffsGroup.getChildren().addAll(cuffs, cuffsShadow, cuffsDetail);
        cuffsGroup.setMouseTransparent(true);
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
        String subfolder = "";
        if (collarType.toUpperCase().startsWith("V")) {
            subfolder = "CUELLO_V/";
        } else if (collarType.toUpperCase().startsWith("REDONDO")) {
            subfolder = "CUELLO_REDONDO/";
        }
        String collarFilename = collarType.replace(" ", "_").toLowerCase() + ".svg";
        String collarPath = ("/vectors/" + genderFolder + "/cuellos/" + cutFolder + "/" + subfolder + collarFilename).toLowerCase();
        loadLayerWithExtras(collar, collarShadow, collarDetail, collarPath);

        // CANEZU Layer
        String canezuFilename = "REDONDO".equalsIgnoreCase(collarType) ? "canezu-R.svg" : "canezu.svg";
        String canezuPath = ("/vectors/" + genderFolder + "/cuellos/" + cutFolder + "/" + subfolder + canezuFilename).toLowerCase();
        safeSetContent(canezuLayer, SVGCache.loadPath(canezuPath));
        boolean isVOrRedondo = "V".equalsIgnoreCase(collarType) || "REDONDO".equalsIgnoreCase(collarType);
        canezuLayer.setVisible(isVOrRedondo);

        // Linea decorativa (carga el vector _linea.svg del arquero si existe)
        String lineaPath = arqueroShirtPath.replace(".svg", "_linea.svg");
        shirtLinea.setContent(SVGCache.loadOptionalPath(lineaPath));
        shirtLinea.setVisible(hasLinea && shirtLinea.getContent() != null && !shirtLinea.getContent().isEmpty());
        shirtLineaClip.setContent(body.getContent() + " " + sleeves.getContent());
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
        String shadowContent = paths.getOrDefault("sombra", "");
        if (shadowContent.isEmpty()) {
            shadowContent = SVGCache.loadOptionalPath(basePath.replace(".svg", "_sombra.svg"));
        }
        if (shadowContent.isEmpty()) {
            shadowContent = SVGCache.loadOptionalPath(basePath.replace(".svg", "_reforsado.svg"));
        }
        if (shadowContent.isEmpty()) {
            shadowContent = SVGCache.loadOptionalPath(basePath.replace(".svg", "_reforzado.svg"));
        }
        shadow.setContent(shadowContent);

        String detailContent = paths.getOrDefault("detalle", "");
        if (detailContent.isEmpty()) {
            detailContent = SVGCache.loadOptionalPath(basePath.replace(".svg", "_detalle.svg"));
        }
        detail.setContent(detailContent);
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

        if (colorState.containsKey("shirtLinea")) {
            Color c = colorState.get("shirtLinea");
            shirtLinea.setFill(sanitizeFillColor(c));
            shirtLinea.setStrokeWidth(0);
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

        Color bodyColorForCanezu = colorState.get("body");
        if (bodyColorForCanezu != null && isDarkColor(bodyColorForCanezu) && !isBlackColor(bodyColorForCanezu)) {
            canezuLayer.setFill(Color.web("#888888"));
        } else {
            canezuLayer.setFill(Color.web("#4a4a4a"));
        }

        applyCollarCustomization();
    }

    public void setCollarDesignConfig(org.example.model.CollarDesignConfig config) {
        this.collarDesignConfig = config;
        applyCollarCustomization();
    }

    public void applyCollarCustomization() {
        collarStripeGroup.getChildren().clear();

        if (collarDesignConfig != null && collarDesignConfig.isEnabled()) {
            Color baseCol = Color.web(collarDesignConfig.getBaseColor());
            collar.setFill(baseCol);

            if (collarDesignConfig.getStripes() != null && !collarDesignConfig.getStripes().isEmpty()
                    && collar.getContent() != null && !collar.getContent().isEmpty()) {

                // Clip mask matching exact collar boundary
                SVGPath clipMask = new SVGPath();
                clipMask.setContent(collar.getContent());
                collarStripeGroup.setClip(clipMask);

                boolean isV = "V".equalsIgnoreCase(currentCollarType);

                // Exact SVG Centerline paths passing right through the middle of the front V collar band and back collar band
                String frontCenterline = isV
                    ? "M221.5,36.2 Q237.0,54.0 252.46,71.65 Q268.0,54.0 283.1,36.2"
                    : "M224.5,32.5 Q252.46,62.5 280.5,32.5";

                String backCenterline = isV
                    ? "M225.46,34.5 Q252.2,39.5 278.92,34.5"
                    : "M224.9,31.0 Q252.4,35.0 279.94,31.0";

                for (org.example.model.CollarDesignConfig.CollarStripe stripe : collarDesignConfig.getStripes()) {
                    if (stripe == null || stripe.getColor() == null) continue;

                    Color stripeColor = Color.web(stripe.getColor());
                    double width = Math.max(3.0, stripe.getThicknessRatio() * 14.0);

                    // 1. Front Collar Centerline Stripe (Left arm -> V point -> Right arm)
                    SVGPath frontStripe = new SVGPath();
                    frontStripe.setContent(frontCenterline);
                    frontStripe.setFill(null);
                    frontStripe.setStroke(stripeColor);
                    frontStripe.setStrokeWidth(width);
                    frontStripe.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                    frontStripe.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                    collarStripeGroup.getChildren().add(frontStripe);

                    // 2. Back Collar Centerline Stripe (Back neck band)
                    SVGPath backStripe = new SVGPath();
                    backStripe.setContent(backCenterline);
                    backStripe.setFill(null);
                    backStripe.setStroke(stripeColor);
                    backStripe.setStrokeWidth(width);
                    backStripe.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                    backStripe.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                    collarStripeGroup.getChildren().add(backStripe);
                }
            } else {
                collarStripeGroup.setClip(null);
            }
        } else {
            collarStripeGroup.setClip(null);
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

    @Override
    public void setShirtLinea(boolean hasLinea) {
        this.hasLinea = hasLinea;
        shirtLinea.setVisible(hasLinea && shirtLinea.getContent() != null && !shirtLinea.getContent().isEmpty());
    }
}

