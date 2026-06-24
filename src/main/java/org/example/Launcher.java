package org.example;

public class Launcher {
    public static void main(String[] args) {
        // Force JavaFX to use Software Rendering (CPU) to avoid NullPointerException crashes
        // caused by Direct3D (D3D) driver texture state loss / device issues on Windows.
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.d3d", "false");

        // This class is necessary for the Fat JAR (Uber-JAR) to work correctly
        // with JavaFX 11+ without complex module-path configurations.
        Main.main(args);
    }
}
