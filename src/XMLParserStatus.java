import java.io.Serializable;

public class XMLParserStatus implements Serializable {

    private static final long serialVersionUID = 1234567L;

    private String filename;
    private long linesThreshold;


    public XMLParserStatus() {}

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
