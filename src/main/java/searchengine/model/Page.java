package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.services.WebSiteService;
import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;
    private String path;
    private Integer code;
    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String content;

    public Page(Site site, String path, Integer code, String content) {
        this(site, path, code);
        this.content = content;
    }

    public Page(Site site, String path, Integer code) {
        this(site, path);
        this.code = code;
    }

    public Page(Site site) {
        this(site, WebSiteService.getUrl(site.getUrl()));
    }

    public Page(Site site, String path) {
        this.site = site;
        this.path = path;
    }

    @Override
    public String toString() {
        return "Page: " + path;
    }
}