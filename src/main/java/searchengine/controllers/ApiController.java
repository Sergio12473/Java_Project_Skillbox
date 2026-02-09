package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexResponse> startIndexing() {
        log.info("Индексация началась");
        return ResponseEntity.ok(indexService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexResponse> stopIndexing() throws InterruptedException {
        log.info("Индексация остановлена");
        return ResponseEntity.ok(indexService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexResponse> startIndexingPage(@RequestBody String url) {
        log.info("Индексация страницы {} началась", url);
        return ResponseEntity.ok(indexService.startIndexingPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchWithSite(@RequestParam("query") String query,
                                                         @RequestParam(defaultValue = "all") String site,
                                                         @RequestParam("offset") int offset,
                                                         @RequestParam("limit") int limit) {
        log.info("Поиск");
        return ResponseEntity.ok(searchService.search(new SearchRequest(query, site, offset, limit)));
    }
}
