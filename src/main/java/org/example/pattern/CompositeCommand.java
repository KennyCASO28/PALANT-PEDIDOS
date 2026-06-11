package org.example.pattern;

import java.util.ArrayList;
import java.util.List;

public class CompositeCommand implements ICommand {
    private final List<ICommand> commands = new ArrayList<>();
    private final String name;

    public CompositeCommand(String name) {
        this.name = name;
    }

    public void addCommand(ICommand cmd) {
        commands.add(cmd);
    }

    @Override
    public void execute() {
        for (ICommand cmd : commands) {
            cmd.execute();
        }
    }

    @Override
    public void undo() {
        // Undo in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }

    @Override
    public void redo() {
        for (ICommand cmd : commands) {
            cmd.redo();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    public int getCommandCount() {
        return commands.size();
    }
}

