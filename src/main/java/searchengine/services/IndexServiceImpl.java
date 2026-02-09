package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.index.CheckSiteResponse;
import searchengine.dto.index.HTTPResponse;
import searchengine.dto.index.IndexResponse;
import searchengine.dto.index.PageServiceResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexServiceImpl implements IndexService {

    private final SitesList sitesList;
    private ForkJoinPool forkJoinPool;
    private ConcurrentHashMap<Site, ForkJoinTask<HTTPResponse>> scheduledTasks;
    private boolean isIndexing;
    private final long delay = 5000L;
    @Getter
    private final WebSiteService webSiteService;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    @Getter
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final ExtractText extractText;
    private ScheduledExecutorService executorService;

    @Override
    public IndexResponse startIndexing() {

        if (isIndexing) {
            return new IndexResponse(false, "Индексация уже запущена");
        }

        if (sitesList == null) {
            return new IndexResponse(false, "Индексация не запущена");
        }

        init();
        parsingSite(sitesList.getSites());
        return new IndexResponse(true, "");
    }

    @Override
    public IndexResponse stopIndexing() throws InterruptedException {

        if (!isIndexing) {
            return new IndexResponse(false, "Индексация не запущена");
        }
        SiteParser.stop();
        Thread.sleep(delay * 3);
        stopThread();
        return new IndexResponse(true, "");
    }

    @Override
    public IndexResponse startIndexingPage(String url) {

        url = convertUrl(url);

        if (url.isEmpty()) {
            return new IndexResponse(false, "Не указан адрес страницы");
        }

        CheckSiteResponse checkResponse = checkAndGetSite(url);
        if (!checkResponse.isResult()) {
            return new IndexResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        PageServiceResponse response = webSiteService.getPage(url, checkResponse.getSite());

        if (!response.isResult()) {
            return new IndexResponse(false, response.getError());
        }

        deletePageAllAndRelatedData(response.getPage());
        savePageAndRelatedData(response.getPage());

        log.info("Сайт '{}' проиндексирован. Ошибки: {}", response.getPage().getPath(), response.getError());

        return new IndexResponse(true, "");
    }

    private void init() {
        forkJoinPool = new ForkJoinPool();
        scheduledTasks = new ConcurrentHashMap<>();
        isIndexing = true;
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::checkTaskList
                , delay * 2
                , delay
                , TimeUnit.MILLISECONDS);
    }

    private String convertUrl(String url) {
        url = url.replace("%2F", "/");
        url = url.replace("%3A", ":");
        url = url.replace("url=", "");
        return url;
    }

    private CheckSiteResponse checkAndGetSite(String url) {

        searchengine.config.Site site = searchSite(url);

        if (site == null) {
            return new CheckSiteResponse(false, null);
        }

        Optional<Site> optional = siteRepository.findByUrlLikeIgnoreCase(site.getUrl());

        if (optional.isEmpty()) {
            searchengine.model.Site siteDTO = new searchengine.model.Site(IndexStatus.INDEXED, site.getUrl(), site.getName());
            siteRepository.save(siteDTO);
            return new CheckSiteResponse(true, siteDTO);
        }

        return new CheckSiteResponse(true, optional.get());
    }

    private searchengine.config.Site searchSite(String url) {
        List<searchengine.config.Site> sites = sitesList.getSites();
        String siteRegex = "https?://(?:www\\.|)" + webSiteService.getDomainName(url);

        for (searchengine.config.Site value : sites) {
            if (value.getUrl().matches(siteRegex)) {
                return value;
            }
        }
        return null;
    }

    private searchengine.model.Site getDTOSite(searchengine.config.Site site) {
        searchengine.model.Site dtoSite = new searchengine.model.Site();
        dtoSite.setStatus(IndexStatus.INDEXING);
        dtoSite.setStatusTime(LocalDateTime.now());
        dtoSite.setUrl(site.getUrl());
        dtoSite.setName(site.getName());
        return dtoSite;
    }

    private void parsingSite(List<searchengine.config.Site> siteList) {

        for (searchengine.config.Site site : siteList) {
            deleteSiteAndRelatedData(site.getUrl());
            searchengine.model.Site dtoSite = getDTOSite(site);
            siteRepository.save(dtoSite);

            try {
                SiteParser siteParser = new SiteParser(this);
                siteParser.setSite(dtoSite);
                scheduledTasks.put(dtoSite, forkJoinPool.submit(siteParser));
            } catch (Exception e) {
                updateSite(dtoSite, e);
            }
        }
    }

    private void checkTaskList() {

        if (isCompleteTask()) {
            return;
        }

        for (searchengine.model.Site site : scheduledTasks.keySet()) {

            ForkJoinTask<HTTPResponse> task = scheduledTasks.get(site);

            if (!task.isDone()) {
                updateSite(site);
                continue;
            }

            try {
                HTTPResponse response = task.get();
                log.info("Сайт '{}' Проиндексирован. {}",
                        site.getName(),
                        response.getError().isEmpty() ? "No errors" : "Error: " + response.getError());
                updateSite(site, response);
            } catch (InterruptedException | ExecutionException e) {
                updateSite(site, IndexStatus.FAILED, e.getMessage());
                log.error("Ошибки: {}", e.getMessage());
            }
            scheduledTasks.remove(site);
        }
    }

    private boolean isCompleteTask() {
        if (scheduledTasks.isEmpty()) {
            stopThread();
        }
        return !isIndexing;
    }

    private void stopThread() {

        isIndexing = false;
        forkJoinPool.shutdown();
        executorService.shutdown();

        for (searchengine.model.Site site : scheduledTasks.keySet()) {
            updateSite(site, IndexStatus.FAILED, "Индексация остановлена пользователем");
            log.info("Индексация сайта {} остановлена пользователем", site.getName());
        }
    }

    private void deleteSiteAndRelatedData(String url) {
        Optional<searchengine.model.Site> optionalSite = siteRepository.findByUrlIgnoreCase(url);
        if (optionalSite.isPresent()) {
            searchengine.model.Site site = optionalSite.get();
            deletePageAllAndRelatedData(site);
            lemmaRepository.deleteBySite(site);
            siteRepository.delete(site);
        }
    }

    private void deletePageAllAndRelatedData(searchengine.model.Site site) {
        deleteRelatedData(pageRepository.findBySite(site));
    }

    private void deletePageAllAndRelatedData(Page page) {
        deleteRelatedData(pageRepository.findBySiteAndPathIgnoreCase(page.getSite(), page.getPath()));
    }

    private void deleteRelatedData(List<Page> pageList) {
        if (pageList.isEmpty()) {
            return;
        }
        indexRepository.deleteByPageInAllIgnoreCase(pageList);
        pageRepository.deleteByIdIn(getIdList(pageList));
    }

    private static List<Integer> getIdList(List<Page> pageList) {
        List<Integer> idList = new ArrayList<>();
        pageList.forEach(page -> idList.add(page.getId()));
        return idList;
    }

    private void updateSite(searchengine.model.Site site, IndexStatus status, String error) {
        synchronized (siteRepository) {
            siteRepository.updateStatusAndStatusTimeAndLastErrorById(status, LocalDateTime.now(), error, site.getId());
        }
    }

    private void updateSite(searchengine.model.Site site, HTTPResponse result) {
        updateSite(site, result.isResult() ? IndexStatus.INDEXED : IndexStatus.FAILED, result.getError());
    }

    private void updateSite(searchengine.model.Site site, Exception result) {
        updateSite(site, IndexStatus.FAILED, result.getMessage());
    }

    private void updateSite(searchengine.model.Site site) {
        updateSite(site, IndexStatus.INDEXING, "");
    }

    public void savePageAndRelatedData(Page page) {
        pageRepository.save(page);
        saveRelatedData(page);
    }

    public void saveRelatedData(Page page) {
        String text = Jsoup.parse(page.getContent()).text();
        HashMap<String, Integer> words = extractText.getWords(text);
        HashMap<Lemma, Integer> lemmaList = saveOrUpdateLemmList(words, page.getSite());
        Set<Index> indexList = getIndexList(lemmaList, page);
        synchronized (indexRepository) {
            indexRepository.saveAllAndFlush(indexList);
        }
    }

    private Set<Index> getIndexList(HashMap<Lemma, Integer> lemmas, Page page) {
        Set<Index> indexMap = new HashSet<>();
        for (Lemma lemma : lemmas.keySet()) {
            indexMap.add(new Index(page, lemma, (float) lemmas.get(lemma)));
        }
        return indexMap;
    }

    public HashMap<Lemma, Integer> saveOrUpdateLemmList(HashMap<String, Integer> words, searchengine.model.Site site) {
        HashMap<Lemma, Integer> lemmaList = new HashMap<>();
        for (String word : words.keySet()) {
            synchronized (lemmaRepository) {
                Optional<Lemma> optional = lemmaRepository.findBySiteAndLemma(site, word);
                Lemma lemma = optional.orElseGet(() -> new Lemma(site, word));
                lemma.incrementFrequency();
                lemmaList.put(lemmaRepository.save(lemma), words.get(word));
            }
        }
        return lemmaList;
    }
}