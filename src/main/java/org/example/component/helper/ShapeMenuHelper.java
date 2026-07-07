package org.example.component.helper;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.example.component.ShapeLayer;
import org.example.component.ImageLayer;
import org.example.component.TextLayer;
import org.example.utils.UIFactory;

/**
 * Helper to externalize and build context menus for ShapeLayer.
 */
public class ShapeMenuHelper {

    public static ContextMenu showContextMenu(ShapeLayer layer, double screenX, double screenY) {
        // 1. Ensure Selection
        if (!layer.isSelected()) {
            if (layer.getOnSelectionRequested() != null) {
                MouseEvent synthetic = new MouseEvent(
                        MouseEvent.MOUSE_PRESSED,
                        0, 0, screenX, screenY,
                        MouseButton.PRIMARY, 1,
                        false, false, false, false,
                        true, false, false, true, false, false, null);
                layer.getOnSelectionRequested().accept(synthetic);
            } else {
                layer.setSelected(true);
            }
        }

        ContextMenu menu = new ContextMenu();
        menu.setAutoHide(true);

        boolean isClipped = (layer.getActiveZone() != null);
        boolean isEditingThis = layer.isBeingEdited() || (isClipped && layer.getVisualizer() != null
                && layer.getVisualizer().getPowerClipManager().isEditing()
                && layer.getActiveZone().equals(layer.getVisualizer().getPowerClipManager().getCurrentEditingZone()));

        // --- 1. CLIPPED (OUTSIDE): Minimal Menu ---
        if (isClipped && !isEditingThis) {
            MenuItem editZone = new MenuItem("EDITAR CONTENIDO: " + layer.getActiveZone().toUpperCase());
            editZone.setGraphic(UIFactory.crearIcono("mdi2c-crop-free", 16, "#27ae60"));
            editZone.setOnAction(ev -> {
                if (layer.getVisualizer() != null)
                    layer.getVisualizer().getPowerClipManager().enterEditMode(layer.getActiveZone());
            });
            menu.getItems().add(editZone);

            if (ShapeLayer.hasClipboard() || ImageLayer.hasClipboard() || TextLayer.hasClipboard()) {
                menu.getItems().add(new SeparatorMenuItem());
                addPasteOptionsToShapeMenu(menu, layer);
            }
            menu.show(layer, screenX, screenY);
            return menu;
        }

        // --- 2. FLOATING / EDITING: Full Hierarchy ---
        // A. Header
        MenuItem header = new MenuItem("Figura");
        header.setDisable(true);
        header.setStyle("-fx-font-weight: bold; -fx-opacity: 1.0; -fx-text-fill: #95a5a6; -fx-font-size: 11px;");
        menu.getItems().add(header);

        // B. PowerClip (Only for floating)
        if (!isClipped && layer.getPowerClipHandler() != null) {
            Menu pcMenu = new Menu("PowerClip (Insertar en...)");
            pcMenu.setGraphic(UIFactory.crearIcono("mdi2a-arrow-right-bold-box", 16, "#555"));
            if (layer.getVisualizer() != null) {
                java.util.List<String> available = layer.getVisualizer().getAvailableZones();
                if (available.contains("PECHO")) {
                    addZoneMenuItem(pcMenu, layer, "PECHO", "mdi2t-tshirt-crew", "#2c3e50", true);
                    addZoneMenuItem(pcMenu, layer, "ESPALDA", "mdi2t-tshirt-crew-outline", "#2c3e50", false);
                }
                if (available.contains("MANGA_DELANTERA")) {
                    pcMenu.getItems().add(new SeparatorMenuItem());
                    addZoneMenuItem(pcMenu, layer, "MANGA_DELANTERA", "mdi2a-arm-flex", "#16a085", true);
                    addZoneMenuItem(pcMenu, layer, "MANGA_TRASERA", "mdi2a-arm-flex-outline", "#16a085", false);
                }
                if (available.contains("SHORT_FRONT")) {
                    pcMenu.getItems().add(new SeparatorMenuItem());
                    addZoneMenuItem(pcMenu, layer, "SHORT_FRONT", "mdi2v-view-column", "#d35400", true);
                    addZoneMenuItem(pcMenu, layer, "SHORT_BACK", "mdi2v-view-column-outline", "#d35400", false);
                }
            }
            if (!pcMenu.getItems().isEmpty()) {
                menu.getItems().add(pcMenu);
            }
        }
        menu.getItems().add(new SeparatorMenuItem());

        // C. Clipboard
        MenuItem copyItem = new MenuItem("Copiar");
        copyItem.setGraphic(UIFactory.crearIcono("mdi2c-content-copy", 16, "#555"));
        copyItem.setOnAction(ev -> layer.copyToClipboard());

        MenuItem cutItem = new MenuItem("Cortar");
        cutItem.setGraphic(UIFactory.crearIcono("mdi2c-content-cut", 16, "#555"));
        cutItem.setOnAction(ev -> layer.cutToClipboard());

        menu.getItems().addAll(copyItem, cutItem);

        // D. Full Tools
        menu.getItems().add(new SeparatorMenuItem());



        // Order
        Menu orderMenu = new Menu("Orden / Capas");
        orderMenu.setGraphic(UIFactory.crearIcono("mdi2l-layers", 16, "#555"));
        MenuItem itemFront = new MenuItem("Traer al Frente");
        itemFront.setGraphic(UIFactory.crearIcono("mdi2c-chevron-double-up", 16, "#27ae60"));
        itemFront.setOnAction(ev -> layer.zBringToFront());
        MenuItem itemUp = new MenuItem("Subir Capa");
        itemUp.setGraphic(UIFactory.crearIcono("mdi2c-chevron-up", 16, "#16a085"));
        itemUp.setOnAction(ev -> layer.zBringForward());
        MenuItem itemDown = new MenuItem("Bajar Capa");
        itemDown.setGraphic(UIFactory.crearIcono("mdi2c-chevron-down", 16, "#e67e22"));
        itemDown.setOnAction(ev -> layer.zSendBackward());
        MenuItem itemBack = new MenuItem("Enviar al Fondo");
        itemBack.setGraphic(UIFactory.crearIcono("mdi2c-chevron-double-down", 16, "#c0392b"));
        itemBack.setOnAction(ev -> layer.zSendToBack());
        orderMenu.getItems().addAll(itemFront, itemUp, itemDown, itemBack);
        menu.getItems().add(orderMenu);

        // Transform
        Menu transMenu = new Menu("Transformar");
        transMenu.setGraphic(UIFactory.crearIcono("mdi2f-format-rotate-90", 16, "#555"));
        MenuItem rot90 = new MenuItem("Girar 90°");
        rot90.setGraphic(UIFactory.crearIcono("mdi2r-rotate-right", 16, "#3498db"));
        rot90.setOnAction(ev -> layer.rotateBy(90));
        MenuItem flipH = new MenuItem("Espejo Horizontal");
        flipH.setGraphic(UIFactory.crearIcono("mdi2f-flip-horizontal", 16, "#e67e22"));
        flipH.setOnAction(ev -> layer.flipHorizontal());
        MenuItem flipV = new MenuItem("Espejo Vertical");
        flipV.setGraphic(UIFactory.crearIcono("mdi2f-flip-vertical", 16, "#9b59b6"));
        flipV.setOnAction(ev -> layer.flipVertical());
        MenuItem resetTrans = new MenuItem("Resetear Transformaciones");
        resetTrans.setGraphic(UIFactory.crearIcono("mdi2r-restore", 16, "#c0392b"));
        resetTrans.setOnAction(ev -> layer.resetTransforms());
        transMenu.getItems().addAll(rot90, flipH, flipV, new SeparatorMenuItem(), resetTrans);
        menu.getItems().add(transMenu);

        // Extract
        if (isClipped) {
            MenuItem extractItem = new MenuItem("Extraer Objeto");
            extractItem.setGraphic(UIFactory.crearIcono("mdi2e-eject", 16, "#555"));
            extractItem.setOnAction(ev -> {
                if (layer.getPowerClipHandler() != null)
                    layer.getPowerClipHandler().accept(null);
            });
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(extractItem);
        }

        // Delete
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem deleteItem = new MenuItem("Eliminar");
        deleteItem.setGraphic(UIFactory.crearIcono("mdi2d-delete", 16, "#e74c3c"));
        deleteItem.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        deleteItem.setOnAction(ev -> layer.removeFromParent());
        menu.getItems().add(deleteItem);

        menu.show(layer, screenX, screenY);
        return menu;
    }

    private static void addPasteOptionsToShapeMenu(ContextMenu menu, ShapeLayer layer) {
        if (ShapeLayer.hasClipboard()) {
            MenuItem pasteItem = new MenuItem("Pegar Figura");
            pasteItem.setGraphic(UIFactory.crearIcono("mdi2c-content-paste", 16, "#555"));
            pasteItem.setOnAction(ev -> {
                if (layer.getPasteHandler() != null)
                    layer.getPasteHandler().run();
            });
            menu.getItems().add(pasteItem);
        }
    }

    private static String formatZoneName(String zone, ShapeLayer layer) {
        if (zone == null)
            return "Global";

        // Check for Shorts logic
        if (zone.startsWith("SHORT")) {
            org.example.model.TipoCorte corte = org.example.model.TipoCorte.PANTALONETA; // Default
            if (layer != null && layer.getVisualizer() != null && layer.getVisualizer().getState() != null) {
                corte = layer.getVisualizer().getState().getCorteShort();
            }

            String base = "Short";
            boolean isFem = false;

            if (corte == org.example.model.TipoCorte.PANTALONETA) {
                base = "Pantaloneta";
                isFem = true;
            } else if (corte == org.example.model.TipoCorte.LICRA) {
                base = "Licra";
                isFem = true;
            }

            String suffix = "";
            if (zone.endsWith("FRONT")) {
                suffix = isFem ? "Delantera" : "Delantero";
            } else {
                suffix = isFem ? "Trasera" : "Trasero";
            }
            return base + " " + suffix;
        }

        switch (zone) {
            case "PECHO":
                return "Pecho";
            case "ESPALDA":
                return "Espalda";
            case "MANGA_DELANTERA":
                return "Manga Delantera";
            case "MANGA_TRASERA":
                return "Manga Trasera";
            default:
                String s = zone.replace("_", " ").toLowerCase();
                return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    private static void addZoneMenuItem(Menu parent, ShapeLayer layer, String zone, String icon, String color,
            boolean bold) {
        String label = formatZoneName(zone, layer);
        MenuItem item = new MenuItem(label);
        item.setGraphic(UIFactory.crearIcono(icon, 16, color));
        item.setStyle("-fx-text-fill: " + color + (bold ? "; -fx-font-weight: bold;" : ""));
        item.setOnAction(ev -> layer.getPowerClipHandler().accept(zone));
        parent.getItems().add(item);
    }
}

