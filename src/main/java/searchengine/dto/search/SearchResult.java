package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import searchengine.model.Page;

@Data
@AllArgsConstructor
public class SearchResult implements Comparable<SearchResult> {

    private Page page;
    private Float absoluteRelevance;

    @Override
    public int compareTo(SearchResult o) {
        if (o == null) {
            return -1;
        }
        return -Float.compare(this.absoluteRelevance, o.absoluteRelevance);
    }

    @Override
    public String toString() {
        return page.getPath() + "\trank: " + absoluteRelevance;
    }
}