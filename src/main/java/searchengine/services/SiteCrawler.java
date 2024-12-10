package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SiteCrawler {

    private Set<Element> links;
    private Set<String> visitedLinks;
    private final int maxDepth;

    public SiteCrawler(int maxDepth) {
        this.maxDepth = maxDepth;
        this.links = new HashSet<>();
        this.visitedLinks = new HashSet<>();
    }

    public Set<Element> getLinks() {
        return links;
    }

    public void getPageLinks(String url, int depth) {
        if (depth > maxDepth || visitedLinks.contains(url)) {
            return;
        }

        visitedLinks.add(url);

        try {
            Document document = Jsoup.connect(url).get();
            Elements linkOnPage = document.select("a[href]");

            for (Element page : linkOnPage) {
                String absUrl = page.attr("abs:href");
                if (absUrl.startsWith("http://") || absUrl.startsWith("https://")) {
                    links.add(page);
                    getPageLinks(absUrl, depth + 1);
                } else {
                    System.out.println("Skipping malformed URL: " + absUrl);
                }
            }

        } catch (IOException e) {
            System.out.println("For '" + url + "': " + e.getMessage());
        }
    }

}
