package org.example.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Side;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import org.example.logic.SessionManager;
import org.example.dao.UsuarioDAO;

public class ShellController {

    @FXML private HBox tabsContainer;
    @FXML private StackPane contentArea;
    @FXML private javafx.scene.shape.SVGPath iconWinMax;
    @FXML private HBox profileContainer;
    @FXML private ImageView imgAvatar;
    @FXML private Label lblProfileName;
    @FXML private Menu menuAdmin;
    @FXML private MenuItem menuItemHistory;
    @FXML private MenuItem menuItemUsers;
    private Button btnBurger;
    private Tooltip burgerTooltip;
    private ContextMenu burgerContextMenu;

    private List<TabInfo> tabs = new ArrayList<>();
    private TabInfo activeTab;
    private int tabCount = 1;

    // Window Drag State (Absolute Screenspace Coordinates)
    private double initialStageX, initialStageY, initialScreenX, initialScreenY;
    private boolean isMaximized = false;
    private double lastX, lastY, lastWidth, lastHeight;
    private boolean isDragging = false;
    private static final double DRAG_THRESHOLD = 5.0; // pixels before drag activates

    // Pre-loading Pool (Shared)
    private static final ConcurrentLinkedQueue<TabInfo> preloadedPool = new ConcurrentLinkedQueue<>();
    private static TabInfo dragBuffer;

    /**
     * STATIC method to be called from Splash Screen (Main.java) 
     * Load N tabs while the splash screen progress bar is running.
     */
    public static void preWarmPoolSync(int count) {
        for (int i = 0; i < count; i++) {
            try {
                // FXML loading is allowed in background if no scene exists
                FXMLLoader loader = new FXMLLoader(ShellController.class.getResource("/nuevo_pedido.fxml"));
                Parent content = loader.load();
                PedidoController controller = loader.getController();
                
                // Set Shell mode immediately
                controller.setShellMode(true);
                
                preloadedPool.add(new TabInfo("Pre-cargada", content, controller));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initialize() {
        tabsContainer.setOnDragOver(this::onTabsDragOver);
        tabsContainer.setOnDragDropped(this::onTabsDragDropped);

        // Create burger button programmatically
        createBurgerButton();

        // Use a tab from the pool (already filled by Splash Screen)
        loadInitialTab();

        // Refill pool for future clicks async
        refillPoolAsync();

        // Setup burger button menu and iconified listener
        setupBurgerMenu();
        setupMinimizeListener();

        // Load profile UI
        refreshProfileUI();

        // Detect initial state and sync icon
        javafx.application.Platform.runLater(() -> {
            try {
                if (tabsContainer != null && tabsContainer.getScene() != null && tabsContainer.getScene().getWindow() != null) {
                    Stage stage = (Stage) tabsContainer.getScene().getWindow();
                    checkInitialMaximization(stage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void createBurgerButton() {
        btnBurger = new Button();
        btnBurger.setVisible(false);
        btnBurger.setManaged(false);
        btnBurger.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        btnBurger.setStyle("-fx-background-color: #0B213E; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 0;");
        FontIcon burgerIcon = new FontIcon("mdi2m-menu");
        burgerIcon.setIconSize(20);
        burgerIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        btnBurger.setGraphic(burgerIcon);
        Tooltip tooltip = new Tooltip("Menú");
        btnBurger.setTooltip(tooltip);
        StackPane.setMargin(btnBurger, new javafx.geometry.Insets(10, 15, 0, 0));
        StackPane.setAlignment(btnBurger, javafx.geometry.Pos.TOP_RIGHT);
    }

    private void ensureBurgerOnTop() {
        if (btnBurger != null && contentArea != null) {
            if (!contentArea.getChildren().contains(btnBurger)) {
                contentArea.getChildren().add(btnBurger);
            }
            btnBurger.toFront();
        }
    }

    private void setupBurgerMenu() {
        if (btnBurger == null) return;
        burgerContextMenu = new ContextMenu();
        burgerContextMenu.setAutoHide(true);

        btnBurger.setOnAction(event -> {
            if (burgerContextMenu != null) {
                if (burgerContextMenu.isShowing()) {
                    burgerContextMenu.hide();
                } else {
                    burgerContextMenu.show(btnBurger, Side.BOTTOM, 0, 0);
                }
            }
        });
        
        refreshBurgerMenu();
    }

    public void refreshBurgerMenu() {
        if (burgerContextMenu == null) return;
        burgerContextMenu.getItems().clear();

        MenuItem newTab = new MenuItem("Nuevo Pedido");
        newTab.setOnAction(this::addNewTabAction);
        newTab.setGraphic(new FontIcon("mdi2p-plus-circle-outline"));

        MenuItem openProject = new MenuItem("Abrir Proyecto...");
        openProject.setOnAction(this::onAbrirProyecto);
        openProject.setGraphic(new FontIcon("mdi2f-folder-open-outline"));

        MenuItem saveProject = new MenuItem("Guardar");
        saveProject.setOnAction(this::onGuardarProyecto);
        saveProject.setGraphic(new FontIcon("mdi2c-content-save-outline"));

        MenuItem saveAs = new MenuItem("Guardar como...");
        saveAs.setOnAction(this::onGuardarProyectoComo);
        saveAs.setGraphic(new FontIcon("mdi2c-content-save-all-outline"));

        MenuItem newWindow = new MenuItem("Nueva Ventana");
        newWindow.setOnAction(this::nuevoPedidoNuevaVentanaAction);
        newWindow.setGraphic(new FontIcon("mdi2w-window-maximize"));

        MenuItem exit = new MenuItem("Salir");
        exit.setOnAction(this::salirAction);
        exit.setGraphic(new FontIcon("mdi2e-exit-to-app"));

        burgerContextMenu.getItems().addAll(newTab, newWindow, new SeparatorMenuItem(), openProject, saveProject, saveAs);

        // Role-based custom items
        if (SessionManager.isLoggedIn()) {
            burgerContextMenu.getItems().add(new SeparatorMenuItem());

            MenuItem historyItem = new MenuItem("Historial de Pedidos");
            historyItem.setGraphic(new FontIcon("mdi2h-history"));
            historyItem.setOnAction(e -> {
                Stage owner = (Stage) btnBurger.getScene().getWindow();
                new org.example.ui.OrderHistoryDialog(this).show(owner);
            });
            burgerContextMenu.getItems().add(historyItem);

            if ("Jefe".equalsIgnoreCase(SessionManager.getCurrentUser().getRol())) {
                MenuItem manageUsers = new MenuItem("Gestionar Vendedores");
                manageUsers.setGraphic(new FontIcon("mdi2a-account-cog"));
                manageUsers.setOnAction(e -> {
                    Stage owner = (Stage) btnBurger.getScene().getWindow();
                    new org.example.ui.UserManagementDialog().show(owner);
                });
                burgerContextMenu.getItems().add(manageUsers);
            }
        }

        burgerContextMenu.getItems().addAll(new SeparatorMenuItem(), exit);
    }

    public void refreshProfileUI() {
        if (lblProfileName == null || imgAvatar == null) return;

        Circle clip = new Circle(12, 12, 12);
        imgAvatar.setClip(clip);

        if (SessionManager.isLoggedIn()) {
            UsuarioDAO.Usuario user = SessionManager.getCurrentUser();
            lblProfileName.setText(user.getNombreCompleto());
            
            String customPath = SessionManager.getCustomAvatarPath(user.getNombreUsuario());
            if (!customPath.isEmpty() && new File(customPath).exists()) {
                imgAvatar.setImage(new Image(new File(customPath).toURI().toString()));
            } else {
                imgAvatar.setImage(new Image(getClass().getResourceAsStream("/vectors/logo_small.png")));
            }

            if (menuAdmin != null) {
                menuAdmin.setVisible(true);
                if ("Jefe".equalsIgnoreCase(user.getRol())) {
                    menuItemUsers.setVisible(true);
                } else {
                    menuItemUsers.setVisible(false);
                }
            }
        } else {
            lblProfileName.setText("Invitado");
            imgAvatar.setImage(new Image(getClass().getResourceAsStream("/vectors/logo_small.png")));
            if (menuAdmin != null) {
                menuAdmin.setVisible(false);
            }
        }

        // Refresh dynamic menu options
        refreshBurgerMenu();
    }

    @FXML
    public void showOrderHistoryAction() {
        Stage owner = (Stage) tabsContainer.getScene().getWindow();
        new org.example.ui.OrderHistoryDialog(this).show(owner);
    }
 
    @FXML
    public void showUserManagementAction() {
        Stage owner = (Stage) tabsContainer.getScene().getWindow();
        new org.example.ui.UserManagementDialog().show(owner);
    }
 
    @FXML
    private void handleProfileClicked(javafx.scene.input.MouseEvent event) {
        event.consume(); // Prevent event from bubbling to the title bar drag handler
        Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
        new org.example.ui.ProfileDialog(this).show(owner);
    }

    @FXML
    private void handleProfileMousePressed(javafx.scene.input.MouseEvent event) {
        event.consume(); // Consume MOUSE_PRESSED so it doesn't reach the titleBar drag handler
    }

    @FXML
    private void handleProfileMouseReleased(javafx.scene.input.MouseEvent event) {
        event.consume(); // Consume MOUSE_RELEASED so it doesn't trigger drag-release snap logic
    }

    public PedidoController getActiveTabController() {
        return (activeTab != null) ? activeTab.controller : null;
    }

    private void setupMinimizeListener() {
        if (tabsContainer == null || tabsContainer.getScene() == null) {
            if (tabsContainer != null) {
                tabsContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        Stage stage = (Stage) newScene.getWindow();
                        if (stage != null) {
                            stage.iconifiedProperty().addListener((obs2, wasIconified, isIconified) -> {
                                updateBurgerVisibility(isIconified);
                                if (burgerContextMenu != null && burgerContextMenu.isShowing()) {
                                    burgerContextMenu.hide();
                                }
                            });
                            stage.focusedProperty().addListener((obs3, wasFocused, isFocused) -> {
                                if (!isFocused && burgerContextMenu != null && burgerContextMenu.isShowing()) {
                                    burgerContextMenu.hide();
                                }
                            });
                            updateBurgerVisibility(stage.isIconified());
                        }
                    }
                });
            }
        } else {
            Stage stage = (Stage) tabsContainer.getScene().getWindow();
            stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
                updateBurgerVisibility(isIconified);
                if (burgerContextMenu != null && burgerContextMenu.isShowing()) {
                    burgerContextMenu.hide();
                }
            });
            stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused && burgerContextMenu != null && burgerContextMenu.isShowing()) {
                    burgerContextMenu.hide();
                }
            });
            updateBurgerVisibility(stage.isIconified());
        }
    }

    private void updateBurgerVisibility(boolean isMinimized) {
        if (btnBurger != null) {
            btnBurger.setVisible(isMinimized);
            btnBurger.setManaged(isMinimized);
        }
    }

    private void loadInitialTab() {
        TabInfo info = preloadedPool.poll();
        if (info != null) {
            setupAndAddTab(info);
        } else {
            // Fallback if splash was too fast or failed
            addAndSelectNewTab();
        }
    }

    private void refillPoolAsync() {
        if (preloadedPool.size() < 2) {
            CompletableFuture.runAsync(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/nuevo_pedido.fxml"));
                    Parent content = loader.load();
                    PedidoController controller = loader.getController();
                    Platform.runLater(() -> {
                        controller.setShellMode(true);
                        preloadedPool.add(new TabInfo("Preloading...", content, controller));
                    });
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }

    @FXML
    public void addNewTabAction(ActionEvent event) {
        TabInfo info = preloadedPool.poll();
        if (info != null) {
            setupAndAddTab(info);
        } else {
            addAndSelectNewTab();
        }
        refillPoolAsync();
    }

    private void setupAndAddTab(TabInfo info) {
        info.controller.setShellController(this);
        info.title = "Sin guardar " + tabCount++;
        tabs.add(info);
        createTabButton(info);
        
        info.controller.setOnTitleChanged(newTitle -> {
            info.title = newTitle;
            if (info.tabLabel != null) info.tabLabel.setText(newTitle);
        });

        switchToTab(info);
    }

    private void addAndSelectNewTab() {
        // Fallback or refill
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/nuevo_pedido.fxml"));
            Parent content = loader.load();
            PedidoController controller = loader.getController();
            controller.setShellMode(true);
            TabInfo info = new TabInfo("", content, controller);
            setupAndAddTab(info);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void createTabButton(TabInfo info) {
        HBox tabBtn = new HBox();
        tabBtn.getStyleClass().add("shell-tab-button");
        tabBtn.setSpacing(8);
        tabBtn.setAlignment(javafx.geometry.Pos.CENTER);

        Label lbl = new Label(info.title);
        lbl.getStyleClass().add("shell-tab-label");
        info.tabLabel = lbl;

        Button closeBtn = new Button();
        closeBtn.setGraphic(new FontIcon("mdi2c-close"));
        closeBtn.getStyleClass().add("shell-tab-close-btn");
        closeBtn.setOnAction(e -> closeTab(info));

        tabBtn.getChildren().addAll(lbl, closeBtn);
        tabBtn.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY) switchToTab(info); });

        // D&D
        tabBtn.setOnDragDetected(event -> {
            Dragboard db = tabBtn.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("PID_DRAG");
            db.setContent(content);
            dragBuffer = info;
            tabBtn.setOpacity(0.5);
            event.consume();
        });

        tabBtn.setOnDragDone(event -> {
            tabBtn.setOpacity(1.0);
            if (event.getTransferMode() == TransferMode.MOVE) removeTabQuietly(info);
            else if (event.getTransferMode() == null && tabs.size() > 1) undock(info);
            event.consume();
        });

        info.tabButton = tabBtn;
        tabsContainer.getChildren().add(tabBtn);
    }

    private void removeTabQuietly(TabInfo info) {
        tabs.remove(info);
        tabsContainer.getChildren().remove(info.tabButton);
        if (tabs.isEmpty()) {
            contentArea.getChildren().clear();
            ensureBurgerOnTop();
            activeTab = null;
            addAndSelectNewTab();
            return;
        }
        if (activeTab == info) switchToTab(tabs.get(0));
    }

    private boolean canCloseAllTabs() {
        for (TabInfo t : new ArrayList<>(tabs)) {
            if (!t.controller.canClose()) return false;
        }
        return true;
    }

    private void undock(TabInfo info) {
        removeTabQuietly(info);
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shell.fxml"));
            Parent root = loader.load();
            ShellController newShell = loader.getController();
            newShell.clearTabs();
            newShell.dock(info);
            Scene scene = new Scene(root); scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.initStyle(StageStyle.UNDECORATED); stage.setScene(scene);
            configureStageSizeAndPosition(stage);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void clearTabs() {
        tabs.clear(); tabsContainer.getChildren().clear(); contentArea.getChildren().clear();
    }

    public void dock(TabInfo info) {
        tabs.add(info); createTabButton(info); switchToTab(info);
    }

    private void onTabsDragOver(DragEvent event) { if (dragBuffer != null) event.acceptTransferModes(TransferMode.MOVE); }
    private void onTabsDragDropped(DragEvent event) {
        if (dragBuffer != null) {
            TabInfo info = dragBuffer;
            if (!tabs.contains(info)) { dock(info); event.setDropCompleted(true); }
            else { moveTabToPosition(info, event.getX()); event.setDropCompleted(true); }
        }
    }

    private void moveTabToPosition(TabInfo info, double xCoord) {
        tabsContainer.getChildren().remove(info.tabButton);
        int index = 0; double currentX = 0;
        for (Node child : tabsContainer.getChildren()) {
            currentX += ((HBox)child).getWidth();
            if (xCoord < currentX) break;
            index++;
        }
        tabsContainer.getChildren().add(index, info.tabButton);
    }

    private void switchToTab(TabInfo info) {
        if (activeTab != null) activeTab.tabButton.getStyleClass().remove("shell-tab-active");
        activeTab = info; activeTab.tabButton.getStyleClass().add("shell-tab-active");
        contentArea.getChildren().setAll(info.content);
        ensureBurgerOnTop();
        info.content.requestFocus();
    }

    private void closeTab(TabInfo info) {
        if (info.controller.canClose()) {
            removeTabQuietly(info);
        }
    }

    @FXML private void onGuardarProyecto(ActionEvent e) { if (activeTab != null) activeTab.controller.guardarProyectoAction(e); }
    @FXML private void onGuardarProyectoComo(ActionEvent e) { if (activeTab != null) activeTab.controller.guardarProyectoComoAction(e); }
    @FXML private void onAbrirProyecto(ActionEvent e) { if (activeTab != null) activeTab.controller.cargarProyectoAction(e); }

    @FXML private void nuevoPedidoNuevaVentanaAction(ActionEvent event) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shell.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root); scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.initStyle(StageStyle.UNDECORATED); stage.setScene(scene);
            configureStageSizeAndPosition(stage);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void salirAction(ActionEvent event) {
        if (canCloseAllTabs()) {
            System.exit(0);
        }
    }

    // Controls
    @FXML private void handleWindowDragPressed(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        initialStageX = stage.getX();
        initialStageY = stage.getY();
        initialScreenX = event.getScreenX();
        initialScreenY = event.getScreenY();
        isDragging = false; // Reset drag flag on each new press
    }
    @FXML private void handleWindowDragDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        double dx = event.getScreenX() - initialScreenX;
        double dy = event.getScreenY() - initialScreenY;

        // Only start moving window after threshold to avoid accidental moves on click
        if (!isDragging) {
            if (Math.abs(dx) < DRAG_THRESHOLD && Math.abs(dy) < DRAG_THRESHOLD) return;
            isDragging = true;
        }

        if (isMaximized) {
            // Restore if dragged down while maximized
            if (Math.abs(event.getScreenY() - lastY) > 10) {
                // Determine current monitor bounds to calculate ratio
                javafx.collections.ObservableList<javafx.stage.Screen> screens = javafx.stage.Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
                javafx.stage.Screen screen = screens.isEmpty() ? javafx.stage.Screen.getPrimary() : screens.get(0);
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                
                double ratio = (event.getScreenX() - bounds.getMinX()) / bounds.getWidth();
                maximizeApp(event); 
                initialStageX = event.getScreenX() - (lastWidth * ratio);
                initialStageY = event.getScreenY() - 15; // Set some vertical padding so cursor is on title bar
                initialScreenX = event.getScreenX();
                initialScreenY = event.getScreenY();
            }
        }
        if (!isMaximized) { 
            stage.setX(initialStageX + (event.getScreenX() - initialScreenX)); 
            stage.setY(initialStageY + (event.getScreenY() - initialScreenY)); 
        }
    }
    @FXML private void handleWindowDragReleased(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        double screenY = event.getScreenY();
        double screenX = event.getScreenX();
        
        // Get current screen
        javafx.collections.ObservableList<javafx.stage.Screen> screens = javafx.stage.Screen.getScreensForRectangle(screenX, screenY, 1, 1);
        javafx.stage.Screen screen = screens.isEmpty() ? javafx.stage.Screen.getPrimary() : screens.get(0);
        javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
        
        if (screenY <= bounds.getMinY() + 15) {
            // SNAP TO TOP: Maximize
            maximizeApp(event);
        } else if (screenX <= bounds.getMinX() + 15) {
            // SNAP TO LEFT: Half Width
            lastX = stage.getX(); lastY = stage.getY(); lastWidth = stage.getWidth(); lastHeight = stage.getHeight();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight());
            isMaximized = false; // Technically not "maximized" mode
        } else if (screenX >= bounds.getMaxX() - 15) {
            // SNAP TO RIGHT: Half Width
            lastX = stage.getX(); lastY = stage.getY(); lastWidth = stage.getWidth(); lastHeight = stage.getHeight();
            stage.setX(bounds.getMinX() + bounds.getWidth() / 2);
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight());
            isMaximized = false;
        }
    }
    @FXML private void handleTitleBarClicked(javafx.scene.input.MouseEvent event) { if (event.getClickCount() == 2) maximizeApp(event); }
    @FXML private void minimizeApp(javafx.scene.input.MouseEvent event) { ((Stage) ((Node) event.getSource()).getScene().getWindow()).setIconified(true); }
    @FXML private void maximizeApp(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (isMaximized) {
            stage.setWidth(lastWidth); stage.setHeight(lastHeight); stage.setX(lastX); stage.setY(lastY);
            if (iconWinMax != null) iconWinMax.setContent("M 4,4 L 12,4 L 12,12 L 4,12 Z M 5,5 L 11,5 L 11,11 L 5,11 Z");
            isMaximized = false;
        } else {
            lastX = stage.getX(); lastY = stage.getY(); lastWidth = stage.getWidth(); lastHeight = stage.getHeight();
            // Multi-monitor support: Get the screen where the cursor/window is
            javafx.collections.ObservableList<javafx.stage.Screen> screens = javafx.stage.Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            javafx.stage.Screen screen = screens.isEmpty() ? javafx.stage.Screen.getPrimary() : screens.get(0);
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            
            stage.setX(bounds.getMinX()); stage.setY(bounds.getMinY()); stage.setWidth(bounds.getWidth()); stage.setHeight(bounds.getHeight());
            if (iconWinMax != null) iconWinMax.setContent("M 4,6 L 12,6 L 12,14 L 4,14 Z M 6,2 L 14,2 L 14,10 L 13,10 L 13,3 L 6,3 L 6,2 Z");
            isMaximized = true;
        }
    }
    private void configureStageSizeAndPosition(Stage stage) {
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        javafx.stage.Window activeWindow = javafx.stage.Window.getWindows().stream()
                .filter(javafx.stage.Window::isFocused).findFirst().orElse(null);
        if (activeWindow != null) {
            javafx.collections.ObservableList<javafx.stage.Screen> screens = javafx.stage.Screen.getScreensForRectangle(
                    activeWindow.getX(), activeWindow.getY(), activeWindow.getWidth(), activeWindow.getHeight());
            if (!screens.isEmpty()) {
                bounds = screens.get(0).getVisualBounds();
            }
        }
        
        // Start maximized occupying the full visual bounds of the screen it opens on
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setResizable(true);
    }

    private void checkInitialMaximization(Stage stage) {
        if (stage == null) return;
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        javafx.collections.ObservableList<javafx.stage.Screen> screens = javafx.stage.Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        if (!screens.isEmpty()) {
            bounds = screens.get(0).getVisualBounds();
        }
        
        lastX = stage.getX();
        lastY = stage.getY();
        lastWidth = stage.getWidth();
        lastHeight = stage.getHeight();
        
        if (Math.abs(stage.getWidth() - bounds.getWidth()) < 20 && Math.abs(stage.getHeight() - bounds.getHeight()) < 20) {
            isMaximized = true;
            if (iconWinMax != null) {
                iconWinMax.setContent("M 4,6 L 12,6 L 12,14 L 4,14 Z M 6,2 L 14,2 L 14,10 L 13,10 L 13,3 L 6,3 L 6,2 Z");
            }
            if (lastWidth <= 100 || lastHeight <= 100 || lastWidth >= bounds.getWidth() - 10 || lastHeight >= bounds.getHeight() - 10) {
                lastWidth = Math.min(1100, bounds.getWidth() - 40);
                lastHeight = Math.min(700, bounds.getHeight() - 40);
                lastX = bounds.getMinX() + (bounds.getWidth() - lastWidth) / 2;
                lastY = bounds.getMinY() + (bounds.getHeight() - lastHeight) / 2;
            }
        } else {
            isMaximized = false;
            if (iconWinMax != null) {
                iconWinMax.setContent("M 4,4 L 12,4 L 12,12 L 4,12 Z M 5,5 L 11,5 L 11,11 L 5,11 Z");
            }
        }
    }

    @FXML
    private void closeApp(javafx.scene.input.MouseEvent event) {
        if (canCloseAllTabs()) {
            ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
        }
    }

    public static class TabInfo {
        String title; Parent content; PedidoController controller; HBox tabButton; Label tabLabel;
        TabInfo(String title, Parent content, PedidoController controller) { this.title = title; this.content = content; this.controller = controller; }
    }
}
