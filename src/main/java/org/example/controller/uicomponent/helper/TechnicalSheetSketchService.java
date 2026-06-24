package org.example.controller.uicomponent.helper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.component.PrendaVisualizer;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.dto.DatosEnvioDTO;
import org.example.model.DetallePedido;
import org.example.model.PrendaState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Service to generate visual components and sketches for the technical sheet.
 */
public class TechnicalSheetSketchService {

    private static final double PAGE_CONTENT_WIDTH = 754;

    public static StackPane createVisualWithOverlays(Node visualNode) {
        StackPane stack = new StackPane(visualNode);
        stack.setAlignment(Pos.CENTER);
        stack.setMinWidth(PAGE_CONTENT_WIDTH);
        stack.setPrefWidth(PAGE_CONTENT_WIDTH);
        stack.setMaxWidth(PAGE_CONTENT_WIDTH);
        return stack;
    }

    public static TechnicalSheetPaginationManager.RosterChunk createArqueroBocetoSection(
            List<DetallePedido> jugadores, PrendaVisualizer visualizer,
            Image arqSketchHombre, Image arqSketchMujer, ConfiguracionPrendaDTO arqueroConfig,
            java.util.Map<String, org.example.dto.save.GoalkeeperDesignDTO> disenosArquero) {

        List<DetallePedido> allArqueros = (jugadores == null) ? new ArrayList<>() :
                jugadores.stream().filter(DetallePedido::isEsArquero)
                        .sorted(Comparator.comparingInt(p -> p.getArqueroOrdenMarcado() > 0 ? p.getArqueroOrdenMarcado() : Integer.MAX_VALUE))
                        .collect(Collectors.toList());

        if (allArqueros.isEmpty()) return null;

        VBox container = new VBox(8);
        container.setPadding(new Insets(10));
        container.getStyleClass().add("ficha-container-box");
        container.setMinWidth(PAGE_CONTENT_WIDTH);

        container.getChildren().add(new Label("ARQUERO (BOCETO)"));
        container.getChildren().get(0).getStyleClass().add("ficha-title-14");

        boolean isFullSet = arqueroConfig != null && (arqueroConfig.llevaShort() || arqueroConfig.llevaMedias());
        double sketchHeight = isFullSet ? 420 : 360;

        HBox sketchBox = new HBox(15);
        sketchBox.setPrefHeight(sketchHeight);

        if (arqSketchHombre != null && arqSketchMujer != null) {
            StackPane leftPane = new StackPane();
            leftPane.setMinSize(260.0, sketchHeight - 20);
            leftPane.setPrefSize(260.0, sketchHeight - 20);
            leftPane.setMaxSize(260.0, sketchHeight - 20);

            ImageView ivH = new ImageView(arqSketchHombre);
            ivH.setPreserveRatio(true);
            ivH.setFitWidth(260.0);
            ivH.setFitHeight(sketchHeight - 20);
            ivH.setSmooth(true);
            ivH.setCache(false);
            StackPane.setAlignment(ivH, Pos.CENTER);
            leftPane.getChildren().add(ivH);
            sketchBox.getChildren().add(leftPane);

            StackPane rightPane = new StackPane();
            rightPane.setMinSize(260.0, sketchHeight - 20);
            rightPane.setPrefSize(260.0, sketchHeight - 20);
            rightPane.setMaxSize(260.0, sketchHeight - 20);

            ImageView ivM = new ImageView(arqSketchMujer);
            ivM.setPreserveRatio(true);
            ivM.setFitWidth(260.0);
            ivM.setFitHeight(sketchHeight - 20);
            ivM.setSmooth(true);
            ivM.setCache(false);
            StackPane.setAlignment(ivM, Pos.CENTER);
            rightPane.getChildren().add(ivM);
            sketchBox.getChildren().add(rightPane);
        } else if (arqSketchHombre != null) {
            StackPane leftPane = new StackPane();
            leftPane.setMinSize(530.0, sketchHeight - 20);
            leftPane.setPrefSize(530.0, sketchHeight - 20);
            leftPane.setMaxSize(530.0, sketchHeight - 20);

            ImageView ivH = new ImageView(arqSketchHombre);
            ivH.setPreserveRatio(true);
            ivH.setFitWidth(530.0);
            ivH.setFitHeight(sketchHeight - 20);
            ivH.setSmooth(true);
            ivH.setCache(false);
            StackPane.setAlignment(ivH, Pos.CENTER);
            leftPane.getChildren().add(ivH);
            sketchBox.getChildren().add(leftPane);
        } else if (arqSketchMujer != null) {
            StackPane rightPane = new StackPane();
            rightPane.setMinSize(530.0, sketchHeight - 20);
            rightPane.setPrefSize(530.0, sketchHeight - 20);
            rightPane.setMaxSize(530.0, sketchHeight - 20);

            ImageView ivM = new ImageView(arqSketchMujer);
            ivM.setPreserveRatio(true);
            ivM.setFitWidth(530.0);
            ivM.setFitHeight(sketchHeight - 20);
            ivM.setSmooth(true);
            ivM.setCache(false);
            StackPane.setAlignment(ivM, Pos.CENTER);
            rightPane.getChildren().add(ivM);
            sketchBox.getChildren().add(rightPane);
        }

        VBox extrasBox = new VBox(8);
        extrasBox.setPadding(new Insets(5));
        extrasBox.setAlignment(Pos.TOP_CENTER);
        extrasBox.setMinWidth(170);
        extrasBox.setPrefWidth(170);
        Label lblSpecs = new Label("ESPECIFICACIONES:");
        lblSpecs.getStyleClass().add("ficha-specs-label");
        extrasBox.getChildren().add(lblSpecs);

        for (DetallePedido arcP : allArqueros) {
            VBox card = new VBox(5);
            card.getStyleClass().add("ficha-card");

            String colorName = TechnicalSheetDataService.resolveColorName(arcP.getColorArquero());
            String manga = resolveArqueroSleeve(arcP);
            String title = "Arquero " + arcP.getArqueroOrdenMarcado() + " (" + colorName + " - " + manga + ")";
            Label lblTitle = new Label(title);
            lblTitle.setWrapText(true);
            lblTitle.setPrefWidth(160); // Constrain to force wrapping
            lblTitle.setMinHeight(Region.USE_PREF_SIZE);

            HBox colorRow = new HBox(8, new Rectangle(18, 18, arcP.getColorArquero() != null ? arcP.getColorArquero() : Color.WHITE),
                    lblTitle);
            ((Rectangle)colorRow.getChildren().get(0)).setStroke(Color.GRAY);
            colorRow.getChildren().get(1).getStyleClass().add("ficha-color-row-label-small"); // Uses a slightly smaller class if we want, or same
            colorRow.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(colorRow);

            String designId = arcP.getArqueroDesignId();
            org.example.dto.save.GoalkeeperDesignDTO dto = disenosArquero != null ? disenosArquero.get(designId) : null;
            if (dto != null && dto.getGarmentConfig() != null && dto.getGarmentConfig().isHasPadding()) {
                Label lblAco = new Label("SI LLEVA ACOLCHADO");
                lblAco.getStyleClass().add("ficha-badge-acolchado");
                card.getChildren().add(lblAco);
            } else if (dto == null && arqueroConfig != null && arqueroConfig.llevaAcolchado()) {
                // Fallback for backward compatibility or when missing
                Label lblAco = new Label("SI LLEVA ACOLCHADO");
                lblAco.getStyleClass().add("ficha-badge-acolchado");
                card.getChildren().add(lblAco);
            }
            extrasBox.getChildren().add(card);
        }

        HBox layout = new HBox(15, sketchBox, extrasBox);
        layout.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(layout);

        VBox arqTableBox = new VBox(5, new Label("TALLERO ESPECIAL ARQUEROS:"));
        arqTableBox.getChildren().get(0).getStyleClass().add("ficha-table-title-small");
        List<TechnicalSheetPaginationManager.RosterChunk> smartTableChunks = TechnicalSheetPaginationManager.createSmartTable(allArqueros, null, false, TechnicalSheetPaginationManager.PAGE_CONTENT_WIDTH - 30);
        if (!smartTableChunks.isEmpty()) {
            arqTableBox.getChildren().add(smartTableChunks.get(0).node());
        }
        container.getChildren().add(arqTableBox);

        return new TechnicalSheetPaginationManager.RosterChunk(container, (isFullSet ? 370 : 310) + (allArqueros.size() * 26) + 100);
    }

    public static TechnicalSheetPaginationManager.RosterChunk createTechnicalReferencesSection(PrendaVisualizer visualizer, List<Image> referencias) {
        PrendaState targetState = (visualizer != null) ? (visualizer.getArqueroState() != null && !visualizer.getArqueroState().getReferenceHotspots().isEmpty() ? visualizer.getArqueroState() : visualizer.getState()) : null;
        if (targetState == null || targetState.getReferenceHotspots().isEmpty()) return null;

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.getStyleClass().add("ficha-container-box");
        container.setMinWidth(PAGE_CONTENT_WIDTH);

        Label lbl = new Label("REFERENCIAS TÉCNICAS");
        lbl.getStyleClass().add("ficha-title-14");
        container.getChildren().add(lbl);

        FlowPane flow = new FlowPane(10, 10);
        flow.setPrefWrapLength(PAGE_CONTENT_WIDTH - 20);

        List<PrendaState.ReferenceHotspot> hotspots = targetState.getReferenceHotspots();
        for (int i = 0; i < hotspots.size() && i < referencias.size(); i++) {
            PrendaState.ReferenceHotspot hs = hotspots.get(i);
            VBox item = new VBox(6, new ImageView(referencias.get(i)), new Label(hs.getLabel()));
            item.setAlignment(Pos.CENTER);
            item.getStyleClass().add("ficha-ref-item");
            ((ImageView)item.getChildren().get(0)).setPreserveRatio(true);
            ((ImageView)item.getChildren().get(0)).setFitHeight(185);
            ((ImageView)item.getChildren().get(0)).setFitWidth(235);
            item.getChildren().get(1).getStyleClass().add("ficha-ref-label");
            flow.getChildren().add(item);
        }
        container.getChildren().add(flow);
        return new TechnicalSheetPaginationManager.RosterChunk(container, 50 + (Math.ceil(hotspots.size() / 3.0) * 230) + 50);
    }

    public static VBox createShippingSection(DatosEnvioDTO info) {
        if (info == null || info.isEmpty()) return null;
        VBox box = new VBox(0);
        box.setMinWidth(PAGE_CONTENT_WIDTH);
        box.getStyleClass().add("ficha-shipping-section");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(18));
        Label title = new Label("DATOS DE ENVÍO");
        title.getStyleClass().add("ficha-shipping-title");
        content.getChildren().add(title);

        GridPane grid = new GridPane();
        grid.getColumnConstraints().addAll(new ColumnConstraints(150), new ColumnConstraints(420, -1, -1, Priority.ALWAYS, null, true));
        
        String[] keys = {"Cliente", "DNI", "Celular", "Lugar de envío"};
        String[] values = {info.getNombreCompleto(), info.getDni(), info.getCelular(), info.getLugarEnvio()};
        for(int i=0; i<4; i++) {
            Label k = new Label(keys[i] + ":"), v = new Label(values[i] == null || values[i].isBlank() ? "-" : values[i]);
            k.getStyleClass().add("ficha-shipping-label-key");
            v.getStyleClass().add("ficha-shipping-label-value");
            StackPane pk = new StackPane(k), pv = new StackPane(v);
            pk.getStyleClass().add("ficha-shipping-grid-key");
            pk.setStyle("-fx-border-width: " + (i==0?1:0) + " 0 1 1;");
            pv.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-border-color: #cbd5e1; -fx-border-width: " + (i==0?1:0) + " 1 1 0;");
            grid.add(pk, 0, i); grid.add(pv, 1, i);
        }
        
        Region bar = new Region(); bar.setPrefHeight(6); bar.getStyleClass().add("ficha-shipping-top-bar");
        content.getChildren().add(grid);
        box.getChildren().addAll(bar, content);
        return box;
    }

    private static String resolveArqueroSleeve(DetallePedido p) {
        String m = (p.getTipoMangaArquero() != null && !p.getTipoMangaArquero().isBlank()) ? p.getTipoMangaArquero() : p.getTipoManga();
        return (m == null || m.isBlank()) ? "CORTA" : m.trim().toUpperCase();
    }
}
