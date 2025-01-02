package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Page;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class IndexingService_01 implements Callable<Site> {

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;

    private Site site;

    public IndexingService_01(Site site) {
        this.site = site;
    }

    @Override
    public Site call() throws Exception {
        int maxDepth = 3;

        SiteEntity siteEntity = Converter.toSiteEntity(site);

        siteEntity.setType(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());

        SiteEntity savedSite = siteRepository.save(siteEntity);

        try {

            SiteCrawler_01 crawler01 = new SiteCrawler_01(maxDepth);

            try {
                if (crawler01.isSiteAvailable(site.getUrl())) {
                    crawler01.getPageLinks(siteEntity.getUrl());
                } else {
                    System.out.println("Сайт недоступен: " + site.getUrl());
                }
            } catch (Exception e) {
                System.err.println("Ошибка при получении страниц из " + site.getUrl() + ": " + e.getMessage());
            }

            List<Page> resultPageList = CrawTask_01.getPageList();

            List<PageEntity> pageEntityList = resultPageList.stream()
                    .map(page -> Converter.toPageEntity(page, savedSite))
                    .collect(Collectors.toList());

            for (PageEntity pageEntity : pageEntityList) {
                pageRepository.save(pageEntity);
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

        return site;
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

//    public void createSiteEntityWithPages(Site site) {
//        int maxDepth = 3;
//
//        SiteEntity siteEntity = Converter.toSiteEntity(site);
//
//        siteEntity.setType(Status.INDEXING);
//        siteEntity.setStatusTime(LocalDateTime.now());
//
//        SiteEntity savedSite = siteRepository.save(siteEntity);
//
//        SiteCrawler_01 crawler01 = new SiteCrawler_01(maxDepth);
//        crawler01.getPageLinks(siteEntity.getUrl());
//
//        List<Page> resultPageList = CrawTask_01.getPageList();
//
//        List<PageEntity> pageEntityList = resultPageList.stream()
//                .map(page -> Converter.toPageEntity(page, savedSite))
//                .collect(Collectors.toList());
//
//        for (PageEntity pageEntity : pageEntityList) {
//            pageRepository.save(pageEntity);
//        }
//
//    }

    public static class SiteCrawler_01 {
        private Set<String> links;
        private Set<String> visitedLinks;
        private final int maxDepth;

        public SiteCrawler_01(int maxDepth) {
            this.maxDepth = maxDepth;
            this.links = new HashSet<>();
            this.visitedLinks = new HashSet<>();
        }

        public void getPageLinks(String startUrl) {
            ForkJoinPool poll = new ForkJoinPool();
            poll.invoke(new CrawTask_01(startUrl, 0, maxDepth, links, visitedLinks));
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

    public static class CrawTask_01 extends RecursiveAction {

        private final String url;
        private final int depth;
        private final int maxDepth;
        private final Set<String> links;
        private final Set<String> visitedLinks;
        private static final List<Page> pageList = new ArrayList<>();


        public CrawTask_01(String url, int depth, int maxDepth, Set<String> links,
                           Set<String> visitedLinks) {
            this.url = url;
            this.depth = depth;
            this.maxDepth = maxDepth;
            this.links = links;
            this.visitedLinks = visitedLinks;
        }

        public static List<Page> getPageList() {
            return pageList;
        }

        @Override
        protected void compute() {
            if (depth > maxDepth || visitedLinks.contains(url)) {
                return;
            }
            synchronized (visitedLinks) {
                if (visitedLinks.add(url)) {
                    processUrl();
                } else {
                    System.out.println("Ссылка '" + url + "' уже была посещена.");
                }
            }
        }

        private void processUrl() {
            Document document;

            try {
                document = Jsoup.connect(url)
                        .timeout(10000)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36")
                        .referrer("http://www.google.com")
                        .get();

                handleResponse(document);

                Thread.sleep(2000); // Ожидаем между запросами

            } catch (IOException e) {
                System.out.println("Access error '" + url + "': " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
            }
        }

        private void handleResponse(Document document) {
            int statusCode = document.connection().response().statusCode();
            String contentType = document.connection().response().contentType();

            if (statusCode != 200 || contentType == null || contentType.contains("text/html")) {
                return;
            }

            Elements linkOnPage = document.select("a[href]");
            for (Element element : linkOnPage) {
                String absUrl = element.attr("abs:href");
                if (!absUrl.isEmpty() && visitedLinks.add(absUrl)) {
                    links.add(absUrl);
                    processPage(absUrl, statusCode, element);
                    invokeAll(createSubTasks());
                }
            }
        }

        private List<CrawTask_01> createSubTasks() {
            List<CrawTask_01> subTasks = new ArrayList<>();
            for (String urlLink : links) {
                if (!visitedLinks.contains(urlLink)) {
                    subTasks.add(new CrawTask_01(urlLink, depth + 1, maxDepth, links, visitedLinks));
                }
            }
            return subTasks;
        }

        private List<Page> processPage(String absUrl, int statusCode, Element element) {
            Page page = new Page();
            page.setPath(absUrl);
            page.setCode(statusCode);
            page.setContent(element.text());

            System.out.println("Сохраняем страницу: " + page.getPath());

            pageList.add(page);
            return pageList;
        }
    }

}
