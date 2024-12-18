package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "site")
@Data
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private Status type;

    @Column(name = "status_time")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;
}
