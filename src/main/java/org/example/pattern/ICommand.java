package org.example.pattern;

public interface ICommand {
    void execute();

    void undo();

    void redo();

    String getName(); // For debug/UI

    default String getContextZone() {
        return null;
    }
}

