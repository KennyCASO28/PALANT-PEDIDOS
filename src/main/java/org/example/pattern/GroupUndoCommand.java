package org.example.pattern;

import java.util.List;
import java.util.ArrayList;

public class GroupUndoCommand implements ICommand {
    private final String name;
    private final List<ICommand> subCommands;
    private final String contextZone;

    public GroupUndoCommand(String name, String contextZone) {
        this.name = name;
        this.contextZone = contextZone;
        this.subCommands = new ArrayList<>();
    }

    public void addCommand(ICommand cmd) {
        subCommands.add(cmd);
    }

    @Override
    public void execute() {
        // Typically already executed during interaction
    }

    @Override
    public void undo() {
        // Reverse order for Undo
        for (int i = subCommands.size() - 1; i >= 0; i--) {
            subCommands.get(i).undo();
        }
    }

    @Override
    public void redo() {
        // Normal order for Redo
        for (ICommand cmd : subCommands) {
            cmd.redo();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContextZone() {
        return contextZone;
    }
}

