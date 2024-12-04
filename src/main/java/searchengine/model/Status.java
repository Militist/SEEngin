package searchengine.model;


public enum Status {
    INDEXING("Indexing"),
    INDEXED("Indexed"),
    FAILED("Failed");

    private final String type;

    Status(String type) {
        this.type = type;
    }

    public String getType(String type) {
        return type;
    }

}
