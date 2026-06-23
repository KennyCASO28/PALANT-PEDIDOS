package org.example.controller.uicomponent.helper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.DetallePedido;
import org.example.model.TipoCorte;
import java.util.*;

/**
 * Service to process player data and generate production summaries for the technical sheet.
 */
public class TechnicalSheetDataService {

    private static final double PAGE_CONTENT_WIDTH = 754; // A4_WIDTH - (20 * 2)

    public static boolean isDensePageOne(ConfiguracionPrendaDTO config, List<?> shieldEntries) {
        if (config == null) return false;
        int sections = 1; // camiseta base
        if (config.llevaShort()) sections++;
        if (config.llevaMedias()) sections++;
        if (shieldEntries != null && shieldEntries.size() > 2) sections++;
        return sections > 3;
    }

    public static VBox createSummaryWidget(List<DetallePedido> roster) {
        VBox container = new VBox(0);
        container.setPadding(new Insets(2));
        container.setStyle("-fx-background-color: white;");
        container.setMinWidth(PAGE_CONTENT_WIDTH);
        container.setPrefWidth(PAGE_CONTENT_WIDTH);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setAlignment(Pos.CENTER);

        VBox contentWrapper = new VBox();
        contentWrapper.setAlignment(Pos.CENTER);
        contentWrapper.setMinWidth(PAGE_CONTENT_WIDTH);
        contentWrapper.setPrefWidth(PAGE_CONTENT_WIDTH);
        contentWrapper.setMaxWidth(Double.MAX_VALUE);
        container.getChildren().add(contentWrapper);

        contentWrapper.getChildren().add(buildSummaryContent(roster));
        return container;
    }

    private static Node buildSummaryContent(List<DetallePedido> roster) {
        Map<String, Map<String, int[]>> summary = calculateSizeSummary(roster);
        List<String> gendersWithTop = new ArrayList<>();
        for (Map.Entry<String, Map<String, int[]>> entry : summary.entrySet()) {
            boolean hasTop = entry.getValue().values().stream().anyMatch(a -> (a[0] + a[1] + a[2]) > 0);
            if (hasTop) gendersWithTop.add(entry.getKey());
        }

        if (gendersWithTop.size() == 2) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER);
            row.setMinWidth(PAGE_CONTENT_WIDTH);
            double targetWidth = (PAGE_CONTENT_WIDTH - 8.0) / 2.0;
            for (String gender : gendersWithTop) {
                row.getChildren().add(createTopSummaryTable(gender, summary.get(gender), targetWidth));
            }
            return row;
        }

        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMinWidth(PAGE_CONTENT_WIDTH);
        for (String gender : gendersWithTop) {
            contentBox.getChildren().add(createTopSummaryTable(gender, summary.get(gender), PAGE_CONTENT_WIDTH));
        }
        return contentBox;
    }

    private static VBox createTopSummaryTable(String gender, Map<String, int[]> sizes, double targetWidth) {
        boolean compact = targetWidth < 500;
        VBox tableContainer = new VBox(0);
        tableContainer.setStyle("-fx-background-color: white; -fx-padding: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 4;");
        tableContainer.setMinWidth(targetWidth);
        tableContainer.setPrefWidth(targetWidth);

        Label lblHeader = new Label("DESGLOSE DE PRENDA SUP. - " + gender);
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.setAlignment(Pos.CENTER);
        lblHeader.setStyle("-fx-background-color: #D8D8D8; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: " 
                + (compact ? "11px" : "13px") + "; -fx-padding: " + (compact ? "6" : "8") + ";");
        tableContainer.getChildren().add(lblHeader);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        VBox.setMargin(grid, new Insets(4, 0, 0, 0));

        double conceptWidth = compact ? 54 : 100;
        double totalWidth = compact ? 46 : 50;
        grid.getColumnConstraints().add(new ColumnConstraints(conceptWidth));

        String conceptHeaderText = compact ? "TALLA" : "CONCEPTO / TALLA";
        String qtyHeaderText = compact ? "CANT." : "CANTIDAD (Uni.)";

        Label lConcepto = new Label(conceptHeaderText);
        lConcepto.setStyle("-fx-font-size: " + (compact ? "8px" : "9px") + "; -fx-font-weight: bold;");
        StackPane pConcepto = new StackPane(lConcepto);
        pConcepto.setStyle("-fx-background-color: #D0D0D0; -fx-border-color: #999999; -fx-border-width: 1 0 1 1; -fx-padding: " + (compact ? "2" : "6") + ";");
        grid.add(pConcepto, 0, 0);

        int totalTop = 0;
        List<String> activeSizes = new ArrayList<>(sizes.keySet());
        int maxColsPerRow = Math.min(activeSizes.size(), 14);
        if (maxColsPerRow < 1) maxColsPerRow = 1;

        double usable = targetWidth - conceptWidth - totalWidth - 24;
        double sizeColWidth = usable / maxColsPerRow;

        int chunkCount = (int) Math.ceil(activeSizes.size() / (double) maxColsPerRow);
        for (int chunkIndex = 0; chunkIndex < Math.max(1, chunkCount); chunkIndex++) {
            int rowHeader = chunkIndex * 2;
            int rowQty = rowHeader + 1;

            if (chunkIndex > 0) {
                Label lConcepto2 = new Label(conceptHeaderText);
                lConcepto2.setStyle("-fx-font-size: " + (compact ? "8px" : "9px") + "; -fx-font-weight: bold;");
                StackPane pConcepto2 = new StackPane(lConcepto2);
                pConcepto2.setStyle("-fx-background-color: #D0D0D0; -fx-border-color: #999999; -fx-border-width: 1 0 1 1; -fx-padding: " + (compact ? "2" : "6") + ";");
                grid.add(pConcepto2, 0, rowHeader);
            }

            int col = 1;
            int start = chunkIndex * maxColsPerRow;
            int end = Math.min(start + maxColsPerRow, activeSizes.size());

            for (int i = start; i < end; i++) {
                String sizeName = activeSizes.get(i);
                Label lHead = new Label(sizeName);
                lHead.setStyle("-fx-font-size: " + (compact ? "8px" : "10px") + "; -fx-font-weight:bold;");
                StackPane pHead = new StackPane(lHead);
                pHead.setStyle("-fx-background-color: #D0D0D0; -fx-border-color: #999999; -fx-border-width: 1 0 1 1; -fx-padding: " + (compact ? "2" : "6") + ";");
                pHead.setMinWidth(sizeColWidth);
                grid.add(pHead, col, rowHeader);

                int[] counts = sizes.get(sizeName);
                int totalShirtsSize = counts[0] + counts[1] + counts[2];
                totalTop += totalShirtsSize;

                Label lblQty = new Label(String.valueOf(totalShirtsSize));
                lblQty.setStyle("-fx-font-weight: bold; -fx-font-size: " + (compact ? "10px" : "12px") + "; -fx-text-fill: #003399;");
                StackPane pQty = new StackPane(lblQty);
                pQty.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 0 0 1 1; -fx-padding: " + (compact ? "2" : "5") + ";");
                pQty.setMinWidth(sizeColWidth);
                grid.add(pQty, col, rowQty);
                col++;
            }

            if (chunkCount > 1) {
                for (; col <= maxColsPerRow; col++) {
                    StackPane emptyH = new StackPane();
                    emptyH.setStyle("-fx-background-color: #D0D0D0; -fx-border-color: #999999; -fx-border-width: 1 0 1 1;");
                    grid.add(emptyH, col, rowHeader);
                    StackPane emptyV = new StackPane();
                    emptyV.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 0 0 1 1;");
                    grid.add(emptyV, col, rowQty);
                }
            }

            Label lCant = new Label(qtyHeaderText);
            lCant.setStyle("-fx-font-size: " + (compact ? "7px" : "8px") + "; -fx-font-weight: bold;");
            StackPane pCant = new StackPane(lCant);
            pCant.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #999999; -fx-border-width: 0 0 1 1; -fx-padding: " + (compact ? "2" : "5") + ";");
            grid.add(pCant, 0, rowQty);
        }

        VBox totalContent = new VBox(2);
        totalContent.setAlignment(Pos.CENTER);
        totalContent.getChildren().addAll(new Label("TOTAL"), new Label(String.valueOf(totalTop)));
        totalContent.getChildren().get(0).setStyle("-fx-font-size: " + (compact ? "9px" : "11px") + "; -fx-font-weight:bold;");
        totalContent.getChildren().get(1).setStyle("-fx-font-weight: bold; -fx-font-size: " + (compact ? "11px" : "13px") + "; -fx-text-fill: #c0392b;");

        StackPane totalCell = new StackPane(totalContent);
        totalCell.setStyle("-fx-background-color: #fdf2f2; -fx-border-color: #999999; -fx-border-width: 1;");
        totalCell.setMinWidth(totalWidth);
        grid.add(totalCell, maxColsPerRow + 1, 0, 1, Math.max(1, chunkCount) * 2);

        tableContainer.getChildren().add(grid);
        return tableContainer;
    }

    public static Map<String, Map<String, int[]>> calculateSizeSummary(List<DetallePedido> roster) {
        Map<String, Map<String, int[]>> summary = new LinkedHashMap<>();
        summary.put("HOMBRE", new LinkedHashMap<>());
        summary.put("MUJER", new LinkedHashMap<>());

        List<String> sizeOrder = Arrays.asList("4", "6", "8", "10", "12", "14", "16", "XXXS", "XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL", "3XL", "4XL", "5XL");
        Comparator<String> sizeComparator = (s1, s2) -> {
            int i1 = sizeOrder.indexOf(s1);
            int i2 = sizeOrder.indexOf(s2);
            return (i1 >= 0 && i2 >= 0) ? Integer.compare(i1, i2) : s1.compareTo(s2);
        };

        for (DetallePedido p : roster) {
            if (p.isEsArquero()) continue;
            String g = (p.getGenero() != null ? p.getGenero().toUpperCase() : "HOMBRE")
                    .replace("MASCULINO", "HOMBRE").replace("FEMENINO", "MUJER");
            
            String tTop = p.getTalla() != null ? p.getTalla().toUpperCase() : "-";
            String tBot = p.getTallaShort() != null ? p.getTallaShort().toUpperCase() : tTop;
            String m = p.getTipoManga() != null ? p.getTipoManga() : "CORTA";

            Map<String, int[]> genderMap = summary.computeIfAbsent(g, k -> new TreeMap<>(sizeComparator));

            if (p.isIncludeTop()) {
                genderMap.compute(tTop, (k, v) -> {
                    if (v == null) v = new int[]{0, 0, 0, 0};
                    if (m.equalsIgnoreCase("LARGA")) v[1]++;
                    else if (m.equalsIgnoreCase("MANGA 0") || m.equalsIgnoreCase("SIN MANGAS")) v[2]++;
                    else v[0]++;
                    return v;
                });
            }
            if (p.isIncludeBottom()) {
                genderMap.compute(tBot, (k, v) -> {
                    if (v == null) v = new int[]{0, 0, 0, 0};
                    v[3]++;
                    return v;
                });
            }
        }
        summary.entrySet().removeIf(e -> e.getValue().isEmpty());
        return summary;
    }

    public static VBox createShortSummarySection(List<DetallePedido> roster, ConfiguracionPrendaDTO config) {
        Map<String, Map<String, int[]>> summary = calculateSizeSummary(roster);
        if (summary.values().stream().flatMap(m -> m.values().stream()).noneMatch(a -> a[3] > 0)) return null;

        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4;");
        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(8));
        content.setMinWidth(PAGE_CONTENT_WIDTH);

        List<String> gendersWithShort = new ArrayList<>();
        summary.forEach((g, m) -> { if (m.values().stream().anyMatch(a -> a[3] > 0)) gendersWithShort.add(g); });

        double targetWidth = (gendersWithShort.size() == 2) ? (PAGE_CONTENT_WIDTH - 32.0) / 2.0 : PAGE_CONTENT_WIDTH - 16;

        for (String gender : gendersWithShort) {
            VBox block = createSmallShortTable(gender, summary.get(gender), roster, targetWidth, config);
            block.setStyle("-fx-background-color: white; -fx-border-color: #dcdde1; -fx-border-width: 1; -fx-border-radius: 4;");
            HBox.setHgrow(block, Priority.ALWAYS);
            content.getChildren().add(block);
        }
        container.getChildren().add(content);
        return container;
    }

    private static VBox createSmallShortTable(String gender, Map<String, int[]> sizes, List<DetallePedido> roster, double targetWidth, ConfiguracionPrendaDTO config) {
        boolean compact = targetWidth < 500;
        VBox container = new VBox(0);
        container.setMinWidth(targetWidth);

        String label = roster.stream()
                .filter(p -> (p.getGenero() != null ? p.getGenero() : "HOMBRE").equalsIgnoreCase(gender))
                .anyMatch(p -> p.isEsArquero() && p.isIncludeBottom()) ? "BERMUDA (ARQ)" : "SHORT";
        if (label.equals("SHORT") && config != null) {
            if (config.getCorteShort() == TipoCorte.CUADRADO) label = "SHORT DEPORTIVO";
            else if (config.getCorteShort() != null) label = "SHORT " + config.getCorteShort().getLabel().toUpperCase();
        }

        Label header = new Label("DESGLOSE DE PRENDA INF. - " + label + " " + gender);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: #D8D8D8; -fx-font-weight: bold; -fx-font-size: " + (compact ? "11px" : "13px") + "; -fx-padding: " + (compact ? "6" : "8") + ";");
        container.getChildren().add(header);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        double conceptWidth = compact ? 54 : 100;
        double totalWidth = compact ? 46 : 50;
        grid.getColumnConstraints().add(new ColumnConstraints(conceptWidth));

        List<String> activeSizes = new ArrayList<>();
        sizes.forEach((k, v) -> { if (v[3] > 0) activeSizes.add(k); });

        int maxCols = Math.max(1, Math.min(activeSizes.size(), 14));
        double usable = targetWidth - conceptWidth - totalWidth - 24;
        double colW = usable / maxCols;

        int chunks = (int) Math.ceil(activeSizes.size() / (double) maxCols);
        int totalQty = 0;

        for (int c = 0; c < Math.max(1, chunks); c++) {
            int rH = c * 2, rQ = rH + 1;
            StackPane pC = new StackPane(new Label(compact ? "TALLA" : "CONCEPTO / TALLA"));
            pC.setStyle("-fx-background-color: #D0D0D0; -fx-border-color: #999999; -fx-border-width: 1 0 1 1;");
            grid.add(pC, 0, rH);

            int col = 1;
            int start = c * maxCols, end = Math.min(start + maxCols, activeSizes.size());
            for (int i = start; i < end; i++) {
                String s = activeSizes.get(i);
                StackPane pH = new StackPane(new Label(s));
                pH.setStyle("-fx-background-color: #D0D0D0; -fx-border-color: #999999; -fx-border-width: 1 0 1 1;");
                pH.setMinWidth(colW);
                grid.add(pH, col, rH);

                int q = sizes.get(s)[3];
                totalQty += q;
                StackPane pQ = new StackPane(new Label(String.valueOf(q)));
                pQ.setStyle("-fx-background-color: white; -fx-border-color: #999999; -fx-border-width: 0 0 1 1;");
                pQ.setMinWidth(colW);
                grid.add(pQ, col, rQ);
                col++;
            }

            StackPane pCant = new StackPane(new Label(compact ? "CANT." : "CANTIDAD (Uni.)"));
            pCant.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #999999; -fx-border-width: 0 0 1 1;");
            grid.add(pCant, 0, rQ);
        }

        VBox totalBox = new VBox(2, new Label("TOTAL"), new Label(String.valueOf(totalQty)));
        totalBox.setAlignment(Pos.CENTER);
        StackPane totalCell = new StackPane(totalBox);
        totalCell.setStyle("-fx-background-color: #fdf2f2; -fx-border-color: #999999; -fx-border-width: 1;");
        grid.add(totalCell, maxCols + 1, 0, 1, Math.max(1, chunks) * 2);

        container.getChildren().add(grid);
        return container;
    }

    public static VBox createSocksSummarySection(List<DetallePedido> roster) {
        if (roster == null || roster.isEmpty()) return null;
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DetallePedido p : roster) {
            if (p.isIncludeSocks()) {
                String type = resolveAutoSocksType(p);
                counts.put(type, counts.getOrDefault(type, 0) + 1);
            }
        }
        if (counts.isEmpty()) return null;

        VBox box = new VBox(0);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4;");
        box.setMinWidth(PAGE_CONTENT_WIDTH);

        HBox row = new HBox(25);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #f8fbfe;");

        counts.forEach((k, v) -> {
            HBox item = new HBox(10, new Label("MEDIAS " + k + ":"), new Label(v + " Uni."));
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-background-color: white; -fx-padding: 5 12; -fx-border-color: #d1d8e0; -fx-border-radius: 20;");
            item.getChildren().get(0).setStyle("-fx-font-weight: bold; -fx-text-fill: #455a64; -fx-font-size: 11px;");
            item.getChildren().get(1).setStyle("-fx-font-weight: bold; -fx-text-fill: #c0392b; -fx-font-size: 13px;");
            row.getChildren().add(item);
        });

        box.getChildren().add(row);
        return box;
    }

    public static String getSizeDisplay(DetallePedido p) {
        String t = (p.getTalla() == null || p.getTalla().isEmpty() || p.getTalla().matches("(?i)MASCULINO|FEMENINO|HOMBRE|MUJER")) ? "S/N" : p.getTalla();
        if (p.isIncludeBottom() && p.getTallaShort() != null && !p.getTallaShort().isEmpty() && !p.getTallaShort().equals("-") && !p.getTallaShort().equals(t)) {
            return t + " / " + p.getTallaShort();
        }
        return t;
    }

    public static String resolveAutoSocksType(DetallePedido p) {
        if (p.getTipoMedias() != null && !p.getTipoMedias().isBlank())
            return p.getTipoMedias().trim().toUpperCase();
        String t = (p.getTalla() != null) ? p.getTalla().toUpperCase().trim() : "";
        if (t.matches("S|M|L|XL|XXL|3XXL|4XXL|G|XG|2XL|3XL|4XL")) return "ADULTO";
        if (t.matches("12|14|16")) return "JUVENIL";
        if (t.matches("4|6|8")) return "NIÑOS";
        return "PROFESIONAL";
    }

    public static String resolveColorName(Color c) {
        if (c == null || c.equals(Color.WHITE)) return "BLANCO";
        if (c.equals(Color.BLACK)) return "NEGRO";
        if (c.equals(Color.RED)) return "ROJO";
        if (c.equals(Color.YELLOW)) return "AMARILLO";
        if (c.equals(Color.BLUE)) return "AZUL";
        if (c.equals(Color.GREEN) || (c.getRed() < 0.2 && c.getGreen() > 0.8 && c.getBlue() < 0.2)) return "VERDE";
        if (c.equals(Color.ORANGE)) return "NARANJA";
        if (c.equals(Color.PURPLE)) return "MORADO";
        if (c.equals(Color.PINK)) return "ROSADO";
        if (c.equals(Color.GRAY) || c.equals(Color.GREY)) return "GRIS";
        if (c.equals(Color.CYAN) || (c.getRed() < 0.2 && c.getGreen() > 0.8 && c.getBlue() > 0.8)) return "CELESTE";
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }
}
