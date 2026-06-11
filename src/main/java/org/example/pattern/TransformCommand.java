package org.example.pattern;

import javafx.scene.Node;
import org.example.component.ShapeLayer;
import org.example.component.ImageLayer;

public class TransformCommand implements ICommand {
    private final Node target;
    private final NodeMemento beforeState;
    private final NodeMemento afterState;
    private final String contextZone;

    // Legacy constructors mapping to Mementos
    public TransformCommand(Node target,
            double oldX, double oldY, double oldScaleX, double oldScaleY, double oldRotate,
            double newX, double newY, double newScaleX, double newScaleY, double newRotate) {
        this(target, oldX, oldY, oldScaleX, oldScaleY, oldRotate, null, null, null, null,
                newX, newY, newScaleX, newScaleY, newRotate, null, null, null, null, null);
    }

    public TransformCommand(Node target,
            double oldX, double oldY, double oldScaleX, double oldScaleY, double oldRotate, Double oldW, Double oldH,
            double newX, double newY, double newScaleX, double newScaleY, double newRotate, Double newW, Double newH,
            String contextZone) {
        this(target, oldX, oldY, oldScaleX, oldScaleY, oldRotate, oldW, oldH, null, null,
                newX, newY, newScaleX, newScaleY, newRotate, newW, newH, null, null, contextZone);
    }

    public TransformCommand(Node target,
            double oldX, double oldY, double oldScaleX, double oldScaleY, double oldRotate,
            Double oldW, Double oldH, Double oldMx, Double oldMy,
            double newX, double newY, double newScaleX, double newScaleY, double newRotate,
            Double newW, Double newH, Double newMx, Double newMy,
            String contextZone) {
        this.target = target;
        this.contextZone = contextZone;

        // 1. Capture base state structure
        this.beforeState = new NodeMemento(target);
        this.afterState = new NodeMemento(target);

        // 2. Explicitly override dimensions and transforms instead of applying them to
        // the live Node
        this.beforeState.overrideTransforms(oldX, oldY, oldScaleX, oldScaleY, oldRotate, oldW, oldH, oldMx, oldMy);
        this.afterState.overrideTransforms(newX, newY, newScaleX, newScaleY, newRotate, newW, newH, newMx, newMy);
    }

    // New exact constructor
    public TransformCommand(Node target, NodeMemento beforeState, NodeMemento afterState, String contextZone) {
        this.target = target;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.contextZone = contextZone;
    }

    @Override
    public void execute() {
        if (afterState != null) {
            afterState.restore();
        }
    }

    @Override
    public void undo() {
        if (beforeState != null) {
            beforeState.restore();
        }
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String getName() {
        return "Transformar Objeto";
    }

    @Override
    public String getContextZone() {
        return contextZone;
    }
}

