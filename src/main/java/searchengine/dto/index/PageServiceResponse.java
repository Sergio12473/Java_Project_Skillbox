package searchengine.dto.index;

import lombok.Data;
import org.jsoup.nodes.Document;
import searchengine.model.Page;

@Data
public class PageServiceResponse {

    private boolean result;
    private Page page;
    private String error;
    private Document document;

    public PageServiceResponse(boolean result, Page page, String error, Document document) {
        this.result = result;
        this.page = page;
        this.error = error;
        this.document = document;
    }

    public PageServiceResponse(Page page, Document document) {
        this(true, page, "", document);
    }

    public PageServiceResponse(Page page, String error) {
        this(false, page, error, null);
    }
}