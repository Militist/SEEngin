package searchengine.model;

public enum TextFields {
    INDEXING("Indexing"),
    INDEXED("Indexed"),
    FAILED("Failed");

    private final String type;

    TextFields(String type) {
        this.type = type;
    }

    public String getType(String type) {
        return type;
    }
}
