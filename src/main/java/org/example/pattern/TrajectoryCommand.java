package org.example.pattern;

import org.example.component.TextLayer;
import org.example.model.TrajectoryPath;

/**
 * Command to undo/redo changes to a text's trajectory path.
 */
public class TrajectoryCommand implements ICommand {
    private final TextLayer layer;
    private final TrajectoryPath beforeState;
    private final TrajectoryPath afterState;

    public TrajectoryCommand(TextLayer layer, TrajectoryPath before, TrajectoryPath after) {
        this.layer = layer;
        this.beforeState = before.copy();
        this.afterState = after.copy();
    }

    @Override
    public void execute() {
        // Redo behavior
        layer.getTrajectory().setFrom(afterState);
        layer.renderText();
    }

    @Override
    public void undo() {
        layer.getTrajectory().setFrom(beforeState);
        layer.renderText();
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String getName() {
        return "Ajustar Trayectoria";
    }
}
