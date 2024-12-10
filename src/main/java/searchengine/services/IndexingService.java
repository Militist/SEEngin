package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;


@Service
public class IndexingService {


    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private SitesList sitesList;

    public synchronized void indexAllSites() {

        List<Site> sites = sitesList.getSites();

        if (sites == null || sites.isEmpty()) {
            System.out.println("No sites found to index.");
            return;
        }

        System.out.println("Starting indexing all sites from thread: " + Thread.currentThread().getName());

        for (Site site : sites) {
            try {
                SiteEntity siteEntity = Converter.toSiteEntity(site);

                executorService.submit(() -> indexSite(siteEntity));
            } catch (IllegalArgumentException e) {
                System.err.println("Error converting Site to SiteEntity: " + e.getMessage());
            }
        }
    }

    private void indexSite(SiteEntity site) {
        System.out.println("Starting indexing all sites...");
        Queue<String> toVisit = new LinkedList<>();

        String siteUrl = site.getUrl();
        if (siteUrl == null) {
            System.err.println("URL равен null для site: " + site.getName());
            return;
        }

        toVisit.add(siteUrl);
        int maxDepth = 2;

        try {
            while (!toVisit.isEmpty()) {
                String currentUrl = toVisit.poll();
                Document doc = null;
                try {
                    doc = Jsoup.connect(currentUrl)
                            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .referrer("http://www.google.com")
                            .method(Connection.Method.GET)
                            .get();
                    SiteCrawler crawler = new SiteCrawler(maxDepth);
                    crawler.getPageLinks(currentUrl, 0);

                    ForkJoinPool pool = new ForkJoinPool();
                    List<Element> elementList = new ArrayList<>(crawler.getLinks());
                    Data dataTask = new Data(elementList, 0, elementList.size(), doc, site);

                    pool.invoke(dataTask);

                    site.setName(doc.title());
                    site.setStatusTime(LocalDateTime.now());

                    for (Element link : doc.select("a[href]")) {
                        String absHref = link.attr("abs:href");
                        if (absHref != null && !absHref.isEmpty()) {
                            toVisit.add(absHref);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("IOException occurred: " + e.getMessage());
                    handleFailure(site, "Ошибка при обработке URL " + currentUrl + ": " + e.getMessage());
                    deleteSiteData(site.getId());
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    site.setLastError("Unexpected Error: " + e.getMessage());
                    handleFailure(site, "Непредвиденная ошибка " + currentUrl + ": " + e.getMessage());
                    return;
                }

            }

            site.setType(Status.INDEXED);
            siteRepository.save(site);
            System.out.println("Saved site successfully: " + site);


        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
            handleFailure(site, "Общая ошибка: " + e.getMessage());
            deleteSiteData(site.getId());
        }

    }

    private void handleFailure(SiteEntity site, String errorMessage) {

        site.setType(Status.FAILED);
        site.setLastError(errorMessage);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);

    }

    private void deleteSiteData(int siteId) {

        pageRepository.deleteBySiteId(siteId);
        siteRepository.deleteById(siteId);

    }

}
