package org.example.controller.configurator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.paint.Color;
import org.example.component.PrendaVisualizer;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.*;
import org.example.service.ConfiguracionPrendaService;
import org.example.utils.UIFactory;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Configurator for shirt/t-shirt garments.
 * Restored visual styles and persistence logic.
 */
public class CamisetaConfigurator {

    private final VBox container;
    private final PrendaVisualizer visualizer;
    private final ConfiguracionPrendaService service;
    private final ConfiguracionPrendaDTO.Builder configBuilder;
    private final java.util.Map<TipoMedias, ToggleButton> buttonsMedias = new java.util.HashMap<>();
    private final javafx.beans.property.BooleanProperty isArqueroModeProperty = new javafx.beans.property.SimpleBooleanProperty(false);
    private Label lblLargoDisplay;
    private Label lblCuelloDisplay;
    private ToggleButton tbPunos; // Field for programmatic access
    private ToggleButton tbLineas; // Field for lineas toggle
    private Runnable onConfigChanged;

    public void setOnConfigChanged(Runnable listener) {
        this.onConfigChanged = listener;
    }

    private java.util.function.Consumer<TipoMedias> onBulkSocksCategoryChanged;

    public void setOnBulkSocksCategoryChanged(java.util.function.Consumer<TipoMedias> listener) {
        this.onBulkSocksCategoryChanged = listener;
    }

    public void setArqueroMode(boolean mode) {
        this.isArqueroModeProperty.set(mode);
    }

    private void notifyChange() {
        if (onConfigChanged != null) {
            onConfigChanged.run();
        }
    }

    public CamisetaConfigurator(VBox container, PrendaVisualizer visualizer) {
        this.container = container;
        this.visualizer = visualizer;
        this.service = new ConfiguracionPrendaService();
        this.configBuilder = new ConfiguracionPrendaDTO.Builder();
    }

    /**
     * Builds the complete UI for shirt configuration.
     * With Persistence Support
     */
    public void buildUI(TipoGenero genero, ConfiguracionPrendaDTO previousConfig, TipoPrenda garmentType) {
        configBuilder.tipoPrenda(garmentType);
        configBuilder.genero(genero);

        visualizer.getState().setHasShirt(true);

        visualizer.setGenero(genero);
        visualizer.setVisible(true);
        // Use the passed type (e.g. "conjunto" or "camiseta")
        visualizer.updateActiveLayers(garmentType.toString());
        // Initial State Logic
        boolean initShort = false;
        boolean initMedias = false;

        if (previousConfig != null) {
            initShort = previousConfig.llevaShort();
            initMedias = previousConfig.llevaMedias();

            // Gender Compatibility Check (Reset if moving to Male with Female-only short)
            if (genero == TipoGenero.HOMBRE && initShort) {
                TipoCorte prevShort = previousConfig.getCorteShort();
                if (prevShort == TipoCorte.LICRA || prevShort == TipoCorte.PANTALONETA) {
                    initShort = false;
                }
            }
        }

        configBuilder.conShort(initShort);
        configBuilder.conMedias(initMedias);
        visualizer.setShorts(initShort);
        visualizer.setMedias(initMedias);

        // --- RESTORE PREVIOUS STATE IF AVAILABLE ---
        if (previousConfig != null) {
            // Restore Basic
            configBuilder.corte(previousConfig.getCorte());
            configBuilder.largo(previousConfig.getLargo());
            configBuilder.cuello(previousConfig.getCuello());
            configBuilder.tipoEscudo(
                    previousConfig.getTipoEscudo() != null ? previousConfig.getTipoEscudo() : TipoEscudo.SUBLIMADO);

            // Restore Extras
            configBuilder.conMalla(previousConfig.llevaMalla());
            configBuilder.conPunoCamiseta(previousConfig.llevaPunoCamiseta());
            configBuilder.conFranjaCamiseta(previousConfig.llevaFranjaCamiseta());
            configBuilder.conLineaCamiseta(previousConfig.llevaLineaCamiseta());
            configBuilder.conAcolchado(previousConfig.llevaAcolchado());
            configBuilder.conLigaMedias(previousConfig.llevaLigaMedias());

            // Apply to visualizer
            visualizer.setCorte(previousConfig.getCorte());
            visualizer.setLargo(previousConfig.getLargo());
            visualizer.setCuello(previousConfig.getCuello());
            visualizer.setMalla(previousConfig.llevaMalla());
            visualizer.setPunos(previousConfig.llevaPunoCamiseta());
            visualizer.setShirtStripe(previousConfig.llevaFranjaCamiseta());
            visualizer.setShirtLinea(previousConfig.llevaLineaCamiseta());
            visualizer.setPadding(previousConfig.llevaAcolchado());
            visualizer.setSocksTop(previousConfig.llevaLigaMedias());

            // Restore Extras to Builder
            configBuilder.conMalla(previousConfig.llevaMalla());
            configBuilder.conPunoCamiseta(previousConfig.llevaPunoCamiseta());
            configBuilder.conFranjaCamiseta(previousConfig.llevaFranjaCamiseta());
            configBuilder.conLineaCamiseta(previousConfig.llevaLineaCamiseta());
            configBuilder.conLigaMedias(previousConfig.llevaLigaMedias());
            configBuilder.tipoMedias(previousConfig.getTipoMedias());

        } else {
            // Defaults
            configBuilder.corte(TipoCorte.CUADRADO);
            configBuilder.largo(TipoLargo.MANGA_CORTA);
            configBuilder.cuello(TipoCuello.V);
            configBuilder.tipoEscudo(TipoEscudo.SUBLIMADO);
            configBuilder.conFranjaCamiseta(false);

            visualizer.setCorte(TipoCorte.CUADRADO);
            visualizer.setLargo(TipoLargo.MANGA_CORTA);
            visualizer.setCuello(TipoCuello.V);
            visualizer.setMalla(false);
            visualizer.setPunos(false);
            visualizer.setShirtStripe(false);
            visualizer.setShirtLinea(false);
        }

        agregarSeccionCorte(configBuilder.build().getCorte());
        agregarSeccionManga(configBuilder.build().getLargo());
        agregarSeccionCuello(configBuilder.build().getCuello());
        // agregarSeccionEscudo(configBuilder.build().getTipoEscudo()); // MOVED to Tab
        // 2
        ConfiguracionPrendaDTO current = configBuilder.build();
        agregarSeccionAdicionales(current.llevaPunoCamiseta(), current.llevaMalla());

        // Add separator and optional set completion
        container.getChildren().add(new Separator());
        agregarSeccionCompletarConjunto(previousConfig);
    }

    private void agregarSeccionCorte(TipoCorte currentCorte) {
        javafx.scene.layout.GridPane boxCorte = new javafx.scene.layout.GridPane();
        boxCorte.setHgap(15);
        boxCorte.setVgap(15);
        boxCorte.setAlignment(Pos.CENTER);
        boxCorte.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
        c1.setPercentWidth(50);
        javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints();
        c2.setPercentWidth(50);
        boxCorte.getColumnConstraints().addAll(c1, c2);

        ToggleGroup grpCorte = new ToggleGroup();

        int row = 0;
        int col = 0;
        for (TipoCorte c : TipoCorte.values()) {
            if (c == TipoCorte.RANGLAN || c == TipoCorte.LICRA ||
                    c == TipoCorte.NOVA || c == TipoCorte.PANTALONETA) {
                continue;
            }

            String iconCode = c == TipoCorte.CUADRADO ? "mdi2s-square-outline" : "mdi2c-circle-outline";
            ToggleButton btn = UIFactory.crearBotonOpcion(c.getLabel(), iconCode, 20);
            UIFactory.applyGenderTheme(btn, configBuilder.build().getGenero());
            btn.setUserData(c);
            btn.setToggleGroup(grpCorte);
            btn.setPrefHeight(46);
            btn.setMaxWidth(Double.MAX_VALUE);

            if (c == currentCorte) {
                btn.setSelected(true);
            }

            boxCorte.add(btn, col, row);
            col++;
            if (col == 2) {
                col = 0;
                row++;
            }
        }

        grpCorte.selectedToggleProperty().addListener((o, v, n) -> {
            if (n == null) {
                // Enforce mandatory selection: Re-select the old value
                if (v != null) {
                    v.setSelected(true);
                }
                return;
            }

            TipoCorte corte = (TipoCorte) n.getUserData();
            configBuilder.corte(corte);
            visualizer.setCorte(corte);
            notifyChange();

            // Smart Sleeve Logic
            validateSleeveCompatibility(corte);
        });

        container.getChildren().add(UIFactory.crearSeccionTarjeta("Tipo de Corte (Manga)", null, boxCorte));
    }

    private void validateSleeveCompatibility(TipoCorte corte) {
        TipoLargo largoActual = TipoLargo.valueOf(configBuilder.build().getLargo().name());
        if (!service.esLargoCompatible(largoActual, corte, configBuilder.build().getGenero())) {
            TipoLargo nuevoLargo = service.getLargoDefault(corte, configBuilder.build().getGenero());
            configBuilder.largo(nuevoLargo);
            visualizer.setLargo(nuevoLargo);

            if (lblLargoDisplay != null) {
                lblLargoDisplay.setText("Selección actual: " + nuevoLargo.getLabel());
            }

            // Optional: notify user slightly less aggressively? Or keep it.
            UIFactory.mostrarAlerta(Alert.AlertType.INFORMATION, "Ajuste Automático",
                    "La manga '" + largoActual.getLabel() + "' no es compatible con corte '" + corte.getLabel()
                            + "'. Se ha ajustado a '" + nuevoLargo.getLabel() + "'.");
        }
    }

    private void agregarSeccionManga(TipoLargo currentLargo) {
        Button btnManga = new Button("Catálogo de Mangas");
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream("/custom_icons/manga icon.png"));
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(30);
            iv.setFitHeight(30);
            iv.setPreserveRatio(true);
            btnManga.setGraphic(iv);
        } catch (Exception ex) {
            FontIcon iconManga = new FontIcon("mdi2r-ruler");
            iconManga.setIconSize(24);
            iconManga.setIconColor(Color.WHITE);
            btnManga.setGraphic(iconManga);
        }
        btnManga.setMaxWidth(Double.MAX_VALUE);
        btnManga.getStyleClass().add("button-catalog");

        lblLargoDisplay = new Label("Selección actual: " + currentLargo.getLabel());
        lblLargoDisplay.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 5 0 0 0;");

        btnManga.setOnAction(e -> mostrarDialogoMangas());

        VBox content = new VBox(5, btnManga, lblLargoDisplay);
        container.getChildren().add(UIFactory.crearSeccionTarjeta("Largo de Manga", null, content));
    }

    private void agregarSeccionCuello(TipoCuello currentCuello) {
        Button btnCuello = new Button("Catálogo de Cuellos");
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream("/custom_icons/cuello icon.png"));
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(30);
            iv.setFitHeight(30);
            iv.setPreserveRatio(true);
            btnCuello.setGraphic(iv);
        } catch (Exception ex) {
            FontIcon iconCuello = new FontIcon("mdi2t-tshirt-v");
            iconCuello.setIconSize(24);
            iconCuello.setIconColor(Color.WHITE);
            btnCuello.setGraphic(iconCuello);
        }
        btnCuello.setMaxWidth(Double.MAX_VALUE);
        btnCuello.getStyleClass().add("button-catalog");

        lblCuelloDisplay = new Label("Selección actual: " + currentCuello.getLabel());
        lblCuelloDisplay.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 5 0 0 0;");

        btnCuello.setOnAction(e -> mostrarDialogoCuellos());

        VBox content = new VBox(5, btnCuello, lblCuelloDisplay);
        container.getChildren().add(UIFactory.crearSeccionTarjeta("Tipo de Cuello", null, content));
    }

    private void agregarSeccionAdicionales(boolean punos, boolean malla) {
        HBox boxAdicionales = new HBox(10);
        boxAdicionales.setAlignment(Pos.CENTER);
        boxAdicionales.setMaxWidth(Double.MAX_VALUE);

        tbPunos = UIFactory.crearBotonOpcion("Puños", "mdi2c-contain", 20);
        UIFactory.applyGenderTheme(tbPunos, configBuilder.build().getGenero());
        tbPunos.setSelected(punos);
        tbPunos.setPrefHeight(65);
        tbPunos.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tbPunos, javafx.scene.layout.Priority.ALWAYS);

        tbPunos.setOnAction(e -> {
            configBuilder.conPunoCamiseta(tbPunos.isSelected());
            visualizer.setPunos(tbPunos.isSelected());
            notifyChange();
        });

        ToggleButton tbMalla = UIFactory.crearBotonOpcion("Malla", "mdi2v-view-grid-plus", 20);
        UIFactory.applyGenderTheme(tbMalla, configBuilder.build().getGenero());
        tbMalla.setSelected(malla);
        tbMalla.setPrefHeight(65);
        tbMalla.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tbMalla, javafx.scene.layout.Priority.ALWAYS);

        tbMalla.setOnAction(e -> {
            configBuilder.conMalla(tbMalla.isSelected());
            visualizer.setMalla(tbMalla.isSelected());
            notifyChange();
        });

        ToggleButton tbAcolchado = UIFactory.crearBotonOpcion("Acolchado", "mdi2s-shield-check-outline", 20);
        UIFactory.applyGenderTheme(tbAcolchado, configBuilder.build().getGenero());
        tbAcolchado.setSelected(configBuilder.build().llevaAcolchado());
        tbAcolchado.setPrefHeight(65);
        tbAcolchado.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tbAcolchado, javafx.scene.layout.Priority.ALWAYS);
        
        // Safety green theme for GKs
        tbAcolchado.getProperties().put("themeColor", "#2ecc71");
        UIFactory.updateButtonStyle(tbAcolchado);
        
        // Dynamic visibility
        tbAcolchado.visibleProperty().bind(isArqueroModeProperty);
        tbAcolchado.managedProperty().bind(tbAcolchado.visibleProperty());
        
        tbAcolchado.setOnAction(e -> {
            configBuilder.conAcolchado(tbAcolchado.isSelected());
            visualizer.setPadding(tbAcolchado.isSelected());
            notifyChange();
        });
        
        boxAdicionales.getChildren().addAll(tbPunos, tbMalla, tbAcolchado);

        tbLineas = UIFactory.crearBotonOpcion("Líneas", "mdi2l-layers-outline", 20);
        UIFactory.applyGenderTheme(tbLineas, configBuilder.build().getGenero());
        tbLineas.setSelected(configBuilder.build().llevaLineaCamiseta());
        tbLineas.setPrefHeight(65);
        tbLineas.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tbLineas, javafx.scene.layout.Priority.ALWAYS);

        tbLineas.setOnAction(e -> {
            configBuilder.conLineaCamiseta(tbLineas.isSelected());
            visualizer.setShirtLinea(tbLineas.isSelected());
            notifyChange();
        });

        ToggleButton tbTelaNatural = UIFactory.crearBotonOpcion("Tela Natural", "fas-scroll", 20);
        UIFactory.applyGenderTheme(tbTelaNatural, configBuilder.build().getGenero());
        tbTelaNatural.setSelected(false);
        tbTelaNatural.setPrefHeight(65);
        tbTelaNatural.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tbTelaNatural, javafx.scene.layout.Priority.ALWAYS);

        tbTelaNatural.setOnAction(e -> {
            boolean selected = tbTelaNatural.isSelected();
            configBuilder.build().getGenero();
            visualizer.getState().setTelaNatural(selected);
            visualizer.getShirtRenderer().setTelaNatural(selected);
            visualizer.cargarCapas();
            notifyChange();
        });

        VBox contentAdic = new VBox(10, boxAdicionales);
        contentAdic.getChildren().addAll(tbLineas, tbTelaNatural);

        container.getChildren().add(UIFactory.crearSeccionTarjeta("Adicionales", null, contentAdic));
    }

    private void agregarSeccionCompletarConjunto(ConfiguracionPrendaDTO previousConfig) {
        VBox boxConjunto = new VBox(10);

        // --- ADD SHORT TOGGLE (Segmented Row) ---
        HBox hbShortToggle = UIFactory.crearFilaOpcion("Añadir Short", configBuilder.build().llevaShort(),
                configBuilder.build().getGenero(), selected -> {
                    configBuilder.conShort(selected);
                    visualizer.setShorts(selected);
                    notifyChange();
                });
        CheckBox swShort = (CheckBox) hbShortToggle.getChildren().get(0);

        // Container for short config
        VBox shortContainer = new VBox(10);
        shortContainer.setVisible(false);
        shortContainer.setManaged(false);
        shortContainer.setPadding(new javafx.geometry.Insets(10));
        shortContainer.setStyle(
                "-fx-background-color: #f9fafb; -fx-background-radius: 5; -fx-border-color: #e0e6ed; -fx-border-radius: 5;");

        ShortConfigurator subShortConfig = new ShortConfigurator(shortContainer, visualizer);

        // Initial State Check
        boolean isShortEnabled = configBuilder.build().llevaShort();
        if (isShortEnabled) {
            shortContainer.setVisible(true);
            shortContainer.setManaged(true);
            subShortConfig.buildUIInline(configBuilder.build().getGenero(), configBuilder, previousConfig);
        }

        swShort.selectedProperty().addListener((obs, old, selected) -> {
            shortContainer.setVisible(selected);
            shortContainer.setManaged(selected);

            if (selected) {
                if (shortContainer.getChildren().isEmpty()) {
                    subShortConfig.buildUIInline(configBuilder.build().getGenero(), configBuilder, previousConfig);
                }
            }
        });

        boxConjunto.getChildren().addAll(hbShortToggle, shortContainer);

        container.getChildren().add(UIFactory.crearSeccionTarjeta("Completar Conjunto", "mdi2p-plus-box-outline", boxConjunto));
        
        // Separator before Medias
        container.getChildren().add(new Separator());
        
        // --- ADD MEDIAS SECTION (NOW OWN CARD) ---
        agregarSeccionMedias();
    }

    private void agregarSeccionMedias() {
        VBox boxMedias = new VBox(10);

        // --- ADD SOCKS TOGGLE (Segmented Row) ---
        HBox hbMediasToggle = UIFactory.crearFilaOpcion("Añadir Medias", configBuilder.build().llevaMedias(),
                configBuilder.build().getGenero(), selected -> {
                    configBuilder.conMedias(selected);
                    visualizer.setMedias(selected);
                    notifyChange();
                });
        CheckBox swMedias = (CheckBox) hbMediasToggle.getChildren().get(0);

        // Extra Options Container (Indent)
        VBox extraOptions = new VBox(12);
        extraOptions.setPadding(new Insets(5, 0, 5, 10));
        extraOptions.managedProperty().bind(swMedias.selectedProperty());
        extraOptions.visibleProperty().bind(swMedias.selectedProperty());

        // --- UPPER GARTER TOGGLE (Segmented Row) ---
        HBox hbLigaToggle = UIFactory.crearFilaOpcion("Liga Superior", configBuilder.build().llevaLigaMedias(),
                configBuilder.build().getGenero(), selected -> {
                    configBuilder.conLigaMedias(selected);
                    visualizer.setSocksTop(selected);
                    if (selected) {
                        visualizer.setSocksTopColor(javafx.scene.paint.Color.web("#7f8c8d"));
                    }
                    notifyChange();
                });
        
        // --- SOCK SIZE / TYPE ---
        Label lblTipo = new Label("Categoría de Medias:");
        lblTipo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        
        GridPane gridButtons = new GridPane();
        gridButtons.setHgap(8);
        gridButtons.setVgap(8);
        gridButtons.setMaxWidth(Double.MAX_VALUE);
        // Allow grid to shrink responsively
        
        ColumnConstraints colConstraint = new ColumnConstraints();
        colConstraint.setPercentWidth(50);
        gridButtons.getColumnConstraints().addAll(colConstraint, colConstraint);

        String baseStyle = "-fx-background-radius: 8; -fx-padding: 8 4; -fx-cursor: hand; -fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-text-fill: #495057; -fx-font-weight: bold; -fx-font-size: 10px; -fx-font-family: 'Segoe UI'; -fx-alignment: CENTER;";
        String activeStyle = "-fx-background-radius: 8; -fx-padding: 8 4; -fx-cursor: hand; -fx-background-color: #3498db; -fx-border-color: #2980b9; -fx-border-width: 1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-font-family: 'Segoe UI'; -fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.3), 5, 0, 0, 2); -fx-alignment: CENTER;";

        int rowIdx = 0;
        int colIdx = 0;
        buttonsMedias.clear();

        for (TipoMedias tm : TipoMedias.values()) {
            ToggleButton rb = new ToggleButton(tm.getLabel().toUpperCase());
            rb.setUserData(tm);
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setAlignment(Pos.CENTER);
            
            rb.setStyle(baseStyle);
            rb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                rb.setStyle(newVal ? activeStyle : baseStyle);
            });
            
            // Interaction: Clicking sets the category for the roster
            rb.setOnAction(e -> {
                TipoMedias chosen = (TipoMedias) rb.getUserData();
                
                // If it was ALREADY selected and we click it, we want to DESELECT IT
                // Note: when setOnAction is called for a ToggleButton, the 'selected' property has already toggled.
                if (!rb.isSelected()) {
                     // Was selected, now toggled OFF
                     configBuilder.tipoMedias(null);
                     visualizer.setTipoMedias(null);
                } else {
                     // Toggle ON: Deselect all other manually (not in a group)
                     buttonsMedias.forEach((k, v) -> { if (v != rb) v.setSelected(false); });
                     
                     configBuilder.tipoMedias(chosen);
                     visualizer.setTipoMedias(chosen);
                }
                
                if (onBulkSocksCategoryChanged != null) {
                    onBulkSocksCategoryChanged.accept(configBuilder.build().getTipoMedias());
                }
                notifyChange();
            });

            if (tm == configBuilder.build().getTipoMedias()) {
                rb.setSelected(true);
            }
            
            gridButtons.add(rb, colIdx, rowIdx);
            buttonsMedias.put(tm, rb);

            colIdx++;
            if (colIdx > 1) {
                colIdx = 0;
                rowIdx++;
            }
        }
        
        extraOptions.getChildren().addAll(hbLigaToggle, lblTipo, gridButtons);
        boxMedias.getChildren().addAll(hbMediasToggle, extraOptions);

        container.getChildren().add(UIFactory.crearSeccionTarjeta("Medias", "mdi2f-foot-print", boxMedias));
    }

    // --- DIALOGS RESTORED TO GRID LAYOUT ---

    private void mostrarDialogoMangas() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Seleccionar Largo de Manga");
        // Add App Icon
        try {
            javafx.scene.image.Image logo = UIFactory.getAppLogo();
            if (logo != null) {
                dialog.getIcons().add(logo);
            }
        } catch (Exception e) {
            System.err.println("Icon not found: " + e.getMessage());
        }

        Label lblTitle = UIFactory.crearTituloSeccion("Tipos de Manga");

        FlowPane container = new FlowPane();
        container.setHgap(10);
        container.setVgap(10);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(10));

        ToggleGroup grp = new ToggleGroup();
        grp.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                TipoLargo largo = (TipoLargo) newVal.getUserData();
                configBuilder.largo(largo);
                visualizer.setLargo(largo);
                lblLargoDisplay.setText("Selección actual: " + largo.getLabel());
                notifyChange();

                // Restriction: If Sleeveless (Manga Cero), enforce Cuffs
                if (largo == TipoLargo.MANGA_CERO) {
                    if (tbPunos != null && !tbPunos.isSelected()) {
                        tbPunos.setSelected(true);
                        configBuilder.conPunoCamiseta(true);
                        visualizer.setPunos(true);
                        // Optional notify user?
                    }
                }

                dialog.close();
            }
        });

        TipoCorte currentCorte = configBuilder.build().getCorte();
        TipoGenero currentGenero = configBuilder.build().getGenero();

        for (TipoLargo l : TipoLargo.values()) {
            // Compatibility Filters
            if (!service.esLargoCompatible(l, currentCorte, currentGenero)) {
                continue;
            }
            ToggleButton btn = UIFactory.crearBotonOpcion(l.getLabel(), getIconoManga(l), 32);
            UIFactory.applyGenderTheme(btn, configBuilder.build().getGenero());
            btn.setMinWidth(110);
            btn.setMaxWidth(110);
            btn.setPrefHeight(90);
            btn.setUserData(l);
            btn.setToggleGroup(grp);

            if (l == configBuilder.build().getLargo()) {
                btn.setSelected(true);
            }

            container.getChildren().add(btn);
        }

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("dialog-button-success");
        btnCerrar.setPrefWidth(120);

        btnCerrar.setOnAction(e -> dialog.close());

        VBox root = new VBox(20, lblTitle, container, btnCerrar);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 1;");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private void mostrarDialogoCuellos() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Seleccionar Cuello");
        // Add App Icon
        try {
            javafx.scene.image.Image logo = UIFactory.getAppLogo();
            if (logo != null) {
                dialog.getIcons().add(logo);
            }
        } catch (Exception e) {
            System.err.println("Icon not found: " + e.getMessage());
        }

        Label lblTitle = UIFactory.crearTituloSeccion("Tipos de Cuello");

        FlowPane container = new FlowPane();
        container.setHgap(10);
        container.setVgap(10);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(10));

        ToggleGroup grp = new ToggleGroup();
        grp.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                TipoCuello cuello = (TipoCuello) newVal.getUserData();
                configBuilder.cuello(cuello);
                visualizer.setCuello(cuello);
                lblCuelloDisplay.setText("Selección actual: " + cuello.getLabel());
                notifyChange();
                dialog.close();
            }
        });

        for (TipoCuello c : TipoCuello.values()) {
            // FILTER: Only REDONDO and V for now, but ready for more.
            if (c != TipoCuello.REDONDO && c != TipoCuello.V) {
                continue;
            }

            // LOAD PNG ICON
            String imagePath = "/images/icons/cuellos/" + c.name().toLowerCase() + ".png";
            Node iconNode;
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath));
                if (img.isError())
                    throw new Exception();
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(64); // Larger for PNG
                iv.setFitHeight(64);
                iv.setPreserveRatio(true);
                iconNode = iv;
            } catch (Exception e) {
                // Fallback to FontIcon if PNG missing
                iconNode = UIFactory.crearIcono(getIconoCuello(c), 32, "#2c3e50");
            }

            ToggleButton btn = UIFactory.crearBotonOpcion(c.getLabel(), iconNode);
            UIFactory.applyGenderTheme(btn, configBuilder.build().getGenero());
            btn.setMinWidth(110);
            btn.setMaxWidth(110);
            btn.setPrefHeight(110); // Slightly taller for image
            btn.setUserData(c);
            btn.setToggleGroup(grp);

            if (c == configBuilder.build().getCuello()) {
                btn.setSelected(true);
            }

            container.getChildren().add(btn);
        }

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("dialog-button-success");
        btnCerrar.setPrefWidth(120);

        btnCerrar.setOnAction(e -> dialog.close());

        VBox root = new VBox(20, lblTitle, container, btnCerrar);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 1;");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    // Helper method for manga icons
    private String getIconoManga(TipoLargo largo) {
        // Simple mapping for now
        return "mdi2r-ruler";
    }

    // Helper method for cuello icons
    private String getIconoCuello(TipoCuello cuello) {
        switch (cuello) {
            case V:
                return "mdi2t-tshirt-v";
            case REDONDO:
                return "mdi2t-tshirt-crew";
            case CAMISERO:
                return "mdi2p-polo";
            default:
                return "mdi2t-tshirt-v";
        }
    }

    public ConfiguracionPrendaDTO.Builder getConfigBuilder() {
        return configBuilder;
    }
    public void updateSocksHighlights(java.util.Set<TipoMedias> activeInRoster) {
        buttonsMedias.forEach((tm, btn) -> {
            btn.setSelected(activeInRoster.contains(tm));
        });
    }
}

