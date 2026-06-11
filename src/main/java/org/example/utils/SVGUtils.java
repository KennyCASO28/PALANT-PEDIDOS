package org.example.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SVGUtils {

    /**
     * Carga el contenido de un archivo de texto/svg desde resources.
     * Soporta detección de Encoding (UTF-8, UTF-16LE, UTF-16BE) y extracción de
     * paths/polygons.
     */
    public static String loadPath(String resourcePath) {
        return loadPath(resourcePath, false);
    }

    public static String loadPath(String resourcePath, boolean silent) {
        try (InputStream is = SVGUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                if (!silent) {
                    System.err.println("No se encontró el recurso: " + resourcePath);
                }
                return "";
            }

            // Read all bytes to detect encoding
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] allBytes = buffer.toByteArray();

            if (allBytes.length == 0)
                return "";

            // Detect Encoding
            Charset charset = detectCharset(allBytes);
            String content = new String(allBytes, charset).trim();

            // Remove BOM if present (String constructor might handle it but trim() helps)
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }

            // Extract Paths or Shapes
            if (content.contains("<svg") || content.contains("d=") || content.contains("points=")) {
                String extracted = extractPathsAndShapesFromSvg(content);
                if (!extracted.isEmpty()) {
                    return extracted;
                }
            }

            return "";

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Extracts individual path strings from an SVG.
     * Also supports Polygons converted to Paths.
     */
    public static java.util.List<String> loadSeparatePaths(String resourcePath) {
        java.util.List<String> paths = new java.util.ArrayList<>();
        try {
            // Re-use logic to get content
            String content = readContentWithEncoding(resourcePath);
            if (content.isEmpty())
                return paths;

            // Extract both standard paths and converted polygons
            paths.addAll(extractRawPaths(content));
            paths.addAll(extractConvertedPolygons(content));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }

    private static String readContentWithEncoding(String resourcePath) {
        try (InputStream is = SVGUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null)
                return "";
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] allBytes = buffer.toByteArray();
            if (allBytes.length == 0)
                return "";
            Charset charset = detectCharset(allBytes);
            String content = new String(allBytes, charset).trim();
            if (content.startsWith("\uFEFF"))
                content = content.substring(1);
            return content;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 2) {
            int b1 = bytes[0] & 0xFF;
            int b2 = bytes[1] & 0xFF;
            if (b1 == 0xFF && b2 == 0xFE)
                return StandardCharsets.UTF_16LE;
            if (b1 == 0xFE && b2 == 0xFF)
                return StandardCharsets.UTF_16BE;
        }
        return StandardCharsets.UTF_8; // Default
    }

    public static java.util.Map<String, String> loadCategorizedPaths(String resourcePath) {
        // Simple fallback to separation logic, implementing full XML parsing with
        // encoding support is safer but
        // for now let's reuse the robust regex extraction if possible or just use
        // loadSeparatePaths.
        // The original implementation used XML parsing. To support UTF-16 XML parsing
        // properly,
        // we should feed the Reader with correct charset.

        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("BODY", "");
        result.put("SLEEVES", "");

        try {
            String content = readContentWithEncoding(resourcePath);
            if (content.isEmpty())
                return result;

            // Simplistic approach: Load all separate paths and split manually if needed?
            // Or recreate the XML parser with StringReader which is already decoded
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(
                    (publicId, systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));

            org.xml.sax.InputSource is = new org.xml.sax.InputSource(new java.io.StringReader(content));
            org.w3c.dom.Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            org.w3c.dom.NodeList allPaths = doc.getElementsByTagName("path");
            org.w3c.dom.NodeList allPolys = doc.getElementsByTagName("polygon");
            org.w3c.dom.NodeList allLines = doc.getElementsByTagName("polyline");

            java.util.List<PathEntry> entries = new java.util.ArrayList<>();
            int minDepth = Integer.MAX_VALUE;

            // Helper to process nodes
            processNodes(allPaths, "d", entries);
            processNodes(allPolys, "points", entries); // Will need conversion
            processNodes(allLines, "points", entries);

            // Calculate min depth
            for (PathEntry e : entries)
                if (e.depth < minDepth)
                    minDepth = e.depth;

            StringBuilder sbBody = new StringBuilder();
            StringBuilder sbSleeves = new StringBuilder();

            for (PathEntry entry : entries) {
                if (entry.depth == minDepth) {
                    sbBody.append(entry.d).append(" ");
                } else {
                    sbSleeves.append(entry.d).append(" ");
                }
            }

            result.put("BODY", sbBody.toString().trim());
            result.put("SLEEVES", sbSleeves.toString().trim());

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback
            java.util.List<String> list = loadSeparatePaths(resourcePath);
            if (!list.isEmpty())
                result.put("BODY", String.join(" ", list));
        }
        return result;
    }

    private static void processNodes(org.w3c.dom.NodeList nodes, String attrName, java.util.List<PathEntry> entries) {
        if (nodes == null)
            return;
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
            org.w3c.dom.Node attr = attrs.getNamedItem(attrName);
            if (attr == null)
                continue;

            String val = attr.getNodeValue().trim();
            if (attrName.equals("points")) {
                val = convertPointsToPath(val);
            }

            int depth = 0;
            org.w3c.dom.Node parent = node.getParentNode();
            while (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                depth++;
                parent = parent.getParentNode();
            }
            entries.add(new PathEntry(val, depth));
        }
    }

    private static class PathEntry {
        String d;
        int depth;

        public PathEntry(String d, int depth) {
            this.d = d;
            this.depth = depth;
        }
    }

    private static String extractPathsAndShapesFromSvg(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(" ", extractRawPaths(content))).append(" ");
        sb.append(String.join(" ", extractConvertedPolygons(content))).append(" ");
        return sb.toString().trim();
    }

    private static java.util.List<String> extractRawPaths(String content) {
        java.util.List<String> list = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\bd\\s*=\\s*[\"']([^\"']+)[\"']",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            list.add(matcher.group(1).trim());
        }
        return list;
    }

    private static java.util.List<String> extractConvertedPolygons(String content) {
        java.util.List<String> list = new java.util.ArrayList<>();
        // Match points="..." for polygon or polyline
        // Limite simplified regex, assumes points attribute exists
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("points\\s*=\\s*[\"']([^\"']+)[\"']",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String points = matcher.group(1).trim();
            list.add(convertPointsToPath(points));
        }
        return list;
    }

    private static String convertPointsToPath(String points) {
        if (points.isEmpty())
            return "";
        // Replace commas with spaces to normalize
        String[] coords = points.replaceAll(",", " ").split("\\s+");
        if (coords.length < 2)
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append("M ").append(coords[0]).append(" ").append(coords[1]);

        for (int i = 2; i < coords.length; i += 2) {
            if (i + 1 < coords.length) {
                sb.append(" L ").append(coords[i]).append(" ").append(coords[i + 1]);
            }
        }
        sb.append(" Z"); // Close path
        return sb.toString();
    }

    public static double parseLeadingDouble(String s) {
        try {
            int i = 0;
            if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+'))
                i++;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.'))
                i++;
            if (i > 0)
                return Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            /* ignore */ }
        return 0.0;
    }

    public static double[] parseCoordinatePair(String s) {
        double[] coords = new double[2];
        try {
            String[] parts = s.trim().split("[,\\s]+");
            if (parts.length >= 2) {
                coords[0] = Double.parseDouble(parts[0]);
                coords[1] = Double.parseDouble(parts[1]);
            }
        } catch (Exception e) {
            // Fallback for messy strings
            coords[0] = parseLeadingDouble(s);
            // Try to find second number: skip first number and find next digit/-
            int i = 0;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.' || s.charAt(i) == '-' || s.charAt(i) == 'e' || s.charAt(i) == 'E')) i++;
            while (i < s.length() && !Character.isDigit(s.charAt(i)) && s.charAt(i) != '.' && s.charAt(i) != '-') i++;
            if (i < s.length()) coords[1] = parseLeadingDouble(s.substring(i));
        }
        return coords;
    }

    /**
     * Converts a relative 'm' or absolute 'M' subpath piece to an absolute 'M' subpath piece,
     * preserving the rest of the path data accurately.
     */
    public static String absoluteifyPiece(String piece, double lastX, double lastY) {
        String trimmed = piece.trim();
        if (trimmed.isEmpty()) return "";
        char cmd = trimmed.charAt(0);
        if (cmd != 'M' && cmd != 'm') return piece;

        String content = trimmed.substring(1).trim();
        double[] coords = parseCoordinatePair(content);
        double absX = (cmd == 'M') ? coords[0] : lastX + coords[0];
        double absY = (cmd == 'M') ? coords[1] : lastY + coords[1];

        // Robustly find the 'rest' of the string after the two coordinates
        // We skip characters that belong to the first two numbers (digits, dots, signs, commas, spaces, 'e')
        int i = 0;
        int count = 0;
        while (i < content.length() && count < 2) {
            // Find start of a number
            while (i < content.length() && !Character.isDigit(content.charAt(i)) && content.charAt(i) != '-' && content.charAt(i) != '.') i++;
            if (i >= content.length()) break;
            // Skip the number
            while (i < content.length() && (Character.isDigit(content.charAt(i)) || content.charAt(i) == '.' || content.charAt(i) == '-' || content.charAt(i) == 'e' || content.charAt(i) == 'E')) i++;
            count++;
            // Skip trailing comma or space after the number
            if (i < content.length() && (content.charAt(i) == ',' || Character.isWhitespace(content.charAt(i)))) i++;
        }
        
        String rest = i < content.length() ? content.substring(i).trim() : "";
        return "M " + absX + " " + absY + " " + rest;
    }
}

