package searchengine.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class Page {
    private String path;
    private int code;
    private String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return Objects.equals(path, page.path) && Objects.equals(content, page.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, content);
    }

    @Override
    public String toString() {
        return "Page{" +
                "path='" + path + "'" +
        ", content='" + content + "}";
    }
}
