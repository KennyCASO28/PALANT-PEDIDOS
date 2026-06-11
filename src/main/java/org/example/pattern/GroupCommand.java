package org.example.pattern;

import javafx.scene.Node;
import org.example.component.UserLayerManager;
import java.util.List;
import java.util.ArrayList;

public class GroupCommand implements ICommand {
    public enum ActionType {
        GROUP, UNGROUP
    }

    private final UserLayerManager manager;
    private final ActionType type;
    private final Node groupNode;
    private final List<Node> children;
    private final String contextZone;

    // Mementos
    private final List<NodeMemento> childrenBeforeState = new ArrayList<>();
    private final List<NodeMemento> childrenAfterState = new ArrayList<>();
    private final NodeMemento groupBeforeState;
    private final NodeMemento groupAfterState;

    public GroupCommand(UserLayerManager manager, Node groupNode, List<Node> children, ActionType type,
            List<NodeMemento> childrenBeforeState, NodeMemento groupBeforeState, String contextZone) {
        this.manager = manager;
        this.groupNode = groupNode;
        this.children = new ArrayList<>(children);
        this.type = type;
        this.contextZone = contextZone;

        if (childrenBeforeState != null) {
            this.childrenBeforeState.addAll(childrenBeforeState);
        }
        this.groupBeforeState = groupBeforeState;

        // Capture AFTER states now that the action is done
        for (Node c : this.children) {
            this.childrenAfterState.add(new NodeMemento(c));
        }
        if (this.groupNode != null) {
            this.groupAfterState = new NodeMemento(this.groupNode);
        } else {
            this.groupAfterState = null;
        }
    }

    @Override
    public void execute() {
        // execute() is usually called by the invoker, but we provide it for redo()
        boolean wasHistory = manager.isPerformingHistoryAction();
        manager.setPerformingHistoryAction(true);
        manager.suspendNotifications();
        try {
            if (type == ActionType.GROUP) {
                if (groupAfterState != null)
                    groupAfterState.restore();
                if (groupNode != null)
                    manager.trackRestoredLayer(groupNode);
                for (NodeMemento m : childrenAfterState)
                    m.restore();
                manager.selectNode(groupNode);
            } else {
                for (NodeMemento m : childrenAfterState)
                    m.restore();
                if (groupNode.getParent() instanceof javafx.scene.Group) {
                    ((javafx.scene.Group) groupNode.getParent()).getChildren().remove(groupNode);
                }
                manager.clearSelection();
            }
        } finally {
            manager.setPerformingHistoryAction(wasHistory);
            manager.resumeNotifications();
        }
    }

    @Override
    public void undo() {
        boolean wasHistory = manager.isPerformingHistoryAction();
        manager.setPerformingHistoryAction(true);
        manager.suspendNotifications();
        try {
            if (type == ActionType.GROUP) {
                // Undo Group = Restore individual children exactly
                for (NodeMemento m : childrenBeforeState)
                    m.restore();
                if (groupNode != null && groupNode.getParent() instanceof javafx.scene.Group) {
                    ((javafx.scene.Group) groupNode.getParent()).getChildren().remove(groupNode);
                }
                manager.clearSelection();
                for (Node c : children)
                    manager.addToSelection(c);
            } else {
                // Undo Ungroup = Restore Group completely
                if (groupBeforeState != null)
                    groupBeforeState.restore();
                if (groupNode != null) {
                    groupNode.setVisible(true);
                    manager.trackRestoredLayer(groupNode);
                }
                for (NodeMemento m : childrenBeforeState)
                    m.restore();
                manager.selectNode(groupNode);
            }
        } finally {
            manager.setPerformingHistoryAction(wasHistory);
            manager.resumeNotifications();
        }
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public String getName() {
        return type == ActionType.GROUP ? "Agrupar Objetos" : "Desagrupar Objetos";
    }

    @Override
    public String getContextZone() {
        return contextZone;
    }
}

