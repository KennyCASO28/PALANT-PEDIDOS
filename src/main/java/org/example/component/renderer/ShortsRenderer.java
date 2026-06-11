package org.example.component.renderer;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.example.logic.GarmentAssetManager;
import org.example.model.TipoGenero;
import org.example.model.TipoCorte;
import org.example.utils.SVGCache;
import java.util.Map;

/**
 * Specialized renderer for the Shorts.
 */
public class ShortsRenderer extends BaseGarmentRenderer {
    private final SVGPath shorts = new SVGPath();
    private final SVGPath shortsStripe = new SVGPath();
    private final SVGPath shortsPicket = new SVGPath();
    private final SVGPath shortsWaist = new SVGPath();
    private final SVGPath shortsElastic = new SVGPath();
    private final SVGPath shortsCuff = new SVGPath();
    private final SVGPath shortsCord = new SVGPath();
    private final SVGPath shortsShadow = new SVGPath();
    private final SVGPath shortsDetail = new SVGPath();
    private final SVGPath shortCrest = new SVGPath();
    private final SVGPath brandBase = new SVGPath();
    private final SVGPath brandDetail = new SVGPath();

    public ShortsRenderer() {
        Color baseColor = Color.WHITE;
        Color strokeColor = Color.web("#2c3e50");
        Color leadColor = Color.web("#7f8c8d");

        configureLayer(shorts, baseColor, strokeColor);
        configureDetailLayer(shortsStripe, leadColor);
        configureLayer(shortsPicket, leadColor, strokeColor);
        configureDetailLayer(shortsWaist, Color.BLACK);
        configureDetailLayer(shortsElastic, Color.BLACK);
        configureLayer(shortsCuff, leadColor, Color.BLACK);

        configureDetailLayer(shortsCord, Color.WHITE);
        shortsCord.setStroke(leadColor);
        shortsCord.setStrokeWidth(0.5);

        configureShadowLayer(shortsShadow);
        configureDetailLayer(shortsDetail, Color.BLACK);

        configureLayer(brandBase, Color.BLACK, null);
        brandBase.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        configureDetailLayer(brandDetail, Color.WHITE);
        brandDetail.setVisible(false);

        // GROUP 1: Base (for clipping/background)
        group.getChildren().addAll(shorts);

        // GROUP 2: Details (On Top)
        detailGroup.getChildren().addAll(
                shortsStripe, shortsPicket, shortsWaist, shortsElastic,
                shortsCuff, shortsCord, shortsShadow, shortsDetail, shortCrest,
                brandBase, brandDetail);
    }

    @Override
    public void updateLayers(String gender, String cut, String length, String collar) {
    }

    public void updateShorts(TipoGenero genero, TipoCorte corteShort, boolean hasStripe, boolean hasPicket,
            boolean hasCuff, boolean hasCord, boolean hasPocket) {
        String basePath = GarmentAssetManager.getShortsPath(genero, corteShort);
        safeSetContent(shorts, SVGCache.loadPath(basePath));

        safeSetContent(shortsStripe, hasStripe ? SVGCache.loadOptionalPath(basePath.replace(".svg", "_franja.svg")) : "");
        safeSetContent(shortsPicket, hasPicket ? SVGCache.loadOptionalPath(basePath.replace(".svg", "_piquete.svg")) : "");
        safeSetContent(shortsWaist, SVGCache.loadOptionalPath(basePath.replace(".svg", "_cintura.svg")));
        safeSetContent(shortsCuff, hasCuff ? SVGCache.loadOptionalPath(basePath.replace(".svg", "_puno.svg")) : "");
        safeSetContent(shortsElastic, SVGCache.loadOptionalPath(basePath.replace(".svg", "_elastico.svg")));
        safeSetContent(shortsCord, hasCord ? SVGCache.loadOptionalPath(basePath.replace(".svg", "_pasador.svg")) : "");
        safeSetContent(shortsShadow, SVGCache.loadOptionalPath(basePath.replace(".svg", "_sombra.svg")));
        safeSetContent(shortsDetail, hasPocket ? SVGCache.loadOptionalPath(basePath.replace(".svg", "_bolsillo.svg")) : "");
    }

    @Override
    public void applyColors(Map<String, Color> colorState) {
        Color c = colorState.getOrDefault("shorts", Color.WHITE);
        Color sanitized = sanitizeFillColor(c);
        shorts.setFill(sanitized);
        shorts.setStroke(getContrastStroke(sanitized));

        if (colorState.containsKey("brandShort")) {
            brandBase.setFill(sanitizeFillColor(colorState.get("brandShort")));
        }

        Color stripeC = colorState.getOrDefault("shortsStripe", Color.WHITE);
        shortsStripe.setFill(sanitizeFillColor(stripeC));
        shortsStripe.setStroke(getContrastStroke(stripeC));

        Color picketC = colorState.getOrDefault("shortsPicket", Color.WHITE);
        shortsPicket.setFill(sanitizeFillColor(picketC));
        shortsPicket.setStroke(getContrastStroke(picketC));

        Color cuffC = colorState.getOrDefault("shortsCuff", Color.WHITE);
        shortsCuff.setFill(sanitizeFillColor(cuffC));
        shortsCuff.setStroke(getContrastStroke(cuffC));

        Color cordC = colorState.getOrDefault("shortsCord", Color.WHITE);
        shortsCord.setFill(sanitizeFillColor(cordC));
        shortsCord.setStroke(Color.web("#7f8c8d"));
    }

    protected Color getContrastStroke(Color fill) {
        return Color.BLACK;
    }

    public void updateBranding(boolean visible, String basePath, String detailPath) {
        brandBase.setVisible(visible);
        if (visible) {
            String baseContent = SVGCache.loadPath(basePath.toLowerCase());
            String detailContent = SVGCache.loadPath(detailPath.toLowerCase());
            safeSetContent(brandBase, baseContent + " " + detailContent);

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

    public void setCrest(String svgContent) {
        safeSetContent(shortCrest, svgContent);
        shortCrest.setVisible(svgContent != null && !svgContent.isEmpty());
    }

    public void applyReferenceColor(Color color) {
        Color sanitized = sanitizeFillColor(color);
        shorts.setFill(sanitized);
        shorts.setStroke(Color.BLACK);
        
        if (shortsStripe.getContent() != null && !shortsStripe.getContent().isEmpty()) {
            shortsStripe.setFill(sanitized);
            shortsStripe.setStroke(Color.BLACK);
        }
    }

    @Override
    public SVGPath getBody() {
        return shorts;
    }

    public SVGPath getShorts() {
        return shorts;
    }

    @Override
    public SVGPath getBrandBase() {
        return brandBase;
    }

    @Override
    public SVGPath getBrandDetail() {
        return brandDetail;
    }

    public SVGPath getShortsCord() {
        return shortsCord;
    }
}
