package org.example.controller.delegate;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.example.component.PrendaVisualizer;
import org.example.controller.configurator.CamisetaConfigurator;
import org.example.controller.configurator.ConjuntoConfigurator;
import org.example.controller.configurator.ShortConfigurator;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.model.TipoGenero;
import org.example.model.TipoPrenda;
import org.example.model.TipoCorte;
import org.example.model.TipoLargo;
import org.example.model.TipoCuello;
import org.example.model.TipoTela;
import org.example.utils.UIFactory;

/**
 * Refactored PrendaDelegate - Now acts as a coordinator.
 * Delegates actual configuration logic to specialized configurators.
 * 
 * Reduced from 1,199 lines to ~250 lines.
 */
public class PrendaDelegate {

    private final VBox container;
    private final PrendaVisualizer visualizer;
    private final Label lblPlaceholder;

    private Runnable onPrendaChanged;
    private Runnable onConfigChanged;

    public void setOnPrendaChanged(Runnable listener) {
        this.onPrendaChanged = listener;
    }

    public void setOnConfigChanged(Runnable listener) {
        this.onConfigChanged = listener;
    }

    private java.util.function.Consumer<org.example.model.TipoMedias> onBulkSocksCategoryChanged;

    public void setOnBulkSocksCategoryChanged(java.util.function.Consumer<org.example.model.TipoMedias> listener) {
        this.onBulkSocksCategoryChanged = listener;
    }

    private TipoPrenda tipoPrendaSeleccionada;
    private TipoGenero generoSeleccionado;
    private TipoTela telaSeleccionada = null; // Default null to enforce selection
    private org.example.model.TipoEscudo tipoEscudo = org.example.model.TipoEscudo.SUBLIMADO;
    private boolean isArqueroMode = false;
    private boolean isUpdatingUI = false;

    // Configurators
    private CamisetaConfigurator camisetaConfig;
    private ShortConfigurator shortConfig;
    private ConjuntoConfigurator conjuntoConfig;

    public PrendaDelegate(VBox container, PrendaVisualizer visualizer, Label lblPlaceholder) {
        this.container = container;
        this.visualizer = visualizer;
        this.lblPlaceholder = lblPlaceholder;
    }

    /**
     * Starts the garment configuration wizard.
     */
    public void iniciarWizard() {
        mostrarSeleccionPrenda();
    }

    /**
     * Resets the configuration to initial state WITHOUT returning to garment
     * selection.
     */
    public void reset() {
        generoSeleccionado = null;

        // Fix: Clear previous configurators to ensure fresh start
        camisetaConfig = null;
        shortConfig = null;
        conjuntoConfig = null;

        if (visualizer != null) {
            visualizer.setNotificationsSuspended(true);
            try {
                visualizer.setGenero(TipoGenero.HOMBRE);
                visualizer.setCorte(TipoCorte.CUADRADO);
                visualizer.setLargo(TipoLargo.MANGA_CORTA);
                visualizer.setCuello(TipoCuello.V);
                visualizer.setPunos(false);
                visualizer.setMalla(false);
                visualizer.setShorts(false);
                visualizer.setMedias(false);
                visualizer.setShortsStripe(false);
                visualizer.setShortsPicket(false);
                visualizer.setShortsPocket(false);
                visualizer.setShortsCuff(false);
                visualizer.setShortsCord(false);
                visualizer.setVisible(false);
            } finally {
                visualizer.setNotificationsSuspended(false);
            }
        }

        if (lblPlaceholder != null) {
            lblPlaceholder.setText("Seleccione Género para continuar ⚥");
            lblPlaceholder.setVisible(true);
        }

        // ONLY reset to gender selection, NOT to garment selection
        if (tipoPrendaSeleccionada != null) {
            mostrarConfiguracionPrenda(tipoPrendaSeleccionada);
        }
    }

    /**
     * Shows the garment type selection screen.
     */
    private void mostrarSeleccionPrenda() {
        // Proactive memory management when switching garment types
        org.example.utils.SVGCache.clear();

        if (visualizer != null) {
            visualizer.setVisible(false);
            // Fix: Reset visualizer state to prevent ghost elements
            visualizer.resetState();
            visualizer.setGenero(TipoGenero.HOMBRE);
            visualizer.setCorte(TipoCorte.CUADRADO);
            visualizer.setLargo(TipoLargo.MANGA_CORTA);
            visualizer.setCuello(TipoCuello.V);
            visualizer.setPunos(false);
            visualizer.setMalla(false);
            visualizer.setShorts(false);
            visualizer.setMedias(false);
            visualizer.setShortsStripe(false);
            visualizer.setShortsPicket(false);
            visualizer.setShortsPocket(false);
            visualizer.setShortsCuff(false);
            visualizer.setShortsCord(false);
        }
        if (lblPlaceholder != null) {
            lblPlaceholder.setVisible(true);
        }

        // Fix: Clear configurators to prevent state bleeding between garment changes
        camisetaConfig = null;
        shortConfig = null;
        conjuntoConfig = null;

        container.getChildren().clear();
        tipoPrendaSeleccionada = null;

        Label lblTitulo = new Label("Selecciona el Tipo de Prenda:");
        lblTitulo.getStyleClass().add("config-title");
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        lblTitulo.setAlignment(Pos.CENTER_LEFT);
        lblTitulo.setPadding(new javafx.geometry.Insets(0, 0, 5, 0));

        javafx.scene.layout.GridPane contenedorBotones = new javafx.scene.layout.GridPane();
        contenedorBotones.setHgap(15);
        contenedorBotones.setVgap(15);
        contenedorBotones.setAlignment(Pos.CENTER);
        contenedorBotones.setMaxWidth(Double.MAX_VALUE); // Let it shrink to fit panel

        // Two columns with 50% width each to ensure regular distribution
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setFillWidth(true);
        col1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setFillWidth(true);
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        contenedorBotones.getColumnConstraints().addAll(col1, col2);

        ToggleGroup grupo = new ToggleGroup();

        int row = 0;
        int col = 0;
        for (TipoPrenda prenda : TipoPrenda.values()) {
            SVGPath svgIcon = new SVGPath();
            svgIcon.setContent(prenda.getIconPath());
            svgIcon.setFill(Color.web("#2c3e50"));

            if (prenda == TipoPrenda.SHORT || prenda == TipoPrenda.CONJUNTO) {
                svgIcon.setScaleX(1.0);
                svgIcon.setScaleY(1.0);
            } else if (prenda == TipoPrenda.MOCHILA) {
                svgIcon.setScaleX(0.9);
                svgIcon.setScaleY(0.9);
            } else {
                svgIcon.setScaleX(0.9);
                svgIcon.setScaleY(0.9);
            }

            ToggleButton btn = UIFactory.crearBotonOpcion(prenda.getLabel(), svgIcon);
            btn.setMinHeight(72);
            btn.setPrefHeight(78);
            btn.setMaxWidth(Double.MAX_VALUE); // Expand to fill the 50% column width
            btn.setGraphicTextGap(8);
            btn.setWrapText(true);
            btn.setToggleGroup(grupo);
            btn.setUserData(prenda);

            contenedorBotones.add(btn, col, row);
            javafx.scene.layout.GridPane.setHgrow(btn, javafx.scene.layout.Priority.ALWAYS);

            col++;
            if (col == 2) {
                col = 0;
                row++;
            }
        }

        grupo.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;
            TipoPrenda prendaSeleccionada = (TipoPrenda) newVal.getUserData();
            mostrarConfiguracionPrenda(prendaSeleccionada);

            if (onPrendaChanged != null) {
                onPrendaChanged.run();
            }
        });

        container.getChildren().addAll(lblTitulo, contenedorBotones);
    }

    /**
     * Shows the configuration screen for a selected garment.
     */
    private void mostrarConfiguracionPrenda(TipoPrenda prenda) {
        container.getChildren().clear();
        tipoPrendaSeleccionada = prenda;
        // Reset Tela selection when switching garment type to enforce fresh selection
        telaSeleccionada = null;

        // Create header
        HBox headerBox = createHeader(prenda);
        container.getChildren().add(headerBox);

        // Show gender selection for configurable garments
        if (prenda == TipoPrenda.CAMISETA || prenda == TipoPrenda.SHORT || prenda == TipoPrenda.CONJUNTO) {
            agregarSeccionGenero(prenda);

            // Layout spacer
            javafx.scene.layout.Region sep = new javafx.scene.layout.Region();
            sep.setMinHeight(10);
            container.getChildren().add(sep);

            if (visualizer != null) {
                visualizer.setVisible(false);
            }
            if (lblPlaceholder != null) {
                lblPlaceholder.setText("Seleccione Género para continuar ⚥");
                lblPlaceholder.setVisible(true);
            }
        } else {
            // Placeholder for other garment types
            Label lblWIP = new Label("Opciones para " + prenda.getLabel() + " en desarrollo...");
            lblWIP.getStyleClass().add("wip-placeholder");
            container.getChildren().add(lblWIP);
        }
    }

    /**
     * Creates the header with garment icon and change button.
     */
    private HBox createHeader(TipoPrenda prenda) {
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.getStyleClass().add("header-container");

        SVGPath headerIcon = new SVGPath();
        headerIcon.setContent(prenda.getIconPath());
        headerIcon.setFill(Color.web("#2c3e50"));
        headerIcon.setScaleX(0.8);
        headerIcon.setScaleY(0.8);

        Label lblSelected = new Label(prenda.getLabel());
        lblSelected.getStyleClass().add("header-title-text");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnCambiar = new Button("Cambiar");
        btnCambiar.getStyleClass().add("button-destructive");
        btnCambiar.setOnAction(e -> confirmarCambioPrenda());

        headerBox.getChildren().addAll(headerIcon, lblSelected, spacer, btnCambiar);
        return headerBox;
    }

    /**
     * Adds gender selection section.
     */
    private void agregarSeccionGenero(TipoPrenda prenda) {
        String titulo = prenda == TipoPrenda.SHORT ? "Género" : "Género (Torso)";

        javafx.scene.layout.GridPane boxGenero = new javafx.scene.layout.GridPane();
        boxGenero.setHgap(15);
        boxGenero.setVgap(15);
        boxGenero.setAlignment(Pos.CENTER);
        boxGenero.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.ColumnConstraints gc1 = new javafx.scene.layout.ColumnConstraints();
        gc1.setPercentWidth(50);
        gc1.setFillWidth(true);
        gc1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.ColumnConstraints gc2 = new javafx.scene.layout.ColumnConstraints();
        gc2.setPercentWidth(50);
        gc2.setFillWidth(true);
        gc2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        boxGenero.getColumnConstraints().addAll(gc1, gc2);

        ToggleGroup grpGenero = new ToggleGroup();

        int col = 0;
        for (TipoGenero g : TipoGenero.values()) {
            if (g == TipoGenero.UNISEX)
                continue;

            String iconCode = g == TipoGenero.HOMBRE ? "mdi2g-gender-male" : "mdi2g-gender-female";

            ToggleButton btn = UIFactory.crearBotonOpcion(
                    g.getLabel(),
                    iconCode,
                    20);

            btn.setUserData(g);
            btn.setToggleGroup(grpGenero);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setGraphicTextGap(8);
            btn.setWrapText(true);

            if (g == generoSeleccionado) {
                btn.setSelected(true);
            }

            boxGenero.add(btn, col, 0);
            javafx.scene.layout.GridPane.setHgrow(btn, javafx.scene.layout.Priority.ALWAYS);
            col++;
        }

        grpGenero.selectedToggleProperty().addListener((o, v, n) -> {
            if (isUpdatingUI) return;
            if (n != null) {
                TipoGenero genero = (TipoGenero) n.getUserData();
                generoSeleccionado = genero;

                // DATA PERSISTENCE: Capture current state before switching/reloading
                ConfiguracionPrendaDTO previousConfig = getConfiguracion();

                // 2. Clear everything after index 2 (Header=0, GenderCard=1)
                // Note: Header is index 0. Gender Card is index 1.
                // We remove everything including the previous Spacer (index 2) to start fresh.
                while (container.getChildren().size() > 2) {
                    container.getChildren().remove(2);
                }

                // Add Spacer
                // Spacer removed for consistent card spacing

                // Add Tela Section
                agregarSeccionTela();

                if (telaSeleccionada != null) {
                    activarConfiguracion(tipoPrendaSeleccionada, generoSeleccionado, previousConfig);
                } else {
                    if (visualizer != null)
                        visualizer.setVisible(false);
                    if (lblPlaceholder != null) {
                        lblPlaceholder.setText("Seleccione el Tipo de Tela para continuar");
                        lblPlaceholder.setVisible(true);
                    }
                }
            } else {
                generoSeleccionado = null;
                // Clear everything after GenderCard
                while (container.getChildren().size() > 2) {
                    container.getChildren().remove(2);
                }
            }
        });

        // Wrap in Card
        container.getChildren().add(UIFactory.crearSeccionTarjeta(titulo, "mdi2g-gender-male-female", boxGenero));
    }

    private void agregarSeccionTela() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5, 0, 0, 0));

        ComboBox<TipoTela> comboTela = new ComboBox<>();
        comboTela.getItems().addAll(TipoTela.values());
        comboTela.setValue(telaSeleccionada);
        comboTela.setPromptText("Seleccione...");
        comboTela.setMaxWidth(Double.MAX_VALUE);
        comboTela.getStyleClass().add("combo-tela");
        comboTela.getStyleClass().add("combo-clean");

        TextField txtOtro = new TextField();
        txtOtro.setPromptText("Especifique el tipo de tela aquí...");
        txtOtro.setStyle(
                "-fx-background-color: white; -fx-border-color: #3498db; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-font-size: 13px;");

        // Initial visibility
        boolean isInitialOtro = (telaSeleccionada == TipoTela.OTRO);
        txtOtro.setVisible(isInitialOtro);
        txtOtro.setManaged(isInitialOtro);

        if (isInitialOtro && visualizer != null) {
            txtOtro.setText(visualizer.getState().getCustomTela());
        }

        comboTela.setOnAction(e -> {
            if (isUpdatingUI) return;
            TipoTela nuevaTela = comboTela.getValue();
            if (nuevaTela != null) {
                telaSeleccionada = nuevaTela;

                boolean isOtro = (nuevaTela == TipoTela.OTRO);
                txtOtro.setVisible(isOtro);
                txtOtro.setManaged(isOtro);

                if (isOtro) {
                    javafx.application.Platform.runLater(txtOtro::requestFocus);
                }

                if (visualizer != null) {
                    visualizer.setTela(nuevaTela);
                    if (!isOtro) {
                        visualizer.getState().setCustomTela("");
                    }
                }

                if (onConfigChanged != null) {
                    onConfigChanged.run();
                }

                if (generoSeleccionado != null) {
                    ConfiguracionPrendaDTO previousConfig = getConfiguracion();
                    activarConfiguracion(tipoPrendaSeleccionada, generoSeleccionado, previousConfig);
                }
            }
        });

        txtOtro.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdatingUI) return;
            if (visualizer != null && telaSeleccionada == TipoTela.OTRO) {
                visualizer.getState().setCustomTela(newV);
                if (onConfigChanged != null) {
                    onConfigChanged.run();
                }
            }
        });

        box.getChildren().addAll(comboTela, txtOtro);

        // Apply Card Style
        container.getChildren().add(UIFactory.crearSeccionTarjeta("TIPO DE TELA", "mdi2t-tshirt-crew", box));
    }

    /**
     * Activates the appropriate configurator based on garment type.
     * DOES NOT clear gender section - allows changing gender anytime.
     */
    private void activarConfiguracion(TipoPrenda prenda, TipoGenero genero, ConfiguracionPrendaDTO previousConfig) {
        if (visualizer != null) {
            visualizer.setNotificationsSuspended(true);
        }
        try {
            // Clear ALL configuration sections (everything after gender selection)
            while (container.getChildren().size() > 3) {
                container.getChildren().remove(3);
            }

            if (lblPlaceholder != null) {
                lblPlaceholder.setVisible(false);
            }

            if (visualizer != null) {
                visualizer.setVisible(true);
            }

            // Initialize Data State
            if (previousConfig != null && previousConfig.getTipoEscudo() != null) {
                this.tipoEscudo = previousConfig.getTipoEscudo();
            } else {
                this.tipoEscudo = org.example.model.TipoEscudo.SUBLIMADO;
            }

            // Always create NEW configurator to avoid state issues
            switch (prenda) {
                case CAMISETA:
                    camisetaConfig = new CamisetaConfigurator(container, visualizer);
                    camisetaConfig.setArqueroMode(this.isArqueroMode);
                    camisetaConfig.setOnConfigChanged(onConfigChanged);
                    camisetaConfig.setOnBulkSocksCategoryChanged(onBulkSocksCategoryChanged);
                    camisetaConfig.buildUI(genero, previousConfig, TipoPrenda.CAMISETA);
                    break;

                case SHORT:
                    shortConfig = new ShortConfigurator(container, visualizer);
                    shortConfig.setOnConfigChanged(onConfigChanged);
                    shortConfig.buildUI(genero, previousConfig);
                    break;

                case CONJUNTO:
                    conjuntoConfig = new ConjuntoConfigurator(container, visualizer);
                    conjuntoConfig.setOnConfigChanged(onConfigChanged);
                    // ConjuntoConfigurator uses CamisetaConfigurator internally
                    camisetaConfig = conjuntoConfig.getCamisetaConfig();
                    if (camisetaConfig != null) {
                        camisetaConfig.setArqueroMode(this.isArqueroMode);
                        camisetaConfig.setOnBulkSocksCategoryChanged(onBulkSocksCategoryChanged);
                    }
                    conjuntoConfig.buildUI(genero, previousConfig);
                    break;

                default:
                    Label lblWIP = new Label("Configuración para " + prenda.getLabel() + " en desarrollo...");
                    lblWIP.getStyleClass().add("wip-placeholder");
                    container.getChildren().add(lblWIP);
            }
        } finally {
            if (visualizer != null) {
                visualizer.setNotificationsSuspended(false);
                // Force a single notification at the end
                visualizer.notifyStateChanged();
            }
        }
    }

    /**
     * Confirms garment change with user.
     */
    private void confirmarCambioPrenda() {
        boolean confirmar = UIFactory.mostrarConfirmacion(
                "¿Estás seguro?",
                "Si cambias de prenda, se perderá la configuración actual.");

        if (confirmar) {
            mostrarSeleccionPrenda();
        }
    }

    // ===== PUBLIC GETTERS FOR CONTROLLER =====

    public TipoPrenda getSeleccionTipoPrenda() {
        return tipoPrendaSeleccionada;
    }

    public TipoGenero getSeleccionGenero() {
        return generoSeleccionado;
    }

    /**
     * Gets the complete configuration from the active configurator.
     */
    public ConfiguracionPrendaDTO getConfiguracion() {
        if (tipoPrendaSeleccionada == null) {
            return null;
        }

        switch (tipoPrendaSeleccionada) {
            case CAMISETA:
                return camisetaConfig != null
                        ? camisetaConfig.getConfigBuilder()
                                .tipoPrenda(TipoPrenda.CAMISETA)
                                .tela(telaSeleccionada)
                                .tipoEscudo(this.tipoEscudo) // Inject managed state
                                .build()
                        : null;

            case SHORT:
                // Note: Short might typically use same fabric or different, assuming global
                // selection for now
                return shortConfig != null
                        ? shortConfig.getConfigBuilder()
                                .tipoPrenda(TipoPrenda.SHORT)
                                .tela(telaSeleccionada)
                                // Short usually doesn't have chest shield, but if it did...
                                .build()
                        : null;

            case CONJUNTO:
                // Conjunto likely has complex internal logic, but we inject the global fabric
                // selection
                // If ConjuntoConfigurator returns a finished DTO, we might need to rebuild it
                // or inject it earlier
                // Let's assume we can rebuild or the configurator needs to know.
                // Ideally, ConjuntoConfigurator should accept the fabric.
                // For now, let's try to wrap it if possible, or just build from it.
                // Simpler: Just rebuild the DTO adding the fabric if ConjuntoConfig doesn't
                // support it directly.
                if (conjuntoConfig != null) {
                    ConfiguracionPrendaDTO c = conjuntoConfig.getConfiguration();
                    return new ConfiguracionPrendaDTO.Builder().from(c).tela(telaSeleccionada).tipoEscudo(this.tipoEscudo).build();
                }
                return null;

            default:
                return null;
        }
    }

    // Legacy compatibility methods (for existing code that uses old API)
    public TipoCorte getSeleccionCorte() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null ? config.getCorte() : null;
    }

    public TipoLargo getSeleccionLargo() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null ? config.getLargo() : null;
    }

    public TipoCuello getSeleccionCuello() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null ? config.getCuello() : null;
    }

    public boolean isLlevaMalla() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null && config.llevaMalla();
    }

    public boolean isLlevaPunoCamiseta() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null && config.llevaPunoCamiseta();
    }

    public boolean llevaShort() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null && config.llevaShort();
    }

    public boolean llevaFranjaShort() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null && config.llevaFranjaShort();
    }

    public boolean llevaBolsilloShort() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null && config.llevaBolsilloShort();
    }

    public boolean isLlevaMedias() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null && config.llevaMedias();
    }

    public boolean isLlevaPunoShort() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        return config != null && config.llevaPunoShort();
    }

    public void setTipoEscudo(org.example.model.TipoEscudo t) {
        this.tipoEscudo = t;
    }

    public org.example.model.TipoEscudo getTipoEscudo() {
        return this.tipoEscudo;
    }

    public String getTipoShortSeleccionado() {
        ConfiguracionPrendaDTO config = getConfiguracion();
        if (config != null && config.getCorteShort() != null) {
            TipoCorte c = config.getCorteShort();
            if (c == TipoCorte.LICRA)
                return "Licra";
            if (c == TipoCorte.PANTALONETA)
                return "Pantaloneta";
            return "Short"; // Default for Cuadrado/Deportivo/Nova
        }
        return "Prenda Inf.";
    }

    /**
     * Restores the full UI state from a saved DTO.
     * This ensures the configuration sidebar matches the loaded design.
     */
    public void restoreFromState(org.example.dto.save.PrendaStateDTO stateDTO) {
        this.isUpdatingUI = true;
        try {
            if (stateDTO == null) {
                // Create a default state DTO if null to avoid throwing or exiting
                stateDTO = new org.example.dto.save.PrendaStateDTO();
                stateDTO.setCurrentGarmentType("CAMISETA");
                stateDTO.setCurrentGenero(TipoGenero.HOMBRE);
                stateDTO.setCurrentTela(TipoTela.WIN);
            }

            // 1. Identify Garment Type
            String gTypeStr = stateDTO.getCurrentGarmentType();
            if (gTypeStr == null) {
                gTypeStr = "CAMISETA";
            }

            TipoPrenda prenda = null;
            try {
                prenda = TipoPrenda.valueOf(gTypeStr.toUpperCase());
            } catch (Exception e) {
                // Fallback: try to find by label if valueOf fails
                for (TipoPrenda tp : TipoPrenda.values()) {
                    if (tp.getLabel().equalsIgnoreCase(gTypeStr)) {
                        prenda = tp;
                        break;
                    }
                }
            }

            if (prenda == null) {
                prenda = TipoPrenda.CAMISETA;
            }

            // 2. Set Basic State
            this.tipoPrendaSeleccionada = prenda;
            this.generoSeleccionado = stateDTO.getCurrentGenero() != null ? stateDTO.getCurrentGenero() : TipoGenero.HOMBRE;
            this.telaSeleccionada = stateDTO.getCurrentTela() != null ? stateDTO.getCurrentTela() : TipoTela.WIN;

            if (visualizer != null && this.telaSeleccionada != null) {
                visualizer.setTela(this.telaSeleccionada);
            }

            // 3. Rebuild UI Hierarchy
            // Let's call the configurator activator directly with a pseudo-DTO
            // reconstructed from the save state
            ConfiguracionPrendaDTO.Builder builder = new ConfiguracionPrendaDTO.Builder()
                    .tipoPrenda(prenda)
                    .genero(this.generoSeleccionado)
                    .tela(this.telaSeleccionada)
                    .customTela(stateDTO.getCustomTela() != null ? stateDTO.getCustomTela() : "")
                    .conMalla(stateDTO.isHasMesh())
                    .conPunoCamiseta(stateDTO.isHasCuffs())
                    .conFranjaCamiseta(stateDTO.isHasShirtStripe())
                    .conLineaCamiseta(stateDTO.isHasShirtLinea())
                    .conAcolchado(stateDTO.isHasPadding())
                    .conShort(stateDTO.isHasShorts())
                    .conMedias(stateDTO.isHasSocks())
                    .conLigaMedias(stateDTO.isHasSocksTop())
                    // Short specific
                    .conFranjaShort(stateDTO.isHasShortsStripe())
                    .conLineaShort(stateDTO.isHasShortsLinea())
                    .conPiqueteShort(stateDTO.isHasShortsPicket())
                    .conBolsilloShort(stateDTO.isHasShortsPocket())
                    .conPunoShort(stateDTO.isHasShortsCuff())
                    .conPasadorShort(stateDTO.isHasShortsCord())
                    .conForroShort(stateDTO.isHasShortsLining());

            if (stateDTO.getCurrentCorte() != null)
                builder.corte(stateDTO.getCurrentCorte());
            if (stateDTO.getCurrentLargo() != null)
                builder.largo(stateDTO.getCurrentLargo());
            if (stateDTO.getCurrentCuello() != null)
                builder.cuello(stateDTO.getCurrentCuello());
            if (stateDTO.getCurrentCorteShort() != null)
                builder.corteShort(stateDTO.getCurrentCorteShort());
            if (stateDTO.getCurrentTipoMedias() != null)
                builder.tipoMedias(stateDTO.getCurrentTipoMedias());

            if (stateDTO.getShirtCrestTech() != null) {
                builder.tipoEscudo(org.example.model.TipoEscudo.valueOfTech(stateDTO.getShirtCrestTech()));
            } else {
                builder.tipoEscudo(org.example.model.TipoEscudo.SUBLIMADO);
            }

            ConfiguracionPrendaDTO reconstructedConfig = builder.build();

            // Refresh Sidebar
            container.getChildren().clear();
            container.getChildren().add(createHeader(prenda));

            // We skip showing the gender selection screen and go straight to the
            // configurator
            // BUT we need to add the gender section and fabric section so they are visible
            // and editable
            agregarSeccionGenero(prenda);
            agregarSeccionTela();

            // Trigger activation
            activarConfiguracion(prenda, this.generoSeleccionado, reconstructedConfig);
        } finally {
            this.isUpdatingUI = false;
        }
    }

    /**
     * Updates the visualizer based on current configuration.
     * Legacy method for compatibility.
     */
    public void actualizarVector() {
        // The visualizer is automatically updated by configurators
    }

    private void resetToGenderSelection() {
        // Clear configuration sections (keep Header + Gender options + Tela)
        // Required indices: 0, 1, 2
        while (container.getChildren().size() > 3) {
            container.getChildren().remove(3);
        }

        // Hide Visualizer
        if (visualizer != null) {
            visualizer.setVisible(false);
        }

        // Show Placeholder
        if (lblPlaceholder != null) {
            lblPlaceholder.setText("Seleccione Género para continuar ⚥");
            lblPlaceholder.setVisible(true);
        }
    }

    public void updateSocksHighlights(java.util.Set<org.example.model.TipoMedias> activeInRoster) {
        if (camisetaConfig != null) {
            camisetaConfig.updateSocksHighlights(activeInRoster);
        }
        if (conjuntoConfig != null && conjuntoConfig.getCamisetaConfig() != null) {
            conjuntoConfig.getCamisetaConfig().updateSocksHighlights(activeInRoster);
        }
    }

    public void setArqueroMode(boolean mode) {
        this.isArqueroMode = mode;
        if (camisetaConfig != null) {
            camisetaConfig.setArqueroMode(mode);
        }
        if (conjuntoConfig != null && conjuntoConfig.getCamisetaConfig() != null) {
            conjuntoConfig.getCamisetaConfig().setArqueroMode(mode);
        }
    }
}
