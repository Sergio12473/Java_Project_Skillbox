package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private IndexStatus status;

    private LocalDateTime statusTime;

    private String lastError;

    private String url;

    private String name;

    public Site(IndexStatus status, String url, String name) {
        this.status = status;
        this.url = url;
        this.name = name;
    }
}