package org.example.component.helper;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.example.component.ImageLayer;

/**
 * Handles Global Clipboard operations for ImageLayers.
 * Migrated from ImageLayer static methods.
 */
public class ImageClipboardSupport {

    private static ImageLayer clipboardLayer = null;
    private static boolean isCut = false;

    public static void copy(ImageLayer layer) {
        clipboardLayer = layer;
        isCut = false;
    }

    public static void cut(ImageLayer layer) {
        clipboardLayer = layer;
        isCut = true;
    }

    public static boolean hasClipboard() {
        return clipboardLayer != null;
    }

    public static void clear() {
        clipboardLayer = null;
    }

    public static ImageLayer getClipboardCopy() {
        if (clipboardLayer == null) return null;

        // 1. Snapshot the CURRENT state
        WritableImage snap = clipboardLayer.snapshotCanvas();
        if (snap == null) return null;

        // 2. Initialize copy with the SNAPSHOT
        ImageLayer copy = new ImageLayer(snap);

        // 4. Match Visual Size (Do this BEFORE copying scales so it doesn't overwrite them)
        copy.setSize(clipboardLayer.getWidth(), clipboardLayer.getHeight());

        copy.updatePivotWithCompensation(clipboardLayer.getCustomPivotX(), clipboardLayer.getCustomPivotY());

        // 3. Copy Properties
        copy.setTranslateX(clipboardLayer.getTranslateX());
        copy.setTranslateY(clipboardLayer.getTranslateY());
        copy.setInternalRotation(clipboardLayer.getInternalRotation());
        copy.setInternalScaleX(clipboardLayer.getInternalScaleX());
        copy.setInternalScaleY(clipboardLayer.getInternalScaleY());
        copy.getShearTransform().setX(clipboardLayer.getShearTransform().getX());
        copy.getShearTransform().setY(clipboardLayer.getShearTransform().getY());
        
        // Match State (Legacy fields)
        if (clipboardLayer.getActiveZone() != null) {
            copy.setActiveZone(clipboardLayer.getActiveZone());
        }

        return copy;
    }
    
    public static boolean isCut() {
        return isCut;
    }
}
