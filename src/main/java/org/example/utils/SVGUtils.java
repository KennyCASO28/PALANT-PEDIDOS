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

    public static class ParsedSVGShape {
        public String pathData;
        public String fill;
        public String stroke;
        public String strokeWidth;
        public String transform;
        public String fillRule;
    }

    /** Extrae reglas CSS de todos los elementos <style> en el documento */
    private static java.util.Map<String, java.util.Map<String, String>> extractCssClasses(org.w3c.dom.Document doc) {
        java.util.Map<String, java.util.Map<String, String>> cssMap = new java.util.HashMap<>();
        try {
            org.w3c.dom.NodeList styleNodes = doc.getElementsByTagName("style");
            for (int i = 0; i < styleNodes.getLength(); i++) {
                org.w3c.dom.Node styleNode = styleNodes.item(i);
                String cssText = styleNode.getTextContent();
                if (cssText == null || cssText.isEmpty()) continue;
                // Parse .className { prop: val; prop2: val2; }
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "\\.([a-zA-Z0-9_-]+)\\s*\\{([^}]*)\\}"
                ).matcher(cssText);
                while (m.find()) {
                    String className = m.group(1);
                    String body = m.group(2);
                    java.util.Map<String, String> props = new java.util.HashMap<>();
                    for (String decl : body.split(";")) {
                        String[] kv = decl.split(":");
                        if (kv.length == 2) {
                            props.put(kv[0].trim(), kv[1].trim());
                        }
                    }
                    cssMap.put(className, props);
                }
            }
        } catch (Exception e) {
            // Ignorar errores de parseo CSS
        }
        return cssMap;
    }

    public static java.util.List<ParsedSVGShape> parseComplexSvg(java.io.File file) {
        java.util.List<ParsedSVGShape> shapes = new java.util.ArrayList<>();
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            
            // Allow DOCTYPE but prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            // Allow namespaces for proper SVG parsing
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();
            
            java.util.Map<String, java.util.Map<String, String>> cssClasses = extractCssClasses(doc);
            traverseDOMForShapes(doc.getDocumentElement(), shapes, cssClasses, null, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shapes;
    }

    public static java.util.List<ParsedSVGShape> parseComplexSvg(String content) {
        java.util.List<ParsedSVGShape> shapes = new java.util.ArrayList<>();
        try {
            if (content != null) {
                int firstAngleBracket = content.indexOf('<');
                if (firstAngleBracket > 0) {
                    content = content.substring(firstAngleBracket);
                }
            }
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            
            // Allow DOCTYPE but prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.xml.sax.InputSource is = new org.xml.sax.InputSource(new java.io.StringReader(content));
            org.w3c.dom.Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            
            java.util.Map<String, java.util.Map<String, String>> cssClasses = extractCssClasses(doc);
            traverseDOMForShapes(doc.getDocumentElement(), shapes, cssClasses, null, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shapes;
    }
    
    private static void traverseDOMForShapes(org.w3c.dom.Node node, java.util.List<ParsedSVGShape> shapes, java.util.Map<String, java.util.Map<String, String>> cssClasses, String currentFill, String currentStroke, String currentStrokeWidth, String currentTransform, String currentFillRule) {
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            org.w3c.dom.Element el = (org.w3c.dom.Element) node;
            String nodeName = el.getNodeName().toLowerCase();
            if (nodeName.equals("defs") || nodeName.equals("pattern") || nodeName.equals("clippath") 
                    || nodeName.equals("mask") || nodeName.equals("style") || nodeName.equals("metadata")
                    || nodeName.equals("lineargradient") || nodeName.equals("radialgradient")) {
                return;
            }
            
            String fill = el.getAttribute("fill");
            if (fill == null || fill.isEmpty()) fill = currentFill;
            String stroke = el.getAttribute("stroke");
            if (stroke == null || stroke.isEmpty()) stroke = currentStroke;
            String strokeW = el.getAttribute("stroke-width");
            if (strokeW == null || strokeW.isEmpty()) strokeW = currentStrokeWidth;
            String fillRule = el.getAttribute("fill-rule");
            if (fillRule == null || fillRule.isEmpty()) fillRule = currentFillRule;
            
            // Aplicar estilos CSS por clase (Inkscape genera SVG con <style> y class="fil0")
            if (cssClasses != null && !cssClasses.isEmpty()) {
                String classAttr = el.getAttribute("class");
                if (classAttr != null && !classAttr.isEmpty()) {
                    for (String cls : classAttr.split("\\s+")) {
                        java.util.Map<String, String> classProps = cssClasses.get(cls);
                        if (classProps != null) {
                            if (classProps.containsKey("fill") && (fill == null || fill.isEmpty() || fill.equals(currentFill))) {
                                fill = classProps.get("fill");
                            }
                            // Inkscape usa "color" en lugar de "fill" para algunos elementos
                            if (classProps.containsKey("color") && (fill == null || fill.isEmpty() || fill.equals(currentFill))
                                    && !classProps.containsKey("fill")) {
                                fill = classProps.get("color");
                            }
                            if (classProps.containsKey("stroke") && (stroke == null || stroke.isEmpty() || stroke.equals(currentStroke))) {
                                stroke = classProps.get("stroke");
                            }
                            if (classProps.containsKey("stroke-width") && (strokeW == null || strokeW.isEmpty() || strokeW.equals(currentStrokeWidth))) {
                                strokeW = classProps.get("stroke-width");
                            }
                            if (classProps.containsKey("fill-rule") && (fillRule == null || fillRule.isEmpty() || fillRule.equals(currentFillRule))) {
                                fillRule = classProps.get("fill-rule");
                            }
                        }
                    }
                }
            }
            
            String style = el.getAttribute("style");
            if (style != null && !style.isEmpty()) {
                for (String part : style.split(";")) {
                    String[] kv = part.split(":");
                    if (kv.length == 2) {
                        String k = kv[0].trim();
                        String v = kv[1].trim();
                        if (k.equals("fill")) fill = v;
                        else if (k.equals("stroke")) stroke = v;
                        else if (k.equals("stroke-width")) strokeW = v;
                        else if (k.equals("fill-rule")) fillRule = v;
                    }
                }
            }
            
            nodeName = el.getNodeName().toLowerCase();
            String pathData = null;
            if (nodeName.equals("path")) {
                pathData = el.getAttribute("d");
            } else if (nodeName.equals("polygon") || nodeName.equals("polyline")) {
                pathData = convertPointsToPath(el.getAttribute("points"));
            } else if (nodeName.equals("rect")) {
                double x = parseDoubleSafe(el.getAttribute("x"), 0);
                double y = parseDoubleSafe(el.getAttribute("y"), 0);
                double w = parseDoubleSafe(el.getAttribute("width"), 0);
                double h = parseDoubleSafe(el.getAttribute("height"), 0);
                if (w > 0 && h > 0) {
                    pathData = String.format(java.util.Locale.US, "M %f %f L %f %f L %f %f L %f %f Z", x, y, x+w, y, x+w, y+h, x, y+h);
                }
            } else if (nodeName.equals("circle")) {
                double cx = parseDoubleSafe(el.getAttribute("cx"), 0);
                double cy = parseDoubleSafe(el.getAttribute("cy"), 0);
                double r = parseDoubleSafe(el.getAttribute("r"), 0);
                if (r > 0) {
                    pathData = String.format(java.util.Locale.US, "M %f %f A %f %f 0 1 0 %f %f A %f %f 0 1 0 %f %f Z",
                        cx-r, cy, r, r, cx+r, cy, r, r, cx-r, cy);
                }
            }
            
            String transform = el.getAttribute("transform");
            String accumulatedTransform = currentTransform != null ? currentTransform : "";
            if (transform != null && !transform.isEmpty()) {
                accumulatedTransform = accumulatedTransform + " " + transform;
            }
            accumulatedTransform = accumulatedTransform.trim();
            
            if (pathData != null && !pathData.trim().isEmpty()) {
                ParsedSVGShape s = new ParsedSVGShape();
                s.pathData = pathData;
                s.fill = fill;
                s.stroke = stroke;
                s.strokeWidth = strokeW;
                s.transform = accumulatedTransform;
                s.fillRule = fillRule;
                shapes.add(s);
            }
            
            org.w3c.dom.NodeList children = el.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                traverseDOMForShapes(children.item(i), shapes, cssClasses, fill, stroke, strokeW, accumulatedTransform.isEmpty() ? null : accumulatedTransform, fillRule);
            }
        }
    }
    
    private static double parseDoubleSafe(String val, double def) {
        if (val == null || val.trim().isEmpty()) return def;
        try { return Double.parseDouble(val.replaceAll("[a-zA-Z]", "")); } 
        catch (Exception e) { return def; }
    }
    
    public static javafx.scene.paint.Color getSafeColor(String colorString, javafx.scene.paint.Color fallback) {
        if (colorString == null || colorString.isEmpty() || colorString.startsWith("url(")) {
            return fallback;
        }
        if (colorString.equalsIgnoreCase("none")) {
            return javafx.scene.paint.Color.TRANSPARENT;
        }
        // Intentar parsear hsl() / hsla() que JavaFX Color.web() no soporta nativamente
        String trimmed = colorString.trim();
        if (trimmed.toLowerCase().startsWith("hsl")) {
             javafx.scene.paint.Color hslColor = parseHslColor(trimmed);
            if (hslColor != null) return hslColor;
        }
        try {
            return javafx.scene.paint.Color.web(colorString);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static javafx.scene.paint.Color parseHslColor(String hslStr) {
        try {
            boolean hasAlpha = hslStr.toLowerCase().startsWith("hsla");
            int start = hslStr.indexOf('(');
            int end = hslStr.lastIndexOf(')');
            if (start < 0 || end < 0) return null;
            String[] parts = hslStr.substring(start + 1, end).split(",");
            if (parts.length < 3) return null;

            double h = Double.parseDouble(parts[0].trim()) / 360.0;
            double s = Double.parseDouble(parts[1].trim().replace("%", "")) / 100.0;
            double l = Double.parseDouble(parts[2].trim().replace("%", "")) / 100.0;
            double a = 1.0;
            if (hasAlpha && parts.length >= 4) {
                a = Double.parseDouble(parts[3].trim());
            }

            // HSL -> RGB conversion
            if (s == 0) {
                double gray = Math.round(l * 255);
                return javafx.scene.paint.Color.rgb((int) gray, (int) gray, (int) gray, a);
            }

            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;

            double r = hueToRgb(p, q, h + 1.0 / 3.0);
            double g = hueToRgb(p, q, h);
            double b = hueToRgb(p, q, h - 1.0 / 3.0);

            return javafx.scene.paint.Color.color(r, g, b, a);
        } catch (Exception e) {
            return null;
        }
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }
    
    public static void applySVGTransform(javafx.scene.Node node, String transformStr) {
        if (transformStr == null || transformStr.isEmpty()) return;
        
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\w+)\\s*\\(([^)]*)\\)").matcher(transformStr);
        while (m.find()) {
            String type = m.group(1).toLowerCase();
            String[] args = m.group(2).split("[\\s,]+");
            try {
                if (type.equals("matrix") && args.length >= 6) {
                    node.getTransforms().add(new javafx.scene.transform.Affine(
                        Double.parseDouble(args[0]), Double.parseDouble(args[2]), Double.parseDouble(args[4]),
                        Double.parseDouble(args[1]), Double.parseDouble(args[3]), Double.parseDouble(args[5])
                    ));
                } else if (type.equals("translate") && args.length >= 1) {
                    double tx = Double.parseDouble(args[0]);
                    double ty = args.length > 1 ? Double.parseDouble(args[1]) : 0;
                    node.getTransforms().add(new javafx.scene.transform.Translate(tx, ty));
                } else if (type.equals("scale") && args.length >= 1) {
                    double sx = Double.parseDouble(args[0]);
                    double sy = args.length > 1 ? Double.parseDouble(args[1]) : sx;
                    node.getTransforms().add(new javafx.scene.transform.Scale(sx, sy));
                } else if (type.equals("rotate") && args.length >= 1) {
                    double angle = Double.parseDouble(args[0]);
                    double px = args.length >= 3 ? Double.parseDouble(args[1]) : 0;
                    double py = args.length >= 3 ? Double.parseDouble(args[2]) : 0;
                    node.getTransforms().add(new javafx.scene.transform.Rotate(angle, px, py));
                }
            } catch (Exception e) {
                // Ignore parse errors for specific transform parts
            }
        }
    }
}

