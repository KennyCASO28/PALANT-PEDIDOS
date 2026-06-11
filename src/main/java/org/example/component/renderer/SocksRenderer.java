package org.example.component.renderer;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.example.logic.GarmentAssetManager;
import org.example.model.TipoGenero;
import org.example.utils.SVGCache;
import java.util.Map;

/**
 * Specialized renderer for the Socks.
 */
public class SocksRenderer extends BaseGarmentRenderer {
    private final SVGPath socks = new SVGPath();
    private final SVGPath socksTop = new SVGPath();
    private final SVGPath socksGround = new SVGPath();
    private final SVGPath socksSole = new SVGPath();
    private final SVGPath socksShadow = new SVGPath();
    private final SVGPath socksDetail = new SVGPath();
    private final SVGPath brandBase = new SVGPath();
    private final SVGPath brandDetail = new SVGPath();

    public SocksRenderer() {
        Color strokeColor = Color.web("#2c3e50");
        configureLayer(socks, Color.web("#ecf0f1"), strokeColor);
        configureShadowLayer(socksGround);
        configureLayer(socksSole, Color.web("#95a5a6"), strokeColor);
        configureShadowLayer(socksShadow);
        configureDetailLayer(socksDetail, strokeColor);

        // Top Band (Liga Superior) - Default config
        configureLayer(socksTop, Color.web("#ecf0f1"), null);
        socksTop.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        // Fix Spikes
        socks.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        socksSole.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        socksDetail.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        // Brand layers
        configureLayer(brandBase, Color.WHITE, null); // Base can have color
        brandBase.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        configureDetailLayer(brandDetail, strokeColor);
        brandDetail.setVisible(false);

        // Correct Layer Order:
        // GROUP 1: Base
        group.getChildren().addAll(socksGround, socks);

        // GROUP 2: Detail
        detailGroup.getChildren().addAll(socksTop, brandBase, brandDetail, socksSole, socksShadow, socksDetail);
    }

    @Override
    public void updateLayers(String gender, String cut, String length, String collar) {
        // Use default update with TipoGenero
    }

    public void updateSocks(TipoGenero genero) {
        String path = GarmentAssetManager.getSocksPath(genero);
        safeSetContent(socks, SVGCache.loadPath(path));
        safeSetContent(socksGround, SVGCache.loadOptionalPath(path.replace(".svg", "_piso.svg")));
        safeSetContent(socksSole, SVGCache.loadOptionalPath(path.replace(".svg", "_suela.svg")));
        safeSetContent(socksShadow, SVGCache.loadOptionalPath(path.replace(".svg", "_sombra.svg")));
        safeSetContent(socksDetail, SVGCache.loadOptionalPath(path.replace(".svg", "_detalle.svg")));

        // Load Top Band
        String topPath = GarmentAssetManager.getSocksTopPath(genero);
        if (topPath != null) {
            safeSetContent(socksTop, SVGCache.loadPath(topPath));
        }
    }

    public void setSocksTopVisible(boolean visible) {
        socksTop.setVisible(visible);
    }

    public void updateBranding(boolean visible, String basePath, String detailPath) {
        brandBase.setVisible(visible);
        // brandDetail hidden and merged
        if (visible) {
            String baseCont = SVGCache.loadPath(basePath);
            String detailCont = SVGCache.loadPath(detailPath);

            // Merge paths for true hole
            safeSetContent(brandBase, baseCont + " " + detailCont);

            // Respect factory alignment (ViewBox)
            brandBase.setLayoutX(0);
            brandBase.setLayoutY(0);
            brandBase.setScaleX(1.0);
            brandBase.setScaleY(1.0);
        }
    }

    @Override
    public void applyColors(Map<String, Color> colorState) {
        Color strokeColor = Color.BLACK;
        Color socksColor = Color.WHITE; // Default

        if (colorState.containsKey("socks")) {
            socksColor = sanitizeFillColor(colorState.get("socks"));
        }
        socks.setFill(socksColor);
        socks.setStroke(strokeColor);

        Color soleC = colorState.getOrDefault("socksSole", Color.web("#95a5a6"));
        socksSole.setFill(sanitizeFillColor(soleC));
        socksSole.setStroke(strokeColor);

        Color detailC = colorState.getOrDefault("socksDetail", Color.BLACK);
        socksDetail.setFill(sanitizeFillColor(detailC));
        socksDetail.setStroke(Color.BLACK);

        if (colorState.containsKey("socksTop")) {
            socksTop.setFill(sanitizeFillColor(colorState.get("socksTop")));
            socksTop.setStroke(null);
        } else {
            if (socksTop.isVisible()) {
                socksTop.setFill(socksColor);
                socksTop.setStroke(null);
            }
        }

        // Apply Brand Color
        Color brandC = colorState.getOrDefault("brandSocks", Color.WHITE);
        brandBase.setFill(brandC);
    }

    public void applyReferenceColor(Color color) {
        Color sanitized = sanitizeFillColor(color);
        socks.setFill(sanitized);
        socks.setStroke(Color.BLACK);
        
        if (socksTop.getContent() != null && !socksTop.getContent().isEmpty()) {
            socksTop.setFill(sanitized);
            socksTop.setStroke(null);
        }
    }

    @Override
    public SVGPath getBody() {
        return socks;
    }

    public SVGPath getSocks() {
        return socks;
    }

    @Override
    public SVGPath getBrandBase() {
        return brandBase;
    }

    @Override
    public SVGPath getBrandDetail() {
        return brandDetail;
    }

    public SVGPath getSocksSole() {
        return socksSole;
    }
}

