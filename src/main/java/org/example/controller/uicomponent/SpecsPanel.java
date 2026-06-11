package org.example.controller.uicomponent;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.example.component.PrendaVisualizer;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.service.PdfExportService;
import org.example.dto.save.LayerDTO;
import org.example.dto.save.ShapeDTO;

import java.util.stream.Collectors;
import java.util.Objects;
import java.util.List;
import java.util.Set;

/**
 * Builds the technical specifications panel in the Ficha Tecnica.
 */
public class SpecsPanel {

    public static Region build(ConfiguracionPrendaDTO config, PrendaVisualizer visualizer,
            List<LayerDTO> layers,
            List<PdfExportService.ShieldEntry> shieldEntries, List<org.example.model.DetallePedido> roster, double targetWidth) {
        VBox root = new VBox(8);
        root.setId("specs-table");
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(4, 0, 4, 0));
        root.setMaxWidth(targetWidth);
        root.setPrefWidth(targetWidth);

        double cardWidth = (targetWidth - 8) / 2.0;
        java.util.List<Region> cards = new java.util.ArrayList<>();

        // 1. CAMISETA
        boolean isShortOnly = config.getTipoPrenda() == org.example.model.TipoPrenda.SHORT;
        if (!isShortOnly) {
            cards.add(createCamisetaCard(config, visualizer, roster, cardWidth));
        }

        // 2. SHORT
        if (config.llevaShort()) {
            cards.add(createShortCard(config, visualizer, cardWidth));
        }

        // 3. MEDIAS
        if (config.llevaMedias()) {
            cards.add(createMediasCard(config, visualizer, roster, cardWidth));
        }

        // 4. PARCHES / ESCUDOS
        if (shieldEntries != null && !shieldEntries.isEmpty()) {
            cards.add(createParchesCard(shieldEntries, cardWidth));
        }

        // 5. COLOR PALETTE (Last)
        if (config != null) {
            // Determine if it will span
            boolean paletteSpans = (cards.size() % 2 == 0);
            cards.add(createPaletteCard(config, visualizer, layers, paletteSpans ? targetWidth : cardWidth, paletteSpans));
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setMaxWidth(targetWidth);
        grid.setPrefWidth(targetWidth);

        int row = 0;
        int col = 0;
        for (int i = 0; i < cards.size(); i++) {
            Region card = cards.get(i);
            boolean isLast = (i == cards.size() - 1);
            boolean isStartOfRow = (col == 0);
            
            if (isLast && isStartOfRow) {
                // Span 2 columns
                grid.add(card, 0, row);
                GridPane.setColumnSpan(card, 2);
                card.setPrefWidth(targetWidth);
                card.setMaxWidth(targetWidth);
            } else {
                grid.add(card, col, row);
                col++;
                if (col > 1) {
                    col = 0;
                    row++;
                }
            }
        }

        root.getChildren().add(grid);
        return root;
    }

    private static VBox createCardBase(String title, double w) {
        VBox card = new VBox(0);
        card.setPrefWidth(w);
        card.setMaxWidth(w);
        card.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        StackPane header = new StackPane();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(7, 5, 7, 5));
        header.setStyle("-fx-background-color: #D8D8D8; -fx-background-radius: 4 4 0 0; -fx-border-color: #999999; -fx-border-width: 0 0 1 0;");

        Label lbl = new Label(title.toUpperCase());
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        header.getChildren().add(lbl);

        card.getChildren().add(header);
        return card;
    }

    private static VBox createPaletteCard(ConfiguracionPrendaDTO config, PrendaVisualizer visualizer, 
            List<LayerDTO> layers, double maxW, boolean isFullWidth) {
        VBox card = new VBox(0);
        card.setPrefWidth(maxW);
        card.setMaxWidth(maxW);
        card.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        StackPane header = new StackPane();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(7,isFullWidth ? 4 : 5, 7, isFullWidth ? 4 : 5));
        header.setStyle("-fx-background-color: #D8D8D8; -fx-background-radius: 4 4 0 0; -fx-border-color: #999999; -fx-border-width: 0 0 1 0;");

        Label lbl = new Label("PALETA DE COLORES");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        header.getChildren().add(lbl);
        card.getChildren().add(header);

        // Gather all colors to display
        java.util.List<Node> colorItems = new java.util.ArrayList<>();
        
        // 1. Add base garment colors from DTO
        if (config.getColors() != null) {
            if (config.getColors().containsKey("body"))
                colorItems.add(createColorItem("Cuerpo", Color.web(config.getColors().get("body")), config.getInternalCodes() != null ? config.getInternalCodes().get("body") : null));
            
            if (config.getLargo() != org.example.model.TipoLargo.MANGA_CERO && config.getColors().containsKey("sleeves")) {
                colorItems.add(createColorItem("Mangas", Color.web(config.getColors().get("sleeves")), config.getInternalCodes() != null ? config.getInternalCodes().get("sleeves") : null));
            }
            
            if (config.getColors().containsKey("collar"))
                colorItems.add(createColorItem("Cuello", Color.web(config.getColors().get("collar")), config.getInternalCodes() != null ? config.getInternalCodes().get("collar") : null));
            
            if (config.llevaShort() && config.getColors().containsKey("shorts")) {
                colorItems.add(createColorItem("Short", Color.web(config.getColors().get("shorts")), config.getInternalCodes() != null ? config.getInternalCodes().get("shorts") : null));
            }
            
            if (config.llevaMedias() && config.getColors().containsKey("socks")) {
                colorItems.add(createColorItem("Medias", Color.web(config.getColors().get("socks")), config.getInternalCodes() != null ? config.getInternalCodes().get("socks") : null));
            }
        }

        // 2. Scan for additional 'bicolor' or detail colors in PROVIDED layers (Design INDEPENDENCE)
        if (layers != null) {
            java.util.Set<String> seenColors = new java.util.HashSet<>();
            for (LayerDTO l : layers) {
                if (l instanceof ShapeDTO) {
                     ShapeDTO sl = (ShapeDTO) l;
                     try {
                        Color c = Color.web(sl.getFillColor());
                        if (c != null && !c.equals(Color.TRANSPARENT)) {
                            String hex = String.format("#%02X%02X%02X", 
                                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
                            String zone = sl.getActiveZone();
                            String label = "Detalle";
                            if (zone != null) {
                                String zlow = zone.toLowerCase();
                                if (zlow.contains("sleeve") || zlow.contains("manga")) label = "Detalle Manga";
                                else if (zlow.contains("body") || zlow.contains("pecho") || zlow.contains("cuerpo")) label = "Detalle Cuerpo";
                                else if (zlow.contains("short")) label = "Detalle Short";
                            }
                            
                            String key = label + "_" + hex;
                            if (!seenColors.contains(key)) {
                                colorItems.add(createColorItem(label, c, null));
                                seenColors.add(key);
                            }
                        }
                     } catch (Exception e) {}
                }
            }
        }

        if (isFullWidth) {
            FlowPane content = new FlowPane();
            content.setHgap(14);
            content.setVgap(6);
            content.setAlignment(Pos.CENTER); 
            content.setPadding(new Insets(8, 10, 8, 10));
            content.setMaxWidth(maxW - 4); 

            for (int i = 0; i < colorItems.size(); i++) {
                content.getChildren().add(colorItems.get(i));
                if (i < colorItems.size() - 1) {
                    content.getChildren().add(new Label("|"));
                }
            }
            card.getChildren().add(content);
        } else {
            VBox body = new VBox(0);
            for (Node item : colorItems) {
                item.setStyle("-fx-border-color: #999999; -fx-border-width: 0 0 1 0; -fx-padding: 6 10; -fx-background-color: white;");
                body.getChildren().add(item);
            }
            card.getChildren().add(body);
        }
        return card;
    }

    private static HBox createColorItem(String name, javafx.scene.paint.Color c, String code) {
        HBox item = new HBox(4);
        item.setAlignment(Pos.CENTER_LEFT);

        Rectangle rect = new Rectangle(14, 14);
        rect.setFill(c);
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(0.8);

        StringBuilder sb = new StringBuilder();
        if (name != null && !name.isEmpty()) {
            sb.append(name);
        }
        if (code != null && !code.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("[").append(code).append("]");
        }

        Label lb = new Label(sb.toString().trim().toUpperCase());
        lb.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #333;");

        item.getChildren().addAll(rect, lb);
        return item;
    }

    private static HBox createColorSummaryRow(String k, String type, Color c, String code) {
        HBox row = new HBox(0);
        row.setStyle("-fx-border-color: #999999; -fx-border-width: 0 0 1 0;");

        // 1. Key Cell (95px)
        Label lblK = new Label(k);
        lblK.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #000000;");
        StackPane keyPane = new StackPane(lblK);
        keyPane.setAlignment(Pos.CENTER_LEFT);
        keyPane.setPadding(new Insets(5, 9, 5, 9));
        keyPane.setPrefWidth(96);
        keyPane.setMinWidth(96);
        keyPane.setMaxWidth(96);
        keyPane.setStyle("-fx-background-color: #EFEFEF; -fx-border-color: #999999; -fx-border-width: 0 1 0 0;");

        // 2. Value/Dato Cell (100px)
        Label lblType = new Label(normalizeTypeLabel(type));
        lblType.setStyle("-fx-font-size: 11px; -fx-text-fill: #333333;");
        StackPane valPane = new StackPane(lblType);
        valPane.setAlignment(Pos.CENTER_LEFT);
        valPane.setPadding(new Insets(5, 9, 5, 9));
        valPane.setPrefWidth(108);
        valPane.setMinWidth(108);
        valPane.setMaxWidth(108);
        valPane.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 0 1 0 0;");

        // 3. Color info Cell (Flexible)
        Node colorLabel = createColorLabel(c, code);
        StackPane colorPane = new StackPane(colorLabel);
        colorPane.setAlignment(Pos.CENTER_LEFT);
        colorPane.setPadding(new Insets(5, 8, 5, 8));
        HBox.setHgrow(colorPane, Priority.ALWAYS);
        colorPane.setStyle("-fx-background-color: white;");

        row.getChildren().addAll(keyPane, valPane, colorPane);
        return row;
    }

    private static String normalizeTypeLabel(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.equalsIgnoreCase("Si") || t.equalsIgnoreCase("Sí")) return "Si";
        if (t.length() <= 6 && (t.startsWith("S") || t.startsWith("s")) && (t.contains("Ã") || t.contains("\uFFFD"))) return "Si";
        if (t.equalsIgnoreCase("SÃ­") || t.equalsIgnoreCase("SÃƒÂ­") || t.equalsIgnoreCase("SÃ")) return "Si";
        return t;
    }

    private static Node createColorLabel(Color c, String code) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        Rectangle r = new Rectangle(12, 12);
        r.setFill(c);
        r.setStroke(Color.BLACK);
        r.setStrokeWidth(0.5);

        String colorName = getColorNameEs(c);
        String text = "";
        if (code != null && !code.trim().isEmpty()) {
            text = code.trim();
        }
        if (colorName != null && !colorName.isEmpty()) {
            if (!text.isEmpty()) text += " ";
            text += colorName;
        }
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #555;");
        l.setWrapText(true);

        box.getChildren().addAll(r, l);
        return box;
    }

    private static String getColorNameEs(Color c) {
        if (c == null) return "";
        double s = c.getSaturation();
        double b = c.getBrightness();
        if (b >= 0.97 && s <= 0.06) return "Blanco";
        if (b <= 0.10 && s <= 0.10) return "Negro";
        if (s <= 0.10) return "Gris";

        double h = c.getHue();
        if (h < 15 || h >= 345) return "Rojo";
        if (h < 40) return "Naranja";
        if (h < 65) return "Amarillo";
        if (h < 160) return "Verde";
        if (h < 200) return "Celeste";
        if (h < 260) return "Azul";
        if (h < 300) return "Morado";
        if (h < 345) return "Rosado";
        return "Color";
    }

    private static VBox createCamisetaCard(ConfiguracionPrendaDTO config, PrendaVisualizer visualizer, List<org.example.model.DetallePedido> roster, double w) {
        VBox card = createCardBase("CAMISETA", w);
        VBox body = new VBox(0);

        body.getChildren().add(createCellRow("Corte", config.getCorte().name()));
        body.getChildren().add(createCellRow("Cuello", config.getCuello().name()));

String telaLabel = (config.getTela() != null) ? config.getTela().getLabel() : "No definida";
        if (config.getTela() == org.example.model.TipoTela.OTRO && config.getCustomTela() != null && !config.getCustomTela().isEmpty()) {
            telaLabel = config.getCustomTela();
        }
        Node extraTag = new StackPane();
        if (visualizer != null && visualizer.getState().isTelaNatural()) {
            Label naturalLabel = new Label("NATURAL");
            naturalLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 2 6 2 6; -fx-background-radius: 3;");
            extraTag = new StackPane(naturalLabel);
        }
        body.getChildren().add(createDetailedCellRow("Tela", telaLabel, extraTag));

        String sleeveSummary = config.getLargo().name();
        if (roster != null && !roster.isEmpty()) {
            Set<String> sleeves = roster.stream()
                .filter(p -> p.isIncludeTop())
                .map(p -> p.getTipoManga())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (!sleeves.isEmpty()) {
                sleeveSummary = sleeves.stream().sorted().collect(Collectors.joining(" / "));
            }
        }
        body.getChildren().add(createDetailedCellRow("Manga", sleeveSummary, createSleeveDetailedSummary(roster)));

        if (config.llevaMalla() || (visualizer != null && visualizer.hasMesh())) {
            Color meshColor = visualizer.getPartColor("mesh", Color.web("#95a5a6"));
            String meshCode = visualizer.getInternalCode("mesh");
            body.getChildren().add(createColorSummaryRow("Malla", "Si", meshColor, meshCode));
        }

        if (config.llevaPunoCamiseta() && visualizer != null) {
            Color cuffColor = visualizer.getPartColor("cuff", Color.web("#95a5a6"));
            String cuffCode = visualizer.getInternalCode("cuff");
            body.getChildren().add(createColorSummaryRow("Puño", "Si", cuffColor, cuffCode));
        }

        if (visualizer != null && visualizer.isChestBrandVisible()) {
            String brandType = visualizer.getBrandTech() != null && !visualizer.getBrandTech().isEmpty() ? visualizer.getBrandTech() : "Bordado";
            Color brandColor = visualizer.getPartColor("brandChest", Color.web("#333333"));
            String brandCode = visualizer.getInternalCode("brandChest");
            body.getChildren().add(createColorSummaryRow("Marca", brandType, brandColor, brandCode));
        }

        card.getChildren().add(body);
        return card;
    }

    private static VBox createShortCard(ConfiguracionPrendaDTO config, PrendaVisualizer visualizer, double w) {
        String title = "SHORT";
        if (config.getCorteShort() != null) {
            if (config.getCorteShort() == org.example.model.TipoCorte.CUADRADO) {
                title = "SHORT DEPORTIVO";
            } else {
                title = "SHORT " + config.getCorteShort().getLabel().toUpperCase();
            }
        }

        VBox card = createCardBase(title, w);
        java.util.List<Node> rows = new java.util.ArrayList<>();

        if (config.llevaFranjaShort()) {
            Color stripeColor = visualizer.getPartColor("shortsStripe", Color.GRAY);
            String stripeCode = visualizer.getInternalCode("shortsStripe");
            rows.add(createColorSummaryRow("Franja", "Si", stripeColor, stripeCode));
        }

        if (config.llevaBolsilloShort()) rows.add(createCellRow("Bolsillo", "Si"));
        if (config.llevaPiqueteShort()) rows.add(createCellRow("Piquete", "Si"));
        
        if (config.llevaPasadorShort()) {
            if (config.llevaForroShort()) {
                Label lblForro = new Label("FORRO INTERNO");
                lblForro.setStyle("-fx-font-size: 11px; " +
                                 "-fx-font-weight: bold; " +
                                 "-fx-text-fill: white; " +
                                 "-fx-background-color: #C0392B; " +
                                 "-fx-padding: 3 8; " +
                                 "-fx-background-radius: 4; " +
                                 "-fx-border-color: #922B21; " +
                                 "-fx-border-width: 1; " +
                                 "-fx-border-radius: 4; " +
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1);");
                rows.add(createDetailedCellRow("Pasador", "Si", lblForro));
            } else {
                rows.add(createCellRow("Pasador", "Si"));
            }
        } else if (config.llevaForroShort()) {
            Label lblForro = new Label("FORRO INTERNO");
            lblForro.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #C0392B; -fx-padding: 3 8; -fx-background-radius: 4;");
            rows.add(createDetailedCellRow("Forro", "Si", lblForro));
        }

        if (config.llevaPunoShort()) {
            Color cuffColor = visualizer.getPartColor("shortsCuff", Color.web("#95a5a6"));
            String cuffCode = visualizer.getInternalCode("shortsCuff");
            rows.add(createColorSummaryRow("Puño", "Si", cuffColor, cuffCode));
        }

        if (visualizer != null && visualizer.isShortBrandVisible()) {
            String brandType = visualizer.getBrandTech() != null && !visualizer.getBrandTech().isEmpty() ? visualizer.getBrandTech() : "Bordado";
            Color brandColor = visualizer.getPartColor("brandShort", Color.web("#333333"));
            String brandCode = visualizer.getInternalCode("brandShort");
            rows.add(createColorSummaryRow("Marca", brandType, brandColor, brandCode));
        }

        GridPane body = createStretchedBody(rows);
        VBox.setVgrow(body, Priority.ALWAYS);
        card.getChildren().add(body);
        return card;
    }

    private static VBox createMediasCard(ConfiguracionPrendaDTO config, PrendaVisualizer visualizer, List<org.example.model.DetallePedido> roster, double w) {
        VBox card = createCardBase("MEDIAS", w);
        java.util.List<Node> rows = new java.util.ArrayList<>();
        if (config.llevaLigaMedias()) {
            rows.add(createCellRow("Liga Superior", "Si"));
        }
        
        String tipoMedias = resolveSocksSummary(roster);
        rows.add(createDetailedCellRow("Tipo", tipoMedias, createSocksCountsSummary(roster)));

        if (visualizer != null && visualizer.isSocksBrandVisible()) {
            Color brandColor = visualizer.getPartColor("brandSocks", Color.BLACK);
            String brandCode = visualizer.getInternalCode("brandSocks");
            rows.add(createColorSummaryRow("Marca", "Si", brandColor, brandCode));
        }

        GridPane body = createStretchedBody(rows);
        VBox.setVgrow(body, Priority.ALWAYS);
        card.getChildren().add(body);
        return card;
    }

    private static GridPane createStretchedBody(java.util.List<Node> rows) {
        GridPane body = new GridPane();
        body.setMaxWidth(Double.MAX_VALUE);
        if (rows == null || rows.isEmpty()) return body;

        double percent = 100.0 / rows.size();
        for (int i = 0; i < rows.size(); i++) {
            Node rowNode = rows.get(i);
            body.add(rowNode, 0, i);
            GridPane.setFillHeight(rowNode, true);
            GridPane.setVgrow(rowNode, Priority.ALWAYS);
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(percent);
            rc.setVgrow(Priority.ALWAYS);
            rc.setValignment(VPos.CENTER);
            body.getRowConstraints().add(rc);
        }
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100);
        cc.setHgrow(Priority.ALWAYS);
        body.getColumnConstraints().add(cc);
        return body;
    }

    private static VBox createParchesCard(List<PdfExportService.ShieldEntry> entries, double w) {
        VBox card = createCardBase("ESCUDOS", w);
        VBox body = new VBox(0);
        VBox.setVgrow(body, Priority.ALWAYS);

        // Filter out entries that are "Ninguno" or have no image
        List<PdfExportService.ShieldEntry> validEntries = entries.stream()
            .filter(e -> e.image != null || (e.label != null && !e.label.equalsIgnoreCase("Ninguno")))
            .collect(Collectors.toList());

        if (validEntries.size() == 1) {
            // Special layout for single shield to fill space as requested by user
            body.getChildren().add(createLargeShieldView(validEntries.get(0)));
        } else {
            for (int i = 0; i < validEntries.size(); i++) {
                PdfExportService.ShieldEntry e = validEntries.get(i);
                boolean isLast = (i == validEntries.size() - 1);
                body.getChildren().add(createShieldRow(e, isLast));
                if (body.getChildren().size() >= 5) break;
            }
        }
        
        card.getChildren().add(body);
        return card;
    }

    private static VBox createLargeShieldView(PdfExportService.ShieldEntry e) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        VBox.setVgrow(box, Priority.ALWAYS);

        if (e.image != null) {
            ImageView iv = new ImageView(e.image);
            iv.setFitWidth(88); 
            iv.setFitHeight(88);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            
            StackPane imgContainer = new StackPane(iv);
            imgContainer.setPadding(new Insets(8));
            imgContainer.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #eee; -fx-border-radius: 8; -fx-background-radius: 8;");
            box.getChildren().add(imgContainer);
        }

        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER);
        Label lblPlace = new Label(normalizePlacement(e.zone).toUpperCase());
        lblPlace.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #000000; -fx-font-family: 'Segoe UI';");
        
        Label lblType = new Label(e.label != null ? e.label.toUpperCase() : "");
        lblType.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        
        info.getChildren().addAll(lblPlace, lblType);
        box.getChildren().add(info);
        
        return box;
    }

    private static HBox createCellRow(String k, String v) {
        HBox row = new HBox(0);
        row.setStyle("-fx-border-color: #999999; -fx-border-width: 0 0 1 0;");

        // 1. Key Cell
        Label lblK = new Label(k);
        lblK.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #000000;");
        StackPane keyPane = new StackPane(lblK);
        keyPane.setAlignment(Pos.CENTER_LEFT);
        keyPane.setPadding(new Insets(5, 9, 5, 9));
        keyPane.setPrefWidth(96);
        keyPane.setMinWidth(96);
        keyPane.setMaxWidth(96);
        keyPane.setStyle("-fx-background-color: #EFEFEF; -fx-border-color: #999999; -fx-border-width: 0 1 0 0;");

        // 2. Value Cell (Aligned with 'type' column)
        Label lblV = new Label(normalizeTypeLabel(v));
        lblV.setStyle("-fx-font-size: 11px; -fx-text-fill: #333333;");
        lblV.setWrapText(true);
        StackPane valPane = new StackPane(lblV);
        valPane.setAlignment(Pos.CENTER_LEFT);
        valPane.setPadding(new Insets(5, 9, 5, 9));
        valPane.setPrefWidth(108);
        valPane.setMinWidth(108);
        valPane.setMaxWidth(108);
        valPane.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 0 1 0 0;");

        // 3. Empty Color/Dummy Cell for standardization
        StackPane dummyPane = new StackPane();
        HBox.setHgrow(dummyPane, Priority.ALWAYS);
        dummyPane.setStyle("-fx-background-color: white;");

        row.getChildren().addAll(keyPane, valPane, dummyPane);
        return row;
    }

    private static HBox createShieldRow(PdfExportService.ShieldEntry e, boolean isLast) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 8, 6, 8));
        
        // Border: only if not last to avoid double line at bottom
        String borderStyle = isLast ? "" : "-fx-border-color: #999999; -fx-border-width: 0 0 1 0;";
        row.setStyle(borderStyle + " -fx-background-color: white;");

        if (e.image != null) {
            ImageView iv = new ImageView(e.image);
            iv.setFitWidth(40); // Slightly smaller than container for padding
            iv.setFitHeight(40);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setCache(true);
            
            // Container for image with a small border/padding
            StackPane imgContainer = new StackPane(iv);
            StackPane.setAlignment(iv, Pos.CENTER);
            imgContainer.setPrefSize(46, 46);
            imgContainer.setMinSize(46, 46);
            imgContainer.setMaxSize(46, 46);
            imgContainer.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 4; -fx-background-radius: 4;");
            row.getChildren().add(imgContainer);
        }

        VBox info = new VBox(2);
        Label lblPlace = new Label(normalizePlacement(e.zone));
        lblPlace.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #000000;");
        
        Label lblType = new Label(e.label != null ? e.label.toUpperCase() : "");
        lblType.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666; -fx-font-weight: bold;");
        
        info.getChildren().addAll(lblPlace, lblType);
        HBox.setHgrow(info, Priority.ALWAYS);
        row.getChildren().add(info);
        
        return row;
    }

    private static String normalizePlacement(String p) {
        if (p == null) return "GENERAL";
        switch (p.toUpperCase()) {
            case "PECHO": return "PECHO (FRONTAL)";
            case "ESPALDA": return "ESPALDA (DORSAL)";
            case "MANGA_DELANTERA": return "MANGA DELANTERA";
            case "MANGA_TRASERA": return "MANGA TRASERA";
            case "SHORT_FRONT": return "SHORT DELANTERO";
            case "SHORT_BACK": return "SHORT TRASERO";
            case "MEDIAS": return "MEDIAS";
            default: return p.toUpperCase();
        }
    }

    private static String resolveSocksSummary(List<org.example.model.DetallePedido> roster) {
        if (roster == null || roster.isEmpty()) return "PROFESIONAL";
        
        java.util.Set<String> types = new java.util.LinkedHashSet<>();
        for (org.example.model.DetallePedido p : roster) {
            if (p.isIncludeSocks()) {
                types.add(resolveAutoSocksType(p));
            }
        }
        
        if (types.isEmpty()) return "PROFESIONAL";
        
        return types.stream().collect(java.util.stream.Collectors.joining(" / "));
    }

    private static HBox createDetailedCellRow(String k, String v, Node extra) {
        HBox row = new HBox(0);
        row.setStyle("-fx-border-color: #999999; -fx-border-width: 0 0 1 0;");

        // 1. Key Cell
        Label lblK = new Label(k);
        lblK.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #000000;");
        StackPane keyPane = new StackPane(lblK);
        keyPane.setAlignment(Pos.CENTER_LEFT);
        keyPane.setPadding(new Insets(5, 9, 5, 9));
        keyPane.setPrefWidth(96);
        keyPane.setMinWidth(96);
        keyPane.setMaxWidth(96);
        keyPane.setStyle("-fx-background-color: #EFEFEF; -fx-border-color: #999999; -fx-border-width: 0 1 0 0;");

        // 2. Value Cell
        Label lblV = new Label(normalizeTypeLabel(v));
        lblV.setStyle("-fx-font-size: 11px; -fx-text-fill: #333333;");
        lblV.setWrapText(true);
        StackPane valPane = new StackPane(lblV);
        valPane.setAlignment(Pos.CENTER_LEFT);
        valPane.setPadding(new Insets(5, 9, 5, 9));
        valPane.setPrefWidth(108);
        valPane.setMinWidth(108);
        valPane.setMaxWidth(108);
        valPane.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 0 1 0 0;");

        // 3. Extra Info Cell
        StackPane extraPane = new StackPane(extra);
        extraPane.setAlignment(Pos.CENTER_LEFT);
        extraPane.setPadding(new Insets(5, 8, 5, 8));
        HBox.setHgrow(extraPane, Priority.ALWAYS);
        extraPane.setStyle("-fx-background-color: white;");

        row.getChildren().addAll(keyPane, valPane, extraPane);
        return row;
    }

    private static Node createSleeveDetailedSummary(List<org.example.model.DetallePedido> roster) {
        if (roster == null || roster.isEmpty()) return new StackPane();
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (org.example.model.DetallePedido p : roster) {
            if (p.isIncludeTop()) {
                String m = p.getTipoManga() != null ? p.getTipoManga().toUpperCase() : "CORTA";
                counts.put(m, counts.getOrDefault(m, 0) + 1);
            }
        }
        return buildTagFlow(counts);
    }

    private static Node createSocksCountsSummary(List<org.example.model.DetallePedido> roster) {
        if (roster == null || roster.isEmpty()) return new StackPane();
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (org.example.model.DetallePedido p : roster) {
            if (p.isIncludeSocks()) {
                String t = resolveAutoSocksType(p);
                counts.put(t, counts.getOrDefault(t, 0) + 1);
            }
        }
        return buildTagFlow(counts);
    }

    private static Node buildTagFlow(java.util.Map<String, Integer> counts) {
        FlowPane container = new FlowPane();
        container.setHgap(4);
        container.setVgap(4);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPrefWrapLength(120); // Force wrap if too many
        for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
            Label l = new Label(e.getValue() + " " + e.getKey());
            l.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: #444; -fx-background-color: #f0f0f0; -fx-padding: 1 3; -fx-background-radius: 2; -fx-border-color: #ddd; -fx-border-width: 0.5; -fx-border-radius: 2;");
            container.getChildren().add(l);
        }
        return container;
    }

    private static String resolveAutoSocksType(org.example.model.DetallePedido p) {
        if (p.getTipoMedias() != null && !p.getTipoMedias().trim().isEmpty() && !p.getTipoMedias().equalsIgnoreCase("PROFESIONAL")) {
            return p.getTipoMedias().trim().toUpperCase();
        }
        String t = p.getTalla() != null ? p.getTalla().toUpperCase().trim() : "";
        if (t.matches("S|M|L|XL|XXL|3XXL|4XXL|G|XG|2XL|3XL|4XL|XXS|XXXS")) {
            return "ADULTO";
        }
        if (t.matches("12|14|16")) {
            return "JUVENIL";
        }
        if (t.matches("4|6|8")) {
            return "NI\u00D1OS";
        }
        return "PROFESIONAL";
    }
}

