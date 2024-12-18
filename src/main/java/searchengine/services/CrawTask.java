package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Page;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;

public class CrawTask extends RecursiveAction {

    private final String url;
    private final int depth;
    private final int maxDepth;
    private final Set<String> links;
    private final Set<String> visitedLinks;
    private static List<Page> allPagesList = new CopyOnWriteArrayList<>();

    private Document document;

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
        visitedLinks.add(url);

        String content = "";
        int statusCode = 0;

        try {
            document = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();

            String contentType = document.connection().response().contentType();

            if (contentType != null && contentType.matches("^(text/|application/(?!json).*|application/xml).*")) {
                content = document.body().text();
                statusCode = document.connection().response().statusCode();
            }

            Elements linkOnPage = document.select("a[href]");

            for (Element element : linkOnPage) {
                String absUrl = element.attr("abs:href");
                if (absUrl.startsWith("http://") || absUrl.startsWith("https://")) {
                    Page page = new Page();
                    page.setPath(url);
                    page.setCode(statusCode);
                    page.setContent(content);
                    allPagesList.add(page);

                    new CrawTask(absUrl, depth + 1, maxDepth, links, visitedLinks);
                }
            }

            Thread.sleep(2000);

        } catch (IOException e) {
            System.out.println("access error '" + url + "': " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
