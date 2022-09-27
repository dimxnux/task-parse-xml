import java.io.Serializable;

public class XMLParserStatus implements Serializable {

    private static final long serialVersionUID = 1234567L;

    private final String filename;
    private final long linesThreshold;

    public XMLParserStatus(String filename, long linesThreshold) {
        this.filename = filename;
        this.linesThreshold = linesThreshold;
    }

    public long getLinesThreshold() {
        return linesThreshold;
    }

    public String getFilename() {
        return filename;
    }

}
