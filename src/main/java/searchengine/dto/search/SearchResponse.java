package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchDataResponse> data;
    private String error;
    private int code;

    public SearchResponse(boolean result, String error, int code) {
        this.result = result;
        this.error = error;
        this.code = code;
    }

    public SearchResponse(boolean result, int count, List<SearchDataResponse> data, String error) {
        this.result = result;
        this.count = count;
        this.data = data;
        this.error = error;
        this.code = 200;
    }
}