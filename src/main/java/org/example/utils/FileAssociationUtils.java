package org.example.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;

/**
 * Registers the .plt file extension with the Palant custom icon in Windows
 * Explorer.
 * Uses HKEY_CURRENT_USER so no administrator rights are required.
 * Call {@link #register()} once on application start.
 */
public class FileAssociationUtils {

    private static final String EXT = ".tlp";
    private static final String PROG_ID = "Palant.ProjectFile";
    private static final String DESCRIPTION = "Proyecto Palant";
    private static final String ICON_FILENAME = "palant_tlp.ico";
    private static final String ICON_RESOURCE = "/vectors/LOGO PALANT-barra.png";

    /** Entry-point: silently register the file association (best-effort). */
    public static void register() {
        if (!isWindows())
            return;
        try {
            Path iconPath = buildAndSaveIcon();
            if (iconPath != null) {
                writeRegistry(iconPath.toAbsolutePath().toString());
            }
        } catch (Exception e) {
            System.err.println("[FileAssociationUtils] Could not register .plt icon: " + e.getMessage());
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Load the bundled PNG, resize it to 256×256, and write a minimal valid ICO
     * file.
     */
    private static Path buildAndSaveIcon() throws Exception {
        URL res = FileAssociationUtils.class.getResource(ICON_RESOURCE);
        if (res == null)
            return null;

        // Load and resize the logo
        BufferedImage srcImg = ImageIO.read(res);
        int[] sizes = { 256, 64, 48, 32, 16 };
        java.util.List<byte[]> pngBytesList = new java.util.ArrayList<>();

        for (int SIZE : sizes) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            // --- 0. HIGH QUALITY RENDERING PROFILE ---
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

            int cornerRadius = SIZE; // Perfect circle

            // --- 2. Draw Deep Dark Blue Gradient Background ---
            java.awt.Color colorTopLeft = new java.awt.Color(20, 30, 48); // Midnight Dark Blue
            java.awt.Color colorBottomRight = new java.awt.Color(40, 65, 95); // Rich Royal Navy
            java.awt.GradientPaint gp = new java.awt.GradientPaint(
                    0, 0, colorTopLeft,
                    SIZE, SIZE, colorBottomRight);
            g.setPaint(gp);
            g.fillRoundRect(0, 0, SIZE, SIZE, cornerRadius, cornerRadius);

            // --- 3. Draw Inner Gloss/Border (Clear Light Blue) ---
            g.setColor(new java.awt.Color(50, 180, 255)); // Solid Light Blue / Cyan
            float strokeWidth = Math.max(1.0f, SIZE * 5.0f / 256.0f);
            g.setStroke(new java.awt.BasicStroke(strokeWidth));
            int offset = Math.round(strokeWidth / 2.0f);
            if (offset < 1)
                offset = 1;
            g.drawRoundRect(offset, offset, SIZE - offset * 2, SIZE - offset * 2, cornerRadius, cornerRadius);

            // --- 4. Draw Logo Centered (High Quality Downscaling) ---
            int pad = Math.round(SIZE * 25.0f / 256.0f); // Proportional padding
            int targetW = SIZE - pad * 2;
            int targetH = SIZE - pad * 2;

            java.awt.Image smoothImg = srcImg.getScaledInstance(targetW, targetH, java.awt.Image.SCALE_SMOOTH);
            g.drawImage(smoothImg, pad, pad, null);
            g.dispose();

            // Encode the resized image as PNG bytes (used inside the ICO)
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", pngOut);
            pngBytesList.add(pngOut.toByteArray());
        }

        // Build a Multi-Resolution ICO file
        // ICO format: 6-byte header + 16-byte directory entry per image + image data
        int headerSize = 6;
        int dirEntrySize = 16 * sizes.length;

        int totalDataSize = 0;
        for (byte[] b : pngBytesList) {
            totalDataSize += b.length;
        }

        ByteBuffer buf = ByteBuffer.allocate(headerSize + dirEntrySize + totalDataSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // ICONDIR (6 bytes)
        buf.putShort((short) 0); // reserved
        buf.putShort((short) 1); // type: 1 = icon
        buf.putShort((short) sizes.length); // count: multiple images

        // ICONDIRENTRY (16 bytes per image)
        int currentOffset = headerSize + dirEntrySize;
        for (int i = 0; i < sizes.length; i++) {
            int s = sizes[i] == 256 ? 0 : sizes[i]; // width/height 0 means 256
            byte[] d = pngBytesList.get(i);

            buf.put((byte) s); // width
            buf.put((byte) s); // height
            buf.put((byte) 0); // color count (0 = no palette)
            buf.put((byte) 0); // reserved
            buf.putShort((short) 1); // planes
            buf.putShort((short) 32); // bit count
            buf.putInt(d.length); // size of image data
            buf.putInt(currentOffset); // offset to image data

            currentOffset += d.length;
        }

        // PNG image data
        for (byte[] d : pngBytesList) {
            buf.put(d);
        }

        // Write to %APPDATA%\Palant\
        Path palantDir = Paths.get(System.getenv("APPDATA"), "Palant");
        Files.createDirectories(palantDir);
        Path icoPath = palantDir.resolve(ICON_FILENAME);
        Files.write(icoPath, buf.array(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return icoPath;
    }

    /**
     * Write the registry keys using reg.exe (HKCU — no admin needed).
     */
    private static void writeRegistry(String iconPath) throws Exception {
        String hkcu = "HKCU\\Software\\Classes";

        // Define the app command (even if in IDE, we try to point to the current
        // jar/classes location)
        String appPath = System.getProperty("java.home") + "\\bin\\javaw.exe";
        String jarPath = new File(
                FileAssociationUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getAbsolutePath();
        String command = "\"" + appPath + "\" -jar \"" + jarPath + "\" \"%1\"";

        // GUIDs for Windows Shell Extensions
        String previewClassId = "{13d3bbb4-bb47-4612-8b4d-10c513ae05b6}"; // Windows Photo Viewer

        // 0. Aggressive Extension Registration
        reg("add", hkcu + "\\" + EXT, "/ve", "/d", PROG_ID, "/f");
        reg("add", hkcu + "\\" + EXT, "/v", "PerceivedType", "/d", "image", "/f");
        reg("add", hkcu + "\\" + EXT, "/v", "Content Type", "/d", "image/png", "/f");
        reg("add", hkcu + "\\" + EXT, "/v", "Kind", "/d", "picture", "/f");

        // 1. ProgID Registration (Deep Professional Identity)
        reg("add", hkcu + "\\" + PROG_ID, "/ve", "/d", DESCRIPTION, "/f");
        reg("add", hkcu + "\\" + PROG_ID, "/v", "FriendlyTypeName", "/d", DESCRIPTION, "/f");
        reg("add", hkcu + "\\" + PROG_ID, "/v", "InfoTip", "/d", "Propiedades del diseño de Palant", "/f");
        reg("add", hkcu + "\\" + PROG_ID + "\\DefaultIcon", "/ve", "/d", iconPath + ",0", "/f");
        reg("add", hkcu + "\\" + PROG_ID + "\\shell\\open\\command", "/ve", "/d", command, "/f");

        // 2. Shell Extensions (Thumbnail & Preview) - Using standard Windows Image
        // Providers
        // {C7657C4A-9F68-40fa-A4DF-96BC08EB3551} is the modern IThumbnailProvider for
        // images
        String imageThumbHandler = "{C7657C4A-9F68-40fa-A4DF-96BC08EB3551}";

        String[] keys = { hkcu + "\\" + EXT, hkcu + "\\" + PROG_ID, hkcu + "\\SystemFileAssociations\\" + EXT };
        for (String key : keys) {
            // IThumbnailProvider
            reg("add", key + "\\ShellEx\\{e357fccd-a995-4576-b01f-234630154e96}", "/ve", "/d", imageThumbHandler, "/f");
            // IExtractImage (Fallback)
            reg("add", key + "\\ShellEx\\{BB2E617C-0920-11d1-9A0B-00C04FC2D6C1}", "/ve", "/d",
                    "{3F30C968-480A-4C6C-862D-EFC0897BB84B}", "/f");
            // Preview Handler
            reg("add", key + "\\ShellEx\\{8895b1c6-b41f-4c1c-a562-0d564250836f}", "/ve", "/d", previewClassId, "/f");

            // TREATMENT Key: Force Windows to treat it as a Photo (enables transparency
            // processing in thumbnails)
            reg("add", key, "/v", "Treatment", "/t", "REG_DWORD", "/d", "2", "/f");

            // TYPEOVERLAY: Muestra el ícono del programa en la esquina inferior derecha del
            // thumbnail
            reg("add", key, "/v", "TypeOverlay", "/d", iconPath + ",0", "/f");
        }

        // 3. Notify Windows Shell
        refreshShellIcons();
    }

    private static void reg(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "reg.exe";
        System.arraycopy(args, 0, cmd, 1, args.length);
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        p.waitFor();
    }

    /** Tell Explorer to refresh its icon cache without restarting the process. */
    private static void refreshShellIcons() throws Exception {
        // Use PowerShell to call SHChangeNotify (System Index refresh)
        // 0x08000000 = SHCNE_ASSOCCHANGED
        String script = "$code = '[DllImport(\"shell32.dll\")] public static extern void SHChangeNotify(uint wEventId, uint uFlags, IntPtr dwItem1, IntPtr dwItem2);';"
                +
                "Add-Type -MemberDefinition $code -Namespace Native -Name Shell32 -ErrorAction SilentlyContinue;" +
                "[Native.Shell32]::SHChangeNotify(0x08000000, 0, [IntPtr]::Zero, [IntPtr]::Zero);";

        new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command",
                script)
                .start();
    }
}
