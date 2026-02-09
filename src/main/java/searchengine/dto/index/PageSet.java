package searchengine.dto.index;

import lombok.Data;

import java.util.concurrent.ConcurrentSkipListSet;

@Data
public class PageSet {
    ConcurrentSkipListSet<String> urls;

    public PageSet() {
        this.urls = new ConcurrentSkipListSet<>();
    }

    public void addUrl(String string) {
        urls.add(string);
    }

    public void remoteUrl(String string) {
        urls.remove(string);
    }
}