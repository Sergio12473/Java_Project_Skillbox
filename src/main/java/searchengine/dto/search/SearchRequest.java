package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest {
    private String query;
    private String site;
    private int offset;
    private int limit;

    public SearchRequest(String query, int offset, int limit) {
        this.query = query;
        this.offset = offset;
        this.limit = limit;
    }
}