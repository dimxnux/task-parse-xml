import java.io.File;
import java.net.URL;

public class Main {

    private static final String XML_URL = "https://makeup.md/sitemap/sitemap.xml";

    public static void main(String[] args) {
        try {
            XMLParser xmlParser = new XMLParser(new URL(XML_URL));
            File linksDest = new File("links.txt");

            xmlParser.setLinesThreshold(8);
            xmlParser.writeLocationsToFile(linksDest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
