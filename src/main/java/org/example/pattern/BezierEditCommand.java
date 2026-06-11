package org.example.pattern;

import org.example.component.ShapeLayer;
import org.example.model.BezierNode;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point2D;

public class BezierEditCommand implements ICommand {
    private final ShapeLayer target;
    private final List<BezierNode> oldNodes;
    private final List<BezierNode> newNodes;
    private final String contextZone;
    private final Runnable onUpdate;

    public BezierEditCommand(ShapeLayer target, List<BezierNode> oldNodes, List<BezierNode> newNodes,
            String contextZone, Runnable onUpdate) {
        this.target = target;
        this.oldNodes = cloneNodes(oldNodes);
        this.newNodes = cloneNodes(newNodes);
        this.contextZone = contextZone;
        this.onUpdate = onUpdate;
    }

    public BezierEditCommand(ShapeLayer target, List<BezierNode> oldNodes, List<BezierNode> newNodes,
            String contextZone) {
        this(target, oldNodes, newNodes, contextZone, null);
    }

    private List<BezierNode> cloneNodes(List<BezierNode> source) {
        List<BezierNode> clone = new ArrayList<>();
        for (BezierNode n : source) {
            clone.add(n.copy());
        }
        return clone;
    }

    @Override
    public void execute() {
        target.setBezierNodes(cloneNodes(newNodes));
        target.refreshPath(); // Refresh the visual SVG path
        if (onUpdate != null) onUpdate.run();
    }

    @Override
    public void undo() {
        target.setBezierNodes(cloneNodes(oldNodes));
        target.refreshPath();
        if (onUpdate != null) onUpdate.run();
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String getName() {
        return "Edit Path Nodes";
    }

    @Override
    public String getContextZone() {
        return contextZone;
    }
}

