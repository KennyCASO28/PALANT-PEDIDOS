package org.example;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.dao.ConexionDB;
import org.example.utils.FileAssociationUtils;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Register .tlp file type icon silently in the background (Windows only)
        new Thread(() -> FileAssociationUtils.register(), "tlp-icon-registration").start();

        // 1. Setup Splash Screen Stage
        Stage splashStage = new Stage();
        FXMLLoader splashLoader = new FXMLLoader(Main.class.getResource("/splash.fxml"));
        Parent splashRoot = splashLoader.load();
        Scene splashScene = new Scene(splashRoot);
        splashScene.setFill(Color.TRANSPARENT); // Allow rounded corners if supported
        splashStage.setScene(splashScene);
        splashStage.initStyle(StageStyle.TRANSPARENT); // Transparent for rounded corners

        // --- Icon for Splash ---
        try {
            splashStage.getIcons().add(new Image(Main.class.getResourceAsStream("/vectors/LOGO PALANT-barra.png")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        splashStage.show();

        // 2. Get Controls to update
        ProgressBar bar = (ProgressBar) splashScene.lookup("#progressBar");
        Label status = (Label) splashScene.lookup("#lblStatus");
        Parent rootPane = (Parent) splashScene.lookup("#rootPane");

        javafx.scene.canvas.Canvas particleCanvas = (javafx.scene.canvas.Canvas) splashScene.lookup("#particleCanvas");

        // --- ANIMATION: Advanced Particle & Color System (TEXTILE THEME) ---
        if (rootPane != null) {
            // Thread System
            java.util.List<ThreadScrap> threads = new java.util.ArrayList<>();
            javafx.scene.canvas.GraphicsContext gc = (particleCanvas != null) ? particleCanvas.getGraphicsContext2D()
                    : null;

            // Random instance
            java.util.Random random = new java.util.Random();

            javafx.animation.AnimationTimer visualEffects = new javafx.animation.AnimationTimer() {
                long lastUpdate = 0;
                double hue = 0;

                // Needle Effect State
                boolean needleActive = false;
                double needleX = 0;
                double needleY = 0;
                double needleTargetX = 0;
                double needleTargetY = 0;

                @Override
                public void handle(long now) {
                    if (now - lastUpdate >= 16_000_000) { // ~60 FPS
                        // 1. Color Cycling Logic (FASTER & VIBRANT)
                        hue = (hue + 0.8) % 360; // Much faster shift

                        // Textile Palette: Rich, saturated colors
                        Color color1 = Color.hsb(hue, 0.9, 1.0);
                        Color color2 = Color.hsb((hue + 120) % 360, 0.8, 1.0); // Triadic
                        Color color3 = Color.hsb((hue + 240) % 360, 0.8, 1.0);

                        String web1 = toHex(color1);
                        String web2 = toHex(color2);
                        String web3 = toHex(color3);

                        // Animated Gradient Border
                        rootPane.setStyle(String.format(
                                "-fx-background-color: linear-gradient(from 0%% 0%% to 100%% 100%%, %s, %s, %s); -fx-background-radius: 50; -fx-padding: 4;",
                                web1, web2, web3));

                        // Animated Inner Background (Deep, expensive fabric look)
                        if (splashScene.lookup("#innerContainer") != null) {
                            Color darkBase = Color.hsb(hue, 0.9, 0.1);
                            Color darkAccent = Color.hsb((hue + 60) % 360, 0.7, 0.2);
                            splashScene.lookup("#innerContainer").setStyle(String.format(
                                    "-fx-background-color: linear-gradient(to bottom right, %s, %s); -fx-background-radius: 46;",
                                    toHex(darkBase), toHex(darkAccent)));
                        }

                        // 2. Thread & Needle Logic
                        if (gc != null && particleCanvas != null) {
                            gc.clearRect(0, 0, particleCanvas.getWidth(), particleCanvas.getHeight());

                            // Spawn Threads (Fiber scraps)
                            if (random.nextDouble() < 0.3) { // High density
                                double startX = random.nextDouble() * particleCanvas.getWidth();
                                double startY = random.nextDouble() * particleCanvas.getHeight();
                                Color threadColor = Color.hsb((hue + random.nextInt(90)) % 360, 0.6, 1.0); // Pastel/Light
                                                                                                           // threads
                                threads.add(new ThreadScrap(startX, startY, threadColor));
                            }

                            // Update and Draw Threads
                            java.util.Iterator<ThreadScrap> it = threads.iterator();
                            while (it.hasNext()) {
                                ThreadScrap t = it.next();
                                t.update();
                                if (!t.isAlive()) {
                                    it.remove();
                                } else {
                                    t.draw(gc);
                                }
                            }

                            // Needle Effect (Zipping across)
                            if (!needleActive && random.nextDouble() < 0.01) { // Rare event
                                needleActive = true;
                                needleX = (random.nextBoolean() ? 0 : particleCanvas.getWidth());
                                needleY = random.nextDouble() * particleCanvas.getHeight();
                                needleTargetX = particleCanvas.getWidth() - needleX;
                                needleTargetY = random.nextDouble() * particleCanvas.getHeight();
                            }

                            if (needleActive) {
                                gc.setStroke(Color.WHITE);
                                gc.setLineWidth(2);
                                gc.setGlobalAlpha(0.8);
                                gc.strokeLine(needleX, needleY, needleX + (needleTargetX - needleX) * 0.1,
                                        needleY + (needleTargetY - needleY) * 0.1); // Trail

                                // Move needle
                                double dx = (needleTargetX - needleX) * 0.2;
                                double dy = (needleTargetY - needleY) * 0.2;
                                needleX += dx;
                                needleY += dy;

                                if (Math.abs(dx) < 1 && Math.abs(dy) < 1) {
                                    needleActive = false;
                                }
                                gc.setGlobalAlpha(1.0);
                            }
                        }

                        lastUpdate = now;
                    }
                }
            };
            visualEffects.start();
        }

        // --- ANIMATION: Logo Float ---
        javafx.scene.Node logoNode = splashScene.lookup("#logoImage");
        if (logoNode != null) {
            javafx.animation.TranslateTransition floatAnim = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.millis(2000), logoNode);
            floatAnim.setByY(-8);
            floatAnim.setCycleCount(javafx.animation.Animation.INDEFINITE);
            floatAnim.setAutoReverse(true);
            floatAnim.play();
        }

        // 3. Create Background Task
        final FXMLLoader mainLoader = new FXMLLoader(Main.class.getResource("/shell.fxml"));

        Task<Parent> properties = new Task<Parent>() {
            @Override
            protected Parent call() throws Exception {
                // Step 1: Init Database
                updateMessage("Conectando con Servidor de Base de Datos...");
                updateProgress(0.1, 1.0);
                Thread.sleep(600); // Cinematic Delay

                if (ConexionDB.getConnection() != null) {
                    System.out.println("Base de datos conectada 🟢");
                } else {
                    updateMessage("Error conectando BD (Continuando Offline)...");
                    Thread.sleep(1000);
                }

                // Step 2: Load FXML Resources
                updateMessage("Cargando Interfaz de Usuario...");
                updateProgress(0.4, 1.0);

                // Heavy lifting here
                Parent mainRoot = mainLoader.load();

                updateMessage("Preparando Motor de Diseño (Pre-carga)...");
                updateProgress(0.8, 1.0);
                org.example.controller.ShellController.preWarmPoolSync(2);

                updateMessage("Listo.");
                updateProgress(1.0, 1.0);
                Thread.sleep(300);

                return mainRoot;
            }
        };

        // 4. Bind UI to Task
        if (bar != null) {
            bar.progressProperty().bind(properties.progressProperty());
        }
        if (status != null) {
            status.textProperty().bind(properties.messageProperty());
        }

        // 5. Completion Handler
        properties.setOnSucceeded(e -> {
            try {
                Parent mainRoot = properties.getValue();
                Scene mainScene = new Scene(mainRoot);

                // GLOBAL STYLES: This is CRITICAL to resolve Modena variable lookups correctly
                // across all windows/popups
                mainScene.getStylesheets().add(Main.class.getResource("/styles.css").toExternalForm());

                primaryStage.setTitle("PALANT - Sistema de Pedidos v2.8.0");
                primaryStage.setScene(mainScene);

                // Icon for Main Stage (Optimized)
                try {
                    javafx.scene.image.Image logo = org.example.utils.UIFactory.getAppLogo();
                    if (logo != null) {
                        primaryStage.getIcons().add(logo);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Handle Close Request (Unsaved Changes Warning)
                primaryStage.setOnCloseRequest(event -> {
                    // PedidoController controller = mainLoader.getController();
                    // if (controller != null) {
                    //     if (!controller.confirmarCierreVentana()) {
                    //         event.consume(); // Cancel close
                    //     }
                    // }
                });

                // Set Undecorated for Custom Title Bar
                primaryStage.initStyle(StageStyle.UNDECORATED);

                primaryStage.setResizable(true);

                // Initial suggested size (fallback)
                primaryStage.setWidth(1280);
                primaryStage.setHeight(850);
                primaryStage.centerOnScreen();

                // INITIAL STATE: Maximized respecting Taskbar (Visual Bounds)
                javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                primaryStage.setX(bounds.getMinX());
                primaryStage.setY(bounds.getMinY());
                primaryStage.setWidth(bounds.getWidth());
                primaryStage.setHeight(bounds.getHeight());

                // Show Main Stage BEHIND the splash first
                primaryStage.show();
                primaryStage.toBack();

                // Force Focus Strategy
                primaryStage.setAlwaysOnTop(true);
                primaryStage.setAlwaysOnTop(false);
                primaryStage.requestFocus();
                primaryStage.toFront();

                // Smooth Fade Out of Splash
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(800), splashRoot);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> splashStage.close());
                fadeOut.play();

            } catch (Exception ex) {
                ex.printStackTrace();
                splashStage.close(); // Ensure closure on error
            }
        });

        properties.setOnFailed(e -> {
            splashStage.close();
            properties.getException().printStackTrace();
            // Fallback?
        });

        // 6. Run Task in new Thread
        new Thread(properties).start();
    }

    public static void main(String[] args) {
        // Enable Hardware Acceleration for better performance in v2.8.0
        System.setProperty("prism.d3d", "true");
        System.setProperty("prism.order", "d3d,sw");
        launch();
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    // --- INNER CLASS: Logic for Floating Threads (Textile Theme) ---
    private static class ThreadScrap {
        double x, y;
        double vx, vy;
        double length;
        double angle;
        double angleVel;
        double life = 1.0;
        double decay;
        Color color;
        double width;

        ThreadScrap(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;

            // Float gently
            this.vx = (Math.random() - 0.5) * 2.0;
            this.vy = (Math.random() - 0.5) * 2.0;

            this.length = 10 + Math.random() * 20;
            this.angle = Math.random() * 360;
            this.angleVel = (Math.random() - 0.5) * 10; // Spin

            this.decay = 0.005 + Math.random() * 0.01; // Slower decay than particles
            this.width = 0.5 + Math.random(); // Thin threads
        }

        void update() {
            x += vx;
            y += vy;
            angle += angleVel;
            life -= decay;
        }

        void draw(javafx.scene.canvas.GraphicsContext gc) {
            gc.setGlobalAlpha(life * 0.6); // Slightly transparent
            gc.setStroke(color);
            gc.setLineWidth(width);

            // Draw as a small curve (QuadCurve)
            gc.beginPath();
            double endX = x + Math.cos(Math.toRadians(angle)) * length;
            double endY = y + Math.sin(Math.toRadians(angle)) * length;
            double ctrlX = x + (Math.random() - 0.5) * 5; // Jittery thread
            double ctrlY = y + (Math.random() - 0.5) * 5;

            gc.moveTo(x, y);
            gc.quadraticCurveTo(ctrlX, ctrlY, endX, endY);
            gc.stroke();
            gc.closePath();

            gc.setGlobalAlpha(1.0);
        }

        boolean isAlive() {
            return life > 0;
        }
    }
}

