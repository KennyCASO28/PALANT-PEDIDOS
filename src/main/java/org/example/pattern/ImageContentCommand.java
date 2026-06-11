package org.example.pattern;

import javafx.scene.image.Image;
import org.example.component.ImageLayer;
import org.example.component.helper.PrendaEditModeManager;

public class ImageContentCommand implements ICommand {
    private final ImageLayer target;
    private final Image oldImage;
    private final Image newImage;
    private final double oldWidth, oldHeight;
    private final double newWidth, newHeight;
    private final double oldTx, oldTy;
    private final double newTx, newTy;
    private final String name;

    public ImageContentCommand(ImageLayer target, String name,
            Image oldImage, double oldW, double oldH, double oldTx, double oldTy,
            Image newImage, double newW, double newH, double newTx, double newTy) {
        this.target = target;
        this.name = name;
        this.oldImage = oldImage;
        this.oldWidth = oldW;
        this.oldHeight = oldH;
        this.oldTx = oldTx;
        this.oldTy = oldTy;

        this.newImage = newImage;
        this.newWidth = newW;
        this.newHeight = newH;
        this.newTx = newTx;
        this.newTy = newTy;
    }

    @Override
    public void execute() {
        // Redo logic is identical to first execution for this type
        apply(newImage, newWidth, newHeight, newTx, newTy);
    }

    @Override
    public void undo() {
        apply(oldImage, oldWidth, oldHeight, oldTx, oldTy);
    }

    @Override
    public void redo() {
        apply(newImage, newWidth, newHeight, newTx, newTy);
    }

    private void apply(Image img, double w, double h, double tx, double ty) {
        if (target == null)
            return;
        target.setImage(img);
        target.setDimensions(w, h); // This needs to be accessible!
        target.setTranslateX(tx);
        target.setTranslateY(ty);
        target.updateVisuals();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContextZone() {
        return target.getActiveZone();
    }
}

