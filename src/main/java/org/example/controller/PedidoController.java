package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.model.DetallePedido; 
import org.example.service.OrderService;
import org.example.service.SvgExportService;
import org.example.service.PdfExportService;
import org.example.component.PrendaVisualizer;

import java.util.List;
import org.example.utils.UIFactory;
import org.example.controller.delegate.JugadoresDelegate;
import org.example.controller.delegate.PrendaDelegate;
import org.example.controller.delegate.PersonalizacionDelegate;
import org.example.dto.ConfiguracionPrendaDTO;

import java.util.function.Supplier;
import java.io.File;
import javafx.scene.paint.Color;

import org.example.controller.helper.KeyboardShortcutManager;
import org.example.controller.helper.WizardFlowManager;



public class PedidoController {
    // Recompile trigger: Resolved UIFactory dependency.

    File currentProjectFile;
    private final PedidoGoalkeeperCoordinator goalkeeperCoordinator = new PedidoGoalkeeperCoordinator(this);
    boolean wizardStarted = false;
    private boolean isStandalone = true; 
    
    // --- LISTENER FOR TAB TITLE ---
    private java.util.function.Consumer<String> onTitleChanged;

    public void setOnTitleChanged(java.util.function.Consumer<String> listener) {
        this.onTitleChanged = listener;
    }

    @FXML
    private HBox titleBar;

    public void setShellMode(boolean shellMode) {
        this.isStandalone = !shellMode;
        if (titleBar != null) {
            titleBar.setManaged(isStandalone);
            titleBar.setVisible(isStandalone);
        }
    }

    org.example.dto.DatosEnvioDTO datosEnvio = new org.example.dto.DatosEnvioDTO();
    
    // --- CABECERA ---
    @FXML
    TextField txtCliente;
    @FXML
    ComboBox<String> comboVendedor;
    @FXML
    Label lblCodigoPedido;
    @FXML
    private Label lblArchivoTitleBar;
    @FXML
    private Button btnDatosEnvio;

    // --- VISTA DINÁMICA (WIZARD) ---
    @FXML
    private VBox dynamicFormContainer;

    // --- NUEVOS CONTROLES ---
    @FXML
    private VBox rootVBox;
    @FXML
    TabPane mainTabPane;
    @FXML
    private Button btnFinalizar;
    @FXML
    private Button btnResetGlobal;
    @FXML
    private ToggleButton btnToggleDesignPanel;
    @FXML
    private ToggleButton btnToggleLogosPanel;

    private Button btnCenterView; // New Application Control (Floating)

    // --- TAB 2 CONTAINERS ---
    @FXML
    private VBox dynamicLogosContainer;
    @FXML
    private StackPane stackVectorContainerLogos;
    @FXML
    private VBox configSideWrapper;
    @FXML
    private VBox logosSideWrapper;
    @FXML
    private VBox configSidePanel;
    @FXML
    private VBox logosSidePanel;

    // --- ESTADO DEL PEDIDO (DELEGADO) ---
    PrendaDelegate prendaDelegate;

    // --- PESTAÑA 2 (Tabla Nombres) ---
    @FXML
    private TableView<DetallePedido> tablaDetalles;
    // ... (otras columnas y campos se mantienen igual, solo limpieza de imports
    // viejos si aplica)
    @FXML
    private TableColumn<DetallePedido, String> colNombre;
    @FXML
    private TableColumn<DetallePedido, String> colNumero;
    @FXML
    private TableColumn<DetallePedido, String> colTalla;

    @FXML
    private TextField txtInputNombre;
    @FXML
    private TextField txtInputNumero;
    @FXML
    private ComboBox<String> comboInputTalla;
    @FXML
    private Label lblTotalPrendas;

    // --- VISTA PREVIA (Vector Engine) ---
    @FXML
    private StackPane stackVectorContainer;
    PrendaVisualizer prendaVisualizer;

    // --- NUEVO GESTOR VENTANA ---
    private org.example.controller.helper.WindowControlManager windowControlManager;

    private void ensureWindowControlManager() {
        if (windowControlManager == null) {
            windowControlManager = new org.example.controller.helper.WindowControlManager(iconWinMax);
        }
    }

    @FXML
    private javafx.scene.shape.SVGPath iconWinMax;

    @FXML
    private void maximizeApp(javafx.scene.input.MouseEvent event) {
        ensureWindowControlManager();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        windowControlManager.toggleMaximize(stage);
    }

    @FXML
    private void handleTitleBarClicked(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            maximizeApp(event);
        }
    }

    @FXML
    private void minimizeApp(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void closeApp(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleWindowDragPressed(javafx.scene.input.MouseEvent event) {
        ensureWindowControlManager();
        windowControlManager.handleWindowDragPressed(event);
    }

    @FXML
    private void handleWindowDragDragged(javafx.scene.input.MouseEvent event) {
        ensureWindowControlManager();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        windowControlManager.handleWindowDragDragged(event, stage);
    }

    @FXML
    private void handleWindowDragReleased(javafx.scene.input.MouseEvent event) {
        ensureWindowControlManager();
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        windowControlManager.handleWindowDragReleased(event, stage);
    }

    @FXML
    private ImageView imgBase, imgManga, imgCuello, imgMalla, imgPuno, imgMedias;

    // Datos de la tabla
    final ObservableList<DetallePedido> listaJugadores = FXCollections.observableArrayList(p -> 
        new javafx.beans.Observable[] { 
            p.nombreProperty(), p.numeroProperty(), p.tallaProperty(),
            p.generoProperty(), p.tipoMangaProperty(), p.tallaShortProperty(),
            p.includeTopProperty(), p.includeBottomProperty(), p.includeSocksProperty(),
            p.esArqueroProperty(), p.colorArqueroProperty(), p.tipoMangaArqueroProperty()
        }
    );
    // private final PedidoDAO pedidoDAO = new PedidoDAO(); // REMOVED
    // private final VendedorDAO vendedorDAO = new VendedorDAO(); // REMOVED

    // SERVICE LAYER
    private final OrderService orderService = new OrderService();
    private final org.example.service.OrderSchedulerService schedulerService = new org.example.service.OrderSchedulerService(); // NEW

    // DELEGATES & MANAGERS
    JugadoresDelegate jugadoresDelegate;
    PersonalizacionDelegate personalizacionDelegate;
    private KeyboardShortcutManager shortcutManager;
    private WizardFlowManager wizardFlowManager;

    @FXML
    private Label lblPlaceholder; // Placeholder Message

    // SCHEDULER UI
    @FXML
    ComboBox<org.example.service.OrderSchedulerService.Priority> comboPrioridad;
    @FXML
    Label lblFechaEntrega;
    java.time.LocalDate fechaEntregaCalculada;

    // DESIGN MODE (Campo / Arquero) - mantiene 2 diseños sin perder el pedido
    @FXML
    ComboBox<String> comboModoDiseno;
    boolean editandoDisenoArquero = false;
    boolean switchingDesignMode = false;
    boolean isRestoringDesign = false; // Flag to prevent UI updates from marking goalie as personalized
    boolean projectDirty = false; // Tracks if there are unsaved changes
    org.example.dto.save.PrendaStateDTO disenoCampoConfig = null;
    java.util.List<org.example.dto.save.LayerDTO> disenoCampoLayers = new java.util.ArrayList<>();
    org.example.dto.save.PrendaStateDTO disenoArqueroConfig = null;
    java.util.List<org.example.dto.save.LayerDTO> disenoArqueroLayers = new java.util.ArrayList<>();
    int nextArqueroMarkOrder = 1;

    boolean fichaDirty = true;
    boolean isGeneratingFicha = false;
    private static final double SIDEBAR_MIN_WIDTH = 360.0;
    private static final double SIDEBAR_MAX_WIDTH = 420.0;
    private static final double WORKFLOW_TABS_COMPACT_WIDTH = 1440.0;
    private static final double WORKFLOW_TABS_MEDIUM_WIDTH = 1640.0;
    private boolean sidebarWidthListenerAttached = false;

    @FXML
    public void initialize() {
        setupResponsiveSidebars();
        setupOverlayPanelToggles();
        cargarVendedores();
        generarProximoCodigo();
        // No iniciar wizard todavía: mostramos una pantalla previa (Nuevo / Abrir)
        // para facilitar manejar múltiples proyectos/pedidos.
        wizardStarted = false;
        actualizarDatosEnvioUI();

        // --- GLOBAL KEYBOARD ACCELERATORS (Managed) ---
        if (rootVBox != null) {
            rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    if (shortcutManager == null) shortcutManager = new KeyboardShortcutManager(prendaVisualizer);
                    shortcutManager.setupAccelerators(newScene);
                    
                    // ADD GLOBAL DELETE KEY HANDLER
                    newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                        if (e.getCode() == javafx.scene.input.KeyCode.DELETE) {
                            if (prendaVisualizer != null && prendaVisualizer.getLayerManager() != null) {
                                javafx.scene.Node focused = newScene.getFocusOwner();
                                // Only delete if we are not typing in a text field
                                if (!(focused instanceof javafx.scene.control.TextInputControl)) {
                                    javafx.scene.Node selected = prendaVisualizer.getLayerManager().getSelectedNode();
                                    if (selected != null) {
                                        // Ensure we don't delete a layer that is currently being edited inline
                                        if (selected instanceof org.example.component.TextLayer && ((org.example.component.TextLayer) selected).isBeingEdited()) {
                                            return; 
                                        }
                                        // Ensure we don't delete the layer if we are editing its nodes
                                        if (selected instanceof org.example.component.ShapeLayer && ((org.example.component.ShapeLayer) selected).isNodeEditing()) {
                                            return;
                                        }
                                        prendaVisualizer.getLayerManager().removeLayer(selected);
                                        e.consume();
                                    }
                                }
                            }
                        }
                    });
                }
            });
        }

        // --- FICHA TÉCNICA REACTION ---
        // Ensure ficha is marked as dirty whenever the roster changes (any add, remove or property update)
        listaJugadores.addListener((javafx.collections.ListChangeListener<DetallePedido>) c -> {
            fichaDirty = true;
            
            if (prendaVisualizer == null) return;

            while (c.next()) {
                if (c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        DetallePedido p = c.getList().get(i);
                        syncArqueroPreviewFromRosterUpdate(p);
                    }
                }
                
                // If an arquero was added or removed, update the design mode option
                if (c.wasAdded() || c.wasRemoved()) {
                    updateArqueroOptionStatus();
                }
                
                // Keep configurator socks highlights in sync with roster content
                syncSocksHighlightsWithRoster();
            }
        });

        // Detect initial state and sync icon
        javafx.application.Platform.runLater(() -> {
            try {
                Stage stage = (Stage) stackVectorContainer.getScene().getWindow();
                // In Shell mode, the ShellController handles closing tabs/windows.
                // We only set this listener if we are in traditional Standalone mode.
                if (isStandalone) {
                    stage.setOnCloseRequest(e -> {
                        if (!confirmarCierreVentana()) {
                            e.consume();
                        }
                    });
                }
                // Check if Main.java already maximized it via manual coords
                ensureWindowControlManager();
                windowControlManager.initChecks(stage);

                actualizarTituloVentana();
            } catch (Exception e) {
            }
        });

        // --- WIZARD: LINEAR NAVIGATION SETUP ---
        if (mainTabPane != null) {
            // Disable Tabs 2 (Logos) and 3 (List) initially
            if (mainTabPane.getTabs().size() > 1)
                mainTabPane.getTabs().get(1).setDisable(true);
            if (mainTabPane.getTabs().size() > 2)
                mainTabPane.getTabs().get(2).setDisable(true);
        }

        // Tabla setup
        if (colNombre != null)
            colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        if (colNumero != null)
            colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        if (colTalla != null)
            colTalla.setCellValueFactory(new PropertyValueFactory<>("talla"));

        // PROGRAMMATIC TABLE REPLACEMENT (Fix Alignment)
        TableView<DetallePedido> tableToUse = tablaDetalles;
        if (tablaDetalles != null) {
            Supplier<ConfiguracionPrendaDTO> configSupplier = () -> (prendaDelegate != null)
                    ? prendaDelegate.getConfiguracion()
                    : null;
            // Initialize TableView using the CLEAN builder
            TableView<DetallePedido> cleanTable = org.example.controller.component.PedidoTableManager.build(
                    listaJugadores,
                    configSupplier); // Uses PedidoTableManager for generic logic

            if (org.example.controller.helper.TableLayoutHelper.replaceTableInParent(tablaDetalles, cleanTable)) {
                tableToUse = cleanTable;
                this.tablaDetalles = cleanTable; // Update reference
            }
        }

        if (tableToUse != null) {
            tableToUse.setItems(listaJugadores);
            // Delegate Init
            jugadoresDelegate = new JugadoresDelegate(
                    tableToUse, txtInputNombre, txtInputNumero, comboInputTalla, lblTotalPrendas, listaJugadores);
            jugadoresDelegate.setGeneroSupplier(() -> prendaDelegate.getSeleccionGenero());
            // Inject Configuration logic
            jugadoresDelegate.setConfigSupplier(() -> prendaDelegate.getConfiguracion());
        }

        // Initialize Priority
        if (comboPrioridad != null) {
            comboPrioridad.setItems(
                    FXCollections.observableArrayList(org.example.service.OrderSchedulerService.Priority.values()));
            comboPrioridad.setValue(org.example.service.OrderSchedulerService.Priority.NORMAL);
            comboPrioridad.valueProperty().addListener((obs, oldVal, newVal) -> calcularFechaEntrega());
            calcularFechaEntrega(); // Initial calc

            // Render friendly name
            comboPrioridad.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(org.example.service.OrderSchedulerService.Priority item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null)
                        setText(null);
                    else
                        setText(item.getLabel());
                }
            });
            comboPrioridad.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(org.example.service.OrderSchedulerService.Priority item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null)
                        setText(null);
                    else
                        setText(item.getLabel());
                }
            });
        }

        // Modo de diseño (Camiseta / Arquero)
        if (comboModoDiseno != null) {
            if (!comboModoDiseno.getItems().contains("Jugador")) {
                comboModoDiseno.getItems().add("Jugador");
            }
            updateArqueroOptionStatus(); // Initial check
            comboModoDiseno.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (switchingDesignMode || !wizardStarted || prendaVisualizer == null || newVal == null)
                    return;
                
                boolean toArquero = "Arquero".equalsIgnoreCase(newVal.trim());
                if (toArquero == editandoDisenoArquero)
                    return;

                System.out.println("SWITCH DESIGN triggered by Combo: " + newVal);
                // Force sync call to avoid async racing with other events
                cambiarModoDiseno(toArquero);
            });
        }

        // Listener para activar/desactivar opción de Arquero según la lista
        listaJugadores.addListener((javafx.collections.ListChangeListener<org.example.model.DetallePedido>) c -> {
            markProjectDirty();
            while (c.next()) {
                if (c.wasAdded()) {
                    for (org.example.model.DetallePedido p : c.getAddedSubList()) {
                        addListenersToJugador(p);
                    }
                }
            }
            updateArqueroOptionStatus();
        });

        // Add listeners to initial items
        for (org.example.model.DetallePedido p : listaJugadores) {
            addListenersToJugador(p);
        }

        // Inputs tabla
        if (comboInputTalla != null) {
            // Tallas handled by Delegate
        }

        prendaVisualizer = new PrendaVisualizer();
        prendaVisualizer.setRulersVisible(false); // Default to Hidden for Tab 1 startup

        if (stackVectorContainer != null) {
            // Eliminar contenido previo (incluyendo ImageViews del FXML si no se usan)
            stackVectorContainer.getChildren().clear();

            // Vincular tamaño del visualizador al contenedor
            // IMPORTANTE: NO bindear width/height manual para que StackPane pueda aplicar
            // Padding
            // prendaVisualizer.prefWidthProperty().bind(stackVectorContainer.widthProperty());
            // prendaVisualizer.prefHeightProperty().bind(stackVectorContainer.heightProperty());

            // Agregar el visualizador al contenedor
            stackVectorContainer.getChildren().add(prendaVisualizer);
            prendaVisualizer.setEditModeContainer(stackVectorContainer);

            // --- FLOATING CONTROLS ---
            // 1. Center View Button
            btnCenterView = new Button();
            btnCenterView.setGraphic(UIFactory.crearIcono("mdi2c-crosshairs-gps", 22, "#555555"));
            btnCenterView.getStyleClass().add("button-center-view");
            btnCenterView.setTooltip(new Tooltip("Centrar Diseño"));
            StackPane.setAlignment(btnCenterView, javafx.geometry.Pos.BOTTOM_RIGHT);
            StackPane.setMargin(btnCenterView, new javafx.geometry.Insets(20));

            // Interaction Effects
            // Interaction Effects (Pulse Animation on Click)
            btnCenterView.setOnAction(e -> {
                // 1. Visual Feedback (Pressed State)
                try {
                    btnCenterView.setGraphic(UIFactory.crearIcono("mdi2c-crosshairs-gps", 22, "white"));
                } catch (Exception ex) {
                }

                // 2. Perform Action
                prendaVisualizer.resetView();

                // 3. Revert after short delay (Pulse)
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(150));
                pause.setOnFinished(ev -> {
                    try {
                        btnCenterView.setGraphic(UIFactory.crearIcono("mdi2c-crosshairs-gps", 22, "#555555"));
                    } catch (Exception ex) {
                    }
                });
                pause.play();
            });

            // Clean Mouse Hover Effect (Optional but nice)
            btnCenterView.setOnMouseEntered(e -> {
                if (!btnCenterView.getStyle().contains("#3498db")) { // Only if not processing click
                    // Hover handled by CSS
                }
            });
            btnCenterView.setOnMouseExited(e -> {
                // CSS handles hover reset automatically via .button-center-view class
            });

            stackVectorContainer.getChildren().add(btnCenterView);

            // --- KEYBOARD SHORTCUTS (Application Scope - Managed) ---
            stackVectorContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    if (shortcutManager == null) shortcutManager = new KeyboardShortcutManager(prendaVisualizer);
                    shortcutManager.installEventFilter(newScene);
                }
            });

            // --- PLACEHOLDER SETUP ---
            lblPlaceholder = new Label("Seleccione una prenda para comenzar 👕");
            lblPlaceholder.getStyleClass().add("placeholder-label");
            lblPlaceholder.setWrapText(true);
            lblPlaceholder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            stackVectorContainer.getChildren().add(lblPlaceholder);

            // Re-agregar Botón Reset (que fue borrado por clear()) y traer al frente
            if (btnResetGlobal != null) {
                stackVectorContainer.getChildren().add(btnResetGlobal);
                btnResetGlobal.toFront();
            }

            // Estado Inicial
            prendaVisualizer.setVisible(false);
            lblPlaceholder.setVisible(true);

            // --- SAVE / LOAD SYSTEM (REMOVED) ---

            // --- INITIALIZE WIZARD FLOW MANAGER ---
            this.wizardFlowManager = new WizardFlowManager(mainTabPane, btnFinalizar, prendaVisualizer, 
                                        stackVectorContainer, stackVectorContainerLogos, 
                                        btnResetGlobal, btnCenterView, lblPlaceholder);

            // --- DELEGATE INITIALIZATION ---
            prendaDelegate = new PrendaDelegate(dynamicFormContainer, prendaVisualizer, lblPlaceholder);
            prendaDelegate.setOnConfigChanged(() -> {
                fichaDirty = true;
                markProjectDirty();
            });
            prendaDelegate.setOnBulkSocksCategoryChanged(this::applySocksCategoryToAll);

            jugadoresDelegate = new JugadoresDelegate(tablaDetalles, txtInputNombre, txtInputNumero, comboInputTalla, lblTotalPrendas, listaJugadores);
            jugadoresDelegate.setConfigSupplier(() -> prendaDelegate.getConfiguracion());

            // --- RESET LOGIC ON GARMENT CHANGE ---
            prendaDelegate.setOnPrendaChanged(() -> {
                // 1. Lock future tabs
                if (mainTabPane != null) {
                    if (mainTabPane.getTabs().size() > 1)
                        mainTabPane.getTabs().get(1).setDisable(true);
                    if (mainTabPane.getTabs().size() > 2)
                        mainTabPane.getTabs().get(2).setDisable(true);
                    mainTabPane.getSelectionModel().select(0);
                    
                    // Add listener to sync highlights when returning to Design tab
                    mainTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
                        if (newIdx.intValue() == 0) {
                            syncSocksHighlightsWithRoster();
                        }
                    });
                }

                // 2. Clear data
                limpiarDatos();
            });

            // Setup Personalization Delegate (Tab 2)
            if (dynamicLogosContainer != null) {
                personalizacionDelegate = new PersonalizacionDelegate(dynamicLogosContainer, prendaVisualizer);
                personalizacionDelegate.setupUI();

                // Link Escudo Change
                personalizacionDelegate.setOnEscudoChange(val -> {
                    if (prendaDelegate != null) {
                        prendaDelegate.setTipoEscudo(val);
                        fichaDirty = true;
                    }
                });

                // should mark the technical sheet as dirty so it re-generates when entering Tab 4.
                prendaVisualizer.addOnStateChanged(() -> {
                    fichaDirty = true;
                    markProjectDirty();
                    if (editandoDisenoArquero) {
                        if (!isRestoringDesign && !switchingDesignMode) {
                            prendaVisualizer.setArqueroDisenoPersonalizado(true); // User is manually painting/designing the goalie
                        }
                    } else if (!prendaVisualizer.isArqueroDisenoPersonalizado()) {
                        // Real-time Inheritance: Sync to the goalie if the user is editing the player
                        // and hasn't manually customized the goalie yet.
                        syncArqueroDesignFromPlayer(getArqueroPrincipal());
                    }

                    if (mainTabPane != null && mainTabPane.getSelectionModel().getSelectedItem() == getFichaTecnicaManager().tabFicha) {
                        generarFichaTecnica();
                    }
                });
            }

            // Widen the sidebar for clearer text
            // Removed fixed widths to allow CSS/FXML to handle responsiveness
            // dynamicFormContainer.setMinWidth(370);
            // dynamicFormContainer.setPrefWidth(370);
        }

        // --- BUTTONS & NAVIGATION SETUP ---

        // 1. Reset Global Logic
        if (btnResetGlobal != null) {
            // Define Styles for Animation - NEW COLOR: Blue (Standard Action)
            final String RESET_NORMAL = "-fx-background-color: #3498db; -fx-background-radius: 50%; -fx-text-fill: white; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 4, 0, 0, 1); -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px; -fx-padding: 0; -fx-alignment: center;";
            final String RESET_PRESSED = "-fx-background-color: #2980b9; -fx-background-radius: 50%; -fx-text-fill: white; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 2, 0, 0, 1); -fx-scale-x: 0.95; -fx-scale-y: 0.95; -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px; -fx-padding: 0; -fx-alignment: center;";

            // Apply Initial Style force
            btnResetGlobal.setStyle(RESET_NORMAL);

            // Explicitly set a larger, clearer icon
            try {
                btnResetGlobal.setText(""); // Ensure no text is visible
                btnResetGlobal.setGraphic(UIFactory.crearIcono("mdi2r-restart", 24, "white"));
            } catch (Exception ex) {
                btnResetGlobal.setText("R");
            }

            btnResetGlobal.setOnAction(e -> {
                // Animate click just in case (e.g if triggered programmatically)
                resetConfiguracion();
            });

            // PRESS ANIMATION (Color Flash)
            btnResetGlobal.setOnMousePressed(e -> {
                btnResetGlobal.setStyle(RESET_PRESSED);
            });

            btnResetGlobal.setOnMouseReleased(e -> {
                btnResetGlobal.setStyle(RESET_NORMAL);
            });

            // Hover effects (Optional polish)
            btnResetGlobal.setOnMouseEntered(e -> {
                if (!e.isPrimaryButtonDown()) {
                    btnResetGlobal.setStyle(
                            "-fx-background-color: #5dade2; -fx-background-radius: 50%; -fx-text-fill: white; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 6, 0, 0, 2); -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px; -fx-padding: 0; -fx-alignment: center;");
                }
            });
            btnResetGlobal.setOnMouseExited(e -> {
                if (!e.isPrimaryButtonDown())
                    btnResetGlobal.setStyle(RESET_NORMAL);
            });
        }

        // 2. Navigation Logic (Strict)
        if (mainTabPane != null && btnFinalizar != null) {
            btnFinalizar.setOnAction(e -> handleMainAction());

            // Listeners
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                int idx = mainTabPane.getSelectionModel().getSelectedIndex();
                wizardFlowManager.actualizarBotonAccion();
                moverVisualizador(idx);

                // Update Reset Button Action per Tab
                if (btnResetGlobal != null) {
                    if (idx == 1) {
                        btnResetGlobal.setOnAction(e -> {
                            prendaVisualizer.resetColors();
                            if (personalizacionDelegate != null) personalizacionDelegate.refreshContent();
                        });
                    } else {
                        btnResetGlobal.setOnAction(e -> resetConfiguracion());
                    }
                }

                // Hide floating toolbar on tab change
                if (personalizacionDelegate != null) {
                    personalizacionDelegate.hideFloatingToolbar();
                }

                // Refresh Personalization UI if switching to Tab 2 (Logos)
                if (idx == 1 && personalizacionDelegate != null) {
                    personalizacionDelegate.refreshContent();
                    if (prendaDelegate != null) {
                        personalizacionDelegate.updateEscudoSelection(prendaDelegate.getTipoEscudo());
                    }
                }

                // Show/Hide Print and Export Buttons if in Ficha Tecnica
                if (newTab == getFichaTecnicaManager().tabFicha) {
                    if (comboModoDiseno != null) comboModoDiseno.setDisable(true);
                    btnFinalizar.setText("IMPRIMIR / PDF");
                    btnFinalizar.getStyleClass().setAll("button", "button-tab-ficha");
                    btnFinalizar.setOnAction(ev -> exportarPDF());
                } else {
                    if (comboModoDiseno != null) comboModoDiseno.setDisable(false);
                    btnFinalizar.setOnAction(ev -> handleMainAction());
                    wizardFlowManager.actualizarBotonAccion();
                }
            });

            // Init state
            wizardFlowManager.actualizarBotonAccion();
        }

        // Pantalla previa: Nuevo Pedido / Abrir Proyecto
        mostrarPantallaInicio();

        // Initialize Ficha
        setupFichaTecnicaTab();
        setupResponsiveMainTabs();

        // Listeners para detectar cambios (para regeneración de ficha y Dirty Check)
        txtCliente.textProperty().addListener(o -> { fichaDirty = true; markProjectDirty(); });
        if (comboPrioridad != null) comboPrioridad.valueProperty().addListener(o -> { 
            fichaDirty = true; markProjectDirty(); calcularFechaEntrega(); 
        });
        if (comboVendedor != null) comboVendedor.valueProperty().addListener(o -> { fichaDirty = true; markProjectDirty(); });

        // --- MANUAL RESIZER SETUP (Safe Initialization) ---
        if (rootVBox.getScene() != null) {
            setupManualResizer();
        } else {
            rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupManualResizer();
                }
            });
        }
    }

    private void setupManualResizer() {
        javafx.scene.Scene scene = rootVBox.getScene();
        if (scene == null)
            return;

        if (scene.getWindow() == null) {
            scene.windowProperty().addListener((obs, oldWin, newWin) -> {
                if (newWin != null) {
                    javafx.application.Platform.runLater(this::setupManualResizer);
                }
            });
            return;
        }

        Stage stage = (Stage) scene.getWindow();

        // Setup window resizing
        ensureWindowControlManager();
        windowControlManager.setupWindowResizing(scene, stage, rootVBox);
    }

    private void setupResponsiveSidebars() {
        if (rootVBox == null) {
            return;
        }

        if (rootVBox.getScene() != null) {
            bindResponsiveSidebarSizing(rootVBox.getScene());
        }

        rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                bindResponsiveSidebarSizing(newScene);
            }
        });
    }

    private void bindResponsiveSidebarSizing(Scene scene) {
        if (scene == null) {
            return;
        }

        updateSidebarWidths(scene.getWidth());

        if (!sidebarWidthListenerAttached) {
            sidebarWidthListenerAttached = true;
            scene.widthProperty().addListener((obs, oldWidth, newWidth) -> updateSidebarWidths(newWidth.doubleValue()));
        }
    }

    private void updateSidebarWidths(double sceneWidth) {
        double targetWidth = SIDEBAR_MIN_WIDTH;

        if (sceneWidth > 0) {
            double proportionalWidth = sceneWidth < 1200 ? sceneWidth * 0.34 : sceneWidth * 0.32;
            double cappedWidth = sceneWidth < 1200 ? Math.min(390.0, proportionalWidth) : proportionalWidth;
            targetWidth = Math.max(SIDEBAR_MIN_WIDTH, Math.min(SIDEBAR_MAX_WIDTH, cappedWidth));
        }

        applySidebarWidth(configSideWrapper, targetWidth);
        applySidebarWidth(logosSideWrapper, targetWidth);
    }

    private void applySidebarWidth(VBox sidebar, double width) {
        if (sidebar == null) {
            return;
        }

        sidebar.setMinWidth(width);
        sidebar.setPrefWidth(width);
        sidebar.setMaxWidth(width);
    }

    private void setupResponsiveMainTabs() {
        if (mainTabPane == null) {
            return;
        }

        if (!mainTabPane.getStyleClass().contains("main-workflow-tabs")) {
            mainTabPane.getStyleClass().add("main-workflow-tabs");
        }

        mainTabPane.widthProperty().addListener((obs, oldWidth, newWidth) -> refreshResponsiveMainTabs(newWidth.doubleValue()));
        javafx.application.Platform.runLater(() -> refreshResponsiveMainTabs(mainTabPane.getWidth()));
    }

    private void refreshResponsiveMainTabs(double width) {
        if (mainTabPane == null || mainTabPane.getTabs().size() < 3) {
            return;
        }

        double effectiveWidth = width > 0 ? width : (rootVBox != null ? rootVBox.getWidth() : 0);
        boolean compact = effectiveWidth > 0 && effectiveWidth < WORKFLOW_TABS_COMPACT_WIDTH;
        boolean medium = effectiveWidth >= WORKFLOW_TABS_COMPACT_WIDTH && effectiveWidth < WORKFLOW_TABS_MEDIUM_WIDTH;

        if (compact) {
            if (!mainTabPane.getStyleClass().contains("compact-tabs")) {
                mainTabPane.getStyleClass().add("compact-tabs");
            }
        } else {
            mainTabPane.getStyleClass().remove("compact-tabs");
        }

        updateWorkflowTab(mainTabPane.getTabs().get(0),
                compact ? "1. Diseño" : medium ? "1. Diseño Técnico" : "1. Diseño y Técnica",
                "1. Diseño y Técnica");
        updateWorkflowTab(mainTabPane.getTabs().get(1),
                compact ? "2. Logos" : medium ? "2. Logos y Sponsors" : "2. Logos / Sponsors",
                "2. Logos / Sponsors");
        updateWorkflowTab(mainTabPane.getTabs().get(2),
                compact ? "3. Jugadores" : medium ? "3. Lista Jugadores" : "3. Lista de Jugadores",
                "3. Lista de Jugadores");

        Tab fichaTab = (fichaTecnicaManager != null) ? fichaTecnicaManager.tabFicha : null;
        if (fichaTab != null) {
            updateWorkflowTab(fichaTab,
                    compact ? "4. Ficha" : medium ? "4. Ficha Técnica" : "4. Ficha Técnica",
                    "4. Ficha Técnica");
        }
    }

    private void updateWorkflowTab(Tab tab, String text, String tooltipText) {
        if (tab == null) {
            return;
        }

        tab.setText(text);
        tab.setTooltip(new Tooltip(tooltipText));
    }

    private void setupOverlayPanelToggles() {
        configureOverlayPanelToggle(btnToggleDesignPanel, true);
        configureOverlayPanelToggle(btnToggleLogosPanel, true);
    }

    private void configureOverlayPanelToggle(ToggleButton button, boolean reserveUpperRightSpace) {
        if (button == null) {
            return;
        }

        button.setMinSize(42, 42);
        button.setPrefSize(42, 42);
        button.setMaxSize(42, 42);

        updateOverlayPanelToggleVisual(button, button.isSelected());
        updateOverlayPanelTogglePosition(button, button.isSelected(), reserveUpperRightSpace);

        button.selectedProperty().addListener((obs, oldSelected, selected) -> {
            updateOverlayPanelToggleVisual(button, selected);
            updateOverlayPanelTogglePosition(button, selected, reserveUpperRightSpace);
        });
    }

    private void updateOverlayPanelToggleVisual(ToggleButton button, boolean expanded) {
        button.setGraphic(UIFactory.crearIcono("mdi2m-menu", 20, expanded ? "#334155" : "white"));
        button.setStyle(expanded
                ? "-fx-background-color: rgba(255,255,255,0.96); -fx-background-radius: 12; -fx-border-color: #cbd5e1; -fx-border-radius: 12; -fx-cursor: hand; -fx-padding: 9; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.10), 8, 0, 0, 2);"
                : "-fx-background-color: rgba(11,33,62,0.92); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 12; -fx-cursor: hand; -fx-padding: 9; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.16), 10, 0, 0, 3);");
    }

    private void updateOverlayPanelTogglePosition(ToggleButton button, boolean expanded, boolean reserveUpperRightSpace) {
        if (expanded) {
            StackPane.setAlignment(button, Pos.TOP_LEFT);
            StackPane.setMargin(button, new Insets(12, 0, 0, 12));
            return;
        }

        StackPane.setAlignment(button, Pos.TOP_RIGHT);
        StackPane.setMargin(button, reserveUpperRightSpace
                ? new Insets(74, 18, 0, 0)
                : new Insets(18, 18, 0, 0));
    }

    // ... (Existing fields) ...



    private FichaTecnicaManager fichaTecnicaManager;

    public FichaTecnicaManager getFichaTecnicaManager() {
        if (fichaTecnicaManager == null) {
            fichaTecnicaManager = new FichaTecnicaManager(this);
        }
        return fichaTecnicaManager;
    }

    private void setupFichaTecnicaTab() {
        getFichaTecnicaManager().setupFichaTecnicaTab();
    }

    @FXML
    private void exportarPDF() {
        getFichaTecnicaManager().exportarPDF();
    }

    private void generarFichaTecnica() {
        getFichaTecnicaManager().generarFichaTecnica();
    }
    private void resetConfiguracion() {
        if (prendaDelegate != null) {
            prendaDelegate.reset();
            fichaDirty = true;
        }
    }

    private void actualizarBotonAccion() {
        if (wizardFlowManager != null) wizardFlowManager.actualizarBotonAccion();
    }

    private void handleMainAction() {
        if (wizardFlowManager == null) return;
        boolean stepped = wizardFlowManager.handleMainAction();
        if (!stepped && mainTabPane.getSelectionModel().getSelectedIndex() == mainTabPane.getTabs().size() - 1) {
            guardarPedido();
        }
    }

    void iniciarWizard() {
        wizardStarted = true;
        fichaDirty = true;

        editandoDisenoArquero = false;
        if (prendaVisualizer != null) {
            prendaVisualizer.setArqueroDisenoPersonalizado(false);
            disenoCampoConfig = org.example.service.save.StateMapper.extractGarmentConfig(prendaVisualizer);
            disenoCampoLayers = org.example.service.save.StateMapper.extractUserLayers(prendaVisualizer);
        } else {
            disenoCampoConfig = null;
            disenoCampoLayers = new java.util.ArrayList<>();
        }
        
        disenoArqueroConfig = null;
        disenoArqueroLayers = new java.util.ArrayList<>();
        
        if (comboModoDiseno != null) {
            switchingDesignMode = true;
            try {
                comboModoDiseno.setDisable(false);
                comboModoDiseno.setValue("Jugador");
            } finally {
                switchingDesignMode = false;
            }
        }

        if (prendaDelegate != null) {
            prendaDelegate.iniciarWizard();
        }

        // Enable all tabs when technical work starts
        if (mainTabPane != null) {
            for (Tab t : mainTabPane.getTabs()) {
                t.setDisable(false);
            }
        }
    }

    void guardarDisenoActualEnSlot() {
        if (prendaVisualizer == null)
            return;
        fichaDirty = true;
        
        // Sync Visualizer state to DTOs for persistent saving
        org.example.dto.save.PrendaStateDTO cfg = org.example.service.save.StateMapper.extractGarmentConfig(prendaVisualizer);
        java.util.List<org.example.dto.save.LayerDTO> layers = org.example.service.save.StateMapper.extractUserLayers(prendaVisualizer);
        
        if (editandoDisenoArquero) {
            disenoArqueroConfig = cfg;
            disenoArqueroLayers = layers;
        } else {
            disenoCampoConfig = cfg;
            disenoCampoLayers = layers;
        }
    }
    void restaurarDisenoSilencioso(boolean forArquero, org.example.dto.save.PrendaStateDTO cfg,
            java.util.List<org.example.dto.save.LayerDTO> layers) {
        if (prendaVisualizer == null || cfg == null)
            return;
        
        isRestoringDesign = true;
        try {
            prendaVisualizer.setActiveDesign(forArquero); // Switch slot
            org.example.service.save.StateMapper.restoreDesign(prendaVisualizer, cfg, layers);
            editandoDisenoArquero = forArquero; 
        } finally {
            isRestoringDesign = false;
        }
    }

    org.example.dto.save.ProjectState extraerEstadoParaGuardado(File fileContext) {
        if (prendaVisualizer == null) {
            return new org.example.dto.save.ProjectState();
        }

        boolean wasArquero = editandoDisenoArquero;
        
        // MUTE NOTIFICATIONS to prevent Ficha re-generation or other listeners during extraction
        prendaVisualizer.setNotificationsSuspended(true);
        try {
            // 1. Capturar el diseño actual en su slot correspondiente
            guardarDisenoActualEnSlot();

            // 2. Asegurar que tenemos AMBOS diseños capturados en los slots de memoria
            if (disenoCampoConfig == null) {
                disenoCampoConfig = org.example.service.save.StateMapper.extractGarmentConfig(prendaVisualizer, 
                        prendaVisualizer.getCamisetaState(), prendaVisualizer.getColorManager());
                disenoCampoLayers = org.example.service.save.StateMapper.extractUserLayers(prendaVisualizer.getCamisetaState());
            }

            // 3. Extraer el estado completo del proyecto
            // El visualizer DEBE estar en modo Camiseta para que extractState capture 
            // el diseño principal correctamente.
            if (wasArquero) {
                prendaVisualizer.setActiveDesign(false); 
                editandoDisenoArquero = false;
            }

            org.example.dto.save.ProjectState state = org.example.service.save.StateMapper
                    .extractState(prendaVisualizer, listaJugadores, datosEnvio);

            // 4. Adjuntar datos del Arquero al DTO
            if (disenoArqueroConfig != null) {
                state.setArqueroGarmentConfig(disenoArqueroConfig);
            }
            if (disenoArqueroLayers != null && !disenoArqueroLayers.isEmpty()) {
                state.setArqueroLayers(disenoArqueroLayers);
            }

            // 5. Restaurar el modo visual original
            if (wasArquero) {
                editandoDisenoArquero = true;
                restaurarDisenoSilencioso(true, disenoArqueroConfig, disenoArqueroLayers);
                if (prendaDelegate != null && disenoArqueroConfig != null) {
                    prendaDelegate.restoreFromState(disenoArqueroConfig);
                }
            }
            
            return state;
            
        } finally {
            prendaVisualizer.setNotificationsSuspended(false);
        }
    }

    private void updateArqueroOptionStatus() {
        goalkeeperCoordinator.updateOptionStatus();
    }

    private void addListenersToJugador(org.example.model.DetallePedido p) {
        goalkeeperCoordinator.attachPlayerListeners(p);
    }

    /**
     * Sincroniza la configuración del diseño del Arquero (Variable B)
     * basándose en la selección del primer arquero en la lista.
     */
    /**
     * Sincroniza la configuración del diseño del Arquero (Variable B)
     * basándose en la selección del primer arquero en la lista.
     * Si el diseño aún no ha sido marcado como "Personalizado" por el usuario,
     * hereda todas las capas (sponsors, parches, etc) del Jugador de campo.
     */
    private void syncArqueroDesignFromPlayer(org.example.model.DetallePedido p) {
        goalkeeperCoordinator.syncDesignFromPlayer(p);
    }

    private void cambiarModoDiseno(boolean toArquero) {
        goalkeeperCoordinator.changeDesignMode(toArquero);
    }

    private void applyGoalieSpecificsFromTable() {
        goalkeeperCoordinator.applySpecificsFromTable();
    }

    void applyGoalieSpecificsFromStoredDesign() {
        goalkeeperCoordinator.applySpecificsFromStoredDesign();
    }

    private org.example.model.DetallePedido getArqueroPrincipal() {
        return goalkeeperCoordinator.getPrimaryGoalkeeper();
    }

    void recomputeNextArqueroMarkOrder() {
        goalkeeperCoordinator.recomputeNextMarkOrder();
    }

    private void syncArqueroPreviewFromRosterUpdate(org.example.model.DetallePedido p) {
        goalkeeperCoordinator.syncLiveStateFromRosterUpdate(p);
    }

    private void applySocksCategoryToAll(org.example.model.TipoMedias category) {
        if (listaJugadores == null) return;
        for (DetallePedido p : listaJugadores) {
            if (p.isIncludeSocks()) {
                if (category == null) {
                    // Re-trigger auto-calculation based on Talla
                    p.setTalla(p.getTalla()); 
                } else {
                    p.setTipoMedias(category.getLabel().toUpperCase());
                }
            }
        }
        if (tablaDetalles != null) {
            tablaDetalles.refresh();
        }
        // Force refresh highlights since we just changed everything
        syncSocksHighlightsWithRoster();
    }

    /**
     * Updates the design configurator to highlight all socks categories currently
     * present in the roster (including auto-resolved ones).
     */
    private void syncSocksHighlightsWithRoster() {
        if (prendaDelegate == null || listaJugadores == null) return;
        
        java.util.Set<org.example.model.TipoMedias> active = new java.util.HashSet<>();
        for (DetallePedido p : listaJugadores) {
            if (!p.isIncludeSocks()) continue;
            
            String effective = resolveEffectiveSocksCategory(p);
            for (org.example.model.TipoMedias tm : org.example.model.TipoMedias.values()) {
                if (tm.getLabel().equalsIgnoreCase(effective)) {
                    active.add(tm);
                }
            }
        }
        prendaDelegate.updateSocksHighlights(active);
    }

    private String resolveEffectiveSocksCategory(DetallePedido p) {
        String base = p.getTipoMedias();
        if (base != null && !base.isBlank() && !base.equalsIgnoreCase("PROFESIONAL")) {
            return base.toUpperCase();
        }
        String t = p.getTalla() != null ? p.getTalla().toUpperCase().trim() : "";
        if (t.matches("S|M|L|XL|XXL|3XXL|4XXL|G|XG|2XL|3XL|4XL")) return "ADULTO";
        if (t.matches("12|14|16")) return "JUVENIL";
        if (t.matches("4|6|8")) return "NIÑOS";
        return "PROFESIONAL";
    }
    
    private void mostrarPantallaInicio() {
        wizardStarted = true;
        editandoDisenoArquero = false;
        if (comboModoDiseno != null) {
            switchingDesignMode = true;
            try {
                comboModoDiseno.setValue("Jugador");
                comboModoDiseno.setDisable(true);
            } finally {
                switchingDesignMode = false;
            }
        }

        actualizarArchivoActualUI();
        actualizarDatosEnvioUI();

        if (dynamicFormContainer == null)
            return;

        dynamicFormContainer.getChildren().clear();
        dynamicFormContainer.setAlignment(Pos.TOP_CENTER);
        dynamicFormContainer.setSpacing(14);
        VBox.setMargin(dynamicFormContainer, new Insets(0));

        Label title = new Label("INICIAR PEDIDO");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0B213E;");

        Label hint = new Label("Elige una opción antes de seleccionar la prenda.");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        hint.setWrapText(true);
        hint.setMaxWidth(320);

        Button btnNuevo = new Button("Nuevo Pedido");
        btnNuevo.setMaxWidth(Double.MAX_VALUE);
        btnNuevo.setPrefHeight(42);
        btnNuevo.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");

        Button btnAbrir = new Button("Abrir Proyecto");
        btnAbrir.setMaxWidth(Double.MAX_VALUE);
        btnAbrir.setPrefHeight(42);
        btnAbrir.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8;");

        VBox box = new VBox(10, title, hint, btnNuevo, btnAbrir);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(14));
        box.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10;");

        btnNuevo.setOnAction(e -> comenzarNuevoPedidoEnEstaVentana());
        btnAbrir.setOnAction(e -> {
            File file = UIFactory.seleccionarArchivoAbrir(
                    (mainTabPane != null && mainTabPane.getScene() != null) ? mainTabPane.getScene().getWindow() : null,
                    "Abrir Proyecto",
                    "*.tlp;*.plt");
            if (file != null) {
                comenzarDesdeArchivoEnEstaVentana(file);
            }
        });

        dynamicFormContainer.getChildren().add(box);

        if (lblPlaceholder != null) {
            lblPlaceholder.setText("Seleccione \"Nuevo Pedido\" o \"Abrir Proyecto\" para comenzar.");
            lblPlaceholder.setVisible(true);
        }
        if (prendaVisualizer != null) {
            prendaVisualizer.setVisible(false);
        }
    }

    private void comenzarNuevoPedidoEnEstaVentana() {
        if (!canClose()) {
            return;
        }
        currentProjectFile = null;
        datosEnvio = new org.example.dto.DatosEnvioDTO();
        aplicarDatosEnvioToHiddenFields();
        actualizarDatosEnvioUI();
        actualizarTituloVentana();
        iniciarWizard();
        projectDirty = false;
    }

    private void comenzarDesdeArchivoEnEstaVentana(File file) {
        iniciarWizard();
        getProjectSaveManager().cargarProyectoDesdeArchivo(file);
    }

    // Logic moved to PrendaDelegate

    // --- REPLACED BY UIFactory ---

    // ----------------------

    private void cargarVendedores() {
        List<String> vendedores = orderService.getSellers();
        if (comboVendedor != null) {
            comboVendedor.getItems().clear();
            if (vendedores.isEmpty()) {
                comboVendedor.getItems().add("Sin vendedores en BD");
            } else {
                comboVendedor.getItems().addAll(vendedores);
            }
        }
    }

    private void generarProximoCodigo() {
        if (lblCodigoPedido != null)
            lblCodigoPedido.setText(orderService.generateNextOrderCode());
    }

    void actualizarDatosEnvioUI() {
        if (btnDatosEnvio == null)
            return;

        fichaDirty = true;

        boolean completo = (datosEnvio != null && datosEnvio.isComplete());
        String resumen = (datosEnvio != null) ? datosEnvio.getResumenCorto() : null;
        
        if (resumen == null || resumen.trim().isEmpty()) {
            btnDatosEnvio.setText("Completar datos");
        } else {
            btnDatosEnvio.setText(resumen);
        }
        
        btnDatosEnvio.setStyle("-fx-border-color: " + (completo ? "#2ecc71;" : "rgba(255,255,255,0.30);") +
                           " -fx-font-size: 11px;");
    }

    void aplicarDatosEnvioToHiddenFields() {
        if (datosEnvio == null)
            return;

        String nombreCompleto = datosEnvio.getNombreCompleto();
        if (txtCliente != null) {
            txtCliente.setText(nombreCompleto != null ? nombreCompleto : "");
        }
        if (comboVendedor != null) {
            String v = datosEnvio.getVendedorAtiende();
            if (v != null && !v.trim().isEmpty()) {
                comboVendedor.setValue(v);
            }
        }
    }

    private void mostrarDialogoDatosEnvio() {
        java.util.List<String> sellers = comboVendedor != null
                ? new java.util.ArrayList<>(comboVendedor.getItems())
                : orderService.getSellers();

        javafx.stage.Window owner = null;
        if (btnDatosEnvio != null && btnDatosEnvio.getScene() != null) {
            owner = btnDatosEnvio.getScene().getWindow();
        } else if (mainTabPane != null && mainTabPane.getScene() != null) {
            owner = mainTabPane.getScene().getWindow();
        }

        PedidoShippingDialog.show(owner, datosEnvio, sellers).ifPresent(nuevo -> {
            datosEnvio = nuevo;
            aplicarDatosEnvioToHiddenFields();
            actualizarDatosEnvioUI();
        });
    }


    // --- FUNCIONES DE JUGADORES (DELEGATED) ---

    @FXML
    public void importarExcel() {
        if (jugadoresDelegate != null) {
            jugadoresDelegate.importarExcel((Stage) txtCliente.getScene().getWindow());
            fichaDirty = true;
        }
    }

    @FXML
    public void agregarJugador() {
        if (jugadoresDelegate != null) {
            jugadoresDelegate.agregarJugador();
            fichaDirty = true;
        }
    }

    // --- GUARDAR PEDIDO ---

    private void moverVisualizador(int tabIndex) {
        if (wizardFlowManager != null) {
            wizardFlowManager.moverVisualizador(tabIndex, personalizacionDelegate);
        }
    }

    private void calcularFechaEntrega() {
        if (comboPrioridad == null || comboPrioridad.getValue() == null)
            return;

        fechaEntregaCalculada = schedulerService.calculateDeliveryDate(comboPrioridad.getValue());

        if (lblFechaEntrega != null) {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("EEE dd/MM/yyyy",
                    java.util.Locale.forLanguageTag("es-ES"));
            lblFechaEntrega.setText(fechaEntregaCalculada.format(fmt).toUpperCase());
        }
    }

    @FXML
    void guardarPedido() {
        // 1. Validaciones previas básicas
        if (!validarDatosPedido()) {
            return;
        }

        if (listaJugadores.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Seguro que quieres guardar sin lista de jugadores?", ButtonType.YES, ButtonType.NO);
            UIFactory.estilizarDialogo(confirm);
            confirm.showAndWait();
            if (confirm.getResult() == ButtonType.NO)
                return;
        }

        // 2. Ejecutar guardado vía Service
        boolean exito = orderService.saveOrder(buildOrderRequest());

        if (exito) {
            UIFactory.mostrarAlerta(Alert.AlertType.INFORMATION, "¡Pedido Exitoso!",
                    "El pedido se guardó correctamente.");
            projectDirty = false; // Reset dirty flag
            // REMOVED: No limpiarTodo() ni generarProximoCodigo() por petición del usuario
            // para mantener el estado tras guardar.
        } else {
            UIFactory.mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "Hubo un problema al guardar el pedido. Verifique los datos o la conexión.");
        }
    }

    private boolean validarDatosPedido() {
        if (datosEnvio == null || !datosEnvio.isComplete()) {
            UIFactory.mostrarAlerta(Alert.AlertType.WARNING, "Faltan Datos de Envío",
                    "Por favor complete todos los datos obligatorios (*) en Datos de Envío.");
            mostrarDialogoDatosEnvio();
            return false;
        }

        if (prendaDelegate.getSeleccionTipoPrenda() == null) {
            UIFactory.mostrarAlerta(Alert.AlertType.WARNING, "Tipo de Prenda",
                    "Debe seleccionar el Tipo de Prenda en el Wizard antes de finalizar.");
            return false;
        }
        return true;
    }

    private org.example.dto.OrderRequestDTO buildOrderRequest() {
        String mangaCombinada = prendaDelegate.getSeleccionCorte() + " - " + prendaDelegate.getSeleccionLargo() +
                (prendaDelegate.isLlevaPunoCamiseta() ? " - PUNO" : "");

        String clienteNombre = (datosEnvio != null && datosEnvio.getNombreCompleto() != null)
                ? datosEnvio.getNombreCompleto()
                : (txtCliente != null ? txtCliente.getText() : null);
        String vendedorNombre = (datosEnvio != null && datosEnvio.getVendedorAtiende() != null)
                ? datosEnvio.getVendedorAtiende()
                : (comboVendedor != null ? comboVendedor.getValue() : null);

        return new org.example.dto.OrderRequestDTO.Builder()
                .clientName(clienteNombre)
                .sellerName(vendedorNombre)
                .orderCode(lblCodigoPedido.getText())
                .tipoPrenda(prendaDelegate.getSeleccionTipoPrenda())
                .gender(prendaDelegate.getSeleccionGenero() != null ? prendaDelegate.getSeleccionGenero().name() : null)
                .sleeveInfo(mangaCombinada)
                .neckInfo(
                        prendaDelegate.getSeleccionCuello() != null ? prendaDelegate.getSeleccionCuello().name() : null)
                .hasMesh(prendaDelegate.isLlevaMalla())
                .hasSocks(prendaDelegate.isLlevaMedias())
                .hasShirtCuffs(prendaDelegate.isLlevaPunoCamiseta())
                .hasShortCuffs(prendaDelegate.isLlevaPunoShort())
                .deliveryDate(fechaEntregaCalculada)
                .priority(
                        (comboPrioridad != null && comboPrioridad.getValue() != null) ? comboPrioridad.getValue().name()
                                : "NORMAL")
                .roster(listaJugadores)
                .build();
    }

    private void limpiarTodo() {
        limpiarDatos();

        // Reiniciar a pantalla previa (Nuevo / Abrir)
        currentProjectFile = null;
        datosEnvio = new org.example.dto.DatosEnvioDTO();
        mostrarPantallaInicio();
        if (prendaVisualizer != null) {
            prendaVisualizer.resetState();
            prendaVisualizer.clearGlobalSelection(); // Ensure selection is gone
        }
        if (prendaDelegate != null) {
            prendaDelegate.actualizarVector();
        }

        // Navigate to Home (First Tab)
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(0);
            // Disable other tabs to force fresh flow
            for (int i = 1; i < mainTabPane.getTabs().size(); i++) {
                mainTabPane.getTabs().get(i).setDisable(true);
            }
        }
    }

    /**
     * Clears data fields and lists, but maintains current wizard state.
     */
    private void limpiarDatos() {
        txtCliente.clear();
        fichaDirty = true;

        if (jugadoresDelegate != null) {
            jugadoresDelegate.limpiarTodo();
        } else {
            listaJugadores.clear();
        }

        if (personalizacionDelegate != null) {
            personalizacionDelegate.resetState();
        }
    }

    // --- UI RELOCATION HELPER (REMOVED) ---

    // --- MENUBAR ACTIONS ---
    // Methods relocated to SAVE / LOAD HELPERS section

    @FXML
    public void cargarProyectoAction(ActionEvent event) {
        cargarProyecto();
        if (prendaVisualizer != null) {
            prendaVisualizer.getHistoryManager().clearHistory();
        }
    }

    @FXML
    public void nuevoPedidoNuevaVentanaAction(ActionEvent event) {
        abrirNuevaVentana(null);
    }

    @FXML
    public void cargarProyectoNuevaVentanaAction(ActionEvent event) {
        File file = UIFactory.seleccionarArchivoAbrir(
                mainTabPane.getScene().getWindow(),
                "Abrir Proyecto (Nueva ventana)",
                "*.tlp;*.plt");
        if (file != null) {
            abrirNuevaVentana(file);
        }
    }

    @FXML
    public void datosEnvioAction(ActionEvent event) {
        mostrarDialogoDatosEnvio();
    }

    @FXML
    public void salirAction(ActionEvent event) {
        confirmarSalida();
    }

    public boolean confirmarSalida() {
        boolean hasUnsavedChanges = prendaVisualizer != null
                && prendaVisualizer.getHistoryManager().getUndoHistorySize() > 0;

        return PedidoCloseConfirmationHelper.confirmExit(hasUnsavedChanges, this::guardarProyecto);
    }

    /**
     * Confirma el cierre de esta ventana. No debe cerrar toda la aplicación.
     * Se usa desde el evento {@link javafx.stage.WindowEvent#WINDOW_CLOSE_REQUEST}.
     */
    public boolean confirmarCierreVentana() {
        return PedidoCloseConfirmationHelper.confirmWindowClose(projectDirty, this::guardarProyecto);
    }

    @FXML
    public void acercaDeAction(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de Palant");
        alert.setHeaderText("Palant S.A.C.");
        alert.setContentText("Empresa de ropa deportiva.\nSistema de Pedidos v2.8.0");

        // Add Application Icon to Dialog Stage
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/vectors/logo_small.png")));
        } catch (Exception e) {
        }

        // Style the "Aceptar" button to be Green
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getScene().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        Button openButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (openButton != null) {
            openButton.setStyle(
                    "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            openButton.setOnMouseEntered(e -> openButton.setStyle(
                    "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;"));
            openButton.setOnMouseExited(e -> openButton.setStyle(
                    "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;"));
        }

        alert.showAndWait();
    }

    // --- SAVE / LOAD HELPERS ---

    private ProjectSaveManager projectSaveManager;

    private ProjectSaveManager getProjectSaveManager() {
        if (projectSaveManager == null) {
            projectSaveManager = new ProjectSaveManager(this);
        }
        return projectSaveManager;
    }

    @FXML
    public void guardarProyectoAction(ActionEvent event) {
        getProjectSaveManager().guardarProyecto();
    }

    @FXML
    public void guardarProyectoComoAction(ActionEvent event) {
        getProjectSaveManager().guardarProyectoComo();
    }

    private boolean guardarProyecto() {
        return getProjectSaveManager().guardarProyecto();
    }

    private boolean guardarProyectoComo() {
        return getProjectSaveManager().guardarProyectoComo();
    }

    private void cargarProyecto() {
        getProjectSaveManager().cargarProyecto();
    }

    public boolean cargarProyectoDesdeArchivo(File file) {
        return getProjectSaveManager().cargarProyectoDesdeArchivo(file);
    }

    /**
     * Refresh jugadores table from source to get latest values from DB/file
     * This is called when switching to Arquero design mode to ensure latest manga/color values
     */
    private void refreshJugadoresTableFromSource() {
        try {
            if (jugadoresDelegate != null) {
                // Delegate should reload data from source
                jugadoresDelegate.cargarListaJugadores();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not refresh jugadores table: " + e.getMessage());
            // Continue anyway, use in-memory data
        }
    }

    void actualizarTituloVentana() {
        try {
            if (rootVBox == null || rootVBox.getScene() == null)
                return;
            Stage stage = (Stage) rootVBox.getScene().getWindow();
            if (stage == null)
                return;

            String codigo = (lblCodigoPedido != null) ? lblCodigoPedido.getText() : null;
            StringBuilder title = new StringBuilder("Palant");
            if (codigo != null && !codigo.trim().isEmpty()) {
                title.append(" - Pedido ").append(codigo.trim());
            } else {
                title.append(" - Pedido");
            }
            if (currentProjectFile != null) {
                title.append(" - ").append(currentProjectFile.getName());
            }
            if (projectDirty) {
                title.append(" *");
            }
            stage.setTitle(title.toString());

            actualizarArchivoActualUI();
        } catch (Exception e) {
        }
    }

    void markProjectDirty() {
        if (!projectDirty) {
            projectDirty = true;
            actualizarTituloVentana(); // Adicionar asterisco (*) si corresponde
        }
    }

    public boolean canClose() {
        return PedidoCloseConfirmationHelper.confirmProjectClose(projectDirty, this::guardarProyecto);
    }

    void actualizarArchivoActualUI() {
        String display = (currentProjectFile == null) ? "(sin guardar)" : currentProjectFile.getName();
        if (projectDirty) {
            display += " *";
        }
        String tooltip = (currentProjectFile == null) ? null : currentProjectFile.getAbsolutePath();

        if (lblArchivoTitleBar != null) {
            lblArchivoTitleBar.setText(display);
            lblArchivoTitleBar.setTooltip((tooltip != null) ? new Tooltip(tooltip) : null);
        }
        if (onTitleChanged != null) {
            onTitleChanged.accept(display);
        }
    }

    private static void abrirNuevaVentana(File proyectoParaCargar) {
        try {
            FXMLLoader loader = new FXMLLoader(PedidoController.class.getResource("/nuevo_pedido.fxml"));
            Parent root = loader.load();
            PedidoController controller = loader.getController();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));

            try {
                javafx.scene.image.Image logo = UIFactory.getAppLogo();
                if (logo != null) {
                    stage.getIcons().add(logo);
                }
            } catch (Exception e) {
            }

            stage.setResizable(true);
            stage.setWidth(1280);
            stage.setHeight(850);
            stage.centerOnScreen();
            stage.show();

            if (proyectoParaCargar != null) {
                javafx.application.Platform.runLater(() -> controller.cargarProyectoDesdeArchivo(proyectoParaCargar));
            }
        } catch (Exception e) {
            e.printStackTrace();
            UIFactory.mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir una nueva ventana: " + e.getMessage());
        }
    }
}



