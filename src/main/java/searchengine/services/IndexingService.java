package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Service
public class IndexingService {


    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    @Autowired
    private SitesList sitesList;

    public void indexAllSites() {

        List<Site> sites = sitesList.getSites();

        if (sites == null || sites.isEmpty()) {
            System.out.println("No sites found to index.");
            return;
        }

        System.out.println("Starting indexing all sites from thread: " + Thread.currentThread().getName());

        for (Site site : sites) {
            try {

                executorService.submit(() -> indexSite(site));

            } catch (IllegalArgumentException e) {
                System.err.println("Error converting Site to SiteEntity: " + e.getMessage());
            }
        }
        shutdown();
    }

    private void indexSite(Site site) {
        System.out.println("Starting indexing all sites...");
        Queue<String> toVisit = new LinkedList<>();

        String siteUrl = site.getUrl();
        if (siteUrl == null) {
            System.err.println("URL равен null для site: " + site.getName());
            return;
        }

        toVisit.add(siteUrl);
        int maxDepth = 3;
        SiteEntity siteEntity = new SiteEntity();

        try {
            siteEntity = Converter.toSiteEntity(site);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка при преобразовании Site в SiteEntity: " + e.getMessage());
            return;
        }

        siteEntity.setType(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());

        try {
            String currentUrl;
            while (!toVisit.isEmpty()) {
                currentUrl = toVisit.poll();
                siteRepository.save(siteEntity);

                SiteCrawler crawler = new SiteCrawler(maxDepth);
                crawler.getPageLinks(currentUrl);

                CrawTask.createMapUniqueLinks(siteEntity, pageRepository);

            }

            siteEntity.setType(Status.INDEXED);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
            System.out.println("Saved site successfully: " + site);

        } catch (Exception e) {
            System.err.println("Ошибка при обработке URL: " + e.getMessage());
            handleFailure(siteEntity, "Общая ошибка: " + e.getMessage());
            deleteSiteData(siteEntity.getId());
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

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public void shotDownNow() {
        executorService.shutdownNow();
    }
}
