package org.example.component.renderer;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import java.util.Map;

/**
 * Specialized renderer for the Shirt (Camiseta).
 */
public class ShirtRenderer extends BaseGarmentRenderer {
    private final SVGPath body = new SVGPath();
    private final SVGPath bodyOutline = new SVGPath();
    private final SVGPath sleeves = new SVGPath();
    private final SVGPath sleevesOutline = new SVGPath();

    // Removed Shirt Stripe as per user request (redundant with mesh)

    private final SVGPath backingLayer = new SVGPath();
    private final SVGPath bodyShadow = new SVGPath();
    private final SVGPath bodyDetail = new SVGPath();
    private final SVGPath baseRedondo = new SVGPath();

    private final SVGPath sleevesShadow = new SVGPath();
    private final SVGPath sleevesDetail = new SVGPath();

    private final SVGPath collar = new SVGPath();
    private final SVGPath collarShadow = new SVGPath();
    private final SVGPath collarDetail = new SVGPath();
    private final SVGPath canezuLayer = new SVGPath();

    private final SVGPath mesh = new SVGPath();
    private final SVGPath shirtStripe = new SVGPath();

    private final SVGPath cuffs = new SVGPath();
    private final SVGPath cuffsShadow = new SVGPath();
    private final SVGPath cuffsDetail = new SVGPath();

    private final SVGPath brandBase = new SVGPath();
    private final SVGPath brandDetail = new SVGPath();

    private boolean telaNatural = false;
    private String currentCollarType = "V";

    public ShirtRenderer() {
        Color baseColor = Color.WHITE;
        Color strokeColor = Color.web("#2c3e50");

        configureLayer(backingLayer, baseColor, null);
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

        configureLayer(collar, baseColor, strokeColor);
        collar.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        collar.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        configureShadowLayer(collarShadow);
        configureDetailLayer(collarDetail, Color.WHITE);
        configureDetailLayer(canezuLayer, Color.web("#4a4a4a"));
        canezuLayer.setVisible(false);
        canezuLayer.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);

        configureLayer(baseRedondo, null, Color.BLACK);
        baseRedondo.setStrokeWidth(0.5);

        configureLayer(mesh, Color.web("#bdc3c7"), Color.BLACK);
        configureLayer(shirtStripe, Color.web("#7f8c8d"), Color.BLACK);

        configureLayer(cuffs, Color.web("#7f8c8d"), Color.BLACK);
        cuffs.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        cuffs.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        configureShadowLayer(cuffsShadow);
        configureDetailLayer(cuffsDetail, strokeColor);

        // Branding
        configureLayer(brandBase, Color.BLACK, null);
        brandBase.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        configureDetailLayer(brandDetail, Color.WHITE);
        brandDetail.setVisible(false); // No longer used as separate layer

        // GROUP 1: BASE LAYERS (Behind User Images)
        group.getChildren().addAll(
                baseRedondo, backingLayer, body, sleeves);

        // GROUP 2: DETAILS (On Top of User Images)
        detailGroup.getChildren().addAll(
                bodyShadow, sleevesShadow,
                bodyOutline, sleevesOutline,
                bodyDetail, sleevesDetail,
                mesh, shirtStripe,
                collarShadow, collar, collarDetail, canezuLayer,
                cuffsShadow, cuffs, cuffsDetail,
                brandBase, brandDetail);
    }

    @Override
    public void updateLayers(String gender, String cut, String length, String collarType) {
        updateLayers(gender, cut, length, collarType, false);
    }

    public void updateLayers(String gender, String cut, String length, String collarType, boolean isArquero) {
        this.currentCollarType = collarType;
        org.example.model.TipoGenero g = org.example.model.TipoGenero.valueOf(gender.toUpperCase());
        org.example.model.TipoCorte c = org.example.model.TipoCorte.valueOf(cut.toUpperCase());
        org.example.model.TipoLargo l = org.example.model.TipoLargo.valueOf(length.toUpperCase());

        // Body & Sleeves (Categorized)
        String shirtPath = org.example.logic.GarmentAssetManager.getShirtPath(g, c, l, isArquero);
        applyCategorizedLayer(body, sleeves, bodyShadow, sleevesShadow, bodyDetail, sleevesDetail, shirtPath);

        // Backing Layer
        safeSetContent(backingLayer, body.getContent() + " " + sleeves.getContent());

        // Mesh
        String meshPath = org.example.logic.GarmentAssetManager.getMeshPath(g, c);
        safeSetContent(mesh, org.example.utils.SVGCache.loadPath(meshPath));

        // Stripe
        String stripePath = org.example.logic.GarmentAssetManager.getShirtPath(g, c, l, isArquero).replace(".svg", "_franja.svg");
        safeSetContent(shirtStripe, org.example.utils.SVGCache.loadOptionalPath(stripePath));

        // Cuffs
        String cuffsPath = org.example.logic.GarmentAssetManager.getCuffsPath(g, c, l);
        loadLayerWithExtras(cuffs, cuffsShadow, cuffsDetail, cuffsPath);

        // Collar
        String genderFolder = g == org.example.model.TipoGenero.MUJER ? "mujer" : "varon";
        String cutFolder = c.name().toLowerCase();
        String collarFilename = collarType.replace(" ", "_").toLowerCase() + ".svg";
        String collarPath = ("/vectors/" + genderFolder + "/cuellos/" + cutFolder + "/" + collarFilename).toLowerCase();
        loadLayerWithExtras(collar, collarShadow, collarDetail, collarPath);

        // CANEZU Layer (always on top of collarDetail for V and REDONDO)
        String canezuFilename = "REDONDO".equalsIgnoreCase(collarType) ? "canezu-R.svg" : "canezu.svg";
        String canezuPath = ("/vectors/" + genderFolder + "/cuellos/" + cutFolder + "/" + canezuFilename).toLowerCase();
        safeSetContent(canezuLayer, org.example.utils.SVGCache.loadPath(canezuPath));
        boolean isVOrRedondo = "V".equalsIgnoreCase(collarType) || "REDONDO".equalsIgnoreCase(collarType);
        canezuLayer.setVisible(isVOrRedondo);

        // Base Redondo (Sketch)
        String baseRedondoPath = org.example.logic.GarmentAssetManager.getBaseRedondoPath(g, c);
        if (baseRedondoPath != null) {
            safeSetContent(baseRedondo, org.example.utils.SVGCache.loadPath(baseRedondoPath));
        } else {
            safeSetContent(baseRedondo, "");
        }
    }

    private void applyCategorizedLayer(SVGPath body, SVGPath sleeves, SVGPath bodyShadow, SVGPath sleevesShadow,
            SVGPath bodyDetail, SVGPath sleevesDetail, String path) {

        java.util.Map<String, String> basePaths = org.example.utils.SVGCache.loadCategorizedPaths(path);

        // PRIORITY: Always try to load separate sleeves file first
        String separateSleevesPath = path.replace(".svg", "_mangas.svg");
        String externalSleevesContent = org.example.utils.SVGCache.loadPath(separateSleevesPath);

        if (!externalSleevesContent.isEmpty()) {
            safeSetContent(body, basePaths.getOrDefault("BODY", "") + " " + basePaths.getOrDefault("SLEEVES", ""));
            safeSetContent(sleeves, externalSleevesContent);
        } else {
            safeSetContent(body, basePaths.getOrDefault("BODY", ""));
            safeSetContent(sleeves, basePaths.getOrDefault("SLEEVES", ""));
        }

        // SYNC OUTLINES
        safeSetContent(bodyOutline, body.getContent());
        safeSetContent(sleevesOutline, sleeves.getContent());

        if (!body.getContent().isEmpty() || !sleeves.getContent().isEmpty()) {
            // Shadow
            java.util.Map<String, String> shadowPaths = org.example.utils.SVGCache
                    .loadCategorizedPaths(path.replace(".svg", "_sombra.svg"));

            String separateSleevesShadowPath = path.replace(".svg", "_mangas_sombra.svg");
            String externalSShadowCont = org.example.utils.SVGCache.loadOptionalPath(separateSleevesShadowPath);

            if (!externalSShadowCont.isEmpty()) {
                safeSetContent(bodyShadow,
                        shadowPaths.getOrDefault("BODY", "") + " " + shadowPaths.getOrDefault("SLEEVES", ""));
                safeSetContent(sleevesShadow, externalSShadowCont);
            } else {
                safeSetContent(bodyShadow, shadowPaths.getOrDefault("BODY", ""));
                safeSetContent(sleevesShadow, shadowPaths.getOrDefault("SLEEVES", ""));
            }

            // Detail
            java.util.Map<String, String> detailPaths = org.example.utils.SVGCache
                    .loadCategorizedPaths(path.replace(".svg", "_detalle.svg"));

            String separateSleevesDetailPath = path.replace(".svg", "_mangas_detalle.svg");
            String externalSDetailCont = org.example.utils.SVGCache.loadOptionalPath(separateSleevesDetailPath);

            if (!externalSDetailCont.isEmpty()) {
                safeSetContent(bodyDetail,
                        detailPaths.getOrDefault("BODY", "") + " " + detailPaths.getOrDefault("SLEEVES", ""));
                safeSetContent(sleevesDetail, externalSDetailCont);
            } else {
                safeSetContent(bodyDetail, detailPaths.getOrDefault("BODY", ""));
                safeSetContent(sleevesDetail, detailPaths.getOrDefault("SLEEVES", ""));
            }
        } else {
            safeSetContent(bodyShadow, "");
            safeSetContent(sleevesShadow, "");
            safeSetContent(bodyDetail, "");
            safeSetContent(sleevesDetail, "");
        }
    }

    @Override
    public void applyColors(Map<String, Color> colorState) {
        Color strokeColor = Color.web("#2c3e50"); // Default dark stroke

        // 1. Calculate Adaptive Stroke based on Body Color
        if (colorState.containsKey("body")) {
            Color c = colorState.get("body");
            Color sanitized = sanitizeFillColor(c);
            body.setFill(sanitized);

            // Adaptive Stroke Logic
            strokeColor = getContrastStroke(sanitized);
            body.setStroke(strokeColor);
            bodyOutline.setStroke(strokeColor); // Update the overlay outline too

            backingLayer.setVisible(false);
        }

        if (colorState.containsKey("brandChest")) {
            brandBase.setFill(sanitizeFillColor(colorState.get("brandChest")));
        }

        if (colorState.containsKey("sleeves")) {
            Color c = colorState.get("sleeves");
            Color sanitized = sanitizeFillColor(c);
            sleeves.setFill(sanitized);
            // Sleeves might need their own contrast if different from body,
            // but usually we want a unified look. Let's calculate per part.
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

        if (colorState.containsKey("shirtStripe")) {
            Color c = colorState.get("shirtStripe");
            shirtStripe.setFill(sanitizeFillColor(c));
            shirtStripe.setStroke(getContrastStroke(c));
        }

// Tela Natural: collarDetail follows body color for V and REDONDO (except dark colors)
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

        // Canezu color: lighter gray for dark fabrics (but not black)
        Color bodyColorForCanezu = colorState.get("body");
        if (bodyColorForCanezu != null && isDarkColor(bodyColorForCanezu) && !isBlackColor(bodyColorForCanezu)) {
            canezuLayer.setFill(Color.web("#888888")); // Lighter gray for dark fabrics
        } else {
            canezuLayer.setFill(Color.web("#4a4a4a")); // Dark gray for light/black fabrics
        }
    }

    /**
     * Specifically used by Goalkeeper Design to apply a single reference color
     * to all primary parts of the jersey.
     */
    public void applyReferenceColor(Color color) {
        Color sanitized = sanitizeFillColor(color);
        Color stroke = getContrastStroke(sanitized);
        
        body.setFill(sanitized);
        body.setStroke(stroke);
        bodyOutline.setStroke(stroke);
        
        sleeves.setFill(sanitized);
        sleeves.setStroke(stroke);
        sleevesOutline.setStroke(stroke);
        
        collar.setFill(sanitized);
        collar.setStroke(stroke);
        
        cuffs.setFill(sanitized);
        cuffs.setStroke(stroke);
        
        // Ensure detail layers are visible and have a contrast color if enabled
        if (mesh.getContent() != null && !mesh.getContent().isEmpty()) {
            mesh.setFill(sanitized.darker());
            mesh.setStroke(stroke);
        }
        if (shirtStripe.getContent() != null && !shirtStripe.getContent().isEmpty()) {
            shirtStripe.setFill(sanitized.brighter());
            shirtStripe.setStroke(stroke);
        }
        
        backingLayer.setVisible(false);

        boolean isVOrRedondoRef = "V".equalsIgnoreCase(currentCollarType) || "REDONDO".equalsIgnoreCase(currentCollarType);
        if (telaNatural && isVOrRedondoRef && !isDarkColor(sanitized)) {
            collarDetail.setFill(sanitized);
        } else {
            collarDetail.setFill(Color.WHITE);
        }

        if (telaNatural && isDarkColor(sanitized) && !isBlackColor(sanitized)) {
            canezuLayer.setFill(Color.web("#888888"));
        } else {
            canezuLayer.setFill(Color.web("#4a4a4a"));
        }
    }

    protected Color getContrastStroke(Color fill) {
        // Enforce Strict Black Outline as per user/company requirement.
        return Color.BLACK;
    }

    public void setMeshVisible(boolean visible) {
        mesh.setVisible(visible);
    }

    public void setCuffsVisible(boolean visible) {
        cuffs.setVisible(visible);
    }

    public void setStripeVisible(boolean visible) {
        shirtStripe.setVisible(visible);
    }

    public void updateBranding(boolean visible, String basePath, String detailPath) {
        brandBase.setVisible(visible);
        // brandDetail is kept invisible, its content is merged into brandBase
        if (visible) {
            String baseContent = org.example.utils.SVGCache.loadPath(basePath.toLowerCase());
            String detailContent = org.example.utils.SVGCache.loadPath(detailPath.toLowerCase());

            // Merge paths to create true holes using EVEN_ODD
            safeSetContent(brandBase, baseContent + " " + detailContent);

            // Respect factory alignment (ViewBox)
            brandBase.setLayoutX(0);
            brandBase.setLayoutY(0);
            brandBase.setScaleX(1.0);
            brandBase.setScaleY(1.0);
            brandDetail.setLayoutX(0);
            brandDetail.setLayoutY(0);
            brandDetail.setScaleX(1.0);
            brandDetail.setScaleY(1.0);
        }
    }

    protected void configureOutlineLayer(SVGPath path, Color strokeColor) {
        path.setFill(Color.TRANSPARENT);
        path.setStroke(strokeColor);
        path.setStrokeWidth(1);
        path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setMouseTransparent(true);
    }

    public SVGPath getBody() {
        return body;
    }

    @Override
    public SVGPath getSleeves() {
        return sleeves;
    }

    @Override
    public SVGPath getBrandBase() {
        return brandBase;
    }

    @Override
    public SVGPath getBrandDetail() {
        return brandDetail;
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
}

