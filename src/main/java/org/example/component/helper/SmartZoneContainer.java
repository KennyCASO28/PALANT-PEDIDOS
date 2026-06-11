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

        // Config Border (Visual only)
        borderShape.setFill(Color.TRANSPARENT);
        borderShape.setStroke(Color.web("#3498db")); // Azul moderno para edición
        borderShape.setStrokeWidth(1.2); // Delgado
        borderShape.getStrokeDashArray().setAll(6.0, 4.0); // Punteado
        borderShape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND); // Guiones redondeados
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

    /**
     * Enters "Edit Content" mode.
     * - Disables Clip.
     * - Shows Red Border.
     * - Unlocks Content.
     */
    public void setEditMode(boolean enable) {
        this.isEditing = enable;

        // 1. Toggle Border
        borderShape.setVisible(enable);

        // 2. Toggle Clip
        refreshClipState();

        // 3. Toggle Interactivity (Lockout when not editing)
        contentGroup.setMouseTransparent(!enable);

        // 4. Update Children Lock State (Visual feedback mostly)
        for (Node n : contentGroup.getChildren()) {
            updateItemState(n);
        }
    }

    public boolean isEditing() {
        return isEditing;
    }

    private void refreshClipState() {
        if (isEditing) {
            contentGroup.setClip(null); // Unclipped to see "outside" parts
        } else {
            // Re-use current clip shape logic
            // We need a COPY for the clip node because a Node can't be added twice or used
            // as clip if in scene?
            // Actually reusing the same SVGPath object as clip allows dynamic updates IF
            // it's not in the scene graph as a child.
            // Our 'clipShape' is NOT added to children, so it's safe to use as clip.
            contentGroup.setClip(clipShape);
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
