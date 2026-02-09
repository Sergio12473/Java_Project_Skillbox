package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchDataResponse;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResult;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private SearchRequest request;
    private String regexSearchBar;
    private String regexBold;
    private List<Lemma> lemmaList;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final ExtractText extractText;

    @Override
    public SearchResponse search(SearchRequest request) {

        this.request = request;
        Set<String> words = extractText.getWords(request.getQuery()).keySet();

        if (words.isEmpty()) {
            return new SearchResponse(false, "Некорректно указаны параметры поиска", 400);
        }

        lemmaList = getAlLLemmaExcludingFrequent(words, request.getSite());

        if (lemmaList.isEmpty()) {
            return new SearchResponse(false, "Ошибка при обработке поискового запроса", 400);
        }

        List<Index> indexList = indexRepository.findByLemmaInOrderByPageAscRankDesc(lemmaList);
        List<SearchResult> searchResult = searchPageResult(indexList);

        if (searchResult.isEmpty()) {
            return new SearchResponse(false, "Ничего не найдено(", 400);
        }

        return new SearchResponse(true, searchResult.size(), getDataList(searchResult), "", 200);
    }

    private String getSnippet(String content) {

        Elements elements = Jsoup.parse(content).body().select(regexSearchBar);

        if (elements.isEmpty()) {
            return "";
        }

        String text = elements.text();
        text = text.length() < 240 ? text : text.substring(0, 237) + "...";
        text = text.replaceAll(regexBold, "<b>$1</b>");

        return "<div>" + text + "</div>";
    }

    private String getRegex() {
        List<String> wordsToBold = new ArrayList<>();
        lemmaList.forEach(i -> wordsToBold.add(extractText.delEndWord(i.getLemma())));
        return "(?iu)(" + String.join("|", wordsToBold) + ")";
    }

    private String getSearchBar() {
        ArrayList<String> searchBar = new ArrayList<>();
        lemmaList.forEach(i -> searchBar.add(":matchesOwn(" + "(?iu)(" + extractText.delEndWord(i.getLemma()) + "))"));
        return String.join(",", searchBar);
    }

    private List<SearchResult> searchPageResult(List<Index> collection) {

        List<SearchResult> searchResults = new ArrayList<>();
        SearchResult result = null;

        for (Index index : collection) {
            if (result == null) {
                result = new SearchResult(index.getPage(), index.getRank());
                continue;
            }
            if (result.getPage() == index.getPage()) {
                result.setAbsoluteRelevance(result.getAbsoluteRelevance() + index.getRank());
                continue;
            }

            searchResults.add(result);
            result = new SearchResult(index.getPage(), index.getRank());
        }
        Collections.sort(searchResults);
        return searchResults;
    }

    private List<SearchDataResponse> getDataList(List<SearchResult> searchResults) {

        List<SearchDataResponse> dataList = new ArrayList<>();
        int end = request.getOffset() + request.getLimit();
        int start = request.getOffset();
        float maxRelevance = searchResults.get(0).getAbsoluteRelevance();

        regexSearchBar = getSearchBar();
        regexBold = getRegex();

        for (int i = start; i < searchResults.size() && i <= end; i++) {
            SearchDataResponse data = new SearchDataResponse();
            Page page = searchResults.get(i).getPage();
            Site site = page.getSite();

            data.setUri(page.getPath());
            data.setRelevance(searchResults.get(i).getAbsoluteRelevance() / maxRelevance);
            data.setTitle(Jsoup.parse(page.getContent()).title());
            data.setSnippet(getSnippet(page.getContent()));
            data.setSite(site.getUrl());
            data.setSiteName(site.getName());
            dataList.add(data);
        }
        return dataList;
    }

    private List<Lemma> getAlLLemmaExcludingFrequent(Collection<String> words, String sitePath) {
        List<Lemma> lemmaList = sitePath.equals("all") ?
                lemmaRepository.findByLemmaInOrderByFrequencyAsc(words) :
                lemmaRepository.findByLemmaInAndSite_UrlIgnoreCaseOrderByFrequencyAsc(words, sitePath);
        return clearManyFrequent(lemmaList);
    }

    private List<Lemma> clearManyFrequent(List<Lemma> lemmaList) {
        List<Lemma> newLemmaList = new ArrayList<>();

        for (Lemma lemma : lemmaList) {
            if ((double) lemma.getFrequency() / pageRepository.countBySite(lemma.getSite()) < 0.8) {
                newLemmaList.add(lemma);
            } else {
                log.info("Remove lemma: {}", lemma.getLemma());
            }
        }
        return newLemmaList;
    }
}