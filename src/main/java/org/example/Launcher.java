package org.example;

public class Launcher {
    public static void main(String[] args) {
        // This class is necessary for the Fat JAR (Uber-JAR) to work correctly
        // with JavaFX 11+ without complex module-path configurations.
        Main.main(args);
    }
}
