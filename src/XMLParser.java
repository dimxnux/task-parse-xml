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
    private long writtenLines;
    private int writtenFiles;
    private boolean hasLinesThreshold;
    private long linesThreshold;
    private String destinationFile;
    private Writer writer;


    public XMLParser(URL xmlSource) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(getURLInputStream(xmlSource));

        InputStream is = getURLInputStream(new URL(""));

        hasLinesThreshold = false;
    }

    public void setLinesThreshold(long linesThreshold) {
        hasLinesThreshold = true;
        this.linesThreshold = Math.max(1, linesThreshold);
    }

    public void disableLinesThreshold() {
        hasLinesThreshold = false;
        linesThreshold = 0;
    }

    private InputStream getURLInputStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Accept", "application/xml");
        return connection.getInputStream();
    }

    private void writeLocations(Node node) throws IOException {
        if (hasLinesThreshold && (writtenLines == linesThreshold)) {
            // prepare the next Writer for the next file
            writer.close();
            ++writtenFiles;
            writtenLines = 0;
        }

        NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);

            if (currentNode.getNodeType() == Node.TEXT_NODE) {
                Node parent = currentNode.getParentNode();

                if (LOCATION.equals(parent.getNodeName())) {
                    String link = currentNode.getNodeValue();
                    if (writtenLines == 0) {
                        writer = getNextWriter();
                    }
                    writer.append(link).append('\n');
                    ++writtenLines;
                }
            }

            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                writeLocations(currentNode);
            }
        }
    }

    private Writer getNextWriter() throws IOException {
        String extension = "";
        String filename;

        int extensionStartIndex = destinationFile.indexOf('.');
        if (extensionStartIndex != -1) {
            extension = destinationFile.substring(extensionStartIndex);
            filename = destinationFile.substring(0, extensionStartIndex);
        } else {
            filename = destinationFile;
        }

        filename = filename + "-" + (writtenFiles + 1) + extension;
        Path writePath = Path.of(destinationDir.toString(), File.separator, filename);

        return new BufferedWriter(new FileWriter(writePath.toFile()));
    }


    public void writeLocationsToFile(File destination) throws IOException {
        destinationFile = destination.toString();
        writtenLines = 0;
        writtenFiles = 0;
        Element rootElement = document.getDocumentElement();

        prepareDestDir();

        writer = getNextWriter();
        writeLocations(rootElement);

        writer.close();
    }

    public void prepareDestDir() throws IOException {
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
