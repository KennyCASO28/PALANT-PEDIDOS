package org.example.controller.delegate;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Separator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import java.util.List;

import org.example.component.PrendaVisualizer;
import org.example.utils.UIFactory;

import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.model.PrendaState;
import org.example.dto.save.ReferenceHotspotDTO;
import org.example.service.save.StateMapper;
import org.example.controller.uicomponent.*;

/**
 * Delegate responsible for Handling Tab 2: Personalization (Logos, Texts).
 */
public class PersonalizacionDelegate {

    private final VBox container;
    private final PrendaVisualizer visualizer;

    // --- CONTROLLERS ---
    private final GarmentColorController colorController;
    private final BrandingController brandingController;
    private final NumberController numberController;
    private final TextManagerController textController;
    private final LogoManagerController logoController;
    private final ShapeManagerController shapeController;
    private final ReferenceManagerController referenceController;
    private javafx.scene.layout.Pane cachedBottomBar = null;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private org.example.controller.uicomponent.DocumentColorPalette colorPalette;


    public ShapeManagerController getShapeController() {
        return shapeController;
    }

    private final ToggleGroup zoneToggleGroup = new ToggleGroup();

    public PersonalizacionDelegate(VBox container, PrendaVisualizer visualizer) {
        this.container = container;
        this.visualizer = visualizer;

        // Initialize Controllers
        this.colorController = new GarmentColorController(visualizer,
                this::startEyedropper);
        this.brandingController = new BrandingController(visualizer,
                this::startEyedropper);
        this.numberController = new NumberController(visualizer,
                this::startEyedropper);
        this.textController = new TextManagerController(visualizer,
                this::startEyedropper);
        this.logoController = new LogoManagerController(visualizer);
        this.shapeController = new ShapeManagerController(visualizer);
        this.referenceController = new ReferenceManagerController(visualizer);
        this.colorPalette = new org.example.controller.uicomponent.DocumentColorPalette(visualizer, this.shapeController);

        // --- UI SYNC ---
        visualizer.addUIUpdateListener(colorController::updateUI);
        visualizer.addUIUpdateListener(brandingController::updateUI);
        visualizer.addUIUpdateListener(numberController::updateUI);
        
        visualizer.getPowerClipManager().editingZoneProperty().addListener((obs, old, val) -> {
            if (val == null) {
                zoneToggleGroup.selectToggle(null);
            }
        });
    }

    public void setupUI() {
        container.getChildren().clear();
        container.setFillWidth(true);

        Accordion accordion = new Accordion();

        // 1. COLORS PANE
        TitledPane paneColores = colorController.getPane();
        paneColores.expandedProperty().addListener((o, old, expanded) -> {
            if (expanded) colorController.updateUI();
        });

        // 2. BRANDING PANE
        TitledPane paneMarca = new TitledPane();
        paneMarca.setText("2. Marca y Escudos");
        paneMarca.setGraphic(UIFactory.crearIcono("mdi2t-tag-text-outline", 18, "#2c3e50"));
        paneMarca.getStyleClass().add("modern-pane");
        VBox brandingBox = new VBox(10);
        brandingBox.setPadding(new Insets(6));
        brandingBox.setFillWidth(true);
        brandingController.addToContainer(brandingBox);
        paneMarca.setContent(brandingBox);

        // 3. NUMBERS PANE
        TitledPane paneNumeros = new TitledPane();
        paneNumeros.setText("3. Dorsales y Números");
        paneNumeros.setGraphic(UIFactory.crearIcono("mdi2n-numeric", 18, "#2c3e50"));
        paneNumeros.getStyleClass().add("modern-pane");
        VBox numbersBox = new VBox(10);
        numbersBox.setPadding(new Insets(6));
        numbersBox.setFillWidth(true);
        numberController.addToContainer(numbersBox);
        paneNumeros.setContent(numbersBox);

        // 4. TEXTS PANE
        TitledPane paneTexto = new TitledPane();
        paneTexto.setText("4. Textos");
        paneTexto.setGraphic(UIFactory.crearIcono("mdi2f-format-letter-case", 18, "#2c3e50"));
        paneTexto.getStyleClass().add("modern-pane");
        paneTexto.setContent(textController.getContainer());

        // 5. REFERENCES PANE
        TitledPane paneReferencias = new TitledPane();
        paneReferencias.setText("5. Referencias");
        paneReferencias.setGraphic(UIFactory.crearIcono("mdi2i-image-multiple", 18, "#2c3e50"));
        paneReferencias.getStyleClass().add("modern-pane");
        paneReferencias.setContent(referenceController.getContainer());

        accordion.getPanes().addAll(paneColores, paneMarca, paneNumeros, paneTexto, paneReferencias);
        accordion.setExpandedPane(paneColores);
        accordion.setMinWidth(0);
        accordion.setMaxWidth(Double.MAX_VALUE);
        accordion.prefWidthProperty().bind(container.widthProperty());

        container.getChildren().add(accordion);
        VBox.setVgrow(accordion, javafx.scene.layout.Priority.ALWAYS);

        // Initial UI Update
        colorController.updateUI();
        visualizer.setOnStateChanged(this::refreshContent);

        // Auto-Expand logic
        visualizer.addSelectionListener(node -> {
            if (node instanceof org.example.component.TextLayer) {
                accordion.setExpandedPane(paneTexto);
                textController.focusInput();
            } else if (node instanceof org.example.component.ImageLayer) {
                org.example.component.ImageLayer il = (org.example.component.ImageLayer) node;
                if (il.getBadgeType() != null && il.getBadgeType() != org.example.model.TipoEscudo.NINGUNO) {
                    accordion.setExpandedPane(paneMarca);
                }
            }
        });

        // --- SMART SCROLL LOGIC ---
        accordion.expandedPaneProperty().addListener((obs, oldPane, newPane) -> {
            if (newPane != null) {
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
                delay.setOnFinished(e -> scrollToExpandedPane(newPane));
                delay.play();
            }
        });

        createLogosSection();

        // --- STICKY FLOATING NAV SYSTEM (Scene-Aware Attachment) ---
        javafx.beans.value.ChangeListener<javafx.scene.Scene> sceneListener = new javafx.beans.value.ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> obs, javafx.scene.Scene old, javafx.scene.Scene scene) {
                if (scene != null) {
                    javafx.application.Platform.runLater(() -> injectStickyFloatingNav(accordion));
                    container.sceneProperty().removeListener(this);
                }
            }
        };
        container.sceneProperty().addListener(sceneListener);
        if (container.getScene() != null) {
            injectStickyFloatingNav(accordion);
        }
    }

    private ScrollPane cachedScrollPane = null;

    private VBox floatingNav = null;

    /**
     * Injects a floating navigation overlay that stays fixed regardless of scroll position.
     */
    private void injectStickyFloatingNav(Accordion accordion) {
        if (cachedScrollPane == null) {
            cachedScrollPane = findScrollPane(container);
        }
        if (cachedScrollPane == null) return;

        javafx.scene.Node currentParent = cachedScrollPane.getParent();
        if (!(currentParent instanceof javafx.scene.layout.Pane)) return;
        javafx.scene.layout.Pane spParent = (javafx.scene.layout.Pane) currentParent;

        // Check if already injected
        if (spParent instanceof StackPane && "nav-wrapper".equals(spParent.getId())) {
             updateDynamicNavButtons(accordion);
             return;
        }

        int index = spParent.getChildren().indexOf(cachedScrollPane);
        spParent.getChildren().remove(cachedScrollPane);

        StackPane stack = new StackPane();
        stack.setId("nav-wrapper");
        stack.setPickOnBounds(false);
        
        floatingNav = new VBox(8);
        StackPane.setAlignment(floatingNav, Pos.TOP_RIGHT);
        StackPane.setMargin(floatingNav, new Insets(80, -22, 0, 0)); // Move more to the right and down
        stack.getChildren().addAll(cachedScrollPane, floatingNav);
        
        floatingNav.setMaxWidth(StackPane.USE_PREF_SIZE);
        floatingNav.setMaxHeight(StackPane.USE_PREF_SIZE);

        // INITIAL BUILD
        updateDynamicNavButtons(accordion);

        // AUTO-UPDATE when references change
        referenceController.setOnItemsChanged(() -> updateDynamicNavButtons(accordion));

        // SYNC VISIBILITY: Show if scrolled down slightly OR if in lower sections
        floatingNav.visibleProperty().bind(new javafx.beans.binding.BooleanBinding() {
            { super.bind(cachedScrollPane.vvalueProperty(), accordion.expandedPaneProperty()); }
            @Override protected boolean computeValue() {
                TitledPane expanded = accordion.getExpandedPane();
                int idx = accordion.getPanes().indexOf(expanded);
                return cachedScrollPane.getVvalue() > 0.08 || idx >= 2; 
            }
        });

        spParent.getChildren().add(index, stack);
        
        if (spParent instanceof VBox) {
            VBox.setVgrow(stack, javafx.scene.layout.Priority.ALWAYS);
        } else if (spParent instanceof HBox) {
            HBox.setHgrow(stack, javafx.scene.layout.Priority.ALWAYS);
        }
    }
    private void updateDynamicNavButtons(Accordion accordion) {
        if (floatingNav == null) return;
        floatingNav.getChildren().clear();
        floatingNav.setSpacing(4); // Tighter for tabs

        // 1. Colores
        addFloatingNavTab(floatingNav, "mdi2p-palette", "1", "Colores", 
            accordion.getPanes().get(0), () -> accordion.setExpandedPane(accordion.getPanes().get(0)));

        // 2. Marca
        addFloatingNavTab(floatingNav, "mdi2t-tag-text-outline", "2", "Marca", 
            accordion.getPanes().get(1), () -> accordion.setExpandedPane(accordion.getPanes().get(1)));

        // 3. Dorsales
        addFloatingNavTab(floatingNav, "mdi2n-numeric", "3", "Dorsales", 
            accordion.getPanes().get(2), () -> accordion.setExpandedPane(accordion.getPanes().get(2)));

        // 4. Textos
        addFloatingNavTab(floatingNav, "mdi2f-format-letter-case", "4", "Textos", 
            accordion.getPanes().get(3), () -> accordion.setExpandedPane(accordion.getPanes().get(3)));
            
        // 5. Referencias
        addFloatingNavTab(floatingNav, "mdi2i-image-multiple", "5", "Referencias", 
            accordion.getPanes().get(4), () -> accordion.setExpandedPane(accordion.getPanes().get(4)));
    }

    private void addFloatingNavTab(VBox parent, String iconName, String labelStr, String tooltip, javafx.scene.Node targetSection, Runnable action) {
        Button btn = new Button();
        VBox internal = new VBox(1);
        internal.setAlignment(Pos.CENTER);

        javafx.scene.Node icon = UIFactory.crearIcono(iconName, 18, "#2c3e50");
        Label lbl = new Label(labelStr);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1e88e5;");
        
        internal.getChildren().addAll(icon, lbl);
        btn.setGraphic(internal);
        btn.setTooltip(new Tooltip(tooltip));
        
        // TAB STYLE: Attached to the left, rounded on the right
        btn.setStyle("-fx-background-color: white; " +
                     "-fx-background-radius: 0 8 8 0; " +
                     "-fx-border-color: #e2e8f0 #1e88e5 #e2e8f0 transparent; " +
                     "-fx-border-radius: 0 8 8 0; " +
                     "-fx-border-width: 1.5; " +
                     "-fx-cursor: hand; -fx-padding: 6 8 6 4; " +
                     "-fx-min-width: 42; -fx-min-height: 48; " +
                     "-fx-max-width: 42; -fx-max-height: 48; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 3, 0, 1, 1);");
        
        btn.visibleProperty().bind(new javafx.beans.binding.BooleanBinding() {
            { super.bind(cachedScrollPane.vvalueProperty(), cachedScrollPane.heightProperty()); }
            @Override protected boolean computeValue() {
                if (cachedScrollPane == null || targetSection == null) return false;
                double yInContent = getYInContent(targetSection);
                double viewportHeight = cachedScrollPane.getViewportBounds().getHeight();
                double contentHeight = ((javafx.scene.layout.Region)cachedScrollPane.getContent()).getHeight();
                double currentY = cachedScrollPane.getVvalue() * (contentHeight - viewportHeight);
                return yInContent < (currentY - 100); 
            }
        });
        btn.managedProperty().bind(btn.visibleProperty());

        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().split("; -fx-effect")[0] + "; -fx-background-color: #f8fafc; -fx-border-color: #e2e8f0 #1565c0 #e2e8f0 transparent; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 4, 0, 1, 1);"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().split("; -fx-effect")[0] + "; -fx-background-color: white; -fx-border-color: #e2e8f0 #1e88e5 #e2e8f0 transparent; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 3, 0, 1, 1);"));

        btn.setOnAction(e -> action.run());
        parent.getChildren().add(btn);
    }

    private double getYInContent(javafx.scene.Node node) {
        double y = 0;
        javafx.scene.Node curr = node;
        javafx.scene.Node stopAt = cachedScrollPane.getContent();
        while (curr != null && curr != stopAt) {
            y += curr.getLayoutY();
            curr = curr.getParent();
        }
        return y;
    }

    private void scrollToNode(javafx.scene.Node target) {
        if (cachedScrollPane == null) return;
        
        javafx.scene.Node content = cachedScrollPane.getContent();
        if (!(content instanceof javafx.scene.layout.Region)) return;
        javafx.scene.layout.Region regionContent = (javafx.scene.layout.Region) content;

        double contentHeight = regionContent.getHeight();
        double viewportHeight = cachedScrollPane.getViewportBounds().getHeight();
        if (contentHeight <= viewportHeight) return;

        double targetY = getYInContent(target) - (viewportHeight * 0.1); 

        double targetVValue = targetY / (contentHeight - viewportHeight);
        targetVValue = Math.max(0, Math.min(1.0, targetVValue));

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(500),
                new javafx.animation.KeyValue(cachedScrollPane.vvalueProperty(), targetVValue, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        timeline.play();
    }


    private void scrollToExpandedPane(TitledPane pane) {
        if (cachedScrollPane == null) {
            cachedScrollPane = findScrollPane(container);
        }
        if (cachedScrollPane == null) return;

        javafx.scene.Node content = cachedScrollPane.getContent();
        if (!(content instanceof javafx.scene.layout.Region)) return;
        javafx.scene.layout.Region regionContent = (javafx.scene.layout.Region) content;

        double contentHeight = regionContent.getHeight();
        double viewportHeight = cachedScrollPane.getViewportBounds().getHeight();
        if (contentHeight <= viewportHeight) return;

        javafx.geometry.Point2D posInContent = regionContent.sceneToLocal(pane.localToScene(0, 0));
        double nodeY = posInContent.getY();
        double nodeHeight = pane.getHeight();

        double targetY;
        if (nodeHeight < viewportHeight * 0.8) {
            targetY = nodeY - (viewportHeight / 2.0) + (nodeHeight / 2.0);
        } else {
            targetY = nodeY - (viewportHeight * 0.1);
        }

        double targetVValue = targetY / (contentHeight - viewportHeight);
        targetVValue = Math.max(0, Math.min(1.0, targetVValue));

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(500),
                new javafx.animation.KeyValue(cachedScrollPane.vvalueProperty(), targetVValue, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        timeline.play();
    }

    private ScrollPane findScrollPane(javafx.scene.Node node) {
        javafx.scene.Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof ScrollPane) return (ScrollPane) parent;
            parent = parent.getParent();
        }
        return null;
    }

    public List<ReferenceHotspotDTO> getReferenceHotspots() {
        return StateMapper.mapHotspots(visualizer.getState().getReferenceHotspots());
    }

    public List<javafx.scene.image.Image> getHotspotImages() {
        List<javafx.scene.image.Image> images = new ArrayList<>();
        for (PrendaState.ReferenceHotspot hotspot : visualizer.getState().getReferenceHotspots()) {
            if (hotspot.getImageData() != null && hotspot.getImageData().length > 0) {
                try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(hotspot.getImageData())) {
                    images.add(new javafx.scene.image.Image(bais, 400, 400, true, true));
                } catch (Exception e) {}
            } else if (hotspot.getImagePath() != null) {
                try {
                    images.add(new javafx.scene.image.Image(hotspot.getImagePath(), 400, 400, true, true));
                } catch (Exception e) {}
            }
        }
        return images;
    }

    public List<javafx.scene.image.Image> getLogoLibrary() {
        return logoController.getLibrary();
    }

    public List<javafx.scene.image.Image> getShieldLibrary() {
        return brandingController.getShieldLibrary();
    }

    public List<javafx.scene.image.Image> getShieldLibraryShirt() {
        return brandingController.getShieldLibraryShirt();
    }

    public List<javafx.scene.image.Image> getShieldLibraryShort() {
        return brandingController.getShieldLibraryShort();
    }

    public List<javafx.scene.image.Image> getShieldLibrarySleeve() {
        return brandingController.getShieldLibrarySleeve();
    }

    public List<javafx.scene.image.Image> getReferenceLibrary() {
        return brandingController.getReferenceLibrary();
    }

    public void restoreLibraries(List<javafx.scene.image.Image> logos, List<javafx.scene.image.Image> shields,
            List<javafx.scene.image.Image> refs) {
        logoController.setLibrary(logos);
        brandingController.setShieldLibrary(shields);
        brandingController.setReferenceLibrary(refs);
    }

    public void restoreLibrariesZoned(List<javafx.scene.image.Image> logos,
            List<javafx.scene.image.Image> shieldsShirt,
            List<javafx.scene.image.Image> shieldsShort,
            List<javafx.scene.image.Image> shieldsSleeve,
            List<javafx.scene.image.Image> refs) {
        logoController.setLibrary(logos);
        brandingController.setShieldLibraryShirt(shieldsShirt);
        brandingController.setShieldLibraryShort(shieldsShort);
        brandingController.setShieldLibrarySleeve(shieldsSleeve);
        brandingController.setReferenceLibrary(refs);
    }

    public void setOnEscudoChange(Consumer<org.example.model.TipoEscudo> listener) {
        brandingController.setOnBadgeTypeChange(listener);
    }

    public void updateEscudoSelection(org.example.model.TipoEscudo current) {
        brandingController.setBadgeType(current);
    }

    public void syncReferences() {
        if (referenceController != null) {
            referenceController.syncFromState();
        }
    }

    public void resetState() {
        if (textController != null)
            textController.clearAll();
        if (logoController != null)
            logoController.clearAll();
        if (visualizer != null) {
            visualizer.resetState();
        }
    }

    public void refreshContent() {
        if (visualizer == null || !isRefreshing.compareAndSet(false, true)) return;
        try {
            colorController.updateUI();
            brandingController.updateUI();
            numberController.updateUI();

            updateDynamicButtons();
        } finally {
            isRefreshing.set(false);
        }
    }

    private void startEyedropper(java.util.function.Consumer<Color> callback,
            java.util.function.Consumer<Color> preview) {
        if (getShapeController() != null) {
            getShapeController().activateEyedropper(callback, preview);
        }
    }

    private void createLogosSection() {
        container.getChildren().add(logoController.getContainer());
    }

    public javafx.scene.Node getColorPaletteNode() {
        return colorPalette.getNode();
    }

    public void hideFloatingToolbar() {
        if (shapeController != null) {
            shapeController.hideFloatingToolbar();
        }
    }

    private org.example.model.ShapeType currentShapeType = org.example.model.ShapeType.RECTANGLE;

    public javafx.scene.layout.Pane getVerticalZoneBar() {
        VBox bar = new VBox(8); 
        bar.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE); 
        bar.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE); 
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color: transparent;"); 

        StackPane wrapper = new StackPane(bar);
        wrapper.setPadding(new Insets(0, 0, 0, 20)); 
        wrapper.setStyle("-fx-background-color: transparent;");

        wrapper.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        wrapper.setPickOnBounds(false); 

        wrapper.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        addZoneButton(bar, "Pecho", "PECHO", "mdi2t-tshirt-crew");
        addZoneButton(bar, "Espalda", "ESPALDA", "mdi2t-tshirt-crew-outline");
        addZoneButton(bar, "Manga Delantera", "MANGA_DELANTERA", "mdi2a-arrow-left-box");
        addZoneButton(bar, "Manga Trasera", "MANGA_TRASERA", "mdi2a-arrow-right-box");

        Separator sep = new Separator(Orientation.HORIZONTAL);
        sep.setPadding(new Insets(5, 0, 5, 0));
        bar.getChildren().add(sep);

        addDynamicZoneButton(bar, "SHORT_FRONT", "mdi2s-shorts");
        addDynamicZoneButton(bar, "SHORT_BACK", "mdi2s-shorts");

        return wrapper;
    }

    public javafx.scene.layout.Pane getIntegratedBottomBar() {
        if (cachedBottomBar != null) return cachedBottomBar;

        ToggleButton btnToolbarToggle = new ToggleButton();
        btnToolbarToggle.setGraphic(UIFactory.crearIcono("mdi2p-palette-swatch-outline", 18, "#2c3e50"));
        btnToolbarToggle.setTooltip(new Tooltip("Mostrar/Ocultar herramientas de edición"));
        btnToolbarToggle.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4; -fx-background-radius: 20;");
        
        btnToolbarToggle.setSelected(true);

        btnToolbarToggle.selectedProperty().addListener((obs, old, val) -> {
            btnToolbarToggle.setGraphic(UIFactory.crearIcono(val ? "mdi2p-palette-swatch" : "mdi2p-palette-swatch-outline", 18, val ? "#3498db" : "#2c3e50"));
            btnToolbarToggle.setStyle(val 
                ? "-fx-background-color: #f4f6f7; -fx-cursor: hand; -fx-padding: 4; -fx-background-radius: 20;"
                : "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4; -fx-background-radius: 20;");
        });

        HBox toolsContainer = new HBox(1);
        toolsContainer.setAlignment(Pos.CENTER);
        toolsContainer.setMinSize(0, 0);

        javafx.scene.control.ScrollPane scrollTools = new javafx.scene.control.ScrollPane(toolsContainer);
        scrollTools.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollTools.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollTools.setFitToHeight(true);
        scrollTools.setPannable(true);
        scrollTools.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent; -fx-padding: 0; -fx-border-color: transparent;");
        // Redirigir rueda del mouse al scroll horizontal (la barra está oculta pero el scroll sigue funcionando)
        scrollTools.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, ev -> {
            double delta = ev.getDeltaY() != 0 ? ev.getDeltaY() : ev.getDeltaX();
            double step = 0.08; // porcentaje del rango total por cada tick de rueda
            double newH = Math.max(0, Math.min(1.0, scrollTools.getHvalue() - (delta > 0 ? step : -step)));
            scrollTools.setHvalue(newH);
            ev.consume();
        });
        scrollTools.visibleProperty().bind(btnToolbarToggle.selectedProperty());
        scrollTools.managedProperty().bind(btnToolbarToggle.selectedProperty());

        HBox bar = new HBox(2); 
        bar.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        bar.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        bar.setMinSize(0, 0);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(2, 6, 2, 6)); 
        bar.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-border-radius: 30; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4);");

        bar.setMinHeight(30); 

        HBox shapeBox = new HBox(1); 
        shapeBox.setAlignment(Pos.CENTER);

        ToggleButton btnShape = new ToggleButton();
        if (getShapeController() != null) {
            getShapeController().getButtonFactory().configureAsToolButton(btnShape, getIconForShape(currentShapeType), "Formas", "TOOL_SHAPE");
        }

        Runnable updateShapeBtn = () -> {
            boolean isSel = btnShape.isSelected();
            btnShape.setStyle(isSel ? "-fx-background-color: #d6eaf8; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 4;" : "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4; -fx-background-radius: 5;");
            btnShape.setGraphic(UIFactory.crearIcono(getIconForShape(currentShapeType), 16, isSel ? "#3498db" : "#2c3e50"));
            btnShape.setTooltip(new Tooltip("Forma Actual: " + currentShapeType.getDisplayName() + "\n(Click derecho para cambiar)"));
        };
        updateShapeBtn.run();

        btnShape.selectedProperty().addListener((obs, old, val) -> updateShapeBtn.run());

        javafx.scene.control.ContextMenu shapeMenu = new javafx.scene.control.ContextMenu();
        for (org.example.model.ShapeType type : org.example.model.ShapeType.values()) {
            if (type == org.example.model.ShapeType.CUSTOM_PATH) continue; 
            javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(type.getDisplayName());
            item.setGraphic(UIFactory.crearIcono(getIconForShape(type), 14, "#555"));
            item.setOnAction(e -> {
                currentShapeType = type;
                updateShapeBtn.run();
            });
            shapeMenu.getItems().add(item);
        }

        btnShape.setOnContextMenuRequested(e -> shapeMenu.show(btnShape, e.getScreenX(), e.getScreenY()));

        btnShape.setOnAction(e -> {
            if (getShapeController() == null) return;
            if (btnShape.isSelected()) {
                Color fill = getShapeController().getFillColor();
                Color stroke = getShapeController().getStrokeColor();
                if (currentShapeType == org.example.model.ShapeType.CUSTOM_PATH) {
                    visualizer.getShapeHelper().startShapeCreation(currentShapeType, fill, stroke, 1.5, null);
                } else {
                    visualizer.getShapeHelper().insertDefaultShape(currentShapeType, fill, stroke, 1.5, (l) -> {});
                    javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
                    p.setOnFinished(ev -> btnShape.setSelected(false));
                    p.play();
                }
            } else { visualizer.cancelShapeCreation(); }
        });

        ToggleButton btnPencil = new ToggleButton();
        if (getShapeController() != null) {
            getShapeController().getButtonFactory().configureAsToolButton(btnPencil, "mdi2p-pen", "Lápiz/Bézier", "TOOL_PENCIL");
        }
        // CRITICAL FIX: The pencil button was missing its action — clicking it changed
        // style but never started the Bézier drawing engine.
        btnPencil.setOnAction(e -> {
            if (getShapeController() == null) return;
            visualizer.getShapeHelper().exitNodeEditMode();
            if (btnPencil.isSelected()) {
                javafx.scene.paint.Color fill = getShapeController().getFillColor();
                javafx.scene.paint.Color stroke = getShapeController().getStrokeColor();
                double strokeWidth = getShapeController().getStrokeWidthSlider().getValue();
                visualizer.startShapeCreationInternal(
                    org.example.model.ShapeType.CUSTOM_PATH, fill, stroke, strokeWidth,
                    (layer) -> { if (layer != null) getShapeController().getSyncHelper().syncUIWithSelection(layer); }
                );
                visualizer.getContentGroup().setCursor(javafx.scene.Cursor.CROSSHAIR);
            } else {
                visualizer.cancelShapeCreation();
                visualizer.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });

        HBox shapeGroup = new HBox(4, btnShape, btnPencil);
        shapeGroup.setAlignment(Pos.CENTER);
        shapeGroup.setPadding(new Insets(0, 8, 0, 8));

        HBox editGroup = new HBox(4);
        editGroup.setAlignment(Pos.CENTER);
        editGroup.setPadding(new Insets(0, 8, 0, 8));
        if (getShapeController() != null) {
            javafx.scene.Node w = getShapeController().getWeldButton();
            javafx.scene.Node uw = getShapeController().getUnweldButton();
            javafx.scene.Node cut = getShapeController().getCutButton();
            javafx.scene.Node comb = getShapeController().getCombineButton();
            javafx.scene.Node n = getShapeController().getNodeEditButton();
            javafx.scene.Node s = getShapeController().getStrokeButton();
            
            if (w != null) editGroup.getChildren().add(w);
            if (uw != null) editGroup.getChildren().add(uw);
            if (cut != null) editGroup.getChildren().add(cut);
            if (comb != null) editGroup.getChildren().add(comb);
            if (n != null) shapeGroup.getChildren().add(n);
            if (s != null) editGroup.getChildren().add(s);
        }

        // --- Fill & Stroke color pickers (tight pair) ---
        HBox colorPickerGroup = new HBox(4);
        colorPickerGroup.setAlignment(Pos.CENTER);
        colorPickerGroup.setPadding(new Insets(0, 8, 0, 8));
        if (getShapeController() != null) {
            javafx.scene.Node f = getShapeController().getFillPicker();
            javafx.scene.Node st = getShapeController().getStrokePicker();
            if (f != null) colorPickerGroup.getChildren().add(f);
            if (st != null) colorPickerGroup.getChildren().add(st);
        }

        // --- Image Tools: image editor button ---
        HBox imageToolsGroup = new HBox(4);
        imageToolsGroup.setAlignment(Pos.CENTER);
        imageToolsGroup.setPadding(new Insets(0, 8, 0, 8));
        if (getShapeController() != null) {
            javafx.scene.Node editorBtn = getShapeController().getImageEditorButton();
            if (editorBtn != null) imageToolsGroup.getChildren().add(editorBtn);
        }

        HBox advancedBox = new HBox(4);
        advancedBox.setAlignment(Pos.CENTER);
        advancedBox.setPadding(new Insets(0, 8, 0, 8));
        if (getShapeController() != null) {
            javafx.scene.Node ang = getShapeController().getAngleBox();
            javafx.scene.Node tr = getShapeController().getTransformButton();
            javafx.scene.Node c = getShapeController().getContourButton();
            javafx.scene.Node t = getShapeController().getTransButton();
            javafx.scene.Node l = getShapeController().getLockButton();

            if (ang != null) advancedBox.getChildren().add(ang);
            if (tr != null) advancedBox.getChildren().add(tr);
            if (c != null) advancedBox.getChildren().add(c);
            if (t != null) advancedBox.getChildren().add(t);
            if (l != null) advancedBox.getChildren().add(l);
            advancedBox.getChildren().add(new Separator(Orientation.VERTICAL));
            javafx.scene.Node d = getShapeController().getDeleteButton();
            if (d != null) advancedBox.getChildren().add(d);
        }

        toolsContainer.getChildren().setAll(
            new Separator(Orientation.VERTICAL), shapeGroup,
            new Separator(Orientation.VERTICAL), editGroup,
            new Separator(Orientation.VERTICAL), colorPickerGroup,
            new Separator(Orientation.VERTICAL), imageToolsGroup,
            new Separator(Orientation.VERTICAL), advancedBox);
        // NOTE: The "Listo" button is NOT added here.
        // It lives exclusively as a floating overlay at TOP_CENTER of PrendaVisualizer.

        bar.getChildren().setAll(btnToolbarToggle, scrollTools);
        
        StackPane wrapper = new StackPane(bar);
        wrapper.setPickOnBounds(false);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.maxWidthProperty().bind(visualizer.widthProperty().subtract(140));
        wrapper.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        this.cachedBottomBar = wrapper;
        return wrapper;
    }

    private void addDynamicZoneButton(javafx.scene.layout.Pane parent, String zone, String icContent) {
        ToggleButton btn = new ToggleButton();
        btn.setToggleGroup(zoneToggleGroup);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8;");
        btn.selectedProperty().addListener((obs, old, val) -> {
            btn.setStyle(val ? "-fx-background-color: #3498db; -fx-background-radius: 5;" : "-fx-background-color: transparent;");
            updateShortsButtonVisuals(btn, visualizer.getState().getCorteShort(), zone, val);
        });
        btn.setOnAction(e -> {
            if (btn.isSelected()) visualizer.getPowerClipManager().enterEditMode(zone);
            else visualizer.getPowerClipManager().finishEditMode();
        });
        btn.visibleProperty().bind(new javafx.beans.binding.BooleanBinding() {
            { super.bind(visualizer.widthProperty()); }
            @Override protected boolean computeValue() { return visualizer.getAvailableZones().contains(zone); }
        });
        btn.managedProperty().bind(btn.visibleProperty());
        updateShortsButtonVisuals(btn, visualizer.getState().getCorteShort(), zone, btn.isSelected());
        dynamicButtons.add(new javafx.util.Pair<>(btn, zone));
        parent.getChildren().add(btn);
    }

    private List<javafx.util.Pair<ToggleButton, String>> dynamicButtons = new ArrayList<>();
    private void updateDynamicButtons() {
        for (javafx.util.Pair<ToggleButton, String> p : dynamicButtons) {
            updateShortsButtonVisuals(p.getKey(), visualizer.getState().getCorteShort(), p.getValue(), p.getKey().isSelected());
        }
    }

    private void updateShortsButtonVisuals(ToggleButton btn, org.example.model.TipoCorte corte, String zone, boolean isSelected) {
        String base = "Short", suffix = "", icon = "mdi2c-crop-square";
        boolean fem = false;
        if (corte == org.example.model.TipoCorte.PANTALONETA || corte == org.example.model.TipoCorte.LICRA) { base = corte.name(); fem = true; }
        if (zone.endsWith("_FRONT")) { suffix = fem ? "Delantera" : "Delantero"; icon = "mdi2c-crop-square"; }
        else if (zone.endsWith("_BACK")) { suffix = fem ? "Trasera" : "Trasero"; icon = "mdi2t-texture"; }
        btn.setTooltip(new Tooltip("Editar " + base + " " + suffix));
        btn.setGraphic(UIFactory.crearIcono(icon, 18, isSelected ? "white" : "#2c3e50"));
    }

    private void addZoneButton(javafx.scene.layout.Pane parent, String label, String zone, String icon) {
        ToggleButton btn = new ToggleButton();
        btn.setToggleGroup(zoneToggleGroup);
        btn.setTooltip(new Tooltip("Editar " + label));
        btn.setGraphic(UIFactory.crearIcono(icon, 18, "#2c3e50"));
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8;");
        btn.selectedProperty().addListener((obs, old, val) -> {
            btn.setStyle(val ? "-fx-background-color: #3498db; -fx-background-radius: 5;" : "-fx-background-color: transparent;");
            btn.setGraphic(UIFactory.crearIcono(icon, 18, val ? "white" : "#2c3e50"));
        });
        btn.setOnAction(e -> {
            if (btn.isSelected()) visualizer.getPowerClipManager().enterEditMode(zone);
            else visualizer.getPowerClipManager().finishEditMode();
        });
        btn.visibleProperty().bind(new javafx.beans.binding.BooleanBinding() {
            { super.bind(visualizer.widthProperty()); }
            @Override protected boolean computeValue() { return visualizer.getAvailableZones().contains(zone); }
        });
        btn.managedProperty().bind(btn.visibleProperty());
        parent.getChildren().add(btn);
    }

    private String getIconForShape(org.example.model.ShapeType type) {
        switch (type) {
            case RECTANGLE: return "mdi2r-rectangle-outline";
            case CIRCLE: return "mdi2c-circle-outline";
            case TRIANGLE: return "mdi2t-triangle-outline";
            case STAR: return "mdi2s-star-outline";
            case PENTAGON: return "mdi2p-pentagon-outline";
            case HEXAGON: return "mdi2h-hexagon-outline";
            case CUSTOM_PATH: return "mdi2p-pen";
            default: return "mdi2s-shape-outline";
        }
    }
}

