package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Page;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;

import javax.print.Doc;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class CrawTask extends RecursiveAction {

    private final String url;
    private final int depth;
    private final int maxDepth;
    private final Set<String> links;
    private final Set<String> visitedLinks;
    private static List<Page> allPagesList = new CopyOnWriteArrayList<>();

    //private Document document;

    public CrawTask(String url, int depth, int maxDepth, Set<String> links, Set<String> visitedLinks) {
        this.url = url;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.links = links;
        this.visitedLinks = visitedLinks;
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

        int statusCode = 0;
        Document document;

        try {
            document = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();

            String contentType = document.connection().response().contentType();
            statusCode = document.connection().response().statusCode();

            Elements linkOnPage = document.select("a[href]");

            for (Element element : linkOnPage) {
                String absUrl = element.attr("abs:href");
                Page page = new Page();
                page.setPath(absUrl);
                page.setCode(statusCode);
                if (contentType != null && contentType.matches("^(text/|application/(?!json).*|application/xml).*")) {
                    if (absUrl.startsWith("http://") || absUrl.startsWith("https://")) {
                        page.setContent(element.text());
                    }
                }
                allPagesList.add(page);
                new CrawTask(absUrl, depth + 1, maxDepth, links, visitedLinks);
            }

            Thread.sleep(2000);

        } catch (IOException e) {
            System.out.println("access error '" + url + "': " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createMapUniqueLinks(SiteEntity siteEntity, PageRepository pageRepository) {
        Map<String, Set<String>> values = new HashMap<>();
        Map<String, String> resultMap = new HashMap<>();
        String contentValue = "";

        List<Page> result = getAllPagesList();
        for (Page page : result) {
            String pathValue = page.getPath();
            contentValue = page.getContent();
            int code = page.getCode();
            values.computeIfAbsent(pathValue, p -> new HashSet<>()).add(contentValue);

            resultMap = preparingConservationToPageEntity(values);

            saveToDatabasePageEntity(resultMap, code, siteEntity, pageRepository);
        }
    }

    private static Map<String, String> preparingConservationToPageEntity(Map<String, Set<String>> inputMap) {
        Map<String, String> outputMap = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : inputMap.entrySet()) {
            String key = entry.getKey();
            String values = entry.getValue()
                    .stream()
                    .collect(Collectors.joining(", "));

            outputMap.put(key, values);

        }
        return outputMap;
    }

    private static void saveToDatabasePageEntity(Map<String, String> componentsPages, int codePage,
                                                 SiteEntity siteEntity, PageRepository pageRepository) {
        for (Map.Entry<String, String> entry : componentsPages.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            PageEntity pageEntity = new PageEntity();
            pageEntity.setPath(path);
            pageEntity.setContent(content);
            pageEntity.setCode(codePage);
            pageEntity.setSiteEntity(siteEntity);

            pageRepository.save(pageEntity);
        }
    }

}
