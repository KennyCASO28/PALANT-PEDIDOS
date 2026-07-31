package org.example.component.helper;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.example.component.ImageLayer;
import org.example.component.ShapeLayer;
import org.example.component.TextLayer;
import org.example.component.GroupLayer;
import org.example.component.GroupLayerV2;

/**
 * Intelligent Container for Garment Zones (Corel PowerClip style).
 * 
 * Responsibilities:
 * 1. Holds content (Images/Shapes) for a specific Zone (e.g. PECHO).
 * 2. Applies a strict Clip Mask based on the zone shape.
 * 3. Manages "Edit Content" Mode:
 * - Removes Clip.
 * - Shows Red Border (Limit).
 * - Unlocks items for editing.
 * 4. Manages "Finish Edit":
 * - Re-applies Clip.
 * - Hides Border.
 * - Locks items strictly.
 */
public class SmartZoneContainer extends Group {

    private final String zoneId;
    private final Group contentGroup;
    private final SVGPath borderShape; // Visual feedback during edit
    private final SVGPath clipShape; // The actual mask
    private final SVGPath backgroundShape; // Semi-transparent fill highlight for active editing zone

    private boolean isEditing = false;

    public SmartZoneContainer(String zoneId) {
        this.zoneId = zoneId;

        // 1. Content Group (Where items live)
        this.contentGroup = new Group();
        this.contentGroup.setPickOnBounds(false);
        this.contentGroup.setMouseTransparent(true); // Default Locked (Must enter Edit Mode to interact)
        this.getChildren().add(contentGroup);

        this.setPickOnBounds(false);

        // 2. Aux Shapes
        this.clipShape = new SVGPath();
        this.borderShape = new SVGPath();
        this.backgroundShape = new SVGPath();

        backgroundShape.setFill(Color.TRANSPARENT);
        backgroundShape.setStroke(Color.TRANSPARENT);
        backgroundShape.setMouseTransparent(true);
        backgroundShape.setVisible(false);
        this.getChildren().add(0, backgroundShape);

        // Config Border (Visual only - Solid Thin Blue Line)
        borderShape.setFill(Color.TRANSPARENT);
        borderShape.setStroke(Color.web("#00A8FF")); // Azul notorio y vibrante
        borderShape.setStrokeWidth(1.0); // Línea fina y precisa
        borderShape.getStrokeDashArray().clear(); // Línea sólida
        borderShape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        borderShape.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        borderShape.setMouseTransparent(true);
        borderShape.setVisible(false); // Hidden by default

        // Add border on top of content
        this.getChildren().add(borderShape);
    }

    /**
     * Updates the geometry path for this zone.
     * Must be called when the garment type/size changes.
     */
    public void setZonePathContent(String svgContent) {
        if (svgContent == null || svgContent.isEmpty())
            return;

        clipShape.setContent(svgContent);
        borderShape.setContent(svgContent);
        backgroundShape.setContent(svgContent);

        refreshClipState();
    }

    /**
     * Adds an item to this container.
     * The item is automatically locked if not editing.
     */
    public void addItem(Node item) {
        if (item == null)
            return;

        // Ensure not already added
        if (!contentGroup.getChildren().contains(item)) {
            contentGroup.getChildren().add(item);
        }

        updateItemState(item);
    }

    /**
     * Remove item if needed (deletion).
     */
    public void removeItem(Node item) {
        contentGroup.getChildren().remove(item);
    }

    public javafx.scene.shape.SVGPath getBorderShape() {
        return borderShape;
    }

    /**
     * Enters "Edit Content" mode.
     * - Disables Clip.
     * - Shows Solid Border & Translucent Highlight Fill.
     * - Unlocks Content for moving and editing.
     */
    public void setEditMode(boolean enable) {
        this.isEditing = enable;

        // 1. Toggle Border (Show ONLY solid cyan border line around active zone piece, NO background fill highlight)
        borderShape.setVisible(enable);
        backgroundShape.setFill(Color.TRANSPARENT);
        backgroundShape.setVisible(false);

        // 2. Toggle Clip
        refreshClipState();

        // 3. Toggle Interactivity (Allow interaction when editing, lock when finished)
        contentGroup.setMouseTransparent(!enable);

        // 4. Update Children Lock State (Unlock items when editing)
        for (Node n : contentGroup.getChildren()) {
            updateItemState(n);
        }
    }

    public boolean isEditing() {
        return isEditing;
    }

    private void refreshClipState() {
        if (isEditing) {
            contentGroup.setClip(null); // Unclipped during PowerClip edit mode to move/transform items freely
        } else {
            contentGroup.setClip(clipShape); // Recortados exactamente a la pieza al hacer clic en LISTO
        }
    }

    public void removeItemState(Node node) {
        if (node instanceof ShapeLayer) {
            ((ShapeLayer) node).setSystemLocked(false);
            ((ShapeLayer) node).setIsBeingEdited(false);
        } else if (node instanceof ImageLayer) {
            ((ImageLayer) node).setSystemLocked(false);
            ((ImageLayer) node).setIsBeingEdited(false);
        } else if (node instanceof TextLayer) {
            ((TextLayer) node).setSystemLocked(false);
            ((TextLayer) node).setBeingEdited(false);
        } else if (node instanceof Group) {
            if (node instanceof GroupLayer) {
                ((GroupLayer) node).setSystemLocked(false);
                ((GroupLayer) node).setIsBeingEdited(false);
            } else if (node instanceof GroupLayerV2) {
                ((GroupLayerV2) node).setSystemLocked(false);
                ((GroupLayerV2) node).setSelected(false);
            } else {
                for (Node child : ((Group) node).getChildren()) {
                    removeItemState(child);
                }
            }
        }
    }

    public void updateItemState(Node node) {
        boolean locked = !isEditing;

        // Logic for Locking:
        // If LOCKED -> We disable interactions via
        // contentGroup.setMouseTransparent(true).
        // So individual children don't strictly need SystemLock, but we keep it
        // in case logic depends on it elsewhere.

        if (node instanceof ShapeLayer) {
            ((ShapeLayer) node).setSystemLocked(locked);
            ((ShapeLayer) node).setIsBeingEdited(isEditing);
        } else if (node instanceof ImageLayer) {
            ((ImageLayer) node).setSystemLocked(locked);
            ((ImageLayer) node).setIsBeingEdited(isEditing);
        } else if (node instanceof TextLayer) {
            ((TextLayer) node).setSystemLocked(locked);
            ((TextLayer) node).setBeingEdited(isEditing);
        } else if (node instanceof Group) {
            // Check for known GroupLayer types to sync state without recursing into handles
            if (node instanceof GroupLayer) {
                ((GroupLayer) node).setSystemLocked(locked);
                ((GroupLayer) node).setIsBeingEdited(isEditing);
            } else if (node instanceof GroupLayerV2) {
                ((GroupLayerV2) node).setSystemLocked(locked);
                ((GroupLayerV2) node).setSelected(isEditing && ((GroupLayerV2) node).isSelected());
            } else {
                // Generic group (recursive)
                for (Node child : ((Group) node).getChildren()) {
                    updateItemState(child);
                }
            }
        }

        // Removed: node.setMouseTransparent(false);
        // We control this at the contentGroup level now.
    }

    public Group getContentGroup() {
        return contentGroup;
    }

    public String getZoneId() {
        return zoneId;
    }
}
