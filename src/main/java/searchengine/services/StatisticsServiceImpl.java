package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    @Override
    public StatisticsResponse getStatistics() {
        total = new TotalStatistics();
        detailed = new ArrayList<>();
        List<searchengine.model.Site> sitesListBD = siteRepository.findAll();
        List<searchengine.config.Site> resultList = getNonIndexedSite(sitesListBD);
        total.setSites(sites.getSites().size());
        total.setIndexing(sitesListBD.size() > 0);
        fillDetailedStatisticsSiteToBD(sitesListBD);
        fillDetailedStatisticsNonIndexed(resultList);
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private List<searchengine.config.Site> getNonIndexedSite(List<searchengine.model.Site> sitesListBD) {
        List<searchengine.config.Site> siteListSett = sites.getSites();
        return siteListSett.stream()
                .filter(siteSett -> sitesListBD.stream()
                        .map(searchengine.model.Site::getUrl)
                        .noneMatch(url -> url.equals(siteSett.getUrl()))).toList();
    }

    private void fillDetailedStatisticsSiteToBD(List<searchengine.model.Site> siteList) {

        for (searchengine.model.Site site : siteList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = (int) pageRepository.countBySite(site);
            int lemmas = (int) lemmaRepository.countBySite(site);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus().name());
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }
    }

    private void fillDetailedStatisticsNonIndexed(List<searchengine.config.Site> siteList) {
        for (searchengine.config.Site site : siteList) {

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setStatusTime(System.currentTimeMillis());
            item.setError("Индексация еще не запускалась");
            detailed.add(item);
        }
    }
}
