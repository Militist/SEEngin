package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "pages", indexes = {
        @Index(name = "idx_page_path", columnList = "path")
})
@Data
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "path")
    private String path;

    @Column(name = "code")
    private int code;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteEntity siteEntity;

}
