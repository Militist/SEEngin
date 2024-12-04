package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class IndexingService {


    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private SitesList sitesList;

    public void indexAllSites() {

        List<Site> sites = sitesList.getSites();

        for (Site site : sites) {
            SiteEntity siteEntity = Converter.toSiteEntity(site);
            executorService.submit(() -> indexSite(siteEntity));
        }
    }

    private void indexSite(SiteEntity site) {
        System.out.println("Starting indexing all sites...");
        Queue<String> toVisit = new LinkedList<>();
        toVisit.add(site.getUrl());

        try {
            while (!toVisit.isEmpty()) {
                String currentUrl = toVisit.poll();
                try {
                    Document doc = Jsoup.connect(currentUrl).get();
                    String content = doc.outerHtml();
                    Page page = new Page();
                    page.setId(site.getId());
                    page.setPath(currentUrl.substring(site.getUrl().length()));
                    page.setCode(200);
                    page.setContent(content);
                    System.out.println("Saving page for URL: " + currentUrl);
                    pageRepository.save(page);
                    System.out.println("Saved page successfully");

                    site.setStatusTime(LocalDateTime.now());

                    System.out.println("Saving site: " + site);
                    siteRepository.save(site);
                    System.out.println("Saved site successfully");

                    for (Element link : doc.select("a[href]")) {
                        String absHref = link.attr("abs:href");
                        toVisit.add(absHref);
                    }
                } catch (IOException e) {
                    System.err.println("IOException occurred: " + e.getMessage());
                    handleFailure(site, "Ошибка при обработке URL " + currentUrl + ": " + e.getMessage());
                    deleteSiteData(site.getId());
                    return;
                }
            }

            site.setType(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

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
