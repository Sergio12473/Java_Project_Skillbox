package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import searchengine.dto.index.HTTPResponse;
import searchengine.dto.index.PageServiceResponse;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Getter
@Setter
@Component
public class SiteParser extends RecursiveTask<HTTPResponse> {

    private static boolean stop;
    private Site site;
    private Page page;
    private long start;
    private static long count;
    private long number;
    private IndexServiceImpl parent;

    public SiteParser(IndexServiceImpl parent) {
        this.parent = parent;
        start = System.currentTimeMillis();
        number = count++;
    }

    public void setPage(Page page) {
        this.page = page;
        this.site = page.getSite();
    }

    public void setSite(Site site) {
        this.page = new Page(site);
        this.site = site;
        WebSiteService.initCache(site);
        parent.getPageRepository().save(page);
    }

    @Override
    protected HTTPResponse compute() {

        if (stop) {
            return new HTTPResponse(false, "Индексация остановлена пользователем");
        }

        parent.getWebSiteService().delUrlByCache(page);
        PageServiceResponse response = parent.getWebSiteService().getPage(page);
        parent.getPageRepository().updateCodeAndContentById(page.getCode(), page.getContent(), page.getId());

        if (response.isResult()) {
            parent.saveRelatedData(page);
            List<Page> pages = parent.getWebSiteService().getPages(response.getDocument(), site);
            synchronized (parent.getPageRepository()) {
                parent.getPageRepository().saveAllAndFlush(pages);
            }
            startJoin(pages);
        }
        return new HTTPResponse(response.isResult(), response.getError());
    }

    private void startJoin(List<Page> pages) {
        List<SiteParser> taskList = new ArrayList<>();
        pages.forEach(page -> addJoin(taskList, page));
        taskList.forEach(ForkJoinTask::join);
    }

    private void addJoin(List<SiteParser> taskList, Page page) {
        SiteParser parser = new SiteParser(parent);
        parser.setPage(page);
        parser.fork();
        taskList.add(parser);
    }

    public static void stop() {
        SiteParser.stop = true;
    }
}