package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;


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
            executorService.submit(() -> {
                try {
                    indexSite(site);
                } catch (Exception e) {
                    System.err.println("Error indexing site " + site.getName() + ": " + e.getMessage());
                }
            });
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
        SiteEntity siteEntity;

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
                System.out.println("Обработка URL: " + currentUrl);
                siteRepository.save(siteEntity);

                SiteCrawler crawler = new SiteCrawler(maxDepth, pageRepository);
                try {
                    if (isSiteAvailable(currentUrl)) {
                        crawler.getPageLinks(currentUrl);
                    } else {
                        System.out.println("Сайт недоступен, пропускаем " + currentUrl);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при получении страниц из " + currentUrl + ": " + e.getMessage());
                    continue;
                }

            }

            siteEntity.setType(Status.INDEXED);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
            System.out.println("Saved site successfully: " + site);

        } catch (Exception e) {
            System.err.println("Ошибка при обработке URL тут: " + e.getMessage());
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

    private boolean isSiteAvailable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (IOException e) {
            System.out.println("Сайт недоступен URL: " + url);
            return false;
        }

    }
}
