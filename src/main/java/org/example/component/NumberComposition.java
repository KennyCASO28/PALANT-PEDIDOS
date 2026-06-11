package org.example.component;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Optimized Number Composition for Palant Pedidos.
 * Manual drag and resize have been removed per user request to maintain factory alignment integrity.
 */
public class NumberComposition {
    private final javafx.scene.Group root = new javafx.scene.Group();
    private final SVGPath[] layers = new SVGPath[5];
    private final javafx.scene.text.Text nameText = new javafx.scene.text.Text();

    // State for persistence
    private final java.util.Map<Integer, Color> layerColors = new java.util.HashMap<>();
    private boolean selected = false;

    public NumberComposition() {
        // Initialize Layers
        for (int i = 0; i < 5; i++) {
            layers[i] = new SVGPath();
            layers[i].setStroke(null);
            
            Color initialFill = Color.BLACK;
            if (i == 1) initialFill = Color.web("#333333");
            else if (i == 2 || i == 4) initialFill = Color.WHITE;
            
            layers[i].setFill(initialFill);
            layerColors.put(i, initialFill);
            layers[i].setCache(false);
            root.getChildren().add(layers[i]);
            
            // Still allow selection to target the color picker, but no visual handles per request
            layers[i].setOnMouseClicked(e -> {
                setSelected(true);
                e.consume();
            });
        }
        
        root.setId("NUMBER_ROOT");
        root.setUserData(this);

        nameText.setStyle("-fx-font-weight: bold;");
        nameText.setFill(Color.BLACK);
        nameText.setVisible(false);
        nameText.setMouseTransparent(true);
        root.getChildren().add(nameText);

        root.setVisible(false);
        root.setCache(false);
        root.setViewOrder(-100.0);
        root.setDepthTest(javafx.scene.DepthTest.DISABLE);
        root.setPickOnBounds(false);
    }

    public boolean isSelected() { return selected; }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public javafx.scene.Group getRoot() { return root; }

    public void setVisible(boolean visible) {
        if (root.isVisible() != visible) {
            root.setVisible(visible);
        }
    }

    public boolean isVisible() {
        return root.isVisible();
    }

    public void setLayerPath(int index, String pathContent) {
        if (index >= 0 && index < 5) {
            String current = layers[index].getContent();
            if (pathContent == null) pathContent = "";
            if (!pathContent.equals(current)) {
                layers[index].setContent(pathContent);
            }
        }
    }

    public void setLayerColor(int index, Color color) {
        if (index >= 0 && index < 5) {
            Color current = layerColors.get(index);
            if (color != null && !color.equals(current)) {
                layerColors.put(index, color);
                layers[index].setFill(color);
            }
        }
    }

    public boolean isLayerColorSet(int index) { return layerColors.containsKey(index); }
    public Color getLayerColor(int index) { return layerColors.get(index); }

    public void setPosition(double x, double y, double scale) {
        if (root.getLayoutX() != x) root.setLayoutX(x);
        if (root.getLayoutY() != y) root.setLayoutY(y);
        root.setTranslateX(0);
        root.setTranslateY(0);
        if (root.getScaleX() != scale) {
            root.setScaleX(scale);
            root.setScaleY(scale);
        }
    }

    public void clear() {
        for (int i = 0; i < 5; i++) {
            setLayerPath(i, "");
        }
        nameText.setText("");
        nameText.setVisible(false);
    }

    public void resetColorsToDefault() {
        layerColors.clear();
        for (int i = 0; i < 5; i++) {
            Color initialFill = Color.BLACK;
            if (i == 1) initialFill = Color.web("#333333");
            else if (i == 2 || i == 4) initialFill = Color.WHITE;
            setLayerColor(i, initialFill);
        }
    }
}
