package searchengine.services;

import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class SiteCrawler {

    private Set<String> links;
    private Set<String> visitedLinks;
    private final int maxDepth;
    private PageRepository pageRepository;

    public SiteCrawler(int maxDepth) {
        this.maxDepth = maxDepth;
        this.links = new HashSet<>();
        this.visitedLinks = new HashSet<>();
    }

    public Set<String> getLinks() {
        return links;
    }

    public void getPageLinks(String startUrl) {
        ForkJoinPool poll = new ForkJoinPool();
        poll.invoke(new CrawTask(startUrl, 0, maxDepth, links, visitedLinks, pageRepository));
    }
}
