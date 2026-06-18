import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.FileWriter;
import java.io.PrintWriter;

public class ExtractPDF {
    public static void main(String[] args) {
        try {
            String pdfPath = "Monografía Proyecto Palant.pdf";
            String outPath = "scratch/monografia_text.txt";
            PdfReader reader = new PdfReader(pdfPath);
            int pages = reader.getNumberOfPages();
            PrintWriter writer = new PrintWriter(new FileWriter(outPath));
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int i = 1; i <= pages; i++) {
                writer.println("=== PAGE " + i + " ===");
                try {
                    String text = extractor.getTextFromPage(i);
                    writer.println(text);
                } catch (Exception ex) {
                    writer.println("ERROR EXTRACTING PAGE " + i + ": " + ex.getMessage());
                }
            }
            writer.close();
            reader.close();
            System.out.println("SUCCESS: Extracted " + pages + " pages to " + outPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
