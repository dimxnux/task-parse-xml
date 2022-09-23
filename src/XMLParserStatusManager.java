import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


public class XMLParserStatusManager {

    private static final String STATUS_FILE = "status.dat";

    private static XMLParserStatus parserStatus;

    public static void initializeStatus(String filename, long linesThreshold) throws IOException {
        parserStatus = new XMLParserStatus(filename, linesThreshold);
        serializeStatus();
    }

    public static void disposeStatus() throws IOException {
        Files.delete(Path.of(STATUS_FILE));
    }

    public static boolean isInProgress() {
        return Files.exists(Path.of(STATUS_FILE));
    }

    public static void serializeStatus() throws IOException {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(STATUS_FILE))) {
            out.writeObject(parserStatus);
        }
    }

    public static XMLParserStatus deserializeStatus() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(STATUS_FILE))) {
            return (XMLParserStatus) in.readObject();
        }
    }

}
