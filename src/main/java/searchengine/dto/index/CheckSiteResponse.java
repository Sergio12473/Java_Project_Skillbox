package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import searchengine.model.Site;

@Data
@AllArgsConstructor
public class CheckSiteResponse {
    private boolean result;
    private Site site;
}