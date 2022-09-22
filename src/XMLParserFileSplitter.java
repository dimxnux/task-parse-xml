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

public class XMLParserFileSplitter {

    private static final String LOCATION_TAG = "loc";
    private static final Path DESTINATION_DIR = Path.of("links");

    private final Document document;
    private String destinationFilename;
    private long writtenLines;
    private long writtenFiles;
    private long linesToSkip;
    private long linesThreshold;
    private Writer writer;

    public XMLParserFileSplitter(URL xmlSource) throws ParserConfigurationException, IOException, SAXException {
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
        if ((linesToSkip == 0) // no more lines to skip
                && (linesThreshold != 0)
                && (writtenLines == linesThreshold)) {
            // prepare the next Writer for the next file
            writer.close();
            ++writtenFiles;
            writtenLines = 0;
            writer = getNextWriter();
        }

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


    public void writeLocationsToFile(String destinationFilename, long linesThreshold) throws IOException, ClassNotFoundException {
        linesThreshold = Math.max(0, linesThreshold);

        if (XMLParserStatusManager.isInProgress()) {
            XMLParserStatus parserStatus = XMLParserStatusManager.deserializeStatus();

            // Ignore the given argument for destinationFilename, while resuming a previous session
            this.destinationFilename = parserStatus.getFilename();

            // Ignore the given argument for linesThreshold, while resuming a previous session
            long previousLinesThreshold = parserStatus.getLinesThreshold();
            linesToSkip = getPreviousSessionWrittenLines();
            writtenLines = linesToSkip % previousLinesThreshold;
            writtenFiles = linesToSkip / previousLinesThreshold;
            this.linesThreshold = previousLinesThreshold;

        } else {
            XMLParserStatusManager.initializeStatus(destinationFilename, linesThreshold);
            prepareDestinationDir();

            this.destinationFilename = destinationFilename;
            linesToSkip = 0;
            writtenLines = 0;
            writtenFiles = 0;
            this.linesThreshold = linesThreshold;
        }

        Element rootElement = document.getDocumentElement();

        writer = getNextWriter();

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

    private Writer getNextWriter() throws IOException {
        String extension = "";
        String filename;

        int extensionStartIndex = destinationFilename.indexOf('.');
        if (extensionStartIndex != -1) {
            extension = destinationFilename.substring(extensionStartIndex);
            filename = destinationFilename.substring(0, extensionStartIndex);
        } else {
            filename = destinationFilename;
        }

        filename = filename + "-" + (writtenFiles + 1) + extension;
        Path outFile = DESTINATION_DIR.resolve(Path.of(filename));

        return new BufferedWriter(new FileWriter(outFile.toFile(), true));
    }

}
