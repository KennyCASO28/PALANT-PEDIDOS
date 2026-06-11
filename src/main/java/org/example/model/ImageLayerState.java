package org.example.model;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * State container for ImageLayer.
 * Separates data from visual representation.
 */
public class ImageLayerState {

    // Core Dimensions
    public double width = 150;
    public double height = 150;

    // Transforms
    public double rotation = 0;
    public double scaleX = 1;
    public double scaleY = 1;
    public double shearX = 0;
    public double shearY = 0;

    // Effects (ColorAdjust)
    public double brightness = 0;
    public double contrast = 0;
    public double saturation = 0;

    // Locking & Visibility
    public boolean isLocked = false;
    public boolean isUserLocked = false;
    public String activeZone = null;

    // Image Content
    public Image originalImage;
    public Image currentImage;
    public String base64Content;

    // Transient / Editor State
    public boolean snapshotDirty = false;
    public boolean isModified = false;
    public boolean isCropMode = false;
    public boolean isRotationMode = false;
    public double pivotX = -1, pivotY = -1;
    public double cropX = 0, cropY = 0, cropW = 0, cropH = 0;
    public double magicWandTolerance = 0.15;

    // Metadata
    public TipoEscudo badgeType = TipoEscudo.NINGUNO;

    public ImageLayerState() {
    }
}
