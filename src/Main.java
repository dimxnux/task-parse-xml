import java.net.URL;

public class Main {

    private static final String XML_URL = "https://makeup.md/sitemap/sitemap.xml";

    public static void main(String[] args) {
        try {
            XMLParser xmlParser = new XMLParser(new URL(XML_URL));
            String linksDest = "links.txt";
            xmlParser.writeLinksToFile(linksDest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
