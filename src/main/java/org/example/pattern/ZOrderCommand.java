package org.example.pattern;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import org.example.component.UserLayerManager;

public class ZOrderCommand implements ICommand {
    private final Node target;
    private final int oldIndex;
    private final int newIndex;
    private final Group parent;
    private final UserLayerManager layerManager; // Optional, for sync if needed

    public ZOrderCommand(Node target, int oldIndex, int newIndex) {
        this.target = target;
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
        this.parent = (Group) target.getParent();
        this.layerManager = null;
    }

    public ZOrderCommand(UserLayerManager manager, Node target, int oldIndex, int newIndex) {
        this.target = target;
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
        this.parent = (Group) target.getParent();
        this.layerManager = manager;
    }

    @Override
    public void execute() {
        if (parent == null)
            return;
        ObservableList<Node> children = parent.getChildren();

        // Remove and Re-insert
        // Note: Indices might shift if we remove first.
        // Robust way: Remove, then insert at index.
        // But we must check bounds.

        if (newIndex >= 0 && newIndex <= children.size()) {
            children.remove(target);
            // If newIndex was greater than oldIndex, it might need adjustment if we remove
            // first?
            // Actually, the "newIndex" should probably be the index *after* the move.
            // But standard JavaFX Lists handle this.
            // Simpler: Use swap if it's just adjacent?
            // No, bringToFront moves to end.

            int targetIdx = newIndex;
            if (targetIdx > children.size())
                targetIdx = children.size();

            children.add(targetIdx, target);
        }
    }

    @Override
    public void undo() {
        apply(oldIndex);
    }

    @Override
    public void redo() {
        apply(newIndex);
    }

    private void apply(int index) {
        if (parent == null)
            return;
        ObservableList<Node> children = parent.getChildren();
        if (children.contains(target)) {
            children.remove(target);
            int idx = index;
            if (idx > children.size())
                idx = children.size();
            children.add(idx, target);
        }
    }

    @Override
    public String getName() {
        return "Orden de Capas";
    }

    @Override
    public String getContextZone() {
        return null;
    }
}

