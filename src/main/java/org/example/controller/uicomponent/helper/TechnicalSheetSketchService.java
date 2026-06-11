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
            Image arqSketchHombre, Image arqSketchMujer, ConfiguracionPrendaDTO arqueroConfig) {

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

        StackPane sketchPane = new StackPane();
        sketchPane.setMinHeight(sketchHeight);
        sketchPane.setPrefWidth(550);
        sketchPane.getStyleClass().add("ficha-sketch-pane");

        if (arqSketchHombre != null) {
            ImageView iv = new ImageView(arqSketchHombre);
            iv.setPreserveRatio(true);
            iv.setFitWidth(arqSketchMujer != null ? 260 : 530);
            iv.setFitHeight(sketchHeight - 20);
            sketchPane.getChildren().add(iv);
            if (arqSketchMujer != null) StackPane.setAlignment(iv, Pos.CENTER_LEFT);
        }
        if (arqSketchMujer != null) {
            ImageView iv = new ImageView(arqSketchMujer);
            iv.setPreserveRatio(true);
            iv.setFitWidth(arqSketchHombre != null ? 260 : 530);
            iv.setFitHeight(sketchHeight - 20);
            sketchPane.getChildren().add(iv);
            if (arqSketchHombre != null) StackPane.setAlignment(iv, Pos.CENTER_RIGHT);
        }

        VBox extrasBox = new VBox(8);
        extrasBox.setPadding(new Insets(5));
        Label lblSpecs = new Label("ESPECIFICACIONES:");
        lblSpecs.getStyleClass().add("ficha-specs-label");
        extrasBox.getChildren().add(lblSpecs);

        DetallePedido arcP = allArqueros.get(0);
        VBox card = new VBox(5);
        card.getStyleClass().add("ficha-card");
        
        HBox colorRow = new HBox(8, new Rectangle(18, 18, arcP.getColorArquero() != null ? arcP.getColorArquero() : Color.WHITE),
                new Label(TechnicalSheetDataService.resolveColorName(arcP.getColorArquero()) + " - " + resolveArqueroSleeve(arcP)));
        ((Rectangle)colorRow.getChildren().get(0)).setStroke(Color.GRAY);
        colorRow.getChildren().get(1).getStyleClass().add("ficha-color-row-label");
        colorRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(colorRow);

        if (arqueroConfig != null && arqueroConfig.llevaAcolchado()) {
            Label lblAco = new Label("SI LLEVA ACOLCHADO");
            lblAco.getStyleClass().add("ficha-badge-acolchado");
            card.getChildren().add(lblAco);
        }
        extrasBox.getChildren().add(card);

        VBox arqSummary = new VBox(4);
        arqSummary.setPadding(new Insets(8, 0, 0, 0));
        Label lblSum = new Label("RESUMEN DE PRODUCCIÓN:");
        lblSum.getStyleClass().add("ficha-summary-label");
        arqSummary.getChildren().add(lblSum);

        Map<String, Integer> tSizes = new TreeMap<>(), bSizes = new TreeMap<>();
        for (DetallePedido p : allArqueros) {
            if (p.isIncludeTop()) tSizes.put(TechnicalSheetDataService.getSizeDisplay(p), tSizes.getOrDefault(TechnicalSheetDataService.getSizeDisplay(p), 0) + 1);
            if (p.isIncludeBottom()) bSizes.put(p.getTallaShort() != null ? p.getTallaShort().toUpperCase() : TechnicalSheetDataService.getSizeDisplay(p), bSizes.getOrDefault(p.getTallaShort(), 0) + 1);
        }

        if (!tSizes.isEmpty()) {
            Label l = new Label("POLOS: " + tSizes.entrySet().stream().map(e -> e.getKey() + "(" + e.getValue() + ")").collect(Collectors.joining(", ")));
            l.getStyleClass().add("ficha-summary-shirt");
            arqSummary.getChildren().add(l);
        }
        if (!bSizes.isEmpty()) {
            Label l = new Label("SHORTS: " + bSizes.entrySet().stream().map(e -> e.getKey() + "(" + e.getValue() + ")").collect(Collectors.joining(", ")));
            l.getStyleClass().add("ficha-summary-shorts");
            arqSummary.getChildren().add(l);
        }
        extrasBox.getChildren().add(arqSummary);

        HBox layout = new HBox(15, sketchPane, extrasBox);
        layout.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(layout);

        VBox arqTableBox = new VBox(5, new Label("TALLERO ESPECIAL ARQUEROS:"));
        arqTableBox.getChildren().get(0).getStyleClass().add("ficha-table-title-small");
        arqTableBox.getChildren().add(TechnicalSheetPaginationManager.createSmartTable(allArqueros, null, false).get(0).node());
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
