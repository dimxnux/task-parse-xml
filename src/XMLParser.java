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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class XMLParser {

    private static final String LOCATION_TAG = "loc";
    private static final Path DESTINATION_DIR = Path.of("links");

    private final Document document;
    private String destinationFilename;
    private long writtenLines;
    private long writtenFiles;
    private long linesToSkip;
    private long linesThreshold;
    private Writer writer;

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

    /**
     * Write all links from the XML document to a single file.
     * */
    public void writeLinksToFile(String destinationFilename)
            throws IOException, ClassNotFoundException {
        writeTextNodeToFile(destinationFilename, 0, LOCATION_TAG);
    }

    /**
     * Write all links from the XML document to multiple files, each containing a limited number of lines.
     * @param linesThreshold max number of lines a file can contain
     * */
    public void writeLinksToFile(String destinationFilename, long linesThreshold)
            throws IOException, ClassNotFoundException {
        if (linesThreshold <= 0) {
            throw new IllegalArgumentException("Lines threshold must be greater than 0." +
                    " Provided linesThreshold: " + linesThreshold);
        }

        writeTextNodeToFile(destinationFilename, linesThreshold, LOCATION_TAG);
    }

    void configureForPreviousSession(String destinationFilename, XMLParserStatus parserStatus) throws IOException, ClassNotFoundException {
        long previousLinesThreshold = parserStatus.getLinesThreshold();
        linesToSkip = getPreviousSessionWrittenLines();
        if (previousLinesThreshold == 0) {
            writtenLines = 0;
            writtenFiles = 0;
        } else {
            long previousSessionWrittenLines = getPreviousSessionWrittenLines();
            writtenLines = previousSessionWrittenLines % previousLinesThreshold;
            writtenFiles = previousSessionWrittenLines / previousLinesThreshold;
        }
        this.linesThreshold = previousLinesThreshold;
        this.destinationFilename = destinationFilename;
    }

    void configureForNewSession(String destinationFilename, long linesThreshold) throws IOException {
        XMLParserStatusManager.initializeStatus(destinationFilename, linesThreshold);
        prepareDestinationDir();

        linesToSkip = 0;
        writtenLines = 0;
        writtenFiles = 0;
        this.linesThreshold = linesThreshold;
        this.destinationFilename = destinationFilename;
    }

    private void writeTextNodeToFile(String destinationFilename, long linesThreshold, String parentTag)
            throws IOException, ClassNotFoundException {

        XMLParserStatus parserStatus = XMLParserStatusManager.deserializeStatus();

        if (XMLParserStatusManager.isInProgress()
                && (parserStatus != null)
                // if the same filename as from the previous session, then resume the previous session
                && destinationFilename.equals(parserStatus.getFilename())) {

            configureForPreviousSession(destinationFilename, parserStatus);
        } else {
            configureForNewSession(destinationFilename, linesThreshold);
        }

        Element rootElement = document.getDocumentElement();
        writeTextNodes(rootElement, parentTag);

        writer.close();
        XMLParserStatusManager.disposeStatus();
    }

    private void writeTextNodes(Node node, String parentTag) throws IOException {
        if ((linesToSkip == 0) // no more lines to skip
                && (linesThreshold != 0) // check if lines threshold is enabled
                && (writtenLines == linesThreshold)) {
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

                if (parentTag.equals(parent.getNodeName())) {
                    if (linesToSkip > 0) {
                        --linesToSkip;
                        continue;
                    }
                    if (writtenLines == 0 // when a new file gets created
                            // writer is null when it skips multiple files and non-zero lines
                            || writer == null) {
                        writer = getNextWriter();
                    }

                    String link = currentNode.getNodeValue();
                    writer.append(link).append('\n');
                    ++writtenLines;
                    // for testing:
//                    writer.flush();
//                    if (writtenFiles == 3 && writtenLines == 2) {
//                        throw new RuntimeException();
//                    }
                }
            }

            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                writeTextNodes(currentNode, parentTag);
            }
        }
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

    private long getPreviousSessionWrittenLines() throws IOException {
        long lines = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DESTINATION_DIR)) {
            for (Path entry : stream) {
                try (BufferedReader reader =
                             new BufferedReader(new FileReader(entry.toFile()))) {
                    while (reader.readLine() != null) {
                        ++lines;
                    }
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

        String fileNumber = (linesThreshold == 0) ? "" : "-" + (writtenFiles + 1);
        filename = filename + fileNumber + extension;
        Path outFile = DESTINATION_DIR.resolve(Path.of(filename));

        return new BufferedWriter(new FileWriter(outFile.toFile(), true));
    }

}
