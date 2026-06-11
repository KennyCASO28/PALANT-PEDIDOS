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
import javafx.scene.transform.Scale;
import org.example.model.DetallePedido;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to manage pagination and structural layout of the technical sheet.
 */
public class TechnicalSheetPaginationManager {

    public static final double A4_WIDTH = 794;
    public static final double A4_HEIGHT = 1123;
    public static final double PAGE_TOP_BAR_HEIGHT = 15;
    public static final double PAGE_FOOTER_HEIGHT = 28;
    public static final double PAGE_SIDE_PADDING = 20;
    public static final double PAGE_CONTENT_WIDTH = A4_WIDTH - (PAGE_SIDE_PADDING * 2);

    private static Image cachedLogo = null;

    public record Chunk(Node node, double height, String id) {}
    public record RosterChunk(Node node, double height) {}
    private record SubListInfo(List<DetallePedido> items, String title) {}

    public static VBox createPageContainer() {
        VBox page = new VBox(15);
        page.setPadding(new Insets(14, 0, 12, 0));
        page.setAlignment(Pos.TOP_CENTER);
        page.setFillWidth(true);
        page.setMinWidth(PAGE_CONTENT_WIDTH);
        page.setPrefWidth(PAGE_CONTENT_WIDTH);
        page.setMaxWidth(PAGE_CONTENT_WIDTH);
        return page;
    }

    public static HBox createHeader(String cliente, String codigo, java.time.LocalDate fechaEntrega, String prioridad, String vendedor) {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMinWidth(PAGE_CONTENT_WIDTH);
        header.setPrefWidth(PAGE_CONTENT_WIDTH);
        header.setMaxWidth(PAGE_CONTENT_WIDTH);
        header.setPadding(new Insets(10));
        header.getStyleClass().add("ficha-header-main");

        HBox leftBox = new HBox(15);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        try {
            if (cachedLogo == null) {
                URL url = TechnicalSheetPaginationManager.class.getResource("/vectors/LOGO PALANT-3.png");
                if (url != null) cachedLogo = new Image(url.toExternalForm(), 400, 0, true, true);
            }
            if (cachedLogo != null) {
                ImageView iv = new ImageView(cachedLogo);
                iv.setPreserveRatio(true);
                iv.setFitHeight(40);
                leftBox.getChildren().add(iv);
            }
        } catch (Exception e) {}

        Label lblTitle = new Label("FICHA TÉCNICA");
        lblTitle.getStyleClass().add("ficha-header-title");
        leftBox.getChildren().add(lblTitle);
        HBox.setHgrow(leftBox, Priority.ALWAYS);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15); infoGrid.setVgap(2);
        infoGrid.setAlignment(Pos.CENTER_RIGHT);

        addHeaderField(infoGrid, "PEDIDO:", codigo, 0, 0);
        addHeaderField(infoGrid, "VENDEDOR:", (vendedor != null ? vendedor : "-"), 1, 0);

        String dateStr = "-";
        if (fechaEntrega != null) {
            dateStr = fechaEntrega.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
            if (prioridad != null && !prioridad.isEmpty()) dateStr += " (" + prioridad + ")";
        }
        addHeaderField(infoGrid, "ENTREGA:", dateStr, 0, 1);
        addHeaderField(infoGrid, "CLIENTE:", cliente, 1, 1);

        header.getChildren().addAll(leftBox, infoGrid);
        return header;
    }

    private static void addHeaderField(GridPane grid, String label, String value, int col, int row) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label), v = new Label(value != null ? value.toUpperCase() : "-");
        l.getStyleClass().add("ficha-header-field-label");
        v.getStyleClass().add("ficha-header-field-value");
        box.getChildren().addAll(l, v);
        grid.add(box, col, row);
    }

    public static Node decorate(VBox content, boolean isFirstPage) {
        VBox wrapper = new VBox(0);
        wrapper.getStyleClass().add("ficha-page-wrapper");
        wrapper.setMinSize(A4_WIDTH, A4_HEIGHT);
        wrapper.setMaxSize(A4_WIDTH, A4_HEIGHT);

        Rectangle topBar = new Rectangle(A4_WIDTH, PAGE_TOP_BAR_HEIGHT, Color.rgb(100, 180, 50));
        wrapper.getChildren().add(topBar);

        double bodyH = A4_HEIGHT - PAGE_TOP_BAR_HEIGHT - PAGE_FOOTER_HEIGHT;
        StackPane body = new StackPane();
        body.setAlignment(Pos.TOP_CENTER);
        body.setMinHeight(bodyH);
        body.setPadding(new Insets(0, PAGE_SIDE_PADDING, 0, PAGE_SIDE_PADDING));

        StackPane fitWrapper = new StackPane(content);
        fitWrapper.setAlignment(Pos.TOP_CENTER);
        fitWrapper.setPrefWidth(PAGE_CONTENT_WIDTH);

        if (isFirstPage) {
            Scale scale = new Scale(1, 1, 0, 0);
            fitWrapper.getTransforms().add(scale);
            Runnable fit = () -> {
                double cw = Math.max(1, content.prefWidth(PAGE_CONTENT_WIDTH));
                double ch = Math.max(1, content.prefHeight(PAGE_CONTENT_WIDTH));
                double s = Math.min(PAGE_CONTENT_WIDTH / cw, bodyH / ch);
                if (s > 1.0) s = 1.0;
                scale.setX(s); scale.setY(s);
                fitWrapper.setTranslateX((cw - (cw * s)) / 2.0);
            };
            content.layoutBoundsProperty().addListener((o, ov, nv) -> fit.run());
            javafx.application.Platform.runLater(fit);
        }

        body.getChildren().add(fitWrapper);
        wrapper.getChildren().add(body);

        StackPane footer = new StackPane(new Label("PALANT - ORIGINAL COMO TÚ"));
        footer.setMinHeight(PAGE_FOOTER_HEIGHT);
        footer.getStyleClass().add("ficha-footer");
        footer.getChildren().get(0).getStyleClass().add("ficha-footer-label");
        wrapper.getChildren().add(footer);

        return wrapper;
    }

    public static List<RosterChunk> createRosterSectionList(List<DetallePedido> jugadores, boolean hasArq) {
        List<RosterChunk> result = new ArrayList<>();
        List<SubListInfo> subLists = new ArrayList<>();
        Set<String> genders = new LinkedHashSet<>();
        jugadores.forEach(p -> genders.add(p.getGenero() != null ? p.getGenero().toUpperCase() : "HOMBRE"));

        List<String> sortedGenders = new ArrayList<>(genders);
        sortedGenders.sort((g1, g2) -> g1.equals("HOMBRE") ? -1 : (g2.equals("HOMBRE") ? 1 : g1.compareTo(g2)));

        for (String gender : sortedGenders) {
            List<DetallePedido> genderList = jugadores.stream().filter(p -> !p.isEsArquero() && (p.getGenero() != null ? p.getGenero().toUpperCase() : "HOMBRE").equals(gender)).collect(Collectors.toList());
            if (genderList.isEmpty()) continue;

            Map<String, List<DetallePedido>> groups = new LinkedHashMap<>();
            for (DetallePedido p : genderList) {
                String botK = (p.getTipoBottom() != null && !p.getTipoBottom().isEmpty()) ? p.getTipoBottom().toUpperCase() : "SHORT";
                String key = (p.isIncludeTop() ? "T" : "F") + (p.isIncludeBottom() ? "T" : "F") + (p.isIncludeSocks() ? "T" : "F") + "_" + botK;
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }

            groups.forEach((key, subset) -> {
                String typeK = key.split("_")[0], botL = key.split("_").length > 1 ? key.split("_")[1] : "SHORT";
                String baseTitle = "LISTA";
                boolean t = typeK.charAt(0) == 'T', b = typeK.charAt(1) == 'T', s = typeK.charAt(2) == 'T';
                if (t && b && s) baseTitle = "JUEGO COMPLETO"; 
                else if (t && !b && !s) baseTitle = "SOLO CAMISETA"; 
                else if (!t && b && !s) baseTitle = "SOLO " + botL; 
                else if (t && b && !s) baseTitle = "CAMISETA Y " + botL;
                
                final String title = baseTitle;

                Map<String, List<DetallePedido>> bySleeve = new LinkedHashMap<>();
                subset.forEach(p -> bySleeve.computeIfAbsent(p.getTipoManga() == null ? "CORTA" : p.getTipoManga(), k -> new ArrayList<>()).add(p));
                bySleeve.forEach((m, list) -> {
                    String gT = gender.equals("HOMBRE") ? "VARON" : (gender.equals("MUJER") ? "DAMA" : gender);
                    subLists.add(new SubListInfo(list, title + " - CORTE " + gT + (t ? " - MANGA " + m : "")));
                });
            });
        }

        int usage = 0; boolean first = true; List<SubListInfo> buffer = new ArrayList<>();
        for (SubListInfo sub : subLists) {
            List<DetallePedido> rem = new ArrayList<>(sub.items);
            boolean p1 = true;
            while (!rem.isEmpty()) {
                int lim = (first && hasArq) ? 12 : 40, space = lim - usage;
                if (space <= 0) { result.addAll(flushPage(buffer)); buffer.clear(); usage = 0; first = false; space = 40; }
                int take = Math.min(rem.size(), space);
                buffer.add(new SubListInfo(new ArrayList<>(rem.subList(0, take)), sub.title + (p1 ? "" : " (CONTINUACION)")));
                usage += take; p1 = false; rem = rem.subList(take, rem.size());
            }
        }
        if (!buffer.isEmpty()) result.addAll(flushPage(buffer));
        return result;
    }

    private static List<RosterChunk> flushPage(List<SubListInfo> buffer) {
        List<RosterChunk> chunks = new ArrayList<>();
        buffer.forEach(bi -> chunks.addAll(createSmartTable(bi.items, bi.title, false)));
        return chunks;
    }

    public static List<RosterChunk> createSmartTable(List<DetallePedido> roster, String title, boolean limitFirst) {
        List<RosterChunk> chunks = new ArrayList<>();
        List<DetallePedido> det = new ArrayList<>(), com = new ArrayList<>(), sim = new ArrayList<>();
        roster.forEach(p -> {
            if (p.getNombre() != null && !p.getNombre().isBlank() && !p.getNombre().equalsIgnoreCase("JUGADOR")) det.add(p);
            else if (p.getNumero() != null && !p.getNumero().isBlank() && !p.getNumero().equals("-")) com.add(p);
            else sim.add(p);
        });

        if (!sim.isEmpty()) {
            VBox box = buildContainer(title, " (RESUMEN)"); box.getChildren().add(createSimpleGrid(sim, PAGE_CONTENT_WIDTH));
            chunks.add(new RosterChunk(box, 40 + (new HashSet<>(sim.stream().map(TechnicalSheetDataService::getSizeDisplay).toList()).size() * 30) + 20));
        }
        if (!com.isEmpty()) {
            VBox box = buildContainer(title, " (NUMERACION)"); box.getChildren().add(createCompactGrid(com, PAGE_CONTENT_WIDTH));
            chunks.add(new RosterChunk(box, 40 + (new HashSet<>(com.stream().map(TechnicalSheetDataService::getSizeDisplay).toList()).size() * 30) + 20));
        }
        if (!det.isEmpty()) {
            int i = 0; while (i < det.size()) {
                int take = Math.min(det.size() - i, (i == 0 && limitFirst) ? 12 : 40);
                VBox box = buildContainer(title, i == 0 ? "" : " (CONT)"); box.getChildren().add(createMergedRoster(det.subList(i, i + take), PAGE_CONTENT_WIDTH));
                chunks.add(new RosterChunk(box, 32 + (take * 30) + 40)); i += take;
            }
        }
        return chunks;
    }

    private static VBox buildContainer(String t, String s) {
        VBox b = new VBox(0); b.getStyleClass().add("ficha-table-container");
        if (t != null) { 
            Label l = new Label(t + s); 
            l.setMinWidth(PAGE_CONTENT_WIDTH-2); 
            l.setAlignment(Pos.CENTER); 
            l.getStyleClass().add("ficha-table-header");
            b.getChildren().add(l); 
        }
        return b;
    }

    private static GridPane createSimpleGrid(List<DetallePedido> list, double w) {
        GridPane g = new GridPane(); g.setMinWidth(w-2); Map<String, Integer> m = new LinkedHashMap<>();
        list.forEach(p -> { String s = TechnicalSheetDataService.getSizeDisplay(p); m.put(s, m.getOrDefault(s, 0) + 1); });
        int r = 0; for (Map.Entry<String, Integer> e : m.entrySet()) {
            String bgClass = (r % 2 == 0) ? "ficha-row-even" : "ficha-row-odd";
            addCell(g, e.getKey(), 0, r, bgClass, Pos.CENTER, 100);
            addCell(g, String.valueOf(e.getValue()), 1, r, bgClass, Pos.CENTER, w - 102);
            r++;
        }
        return g;
    }

    private static GridPane createCompactGrid(List<DetallePedido> list, double w) {
        GridPane g = new GridPane(); g.setMinWidth(w-2); Map<String, List<String>> m = new LinkedHashMap<>();
        list.forEach(p -> m.computeIfAbsent(TechnicalSheetDataService.getSizeDisplay(p), k -> new ArrayList<>()).add(p.getNumero()));
        int r = 0; for (Map.Entry<String, List<String>> e : m.entrySet()) {
            String bgClass = (r % 2 == 0) ? "ficha-row-even" : "ficha-row-odd";
            addCell(g, e.getKey(), 0, r, bgClass, Pos.CENTER, 80);
            addCell(g, String.valueOf(e.getValue().size()), 1, r, bgClass, Pos.CENTER, 60);
            addCell(g, String.join(" - ", e.getValue()), 2, r, bgClass, Pos.CENTER_LEFT, w - 142);
            r++;
        }
        return g;
    }

    private static GridPane createMergedRoster(List<DetallePedido> list, double w) {
        GridPane g = new GridPane(); g.setMinWidth(w-2);
        addCell(g, "TALLA", 0, 0, "ficha-table-header-dark", Pos.CENTER, 80); 
        addCell(g, "NOMBRE", 1, 0, "ficha-table-header-dark", Pos.CENTER, w - 160); 
        addCell(g, "NUMERO", 2, 0, "ficha-table-header-dark", Pos.CENTER, 80);
        int r = 1; Map<String, List<DetallePedido>> m = new LinkedHashMap<>();
        list.forEach(p -> m.computeIfAbsent(TechnicalSheetDataService.getSizeDisplay(p), k -> new ArrayList<>()).add(p));
        for (Map.Entry<String, List<DetallePedido>> e : m.entrySet()) {
            StackPane s = new StackPane(new Label(e.getKey())); 
            s.getStyleClass().add("ficha-row-side");
            g.add(s, 0, r); GridPane.setRowSpan(s, e.getValue().size());
            for (DetallePedido p : e.getValue()) {
                String bgClass = (r % 2 == 0) ? "ficha-row-even" : "ficha-row-odd";
                addCell(g, p.getNombre(), 1, r, bgClass, Pos.CENTER_LEFT, w - 160);
                addCell(g, p.getNumero(), 2, r, bgClass, Pos.CENTER, 80);
                r++;
            }
        }
        return g;
    }

    private static void addCell(GridPane g, String t, int c, int r, String bgClass, Pos a, double w) {
        Label l = new Label(t == null ? "" : t); 
        l.setAlignment(a); 
        l.setMinWidth(w); 
        l.getStyleClass().add("ficha-table-cell");
        StackPane p = new StackPane(l); 
        p.getStyleClass().add(bgClass);
        p.setAlignment(a); g.add(p, c, r);
    }

    public static double getPrintableContentHeight() {
        return A4_HEIGHT - 60;
    }
}
