package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Page;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class CrawTask extends RecursiveAction {

    private final String url;
    private final int depth;
    private final int maxDepth;
    private final Set<String> links;
    private final Set<String> visitedLinks;
    private static List<Page> allPagesList = Collections.synchronizedList(new ArrayList<>());

    private SiteEntity siteEntity;

    private PageRepository pageRepository;

    public CrawTask(String url, int depth, int maxDepth, Set<String> links, Set<String> visitedLinks, PageRepository pageRepository) {
        this.url = url;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.links = links;
        this.visitedLinks = visitedLinks;
        this.siteEntity = new SiteEntity();
        this.pageRepository = pageRepository;
    }

    public static List<Page> getAllPagesList() {
        return allPagesList;
    }

    @Override
    protected void compute() {
        if (depth > maxDepth || visitedLinks.contains(url)) {
            return;
        }
        synchronized (visitedLinks) {
            visitedLinks.add(url);
        }

        Document document;

        try {
            document = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();

            String contentType = document.connection().response().contentType();
            int statusCode = document.connection().response().statusCode();

            if (statusCode == 200 && contentType != null && !contentType.contains("text/html")) {
                return;
            }

            Elements linkOnPage = document.select("a[href]");

            for (Element element : linkOnPage) {
                String absUrl = element.attr("abs:href");

                if (!absUrl.isEmpty() && !visitedLinks.contains(absUrl)) {
                    links.add(absUrl);
                    processPage(absUrl, statusCode, element);
                }

                Page page = new Page();
                page.setPath(absUrl);
                page.setCode(statusCode);
                if (contentType != null && contentType.matches("^(text/|application/(?!json).*|application/xml).*")) {

                    page.setContent(element.text());

                }
                saveToDatabasePageEntity(page, siteEntity);

                List<CrawTask> subTasks = createSubTasks();
                invokeAll(subTasks);
            }

            Thread.sleep(2000);

        } catch (IOException e) {
            System.out.println("access error '" + url + "': " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveToDatabasePageEntity(Page page, SiteEntity siteEntity) {

        if (page == null || siteEntity == null || page.getPath() == null || page.getPath().isEmpty()) {
            throw new IllegalArgumentException("Page, SiteEntity или путь Page не должны быть null или пустыми");
        }

        Optional<PageEntity> existingPageEntity = pageRepository.findByUrl(page.getPath());

        if (existingPageEntity.isPresent()) {
            PageEntity pageEntity = existingPageEntity.get();
            pageEntity.setContent(page.getContent());
            pageEntity.setCode(page.getCode());
            pageRepository.save(pageEntity);

        } else {
            PageEntity pageEntity = Converter.toPageEntity(page, siteEntity);
            pageRepository.save(pageEntity);
        }
    }

    private void processPage(String absUrl, int statusCode, Element element) {
        Page page = new Page();
        page.setPath(absUrl);
        page.setCode(statusCode);
        page.setContent(element.text());

        saveToDatabasePageEntity(page, siteEntity);
    }


    private List<CrawTask> createSubTasks() {
        List<CrawTask> subTasks = new ArrayList<>();
        for (String urlLink : links) {
            if (!visitedLinks.contains(urlLink)) {
                subTasks.add(new CrawTask(urlLink, depth + 1, maxDepth, links, visitedLinks, pageRepository));
            }
        }
        return subTasks;
    }
}
