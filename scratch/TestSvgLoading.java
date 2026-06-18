package scratch;

import org.example.utils.SVGUtils;
import java.io.InputStream;

public class TestSvgLoading {
    public static void main(String[] args) {
        String path = "/vectors/varon/cuadrado/corta.svg";
        System.out.println("Checking resource stream directly...");
        try (InputStream is = TestSvgLoading.class.getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("Direct stream is NULL!");
            } else {
                System.out.println("Direct stream is NOT null! Available bytes: " + is.available());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Loading path via SVGUtils...");
        try {
            String content = SVGUtils.loadPath(path);
            System.out.println("Extracted path length: " + content.length());
            if (content.length() > 0) {
                System.out.println("Start: " + content.substring(0, Math.min(content.length(), 100)));
            } else {
                System.out.println("Content is EMPTY!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
