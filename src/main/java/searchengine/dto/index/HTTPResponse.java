package searchengine.dto.index;

import lombok.Data;

@Data
public class HTTPResponse {
    private boolean result;
    private String error;

    public HTTPResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}