package org.example.controller.uicomponent;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.DetallePedido;
import org.example.component.PrendaVisualizer;
import org.example.controller.uicomponent.helper.TechnicalSheetDataService;
import org.example.controller.uicomponent.helper.TechnicalSheetPaginationManager;
import org.example.controller.uicomponent.helper.TechnicalSheetSketchService;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Orchestrator for Technical Sheet view.
 * Delegated responsibilities to:
 * - TechnicalSheetDataService: Data processing and summaries.
 * - TechnicalSheetPaginationManager: A4 layout and pagination.
 * - TechnicalSheetSketchService: Visual sketches and references.
 */
public class FichaTecnicaView {

    private static final double A4_WIDTH = TechnicalSheetPaginationManager.A4_WIDTH;
    private static final double PAGE_CONTENT_WIDTH = TechnicalSheetPaginationManager.PAGE_CONTENT_WIDTH;
    private static final double EPIC_CONTENT_WIDTH = A4_WIDTH - 24;

    public static javafx.scene.layout.Region build(ConfiguracionPrendaDTO config, Image mainSnapshot,
            List<org.example.dto.save.LayerDTO> mainLayers, PrendaVisualizer visualizer, 
            List<DetallePedido> jugadores, String cliente,
            String codigo, List<org.example.service.PdfExportService.ShieldEntry> shieldEntries, LocalDate fechaLocalDate,
            String prioridad, String shortType, String vendedor, List<Image> referencias,
            org.example.dto.DatosEnvioDTO shippingInfo, Image arqSketchHombre,
            Image arqSketchMujer,
            ConfiguracionPrendaDTO arqueroConfig) {

        try {
            // Wrapper for Centering
            StackPane outerContainer = new StackPane();
            outerContainer.getStyleClass().add("ficha-outer-container");
            outerContainer.setPadding(new Insets(20));
            outerContainer.setAlignment(Pos.TOP_CENTER);

            VBox pagesContainer = new VBox(20);
            pagesContainer.setAlignment(Pos.TOP_CENTER);
            pagesContainer.setMinWidth(A4_WIDTH + 6);

            // --- PAGE 1: PRENDA & SPECS ---
            VBox page1 = TechnicalSheetPaginationManager.createPageContainer();

            boolean hasArqueros = jugadores != null && jugadores.stream().anyMatch(DetallePedido::isEsArquero);
            boolean densePageOne = TechnicalSheetDataService.isDensePageOne(config, shieldEntries) || hasArqueros;

            // 1. Header
            HBox header = TechnicalSheetPaginationManager.createHeader(cliente, codigo, fechaLocalDate, prioridad, vendedor);
            page1.getChildren().add(header);

            VBox contentWrapper = new VBox(0);
            contentWrapper.setId("content-page");
            contentWrapper.setAlignment(Pos.CENTER);
            contentWrapper.setMinWidth(PAGE_CONTENT_WIDTH);
            contentWrapper.setSpacing(densePageOne ? 6 : 10);

            // --- UNIFIED TECHNICAL FRAME ---
            VBox unifiedFrame = new VBox(0);
            unifiedFrame.setAlignment(Pos.TOP_CENTER);
            unifiedFrame.setMinWidth(EPIC_CONTENT_WIDTH);
            unifiedFrame.getStyleClass().add("ficha-unified-frame");

            // 1. Top Totals Summary
            if (jugadores != null && !jugadores.isEmpty()) {
                unifiedFrame.getChildren().add(TechnicalSheetDataService.createSummaryWidget(jugadores));
            }

            // 2. Visual Box
            VBox imageBox = new VBox();
            imageBox.setAlignment(Pos.CENTER);
            imageBox.setMinWidth(EPIC_CONTENT_WIDTH);
            imageBox.setPadding(new Insets(densePageOne ? 5 : 8));
            imageBox.getStyleClass().add("ficha-image-box");

            if (mainSnapshot != null) {
                ImageView imgView = new ImageView(mainSnapshot);
                imgView.setPreserveRatio(true);
                imgView.setFitWidth(EPIC_CONTENT_WIDTH - 10);

                boolean isFullSet = config != null && (config.llevaShort() || config.llevaMedias());
                double baseHeight = densePageOne ? 320 : 360;
                imgView.setFitHeight(isFullSet ? baseHeight + 100 : baseHeight);

                StackPane visualWithOverlays = TechnicalSheetSketchService.createVisualWithOverlays(imgView);
                visualWithOverlays.setMaxWidth(Double.MAX_VALUE);
                imageBox.getChildren().add(visualWithOverlays);
            }
            unifiedFrame.getChildren().add(imageBox);

            // 3. Bottom Summary (Shorts & Socks)
            VBox shortSummaryBox = TechnicalSheetDataService.createShortSummarySection(jugadores, config);
            if (shortSummaryBox != null) unifiedFrame.getChildren().add(shortSummaryBox);

            VBox socksSummaryBox = TechnicalSheetDataService.createSocksSummarySection(jugadores);
            if (socksSummaryBox != null) unifiedFrame.getChildren().add(socksSummaryBox);

            contentWrapper.getChildren().add(unifiedFrame);

            // 5. Specs Panel (Uses the visualizer ONLY for non-visual data like colors/layers)
            Region specsPanel = SpecsPanel.build(config, visualizer, mainLayers, shieldEntries, jugadores, EPIC_CONTENT_WIDTH);
            contentWrapper.getChildren().add(specsPanel);

            // --- CONTENT DECOUPLING & SEQUENCING ---
            boolean hasReferences = visualizer != null && (visualizer.getState().getReferenceHotspots().size() > 0 || 
                    (visualizer.getArqueroState() != null && visualizer.getArqueroState().getReferenceHotspots().size() > 0));
            
            List<TechnicalSheetPaginationManager.RosterChunk> rosterSections = (jugadores != null && !jugadores.isEmpty())
                    ? TechnicalSheetPaginationManager.createRosterSectionList(jugadores, hasArqueros)
                    : new ArrayList<>();

            VBox shippingSection = TechnicalSheetSketchService.createShippingSection(shippingInfo);

            List<Node> pagesList = new ArrayList<>();
            List<TechnicalSheetPaginationManager.Chunk> prioritizedChunks = new ArrayList<>();

            // Priority 1: Arquero Boceto
            if (hasArqueros) {
                TechnicalSheetPaginationManager.RosterChunk arq = TechnicalSheetSketchService.createArqueroBocetoSection(jugadores, visualizer, arqSketchHombre, arqSketchMujer, arqueroConfig);
                if (arq != null) prioritizedChunks.add(new TechnicalSheetPaginationManager.Chunk(arq.node(), arq.height(), "arquero"));
            }

            // Priority 2: Roster Title + Tables
            if (!rosterSections.isEmpty()) {
                Label rosterTitle = new Label("LISTA DE JUGADORES");
                rosterTitle.getStyleClass().add("ficha-roster-title");
                rosterTitle.setPadding(new Insets(10, 0, 6, 0));
                prioritizedChunks.add(new TechnicalSheetPaginationManager.Chunk(rosterTitle, 35, "roster-header"));
            }

            for (TechnicalSheetPaginationManager.RosterChunk rc : rosterSections) {
                prioritizedChunks.add(new TechnicalSheetPaginationManager.Chunk(rc.node(), rc.height(), "roster"));
            }

            // Priority 3: Technical References
            if (hasReferences) {
                TechnicalSheetPaginationManager.RosterChunk refs = TechnicalSheetSketchService.createTechnicalReferencesSection(visualizer, referencias);
                if (refs != null) prioritizedChunks.add(new TechnicalSheetPaginationManager.Chunk(refs.node(), refs.height(), "refs"));
            }

            final TechnicalSheetPaginationManager.Chunk shippingChunk = (shippingSection != null) ? new TechnicalSheetPaginationManager.Chunk(shippingSection, 300, "shipping") : null;
            boolean shippingPlaced = (shippingChunk == null);

            // --- INTELLIGENT DISTRIBUTION ---
            page1.getChildren().add(contentWrapper);
            VBox currentPage = page1;
            VBox currentContent = contentWrapper;

            boolean isFullSet = config != null && (config.llevaShort() || config.llevaMedias());
            double currentUsedHeight = 70 + (isFullSet ? 320 : 180) + 110 + 5;
            boolean isFirstPage = true;

            for (int i = 0; i < prioritizedChunks.size(); i++) {
                TechnicalSheetPaginationManager.Chunk chunk = prioritizedChunks.get(i);
                double pHeight = TechnicalSheetPaginationManager.getPrintableContentHeight();
                double remaining = pHeight - currentUsedHeight;

                boolean forceNewPage = isFirstPage || (chunk.height() + 10 > remaining);

                if (forceNewPage) {
                    if (!isFirstPage && !shippingPlaced && shippingChunk != null && (shippingChunk.height() + 10 <= remaining)) {
                        currentContent.getChildren().add(shippingChunk.node());
                        shippingPlaced = true;
                    }

                    pagesList.add(TechnicalSheetPaginationManager.decorate(currentPage, isFirstPage));
                    currentPage = TechnicalSheetPaginationManager.createPageContainer();
                    currentPage.getChildren().add(TechnicalSheetPaginationManager.createHeader(cliente, codigo, fechaLocalDate, prioridad, vendedor));
                    currentContent = new VBox(5);
                    currentContent.setAlignment(Pos.TOP_CENTER);
                    currentContent.setMinWidth(PAGE_CONTENT_WIDTH);
                    currentPage.getChildren().add(currentContent);

                    currentUsedHeight = 90;
                    isFirstPage = false;
                    remaining = pHeight - currentUsedHeight;
                }

                currentContent.getChildren().add(chunk.node());
                currentUsedHeight += (chunk.height() + 10);

                if (i == prioritizedChunks.size() - 1 && !shippingPlaced && shippingChunk != null) {
                    remaining = pHeight - currentUsedHeight;
                    if (remaining >= shippingChunk.height() + 10) {
                        currentContent.getChildren().add(shippingChunk.node());
                        shippingPlaced = true;
                    }
                }
            }

            if (!shippingPlaced && shippingChunk != null) {
                if ((TechnicalSheetPaginationManager.getPrintableContentHeight() - currentUsedHeight) < (shippingChunk.height() + 10)) {
                    pagesList.add(TechnicalSheetPaginationManager.decorate(currentPage, isFirstPage));
                    currentPage = TechnicalSheetPaginationManager.createPageContainer();
                    currentPage.getChildren().add(TechnicalSheetPaginationManager.createHeader(cliente, codigo, fechaLocalDate, prioridad, vendedor));
                    currentContent = new VBox(5);
                    currentContent.setAlignment(Pos.TOP_CENTER);
                    currentContent.setMinWidth(PAGE_CONTENT_WIDTH);
                    currentPage.getChildren().add(currentContent);
                    isFirstPage = false;
                }
                currentContent.getChildren().add(shippingChunk.node());
            }

            pagesList.add(TechnicalSheetPaginationManager.decorate(currentPage, isFirstPage));

            for (Node p : pagesList) pagesContainer.getChildren().add(p);
            outerContainer.getChildren().add(pagesContainer);
            return outerContainer;

        } catch (Exception e) {
            e.printStackTrace();
            VBox errorBox = new VBox(20);
            errorBox.setAlignment(Pos.CENTER);
            errorBox.getChildren().add(new Label("ERROR AL GENERAR VISTA PREVIA"));
            return errorBox;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
