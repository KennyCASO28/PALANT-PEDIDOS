package org.example.controller.helper;

import javafx.geometry.Rectangle2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

public class WindowControlManager {

    private double xOffset = 0;
    private double yOffset = 0;

    private boolean isMaximized = false;
    private double lastX = 100, lastY = 80, lastWidth = 1100, lastHeight = 700;

    private boolean isResizing = false;

    private final String PATH_MAX = "M2,2 H14 V14 H2 V2 Z M3,3 V13 H13 V3 H3 Z";
    private final String PATH_RESTORE = "M4,2 H14 V12 H12 V13 H3 V4 H4 V2 Z M13,3 V11 H5 V3 H13 Z M4,5 V12 H11 V5 H4 Z";

    private static final int RESIZE_MARGIN = 8;
    private javafx.scene.Cursor resizeCursor = javafx.scene.Cursor.DEFAULT;

    private final SVGPath iconWinMax;

    public WindowControlManager(SVGPath iconWinMax) {
        this.iconWinMax = iconWinMax;
    }

    public void initChecks(Stage stage) {
        if (stage == null) return;
        List<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        Rectangle2D visualBounds = screen.getVisualBounds();

        // If window matches visual bounds, we are "maximized"
        if (Math.abs(stage.getWidth() - visualBounds.getWidth()) < 20 &&
            Math.abs(stage.getHeight() - visualBounds.getHeight()) < 20) {
            isMaximized = true;
            if (iconWinMax != null) {
                iconWinMax.setContent(PATH_RESTORE);
            }
        }
    }

    public void toggleMaximize(Stage stage) {
        double centerX = stage.getX() + stage.getWidth() / 2;
        double centerY = stage.getY() + stage.getHeight() / 2;

        List<Screen> screens = Screen.getScreensForRectangle(centerX, centerY, 1, 1);
        Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        Rectangle2D visualBounds = screen.getVisualBounds();

        boolean physicallyMaximized = Math.abs(stage.getWidth() - visualBounds.getWidth()) < 20 &&
                Math.abs(stage.getHeight() - visualBounds.getHeight()) < 20;

        if (physicallyMaximized && !isMaximized) {
            isMaximized = true;
        }

        if (isMaximized) {
            if (lastWidth >= visualBounds.getWidth() - 10 || lastHeight >= visualBounds.getHeight() - 10) {
                lastWidth = Math.min(1100, visualBounds.getWidth() - 40);
                lastHeight = Math.min(700, visualBounds.getHeight() - 40);
                lastX = visualBounds.getMinX() + (visualBounds.getWidth() - lastWidth) / 2;
                lastY = visualBounds.getMinY() + (visualBounds.getHeight() - lastHeight) / 2;
            }

            stage.setX(lastX);
            stage.setY(lastY);
            stage.setWidth(lastWidth);
            stage.setHeight(lastHeight);

            isMaximized = false;
            if (iconWinMax != null) {
                iconWinMax.setContent(PATH_MAX);
            }
        } else {
            if (!physicallyMaximized) {
                lastX = stage.getX();
                lastY = stage.getY();
                lastWidth = stage.getWidth();
                lastHeight = stage.getHeight();
            }

            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());

            isMaximized = true;
            if (iconWinMax != null) {
                iconWinMax.setContent(PATH_RESTORE);
            }
        }
    }

    public void handleWindowDragPressed(MouseEvent event) {
        if (isMaximized) return;
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    public void handleWindowDragDragged(MouseEvent event, Stage stage) {
        if (isResizing) return;

        if (isMaximized) {
            double percX = event.getX() / stage.getWidth();
            isMaximized = false;
            if (iconWinMax != null) {
                iconWinMax.setContent(PATH_MAX);
            }

            stage.setWidth(lastWidth);
            stage.setHeight(lastHeight);

            xOffset = lastWidth * percX;
        }

        List<Screen> screens = Screen.getScreensForRectangle(event.getScreenX(), event.getScreenY(), 1, 1);
        if (screens.isEmpty()) return;

        Screen screen = screens.get(0);
        Rectangle2D visualBounds = screen.getVisualBounds();

        boolean resized = false;
        if (stage.getWidth() > visualBounds.getWidth()) {
            stage.setWidth(visualBounds.getWidth() * 0.9);
            xOffset = stage.getWidth() / 2;
            resized = true;
        }
        if (stage.getHeight() > visualBounds.getHeight()) {
            stage.setHeight(visualBounds.getHeight() * 0.9);
            yOffset = 15;
            resized = true;
        }

        if (resized) {
            lastWidth = stage.getWidth();
            lastHeight = stage.getHeight();
        }

        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    public void handleWindowDragReleased(MouseEvent event, Stage stage) {
        Screen screen = Screen.getScreensForRectangle(event.getScreenX(), event.getScreenY(), 1, 1).get(0);
        Rectangle2D visualBounds = screen.getVisualBounds();

        if (event.getScreenY() <= visualBounds.getMinY() + 60) {
            if (!isMaximized) {
                lastX = stage.getX();
                lastY = stage.getY();
                lastWidth = stage.getWidth();
                lastHeight = stage.getHeight();

                stage.setX(visualBounds.getMinX());
                stage.setY(visualBounds.getMinY());
                stage.setWidth(visualBounds.getWidth());
                stage.setHeight(visualBounds.getHeight());
                isMaximized = true;
                if (iconWinMax != null) {
                    iconWinMax.setContent(PATH_RESTORE);
                }
            }
        }
    }

    public void setupWindowResizing(javafx.scene.Scene scene, Stage stage, javafx.scene.layout.Region rootVBox) {
        // Event filter to capture mouse moves and update cursor
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (isMaximized || isResizing) return;

            double x = e.getSceneX();
            double y = e.getSceneY();
            double width = stage != null ? stage.getWidth() : rootVBox.getWidth();
            double height = stage != null ? stage.getHeight() : rootVBox.getHeight();

            boolean right = x > width - RESIZE_MARGIN;
            boolean left = x < RESIZE_MARGIN;
            boolean bottom = y > height - RESIZE_MARGIN;
            boolean top = y < RESIZE_MARGIN;

            if (right && bottom)
                resizeCursor = javafx.scene.Cursor.SE_RESIZE;
            else if (left && bottom)
                resizeCursor = javafx.scene.Cursor.SW_RESIZE;
            else if (right && top)
                resizeCursor = javafx.scene.Cursor.NE_RESIZE;
            else if (left && top)
                resizeCursor = javafx.scene.Cursor.NW_RESIZE;
            else if (right)
                resizeCursor = javafx.scene.Cursor.E_RESIZE;
            else if (left)
                resizeCursor = javafx.scene.Cursor.W_RESIZE;
            else if (bottom)
                resizeCursor = javafx.scene.Cursor.S_RESIZE;
            else if (top)
                resizeCursor = javafx.scene.Cursor.N_RESIZE;
            else
                resizeCursor = javafx.scene.Cursor.DEFAULT;

            scene.setCursor(resizeCursor);
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (isMaximized) return;

            if (resizeCursor != javafx.scene.Cursor.DEFAULT) {
                isResizing = true;
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (isResizing) {
                isResizing = false;
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!isResizing || isMaximized) return;

            double mouseX = e.getScreenX();
            double mouseY = e.getScreenY();

            if (resizeCursor == javafx.scene.Cursor.E_RESIZE || resizeCursor == javafx.scene.Cursor.SE_RESIZE
                    || resizeCursor == javafx.scene.Cursor.NE_RESIZE) {
                stage.setWidth(Math.max(1024, mouseX - stage.getX()));
            }
            if (resizeCursor == javafx.scene.Cursor.S_RESIZE || resizeCursor == javafx.scene.Cursor.SE_RESIZE
                    || resizeCursor == javafx.scene.Cursor.SW_RESIZE) {
                stage.setHeight(Math.max(650, mouseY - stage.getY()));
            }
            if (resizeCursor == javafx.scene.Cursor.W_RESIZE || resizeCursor == javafx.scene.Cursor.SW_RESIZE
                    || resizeCursor == javafx.scene.Cursor.NW_RESIZE) {
                double oldX = stage.getX();
                double newWidth = Math.max(1024, stage.getWidth() + (oldX - mouseX));
                if (newWidth > 1024) {
                    stage.setX(mouseX);
                    stage.setWidth(newWidth);
                }
            }
            if (resizeCursor == javafx.scene.Cursor.N_RESIZE || resizeCursor == javafx.scene.Cursor.NE_RESIZE
                    || resizeCursor == javafx.scene.Cursor.NW_RESIZE) {
                double oldY = stage.getY();
                double newHeight = Math.max(650, stage.getHeight() + (oldY - mouseY));
                if (newHeight > 650) {
                    stage.setY(mouseY);
                    stage.setHeight(newHeight);
                }
            }

            lastWidth = stage.getWidth();
            lastHeight = stage.getHeight();
            e.consume();
        });
    }
}
