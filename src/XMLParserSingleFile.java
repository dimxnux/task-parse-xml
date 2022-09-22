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

public class XMLParserSingleFile {

    private static final String LOCATION_TAG = "loc";
    private static final Path DESTINATION_DIR = Path.of("links");

    private final Document document;
    private long writtenLines;
    private long linesToSkip;
    private Writer writer;

    public XMLParserSingleFile(URL xmlSource) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(getURLInputStream(xmlSource));
    }

    private InputStream getURLInputStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Accept", "application/xml");
        return connection.getInputStream();
    }

    private void writeLocations(Node node) throws IOException {
        NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);

            if (currentNode.getNodeType() == Node.TEXT_NODE) {
                Node parent = currentNode.getParentNode();

                if (LOCATION_TAG.equals(parent.getNodeName())) {
                    if (linesToSkip > 0) {
                        --linesToSkip;
                        continue;
                    }

                    String link = currentNode.getNodeValue();
                    writer.append(link).append('\n');
                    ++writtenLines;
                }
            }

            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                writeLocations(currentNode);
            }
        }
    }


    public void writeLocationsToFile(String destination) throws IOException {
        if (XMLParserStatusManager.isInProgress()) {
            linesToSkip = getPreviousSessionWrittenLines();
            writtenLines = linesToSkip;
        } else {
            XMLParserStatusManager.initializeStatus();
            prepareDestinationDir();
            linesToSkip = 0;
            writtenLines = 0;
        }

        Element rootElement = document.getDocumentElement();
        Path outFile = DESTINATION_DIR.resolve(Path.of(destination));

        writer = new BufferedWriter(new FileWriter(outFile.toFile(), true));

        writeLocations(rootElement);

        writer.close();
        XMLParserStatusManager.disposeStatus();
    }

    private void prepareDestinationDir() throws IOException {
        if (!Files.exists(DESTINATION_DIR)) {
            Files.createDirectory(DESTINATION_DIR);
            return;
        }

        File[] contents = DESTINATION_DIR.toFile().listFiles();
        if (contents != null) {
            for (File file : contents) {
                boolean success = file.delete();
            }
        }
    }

    public long getPreviousSessionWrittenLines() throws IOException {
        long lines = 0;
        File[] files = DESTINATION_DIR.toFile().listFiles();

        for (File file : files) {
            try (BufferedReader reader =
                         new BufferedReader(new FileReader(file))) {
                while (reader.readLine() != null) {
                    ++lines;
                }
            }
        }

        return lines;
    }

}
