package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@Component
public class Data extends RecursiveAction {

    @Autowired
    private PageRepository pageRepository;

    private final Integer start;
    private final Integer end;
    private List<Element> tempList;

    private Document document;
    private SiteEntity siteEntity;

    public Data(List<Element> tempList, Integer start, Integer end, Document document, SiteEntity siteEntity) {
        this.tempList = tempList;
        this.start = start;
        this.end = end;
        this.document = document;
        this.siteEntity = siteEntity;
    }

    @Override
    protected void compute() {
        if (end - start <= 2) {
            for (int i = start; i < end; i++) {
                getPageStructure(i);
            }
        } else {
            int mid = (start + end) / 2;
            Data leftTask = new Data(tempList, start, mid, document, siteEntity);
            Data rightTask = new Data(tempList, mid, end, document, siteEntity);
            leftTask.fork();
            rightTask.compute();
            leftTask.join();
        }
    }

    private void getPageStructure(int index) {
        Element element = tempList.get(index);
        if (element == null) {
            System.out.println("Element at index " + index + " is null.");
            return;
        }

        String url = element.attr("abs:href");

        if (url.isEmpty() || !url.startsWith("http") || !url.startsWith("https")) {
            System.out.println("Invalid or empty URL format at index: " + index);
            return;
        }

        String content = "";
        int statusCode = 0;

        try {
            Document document = Jsoup.connect(url).get();

            String contentType = document.connection().response().contentType();
            if (contentType != null && contentType.matches("^(text/|application/(?!json).*|application/xml).*")) {
                content = document.body().text();
                statusCode = document.connection().response().statusCode();
            } else {
                System.out.println("Unhandled content type: " + contentType + " for URL: " + url);
                return;
            }
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + url + " - " + e.getMessage());
            return;
        } catch (IOException e) {
            System.out.println("Error fetching URL: " + url + " - " + e.getMessage());
            return;
        }

        Page page = new Page();
        page.setId(siteEntity.getId());
        page.setPath(url.substring(siteEntity.getUrl().length()));
        page.setCode(statusCode);
        page.setContent(content);
        pageRepository.save(page);
    }
}
