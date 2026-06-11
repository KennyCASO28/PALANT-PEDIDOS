package org.example.controller;

import java.io.File;
import java.util.List;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import org.example.utils.UIFactory;
import org.example.dto.ConfiguracionPrendaDTO;
import org.example.service.PdfExportService;
import org.example.service.SvgExportService;

public class FichaTecnicaManager {
    
    private final PedidoController controller;
    
    public FichaTecnicaManager(PedidoController controller) {
        this.controller = controller;
    }


    public void exportarVector() {
        if (controller.prendaVisualizer == null)
            return;

        File file = UIFactory.seleccionarArchivoGuardar(controller.mainTabPane.getScene().getWindow(),
                "Exportar Vector (SVG)",
                "Vector_" + controller.lblCodigoPedido.getText() + ".svg", "*.svg");

        if (file != null) {
            SvgExportService svgService = new SvgExportService();
            svgService.exportarVector(buildExportData(file));
        }
    }

    private org.example.dto.ExportDataDTO buildExportData(File file) {
        String clienteNombre = (controller.datosEnvio != null && controller.datosEnvio.getNombreCompleto() != null)
                ? controller.datosEnvio.getNombreCompleto()
                : (controller.txtCliente != null ? controller.txtCliente.getText() : "");
        String vendedorNombre = (controller.datosEnvio != null && controller.datosEnvio.getVendedorAtiende() != null)
                ? controller.datosEnvio.getVendedorAtiende()
                : (controller.comboVendedor != null ? controller.comboVendedor.getValue() : "");

        // 1. Ensure slots are up to date
        refreshAndApplyStoredDesignsForSnapshot();

        // 2. Isolated Snapshots
        System.out.println("DEBUG: Taking isolated snapshots for PDF Export...");
        javafx.scene.image.Image mainSnapshot = (controller.prendaVisualizer != null) ? controller.prendaVisualizer.takeSafeSnapshot(false) : null;

        javafx.scene.image.Image arqSnapshotHombre = null;
        javafx.scene.image.Image arqSnapshotMujer = null;

        if (controller.prendaVisualizer != null) {
            boolean hasManArq = controller.listaJugadores.stream().anyMatch(p -> p.isEsArquero() && "HOMBRE".equalsIgnoreCase(p.getGenero()));
            boolean hasWomanArq = controller.listaJugadores.stream().anyMatch(p -> p.isEsArquero() && "MUJER".equalsIgnoreCase(p.getGenero()));

            if (hasManArq || hasWomanArq) {
                // Determine original state to restore
                org.example.model.TipoGenero originalGen = controller.prendaVisualizer.getArqueroState().getGenero();

                if (hasManArq) {
                    controller.prendaVisualizer.getArqueroState().setGenero(org.example.model.TipoGenero.HOMBRE);
                    arqSnapshotHombre = controller.prendaVisualizer.takeSafeSnapshot(true);
                }
                if (hasWomanArq) {
                    controller.prendaVisualizer.getArqueroState().setGenero(org.example.model.TipoGenero.MUJER);
                    arqSnapshotMujer = controller.prendaVisualizer.takeSafeSnapshot(true);
                }

                // Restore
                controller.prendaVisualizer.getArqueroState().setGenero(originalGen);
            }
        }

        // 3. Force Player Mode for Shield Collection (Design Independence)
        if (controller.prendaVisualizer != null) {
            controller.prendaVisualizer.setActiveDesign(false);
        }
        List<PdfExportService.ShieldEntry> shields = PdfExportService.collectShields(controller.prendaVisualizer);

        // 4. Restore User's original mode
        if (controller.prendaVisualizer != null) {
            controller.prendaVisualizer.setActiveDesign(controller.editandoDisenoArquero);
        }

        // 5. Build DTO
        ConfiguracionPrendaDTO arqueroConfig = (controller.disenoArqueroConfig != null) 
            ? org.example.service.save.StateMapper.toConfigDTO(controller.disenoArqueroConfig) 
            : null;

        return new org.example.dto.ExportDataDTO.Builder()
                .targetFile(file)
                .visualizer(controller.prendaVisualizer)
                .config(org.example.service.save.StateMapper.toConfigDTO(controller.disenoCampoConfig))
                .arqueroConfig(arqueroConfig)
                .arqueroSnapshotHombre(arqSnapshotHombre)
                .arqueroSnapshotMujer(arqSnapshotMujer)
                .mainGarmentSnapshot(mainSnapshot)
                .shields(shields)
                .orderCode(controller.lblCodigoPedido != null ? controller.lblCodigoPedido.getText() : "S/N")
                .clientName(clienteNombre)
                .sellerName(vendedorNombre)
                .shippingInfo(controller.datosEnvio)
                .roster(new java.util.ArrayList<>(controller.listaJugadores))
                .deliveryDate(controller.fechaEntregaCalculada != null ? controller.fechaEntregaCalculada : java.time.LocalDate.now())
                .priority((controller.comboPrioridad != null && controller.comboPrioridad.getValue() != null) ? controller.comboPrioridad.getValue().getLabel() : "NORMAL")
                .shortType((controller.disenoCampoConfig != null && controller.disenoCampoConfig.getCurrentCorteShort() != null) ? controller.disenoCampoConfig.getCurrentCorteShort().name() : "N/A")
                .referenceImages(controller.personalizacionDelegate != null ? controller.personalizacionDelegate.getHotspotImages() : new java.util.ArrayList<>())
                .build();
    }


    public Tab tabFicha;
    public ScrollPane scrollFicha;
    public javafx.scene.Group zoomGroup; // Wrapper for scaling
    public javafx.scene.transform.Scale zoomScale = new javafx.scene.transform.Scale(1, 1, 0, 0); // Pivot Top-Left
    public double currentZoom = 1.0;
    public Label lblZoomLevel;

    public void setupFichaTecnicaTab() {
        if (controller.mainTabPane != null) {
            tabFicha = new Tab("4. Ficha Técnica");
            tabFicha.setClosable(false);

            // 1. Content Wrapper (Group allows ScrollPane to detect scaled bounds)
            zoomGroup = new javafx.scene.Group();
            // Apply Top-Left Pivot Scaling
            zoomGroup.getTransforms().add(zoomScale); // Initialize with identity

            // Wrapper to CENTER the Group
            StackPane zoomCenterContainer = new StackPane(zoomGroup);
            zoomCenterContainer.setAlignment(Pos.CENTER);
            // Bind min size to ScrollPane viewport to ensure centering works when content <
            // viewport

            // 2. ScrollPane
            scrollFicha = new ScrollPane();
            scrollFicha.setContent(zoomCenterContainer);
            scrollFicha.setFitToWidth(true);
            scrollFicha.setFitToHeight(true); // Allow height to expand for centering
            scrollFicha.setPannable(true);
            scrollFicha.getStyleClass().add("scroll-ficha-preview");

            // --- USER REQUEST: Enable Ctrl + Mouse Wheel Zoom ---
            scrollFicha.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
                if (event.isControlDown()) {
                    double delta = event.getDeltaY();
                    if (delta > 0) {
                        applyZoom(0.05);
                    } else if (delta < 0) {
                        applyZoom(-0.05);
                    }
                    event.consume();
                }
            });

            // 3. Floating Toolbar
            HBox zoomTools = new HBox(10);
            zoomTools.setAlignment(Pos.CENTER);
            // Fix: Softer Shadow
            zoomTools.getStyleClass().add("zoom-toolbar");
            zoomTools.setMaxHeight(40);
            zoomTools.setMaxWidth(400);

            Button btnMinus = createZoomButton("mdi2m-magnify-minus-outline", e -> applyZoom(-0.1));
            Button btnPlus = createZoomButton("mdi2m-magnify-plus-outline", e -> applyZoom(0.1));

            // Action: Save and Vector Export
            Button btnSave = createZoomButton("mdi2c-content-save", e -> controller.guardarPedido());
            btnSave.setTooltip(new Tooltip("Guardar Pedido"));
            btnSave.getStyleClass().add("zoom-button");

            Button btnVector = createZoomButton("mdi2v-vector-curve", e -> exportarVector());
            btnVector.setTooltip(new Tooltip("Exportar Vector (SVG)"));
            btnVector.getStyleClass().add("zoom-button");

            lblZoomLevel = new Label("100%");
            lblZoomLevel.getStyleClass().add("zoom-label");
            lblZoomLevel.setMinWidth(40);
            lblZoomLevel.setAlignment(Pos.CENTER);

            zoomTools.getChildren().addAll(btnMinus, lblZoomLevel, btnPlus,
                    new Separator(javafx.geometry.Orientation.VERTICAL), btnSave, btnVector);

            // 4. Root StackPane
            StackPane rootFicha = new StackPane();
            rootFicha.getChildren().addAll(scrollFicha, zoomTools);
            StackPane.setAlignment(zoomTools, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(zoomTools, new Insets(0, 30, 30, 0));

            tabFicha.setContent(rootFicha);

            controller.mainTabPane.getTabs().add(tabFicha);
            tabFicha.setDisable(true);

            // Listener for Ficha Generation
            controller.mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == tabFicha && controller.fichaDirty) {
                    generarFichaTecnica();
                }
            });
        }
    }

    private Button createZoomButton(String iconCode, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button();
        try {
            btn.setGraphic(UIFactory.crearIcono(iconCode, 20, "#333"));
        } catch (Exception e) {
            btn.setText("?");
        }
        btn.getStyleClass().add("zoom-button");
        btn.setOnAction(action);
        return btn;
    }

    private void applyZoom(double delta) {
        double newZoom = currentZoom + delta;
        // Limits: 10% to 300%
        if (newZoom < 0.1)
            newZoom = 0.1;
        if (newZoom > 3.0)
            newZoom = 3.0;

        currentZoom = newZoom;
        updateZoom();
    }



    private void updateZoom() {
        // Update Roster Column Labels based on Config
        if (controller.prendaDelegate != null && controller.jugadoresDelegate != null) {
            ConfiguracionPrendaDTO cfg = controller.prendaDelegate.getConfiguracion();
            String label = "Prenda Inf.";
            if (cfg != null && cfg.getCorteShort() != null) {
                // Map internal names to display names if needed, or use as is
                // "Pantaloneta", "Licra", "Falda" are distinct.
                // Assuming tipoShort is the user facing string or enum.
                // If it's a path or code, we might need mapping.
                // Looking at PrendaDelegate, it seems to track the selected ToggleButton text.
                label = controller.prendaDelegate.getTipoShortSeleccionado();
                if (label == null || label.isEmpty())
                    label = "Prenda Inf.";
            }
            controller.jugadoresDelegate.updateColumnLabels(label);

            // NEW: SMART VISIBILITY
            boolean showTop = (cfg != null) && cfg.getTipoPrenda() != org.example.model.TipoPrenda.SHORT;
            boolean showBottom = (cfg != null) && cfg.llevaShort();
            boolean showSocks = (cfg != null) && cfg.llevaMedias();

            // Special case: Conjuncts usually imply Top + Short + Maybe Socks
            // If TipoPrenda is CONJUNTO, PrendaDelegate usually sets 'llevaShort' true via
            // logic
            // But let's verify. If configurator is ConjuntoConfigurator, it should return
            // DTO with 'short'=true.
            // If not, we force showBottom for Conjunto just in case.
            if (cfg != null && cfg.getTipoPrenda() == org.example.model.TipoPrenda.CONJUNTO) {
                showTop = true;
                showBottom = true;
            }
            // For Short, showTop is false.
            if (cfg != null && cfg.getTipoPrenda() == org.example.model.TipoPrenda.SHORT) {
                showTop = false; // "Solo Short"
                showBottom = true;
            }

            // User said: "NO SELECIONE MEDIAS PERO AUN ASI ME SALE"
            // So default behavior is correct with 'llevaMedias()'.

            controller.jugadoresDelegate.updateGranularColumnsVisibility(showTop, showBottom, showSocks);
        }

        if (zoomGroup != null && !zoomGroup.getTransforms().isEmpty()) {
            // Assume the first transform is our Scale(1,1,0,0)
            if (zoomGroup.getTransforms().get(0) instanceof javafx.scene.transform.Scale) {
                javafx.scene.transform.Scale scale = (javafx.scene.transform.Scale) zoomGroup.getTransforms().get(0);
                scale.setX(currentZoom);
                scale.setY(currentZoom);
            }
        }
        if (lblZoomLevel != null) {
            lblZoomLevel.setText(String.format("%.0f%%", currentZoom * 100));
        }
    }

    public void generarFichaTecnica() {
        if (controller.isGeneratingFicha || controller.prendaDelegate == null || zoomGroup == null) {
            return;
        }
        controller.isGeneratingFicha = true;

        System.out.println("DEBUG: [PERF] Iniciando Generación de Ficha Técnica OPTIMIZADA...");
        try {
            // Force reset zoom to 100% for clean capture/layout
            currentZoom = 1.0;
            if (zoomScale != null) {
                zoomScale.setX(1.0);
                zoomScale.setY(1.0);
            }
            if (lblZoomLevel != null) lblZoomLevel.setText("100%");
            
            boolean modoOriginalArquero = controller.editandoDisenoArquero;
            controller.prendaVisualizer.setVisible(false);

            try {
                // 1. SYNC: Ensure both design slots are up-to-date in memory
                refreshAndApplyStoredDesignsForSnapshot();

                // 2. PLAYER SNAPSHOT (Always needed for Page 1)
                System.out.println("DEBUG: [PERF] Capturando Camiseta Campo...");
                // If we are already in player mode, this is fast. If not, it swaps once.
                javafx.scene.image.Image mainGarmentSnapshot = controller.prendaVisualizer.takeSafeSnapshot(false);
                
                // 3. GOALIE SNAPSHOT (Dual-Gender Support + Sleeve Consistency)
                javafx.scene.image.Image arqSketchHombre = null;
                javafx.scene.image.Image arqSketchMujer = null;
                
                // We identify the first arquero of each gender in the roster to use as a model for the sketches
                org.example.model.DetallePedido firstManArq = controller.listaJugadores.stream()
                        .filter(p -> p.isEsArquero() && (p.getGenero() == null || p.getGenero().toUpperCase().contains("HO") || p.getGenero().toUpperCase().contains("VA")))
                        .findFirst().orElse(null);
                        
                org.example.model.DetallePedido firstWomanArq = controller.listaJugadores.stream()
                        .filter(p -> p.isEsArquero() && p.getGenero() != null && (p.getGenero().toUpperCase().contains("MU") || p.getGenero().toUpperCase().contains("FE") || p.getGenero().toUpperCase().contains("DA")))
                        .findFirst().orElse(null);

                // Fallback check: if we have arqueros but logic failed to categorize, use the first one available
                if (firstManArq == null && firstWomanArq == null) {
                    org.example.model.DetallePedido anyArq = controller.listaJugadores.stream()
                        .filter(org.example.model.DetallePedido::isEsArquero)
                        .findFirst().orElse(null);
                    if (anyArq != null) firstManArq = anyArq; 
                }

                if (firstManArq != null || firstWomanArq != null) {
                    System.out.println("DEBUG: [PERF] Capturando Arqueros Independientes...");
                    org.example.model.PrendaState qState = controller.prendaVisualizer.getArqueroState();
                    org.example.model.TipoGenero originalGen = qState.getGenero();
                    org.example.model.TipoLargo originalLargo = qState.getLargo();

                    if (firstManArq != null) {
                        qState.setGenero(org.example.model.TipoGenero.HOMBRE);
                        // Respect roster sleeve type for this specific gender's sketch
                        if ("LARGA".equalsIgnoreCase(firstManArq.getTipoMangaArquero())) {
                            qState.setLargo(org.example.model.TipoLargo.MANGA_LARGA);
                        } else {
                            qState.setLargo(org.example.model.TipoLargo.MANGA_CORTA);
                        }
                        arqSketchHombre = controller.prendaVisualizer.takeSafeSnapshot(true);
                    }
                    if (firstWomanArq != null) {
                        qState.setGenero(org.example.model.TipoGenero.MUJER);
                        // Respect roster sleeve type for this specific gender's sketch
                        if ("LARGA".equalsIgnoreCase(firstWomanArq.getTipoMangaArquero())) {
                            qState.setLargo(org.example.model.TipoLargo.MANGA_LARGA);
                        } else {
                            qState.setLargo(org.example.model.TipoLargo.MANGA_CORTA);
                        }
                        arqSketchMujer = controller.prendaVisualizer.takeSafeSnapshot(true);
                    }
                    // Restore original state
                    qState.setGenero(originalGen);
                    qState.setLargo(originalLargo);
                }

                // 4. DATA COLLECTION: COLLECT DATA while in a deterministic state (Player mode)
                if (controller.editandoDisenoArquero) {
                    controller.prendaVisualizer.setActiveDesign(false);
                }
                
                List<PdfExportService.ShieldEntry> shields = PdfExportService.collectShields(controller.prendaVisualizer);
                ConfiguracionPrendaDTO configReporte = org.example.service.save.StateMapper.toConfigDTO(controller.disenoCampoConfig);
                
                // 5. BUILD VIEW (Static layout construction)
                System.out.println("DEBUG: [PERF] Construyendo FichaTecnicaView...");
                java.time.LocalDate fechaLocalDate = (controller.fechaEntregaCalculada != null) ? controller.fechaEntregaCalculada : java.time.LocalDate.now();
                String cliente = (controller.datosEnvio != null && controller.datosEnvio.getNombreCompleto() != null) ? controller.datosEnvio.getNombreCompleto() : (controller.txtCliente != null ? controller.txtCliente.getText() : null);
                String codigo = (controller.lblCodigoPedido != null) ? controller.lblCodigoPedido.getText() : "S/N";
                String prioridad = controller.comboPrioridad != null && controller.comboPrioridad.getValue() != null ? controller.comboPrioridad.getValue().getLabel() : "Normal";
                String vendedor = (controller.datosEnvio != null && controller.datosEnvio.getVendedorAtiende() != null) ? controller.datosEnvio.getVendedorAtiende() : (controller.comboVendedor != null ? controller.comboVendedor.getValue() : null);
                ConfiguracionPrendaDTO arqueroConfig = (controller.disenoArqueroConfig != null) ? org.example.service.save.StateMapper.toConfigDTO(controller.disenoArqueroConfig) : null;

                final javafx.scene.layout.Region content = org.example.controller.uicomponent.FichaTecnicaView.build(
                    configReporte, 
                    mainGarmentSnapshot, 
                    controller.disenoCampoLayers, 
                    controller.prendaVisualizer, 
                    (java.util.List<org.example.model.DetallePedido>) controller.listaJugadores, 
                    cliente, 
                    codigo, 
                    (java.util.List<org.example.service.PdfExportService.ShieldEntry>) shields, 
                    fechaLocalDate, 
                    prioridad, 
                    (configReporte != null && configReporte.getCorteShort() != null) ? configReporte.getCorteShort().name() : "N/A", 
                    vendedor, 
                    (java.util.List<javafx.scene.image.Image>) (controller.personalizacionDelegate != null ? controller.personalizacionDelegate.getHotspotImages() : new java.util.ArrayList<javafx.scene.image.Image>()), 
                    controller.datosEnvio, 
                    arqSketchHombre, 
                    arqSketchMujer, 
                    arqueroConfig
                );

                // 6. RESTORE: Return to user's original editing mode
                if (modoOriginalArquero) {
                    controller.prendaVisualizer.setActiveDesign(true);
                }

                // Final UI Update
                javafx.application.Platform.runLater(() -> {
                    zoomGroup.getChildren().setAll(content);
                    content.applyCss();
                    content.layout();
                    
                    controller.fichaDirty = false;
                    controller.isGeneratingFicha = false;
                    System.out.println("DEBUG: [PERF] Ficha Técnica Generada.");
                });

            } finally {
                controller.prendaVisualizer.setVisible(true);
            }

        } catch (Exception e) {
            controller.isGeneratingFicha = false;
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                UIFactory.mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo generar la ficha: " + e.getMessage());
            });
        }
    }


    /**
     * Ensures both stored design slots (Jugador/Campo and Arquero) are up-to-date
     * AND applied into the live visualizer states before taking snapshots.
     *
     * Root cause fixed: we used to only persist the CURRENT active design into its
     * slot (via controller.guardarDisenoActualEnSlot). The other slot could remain stale unless
     * the user manually switched modes before generating the Ficha Técnica.
     */
    private void refreshAndApplyStoredDesignsForSnapshot() {
        if (controller.prendaVisualizer == null) return;

        // 1) Save current active UI work into its slot DTO
        controller.guardarDisenoActualEnSlot();

        // 2) Update arquero DTO from roster (short/socks/sleeve/reference color) 
        // derived from the active configuration
        controller.applyGoalieSpecificsFromStoredDesign();

        // [PERF] Removed redundant setActiveDesign(true/false) calls here.
        // The slots (controller.disenoCampoConfig/controller.disenoArqueroConfig) are already in sync 
        // with the Visualizer's internal states (camisetaState/arqueroState) 
        // because extractDesign/restoreDesign happen during manual swaps.
        // takeSafeSnapshot() will handle the necessary mode switches.
    }

    
    public void exportarPDF() {
        File file = UIFactory.seleccionarArchivoGuardar(controller.mainTabPane.getScene().getWindow(),
                "Exportar Ficha Técnica PDF",
                controller.lblCodigoPedido.getText() + ".pdf", "*.pdf");

        if (file != null) {
            try {
                PdfExportService pdfService = new PdfExportService();
                pdfService.exportarFicha(buildExportData(file));

                UIFactory.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "PDF Exportado correctamente.");

                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(file);
                    }
                } catch (Exception ex) {
                }
            } catch (Exception e) {
                e.printStackTrace();
                UIFactory.mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar el PDF: " + e.getMessage());
            }
        }
    }

}

