package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Page {
    private String path;
    private int code;
    private String content;
}
