import java.net.URL;

public class Main {

    private static final String XML_URL = "https://makeup.md/sitemap/sitemap.xml";

    public static void main(String[] args) {
        try {
//            XMLParserSingleFile xmlParser = new XMLParserSingleFile(new URL(XML_URL));
//            String linksDest = "links.txt";
//
//            xmlParser.writeLocationsToFile(linksDest);

            XMLParserFileSplitter xmlParser = new XMLParserFileSplitter(new URL(XML_URL));
            String linksDest = "links.txt";
            xmlParser.writeLocationsToFile(linksDest, 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
