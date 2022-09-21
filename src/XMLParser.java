import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public class XMLParser {

    private final String LOCATION = "loc";
    private final Path destinationDir = Path.of("links");

    private final Document document;

    private static long writtenLinks;

    public XMLParser(URL xmlSource) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(getURLInputStream(xmlSource));
    }

    private InputStream getURLInputStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Accept", "application/xml");
        return connection.getInputStream();
    }


    private void writeLocations(Node node, Writer writer) throws IOException {
        NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);

            if (currentNode.getNodeType() == Node.TEXT_NODE) {
                Node parent = currentNode.getParentNode();

                if (LOCATION.equals(parent.getNodeName())) {
                    String link = currentNode.getNodeValue();
                    writer.append(link).append('\n');
                    ++writtenLinks;
                }
            }

            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                writeLocations(currentNode, writer);
            }
        }
    }

    public void writeLocationsToFile(File destination) throws IOException {
        writtenLinks = 0;
        Element rootElement = document.getDocumentElement();

        prepareDestDir(destinationDir.toFile());
        Path writePath = Path.of(destinationDir.toString(), File.separator, destination.toString());

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(writePath.toFile()))) {
            writeLocations(rootElement, writer);
        }
    }

    public void prepareDestDir(File destination) throws IOException {
        if (!Files.exists(destinationDir)) {
            Files.createDirectory(destinationDir);
            return;
        }

        File[] contents = destinationDir.toFile().listFiles();
        if (contents != null) {
            for (File file : contents) {
                file.delete();
            }
        }
    }

}
