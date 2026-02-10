package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.index.IndexResponse;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final SearchService searchService;
    private final IndexService indexService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public IndexResponse startIndexing() {
        log.info("Индексация началась");
        return indexService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public IndexResponse stopIndexing() throws InterruptedException {
        log.info("Индексация остановлена");
        return indexService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public IndexResponse startIndexingPage(@RequestBody String url) {
        log.info("Индексация страницы {} началась", url);
        return indexService.startIndexingPage(url);
    }

    @GetMapping("/search")
    public SearchResponse searchWithSite(@RequestParam("query") String query,
                                                         @RequestParam(defaultValue = "all") String site,
                                                         @RequestParam("offset") int offset,
                                                         @RequestParam("limit") int limit) {
        log.info("Поиск");
        return searchService.search(new SearchRequest(query, site, offset, limit));
    }
}
