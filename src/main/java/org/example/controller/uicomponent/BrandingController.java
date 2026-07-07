package org.example.controller.uicomponent;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.example.component.PrendaVisualizer;
import org.example.utils.UIFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BrandingController {

    private final PrendaVisualizer visualizer;
    private final String sectionHeaderStyle = "-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;";

    private final java.util.function.BiConsumer<java.util.function.Consumer<Color>, java.util.function.Consumer<Color>> eyedropperCallback;

    // Shield Gallery State
    private final java.util.Map<String, FlowPane> shieldGalleries = new java.util.HashMap<>();
    private final java.util.Map<String, List<Image>> shieldImagesMap = new java.util.HashMap<>();

    // Tech groups per zone
    private final java.util.Map<String, ToggleGroup> techGroups = new java.util.HashMap<>();

    private java.util.function.Consumer<org.example.model.TipoEscudo> onBadgeTypeChange;

    // Referencias para sincronización cruzada en Short
    private CheckBox uiShortCrestCheckbox;
    private CheckBox uiShortBadgeToggle;
    
    private ComboBox<String> cmbShirtBrandTechUI;
    private ComboBox<String> cmbShortBrandTechUI;

    public void setOnBadgeTypeChange(java.util.function.Consumer<org.example.model.TipoEscudo> listener) {
        this.onBadgeTypeChange = listener;
    }

    public void setBadgeType(String zone, org.example.model.TipoEscudo type) {
        if (type == null || zone == null)
            return;
        ToggleGroup group = techGroups.get(zone);
        if (group != null) {
            for (Toggle t : group.getToggles()) {
                if (t.getUserData() == type) {
                    t.setSelected(true);
                    break;
                }
            }
        }
    }

    public void setBadgeType(org.example.model.TipoEscudo type) {
        setBadgeType("SHIRT", type);
    }

    public BrandingController(PrendaVisualizer visualizer,
            java.util.function.BiConsumer<java.util.function.Consumer<Color>, java.util.function.Consumer<Color>> eyedropperCallback) {
        this.visualizer = visualizer;
        this.eyedropperCallback = eyedropperCallback;

        // Listen for Selection Changes from Visualizer (Bi-directional Sync)
        if (this.visualizer.getLayerManager() != null) {
            this.visualizer.getLayerManager().setOnSelectionChanged(layer -> {
                if (layer instanceof org.example.component.ImageLayer) {
                    org.example.component.ImageLayer il = (org.example.component.ImageLayer) layer;
                    // If it's a Badge, sync the UI buttons
                    if (il.getBadgeType() != null && il.getBadgeType() != org.example.model.TipoEscudo.NINGUNO) {
                        setBadgeType(il.getBadgeType());
                    }
                }
            });
        }
        // Initialize maps
        shieldImagesMap.put("SHIRT", new ArrayList<>());
        shieldImagesMap.put("SHORT", new ArrayList<>());
        shieldImagesMap.put("SLEEVE", new ArrayList<>());

        shieldGalleries.put("SHIRT", new FlowPane(10, 10));
        shieldGalleries.put("SHORT", new FlowPane(10, 10));
        shieldGalleries.put("SLEEVE", new FlowPane(10, 10));
    }

    private VBox mainContainer;

    public void addToContainer(VBox container) {
        this.mainContainer = container;
        updateUI();
    }

    private boolean isUpdatingUI = false;

    public void updateUI() {
        if (mainContainer == null)
            return;

        isUpdatingUI = true;
        try {
            mainContainer.getChildren().clear();

            VBox content = new VBox(10);
            content.setPadding(new Insets(5, 12, 5, 12)); 
            content.setStyle("-fx-background-color: white;");
            content.setFillWidth(true);
            content.setMaxWidth(Double.MAX_VALUE);

            mainContainer.getChildren().add(content);
            VBox.setVgrow(content, Priority.ALWAYS);

            // 1. SHIRT BRANDING
            if (visualizer.hasShirt()) {
                VBox brandSection = new VBox(8);
                // ... remaining sections added to 'content' instead of 'mainContainer' ...

                brandSection.setStyle(
                        "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 14; -fx-border-color: #ecf0f1; -fx-border-radius: 8;");
                brandSection.setMaxWidth(Double.MAX_VALUE);
                brandSection.setFillWidth(true);

                HBox headerBox = new HBox(10);
                headerBox.setAlignment(Pos.CENTER_LEFT);
                Label lblBrand = new Label("Marca PALANT (Camiseta)");
                lblBrand.setStyle(sectionHeaderStyle);
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                CheckBox chkBrandBox = UIFactory.crearToggleSwitch(visualizer.isChestBrandVisible());
                headerBox.getChildren().addAll(lblBrand, spacer, chkBrandBox);
                brandSection.getChildren().add(headerBox);

                VBox controlsCols = new VBox(8);
                controlsCols.setPadding(new Insets(5, 0, 0, 0));
                controlsCols.managedProperty().bind(chkBrandBox.selectedProperty());
                controlsCols.visibleProperty().bind(chkBrandBox.selectedProperty());

                GridPane row1 = createTwoColumnGrid();

                ComboBox<String> cmbBrandPos = new ComboBox<>();
                cmbBrandPos.getItems().addAll("Derecha", "Centro");
                cmbBrandPos.setValue(visualizer.getState().getChestBrandPosition().equals("centro") ? "Centro" : "Derecha");
                cmbBrandPos.setMaxWidth(Double.MAX_VALUE);
                UIFactory.fixComboBoxReadability(cmbBrandPos);
                
                // Add icons to position items if possible via a cell factory
                cmbBrandPos.setCellFactory(lv -> new ListCell<String>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setGraphic(null); setText(null); }
                        else {
                            setText(item);
                            if (item.equals("Derecha")) setGraphic(UIFactory.crearIcono("mdi2a-align-horizontal-right", 14, "#34495e"));
                            else setGraphic(UIFactory.crearIcono("mdi2a-align-horizontal-center", 14, "#34495e"));
                        }
                    }
                });
                cmbBrandPos.setButtonCell(cmbBrandPos.getCellFactory().call(null));

                ComboBox<String> cmbBrandTech = new ComboBox<>();
                cmbBrandTech.getItems().addAll("Bordado", "Sublimado", "Vinil");
                // Sync with Visualizer state or Default
                cmbBrandTech.setValue(visualizer.getBrandTech() != null ? visualizer.getBrandTech() : "Bordado");
                cmbBrandTech.setMaxWidth(Double.MAX_VALUE);
                UIFactory.fixComboBoxReadability(cmbBrandTech);
                cmbBrandTech.valueProperty().addListener((obs, old, val) -> {
                    if (!isUpdatingUI && val != null) {
                        visualizer.setBrandTech(val);
                        if (cmbShortBrandTechUI != null && !val.equals(cmbShortBrandTechUI.getValue())) {
                            cmbShortBrandTechUI.setValue(val);
                        }
                    }
                });
                this.cmbShirtBrandTechUI = cmbBrandTech;

                MenuButton[] cpBrandBtnArr = new MenuButton[1];
                MenuButton cpBrandBtn = UIFactory.createColorMenuButton(
                        visualizer.getPartColor("brandChest", Color.BLACK),
                        "Color de Marca",
                        newColor -> {
                            if (!isUpdatingUI) {
                                Color oldColor = visualizer.getPartColor("brandChest", Color.BLACK);
                                if (!newColor.equals(oldColor)) {
                                    visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                                            "Chest Brand Color", oldColor, newColor,
                                            c -> {
                                                UIFactory.setColorMenuButtonColor(cpBrandBtnArr[0], c);
                                                visualizer.setChestBrandColor(c, true);
                                            }));
                                    visualizer.setChestBrandColor(newColor, true);
                                }
                            }
                        },
                        (onCommit, onPreview) -> {
                            eyedropperCallback.accept(
                                    color -> {
                                        if (color == null) return;
                                        Color oldColor = visualizer.getPartColor("brandChest", Color.BLACK);
                                        if (!color.equals(oldColor)) {
                                            visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                                                    "Chest Brand Color", oldColor, color,
                                                    c -> {
                                                        UIFactory.setColorMenuButtonColor(cpBrandBtnArr[0], c);
                                                        visualizer.setChestBrandColor(c, true);
                                                    }));
                                            onCommit.accept(color);
                                            visualizer.setChestBrandColor(color, true);
                                        }
                                    },
                                    color -> {
                                        if (color == null) visualizer.clearPreviewColors();
                                        else visualizer.setPreviewColor("brandChest", color);
                                    });
                });
                cpBrandBtnArr[0] = cpBrandBtn;
                cpBrandBtn.setMaxWidth(Double.MAX_VALUE);

                // Logic
                Runnable updateBrand = () -> {
                    if (isUpdatingUI) return;
                    if (chkBrandBox.isSelected()) {
                        String filename = cmbBrandPos.getValue().equals("Centro") ? "centro" : "derecha";
                        visualizer.loadBrandVector("pecho", filename);
                        visualizer.setChestBrandColor((Color) cpBrandBtn.getUserData(), true);
                    } else {
                        visualizer.loadBrandVector("pecho", "");
                    }
                };
                
                chkBrandBox.selectedProperty().addListener((obs, old, val) -> updateBrand.run());
                cmbBrandPos.valueProperty().addListener((obs, old, val) -> updateBrand.run());

                row1.add(cmbBrandPos, 0, 0);
                row1.add(cmbBrandTech, 1, 0);

                controlsCols.getChildren().addAll(row1, cpBrandBtn);
                brandSection.getChildren().add(controlsCols);
                content.getChildren().add(brandSection);
            }

            // 2. SHORTS BRANDING
            if (visualizer.hasShorts()) {
                org.example.model.TipoCorte cut = visualizer.getCurrentCorteShort();

                VBox shortSection = new VBox(8);
                shortSection.setStyle(
                        "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 14; -fx-border-color: #ecf0f1; -fx-border-radius: 8;");
                shortSection.setMaxWidth(Double.MAX_VALUE);
                shortSection.setFillWidth(true);

                // Dynamic Label based on Cut
                String labelText = "Marca en Short";
                if (cut == org.example.model.TipoCorte.LICRA)
                    labelText = "Marca en Licra";
                else if (cut == org.example.model.TipoCorte.PANTALONETA)
                    labelText = "Marca en Pantaloneta";

                HBox headerShort = new HBox(10);
                headerShort.setAlignment(Pos.CENTER_LEFT);
                Label lblShort = new Label(labelText);
                lblShort.setStyle(sectionHeaderStyle);
                
                Region spacerS = new Region();
                HBox.setHgrow(spacerS, Priority.ALWAYS);
                
                CheckBox chkShortBrand = UIFactory.crearToggleSwitch(visualizer.isShortBrandVisible());
                headerShort.getChildren().addAll(lblShort, spacerS, chkShortBrand);
                shortSection.getChildren().add(headerShort);

                VBox shortControls = new VBox(8);
                shortControls.managedProperty().bind(chkShortBrand.selectedProperty());
                shortControls.visibleProperty().bind(chkShortBrand.selectedProperty());
                shortControls.setPadding(new Insets(5, 0, 0, 0));

                GridPane shortRow1 = createTwoColumnGrid();

                ComboBox<String> cmbShortBrandPos = new ComboBox<>();
                cmbShortBrandPos.getItems().add("Pierna Izquierda");
                cmbShortBrandPos.setValue("Pierna Izquierda");
                cmbShortBrandPos.setMaxWidth(Double.MAX_VALUE);
                UIFactory.fixComboBoxReadability(cmbShortBrandPos);

                ComboBox<String> cmbShortBrandTech = new ComboBox<>();
                cmbShortBrandTech.getItems().addAll("Bordado", "Sublimado", "Vinil");
                cmbShortBrandTech.setValue(visualizer.getBrandTech() != null ? visualizer.getBrandTech() : "Bordado");
                cmbShortBrandTech.setMaxWidth(Double.MAX_VALUE);
                UIFactory.fixComboBoxReadability(cmbShortBrandTech);
                cmbShortBrandTech.valueProperty().addListener((obs, old, val) -> {
                    if (!isUpdatingUI && val != null) {
                        visualizer.setBrandTech(val);
                        if (cmbShirtBrandTechUI != null && !val.equals(cmbShirtBrandTechUI.getValue())) {
                            cmbShirtBrandTechUI.setValue(val);
                        }
                    }
                });
                this.cmbShortBrandTechUI = cmbShortBrandTech;

                MenuButton[] cpShortBrandBtnArr = new MenuButton[1];
                MenuButton cpShortBrandBtn = UIFactory.createColorMenuButton(
                        visualizer.getPartColor("brandShort", Color.BLACK),
                        "Color de Marca",
                        newColor -> {
                            if (!isUpdatingUI) {
                                Color oldColor = visualizer.getPartColor("brandShort", Color.BLACK);
                                if (!newColor.equals(oldColor)) {
                                    visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                                            "Short Brand Color", oldColor, newColor,
                                            c -> {
                                                UIFactory.setColorMenuButtonColor(cpShortBrandBtnArr[0], c);
                                                visualizer.setShortBrandColor(c, true);
                                            }));
                                    visualizer.setShortBrandColor(newColor, true);
                                }
                            }
                        },
                        (onCommit, onPreview) -> {
                            eyedropperCallback.accept(
                                    color -> {
                                        if (color == null) return;
                                        Color oldColor = visualizer.getPartColor("brandShort", Color.BLACK);
                                        if (!color.equals(oldColor)) {
                                            visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                                                    "Short Brand Color", oldColor, color,
                                                    c -> {
                                                        UIFactory.setColorMenuButtonColor(cpShortBrandBtnArr[0], c);
                                                        visualizer.setShortBrandColor(c, true);
                                                    }));
                                            onCommit.accept(color);
                                            visualizer.setShortBrandColor(color, true);
                                        }
                                    },
                                    color -> {
                                        if (color == null) visualizer.clearPreviewColors();
                                        else visualizer.setPreviewColor("brandShort", color);
                                    });
                        });
                cpShortBrandBtnArr[0] = cpShortBrandBtn;
                cpShortBrandBtn.setMaxWidth(Double.MAX_VALUE);
                  
                CheckBox chkShortCrest = new CheckBox("¿Lleva Escudo?");
                chkShortCrest.setSelected(visualizer.isShortCrestVisible());
                this.uiShortCrestCheckbox = chkShortCrest;
                chkShortCrest.setTooltip(new Tooltip("Mueve el contenido para dejar espacio al escudo"));
                chkShortCrest.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22; -fx-font-size: 11px;");
                chkShortCrest.setWrapText(true);

                // ... Logic ...
                chkShortCrest.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!visualizer.isNotificationsSuspended()) {
                        visualizer.getState().setShortNumberX(0);
                        visualizer.getState().setShortNumberY(0);
                        visualizer.getState().setShortNumberScale(1.0);
                        visualizer.setShortCrestVisible(newVal);
                    }
                    if (isUpdatingUI) return;
                    if (uiShortBadgeToggle != null && uiShortBadgeToggle.isSelected() != newVal) {
                        isUpdatingUI = true;
                        uiShortBadgeToggle.setSelected(newVal);
                        if (!newVal) visualizer.getState().setShortCrestTech("Ninguno");
                        else if (visualizer.getState().getShortCrestTech() == null || visualizer.getState().getShortCrestTech().equals("Ninguno")) {
                            visualizer.getState().setShortCrestTech("Bordado");
                        }
                        isUpdatingUI = false;
                    }
                });

                Runnable updateShortBrand = () -> {
                    if (isUpdatingUI) return;
                    if (chkShortBrand.isSelected()) {
                        visualizer.loadBrandVector("short", "pierna");
                        visualizer.setShortBrandColor((Color) cpShortBrandBtn.getUserData(), true);
                    } else {
                        visualizer.loadBrandVector("short", "");
                    }
                };

                chkShortBrand.selectedProperty().addListener((obs, old, val) -> updateShortBrand.run());
                
                shortRow1.add(cmbShortBrandPos, 0, 0);
                shortRow1.add(cmbShortBrandTech, 1, 0);

                HBox crestRow = new HBox(chkShortCrest);
                crestRow.setAlignment(Pos.CENTER_LEFT);

                shortControls.getChildren().addAll(shortRow1, cpShortBrandBtn, crestRow);
                shortSection.getChildren().add(shortControls);
                content.getChildren().add(shortSection);
            }

            // 3. SOCKS BRANDING
            if (visualizer.hasSocks()) {
                VBox socksSection = new VBox(8);
                socksSection.setStyle(
                        "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 14; -fx-border-color: #ecf0f1; -fx-border-radius: 8;");
                socksSection.setMaxWidth(Double.MAX_VALUE);
                socksSection.setFillWidth(true);

                HBox headerSocks = new HBox(10);
                headerSocks.setAlignment(Pos.CENTER_LEFT);
                Label lblSocks = new Label("Marca en Medias");
                lblSocks.setStyle(sectionHeaderStyle);
                
                Region spacerSc = new Region();
                HBox.setHgrow(spacerSc, Priority.ALWAYS);
                
                CheckBox chkSocksBrand = UIFactory.crearToggleSwitch(visualizer.isSocksBrandVisible());
                headerSocks.getChildren().addAll(lblSocks, spacerSc, chkSocksBrand);
                socksSection.getChildren().add(headerSocks);

                HBox socksRow = new HBox(8);
                socksRow.setAlignment(Pos.CENTER_LEFT);
                socksRow.setPadding(new Insets(5, 0, 0, 0));
                socksRow.managedProperty().bind(chkSocksBrand.selectedProperty());
                socksRow.visibleProperty().bind(chkSocksBrand.selectedProperty());

                MenuButton[] cpSocksBtnArr = new MenuButton[1];
                MenuButton cpSocksBtn = UIFactory.createColorMenuButton(
                        visualizer.getPartColor("brandSocks", Color.BLACK),
                        "Color de Marca",
                        newColor -> {
                            if (!isUpdatingUI) {
                                Color oldColor = visualizer.getPartColor("brandSocks", Color.BLACK);
                                if (!newColor.equals(oldColor)) {
                                    visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                                            "Socks Brand Color", oldColor, newColor,
                                            c -> {
                                                UIFactory.setColorMenuButtonColor(cpSocksBtnArr[0], c);
                                                visualizer.setSocksBrandColor(c, true);
                                            }));
                                    visualizer.setSocksBrandColor(newColor, true);
                                }
                            }
                        },
                        (onCommit, onPreview) -> {
                            eyedropperCallback.accept(
                                    color -> {
                                        if (color == null) return;
                                        Color oldColor = visualizer.getPartColor("brandSocks", Color.BLACK);
                                        if (!color.equals(oldColor)) {
                                            visualizer.getHistoryManager().addCommand(new org.example.pattern.ColorChangeCommand(
                                                    "Socks Brand Color", oldColor, color,
                                                    c -> {
                                                        UIFactory.setColorMenuButtonColor(cpSocksBtnArr[0], c);
                                                        visualizer.setSocksBrandColor(c, true);
                                                    }));
                                            onCommit.accept(color);
                                            visualizer.setSocksBrandColor(color, true);
                                        }
                                    },
                                    color -> {
                                        if (color == null) visualizer.clearPreviewColors();
                                        else visualizer.setPreviewColor("brandSocks", color);
                                    });
                        });
                cpSocksBtnArr[0] = cpSocksBtn;
                cpSocksBtn.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(cpSocksBtn, Priority.ALWAYS);
                socksRow.getChildren().add(cpSocksBtn);
                socksSection.getChildren().add(socksRow);

                // Logic integrated into listeners or row removal
                chkSocksBrand.selectedProperty().addListener((obs, old, val) -> {
                    if (isUpdatingUI) return;
                    visualizer.setSocksBrandVisible(val);
                });

                // End of Socks Branding
                content.getChildren().add(socksSection);
            }

            // 4. SHIELDS & BADGES - ZONED SECTIONS

            // --- Separador Visual ---
            if (visualizer.hasShirt() || visualizer.hasShorts()) {
                VBox separatorContainer = new VBox(5);
                // Aumento de Margen Superior (25px) para gran diferenciación
                separatorContainer.setPadding(new javafx.geometry.Insets(25, 0, 5, 0));

                Label lblSeparador = UIFactory.crearTituloSeccion("CONFIGURACIÓN DE ESCUDOS Y PARCHES");

                separatorContainer.getChildren().add(lblSeparador);
                content.getChildren().add(separatorContainer);
            }

            // 4A. SHIRT SHIELDS (Always Visible)
            if (visualizer.hasShirt()) {
                content.getChildren().add(createZonedBadgeSection("SHIRT", "Escudos Camiseta", false, true));
            }

            if (visualizer.hasShorts()) {
                // Ensure it defaults to off if it's "Ninguno" or null.
                String tech = visualizer.getState().getShortCrestTech();
                boolean active = visualizer.isShortCrestVisible()
                        || (tech != null && !tech.equals("Ninguno") && !tech.isEmpty());
                content.getChildren().add(createZonedBadgeSection("SHORT", "Escudos Short", true, active));
            }

            if (visualizer.hasShirt()) { // Sleeves are tied to shirt
                boolean active = visualizer.getState().isSleeveCrestVisible();
                content.getChildren().add(createZonedBadgeSection("SLEEVE", "Escudos Mangas", true, active));
            }

        } finally {
            isUpdatingUI = false;
        }
    }

    private GridPane createTwoColumnGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c1.setFillWidth(true);
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        c2.setFillWidth(true);
        c2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(c1, c2);
        return grid;
    }

    private VBox createZonedBadgeSection(String zone, String title, boolean optional, boolean initiallyVisible) {
        VBox section = new VBox(8);
        section.setMaxWidth(Double.MAX_VALUE);
        section.setFillWidth(true);
        section.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 14; -fx-border-color: #ecf0f1; -fx-border-radius: 8;");

        Label lblHeader = new Label(title);
        lblHeader.setStyle(sectionHeaderStyle);
        section.getChildren().add(lblHeader);

        VBox inner = new VBox(10);
        inner.setFillWidth(true);
        inner.managedProperty().bind(inner.visibleProperty());

        if (optional) {
            HBox headerRow = new HBox(10);
            headerRow.setAlignment(Pos.CENTER_LEFT);

            Label lblOptional = new Label(title);
            lblOptional.setStyle(sectionHeaderStyle);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            CheckBox toggle = UIFactory.crearToggleSwitch(initiallyVisible);
            if ("SHORT".equals(zone)) {
                this.uiShortBadgeToggle = toggle;
            }

            headerRow.getChildren().addAll(lblOptional, spacer, toggle);
            section.getChildren().set(0, headerRow); // Replace simple label with HBox header

            inner.setVisible(initiallyVisible);

            toggle.selectedProperty().addListener((obs, oldVal, active) -> {
                if (isUpdatingUI) return;

                inner.setVisible(active);
                if ("SLEEVE".equals(zone)) {
                    visualizer.getState().setSleeveCrestVisible(active);
                } else if ("SHORT".equals(zone)) {
                    if (!active) {
                        visualizer.getState().setShortCrestTech("Ninguno");
                    } else {
                        if (visualizer.getState().getShortCrestTech() == null
                                || visualizer.getState().getShortCrestTech().equals("Ninguno")
                                || visualizer.getState().getShortCrestTech().isEmpty()) {
                            visualizer.getState().setShortCrestTech("Bordado");
                        }
                    }

                    // Sincro silenciosa con el check de arriba
                    if (uiShortCrestCheckbox != null && uiShortCrestCheckbox.isSelected() != active) {
                        isUpdatingUI = true; // Evitar rebote
                        visualizer.getState().setShortNumberX(0);
                        visualizer.getState().setShortNumberY(0);
                        visualizer.getState().setShortNumberScale(1.0);
                        uiShortCrestCheckbox.setSelected(active);
                        visualizer.setShortCrestVisible(active);
                        isUpdatingUI = false;
                    }
                }
            });
        }

        inner.getChildren().add(createBadgeSelection(zone, "Tecnología de Fabricación:"));

        ScrollPane scroll = new ScrollPane(shieldGalleries.get(zone));
        scroll.setFitToWidth(true); // Ensure it takes container width
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinHeight(110);
        scroll.setStyle(
                "-fx-background: #ecf0f1; -fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-style: dashed;");

        GridPane controls = createTwoColumnGrid();

        Button btnUpload = new Button("Subir Escudos");
        btnUpload.setGraphic(UIFactory.crearIcono("mdi2u-upload", 16, "white"));
        btnUpload.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 11px;");
        btnUpload.setMaxWidth(Double.MAX_VALUE);
        btnUpload.setOnAction(e -> uploadShieldImage(zone));

        Button btnClear = new Button("Borrar Todo");
        btnClear.setGraphic(UIFactory.crearIcono("mdi2e-eraser", 16, "white"));
        btnClear.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-size: 11px;");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setOnAction(e -> {
            shieldImagesMap.get(zone).clear();
            shieldGalleries.get(zone).getChildren().clear();
        });

        controls.add(btnUpload, 0, 0);
        controls.add(btnClear, 1, 0);
        inner.getChildren().addAll(scroll, controls);
        section.getChildren().add(inner);

        return section;
    }

    private void updateTechRecursive(javafx.scene.Node node, String zone, org.example.model.TipoEscudo type) {
        if (node instanceof org.example.component.ImageLayer) {
            org.example.component.ImageLayer il = (org.example.component.ImageLayer) node;
            String activeZone = il.getActiveZone();
            if (activeZone == null) return;

            boolean match = false;
            if ("SHIRT".equals(zone)) {
                match = activeZone.equals("PECHO") || activeZone.equals("ESPALDA") || activeZone.equals("LOMO") || activeZone.equals("CENTRO");
            } else if ("SHORT".equals(zone)) {
                match = activeZone.startsWith("SHORT");
            } else if ("SLEEVE".equals(zone)) {
                match = activeZone.startsWith("MANGA");
            }

            if (match && il.getBadgeType() != org.example.model.TipoEscudo.NINGUNO) {
                il.setBadgeType(type);
            }
        } else if (node instanceof javafx.scene.Group) {
            for (javafx.scene.Node child : ((javafx.scene.Group) node).getChildren()) {
                updateTechRecursive(child, zone, type);
            }
        }
    }

    private VBox createBadgeSelection(String zone, String title) {
        VBox box = new VBox(5);
        box.setMaxWidth(Double.MAX_VALUE);
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 11px;");
        lbl.setWrapText(true);
        box.getChildren().add(lbl);

        GridPane options = createTwoColumnGrid();

        ToggleGroup group = new ToggleGroup();
        techGroups.put(zone, group);

        int colIndex = 0;

        org.example.model.TipoEscudo selectedType = org.example.model.TipoEscudo.BORDADO;
        if ("SHIRT".equals(zone))
            selectedType = org.example.model.TipoEscudo.valueOfTech(visualizer.getState().getShirtCrestTech());
        else if ("SHORT".equals(zone))
            selectedType = org.example.model.TipoEscudo.valueOfTech(visualizer.getState().getShortCrestTech());
        else if ("SLEEVE".equals(zone))
            selectedType = org.example.model.TipoEscudo.valueOfTech(visualizer.getState().getSleeveCrestTech());

        for (org.example.model.TipoEscudo type : org.example.model.TipoEscudo.values()) {
            if (type == org.example.model.TipoEscudo.NINGUNO)
                continue;

            javafx.scene.Node graphic = null;
            try {
                String iconName = (type == org.example.model.TipoEscudo.SUBLIMADO) ? "shield_sublimado.png"
                        : "shield_bordado.png";
                javafx.scene.image.Image iconImg = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("/custom_icons/" + iconName));
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(iconImg);
                iv.setFitHeight(24);
                iv.setPreserveRatio(true);
                graphic = iv;
            } catch (Exception e) {
                // Fallback to source icon if image fails
                graphic = UIFactory.crearIcono(
                        type == org.example.model.TipoEscudo.SUBLIMADO ? "mdi2p-printer" : "mdi2n-needle", 20,
                        "#34495e");
            }

            ToggleButton btn = UIFactory.crearBotonOpcion(type.getLabel(), graphic);
            btn.getStyleClass().add("tech-selection-button");
            btn.setUserData(type);
            btn.setToggleGroup(group);
            btn.setMinWidth(40); // Allow shrinking
            btn.setPrefHeight(54);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setWrapText(true);
            GridPane.setHgrow(btn, Priority.ALWAYS); // Fill grid cell
            UIFactory.applyGenderTheme(btn, visualizer.getGenero());

            if (type == selectedType)
                btn.setSelected(true);
            options.add(btn, colIndex++, 0);
        }

        group.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == null) {
                if (old != null)
                    old.setSelected(true);
                return;
            }
            org.example.model.TipoEscudo type = (org.example.model.TipoEscudo) val.getUserData();
            if ("SHIRT".equals(zone))
                visualizer.getState().setShirtCrestTech(type.getLabel());
            else if ("SHORT".equals(zone))
                visualizer.getState().setShortCrestTech(type.getLabel());
            else if ("SLEEVE".equals(zone))
                visualizer.getState().setSleeveCrestTech(type.getLabel());

            if (visualizer.getLayerManager() != null) {
                for (javafx.scene.Node n : visualizer.getLayerManager().getLayers()) {
                    updateTechRecursive(n, zone, type);
                }
            }

            if (onBadgeTypeChange != null) {
                onBadgeTypeChange.accept(type);
            }
        });

        box.getChildren().add(options);
        return box;
    }

    private void uploadShieldImage(String zone) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        List<File> files = fc.showOpenMultipleDialog(mainContainer.getScene().getWindow());
        if (files != null) {
            for (File f : files) {
                try {
                    Image img = new Image(f.toURI().toString(), 800, 800, true, true);
                    addShieldToZonedGallery(zone, img);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addShieldToZonedGallery(String zone, Image img) {
        shieldImagesMap.get(zone).add(img);
        ImageView thumb = new ImageView(img);
        thumb.setFitWidth(55);
        thumb.setFitHeight(55);
        thumb.setPreserveRatio(true);

        VBox box = new VBox(thumb);
        box.setPrefSize(80, 80);
        String transparencyGrid = "linear-gradient(from 0px 0px to 8px 8px, repeat, #475569 0%, #475569 25%, #334155 25%, #334155 50%, #475569 50%, #475569 75%, #334155 75%, #334155 100%)";
        box.setStyle(
                "-fx-background-color: " + transparencyGrid + "; " +
                        "-fx-border-color: #1e293b; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand;");
        box.setAlignment(Pos.CENTER);

        box.setOnDragDetected(e -> {
            Dragboard db = box.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putImage(img);
            ToggleGroup tg = techGroups.get(zone);
            org.example.model.TipoEscudo type = (tg != null && tg.getSelectedToggle() != null)
                    ? (org.example.model.TipoEscudo) tg.getSelectedToggle().getUserData()
                    : org.example.model.TipoEscudo.BORDADO;
            content.putString("BADGE:" + type.name() + ":" + zone);
            db.setContent(content);
            e.consume();
        });

        box.setOnMouseClicked(e -> {
            org.example.component.ImageLayer layer = new org.example.component.ImageLayer(img);
            ToggleGroup tg = techGroups.get(zone);
            org.example.model.TipoEscudo type = (tg != null && tg.getSelectedToggle() != null)
                    ? (org.example.model.TipoEscudo) tg.getSelectedToggle().getUserData()
                    : org.example.model.TipoEscudo.BORDADO;
            layer.setBadgeType(type);
            visualizer.addImageLayer(layer);
            String bucket = "PECHO";
            if ("SHORT".equals(zone))
                bucket = "SHORT_FRONT";
            else if ("SLEEVE".equals(zone))
                bucket = "MANGA_DELANTERA";
            visualizer.applySmartPowerClip(layer, bucket, true);
        });

        shieldGalleries.get(zone).getChildren().add(box);
    }

    public List<Image> getShieldLibraryShirt() {
        return shieldImagesMap.get("SHIRT");
    }

    public List<Image> getShieldLibraryShort() {
        return shieldImagesMap.get("SHORT");
    }

    public List<Image> getShieldLibrarySleeve() {
        return shieldImagesMap.get("SLEEVE");
    }

    public List<Image> getShieldLibrary() {
        return getShieldLibraryShirt();
    }

    public void setShieldLibraryShirt(List<Image> shields) {
        populateGallery("SHIRT", shields);
    }

    public void setShieldLibraryShort(List<Image> shields) {
        populateGallery("SHORT", shields);
    }

    public void setShieldLibrarySleeve(List<Image> shields) {
        populateGallery("SLEEVE", shields);
    }

    public void setShieldLibrary(List<Image> shields) {
        setShieldLibraryShirt(shields);
    }

    private void populateGallery(String zone, List<Image> images) {
        shieldImagesMap.get(zone).clear();
        shieldGalleries.get(zone).getChildren().clear();
        if (images != null) {
            for (Image img : images) {
                addShieldToZonedGallery(zone, img);
            }
        }
    }

    // --- REFERENCE IMAGES SECTION ---
    private final List<Image> referenceImages = new ArrayList<>();
    private FlowPane referenceGallery = new FlowPane(10, 10);

    public VBox getReferencesView() {
        VBox refSection = new VBox(8);
        refSection.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-padding: 10; -fx-border-color: #ecf0f1; -fx-border-radius: 5;");
        Label lblRef = new Label("Referencias (Ficha Técnica)");
        lblRef.setStyle(sectionHeaderStyle);
        refSection.getChildren().addAll(lblHeaderStyleCheck(lblRef),
                new Label("Máximo 6 imágenes para la zona inferior de ficha."));

        referenceGallery.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 5;");
        refSection.getChildren().add(referenceGallery);

        HBox controls = new HBox(10);
        Button btnUploadRef = new Button("Subir Ref.");
        btnUploadRef.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        btnUploadRef.setOnAction(e -> uploadReferenceImage());
        Button btnClearRef = new Button("Limpiar");
        btnClearRef.setStyle("-fx-border-color: #c0392b; -fx-text-fill: #c0392b;");
        btnClearRef.setOnAction(e -> {
            referenceImages.clear();
            refreshReferenceGallery();
        });
        controls.getChildren().addAll(btnUploadRef, btnClearRef);
        refSection.getChildren().add(controls);
        return refSection;
    }

    private Label lblHeaderStyleCheck(Label l) {
        return l;
    } // Dummy to maintain flow if needed

    private void uploadReferenceImage() {
        if (referenceImages.size() >= 6)
            return;
        FileChooser fc = new FileChooser();
        List<File> files = fc.showOpenMultipleDialog(mainContainer.getScene().getWindow());
        if (files != null) {
            for (File f : files) {
                if (referenceImages.size() >= 6)
                    break;
                try {
                    referenceImages.add(new Image(f.toURI().toString(), 800, 800, true, true));
                } catch (Exception e) {
                }
            }
            refreshReferenceGallery();
        }
    }

    private void refreshReferenceGallery() {
        referenceGallery.getChildren().clear();
        for (Image img : referenceImages) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(50);
            iv.setFitHeight(50);
            iv.setPreserveRatio(true);
            referenceGallery.getChildren().add(new VBox(iv));
        }
    }

    public List<Image> getReferenceLibrary() {
        return new ArrayList<>(referenceImages);
    }

    public void setReferenceLibrary(List<Image> images) {
        referenceImages.clear();
        if (images != null)
            referenceImages.addAll(images);
        refreshReferenceGallery();
    }

    public List<Image> getReferenceImages() {
        return getReferenceLibrary();
    }
}

